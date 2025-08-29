package com.bluelotuscoding.eidolonunchained.chat;

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
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles chat-based conversations with AI deities
 */
@Mod.EventBusSubscriber(modid = "eidolonunchained")
public class DeityChat {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Track active conversations: player UUID -> deity ID
    private static final Map<UUID, ResourceLocation> activeConversations = new ConcurrentHashMap<>();
    
    // Track conversation history: player UUID -> list of messages
    private static final Map<UUID, List<String>> conversationHistory = new ConcurrentHashMap<>();
    
    /**
     * Start a conversation between a player and a deity
     */
    public static void startConversation(ServerPlayer player, ResourceLocation deityId) {
        UUID playerId = player.getUUID();
        
        // End any existing conversation
        if (activeConversations.containsKey(playerId)) {
            endConversation(player);
        }
        
        // Start new conversation
        activeConversations.put(playerId, deityId);
        conversationHistory.put(playerId, new ArrayList<>());
        
        // Get deity info
        DatapackDeity deity = DatapackDeityManager.getDeity(deityId);
        if (deity == null) {
            player.sendSystemMessage(Component.literal("§cDeity not found: " + deityId));
            return;
        }
        
        String deityName = deity.getName();
        
        // Send initial message
        player.sendSystemMessage(Component.literal("§6You feel a divine presence..."));
        player.sendSystemMessage(Component.literal("§e" + deityName + " is listening to your prayers."));
        player.sendSystemMessage(Component.literal("§7Speak your mind in chat, or type 'amen' to end the conversation."));
        
        LOGGER.info("Started conversation between player {} and deity {}", player.getName().getString(), deityName);
    }
    
    /**
     * End an active conversation
     */
    public static void endConversation(ServerPlayer player) {
        UUID playerId = player.getUUID();
        ResourceLocation deityId = activeConversations.remove(playerId);
        conversationHistory.remove(playerId);
        
        if (deityId != null) {
            DatapackDeity deity = DatapackDeityManager.getDeity(deityId);
            String deityName = deity != null ? deity.getName() : deityId.toString();
            
            player.sendSystemMessage(Component.literal("§6The divine presence fades..."));
            player.sendSystemMessage(Component.literal("§e" + deityName + " has heard your prayers."));
            
            LOGGER.info("Ended conversation between player {} and deity {}", player.getName().getString(), deityName);
        }
    }
    
    /**
     * Check if a player is in an active conversation
     */
    public static boolean isInConversation(ServerPlayer player) {
        return activeConversations.containsKey(player.getUUID());
    }
    
    /**
     * Handle chat events for active conversations
     */
    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        UUID playerId = player.getUUID();
        
        if (!activeConversations.containsKey(playerId)) {
            return; // Not in a conversation
        }
        
        String message = event.getMessage().getString();
        
        // Check for conversation end commands
        if (message.equalsIgnoreCase("amen") || message.equalsIgnoreCase("end") || message.equalsIgnoreCase("stop")) {
            endConversation(player);
            event.setCanceled(true); // Don't broadcast the end command
            return;
        }
        
        // Process the message with the deity
        ResourceLocation deityId = activeConversations.get(playerId);
        processDeityConversation(player, deityId, message);
        
