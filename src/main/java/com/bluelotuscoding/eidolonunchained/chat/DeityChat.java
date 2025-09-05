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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;
import net.minecraft.server.MinecraftServer;

import java.util.Arrays;
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
        
        // Check if deity exists
        DatapackDeity deity = DatapackDeityManager.getDeity(deityId);
        if (deity == null) {
            player.sendSystemMessage(Component.translatable("eidolonunchained.ui.deity.not_found", deityId));
            return;
        }
        
        // Check AI configuration and patron allegiance rules
        AIDeityConfig aiConfig = AIDeityManager.getInstance().getAIConfig(deityId);
        if (aiConfig == null) {
            player.sendSystemMessage(Component.translatable("eidolonunchained.ui.deity.no_mortal_contact", deity.getName()));
            return;
        }
        
        // PATRON ALLEGIANCE CHECK - Core new functionality
        if (!aiConfig.canRespondToPlayer(player)) {
            // Send patron-specific rejection message
            sendPatronRejectionMessage(player, deity, aiConfig);
            return;
        }
        
        // End any existing conversation
        if (activeConversations.containsKey(playerId)) {
            endConversation(player);
        }
        
        // Start new conversation
        activeConversations.put(playerId, deityId);
        conversationHistory.put(playerId, new ArrayList<>());
        
        String deityName = deity.getName();
        
        // Send patron-aware initial message
        sendPatronAwareGreeting(player, deity, aiConfig);
        
        LOGGER.info("Started conversation between player {} and deity {}", player.getName().getString(), deityName);
    }
    
    /**
     * Send patron-specific rejection message based on allegiance
     */
    private static void sendPatronRejectionMessage(ServerPlayer player, DatapackDeity deity, AIDeityConfig aiConfig) {
        try {
            player.level().getCapability(com.bluelotuscoding.eidolonunchained.capability.CapabilityHandler.PATRON_DATA_CAPABILITY)
                .ifPresent(patronData -> {
                    ResourceLocation playerPatron = patronData.getPatron(player);
                    AIDeityConfig.PatronRelationship relationship = aiConfig.determinePatronRelationship(playerPatron);
                    
                    switch (relationship) {
                        case NO_PATRON:
                            player.sendSystemMessage(Component.translatable("eidolonunchained.ui.deity.godless_rejected", deity.getName()));
                            player.sendSystemMessage(Component.translatable("eidolonunchained.ui.deity.choose_patron"));
                            break;
                        case ENEMY:
                            player.sendSystemMessage(Component.translatable("eidolonunchained.ui.deity.corrupted_presence", deity.getName()));
                            player.sendSystemMessage(Component.translatable("eidolonunchained.ui.deity.enemy_allegiance"));
                            // Apply reputation penalty for daring to contact enemy
                            if (aiConfig.patron_config.conversationRules.containsKey("enemy_restrictions")) {
                                Map<String, Object> rules = (Map<String, Object>) aiConfig.patron_config.conversationRules.get("enemy_restrictions");
                                if (rules.containsKey("reputation_penalty_on_contact")) {
                                    int penalty = (Integer) rules.get("reputation_penalty_on_contact");
                                    // Apply penalty through Eidolon's reputation system
                                    player.level().getCapability(elucent.eidolon.capability.IReputation.INSTANCE)
                                        .ifPresent(reputation -> {
                                            double currentRep = reputation.getReputation(player, aiConfig.deity_id);
                                            reputation.setReputation(player, aiConfig.deity_id, currentRep + penalty);
                                        });
                                }
                            }
                            break;
                        case NEUTRAL:
                            if (aiConfig.patron_config.requiresPatronStatus.equals("follower_only")) {
                                player.sendSystemMessage(Component.translatable("eidolonunchained.ui.deity.faithful_only", deity.getName()));
                                player.sendSystemMessage(Component.translatable("eidolonunchained.ui.deity.prove_devotion"));
                            }
                            break;
                    }
                });
        } catch (Exception e) {
            player.sendSystemMessage(Component.translatable("eidolonunchained.ui.deity.is_silent", deity.getName()));
        }
    }
    
    /**
     * Send patron-aware greeting message
     */
    private static void sendPatronAwareGreeting(ServerPlayer player, DatapackDeity deity, AIDeityConfig aiConfig) {
        try {
            player.level().getCapability(com.bluelotuscoding.eidolonunchained.capability.CapabilityHandler.PATRON_DATA_CAPABILITY)
                .ifPresent(patronData -> {
                    ResourceLocation playerPatron = patronData.getPatron(player);
                    AIDeityConfig.PatronRelationship relationship = aiConfig.determinePatronRelationship(playerPatron);
                    String title = patronData.getTitle(player);
                    
                    switch (relationship) {
                        case FOLLOWER:
                            player.sendSystemMessage(Component.translatable("eidolonunchained.ui.deity.divine_presence"));
                            if (title != null && !title.isEmpty()) {
                                player.sendSystemMessage(Component.translatable("eidolonunchained.ui.deity.recognizes_faithful", deity.getName(), title));
                            } else {
                                player.sendSystemMessage(Component.translatable("eidolonunchained.ui.deity.recognizes_faithful", deity.getName(), "servant"));
                            }
                            break;
                        case ALLIED:
                            player.sendSystemMessage(Component.literal("§b" + deity.getName() + " §7acknowledges an ally of the divine."));
                            break;
                        case NEUTRAL:
                            player.sendSystemMessage(Component.literal("§7A cautious divine presence observes you..."));
                            player.sendSystemMessage(Component.literal("§e" + deity.getName() + " §7regards you with wariness."));
                            break;
                        default:
                            player.sendSystemMessage(Component.literal("§6You feel a divine presence..."));
                            player.sendSystemMessage(Component.literal("§e" + deity.getName() + " is listening to your prayers."));
                    }
                });
        } catch (Exception e) {
            // Fallback to generic greeting
            player.sendSystemMessage(Component.literal("§6You feel a divine presence..."));
            player.sendSystemMessage(Component.literal("§e" + deity.getName() + " is listening to your prayers."));
        }
        
        player.sendSystemMessage(Component.literal("§7Speak your mind in chat, or type 'amen' to end the conversation."));
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
     * SAFE VERSION: Only processes messages for players in active conversations
     */
    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        UUID playerId = player.getUUID();
        
        // SAFETY CHECK: Only process if player is actually in a conversation
        if (!activeConversations.containsKey(playerId)) {
            return; // Not in a conversation - let normal chat proceed
        }
        
        try {
            String message = event.getMessage().getString();
            
            // Check for conversation end commands
            if (message.equalsIgnoreCase("amen") || message.equalsIgnoreCase("end") || message.equalsIgnoreCase("stop")) {
                endConversation(player);
                event.setCanceled(true); // Don't broadcast the end command
                return;
            }
            
            // Process the message with the deity
            ResourceLocation deityId = activeConversations.get(playerId);
            if (deityId != null) {
                processDeityConversation(player, deityId, message);
                event.setCanceled(true); // Only cancel if successfully processed
            }
            
        } catch (Exception e) {
            // SAFETY: If anything goes wrong, end the conversation and let chat proceed normally
            LOGGER.error("Error in deity chat handler, ending conversation for safety: {}", e.getMessage());
            endConversation(player);
            // Don't cancel the event so normal chat works
        }
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
                player.sendSystemMessage(Component.translatable("eidolonunchained.chat.deity_not_found"));
                endConversation(player);
                return;
            }
            
            AIDeityConfig aiConfig = AIDeityManager.getInstance().getAIConfig(deityId);
            if (aiConfig == null) {
                player.sendSystemMessage(Component.translatable("eidolonunchained.chat.ai_config_not_found"));
                endConversation(player);
                return;
            }
            
            // Add message to conversation history
            List<String> history = conversationHistory.get(playerId);
            history.add("Player: " + message);
            
            // Add to persistent history
            ConversationHistoryManager.get().addMessage(player.getUUID(), deityId, "Player", message);
            
            // Build conversation prompt with persistent history
            String conversationPrompt = buildConversationPrompt(player, deity, message, deityId);
            
            // Generate AI response
            String personality = aiConfig.buildDynamicPersonalityWithPatron(new PlayerContext(player, deity), player);
            
            // Generate AI response using deity-specific provider configuration
            String deityProvider = aiConfig.ai_provider != null ? aiConfig.ai_provider : EidolonUnchainedConfig.COMMON.aiProvider.get();
            String apiKey = APIKeyManager.getAPIKey(deityProvider);
            
            // If deity-specific provider doesn't have API key, fall back to global provider
            if (apiKey == null || apiKey.trim().isEmpty()) {
                String globalProvider = EidolonUnchainedConfig.COMMON.aiProvider.get();
                String globalApiKey = APIKeyManager.getAPIKey(globalProvider);
                
                if (globalApiKey != null && !globalApiKey.trim().isEmpty()) {
                    LOGGER.info("Deity {} specified provider '{}' has no API key, falling back to global provider '{}'", 
                        deityId, deityProvider, globalProvider);
                    deityProvider = globalProvider;
                    apiKey = globalApiKey;
                } else {
                    LOGGER.error("No {} API key configured. Please set up API key using /eidolon-unchained api set {} YOUR_KEY", deityProvider, deityProvider);
                    player.sendSystemMessage(Component.translatable("eidolonunchained.ui.deity.api_key_required"));
                    player.sendSystemMessage(Component.translatable("eidolonunchained.chat.api_key_instruction", deityProvider));
                    endConversation(player);
                    return;
                }
            }
            
            // Create AI provider based on effective provider (deity-specific or fallback)
            com.bluelotuscoding.eidolonunchained.ai.AIProviderFactory.AIProvider provider = 
                com.bluelotuscoding.eidolonunchained.ai.AIProviderFactory.createProvider(deityProvider, aiConfig.model);
            
            if (!provider.isAvailable()) {
                LOGGER.error("AI provider {} is not available", provider.getProviderName());
                player.sendSystemMessage(Component.translatable("eidolonunchained.chat.provider_not_available", provider.getProviderName()));
                endConversation(player);
                return;
            }
            
            // Build context for AI provider
            String context = "deity:" + deityId.toString() + ",player:" + player.getStringUUID();
            
            // Generate AI response asynchronously
            provider.generateResponse(
                conversationPrompt, 
                personality,
                context,
                aiConfig.api_settings.generationConfig, 
                aiConfig.api_settings.safetySettings
            ).thenAccept(aiResponse -> {
                if (aiResponse == null) {
                    player.sendSystemMessage(Component.translatable("eidolonunchained.ui.deity.no_response"));
                    return;
                }
                
                String rawResponse = aiResponse.dialogue;
                LOGGER.info("🔥 DEBUG: AI Response received: '{}'", rawResponse);
                
                // 🔥 HYBRID APPROACH: Check player input first, then AI decision
                LOGGER.info("🔥 DEBUG: Starting hybrid command extraction...");
                
                // Step 1: Check if player explicitly requested something
                List<String> playerRequestCommands = com.bluelotuscoding.eidolonunchained.integration.ai.EnhancedCommandExtractor
                    .extractExplicitRequests(message, player);
                LOGGER.info("🔥 DEBUG: Player explicit requests: {}", playerRequestCommands);
                
                // Step 2: If no explicit requests, check if AI wants to give something contextually
                List<String> aiContextCommands = new ArrayList<>();
                if (playerRequestCommands.isEmpty()) {
                    aiContextCommands = com.bluelotuscoding.eidolonunchained.integration.ai.EnhancedCommandExtractor
                        .extractContextualActions(rawResponse, player, message);
                    LOGGER.info("🔥 DEBUG: AI contextual actions: {}", aiContextCommands);
                }
                
                // Combine commands (player requests take priority)
                List<String> extractedCommands = new ArrayList<>();
                extractedCommands.addAll(playerRequestCommands);
                extractedCommands.addAll(aiContextCommands);
                
                LOGGER.info("🔥 DEBUG: Total extracted commands: {}", extractedCommands);
                
                int commandsExecuted = 0;
                if (!extractedCommands.isEmpty()) {
                    // 🔥 TIER ENFORCEMENT: Check if player is allowed to receive blessings
                    if (shouldAllowBlessing(player, deity, message)) {
                        // Limit commands based on progression level
                        String progressionLevel = getDynamicProgressionLevel(deity, player);
                        int maxCommands = getMaxCommandsForTier(progressionLevel);
                        
                        // Limit the commands to appropriate tier
                        List<String> limitedCommands = extractedCommands.size() > maxCommands ? 
                            extractedCommands.subList(0, maxCommands) : extractedCommands;
                        
                        commandsExecuted = com.bluelotuscoding.eidolonunchained.integration.ai.EnhancedCommandExtractor
                            .executeCommands(limitedCommands, player);
                        
                        LOGGER.info("🔥 Player request fulfilled: executed {} commands for {} (tier: {}, max: {}): {}", 
                            commandsExecuted, player.getName().getString(), progressionLevel, maxCommands, limitedCommands);
                    } else {
                        LOGGER.info("🚫 Blessing request denied for {} due to tier restrictions or cooldown", 
                            player.getName().getString());
                        // Still allow AI to respond, just don't give items
                    }
                }
                
                // Clean response for display (remove any technical mod IDs that leaked through)
                String cleanedResponse = cleanModIdLeakage(rawResponse);
                
                // Add response to history (using cleaned version)
                history.add("Deity: " + cleanedResponse);
                
                // Add to persistent history
                ConversationHistoryManager.get().addMessage(player.getUUID(), deityId, deity.getName(), cleanedResponse);
                
                // Check for auto-judgment and additional commands only if no commands were already executed
                if (commandsExecuted == 0 && aiConfig.prayer_configs.containsKey("conversation")) {
                    PrayerAIConfig prayerConfig = aiConfig.prayer_configs.get("conversation");
                    if (prayerConfig.auto_judge_commands) {
                        List<String> commands = getJudgedCommands(player, deity, prayerConfig);
                        if (!commands.isEmpty()) {
                            // Log AI decision for debugging
                            ConversationHistoryManager.logAIDecisionStatic(player, deityId, "AUTO_JUDGMENT", 
                                "Reputation: " + (int)deity.getPlayerReputation(player) + ", Health: " + (int)player.getHealth(), commands);
                            
                            executeCommands(player, deityId, commands);
                            cleanedResponse += "\n\n§6[Divine intervention enacted]";
                        }
                    }
                }
                
                // Send deity response to player using prominent title/subtitle display
                sendDeityResponse(player, deity.getName(), cleanedResponse);
                
                // Award reputation for meaningful conversations using Eidolon's reputation system
                player.getCapability(elucent.eidolon.capability.IReputation.INSTANCE).ifPresent(reputation -> {
                    double currentRep = reputation.getReputation(player.getUUID(), deity.getId());
                    // Calculate conversation reputation gain (diminishing returns)
                    double baseGain = 2.0;
                    if (currentRep > 75) {
                        baseGain *= 0.3; // Much slower gain at high reputation
                    } else if (currentRep > 50) {
                        baseGain *= 0.5; // Slower gain at medium reputation  
                    } else if (currentRep > 25) {
                        baseGain *= 0.7; // Slightly slower gain
                    }
                    
                    reputation.addReputation(player.getUUID(), deity.getId(), baseGain);
                    
                    // Trigger immediate title update for reputation change
                    com.bluelotuscoding.eidolonunchained.events.ReputationChangeHandler.forceUpdatePlayer(player);
                    
                    // Notify player of reputation gain
                    player.sendSystemMessage(Component.translatable("eidolonunchained.ui.deity.acknowledges_devotion", 
                        deity.getDisplayName(), String.format("%.1f", baseGain)));
                });
                
            }).exceptionally(throwable -> {
                LOGGER.error("🔥 DEBUG: Exception in AI response processing: {}", throwable.getMessage(), throwable);
                LOGGER.error("Error generating AI response: {}", throwable.getMessage(), throwable);
                
                // Enhanced error handling for specific issues
                String errorMessage = throwable.getMessage();
                if (errorMessage != null) {
                    if (errorMessage.toLowerCase().contains("quota") || errorMessage.toLowerCase().contains("limit")) {
                        player.sendSystemMessage(Component.translatable("eidolonunchained.error.quota_exceeded", deity.getName()));
                        player.sendSystemMessage(Component.translatable("eidolonunchained.error.quota_exceeded_hint"));
                        sendDeityResponse(player, deity.getName(), Component.translatable("eidolonunchained.ui.deity.energy_conserve").getString());
                    } else if (errorMessage.toLowerCase().contains("token")) {
                        player.sendSystemMessage(Component.translatable("eidolonunchained.error.response_too_long", deity.getName()));
                        player.sendSystemMessage(Component.translatable("eidolonunchained.error.response_too_long_hint"));
                        sendDeityResponse(player, deity.getName(), Component.translatable("eidolonunchained.ui.deity.words_exceed").getString());
                    } else if (errorMessage.toLowerCase().contains("timeout")) {
                        player.sendSystemMessage(Component.translatable("eidolonunchained.error.api_timeout", deity.getName()));
                        player.sendSystemMessage(Component.translatable("eidolonunchained.error.api_timeout_hint"));
                        sendDeityResponse(player, deity.getName(), Component.translatable("eidolonunchained.ui.deity.give_moment").getString());
                    } else {
                        sendDeityResponse(player, "Divine Connection", Component.translatable("eidolonunchained.ui.deity.connection_falters").getString());
                    }
                } else {
                    sendDeityResponse(player, "Divine Connection", Component.translatable("eidolonunchained.ui.deity.connection_falters").getString());
                }
                return null;
            });
            
        } catch (Exception e) {
            LOGGER.error("Error processing deity conversation for player {} with deity {}: {}", 
                player.getName().getString(), deityId, e.getMessage(), e);
            player.sendSystemMessage(Component.translatable("eidolonunchained.chat.connection_falters"));
            endConversation(player);
        }
    }
    
    /**
     * Build a comprehensive prompt for conversation context including proactive assistance
     */
    private static String buildConversationPrompt(ServerPlayer player, DatapackDeity deity, String currentMessage, ResourceLocation deityId) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are ").append(deity.getName()).append(", a deity in the world of Minecraft. ");
        prompt.append("You are having a conversation with ").append(player.getName().getString()).append(". ");
        
        // 🎯 USE AI DEITY CONFIGURATION FOR PROGRESSION CONTEXT
        // Get AI config to access reputation-based behavior rules
        AIDeityConfig aiConfig = AIDeityManager.getInstance().getAIConfig(deityId);
        double reputation = deity.getPlayerReputation(player);
        String progressionLevel = getDynamicProgressionLevel(deity, player);
        
        // 🔥 ENHANCED DEITY CONTEXT FROM AI_DEITIES FOLDER
        if (aiConfig != null) {
            // Add comprehensive AI deity identity context
            prompt.append("\n\n=== DEITY IDENTITY & CONFIGURATION ===\n");
            prompt.append("Deity ID: ").append(aiConfig.deity_id).append("\n");
            prompt.append("AI Provider: ").append(aiConfig.ai_provider).append("\n");
            prompt.append("Base Personality: ").append(aiConfig.personality).append("\n");
            
            // Add patron configuration details
            if (aiConfig.patron_config != null) {
                prompt.append("Accepts Followers: ").append(aiConfig.patron_config.acceptsFollowers).append("\n");
                prompt.append("Patron Status Required: ").append(aiConfig.patron_config.requiresPatronStatus).append("\n");
                if (!aiConfig.patron_config.opposingDeities.isEmpty()) {
                    prompt.append("Opposing Deities: ").append(String.join(", ", aiConfig.patron_config.opposingDeities)).append("\n");
                }
                if (!aiConfig.patron_config.alliedDeities.isEmpty()) {
                    prompt.append("Allied Deities: ").append(String.join(", ", aiConfig.patron_config.alliedDeities)).append("\n");
                }
            }
            
            // Add reputation-based behavior context
            String reputationBehavior = aiConfig.getReputationBehavior(reputation);
            if (reputationBehavior != null) {
                prompt.append("Current Behavioral Context: ").append(reputationBehavior).append("\n");
            }
            
            // Add progression stage context from AI config
            if (aiConfig.patron_config.followerPersonalityModifiers.containsKey(progressionLevel)) {
                prompt.append("Progression Modifier: ").append(aiConfig.patron_config.followerPersonalityModifiers.get(progressionLevel)).append("\n");
            }
            
            // Add mod context awareness
            if (!aiConfig.mod_context_ids.isEmpty()) {
                prompt.append("Aware of Mods: ").append(String.join(", ", aiConfig.mod_context_ids)).append("\n");
            }
        }
        
        prompt.append("This player currently holds the rank of '").append(progressionLevel)
              .append("' with you (reputation: ").append((int)reputation).append("). ");
        
        // 🎯 ENHANCED PATRON & TITLE TRACKING
        try {
            player.level().getCapability(com.bluelotuscoding.eidolonunchained.capability.CapabilityHandler.PATRON_DATA_CAPABILITY)
                .ifPresent(patronData -> {
                    ResourceLocation playerPatron = patronData.getPatron(player);
                    String title = patronData.getTitle(player);
                    
                    prompt.append("\n\n=== PLAYER PATRON STATUS ===\n");
                    if (playerPatron != null) {
                        prompt.append("Current Patron: ").append(playerPatron.toString()).append("\n");
                        if (playerPatron.equals(deityId)) {
                            prompt.append("This player is YOUR devoted follower!\n");
                        } else {
                            prompt.append("This player follows another deity. Act accordingly.\n");
                        }
                    } else {
                        prompt.append("This player has no patron deity (godless).\n");
                    }
                    
                    if (title != null && !title.isEmpty()) {
                        prompt.append("Player Title: ").append(title).append("\n");
                    } else {
                        prompt.append("Player has no special title.\n");
                    }
                });
        } catch (Exception e) {
            LOGGER.warn("Failed to get patron capability: {}", e.getMessage());
            prompt.append("\n\n=== PLAYER PATRON STATUS ===\n");
            prompt.append("Patron data unavailable\n");
        }
        
        // Check if the rank has recently changed (within conversation history)
        String fullHistory = ConversationHistoryManager.getPlayerFullContext(player, deityId);
        if (fullHistory.contains("Title updated") || fullHistory.contains("rank.*changed")) {
            prompt.append("IMPORTANT: This player's rank or title has recently changed - acknowledge their progression! ");
        }
        
        // Add achievement context instruction
        prompt.append("Use player achievements as BACKGROUND CONTEXT only - don't mention them unless directly relevant. ");
        prompt.append("Focus on the present conversation, not past accomplishments. ");
        
        // 🌍 ADD COMPREHENSIVE WORLD REGISTRY INFORMATION
        prompt.append("\n\n=== MINECRAFT WORLD KNOWLEDGE ===\n");
        prompt.append(buildMinecraftRegistryContext(player));
        
        // 🎯 ADD REAL-TIME WORLD STATE INFORMATION
        prompt.append("\n\n=== CURRENT WORLD STATE ===\n");
        try {
            String worldContext = com.bluelotuscoding.eidolonunchained.ai.WorldContextProvider
                .generatePlayerWorldContext(player);
            prompt.append(worldContext);
        } catch (Exception e) {
            LOGGER.warn("Failed to build world context: {}", e.getMessage());
            // Fallback to basic position info
            prompt.append("Player is at coordinates: ")
                .append(player.getX()).append(", ")
                .append(player.getY()).append(", ")
                .append(player.getZ()).append("\n");
        }
        
        // 🔮 ADD INTER-DEITY RELATIONSHIP CONTEXT (New!)
        try {
            String interDeityContext = com.bluelotuscoding.eidolonunchained.ai.InterDeityRelationshipManager
                .generateInterDeityContext(deityId, player);
            prompt.append(interDeityContext);
            
            String deityOpinions = com.bluelotuscoding.eidolonunchained.ai.InterDeityRelationshipManager
                .generateDetailedDeityOpinions(deityId);
            prompt.append(deityOpinions);
        } catch (Exception e) {
            LOGGER.warn("Failed to build inter-deity context: {}", e.getMessage());
        }
        
        // Add detailed player context using Universal AI Context Builder (for ALL providers)
        try {
            String playerContext = com.bluelotuscoding.eidolonunchained.ai.UniversalAIContextBuilder
                .buildCompleteContext(player, aiConfig, null);
            prompt.append("\n\nPlayer Current State & World Knowledge:\n").append(playerContext);
        } catch (Exception e) {
            LOGGER.warn("Failed to build universal context: {}", e.getMessage());
            // Fallback to basic context
            prompt.append("\nPlayer: ").append(player.getName().getString());
            prompt.append("\nHealth: ").append(player.getHealth()).append("/").append(player.getMaxHealth());
        }
        
        // Add AI deity configuration-based command guidelines (SUBTLE)
        if (aiConfig != null && aiConfig.prayer_configs.containsKey("conversation")) {
            PrayerAIConfig prayerConfig = aiConfig.prayer_configs.get("conversation");
            // SUBTLE: Don't explicitly list commands, just provide context
            prompt.append("\n\n=== CONVERSATION CONTEXT ===\n");
            prompt.append("Focus on meaningful conversation. Use divine powers sparingly and only when truly needed.\n");
            prompt.append("Your powers should feel natural and contextual, not excessive or forced.\n");
            // Let the JSON config control the actual limits, don't override
            if (prayerConfig.max_commands > 0) {
                prompt.append("You may use up to ").append(prayerConfig.max_commands).append(" divine actions if needed.\n");
            }
        }
        
        // Add FULL conversation history for complete context
        String fullConversationContext = ConversationHistoryManager.getPlayerFullContext(player, deityId);
        if (!fullConversationContext.isEmpty()) {
            prompt.append("\n\n").append(fullConversationContext);
            prompt.append("\nIMPORTANT: Review the complete conversation history above before responding. ");
            prompt.append("This gives you full context of your relationship with this player.\n");
        } else {
            prompt.append("\n\nThis is your first conversation with this player.\n");
        }
        
        // Add current player message with emphasis
        prompt.append("\n\nPlayer's Current Message: \"").append(currentMessage).append("\"\n");
        
        // Enhanced proactive assistance with configuration-driven guidance
        prompt.append("\n\n=== RESPONSE INSTRUCTIONS ===\n");
        prompt.append("1. ADAPTIVE PERSONALITY: Use your unique character traits and reputation-based behavior\n");
        prompt.append("2. WORLD AWARENESS: You know all Minecraft items, blocks, biomes, and dimensions listed above\n");
        prompt.append("3. IMMERSIVE CONVERSATION: Speak naturally as your deity character would\n");
        prompt.append("4. NO ACTION TAGS: NEVER use [ACTION:...] or similar tags - speak naturally instead\n");
        prompt.append("5. NO MOD IDS: Never say technical names like 'eidolon:light_blessing' - use natural language like 'divine light'\n");
        prompt.append("6. TIER AWARENESS: Your follower is currently ").append(getDynamicProgressionLevel(deity, player)).append(" level - respond appropriately\n");
        prompt.append("7. CONVERSATIONAL PRIORITY: Focus on conversation over item-giving unless specifically requested\n");
        prompt.append("8. AVOID REPETITION: Each response should be unique and situational\n");
        prompt.append("9. RESPOND TO PLAYER: Address what the PLAYER actually said, not what you want to give\n");
        prompt.append("10. DYNAMIC JUDGMENT: Consider player's immediate context for appropriate responses\n");
        prompt.append("\nCRITICAL BLESSING GUIDELINES:\n");
        prompt.append("- ONLY give items if player explicitly asks (uses words like 'give', 'bless', 'help', 'need')\n");
        prompt.append("- LOW TIER players should mostly receive conversation, not constant gifts\n");
        prompt.append("- When giving items, speak naturally: 'Take this blade' (NOT technical mod IDs)\n");
        prompt.append("- Focus on being a conversational deity, not a vending machine\n");
        prompt.append("- Your words should match your character - mysterious, divine, personality-driven\n");
        prompt.append("Remember: Quality conversation over quantity of gifts! Be adaptive, not scripted!\n");
        
        return prompt.toString();
    }
    
    /**
     * Build comprehensive Minecraft registry context for AI knowledge
     */
    private static String buildMinecraftRegistryContext(ServerPlayer player) {
        StringBuilder context = new StringBuilder();
        
        // Current world context
        context.append("Current World: ").append(player.level().dimension().location()).append("\n");
        context.append("Current Biome: ").append(player.level().getBiome(player.blockPosition()).unwrapKey()
            .map(key -> key.location().toString()).orElse("unknown")).append("\n");
        context.append("Time of Day: ").append(getTimeOfDay(player.level().getDayTime())).append("\n");
        context.append("Weather: ").append(player.level().isRaining() ? "Raining" : "Clear").append("\n");
        
        // Key Minecraft item categories for AI reference
        context.append("\nKEY ITEM CATEGORIES:\n");
        context.append("- Basic Items: minecraft:iron_ingot, minecraft:gold_ingot, minecraft:diamond, minecraft:emerald\n");
        context.append("- Food: minecraft:bread, minecraft:cooked_beef, minecraft:golden_apple, minecraft:enchanted_golden_apple\n");
        context.append("- Tools: minecraft:iron_sword, minecraft:diamond_pickaxe, minecraft:bow, minecraft:crossbow\n");
        context.append("- Blocks: minecraft:stone, minecraft:oak_log, minecraft:iron_ore, minecraft:diamond_ore\n");
        context.append("- Potions: minecraft:potion, minecraft:healing_potion, minecraft:strength_potion\n");
        
        // Eidolon-specific items if mod is loaded
        context.append("\nEIDOLON ITEMS (if available):\n");
        context.append("- eidolon:soul_shard, eidolon:death_essence, eidolon:shadow_gem\n");
        context.append("- eidolon:arcane_gold_ingot, eidolon:pewter_ingot, eidolon:silver_ingot\n");
        context.append("- eidolon:wicked_weave, eidolon:wraith_heart, eidolon:tattered_cloth\n");
        context.append("- eidolon:research_notes, eidolon:codex, eidolon:holy_symbol\n");
        
        // Available dimensions
        context.append("\nDIMENSIONS:\n");
        context.append("- minecraft:overworld, minecraft:the_nether, minecraft:the_end\n");
        
        // Common biomes for context-aware responses
        context.append("\nCOMMON BIOMES:\n");
        context.append("- minecraft:forest, minecraft:desert, minecraft:plains, minecraft:ocean\n");
        context.append("- minecraft:deep_dark, minecraft:warped_forest, minecraft:soul_sand_valley\n");
        context.append("- minecraft:end_highlands, minecraft:crimson_forest, minecraft:basalt_deltas\n");
        
        // Effects available for blessings/curses
        context.append("\nAVAILABLE EFFECTS:\n");
        context.append("- Blessings: minecraft:strength, minecraft:speed, minecraft:regeneration, minecraft:resistance\n");
        context.append("- Utility: minecraft:night_vision, minecraft:water_breathing, minecraft:fire_resistance\n");
        context.append("- Curses: minecraft:weakness, minecraft:slowness, minecraft:poison, minecraft:wither\n");
        
        return context.toString();
    }
    
    /**
     * Get readable time of day
     */
    private static String getTimeOfDay(long worldTime) {
        long dayTime = worldTime % 24000;
        if (dayTime >= 0 && dayTime < 6000) return "Morning";
        if (dayTime >= 6000 && dayTime < 12000) return "Noon";
        if (dayTime >= 12000 && dayTime < 18000) return "Evening";
        return "Night";
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
        if (reputation >= prayerConfig.judgment_config.blessingThreshold) {
            commands.addAll(prayerConfig.judgment_config.blessingCommands);
        } else if (reputation <= prayerConfig.judgment_config.curseThreshold) {
            commands.addAll(prayerConfig.judgment_config.curseCommands);
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
            } else if (reputation >= prayerConfig.judgment_config.blessingThreshold / 2) {
                // Neutral commands for moderate reputation
                commands.addAll(prayerConfig.judgment_config.neutralCommands);
            }
        }
        
        return commands;
    }
    
    /**
     * Execute a list of commands with enhanced feedback and debugging
     */
    private static void executeCommands(ServerPlayer player, ResourceLocation deityId, List<String> commands) {
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
                
                // Enhanced logging with debugging
                LOGGER.info("Successfully executed deity command for {}: {}", player.getName().getString(), processedCommand);
                ConversationHistoryManager.logCommandExecutionStatic(player, deityId, processedCommand, true, "Command executed successfully");
                
            } catch (Exception e) {
                // Enhanced error logging with debugging
                String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                LOGGER.error("Failed to execute deity command '{}' for player {}: {}", command, player.getName().getString(), errorMsg);
                ConversationHistoryManager.logCommandExecutionStatic(player, deityId, command, false, errorMsg);
            }
        }
        
        // Send feedback to player about divine intervention
        if (successCount > 0) {
            player.sendSystemMessage(Component.literal("§6✦ Divine power flows through you... §7(" + successCount + " blessing" + (successCount == 1 ? "" : "s") + " granted)"));
        }
    }
    
    /**
     * Clear conversation history for a player and deity
     */
    public static void clearConversationHistory(ServerPlayer player, ResourceLocation deityId) {
        ConversationHistoryManager.get().clearHistory(player.getUUID(), deityId, player);
        
        DatapackDeity deity = DatapackDeityManager.getDeity(deityId);
        String deityName = deity != null ? deity.getName() : deityId.toString();
        
        player.sendSystemMessage(Component.literal("§6Conversation history with " + deityName + " has been cleared."));
        LOGGER.info("Cleared conversation history for player {} with deity {}", player.getName().getString(), deityId);
    }
    
    /**
     * Clear all conversation history for a player
     */
    public static void clearAllConversationHistory(ServerPlayer player) {
        ConversationHistoryManager.get().clearAllHistory(player.getUUID(), player);
        
        player.sendSystemMessage(Component.literal("§6All conversation history has been cleared."));
        LOGGER.info("Cleared all conversation history for player {}", player.getName().getString());
    }
    
    /**
     * Get conversation history statistics for a player
     */
    public static void showConversationStats(ServerPlayer player) {
        // TODO: Implement stats functionality
        player.sendSystemMessage(Component.literal("§6Conversation stats functionality coming soon!"));
        /*
        Map<String, Object> stats = ConversationHistoryManager.getHistoryStats(player);
        
        int totalConversations = (Integer) stats.get("total_conversations");
        int totalMessages = (Integer) stats.get("total_messages");
        @SuppressWarnings("unchecked")
        Map<String, Integer> deityMessageCounts = (Map<String, Integer>) stats.get("deity_message_counts");
        
        player.sendSystemMessage(Component.literal("§6=== Conversation History Statistics ==="));
        player.sendSystemMessage(Component.literal("§eTotalConversations: §f" + totalConversations));
        player.sendSystemMessage(Component.literal("§eTotal Messages: §f" + totalMessages));
        
        if (!deityMessageCounts.isEmpty()) {
            player.sendSystemMessage(Component.literal("§eMessages per Deity:"));
            for (Map.Entry<String, Integer> entry : deityMessageCounts.entrySet()) {
                player.sendSystemMessage(Component.literal("  §7" + entry.getKey() + ": §f" + entry.getValue() + " messages"));
            }
        }
        */
    }
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
        
        // 🔥 FIX: Only auto-select if explicitly set to AUTO
        if ("AUTO".equals(displayMethod)) {
            if (message.length() > maxSubtitleLength) {
                displayMethod = "ENHANCED_CHAT";
            } else {
                displayMethod = "TITLE_SUBTITLE"; // Default to title/subtitle, not action bar
            }
        }
        
        // 🔥 FIX: Route to appropriate display method - Enhanced Action Bar by default
        switch (displayMethod) {
            case "TITLE_SUBTITLE":
                // Use enhanced action bar instead of problematic title system
                sendPureActionBarDisplay(player, deityName, message);
                break;
            case "ACTION_BAR":
                sendPureActionBarDisplay(player, deityName, message); // Pure action bar only
                break;
            case "ENHANCED_CHAT":
                sendEnhancedChatMessage(player, deityName, message);
                break;
            default:
                // Fallback to enhanced action bar for best experience
                sendPureActionBarDisplay(player, deityName, message);
        }
    }
    
    /**
     * 🔥 FULLY CONFIGURABLE ACTION BAR DISPLAY with typing animation
     */
    private static void sendPureActionBarDisplay(ServerPlayer player, String deityName, String message) {
        // Get all action bar configuration
        boolean enableTyping = EidolonUnchainedConfig.COMMON.enableActionBarTyping.get();
        int typingSpeed = EidolonUnchainedConfig.COMMON.actionBarTypingSpeed.get();
        int sentenceDelay = EidolonUnchainedConfig.COMMON.actionBarSentenceDelay.get();
        int fadeDelay = EidolonUnchainedConfig.COMMON.actionBarFadeDelayTicks.get();
        int maxWidth = EidolonUnchainedConfig.COMMON.actionBarMaxWidth.get();
        boolean centerText = EidolonUnchainedConfig.COMMON.actionBarCenterText.get();
        boolean wrapText = EidolonUnchainedConfig.COMMON.actionBarWrapText.get();
        
        if (enableTyping) {
            // Use animated typing
            startActionBarTypingAnimation(player, deityName, message, typingSpeed, sentenceDelay, fadeDelay, maxWidth, centerText, wrapText);
        } else {
            // Show instant message
            String formattedMessage = formatActionBarMessage(deityName, message, maxWidth, centerText, wrapText);
            Component actionBarComponent = Component.literal(formattedMessage);
            player.sendSystemMessage(actionBarComponent, true);
            
            LOGGER.debug("Sent instant action bar message to {}: {}", player.getName().getString(), formattedMessage);
        }
    }
    
    /**
     * 🔥 ENHANCED ACTION BAR MESSAGES - Smart wrapping with sequential display
     * Breaks long messages into action bar-sized chunks and displays them sequentially
     */
    private static void startActionBarTypingAnimation(ServerPlayer player, String deityName, String message, 
                                                    int typingSpeed, int sentenceDelay, int fadeDelay, 
                                                    int maxWidth, boolean centerText, boolean wrapText) {
        
        // Split message into action bar-friendly chunks
        List<String> messageChunks = intelligentTextWrap(message, deityName, maxWidth);
        
        if (messageChunks.isEmpty()) return;
        
        // Display each chunk sequentially with typing animation
        startSequentialActionBarDisplay(player, deityName, messageChunks, typingSpeed, sentenceDelay, fadeDelay, maxWidth, centerText);
    }
    
    /**
     * Display message chunks sequentially in action bar with typing animation
     */
    private static void startSequentialActionBarDisplay(ServerPlayer player, String deityName, List<String> messageChunks,
                                                       int typingSpeed, int sentenceDelay, int fadeDelay, 
                                                       int maxWidth, boolean centerText) {
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                for (int chunkIndex = 0; chunkIndex < messageChunks.size(); chunkIndex++) {
                    String chunk = messageChunks.get(chunkIndex);
                    
                    // Type out this chunk character by character
                    for (int charIndex = 0; charIndex <= chunk.length(); charIndex++) {
                        String partialChunk = chunk.substring(0, charIndex);
                        String formattedMessage = formatActionBarMessage(deityName, partialChunk, maxWidth, centerText, false);
                        Component actionBarComponent = Component.literal(formattedMessage);
                        
                        // Send to action bar on main thread
                        player.getServer().execute(() -> {
                            player.sendSystemMessage(actionBarComponent, true);
                        });
                        
                        // Wait for typing speed
                        Thread.sleep(typingSpeed);
                    }
                    
                    // Pause between chunks (except for last chunk)
                    if (chunkIndex < messageChunks.size() - 1) {
                        Thread.sleep(sentenceDelay);
                    }
                }
                
                // Final message stays for fade delay
                Thread.sleep(fadeDelay * 50); // Convert ticks to milliseconds
                
                // Clear action bar
                player.getServer().execute(() -> {
                    player.sendSystemMessage(Component.literal(""), true);
                });
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.warn("Sequential action bar display interrupted for player {}", player.getName().getString());
            }
        });
    }

    /**
     * Intelligently wrap text into action bar-friendly chunks
     * Splits on sentence boundaries first, then word boundaries if needed
     */
    private static List<String> intelligentTextWrap(String message, String deityName, int maxWidth) {
        List<String> chunks = new ArrayList<>();
        
        // Calculate usable width (subtract deity header space)
        String header = "⟦ " + deityName + " ⟧ ";
        int headerLength = header.length();
        int usableWidth = maxWidth - headerLength;
        
        // First, split on sentence boundaries
        String[] sentences = message.split("(?<=[.!?])\\s+");
        
        for (String sentence : sentences) {
            sentence = sentence.trim();
            if (sentence.isEmpty()) continue;
            
            // If sentence fits in one action bar, add it as is
            if (sentence.length() <= usableWidth) {
                chunks.add(sentence);
            } else {
                // Break long sentence into word-wrapped chunks
                String[] words = sentence.split("\\s+");
                StringBuilder currentChunk = new StringBuilder();
                
                for (String word : words) {
                    // Check if adding this word would exceed the width
                    String testChunk = currentChunk.length() == 0 ? word : currentChunk + " " + word;
                    
                    if (testChunk.length() <= usableWidth) {
                        // Add word to current chunk
                        if (currentChunk.length() > 0) currentChunk.append(" ");
                        currentChunk.append(word);
                    } else {
                        // Start new chunk with this word
                        if (currentChunk.length() > 0) {
                            chunks.add(currentChunk.toString());
                            currentChunk = new StringBuilder();
                        }
                        
                        // Handle very long single words
                        if (word.length() > usableWidth) {
                            // Split the word itself
                            for (int i = 0; i < word.length(); i += usableWidth) {
                                chunks.add(word.substring(i, Math.min(i + usableWidth, word.length())));
                            }
                        } else {
                            currentChunk.append(word);
                        }
                    }
                }
                
                // Add remaining chunk
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString());
                }
            }
        }
        
        return chunks;
    }
    

    
    /**
     * 🔥 FORMAT ACTION BAR MESSAGE with proper centering - NO TRUNCATION
     */
    private static String formatActionBarMessage(String deityName, String message, int maxWidth, boolean centerText, boolean wrapText) {
        // Create header with deity name
        String header = "§6⟦ " + deityName + " ⟧";
        
        // Combine header and message - NO TRUNCATION, show full message
        String fullText = header + " §f" + message;
        
        // Center text if enabled and reasonable length
        if (centerText && fullText.replaceAll("§.", "").length() <= maxWidth * 1.2) { // Allow slight overflow for centering
            int textLength = fullText.replaceAll("§.", "").length();
            int padding = Math.max(0, (maxWidth - textLength) / 2);
            String paddingSpaces = " ".repeat(padding);
            return paddingSpaces + fullText;
        } else {
            return fullText;
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
    
    /**
     * 🎯 DYNAMIC PROGRESSION LEVEL HELPER - USES AI DEITY CONFIGURATIONS
     * 
     * Gets the player's current progression level based on AI deity configurations.
     * This ensures the AI recognizes the player's actual rank from the JSON configs.
     */
    private static String getDynamicProgressionLevel(DatapackDeity deity, ServerPlayer player) {
        double reputation = deity.getPlayerReputation(player);
        
        try {
            // 🔥 PRIORITY: Use deity's progression stages from JSON first (most accurate)
            Map<String, Object> stagesMap = deity.getProgressionStages();
            if (!stagesMap.isEmpty()) {
                String bestStage = "Initiate";
                double highestQualifyingReputation = -1;
                
                for (Map.Entry<String, Object> stageEntry : stagesMap.entrySet()) {
                    String stageName = stageEntry.getKey();
                    Object stageData = stageEntry.getValue();
                    
                    if (!(stageData instanceof Map)) continue;
                    @SuppressWarnings("unchecked")
                    Map<String, Object> stageDataMap = (Map<String, Object>) stageData;
                    
                    Object repReqObj = stageDataMap.get("reputationRequired");
                    if (!(repReqObj instanceof Number)) continue;
                    
                    double requiredReputation = ((Number) repReqObj).doubleValue();
                    
                    if (reputation >= requiredReputation && requiredReputation > highestQualifyingReputation) {
                        // Get the actual title from the stage data
                        Object titleObj = stageDataMap.get("title");
                        if (titleObj instanceof String) {
                            bestStage = (String) titleObj;
                            highestQualifyingReputation = requiredReputation;
                        }
                    }
                }
                
                LOGGER.debug("🎭 JSON progression for {}/{}: {} ({}rep)", 
                    player.getName().getString(), deity.getName(), bestStage, (int)reputation);
                
                return bestStage;
            }
            
            // Secondary: Use AI deity config reputation thresholds if available
            AIDeityConfig aiConfig = AIDeityManager.getInstance().getAIConfig(deity.getId());
            if (aiConfig != null && !aiConfig.getReputationBehaviors().isEmpty()) {
                // Find the highest threshold the player qualifies for
                String progressionTitle = "Initiate";
                int highestThreshold = -1;
                
                // Check AI config reputation thresholds and extract proper titles
                for (Map.Entry<Integer, String> entry : aiConfig.getReputationBehaviors().entrySet()) {
                    if (reputation >= entry.getKey() && entry.getKey() > highestThreshold) {
                        highestThreshold = entry.getKey();
                        
                        // Use deity-specific progression titles based on thresholds
                        if (entry.getKey() >= 100) progressionTitle = getDeitySpecificTitle(aiConfig, "champion");
                        else if (entry.getKey() >= 75) progressionTitle = getDeitySpecificTitle(aiConfig, "master");
                        else if (entry.getKey() >= 50) progressionTitle = getDeitySpecificTitle(aiConfig, "priest");
                        else if (entry.getKey() >= 25) progressionTitle = getDeitySpecificTitle(aiConfig, "acolyte");
                        else if (entry.getKey() >= 0) progressionTitle = getDeitySpecificTitle(aiConfig, "initiate");
                    }
                }
                
                LOGGER.debug("🤖 AI Config progression for {}/{}: {} ({}rep, threshold={})", 
                    player.getName().getString(), deity.getName(), progressionTitle, (int)reputation, highestThreshold);
                
                return progressionTitle;
            }
            
            // Final fallback to hardcoded levels
            LOGGER.debug("🔍 No AI config or JSON stages for deity {}, using fallback", deity.getId());
            if (reputation >= 75) return "Master";
            if (reputation >= 50) return "Priest"; 
            if (reputation >= 25) return "Acolyte";
            if (reputation >= 10) return "Initiate";
            return "Initiate";
            
        } catch (Exception e) {
            LOGGER.error("🚨 Error determining progression level for {}/{}, using fallback: {}", 
                player.getName().getString(), deity.getName(), e.getMessage());
            
            // Emergency fallback
            if (reputation >= 75) return "master";
            if (reputation >= 50) return "advanced";
            if (reputation >= 25) return "intermediate";
            if (reputation >= 10) return "novice";
            return "beginner";
        }
    }
    
    /**
     * Clean mod ID leakage from AI responses
     * Converts technical IDs like "eidolon:light_blessing" to natural language
     */
    private static String cleanModIdLeakage(String response) {
        if (response == null) return "";
        
        String cleaned = response;
        
        // Remove or replace common mod ID patterns with natural language
        cleaned = cleaned.replaceAll("\\bminecraft:", "");
        cleaned = cleaned.replaceAll("\\beidolon:([\\w_]+)", "ancient $1");
        cleaned = cleaned.replaceAll("\\beidolonunchained:([\\w_]+)", "divine $1");
        
        // Fix specific common cases
        cleaned = cleaned.replaceAll("\\blight_blessing\\b", "divine light");
        cleaned = cleaned.replaceAll("\\bshadow_gem\\b", "dark crystal");
        cleaned = cleaned.replaceAll("\\bsoul_shard\\b", "soul fragment");
        cleaned = cleaned.replaceAll("\\biron_sword\\b", "iron blade");
        cleaned = cleaned.replaceAll("\\bgolden_apple\\b", "golden fruit");
        
        // Remove underscores in remaining technical terms
        cleaned = cleaned.replaceAll("\\b([a-z]+)_([a-z]+)\\b", "$1 $2");
        
        return cleaned;
    }
    
    /**
     * Get deity-specific title for a tier level
     */
    private static String getDeitySpecificTitle(AIDeityConfig aiConfig, String tierLevel) {
        // Try to extract the actual title from followerPersonalityModifiers
        if (aiConfig.patron_config != null && aiConfig.patron_config.followerPersonalityModifiers != null) {
            // Look for titles matching the tier level pattern
            for (String title : aiConfig.patron_config.followerPersonalityModifiers.keySet()) {
                String lowerTitle = title.toLowerCase();
                if (lowerTitle.contains(tierLevel.toLowerCase()) || 
                    (tierLevel.equals("initiate") && (lowerTitle.contains("initiate") || lowerTitle.contains("newcomer"))) ||
                    (tierLevel.equals("acolyte") && lowerTitle.contains("acolyte")) ||
                    (tierLevel.equals("priest") && lowerTitle.contains("priest")) ||
                    (tierLevel.equals("master") && lowerTitle.contains("master")) ||
                    (tierLevel.equals("champion") && lowerTitle.contains("champion"))) {
                    return title;
                }
            }
        }
        
        // Fallback to generic titles
        switch (tierLevel.toLowerCase()) {
            case "champion": return "Champion";
            case "master": return "Master";
            case "priest": return "Priest";
            case "acolyte": return "Acolyte";
            case "initiate": 
            default: return "Initiate";
        }
    }
    
    /**
     * Enforce tier-based blessing limits
     * Prevents over-giving items based on player progression
     */
    private static boolean shouldAllowBlessing(ServerPlayer player, DatapackDeity deity, String playerMessage) {
        try {
            double reputation = deity.getPlayerReputation(player);
            String progressionLevel = getDynamicProgressionLevel(deity, player);
            
            // Check if player explicitly requested something
            String lowerMessage = playerMessage.toLowerCase();
            boolean explicitRequest = lowerMessage.contains("give") || lowerMessage.contains("bless") || 
                                    lowerMessage.contains("grant") || lowerMessage.contains("help") ||
                                    lowerMessage.contains("need") || lowerMessage.contains("want");
            
            // No blessings for non-explicit requests at low levels
            if (!explicitRequest && reputation < 25) {
                LOGGER.info("🚫 Blocked non-explicit blessing request for low-tier player {}: '{}'", 
                    player.getName().getString(), playerMessage);
                return false;
            }
            
            // Check recent blessing cooldown (simple time-based)
            long currentTime = System.currentTimeMillis();
            String cooldownKey = player.getUUID() + "_" + deity.getId();
            Long lastBlessing = lastBlessingTimes.get(cooldownKey);
            
            if (lastBlessing != null) {
                long timeSince = currentTime - lastBlessing;
                long cooldownMs = getBlessingCooldown(progressionLevel);
                
                if (timeSince < cooldownMs) {
                    LOGGER.info("🕒 Blessing on cooldown for {}: {}ms remaining", 
                        player.getName().getString(), cooldownMs - timeSince);
                    return false;
                }
            }
            
            // Update last blessing time
            lastBlessingTimes.put(cooldownKey, currentTime);
            
            return true;
            
        } catch (Exception e) {
            LOGGER.error("Error checking blessing allowance: {}", e.getMessage());
            return false; // Default to safe side
        }
    }
    
    /**
     * Get blessing cooldown based on progression level
     */
    private static long getBlessingCooldown(String progressionLevel) {
        switch (progressionLevel.toLowerCase()) {
            case "master": return 30000; // 30 seconds
            case "advanced": return 60000; // 1 minute  
            case "intermediate": return 120000; // 2 minutes
            case "novice": return 300000; // 5 minutes
            default: return 600000; // 10 minutes for beginners
        }
    }
    
    /**
     * Get maximum commands allowed per tier to prevent over-blessing
     */
    private static int getMaxCommandsForTier(String progressionLevel) {
        String lowerLevel = progressionLevel.toLowerCase();
        
        // Handle deity-specific tier names
        if (lowerLevel.contains("champion")) return 3; // Champions get 3 blessings
        if (lowerLevel.contains("master") || lowerLevel.contains("void")) return 3; // Masters/Void Masters get 3 blessings
        if (lowerLevel.contains("priest") || lowerLevel.contains("high")) return 2; // Priests get 2 blessings
        if (lowerLevel.contains("acolyte") || lowerLevel.contains("dark")) return 1; // Acolytes get 1 blessing
        if (lowerLevel.contains("initiate") || lowerLevel.contains("shadow")) return 1; // Initiates get 1 blessing
        
        // Fallback to generic tier names
        switch (lowerLevel) {
            case "master": return 3; // Masters can receive multiple blessings
            case "advanced": return 2; // Advanced gets 2 blessings
            case "intermediate": return 1; // Intermediate gets 1 blessing
            case "novice": return 1; // Novice gets 1 blessing
            default: return 1; // Default to 1 blessing maximum
        }
    }
    
    // Static map to track blessing cooldowns
    private static final java.util.Map<String, Long> lastBlessingTimes = new java.util.concurrent.ConcurrentHashMap<>();
}
