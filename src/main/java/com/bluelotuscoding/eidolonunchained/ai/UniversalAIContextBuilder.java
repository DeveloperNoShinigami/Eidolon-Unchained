package com.bluelotuscoding.eidolonunchained.ai;

import com.bluelotuscoding.eidolonunchained.integration.ai.AIResponseProcessor;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Universal AI Context Builder for ALL AI providers
 * Ensures consistent game state knowledge across Gemini, OpenRouter, Player2AI, and future providers
 */
public class UniversalAIContextBuilder {
    private static final Logger LOGGER = LogManager.getLogger();
    
    /**
     * Build complete context for ANY AI provider with full game state knowledge
     */
    public static String buildCompleteContext(ServerPlayer player, AIDeityConfig config, PrayerAIConfig prayerConfig) {
        StringBuilder context = new StringBuilder();
        
        try {
            // 1. Core game state using UniversalAIContext
            String gameStateContext = UniversalAIContext.buildFullContext(player, config);
            context.append(gameStateContext);
            
            // 2. Universal AI behavior guidance
            context.append("\n").append(UniversalAIPrompts.UNIVERSAL_BASE_PROMPT);
            context.append("\n").append(UniversalAIPrompts.COMMAND_GUIDANCE);
            
            // 3. Command execution system guidance
            context.append("\n=== COMMAND EXECUTION SYSTEM ===\n");
            context.append(AIResponseProcessor.getCommandMarkupGuide());
            
            // 4. Prayer-specific configuration if available
            if (prayerConfig != null) {
                context.append(buildPrayerConfigContext(prayerConfig));
            }
            
            // 5. Provider-agnostic final instructions
            context.append("\n=== RESPONSE GUIDELINES ===\n");
            context.append("- Keep responses 1-3 sentences for immersion\n");
            context.append("- Execute commands silently with thematic responses\n");
            context.append("- Use appropriate items from the complete registry\n");
            context.append("- Stay in character as a deity\n");
            context.append("- Address emergencies immediately\n");
            
        } catch (Exception e) {
            LOGGER.error("Failed to build universal context: {}", e.getMessage());
            // Provide fallback context to prevent AI failure
            context.append("=== BASIC CONTEXT ===\n");
            context.append("Player: ").append(player.getName().getString()).append("\n");
            context.append("Health: ").append(player.getHealth()).append("/").append(player.getMaxHealth()).append("\n");
            context.append("Location: ").append(player.blockPosition().toString()).append("\n");
            context.append("You are a deity in the Minecraft world. Respond helpfully and stay in character.\n");
        }
        
        return context.toString();
    }
    
    /**
     * Build provider-specific context wrapper for different AI services
     */
    public static String buildProviderSpecificContext(ServerPlayer player, AIDeityConfig config, 
            PrayerAIConfig prayerConfig, String providerType) {
        
        String baseContext = buildCompleteContext(player, config, prayerConfig);
        
        // Add provider-specific formatting if needed
        switch (providerType.toLowerCase()) {
            case "gemini":
                return "=== GEMINI AI CONTEXT ===\n" + baseContext + "\n=== END CONTEXT - RESPOND AS DEITY ===\n";
                
            case "openrouter":
            case "claude":
            case "gpt":
                return "=== OPENROUTER AI CONTEXT ===\n" + baseContext + "\n=== END CONTEXT - RESPOND AS DEITY ===\n";
                
            case "player2ai":
            case "local":
                return "=== PLAYER2AI CONTEXT ===\n" + baseContext + "\n=== END CONTEXT - RESPOND AS DEITY ===\n";
                
            default:
                return baseContext;
        }
    }
    
    /**
     * Build prayer-specific configuration context
     */
    private static String buildPrayerConfigContext(PrayerAIConfig prayerConfig) {
        StringBuilder prayerContext = new StringBuilder();
        prayerContext.append("\n=== PRAYER CONFIGURATION ===\n");
        
        if (!prayerConfig.reference_commands.isEmpty()) {
            prayerContext.append("Available Reference Commands:\n");
            for (String refCmd : prayerConfig.reference_commands) {
                prayerContext.append("- ").append(refCmd).append("\n");
            }
            prayerContext.append("Remember: NEVER show these commands to players. Use them silently with immersive messages.\n");
        }
        
        if (!prayerConfig.additional_prompts.isEmpty()) {
            prayerContext.append("\nAdditional Guidance:\n");
            for (String prompt : prayerConfig.additional_prompts) {
                prayerContext.append(prompt).append("\n");
            }
        }
        
        return prayerContext.toString();
    }
    
    /**
     * Get simplified context for debugging/testing purposes
     */
    public static String buildDebugContext(ServerPlayer player) {
        StringBuilder debug = new StringBuilder();
        debug.append("=== DEBUG CONTEXT ===\n");
        debug.append("Player: ").append(player.getName().getString()).append("\n");
        debug.append("Health: ").append(player.getHealth()).append("/").append(player.getMaxHealth()).append("\n");
        debug.append("Position: ").append(player.blockPosition().toString()).append("\n");
        debug.append("Dimension: ").append(player.level().dimension().location().toString()).append("\n");
        
        // Add basic registry info
        try {
            AIDeityConfig basicConfig = new AIDeityConfig();
            String registryInfo = UniversalAIContext.buildFullContext(player, basicConfig);
            debug.append("\n").append(registryInfo);
        } catch (Exception e) {
            debug.append("Registry access failed: ").append(e.getMessage()).append("\n");
        }
        
        return debug.toString();
    }
}
