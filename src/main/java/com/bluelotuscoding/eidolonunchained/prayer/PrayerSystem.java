package com.bluelotuscoding.eidolonunchained.prayer;

import com.bluelotuscoding.eidolonunchained.deity.DatapackDeity;
import com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager;
import com.bluelotuscoding.eidolonunchained.ai.AIDeityManager;
import com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig;
import com.bluelotuscoding.eidolonunchained.ai.PrayerAIConfig;
import com.bluelotuscoding.eidolonunchained.ai.PlayerContext;
import com.bluelotuscoding.eidolonunchained.integration.gemini.GeminiAPIClient;
import com.bluelotuscoding.eidolonunchained.config.APIKeyManager;
import com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles prayer interactions with AI-enabled deities
 */
@Mod.EventBusSubscriber
public class PrayerSystem {
    private static final Logger LOGGER = LogManager.getLogger();
    
    // Cooldown tracking: playerId -> (deityId + prayerType) -> lastUsedTime
    private static final Map<UUID, Map<String, Long>> playerCooldowns = new ConcurrentHashMap<>();
    
    /**
     * Handle effigy interactions - now uses the unified prayer system
     */
    public static boolean handleEffigyInteraction(ServerPlayer player, ResourceLocation deityId) {
        // Get the deity
        DatapackDeity deity = DatapackDeityManager.getDeity(deityId);
        if (deity == null) {
            return false; // Let Eidolon handle unknown deities
        }

        // Check if AI is enabled for this deity
        AIDeityConfig aiConfig = AIDeityManager.getInstance().getAIConfig(deityId);
        if (aiConfig == null) {
            return false; // Let Eidolon handle non-AI deities
        }

        // Use the unified prayer system with "conversation" type
        handlePrayer(player, deityId, "conversation");
        
        return true; // We handled the interaction
    }    /**
     * Generate a contextual prayer based on player's current situation
     */
    private static String generateContextualPrayer(ServerPlayer player) {
        StringBuilder prayer = new StringBuilder();
        
        // Check player's health
        if (player.getHealth() < player.getMaxHealth() * 0.3f) {
            prayer.append("I am wounded and seek healing. ");
        }
        
        // Check time of day
        if (player.level().isNight()) {
            prayer.append("In this dark hour, I seek your guidance. ");
        } else {
            prayer.append("I come before you in the light of day. ");
        }
        
        // Check player's hunger
        if (player.getFoodData().getFoodLevel() < 6) {
            prayer.append("I am hungry and in need of sustenance. ");
        }
        
        // Check if player is in danger (nearby hostile mobs)
        boolean inDanger = !player.level().getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class, 
            player.getBoundingBox().inflate(16.0D)).isEmpty();
        
        if (inDanger) {
            prayer.append("Danger surrounds me and I fear for my safety. ");
        }
        
        // Default prayer if no specific conditions
        if (prayer.length() == 0) {
            prayer.append("I humbly seek your wisdom and blessing. ");
        }
        
        prayer.append("What would you have me do?");
        
