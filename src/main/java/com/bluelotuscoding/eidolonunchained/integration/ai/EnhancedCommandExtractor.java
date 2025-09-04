package com.bluelotuscoding.eidolonunchained.integration.ai;

import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enhanced AI command extractor that can parse natural language commands
 * and convert them to proper Minecraft commands for execution.
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
        "\\[ACTION:(?:give|grant|bestow)\\s+([\\w:_-]+)(?:\\s+(\\d+))?\\]",
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * Extract and convert AI natural language to executable commands
     */
    public static List<String> extractAndConvertCommands(String aiResponse, ServerPlayer player) {
        List<String> commands = new ArrayList<>();
        
        try {
            // Clean the AI response first
            String cleanResponse = cleanAIResponse(aiResponse);
            
            // 1. Look for [ACTION:...] patterns first (highest priority)
            Matcher actionMatcher = ACTION_PATTERN.matcher(cleanResponse);
            while (actionMatcher.find()) {
                String item = actionMatcher.group(1);
                String amount = actionMatcher.group(2);
                
                // Validate and normalize item ID
                String normalizedItem = normalizeItemId(item);
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
                
                // Validate and normalize item ID
                String normalizedItem = normalizeItemId(item);
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
                
                // Validate and normalize effect ID
                String normalizedEffect = normalizeEffectId(effect);
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
            commands.addAll(extractDeityActions(cleanResponse, player));
            
        } catch (Exception e) {
            LOGGER.error("Error extracting commands from AI response: {}", e.getMessage());
        }
        
        return commands;
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
     */
    private static List<String> extractDeityActions(String response, ServerPlayer player) {
        List<String> commands = new ArrayList<>();
        String lowerResponse = response.toLowerCase();
        String playerName = player.getName().getString();
        
        // Healing actions
        if (lowerResponse.contains("heal") || lowerResponse.contains("restore health")) {
            commands.add(String.format("effect give %s minecraft:regeneration 300 1", playerName));
        }
        
        // Blessing actions
        if (lowerResponse.contains("bless") && lowerResponse.contains("strength")) {
            commands.add(String.format("effect give %s minecraft:strength 600 1", playerName));
        }
        
        // Protection actions
        if (lowerResponse.contains("protect") || lowerResponse.contains("shield")) {
            commands.add(String.format("effect give %s minecraft:resistance 600 1", playerName));
        }
        
        // Night vision for shadow deities
        if (lowerResponse.contains("shadow sight") || lowerResponse.contains("dark vision")) {
            commands.add(String.format("effect give %s minecraft:night_vision 1200 0", playerName));
        }
        
        // Speed blessings
        if (lowerResponse.contains("swift") || lowerResponse.contains("speed")) {
            commands.add(String.format("effect give %s minecraft:speed 600 1", playerName));
        }
        
        // Gift of sustenance
        if (lowerResponse.contains("food") || lowerResponse.contains("nourish")) {
            commands.add(String.format("give %s minecraft:golden_apple 2", playerName));
        }
        
        // Curse actions
        if (lowerResponse.contains("curse") || lowerResponse.contains("punish")) {
            commands.add(String.format("effect give %s minecraft:weakness 300 1", playerName));
        }
        
        return commands;
    }
    
    /**
     * Normalize item IDs to proper Minecraft format
     */
    private static String normalizeItemId(String item) {
        if (item == null || item.trim().isEmpty()) return null;
        
        item = item.toLowerCase().trim();
        
        // Already properly formatted
        if (item.contains(":")) {
            return item;
        }
        
        // Common item mappings
        switch (item) {
            case "soul_shard": case "soul shard": return "eidolon:soul_shard";
            case "death_essence": case "death essence": return "eidolon:death_essence";
            case "shadow_gem": case "shadow gem": return "eidolon:shadow_gem";
            case "arcane_gold": case "arcane gold": return "eidolon:arcane_gold_ingot";
            case "wicked_weave": case "wicked weave": return "eidolon:wicked_weave";
            case "holy_symbol": case "holy symbol": return "eidolon:holy_symbol";
            case "research_notes": case "research notes": return "eidolon:research_notes";
            case "codex": return "eidolon:codex";
            
            // Standard Minecraft items
            case "diamond": return "minecraft:diamond";
            case "gold": case "gold_ingot": return "minecraft:gold_ingot";
            case "iron": case "iron_ingot": return "minecraft:iron_ingot";
            case "emerald": return "minecraft:emerald";
            case "bread": return "minecraft:bread";
            case "golden_apple": case "golden apple": return "minecraft:golden_apple";
            case "enchanted_golden_apple": return "minecraft:enchanted_golden_apple";
            
            default:
                // Try adding minecraft namespace
                return "minecraft:" + item;
        }
    }
    
    /**
     * Normalize effect IDs to proper Minecraft format
     */
    private static String normalizeEffectId(String effect) {
        if (effect == null || effect.trim().isEmpty()) return null;
        
        effect = effect.toLowerCase().trim();
        
        // Already properly formatted
        if (effect.contains(":")) {
            return effect;
        }
        
        // Common effect mappings
        switch (effect) {
            case "strength": case "strong": return "minecraft:strength";
            case "speed": case "swift": case "swiftness": return "minecraft:speed";
            case "regeneration": case "regen": case "healing": return "minecraft:regeneration";
            case "resistance": case "protection": return "minecraft:resistance";
            case "night_vision": case "nightvision": case "dark_vision": return "minecraft:night_vision";
            case "water_breathing": case "waterbreathing": return "minecraft:water_breathing";
            case "fire_resistance": case "fireresistance": return "minecraft:fire_resistance";
            case "invisibility": case "invisible": return "minecraft:invisibility";
            
            // Negative effects
            case "weakness": case "weak": return "minecraft:weakness";
            case "slowness": case "slow": return "minecraft:slowness";
            case "poison": case "poisoned": return "minecraft:poison";
            case "wither": case "withering": return "minecraft:wither";
            case "blindness": case "blind": return "minecraft:blindness";
            case "nausea": case "nauseated": return "minecraft:nausea";
            case "hunger": case "hungry": return "minecraft:hunger";
            case "mining_fatigue": case "fatigue": return "minecraft:mining_fatigue";
            
            default:
                // Try adding minecraft namespace
                return "minecraft:" + effect;
        }
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
