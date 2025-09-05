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
     * Handles searches like "zombie heart" or "bone paladin helm"
     */
    public static List<ResourceLocation> findMatchingItemsWithScoring(String searchTerm, List<String> modIds) {
        // Split search term into individual words
        String[] searchWords = searchTerm.toLowerCase().split("\\s+");
        List<ScoredItem> scoredItems = new ArrayList<>();
        
        // If single word, use existing logic
        if (searchWords.length == 1) {
            return findMatchingItems(searchTerm, modIds);
        }
        
        // For multi-word search, expand to all loaded mods if only default mods specified
        List<String> searchMods = modIds;
        if (modIds.size() <= 3 && modIds.contains("minecraft") && modIds.contains("eidolon")) {
            searchMods = getAllLoadedModIds();
            LOGGER.info("🔥 Multi-word search '{}' expanded to all {} loaded mods", searchTerm, searchMods.size());
        }
        
        // Multi-word search: score items based on how many words they contain
        for (ResourceLocation itemKey : BuiltInRegistries.ITEM.keySet()) {
            // Only search in specified mod namespaces
            if (!searchMods.contains(itemKey.getNamespace())) continue;
            
            String path = itemKey.getPath().toLowerCase();
            int score = calculateItemScore(path, searchWords);
            
            if (score > 0) {
                scoredItems.add(new ScoredItem(itemKey, score));
            }
        }
        
        // Sort by score (highest first)
        scoredItems.sort((a, b) -> Integer.compare(b.score, a.score));
        
        // Extract ResourceLocations
        List<ResourceLocation> matches = scoredItems.stream()
            .map(item -> item.resourceLocation)
            .collect(Collectors.toList());
        
        LOGGER.info("🔥 Enhanced search for '{}' found {} items in {} mods. Top matches: {}", 
            searchTerm, matches.size(), searchMods.size(),
            matches.subList(0, Math.min(5, matches.size())));
        
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
     * Calculate score for an item based on how many search words it contains
     * Higher score = better match
     */
    private static int calculateItemScore(String itemPath, String[] searchWords) {
        int score = 0;
        
        for (String word : searchWords) {
            if (itemPath.contains(word)) {
                // Exact word match gets highest score
                if (itemPath.equals(word)) {
                    score += 10;
                } else if (itemPath.startsWith(word) || itemPath.endsWith(word)) {
                    // Word at start/end gets high score
                    score += 5;
                } else {
                    // Word anywhere in item gets basic score
                    score += 2;
                }
            }
        }
        
        // Bonus for items that contain all search words
        boolean containsAllWords = true;
        for (String word : searchWords) {
            if (!itemPath.contains(word)) {
                containsAllWords = false;
                break;
            }
        }
        
        if (containsAllWords) {
            score += 5; // Bonus for containing all words
        }
        
        return score;
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