        return prayer.toString();
    }
    
    /**
     * Send patron-specific prayer rejection message
     */
    private static void sendPatronPrayerRejection(ServerPlayer player, DatapackDeity deity, AIDeityConfig aiConfig, String prayerType) {
        try {
            player.level().getCapability(com.bluelotuscoding.eidolonunchained.capability.CapabilityHandler.PATRON_DATA_CAPABILITY)
                .ifPresent(patronData -> {
                    ResourceLocation playerPatron = patronData.getPatron(player);
                    AIDeityConfig.PatronRelationship relationship = aiConfig.determinePatronRelationship(playerPatron);
                    
                    switch (relationship) {
                        case NO_PATRON:
                            player.sendSystemMessage(Component.literal("§c" + deity.getDisplayName() + " §7ignores your godless prayers."));
                            player.sendSystemMessage(Component.literal("§7Dedicate yourself to a patron to earn divine favor."));
                            break;
                        case ENEMY:
                            player.sendSystemMessage(Component.literal("§4" + deity.getDisplayName() + " §crejects your corrupt prayers with disgust!"));
                            
                            // Check for auto-punish prayer attempts
                            if (aiConfig.patronConfig.conversationRules.containsKey("enemy_restrictions")) {
                                Map<String, Object> rules = (Map<String, Object>) aiConfig.patronConfig.conversationRules.get("enemy_restrictions");
                                if (rules.containsKey("reputation_penalty_on_contact")) {
                                    int penalty = (Integer) rules.get("reputation_penalty_on_contact");
                                    player.level().getCapability(elucent.eidolon.capability.IReputation.INSTANCE)
                                        .ifPresent(reputation -> {
                                            double currentRep = reputation.getReputation(player, aiConfig.deityId);
                                            reputation.setReputation(player, aiConfig.deityId, currentRep + penalty);
                                            player.sendSystemMessage(Component.literal("§4Your boldness angers " + deity.getDisplayName() + "! (§c" + penalty + " reputation§4)"));
                                        });
                                }
                            }
                            break;
                        case NEUTRAL:
                            if (aiConfig.patronConfig.requiresPatronStatus.equals("follower_only")) {
                                player.sendSystemMessage(Component.literal("§e" + deity.getDisplayName() + " §7only hears the prayers of their faithful servants."));
                                player.sendSystemMessage(Component.literal("§7Become their devoted follower to gain their ear."));
                            }
                            break;
                    }
                });
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("§c" + deity.getDisplayName() + " §7does not hear your prayer."));
        }
    }

    /**
     * Handle prayer requests from players
     */
    public static void handlePrayer(ServerPlayer player, ResourceLocation deityId, String prayerType, String... args) {
        LOGGER.info("Prayer request received: player={}, deity={}, type={}", 
            player.getName().getString(), deityId, prayerType);
        
        // Get the deity
        DatapackDeity deity = DatapackDeityManager.getDeity(deityId);
        if (deity == null) {
            player.sendSystemMessage(Component.literal("§cUnknown deity: " + deityId));
            return;
        }
        
        // Get AI configuration
        AIDeityConfig aiConfig = AIDeityManager.getInstance().getAIConfig(deityId);
        if (aiConfig == null) {
            player.sendSystemMessage(Component.literal("§e" + deity.getDisplayName() + " §cdoes not respond to prayers at this time."));
            return;
        }
        
        // PATRON ALLEGIANCE CHECK - Core new functionality
        if (!aiConfig.canRespondToPlayer(player)) {
            sendPatronPrayerRejection(player, deity, aiConfig, prayerType);
            return;
        }
        
        // Get prayer configuration
        PrayerAIConfig prayerConfig = aiConfig.prayerConfigs.get(prayerType);
        if (prayerConfig == null) {
            player.sendSystemMessage(Component.literal("§e" + deity.getDisplayName() + " §cdoes not understand that type of prayer."));
            return;
        }
        
        // Check reputation requirement
        int playerReputation = (int) Math.round(deity.getPlayerReputation(player));
        if (playerReputation < prayerConfig.reputationRequired) {
            player.sendSystemMessage(Component.literal("§eYou need at least §6" + prayerConfig.reputationRequired + " reputation §ewith §6" + deity.getDisplayName() + "§e."));
            return;
        }
        
        // Check cooldown
        String cooldownKey = deityId.toString() + ":" + prayerType;
        if (isOnCooldown(player.getUUID(), cooldownKey, prayerConfig.cooldownMinutes)) {
            long remainingMinutes = getCooldownRemaining(player.getUUID(), cooldownKey, prayerConfig.cooldownMinutes);
            player.sendSystemMessage(Component.literal("§eYou must wait §6" + remainingMinutes + " more minutes §ebefore praying to §6" + deity.getDisplayName() + " §eagain."));
            return;
        }
        
        // Build context and prompt
        String prompt = buildPrompt(player, deity, prayerConfig, args);
        
        // Create player context for dynamic personality
        PlayerContext playerContext = new PlayerContext(player, deity);
        playerContext.researchCount = 0; // TODO: Get from research system if available
        
        // 🎯 USE DYNAMIC PROGRESSION INSTEAD OF HARDCODED LOGIC
        // This fixes the AI treating 50 reputation players as "new members"
        playerContext.progressionLevel = getProgressionLevel(deity, player);
        
        playerContext.biome = player.level().getBiome(player.blockPosition()).unwrapKey()
            .map(resourceKey -> resourceKey.location().toString())
            .orElse("minecraft:plains");
        playerContext.timeOfDay = player.level().isNight() ? "night" : "day";
        
        String personality = aiConfig.buildDynamicPersonality(playerContext);
        
        // Get API key from APIKeyManager instead of environment variable
        String apiKey = APIKeyManager.getAPIKey("gemini");
        LOGGER.info("API Key check - hasKey: {}, keyLength: {}", 
            (apiKey != null && !apiKey.isEmpty()), 
            (apiKey != null ? apiKey.length() : 0));
        
        if (apiKey == null || apiKey.isEmpty()) {
            sendDeityMessage(player, deity.getDisplayName(), "The deity remains silent... (API key not configured)", false);
            LOGGER.error("No Gemini API key configured for deity interaction. Expected format: gemini.api_key=YOUR_KEY in config/eidolonunchained/server-api-keys.properties");
            return;
        }
        
        // Get API client
        GeminiAPIClient client = new GeminiAPIClient(
            apiKey,
            aiConfig.apiSettings.getModel(),
            aiConfig.apiSettings.timeoutSeconds
        );
        
        // Generate response asynchronously
        sendDeityMessage(player, "Communing with " + deity.getDisplayName() + "...", null, true);
        
        client.generateResponse(prompt, personality, aiConfig.apiSettings.generationConfig, aiConfig.apiSettings.safetySettings)
            .thenAccept(response -> {
                if (response.success) {
                    // Send deity response to player with prominent display
                    sendDeityMessage(player, deity.getDisplayName(), response.dialogue, false);
                    
                    // Execute commands
                    executeCommands(player, response.commands, prayerConfig);
                    
                    // Set cooldown
                    setCooldown(player.getUUID(), cooldownKey);
                    
                } else {
                    sendDeityMessage(player, deity.getDisplayName(), "does not respond to your prayer.", true);
                }
            })
            .exceptionally(throwable -> {
                LOGGER.error("Prayer failed for player " + player.getName().getString(), throwable);
                sendDeityMessage(player, "Divine Connection", "was interrupted.", true);
                return null;
            });
    }
    
    private static String buildPrompt(ServerPlayer player, DatapackDeity deity, PrayerAIConfig prayerConfig, String... args) {
        String basePrompt = prayerConfig.base_prompt;
        
        // Get enhanced context
        String enhancedContext = com.bluelotuscoding.eidolonunchained.ai.PlayerContextTracker.getContextSummary(player);
        String ritualHistory = com.bluelotuscoding.eidolonunchained.ai.PlayerContextTracker.getRitualHistorySummary(player);
        
        // Replace placeholders
        basePrompt = basePrompt.replace("{player}", player.getName().getString());
        basePrompt = basePrompt.replace("{reputation}", String.valueOf(deity.getPlayerReputation(player)));
        basePrompt = basePrompt.replace("{research_count}", "0"); // TODO: Integrate with research system
        basePrompt = basePrompt.replace("{progression_level}", getProgressionLevel(deity, player));
        
        // Add context
        String context = GeminiAPIClient.buildPlayerContext(player);
        basePrompt = basePrompt.replace("{location}", extractFromContext(context, "Location"));
        basePrompt = basePrompt.replace("{biome}", extractFromContext(context, "Biome"));
        basePrompt = basePrompt.replace("{time}", extractFromContext(context, "Time"));
        basePrompt = basePrompt.replace("{weather}", extractFromContext(context, "Weather"));
        
        // Add any additional arguments
        if (args.length > 0) {
            basePrompt += "\nAdditional context: " + String.join(" ", args);
        }
        
        // Add enhanced context
        if (!enhancedContext.isEmpty()) {
            basePrompt += "\nRecent Player Activity: " + enhancedContext;
        }
        
        if (!ritualHistory.isEmpty()) {
            basePrompt += "\nRitual History: " + ritualHistory;
        }
        
        // Add task system guidance if enabled
        com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig config = 
            com.bluelotuscoding.eidolonunchained.ai.AIDeityManager.getInstance().getAIConfig(deity.getId());
        if (config != null && config.taskConfig.enabled) {
            basePrompt += "\nTask Assignment: You can assign tasks to worthy players using: Command: /eidolon:assign_task " + player.getName().getString() + " <task_id>";
        }
        
        // Add command constraints
        basePrompt += "\n\nIMPORTANT: You may use up to " + prayerConfig.maxCommands + " commands. ";
        basePrompt += "Allowed commands: " + String.join(", ", prayerConfig.allowedCommands) + ". ";
        basePrompt += "Format commands as: Command: /command arguments";
        
        return basePrompt;
    }
    
    /**
     * 🎯 DYNAMIC PROGRESSION LEVEL DETECTION
     * 
     * Gets the player's current progression level based on JSON-defined stages
     * instead of hardcoded reputation thresholds. This fixes the AI treating
     * 50 reputation players as "new members" when they should be "priests".
     */
    private static String getProgressionLevel(DatapackDeity deity, ServerPlayer player) {
        double reputation = deity.getPlayerReputation(player);
        
        try {
            // Get deity's progression stages from JSON
            Map<String, Object> stagesMap = deity.getProgressionStages();
            
            if (stagesMap.isEmpty()) {
                LOGGER.debug("🔍 No progression stages defined for deity {}, using fallback", deity.getId());
                return getFallbackProgressionLevel(reputation);
            }
            
            // Find the highest stage the player qualifies for
            String bestStage = "initiate"; // Default lowest stage
            double highestQualifyingReputation = -1;
            
            for (Map.Entry<String, Object> stageEntry : stagesMap.entrySet()) {
                String stageName = stageEntry.getKey();
                Object stageData = stageEntry.getValue();
                
                // Handle the case where stage data is a Map
                if (!(stageData instanceof Map)) continue;
                @SuppressWarnings("unchecked")
                Map<String, Object> stageDataMap = (Map<String, Object>) stageData;
                
                Object repReqObj = stageDataMap.get("reputationRequired");
                if (!(repReqObj instanceof Number)) continue;
                
                double requiredReputation = ((Number) repReqObj).doubleValue();
                
                // Check if player qualifies for this stage and it's higher than current best
                if (reputation >= requiredReputation && requiredReputation > highestQualifyingReputation) {
                    bestStage = stageName;
                    highestQualifyingReputation = requiredReputation;
                }
            }
            
            LOGGER.debug("🎭 Player {} progression with {}: {} ({}rep vs {}req)", 
                player.getName().getString(), deity.getName(), bestStage, (int)reputation, (int)highestQualifyingReputation);
            
            return bestStage;
            
        } catch (Exception e) {
            LOGGER.error("🚨 Error determining progression level for {}/{}, using fallback", 
                player.getName().getString(), deity.getId(), e);
            return getFallbackProgressionLevel(reputation);
        }
    }
    
    /**
     * 🔄 FALLBACK PROGRESSION LEVELS
     * 
     * Used when JSON progression stages are not available.
     * These are the old hardcoded levels for compatibility.
     */
    private static String getFallbackProgressionLevel(double reputation) {
        if (reputation >= 75) return "master";
        if (reputation >= 50) return "advanced";
        if (reputation >= 25) return "intermediate";
        if (reputation >= 10) return "novice";
        return "beginner";
    }
    
    private static String extractFromContext(String context, String key) {
        String[] lines = context.split("\n");
        for (String line : lines) {
            if (line.startsWith(key + ": ")) {
                return line.substring(key.length() + 2);
            }
        }
        return "unknown";
    }
    
    /**
     * 🔧 ENHANCED COMMAND EXECUTION WITH TRACKING
     * 
     * Executes deity commands with comprehensive logging and debug output.
     * This addresses the issue of "no command triggers in chat logs".
     */
    private static void executeCommands(ServerPlayer player, java.util.List<String> commands, PrayerAIConfig prayerConfig) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            LOGGER.error("🚨 Cannot execute commands: server is null for player {}", player.getName().getString());
            return;
        }
        
        Commands commandManager = server.getCommands();
        int commandsExecuted = 0;
        int commandsSkipped = 0;
        
        LOGGER.info("🎯 DEITY COMMAND EXECUTION: Starting {} commands for player {}", 
            commands.size(), player.getName().getString());
        
        for (String command : commands) {
            if (commandsExecuted >= prayerConfig.max_commands) {
                LOGGER.warn("🚫 Command limit reached ({}/{}), skipping remaining commands", 
                    commandsExecuted, prayerConfig.max_commands);
                break;
            }
            
            // Validate command is allowed
            if (!isCommandAllowed(command, prayerConfig.allowed_commands)) {
                LOGGER.warn("🚫 BLOCKED COMMAND: Deity attempted disallowed command: {}", command);
                commandsSkipped++;
                continue;
            }
            
            try {
                // Create command source as the player with proper permissions
                CommandSourceStack source = player.createCommandSourceStack()
                    .withPermission(2) // Op level 2 for deity commands
                    .withSuppressedOutput();
                
                // Clean command format
                String cleanCommand = command.startsWith("/") ? command.substring(1) : command;
                
                LOGGER.info("🔮 EXECUTING DEITY COMMAND: '{}' for player {}", cleanCommand, player.getName().getString());
                
                // Execute command and capture result
                int result = commandManager.performPrefixedCommand(source, cleanCommand);
                commandsExecuted++;
                
                if (result > 0) {
                    LOGGER.info("✅ COMMAND SUCCESS: '{}' executed successfully (result: {})", cleanCommand, result);
                    
                    // Send feedback to player
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§6✦ Divine intervention: §7" + cleanCommand));
                } else {
                    LOGGER.warn("⚠️ COMMAND WARNING: '{}' executed but returned {}", cleanCommand, result);
                }
                
            } catch (Exception e) {
                LOGGER.error("🚨 COMMAND FAILED: '{}' execution error: {}", command, e.getMessage());
                commandsSkipped++;
                
                // Send error feedback to player
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§c✖ Divine power faltered..."));
            }
        }
        
        // Summary logging
        LOGGER.info("📊 COMMAND EXECUTION SUMMARY: {}/{} executed, {} skipped for player {}", 
            commandsExecuted, commands.size(), commandsSkipped, player.getName().getString());
        
        if (commandsExecuted > 0) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§6Divine blessings granted: " + commandsExecuted + " intervention" + 
                (commandsExecuted == 1 ? "" : "s")));
        }
    }
    
    private static boolean isCommandAllowed(String command, java.util.List<String> allowedCommands) {
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        
        String commandName = command.split(" ")[0];
        return allowedCommands.contains(commandName);
    }
    
    private static boolean isOnCooldown(UUID playerId, String cooldownKey, int cooldownMinutes) {
        Map<String, Long> playerCds = playerCooldowns.get(playerId);
        if (playerCds == null) return false;
        
        Long lastUsed = playerCds.get(cooldownKey);
        if (lastUsed == null) return false;
        
        long currentTime = System.currentTimeMillis();
        long cooldownMs = cooldownMinutes * 60L * 1000L;
        
        return (currentTime - lastUsed) < cooldownMs;
    }
    
    private static long getCooldownRemaining(UUID playerId, String cooldownKey, int cooldownMinutes) {
        Map<String, Long> playerCds = playerCooldowns.get(playerId);
        if (playerCds == null) return 0;
        
        Long lastUsed = playerCds.get(cooldownKey);
        if (lastUsed == null) return 0;
        
        long currentTime = System.currentTimeMillis();
        long cooldownMs = cooldownMinutes * 60L * 1000L;
        long remaining = cooldownMs - (currentTime - lastUsed);
        
        return Math.max(0, remaining / (60L * 1000L)); // Convert to minutes
    }
    
    private static void setCooldown(UUID playerId, String cooldownKey) {
        playerCooldowns.computeIfAbsent(playerId, k -> new HashMap<>())
            .put(cooldownKey, System.currentTimeMillis());
    }
    
    /**
     * Clean up old cooldown entries
     */
    public static void cleanupCooldowns() {
        long currentTime = System.currentTimeMillis();
        long maxAge = 24L * 60L * 60L * 1000L; // 24 hours
        
        playerCooldowns.entrySet().removeIf(entry -> {
            entry.getValue().entrySet().removeIf(cooldownEntry -> 
                (currentTime - cooldownEntry.getValue()) > maxAge);
            return entry.getValue().isEmpty();
        });
    }
    
    /**
     * Send deity message with configurable display mode using improved system
     */
    private static void sendDeityMessage(ServerPlayer player, String deityName, String message, boolean isError) {
        // Handle temporary/status messages (like "Communing...")
        if (message == null || deityName.contains("Communing") || deityName.contains("Divine Connection")) {
            Component actionBarMessage = Component.literal("§e⟨ " + deityName + " ⟩");
            player.sendSystemMessage(actionBarMessage, true); // true = action bar
            return;
        }
        
        // Use the same improved display system as DeityChat
        com.bluelotuscoding.eidolonunchained.chat.DeityChat.sendDeityResponsePublic(
            player, deityName, message, isError);
    }
}
