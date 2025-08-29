package com.bluelotuscoding.eidolonunchained.chat;

import com.bluelotuscoding.eidolonunchained.ai.PrayerAIConfig;
import com.bluelotuscoding.eidolonunchained.ai.JudgmentConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side only conversation history manager using world saved data.
 * Stores conversation history, AI configurations, and API keys in world data folder.
 * Only server operators can manage conversation history and AI settings.
 * 
 * Implements hybrid configuration system:
 * Priority: Server Override > JSON Default > System Default
 */
public class ConversationHistoryManager extends SavedData {
    private static final Logger LOGGER = LogManager.getLogger(ConversationHistoryManager.class);
    private static final String DATA_NAME = "eidolon_unchained_conversations";
    private static final int DEFAULT_MAX_MESSAGES = 1000;
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // Configuration settings (stored in world data)
    private int maxMessagesPerDeity = DEFAULT_MAX_MESSAGES;
    private final Map<String, String> apiKeys = new ConcurrentHashMap<>(); // provider -> key
    private final Map<ResourceLocation, Map<String, Object>> deitySettings = new ConcurrentHashMap<>(); // deity -> settings
    private final Map<String, Object> globalAISettings = new ConcurrentHashMap<>(); // Global AI configuration
    
    // Player UUID -> Deity ID -> Conversation Messages
    private final Map<UUID, Map<ResourceLocation, List<ConversationMessage>>> conversations = new ConcurrentHashMap<>();
    
    // Server-side AI Configuration (moves settings from JSON to world data)
    private final Map<ResourceLocation, Map<String, Object>> deityAIConfig = new ConcurrentHashMap<>();

    public ConversationHistoryManager() {
        super();
    }
    
