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
     * Build a comprehensive prompt for conversation context including proactive assistance
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
            prompt.append("\n\nPlayer Current State:\n").append(playerContext);
        } catch (Exception e) {
            LOGGER.warn("Failed to build player context: {}", e.getMessage());
        }
        
        // Add conversation history for context
        if (!history.isEmpty()) {
            prompt.append("\n\nConversation History (last few messages):\n");
            int startIndex = Math.max(0, history.size() - 4); // Last 4 messages
            for (int i = startIndex; i < history.size(); i++) {
                prompt.append(history.get(i)).append("\n");
            }
        }
        
        // Add current player message with emphasis
        prompt.append("\n\nPlayer's Current Message: \"").append(currentMessage).append("\"\n");
        
        // Enhanced proactive assistance and request handling guidance
        prompt.append("\n\nCRITICAL DIVINE RESPONSE INSTRUCTIONS:\n");
        prompt.append("1. ANALYZE PLAYER'S MESSAGE: Look for requests, questions, or needs\n");
        prompt.append("2. ASSESS THEIR CONDITION: Check health, hunger, and danger status\n");
        prompt.append("3. CONSIDER YOUR RELATIONSHIP: Factor in reputation and past interactions\n");
        prompt.append("4. RESPOND APPROPRIATELY:\n");
        prompt.append("   - If they ask for help/items/powers: Judge worthiness and grant/deny with reason\n");
        prompt.append("   - If they're in danger/hurt/hungry: Offer assistance based on reputation\n");
        prompt.append("   - If they're just talking: Engage meaningfully based on their message\n");
        prompt.append("   - If they're thankful: Acknowledge and build relationship\n");
        prompt.append("5. BE ENGAGING: Reference their specific situation, respond to their exact words\n");
        prompt.append("6. COMMAND AUTHORITY: You CAN grant items, effects, and blessings when appropriate\n");
        prompt.append("7. STAY IN CHARACTER: Maintain your divine personality while being helpful\n");
        prompt.append("\nIMPORTANT: Address their specific message directly, don't just give generic responses!\n");
        
        return prompt.toString();
    }
    
    /**
     * Get commands based on automatic judgment (enhanced for request handling)
     */
    /**
     * Get commands based on automatic judgment (enhanced for request handling)
     */
    private static List<String> getJudgedCommands(ServerPlayer player, DatapackDeity deity, PrayerAIConfig prayerConfig) {
        int reputation = (int) deity.getPlayerReputation(player);
        List<String> commands = new ArrayList<>();
        
        // Check if player meets basic blessing threshold
        if (reputation >= prayerConfig.judgmentConfig.blessingThreshold) {
            commands.addAll(prayerConfig.judgmentConfig.blessingCommands);
        } else if (reputation <= prayerConfig.judgmentConfig.curseThreshold) {
            commands.addAll(prayerConfig.judgmentConfig.curseCommands);
        } else {
            // Even neutral players might get basic help if in dire need
            float healthPercentage = (player.getHealth() / player.getMaxHealth()) * 100;
            int foodLevel = player.getFoodData().getFoodLevel();
            
            if (healthPercentage <= 25 && reputation >= 0) {
                // Emergency healing for anyone with non-negative reputation
                commands.add("effect give {player} minecraft:regeneration 60 0");
            } else if (foodLevel <= 6 && reputation >= 5) {
                // Basic food for hungry players with minimal reputation
                commands.add("give {player} minecraft:bread 2");
            } else if (reputation >= prayerConfig.judgmentConfig.blessingThreshold / 2) {
                // Neutral commands for moderate reputation
                commands.addAll(prayerConfig.judgmentConfig.neutralCommands);
            }
        }
        
        return commands;
    }
    
    /**
     * Execute a list of commands with enhanced feedback
     */
    private static void executeCommands(ServerPlayer player, List<String> commands) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        
        CommandSourceStack commandSource = server.createCommandSourceStack()
            .withSource(CommandSource.NULL)
            .withLevel(player.serverLevel())
            .withPosition(player.position())
            .withPermission(4); // Admin permission level
        
        int successCount = 0;
        for (String command : commands) {
            try {
                // Replace placeholders
                String processedCommand = command
                    .replace("{player}", player.getName().getString())
                    .replace("{x}", String.valueOf((int) player.getX()))
                    .replace("{y}", String.valueOf((int) player.getY()))
                    .replace("{z}", String.valueOf((int) player.getZ()));
                
                server.getCommands().performPrefixedCommand(commandSource, processedCommand);
                successCount++;
                LOGGER.info("Successfully executed deity command for {}: {}", player.getName().getString(), processedCommand);
                
            } catch (Exception e) {
                LOGGER.error("Failed to execute deity command '{}' for player {}: {}", command, player.getName().getString(), e.getMessage());
            }
        }
        
        // Send feedback to player about divine intervention
        if (successCount > 0) {
            player.sendSystemMessage(Component.literal("§6✦ Divine power flows through you... §7(" + successCount + " blessing" + (successCount == 1 ? "" : "s") + " granted)"));
        }
    }
    
    /**
     * Public method for other systems to use the improved deity display
     */
    public static void sendDeityResponsePublic(ServerPlayer player, String deityName, String message, boolean isError) {
        if (isError) {
            // For error messages, use enhanced chat with red formatting
            sendEnhancedChatMessage(player, "§c" + deityName, "§c" + message);
        } else {
            // For normal messages, use the standard display system
            sendDeityResponse(player, deityName, message);
        }
    }

    /**
     * Send deity response with improved formatting and display options
     */
    private static void sendDeityResponse(ServerPlayer player, String deityName, String message) {
        // Get display configuration
        String displayMethod = EidolonUnchainedConfig.COMMON.displayMethod.get();
        boolean useProminentDisplay = EidolonUnchainedConfig.COMMON.useProminentDisplay.get();
        int maxSubtitleLength = EidolonUnchainedConfig.COMMON.maxSubtitleLength.get();
        
        // Auto-select display method if configured
        if ("AUTO".equals(displayMethod)) {
            if (message.length() > maxSubtitleLength) {
                displayMethod = "ENHANCED_CHAT";
            } else {
                displayMethod = "ACTION_BAR";
            }
        }
        
        // Route to appropriate display method
        switch (displayMethod) {
            case "TITLE_SUBTITLE":
                sendLegacyTitleSubtitle(player, deityName, message);
                break;
            case "ACTION_BAR":
                sendActionBarDisplay(player, deityName, message);
                break;
            case "ENHANCED_CHAT":
                sendEnhancedChatMessage(player, deityName, message);
                break;
            default:
                // Fallback to enhanced chat for safety
                sendEnhancedChatMessage(player, deityName, message);
        }
    }
    
    /**
     * Legacy title/subtitle display (original system)
     */
    private static void sendLegacyTitleSubtitle(ServerPlayer player, String deityName, String message) {
        int maxSubtitleLength = EidolonUnchainedConfig.COMMON.maxSubtitleLength.get();
        
        Component titleComponent = Component.literal("§6§l" + deityName);
        Component subtitleComponent;
        
        if (message.length() > maxSubtitleLength) {
            // Split long messages
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
            
            subtitleComponent = Component.literal("§f" + line1.toString());
            if (line2.length() > 0) {
                // Send second line to action bar
                Component actionBar = Component.literal("§7" + line2.toString());
                player.sendSystemMessage(actionBar, true);
            }
        } else {
            subtitleComponent = Component.literal("§f" + message);
        }
        
        // Send title/subtitle packets
        int fadeInTicks = EidolonUnchainedConfig.COMMON.fadeInTicks.get();
        int displayDurationTicks = EidolonUnchainedConfig.COMMON.displayDurationTicks.get();
        int fadeOutTicks = EidolonUnchainedConfig.COMMON.fadeOutTicks.get();
        
        ClientboundSetTitlesAnimationPacket animationPacket = new ClientboundSetTitlesAnimationPacket(
            fadeInTicks, displayDurationTicks, fadeOutTicks);
        
        player.connection.send(animationPacket);
        player.connection.send(new ClientboundSetTitleTextPacket(titleComponent));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitleComponent));
    }
    
    /**
     * Action bar display - centered, readable, persistent
     */
    private static void sendActionBarDisplay(ServerPlayer player, String deityName, String message) {
        int maxSubtitleLength = EidolonUnchainedConfig.COMMON.maxSubtitleLength.get();
        
        if (message.length() > maxSubtitleLength) {
            // Long messages: use action bar + chat combination
            sendActionBarWithChat(player, deityName, message);
        } else {
            // Short messages: use persistent action bar
            sendPersistentActionBar(player, deityName, message);
        }
    }
    
    /**
     * Send enhanced chat message with proper formatting and visual appeal
     */
    private static void sendEnhancedChatMessage(ServerPlayer player, String deityName, String message) {
        // Send a visual separator
        player.sendSystemMessage(Component.literal("§8§l═══════════════════════════════════════"));
        
        // Send deity name header
        player.sendSystemMessage(Component.literal("§6§l⟦ " + deityName + " ⟧"));
        
        // Split long messages into readable chunks
        String[] words = message.split(" ");
        StringBuilder currentLine = new StringBuilder();
        
        for (String word : words) {
            if (currentLine.length() + word.length() + 1 > 50) { // 50 chars per line for readability
                if (currentLine.length() > 0) {
                    player.sendSystemMessage(Component.literal("§f" + currentLine.toString()));
                    currentLine = new StringBuilder();
                }
            }
            if (currentLine.length() > 0) currentLine.append(" ");
            currentLine.append(word);
        }
        
        // Send remaining text
        if (currentLine.length() > 0) {
            player.sendSystemMessage(Component.literal("§f" + currentLine.toString()));
        }
        
        // Send footer
        player.sendSystemMessage(Component.literal("§8§l═══════════════════════════════════════"));
        
        // Add a brief action bar notification
        Component actionBarNotice = Component.literal("§6" + deityName + " §7has spoken to you");
        player.sendSystemMessage(actionBarNotice, true);
    }
    
    /**
     * Send action bar message combined with chat for long content
     */
    private static void sendActionBarWithChat(ServerPlayer player, String deityName, String message) {
        // Brief action bar notification
        Component actionBar = Component.literal("§6§l" + deityName + " §7⟨ speaks ⟩");
        player.sendSystemMessage(actionBar, true);
        
        // Detailed message in chat with better formatting
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§8§m         §r §6§l" + deityName + " §8§m         "));
        
        // Word wrap the message for better readability
        String[] sentences = message.split("\\. ");
        for (int i = 0; i < sentences.length; i++) {
            String sentence = sentences[i];
            if (i < sentences.length - 1 && !sentence.endsWith(".")) {
                sentence += ".";
            }
            
            // Further split long sentences
            if (sentence.length() > 60) {
                String[] words = sentence.split(" ");
                StringBuilder line = new StringBuilder();
                
                for (String word : words) {
                    if (line.length() + word.length() + 1 > 60) {
                        if (line.length() > 0) {
                            player.sendSystemMessage(Component.literal("§f" + line.toString()));
                            line = new StringBuilder();
                        }
                    }
                    if (line.length() > 0) line.append(" ");
                    line.append(word);
                }
                
                if (line.length() > 0) {
                    player.sendSystemMessage(Component.literal("§f" + line.toString()));
                }
            } else {
                player.sendSystemMessage(Component.literal("§f" + sentence));
            }
        }
        
        player.sendSystemMessage(Component.literal("§8§m                    "));
    }
    
    /**
     * Send persistent action bar message that stays visible longer
     */
    private static void sendPersistentActionBar(ServerPlayer player, String deityName, String message) {
        // Create the action bar message with proper formatting
        String formattedMessage = "§6§l" + deityName + "§r§7: §f" + message;
        Component actionBarComponent = Component.literal(formattedMessage);
        
        // Send multiple times for persistence (action bar messages fade quickly)
        player.sendSystemMessage(actionBarComponent, true);
        
        // Schedule additional sends for persistence
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(1000); // 1 second
                player.sendSystemMessage(actionBarComponent, true);
                
                Thread.sleep(1000); // Another second
                player.sendSystemMessage(actionBarComponent, true);
                
                Thread.sleep(1000); // Final send
                player.sendSystemMessage(actionBarComponent, true);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        // Also send a brief chat notification for reference
        player.sendSystemMessage(Component.literal("§8[§6" + deityName + " §8has spoken]"));
    }
}
