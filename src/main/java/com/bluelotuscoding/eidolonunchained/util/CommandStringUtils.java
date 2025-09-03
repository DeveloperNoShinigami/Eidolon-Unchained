package com.bluelotuscoding.eidolonunchained.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Utility class for handling string validation and sanitization in commands,
 * with comprehensive special character support.
 */
public class CommandStringUtils {
    
    // Pattern for valid resource location characters (namespace:path)
    private static final Pattern RESOURCE_LOCATION_PATTERN = Pattern.compile("^[a-z0-9_.-]+:[a-z0-9_.-/]+$");
    
    // Pattern for safe player names (allowing Unicode letters, numbers, underscores)
    private static final Pattern PLAYER_NAME_PATTERN = Pattern.compile("^[\\p{L}\\p{N}_]{1,16}$");
    
    // Pattern for API keys (alphanumeric plus common special characters)
    private static final Pattern API_KEY_PATTERN = Pattern.compile("^[\\w\\-_.+=/$]{8,}$");
    
    /**
     * Safely normalizes Unicode text for consistent processing.
     * Handles accented characters, emoji, and other Unicode properly.
     */
    public static String normalizeUnicode(String input) {
        if (input == null) return null;
        
        // Normalize to NFD (Canonical Decomposition) then remove combining marks
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", ""); // Remove combining marks
    }
    
    /**
     * Safely trims and validates string input with null checks.
     */
    public static String safeTrim(String input) {
        if (input == null) return null;
        String trimmed = input.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
    
    /**
     * Validates if a string is a properly formatted ResourceLocation.
     */
    public static boolean isValidResourceLocation(String input) {
        if (input == null) return false;
        String cleaned = safeTrim(input);
        return cleaned != null && RESOURCE_LOCATION_PATTERN.matcher(cleaned.toLowerCase()).matches();
    }
    
    /**
     * Validates if a string is a valid player name (supports Unicode).
     */
    public static boolean isValidPlayerName(String input) {
        if (input == null) return false;
        String cleaned = safeTrim(input);
        return cleaned != null && PLAYER_NAME_PATTERN.matcher(cleaned).matches();
    }
    
    /**
     * Validates if a string is a valid API key format.
     */
    public static boolean isValidApiKey(String input) {
        if (input == null) return false;
        String cleaned = safeTrim(input);
        return cleaned != null && API_KEY_PATTERN.matcher(cleaned).matches();
    }
    
    /**
     * Safely extracts and cleans command text, preserving Unicode characters.
     * Removes only leading/trailing whitespace and optional leading slash.
     */
    public static String cleanCommandText(String input) {
        if (input == null) return null;
        
        String cleaned = safeTrim(input);
        if (cleaned == null) return null;
        
        // Remove leading slash if present
        if (cleaned.startsWith("/")) {
            cleaned = cleaned.substring(1);
        }
        
        return safeTrim(cleaned);
    }
    
    /**
     * Safely formats a string for display in chat, preserving Unicode.
     * Escapes only dangerous characters that could break chat formatting.
     */
    public static String safeChatDisplay(String input) {
        if (input == null) return "null";
        
        // Replace potentially dangerous characters but preserve Unicode
        return input.replace("§", "\\§")  // Escape color codes
                   .replace("\n", "\\n")  // Escape newlines
                   .replace("\r", "\\r")  // Escape carriage returns
                   .replace("\t", "\\t"); // Escape tabs
    }
    
    /**
     * Creates a user-friendly error message for invalid input.
     */
    public static String createValidationError(String fieldName, String input, String expectedFormat) {
        return String.format("Invalid %s '%s'. Expected format: %s", 
                           fieldName, 
                           safeChatDisplay(input), 
                           expectedFormat);
    }
}
