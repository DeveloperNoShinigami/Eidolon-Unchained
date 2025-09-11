package com.bluelotuscoding.eidolonunchained.integration.ai;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
     * 🔧 UTILITY: Get mod context IDs from AI config, respecting JSON configuration
     * NO FALLBACKS - if config is missing, something is wrong and we should know about it
     */
    private static List<String> getModContextIds(com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig aiConfig) {
        if (aiConfig.mod_context_ids == null || aiConfig.mod_context_ids.isEmpty()) {
            LOGGER.error("❌ CONFIGURATION ERROR: mod_context_ids is null or empty in AI deity config!");
            LOGGER.error("   This means the JSON configuration is not loaded properly.");
            LOGGER.error("   Check that ai_deities/*.json files contain 'mod_context_ids' field.");
            throw new IllegalStateException("AI deity configuration missing mod_context_ids - check JSON files");
        }
        
        LOGGER.info("🔧 Using configured mod context IDs: {}", aiConfig.mod_context_ids);
        return aiConfig.mod_context_ids;
    }
    
    /**
     * 🎯 SIMPLIFIED: Let AI handle item extraction directly through chat
     * The AI is good at understanding requests - just let it generate commands
     */
    public static List<String> extractItemsViaAI(String playerMessage, ServerPlayer player, String aiResponse) {
        List<String> commands = new ArrayList<>();
        
        try {
            ResourceLocation activeDeityId = com.bluelotuscoding.eidolonunchained.chat.DeityChat.getActiveConversationDeity(player);
            if (activeDeityId != null) {
                var aiConfig = com.bluelotuscoding.eidolonunchained.ai.AIDeityManager.getInstance().getAIConfig(activeDeityId);
                if (aiConfig != null) {
                    // 🎯 SIMPLE: Process player message with AI to generate commands
                    commands.addAll(processPlayerMessageWithAI(playerMessage, player, aiConfig));
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("🤖 Error in AI item extraction: {}", e.getMessage());
        }
        
        return commands;
    }
    
    /**
     * 🎯 ENHANCED: Multi-method item extraction for maximum accuracy
     */
    private static List<String> processPlayerMessageWithAI(String playerMessage, ServerPlayer player, 
                                                          com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig aiConfig) {
        List<String> commands = new ArrayList<>();
        
        LOGGER.info("🤖 Enhanced AI processing player request: '{}'", playerMessage);
        
        // Check if this looks like an item request
        if (!isItemRequest(playerMessage)) {
            LOGGER.info("🤖 No item request detected in message");
            return commands;
        }
        
        try {
            // 🔥 FIXED: Try methods sequentially and stop when we get a good match
            // This prevents multiple items being extracted from the same request
            
            // METHOD 1: Enhanced pattern matching (best for explicit requests)
            List<String> method1Results = processWithEnhancedPatternMatching(playerMessage, player, aiConfig);
            if (!method1Results.isEmpty()) {
                // Limit to 1 item for blessing requests to avoid spam
                commands.add(method1Results.get(0));
                LOGGER.info("🔥 METHOD 1 SUCCESS: Found item via pattern matching, stopping here");
            } else {
                // METHOD 2: Semantic analysis (fallback for complex descriptions)
                List<String> method2Results = processWithSemanticAnalysis(playerMessage, player, aiConfig);
                if (!method2Results.isEmpty()) {
                    commands.add(method2Results.get(0));
                    LOGGER.info("🔥 METHOD 2 SUCCESS: Found item via semantic analysis, stopping here");
                } else {
                    // METHOD 3: Registry scanning (last resort)
                    List<String> method3Results = processWithRegistryScanning(playerMessage, player, aiConfig);
                    if (!method3Results.isEmpty()) {
                        commands.add(method3Results.get(0));
                        LOGGER.info("🔥 METHOD 3 SUCCESS: Found item via registry scanning");
                    }
                }
            }
            
            LOGGER.info("🔥 FIXED EXTRACTION: Generated {} command (limited to 1 per request)", commands.size());
            
        } catch (Exception e) {
            LOGGER.error("🤖 Enhanced AI processing failed: {}", e.getMessage());
            // Fallback to basic pattern matching
            List<String> fallbackResults = processWithPatternMatching(playerMessage, player, aiConfig);
            if (!fallbackResults.isEmpty()) {
                commands.add(fallbackResults.get(0)); // Still limit to 1
            }
        }
        
        return commands;
    }
    
    /**
     * Build prompt for AI to understand item requests
     */
    private static String buildItemRequestPrompt(String playerMessage, ServerPlayer player) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("PLAYER ITEM REQUEST ANALYSIS:\n");
        prompt.append("Player Message: \"").append(playerMessage).append("\"\n\n");
        prompt.append("TASK: Generate Minecraft give commands for requested items.\n");
        prompt.append("FORMAT: If player wants items, respond with commands like:\n");
        prompt.append("/give ").append(player.getName().getString()).append(" minecraft:iron_sword 1\n");
        prompt.append("/give ").append(player.getName().getString()).append(" eidolon:shadow_cloak 1\n\n");
        prompt.append("RULES:\n");
        prompt.append("- Only generate commands for clearly requested items\n");
        prompt.append("- Use exact mod:item_id format\n");
        prompt.append("- If no specific items requested, respond with 'NO_ITEMS'\n");
        
        return prompt.toString();
    }
    
    /**
     * 🎯 SIMPLE: Pattern matching fallback when AI API not available
     */
    private static List<String> processWithPatternMatching(String playerMessage, ServerPlayer player,
                                                          com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig aiConfig) {
        List<String> commands = new ArrayList<>();
        
        // Simple pattern to extract item requests
        Pattern itemPattern = Pattern.compile(
            "(?:give me|i need|i want|can i have)\\s+(?:a|an|the|some)?\\s*([a-zA-Z\\s]+?)(?:\\s*[.!?]|$)",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = itemPattern.matcher(playerMessage);
        while (matcher.find()) {
            String requestedItem = matcher.group(1).trim();
            if (requestedItem != null && !requestedItem.isEmpty()) {
                String cleanedItem = cleanupItemName(requestedItem);
                LOGGER.info("🎯 Pattern matched item request: '{}'", cleanedItem);
                
                // Find matching items using scoring
                List<String> modContextIds = getModContextIds(aiConfig);
                
                List<ResourceLocation> matches = com.bluelotuscoding.eidolonunchained.integration.ai.RegistryContextProvider
                    .findMatchingItemsWithScoring(cleanedItem, modContextIds);
                
                if (!matches.isEmpty()) {
                    ResourceLocation bestMatch = matches.get(0);
                    if (deityAllowsItem(bestMatch.toString(), aiConfig, player)) {
                        String command = String.format("/give %s %s 1", player.getName().getString(), bestMatch.toString());
                        commands.add(command);
                        LOGGER.info("🎯 Generated command: {}", command);
                    }
                }
            }
        }
        
        return commands;
    }
    
    /**
     * AI helper: Determine if message contains item requests
     * 🔧 FIXED: Now matches ALL the actual patterns we support
     */
    private static boolean isItemRequest(String message) {
        String lowerMessage = message.toLowerCase();
        return lowerMessage.matches(".*(?:can i have|give me|i need|i want|bestow|grant me|bless me with|blessing of|i wish for|i desire|i seek|help me get|help me find|help me obtain|could you give me).*");
    }
    
    /**
     * 🎯 HYBRID STEP 2: Validate AI extractions using scoring system
     */
    private static List<String> validateExtractedItemsWithScoring(Set<String> extractedItems, ServerPlayer player,
                                                                 com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig aiConfig) {
        List<String> commands = new ArrayList<>();
        
        LOGGER.info("🎯 HYBRID STEP 2: Validating {} AI-extracted items with scoring system", extractedItems.size());
        
        List<String> modContextIds = getModContextIds(aiConfig);
        
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
        List<String> modContextIds = getModContextIds(aiConfig);
        
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
        List<String> modContextIds = getModContextIds(aiConfig);
        
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
            List<String> modContextIds = getModContextIds(aiConfig);
                
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
                List<String> modContextIds = getModContextIds(aiConfig);
                
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
                List<String> modContextIds = getModContextIds(aiConfig);
                
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
     * METHOD 1: Enhanced pattern matching with improved patterns
     */
    private static List<String> processWithEnhancedPatternMatching(String playerMessage, ServerPlayer player,
                                                                  com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig aiConfig) {
        List<String> commands = new ArrayList<>();
        
        LOGGER.info("🔥 ENHANCED PATTERN MATCHING: Processing '{}'", playerMessage);
        
        // Enhanced patterns that capture more natural language variations
        Pattern[] enhancedPatterns = {
            // Direct requests with articles
            Pattern.compile("(?:give me|i need|i want|can i have|grant me|bestow)\\s+(?:a|an|the|some)?\\s*([a-zA-Z][a-zA-Z0-9\\s]*?)(?:\\s*[,.!?]|\\s+(?:please|now|today)|$)", Pattern.CASE_INSENSITIVE),
            
            // Blessing requests (🔥 FIXED: More greedy capture to get full item names)
            Pattern.compile("(?:bless me with|blessing of)\\s+(?:a|an|the)?\\s*([a-zA-Z][a-zA-Z0-9\\s]+?)(?:\\s*[,.!?]|\\?|$)", Pattern.CASE_INSENSITIVE),
            
            // Desire/wish patterns
            Pattern.compile("(?:i wish for|i desire|i seek)\\s+(?:a|an|the|some)?\\s*([a-zA-Z][a-zA-Z0-9\\s]*?)(?:\\s*[,.!?]|$)", Pattern.CASE_INSENSITIVE),
            
            // Help patterns
            Pattern.compile("(?:help me (?:get|find|obtain)|could you give me)\\s+(?:a|an|the|some)?\\s*([a-zA-Z][a-zA-Z0-9\\s]*?)(?:\\s*[,.!?]|$)", Pattern.CASE_INSENSITIVE)
        };
        
        Set<String> foundItems = new HashSet<>(); // Prevent duplicates
        
        for (Pattern pattern : enhancedPatterns) {
            Matcher matcher = pattern.matcher(playerMessage);
            while (matcher.find()) {
                String requestedItem = matcher.group(1).trim();
                if (requestedItem != null && !requestedItem.isEmpty()) {
                    String cleanedItem = cleanupItemName(requestedItem);
                    LOGGER.info("🔍 Enhanced pattern found: '{}' → '{}'", requestedItem, cleanedItem);
                    foundItems.add(cleanedItem);
                }
            }
        }
        
        // Process found items with scoring
        commands.addAll(processFoundItemsWithScoring(foundItems, player, aiConfig));
        
        return commands;
    }
    
    /**
     * METHOD 2: Semantic analysis for complex item descriptions
     */
    private static List<String> processWithSemanticAnalysis(String playerMessage, ServerPlayer player,
                                                           com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig aiConfig) {
        List<String> commands = new ArrayList<>();
        Set<String> semanticItems = new HashSet<>();
        
        String lowerMessage = playerMessage.toLowerCase();
        
        // Item categories with context patterns
        Map<String, Pattern> categoryPatterns = new HashMap<>();
        categoryPatterns.put("sword", Pattern.compile("(?:dark|light|shadow|divine|magical|enchanted|blessed|cursed|ancient|powerful|iron|diamond|steel)?\\s*sword(?:\\s+of\\s+(?:power|magic|strength|darkness|light))?", Pattern.CASE_INSENSITIVE));
        categoryPatterns.put("armor", Pattern.compile("(?:dark|light|shadow|divine|magical|enchanted|blessed|cursed|ancient|powerful)?\\s*(?:armor|chestplate|helmet|boots|leggings)(?:\\s+of\\s+(?:power|magic|strength|protection|darkness|light))?", Pattern.CASE_INSENSITIVE));
        categoryPatterns.put("cloak", Pattern.compile("(?:dark|light|shadow|divine|magical|enchanted|blessed|cursed|ancient|powerful)?\\s*(?:cloak|robe)(?:\\s+of\\s+(?:power|magic|strength|protection|darkness|light))?", Pattern.CASE_INSENSITIVE));
        categoryPatterns.put("staff", Pattern.compile("(?:dark|light|shadow|divine|magical|enchanted|blessed|cursed|ancient|powerful)?\\s*(?:staff|wand)(?:\\s+of\\s+(?:power|magic|strength|darkness|light))?", Pattern.CASE_INSENSITIVE));
        categoryPatterns.put("ring", Pattern.compile("(?:dark|light|shadow|divine|magical|enchanted|blessed|cursed|ancient|powerful)?\\s*(?:ring|amulet)(?:\\s+of\\s+(?:power|magic|strength|protection|darkness|light))?", Pattern.CASE_INSENSITIVE));
        
        for (Map.Entry<String, Pattern> entry : categoryPatterns.entrySet()) {
            Matcher matcher = entry.getValue().matcher(playerMessage);
            if (matcher.find()) {
                String semanticMatch = matcher.group().trim();
                String cleanedMatch = cleanupItemName(semanticMatch);
                LOGGER.info("🧠 Semantic match: '{}' → '{}'", semanticMatch, cleanedMatch);
                semanticItems.add(cleanedMatch);
            }
        }
        
        // Process semantic items with scoring
        commands.addAll(processFoundItemsWithScoring(semanticItems, player, aiConfig));
        
        return commands;
    }
    
    /**
     * METHOD 3: Word-by-word registry scanning for exact matches
     */
    private static List<String> processWithRegistryScanning(String playerMessage, ServerPlayer player,
                                                           com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig aiConfig) {
        List<String> commands = new ArrayList<>();
        Set<String> foundItems = new HashSet<>();
        
        List<String> modContextIds = getModContextIds(aiConfig);
        
        List<ResourceLocation> availableItems = com.bluelotuscoding.eidolonunchained.integration.ai.RegistryContextProvider
            .getAllItemsForMods(modContextIds);
        
        String[] messageWords = playerMessage.toLowerCase().replace(",", " ").replace(".", " ").split("\\s+");
        
        // Look for exact item name matches in the message
        for (ResourceLocation item : availableItems) {
            String itemName = item.getPath().replace("_", " ");
            String[] itemWords = itemName.split("\\s+");
            
            // For multi-word items, check if all words appear in sequence or nearby
            if (itemWords.length > 1) {
                if (containsWordSequence(messageWords, itemWords)) {
                    LOGGER.info("🔍 Registry scan found: '{}' → {}", itemName, item);
                    foundItems.add(itemName);
                }
            } else {
                // Single word items - check for exact matches
                for (String messageWord : messageWords) {
                    if (messageWord.equals(itemWords[0]) || 
                        (messageWord.contains(itemWords[0]) && messageWord.length() <= itemWords[0].length() + 2)) {
                        LOGGER.info("🔍 Registry scan found single word: '{}' → {}", itemWords[0], item);
                        foundItems.add(itemName);
                        break;
                    }
                }
            }
        }
        
        // Process found items with scoring
        commands.addAll(processFoundItemsWithScoring(foundItems, player, aiConfig));
        
        return commands;
    }
    
    /**
     * Helper: Check if message words contain item word sequence
     */
    private static boolean containsWordSequence(String[] messageWords, String[] itemWords) {
        if (itemWords.length > messageWords.length) return false;
        
        for (int i = 0; i <= messageWords.length - itemWords.length; i++) {
            boolean sequenceMatch = true;
            for (int j = 0; j < itemWords.length; j++) {
                if (!messageWords[i + j].contains(itemWords[j]) && !itemWords[j].contains(messageWords[i + j])) {
                    sequenceMatch = false;
                    break;
                }
            }
            if (sequenceMatch) return true;
        }
        
        return false;
    }
    
    /**
     * Process found items using the scoring system
     */
    private static List<String> processFoundItemsWithScoring(Set<String> foundItems, ServerPlayer player,
                                                            com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig aiConfig) {
        List<String> commands = new ArrayList<>();
        
        List<String> modContextIds = getModContextIds(aiConfig);
        
        for (String itemName : foundItems) {
            List<ResourceLocation> matches = com.bluelotuscoding.eidolonunchained.integration.ai.RegistryContextProvider
                .findMatchingItemsWithScoring(itemName, modContextIds);
            
            if (!matches.isEmpty()) {
                ResourceLocation bestMatch = matches.get(0);
                
                if (deityAllowsItem(bestMatch.toString(), aiConfig, player)) {
                    String command = String.format("/give %s %s 1", player.getName().getString(), bestMatch.toString());
                    commands.add(command);
                    LOGGER.info("🎯 Enhanced extraction SUCCESS: '{}' → {}", itemName, bestMatch);
                }
            }
        }
        
        return commands;
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
