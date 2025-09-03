package com.bluelotuscoding.eidolonunchained.ai;

import net.minecraft.server.level.ServerPlayer;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Universal AI Provider Interface
 * All AI providers (Gemini, OpenAI, Anthropic, etc.) should implement this interface
 * This ensures consistent behavior and shared context access across all AI systems
 */
public interface UniversalAIProvider {
    
    /**
     * Generate AI response using universal context system
     * @param player The player making the prayer/request
     * @param prompt The player's message/prayer
     * @param personality The deity's personality prompt
     * @param config The deity's AI configuration (includes priority mods, etc.)
     * @return Future containing the AI response with dialogue and commands
     */
    CompletableFuture<AIResponse> generateResponse(
        ServerPlayer player,
        String prompt, 
        String personality,
        AIDeityConfig config
    );
    
    /**
     * Get the provider type name (e.g., "gemini", "openai", "anthropic")
     */
    String getProviderType();
    
    /**
     * Check if this provider is properly configured and ready to use
     */
    boolean isConfigured();
    
    /**
     * Get configuration requirements for this provider
     */
    List<String> getConfigurationRequirements();
    
    /**
     * AI Response container - standardized across all providers
     */
    class AIResponse {
        public final boolean success;
        public final String dialogue;
        public final List<String> commands;
        public final String errorMessage;
        
        public AIResponse(boolean success, String dialogue, List<String> commands) {
            this.success = success;
            this.dialogue = dialogue;
            this.commands = commands;
            this.errorMessage = null;
        }
        
        public AIResponse(boolean success, String dialogue, List<String> commands, String errorMessage) {
            this.success = success;
            this.dialogue = dialogue;
            this.commands = commands;
            this.errorMessage = errorMessage;
        }
    }
}
