package com.bluelotuscoding.eidolonunchained.integration.ai;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.entity.EntityType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Provides dynamic registry context to AI for real-time awareness of what's available in the world
 * NO HARDCODING - Everything comes from actual game registries
 */
public class RegistryContextProvider {
    private static final Logger LOGGER = LogManager.getLogger();
    
    /**
     * Generate complete context for AI based on specified mod namespaces
     * This gives the AI real-time knowledge of what exists in the world
     */
    public static String generateContextForMods(List<String> modIds) {
        StringBuilder context = new StringBuilder();
        
        context.append("=== WORLD CONTEXT (Real-time Registry Data) ===\n");
        
        for (String modId : modIds) {
            context.append(generateModContext(modId));
        }
        
        // Add totals for perspective
        context.append("\n=== WORLD TOTALS ===\n");
        context.append("Total Items: ").append(BuiltInRegistries.ITEM.keySet().size()).append("\n");
        context.append("Total Blocks: ").append(BuiltInRegistries.BLOCK.keySet().size()).append("\n");
        context.append("Total Effects: ").append(BuiltInRegistries.MOB_EFFECT.keySet().size()).append("\n");
        context.append("Total Entities: ").append(BuiltInRegistries.ENTITY_TYPE.keySet().size()).append("\n");
        
        return context.toString();
    }
    
    /**
     * Generate context for a specific mod namespace
     */
    private static String generateModContext(String modId) {
        StringBuilder modContext = new StringBuilder();
        
        modContext.append("\n--- ").append(modId.toUpperCase()).append(" MOD CONTENT ---\n");
        
        // Items from this mod
        List<String> items = getRegistryEntriesForMod(BuiltInRegistries.ITEM.keySet(), modId);
        if (!items.isEmpty()) {
            modContext.append("Items (").append(items.size()).append("): ");
            modContext.append(String.join(", ", items.subList(0, Math.min(20, items.size()))));
            if (items.size() > 20) modContext.append(" ... (").append(items.size() - 20).append(" more)");
            modContext.append("\n");
        }
        
        // Blocks from this mod
        List<String> blocks = getRegistryEntriesForMod(BuiltInRegistries.BLOCK.keySet(), modId);
        if (!blocks.isEmpty()) {
            modContext.append("Blocks (").append(blocks.size()).append("): ");
            modContext.append(String.join(", ", blocks.subList(0, Math.min(15, blocks.size()))));
            if (blocks.size() > 15) modContext.append(" ... (").append(blocks.size() - 15).append(" more)");
            modContext.append("\n");
        }
        
        // Effects from this mod
        List<String> effects = getRegistryEntriesForMod(BuiltInRegistries.MOB_EFFECT.keySet(), modId);
        if (!effects.isEmpty()) {
            modContext.append("Effects (").append(effects.size()).append("): ");
            modContext.append(String.join(", ", effects));
            modContext.append("\n");
        }
        
        // Entities from this mod
        List<String> entities = getRegistryEntriesForMod(BuiltInRegistries.ENTITY_TYPE.keySet(), modId);
        if (!entities.isEmpty()) {
            modContext.append("Entities (").append(entities.size()).append("): ");
            modContext.append(String.join(", ", entities.subList(0, Math.min(10, entities.size()))));
            if (entities.size() > 10) modContext.append(" ... (").append(entities.size() - 10).append(" more)");
            modContext.append("\n");
        }
        
        return modContext.toString();
    }
    
    /**
     * Get all registry entries for a specific mod namespace
     */
    private static List<String> getRegistryEntriesForMod(Set<ResourceLocation> registryKeys, String modId) {
        return registryKeys.stream()
            .filter(key -> modId.equals(key.getNamespace()))
            .map(ResourceLocation::getPath)
            .sorted()
            .collect(Collectors.toList());
    }
    
    /**
     * Get all available items for specified mods (used for word matching)
     */
    public static List<ResourceLocation> getAllItemsForMods(List<String> modIds) {
        List<ResourceLocation> items = new ArrayList<>();
        
        for (ResourceLocation itemKey : BuiltInRegistries.ITEM.keySet()) {
            if (modIds.contains(itemKey.getNamespace())) {
                items.add(itemKey);
            }
        }
        
        return items;
    }
    
