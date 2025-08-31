package com.bluelotuscoding.eidolonunchained.integration.commands;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Silent command execution system for AI deities
 * Executes commands invisibly while logging for debugging
 * Provides immersive player experience without technical command spam
 */
public class SilentCommandExecutor {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Pattern PLAYER_PLACEHOLDER = Pattern.compile("\\{player\\}");
    
    /**
     * Execute a command silently without showing it to the player
     * @param command The command to execute (may contain {player} placeholder)
     * @param player The target player
     * @param deityName The name of the deity executing the command (for logging)
     * @return CompletableFuture that completes when command finishes
     */
    public static CompletableFuture<Boolean> executeSilentCommand(String command, ServerPlayer player, String deityName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Replace player placeholder with actual player name
                String processedCommand = PLAYER_PLACEHOLDER.matcher(command)
                    .replaceAll(player.getName().getString());
                
                // Log the command execution for debugging/admin purposes
                LOGGER.info("🔮 DEITY COMMAND: {} executed '{}' for player {}", 
                    deityName, processedCommand, player.getName().getString());
                
                // Get the server's command manager
                var server = ServerLifecycleHooks.getCurrentServer();
                if (server == null) {
                    LOGGER.warn("Server not available for command execution");
                    return false;
                }
                
                var commandManager = server.getCommands();
                
                // Create a command source that won't spam chat
                CommandSourceStack commandSource = server.createCommandSourceStack()
                    .withLevel(player.serverLevel())
                    .withPosition(player.position())
                    .withSuppressedOutput(); // This prevents command feedback in chat
                
                // Execute the command silently
                int result = commandManager.performPrefixedCommand(commandSource, processedCommand);
                
                boolean success = result > 0;
                if (success) {
                    LOGGER.debug("✅ Command executed successfully: {}", processedCommand);
                } else {
                    LOGGER.warn("❌ Command execution failed: {}", processedCommand);
                }
                
                return success;
                
            } catch (Exception e) {
                LOGGER.error("💥 Error executing deity command '{}': {}", command, e.getMessage(), e);
                return false;
            }
        });
    }
    
    /**
     * Execute multiple commands in sequence, all silently
     */
    public static CompletableFuture<Integer> executeSilentCommands(String[] commands, ServerPlayer player, String deityName) {
        return CompletableFuture.supplyAsync(() -> {
            int successCount = 0;
            
            for (String command : commands) {
                try {
                    boolean success = executeSilentCommand(command, player, deityName).get();
                    if (success) {
                        successCount++;
                    }
                    
                    // Small delay between commands to prevent overwhelming the server
                    Thread.sleep(100);
                    
                } catch (Exception e) {
                    LOGGER.warn("Failed to execute command in sequence: {}", command, e);
                }
            }
            
            LOGGER.info("🎯 Deity {} executed {}/{} commands successfully for {}", 
                deityName, successCount, commands.length, player.getName().getString());
            
            return successCount;
        });
    }
    
    /**
     * Send an immersive deity message to the player BEFORE command execution
     * This creates the narrative flow: message → invisible command → result
     */
    public static void sendImmersiveMessage(ServerPlayer player, String deityName, String message) {
        // Format the message with deity styling
        Component formattedMessage = Component.literal("✨ ")
            .append(Component.literal(deityName).withStyle(style -> style.withColor(0xDAA520))) // Gold color
            .append(Component.literal(": "))
            .append(Component.literal(message).withStyle(style -> style.withColor(0xF0F8FF))); // Alice blue
        
        player.sendSystemMessage(formattedMessage);
        
        // Log the message for debugging
        LOGGER.info("💬 DEITY MESSAGE: {} → {}: {}", deityName, player.getName().getString(), message);
    }
    
    /**
     * Execute a deity action with full immersive experience:
     * 1. Send themed message to player
     * 2. Execute command(s) silently
     * 3. Log everything for debugging
     */
    public static CompletableFuture<Boolean> executeDeityAction(
            String deityMessage, 
            String[] commands, 
            ServerPlayer player, 
            String deityName) {
        
        // Send the immersive message first
        sendImmersiveMessage(player, deityName, deityMessage);
        
        // Then execute commands silently
        return executeSilentCommands(commands, player, deityName)
            .thenApply(successCount -> {
                boolean overallSuccess = successCount > 0;
                
                if (overallSuccess) {
                    LOGGER.info("🌟 DEITY ACTION COMPLETE: {} successfully helped {} with {} commands", 
                        deityName, player.getName().getString(), successCount);
                } else {
                    LOGGER.warn("⚠️ DEITY ACTION FAILED: {} could not help {} - all commands failed", 
                        deityName, player.getName().getString());
                }
                
                return overallSuccess;
            });
    }
    
    /**
     * Special method for curse commands - includes warning message
     */
    public static CompletableFuture<Boolean> executeCurseAction(
            String curseMessage, 
            String[] curseCommands, 
            ServerPlayer player, 
            String deityName, 
            String reason) {
        
        // Log the curse reasoning
        LOGGER.warn("⚡ DEITY CURSE: {} is cursing {} for reason: {}", deityName, player.getName().getString(), reason);
        
        // Send the curse message
        Component curseComponent = Component.literal("⚡ ")
            .append(Component.literal(deityName).withStyle(style -> style.withColor(0x8B0000))) // Dark red
            .append(Component.literal(": "))
            .append(Component.literal(curseMessage).withStyle(style -> style.withColor(0xFF4500))); // Red orange
        
        player.sendSystemMessage(curseComponent);
        
        // Execute curse commands silently
        return executeSilentCommands(curseCommands, player, deityName)
            .thenApply(successCount -> {
                if (successCount > 0) {
                    LOGGER.warn("💀 CURSE EXECUTED: {} cursed {} with {} effects", 
                        deityName, player.getName().getString(), successCount);
                } else {
                    LOGGER.error("🛡️ CURSE FAILED: {} could not curse {} - divine protection?", 
                        deityName, player.getName().getString());
                }
                
                return successCount > 0;
            });
    }
    
    /**
     * Validate command safety before execution
     */
    public static boolean isCommandSafe(String command) {
        String lowerCommand = command.toLowerCase().trim();
        
        // Block dangerous commands
        if (lowerCommand.startsWith("/stop") || 
            lowerCommand.startsWith("/ban") || 
            lowerCommand.startsWith("/op") ||
            lowerCommand.startsWith("/deop") ||
            lowerCommand.startsWith("/kick") ||
            lowerCommand.contains("delete") ||
            lowerCommand.contains("/kill @a") ||
            lowerCommand.contains("/kill @e")) {
            LOGGER.error("🚫 BLOCKED DANGEROUS COMMAND: {}", command);
            return false;
        }
        
        return true;
    }
    
    /**
     * Get suggested immersive messages for common command types
     */
    public static String getImmersiveMessageForCommand(String command, String deityDomain) {
        String lowerCommand = command.toLowerCase();
        
        if (lowerCommand.contains("/give")) {
            return switch (deityDomain.toLowerCase()) {
                case "nature" -> "Nature provides for those who respect her gifts...";
                case "dark", "shadow" -> "From the shadows, I grant you dark power...";
                case "light", "divine" -> "Receive this blessing from the divine realm...";
                default -> "Accept this token of divine favor...";
            };
        }
        
        if (lowerCommand.contains("/effect")) {
            if (lowerCommand.contains("regeneration") || lowerCommand.contains("healing")) {
                return "Let divine energy restore your mortal form...";
            } else if (lowerCommand.contains("strength") || lowerCommand.contains("speed")) {
                return "Feel my power flowing through you...";
            } else if (lowerCommand.contains("weakness") || lowerCommand.contains("poison")) {
                return "Your transgressions have consequences...";
            }
        }
        
        if (lowerCommand.contains("/weather")) {
            return "I command the very heavens themselves...";
        }
        
        if (lowerCommand.contains("/summon")) {
            return "From the divine realm, I call forth assistance...";
        }
        
        return "By my divine will, let this be done...";
    }
    
    /**
     * Enhanced command execution that auto-generates immersive messages
     */
    public static CompletableFuture<Boolean> executeSmartDeityAction(
            String[] commands, 
            ServerPlayer player, 
            String deityName, 
            String deityDomain) {
        
        // Validate all commands first
        for (String command : commands) {
            if (!isCommandSafe(command)) {
                LOGGER.error("🚫 Blocked unsafe command from deity {}: {}", deityName, command);
                return CompletableFuture.completedFuture(false);
            }
        }
        
        // Generate appropriate message based on primary command
        String immersiveMessage = commands.length > 0 ? 
            getImmersiveMessageForCommand(commands[0], deityDomain) :
            "By my divine will, let this be done...";
        
        return executeDeityAction(immersiveMessage, commands, player, deityName);
    }
}
