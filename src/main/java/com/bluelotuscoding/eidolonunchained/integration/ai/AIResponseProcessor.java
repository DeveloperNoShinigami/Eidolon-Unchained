package com.bluelotuscoding.eidolonunchained.integration.ai;

import com.bluelotuscoding.eidolonunchained.integration.commands.SilentCommandExecutor;
import com.bluelotuscoding.eidolonunchained.ai.PrayerAIConfig;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.CompletableFuture;

/**
 * AI Response Processor that extracts commands from AI responses and executes them silently
 * Handles the separation between immersive chat and technical command execution
 */
public class AIResponseProcessor {
    private static final Logger LOGGER = LogManager.getLogger();
    
    // Pattern to match commands in AI responses (hidden from players)
    private static final Pattern COMMAND_PATTERN = Pattern.compile("\\[COMMAND:(.*?)\\]", Pattern.CASE_INSENSITIVE);
    
    // Pattern to match explicit action indicators
    private static final Pattern ACTION_PATTERN = Pattern.compile("\\[ACTION:(.*?)\\]", Pattern.CASE_INSENSITIVE);
    
    /**
     * Process an AI response by extracting commands and executing them silently
     * Returns the cleaned response for display to the player
     */
    public static ProcessedResponse processAIResponse(
            String rawResponse, 
            ServerPlayer player, 
            String deityName,
            String deityDomain,
            PrayerAIConfig prayerConfig) {
        
        try {
            // Extract commands from the response
            List<String> extractedCommands = extractCommands(rawResponse);
            
            // Clean the response for player display
            String cleanedResponse = cleanResponseForPlayer(rawResponse);
            
            // Validate and execute commands if any were found
            CompletableFuture<Boolean> executionFuture = null;
            if (!extractedCommands.isEmpty()) {
                executionFuture = executeExtractedCommands(
                    extractedCommands, player, deityName, deityDomain, prayerConfig);
            }
            
            return new ProcessedResponse(cleanedResponse, extractedCommands, executionFuture);
            
        } catch (Exception e) {
            LOGGER.error("Failed to process AI response: {}", e.getMessage(), e);
            return new ProcessedResponse(cleanAnyMarkup(rawResponse), new ArrayList<>(), null);
        }
    }
    
    /**
     * Extract command patterns from AI response
     */
    private static List<String> extractCommands(String response) {
        List<String> commands = new ArrayList<>();
        
        // Extract [COMMAND:...] patterns
        Matcher commandMatcher = COMMAND_PATTERN.matcher(response);
        while (commandMatcher.find()) {
            String command = commandMatcher.group(1).trim();
            if (!command.isEmpty()) {
                commands.add(command);
            }
        }
        
        // Extract [ACTION:...] patterns and convert to commands
        Matcher actionMatcher = ACTION_PATTERN.matcher(response);
        while (actionMatcher.find()) {
            String action = actionMatcher.group(1).trim();
            String convertedCommand = convertActionToCommand(action);
            if (convertedCommand != null) {
                commands.add(convertedCommand);
            }
        }
        
        return commands;
    }
    
    /**
     * Convert action descriptions to actual commands
     */
    private static String convertActionToCommand(String action) {
        String lowerAction = action.toLowerCase();
        
        // Healing actions
        if (lowerAction.contains("heal") || lowerAction.contains("restore")) {
            return "/effect give {player} minecraft:regeneration 300 1";
        }
        
        // Strength actions
        if (lowerAction.contains("strengthen") || lowerAction.contains("empower")) {
            return "/effect give {player} minecraft:strength 600 1";
        }
        
        // Protection actions
        if (lowerAction.contains("protect") || lowerAction.contains("shield")) {
            return "/effect give {player} minecraft:resistance 600 1";
        }
        
        // Gift actions
        if (lowerAction.contains("gift diamond")) {
            return "/give {player} minecraft:diamond 1";
        }
        if (lowerAction.contains("gift food") || lowerAction.contains("provide sustenance")) {
            return "/give {player} minecraft:golden_apple 3";
        }
        
        // Weather actions
        if (lowerAction.contains("clear weather") || lowerAction.contains("stop storm")) {
            return "/weather clear 6000";
        }
        if (lowerAction.contains("summon storm") || lowerAction.contains("create thunder")) {
            return "/weather thunder 6000";
        }
        
        // Curse actions
        if (lowerAction.contains("curse") || lowerAction.contains("punish")) {
            return "/effect give {player} minecraft:weakness 300 1";
        }
        
        LOGGER.debug("Could not convert action to command: {}", action);
        return null;
    }
    
