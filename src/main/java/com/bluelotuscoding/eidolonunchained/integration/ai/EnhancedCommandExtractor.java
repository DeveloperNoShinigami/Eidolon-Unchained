package com.bluelotuscoding.eidolonunchained.integration.ai;

import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enhanced AI command extractor that can parse natural language commands
 * and convert them to proper Minecraft commands for execution.
 * 🔥 DYNAMIC REGISTRY-BASED - NO HARDCODING!
 */
public class EnhancedCommandExtractor {
    private static final Logger LOGGER = LogManager.getLogger();
    
    // Patterns for natural language command detection
    private static final Pattern GIVE_PATTERN = Pattern.compile(
        "\\b(?:give|grant|bestow)\\s+(?:(?:you|player|\\{player\\})\\s+)?([\\w:_-]+)(?:\\s+(\\d+))?", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern EFFECT_PATTERN = Pattern.compile(
        "\\b(?:effect|blessing|curse|apply)\\s+(?:give\\s+)?(?:(?:you|player|\\{player\\})\\s+)?([\\w:_-]+)(?:\\s+(\\d+))?(?:\\s+(\\d+))?", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern EXPLICIT_COMMAND_PATTERN = Pattern.compile(
        "/(\\w+)\\s+([^\\n\\]]+)", 
        Pattern.CASE_INSENSITIVE
    );
    
    // Pattern for [ACTION:give item] format used by AI responses
    private static final Pattern ACTION_PATTERN = Pattern.compile(
        "\\[ACTION:(?:give|grant|bestow|gift)\\s+([\\w:_-]+)(?:\\s+(\\d+))?\\]",
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * Extract and convert AI natural language to executable commands
     * 🔥 NOW WITH DYNAMIC REGISTRY CONTEXT
     */
    public static List<String> extractAndConvertCommands(String aiResponse, ServerPlayer player, List<String> modContextIds) {
        List<String> commands = new ArrayList<>();
        
        try {
            // Clean the AI response first
            String cleanResponse = cleanAIResponse(aiResponse);
            
            // 1. Look for [ACTION:...] patterns first (highest priority)
            Matcher actionMatcher = ACTION_PATTERN.matcher(cleanResponse);
            while (actionMatcher.find()) {
                String item = actionMatcher.group(1);
                String amount = actionMatcher.group(2);
                
                // 🔥 Use dynamic registry lookup
                String normalizedItem = normalizeItemId(item, modContextIds);
                if (normalizedItem != null) {
                    String command = String.format("/give %s %s %s", 
                        player.getName().getString(),
                        normalizedItem,
                        amount != null ? amount : "1"
                    );
                    commands.add(command);
                    LOGGER.info("🔥 Extracted ACTION command: {} -> {}", actionMatcher.group(0), command);
                }
            }
            
            // 2. Look for explicit command patterns
            Matcher explicitMatcher = EXPLICIT_COMMAND_PATTERN.matcher(cleanResponse);
            while (explicitMatcher.find()) {
                String command = explicitMatcher.group(0);
                String cleanCommand = cleanAndValidateCommand(command, player);
                if (cleanCommand != null) {
                    commands.add(cleanCommand);
                    LOGGER.debug("Found explicit command: {}", cleanCommand);
                }
            }
            
            // 3. Look for give/grant patterns
            Matcher giveMatcher = GIVE_PATTERN.matcher(cleanResponse);
            while (giveMatcher.find()) {
                String item = giveMatcher.group(1);
                String amount = giveMatcher.group(2);
                
                // 🔥 Use dynamic registry lookup
                String normalizedItem = normalizeItemId(item, modContextIds);
                if (normalizedItem != null) {
                    String command = String.format("/give %s %s %s", 
                        player.getName().getString(),
                        normalizedItem,
                        amount != null ? amount : "1"
                    );
                    commands.add(command);
                    LOGGER.debug("Converted give pattern to command: {}", command);
                }
            }
            
            // 4. Look for effect patterns
            Matcher effectMatcher = EFFECT_PATTERN.matcher(cleanResponse);
            while (effectMatcher.find()) {
                String effect = effectMatcher.group(1);
                String duration = effectMatcher.group(2);
                String amplifier = effectMatcher.group(3);
                
                // 🔥 Use dynamic registry lookup
                String normalizedEffect = normalizeEffectId(effect, modContextIds);
                if (normalizedEffect != null) {
                    String command = String.format("/effect give %s %s %s %s", 
                        player.getName().getString(),
                        normalizedEffect,
                        duration != null ? duration : "300",
                        amplifier != null ? amplifier : "0"
                    );
                    commands.add(command);
                    LOGGER.debug("Converted effect pattern to command: {}", command);
                }
            }
            
            // 5. Look for common deity actions and convert to commands
            commands.addAll(extractDeityActions(cleanResponse, player, modContextIds));
            
        } catch (Exception e) {
            LOGGER.error("Error extracting commands from AI response: {}", e.getMessage());
        }
        
        return commands;
    }
    
    /**
     * Overload for backward compatibility
     */
    public static List<String> extractAndConvertCommands(String aiResponse, ServerPlayer player) {
        return extractAndConvertCommands(aiResponse, player, Arrays.asList("minecraft", "eidolon", "eidolonunchained"));
    }
    
    /**
     * Clean AI response text for better parsing
     */
    private static String cleanAIResponse(String response) {
        if (response == null) return "";
        
        // Remove markdown formatting
        String cleaned = response.replaceAll("\\*\\*([^*]+)\\*\\*", "$1"); // Bold
        cleaned = cleaned.replaceAll("\\*([^*]+)\\*", "$1"); // Italic
        cleaned = cleaned.replaceAll("`([^`]+)`", "$1"); // Code
        
        // Remove extra whitespace
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        
        return cleaned;
    }
    
    /**
     * Clean and validate a command string
     */
    private static String cleanAndValidateCommand(String command, ServerPlayer player) {
        if (command == null || command.trim().isEmpty()) return null;
        
        // Remove extra brackets and clean up
        String cleaned = command.replaceAll("\\]\\s*$", ""); // Remove trailing brackets
        cleaned = cleaned.replaceAll("\\s+", " ").trim(); // Normalize whitespace
        
        // Replace placeholders
        cleaned = cleaned.replace("{player}", player.getName().getString());
        cleaned = cleaned.replace("@p", player.getName().getString());
        
        // Ensure command starts with /
        if (!cleaned.startsWith("/")) {
            cleaned = "/" + cleaned;
        }
        
        // Basic validation - ensure it's a reasonable command
        if (cleaned.length() > 200) return null; // Too long
        if (cleaned.contains("..") || cleaned.contains("//")) return null; // Suspicious patterns
        
        return cleaned;
    }
    
    /**
     * Extract common deity actions and convert to commands
     * Backward compatibility version
     */
    private static List<String> extractDeityActions(String response, ServerPlayer player) {
        return extractDeityActions(response, player, Arrays.asList("minecraft", "eidolon", "eidolonunchained"));
    }
    
    /**
     * Extract common deity actions and convert to commands
     * 🔥 Now uses dynamic registry context instead of hardcoded effects
     */
    private static List<String> extractDeityActions(String response, ServerPlayer player, List<String> modContextIds) {
        List<String> commands = new ArrayList<>();
        String lowerResponse = response.toLowerCase();
        String playerName = player.getName().getString();
        
        // 🔥 DYNAMIC NATURAL LANGUAGE PATTERNS - NO HARDCODING!
        
        // Pattern: "Take this [item]" / "I grant you [item]" / "Receive this [item]"
        Pattern naturalGiftPattern = Pattern.compile(
            "\\b(?:take|receive|accept|i (?:grant|give|bestow)|here is)\\s+(?:this\\s+|a\\s+)?([\\w\\s:_-]+?)(?:\\s*[,.]|$)",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher naturalGiftMatcher = naturalGiftPattern.matcher(response);
        while (naturalGiftMatcher.find()) {
            String itemText = naturalGiftMatcher.group(1).trim();
            // 🔥 USE DYNAMIC REGISTRY MATCHING - Let the registry find the best match
            String itemId = normalizeItemId(itemText.replace(" ", "_"), modContextIds);
            if (itemId != null) {
                commands.add(String.format("give %s %s 1", playerName, itemId));
                LOGGER.info("🔥 Extracted natural gift: '{}' -> {}", naturalGiftMatcher.group(0), itemId);
            }
        }
        
        // 🔥 DYNAMIC ITEM DETECTION - Find ANY mentioned item using fuzzy registry matching
        Pattern itemMentionPattern = Pattern.compile("\\b([a-z_]+(?:_[a-z_]+)*)\\b", Pattern.CASE_INSENSITIVE);
        Matcher itemMentionMatcher = itemMentionPattern.matcher(lowerResponse);
        while (itemMentionMatcher.find()) {
            String potentialItem = itemMentionMatcher.group(1);
            // Skip common words that aren't items
            if (isCommonWord(potentialItem)) continue;
            
            String itemId = normalizeItemId(potentialItem, modContextIds);
            if (itemId != null) {
                commands.add(String.format("give %s %s 1", playerName, itemId));
                LOGGER.info("🔥 Detected item mention: '{}' -> {}", potentialItem, itemId);
                break; // Only give one item per response to avoid spam
            }
        }
        
        // 🔥 DYNAMIC EFFECT DETECTION - Find ANY mentioned effect using fuzzy registry matching
        Pattern effectMentionPattern = Pattern.compile("\\b([a-z_]+(?:_[a-z_]+)*)\\b", Pattern.CASE_INSENSITIVE);
        Matcher effectMentionMatcher = effectMentionPattern.matcher(lowerResponse);
        while (effectMentionMatcher.find()) {
            String potentialEffect = effectMentionMatcher.group(1);
            // Skip common words that aren't effects
            if (isCommonWord(potentialEffect)) continue;
            
            String effectId = normalizeEffectId(potentialEffect, modContextIds);
            if (effectId != null) {
                // Determine duration and amplifier based on context
                int duration = lowerResponse.contains("brief") ? 300 : 600;
                int amplifier = lowerResponse.contains("strong") || lowerResponse.contains("powerful") ? 2 : 1;
                
                commands.add(String.format("effect give %s %s %d %d", playerName, effectId, duration, amplifier));
                LOGGER.info("🔥 Detected effect mention: '{}' -> {}", potentialEffect, effectId);
                break; // Only apply one effect per response to avoid spam
            }
        }
        
        return commands;
    }
    
    /**
     * 🔥 DYNAMIC ITEM ID NORMALIZATION - Uses actual registries!
     * Backward compatibility version that uses default mods
     */
    private static String normalizeItemId(String item) {
        return normalizeItemId(item, Arrays.asList("minecraft", "eidolon", "eidolonunchained"));
    }
    
    /**
     * 🔥 DYNAMIC ITEM ID NORMALIZATION - Uses actual registries!
     * Full version with mod context control
     */
    private static String normalizeItemId(String item, List<String> modContextIds) {
        if (item == null || item.trim().isEmpty()) return null;
        
        item = item.toLowerCase().trim().replace(" ", "_");
        
        // Already properly formatted with namespace
        if (item.contains(":")) {
            ResourceLocation itemId = ResourceLocation.tryParse(item);
            if (itemId != null && BuiltInRegistries.ITEM.containsKey(itemId)) {
                return item;
            }
            return null; // Invalid namespaced item
        }
        
        // 🔥 DYNAMIC REGISTRY SEARCH - Use context mods first, then fallback
        List<String> searchMods = modContextIds != null ? new ArrayList<>(modContextIds) : 
            Arrays.asList("minecraft", "eidolon", "eidolonunchained");
        
        // Try exact matches in context mods first
        for (String namespace : searchMods) {
            ResourceLocation testId = new ResourceLocation(namespace, item);
            if (BuiltInRegistries.ITEM.containsKey(testId)) {
                LOGGER.info("🔥 Found exact item match in {} registry: {} -> {}", namespace, item, testId);
                return testId.toString();
            }
        }
        
        // 🔥 USE REGISTRY CONTEXT PROVIDER for fuzzy matching
        List<ResourceLocation> matches = RegistryContextProvider.findMatchingItems(item, searchMods);
        if (!matches.isEmpty()) {
            ResourceLocation bestMatch = matches.get(0); // First is best match
            LOGGER.info("🔥 Found fuzzy item match: {} -> {}", item, bestMatch);
            return bestMatch.toString();
        }
        
        // 🔥 BLOCK REGISTRY SEARCH - Maybe they meant a block
        for (String namespace : searchMods) {
            ResourceLocation testId = new ResourceLocation(namespace, item);
            if (BuiltInRegistries.BLOCK.containsKey(testId)) {
                LOGGER.info("🔥 Found block in {} registry: {} -> {}", namespace, item, testId);
                return testId.toString(); // Blocks can be given as items
            }
        }
        
        LOGGER.warn("🔥 Could not find item '{}' in any registry from mods {}. Available items: {}", 
            item, searchMods, BuiltInRegistries.ITEM.keySet().size());
        return null; // Don't guess - return null if not found
    }
    
    /**
     * 🔥 DYNAMIC EFFECT ID NORMALIZATION - Uses actual registries!
     * Backward compatibility version
     */
    private static String normalizeEffectId(String effect) {
        return normalizeEffectId(effect, Arrays.asList("minecraft", "eidolon", "eidolonunchained"));
    }
    
    /**
     * 🔥 DYNAMIC EFFECT ID NORMALIZATION - Uses actual registries!
     * Full version with mod context control
     */
    private static String normalizeEffectId(String effect, List<String> modContextIds) {
        if (effect == null || effect.trim().isEmpty()) return null;
        
        effect = effect.toLowerCase().trim().replace(" ", "_");
        
        // Already properly formatted with namespace
        if (effect.contains(":")) {
            ResourceLocation effectId = ResourceLocation.tryParse(effect);
            if (effectId != null && BuiltInRegistries.MOB_EFFECT.containsKey(effectId)) {
                return effect;
            }
            return null; // Invalid namespaced effect
        }
        
        // 🔥 DYNAMIC REGISTRY SEARCH - Use context mods first
        List<String> searchMods = modContextIds != null ? new ArrayList<>(modContextIds) : 
            Arrays.asList("minecraft", "eidolon", "eidolonunchained");
        
        // Try exact matches in context mods first
        for (String namespace : searchMods) {
            ResourceLocation testId = new ResourceLocation(namespace, effect);
            if (BuiltInRegistries.MOB_EFFECT.containsKey(testId)) {
                LOGGER.info("🔥 Found exact effect match in {} registry: {} -> {}", namespace, effect, testId);
                return testId.toString();
            }
        }
        
        // 🔥 USE REGISTRY CONTEXT PROVIDER for fuzzy matching
        List<ResourceLocation> matches = RegistryContextProvider.findMatchingEffects(effect, searchMods);
        if (!matches.isEmpty()) {
            ResourceLocation bestMatch = matches.get(0); // First is best match
            LOGGER.info("🔥 Found fuzzy effect match: {} -> {}", effect, bestMatch);
            return bestMatch.toString();
        }
        
        LOGGER.warn("🔥 Could not find effect '{}' in any registry from mods {}. Available effects: {}", 
            effect, searchMods, BuiltInRegistries.MOB_EFFECT.keySet().size());
        return null; // Don't guess - return null if not found
    }
    
    /**
     * Extract common deity actions and convert to commands
    
    /**
     * Generic method to find ResourceLocation in any registry
     * This can be used for biomes, entities, enchantments, etc.
     */
    private static String findInRegistry(String name, String registryType) {
        if (name == null || name.trim().isEmpty()) return null;
        
        name = name.toLowerCase().trim().replace(" ", "_");
        
        // Already properly formatted with namespace
        if (name.contains(":")) {
            ResourceLocation resourceId = ResourceLocation.tryParse(name);
            if (resourceId != null) {
                return name; // Assume valid if properly formatted
            }
            return null;
        }
        
        // Try common namespaces
        String[] namespaces = {"minecraft", "eidolon", "eidolonunchained", "forge"};
        
        for (String namespace : namespaces) {
            ResourceLocation testId = new ResourceLocation(namespace, name);
            // Note: We can't easily check all registries generically,
            // but this method can be expanded for specific registry types
            LOGGER.info("🔥 Checking {} registry for: {} -> {}", registryType, name, testId);
        }
        
        // Default to minecraft namespace as last resort
        return "minecraft:" + name;
    }
    
    /**
     * Clean response for display by removing command patterns but keeping conversational content
     */
    public static String cleanResponseForDisplay(String response) {
        if (response == null) return "";
        
        String cleaned = response;
        
        // Remove [ACTION:...] patterns first
        cleaned = ACTION_PATTERN.matcher(cleaned).replaceAll("");
        
        // Remove explicit command patterns
        cleaned = EXPLICIT_COMMAND_PATTERN.matcher(cleaned).replaceAll("");
        
        // Remove give patterns that were converted to commands
        cleaned = GIVE_PATTERN.matcher(cleaned).replaceAll("");
        
        // Remove effect patterns that were converted to commands
        cleaned = EFFECT_PATTERN.matcher(cleaned).replaceAll("");
        
        // Log what we're cleaning for debugging
        LOGGER.debug("🔥 Cleaning AI response for display - removed command patterns");
        
        // Clean up markdown and extra whitespace
        cleaned = cleanAIResponse(cleaned);
        
        // Remove empty lines and normalize spacing
        cleaned = cleaned.replaceAll("\\n\\s*\\n", "\n").trim();
        
        return cleaned;
    }
    
    /**
     * Check if a word is too common to be an item/effect name
     * Prevents false positives in dynamic registry matching
     */
    private static boolean isCommonWord(String word) {
        if (word == null || word.length() < 3) return true;
        
        // Common English words that shouldn't be interpreted as items/effects
        String[] commonWords = {
            "the", "and", "you", "are", "for", "not", "but", "can", "has", "had", 
            "this", "that", "with", "will", "your", "from", "they", "have", "been",
            "take", "give", "make", "come", "know", "time", "work", "look", "get",
            "way", "see", "him", "two", "may", "say", "she", "use", "her", "each",
            "which", "their", "said", "how", "out", "many", "them", "these", "so",
            "some", "what", "would", "make", "like", "into", "than", "find", "was",
            "more", "very", "when", "where", "much", "before", "right", "too", "any",
            "same", "tell", "boy", "follow", "came", "want", "show", "also", "around",
            "form", "three", "small", "set", "put", "end", "why", "turn", "ask",
            "went", "men", "read", "need", "land", "different", "home", "move", "try",
            "kind", "hand", "picture", "again", "change", "off", "play", "spell",
            "air", "away", "animal", "house", "point", "page", "letter", "mother",
            "answer", "found", "study", "still", "learn", "should", "america", "world"
        };
        
        String lowerWord = word.toLowerCase();
        for (String common : commonWords) {
            if (common.equals(lowerWord)) return true;
        }
        
        return false;
    }
    
    /**
     * Execute commands with enhanced error handling and feedback
     */
    public static int executeCommands(List<String> commands, ServerPlayer player) {
        if (commands.isEmpty()) return 0;
        
        MinecraftServer server = player.getServer();
        if (server == null) {
            LOGGER.error("Cannot execute commands: server is null");
            return 0;
        }
        
        CommandSourceStack commandSource = server.createCommandSourceStack()
            .withSource(CommandSource.NULL)
            .withLevel(player.serverLevel())
            .withPosition(player.position())
            .withPermission(4); // Admin permission level
        
        int successCount = 0;
        for (String command : commands) {
            try {
                // Clean and validate the command before execution
                String cleanCommand = cleanAndValidateCommand(command, player);
                if (cleanCommand == null) {
                    LOGGER.warn("Skipping invalid command: {}", command);
                    continue;
                }
                
                // Remove leading slash for execution
                String execCommand = cleanCommand.startsWith("/") ? cleanCommand.substring(1) : cleanCommand;
                
                LOGGER.info("Executing deity command for {}: {}", player.getName().getString(), cleanCommand);
                int result = server.getCommands().performPrefixedCommand(commandSource, execCommand);
                
                if (result > 0) {
                    successCount++;
                    LOGGER.debug("Command executed successfully: {}", cleanCommand);
                } else {
                    LOGGER.warn("Command returned 0 result: {}", cleanCommand);
                }
                
            } catch (Exception e) {
                LOGGER.error("Failed to execute deity command '{}' for player {}: {}", 
                    command, player.getName().getString(), e.getMessage());
            }
        }
        
        // Send feedback to player about divine intervention
        if (successCount > 0) {
            player.sendSystemMessage(Component.literal(
                "§6✦ Divine power flows through you... §7(" + successCount + 
                " blessing" + (successCount == 1 ? "" : "s") + " granted)"));
        }
        
        return successCount;
    }
}
