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
     * Handle effigy right-click interactions for AI deities - Start chat conversation
     * @param player The player interacting with the effigy
     * @param deityId The deity ID associated with the effigy
     * @return true if AI handled the interaction, false to let Eidolon handle it
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
        
        // Start conversation with the deity through the chat system
        com.bluelotuscoding.eidolonunchained.chat.DeityChat.startConversation(player, deityId);
        
        return true; // We handled the interaction
    }
    
    /**
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
     * Handle prayer requests from players
     */
    public static void handlePrayer(ServerPlayer player, ResourceLocation deityId, String prayerType, String... args) {
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
        playerContext.progressionLevel = (playerContext.reputation > 50) ? "advanced" : 
                                        (playerContext.reputation > 25) ? "intermediate" : 
                                        (playerContext.reputation > 0) ? "novice" : "beginner";
        playerContext.biome = player.level().getBiome(player.blockPosition()).unwrapKey()
            .map(resourceKey -> resourceKey.location().toString())
            .orElse("minecraft:plains");
        playerContext.timeOfDay = player.level().isNight() ? "night" : "day";
        
        String personality = aiConfig.buildDynamicPersonality(playerContext);
        
        // Get API key from APIKeyManager instead of environment variable
        String apiKey = APIKeyManager.getAPIKey("gemini");
        if (apiKey == null || apiKey.isEmpty()) {
            sendDeityMessage(player, deity.getDisplayName(), "The deity remains silent... (API key not configured)", false);
            LOGGER.warn("No Gemini API key configured for deity interaction");
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
    
    private static String getProgressionLevel(DatapackDeity deity, ServerPlayer player) {
        int reputation = (int) Math.round(deity.getPlayerReputation(player));
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
    
    private static void executeCommands(ServerPlayer player, java.util.List<String> commands, PrayerAIConfig prayerConfig) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        
        Commands commandManager = server.getCommands();
        int commandsExecuted = 0;
        
        for (String command : commands) {
            if (commandsExecuted >= prayerConfig.max_commands) {
                break;
            }
            
            // Validate command is allowed
            if (!isCommandAllowed(command, prayerConfig.allowed_commands)) {
                LOGGER.warn("Deity attempted to use disallowed command: " + command);
                continue;
            }
            
            try {
                // Create command source as the player
                CommandSourceStack source = player.createCommandSourceStack()
                    .withPermission(2) // Op level 2 for deity commands
                    .withSuppressedOutput();
                
                // Execute command
                commandManager.performPrefixedCommand(source, command.startsWith("/") ? command.substring(1) : command);
                commandsExecuted++;
                
                LOGGER.debug("Executed deity command for {}: {}", player.getName().getString(), command);
                
            } catch (Exception e) {
                LOGGER.error("Failed to execute deity command: " + command, e);
            }
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
     * Send deity message with configurable display mode
     */
    private static void sendDeityMessage(ServerPlayer player, String deityName, String message, boolean isError) {
        // Check configuration for display mode
        boolean useProminentDisplay = EidolonUnchainedConfig.COMMON.useProminentDisplay.get();
        boolean useChatForLong = EidolonUnchainedConfig.COMMON.useChatForLongMessages.get();
        int maxSubtitleLength = EidolonUnchainedConfig.COMMON.maxSubtitleLength.get();
        
        // If prominent display is disabled, just use chat
        if (!useProminentDisplay) {
            if (message != null) {
                String chatColor = isError ? "§c" : "§6";
                player.sendSystemMessage(Component.literal(chatColor + "[" + deityName + "] " + message));
            }
            return;
        }
        
        // For long messages, optionally fall back to chat
        if (message != null && message.length() > maxSubtitleLength && useChatForLong) {
            String chatColor = isError ? "§c" : "§6";
            player.sendSystemMessage(Component.literal(chatColor + "[" + deityName + "] " + message));
            return;
        }
        
        // Use prominent title/subtitle display
        Component titleComponent;
        Component subtitleComponent;
        
        if (message == null) {
            // Single line message (for communing status)
            titleComponent = Component.literal(isError ? "§c" + deityName : "§6" + deityName);
            subtitleComponent = Component.empty();
        } else {
            // Two line message (deity name + response)
            titleComponent = Component.literal(isError ? "§c" + deityName : "§6" + deityName);
            
            // Split long messages for better display
            if (message.length() > maxSubtitleLength) {
                String[] words = message.split(" ");
                StringBuilder line1 = new StringBuilder();
                StringBuilder line2 = new StringBuilder();
                boolean firstLine = true;
                
                for (String word : words) {
                    if (firstLine && (line1.length() + word.length() + 1) <= maxSubtitleLength) {
                        if (line1.length() > 0) line1.append(" ");
                        line1.append(word);
                    } else {
                        firstLine = false;
                        if (line2.length() > 0) line2.append(" ");
                        line2.append(word);
                    }
                }
                
                // Use action bar for longer messages
                Component actionBarMessage = Component.literal("§r" + line1.toString());
                if (line2.length() > 0) {
                    actionBarMessage = Component.literal("§r" + line1.toString() + " " + line2.toString());
                }
                player.sendSystemMessage(actionBarMessage, true); // true = action bar
                
                // Still show deity name as title
                subtitleComponent = Component.literal("§7speaks to you");
            } else {
                subtitleComponent = Component.literal("§r" + message);
            }
        }
        
        // Get configurable timing values
        int fadeInTicks = EidolonUnchainedConfig.COMMON.fadeInTicks.get();
        int displayTicks = EidolonUnchainedConfig.COMMON.displayDurationTicks.get();
        int fadeOutTicks = EidolonUnchainedConfig.COMMON.fadeOutTicks.get();
        
        // Set title animation timing (fade in, stay, fade out) in ticks
        ClientboundSetTitlesAnimationPacket animationPacket = new ClientboundSetTitlesAnimationPacket(
            fadeInTicks,   // configurable fade in
            displayTicks,  // configurable display duration
            fadeOutTicks   // configurable fade out
        );
        
        // Send packets to display the title/subtitle
        player.connection.send(animationPacket);
        player.connection.send(new ClientboundSetTitleTextPacket(titleComponent));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitleComponent));
    }
}
