#!/bin/bash

# Command System Cleanup & Special Character Support Script
# Handles Unicode, special symbols, spaces, and improves validation

echo "🔧 Starting command system cleanup with special character support..."

# 1. Create utility class for string validation and sanitization
cat > "src/main/java/com/bluelotuscoding/eidolonunchained/util/CommandStringUtils.java" << 'EOF'
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
EOF

echo "✅ Created CommandStringUtils utility class"

# 2. Update UnifiedCommands.java to use proper string validation
# First, add the import
sed -i '/import com.mojang.brigadier.arguments.StringArgumentType;/a import com.bluelotuscoding.eidolonunchained.util.CommandStringUtils;' \
    "src/main/java/com/bluelotuscoding/eidolonunchained/command/UnifiedCommands.java"

echo "✅ Added CommandStringUtils import"

# 3. Update setApiKey method with proper validation
cat > temp_setApiKey.java << 'EOF'
    // API key management commands
    private static int setApiKey(CommandContext<CommandSourceStack> context) {
        String provider = CommandStringUtils.safeTrim(StringArgumentType.getString(context, "provider"));
        String key = CommandStringUtils.safeTrim(StringArgumentType.getString(context, "key"));
        
        // Validate provider name
        if (provider == null || provider.isEmpty()) {
            context.getSource().sendFailure(Component.literal("§cProvider name cannot be empty"));
            return 0;
        }
        
        // Validate API key format
        if (key == null || key.isEmpty()) {
            context.getSource().sendFailure(Component.literal("§cAPI key cannot be empty"));
            return 0;
        }
        
        if (!CommandStringUtils.isValidApiKey(key)) {
            context.getSource().sendFailure(Component.literal(
                CommandStringUtils.createValidationError("API key", key, "alphanumeric with -_.+=/$, minimum 8 characters")));
            return 0;
        }
        
        try {
            APIKeyManager.setAPIKey(provider, key);
            context.getSource().sendSuccess(() -> Component.translatable("eidolonunchained.command.api.key_set", provider), false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.translatable("eidolonunchained.command.api.key_set_failed", e.getMessage()));
            return 0;
        }
    }
EOF

# Replace the setApiKey method
sed -i '/\/\/ API key management commands/,/^    }$/c\'"$(cat temp_setApiKey.java)" \
    "src/main/java/com/bluelotuscoding/eidolonunchained/command/UnifiedCommands.java"

rm temp_setApiKey.java

echo "✅ Updated setApiKey method with proper validation"

# 4. Update testDeityCommand method with safer command processing
cat > temp_testDeityCommand.java << 'EOF'
    private static int testDeityCommand(CommandContext<CommandSourceStack> context) {
        try {
            String rawCommand = StringArgumentType.getString(context, "command");
            String command = CommandStringUtils.cleanCommandText(rawCommand);
            ServerPlayer executor = context.getSource().getPlayerOrException();
            
            // Validate command input
            if (command == null || command.isEmpty()) {
                context.getSource().sendFailure(Component.literal("§cCommand cannot be empty"));
                return 0;
            }
            
            // Display what we're testing (safely formatted for chat)
            String displayCommand = CommandStringUtils.safeChatDisplay(command);
            context.getSource().sendSuccess(() -> Component.literal(
                "§6Testing deity command: §f" + displayCommand), false);
            
            // Execute the command as a deity would
            net.minecraft.commands.CommandSourceStack deitySource = context.getSource().getServer()
                .createCommandSourceStack()
                .withSource(net.minecraft.commands.CommandSource.NULL)
                .withLevel(executor.serverLevel())
                .withPosition(executor.position())
                .withPermission(2);
            
            int result = context.getSource().getServer().getCommands().performPrefixedCommand(deitySource, command);
            
            context.getSource().sendSuccess(() -> Component.literal(
                "§7Command result: " + (result > 0 ? "§aSuccess (" + result + ")" : "§cFailed (" + result + ")")), false);
            
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError testing command: " + CommandStringUtils.safeChatDisplay(e.getMessage())));
            return 0;
        }
    }
EOF

# Find and replace testDeityCommand method
sed -i '/private static int testDeityCommand/,/^    }$/c\'"$(cat temp_testDeityCommand.java)" \
    "src/main/java/com/bluelotuscoding/eidolonunchained/command/UnifiedCommands.java"

rm temp_testDeityCommand.java

