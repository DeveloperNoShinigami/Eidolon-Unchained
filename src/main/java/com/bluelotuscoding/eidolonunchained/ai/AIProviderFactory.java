package com.bluelotuscoding.eidolonunchained.ai;

import com.bluelotuscoding.eidolonunchained.config.APIKeyManager;
import com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig;
import com.bluelotuscoding.eidolonunchained.integration.gemini.GeminiAPIClient;
import com.bluelotuscoding.eidolonunchained.integration.openrouter.OpenRouterClient;
import com.bluelotuscoding.eidolonunchained.integration.player2ai.Player2AIClient;
import com.bluelotuscoding.eidolonunchained.integration.player2ai.Player2HealthSignal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

/**
 * Factory for creating AI provider clients based on configuration
 * Supports multiple AI providers: Gemini, Player2AI, OpenAI (future)
 */
public class AIProviderFactory {
    private static final Logger LOGGER = LogManager.getLogger();
    
    /**
     * Create an AI client based on the configured provider
     */
    public static AIProvider createProvider() {
        String provider = EidolonUnchainedConfig.COMMON.aiProvider.get().toLowerCase();
        
        switch (provider) {
            case "gemini":
                return createGeminiProvider();
            case "player2ai":
            case "player2":
                return createPlayer2AIProvider();
            case "openrouter":
                return createOpenRouterProvider();
            case "openai":
                return createOpenAIProvider(); // Future implementation
            default:
                LOGGER.warn("Unknown AI provider: {}, falling back to Gemini", provider);
                return createGeminiProvider();
        }
    }
    
    /**
     * Create an AI client based on specified provider and model (for deity-specific configurations)
     */
    public static AIProvider createProvider(String provider, String model) {
        if (provider == null || provider.trim().isEmpty()) {
            LOGGER.warn("No provider specified, using global configuration");
            return createProvider();
        }
        
        provider = provider.toLowerCase();
        
        switch (provider) {
            case "gemini":
                return createGeminiProvider(model);
            case "player2ai":
            case "player2":
                return createPlayer2AIProvider(); // Player2AI doesn't use model parameter
            case "openrouter":
                return createOpenRouterProvider(model);
            case "openai":
                return createOpenAIProvider(model); // Future implementation
            default:
                LOGGER.warn("Unknown AI provider: {}, falling back to Gemini", provider);
                return createGeminiProvider();
        }
    }
    
    /**
     * Create Gemini AI provider
     */
    private static AIProvider createGeminiProvider() {
        String model = EidolonUnchainedConfig.COMMON.geminiModel.get();
        return createGeminiProvider(model);
    }
    
    /**
     * Create Gemini AI provider with specific model
     */
    private static AIProvider createGeminiProvider(String model) {
        String apiKey = APIKeyManager.getAPIKey("gemini");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            LOGGER.error("No Gemini API key configured");
            return new DummyAIProvider();
        }
        
        if (model == null || model.trim().isEmpty()) {
            model = EidolonUnchainedConfig.COMMON.geminiModel.get();
        }
        
        int timeout = EidolonUnchainedConfig.COMMON.geminiTimeout.get();
        
