package com.bluelotuscoding.eidolonunchained.integration.ai;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 🤖 PURE AI-DRIVEN ITEM EXTRACTION
 * Let the AI understand natural language and suggest items, then validate through registry
 */
public class AIItemExtractor {
    private static final Logger LOGGER = LogManager.getLogger();
    
    // Pattern to extract AI-suggested items from AI responses
    // Supports both intended format [ITEM:...] and actual AI behavior **item** or "take this item"
    private static final Pattern AI_ITEM_SUGGESTION_PATTERN = Pattern.compile(
        "\\[ITEM:([^\\]]+)\\]|\\*\\*([^\\*]+)\\*\\*|(?:take this|receive|here is|behold)\\s+([a-zA-Z\\s]+)(?=\\s*[.!])", 
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * Pure AI-driven approach: Ask AI to identify what the player wants
     */
    public static List<String> extractItemsViaAI(String playerMessage, ServerPlayer player, String aiResponse) {
        List<String> commands = new ArrayList<>();
        
        try {
            // Step 1: Ask AI to analyze the player's request and suggest items
            String itemAnalysisPrompt = buildItemAnalysisPrompt(playerMessage, player);
            
            // Step 2: Get AI to process the request
            ResourceLocation activeDeityId = com.bluelotuscoding.eidolonunchained.chat.DeityChat.getActiveConversationDeity(player);
            if (activeDeityId != null) {
                var aiConfig = com.bluelotuscoding.eidolonunchained.ai.AIDeityManager.getInstance().getAIConfig(activeDeityId);
                if (aiConfig != null) {
                    commands.addAll(processAIItemSuggestions(aiResponse, player, aiConfig));
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("🤖 Error in AI-driven item extraction: {}", e.getMessage());
        }
        
        return commands;
    }
    
    /**
     * Build a specific prompt asking AI to identify requested items
     */
    private static String buildItemAnalysisPrompt(String playerMessage, ServerPlayer player) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("ITEM REQUEST ANALYSIS:\n");
        prompt.append("Player Message: \"").append(playerMessage).append("\"\n\n");
        prompt.append("INSTRUCTIONS:\n");
        prompt.append("- If the player is requesting a specific item, respond with [ITEM:item_name]\n");
        prompt.append("- Use natural language understanding to identify what they want\n");
        prompt.append("- Examples:\n");
        prompt.append("  * \"give me a sword\" → [ITEM:sword]\n");
        prompt.append("  * \"I need the raven cloak\" → [ITEM:raven cloak]\n");
        prompt.append("  * \"bestow upon me dark armor\" → [ITEM:dark armor]\n");
        prompt.append("- If no specific item is requested, don't include [ITEM:...] tags\n");
        prompt.append("- Be conversational and immersive in your response\n\n");
        
        return prompt.toString();
    }
    
    /**
     * Process AI suggestions and validate them against registry
     */
    private static List<String> processAIItemSuggestions(String aiResponse, ServerPlayer player, 
                                                       com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig aiConfig) {
        List<String> commands = new ArrayList<>();
        
        // Extract items from AI response using multiple patterns
        LOGGER.info("🤖 Analyzing AI response for items: '{}'", aiResponse);
        Matcher itemMatcher = AI_ITEM_SUGGESTION_PATTERN.matcher(aiResponse);
        while (itemMatcher.find()) {
            String suggestedItem = null;
            String matchType = "unknown";
            
            // Check which capture group matched
            if (itemMatcher.group(1) != null) {
                // [ITEM:...] format
                suggestedItem = itemMatcher.group(1).trim();
                matchType = "ITEM_TAG";
            } else if (itemMatcher.group(2) != null) {
                // **item** format
                suggestedItem = itemMatcher.group(2).trim();
                matchType = "BOLD_FORMAT";
            } else if (itemMatcher.group(3) != null) {
                // "take this item" format
                suggestedItem = itemMatcher.group(3).trim();
                matchType = "TAKE_THIS_FORMAT";
            }
            
            if (suggestedItem != null && !suggestedItem.isEmpty()) {
                // 🔧 SMART CLEANING: Remove common filler words and clean up the item name
                String cleanedItem = cleanupItemName(suggestedItem);
                LOGGER.info("🤖 AI suggested item: '{}' → cleaned: '{}' (pattern: {})", 
                    suggestedItem, cleanedItem, matchType);
            
                // Get mod context for this deity
                List<String> modContextIds = aiConfig.mod_context_ids != null && !aiConfig.mod_context_ids.isEmpty() ? 
                    aiConfig.mod_context_ids : Arrays.asList("minecraft", "eidolon", "eidolonunchained");
                
                // Validate item exists in registry
                List<ResourceLocation> matches = RegistryContextProvider.findMatchingItemsWithScoring(cleanedItem, modContextIds);
                
                if (!matches.isEmpty()) {
                    ResourceLocation bestMatch = matches.get(0);
                    
                    // Check deity permissions
                    if (deityAllowsItem(bestMatch.toString(), aiConfig, player)) {
                        String command = String.format("give %s %s 1", player.getName().getString(), bestMatch.toString());
                        commands.add(command);
                        LOGGER.info("🤖 AI suggestion APPROVED: '{}' → {} (deity: {})", 
                            cleanedItem, bestMatch, aiConfig.deity_id);
                    } else {
                        LOGGER.info("🚫 AI suggestion DENIED: '{}' → {} (not allowed by deity)", 
                            cleanedItem, bestMatch);
                    }
                } else {
                    LOGGER.info("🤖 AI suggested item '{}' has no valid registry matches - no item will be given", cleanedItem);
                    // This is GOOD behavior - prevents random items from being given!
                }
            }
        }
        
        return commands;
    }
    
    /**
     * 🔧 NEW: Clean up AI-suggested item names by removing filler words
     */
    private static String cleanupItemName(String itemName) {
        // Remove common articles and prepositions at the beginning
        String cleaned = itemName.replaceFirst("^(the|a|an)\\s+", "");
        
        // Remove common filler phrases
        cleaned = cleaned.replaceAll("\\s+(of|from|with|made of|crafted from)\\s+", " ");
        
        // Clean up extra whitespace
        cleaned = cleaned.trim().replaceAll("\\s+", " ");
        
        return cleaned;
    }
    
    /**
     * Check if deity allows this specific item
     */
    private static boolean deityAllowsItem(String itemId, com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig aiConfig, ServerPlayer player) {
        // TODO: Implement deity-specific item restrictions
        // For now, allow all items (you can add restrictions based on aiConfig.allowed_items, etc.)
        return true;
    }
}