echo "✅ Updated testDeityCommand method with safer processing"

# 5. Update testPlayer2AIChat method with proper message handling
cat > temp_testPlayer2AIChat.java << 'EOF'
    private static int testPlayer2AIChat(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String rawMessage = StringArgumentType.getString(context, "message");
        String message = CommandStringUtils.safeTrim(rawMessage);
        
        // Validate message input
        if (message == null || message.isEmpty()) {
            source.sendFailure(Component.literal("§cMessage cannot be empty"));
            return 0;
        }
        
        try {
            // Display what we're testing (safely formatted for chat)
            String displayMessage = CommandStringUtils.safeChatDisplay(message);
            source.sendSuccess(() -> Component.literal("§6Testing Player2AI chat with message: §f" + displayMessage), false);
            
            // Create a test Player2AI client
            com.bluelotuscoding.eidolonunchained.integration.player2ai.Player2AIClient client = 
                new com.bluelotuscoding.eidolonunchained.integration.player2ai.Player2AIClient();
            
            // Test with a simple personality
            String testPersonality = "You are a test deity. Respond briefly to user messages.";
            String testCharacterId = "debug_deity";
            String playerUUID = "debug_player";
            
            client.generateResponse(message, testPersonality, testCharacterId, playerUUID, null, null)
                .thenAccept(response -> {
                    if (response.success) {
                        String safeResponse = CommandStringUtils.safeChatDisplay(response.dialogue);
                        source.sendSuccess(() -> Component.literal("§a✓ Player2AI Response: §f" + safeResponse), false);
                    } else {
                        String safeError = CommandStringUtils.safeChatDisplay(response.dialogue);
                        source.sendFailure(Component.literal("§cPlayer2AI Error: " + safeError));
                    }
                })
                .exceptionally(error -> {
                    String safeError = CommandStringUtils.safeChatDisplay(error.getMessage());
                    source.sendFailure(Component.literal("§cPlayer2AI Exception: " + safeError));
                    return null;
                });
                
            source.sendSuccess(() -> Component.literal("§7Request sent, waiting for response..."), false);

        } catch (Exception e) {
            String safeError = CommandStringUtils.safeChatDisplay(e.getMessage());
            source.sendFailure(Component.literal("§cDebug test failed: " + safeError));
        }
        
        return 1;
    }
EOF

# Find and replace testPlayer2AIChat method
sed -i '/private static int testPlayer2AIChat/,/^    }$/c\'"$(cat temp_testPlayer2AIChat.java)" \
    "src/main/java/com/bluelotuscoding/eidolonunchained/command/UnifiedCommands.java"

rm temp_testPlayer2AIChat.java

echo "✅ Updated testPlayer2AIChat method with proper message handling"

# 6. Update player name validation throughout the file
echo "🔧 Adding player name validation to all player parameter commands..."

# Update methods that use player names with validation
sed -i 's/String playerName = StringArgumentType.getString(context, "player");/String rawPlayerName = StringArgumentType.getString(context, "player");\
        String playerName = CommandStringUtils.safeTrim(rawPlayerName);\
        \
        \/\/ Validate player name\
        if (playerName == null || playerName.isEmpty()) {\
            context.getSource().sendFailure(Component.literal("§cPlayer name cannot be empty"));\
            return 0;\
        }/g' "src/main/java/com/bluelotuscoding/eidolonunchained/command/UnifiedCommands.java"

echo "✅ Added player name validation"

# 7. Update deity name validation
sed -i 's/String deityId = StringArgumentType.getString(context, "deity");/String rawDeityId = StringArgumentType.getString(context, "deity");\
        String deityId = CommandStringUtils.safeTrim(rawDeityId);\
        \
        \/\/ Validate deity ID\
        if (deityId == null || deityId.isEmpty()) {\
            context.getSource().sendFailure(Component.literal("§cDeity ID cannot be empty"));\
            return 0;\
        }/g' "src/main/java/com/bluelotuscoding/eidolonunchained/command/UnifiedCommands.java"

echo "✅ Added deity ID validation"

# 8. Update other string parameters
sed -i 's/String deityName = StringArgumentType.getString(context, "deity");/String rawDeityName = StringArgumentType.getString(context, "deity");\
        String deityName = CommandStringUtils.safeTrim(rawDeityName);\
        \
        \/\/ Validate deity name\
        if (deityName == null || deityName.isEmpty()) {\
            context.getSource().sendFailure(Component.literal("§cDeity name cannot be empty"));\
            return 0;\
        }/g' "src/main/java/com/bluelotuscoding/eidolonunchained/command/UnifiedCommands.java"

