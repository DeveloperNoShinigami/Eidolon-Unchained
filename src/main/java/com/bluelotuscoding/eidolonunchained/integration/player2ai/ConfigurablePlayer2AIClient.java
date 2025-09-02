package com.bluelotuscoding.eidolonunchained.integration.player2ai;

import com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig;
import com.bluelotuscoding.eidolonunchained.ai.GenerationConfig;
import com.bluelotuscoding.eidolonunchained.ai.SafetySettings;
import com.bluelotuscoding.eidolonunchained.integration.gemini.GeminiAPIClient;
import com.google.gson.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.server.level.ServerPlayer;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Configurable Player2AI client that supports both local and server-hosted instances
 * 
 * Architecture Analysis:
 * 
 * LOCAL MODE (Current):
 * - Each player runs Player2AI desktop app on their machine
 * - Characters and memories are isolated per player
 * - Deities have different personalities per player
 * 
 * SERVER MODE (Multiplayer):
 * - One Player2AI instance runs on the server machine
 * - All players connect to the same Player2AI instance
 * - Shared deity characters with individual player memory tracking
 * - Consistent deity personalities across all players
 */
public class ConfigurablePlayer2AIClient {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String GAME_CLIENT_ID = "eidolon-unchained";
    private static final Executor EXECUTOR = Executors.newCachedThreadPool();
    
    private final int timeoutSeconds;
    private final Map<String, String> characterCache = new HashMap<>();
    private String currentApiBase;
    private String currentMode;
    
    public ConfigurablePlayer2AIClient() {
        this(30);
    }
    
    public ConfigurablePlayer2AIClient(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
        updateConfiguration();
    }
    
    /**
     * Update configuration based on current mod settings
     */
    public void updateConfiguration() {
        String mode = EidolonUnchainedConfig.COMMON.player2aiConnectionMode.get();
        String serverUrl = EidolonUnchainedConfig.COMMON.player2aiServerUrl.get();
        int serverPort = EidolonUnchainedConfig.COMMON.player2aiServerPort.get();
        
        if ("server".equalsIgnoreCase(mode)) {
            this.currentApiBase = "http://" + serverUrl + ":" + serverPort;
            this.currentMode = "server";
            LOGGER.info("Player2AI configured for SERVER mode: {}", currentApiBase);
        } else {
            this.currentApiBase = "http://127.0.0.1:4315";
            this.currentMode = "local";
            LOGGER.info("Player2AI configured for LOCAL mode: {}", currentApiBase);
        }
    }
    
    /**
     * Get the effective Player2AI URL for API calls
     */
    private String getApiUrl(String endpoint) {
        if (endpoint.startsWith("/")) {
            endpoint = endpoint.substring(1);
        }
        return currentApiBase + "/v1/" + endpoint;
    }
    
    /**
     * Get server-specific player key for shared instances
     * This ensures player isolation across different servers using the same Player2AI instance
     */
    private String getServerSpecificPlayerKey(ServerPlayer player) {
        if ("server".equals(currentMode)) {
            // Create unique identifier that includes server context
            String serverHash = Integer.toString(player.getServer().getServerDirectory().hashCode());
            return serverHash + ":" + player.getStringUUID();
        } else {
            // Local mode uses simple player UUID
            return player.getStringUUID();
        }
    }
    
    /**
     * Generate AI response with server-aware player tracking
     */
    public CompletableFuture<GeminiAPIClient.AIResponse> generateResponse(
            String prompt, 
            String personality,
            String characterId,
            ServerPlayer player,
            GenerationConfig genConfig,
            SafetySettings safetySettings) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                String playerKey = getServerSpecificPlayerKey(player);
                String response = sendChatCompletionRequest(prompt, personality, characterId, playerKey);
                
