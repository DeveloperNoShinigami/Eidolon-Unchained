package com.bluelotuscoding.eidolonunchained.integration.ai;

import com.bluelotuscoding.eidolonunchained.config.AIProviderConfig;
import com.bluelotuscoding.eidolonunchained.config.APIKeyManager;
import com.bluelotuscoding.eidolonunchained.integration.gemini.GeminiAPIClient;
import com.bluelotuscoding.eidolonunchained.ai.GenerationConfig;
import com.bluelotuscoding.eidolonunchained.ai.SafetySettings;
import java.util.concurrent.CompletableFuture;

/**
 * Abstract factory for different AI providers
 */
public class AIProviderFactory {
    
    public static AIProvider createProvider() {
        String providerType = AIProviderConfig.INSTANCE.providerType.get();
        
        return switch (providerType.toLowerCase()) {
            case "direct" -> new DirectAIProvider();
            case "proxy" -> new ProxyAIProvider();
            case "hybrid" -> new HybridAIProvider();
            default -> new DirectAIProvider(); // Default fallback
        };
    }
    
    /**
     * Interface for AI providers
     */
    public interface AIProvider {
        CompletableFuture<String> generateResponse(String prompt, String playerName, int reputation);
        boolean isConfigured();
        String getProviderName();
    }
    
    /**
     * Direct API provider (OpenAI, Gemini, etc.)
     */
    public static class DirectAIProvider implements AIProvider {
        @Override
        public CompletableFuture<String> generateResponse(String prompt, String playerName, int reputation) {
            // Create a simple GeminiAPIClient and use it with default settings
            String apiKey = APIKeyManager.getAPIKey("gemini");
            if (apiKey == null) {
                return CompletableFuture.completedFuture("§cAI provider not configured.");
            }
            
            GeminiAPIClient client = new GeminiAPIClient(apiKey, "gemini-1.5-flash", 30);
            
            // Create basic generation config and safety settings
            GenerationConfig config = new GenerationConfig();
            config.temperature = 0.7f;
            config.max_output_tokens = 500;
            
            SafetySettings safety = new SafetySettings();
            
            return client.generateResponse(prompt, "AI Assistant", config, safety)
                .thenApply(response -> response != null ? response.dialogue : "§cNo response received.");
        }
        
        @Override
        public boolean isConfigured() {
            String apiKey = APIKeyManager.getAPIKey("gemini");
            return apiKey != null && !apiKey.isEmpty();
        }
        
        @Override
        public String getProviderName() {
            return "Direct API";
        }
    }
    
    /**
     * Proxy service provider (Player2.game, etc.)
     */
    public static class ProxyAIProvider implements AIProvider {
        @Override
        public CompletableFuture<String> generateResponse(String prompt, String playerName, int reputation) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    // Call proxy service API
                    String proxyUrl = AIProviderConfig.INSTANCE.proxyServiceUrl.get();
                    String proxyKey = AIProviderConfig.INSTANCE.proxyServiceKey.get();
                    
                    // TODO: Implement HTTP client for proxy service
                    // Example for Player2.game API:
                    /*
                    HttpClient client = HttpClient.newHttpClient();
                    HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(proxyUrl))
                        .header("Authorization", "Bearer " + proxyKey)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(buildJsonPayload(prompt)))
                        .build();
                    
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    return parseResponse(response.body());
                    */
                    
                    return "Response from proxy service for: " + playerName;
                } catch (Exception e) {
                    return "Error connecting to proxy service: " + e.getMessage();
                }
            });
        }
        
        @Override
        public boolean isConfigured() {
            String proxyKey = AIProviderConfig.INSTANCE.proxyServiceKey.get();
            return proxyKey != null && !proxyKey.isEmpty();
        }
        
        @Override
        public String getProviderName() {
            return "Proxy Service";
        }
    }
    
    /**
     * Hybrid provider - tries proxy first, fallback to direct
     */
    public static class HybridAIProvider implements AIProvider {
        private final ProxyAIProvider proxyProvider = new ProxyAIProvider();
        private final DirectAIProvider directProvider = new DirectAIProvider();
        
        @Override
        public CompletableFuture<String> generateResponse(String prompt, String playerName, int reputation) {
            if (proxyProvider.isConfigured()) {
                return proxyProvider.generateResponse(prompt, playerName, reputation)
                    .exceptionally(throwable -> {
                        // Fallback to direct provider if proxy fails
                        if (directProvider.isConfigured()) {
                            try {
                                return directProvider.generateResponse(prompt, playerName, reputation).get();
                            } catch (Exception e) {
                                return "AI service unavailable";
                            }
                        }
                        return "AI service unavailable";
                    });
            } else if (directProvider.isConfigured()) {
                return directProvider.generateResponse(prompt, playerName, reputation);
            } else {
                return CompletableFuture.completedFuture("AI not configured");
            }
        }
        
        @Override
        public boolean isConfigured() {
            return proxyProvider.isConfigured() || directProvider.isConfigured();
        }
        
        @Override
        public String getProviderName() {
            return "Hybrid (Proxy + Direct)";
        }
    }
}