        GeminiAPIClient client = new GeminiAPIClient(apiKey, model, timeout);
        return new GeminiAIProvider(client);
    }
    
    /**
     * Create Player2AI provider
     */
    private static AIProvider createPlayer2AIProvider() {
        String apiKey = APIKeyManager.getAPIKey("player2ai");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            LOGGER.error("No Player2AI API key configured");
            return new DummyAIProvider();
        }
        
        int timeout = EidolonUnchainedConfig.COMMON.geminiTimeout.get(); // Reuse timeout setting
        
        Player2AIClient client;
        
        // Check if this is a local connection (no real API key needed)
        if ("local".equals(apiKey.trim().toLowerCase())) {
            client = new Player2AIClient(); // Use default constructor for local connection
            LOGGER.info("Creating Player2AI provider for local Player2 App connection");
        } else {
            client = new Player2AIClient(apiKey, timeout); // Use API key for cloud connection
            LOGGER.info("Creating Player2AI provider for cloud connection");
        }
        
        // Start health signal as required by Player2AI jam submission rules
        Player2HealthSignal.startHealthSignal();
        
        return new Player2AIProvider(client);
    }
    
    /**
     * Create OpenRouter provider
     */
    private static AIProvider createOpenRouterProvider() {
        String model = EidolonUnchainedConfig.COMMON.openrouterModel.get();
        return createOpenRouterProvider(model);
    }
    
    /**
     * Create OpenRouter provider with specific model
     */
    private static AIProvider createOpenRouterProvider(String model) {
        String apiKey = APIKeyManager.getAPIKey("openrouter");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            LOGGER.error("No OpenRouter API key configured");
            return new DummyAIProvider();
        }
        
        if (model == null || model.trim().isEmpty()) {
            model = EidolonUnchainedConfig.COMMON.openrouterModel.get();
        }
        
        int timeout = EidolonUnchainedConfig.COMMON.geminiTimeout.get(); // Reuse timeout setting
        
        // Map common model names to OpenRouter format
        String openRouterModel = mapToOpenRouterModel(model);
        
        OpenRouterClient client = new OpenRouterClient(apiKey, openRouterModel, timeout);
        return new OpenRouterAIProvider(client);
    }
    
    /**
     * Map common model names to OpenRouter model identifiers
     */
    private static String mapToOpenRouterModel(String model) {
        if (model == null || model.trim().isEmpty()) {
            return "anthropic/claude-3.5-sonnet"; // Default to Claude 3.5 Sonnet
        }
        
        // If it's already in OpenRouter format (contains '/'), use as-is
        if (model.contains("/")) {
            return model;
        }
        
        // Map common names to OpenRouter identifiers
        switch (model.toLowerCase()) {
            case "claude":
            case "claude-3.5":
            case "claude-3.5-sonnet":
                return "anthropic/claude-3.5-sonnet";
            case "claude-3-haiku":
                return "anthropic/claude-3-haiku";
            case "gpt-4":
            case "gpt-4o":
                return "openai/gpt-4o";
            case "gpt-4-turbo":
                return "openai/gpt-4-turbo";
            case "gpt-3.5":
            case "gpt-3.5-turbo":
                return "openai/gpt-3.5-turbo";
            case "llama":
            case "llama-3.1":
            case "llama-3.1-8b":
                return "meta-llama/llama-3.1-8b-instruct";
            case "llama-3.1-70b":
                return "meta-llama/llama-3.1-70b-instruct";
            default:
                LOGGER.warn("Unknown model '{}', defaulting to Claude 3.5 Sonnet", model);
                return "anthropic/claude-3.5-sonnet";
        }
    }

    /**
     * Create OpenAI provider (future implementation)
     */
    private static AIProvider createOpenAIProvider() {
        LOGGER.warn("OpenAI provider not yet implemented, falling back to Gemini");
        return createGeminiProvider();
    }
    
    /**
     * Create OpenAI provider with specific model (future implementation)
     */
    private static AIProvider createOpenAIProvider(String model) {
        LOGGER.warn("OpenAI provider not yet implemented, falling back to Gemini with model: {}", model);
        return createGeminiProvider(model);
    }
    
    /**
     * Interface for AI providers
     */
    public interface AIProvider {
        CompletableFuture<GeminiAPIClient.AIResponse> generateResponse(
            String prompt, 
            String personality,
            String context,
            GenerationConfig genConfig,
            SafetySettings safetySettings
        );
        
        String getProviderName();
        boolean isAvailable();
    }
    
    /**
     * Gemini AI provider implementation
     */
    public static class GeminiAIProvider implements AIProvider {
        private final GeminiAPIClient client;
        
        public GeminiAIProvider(GeminiAPIClient client) {
            this.client = client;
        }
        
        @Override
        public CompletableFuture<GeminiAPIClient.AIResponse> generateResponse(
                String prompt, String personality, String context,
                GenerationConfig genConfig, SafetySettings safetySettings) {
            return client.generateResponse(prompt, personality, genConfig, safetySettings);
        }
        
        @Override
        public String getProviderName() {
            return "Google Gemini";
        }
        
        @Override
        public boolean isAvailable() {
            return APIKeyManager.hasAPIKey("gemini");
        }
    }
    
    /**
     * Player2AI provider implementation
     */
    public static class Player2AIProvider implements AIProvider {
        private final Player2AIClient client;
        
        public Player2AIProvider(Player2AIClient client) {
            this.client = client;
        }
        
        @Override
        public CompletableFuture<GeminiAPIClient.AIResponse> generateResponse(
                String prompt, String personality, String context,
                GenerationConfig genConfig, SafetySettings safetySettings) {
            
            // Extract deity ID and player UUID from context
            String characterId = extractDeityId(context);
            String playerUUID = extractPlayerUUID(context);
            
            return client.generateResponse(prompt, personality, characterId, playerUUID, genConfig, safetySettings);
        }
        
        @Override
        public String getProviderName() {
            return "Player2AI";
        }
        
        @Override
        public boolean isAvailable() {
            return APIKeyManager.hasAPIKey("player2ai");
        }
        
        private String extractDeityId(String context) {
            // Extract deity identifier from context
            // Context might be like "deity:eidolonunchained:nature_deity,player:uuid"
            if (context != null && context.contains("deity:")) {
                String[] parts = context.split(",");
                for (String part : parts) {
                    if (part.startsWith("deity:")) {
                        return part.substring("deity:".length());
                    }
                }
            }
            return "unknown_deity";
        }
        
        private String extractPlayerUUID(String context) {
            // Extract player UUID from context
            if (context != null && context.contains("player:")) {
                String[] parts = context.split(",");
                for (String part : parts) {
                    if (part.startsWith("player:")) {
                        return part.substring("player:".length());
                    }
                }
            }
            return "unknown_player";
        }
    }
    
    /**
     * OpenRouter AI provider implementation
     */
    public static class OpenRouterAIProvider implements AIProvider {
        private final OpenRouterClient client;
        
        public OpenRouterAIProvider(OpenRouterClient client) {
            this.client = client;
        }
        
        @Override
        public CompletableFuture<GeminiAPIClient.AIResponse> generateResponse(
                String prompt, String personality, String context,
                GenerationConfig genConfig, SafetySettings safetySettings) {
            return client.generateResponse(prompt, personality, genConfig, safetySettings);
        }
        
        @Override
        public String getProviderName() {
            return "OpenRouter (" + client.getModel() + ")";
        }
        
        @Override
        public boolean isAvailable() {
            return APIKeyManager.hasAPIKey("openrouter");
        }
    }
    
    /**
     * Dummy provider for when no valid provider is available
     */
    public static class DummyAIProvider implements AIProvider {
        @Override
        public CompletableFuture<GeminiAPIClient.AIResponse> generateResponse(
                String prompt, String personality, String context,
                GenerationConfig genConfig, SafetySettings safetySettings) {
            return CompletableFuture.completedFuture(
                new GeminiAPIClient.AIResponse(false, 
                    "No AI provider configured. Please set up an API key.", Collections.emptyList())
            );
        }
        
        @Override
        public String getProviderName() {
            return "None (No API key configured)";
        }
        
        @Override
        public boolean isAvailable() {
            return false;
        }
    }
}