    /**
     * Find items that match a search term across all specified mods
     * This is used for dynamic AI command generation
     */
    public static List<ResourceLocation> findMatchingItems(String searchTerm, List<String> modIds) {
        String normalizedSearch = searchTerm.toLowerCase().replace(" ", "_");
        List<ResourceLocation> matches = new ArrayList<>();
        
        for (ResourceLocation itemKey : BuiltInRegistries.ITEM.keySet()) {
            // Only search in specified mod namespaces
            if (!modIds.contains(itemKey.getNamespace())) continue;
            
            String path = itemKey.getPath().toLowerCase();
            
            // Exact match (highest priority)
            if (path.equals(normalizedSearch)) {
                matches.add(0, itemKey); // Add to front
                continue;
            }
            
            // Contains match
            if (path.contains(normalizedSearch) || normalizedSearch.contains(path)) {
                matches.add(itemKey);
            }
        }
        
        LOGGER.info("🔥 Found {} matching items for '{}' in mods {}: {}", 
            matches.size(), searchTerm, modIds, 
            matches.subList(0, Math.min(5, matches.size())));
        
        return matches;
    }
    
    /**
     * Enhanced multi-word item matching with scoring system
     * 🔥 FIXED: Searches for items containing ANY of the individual words
     * "bone paladin helm" → finds items with "bone" OR "paladin" OR "helm"
     */
    public static List<ResourceLocation> findMatchingItemsWithScoring(String searchTerm, List<String> modIds) {
        // Split search term into individual words
        String[] searchWords = searchTerm.toLowerCase().split("\\s+");
        List<ScoredItem> scoredItems = new ArrayList<>();
        
        // If single word, use existing logic
        if (searchWords.length == 1) {
            return findMatchingItems(searchTerm, modIds);
        }
        
        // 🔥 FIXED: Always respect the provided mod_context_ids from configuration
        List<String> searchMods = modIds;
        LOGGER.info("🔍 DEBUG: Using configured mod IDs from AI deity config: {}", searchMods);
        
        // 🔍 DEBUG: Show what items are actually available in each mod
        for (String modId : searchMods) {
            List<String> modItems = getRegistryEntriesForMod(BuiltInRegistries.ITEM.keySet(), modId);
            LOGGER.info("🔍 Available items in '{}' mod: {} items", modId, modItems.size());
            if (modItems.size() > 0) {
                LOGGER.info("  Sample items: {}", modItems.subList(0, Math.min(10, modItems.size())));
            }
        }
        
        // 🔥 NEW APPROACH: Search for items that contain ANY of the search words
        for (ResourceLocation itemKey : BuiltInRegistries.ITEM.keySet()) {
            // Only search in specified mod namespaces
            if (!searchMods.contains(itemKey.getNamespace())) continue;
            
            String path = itemKey.getPath().toLowerCase();
            int score = calculateIndividualWordScore(path, searchWords);
            
            // Lower threshold - we want to find items with any matching words
            if (score > 0) {
                scoredItems.add(new ScoredItem(itemKey, score));
                LOGGER.debug("🔥 WORD MATCH: '{}' contains words from '{}' → score: {}", itemKey, searchTerm, score);
            }
        }
        
        // Sort by score (highest first)
        scoredItems.sort((a, b) -> Integer.compare(b.score, a.score));
        
        // 🔧 DEBUG: Log top scoring items
        LOGGER.info("🔥 INDIVIDUAL WORD MATCHES for '{}':", searchTerm);
        for (int i = 0; i < Math.min(10, scoredItems.size()); i++) {
            ScoredItem item = scoredItems.get(i);
            LOGGER.info("  {}. {} (score: {})", i+1, item.resourceLocation, item.score);
        }
        
        // Extract ResourceLocations
        List<ResourceLocation> matches = scoredItems.stream()
            .map(item -> item.resourceLocation)
            .collect(Collectors.toList());
        
        LOGGER.info("🔥 Individual word search for '{}' found {} items in {} mods", 
            searchTerm, matches.size(), searchMods.size());
        
        return matches;
    }
    