    /**
     * Get the conversation history manager for the current world.
     * Server-side only - automatically uses world data folder.
     */
    public static ConversationHistoryManager get() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            LOGGER.warn("Cannot access conversation history - server not available");
            return null;
        }
        
        ServerLevel overworld = server.getLevel(ServerLevel.OVERWORLD);
        if (overworld == null) {
            LOGGER.warn("Cannot access conversation history - overworld not available");
            return null;
        }
        
        return overworld.getDataStorage().computeIfAbsent(
            ConversationHistoryManager::load,
            ConversationHistoryManager::new,
            DATA_NAME
        );
    }
    
    /**
     * Load conversation data from NBT (called by Minecraft's data system)
     */
    public static ConversationHistoryManager load(CompoundTag tag) {
        ConversationHistoryManager manager = new ConversationHistoryManager();
        
        // Load configuration settings
        if (tag.contains("config")) {
            CompoundTag configTag = tag.getCompound("config");
            manager.maxMessagesPerDeity = configTag.getInt("maxMessagesPerDeity");
            if (manager.maxMessagesPerDeity <= 0) {
                manager.maxMessagesPerDeity = DEFAULT_MAX_MESSAGES;
            }
        }
        
        // Load API keys
        if (tag.contains("apiKeys")) {
            CompoundTag apiKeysTag = tag.getCompound("apiKeys");
            for (String provider : apiKeysTag.getAllKeys()) {
                manager.apiKeys.put(provider, apiKeysTag.getString(provider));
            }
        }
        
        // Load conversations
        CompoundTag conversationsTag = tag.getCompound("conversations");
        for (String playerUuidStr : conversationsTag.getAllKeys()) {
            try {
                UUID playerUuid = UUID.fromString(playerUuidStr);
                CompoundTag playerTag = conversationsTag.getCompound(playerUuidStr);
                Map<ResourceLocation, List<ConversationMessage>> playerConversations = new HashMap<>();
                
                for (String deityIdStr : playerTag.getAllKeys()) {
                    ResourceLocation deityId = new ResourceLocation(deityIdStr);
                    ListTag messagesTag = playerTag.getList(deityIdStr, Tag.TAG_COMPOUND);
                    List<ConversationMessage> messages = new ArrayList<>();
                    
                    for (int i = 0; i < messagesTag.size(); i++) {
                        CompoundTag messageTag = messagesTag.getCompound(i);
                        ConversationMessage message = ConversationMessage.fromNBT(messageTag);
                        messages.add(message);
                    }
                    
                    playerConversations.put(deityId, messages);
                }
                
                manager.conversations.put(playerUuid, playerConversations);
            } catch (Exception e) {
                LOGGER.warn("Failed to load conversation data for player {}: {}", playerUuidStr, e.getMessage());
            }
        }
        
        LOGGER.info("Loaded conversation history for {} players with {} API keys", 
                   manager.conversations.size(), manager.apiKeys.size());
        return manager;
    }
    
    /**
     * Save conversation data to NBT (called by Minecraft's data system)
     */
    @Override
    public CompoundTag save(CompoundTag tag) {
        // Save configuration settings
        CompoundTag configTag = new CompoundTag();
        configTag.putInt("maxMessagesPerDeity", maxMessagesPerDeity);
        tag.put("config", configTag);
        
        // Save API keys
        CompoundTag apiKeysTag = new CompoundTag();
        for (Map.Entry<String, String> entry : apiKeys.entrySet()) {
            apiKeysTag.putString(entry.getKey(), entry.getValue());
        }
        tag.put("apiKeys", apiKeysTag);
        
        // Save conversations
        CompoundTag conversationsTag = new CompoundTag();
        for (Map.Entry<UUID, Map<ResourceLocation, List<ConversationMessage>>> playerEntry : conversations.entrySet()) {
            String playerUuidStr = playerEntry.getKey().toString();
            CompoundTag playerTag = new CompoundTag();
            
            for (Map.Entry<ResourceLocation, List<ConversationMessage>> deityEntry : playerEntry.getValue().entrySet()) {
                String deityIdStr = deityEntry.getKey().toString();
                ListTag messagesTag = new ListTag();
                
                for (ConversationMessage message : deityEntry.getValue()) {
                    messagesTag.add(message.toNBT());
                }
                
                playerTag.put(deityIdStr, messagesTag);
            }
            
            conversationsTag.put(playerUuidStr, playerTag);
        }
        
        tag.put("conversations", conversationsTag);
        return tag;
    }
    
    /**
     * Add a message to conversation history (server-side only)
     * Automatically saves to world data and maintains message limit
     */
    public void addMessage(UUID playerUuid, ResourceLocation deityId, String speaker, String message) {
        conversations.computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>())
                   .computeIfAbsent(deityId, k -> new ArrayList<>())
                   .add(new ConversationMessage(speaker, message, LocalDateTime.now()));
        
        // Maintain configurable message limit
        List<ConversationMessage> messages = conversations.get(playerUuid).get(deityId);
        if (messages.size() > maxMessagesPerDeity) {
            messages.subList(0, messages.size() - maxMessagesPerDeity).clear();
        }
        
        // Mark as dirty to trigger automatic save
        setDirty();
    }
    
    /**
     * Get conversation history for a player and deity (server-side only)
     */
    public List<ConversationMessage> getHistory(UUID playerUuid, ResourceLocation deityId) {
        return conversations.getOrDefault(playerUuid, Collections.emptyMap())
                          .getOrDefault(deityId, Collections.emptyList());
    }
    
    /**
     * Get recent conversation history with line limit (server-side only)
     */
    public List<ConversationMessage> getRecentHistory(UUID playerUuid, ResourceLocation deityId, int maxLines) {
        List<ConversationMessage> fullHistory = getHistory(playerUuid, deityId);
        if (fullHistory.size() <= maxLines) {
            return new ArrayList<>(fullHistory);
        }
        return new ArrayList<>(fullHistory.subList(fullHistory.size() - maxLines, fullHistory.size()));
    }
    
    /**
     * Clear conversation history for a specific deity (admin only)
     */
    public boolean clearHistory(UUID playerUuid, ResourceLocation deityId, ServerPlayer requester) {
        if (!requester.hasPermissions(2)) {
            requester.sendSystemMessage(Component.literal("§cYou need admin permissions to clear conversation history."));
            return false;
        }
        
        Map<ResourceLocation, List<ConversationMessage>> playerConversations = conversations.get(playerUuid);
        if (playerConversations != null) {
            playerConversations.remove(deityId);
            setDirty();
            return true;
        }
        return false;
    }
    
    /**
     * Clear all conversation history for a player (admin only)
     */
    public boolean clearAllHistory(UUID playerUuid, ServerPlayer requester) {
        if (!requester.hasPermissions(2)) {
            requester.sendSystemMessage(Component.literal("§cYou need admin permissions to clear conversation history."));
            return false;
        }
        
        conversations.remove(playerUuid);
        setDirty();
        return true;
    }
    
    // ===== API KEY MANAGEMENT =====
    
    /**
     * Set API key for a provider (server-side only, stored in world data)
     */
    public void setApiKey(String provider, String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            apiKeys.remove(provider);
        } else {
            apiKeys.put(provider, apiKey.trim());
        }
        setDirty();
        LOGGER.info("API key for provider '{}' has been {}", provider, apiKey == null ? "removed" : "updated");
    }
    
    /**
     * Get API key for a provider (server-side only)
     */
    public String getApiKey(String provider) {
        return apiKeys.get(provider);
    }
    
    /**
     * Check if API key exists for provider
     */
    public boolean hasApiKey(String provider) {
        return apiKeys.containsKey(provider) && !apiKeys.get(provider).trim().isEmpty();
    }
    
    // ===== CONFIGURATION MANAGEMENT WITH HYBRID PRIORITY =====
    
    /**
     * Get effective cooldown value (supports JSON preset + server override)
     * Priority: Server Override > JSON Value > System Default
     */
    public int getEffectiveCooldown(ResourceLocation deityId, String prayerType, PrayerAIConfig jsonConfig) {
        // Check for server override first
        Map<String, Object> serverConfig = deityAIConfig.get(deityId);
        if (serverConfig != null && serverConfig.containsKey(prayerType + "Cooldown")) {
            return (Integer) serverConfig.get(prayerType + "Cooldown");
        }
        
        // Fall back to JSON value (preset)
        if (jsonConfig != null && jsonConfig.cooldownMinutes > 0) {
            return jsonConfig.cooldownMinutes;
        }
        
        // System default
        return 30;
    }
    
    /**
     * Get effective max commands value
     * Priority: Global Server Limit > Deity Server Override > JSON Value > System Default
     */
    public int getEffectiveMaxCommands(ResourceLocation deityId, String prayerType, PrayerAIConfig jsonConfig) {
        // Check for deity-specific server override
        Map<String, Object> serverConfig = deityAIConfig.get(deityId);
        int effectiveMax = 3; // System default
        
        if (serverConfig != null && serverConfig.containsKey("maxCommands")) {
            effectiveMax = (Integer) serverConfig.get("maxCommands");
        } else if (jsonConfig != null && jsonConfig.maxCommands > 0) {
            effectiveMax = jsonConfig.maxCommands; // Use JSON preset
        }
        
        // Apply global limit if set (acts as ceiling)
        Integer globalMax = (Integer) globalAISettings.get("globalMaxCommands");
        if (globalMax != null) {
            effectiveMax = Math.min(effectiveMax, globalMax);
        }
        
        return effectiveMax;
    }
    
    /**
     * Get effective reputation requirement
     * Priority: Server Override > JSON Value > System Default
     */
    public int getEffectiveReputationRequired(ResourceLocation deityId, String prayerType, PrayerAIConfig jsonConfig) {
        // Check for server override first
        Map<String, Object> serverConfig = deityAIConfig.get(deityId);
        if (serverConfig != null && serverConfig.containsKey(prayerType + "RepRequired")) {
            return (Integer) serverConfig.get(prayerType + "RepRequired");
        }
        
        // Fall back to JSON value (preset)
        if (jsonConfig != null) {
            return jsonConfig.reputationRequired;
        }
        
        // System default
        return 0;
    }
    
    /**
     * Get effective auto-judge commands setting
     * Priority: Server Override > JSON Value > System Default
     */
    public boolean getEffectiveAutoJudgeCommands(ResourceLocation deityId, String prayerType, PrayerAIConfig jsonConfig) {
        // Check for server override first
        Map<String, Object> serverConfig = deityAIConfig.get(deityId);
        if (serverConfig != null && serverConfig.containsKey("autoJudgeCommands")) {
            return (Boolean) serverConfig.get("autoJudgeCommands");
        }
        
        // Fall back to JSON value (preset)
        if (jsonConfig != null) {
            return jsonConfig.autoJudgeCommands;
        }
        
        // System default
        return false;
    }
    
    /**
     * Get effective judgment threshold value
     * Priority: Server Override > JSON Value > System Default
     */
    public int getEffectiveJudgmentThreshold(ResourceLocation deityId, String thresholdType, JudgmentConfig jsonJudgment) {
        // Check for server override first
        Map<String, Object> serverConfig = deityAIConfig.get(deityId);
        if (serverConfig != null && serverConfig.containsKey(thresholdType + "Threshold")) {
            return (Integer) serverConfig.get(thresholdType + "Threshold");
        }
        
        // Fall back to JSON value (preset)
        if (jsonJudgment != null) {
            switch (thresholdType) {
                case "blessing": return jsonJudgment.blessingThreshold;
                case "curse": return jsonJudgment.curseThreshold;
            }
        }
        
        // System defaults
        switch (thresholdType) {
            case "blessing": return 10;
            case "curse": return -5;
            default: return 0;
        }
    }
    
    // ===== SERVER CONFIGURATION METHODS =====
    
    /**
     * Set server override for cooldown (overrides JSON preset)
     */
    public void setDeityCooldownOverride(ResourceLocation deityId, String prayerType, int minutes) {
        deityAIConfig.computeIfAbsent(deityId, k -> new HashMap<>())
                    .put(prayerType + "Cooldown", minutes);
        setDirty();
    }
    
    /**
     * Set server override for max commands (overrides JSON preset)
     */
    public void setDeityMaxCommandsOverride(ResourceLocation deityId, int maxCommands) {
        deityAIConfig.computeIfAbsent(deityId, k -> new HashMap<>())
                    .put("maxCommands", maxCommands);
        setDirty();
    }
    
    /**
     * Set server override for reputation requirement (overrides JSON preset)
     */
    public void setDeityRepRequirementOverride(ResourceLocation deityId, String prayerType, int reputation) {
        deityAIConfig.computeIfAbsent(deityId, k -> new HashMap<>())
                    .put(prayerType + "RepRequired", reputation);
        setDirty();
    }
    
    /**
     * Set server override for judgment threshold (overrides JSON preset)
     */
    public void setDeityJudgmentThresholdOverride(ResourceLocation deityId, String thresholdType, int threshold) {
        deityAIConfig.computeIfAbsent(deityId, k -> new HashMap<>())
                    .put(thresholdType + "Threshold", threshold);
        setDirty();
    }
    
    /**
     * Remove server override (reverts to JSON preset)
     */
    public void removeDeityOverride(ResourceLocation deityId, String configKey) {
        Map<String, Object> config = deityAIConfig.get(deityId);
        if (config != null) {
            config.remove(configKey);
            if (config.isEmpty()) {
                deityAIConfig.remove(deityId);
            }
            setDirty();
        }
    }
    
    /**
     * Check if deity has any server-side configuration overrides
     */
    public boolean hasServerOverrides(ResourceLocation deityId) {
        return deityAIConfig.containsKey(deityId) && !deityAIConfig.get(deityId).isEmpty();
    }
    
    // ===== FULL CONVERSATION CONTEXT =====
    
    /**
     * Get full conversation context for AI (entire conversation history)
     */
    public String getFullConversationContext(UUID playerUuid, ResourceLocation deityId) {
        List<ConversationMessage> allMessages = getHistory(playerUuid, deityId);
        if (allMessages.isEmpty()) {
            return "";
        }
        
        StringBuilder context = new StringBuilder();
        context.append("=== COMPLETE CONVERSATION HISTORY ===\n");
        
        for (ConversationMessage message : allMessages) {
            context.append(String.format("[%s] %s: %s\n", 
                message.getTimestamp().format(TIMESTAMP_FORMAT),
                message.getSpeaker(), 
                message.getMessage()));
        }
        
        context.append("=== END CONVERSATION HISTORY ===\n");
        return context.toString();
    }
    
    // ===== STATIC CONVENIENCE METHODS =====
    
    /**
     * Convenience method for getting player conversation history
     */
    public static List<ConversationMessage> getPlayerHistory(ServerPlayer player, ResourceLocation deityId) {
        ConversationHistoryManager manager = get();
        if (manager != null) {
            return manager.getHistory(player.getUUID(), deityId);
        }
        return Collections.emptyList();
    }
    
    /**
     * Convenience method for getting recent player conversation history
     */
    public static List<ConversationMessage> getPlayerRecentHistory(ServerPlayer player, ResourceLocation deityId, int maxLines) {
        ConversationHistoryManager manager = get();
        if (manager != null) {
            return manager.getRecentHistory(player.getUUID(), deityId, maxLines);
        }
        return Collections.emptyList();
    }
    
    /**
     * Get full conversation context (static convenience method)
     */
    public static String getPlayerFullContext(ServerPlayer player, ResourceLocation deityId) {
        ConversationHistoryManager manager = get();
        return manager != null ? manager.getFullConversationContext(player.getUUID(), deityId) : "";
    }
    
    /**
     * Get API key from world data (static convenience method)
     */
    public static String getWorldApiKey(String provider) {
        ConversationHistoryManager manager = get();
        return manager != null ? manager.getApiKey(provider) : null;
    }
    
    /**
     * Set API key in world data (static convenience method - admin only)
     */
    public static boolean setWorldApiKey(String provider, String apiKey, ServerPlayer requester) {
        if (!requester.hasPermissions(2)) {
            requester.sendSystemMessage(Component.literal("§cYou need admin permissions to manage API keys."));
            return false;
        }
        
        ConversationHistoryManager manager = get();
        if (manager != null) {
            manager.setApiKey(provider, apiKey);
            requester.sendSystemMessage(Component.literal("§aAPI key for " + provider + " has been updated."));
            return true;
        }
        return false;
    }
    
    // ===== STATIC METHODS FOR EFFECTIVEAICONFIG =====
    
    /**
     * Static wrapper for getEffectiveCooldown
     */
    public static int getEffectiveCooldownStatic(ResourceLocation deityId, String prayerType, PrayerAIConfig jsonConfig) {
        ConversationHistoryManager manager = get();
        return manager != null ? manager.getEffectiveCooldown(deityId, prayerType, jsonConfig) : 30;
    }
    
    /**
     * Static wrapper for getEffectiveMaxCommands
     */
    public static int getEffectiveMaxCommandsStatic(ResourceLocation deityId, String prayerType, PrayerAIConfig jsonConfig) {
        ConversationHistoryManager manager = get();
        return manager != null ? manager.getEffectiveMaxCommands(deityId, prayerType, jsonConfig) : 3;
    }
    
    /**
     * Static wrapper for getEffectiveReputationRequired
     */
    public static int getEffectiveReputationRequiredStatic(ResourceLocation deityId, String prayerType, PrayerAIConfig jsonConfig) {
        ConversationHistoryManager manager = get();
        return manager != null ? manager.getEffectiveReputationRequired(deityId, prayerType, jsonConfig) : 0;
    }
    
    /**
     * Static wrapper for getEffectiveAutoJudgeCommands
     */
    public static boolean getEffectiveAutoJudgeCommandsStatic(ResourceLocation deityId, String prayerType, PrayerAIConfig jsonConfig) {
        ConversationHistoryManager manager = get();
        return manager != null ? manager.getEffectiveAutoJudgeCommands(deityId, prayerType, jsonConfig) : false;
    }
    
    /**
     * Static wrapper for getEffectiveJudgmentThreshold
     */
    public static int getEffectiveJudgmentThresholdStatic(ResourceLocation deityId, String thresholdType, JudgmentConfig jsonJudgment) {
        ConversationHistoryManager manager = get();
        return manager != null ? manager.getEffectiveJudgmentThreshold(deityId, thresholdType, jsonJudgment) : 0;
    }
    
    /**
     * Static wrapper for hasServerOverrides
     */
    public static boolean hasServerOverridesStatic(ResourceLocation deityId) {
        ConversationHistoryManager manager = get();
        return manager != null ? manager.hasServerOverrides(deityId) : false;
    }
    
    /**
     * Get deity AI provider (for EffectiveAIConfig compatibility)
     */
    public String getDeityAIProvider(ResourceLocation deityId) {
        Map<String, Object> config = deityAIConfig.get(deityId);
        if (config != null && config.containsKey("aiProvider")) {
            return (String) config.get("aiProvider");
        }
        return null;
    }
    
    /**
     * Get deity AI model (for EffectiveAIConfig compatibility)
     */
    public String getDeityAIModel(ResourceLocation deityId) {
        Map<String, Object> config = deityAIConfig.get(deityId);
        if (config != null && config.containsKey("aiModel")) {
            return (String) config.get("aiModel");
        }
        return null;
    }
    
    // ===== COMMAND EXECUTION DEBUGGING =====
    
    /**
     * Log AI command execution with full debugging context
     */
    public void logCommandExecution(UUID playerUuid, ResourceLocation deityId, String command, boolean success, String result) {
        ConversationMessage debugMessage = new ConversationMessage(
            "SYSTEM_DEBUG", 
            String.format("COMMAND_EXECUTION: %s | SUCCESS: %s | RESULT: %s", command, success, result),
            LocalDateTime.now()
        );
        
        // Add to conversation history for debugging
        conversations.computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>())
                   .computeIfAbsent(deityId, k -> new ArrayList<>())
                   .add(debugMessage);
        
        // Also log to server console
        if (success) {
            LOGGER.info("✅ AI Command Success - Player: {}, Deity: {}, Command: '{}', Result: '{}'", 
                playerUuid, deityId, command, result);
        } else {
            LOGGER.warn("❌ AI Command Failed - Player: {}, Deity: {}, Command: '{}', Error: '{}'", 
                playerUuid, deityId, command, result);
        }
        
        setDirty();
    }
    
    /**
     * Log AI decision-making process for commands
     */
    public void logAIDecision(UUID playerUuid, ResourceLocation deityId, String decisionType, String reasoning, List<String> commands) {
        ConversationMessage aiDebugMessage = new ConversationMessage(
            "AI_DECISION", 
            String.format("TYPE: %s | REASONING: %s | COMMANDS: %s", decisionType, reasoning, String.join(", ", commands)),
            LocalDateTime.now()
        );
        
        conversations.computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>())
                   .computeIfAbsent(deityId, k -> new ArrayList<>())
                   .add(aiDebugMessage);
        
        LOGGER.info("🧠 AI Decision - Player: {}, Deity: {}, Type: '{}', Commands: {}, Reasoning: '{}'", 
            playerUuid, deityId, decisionType, commands.size(), reasoning);
        
        setDirty();
    }
    
    /**
     * Get command execution history for debugging
     */
    public List<ConversationMessage> getCommandExecutionHistory(UUID playerUuid, ResourceLocation deityId) {
        List<ConversationMessage> allMessages = getHistory(playerUuid, deityId);
        return allMessages.stream()
                          .filter(msg -> msg.getSpeaker().equals("SYSTEM_DEBUG") || msg.getSpeaker().equals("AI_DECISION"))
                          .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Get debugging statistics for admin overview
     */
    public String generateCommandExecutionReport(UUID playerUuid, ResourceLocation deityId) {
        List<ConversationMessage> debugMessages = getCommandExecutionHistory(playerUuid, deityId);
        
        int totalCommands = 0;
        int successfulCommands = 0;
        int failedCommands = 0;
        int aiDecisions = 0;
        
        for (ConversationMessage msg : debugMessages) {
            if (msg.getSpeaker().equals("SYSTEM_DEBUG")) {
                totalCommands++;
                if (msg.getMessage().contains("SUCCESS: true")) {
                    successfulCommands++;
                } else {
                    failedCommands++;
                }
            } else if (msg.getSpeaker().equals("AI_DECISION")) {
                aiDecisions++;
            }
        }
        
        return String.format(
            "§6=== Command Execution Report ===\n" +
            "§ePlayer: %s\n" +
            "§eDeity: %s\n" +
            "§aTotal Commands: %d\n" +
            "§2Successful: %d\n" +
            "§cFailed: %d\n" +
            "§bAI Decisions: %d\n" +
            "§fSuccess Rate: %.1f%%",
            playerUuid.toString().substring(0, 8),
            deityId.toString(),
            totalCommands,
            successfulCommands,
            failedCommands,
            aiDecisions,
            totalCommands > 0 ? (successfulCommands * 100.0 / totalCommands) : 0.0
        );
    }
    
    /**
     * Static convenience method for logging command execution
     */
    public static void logCommandExecutionStatic(ServerPlayer player, ResourceLocation deityId, String command, boolean success, String result) {
        ConversationHistoryManager manager = get();
        if (manager != null) {
            manager.logCommandExecution(player.getUUID(), deityId, command, success, result);
        }
    }
    
    /**
     * Static convenience method for logging AI decisions
     */
    public static void logAIDecisionStatic(ServerPlayer player, ResourceLocation deityId, String decisionType, String reasoning, List<String> commands) {
        ConversationHistoryManager manager = get();
        if (manager != null) {
            manager.logAIDecision(player.getUUID(), deityId, decisionType, reasoning, commands);
        }
    }
    
    /**
     * Static convenience method for getting command execution report
     */
    public static String getCommandExecutionReportStatic(ServerPlayer player, ResourceLocation deityId) {
        ConversationHistoryManager manager = get();
        return manager != null ? manager.generateCommandExecutionReport(player.getUUID(), deityId) : "§cNo debugging data available";
    }
}