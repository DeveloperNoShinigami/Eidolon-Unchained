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
import java.util.Set;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enhanced AI command extractor that can parse natural language commands
 * and convert them to proper Minecraft commands for execution.
 * 🔥 DYNAMIC REGISTRY-BASED - NO HARDCODING!
 */
public class EnhancedCommandExtractor {
    private static final Logger LOGGER = LogManager.getLogger();
    
    // Patterns for natural language command detection - MORE CONSERVATIVE
    private static final Pattern EXPLICIT_REQUEST_PATTERN = Pattern.compile(
        "\\b(?:give me|grant me|bless me with|i need|i want|can you give|please give)\\s+([\\w\\s:_-]+?)(?:\\s*[?!.]|$)", 
        Pattern.CASE_INSENSITIVE
    );
    
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
    
    // 🔥 NEW: Track last performed chants for prayer type detection
    private static final Map<UUID, com.bluelotuscoding.eidolonunchained.chant.DatapackChant> lastPerformedChants = new ConcurrentHashMap<>();
    
    // 🔥 NEW: Track prayer cooldowns by player and prayer type
    private static final Map<String, Long> prayerCooldowns = new ConcurrentHashMap<>();
    
    /**
     * Store the last chant performed by a player (called from DatapackChantSpell.cast)
     */
    public static void setLastPerformedChant(ServerPlayer player, com.bluelotuscoding.eidolonunchained.chant.DatapackChant chant) {
        lastPerformedChants.put(player.getUUID(), chant);
        LOGGER.info("🔥 Stored last performed chant for {}: {}", player.getName().getString(), chant.getId());
    }
    
    /**
     * Clear the last performed chant for a player
     */
    public static void clearLastPerformedChant(ServerPlayer player) {
        lastPerformedChants.remove(player.getUUID());
    }
    
    /**
     * Send a denial message from the deity to the player
     */
    private static void sendDeityDenialMessage(ServerPlayer player, String requestedItem, String reason) {
        try {
            net.minecraft.resources.ResourceLocation activeDeityId = getActiveDeityForPlayer(player);
            if (activeDeityId != null) {
                com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig aiConfig = 
                    com.bluelotuscoding.eidolonunchained.ai.AIDeityManager.getInstance().getAIConfig(activeDeityId);
                if (aiConfig != null) {
                    // Get deity name from config
                    String deityName = aiConfig.deity_id.getPath().replace("_deity", "").replace("_", " ");
                    deityName = deityName.substring(0, 1).toUpperCase() + deityName.substring(1);
                    
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§c✦ " + deityName + " speaks: §7\"I cannot grant you " + requestedItem + ". " + reason + "\""));
                    return;
                }
            }
            
