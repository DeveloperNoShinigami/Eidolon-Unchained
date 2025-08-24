package com.bluelotuscoding.eidolonunchained.chat;

import com.bluelotuscoding.eidolonunchained.deity.DatapackDeity;
import com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager;
import com.bluelotuscoding.eidolonunchained.ai.AIDeityManager;
import com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig;
import com.bluelotuscoding.eidolonunchained.integration.gemini.GeminiAPIClient;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * Handles chat-based conversations with AI-enabled deities
 */
@Mod.EventBusSubscriber
public class DeityChat {
    private static final Logger LOGGER = LogManager.getLogger();
    
    // Track active conversations: playerId -> deityId
    private static final Map<UUID, ResourceLocation> activeConversations = new ConcurrentHashMap<>();
    
    // Track pending commands: playerId -> List of suggested commands
    private static final Map<UUID, List<String>> pendingCommands = new ConcurrentHashMap<>();
    
    // Cooldown tracking: playerId -> (deityId) -> lastUsedTime
    private static final Map<UUID, Map<String, Long>> playerCooldowns = new ConcurrentHashMap<>();
    
    /**
     * Start a conversation with a deity through effigy interaction
     */
    public static void startConversation(ServerPlayer player, ResourceLocation deityId) {
        // Get the deity
        DatapackDeity deity = DatapackDeityManager.getDeity(deityId);
        if (deity == null) {
            player.sendSystemMessage(Component.literal("§cUnknown deity: " + deityId));
            return;
        }
        
        // Get AI configuration
        AIDeityConfig aiConfig = AIDeityManager.getAIConfig(deityId);
        if (aiConfig == null) {
            player.sendSystemMessage(Component.literal("§c" + deity.getDisplayName() + " is not available for conversation."));
            return;
        }
        
        // Check reputation requirements for "conversation" prayer type
        AIDeityConfig.PrayerAIConfig conversationConfig = aiConfig.getPrayerConfig("conversation");
        if (conversationConfig == null) {
            conversationConfig = aiConfig.getPrayerConfig("general");
        }
        
        if (conversationConfig != null) {
            int playerReputation = deity.getPlayerReputation(player);
            if (playerReputation < conversationConfig.reputationRequired) {
                player.sendSystemMessage(Component.literal("§c" + deity.getDisplayName() + " requires " + 
                    conversationConfig.reputationRequired + " reputation to converse with you. You have " + playerReputation + "."));
                return;
            }
            
            // Check cooldown
            String cooldownKey = deityId.toString();
            if (isOnCooldown(player.getUUID(), cooldownKey, conversationConfig.cooldownMinutes)) {
                long remaining = getCooldownRemaining(player.getUUID(), cooldownKey, conversationConfig.cooldownMinutes);
                player.sendSystemMessage(Component.literal("§c" + deity.getDisplayName() + " cannot be reached for " + remaining + " more minutes."));
                return;
            }
        }
        
        // Start the conversation
        activeConversations.put(player.getUUID(), deityId);
        
        // Send conversation start message
        player.sendSystemMessage(Component.literal("§6✦ You reach out to " + deity.getDisplayName() + " ✦"));
        player.sendSystemMessage(Component.literal("§7Type your message in chat to speak with the deity."));
        player.sendSystemMessage(Component.literal("§7Type 'goodbye' or 'end' to end the conversation."));
        
        // Send initial greeting from deity
        sendDeityGreeting(player, deity, aiConfig);
    }
    
    /**
     * Handle chat messages when player is in conversation with a deity
     */
    @SubscribeEvent
    public static void onPlayerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        UUID playerId = player.getUUID();
        
        // Check if player is in an active conversation
        ResourceLocation deityId = activeConversations.get(playerId);
        if (deityId == null) {
            return; // Not in conversation, let chat proceed normally
        }
        
        // Cancel the normal chat event
        event.setCanceled(true);
        
        String message = event.getMessage().getString();
        
        // Check for conversation end commands
        if (message.toLowerCase().matches(".*(goodbye|end|farewell|stop).*")) {
            endConversation(player, deityId);
            return;
        }
        