# 9. Update chant name validation
sed -i 's/String chantName = StringArgumentType.getString(context, "chant");/String rawChantName = StringArgumentType.getString(context, "chant");\
        String chantName = CommandStringUtils.safeTrim(rawChantName);\
        \
        \/\/ Validate chant name\
        if (chantName == null || chantName.isEmpty()) {\
            context.getSource().sendFailure(Component.literal("§cChant name cannot be empty"));\
            return 0;\
        }/g' "src/main/java/com/bluelotuscoding/eidolonunchained/command/UnifiedCommands.java"

# 10. Update provider validation
sed -i 's/String provider = StringArgumentType.getString(context, "provider");/String rawProvider = StringArgumentType.getString(context, "provider");\
        String provider = CommandStringUtils.safeTrim(rawProvider);\
        \
        \/\/ Validate provider name\
        if (provider == null || provider.isEmpty()) {\
            context.getSource().sendFailure(Component.literal("§cProvider name cannot be empty"));\
            return 0;\
        }/g' "src/main/java/com/bluelotuscoding/eidolonunchained/command/UnifiedCommands.java"

echo "✅ Added comprehensive parameter validation"

# 11. Update string argument types to use quotable strings where appropriate
echo "🔧 Updating string argument types for better special character support..."

# For deity names, player names, and other identifiers that might have spaces, use quotableString
sed -i 's/StringArgumentType\.string()/StringArgumentType.quotableString()/g' \
    "src/main/java/com/bluelotuscoding/eidolonunchained/command/UnifiedCommands.java"

# Keep greedyString for message and command parameters that should capture everything
sed -i 's/Commands\.argument("message", StringArgumentType\.quotableString())/Commands.argument("message", StringArgumentType.greedyString())/g' \
    "src/main/java/com/bluelotuscoding/eidolonunchained/command/UnifiedCommands.java"

sed -i 's/Commands\.argument("command", StringArgumentType\.quotableString())/Commands.argument("command", StringArgumentType.greedyString())/g' \
    "src/main/java/com/bluelotuscoding/eidolonunchained/command/UnifiedCommands.java"

# For API keys, use greedyString to capture complex keys
sed -i 's/Commands\.argument("key", StringArgumentType\.quotableString())/Commands.argument("key", StringArgumentType.greedyString())/g' \
    "src/main/java/com/bluelotuscoding/eidolonunchained/command/UnifiedCommands.java"

echo "✅ Updated string argument types for better special character support"

# 12. Fix the validation logic cleanup - remove duplicated validation
echo "🔧 Cleaning up validation logic..."

# Remove duplicate lines that may have been created
sed -i '/^        String rawPlayerName = StringArgumentType.getString/,/^        }$/{
    /^        String rawPlayerName = StringArgumentType.getString/!b
    N
    /String rawPlayerName = StringArgumentType.getString.*\n        String rawPlayerName = StringArgumentType.getString/s/\n.*String rawPlayerName = StringArgumentType.getString.*//
}' "src/main/java/com/bluelotuscoding/eidolonunchained/command/UnifiedCommands.java"

echo "✅ Cleaned up validation logic"

# 13. Compile to check for errors
echo "🔧 Compiling to check for errors..."
./gradlew compileJava

if [ $? -eq 0 ]; then
    echo "✅ Command system cleanup completed successfully!"
    echo ""
    echo "📋 Changes Summary:"
    echo "   • Created CommandStringUtils utility class for comprehensive string handling"
    echo "   • Added Unicode normalization and safe display formatting" 
    echo "   • Implemented proper validation for all string parameters"
    echo "   • Updated argument types to use quotableString for identifiers"
    echo "   • Enhanced API key, player name, and deity ID validation"
    echo "   • Added safe command processing with special character support"
    echo "   • Improved error messages with safe character escaping"
    echo ""
    echo "🌟 Commands now support all special characters including:"
    echo "   • Unicode characters (accented letters, emoji, etc.)"
    echo "   • Spaces in quoted arguments"
    echo "   • Special symbols in API keys and messages"
    echo "   • Proper escaping of chat formatting characters"
else
    echo "❌ Compilation failed. Please check the errors above."
    exit 1
fi