    /**
     * Get all loaded mod IDs for comprehensive searching
     */
    public static List<String> getAllLoadedModIds() {
        Set<String> modIds = new HashSet<>();
        
        // Collect mod IDs from item registry
        for (ResourceLocation itemKey : BuiltInRegistries.ITEM.keySet()) {
            modIds.add(itemKey.getNamespace());
        }
        
        return new ArrayList<>(modIds);
    }
    
    /**
     * 🔥 NEW: Calculate score for items that contain ANY of the search words
     * "bone paladin helm" → items with "bone" OR "paladin" OR "helm" get points
     */
    private static int calculateIndividualWordScore(String itemPath, String[] searchWords) {
        int score = 0;
        List<String> matchedWords = new ArrayList<>();
        
        for (String word : searchWords) {
            // Skip very short or common words
            if (word.length() < 2 || word.matches("the|of|a|an|and|or|in|on|at|to|for|with|by")) {
                continue;
            }
            
            if (itemPath.equals(word)) {
                score += 100; // Exact single word match
                matchedWords.add(word + "(exact)");
            } else if (itemPath.startsWith(word + "_") || itemPath.endsWith("_" + word)) {
                score += 50; // Word at boundary
                matchedWords.add(word + "(boundary)");
            } else if (itemPath.contains("_" + word + "_")) {
                score += 40; // Word in middle with boundaries
                matchedWords.add(word + "(middle)");
            } else if (itemPath.contains(word)) {
                score += 20; // Word anywhere
                matchedWords.add(word + "(partial)");
            }
            
            // Special bonus for important item words
            if (word.matches("helm|helmet|sword|armor|weapon|tool|ring|amulet|cloak|robe|staff|wand|bow|shield|boots|gloves")) {
                score += 10; // Bonus for equipment words
            }
        }
        
        // 🔍 DEBUG: Log matches for debugging
        if (score > 0) {
            LOGGER.debug("  📋 ITEM MATCH: '{}' matched words: {} → total score: {}", itemPath, matchedWords, score);
        }
        
        return score;
    }

    /**
     * Calculate score for an item based on how many search words it contains
     * 🔧 STRICT MATCHING: Only allow items that contain ALL important search words
     * This prevents giving random items based on single word matches
     */
    private static int calculateItemScore(String itemPath, String[] searchWords) {
        // 🔥 NEW APPROACH: Require ALL important words to be present
        List<String> importantWords = new ArrayList<>();
        List<String> fillerWords = new ArrayList<>();
        
        for (String word : searchWords) {
            if (getWordImportance(word) > 1) {
                importantWords.add(word);
            } else {
                fillerWords.add(word);
            }
        }
        
        // If no important words, require exact match of all words
        if (importantWords.isEmpty()) {
            // Convert search words to item path format (spaces to underscores)
            String expectedPath = String.join("_", searchWords);
            return itemPath.equals(expectedPath) ? 100 : 0;
        }
        
        // 🔧 STRICT RULE: Item MUST contain ALL important words
        for (String importantWord : importantWords) {
            if (!itemPath.contains(importantWord)) {
                return 0; // Reject immediately if any important word is missing
            }
        }
        
        // Calculate score only if ALL important words are present
        int score = 0;
        
        // Bonus for exact match
        String expectedPath = String.join("_", searchWords).toLowerCase();
        if (itemPath.equals(expectedPath)) {
            score += 100; // Exact match gets highest priority
        }
        
        // Score based on how well words match
        for (String word : importantWords) {
            if (itemPath.equals(word)) {
                score += 50; // Single word exact match
            } else if (itemPath.startsWith(word + "_") || itemPath.endsWith("_" + word)) {
                score += 30; // Word at boundary
            } else if (itemPath.contains("_" + word + "_")) {
                score += 20; // Word in middle with boundaries
            } else if (itemPath.contains(word)) {
                score += 10; // Word anywhere (lowest priority)
            }
        }
        
        // Bonus for containing all words (including filler words)
        boolean containsAllWords = true;
        for (String word : searchWords) {
            if (!itemPath.contains(word)) {
                containsAllWords = false;
                break;
            }
        }
        
        if (containsAllWords) {
            score += 25; // Bonus for complete match
        }
        
        return score;
    }
    