        // Check for command acceptance
        if (message.toLowerCase().startsWith("yes") || message.toLowerCase().startsWith("accept")) {
            executeAccountedCommands(player);
            return;
        }
        
        if (message.toLowerCase().startsWith("no") || message.toLowerCase().startsWith("decline")) {
            declineCommands(player);
            return;
        }
        
        // Process the message with the deity
        processDeityConversation(player, deityId, message);
    }
    
    private static void sendDeityGreeting(ServerPlayer player, DatapackDeity deity, AIDeityConfig aiConfig) {
        AIDeityConfig.PrayerAIConfig conversationConfig = aiConfig.getPrayerConfig("conversation");
        if (conversationConfig == null) {
            conversationConfig = aiConfig.getPrayerConfig("general");
        }
        
        if (conversationConfig == null) {
            player.sendSystemMessage(Component.literal("§6[" + deity.getDisplayName() + "] Greetings, mortal."));
            return;
        }
        
        // Build greeting prompt
        String greetingPrompt = "Player " + player.getName().getString() + " has approached your sacred altar and wishes to speak with you. " +
            "Greet them appropriately for their reputation level (" + deity.getPlayerReputation(player) + "). " +
            "Keep your greeting brief and in character. Do not offer commands yet.";
        
        // Add personality context
        String personality = aiConfig.buildDynamicPersonality(player, deity);
        
        // Get API client
        GeminiAPIClient client = new GeminiAPIClient(
            System.getenv(aiConfig.api_settings.api_key_env),
            aiConfig.api_settings.model,
            aiConfig.api_settings.timeout_seconds
        );
        
        // Generate greeting asynchronously
        client.generateResponse(greetingPrompt, personality, aiConfig.api_settings.generation_config, aiConfig.api_settings.safety_settings)
            .thenAccept(response -> {
                if (response.success && !response.dialogue.isEmpty()) {
                    sendDeityMessage(player, deity.getDisplayName(), response.dialogue);
                } else {
                    sendDeityMessage(player, deity.getDisplayName(), "Greetings, mortal. You may speak.");
                }
            })
            .exceptionally(throwable -> {
                LOGGER.error("Failed to generate deity greeting", throwable);
                sendDeityMessage(player, deity.getDisplayName(), "Greetings, mortal. You may speak.");
                return null;
            });
    }
    
    private static void processDeityConversation(ServerPlayer player, ResourceLocation deityId, String message) {
        DatapackDeity deity = DatapackDeityManager.getDeity(deityId);
        AIDeityConfig aiConfig = AIDeityManager.getAIConfig(deityId);
        
        if (deity == null || aiConfig == null) {
            endConversation(player, deityId);
            return;
        }
        
        // Display player's message in chat
        player.sendSystemMessage(Component.literal("§7You: §f" + message));
        
        // Get conversation configuration
        AIDeityConfig.PrayerAIConfig conversationConfig = aiConfig.getPrayerConfig("conversation");
        if (conversationConfig == null) {
            conversationConfig = aiConfig.getPrayerConfig("general");
        }
        
        // Build conversation prompt
        String prompt = buildConversationPrompt(player, deity, aiConfig, conversationConfig, message);
        String personality = aiConfig.buildDynamicPersonality(player, deity);
        
        // Get API client
        GeminiAPIClient client = new GeminiAPIClient(
            System.getenv(aiConfig.api_settings.api_key_env),
            aiConfig.api_settings.model,
            aiConfig.api_settings.timeout_seconds
        );
        
        // Show "deity is thinking" message
        player.sendSystemMessage(Component.literal("§7" + deity.getDisplayName() + " considers your words..."));
        
        // Generate response asynchronously
        client.generateResponse(prompt, personality, aiConfig.api_settings.generation_config, aiConfig.api_settings.safety_settings)
            .thenAccept(response -> {
                if (response.success) {
                    // Send deity response
                    sendDeityMessage(player, deity.getDisplayName(), response.dialogue);
                    
                    // Handle commands based on configuration
                    if (conversationConfig.autoJudgeCommands) {
                        // Automatically execute commands based on deity's judgment
                        List<String> judgedCommands = getJudgedCommands(player, deity, conversationConfig);
                        if (!judgedCommands.isEmpty()) {
                            executeCommands(player, judgedCommands);
                            player.sendSystemMessage(Component.literal("§6" + deity.getDisplayName() + " grants divine intervention..."));
                        }
                    } else {
                        // Suggest commands for player approval
                        if (!response.commands.isEmpty()) {
                            suggestCommands(player, deity.getDisplayName(), response.commands, conversationConfig);
                        }
                    }
                } else {
                    sendDeityMessage(player, deity.getDisplayName(), "I cannot hear you clearly. Perhaps try speaking again later.");
                }
            })
            .exceptionally(throwable -> {
                LOGGER.error("Failed to process deity conversation", throwable);
                sendDeityMessage(player, deity.getDisplayName(), "The divine connection wavers. Perhaps try again later.");
                return null;
            });
    }
    
    /**
     * Get commands based on deity's automatic judgment of the player
     */
    private static List<String> getJudgedCommands(ServerPlayer player, DatapackDeity deity, AIDeityConfig.PrayerAIConfig config) {
        List<String> commands = new ArrayList<>();
        
        if (!config.autoJudgeCommands || config.judgmentConfig == null) {
            return commands;
        }
        
        int reputation = deity.getPlayerReputation(player);
        AIDeityConfig.JudgmentConfig judgment = config.judgmentConfig;
        
        // Determine command category based on reputation
        List<String> sourceCommands;
        if (reputation >= judgment.blessingThreshold) {
            sourceCommands = judgment.blessingCommands;
        } else if (reputation <= judgment.curseThreshold) {
            sourceCommands = judgment.curseCommands;
        } else {
            sourceCommands = judgment.neutralCommands;
        }
        
        // Select up to max_commands from the appropriate category
        int maxCommands = Math.min(config.maxCommands, sourceCommands.size());
        for (int i = 0; i < maxCommands; i++) {
            String command = sourceCommands.get(i);
            // Validate command is allowed
            if (isCommandAllowed(command, config.allowedCommands)) {
                commands.add(command);
            }
        }
        
        return commands;
    }
    
    private static String buildConversationPrompt(ServerPlayer player, DatapackDeity deity, AIDeityConfig aiConfig, 
                                                  AIDeityConfig.PrayerAIConfig config, String playerMessage) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Player ").append(player.getName().getString()).append(" says to you: \"").append(playerMessage).append("\"");
        prompt.append("\n\nRespond as ").append(deity.getDisplayName()).append(" in character. ");
        prompt.append("Player reputation: ").append(deity.getPlayerReputation(player));
        
        // Add context
        String context = GeminiAPIClient.buildPlayerContext(player);
        prompt.append("\n\nPlayer situation: ").append(context);
        
        // Add command guidelines if applicable
        if (config != null && config.maxCommands > 0) {
            prompt.append("\n\nIf appropriate, you may suggest up to ").append(config.maxCommands).append(" commands to help the player. ");
            prompt.append("Available commands: ").append(String.join(", ", config.allowedCommands));
            prompt.append("\nFormat any commands as: Command: /command arguments");
            prompt.append("\nOnly suggest commands if the player specifically asks for help or if it fits the conversation naturally.");
        }
        
        prompt.append("\n\nKeep your response conversational and in character. Do not automatically grant blessings unless the player specifically asks and it makes sense contextually.");
        
        return prompt.toString();
    }
    
    private static void suggestCommands(ServerPlayer player, String deityName, List<String> commands, AIDeityConfig.PrayerAIConfig config) {
        // Store pending commands
        pendingCommands.put(player.getUUID(), new ArrayList<>(commands));
        
        // Display command suggestions
        player.sendSystemMessage(Component.literal("§e" + deityName + " offers divine assistance:"));
        
        for (String command : commands) {
            // Validate command is allowed
            if (config != null && !isCommandAllowed(command, config.allowedCommands)) {
                continue;
            }
            
            player.sendSystemMessage(Component.literal("§7  • " + command));
        }
        
        // Create interactive acceptance buttons
        Component acceptButton = Component.literal("§a[Accept]")
            .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/deity_accept_commands"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Accept the deity's assistance")))
                .withColor(ChatFormatting.GREEN));
        
        Component declineButton = Component.literal("§c[Decline]")
            .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/deity_decline_commands"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Decline the deity's assistance")))
                .withColor(ChatFormatting.RED));
        
        Component message = Component.literal("§7Would you like to accept this assistance? ")
            .append(acceptButton)
            .append(Component.literal(" "))
            .append(declineButton);
        
        player.sendSystemMessage(message);
        player.sendSystemMessage(Component.literal("§7Or type 'yes'/'accept' or 'no'/'decline' in chat."));
    }
    
    private static void executeAccountedCommands(ServerPlayer player) {
        executePlayerCommands(player, true);
    }
    
    private static void declineCommands(ServerPlayer player) {
        executePlayerCommands(player, false);
    }
    
    /**
     * Execute or decline pending commands for a player
     * Public method for command system
     */
    public static void executePlayerCommands(ServerPlayer player, boolean accept) {
        List<String> commands = pendingCommands.remove(player.getUUID());
        if (commands == null || commands.isEmpty()) {
            if (accept) {
                player.sendSystemMessage(Component.literal("§cNo pending commands to execute."));
            } else {
                player.sendSystemMessage(Component.literal("§7No assistance was offered."));
            }
            return;
        }
        
        if (accept) {
            player.sendSystemMessage(Component.literal("§aYou accept the divine assistance..."));
            executeCommands(player, commands);
        } else {
            player.sendSystemMessage(Component.literal("§7You politely decline the divine assistance."));
        }
    }
    
    private static void executeCommands(ServerPlayer player, List<String> commands) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        
        Commands commandManager = server.getCommands();
        
        for (String command : commands) {
            try {
                // Create command source as the player
                CommandSourceStack source = player.createCommandSourceStack()
                    .withPermission(2) // Op level 2 for deity commands
                    .withSuppressedOutput();
                
                // Execute command
                String cleanCommand = command.startsWith("/") ? command.substring(1) : command;
                commandManager.performPrefixedCommand(source, cleanCommand);
                
                LOGGER.debug("Executed deity command for {}: {}", player.getName().getString(), command);
                
            } catch (Exception e) {
                LOGGER.error("Failed to execute deity command: " + command, e);
                player.sendSystemMessage(Component.literal("§cFailed to execute: " + command));
            }
        }
    }
    
    private static void endConversation(ServerPlayer player, ResourceLocation deityId) {
        activeConversations.remove(player.getUUID());
        pendingCommands.remove(player.getUUID());
        
        DatapackDeity deity = DatapackDeityManager.getDeity(deityId);
        String deityName = deity != null ? deity.getDisplayName() : "the deity";
        
        player.sendSystemMessage(Component.literal("§6✦ Your conversation with " + deityName + " ends ✦"));
        
        // Set cooldown for future conversations
        AIDeityConfig aiConfig = AIDeityManager.getAIConfig(deityId);
        if (aiConfig != null) {
            AIDeityConfig.PrayerAIConfig conversationConfig = aiConfig.getPrayerConfig("conversation");
            if (conversationConfig == null) {
                conversationConfig = aiConfig.getPrayerConfig("general");
            }
            
            if (conversationConfig != null) {
                setCooldown(player.getUUID(), deityId.toString());
            }
        }
    }
    
    private static void sendDeityMessage(ServerPlayer player, String deityName, String message) {
        player.sendSystemMessage(Component.literal("§6[" + deityName + "] §f" + message));
    }
    
    private static boolean isCommandAllowed(String command, List<String> allowedCommands) {
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
     * Check if player is currently in conversation with a deity
     */
    public static boolean isInConversation(ServerPlayer player) {
        return activeConversations.containsKey(player.getUUID());
    }
    
    /**
     * Get the deity the player is currently conversing with
     */
    public static ResourceLocation getCurrentDeity(ServerPlayer player) {
        return activeConversations.get(player.getUUID());
    }
}
