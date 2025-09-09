package com.bluelotuscoding.eidolonunchained.integration.ai;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
     * 🔧 FIXED: Process player's original request, NOT AI's interpretation
     * This prevents confusion from AI creativity and focuses on what player actually wants
     * HYBRID APPROACH: AI extraction first, then scoring validation
     */
    public static List<String> extractItemsViaAI(String playerMessage, ServerPlayer player, String aiResponse) {
        List<String> commands = new ArrayList<>();
        
        try {
            ResourceLocation activeDeityId = com.bluelotuscoding.eidolonunchained.chat.DeityChat.getActiveConversationDeity(player);
            if (activeDeityId != null) {
                var aiConfig = com.bluelotuscoding.eidolonunchained.ai.AIDeityManager.getInstance().getAIConfig(activeDeityId);
                if (aiConfig != null) {
                    // 🎯 HYBRID STEP 1: AI-based extraction from player message
                    Set<String> aiExtractedItems = extractItemsWithAI(playerMessage, player, aiConfig);
                    
                    // 🎯 HYBRID STEP 2: Validate AI extractions with scoring system
                    commands.addAll(validateExtractedItemsWithScoring(aiExtractedItems, player, aiConfig));
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("🤖 Error in hybrid AI item extraction: {}", e.getMessage());
        }
        
        return commands;
    }
    
    /**
     * 🎯 HYBRID STEP 1: AI-based item extraction using natural language understanding
     */
    private static Set<String> extractItemsWithAI(String playerMessage, ServerPlayer player, 
                                                 com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig aiConfig) {
        Set<String> extractedItems = new HashSet<>();
        
        LOGGER.info("🤖 HYBRID STEP 1: AI extraction from player message: '{}'", playerMessage);
        
        // Check if this looks like an item request using AI-like understanding
        if (!isItemRequest(playerMessage)) {
            LOGGER.info("🤖 AI analysis: No item request patterns detected");
            return extractedItems;
        }
        
        // AI METHOD 1: Enhanced pattern matching for natural language
        Pattern naturalLanguagePattern = Pattern.compile(
            "(?:can i have|give me|i need|i want|bestow|grant me|i would like|may i have|could you give me)\\s+" +
            "(?:a|an|the|some)?\\s*" +
            "(?:new|old|fresh|good|nice|strong|powerful|magical|enchanted|blessed|divine)?\\s*" +
            "([a-zA-Z][a-zA-Z\\s]*?)(?:\\s*[,.!?]|\\s+(?:for|to|please|now)|$)",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = naturalLanguagePattern.matcher(playerMessage);
        while (matcher.find()) {
            String extractedItem = matcher.group(1).trim();
            if (extractedItem != null && !extractedItem.isEmpty()) {
                String cleanedItem = cleanupItemName(extractedItem);
                LOGGER.info("🤖 AI EXTRACTION (Pattern): '{}' → '{}'", extractedItem, cleanedItem);
                extractedItems.add(cleanedItem);
            }
        }
        
        // AI METHOD 2: Semantic word analysis for complex requests
        extractedItems.addAll(performSemanticItemAnalysis(playerMessage, aiConfig));
        
        // AI METHOD 3: Context-aware multi-word extraction
        extractedItems.addAll(performContextAwareExtraction(playerMessage, aiConfig));
        
        LOGGER.info("🤖 HYBRID STEP 1 COMPLETE: AI extracted {} unique items: {}", 
            extractedItems.size(), extractedItems);
        
        return extractedItems;
    }
    
    /**
     * 🎯 HYBRID STEP 2: Validate AI extractions using scoring system
     */
    private static List<String> validateExtractedItemsWithScoring(Set<String> extractedItems, ServerPlayer player,
                                                                 com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig aiConfig) {
        List<String> commands = new ArrayList<>();
        
        LOGGER.info("🎯 HYBRID STEP 2: Validating {} AI-extracted items with scoring system", extractedItems.size());
        
        List<String> modContextIds = aiConfig.mod_context_ids != null && !aiConfig.mod_context_ids.isEmpty() ? 
            aiConfig.mod_context_ids : Arrays.asList("minecraft", "eidolon", "eidolonunchained");
        
        for (String extractedItem : extractedItems) {
            LOGGER.info("🎯 SCORING VALIDATION: Testing '{}'", extractedItem);
            
            // Use scoring system to find best matches
            List<ResourceLocation> scoredMatches = com.bluelotuscoding.eidolonunchained.integration.ai.RegistryContextProvider
                .findMatchingItemsWithScoring(extractedItem, modContextIds);
            
            if (!scoredMatches.isEmpty()) {
                ResourceLocation bestMatch = scoredMatches.get(0);
                
                // Check deity permissions
                if (deityAllowsItem(bestMatch.toString(), aiConfig, player)) {
                    String command = String.format("/give %s %s 1", player.getName().getString(), bestMatch.toString());
                    commands.add(command);
                    LOGGER.info("🔥 HYBRID SUCCESS: AI extracted '{}' → SCORING validated '{}' → APPROVED", 
                        extractedItem, bestMatch);
                } else {
                    LOGGER.info("🚫 HYBRID BLOCKED: AI extracted '{}' → SCORING validated '{}' → DEITY DENIED", 
                        extractedItem, bestMatch);
                }
            } else {
                LOGGER.info("🎯 HYBRID FAILED: AI extracted '{}' → SCORING found no matches", extractedItem);
            }
        }
        
        LOGGER.info("🎯 HYBRID STEP 2 COMPLETE: {} final commands generated", commands.size());
        return commands;
    }
    
    /**
     * AI helper: Determine if message contains item requests
     */
    private static boolean isItemRequest(String message) {
        String lowerMessage = message.toLowerCase();
        return lowerMessage.matches(".*(?:can i have|give me|i need|i want|bestow|grant me|i would like|may i have|could you give me).*");
    }
    
    /**
     * AI helper: Semantic analysis for complex item descriptions
     */
    private static Set<String> performSemanticItemAnalysis(String playerMessage, 
                                                          com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig aiConfig) {
        Set<String> semanticItems = new HashSet<>();
        
        LOGGER.info("🧠 SEMANTIC ANALYSIS: Analyzing '{}'", playerMessage);
        
        // Look for item-related keywords and their context
        String[] itemCategories = {"sword", "armor", "cloak", "staff", "wand", "ring", "amulet", "potion", 
                                 "scroll", "book", "crystal", "gem", "robe", "helmet", "boots", "gloves"};
        
        String lowerMessage = playerMessage.toLowerCase();
        for (String category : itemCategories) {
            if (lowerMessage.contains(category)) {
                // Look for modifiers around the category
                Pattern contextPattern = Pattern.compile(
                    "(?:(?:dark|light|shadow|divine|magical|enchanted|blessed|cursed|ancient|powerful)\\s+)?" +
                    category + 
                    "(?:\\s+(?:of|with)\\s+(?:power|magic|strength|protection|the|darkness|light))?",
                    Pattern.CASE_INSENSITIVE
                );
                
                Matcher contextMatcher = contextPattern.matcher(playerMessage);
                if (contextMatcher.find()) {
                    String semanticExtraction = contextMatcher.group().trim();
                    LOGGER.info("🧠 SEMANTIC MATCH: '{}'", semanticExtraction);
                    semanticItems.add(cleanupItemName(semanticExtraction));
                }
            }
        }
        
        return semanticItems;
    }
    
    /**
     * AI helper: Context-aware extraction for multi-word items
     */
    private static Set<String> performContextAwareExtraction(String playerMessage,
                                                           com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig aiConfig) {
        Set<String> contextItems = new HashSet<>();
        
        LOGGER.info("🎯 CONTEXT-AWARE EXTRACTION: Analyzing '{}'", playerMessage);
        
        // Get available items for context matching
        List<String> modContextIds = aiConfig.mod_context_ids != null && !aiConfig.mod_context_ids.isEmpty() ? 
            aiConfig.mod_context_ids : Arrays.asList("minecraft", "eidolon", "eidolonunchained");
        
        List<ResourceLocation> availableItems = com.bluelotuscoding.eidolonunchained.integration.ai.RegistryContextProvider
            .getAllItemsForMods(modContextIds);
        
        String[] messageWords = playerMessage.toLowerCase().split("\\s+");
        
        // Look for multi-word item matches in the message
        for (ResourceLocation item : availableItems) {
            String itemName = item.getPath().replace("_", " ");
            String[] itemWords = itemName.split("\\s+");
            
            if (itemWords.length > 1) { // Focus on multi-word items
                int matchedWords = 0;
                int totalWords = itemWords.length;
                
                for (String itemWord : itemWords) {
                    for (String messageWord : messageWords) {
                        // Fuzzy matching with edit distance tolerance
                        if (calculateSimilarity(itemWord, messageWord) > 0.7) {
                            matchedWords++;
                            break;
                        }
                    }
                }
                
                // If most words match, consider it a context match
                double matchRatio = (double) matchedWords / totalWords;
                if (matchRatio >= 0.6) { // 60% word match threshold
                    LOGGER.info("🎯 CONTEXT MATCH: '{}' ({}% word match)", itemName, Math.round(matchRatio * 100));
                    contextItems.add(itemName);
                }
            }
        }
        
        return contextItems;
    }
    
    /**
     * Calculate similarity between two strings (0.0 = no match, 1.0 = perfect match)
     */
    private static double calculateSimilarity(String s1, String s2) {
        if (s1.equals(s2)) return 1.0;
        if (s1.contains(s2) || s2.contains(s1)) return 0.8;
        
        // Simple Levenshtein-based similarity
        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) return 1.0;
        
        int editDistance = getLevenshteinDistance(s1, s2);
        return 1.0 - (double) editDistance / maxLength;
    }
    
    /**
     * Calculate Levenshtein distance between two strings
     */
    private static int getLevenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + (s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1)
                    );
                }
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
    
    /**
     * 🎯 NEW: Process player's direct requests for items using simplified AI understanding
     */
    private static List<String> processPlayerItemRequests(String playerMessage, ServerPlayer player, 
                                                         com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig aiConfig) {
        List<String> commands = new ArrayList<>();
        
        LOGGER.info("🎯 HYBRID ANALYSIS: Analyzing PLAYER request for items: '{}'", playerMessage);
        
        // Check if this looks like an item request
        if (!playerMessage.toLowerCase().matches(".*(?:can i have|give me|i need|i want|bestow|grant me).*")) {
            LOGGER.info("🎯 No item request patterns detected in player message");
            return commands;
        }
        
        Set<String> foundItems = new HashSet<>(); // Track items to avoid duplicates
        List<String> modContextIds = aiConfig.mod_context_ids != null && !aiConfig.mod_context_ids.isEmpty() ? 
            aiConfig.mod_context_ids : Arrays.asList("minecraft", "eidolon", "eidolonunchained");
        
        // METHOD 1: Enhanced pattern matching for immediate extraction
        Pattern playerItemPattern = Pattern.compile(
            "(?:can i have|give me|i need|i want|bestow|grant me)\\s+(?:a|an|the|some)?\\s*(?:new|old|fresh|good|nice|strong|powerful)?\\s*([a-zA-Z][a-zA-Z\\s]*?)(?:\\s*[,.!?]|\\s+for|\\s+to|$)",
            Pattern.CASE_INSENSITIVE
        );
        
        LOGGER.info("🔍 METHOD 1 - Testing regex pattern against: '{}'", playerMessage);
        Matcher matcher = playerItemPattern.matcher(playerMessage);
        
        while (matcher.find()) {
            String requestedItem = matcher.group(1).trim();
            
            if (requestedItem != null && !requestedItem.isEmpty()) {
                String cleanedItem = cleanupItemName(requestedItem)
                    .replaceAll("\\b(?:new|old|fresh|good|nice|strong|powerful)\\b", "") // Remove adjectives
                    .replaceAll("\\s+", " ") // Normalize whitespace
                    .trim();
                    
                LOGGER.info("🎯 METHOD 1 - PLAYER requested item: '{}' → cleaned: '{}'", requestedItem, cleanedItem);
                foundItems.add(cleanedItem);
            }
        }
        
        // METHOD 2: Word-matching approach for items missed by regex
        LOGGER.info("🔍 METHOD 2 - Starting word-matching analysis on: '{}'", playerMessage);
        List<ResourceLocation> availableItems = com.bluelotuscoding.eidolonunchained.integration.ai.RegistryContextProvider
            .getAllItemsForMods(modContextIds);
        
        // Look for item names in the message using word matching
        String[] words = playerMessage.toLowerCase().split("\\s+");
        for (ResourceLocation item : availableItems) {
            String itemName = item.getPath().replace("_", " ");
            String[] itemWords = itemName.split("\\s+");
            
            // Check if all item words appear in the message
            boolean allWordsFound = true;
            for (String itemWord : itemWords) {
                boolean wordFound = false;
                for (String messageWord : words) {
                    if (messageWord.contains(itemWord) || itemWord.contains(messageWord)) {
                        wordFound = true;
                        break;
                    }
                }
                if (!wordFound) {
                    allWordsFound = false;
                    break;
                }
            }
            
            if (allWordsFound && itemWords.length > 1) { // Only multi-word items for specificity
                LOGGER.info("🔍 METHOD 2 - Found word match: '{}' → {}", itemName, item);
                foundItems.add(itemName);
            }
        }
        
        // Process all found items (from both methods)
        for (String itemName : foundItems) {
            List<ResourceLocation> matches = com.bluelotuscoding.eidolonunchained.integration.ai.RegistryContextProvider
                .findMatchingItemsWithScoring(itemName, modContextIds);
            
            if (!matches.isEmpty()) {
                ResourceLocation bestMatch = matches.get(0);
                
                // Check deity permissions
                if (deityAllowsItem(bestMatch.toString(), aiConfig, player)) {
                    String command = String.format("/give %s %s 1", player.getName().getString(), bestMatch.toString());
                    commands.add(command);
                    LOGGER.info("🔥 HYBRID SUCCESS: '{}' -> {}", itemName, bestMatch);
                } else {
                    LOGGER.info("🚫 HYBRID DENIED: '{}' → {} (not allowed by deity)", 
                        itemName, bestMatch);
                }
            } else {
                LOGGER.info("🎯 HYBRID: Item '{}' has no valid registry matches", itemName);
            }
        }
        
        LOGGER.info("🎯 HYBRID COMPLETE: Found {} total item commands", commands.size());
        
        return commands;
    }
    
    /**
     * Process AI's identification of requested items
     */
    private static void processAIItemIdentification(String aiResponse, ServerPlayer player, 
                                                   com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig aiConfig,
                                                   List<String> commands) {
        // Look for [ITEM:...] tags in AI response
        Pattern itemPattern = Pattern.compile("\\[ITEM:([^\\]]+)\\]", Pattern.CASE_INSENSITIVE);
        Matcher matcher = itemPattern.matcher(aiResponse);
        
        while (matcher.find()) {
            String requestedItem = matcher.group(1).trim();
            LOGGER.info("🤖 AI identified requested item: '{}'", requestedItem);
            
            // Clean and find matching items
            String cleanedItem = cleanupItemName(requestedItem);
            List<String> modContextIds = aiConfig.mod_context_ids != null && !aiConfig.mod_context_ids.isEmpty() ? 
                aiConfig.mod_context_ids : Arrays.asList("minecraft", "eidolon", "eidolonunchained");
                
            List<ResourceLocation> matches = com.bluelotuscoding.eidolonunchained.integration.ai.RegistryContextProvider
                .findMatchingItemsWithScoring(cleanedItem, modContextIds);
            
            if (!matches.isEmpty()) {
                String normalizedItem = matches.get(0).toString();
                String command = String.format("/give %s %s 1", player.getName().getString(), normalizedItem);
                commands.add(command);
                LOGGER.info("🎯 AI-identified item '{}' -> {}", requestedItem, normalizedItem);
            } else {
                LOGGER.info("🎯 AI-identified item '{}' has no valid registry matches", requestedItem);
            }
        }
    }
    
    /**
     * Fallback pattern matching when AI fails
     */
    private static void processPlayerItemRequestsFallback(String playerMessage, ServerPlayer player, 
                                                         com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig aiConfig,
                                                         List<String> commands) {
        // Pattern to detect item requests in player messages
        Pattern playerItemPattern = Pattern.compile(
            "(?:can i have|give me|i need|i want|bestow|grant me)\\s+(?:a|an|the|some)?\\s*([a-zA-Z\\s]+?)(?:\\s*[.!?]|$)",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = playerItemPattern.matcher(playerMessage);
        while (matcher.find()) {
            String requestedItem = matcher.group(1).trim();
            
            if (requestedItem != null && !requestedItem.isEmpty()) {
                // Clean up the requested item name
                String cleanedItem = cleanupItemName(requestedItem);
                LOGGER.info("🔄 FALLBACK: Player requested item: '{}' → cleaned: '{}'", requestedItem, cleanedItem);
                
                // Get mod context for this deity
                List<String> modContextIds = aiConfig.mod_context_ids != null && !aiConfig.mod_context_ids.isEmpty() ? 
                    aiConfig.mod_context_ids : Arrays.asList("minecraft", "eidolon", "eidolonunchained");
                
                // Find matching items using strict matching
                List<ResourceLocation> matches = RegistryContextProvider.findMatchingItemsWithScoring(cleanedItem, modContextIds);
                
                if (!matches.isEmpty()) {
                    ResourceLocation bestMatch = matches.get(0);
                    
                    // Check deity permissions
                    if (deityAllowsItem(bestMatch.toString(), aiConfig, player)) {
                        String command = String.format("give %s %s 1", player.getName().getString(), bestMatch.toString());
                        commands.add(command);
                        LOGGER.info("🎯 PLAYER request APPROVED: '{}' → {} (deity: {})", 
                            cleanedItem, bestMatch, aiConfig.deity_id);
                    } else {
                        LOGGER.info("🚫 PLAYER request DENIED: '{}' → {} (not allowed by deity)", 
                            cleanedItem, bestMatch);
                    }
                } else {
                    LOGGER.info("🎯 PLAYER requested item '{}' has no valid registry matches - no item will be given", cleanedItem);
                }
            }
        }
    }
    
    /**
     * Process AI suggestions and validate them against registry
     * 🔧 DEPRECATED: Keeping for fallback, but player requests take priority
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
        LOGGER.info("🔍 Cleaning item name: '{}'", itemName);
        
        // Remove common articles and prepositions at the beginning
        String cleaned = itemName.replaceFirst("^(the|a|an)\\s+", "");
        LOGGER.info("🔍 After removing articles: '{}'", cleaned);
        
        // Remove common filler phrases
        cleaned = cleaned.replaceAll("\\s+(of|from|with|made of|crafted from)\\s+", " ");
        LOGGER.info("🔍 After removing prepositions: '{}'", cleaned);
        
        // Clean up extra whitespace
        cleaned = cleaned.trim().replaceAll("\\s+", " ");
        LOGGER.info("🔍 Final cleaned item name: '{}'", cleaned);
        
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
