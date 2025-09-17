package com.bluelotuscoding.eidolonunchained.chat;

import com.bluelotuscoding.eidolonunchained.deity.DatapackDeity;
import com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager;
import com.bluelotuscoding.eidolonunchained.ai.AIDeityManager;
import com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig;
import com.bluelotuscoding.eidolonunchained.ai.TaskSystemConfig;
import com.bluelotuscoding.eidolonunchained.ai.PrayerAIConfig;
import com.bluelotuscoding.eidolonunchained.ai.PlayerContext;
import com.bluelotuscoding.eidolonunchained.integration.gemini.GeminiAPIClient;
import com.bluelotuscoding.eidolonunchained.config.APIKeyManager;
import com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig;
import com.bluelotuscoding.eidolonunchained.chant.PlayerChantingSystem;
import com.bluelotuscoding.eidolonunchained.util.CommandStringUtils;
import com.bluelotuscoding.eidolonunchained.effects.EffigyEffectsManager;
import elucent.eidolon.common.tile.EffigyTileEntity;
import net.minecraft.core.BlockPos;
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
    
    // Track commands executed per conversation session: player UUID -> command count
    private static final Map<UUID, Integer> conversationCommandCounts = new ConcurrentHashMap<>();
    
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
        conversationCommandCounts.put(playerId, 0); // Initialize command count for this session
        
        // Start effigy effects for the conversation
        // Start effigy effects for the conversation
        net.minecraft.core.BlockPos effigyPos = com.bluelotuscoding.eidolonunchained.effects.EffigyEffectsManager.findNearbyEffigyPos(player, 10.0);
        if (effigyPos != null) {
            com.bluelotuscoding.eidolonunchained.effects.EffigyEffectsManager.startConversationEffects(player, effigyPos, deityId);
            LOGGER.info("Started effigy effects for deity {} at {}", deityId, effigyPos);
        } else {
            LOGGER.warn("No effigy found within 10 blocks of player {} at {}", player.getName().getString(), player.blockPosition());
        }
        
        String deityName = deity.getName();
        
        // Send patron-aware initial message
        sendPatronAwareGreeting(player, deity, aiConfig);
        
        // Effigy effects are now handled by EffigyEffectsManager
        
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
                            player.sendSystemMessage(Component.translatable("eidolonunchained.ui.deity.divine_presence")
                                .withStyle(net.minecraft.ChatFormatting.YELLOW));
                            if (title != null && !title.isEmpty()) {
                                player.sendSystemMessage(Component.translatable("eidolonunchained.ui.deity.recognizes_faithful", deity.getName(), title)
                                    .withStyle(net.minecraft.ChatFormatting.YELLOW));
                            } else {
                                player.sendSystemMessage(Component.translatable("eidolonunchained.ui.deity.recognizes_faithful", deity.getName(), "servant")
                                    .withStyle(net.minecraft.ChatFormatting.YELLOW));
                            }
                            break;
                        case ALLIED:
                            player.sendSystemMessage(Component.translatable("eidolonunchained.ui.deity.divine_ally_greeting", deity.getName()));
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
        conversationCommandCounts.remove(playerId); // Clear command count for this session
        
        if (deityId != null) {
            DatapackDeity deity = DatapackDeityManager.getDeity(deityId);
            String deityName = deity != null ? deity.getName() : deityId.toString();
            
            // Stop effigy effects when conversation ends
            EffigyEffectsManager.stopEffects(player);
            
            
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
     * Get the active conversation deity for a player (for command extraction)
     */
    public static ResourceLocation getActiveConversationDeity(ServerPlayer player) {
        return activeConversations.get(player.getUUID());
    }
    
    /**
     * Handle chat events for active conversations
     * SAFE VERSION: Only processes messages for players in active conversations
     */
    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        UUID playerId = player.getUUID();
        
        // 🎯 NO TICKING NEEDED: Player-centered system uses event-driven execution like ribbon
        // PlayerChantingSystem uses scheduled tasks for spell delays, not polling!
        
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
        processDeityConversation(player, deityId, message, null);
    }
    
    /**
     * Process a conversation message with a deity with optional completion callback
     */
    private static void processDeityConversation(ServerPlayer player, ResourceLocation deityId, String message, Runnable onComplete) {
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

            // QUICK NATURAL-LANGUAGE TRIGGER PASS (pre-AI): lets natural phrases cause actions
            // Honor config: skip JSON triggers unless mode is json_only or both
            String nlMode = com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig.COMMON.naturalLanguageTriggerMode.get();
            boolean allowJsonTriggers = "json_only".equalsIgnoreCase(nlMode) || "both".equalsIgnoreCase(nlMode);
            if (allowJsonTriggers && evaluateNaturalLanguageTriggers(player, deityId, message, aiConfig)) {
                if (onComplete != null) onComplete.run();
                return; // If a trigger consumed this message, stop here
            }

            // Offer/accept conversational control (accept/decline/explicit ask)
            if (handleFateOfferControl(player, deityId, message)) {
                if (onComplete != null) onComplete.run();
                return;
            }
            
            // Add message to conversation history
            List<String> history = conversationHistory.computeIfAbsent(playerId, k -> {
                LOGGER.warn("🚨 Conversation history was null for player {}, initializing emergency backup", player.getName().getString());
                return new ArrayList<>();
            });
            history.add("Player: " + message);
            
            // Add to persistent history on main thread to avoid SavedData classloader issues
            final UUID playerUuid = player.getUUID();
            final ResourceLocation targetDeityId = deityId;
            final String messageText = message;
            
            MinecraftServer server = player.getServer();
            if (server != null) {
                server.execute(() -> {
                    try {
                        ConversationHistoryManager.get().addMessage(playerUuid, targetDeityId, "Player", messageText);
                    } catch (Exception e) {
                        LOGGER.warn("Failed to save conversation message to persistent storage: {}", e.getMessage());
                    }
                });
            }
            
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
            
            // Build comprehensive context for AI provider with full game awareness
            PlayerContext fullContext = new PlayerContext(player, deity);
            String context = buildComprehensiveAIContext(fullContext, deityId, player);
            
            // 💡 SIMPLE SOLUTION: Check if player wants TTS-only mode (saves money!)
            com.bluelotuscoding.eidolonunchained.ai.TTSManager ttsManager = com.bluelotuscoding.eidolonunchained.ai.TTSManager.getInstance();
            boolean isTTSOnlyMode = ttsManager.getPlayerSettings(player).ttsOnly;
            
            if (isTTSOnlyMode) {
                // TTS-Only Mode: Skip LLM, send prompt directly to TTS
                handleTTSOnlyMode(player, deity, conversationPrompt, personality, context, deityId, playerId, history, onComplete);
            } else {
                // Hybrid Mode: LLM generates text + TTS speaks it (original behavior)
                handleHybridMode(provider, conversationPrompt, personality, context, aiConfig, player, deity, deityId, playerId, history, onComplete);
            }
            
        } catch (Exception e) {
            LOGGER.error("Error in deity conversation processing: {}", e.getMessage());
            player.sendSystemMessage(Component.translatable("eidolonunchained.chat.conversation_error"));
            endConversation(player);
        }
    }
    
    /**
     * Handle TTS-Only mode: Skip LLM, send prompt directly to TTS for voice generation
     */
    private static void handleTTSOnlyMode(ServerPlayer player, DatapackDeity deity, String conversationPrompt, 
                                          String personality, String context, ResourceLocation deityId, UUID playerId, 
                                          List<String> history, Runnable onComplete) {
        // Check if TTS is enabled for this player
        com.bluelotuscoding.eidolonunchained.ai.TTSManager ttsManager = com.bluelotuscoding.eidolonunchained.ai.TTSManager.getInstance();
        boolean isTTSEnabled = ttsManager.getPlayerSettings(player).enabled;
        
        if (!isTTSEnabled) {
            LOGGER.warn("TTS-Only mode requested but TTS is disabled for player {}", player.getName().getString());
            player.sendSystemMessage(Component.literal("§c[TTS-Only Mode] TTS must be enabled to use this mode. Use /eidolon-unchained tts enable"));
            endConversation(player);
            return;
        }
        
        // Create a contextual prompt for direct TTS generation
        String ttsPrompt = buildTTSDirectPrompt(conversationPrompt, personality, context, deity.getName());
        
        // Send directly to TTS - no LLM call
        LOGGER.info("🎙️ TTS-Only Mode: Sending prompt directly to TTS for player {}", player.getName().getString());
        ttsManager.generateAndSendTTS(player, ttsPrompt, deityId.getPath())
            .thenAccept(success -> {
                if (success) {
                    // Add to conversation history
                    if (history != null) {
                        history.add("Player: " + conversationPrompt);
                        history.add("Deity: [TTS Response Generated]");
                    }
                    
                    // Send minimal visual feedback
                    player.sendSystemMessage(Component.literal("§6⟦ " + deity.getName() + " ⟧ §7[Speaking via TTS...]"));
                    
                    if (onComplete != null) {
                        onComplete.run();
                    }
                } else {
                    LOGGER.error("TTS-Only mode failed for player {}", player.getName().getString());
                    player.sendSystemMessage(Component.literal("§c[TTS-Only Mode] TTS generation failed"));
                    endConversation(player);
                }
            })
            .exceptionally(throwable -> {
                LOGGER.error("TTS-Only mode error for player {}: {}", player.getName().getString(), throwable.getMessage());
                player.sendSystemMessage(Component.literal("§c[TTS-Only Mode] TTS error"));
                endConversation(player);
                return null;
            });
    }
    
    /**
     * Handle LLM-Only mode: Generate AI response but disable TTS
     */
    private static void handleLLMOnlyMode(com.bluelotuscoding.eidolonunchained.ai.AIProviderFactory.AIProvider provider,
                                          String conversationPrompt, String personality, String context, AIDeityConfig aiConfig,
                                          ServerPlayer player, DatapackDeity deity, ResourceLocation deityId, UUID playerId, 
                                          List<String> history, Runnable onComplete) {
        // Generate AI response normally but skip TTS
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
                LOGGER.info("🔥 LLM-Only Mode: AI Response received: '{}'", rawResponse);
                
                // Process the response normally but with TTS disabled
                processResponseWithoutTTS(player, deity, rawResponse, history, playerId, deityId, conversationPrompt, onComplete);
                
            }).exceptionally(ex -> {
                LOGGER.error("LLM-Only mode error: {}", ex.getMessage());
                player.sendSystemMessage(Component.translatable("eidolonunchained.ui.deity.no_response"));
                return null;
            });
    }
    
    /**
     * Handle Hybrid mode: Original behavior with both LLM and TTS
     */
    private static void handleHybridMode(com.bluelotuscoding.eidolonunchained.ai.AIProviderFactory.AIProvider provider,
                                         String conversationPrompt, String personality, String context, AIDeityConfig aiConfig,
                                         ServerPlayer player, DatapackDeity deity, ResourceLocation deityId, UUID playerId, 
                                         List<String> history, Runnable onComplete) {
        // Generate AI response asynchronously (original behavior)
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
                
                // Start pulsing effigy effects when AI begins responding
                EffigyEffectsManager.startAIResponseEffects(player);
                // Failsafe: end pulsing shortly after to return to steady baseline
                java.util.concurrent.CompletableFuture.runAsync(() -> {
                    try { Thread.sleep(2500); } catch (InterruptedException ignored) {}
                    try { com.bluelotuscoding.eidolonunchained.effects.EffigyEffectsManager.endAIResponseEffects(player); } catch (Exception ignored) {}
                });
                
                String rawResponse = aiResponse.dialogue;
                LOGGER.info("🔥 DEBUG: AI Response received: '{}'", rawResponse);
                
                // Continue with original hybrid processing (includes both AI commands and TTS)
                processHybridResponse(player, deity, rawResponse, history, playerId, deityId, conversationPrompt, onComplete);
                
            }).exceptionally(ex -> {
                LOGGER.error("Error generating AI response: {}", ex.getMessage());
                player.sendSystemMessage(Component.translatable("eidolonunchained.ui.deity.no_response"));
                return null;
            });
    }
    
    /**
     * Build a direct prompt for TTS-only mode
     */
    private static String buildTTSDirectPrompt(String userMessage, String personality, String context, String deityName) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are ").append(deityName).append(". ");
        prompt.append(personality).append(" ");
        prompt.append("Context: ").append(context).append(" ");
        prompt.append("The user says: \"").append(userMessage).append("\" ");
        prompt.append("Respond as ").append(deityName).append(" in character:");
        return prompt.toString();
    }
    
    /**
     * Process AI response without TTS (LLM-only mode)
     */
    private static void processResponseWithoutTTS(ServerPlayer player, DatapackDeity deity, String rawResponse, 
                                                  List<String> history, UUID playerId, ResourceLocation deityId, 
                                                  String originalMessage, Runnable onComplete) {
        try {
            // Process blessing requests and commands (same as hybrid mode)
            boolean isExplicitRequest = originalMessage.toLowerCase().matches(".*\\b(give|grant|bless|provide|can i have|i need|i want)\\b.*");
            if (isExplicitRequest && !shouldAllowBlessing(player, deity, originalMessage)) {
                // Player is on cooldown or tier restricted - skip AI processing entirely
                LOGGER.info("🚫 Skipping AI item extraction due to cooldown/tier restrictions");
                
                // Send only the AI's conversational response, no item processing
                String cleanedResponse = CommandStringUtils.safeChatDisplay(cleanModIdLeakage(rawResponse));
                player.sendSystemMessage(Component.literal("§6⟦ " + deity.getName() + " ⟧ §f" + cleanedResponse));
                
                // Add to history
                if (history != null) {
                    history.add("Deity: " + cleanedResponse);
                }
                return;
            }
            
            // Continue with regular response processing but disable TTS in final display
            processRegularResponse(player, deity, rawResponse, history, playerId, deityId, 0, onComplete);
            
        } catch (Exception e) {
            LOGGER.error("Error in LLM-only response processing: {}", e.getMessage());
            player.sendSystemMessage(Component.literal("§c[LLM-Only Mode] Error processing response"));
        }
    }
    
    /**
     * Process hybrid response with both AI commands and TTS
     */
    private static void processHybridResponse(ServerPlayer player, DatapackDeity deity, String rawResponse, 
                                              List<String> history, UUID playerId, ResourceLocation deityId, 
                                              String originalMessage, Runnable onComplete) {
        try {
            // This is the original hybrid processing logic
            boolean isExplicitRequest = originalMessage.toLowerCase().matches(".*\\b(give|grant|bless|provide|can i have|i need|i want)\\b.*");
            if (isExplicitRequest && !shouldAllowBlessing(player, deity, originalMessage)) {
                // Player is on cooldown or tier restricted - skip AI processing entirely
                LOGGER.info("🚫 Skipping AI item extraction due to cooldown/tier restrictions");
                
                // Send only the AI's conversational response, no item processing
                String cleanedResponse = CommandStringUtils.safeChatDisplay(cleanModIdLeakage(rawResponse));
                player.sendSystemMessage(Component.literal("§6⟦ " + deity.getName() + " ⟧ §f" + cleanedResponse));
                
                // Add to history
                if (history != null) {
                    history.add("Deity: " + cleanedResponse);
                }
                return;
            }
            
            // Continue with regular response processing
            processRegularResponse(player, deity, rawResponse, history, playerId, deityId, 0, onComplete);
            
        } catch (Exception e) {
            LOGGER.error("Error in hybrid response processing: {}", e.getMessage());
            player.sendSystemMessage(Component.literal("§c[Hybrid Mode] Error processing response"));
        }
    }

    // Evaluate JSON-driven natural-language triggers and perform actions if matched
    private static boolean evaluateNaturalLanguageTriggers(ServerPlayer player, ResourceLocation deityId, String message, AIDeityConfig aiConfig) {
        if (aiConfig.naturalLanguageTriggers == null || aiConfig.naturalLanguageTriggers.isEmpty()) return false;
        String msg = message.toLowerCase(java.util.Locale.ROOT);

        // Reputation helper
        DatapackDeity d = DatapackDeityManager.getDeity(deityId);
        double rep = d != null ? d.getPlayerReputation(player) : 0.0;

        // Per-player cooldown bucket
        var ctx = com.bluelotuscoding.eidolonunchained.ai.PlayerContextTracker.getOrCreateContext(player.getUUID(), player);
        if (ctx == null) return false;
        if (ctx.triggerCooldowns == null) ctx.triggerCooldowns = new java.util.HashMap<>();

        for (AIDeityConfig.NLTrigger trig : aiConfig.naturalLanguageTriggers) {
            try {
                // Reputation gate
                if (rep < trig.minReputation) continue;

                // Cooldown gate per deity+trigger key
                String key = deityId.toString() + "::" + (trig.id != null ? trig.id : Integer.toHexString(trig.hashCode()));
                long last = ctx.triggerCooldowns.getOrDefault(key, 0L);
                if (trig.cooldownSeconds > 0 && (System.currentTimeMillis() - last) < trig.cooldownSeconds * 1000L) continue;

                // Match check: contains OR regex
                boolean match = false;
                if (trig.contains != null && !trig.contains.isEmpty()) {
                    for (String kw : trig.contains) {
                        if (kw != null && !kw.isEmpty() && msg.contains(kw.toLowerCase(java.util.Locale.ROOT))) { match = true; break; }
                    }
                }
                if (!match && trig.regex != null && !trig.regex.isEmpty()) {
                    for (String r : trig.regex) {
                        try { if (msg.matches(r)) { match = true; break; } } catch (Exception ignored) {}
                    }
                }
                if (!match) continue;

                // Perform action
                if ("offer_fate".equalsIgnoreCase(trig.action)) {
                    tryOfferFateToPlayer(player, deityId, message);
                } else if ("run_commands".equalsIgnoreCase(trig.action)) {
                    java.util.List<String> cmds = new java.util.ArrayList<>();
                    if (trig.params != null && trig.params.has("commands")) {
                        com.google.gson.JsonArray arr = trig.params.getAsJsonArray("commands");
                        for (com.google.gson.JsonElement e : arr) cmds.add(e.getAsString());
                    }
                    // Optional prayer_type routing for blessing-like NL triggers
                    String prayerTypeParam = null;
                    if (trig.params != null && trig.params.has("prayer_type")) {
                        try { prayerTypeParam = trig.params.get("prayer_type").getAsString(); } catch (Exception ignored) {}
                    }
                    if ("blessing".equalsIgnoreCase(prayerTypeParam)) {
                        DatapackDeity deityObj = DatapackDeityManager.getDeity(deityId);
                        // If gating fails, provide polite feedback and do not execute
                        if (!shouldAllowBlessing(player, deityObj, message)) {
                            long remaining = getBlessingCooldownRemainingMs(player, deityId);
                            if (remaining > 0) {
                                player.sendSystemMessage(Component.literal(
                                    "§e⟦ " + (deityObj != null ? deityObj.getName() : deityId.toString()) +
                                    " ⟧ §7The divine energies still resonate from your last blessing. " +
                                    "§cWait " + formatShortDuration(remaining) + " before requesting another."));
                            } else {
                                player.sendSystemMessage(Component.literal(
                                    "§e⟦ " + (deityObj != null ? deityObj.getName() : deityId.toString()) +
                                    " ⟧ §7Your standing does not currently merit this blessing."));
                            }
                        } else if (!cmds.isEmpty()) {
                            executeCommands(player, deityId, cmds);
                        }
                    } else {
                        if (!cmds.isEmpty()) executeCommands(player, deityId, cmds);
                    }
                } else if ("send_message".equalsIgnoreCase(trig.action)) {
                    String text = trig.params != null && trig.params.has("text") ? trig.params.get("text").getAsString() : null;
                    if (text != null && !text.isEmpty()) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(text));
                    }
                } else if ("curse_target".equalsIgnoreCase(trig.action)) {
                    // Apply configured curse effects to nearby opposing entities (by team/tag)
                    try {
                        double radius = 10.0;
                        int maxTargets = 3;
                        String includeTeam = null; // only affect entities on this team (optional)
                        String excludeTeam = null; // skip entities on this team (optional)
                        String requireTag = null;  // only affect entities with this entity tag (optional)
                        String excludeTag = null;  // skip entities with this entity tag (optional)
                        boolean opposingToPlayer = true; // default: target entities not on player's team

                        if (trig.params != null) {
                            if (trig.params.has("radius")) radius = trig.params.get("radius").getAsDouble();
                            if (trig.params.has("max_targets")) maxTargets = Math.max(1, trig.params.get("max_targets").getAsInt());
                            if (trig.params.has("team")) includeTeam = trig.params.get("team").getAsString();
                            if (trig.params.has("not_team")) excludeTeam = trig.params.get("not_team").getAsString();
                            if (trig.params.has("tag")) requireTag = trig.params.get("tag").getAsString();
                            if (trig.params.has("not_tag")) excludeTag = trig.params.get("not_tag").getAsString();
                            if (trig.params.has("opposing_to_player")) opposingToPlayer = trig.params.get("opposing_to_player").getAsBoolean();
                        }

                        final double fRadius = radius;
                        final int fMaxTargets = maxTargets;
                        final String fIncludeTeam = includeTeam;
                        final String fExcludeTeam = excludeTeam;
                        final String fRequireTag = requireTag;
                        final String fExcludeTag = excludeTag;
                        final boolean fOpposingToPlayer = opposingToPlayer;

                        java.util.List<net.minecraft.world.entity.LivingEntity> targets = player.level().getEntitiesOfClass(
                            net.minecraft.world.entity.LivingEntity.class,
                            player.getBoundingBox().inflate(fRadius),
                            e -> {
                                if (e == player) return false;
                                if (!e.isAlive()) return false;
                                // Team filters
                                net.minecraft.world.scores.Team et = e.getTeam();
                                net.minecraft.world.scores.Team pt = player.getTeam();
                                if (fIncludeTeam != null && (et == null || !fIncludeTeam.equals(et.getName()))) return false;
                                if (fExcludeTeam != null && et != null && fExcludeTeam.equals(et.getName())) return false;
                                if (fOpposingToPlayer) {
                                    // Opposing means different non-null teams, or entity has explicit 'enemy' tag
                                    boolean teamOpposing = (pt != null && et != null && pt != et);
                                    boolean tagEnemy = e.getTags().contains("enemy");
                                    // If player has no team, consider any entity with 'enemy' tag as opposing
                                    if (pt == null) {
                                        if (!tagEnemy) return false;
                                    } else {
                                        if (!(teamOpposing || tagEnemy)) return false;
                                    }
                                }
                                // Tag filters
                                if (fRequireTag != null && !e.getTags().contains(fRequireTag)) return false;
                                if (fExcludeTag != null && e.getTags().contains(fExcludeTag)) return false;
                                return true;
                            }
                        );

                        if (!targets.isEmpty()) {
                            // Deterministic order by distance
                            targets.sort(java.util.Comparator.comparingDouble(t -> t.distanceToSqr(player)));
                            if (targets.size() > fMaxTargets) targets = targets.subList(0, fMaxTargets);

                            // Parse effects from params
                            java.util.List<String> effectSpecs = new java.util.ArrayList<>();
                            if (trig.params != null && trig.params.has("effects")) {
                                com.google.gson.JsonArray effArr = trig.params.getAsJsonArray("effects");
                                for (com.google.gson.JsonElement el : effArr) {
                                    if (el.isJsonPrimitive()) {
                                        // String like "minecraft:slowness 200 1" (duration seconds, amplifier)
                                        effectSpecs.add(el.getAsString());
                                    } else if (el.isJsonObject()) {
                                        com.google.gson.JsonObject o = el.getAsJsonObject();
                                        String id = o.has("id") ? o.get("id").getAsString() : "minecraft:slowness";
                                        int duration = o.has("duration") ? o.get("duration").getAsInt() : 120; // seconds
                                        int amp = o.has("amplifier") ? o.get("amplifier").getAsInt() : 0;
                                        effectSpecs.add(id + " " + duration + " " + amp);
                                    }
                                }
                            }
                            if (effectSpecs.isEmpty()) {
                                effectSpecs.add("minecraft:weakness 120 0"); // default
                            }

                            // Apply effects directly (avoid command selectors)
                            for (net.minecraft.world.entity.LivingEntity target : targets) {
                                for (String spec : effectSpecs) {
                                    try {
                                        String[] parts = spec.trim().split("\\s+");
                                        if (parts.length == 0) continue;
                                        net.minecraft.resources.ResourceLocation effId = new net.minecraft.resources.ResourceLocation(parts[0]);
                                        net.minecraft.world.effect.MobEffect eff = net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getValue(effId);
                                        if (eff == null) {
                                            LOGGER.warn("Unknown mob effect '{}', skipping", effId);
                                            continue;
                                        }
                                        int seconds = parts.length > 1 ? Integer.parseInt(parts[1]) : 120;
                                        int amplifier = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
                                        int ticks = Math.max(1, seconds * 20);
                                        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(eff, ticks, amplifier), player);
                                    } catch (Exception ie) {
                                        LOGGER.warn("Failed applying curse effect '{}': {}", spec, ie.getMessage());
                                    }
                                }
                            }

                            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                "§5✦ A shadow passes... §7(" + targets.size() + " foe" + (targets.size() == 1 ? "" : "s") + " afflicted)"));
                        }
                    } catch (Exception cex) {
                        LOGGER.warn("Error executing curse_target NL action: {}", cex.getMessage());
                    }
                }

                // Stamp cooldown
                ctx.triggerCooldowns.put(key, System.currentTimeMillis());
                return true; // consumed
            } catch (Exception ex) {
                LOGGER.warn("NL trigger error for deity {}: {}", deityId, ex.getMessage());
            }
        }
        return false;
    }

    // ===== Fate Offer/Accept Flow =====
    private static boolean handleFateOfferControl(ServerPlayer player, ResourceLocation deityId, String message) {
        String msg = message.trim().toLowerCase(java.util.Locale.ROOT);
        var ctx = com.bluelotuscoding.eidolonunchained.ai.PlayerContextTracker.getOrCreateContext(player.getUUID(), player);
        boolean hasOffer = ctx.pendingFateOfferTaskId != null && deityId.equals(ctx.pendingFateOfferDeity);

        // Accept keywords
        if (hasOffer && (msg.equals("yes") || msg.equals("sure") || msg.equals("accept") || msg.equals("okay") || msg.equals("ok") || msg.equals("yep") || msg.equals("y"))) {
            assignOfferedFate(player);
            return true;
        }
        // Decline keywords
        if (hasOffer && (msg.equals("no") || msg.equals("nope") || msg.equals("decline") || msg.equals("not now") || msg.equals("later") || msg.equals("n"))) {
            clearPendingOffer(ctx);
            player.sendSystemMessage(Component.literal("§7Very well. Another time."));
            return true;
        }

        // Explicit ask for a task/quest/fate – prompt an offer (still requires confirmation)
        if (msg.matches(".*\\b(task|quest|fate|job|mission)\\b.*") && msg.matches(".*\\b(give|have|offer|assign|got|any)\\b.*")) {
            tryOfferFateToPlayer(player, deityId, message);
            return false; // continue normal flow
        }

        return false;
    }

    private static void clearPendingOffer(com.bluelotuscoding.eidolonunchained.ai.PlayerContextTracker.EnhancedPlayerContext ctx) {
        ctx.pendingFateOfferTaskId = null;
        ctx.pendingFateOfferDeity = null;
    }

    private static void assignOfferedFate(ServerPlayer player) {
        var ctx = com.bluelotuscoding.eidolonunchained.ai.PlayerContextTracker.getOrCreateContext(player.getUUID(), player);
        if (ctx.pendingFateOfferTaskId == null || ctx.pendingFateOfferDeity == null) return;

        var cfg = AIDeityManager.getInstance().getAIConfig(ctx.pendingFateOfferDeity);
        if (cfg == null || cfg.task_config == null) { clearPendingOffer(ctx); return; }
        TaskSystemConfig.TaskTemplate tpl = null;
        for (TaskSystemConfig.TaskTemplate t : cfg.task_config.availableTasks) {
            if (ctx.pendingFateOfferTaskId.equals(t.taskId)) { tpl = t; break; }
        }
        if (tpl == null) { clearPendingOffer(ctx); return; }

        // Assign fate
        com.bluelotuscoding.eidolonunchained.ai.PlayerContextTracker.assignTask(player, tpl.taskId, tpl.description, cfg.deity_id, tpl.reputationReward);
        player.sendSystemMessage(Component.translatable("eidolonunchained.fate.assigned", tpl.description));
        player.sendSystemMessage(Component.translatable("eidolonunchained.fate.reward", tpl.reputationReward));

        // Cooldown stamp
        ctx.lastFateOfferByDeity.put(cfg.deity_id.toString(), System.currentTimeMillis());
        clearPendingOffer(ctx);
    }

    private static void tryOfferFateToPlayer(ServerPlayer player, ResourceLocation deityId, String playerMessage) {
        AIDeityConfig cfg = AIDeityManager.getInstance().getAIConfig(deityId);
        if (cfg == null || cfg.task_config == null || !cfg.task_config.enabled) return;

        var ctx = com.bluelotuscoding.eidolonunchained.ai.PlayerContextTracker.getOrCreateContext(player.getUUID(), player);
        // Respect cooldown between offers for this deity
        long last = ctx.lastFateOfferByDeity.getOrDefault(deityId.toString(), 0L);
        long cdMillis = cfg.task_config.taskAssignmentBehavior.cooldownBetweenAssignmentsHours * 3600_000L;
        if (System.currentTimeMillis() - last < cdMillis) return;

        // Max active tasks gate
        if (ctx.activeTasks.size() >= cfg.task_config.maxActiveTasks) return;

        // Reputation threshold gate
        DatapackDeity d = DatapackDeityManager.getDeity(deityId);
        double reputation = d != null ? d.getPlayerReputation(player) : 0.0;
        if (reputation < cfg.task_config.taskAssignmentBehavior.minReputationForAutoAssign) return;

        // More sophisticated detection: only offer new fates for explicit requests
        boolean explicitRequest = playerMessage.toLowerCase(java.util.Locale.ROOT)
            .matches(".*\\b(give|have|offer|assign|got|any|new)\\b.*\\b(task|quest|fate|job|mission)\\b.*");

        // Don't offer fates for questions about existing ones
        boolean askingAboutExisting = playerMessage.toLowerCase(java.util.Locale.ROOT)
            .matches(".*\\b(recall|remember|current|my|what|which|about|status|progress)\\b.*\\b(task|quest|fate|job|mission)\\b.*");

        if (askingAboutExisting) return; // Let AI respond conversationally instead

        // Only offer if explicitly requesting new tasks or by probability
        float p = cfg.task_config.taskAssignmentBehavior.autoAssignProbability;
        float roll = new java.util.Random().nextFloat();
        if (!explicitRequest && roll > p) return;

        // Select eligible fate and offer (not assign)
        TaskSystemConfig.TaskTemplate candidate = selectEligibleFate(player, cfg);
        if (candidate == null) return;

        ctx.pendingFateOfferTaskId = candidate.taskId;
        ctx.pendingFateOfferDeity = deityId;
        player.sendSystemMessage(Component.literal("§6I have a task for you: §e" + candidate.description));
        player.sendSystemMessage(Component.literal("§7Reward: §6" + candidate.reputationReward + " reputation"));
        player.sendSystemMessage(Component.literal("§7Do you accept? (yes/no)"));
    }

    private static TaskSystemConfig.TaskTemplate selectEligibleFate(ServerPlayer player, AIDeityConfig cfg) {
        java.util.List<TaskSystemConfig.TaskTemplate> pool = new java.util.ArrayList<>();
        var ctx = com.bluelotuscoding.eidolonunchained.ai.PlayerContextTracker.getOrCreateContext(player.getUUID(), player);
        for (TaskSystemConfig.TaskTemplate t : cfg.task_config.availableTasks) {
            if (ctx.activeTasks.containsKey(t.taskId)) continue; // avoid duplicates
            if (isFateEligibleForOffer(player, t)) pool.add(t);
        }
        if (pool.isEmpty()) return null;
        return pool.get(new java.util.Random().nextInt(pool.size()));
    }

    private static boolean isFateEligibleForOffer(ServerPlayer player, TaskSystemConfig.TaskTemplate tpl) {
        // Progression tier gate with "none" bypass
        if (tpl.progressionTier != null && !tpl.progressionTier.isEmpty() && !"none".equalsIgnoreCase(tpl.progressionTier)) {
            var c = com.bluelotuscoding.eidolonunchained.ai.PlayerContextTracker.getOrCreateContext(player.getUUID(), player);
            if (c == null || !c.unlockedProgressions.contains(tpl.progressionTier)) return false;
        }
        // ai_assignment_context gating
        if (tpl.aiAssignmentContext != null && !tpl.aiAssignmentContext.isEmpty()) {
            try {
                com.google.gson.JsonObject rules = com.bluelotuscoding.eidolonunchained.util.JsonUtils.GSON.fromJson(tpl.aiAssignmentContext, com.google.gson.JsonObject.class);
                if (rules.has("min_reputation")) {
                    int minRep = rules.get("min_reputation").getAsInt();
                    DatapackDeity deity = DatapackDeityManager.getDeity(getDeityForTask(tpl));
                    double rep = deity != null ? deity.getPlayerReputation(player) : 0.0;
                    if (rep < minRep) return false;
                }
                if (rules.has("required_dimension")) {
                    String reqDim = rules.get("required_dimension").getAsString();
                    if (!player.level().dimension().location().toString().equals(reqDim)) return false;
                }
                if (rules.has("required_items")) {
                    com.google.gson.JsonArray items = rules.getAsJsonArray("required_items");
                    for (com.google.gson.JsonElement itemEl : items) {
                        com.google.gson.JsonObject obj = itemEl.getAsJsonObject();
                        String itemId = obj.get("item").getAsString();
                        int count = obj.has("count") ? obj.get("count").getAsInt() : 1;
                        if (!hasItem(player, itemId, count)) return false;
                    }
                }
            } catch (Exception ignored) {}
        }
        return true;
    }

    private static ResourceLocation getDeityForTask(TaskSystemConfig.TaskTemplate tpl) {
        for (AIDeityConfig c : AIDeityManager.getInstance().getAllConfigs()) {
            if (c.task_config != null && c.task_config.availableTasks.contains(tpl)) return c.deity_id;
        }
        return null;
    }

    private static boolean hasItem(ServerPlayer player, String itemId, int count) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            net.minecraft.world.item.ItemStack stack = player.getInventory().getItem(i);
            if (net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).toString().equals(itemId)) {
                total += stack.getCount();
                if (total >= count) return true;
            }
        }
        return total >= count;
    }
    
    /**
     * Process regular response handling (separated for hybrid integration)
     */
    private static void processRegularResponse(ServerPlayer player, DatapackDeity deity, String rawResponse, 
                                             List<String> history, UUID playerId, ResourceLocation deityId, int commandsExecuted, Runnable onComplete) {
        try {
            // Get AI config for additional processing
            AIDeityConfig aiConfig = AIDeityManager.getInstance().getAIConfig(deityId);
            if (aiConfig == null) {
                LOGGER.warn("No AI config found for deity {}", deityId);
                return;
            }
            
            // Clean response for display (remove commands/triggers and technical mod IDs)
            String cleanedResponse = CommandStringUtils.safeChatDisplay(cleanModIdLeakage(removeAIMarkup(rawResponse)));
            
            // Add response to history (using cleaned version)
            if (history != null) {
                history.add("Deity: " + cleanedResponse);
            } else {
                LOGGER.warn("🚨 Conversation history is null when adding deity response for player {}", playerId);
            }
            
            // Add to persistent history on main thread to avoid SavedData classloader issues
            final UUID uuid = playerId;
            final ResourceLocation deityResourceId = deityId;
            final String deityDisplayName = deity.getName();
            final String responseText = cleanedResponse;
            
            MinecraftServer mcServer = player.getServer();
            if (mcServer != null) {
                mcServer.execute(() -> {
                    try {
                        ConversationHistoryManager.get().addMessage(uuid, deityResourceId, deityDisplayName, responseText);
                    } catch (Exception e) {
                        LOGGER.warn("Failed to save deity response to persistent storage: {}", e.getMessage());
                    }
                });
            }
            
            // Check for auto-judgment and additional commands only if no commands were already executed
            if (commandsExecuted == 0 && aiConfig.prayer_configs != null && aiConfig.prayer_configs.containsKey("conversation")) {
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
            
            // Check if this is a fate completion response for auto-close
            boolean isFateCompletion = false;
            com.google.gson.JsonObject autoCloseConfig = null;

            // Check if the original message was a fate completion
            if (history != null && !history.isEmpty()) {
                String lastPlayerMessage = "";
                for (int i = history.size() - 1; i >= 0; i--) {
                    String msg = history.get(i);
                    if (msg.startsWith("Player: ")) {
                        lastPlayerMessage = msg.substring("Player: ".length());
                        break;
                    }
                }

                if (lastPlayerMessage.startsWith("FATE_COMPLETED:")) {
                    isFateCompletion = true;
                    String taskId = lastPlayerMessage.substring("FATE_COMPLETED:".length());

                    // Get auto-close configuration from fate data
                    try {
                        com.google.gson.JsonObject fateData = com.bluelotuscoding.eidolonunchained.data.FateDataLoader.getFateData(taskId);
                        if (fateData != null && fateData.has("ai_assignment_context")) {
                            autoCloseConfig = fateData.getAsJsonObject("ai_assignment_context");
                        }
                    } catch (Exception e) {
                        LOGGER.error("Error getting auto-close config for fate {}: {}", taskId, e.getMessage());
                    }
                }
            }

            // Send deity response to player using prominent title/subtitle display
            sendDeityResponse(player, deity.getName(), cleanedResponse, onComplete);

            // Parse AI-decided triggers AFTER sending narrative to keep chat snappy
            try {
                String nlMode = com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig.COMMON.naturalLanguageTriggerMode.get();
                boolean allowAiTriggers = "ai_only".equalsIgnoreCase(nlMode) || "both".equalsIgnoreCase(nlMode);
                if (allowAiTriggers) {
                    handleAITriggersFromResponse(player, deityId, rawResponse);
                }
            } catch (Exception ex) {
                LOGGER.warn("Failed to handle AI triggers: {}", ex.getMessage());
            }
            
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
                
                // Notify player of reputation gain with proper localization
                player.sendSystemMessage(Component.translatable("eidolonunchained.ui.deity.reputation_gained", 
                    deity.getDisplayName(), String.format("%.1f", baseGain)));
            });
            
        } catch (Exception e) {
            LOGGER.error("Error in processRegularResponse: {}", e.getMessage(), e);
            player.sendSystemMessage(Component.translatable("eidolonunchained.chat.connection_falters"));
        }
    }

    // Remove any AI control markup from text shown to players
    private static String removeAIMarkup(String text) {
        if (text == null) return null;
        // Remove [COMMAND:...], [ACTION:...], [TRIGGER:...]
        return text.replaceAll("\\[(?:COMMAND|ACTION|TRIGGER):.*?]", "");
    }

    // Parse and perform AI-decided triggers included in AI responses
    private static void handleAITriggersFromResponse(ServerPlayer player, ResourceLocation deityId, String rawResponse) {
        if (rawResponse == null || rawResponse.isEmpty()) return;

        // Collect all [TRIGGER:...] occurrences
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\[TRIGGER:(.*?)]", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher m = p.matcher(rawResponse);

        // Avoid spamming: handle at most one trigger per response
        if (!m.find()) return;
        String payload = m.group(1).trim();
        if (payload.isEmpty()) return;

        // Parse action and optional key=value pairs (very lightweight parser)
        String action;
        java.util.Map<String, String> kv = new java.util.HashMap<>();
        int space = payload.indexOf(' ');
        if (space > 0) {
            action = payload.substring(0, space).trim();
            String rest = payload.substring(space + 1).trim();
            // Parse key=value tokens; values may be quoted
            java.util.regex.Matcher kvMatcher = java.util.regex.Pattern
                .compile("(\\w+)=\\\"([^\\\"]*)\\\"|(\\w+)=([^;]+)")
                .matcher(rest);
            while (kvMatcher.find()) {
                if (kvMatcher.group(1) != null) {
                    kv.put(kvMatcher.group(1), kvMatcher.group(2));
                } else {
                    kv.put(kvMatcher.group(3), kvMatcher.group(4).trim());
                }
            }
        } else {
            action = payload;
        }

        // Respect existing pending offers to avoid duplicates
        var ctx = com.bluelotuscoding.eidolonunchained.ai.PlayerContextTracker.getOrCreateContext(player.getUUID(), player);
        boolean hasOffer = ctx != null && ctx.pendingFateOfferTaskId != null && deityId.equals(ctx.pendingFateOfferDeity);

        switch (action.toLowerCase(java.util.Locale.ROOT)) {
            case "offer_fate":
                if (!hasOffer) {
                    tryOfferFateToPlayer(player, deityId, "(ai-trigger)");
                }
                break;
            case "send_message": {
                String text = kv.getOrDefault("text", "");
                if (!text.isEmpty()) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(text));
                }
                break;
            }
            case "run_commands": {
                String cmds = kv.getOrDefault("commands", "");
                if (!cmds.isEmpty()) {
                    java.util.List<String> list = new java.util.ArrayList<>();
                    for (String c : cmds.split(";")) {
                        if (c != null && !c.trim().isEmpty()) list.add(c.trim());
                    }
                    if (!list.isEmpty()) executeCommands(player, deityId, list);
                }
                break;
            }
            default:
                LOGGER.debug("Unknown AI trigger action: {}", action);
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
                        prompt.append("IMPORTANT: Acknowledge this player's title (").append(title).append(") in your greeting - they have earned recognition!\n");
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

        // 🐺 ADD NEARBY MOB AWARENESS CONTEXT
        if (aiConfig != null && aiConfig.patron_config != null && !aiConfig.patron_config.supportedMobIds.isEmpty()) {
            try {
                String mobContext = buildNearbyMobContext(player, aiConfig.patron_config.supportedMobIds);
                if (!mobContext.isEmpty()) {
                    prompt.append("\n\n=== NEARBY CREATURES ===\n");
                    prompt.append(mobContext);
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to build mob awareness context: {}", e.getMessage());
            }
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
        
        // Optionally expose blessing/trigger cooldowns so AI can avoid promising actions on cooldown
        if (com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig.COMMON.exposeCooldownsInAIContext.get()) {
            long remaining = getBlessingCooldownRemainingMs(player, deityId);
            prompt.append("\n=== COOLDOWNS ===\n");
            if (remaining > 0) {
                prompt.append("Blessing cooldown active: ").append(formatShortDuration(remaining)).append(" remaining.\n");
                prompt.append("If the player asks for a blessing while on cooldown, politely inform them to wait that long.\n");
            } else {
                prompt.append("Blessing cooldown: ready now.\n");
            }
        }

        // Check if this is a fate completion message
        if (currentMessage.startsWith("FATE_COMPLETED:")) {
            String taskId = currentMessage.substring("FATE_COMPLETED:".length());
            prompt.append("\n\n=== FATE COMPLETION CELEBRATION ===\n");
            prompt.append("SPECIAL CONTEXT: The player has just completed a divine fate/task: ").append(taskId).append("\n");

            // Get completion phrases from fate data if available
            try {
                com.google.gson.JsonObject fateData = com.bluelotuscoding.eidolonunchained.data.FateDataLoader.getFateData(taskId);
                if (fateData != null) {
                    prompt.append("Fate Description: ").append(fateData.get("description").getAsString()).append("\n");

                    if (fateData.has("ai_assignment_context") &&
                        fateData.getAsJsonObject("ai_assignment_context").has("completion_phrases")) {
                        com.google.gson.JsonArray phrases = fateData.getAsJsonObject("ai_assignment_context")
                                                                   .getAsJsonArray("completion_phrases");
                        prompt.append("Suggested completion phrases: ");
                        for (int i = 0; i < phrases.size(); i++) {
                            if (i > 0) prompt.append(", ");
                            prompt.append("\"").append(phrases.get(i).getAsString()).append("\"");
                        }
                        prompt.append("\n");
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error getting fate completion data: {}", e.getMessage());
            }

            prompt.append("RESPONSE REQUIRED: Acknowledge the completion with appropriate celebration, pride, and perhaps hints about future paths. ");
            prompt.append("The player has proven their dedication and deserves recognition. Keep it conversational and in character.\n");
            prompt.append("Player's Current Message: \"[Fate automatically completed - respond with celebration]\"");
        } else {
            // Add current player message with emphasis
            prompt.append("\n\nPlayer's Current Message: \"").append(currentMessage).append("\"");
        }
        prompt.append("\n");
        
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
        if (server == null) {
            LOGGER.error("Cannot execute deity commands - server is null for player {}", player.getName().getString());
            return;
        }

        CommandSourceStack commandSource = server.createCommandSourceStack()
            .withSource(player)  // 🔧 FIX: Use player as source so @s selector works
            .withLevel(player.serverLevel())
            .withPosition(player.position())
            .withPermission(4) // Admin permission level
            .withSuppressedOutput(); // 🔥 SILENT: Suppress command output to chat

        int successCount = 0;
        for (String command : commands) {
            try {
                if (command == null || command.trim().isEmpty()) {
                    LOGGER.warn("Skipping empty/null command for player {}", player.getName().getString());
                    continue;
                }

                // Replace placeholders
                String processedCommand = command
                    .replace("{player}", player.getName().getString())
                    .replace("{x}", String.valueOf((int) player.getX()))
                    .replace("{y}", String.valueOf((int) player.getY()))
                    .replace("{z}", String.valueOf((int) player.getZ()));

                LOGGER.info("🔧 Executing deity command for {}: {}", player.getName().getString(), processedCommand);
                
                // Execute command and check result
                int result = server.getCommands().performPrefixedCommand(commandSource, processedCommand);
                
                if (result > 0) {
                    successCount++;
                    LOGGER.info("✅ Successfully executed deity command for {}: {} (result: {})", 
                        player.getName().getString(), processedCommand, result);
                    ConversationHistoryManager.logCommandExecutionStatic(player, deityId, processedCommand, true, "Command executed successfully");
                } else {
                    // 🔧 FALLBACK: If @s selector failed, try with explicit player name
                    if (processedCommand.contains("@s")) {
                        String fallbackCommand = processedCommand.replace("@s", player.getName().getString());
                        LOGGER.info("🔄 Retrying command with explicit player name: {}", fallbackCommand);
                        
                        int fallbackResult = server.getCommands().performPrefixedCommand(commandSource, fallbackCommand);
                        if (fallbackResult > 0) {
                            successCount++;
                            LOGGER.info("✅ Fallback command succeeded: {} (result: {})", fallbackCommand, fallbackResult);
                            ConversationHistoryManager.logCommandExecutionStatic(player, deityId, fallbackCommand, true, "Command succeeded with fallback");
                        } else {
                            LOGGER.warn("❌ Both @s and explicit name failed for command: {}", processedCommand);
                            ConversationHistoryManager.logCommandExecutionStatic(player, deityId, processedCommand, false, "Command failed even with @s fallback");
                        }
                    } else {
                        LOGGER.warn("❌ Command returned 0 result: {} (No @s selector to retry)", processedCommand);
                        ConversationHistoryManager.logCommandExecutionStatic(player, deityId, processedCommand, false, "Command returned 0 result - no fallback available");
                    }
                }

            } catch (Exception e) {
                // Enhanced error logging with stack trace for debugging
                String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                LOGGER.error("❌ Failed to execute deity command '{}' for player {}: {}", 
                    command, player.getName().getString(), errorMsg, e);
                ConversationHistoryManager.logCommandExecutionStatic(player, deityId, command, false, errorMsg);
            }
        }

        // Send feedback to player about divine intervention
        if (successCount > 0) {
            player.sendSystemMessage(Component.literal("§6✦ Divine power flows through you... §7(" + successCount + " blessing" + (successCount == 1 ? "" : "s") + " granted)"));
        } else if (!commands.isEmpty()) {
            player.sendSystemMessage(Component.literal("§c✦ The divine energy falters... §7(No blessings were granted)"));
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
        sendDeityResponse(player, deityName, message, null);
    }

    /**
     * Send deity response with improved formatting and display options with optional completion callback and auto-close
     */
    private static void sendDeityResponse(ServerPlayer player, String deityName, String message, Runnable onComplete) {

        // Check if TTS is enabled for this specific player (via commands)
        com.bluelotuscoding.eidolonunchained.ai.TTSManager ttsManager = com.bluelotuscoding.eidolonunchained.ai.TTSManager.getInstance();
        boolean isTTSEnabled = ttsManager.getPlayerSettings(player).enabled;

        LOGGER.info("🔊 TTS DEBUG: Player {} TTS enabled: {}", player.getName().getString(), isTTSEnabled);

        // Generate TTS audio for deity response (async, non-blocking)
        ResourceLocation currentDeityId = activeConversations.get(player.getUUID());
        if (currentDeityId != null && isTTSEnabled) {
            try {
                com.bluelotuscoding.eidolonunchained.ai.TTSManager.getInstance()
                    .generateAndSendTTS(player, message, currentDeityId.getPath())
                    .exceptionally(throwable -> {
                        LOGGER.debug("TTS generation failed for player {}: {}",
                            player.getName().getString(), throwable.getMessage());
                        return false;
                    });
            } catch (Exception e) {
                LOGGER.debug("Error initiating TTS for player {}: {}",
                    player.getName().getString(), e.getMessage());
            }
        }

        // 🔥 TTS-AWARE VISUAL DISPLAY: Different display logic based on TTS status
        if (isTTSEnabled) {
            // TTS Mode: Pure audio experience - no visual interference
            // Let Player2 TTS handle the audio, don't show any action bar text
            if (onComplete != null) {
                onComplete.run();
            }
        } else {
            // No TTS Mode: Full visual display with typing animation
            // Get display configuration
            String displayMethod = EidolonUnchainedConfig.COMMON.displayMethod.get();
            int maxSubtitleLength = EidolonUnchainedConfig.COMMON.maxSubtitleLength.get();
            
            // Only auto-select if explicitly set to AUTO
            if ("AUTO".equals(displayMethod)) {
                if (message.length() > maxSubtitleLength) {
                    displayMethod = "ENHANCED_CHAT";
                } else {
                    displayMethod = "TITLE_SUBTITLE";
                }
            }
            
            // Route to appropriate display method for full visual experience
            switch (displayMethod) {
                case "TITLE_SUBTITLE":
                    sendPureActionBarDisplay(player, deityName, message, onComplete);
                    break;
                case "ACTION_BAR":
                    sendPureActionBarDisplay(player, deityName, message, onComplete);
                    break;
                case "ENHANCED_CHAT":
                    sendEnhancedChatMessage(player, deityName, message);
                    if (onComplete != null) {
                        onComplete.run();
                    }
                    break;
                default:
                    sendPureActionBarDisplay(player, deityName, message, onComplete);
            }
        }
    }

    /**
     * 🔥 MINIMAL TTS FEEDBACK: Simple visual indicator that deity is speaking via TTS
     */
    private static void sendMinimalTTSFeedback(ServerPlayer player, String deityName, Runnable onComplete) {
        // Simple action bar message indicating deity is speaking
        Component feedbackMessage = Component.literal("🎙️ ")
            .append(Component.literal(deityName))
            .append(Component.literal(" is speaking..."));

        // Send immediate feedback via action bar
        player.sendSystemMessage(feedbackMessage, true);
        
        // Brief display duration (2 seconds), then clear and run callback
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                // Clear action bar on server thread
                if (player.getServer() != null) {
                    player.getServer().execute(() -> {
                        player.sendSystemMessage(Component.empty(), true);
                        if (onComplete != null) {
                            onComplete.run();
                        }
                    });
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        }).start();
    }
    
    /**
     * 🔥 FULLY CONFIGURABLE ACTION BAR DISPLAY with typing animation
     */
    private static void sendPureActionBarDisplay(ServerPlayer player, String deityName, String message) {
        sendPureActionBarDisplay(player, deityName, message, null);
    }
    
    /**
     * 🔥 FULLY CONFIGURABLE ACTION BAR DISPLAY with typing animation and completion callback
     */
    private static void sendPureActionBarDisplay(ServerPlayer player, String deityName, String message, Runnable onComplete) {
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
            startActionBarTypingAnimation(player, deityName, message, typingSpeed, sentenceDelay, fadeDelay, maxWidth, centerText, wrapText, onComplete);
        } else {
            // Show instant message
            String formattedMessage = formatActionBarMessage(deityName, message, maxWidth, centerText, wrapText);
            Component actionBarComponent = Component.literal(formattedMessage);
            // Pulse effigy eyes for each action bar send to emulate speech
            try { com.bluelotuscoding.eidolonunchained.effects.EffigyEffectsManager.startAIResponseEffects(player); } catch (Exception ignored) {}
            player.sendSystemMessage(actionBarComponent, true);
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                try { com.bluelotuscoding.eidolonunchained.effects.EffigyEffectsManager.endAIResponseEffects(player); } catch (Exception ignored) {}
            });
            
            LOGGER.debug("Sent instant action bar message to {}: {}", player.getName().getString(), formattedMessage);
            
            // Execute callback immediately for instant messages
            if (onComplete != null) {
                onComplete.run();
            }
        }
    }
    
    /**
     * 🔥 ENHANCED ACTION BAR MESSAGES - Sentence-based display with typing animation
     * Breaks messages into complete sentences and displays each sentence sequentially
     * Starts new action bar lines at sentence endings (periods, exclamation points, question marks)
     */
    private static void startActionBarTypingAnimation(ServerPlayer player, String deityName, String message,
                                                    int typingSpeed, int sentenceDelay, int fadeDelay,
                                                    int maxWidth, boolean centerText, boolean wrapText, Runnable onComplete) {
        
        // Split message into action bar-friendly chunks
        List<String> messageChunks = intelligentTextWrap(message, deityName, maxWidth);
        
        if (messageChunks.isEmpty()) {
            // Execute callback immediately if no content
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }
        
        // Display each chunk sequentially with typing animation
        startSequentialActionBarDisplay(player, deityName, messageChunks, typingSpeed, sentenceDelay, fadeDelay, maxWidth, centerText, onComplete);
    }
    
    /**
     * Display message chunks sequentially in action bar with typing animation
     */
    private static void startSequentialActionBarDisplay(ServerPlayer player, String deityName, List<String> messageChunks,
                                                       int typingSpeed, int sentenceDelay, int fadeDelay,
                                                       int maxWidth, boolean centerText, Runnable onComplete) {
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                for (int chunkIndex = 0; chunkIndex < messageChunks.size(); chunkIndex++) {
                    String chunk = messageChunks.get(chunkIndex);
                    
                    // Type out this chunk character by character
                    for (int charIndex = 0; charIndex <= chunk.length(); charIndex++) {
                        String partialChunk = chunk.substring(0, charIndex);
                        String formattedMessage = formatActionBarMessage(deityName, partialChunk, maxWidth, centerText, false);
                        Component actionBarComponent = Component.literal(formattedMessage);
                        
                        // Send to action bar on main thread + pulse per update
                        player.getServer().execute(() -> {
                            try { com.bluelotuscoding.eidolonunchained.effects.EffigyEffectsManager.startAIResponseEffects(player); } catch (Exception ignored) {}
                            player.sendSystemMessage(actionBarComponent, true);
                            java.util.concurrent.CompletableFuture.runAsync(() -> {
                                try { Thread.sleep(400); } catch (InterruptedException ignored) {}
                                try { com.bluelotuscoding.eidolonunchained.effects.EffigyEffectsManager.endAIResponseEffects(player); } catch (Exception ignored) {}
                            });
                        });
                        
                        // Wait for typing speed
                        Thread.sleep(typingSpeed);
                    }
                    
                    // 🔥 SMART PAUSING: Longer pause at sentence endings
                    if (chunkIndex < messageChunks.size() - 1) {
                        String currentChunk = messageChunks.get(chunkIndex);
                        
                        // Check if this chunk ends with sentence punctuation
                        if (currentChunk.trim().matches(".*[.!?]\\s*$")) {
                            // This is a complete sentence - use longer pause
                            Thread.sleep(sentenceDelay * 2); // Double pause for sentence endings
                            LOGGER.debug("🔥 SENTENCE END: Extended pause after '{}'", currentChunk.trim());
                        } else {
                            // Regular chunk break - shorter pause
                            Thread.sleep(sentenceDelay);
                        }
                    }
                }
                
                // Final message stays for fade delay
                Thread.sleep(fadeDelay * 50); // Convert ticks to milliseconds
                
                // Clear action bar
                player.getServer().execute(() -> {
                    player.sendSystemMessage(Component.literal(""), true);
                });
                
                // 🔥 EXECUTE CALLBACK AFTER ACTION BAR ANIMATION COMPLETES
                if (onComplete != null) {
                    player.getServer().execute(() -> {
                        try {
                            onComplete.run();
                        } catch (Exception e) {
                            LOGGER.error("Error executing action bar completion callback: {}", e.getMessage());
                        }
                    });
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.warn("Sequential action bar display interrupted for player {}", player.getName().getString());
                
                // Execute callback even if interrupted
                if (onComplete != null) {
                    player.getServer().execute(() -> {
                        try {
                            onComplete.run();
                        } catch (Exception ex) {
                            LOGGER.error("Error executing interrupted callback: {}", ex.getMessage());
                        }
                    });
                }
            }
        });
    }

    /**
     * Sentence-first wrap for action bar typing.
     * Break only at sentence boundaries (., !, ?). Avoid mid-sentence chopping.
     */
    private static List<String> intelligentTextWrap(String message, String deityName, int maxWidth) {
        List<String> chunks = new ArrayList<>();
        // Split on sentence boundaries: keep end punctuation with the sentence
        String[] sentences = message.split("(?<=[.!?])\\s+");
        for (String s : sentences) {
            String sentence = s.trim();
            if (!sentence.isEmpty()) chunks.add(sentence);
        }
        return chunks;
    }
    

    
    /**
     * 🔥 FORMAT ACTION BAR MESSAGE with proper centering - NO TRUNCATION
     */
    private static String formatActionBarMessage(String deityName, String message, int maxWidth, boolean centerText, boolean wrapText) {
        // Create header with deity name
        String header = "§6⟦ " + deityName + " ⟧";
        
        // If message is already properly chunked (from intelligentTextWrap), just format it
        String fullText = header + " §f" + message;
        
        // Check if the full message exceeds action bar width
        int visibleLength = fullText.replaceAll("§.", "").length();
        
        if (visibleLength > maxWidth && wrapText) {
            // This should not happen if intelligentTextWrap worked correctly
            LOGGER.warn("Action bar message still too long after wrapping: {} chars vs {} max", visibleLength, maxWidth);
            
            // Emergency truncation to prevent overflow
            String visibleText = fullText.replaceAll("§.", "");
            if (visibleText.length() > maxWidth) {
                // Calculate how much of the message we can show
                int headerLength = header.replaceAll("§.", "").length() + 1; // +1 for space
                int availableLength = maxWidth - headerLength - 3; // -3 for "..."
                
                if (availableLength > 0) {
                    String truncatedMessage = message.substring(0, Math.min(availableLength, message.length())) + "...";
                    fullText = header + " §f" + truncatedMessage;
                } else {
                    // Not enough space even for truncation
                    fullText = header;
                }
            }
        }
        
        // Center text if enabled and reasonable length
        if (centerText && fullText.replaceAll("§.", "").length() <= maxWidth) {
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
        // Pulse for the speak notice
        try { com.bluelotuscoding.eidolonunchained.effects.EffigyEffectsManager.startAIResponseEffects(player); } catch (Exception ignored) {}
        player.sendSystemMessage(actionBar, true);
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            try { com.bluelotuscoding.eidolonunchained.effects.EffigyEffectsManager.endAIResponseEffects(player); } catch (Exception ignored) {}
        });
        
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
        try { com.bluelotuscoding.eidolonunchained.effects.EffigyEffectsManager.startAIResponseEffects(player); } catch (Exception ignored) {}
        player.sendSystemMessage(actionBarComponent, true);
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            try { com.bluelotuscoding.eidolonunchained.effects.EffigyEffectsManager.endAIResponseEffects(player); } catch (Exception ignored) {}
        });
        
        // Schedule additional sends for persistence
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(1000); // 1 second
                try { com.bluelotuscoding.eidolonunchained.effects.EffigyEffectsManager.startAIResponseEffects(player); } catch (Exception ignored) {}
                player.sendSystemMessage(actionBarComponent, true);
                java.util.concurrent.CompletableFuture.runAsync(() -> {
                    try { Thread.sleep(400); } catch (InterruptedException ignored) {}
                    try { com.bluelotuscoding.eidolonunchained.effects.EffigyEffectsManager.endAIResponseEffects(player); } catch (Exception ignored) {}
                });
                
                Thread.sleep(1000); // Another second
                try { com.bluelotuscoding.eidolonunchained.effects.EffigyEffectsManager.startAIResponseEffects(player); } catch (Exception ignored) {}
                player.sendSystemMessage(actionBarComponent, true);
                java.util.concurrent.CompletableFuture.runAsync(() -> {
                    try { Thread.sleep(400); } catch (InterruptedException ignored) {}
                    try { com.bluelotuscoding.eidolonunchained.effects.EffigyEffectsManager.endAIResponseEffects(player); } catch (Exception ignored) {}
                });
                
                Thread.sleep(1000); // Final send
                try { com.bluelotuscoding.eidolonunchained.effects.EffigyEffectsManager.startAIResponseEffects(player); } catch (Exception ignored) {}
                player.sendSystemMessage(actionBarComponent, true);
                java.util.concurrent.CompletableFuture.runAsync(() -> {
                    try { Thread.sleep(400); } catch (InterruptedException ignored) {}
                    try { com.bluelotuscoding.eidolonunchained.effects.EffigyEffectsManager.endAIResponseEffects(player); } catch (Exception ignored) {}
                });
                
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
                        // Get the actual title from the stage data (for AI roleplay)
                        Object titleObj = stageDataMap.get("title");
                        if (titleObj instanceof String) {
                            bestStage = (String) titleObj;
                            highestQualifyingReputation = requiredReputation;
                        } else {
                            // Fallback to stage name if no title defined
                            bestStage = stageName;
                            highestQualifyingReputation = requiredReputation;
                        }
                    }
                }
                
                LOGGER.info("🎭 AI progression for {}/{}: {} ({}rep) - using datapack title", 
                    player.getName().getString(), deity.getName(), bestStage, (int)reputation);
                
                return bestStage;
            }
            
            // Secondary: Use AI deity config reputation thresholds if available
            AIDeityConfig aiConfig = AIDeityManager.getInstance().getAIConfig(deity.getId());
            if (aiConfig != null && !aiConfig.getReputationBehaviors().isEmpty()) {
                
                // Get follower personality modifiers (tier names) from JSON
                if (aiConfig.patron_config != null && aiConfig.patron_config.followerPersonalityModifiers != null) {
                    Map<String, String> personalityModifiers = aiConfig.patron_config.followerPersonalityModifiers;
                    Map<Integer, String> reputationBehaviors = aiConfig.getReputationBehaviors();
                    
                    // Sort reputation thresholds to find the highest one the player qualifies for
                    List<Integer> sortedThresholds = reputationBehaviors.keySet().stream()
                        .filter(threshold -> reputation >= threshold)
                        .sorted(Integer::compareTo)
                        .collect(java.util.stream.Collectors.toList());
                    
                    if (!sortedThresholds.isEmpty()) {
                        // Get the highest threshold the player qualifies for
                        int qualifyingThreshold = sortedThresholds.get(sortedThresholds.size() - 1);
                        
                        LOGGER.debug("🎯 Player {} qualifies for reputation threshold: {} (reputation: {})", 
                            player.getName().getString(), qualifyingThreshold, (int)reputation);
                        
                        // Now map this threshold to the appropriate tier name from followerPersonalityModifiers
                        String tierName = mapThresholdToTierName(qualifyingThreshold, personalityModifiers, reputationBehaviors);
                        
                        LOGGER.info("✅ AI Config progression for {}/{}: '{}' ({}rep, threshold={})", 
                            player.getName().getString(), deity.getName(), tierName, (int)reputation, qualifyingThreshold);
                        
                        return tierName;
                    }
                }
                
                // Fallback if no personality modifiers found
                LOGGER.warn("⚠️ No followerPersonalityModifiers found in AI config for deity {}", deity.getName());
            }
            
            // Final fallback - only if no AI config at all
            LOGGER.debug("🔍 No AI config or JSON stages for deity {}, using minimal fallback", deity.getId());
            return "Initiate"; // Single fallback instead of hardcoded progression
            
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
    
    // Track player progression levels to detect tier changes
    private static final Map<UUID, Map<ResourceLocation, String>> playerProgressionTracker = new ConcurrentHashMap<>();
    
    // Track which tier rewards have been given to prevent duplicates
    private static final Map<UUID, Map<ResourceLocation, Set<String>>> playerTierRewardsTracker = new ConcurrentHashMap<>();
    
    // Track highest tier achieved to prevent downgrade rewards
    private static final Map<UUID, Map<ResourceLocation, String>> playerHighestTierTracker = new ConcurrentHashMap<>();
    
    /**
     * 🎉 AUTOMATIC TIER PROGRESSION CONGRATULATION SYSTEM
     * 
     * Checks if player has achieved a new tier and automatically triggers deity congratulation
     * Uses actual AI config and deity progression data instead of hardcoded values
     */
    public static void checkAndHandleTierProgression(ServerPlayer player, ResourceLocation deityId) {
        try {
            DatapackDeity deity = DatapackDeityManager.getDeity(deityId);
            if (deity == null) {
                LOGGER.warn("🚫 TIER PROGRESSION CHECK: Deity {} not found", deityId);
                return;
            }

            String currentTier = getDynamicProgressionLevel(deity, player);
            UUID playerId = player.getUUID();
            double currentReputation = deity.getPlayerReputation(player);
            
            LOGGER.info("🔍 TIER PROGRESSION CHECK for {}: deity={}, currentTier='{}', reputation={}", 
                player.getName().getString(), deity.getName(), currentTier, (int)currentReputation);
            
            // Get player's progression tracking
            Map<ResourceLocation, String> playerTiers = playerProgressionTracker.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
            Map<ResourceLocation, String> playerHighestTiers = playerHighestTierTracker.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
            String previousTier = playerTiers.get(deityId);
            String highestTierEver = playerHighestTiers.get(deityId);
            
            LOGGER.info("🔄 TIER TRACKING for {}: previousTier='{}', highestTierEver='{}', currentTier='{}'", 
                player.getName().getString(), previousTier, highestTierEver, currentTier);

            // 🎯 SPECIAL CASE: Initial patron selection (no previous tier recorded)
            if (previousTier == null) {
                LOGGER.info("🆕 INITIAL TIER ASSIGNMENT: Player {} starting with tier '{}' for deity {} (rep: {})", 
                    player.getName().getString(), currentTier, deity.getName(), (int)currentReputation);
                
                // This is a new player with this deity - give initial tier rewards
                playerTiers.put(deityId, currentTier);
                playerHighestTiers.put(deityId, currentTier);
                
                // Trigger congratulation for initial tier (e.g., "Shadow Initiate")
                LOGGER.info("🚀 TRIGGERING INITIAL TIER CONGRATULATION: player={}, deity={}, initialTier='{}'", 
                    player.getName().getString(), deity.getName(), currentTier);
                
                triggerTierCongratulation(player, deity, null, currentTier);
                
                LOGGER.info("🏆 INITIAL TIER SET: {} for player {} with deity {}", 
                    currentTier, player.getName().getString(), deity.getName());
                return; // Early return for initial setup
            }

            // Check if this is a tier advancement
            if (!previousTier.equals(currentTier)) {
                // Log the tier change for debugging
                LOGGER.info("🔄 TIER CHANGE detected for player {}: '{}' → '{}' with deity {} (rep: {})", 
                    player.getName().getString(), previousTier, currentTier, deity.getName(), (int)currentReputation);
                
                // Check if this is actual advancement using proper tier comparison
                boolean isAdvancement = isTierAdvancement(previousTier, currentTier, deity);
                LOGGER.info("🎯 TIER ADVANCEMENT CHECK: previousTier='{}' → currentTier='{}', isAdvancement={}", 
                    previousTier, currentTier, isAdvancement);
                
                if (isAdvancement) {
                    LOGGER.info("🎉 TIER ADVANCEMENT confirmed for player {}: '{}' → '{}' with deity {}", 
                        player.getName().getString(), previousTier, currentTier, deity.getName());
                    
                    // Update tracking
                    playerTiers.put(deityId, currentTier);
                    
                    // Check if this is truly a new highest tier (prevents downgrade-upgrade reward abuse)
                    boolean isNewHighestTier = isNewHighestTier(currentTier, highestTierEver, deity);
                    LOGGER.info("🏆 NEW HIGHEST TIER CHECK: currentTier='{}', highestTierEver='{}', isNewHighest={}", 
                        currentTier, highestTierEver, isNewHighestTier);
                    
                    if (isNewHighestTier) {
                        // Update highest tier achieved
                        playerHighestTiers.put(deityId, currentTier);
                        
                        // Auto-trigger congratulation conversation with rewards
                        LOGGER.info("🚀 TRIGGERING TIER CONGRATULATION: player={}, deity={}, previousTier='{}', currentTier='{}'", 
                            player.getName().getString(), deity.getName(), previousTier, currentTier);
                        
                        triggerTierCongratulation(player, deity, previousTier, currentTier);
                        
                        LOGGER.info("🏆 NEW HIGHEST TIER achieved: {} for player {} with deity {}", 
                            currentTier, player.getName().getString(), deity.getName());
                    } else {
                        // Player re-achieved a tier they had before - just acknowledge, no rewards
                        player.sendSystemMessage(Component.literal("§6⟦ " + deity.getName() + " ⟧ §f" +
                            "You have regained your former rank of " + currentTier + "."));
                        
                        LOGGER.info("🔄 TIER RE-ACHIEVED (no rewards): {} for player {} with deity {}", 
                            currentTier, player.getName().getString(), deity.getName());
                    }
                } else {
                    // Just update the tracking without congratulation (demotion)
                    playerTiers.put(deityId, currentTier);
                    LOGGER.info("⬇️ Tier demotion for player {}: '{}' → '{}' with deity {}", 
                        player.getName().getString(), previousTier, currentTier, deity.getName());
                }
                
            } else if (previousTier == null) {
                // First time tracking - record tier and highest tier
                playerTiers.put(deityId, currentTier);
                
                LOGGER.info("📝 FIRST TIME TRACKING for {}: currentTier='{}', reputation={}", 
                    player.getName().getString(), currentTier, (int)currentReputation);
                
                // If starting above initiate tier, they should get progression rewards
                if (!currentTier.toLowerCase().contains("initiate")) {
                    playerHighestTiers.put(deityId, currentTier);
                    
                    // Get the proper first tier instead of hardcoding
                    String firstTier = getFirstTierForDeity(deity);
                    LOGGER.info("🚀 INITIAL HIGH TIER detected: {} for player {} with deity {} (vs firstTier: {})", 
                        currentTier, player.getName().getString(), deity.getName(), firstTier);
                    
                    triggerTierCongratulation(player, deity, firstTier, currentTier);
                } else {
                    playerHighestTiers.put(deityId, currentTier);
                    LOGGER.info("📝 Initial tier tracking for player {}: '{}' with deity {}", 
                        player.getName().getString(), currentTier, deity.getName());
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("❌ Error checking tier progression for player {}: {}", 
                player.getName().getString(), e.getMessage());
        }
    }
    
    /**
     * Determine if tier change represents advancement (not demotion)
     * Uses actual AI config reputation thresholds and deity progression stages
     */
    private static boolean isTierAdvancement(String previousTier, String currentTier, DatapackDeity deity) {
        if (previousTier == null || currentTier == null) return true;
        if (previousTier.equals(currentTier)) return false;
        
        try {
            // 🎯 FIXED: Use base deity progression stages instead of AI config
            Map<String, Object> stagesMap = deity.getProgressionStages();
            if (stagesMap != null && !stagesMap.isEmpty()) {
                
                // Get reputation thresholds for both tiers from base deity
                int previousThreshold = getTierReputationThreshold(previousTier, deity);
                int currentThreshold = getTierReputationThreshold(currentTier, deity);
                
                // Advancement means moving to a higher threshold
                boolean isAdvancement = currentThreshold > previousThreshold;
                
                LOGGER.debug("🔍 Tier comparison for {}: '{}' ({}rep) → '{}' ({}rep) = {} advancement",
                    deity.getName(), previousTier, previousThreshold, currentTier, currentThreshold, 
                    isAdvancement ? "IS" : "NOT");
                
                return isAdvancement;
            }
            
            // Fallback: Use basic alphabetical comparison if no progression data
            if (!stagesMap.isEmpty()) {
                double previousRep = getStageReputation(previousTier, stagesMap);
                double currentRep = getStageReputation(currentTier, stagesMap);
                return currentRep > previousRep;
            }
            
        } catch (Exception e) {
            LOGGER.warn("Error determining tier advancement: {}", e.getMessage());
        }
        
        // Safe fallback: Assume any change is advancement (this was the old behavior)
        return true;
    }
    
    /**
     * 🎯 FIXED: Get the reputation threshold for a specific tier from BASE DEITY progression
     * NO MORE AI CONFIG DEPENDENCY - Uses actual progression stages from /deities/ JSON
     */
    private static int getTierReputationThreshold(String tierName, DatapackDeity deity) {
        LOGGER.debug("🔍 Getting reputation threshold for tier: '{}' from base deity progression", tierName);
        
        // Get progression stages from base deity (loaded from /deities/ JSON)
        Map<String, Object> stagesMap = deity.getProgressionStages();
        
        if (stagesMap == null || stagesMap.isEmpty()) {
            LOGGER.warn("❌ No progression stages found for deity {}", deity.getId());
            return 0;
        }
        
        // Find the stage with matching title or ID
        for (Map.Entry<String, Object> entry : stagesMap.entrySet()) {
            String stageId = entry.getKey();
            Map<String, Object> stageData = (Map<String, Object>) entry.getValue();
            String stageTitle = (String) stageData.get("title");
            Integer reputation = (Integer) stageData.get("reputationRequired");
            
            // Match by title or by stage ID
            if (tierName.equals(stageTitle) || tierName.equals(stageId)) {
                LOGGER.debug("✅ Found tier '{}' → reputation threshold: {}", tierName, reputation);
                return reputation != null ? reputation : 0;
            }
        }
        
        LOGGER.warn("❌ Tier '{}' not found in base deity progression stages. Available: {}", 
            tierName, stagesMap.keySet());
        return 0;
    }
    
    /**
     * Helper method to score tier names for progression ordering
     * Lower scores = earlier tiers, Higher scores = later tiers
     */
    private static int getTierProgressionScore(String tierName) {
        String lower = tierName.toLowerCase();
        
        // Score based on tier progression keywords
        if (lower.contains("initiate") || lower.contains("novice") || lower.contains("beginner")) return 0;
        if (lower.contains("acolyte") || lower.contains("apprentice")) return 1;
        if (lower.contains("priest") || lower.contains("adept")) return 2;  
        if (lower.contains("master") || lower.contains("void") || lower.contains("high")) return 3;
        if (lower.contains("champion") || lower.contains("supreme") || lower.contains("ultimate")) return 4;
        
        // Default scoring based on common progression words
        if (lower.contains("shadow") && !lower.contains("champion")) return 0; // Shadow Initiate
        if (lower.contains("dark") && !lower.contains("master")) return 1; // Dark Acolyte  
        
        return 2; // Default middle tier
    }
    
    /**
     * Map a reputation threshold to the corresponding tier name from JSON
     * This creates the connection between reputation_thresholds and followerPersonalityModifiers
     */
    private static String mapThresholdToTierName(int threshold, Map<String, String> personalityModifiers, Map<Integer, String> reputationBehaviors) {
        
        // Get all thresholds in sorted order
        List<Integer> sortedThresholds = reputationBehaviors.keySet().stream()
            .sorted()
            .collect(java.util.stream.Collectors.toList());
            
        // Get all tier names in progression order
        List<String> sortedTierNames = personalityModifiers.keySet().stream()
            .sorted((a, b) -> {
                int scoreA = getTierProgressionScore(a);
                int scoreB = getTierProgressionScore(b);
                return Integer.compare(scoreA, scoreB);
            })
            .collect(java.util.stream.Collectors.toList());
        
        LOGGER.debug("🔗 Mapping threshold {} to tier name. Thresholds: {}, Tiers: {}", 
            threshold, sortedThresholds, sortedTierNames);
        
        // Find the index of this threshold
        int thresholdIndex = sortedThresholds.indexOf(threshold);
        
        if (thresholdIndex >= 0 && thresholdIndex < sortedTierNames.size()) {
            String tierName = sortedTierNames.get(thresholdIndex);
            LOGGER.debug("✅ Mapped threshold {} (index {}) to tier '{}'", threshold, thresholdIndex, tierName);
            return tierName;
        }
        
        // Fallback: return the first tier name if mapping fails
        if (!sortedTierNames.isEmpty()) {
            String fallbackTier = sortedTierNames.get(0);
            LOGGER.warn("⚠️ Could not map threshold {} to tier, using fallback: '{}'", threshold, fallbackTier);
            return fallbackTier;
        }
        
        // Ultimate fallback
        LOGGER.error("❌ No tier names available for mapping threshold {}", threshold);
        return "Initiate";
    }
    
    /**
     * Get reputation requirement for a stage from progression stages map
     */
    private static double getStageReputation(String stageName, Map<String, Object> stagesMap) {
        for (Map.Entry<String, Object> entry : stagesMap.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(stageName) && entry.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> stageData = (Map<String, Object>) entry.getValue();
                Object repReq = stageData.get("reputationRequired");
                if (repReq instanceof Number) {
                    return ((Number) repReq).doubleValue();
                }
            }
        }
        return 0; // Default
    }
    
    /**
     * Check if the current tier is higher than the player's previous highest
     */
    private static boolean isNewHighestTier(String currentTier, String highestTierEver, DatapackDeity deity) {
        if (highestTierEver == null) return true; // First time reaching any tier
        if (currentTier.equals(highestTierEver)) return false; // Same as highest
        
        // Use the same logic as tier advancement to compare tiers
        return isTierAdvancement(highestTierEver, currentTier, deity);
    }
    
    /**
     * 🎭 AUTOMATIC TIER CONGRATULATION CONVERSATION
     * 
     * Triggers an automatic deity conversation when player advances in tier
     */
    private static void triggerTierCongratulation(ServerPlayer player, DatapackDeity deity, String previousTier, String newTier) {
        try {
            LOGGER.info("🎊 TRIGGER TIER CONGRATULATION: player={}, deity={}, previousTier='{}', newTier='{}'", 
                player.getName().getString(), deity.getName(), previousTier, newTier);
            
            // Create congratulation message prompt
            String congratulationPrompt = String.format(
                "The player %s has just advanced from %s to %s tier with you! " +
                "Congratulate them on their progression and acknowledge their new title. " +
                "Be proud and offer a blessing or reward appropriate for their new rank. " +
                "Make this feel like a special achievement moment.",
                player.getName().getString(), previousTier, newTier
            );
            
            LOGGER.info("🎭 Generated congratulation prompt: {}", congratulationPrompt);
            
            // Send immediate notification
            player.sendSystemMessage(Component.literal("§6✨ " + deity.getName() + " senses your growing devotion... ✨"));
            LOGGER.info("📧 Sent notification to player: 'senses your growing devotion'");
            
            // Trigger AI conversation with congratulation context
            AIDeityConfig aiConfig = AIDeityManager.getInstance().getAIConfig(deity.getId());
            if (aiConfig != null) {
                LOGGER.info("✅ AI config found for deity {}, starting congratulation conversation", deity.getName());
                
                // Start conversation automatically
                activeConversations.put(player.getUUID(), deity.getId());
                // 🔥 CRITICAL FIX: Initialize conversation history for tier congratulations
                conversationHistory.put(player.getUUID(), new ArrayList<>());
                
                // Start effigy effects for tier congratulations
                EffigyTileEntity effigy = EffigyEffectsManager.findNearbyEffigy(player, 10.0);
                if (effigy != null) {
                    EffigyEffectsManager.startConversationEffects(player, effigy.getBlockPos(), deity.getId());
                    LOGGER.info("🔮 Started tier congratulation effigy effects for deity {} at {}", deity.getId(), effigy.getBlockPos());
                } else {
                    LOGGER.warn("🔮 No effigy found for tier congratulation - player {} at {}", player.getName().getString(), player.blockPosition());
                }
                LOGGER.info("🗣️ Added player {} to active conversations with deity {} (with history initialization)", 
                    player.getName().getString(), deity.getName());
                
                // Process the congratulation with callback for rewards execution
                LOGGER.info("🤖 Processing deity conversation with congratulation prompt...");
                processDeityConversation(player, deity.getId(), congratulationPrompt, () -> {
                    // This callback executes AFTER the AI action bar message completes
                    try {
                        LOGGER.info("🎁 [POST-MESSAGE] Executing tier advancement rewards for tier: {}", newTier);
                        executeTierAdvancementRewards(player, deity, newTier);
                        
                        // Send completion message after rewards
                        player.sendSystemMessage(Component.literal(
                            "§6⟦ " + deity.getName() + " ⟧ §f" +
                            "Your advancement has been acknowledged. Go forth with your new power, " + newTier + "."));
                        
                        // Close the conversation
                        endConversation(player);
                        
                        LOGGER.info("🔚 Auto-closed tier advancement conversation for player {} with {}", 
                            player.getName().getString(), deity.getName());
                            
                    } catch (Exception e) {
                        LOGGER.error("❌ Error in post-message tier advancement rewards: {}", e.getMessage());
                    }
                });
                
            } else {
                LOGGER.error("❌ AI config not found for deity {}, cannot trigger congratulation", deity.getName());
                
                // Fallback: Send a simple congratulation message
                player.sendSystemMessage(Component.literal("§6⟦ " + deity.getName() + " ⟧ §f" +
                    "You have advanced to " + newTier + "! Your devotion is acknowledged."));
            }
            
        } catch (Exception e) {
            LOGGER.error("❌ Error triggering tier congratulation for player {}: {}", 
                player.getName().getString(), e.getMessage());
        }
    }
    
    /**
     * Execute tier-specific advancement rewards with proper tracking to prevent duplicates
     * 🔥 FIXED: Now reads rewards from JSON progression stages instead of using hardcoded fallbacks
     */
    private static void executeTierAdvancementRewards(ServerPlayer player, DatapackDeity deity, String newTier) {
        try {
            UUID playerId = player.getUUID();
            ResourceLocation deityId = deity.getId();
            
            // Get or create tier rewards tracking
            Map<ResourceLocation, Set<String>> playerRewards = playerTierRewardsTracker.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
            Set<String> givenRewards = playerRewards.computeIfAbsent(deityId, k -> new HashSet<>());
            
            // 🔥 SPECIAL LOGIC: For initial tier assignments during patron selection, be more permissive
            // Multiple calls can happen during the patron selection process, but we want to ensure
            // the player gets their initial tier rewards at least once
            boolean isInitialAssignment = !playerTierRewardsTracker.containsKey(playerId) || 
                                        !playerTierRewardsTracker.get(playerId).containsKey(deityId) ||
                                        playerTierRewardsTracker.get(playerId).get(deityId).isEmpty();
            
            if (isInitialAssignment) {
                LOGGER.info("🎉 INITIAL TIER ASSIGNMENT: Player {} getting first-time rewards for '{}' with deity {} (allowing despite previous calls)", 
                    player.getName().getString(), newTier, deity.getName());
            } else {
                // For non-initial assignments, enforce strict duplicate checking
                if (givenRewards.contains(newTier)) {
                    LOGGER.info("🔒 Tier rewards for '{}' already given to player {} - skipping duplicates", 
                        newTier, player.getName().getString());
                    return;
                }
            }
            
            // 🔥 SIMPLIFIED: Use DatapackDeity's existing getStageRewards method
            List<String> tierCommands = deity.getStageRewards(newTier);
            
            // DEBUG: Check what's available vs what's requested
            LOGGER.info("🔍 DEBUG - Looking for rewards for tier: '{}' in deity: {}", newTier, deity.getId());
            LOGGER.info("🔍 DEBUG - Available stage rewards keys: {}", deity.getProgressionStages().keySet());
            LOGGER.info("🔍 DEBUG - Found tier commands: {}", tierCommands != null ? tierCommands.size() + " commands" : "null");
            
            if (tierCommands != null && !tierCommands.isEmpty()) {
                LOGGER.info("🎁 Executing {} JSON-defined tier advancement rewards for player {} (tier: '{}')", 
                    tierCommands.size(), player.getName().getString(), newTier);
                
                // Commands are now in proper Minecraft format, execute directly
                executeCommands(player, deity.getId(), tierCommands);
                
                // Mark this tier's rewards as given
                givenRewards.add(newTier);
                
                if (isInitialAssignment) {
                    LOGGER.info("✅ Successfully completed INITIAL tier rewards for player {} - tier '{}' with deity {}", 
                        player.getName().getString(), newTier, deity.getName());
                } else {
                    LOGGER.info("✅ JSON tier rewards marked as given for player {} tier '{}'", 
                        player.getName().getString(), newTier);
                }
                    
                // Send player notification about tier advancement
                player.sendSystemMessage(Component.literal("§6✨ " + deity.getName() + 
                    " grants you advancement rewards for reaching " + newTier + "! ✨"));
                    
            } else {
                LOGGER.warn("❌ No rewards found in JSON for tier '{}' - using fallback", newTier);
                executeFallbackTierRewards(player, newTier);
                givenRewards.add(newTier);
            }
            
        } catch (Exception e) {
            LOGGER.error("❌ Error executing tier advancement rewards for player {}: {}", 
                player.getName().getString(), e.getMessage());
        }
    }
    
    /**
     * Execute fallback tier rewards when AI config is not available
     */
    private static void executeFallbackTierRewards(ServerPlayer player, String newTier) {
        List<String> fallbackCommands = new ArrayList<>();
        String tierLower = newTier.toLowerCase();
        
        // Basic fallback rewards based on tier name
        if (tierLower.contains("champion") || tierLower.contains("master") || tierLower.contains("void")) {
            fallbackCommands.add("give {player} minecraft:experience_bottle 10");
            fallbackCommands.add("give {player} minecraft:golden_apple 2");
            fallbackCommands.add("give {player} minecraft:diamond 1");
        } else if (tierLower.contains("priest") || tierLower.contains("high") || tierLower.contains("dark")) {
            fallbackCommands.add("give {player} minecraft:experience_bottle 5");
            fallbackCommands.add("give {player} minecraft:golden_apple 1");
        } else if (tierLower.contains("acolyte") || tierLower.contains("adept")) {
            fallbackCommands.add("give {player} minecraft:experience_bottle 3");
        } else {
            fallbackCommands.add("give {player} minecraft:experience_bottle 1");
        }
        
        if (!fallbackCommands.isEmpty()) {
            executeCommands(player, null, fallbackCommands);
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
            
            LOGGER.info("🔥 BLESSING DEBUG: Player {} has {} reputation with {}, tier: {}", 
                player.getName().getString(), reputation, deity.getName(), progressionLevel);
            
            // Check if player explicitly requested something
            String lowerMessage = playerMessage.toLowerCase();
            boolean explicitRequest = lowerMessage.contains("give") || lowerMessage.contains("bless") || 
                                    lowerMessage.contains("grant") || lowerMessage.contains("help") ||
                                    lowerMessage.contains("need") || lowerMessage.contains("want");
            
            LOGGER.info("🔥 BLESSING DEBUG: Message '{}' contains explicit request: {}", playerMessage, explicitRequest);
            
            // 🔥 FIXED: Use JSON configuration instead of hardcoded reputation threshold
            // Get the reputation requirement from AI deity config
            AIDeityConfig aiConfig = AIDeityManager.getInstance().getAIConfig(deity.getId());
            if (aiConfig != null) {
                // Get the conversation prayer config to check reputation_required
                PrayerAIConfig conversationConfig = aiConfig.getPrayerConfig("conversation");
                if (conversationConfig != null) {
                    int requiredReputation = conversationConfig.reputation_required;
                    
                    // No blessings for non-explicit requests if under required reputation
                    if (!explicitRequest && reputation < requiredReputation) {
                        LOGGER.info("🚫 Blocked non-explicit blessing request for player {} ({}rep < {}req): '{}'", 
                            player.getName().getString(), (int)reputation, requiredReputation, playerMessage);
                        return false;
                    }
                    
                    LOGGER.info("🔥 REPUTATION CHECK PASSED: Player {} has {}rep >= {}req (from JSON config)", 
                        player.getName().getString(), (int)reputation, requiredReputation);
                } else {
                    LOGGER.warn("No conversation config found for deity {}, allowing blessing", deity.getId());
                }
            } else {
                LOGGER.warn("No AI config found for deity {}, using legacy hardcoded threshold", deity.getId());
                // Fallback to old hardcoded logic only if config is missing
                if (!explicitRequest && reputation < 25) {
                    LOGGER.info("🚫 FALLBACK: Blocked non-explicit blessing request for low-tier player {}: '{}'", 
                        player.getName().getString(), playerMessage);
                    return false;
                }
            }
            
            // Check recent blessing cooldown (simple time-based)
            long currentTime = System.currentTimeMillis();
            String cooldownKey = player.getUUID() + "_" + deity.getId();
            Long lastBlessing = lastBlessingTimes.get(cooldownKey);
            
            if (lastBlessing != null) {
                long timeSince = currentTime - lastBlessing;
                long cooldownMs = getBlessingCooldown(progressionLevel);
                
                if (timeSince < cooldownMs) {
                    // Calculate remaining time in minutes and seconds
                    long remainingMs = cooldownMs - timeSince;
                    long remainingMinutes = remainingMs / 60000;
                    long remainingSeconds = (remainingMs % 60000) / 1000;
                    
                    // Inform the player about the cooldown
                    if (remainingMinutes > 0) {
                        player.sendSystemMessage(Component.literal(
                            "§e⟦ " + deity.getName() + " ⟧ §7The divine energies still resonate from your last blessing. " +
                            "§cWait " + remainingMinutes + "m " + remainingSeconds + "s before requesting another."));
                    } else {
                        player.sendSystemMessage(Component.literal(
                            "§e⟦ " + deity.getName() + " ⟧ §7The divine energies are still settling. " +
                            "§cWait " + remainingSeconds + " seconds before requesting another blessing."));
                    }
                    
                    LOGGER.info("🕒 Blessing on cooldown for {}: {}ms remaining", 
                        player.getName().getString(), remainingMs);
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
     * Get maximum commands allowed based on AI deity config, NOT hardcoded values
     * This respects the JSON configuration for each prayer type
     */
    private static int getMaxCommandsForPrayerType(ResourceLocation deityId, String prayerType) {
        try {
            AIDeityConfig aiConfig = AIDeityManager.getInstance().getAIConfig(deityId);
            if (aiConfig == null) {
                LOGGER.warn("No AI config found for deity {}, using default limit", deityId);
                return 1; // Safe fallback
            }
            
            // Get the specific prayer config for this type
            PrayerAIConfig prayerConfig = aiConfig.getPrayerConfig(prayerType);
            if (prayerConfig != null) {
                // Use the configured max_commands from JSON
                int configuredMax = com.bluelotuscoding.eidolonunchained.ai.EffectiveAIConfig
                    .getMaxCommands(deityId, prayerType, prayerConfig);
                
                LOGGER.debug("Using configured max commands for {}/{}: {}", 
                    deityId, prayerType, configuredMax);
                return configuredMax;
            }
            
            LOGGER.warn("No prayer config found for {}/{}, using fallback", deityId, prayerType);
            return 1; // Safe fallback
            
        } catch (Exception e) {
            LOGGER.error("Error getting max commands for {}/{}: {}", deityId, prayerType, e.getMessage());
            return 1; // Safe fallback
        }
    }
    
    /**
     * @deprecated Use getMaxCommandsForPrayerType() instead to respect JSON configuration
     */
    @Deprecated
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
    
    // 🔧 DEBUG METHODS FOR TESTING TIER PROGRESSION
    
    /**
     * Clear all tier progression tracking for a specific player and deity (for testing)
     */
    public static void clearPlayerTierTracking(UUID playerId, ResourceLocation deityId) {
        Map<ResourceLocation, String> playerTiers = playerProgressionTracker.get(playerId);
        if (playerTiers != null) {
            playerTiers.remove(deityId);
            if (playerTiers.isEmpty()) {
                playerProgressionTracker.remove(playerId);
            }
        }
        
        Map<ResourceLocation, Set<String>> playerRewards = playerTierRewardsTracker.get(playerId);
        if (playerRewards != null) {
            playerRewards.remove(deityId);
            if (playerRewards.isEmpty()) {
                playerTierRewardsTracker.remove(playerId);
            }
        }
        
        Map<ResourceLocation, String> playerHighestTiers = playerHighestTierTracker.get(playerId);
        if (playerHighestTiers != null) {
            playerHighestTiers.remove(deityId);
            if (playerHighestTiers.isEmpty()) {
                playerHighestTierTracker.remove(playerId);
            }
        }
        
        LOGGER.info("🧹 Cleared tier progression tracking for player {} with deity {}", playerId, deityId);
    }
    
    /**
     * Clear all tier progression tracking for a specific player (all deities)
     */
    public static void clearAllPlayerTierTracking(UUID playerId) {
        playerProgressionTracker.remove(playerId);
        playerTierRewardsTracker.remove(playerId);
        playerHighestTierTracker.remove(playerId);
        
        LOGGER.info("🧹 Cleared ALL tier progression tracking for player {}", playerId);
    }
    
    /**
     * Get debugging info about a player's tier progression tracking
     */
    public static String getPlayerTierTrackingDebugInfo(UUID playerId, ResourceLocation deityId) {
        StringBuilder info = new StringBuilder();
        
        Map<ResourceLocation, String> playerTiers = playerProgressionTracker.get(playerId);
        String currentTrackedTier = playerTiers != null ? playerTiers.get(deityId) : "none";
        
        Map<ResourceLocation, Set<String>> playerRewards = playerTierRewardsTracker.get(playerId);
        Set<String> rewardsReceived = playerRewards != null ? playerRewards.get(deityId) : null;
        
        Map<ResourceLocation, String> playerHighestTiers = playerHighestTierTracker.get(playerId);
        String highestTier = playerHighestTiers != null ? playerHighestTiers.get(deityId) : "none";
        
        info.append("Current tracked tier: ").append(currentTrackedTier).append("\n");
        info.append("Highest tier achieved: ").append(highestTier).append("\n");
        info.append("Rewards received: ").append(rewardsReceived != null ? rewardsReceived.toString() : "none");
        
        return info.toString();
    }
    
    /**
     * Get the first/lowest tier for a deity based on their progression system
     */
    /**
     * Get the first/lowest tier for a deity based on their JSON configuration
     * NO HARDCODING - reads directly from JSON
     */
    private static String getFirstTierForDeity(DatapackDeity deity) {
        try {
            AIDeityConfig aiConfig = AIDeityManager.getInstance().getAIConfig(deity.getId());
            if (aiConfig != null && aiConfig.getReputationBehaviors() != null && !aiConfig.getReputationBehaviors().isEmpty()) {
                
                // Get follower personality modifiers (tier names) from JSON
                if (aiConfig.patron_config != null && aiConfig.patron_config.followerPersonalityModifiers != null) {
                    Map<String, String> personalityModifiers = aiConfig.patron_config.followerPersonalityModifiers;
                    Map<Integer, String> reputationBehaviors = aiConfig.getReputationBehaviors();
                    
                    // Find the lowest reputation threshold
                    int lowestThreshold = reputationBehaviors.keySet().stream()
                        .mapToInt(Integer::intValue)
                        .min()
                        .orElse(0);
                    
                    LOGGER.debug("🔍 Lowest reputation threshold for deity {}: {}", deity.getName(), lowestThreshold);
                    
                    // Map this threshold to the corresponding tier name
                    String firstTier = mapThresholdToTierName(lowestThreshold, personalityModifiers, reputationBehaviors);
                    
                    LOGGER.info("✅ First tier for deity {}: '{}' (threshold: {})", 
                        deity.getName(), firstTier, lowestThreshold);
                    
                    return firstTier;
                }
            }
            
            // Check for datapack stages if no AI config
            if (deity.getProgressionStages() != null && !deity.getProgressionStages().isEmpty()) {
                // Find the stage with the lowest reputation requirement
                String firstTier = "initiate";
                int lowestRep = Integer.MAX_VALUE;
                
                for (java.util.Map.Entry<String, Object> entry : deity.getProgressionStages().entrySet()) {
                    Object stage = entry.getValue();
                    if (stage instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> stageMap = (java.util.Map<String, Object>) stage;
                        Object repReq = stageMap.get("reputation_required");
                        if (repReq instanceof Number) {
                            int repValue = ((Number) repReq).intValue();
                            if (repValue < lowestRep) {
                                lowestRep = repValue;
                                firstTier = entry.getKey();
                            }
                        }
                    }
                }
                return firstTier;
            }
            
        } catch (Exception e) {
            LOGGER.warn("❌ Error getting first tier for deity {}: {}", deity.getName(), e.getMessage());
        }
        
        // Minimal fallback
        return "Initiate";
    }
    
    /**
     * Helper to get tier name for reputation threshold
     */
    private static String getTierNameForThreshold(int threshold) {
        if (threshold >= 100) return "champion";
        else if (threshold >= 75) return "master";
        else if (threshold >= 50) return "priest";
        else if (threshold >= 25) return "acolyte";
        else return "initiate";
    }
    
    // Static map to track blessing cooldowns
    private static final java.util.Map<String, Long> lastBlessingTimes = new java.util.concurrent.ConcurrentHashMap<>();

    

    /**
     * Public helper for AI context: get remaining blessing cooldown (ms) for this player/deity.
     * Returns 0 when available now or if no cooldown is tracked yet.
     */
    public static long getBlessingCooldownRemainingMs(ServerPlayer player, ResourceLocation deityId) {
        try {
            if (player == null || deityId == null) return 0L;
            DatapackDeity deity = DatapackDeityManager.getDeity(deityId);
            if (deity == null) return 0L;
            String progressionLevel = getDynamicProgressionLevel(deity, player);
            long cooldownMs = getBlessingCooldown(progressionLevel);
            String cooldownKey = player.getUUID() + "_" + deityId;
            Long last = lastBlessingTimes.get(cooldownKey);
            if (last == null) return 0L;
            long elapsed = System.currentTimeMillis() - last;
            long remaining = cooldownMs - elapsed;
            return Math.max(remaining, 0L);
        } catch (Exception ignored) { return 0L; }
    }

    /** Format milliseconds as compact Xm Ys string for prompts */
    public static String formatShortDuration(long ms) {
        if (ms <= 0) return "0s";
        long totalSeconds = ms / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        if (minutes > 0) return minutes + "m " + seconds + "s";
        return seconds + "s";
    }
    
    /**
     * Build comprehensive AI context with full game state awareness
     */
    private static String buildComprehensiveAIContext(PlayerContext context, ResourceLocation deityId, ServerPlayer player) {
        StringBuilder aiContext = new StringBuilder();
        
        // Core identifiers
        aiContext.append("DEITY: ").append(deityId.toString()).append("\n");
        aiContext.append("PLAYER: ").append(context.playerName).append(" (").append(player.getStringUUID()).append(")\n");
        aiContext.append("REPUTATION: ").append(context.reputation).append(" (").append(context.progressionLevel).append(")\n\n");
        
        // Location & Environment  
        aiContext.append("=== LOCATION & ENVIRONMENT ===\n");
        aiContext.append("Position: ").append(context.location).append(" (Y-Level: ").append(context.yLevel).append(")\n");
        aiContext.append("Dimension: ").append(context.dimension).append("\n");
        aiContext.append("Biome: ").append(context.biome).append("\n");
        aiContext.append("Time: ").append(context.timeOfDay).append(" | Weather: ").append(context.weather).append("\n");
        aiContext.append("Light Level: ").append(context.lightLevel).append(" | Underground: ").append(context.underground).append("\n\n");
        
        // Player State
        aiContext.append("=== PLAYER STATE ===\n");
        aiContext.append("Health: ").append(context.health).append("/").append(context.maxHealth).append(" | Hunger: ").append(context.hunger).append("/20\n");
        aiContext.append("XP Level: ").append(context.xpLevel).append("\n");
        
        List<String> conditions = new ArrayList<>();
        if (context.isOnFire) conditions.add("ON FIRE");
        if (context.isInWater) conditions.add("IN WATER");  
        if (context.isFlying) conditions.add("FLYING");
        if (context.isSneaking) conditions.add("SNEAKING");
        if (context.isSwimming) conditions.add("SWIMMING");
        if (!conditions.isEmpty()) {
            aiContext.append("Conditions: ").append(String.join(", ", conditions)).append("\n");
        }
        
        if (!context.activeEffects.isEmpty()) {
            aiContext.append("Active Effects: ").append(String.join(", ", context.activeEffects)).append("\n");
        }
        aiContext.append("\n");
        
        // Equipment & Inventory
        aiContext.append("=== EQUIPMENT & INVENTORY ===\n");
        aiContext.append("Main Hand: ").append(context.mainHandItem).append("\n");
        aiContext.append("Off Hand: ").append(context.offHandItem).append("\n");
        if (!context.equippedArmor.isEmpty()) {
            aiContext.append("Armor: ").append(String.join(", ", context.equippedArmor)).append("\n");
        }
        aiContext.append(context.inventorySummary).append("\n");
        if (!context.notableItems.isEmpty()) {
            aiContext.append("Notable Items: ").append(String.join(", ", context.notableItems)).append("\n");
        }
        aiContext.append("\n");
        
        // Nearby Environment
        aiContext.append("=== NEARBY ENVIRONMENT ===\n");
        if (!context.nearbyBlocks.isEmpty()) {
            aiContext.append("Nearby Blocks: ").append(String.join(", ", context.nearbyBlocks)).append("\n");
        }
        if (!context.nearbyEntities.isEmpty()) {
            aiContext.append("Nearby Entities: ").append(String.join(", ", context.nearbyEntities)).append("\n");
        }
        
        List<String> environmentalNotes = new ArrayList<>();
        if (context.nearWater) environmentalNotes.add("water nearby");
        if (context.nearLava) environmentalNotes.add("lava nearby");  
        if (context.nearFire) environmentalNotes.add("fire nearby");
        if (context.hasNearbyBed) environmentalNotes.add("bed nearby");
        if (context.hasNearbyWorkstation) environmentalNotes.add("workstation nearby");
        
        if (!environmentalNotes.isEmpty()) {
            aiContext.append("Environment Notes: ").append(String.join(", ", environmentalNotes)).append("\n");
        }
        aiContext.append("\n");
        
        // Deity-specific context
        aiContext.append("=== DEITY CONTEXT ===\n");
        if (context.lastChantPerformed != null) {
            aiContext.append("Last Chant: ").append(context.lastChantPerformed).append("\n");
        }
        if (context.lastPrayerType != null) {
            aiContext.append("Last Prayer Type: ").append(context.lastPrayerType).append("\n");
        }
        if (!context.recentActions.isEmpty()) {
            aiContext.append("Recent Actions: ").append(String.join(", ", context.recentActions)).append("\n");
        }
        
        aiContext.append("\n=== INSTRUCTIONS ===\n");
        aiContext.append("You have FULL awareness of the player's state, location, inventory, and surroundings.\n");
        aiContext.append("Use this information to provide contextually appropriate responses.\n");
        aiContext.append("React to their environment, condition, equipment, and circumstances.\n");
        aiContext.append("You are omniscient within the game world - you can see everything the player can and more.\n");
        
        return aiContext.toString();
    }

    /**
     * Build contextual information about nearby mobs that this deity supports/controls
     */
    private static String buildNearbyMobContext(ServerPlayer player, java.util.List<String> supportedMobIds) {
        StringBuilder context = new StringBuilder();

        // Search for nearby entities within 32 blocks
        double searchRadius = 32.0;
        java.util.List<net.minecraft.world.entity.Entity> nearbyEntities = player.level().getEntities(
            player,
            player.getBoundingBox().inflate(searchRadius),
            entity -> entity instanceof net.minecraft.world.entity.LivingEntity
        );

        java.util.Map<String, Integer> supportedMobCounts = new java.util.HashMap<>();
        java.util.Map<String, Integer> otherMobCounts = new java.util.HashMap<>();

        for (net.minecraft.world.entity.Entity entity : nearbyEntities) {
            if (entity instanceof net.minecraft.world.entity.LivingEntity livingEntity) {
                String entityId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                    .getKey(livingEntity.getType()).toString();

                if (supportedMobIds.contains(entityId)) {
                    supportedMobCounts.put(entityId, supportedMobCounts.getOrDefault(entityId, 0) + 1);
                } else {
                    // Only track hostile mobs and important entities for context
                    if (livingEntity instanceof net.minecraft.world.entity.monster.Monster ||
                        livingEntity instanceof net.minecraft.world.entity.animal.Animal ||
                        livingEntity instanceof net.minecraft.world.entity.npc.AbstractVillager) {
                        otherMobCounts.put(entityId, otherMobCounts.getOrDefault(entityId, 0) + 1);
                    }
                }
            }
        }

        // Add supported creatures context
        if (!supportedMobCounts.isEmpty()) {
            context.append("YOUR SUPPORTED CREATURES NEARBY:\n");
            for (java.util.Map.Entry<String, Integer> entry : supportedMobCounts.entrySet()) {
                String entityName = entry.getKey().replace("minecraft:", "");
                context.append("- ").append(entry.getValue()).append(" ").append(entityName);
                if (entry.getValue() > 1) context.append("s");
                context.append(" (under your divine influence)\n");
            }
            context.append("IMPORTANT: These creatures are connected to your divine essence. ");
            context.append("You can sense their presence and may reference them in conversation. ");
            context.append("They represent your power in the mortal realm.\n\n");
        }

        // Add other notable creatures for context
        if (!otherMobCounts.isEmpty() && otherMobCounts.size() <= 5) { // Limit to avoid spam
            context.append("OTHER CREATURES NEARBY:\n");
            for (java.util.Map.Entry<String, Integer> entry : otherMobCounts.entrySet()) {
                String entityName = entry.getKey().replace("minecraft:", "");
                context.append("- ").append(entry.getValue()).append(" ").append(entityName);
                if (entry.getValue() > 1) context.append("s");
                context.append("\n");
            }
            context.append("You are aware of these creatures but they are not directly under your influence.\n");
        }

        return context.toString();
    }


}
