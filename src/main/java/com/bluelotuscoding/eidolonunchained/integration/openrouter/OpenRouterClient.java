package com.bluelotuscoding.eidolonunchained.integration.openrouter;

import com.bluelotuscoding.eidolonunchained.ai.GenerationConfig;
import com.bluelotuscoding.eidolonunchained.ai.SafetySettings;
import com.bluelotuscoding.eidolonunchained.integration.gemini.GeminiAPIClient;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * OpenRouter API client for AI deity interactions
 * OpenRouter provides access to multiple AI models through a unified OpenAI-compatible API
 * https://openrouter.ai/docs
 */
public class OpenRouterClient {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String OPENROUTER_API_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final Gson GSON = new Gson();

    private final String apiKey;
    private final String model;
    private final int timeoutSeconds;
    private final HttpClient httpClient;

    /**
     * Create OpenRouter client with API key and model
     * @param apiKey OpenRouter API key
     * @param model Model to use (e.g., "anthropic/claude-3.5-sonnet", "meta-llama/llama-3.1-8b-instruct")
     * @param timeoutSeconds Request timeout in seconds
     */
    public OpenRouterClient(String apiKey, String model, int timeoutSeconds) {
        this.apiKey = apiKey;
        this.model = model != null ? model : "anthropic/claude-3.5-sonnet"; // Default to Claude 3.5 Sonnet
        this.timeoutSeconds = timeoutSeconds;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(timeoutSeconds))
            .build();
    }

    /**
     * Generate AI response using OpenRouter API
     */
    public CompletableFuture<GeminiAPIClient.AIResponse> generateResponse(
            String prompt, String personality, GenerationConfig genConfig, SafetySettings safetySettings) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Build the conversation with system and user messages
                JsonArray messages = new JsonArray();
                
                // Add system message (personality)
                if (personality != null && !personality.trim().isEmpty()) {
                    JsonObject systemMessage = new JsonObject();
                    systemMessage.addProperty("role", "system");
                    systemMessage.addProperty("content", personality);
                    messages.add(systemMessage);
                }
                
                // Add user message (prompt)
                JsonObject userMessage = new JsonObject();
                userMessage.addProperty("role", "user");
                userMessage.addProperty("content", prompt);
                messages.add(userMessage);
                
                // Build request body
                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("model", model);
                requestBody.add("messages", messages);
                
                // Apply generation config
                if (genConfig != null) {
                    requestBody.addProperty("max_tokens", genConfig.max_output_tokens);
                    requestBody.addProperty("temperature", genConfig.temperature);
                    requestBody.addProperty("top_p", genConfig.top_p);
                }
                
                // Create HTTP request
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OPENROUTER_API_URL))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("HTTP-Referer", "https://github.com/YourUsername/Eidolon-Unchained") // Optional but recommended
                    .header("X-Title", "Eidolon Unchained - AI Deity Mod") // Optional but recommended
                    .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(requestBody)))
                    .build();
                
                LOGGER.debug("Sending OpenRouter API request with model: {} and prompt length: {}", 
                    model, prompt.length());
                
                // Send request
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    return parseSuccessResponse(response.body());
                } else {
                    LOGGER.error("OpenRouter API request failed with status {}: {}", 
                        response.statusCode(), response.body());
                    return createErrorResponse("API request failed: " + response.statusCode());
                }
                
            } catch (IOException | InterruptedException e) {
                LOGGER.error("OpenRouter API request failed: {}", e.getMessage(), e);
                return createErrorResponse("Request failed: " + e.getMessage());
            } catch (Exception e) {
                LOGGER.error("Unexpected error in OpenRouter API call: {}", e.getMessage(), e);
                return createErrorResponse("Unexpected error: " + e.getMessage());
            }
        });
    }
    
    /**
     * Parse successful OpenRouter API response
     */
    private GeminiAPIClient.AIResponse parseSuccessResponse(String responseBody) {
        try {
            JsonObject jsonResponse = GSON.fromJson(responseBody, JsonObject.class);
            
            if (jsonResponse.has("choices") && jsonResponse.getAsJsonArray("choices").size() > 0) {
                JsonObject choice = jsonResponse.getAsJsonArray("choices").get(0).getAsJsonObject();
                JsonObject message = choice.getAsJsonObject("message");
                String content = message.get("content").getAsString();
                
                LOGGER.debug("OpenRouter API response received, content length: {}", content.length());
                
                return new GeminiAPIClient.AIResponse(content, null, true);
            } else {
                LOGGER.warn("OpenRouter API response missing choices: {}", responseBody);
                return createErrorResponse("Invalid response format");
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to parse OpenRouter API response: {}", e.getMessage(), e);
            return createErrorResponse("Failed to parse response");
        }
    }
    
    /**
     * Create error response
     */
    private GeminiAPIClient.AIResponse createErrorResponse(String error) {
        return new GeminiAPIClient.AIResponse(
            "The deity remains silent...", 
            error, 
            false
        );
    }
    
    /**
     * Test OpenRouter connection
     */
    public CompletableFuture<Boolean> testConnection() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Simple test prompt
                JsonArray messages = new JsonArray();
                JsonObject userMessage = new JsonObject();
                userMessage.addProperty("role", "user");
                userMessage.addProperty("content", "Respond with 'OK' if you can hear me.");
                messages.add(userMessage);
                
                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("model", model);
                requestBody.add("messages", messages);
                requestBody.addProperty("max_tokens", 10);
                
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OPENROUTER_API_URL))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(requestBody)))
                    .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                boolean success = response.statusCode() == 200;
                if (success) {
                    LOGGER.info("OpenRouter connection test successful with model: {}", model);
                } else {
                    LOGGER.warn("OpenRouter connection test failed: {} - {}", response.statusCode(), response.body());
                }
                
                return success;
                
            } catch (Exception e) {
                LOGGER.error("OpenRouter connection test failed: {}", e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Get the configured model name
     */
    public String getModel() {
        return model;
    }
}