        // Cancel the event to prevent normal chat broadcasting
        event.setCanceled(true);
    }
    
    /**
     * Process a conversation message with a deity
     */
    private static void processDeityConversation(ServerPlayer player, ResourceLocation deityId, String message) {
        UUID playerId = player.getUUID();
        
        try {
            // Get deity and AI config
            DatapackDeity deity = DatapackDeityManager.getDeity(deityId);
            if (deity == null) {
                player.sendSystemMessage(Component.literal("§cDeity not found."));
                endConversation(player);
                return;
            }
            
            AIDeityConfig aiConfig = AIDeityManager.getInstance().getAIConfig(deityId);
            if (aiConfig == null) {
                player.sendSystemMessage(Component.literal("§cAI configuration not found for this deity."));
                endConversation(player);
                return;
            }
            
            // Add message to conversation history
            List<String> history = conversationHistory.get(playerId);
            history.add("Player: " + message);
            
            // Build conversation prompt
            String conversationPrompt = buildConversationPrompt(player, deity, message, history);
            
            // Generate AI response
            String personality = aiConfig.buildDynamicPersonality(new PlayerContext(player, deity));
            
            // Get API key using the API key manager
            String apiKey = APIKeyManager.getAPIKey("gemini");
            if (apiKey == null || apiKey.trim().isEmpty()) {
                LOGGER.error("No Gemini API key configured. Please set up API key using /eidolon-config or environment variables.");
                player.sendSystemMessage(Component.translatable("eidolonunchained.ui.deity.api_key_required"));
                player.sendSystemMessage(Component.literal("§7(Set GEMINI_API_KEY environment variable or configure via /eidolon-config)"));
                endConversation(player);
                return;
            }
            
            GeminiAPIClient client = new GeminiAPIClient(
                apiKey,
                aiConfig.apiSettings.getModel(),
                aiConfig.apiSettings.timeoutSeconds
            );
            
            // Generate AI response asynchronously
            client.generateResponse(
                conversationPrompt, 
                personality, 
                aiConfig.apiSettings.generationConfig, 
                aiConfig.apiSettings.safetySettings
            ).thenAccept(aiResponse -> {
                if (aiResponse == null) {
                    player.sendSystemMessage(Component.translatable("eidolonunchained.ui.deity.no_response"));
                    return;
                }
                
                String response = aiResponse.dialogue;
                
                // Add response to history
                history.add("Deity: " + response);
                
                // Check for auto-judgment and commands
                if (aiConfig.prayerConfigs.containsKey("conversation")) {
                    PrayerAIConfig prayerConfig = aiConfig.prayerConfigs.get("conversation");
                    if (prayerConfig.autoJudgeCommands) {
                        List<String> commands = getJudgedCommands(player, deity, prayerConfig);
                        if (!commands.isEmpty()) {
                            executeCommands(player, commands);
                            response += "\n\n§6[Divine intervention enacted]";
                        }
                    }
                }
                
                // Send deity response to player using prominent title/subtitle display
                sendDeityResponse(player, deity.getName(), response);
                
            }).exceptionally(throwable -> {
                LOGGER.error("Error generating AI response: {}", throwable.getMessage(), throwable);
                sendDeityResponse(player, "Divine Connection", "falters...");
                return null;
            });
            
        } catch (Exception e) {
            LOGGER.error("Error processing deity conversation for player {} with deity {}: {}", 
                player.getName().getString(), deityId, e.getMessage(), e);
            player.sendSystemMessage(Component.literal("§cThe divine connection falters..."));
            endConversation(player);
        }
    }
    
    /**
     * Build a prompt for conversation context
     */
    private static String buildConversationPrompt(ServerPlayer player, DatapackDeity deity, String currentMessage, List<String> history) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are ").append(deity.getName()).append(", a deity in the world of Minecraft. ");
        prompt.append("You are having a conversation with ").append(player.getName().getString()).append(". ");
        
        // Add context about player's reputation
        int reputation = (int) deity.getPlayerReputation(player);
        if (reputation > 25) {
            prompt.append("This player is highly favored by you (reputation: ").append(reputation).append("). ");
        } else if (reputation < -25) {
            prompt.append("This player has greatly displeased you (reputation: ").append(reputation).append("). ");
        } else {
            prompt.append("This player is neutral in your eyes (reputation: ").append(reputation).append("). ");
        }
        
        // Add detailed player context using GeminiAPIClient's context builder
        try {
            String playerContext = GeminiAPIClient.buildPlayerContext(player);
            prompt.append("\n\nPlayer Context:\n").append(playerContext);
        } catch (Exception e) {
            LOGGER.warn("Failed to build player context: {}", e.getMessage());
        }
        
        // Add conversation history if available
        if (!history.isEmpty()) {
            prompt.append("\n\nConversation history:\n");
            for (String historyMessage : history.subList(Math.max(0, history.size() - 6), history.size())) {
                prompt.append(historyMessage).append("\n");
            }
        }
        
        prompt.append("\nCurrent message: ").append(currentMessage);
        prompt.append("\n\nRespond as the deity, keeping your response conversational and under 80 words. ");
        prompt.append("If the player asks for help, items, powers, blessings, or requests anything, ");
        prompt.append("judge their worthiness based on reputation and either grant or deny their request. ");
        prompt.append("When you grant requests, mention that you are bestowing gifts or power. ");
        prompt.append("When you deny requests, explain why they are unworthy. ");
        prompt.append("Acknowledge the player's current situation and recent activities if relevant. ");
        prompt.append("Do not include commands or actions in brackets - the system will handle rewards automatically.");
        
        return prompt.toString();
    }
    
    /**
     * Get commands based on automatic judgment
     */
    private static List<String> getJudgedCommands(ServerPlayer player, DatapackDeity deity, PrayerAIConfig prayerConfig) {
        int reputation = (int) deity.getPlayerReputation(player);
        
        if (reputation >= prayerConfig.judgmentConfig.blessingThreshold) {
            return new ArrayList<>(prayerConfig.judgmentConfig.blessingCommands);
        } else if (reputation <= prayerConfig.judgmentConfig.curseThreshold) {
            return new ArrayList<>(prayerConfig.judgmentConfig.curseCommands);
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Execute a list of commands
     */
    private static void executeCommands(ServerPlayer player, List<String> commands) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        
        CommandSourceStack commandSource = server.createCommandSourceStack()
            .withSource(CommandSource.NULL)
            .withLevel(player.serverLevel())
            .withPosition(player.position())
            .withPermission(4); // Admin permission level
        
        for (String command : commands) {
            try {
                // Replace placeholders
                String processedCommand = command
                    .replace("{player}", player.getName().getString())
                    .replace("{x}", String.valueOf((int) player.getX()))
                    .replace("{y}", String.valueOf((int) player.getY()))
                    .replace("{z}", String.valueOf((int) player.getZ()));
                
                server.getCommands().performPrefixedCommand(commandSource, processedCommand);
                LOGGER.debug("Executed deity command: {}", processedCommand);
                
            } catch (Exception e) {
                LOGGER.error("Failed to execute deity command '{}': {}", command, e.getMessage());
            }
        }
    }
    
    /**
     * Send deity response with prominent display above action bar (matches PrayerSystem)
     */
    private static void sendDeityResponse(ServerPlayer player, String deityName, String message) {
        // Check configuration for display preferences
        boolean useProminentDisplay = EidolonUnchainedConfig.COMMON.useProminentDisplay.get();
        boolean useChatForLongMessages = EidolonUnchainedConfig.COMMON.useChatForLongMessages.get();
        int maxSubtitleLength = EidolonUnchainedConfig.COMMON.maxSubtitleLength.get();
        
        if (!useProminentDisplay) {
            // Use regular chat message
            Component chatMessage = Component.literal("§6[" + deityName + "]§r " + message);
            player.sendSystemMessage(chatMessage);
            return;
        }
        
        // Create title and subtitle components with enhanced formatting
        Component titleComponent = Component.literal("§6§l" + deityName); // Bold deity name
        Component subtitleComponent;
        
        // Handle long messages
        if (message.length() > maxSubtitleLength) {
            if (useChatForLongMessages) {
                // Use regular chat for long messages
                Component chatMessage = Component.literal("§6[" + deityName + "]§r " + message);
                player.sendSystemMessage(chatMessage);
                return;
            } else {
                // Split message for display
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
                
                // Use action bar for longer messages with better formatting
                Component actionBarMessage = Component.literal("§f" + line1.toString());
                if (line2.length() > 0) {
                    actionBarMessage = Component.literal("§f" + line1.toString() + " §7" + line2.toString());
                }
                player.sendSystemMessage(actionBarMessage, true); // true = action bar
                
                // Still show deity name as title with indication
                subtitleComponent = Component.literal("§7⟨ speaks to you ⟩");
            }
        } else {
            // Short message - use white text for better readability
            subtitleComponent = Component.literal("§f" + message);
        }
        
        // Set title animation timing (fade in, stay, fade out) in ticks
        int fadeInTicks = EidolonUnchainedConfig.COMMON.fadeInTicks.get();
        int displayDurationTicks = EidolonUnchainedConfig.COMMON.displayDurationTicks.get();
        int fadeOutTicks = EidolonUnchainedConfig.COMMON.fadeOutTicks.get();
        
        ClientboundSetTitlesAnimationPacket animationPacket = new ClientboundSetTitlesAnimationPacket(
            fadeInTicks,
            displayDurationTicks,
            fadeOutTicks
        );
        
        // Send packets to display the title/subtitle
        player.connection.send(animationPacket);
        player.connection.send(new ClientboundSetTitleTextPacket(titleComponent));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitleComponent));
    }
}