    /**
     * Clean response by removing all command/action markup
     */
    private static String cleanResponseForPlayer(String response) {
        String cleaned = response;
        
        // Remove [COMMAND:...] patterns
        cleaned = COMMAND_PATTERN.matcher(cleaned).replaceAll("");
        
        // Remove [ACTION:...] patterns
        cleaned = ACTION_PATTERN.matcher(cleaned).replaceAll("");
        
        // Clean up any double spaces or line breaks caused by removal
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        
        return cleaned;
    }
    
    /**
     * Remove any markup patterns that might confuse players
     */
    private static String cleanAnyMarkup(String response) {
        return response.replaceAll("\\[\\w+:.*?\\]", "").replaceAll("\\s+", " ").trim();
    }
    
    /**
     * Execute extracted commands with validation and safety checks
     */
    private static CompletableFuture<Boolean> executeExtractedCommands(
            List<String> commands, 
            ServerPlayer player, 
            String deityName,
            String deityDomain,
            PrayerAIConfig prayerConfig) {
        
        // Validate command limits if prayer config exists
        if (prayerConfig != null && commands.size() > prayerConfig.maxCommands) {
            LOGGER.warn("AI {} tried to execute {} commands, but limit is {}. Truncating.", 
                deityName, commands.size(), prayerConfig.maxCommands);
            commands = commands.subList(0, prayerConfig.maxCommands);
        }
        
        // Filter out unsafe commands
        List<String> safeCommands = new ArrayList<>();
        for (String command : commands) {
            if (SilentCommandExecutor.isCommandSafe(command)) {
                safeCommands.add(command);
            } else {
                LOGGER.warn("Blocked unsafe command from AI {}: {}", deityName, command);
            }
        }
        
        if (safeCommands.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }
        
        // Execute commands using the silent executor
        return SilentCommandExecutor.executeSmartDeityAction(
            safeCommands.toArray(new String[0]), 
            player, 
            deityName, 
            deityDomain);
    }
    
    /**
     * Result container for processed AI responses
     */
    public static class ProcessedResponse {
        public final String cleanedMessage;
        public final List<String> extractedCommands;
        public final CompletableFuture<Boolean> executionFuture;
        
        public ProcessedResponse(String cleanedMessage, List<String> extractedCommands, CompletableFuture<Boolean> executionFuture) {
            this.cleanedMessage = cleanedMessage;
            this.extractedCommands = extractedCommands;
            this.executionFuture = executionFuture;
        }
        
        public boolean hasCommands() {
            return !extractedCommands.isEmpty();
        }
        
        public int getCommandCount() {
            return extractedCommands.size();
        }
        
        /**
         * Wait for command execution to complete (use carefully - may block)
         */
        public boolean waitForExecution() {
            if (executionFuture == null) return false;
            try {
                return executionFuture.get();
            } catch (Exception e) {
                LOGGER.error("Error waiting for command execution: {}", e.getMessage());
                return false;
            }
        }
    }
    
    /**
     * Enhanced AI prompt that teaches the AI how to use command markup
     */
    public static String getCommandMarkupGuide() {
        return """
            COMMAND EXECUTION MARKUP:
            You can execute commands by including special markup in your responses:
            
            [COMMAND:/give {player} minecraft:diamond 1] - Executes the exact command
            [ACTION:heal player] - Converts common actions to appropriate commands
            [ACTION:strengthen player] - Gives strength effect
            [ACTION:protect player] - Gives resistance effect
            [ACTION:gift food] - Gives golden apples
            [ACTION:clear weather] - Clears bad weather
            [ACTION:summon storm] - Creates thunder weather
            
            IMPORTANT RULES:
            - Commands execute silently - players never see the command syntax
            - Use {player} placeholder for the target player name
            - Your regular message will be shown to the player WITHOUT the markup
            - Combine narrative responses with hidden commands for immersive experience
            
            Example Response:
            "I sense your wounds, mortal. Let my divine energy restore you... [ACTION:heal player] You are now under my protection. [ACTION:protect player]"
            
            The player sees: "I sense your wounds, mortal. Let my divine energy restore you... You are now under my protection."
            Commands executed silently: regeneration + resistance effects
            """;
    }
}