    /**
     * 🔧 NEW: Get importance weight for search words
     * Common/filler words get low weight, meaningful words get high weight
     */
    private static int getWordImportance(String word) {
        // Filler words - very low importance
        if (word.matches("the|of|a|an|and|or|in|on|at|to|for|with|by")) {
            return 1;
        }
        
        // Important item category words - high importance  
        if (word.matches("hat|helmet|sword|armor|scythe|weapon|tool|potion|ring|amulet|cloak|robe|staff|wand|bow|shield|boots|gloves")) {
            return 10;
        }
        
        // Modifier words - medium importance
        if (word.matches("dark|light|fire|ice|shadow|divine|holy|cursed|magic|enchanted|iron|gold|diamond|leather|chain|plate")) {
            return 5;
        }
        
        // Default importance for other words
        return 3;
    }
    
    /**
     * Helper class for scoring items
     */
    private static class ScoredItem {
        final ResourceLocation resourceLocation;
        final int score;
        
        ScoredItem(ResourceLocation resourceLocation, int score) {
            this.resourceLocation = resourceLocation;
            this.score = score;
        }
    }
    
    /**
     * Find effects that match a search term across all specified mods
     */
    public static List<ResourceLocation> findMatchingEffects(String searchTerm, List<String> modIds) {
        String normalizedSearch = searchTerm.toLowerCase().replace(" ", "_");
        List<ResourceLocation> matches = new ArrayList<>();
        
        for (ResourceLocation effectKey : BuiltInRegistries.MOB_EFFECT.keySet()) {
            // Only search in specified mod namespaces
            if (!modIds.contains(effectKey.getNamespace())) continue;
            
            String path = effectKey.getPath().toLowerCase();
            
            // Exact match (highest priority)
            if (path.equals(normalizedSearch)) {
                matches.add(0, effectKey); // Add to front
                continue;
            }
            
            // Contains match
            if (path.contains(normalizedSearch) || normalizedSearch.contains(path)) {
                matches.add(effectKey);
            }
        }
        
        LOGGER.info("🔥 Found {} matching effects for '{}' in mods {}: {}", 
            matches.size(), searchTerm, modIds, matches);
        
        return matches;
    }
    
    /**
     * Generate a debug report of all available content for testing AI knowledge
     */
    public static String generateDebugReport() {
        StringBuilder report = new StringBuilder();
        
        report.append("=== MINECRAFT REGISTRY DEBUG REPORT ===\n");
        report.append("Generated at: ").append(new Date()).append("\n\n");
        
        // Count registries
        Map<String, Integer> namespaceCounts = new HashMap<>();
        for (ResourceLocation key : BuiltInRegistries.ITEM.keySet()) {
            namespaceCounts.merge(key.getNamespace(), 1, Integer::sum);
        }
        
        report.append("ITEM COUNTS BY MOD:\n");
        namespaceCounts.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .forEach(entry -> report.append("  ")
                .append(entry.getKey())
                .append(": ")
                .append(entry.getValue())
                .append(" items\n"));
        
        report.append("\nSAMPLE ITEMS FROM EACH MOD:\n");
        for (String namespace : namespaceCounts.keySet()) {
            List<String> sampleItems = getRegistryEntriesForMod(BuiltInRegistries.ITEM.keySet(), namespace);
            report.append("  ").append(namespace).append(": ");
            report.append(String.join(", ", sampleItems.subList(0, Math.min(5, sampleItems.size()))));
            if (sampleItems.size() > 5) report.append(" ... (").append(sampleItems.size() - 5).append(" more)");
            report.append("\n");
        }
        
        return report.toString();
    }
}