                return new GeminiAPIClient.AIResponse(true, response, Collections.emptyList());
                
            } catch (Exception e) {
                LOGGER.error("Player2AI request failed (mode: {})", currentMode, e);
                
                String errorMessage = "The deity's voice echoes from beyond the veil...";
                if (LOGGER.isDebugEnabled() || e.getMessage().contains("Connection refused")) {
                    if ("server".equals(currentMode)) {
                        errorMessage = "Server Player2AI Error: " + e.getMessage() + 
                            " (Check if Player2AI is running on server: " + currentApiBase + ")";
                    } else {
                        errorMessage = "Local Player2AI Error: " + e.getMessage() + 
                            " (Check if Player2AI desktop app is running)";
                    }
                }
                
                return new GeminiAPIClient.AIResponse(false, errorMessage, Collections.emptyList());
            }
        }, EXECUTOR);
    }
    
    /**
     * Send chat completion request using OpenAI-compatible format
     */
    private String sendChatCompletionRequest(String prompt, String personality, String characterId, String playerKey) throws IOException {
        // Ensure character exists first
        String npcId = ensureCharacterExists(characterId, personality);
        
        JsonObject request = new JsonObject();
        request.addProperty("model", "player2");
        
        JsonArray messages = new JsonArray();
        
        // System message with personality
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", personality + "\\n\\nYou are " + characterId + 
            " in a magical Minecraft world. Players interact with you through sacred rituals and prayers. " +
            "Remember past conversations and build relationships over time.\\n" +
            "Server mode: " + currentMode + " - " + 
            (currentMode.equals("server") ? "shared personality with individual memories" : "individual instance") +
            "\\nRespond in character. Keep responses engaging and immersive.");
        messages.add(systemMessage);
        
        // User message
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", prompt);
        userMessage.addProperty("user_id", playerKey); // Player2AI uses this for memory tracking
        messages.add(userMessage);
        
        request.add("messages", messages);
        request.addProperty("temperature", 0.8);
        request.addProperty("max_tokens", 200);
        
        String response = sendRequest("chat/completions", "POST", request.toString());
        
        JsonObject responseObj = JsonParser.parseString(response).getAsJsonObject();
        if (responseObj.has("choices")) {
            JsonArray choices = responseObj.getAsJsonArray("choices");
            if (choices.size() > 0) {
                JsonObject choice = choices.get(0).getAsJsonObject();
                JsonObject message = choice.getAsJsonObject("message");
                return message.get("content").getAsString();
            }
        }
        
        throw new IOException("Invalid response format from Player2AI");
    }
    
    /**
     * Ensure character exists, with different behavior for local vs server modes
     */
    private String ensureCharacterExists(String characterId, String personality) throws IOException {
        // Check cache first
        if (characterCache.containsKey(characterId)) {
            return characterCache.get(characterId);
        }
        
        // Try to find existing character
        String existingId = findExistingCharacter(characterId);
        if (existingId != null) {
            characterCache.put(characterId, existingId);
            return existingId;
        }
        
        // Create new character
        String newId = createCharacter(characterId, personality);
        characterCache.put(characterId, newId);
        return newId;
    }
    
    /**
     * Find existing character - simplified for now
     */
    private String findExistingCharacter(String characterId) throws IOException {
        // TODO: Implement character lookup by name
        return null;
    }
    
    /**
     * Create new character with server-aware configuration
     */
    private String createCharacter(String characterId, String personality) throws IOException {
        JsonObject request = new JsonObject();
        
        JsonObject character = new JsonObject();
        character.addProperty("name", characterId);
        character.addProperty("description", personality);
        character.addProperty("personality", personality);
        
        String contextNote = "server".equals(currentMode) 
            ? "This deity serves multiple players on a shared server. Maintain consistent personality while tracking individual relationships."
            : "This deity serves a single player on their local instance.";
            
        character.addProperty("context", "You are a divine entity in a magical Minecraft world. " +
            "Players interact with you through sacred rituals and prayers. " +
            "Remember past conversations and build relationships over time. " + contextNote);
        
        JsonObject settings = new JsonObject();
        settings.addProperty("temperature", 0.8);
        settings.addProperty("max_tokens", 200);
        settings.addProperty("memory_enabled", true);
        settings.addProperty("relationship_tracking", true);
        
        request.add("character", character);
        request.add("settings", settings);
        
        String response = sendRequest("characters", "POST", request.toString());
        
        JsonObject responseObj = JsonParser.parseString(response).getAsJsonObject();
        if (responseObj.has("id")) {
            String npcId = responseObj.get("id").getAsString();
            LOGGER.info("Created Player2AI character: {} with ID: {} (mode: {})", characterId, npcId, currentMode);
            return npcId;
        } else {
            throw new IOException("Failed to create character: " + response);
        }
    }
    
    /**
     * Send HTTP request to Player2AI API
     */
    private String sendRequest(String endpoint, String method, String jsonBody) throws IOException {
        URL url = URI.create(getApiUrl(endpoint)).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        connection.setRequestMethod(method);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("player2-game-key", GAME_CLIENT_ID);
        connection.setConnectTimeout(timeoutSeconds * 1000);
        connection.setReadTimeout(timeoutSeconds * 1000);
        connection.setDoOutput(true);
        
        // Send request body
        if (jsonBody != null && !jsonBody.isEmpty()) {
            try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(jsonBody);
                writer.flush();
            }
        }
        
        // Read response
        int responseCode = connection.getResponseCode();
        StringBuilder response = new StringBuilder();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                responseCode >= 200 && responseCode < 300 
                    ? connection.getInputStream() 
                    : connection.getErrorStream(), StandardCharsets.UTF_8))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        
        if (responseCode < 200 || responseCode >= 300) {
            LOGGER.error("Player2AI API error {} (mode: {}): {}", responseCode, currentMode, response.toString());
            throw new IOException("Player2AI API error: " + responseCode + " - " + response.toString());
        }
        
        return response.toString();
    }
    
    /**
     * Test connection to current Player2AI configuration
     */
    public static String testCurrentConfiguration() {
        try {
            ConfigurablePlayer2AIClient client = new ConfigurablePlayer2AIClient();
            StringBuilder results = new StringBuilder();
            
            results.append("=== Player2AI Configuration Test ===\\n");
            results.append("Mode: ").append(client.currentMode).append("\\n");
            results.append("API Base: ").append(client.currentApiBase).append("\\n");
            results.append("Shared Personalities: ").append(
                EidolonUnchainedConfig.COMMON.enableSharedDeityPersonalities.get()).append("\\n\\n");
            
            // Test health endpoint
            try {
                String healthUrl = client.getApiUrl("health");
                URL testUrl = URI.create(healthUrl).toURL();
                HttpURLConnection testConn = (HttpURLConnection) testUrl.openConnection();
                testConn.setRequestMethod("GET");
                testConn.setRequestProperty("player2-game-key", GAME_CLIENT_ID);
                testConn.setConnectTimeout(3000);
                testConn.setReadTimeout(3000);
                
                int responseCode = testConn.getResponseCode();
                results.append("Health Check: ").append(responseCode);
                
                if (responseCode == 200) {
                    results.append(" (HEALTHY)\\n");
                } else {
                    results.append(" (RESPONDING)\\n");
                }
                
            } catch (Exception e) {
                results.append("Health Check: CONNECTION REFUSED\\n");
                if ("server".equals(client.currentMode)) {
                    results.append("Server-hosted Player2AI is not responding\\n");
                    results.append("Ensure Player2AI desktop app is running on server: ")
                           .append(client.currentApiBase).append("\\n");
                } else {
                    results.append("Local Player2AI desktop app is not running\\n");
                }
            }
            
            return results.toString();
            
        } catch (Exception e) {
            return "Configuration test failed: " + e.getMessage();
        }
    }
    
    /**
     * Get current connection mode
     */
    public String getCurrentMode() {
        return currentMode;
    }
    
    /**
     * Get current API base URL
     */
    public String getCurrentApiBase() {
        return currentApiBase;
    }
    
    /**
     * Update all existing ConfigurablePlayer2AIClient instances with new configuration
     * This method is called when configuration changes via commands
     */
    public static void updateGlobalConfiguration() {
        // Note: This is a placeholder for global configuration updates
        // In practice, we might maintain a registry of active clients to update
        // For now, new instances will automatically pick up the updated configuration
        LOGGER.info("Player2AI configuration updated - new instances will use updated settings");
    }
    
    /**
     * Creates a new instance with current configuration
     */
    public static ConfigurablePlayer2AIClient createWithCurrentConfig() {
        return new ConfigurablePlayer2AIClient();
    }
}