            // Fallback generic message
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§c✦ The divine powers do not permit this request for " + requestedItem + "."));
                
        } catch (Exception e) {
            LOGGER.error("Error sending deity denial message: {}", e.getMessage());
        }
    }
    
    // Common words to filter out to prevent false positives
    private static final Set<String> COMMON_WORDS = Set.of(
        "the", "and", "or", "but", "with", "from", "of", "in", "on", "at", "by", "for", "as", "to", "is", "are", "was", "were",
        "you", "your", "me", "my", "i", "we", "they", "them", "this", "that", "these", "those", "here", "there",
        "dark", "light", "shadow", "void", "breath", "pulse", "hands", "eyes", "power", "energy", "spirit", "soul"
    );
    
    /**
     * Extract and convert AI natural language to executable commands
     * 🔥 NOW WITH DYNAMIC REGISTRY CONTEXT
     */
    public static List<String> extractAndConvertCommands(String aiResponse, ServerPlayer player, List<String> modContextIds) {
        List<String> commands = new ArrayList<>();
        
        try {
            // Clean the input first
            String cleanResponse = cleanAIResponse(aiResponse);
            
            // 🔥 PRIORITY 1: Look for explicit player requests (most important)
            Matcher explicitRequestMatcher = EXPLICIT_REQUEST_PATTERN.matcher(cleanResponse);
            while (explicitRequestMatcher.find()) {
                String requestedItem = explicitRequestMatcher.group(1).trim();
                
                // Filter out common words to prevent false positives
                if (!isCommonWord(requestedItem)) {
                    List<ResourceLocation> matches = com.bluelotuscoding.eidolonunchained.integration.ai.RegistryContextProvider
                        .findMatchingItemsWithScoring(requestedItem, Arrays.asList("minecraft", "eidolon", "eidolonunchained"));
                    
                    if (!matches.isEmpty()) {
                        String normalizedItem = matches.get(0).toString(); // Take best match
                        String command = String.format("/give %s %s 1", player.getName().getString(), normalizedItem);
                        commands.add(command);
                        LOGGER.info("🔥 Player explicit request: '{}' -> {}", explicitRequestMatcher.group(0), normalizedItem);
                    }
                }
            }
            
            // 🔥 PRIORITY 2: Look for [ACTION:...] patterns (old format compatibility)
            Matcher actionMatcher = ACTION_PATTERN.matcher(cleanResponse);
            while (actionMatcher.find()) {
                String item = actionMatcher.group(1);
                String amount = actionMatcher.group(2);
                
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
            
            // 🔥 PRIORITY 3: Look for explicit blessing/help requests only
            if (cleanResponse.toLowerCase().contains("help") || cleanResponse.toLowerCase().contains("bless")) {
                // 3a. Look for give/grant patterns only if player asked for help
                Matcher giveMatcher = GIVE_PATTERN.matcher(cleanResponse);
                while (giveMatcher.find()) {
                    String item = giveMatcher.group(1);
                    String amount = giveMatcher.group(2);
                    
                    if (!isCommonWord(item)) {
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
                }
                
                // 3b. Look for effect patterns only if player asked for help
                Matcher effectMatcher = EFFECT_PATTERN.matcher(cleanResponse);
                while (effectMatcher.find()) {
                    String effect = effectMatcher.group(1);
                    String duration = effectMatcher.group(2);
                    String amplifier = effectMatcher.group(3);
                    
                    if (!isCommonWord(effect)) {
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
                }
            }
            
            // Skip the old aggressive deity actions - let the AI decide naturally
            
        } catch (Exception e) {
            LOGGER.error("Error extracting commands from input: {}", e.getMessage());
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
     * Check if a word is too common to be an item/effect name
     */
    private static boolean isCommonWord(String word) {
        if (word == null || word.trim().isEmpty()) return true;
        
        String normalized = word.toLowerCase().trim();
        
        // Check against common words set
        if (COMMON_WORDS.contains(normalized)) return true;
        
        // Check for overly short words (likely articles/prepositions)
        if (normalized.length() <= 2) return true;
        
        // Check for very long phrases (likely not item names)
        if (normalized.length() > 30) return true;
        
        return false;
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
    
    // 🔥 OLD AGGRESSIVE DEITY ACTIONS REMOVED
    // These methods were causing inappropriate item giving by scanning AI narrative
    // for random words that might be items. Replaced with explicit request detection.
    
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
        List<ResourceLocation> matches = RegistryContextProvider.findMatchingItemsWithScoring(item, searchMods);
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
            .withSource(player)  // 🔧 FIX: Use player as source so @s selector works
            .withLevel(player.serverLevel())
            .withPosition(player.position())
            .withPermission(4) // Admin permission level
            .withSuppressedOutput(); // 🔥 SILENT: Suppress command output to chat
        
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
                
                LOGGER.info("🔧 Executing deity command for {}: {}", player.getName().getString(), cleanCommand);
                int result = server.getCommands().performPrefixedCommand(commandSource, execCommand);
                
                if (result > 0) {
                    successCount++;
                    LOGGER.info("✅ Command executed successfully: {} (result: {})", cleanCommand, result);
                } else {
                    // 🔧 FALLBACK: If @s selector failed, try with explicit player name
                    if (execCommand.contains("@s")) {
                        String fallbackCommand = execCommand.replace("@s", player.getName().getString());
                        LOGGER.info("🔄 Retrying command with explicit player name: {}", fallbackCommand);
                        
                        int fallbackResult = server.getCommands().performPrefixedCommand(commandSource, fallbackCommand);
                        if (fallbackResult > 0) {
                            successCount++;
                            LOGGER.info("✅ Fallback command succeeded: {} (result: {})", fallbackCommand, fallbackResult);
                        } else {
                            LOGGER.warn("❌ Both @s and explicit name failed for command: {}", cleanCommand);
                        }
                    } else {
                        LOGGER.warn("❌ Command returned 0 result: {} (No @s selector to retry)", cleanCommand);
                    }
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
    
    /**
     * 🔥 NEW: Extract only explicit player requests like "give me", "I need", "can I have"
     */
    public static List<String> extractExplicitRequests(String playerInput, ServerPlayer player) {
        LOGGER.info("🔥 Starting explicit request extraction for input: '{}'", playerInput);
        List<String> commands = new ArrayList<>();
        
        // Pattern for explicit requests from player
        Pattern explicitPattern = Pattern.compile(
            "\\b(?:give\\s+me|i\\s+(?:need|want|require)|can\\s+i\\s+(?:have|get)|please\\s+(?:give|grant))\\s+(?:a\\s+|an\\s+|some\\s+)?([\\w\\s:_-]+?)(?:\\s*[,.?!]|$)",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = explicitPattern.matcher(playerInput);
        LOGGER.info("🔥 Compiled regex pattern for explicit requests");
        
        try {
            // Get active deity to check permissions
            net.minecraft.resources.ResourceLocation activeDeityId = getActiveDeityForPlayer(player);
            com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig aiConfig = null;
            
            if (activeDeityId != null) {
                aiConfig = com.bluelotuscoding.eidolonunchained.ai.AIDeityManager.getInstance().getAIConfig(activeDeityId);
                LOGGER.info("🔥 Found active deity: {} with config: {}", activeDeityId, aiConfig != null);
            } else {
                LOGGER.info("🔥 No active deity found for player");
            }
            
            int matchCount = 0;
            while (matcher.find()) {
                matchCount++;
                String fullMatch = matcher.group(0);
                String requestedItem = matcher.group(1).trim();
                
                LOGGER.info("🔥 Match {}: Full='{}', Item='{}' (length: {})", 
                    matchCount, fullMatch, requestedItem, requestedItem.length());
                
                // Filter out common words with detailed logging
                if (!isCommonWord(requestedItem)) {
                    LOGGER.info("🔥 Item '{}' passed common word filter", requestedItem);
                    
                    // Get deity-specific mod context IDs for registry lookup
                    List<String> modContextIds = Arrays.asList("minecraft", "eidolon", "eidolonunchained"); // default
                    if (aiConfig != null && aiConfig.mod_context_ids != null && !aiConfig.mod_context_ids.isEmpty()) {
                        modContextIds = aiConfig.mod_context_ids;
                        LOGGER.info("🔧 Using deity-specific mod context IDs: {}", modContextIds);
                    } else {
                        LOGGER.info("🔧 Using default mod context IDs: {}", modContextIds);
                    }
                    
                    // Step 1: Registry lookup to find the actual item using deity's mod context
                    LOGGER.info("🔍 Searching registry for item: '{}' with contexts: {}", requestedItem, modContextIds);
                    List<ResourceLocation> matches = com.bluelotuscoding.eidolonunchained.integration.ai.RegistryContextProvider
                        .findMatchingItemsWithScoring(requestedItem, modContextIds);
                    
                    LOGGER.info("🔍 Registry search returned {} matches: {}", matches.size(), matches);
                    
                    if (!matches.isEmpty()) {
                        String itemId = matches.get(0).toString();
                        LOGGER.info("🔍 Best match: '{}'", itemId);
                        
                        // Step 2: Check if deity allows this specific item
                        boolean allowed = deityAllowsItem(itemId, aiConfig, player);
                        LOGGER.info("🔐 Deity permission check: {} -> {}", itemId, allowed ? "ALLOWED" : "DENIED");
                        
                        if (allowed) {
                            String command = String.format("give %s %s 1", player.getName().getString(), itemId);
                            commands.add(command);
                            LOGGER.info("🔥 Player request APPROVED: '{}' -> {} (deity: {})", 
                                fullMatch, itemId, activeDeityId);
                        } else {
                            LOGGER.info("🚫 Player request DENIED: '{}' -> {} (not allowed by deity: {})", 
                                fullMatch, itemId, activeDeityId);
                            // Send denial message to player explaining why
                            sendDeityDenialMessage(player, requestedItem, "This item is not within my divine domain to grant.");
                        }
                    } else {
                        LOGGER.info("🔍 Player request UNKNOWN: '{}' (no registry match found)", requestedItem);
                    }
                } else {
                    LOGGER.info("🚫 Item '{}' filtered out as common word", requestedItem);
                }
            }
            
            LOGGER.info("🔥 Explicit request extraction complete. Found {} matches, generated {} commands: {}", 
                matchCount, commands.size(), commands);
                
        } catch (Exception e) {
            LOGGER.error("🔥 Error in explicit request extraction: {}", e.getMessage(), e);
        }
        
        return commands;
    }
    
    /**
     * 🔥 NEW: Extract contextual actions from AI response (deity-specific approach)
     */
    public static List<String> extractContextualActions(String aiResponse, ServerPlayer player, String playerMessage) {
        List<String> commands = new ArrayList<>();
        
        try {
            // Get the current deity configuration to use THEIR specific commands
            net.minecraft.resources.ResourceLocation activeDeityId = getActiveDeityForPlayer(player);
            if (activeDeityId != null) {
                com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig aiConfig = 
                    com.bluelotuscoding.eidolonunchained.ai.AIDeityManager.getInstance().getAIConfig(activeDeityId);
                
                if (aiConfig != null && aiConfig.prayer_configs != null) {
                    // Use deity-specific reference commands based on context
                    commands.addAll(extractDeitySpecificCommands(aiResponse, playerMessage, aiConfig, player));
                    LOGGER.info("🔥 Using deity-specific commands from {}: {}", activeDeityId, commands);
                } else {
                    // Fallback to generic extraction only if no deity config
                    commands.addAll(extractGenericContextualActions(aiResponse, player, playerMessage));
                    LOGGER.info("🔥 Fallback to generic commands: {}", commands);
                }
            } else {
                // No active deity conversation - minimal extraction
                LOGGER.info("🔥 No active deity - minimal command extraction");
            }
        } catch (Exception e) {
            LOGGER.error("Error in contextual action extraction: {}", e.getMessage());
        }
        
        return commands;
    }
    
    /**
     * Extract commands using deity-specific configurations from JSON
     */
    private static List<String> extractDeitySpecificCommands(String aiResponse, String playerMessage, 
                                                            com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig aiConfig, 
                                                            ServerPlayer player) {
        List<String> commands = new ArrayList<>();
        
        // 🔥 NEW: Use dedicated prayer type resolver
        String prayerType = com.bluelotuscoding.eidolonunchained.prayer.PrayerTypeResolver
            .resolve(player, aiConfig.deity_id, playerMessage, aiResponse);
        LOGGER.info("🔥 Determined prayer type: {}", prayerType);
        
        if (aiConfig.prayer_configs.containsKey(prayerType)) {
            com.bluelotuscoding.eidolonunchained.ai.PrayerAIConfig prayerConfig = aiConfig.prayer_configs.get(prayerType);
            
            // Check if player meets requirements for this prayer type
            if (meetsRequirements(player, prayerConfig, aiConfig, prayerType)) {
                // 🔥 NEW: Use reputation-based command selection like the JSON intended
                commands.addAll(selectReputationBasedCommands(player, prayerConfig, aiConfig));
                
                LOGGER.info("🔥 Selected {} reputation-based commands for prayer type {} (player rep: {})", 
                    commands.size(), prayerType, getPlayerReputation(player, aiConfig));
            } else {
                LOGGER.info("🔥 Player {} doesn't meet requirements for prayer type {}", 
                    player.getName().getString(), prayerType);
            }
        } else {
            LOGGER.warn("🔥 Prayer type '{}' not found in deity config. Available types: {}", 
                prayerType, aiConfig.prayer_configs.keySet());
        }
        
        return commands;
    }
    
    /**
     * 🔥 NEW: Get the last chant performed by the player (if available)
     * This allows us to determine prayer type based on the actual chant performed
     */
    public static com.bluelotuscoding.eidolonunchained.chant.DatapackChant getLastPerformedChant(ServerPlayer player) {
        try {
            return lastPerformedChants.get(player.getUUID());
        } catch (Exception e) {
            LOGGER.error("🔥 Error getting last performed chant: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 🔥 NEW: Select commands based on player reputation using JSON logic
     * This implements the reputation-based blessing system described in the JSON base_prompts
     */
    private static List<String> selectReputationBasedCommands(ServerPlayer player, 
                                                             com.bluelotuscoding.eidolonunchained.ai.PrayerAIConfig prayerConfig,
                                                             com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig aiConfig) {
        List<String> commands = new ArrayList<>();
        
        try {
            double reputation = getPlayerReputation(player, aiConfig);
            List<String> referenceCommands = prayerConfig.reference_commands;
            
            if (referenceCommands == null || referenceCommands.isEmpty()) {
                LOGGER.warn("🔥 No reference commands available for prayer config");
                return commands;
            }
            
            // Determine progression level based on reputation (matching JSON base_prompts)
            String progressionLevel = determineProgressionLevel(reputation);
            int commandCount = determineCommandCount(reputation, prayerConfig.max_commands);
            
            LOGGER.info("🔥 Player reputation: {}, progression: {}, command count: {}", 
                reputation, progressionLevel, commandCount);
            
            // Select appropriate commands based on progression level
            List<String> selectedCommands = selectCommandsForProgression(
                referenceCommands, progressionLevel, reputation, commandCount);
            
            // Replace placeholders and add to final list
            for (String command : selectedCommands) {
                String processedCommand = command.replace("{player}", player.getName().getString());
                commands.add(processedCommand);
            }
            
            LOGGER.info("🔥 Final selected commands: {}", commands);
            
        } catch (Exception e) {
            LOGGER.error("🔥 Error in reputation-based command selection: {}", e.getMessage());
        }
        
        return commands;
    }
    
    /**
     * Get player reputation for the current deity
     */
    private static double getPlayerReputation(ServerPlayer player, com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig aiConfig) {
        try {
            com.bluelotuscoding.eidolonunchained.deity.DatapackDeity deity = 
                com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager.getDeity(aiConfig.deity_id);
            if (deity != null) {
                return deity.getPlayerReputation(player);
            }
        } catch (Exception e) {
            LOGGER.error("🔥 Error getting player reputation: {}", e.getMessage());
        }
        return 0.0;
    }
    
    /**
     * Determine progression level based on reputation (matching JSON base_prompts)
     */
    private static String determineProgressionLevel(double reputation) {
        if (reputation >= 100) return "champion";           // 100+ rep
        if (reputation >= 75) return "high_priest";         // 75-99 rep  
        if (reputation >= 50) return "priest";              // 50-74 rep
        if (reputation >= 25) return "acolyte";             // 25-49 rep
        if (reputation >= 0) return "initiate";             // 0-24 rep
        return "unknown";                                    // Negative rep
    }
    
    /**
     * Determine how many commands to execute based on reputation and config
     */
    private static int determineCommandCount(double reputation, int maxCommands) {
        if (reputation >= 75) return Math.min(2, maxCommands);  // High tier: 2 commands
        if (reputation >= 25) return Math.min(2, maxCommands);  // Mid tier: 1-2 commands  
        return Math.min(1, maxCommands);                        // Low tier: 1 command
    }
    
    /**
     * Select appropriate commands for the player's progression level
     */
    private static List<String> selectCommandsForProgression(List<String> referenceCommands, 
                                                            String progressionLevel, 
                                                            double reputation,
                                                            int commandCount) {
        List<String> selected = new ArrayList<>();
        
        // Categorize commands by type for intelligent selection
        List<String> giveCommands = new ArrayList<>();
        List<String> effectCommands = new ArrayList<>();
        List<String> messageCommands = new ArrayList<>();
        
        for (String command : referenceCommands) {
            String lower = command.toLowerCase();
            if (lower.startsWith("give")) {
                giveCommands.add(command);
            } else if (lower.startsWith("effect")) {
                effectCommands.add(command);
            } else if (lower.startsWith("tellraw")) {
                messageCommands.add(command);
            }
        }
        
        // Select commands based on progression level
        switch (progressionLevel) {
            case "champion":
                // High tier: Best items + strong effects
                addBestCommands(selected, giveCommands, 1);
                addBestCommands(selected, effectCommands, 1);
                break;
                
            case "high_priest":
                // High-mid tier: Good items + effects
                addMidTierCommands(selected, giveCommands, 1);
                addMidTierCommands(selected, effectCommands, 1);
                break;
                
            case "priest":
                // Mid tier: Moderate items OR effects
                if (commandCount >= 2) {
                    addMidTierCommands(selected, giveCommands, 1);
                    addBasicCommands(selected, effectCommands, 1);
                } else {
                    addMidTierCommands(selected, giveCommands, 1);
                }
                break;
                
            case "acolyte":
                // Low-mid tier: Basic items + weak effects
                addBasicCommands(selected, giveCommands, 1);
                if (commandCount >= 2) {
                    addBasicCommands(selected, effectCommands, 1);
                }
                break;
                
            case "initiate":
            default:
                // Low tier: Very basic items only
                addBasicCommands(selected, giveCommands, 1);
                break;
        }
        
        // Limit to requested command count
        if (selected.size() > commandCount) {
            selected = selected.subList(0, commandCount);
        }
        
        LOGGER.info("🔥 Selected commands for {}: {}", progressionLevel, selected);
        return selected;
    }
    
    /**
     * Add best/highest tier commands (for champions)
     */
    private static void addBestCommands(List<String> selected, List<String> commands, int count) {
        // Look for premium items: enchanted, golden, rare
        List<String> premium = commands.stream()
            .filter(cmd -> cmd.toLowerCase().contains("enchanted") || 
                          cmd.toLowerCase().contains("golden") ||
                          cmd.toLowerCase().contains("totem") ||
                          cmd.toLowerCase().contains("diamond"))
            .limit(count)
            .toList();
        selected.addAll(premium);
        
        // Fallback to any available if no premium found
        if (premium.isEmpty() && !commands.isEmpty()) {
            selected.addAll(commands.stream().limit(count).toList());
        }
    }
    
    /**
     * Add mid-tier commands (for priests)
     */
    private static void addMidTierCommands(List<String> selected, List<String> commands, int count) {
        // Look for moderate items: gold, iron, useful but not premium
        List<String> midTier = commands.stream()
            .filter(cmd -> !cmd.toLowerCase().contains("enchanted") && 
                          (cmd.toLowerCase().contains("golden") ||
                           cmd.toLowerCase().contains("iron") ||
                           cmd.toLowerCase().contains("apple")))
            .limit(count)
            .toList();
        selected.addAll(midTier);
        
        // Fallback to basic commands if no mid-tier found
        if (midTier.isEmpty()) {
            addBasicCommands(selected, commands, count);
        }
    }
    
    /**
     * Add basic/low tier commands (for initiates)
     */
    private static void addBasicCommands(List<String> selected, List<String> commands, int count) {
        // Look for basic items: wood, stone, food, basic effects
        List<String> basic = commands.stream()
            .filter(cmd -> cmd.toLowerCase().contains("sapling") ||
                          cmd.toLowerCase().contains("bone_meal") ||
                          cmd.toLowerCase().contains("bread") ||
                          cmd.toLowerCase().contains("regeneration") ||
                          cmd.toLowerCase().contains("resistance"))
            .limit(count)
            .toList();
        selected.addAll(basic);
        
        // Ultimate fallback: just take first available
        if (basic.isEmpty() && !commands.isEmpty()) {
            selected.addAll(commands.stream().limit(count).toList());
        }
    }
    
    /**
     * Determine prayer type based on player message and match actual JSON prayer config keys
     * DEPRECATED: Use determinePrayerTypeFromChant() instead when chant context is available
     */
    @Deprecated
    public static String determinePrayerType(String playerMessage, String aiResponse) {
        String lowerMessage = playerMessage.toLowerCase();
        String lowerResponse = aiResponse.toLowerCase();
        
        // Get the active deity to check THEIR prayer types
        try {
            // This method is deprecated in favor of determinePrayerTypeForDeity()
            // which takes proper deity configuration context
            // For backward compatibility, return a safe default
            LOGGER.warn("🔥 DEPRECATED: determinePrayerType called without deity context");
            LOGGER.warn("🔥 Please use determinePrayerTypeForDeity() or determinePrayerTypeFromChant() instead");
            
            return "conversation"; // Safe default - all deities have this
            
        } catch (Exception e) {
            LOGGER.error("Error determining prayer type: {}", e.getMessage());
            return "conversation";
        }
    }
    
    /**
     * @deprecated Moved to PrayerTypeResolver.determinePrayerTypeFromChant()
     */
    @Deprecated
    public static String determinePrayerTypeFromChant(com.bluelotuscoding.eidolonunchained.chant.DatapackChant chant, 
                                                     com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig aiConfig) {
        LOGGER.warn("🔥 DEPRECATED: determinePrayerTypeFromChant moved to PrayerTypeResolver");
        return com.bluelotuscoding.eidolonunchained.prayer.PrayerTypeResolver
            .determinePrayerTypeFromChant(chant, aiConfig);
    }
    
    /**
     * @deprecated Moved to PrayerTypeResolver.determinePrayerTypeFromMessage()
     */
    @Deprecated
    public static String determinePrayerTypeForDeity(String playerMessage, String aiResponse, 
                                                   com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig aiConfig) {
        LOGGER.warn("🔥 DEPRECATED: determinePrayerTypeForDeity moved to PrayerTypeResolver");
        return com.bluelotuscoding.eidolonunchained.prayer.PrayerTypeResolver
            .determinePrayerTypeFromMessage(playerMessage, aiResponse, aiConfig);
    }
    
    /**
     * Check if player meets requirements for this prayer type
     */
    private static boolean meetsRequirements(ServerPlayer player, com.bluelotuscoding.eidolonunchained.ai.PrayerAIConfig prayerConfig,
                                           com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig aiConfig, String prayerType) {
        try {
            // Check reputation requirement
            if (prayerConfig.reputation_required > 0) {
                com.bluelotuscoding.eidolonunchained.deity.DatapackDeity deity = 
                    com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager.getDeity(aiConfig.deity_id);
                if (deity != null) {
                    double reputation = deity.getPlayerReputation(player);
                    if (reputation < prayerConfig.reputation_required) {
                        LOGGER.info("🚫 Player {} reputation {} < required {}", 
                            player.getName().getString(), reputation, prayerConfig.reputation_required);
                        sendDeityDenialMessage(player, "divine blessing", 
                            "You must prove yourself more worthy through faithful service. (Reputation " + 
                            (int)reputation + "/" + prayerConfig.reputation_required + " required)");
                        return false;
                    }
                }
            }
            
            // Check cooldown
            if (!checkPrayerCooldown(player, prayerType, prayerConfig)) {
                LOGGER.info("🚫 Player {} prayer type '{}' is on cooldown", 
                    player.getName().getString(), prayerType);
                return false;
            }
            
            return true;
        } catch (Exception e) {
            LOGGER.error("Error checking prayer requirements: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if a prayer type is on cooldown for a player
     */
    private static boolean checkPrayerCooldown(ServerPlayer player, String prayerType, 
                                              com.bluelotuscoding.eidolonunchained.ai.PrayerAIConfig prayerConfig) {
        if (prayerConfig.cooldown_minutes <= 0) {
            return true; // No cooldown
        }
        
        // Generate cooldown key: playerUUID:deityId:prayerType
        String cooldownKey = player.getUUID().toString() + ":" + 
                           getActiveDeityForPlayer(player) + ":" + 
                           (prayerType != null ? prayerType : "unknown");
        
        long currentTime = System.currentTimeMillis();
        Long lastUseTime = prayerCooldowns.get(cooldownKey);
        
        if (lastUseTime != null) {
            long cooldownMillis = prayerConfig.cooldown_minutes * 60 * 1000L;
            long timeSinceLastUse = currentTime - lastUseTime;
            
            if (timeSinceLastUse < cooldownMillis) {
                long remainingSeconds = (cooldownMillis - timeSinceLastUse) / 1000;
                LOGGER.info("🕐 Prayer cooldown active for {}. {} seconds remaining", 
                    player.getName().getString(), remainingSeconds);
                
                // Send cooldown message to player
                long remainingMinutes = remainingSeconds / 60;
                String timeMessage = remainingMinutes > 0 ? 
                    remainingMinutes + " minute" + (remainingMinutes == 1 ? "" : "s") :
                    remainingSeconds + " second" + (remainingSeconds == 1 ? "" : "s");
                sendDeityDenialMessage(player, "divine intervention", 
                    "You must wait " + timeMessage + " before seeking my favor again.");
                return false;
            }
        }
        
        // Update last use time
        prayerCooldowns.put(cooldownKey, currentTime);
        return true;
    }
    
    /**
     * Get the active deity for a player (from conversation context)
     */
    private static net.minecraft.resources.ResourceLocation getActiveDeityForPlayer(ServerPlayer player) {
        // Check if player is in an active conversation
        try {
            return com.bluelotuscoding.eidolonunchained.chat.DeityChat.getActiveConversationDeity(player);
        } catch (Exception e) {
            LOGGER.debug("No active deity conversation for player {}", player.getName().getString());
            return null;
        }
    }
    
    /**
     * Fallback generic contextual action extraction (original approach)
     */
    private static List<String> extractGenericContextualActions(String aiResponse, ServerPlayer player, String playerMessage) {
        List<String> commands = new ArrayList<>();
        
        // Only extract contextual actions if:
        // 1. Player seems to be in need (low health, asking for help)
        // 2. AI explicitly mentions giving something specific
        // 3. Context suggests appropriate divine intervention
        
        String lowerResponse = aiResponse.toLowerCase();
        String lowerPlayerMessage = playerMessage.toLowerCase();
        String playerName = player.getName().getString();
        
        // Check if player seems to need help
        boolean playerNeedsHelp = player.getHealth() < (player.getMaxHealth() * 0.5f) ||
                                 lowerPlayerMessage.contains("help") ||
                                 lowerPlayerMessage.contains("blessing") ||
                                 lowerPlayerMessage.contains("aid");
        
        // Only give contextual items if player actually needs help
        if (playerNeedsHelp) {
            // Look for AI explicitly mentioning giving healing items
            if (lowerResponse.contains("heal") || lowerResponse.contains("restore")) {
                String regenEffect = normalizeEffectId("regeneration", Arrays.asList("minecraft"));
                if (regenEffect != null) {
                    commands.add(String.format("effect give %s %s 300 1", playerName, regenEffect));
                    LOGGER.info("🔥 AI contextual healing for needy player");
                }
            }
            
            // Look for AI blessing mentions when player asked for blessing
            if (lowerPlayerMessage.contains("blessing") && lowerResponse.contains("bless")) {
                String strengthEffect = normalizeEffectId("strength", Arrays.asList("minecraft"));
                if (strengthEffect != null) {
                    commands.add(String.format("effect give %s %s 600 1", playerName, strengthEffect));
                    LOGGER.info("🔥 AI contextual blessing for requesting player");
                }
            }
        }
        
        return commands;
    }
    
    /**
     * Check if a deity allows a specific item to be given
     */
    private static boolean deityAllowsItem(String itemId, com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig aiConfig, ServerPlayer player) {
        if (aiConfig == null) {
            // No deity config - allow basic items only
            return isBasicItem(itemId);
        }
        
        try {
            // METHOD 1: Check if item is explicitly in reference_commands (preferred)
            boolean foundInReference = false;
            com.bluelotuscoding.eidolonunchained.ai.PrayerAIConfig matchingPrayerConfig = null;
            
            for (com.bluelotuscoding.eidolonunchained.ai.PrayerAIConfig prayerConfig : aiConfig.prayer_configs.values()) {
                if (prayerConfig.reference_commands != null) {
                    for (String referenceCommand : prayerConfig.reference_commands) {
                        // Check if this command gives the requested item
                        if (referenceCommand.contains("give") && referenceCommand.contains(itemId)) {
                            foundInReference = true;
                            matchingPrayerConfig = prayerConfig;
                            break;
                        }
                    }
                    if (foundInReference) break;
                }
            }
            
            // If found in reference commands, check requirements
            if (foundInReference && matchingPrayerConfig != null) {
                if (meetsRequirements(player, matchingPrayerConfig, aiConfig, "item_request")) {
                    LOGGER.info("✅ Item {} APPROVED - found in reference commands and player meets requirements", itemId);
                    return true;
                } else {
                    LOGGER.info("🚫 Item {} found in reference but player doesn't meet requirements", itemId);
                    return false;
                }
            }
            
            // METHOD 2: If NOT in reference commands OR reference commands are empty, 
            // check if item fits deity's domain and player meets general requirements
            LOGGER.info("🔍 Item {} not in reference commands, checking deity domain compatibility...", itemId);
            
            if (isItemCompatibleWithDeityDomain(itemId, aiConfig) && 
                meetsGeneralRequirements(player, aiConfig)) {
                LOGGER.info("✅ Item {} APPROVED - compatible with deity domain and player meets general requirements", itemId);
                return true;
            } else {
                LOGGER.info("🚫 Item {} DENIED - not compatible with deity domain or player doesn't meet requirements", itemId);
                return false;
            }
            
        } catch (Exception e) {
            LOGGER.error("Error checking deity item permissions: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if an item is thematically compatible with the deity's domain
     */
    private static boolean isItemCompatibleWithDeityDomain(String itemId, com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig aiConfig) {
        // Extract deity type from ID to determine thematic compatibility
        String deityId = aiConfig.deity_id.toString().toLowerCase();
        String lowerItemId = itemId.toLowerCase();
        
        // Dark/Shadow deity compatibility
        if (deityId.contains("dark") || deityId.contains("shadow")) {
            return lowerItemId.contains("soul") || lowerItemId.contains("shadow") || lowerItemId.contains("death") ||
                   lowerItemId.contains("wither") || lowerItemId.contains("skull") || lowerItemId.contains("obsidian") ||
                   lowerItemId.contains("black") || lowerItemId.contains("void") || lowerItemId.contains("dark") ||
                   itemId.equals("minecraft:coal") || itemId.equals("minecraft:charcoal") || 
                   itemId.equals("minecraft:ink_sac") || itemId.equals("minecraft:ender_pearl");
        }
        
        // Nature deity compatibility  
        if (deityId.contains("nature") || deityId.contains("forest") || deityId.contains("earth")) {
            return lowerItemId.contains("seed") || lowerItemId.contains("sapling") || lowerItemId.contains("flower") ||
                   lowerItemId.contains("wood") || lowerItemId.contains("leaf") || lowerItemId.contains("moss") ||
                   lowerItemId.contains("vine") || lowerItemId.contains("bone_meal") || lowerItemId.contains("wheat") ||
                   itemId.equals("minecraft:apple") || itemId.equals("minecraft:carrot") || itemId.equals("minecraft:potato");
        }
        
        // Light deity compatibility
        if (deityId.contains("light") || deityId.contains("sun") || deityId.contains("holy")) {
            return lowerItemId.contains("gold") || lowerItemId.contains("light") || lowerItemId.contains("torch") ||
                   lowerItemId.contains("glowstone") || lowerItemId.contains("beacon") || lowerItemId.contains("lantern") ||
                   itemId.equals("minecraft:golden_apple") || itemId.equals("minecraft:experience_bottle") ||
                   itemId.equals("minecraft:totem_of_undying");
        }
        
        // Fire deity compatibility
        if (deityId.contains("fire") || deityId.contains("flame") || deityId.contains("inferno")) {
            return lowerItemId.contains("fire") || lowerItemId.contains("flame") || lowerItemId.contains("blaze") ||
                   lowerItemId.contains("magma") || lowerItemId.contains("lava") || lowerItemId.contains("coal") ||
                   itemId.equals("minecraft:flint_and_steel") || itemId.equals("minecraft:fire_charge");
        }
        
        // Water deity compatibility
        if (deityId.contains("water") || deityId.contains("ocean") || deityId.contains("sea")) {
            return lowerItemId.contains("water") || lowerItemId.contains("ice") || lowerItemId.contains("fish") ||
                   lowerItemId.contains("kelp") || lowerItemId.contains("coral") || lowerItemId.contains("prismarine") ||
                   itemId.equals("minecraft:bucket") || itemId.equals("minecraft:sponge");
        }
        
        // Default: Allow basic minecraft items that are generally neutral
        return isNeutralItem(itemId);
    }
    
    /**
     * Check if player meets general requirements for the deity (not prayer-specific)
     */
    private static boolean meetsGeneralRequirements(ServerPlayer player, com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig aiConfig) {
        try {
            // For non-reference items, use a reasonable default requirement
            // This should be lower than specific prayer requirements since it's fallback
            com.bluelotuscoding.eidolonunchained.deity.DatapackDeity deity = 
                com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager.getDeity(aiConfig.deity_id);
            if (deity != null) {
                double reputation = deity.getPlayerReputation(player);
                // Use minimal requirement for domain-compatible items (not reference items)
                if (reputation < 5) {
                    LOGGER.info("🚫 Player {} reputation {} < 5 required for non-reference domain items", 
                        player.getName().getString(), reputation);
                    return false;
                }
            }
            
            // Could add more general checks here (cooldowns, patron status, etc.)
            return true;
        } catch (Exception e) {
            LOGGER.error("Error checking general requirements: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if an item is neutral/safe for any deity to give
     */
    private static boolean isNeutralItem(String itemId) {
        return itemId.equals("minecraft:bread") || itemId.equals("minecraft:apple") || 
               itemId.equals("minecraft:cooked_beef") || itemId.equals("minecraft:cooked_porkchop") ||
               itemId.equals("minecraft:iron_ingot") || itemId.equals("minecraft:stone") ||
               itemId.equals("minecraft:cobblestone") || itemId.equals("minecraft:stick");
    }
    
    /**
     * Check if an item is considered basic/safe to give without deity restrictions
     */
    private static boolean isBasicItem(String itemId) {
        // Only allow very basic survival items when no deity config
        return itemId.equals("minecraft:bread") || 
               itemId.equals("minecraft:apple") || 
               itemId.equals("minecraft:wooden_sword") ||
               itemId.equals("minecraft:stick");
    }
}
