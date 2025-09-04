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
                
                // 🔥 ENHANCED COMMAND EXTRACTION AND EXECUTION
                // Use the enhanced command extractor to detect and execute natural language commands
                List<String> extractedCommands = com.bluelotuscoding.eidolonunchained.integration.ai.EnhancedCommandExtractor
                    .extractAndConvertCommands(rawResponse, player);
                
                int commandsExecuted = 0;
                if (!extractedCommands.isEmpty()) {
                    // Execute enhanced extracted commands
                    commandsExecuted = com.bluelotuscoding.eidolonunchained.integration.ai.EnhancedCommandExtractor
                        .executeCommands(extractedCommands, player);
                    
                    LOGGER.info("🔥 Enhanced AI extraction executed {} commands for {}: {}", 
                        commandsExecuted, player.getName().getString(), extractedCommands);
                }
                
                // Clean response for display (remove command patterns but keep the conversational parts)
                String cleanedResponse = com.bluelotuscoding.eidolonunchained.integration.ai.EnhancedCommandExtractor
                    .cleanResponseForDisplay(rawResponse);
                
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
        
        // Use AI deity configuration behavior rules for context
        if (aiConfig != null) {
            String reputationBehavior = aiConfig.getReputationBehavior(reputation);
            if (reputationBehavior != null) {
                prompt.append(reputationBehavior).append(" ");
            }
            
            // Add progression stage context from AI config
            if (aiConfig.patron_config.followerPersonalityModifiers.containsKey(progressionLevel)) {
                prompt.append(aiConfig.patron_config.followerPersonalityModifiers.get(progressionLevel)).append(" ");
            }
        }
        
        prompt.append("This player currently holds the rank of '").append(progressionLevel)
              .append("' with you (reputation: ").append((int)reputation).append("). ");
        
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
        
        // Add AI deity configuration-based command guidelines (CONTEXTUAL)
        if (aiConfig != null && aiConfig.prayer_configs.containsKey("conversation")) {
            PrayerAIConfig prayerConfig = aiConfig.prayer_configs.get("conversation");
            
            prompt.append("\n\n=== DIVINE INTERVENTION GUIDELINES ===\n");
            prompt.append("You have the power to use divine commands, but use them wisely:\n");
            prompt.append("- For simple greetings, questions, or casual conversation: Use NO commands\n");
            prompt.append("- For direct requests for help or significant moments: Use 1 command maximum\n");
            prompt.append("- Complex requests requiring multiple actions: Still limit to 1 primary command\n");
            prompt.append("- Focus on meaningful dialogue first, divine intervention second\n");
            
            // Add item knowledge context
            if (prayerConfig.additional_prompts != null && !prayerConfig.additional_prompts.isEmpty()) {
                prompt.append("\n=== WORLD KNOWLEDGE ===\n");
                for (String additionalPrompt : prayerConfig.additional_prompts) {
                    prompt.append("- ").append(additionalPrompt).append("\n");
                }
            }
            
            // Add example commands for reference
            if (prayerConfig.reference_commands != null && !prayerConfig.reference_commands.isEmpty()) {
                prompt.append("\n=== COMMAND EXAMPLES (use sparingly) ===\n");
                for (String refCommand : prayerConfig.reference_commands) {
                    prompt.append("- ").append(refCommand).append("\n");
                }
            }
            
            prompt.append("\nRemember: The goal is immersive conversation, not showcasing divine power.\n");
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
        prompt.append("3. COMMAND EXECUTION: When granting items/effects, use proper item IDs from the registry\n");
        prompt.append("4. CONTEXTUAL RESPONSE: Adapt to the player's current situation, health, and needs\n");
        prompt.append("5. REPUTATION-BASED REWARDS: Higher reputation = better rewards and privileges\n");
        prompt.append("6. AVOID REPETITION: Each response should be unique and situational\n");
        prompt.append("7. EXECUTE REQUESTS: When asked for specific items, grant them if worthy\n");
        prompt.append("8. DYNAMIC JUDGMENT: Consider player's immediate context for appropriate responses\n");
        prompt.append("\nRemember: Be adaptive, not scripted! Respond uniquely to each situation!\n");
        
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
        context.append("- Basic Materials: minecraft:iron_ingot, minecraft:gold_ingot, minecraft:diamond, minecraft:emerald\n");
        context.append("- Food: minecraft:bread, minecraft:cooked_beef, minecraft:golden_apple, minecraft:enchanted_golden_apple\n");
        context.append("- Tools: minecraft:iron_sword, minecraft:diamond_pickaxe, minecraft:bow, minecraft:crossbow\n");
        context.append("- Building: minecraft:stone, minecraft:oak_log, minecraft:iron_ore, minecraft:diamond_ore\n");
        context.append("- Potions: minecraft:potion, minecraft:healing_potion, minecraft:strength_potion\n");
        
        // Eidolon-specific items with proper IDs
        context.append("\nEIDOLON MAGICAL ITEMS:\n");
        context.append("- Soul Magic: eidolon:soul_shard, eidolon:lesser_soul_gem, eidolon:greater_soul_gem\n");
        context.append("- Dark Magic: eidolon:death_essence, eidolon:zombie_heart, eidolon:wraith_heart, eidolon:shadow_gem\n");
        context.append("- Arcane Materials: eidolon:arcane_gold_ingot, eidolon:pewter_ingot, eidolon:silver_ingot\n");
        context.append("- Ritual Components: eidolon:wicked_weave, eidolon:tattered_cloth, eidolon:sulfur, eidolon:ender_calx\n");
        context.append("- Holy Items: eidolon:holy_symbol, eidolon:unholy_symbol, eidolon:gold_inlay, eidolon:pewter_inlay\n");
        context.append("- Research: eidolon:research_notes, eidolon:codex, eidolon:parchment, eidolon:quill\n");
        
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
        context.append("- Beneficial: minecraft:strength, minecraft:speed, minecraft:regeneration, minecraft:resistance\n");
        context.append("- Utility: minecraft:night_vision, minecraft:water_breathing, minecraft:fire_resistance\n");
        context.append("- Negative: minecraft:weakness, minecraft:slowness, minecraft:poison, minecraft:wither\n");
        
        // Command format reminders
        context.append("\nCOMMAND FORMAT EXAMPLES:\n");
        context.append("- Give Item: give {player} eidolon:zombie_heart 1\n");
        context.append("- Apply Effect: effect give {player} minecraft:regeneration 300 1\n");
        context.append("- Set Title: title {player} subtitle {\"text\":\"Your message\",\"color\":\"purple\"}\n");
        context.append("- Play Sound: playsound minecraft:block.beacon.activate master {player} ~ ~ ~ 1.0 1.0\n");
        
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
    
    /**
     * 🎯 DYNAMIC PROGRESSION LEVEL HELPER - USES ACTUAL DEITY PROGRESSION STAGES
     * 
     * Gets the player's current progression level from deity JSON progression stages.
     * This ensures the AI recognizes the player's actual rank from the deity JSON configs.
     */
    private static String getDynamicProgressionLevel(DatapackDeity deity, ServerPlayer player) {
        double reputation = deity.getPlayerReputation(player);
        
        try {
            // 🔥 PRIMARY: Use actual deity progression stages from JSON
            Map<String, Object> progressionData = deity.getProgressionStages();
            if (progressionData != null && !progressionData.isEmpty()) {
                String bestTitle = "Newcomer";
                double highestQualifyingReputation = -1;
                
                // Check all progression stages to find the highest one the player qualifies for
                for (Map.Entry<String, Object> entry : progressionData.entrySet()) {
                    String stageName = entry.getKey();
                    
                    try {
                        // Handle progression data structure
                        Object stageValue = entry.getValue();
                        Map<String, Object> stageData = null;
                        
                        if (stageValue instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> mapValue = (Map<String, Object>) stageValue;
                            stageData = mapValue;
                        } else if (stageValue instanceof List) {
                            // Handle stages array format
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> stagesList = (List<Map<String, Object>>) stageValue;
                            for (Map<String, Object> stage : stagesList) {
                                Object repObj = stage.get("reputation");
                                if (repObj instanceof Number) {
                                    double requiredRep = ((Number) repObj).doubleValue();
                                    if (reputation >= requiredRep && requiredRep > highestQualifyingReputation) {
                                        highestQualifyingReputation = requiredRep;
                                        Object titleObj = stage.get("title");
                                        if (titleObj instanceof String) {
                                            bestTitle = (String) titleObj;
                                        }
                                    }
                                }
                            }
                            continue;
                        }
                        
                        if (stageData != null) {
                            // Look for reputation requirement
                            Object repObj = stageData.get("reputation");
                            if (repObj == null) repObj = stageData.get("reputation_required");
                            
                            if (repObj instanceof Number) {
                                double requiredRep = ((Number) repObj).doubleValue();
                                
                                // Check if player qualifies for this stage
                                if (reputation >= requiredRep && requiredRep > highestQualifyingReputation) {
                                    highestQualifyingReputation = requiredRep;
                                    
                                    // Use the title from the stage data, or format the stage name
                                    Object titleObj = stageData.get("title");
                                    if (titleObj instanceof String) {
                                        bestTitle = (String) titleObj;
                                    } else {
                                        // Convert stage ID to title format
                                        bestTitle = formatStageNameToTitle(stageName);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Error processing progression stage {}: {}", stageName, e.getMessage());
                    }
                }
                
                LOGGER.debug("🏆 Actual Progression Level for {}/{}: {} ({}rep, qualifying={})", 
                    player.getName().getString(), deity.getName(), bestTitle, (int)reputation, (int)highestQualifyingReputation);
                
                return bestTitle;
            }
            
            // FALLBACK: Try AI config reputation behaviors if deity stages aren't available
            AIDeityConfig aiConfig = AIDeityManager.getInstance().getAIConfig(deity.getId());
            if (aiConfig != null && !aiConfig.getReputationBehaviors().isEmpty()) {
                String progressionTitle = "Newcomer";
                int highestThreshold = -1;
                
                for (Map.Entry<Integer, String> entry : aiConfig.getReputationBehaviors().entrySet()) {
                    if (reputation >= entry.getKey() && entry.getKey() > highestThreshold) {
                        highestThreshold = entry.getKey();
                        progressionTitle = "Follower (Level " + entry.getKey() + ")"; // Use level-based title
                    }
                }
                
                LOGGER.debug("🤖 AI Config fallback progression for {}/{}: {} ({}rep, threshold={})", 
                    player.getName().getString(), deity.getName(), progressionTitle, (int)reputation, highestThreshold);
                
                return progressionTitle;
            }
            
            // LAST RESORT: Generic progression stages based on reputation
            LOGGER.debug("🔍 No progression data for deity {}, using generic levels", deity.getId());
            if (reputation >= 100) return "Champion";
            else if (reputation >= 75) return "Master";
            else if (reputation >= 50) return "Priest";
            else if (reputation >= 25) return "Acolyte";
            else if (reputation >= 0) return "Initiate";
            else return "Outcast";
            
        } catch (Exception e) {
            LOGGER.error("🚨 Error determining progression level for {}/{}, using fallback: {}", 
                player.getName().getString(), deity.getName(), e.getMessage());
            
            // Emergency fallback
            if (reputation >= 75) return "Master";
            if (reputation >= 50) return "Priest";
            if (reputation >= 25) return "Acolyte";
            if (reputation >= 10) return "Initiate";
            return "Newcomer";
        }
    }
    
    /**
     * Converts stage names like "shadow_initiate" to proper titles like "Shadow Initiate"
     */
    private static String formatStageNameToTitle(String stageName) {
        if (stageName == null || stageName.isEmpty()) return "Newcomer";
        
        // Convert underscores to spaces and capitalize each word
        String[] words = stageName.replace("_", " ").split(" ");
        StringBuilder titleBuilder = new StringBuilder();
        
        for (String word : words) {
            if (word.length() > 0) {
                titleBuilder.append(Character.toUpperCase(word.charAt(0)))
                   .append(word.substring(1).toLowerCase())
                   .append(" ");
            }
        }
        
        return titleBuilder.toString().trim();
    }
}
