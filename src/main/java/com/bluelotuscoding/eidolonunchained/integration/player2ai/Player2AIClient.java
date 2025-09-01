package com.bluelotuscoding.eidolonunchained.integration.player2ai;

import com.bluelotuscoding.eidolonunchained.ai.GenerationConfig;
import com.bluelotuscoding.eidolonunchained.ai.SafetySettings;
import com.bluelotuscoding.eidolonunchained.integration.gemini.GeminiAPIClient;
import com.google.gson.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
import java.util.stream.Collectors;

/**
 * Player2AI API client for AI deity interactions
 * Provides character persistence, memory, and gaming-focused AI responses
 */
public class Player2AIClient {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String PLAYER2_CLOUD_API_BASE = "https://api.player2.game/v1/npc/";
    private static final String PLAYER2_LOCAL_API_BASE = "http://127.0.0.1:4315/v1/chat/completions"; // OpenAI-compatible endpoint
    private static final String PLAYER2_AUTH_BASE = "http://localhost:4316/v1/login/web/";
    private static final String GAME_CLIENT_ID = "eidolon-unchained"; // Player2AI game client ID
    private static final Executor EXECUTOR = Executors.newCachedThreadPool();
    
    private final String apiKey;
    private final int timeoutSeconds;
    private final boolean useLocalInstance;
    private final Map<String, String> characterCache = new HashMap<>();
    
    /**
     * Default constructor for local Player2 App connection
     */
    public Player2AIClient() {
        this("local", 30); // Use "local" as placeholder and 30 second timeout
    }
    
    public Player2AIClient(String apiKey, int timeoutSeconds) {
        this.apiKey = apiKey;
        this.timeoutSeconds = timeoutSeconds;
        // Determine if using local instance (no API key or localhost URL)
        this.useLocalInstance = apiKey == null || apiKey.trim().isEmpty() || 
            apiKey.contains("localhost") || apiKey.contains("127.0.0.1") || 
            "local".equals(apiKey.trim().toLowerCase());
        
        if (useLocalInstance) {
            LOGGER.info("Player2AI client initialized for LOCAL instance (Player2AI desktop app)");
        } else {
            LOGGER.info("Player2AI client initialized for CLOUD instance with API key: {}...", 
                apiKey.substring(0, Math.min(8, apiKey.length())));
        }
    }
    
    /**
     * Quick Start authentication - get API key from local Player2 App
     * This is the easiest method if the player has Player2 App installed and logged in
     */
    public static String authenticateWithPlayer2App() {
        try {
            String authUrl = PLAYER2_AUTH_BASE + GAME_CLIENT_ID;
            URL url = URI.create(authUrl).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(5000); // 5 second timeout for local app
            connection.setReadTimeout(5000);
            
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String response = reader.lines().collect(Collectors.joining("\n"));
                    
                    // Parse {"p2Key": "<api-key>"}
                    JsonObject responseObj = JsonParser.parseString(response).getAsJsonObject();
                    if (responseObj.has("p2Key")) {
                        String apiKey = responseObj.get("p2Key").getAsString();
                        LOGGER.info("Successfully authenticated with Player2 App, got API key: {}...", 
                            apiKey.substring(0, Math.min(8, apiKey.length())));
                        return apiKey;
                    } else {
                        LOGGER.warn("Player2 App authentication response missing p2Key: {}", response);
                        return null;
                    }
                }
            } else {
                LOGGER.warn("Player2 App authentication failed with status: {} - {}", 
                    responseCode, connection.getResponseMessage());
                return null;
            }
        } catch (Exception e) {
            // This is normal when Player2 App isn't installed/running
            // Only log connection issues at trace level to avoid spam
            if (e.getMessage() != null && e.getMessage().contains("Connection refused")) {
                LOGGER.trace("Player2 App not running locally (normal): {}", e.getMessage());
            } else {
                LOGGER.debug("Player2 App not available for authentication: {}", e.getMessage());
            }
            return null;
        }
    }
    
    /**
     * Test connection to Player2 App and return diagnostic information
     */
    public static String testPlayer2AppConnection() {
        try {
            // Test the OpenAI-compatible chat completions endpoint that MCA uses
            StringBuilder results = new StringBuilder();
            
            try {
                URL testUrl = URI.create("http://127.0.0.1:4315/v1/chat/completions").toURL();
                HttpURLConnection testConn = (HttpURLConnection) testUrl.openConnection();
                testConn.setRequestMethod("POST");
                testConn.setRequestProperty("Content-Type", "application/json");
                testConn.setRequestProperty("player2-game-key", GAME_CLIENT_ID);
                testConn.setConnectTimeout(3000);
                testConn.setReadTimeout(3000);
                testConn.setDoOutput(true);
                
                // Send minimal test payload
                String testPayload = "{\"model\":\"player2\",\"messages\":[{\"role\":\"user\",\"content\":\"test\"}]}";
                try (OutputStreamWriter writer = new OutputStreamWriter(testConn.getOutputStream())) {
                    writer.write(testPayload);
                    writer.flush();
                }
                
                int responseCode = testConn.getResponseCode();
                results.append("Player2 App Chat API (4315): ").append(responseCode);
                
                if (responseCode == 200) {
                    results.append(" (AVAILABLE - Player2 App is running!)\n");
                } else if (responseCode == 400 || responseCode == 422) {
                    results.append(" (AVAILABLE - App running, expects different format)\n");
                } else {
                    results.append(" (HTTP ERROR - App may not be fully initialized)\n");
                }
                
            } catch (Exception e) {
                results.append("Player2 App Chat API (4315): CONNECTION REFUSED\n");
                results.append("This means Player2 App is not running or not responding\n");
            }
            
            // Test authentication endpoint (different port)
            try {
                URL authUrl = URI.create("http://127.0.0.1:4316/v1/login/web/" + GAME_CLIENT_ID).toURL();
                HttpURLConnection authConn = (HttpURLConnection) authUrl.openConnection();
                authConn.setRequestMethod("POST");
                authConn.setRequestProperty("Content-Type", "application/json");
                authConn.setConnectTimeout(3000);
                authConn.setReadTimeout(3000);
                authConn.setDoOutput(true);
                
                int authCode = authConn.getResponseCode();
                results.append("Player2 Auth API (4316): ").append(authCode);
                
                if (authCode == 200) {
                    results.append(" (AVAILABLE - Authentication ready)\n");
                } else {
                    results.append(" (RESPONDING - Auth may need login)\n");
                }
                
            } catch (Exception e) {
                results.append("Player2 Auth API (4316): CONNECTION REFUSED\n");
                results.append("NOTE: Auth port 4316 not responding. This is common.\n");
                results.append("You can still use Player2AI with manual API key setup.\n");
            }
            
            // Also test health endpoint
            try {
                URL healthUrl = URI.create("http://127.0.0.1:4315/v1/health").toURL();
                HttpURLConnection healthConn = (HttpURLConnection) healthUrl.openConnection();
                healthConn.setRequestMethod("GET");
                healthConn.setRequestProperty("player2-game-key", GAME_CLIENT_ID);
                healthConn.setConnectTimeout(3000);
                healthConn.setReadTimeout(3000);
                
                int healthCode = healthConn.getResponseCode();
                results.append("Player2 Health Check (4315): ").append(healthCode);
                
                if (healthCode == 200) {
                    results.append(" (HEALTHY)\n");
                } else {
                    results.append(" (RESPONDING)\n");
                }
                
            } catch (Exception e) {
                results.append("Player2 Health Check (4315): CONNECTION REFUSED\n");
            }
            
            return results.toString();
        } catch (Exception e) {
            return "Diagnostic failed: " + e.getMessage();
        }
    }
    
    /**
     * Test if Player2 App is available and responding
     * @return true if Player2 App is responding, false otherwise
     */
    public static boolean isPlayer2AppAvailable() {
        try {
            URL testUrl = URI.create("http://127.0.0.1:4315/v1/health").toURL();
            HttpURLConnection testConn = (HttpURLConnection) testUrl.openConnection();
            testConn.setRequestMethod("GET");
            testConn.setRequestProperty("player2-game-key", GAME_CLIENT_ID);
            testConn.setConnectTimeout(3000);
            testConn.setReadTimeout(3000);
            
            int responseCode = testConn.getResponseCode();
            return responseCode == 200 || responseCode == 400 || responseCode == 422; // Any response means it's running
        } catch (Exception e) {
            return false; // Connection failed
        }
    }
    
    /**
     * Generate AI response for deity interaction using Player2AI OpenAI-compatible endpoint
     */
    public CompletableFuture<GeminiAPIClient.AIResponse> generateResponse(
            String prompt, 
            String personality,
            String characterId,
            String playerUUID,
            GenerationConfig genConfig,
            SafetySettings safetySettings) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Use OpenAI-compatible chat completions format like MCA
                String response = sendChatCompletionRequest(prompt, personality, characterId, playerUUID);
                
                return new GeminiAPIClient.AIResponse(true, response, Collections.emptyList());
                
            } catch (Exception e) {
                LOGGER.error("Player2AI request failed", e);
                
                // In debug mode or development, show actual error details
                String errorMessage = "The deity's voice echoes from beyond the veil...";
                if (LOGGER.isDebugEnabled() || e.getMessage().contains("Connection refused")) {
                    errorMessage = "Player2AI Error: " + e.getMessage() + 
                        " (Check if Player2 App is running on localhost:4315)";
                }
                
                return new GeminiAPIClient.AIResponse(false, errorMessage, Collections.emptyList());
            }
        }, EXECUTOR);
    }
    
    /**
     * Send OpenAI-compatible chat completion request to Player2 App (like MCA does)
     */
    private String sendChatCompletionRequest(String prompt, String personality, String characterId, String playerUUID) throws IOException {
        // Build OpenAI-compatible request
        JsonObject request = new JsonObject();
        request.addProperty("model", "player2");
        
        JsonArray messages = new JsonArray();
        
        // System message with personality and divine behavior instructions
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", personality + "\n\nYou are " + characterId + 
            ", a deity in a Minecraft world. " +
            "\n\nAs a divine being:" +
            "\n- Engage meaningfully with mortals who seek your guidance" +
            "\n- Consider their reputation and worthiness when granting requests" +
            "\n- Respond to their actual words and needs" +
            "\n- You may grant items, effects, or blessings as appropriate" +
            "\n- Speak with divine wisdom but remain accessible" +
            "\nRespond in character. Keep responses engaging and immersive.");
        messages.add(systemMessage);
        
        // User message
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("name", playerUUID);
        userMessage.addProperty("content", prompt);
        messages.add(userMessage);
        
        request.add("messages", messages);
        
        // Optional parameters
        request.addProperty("max_tokens", 150);
        request.addProperty("temperature", 0.8);
        
        // Send request to the OpenAI-compatible endpoint
        String endpoint = useLocalInstance ? PLAYER2_LOCAL_API_BASE : PLAYER2_CLOUD_API_BASE + "chat/completions";
        String response = sendRequest(endpoint, "POST", request.toString());
        
        // Parse OpenAI-compatible response
        JsonObject responseObj = JsonParser.parseString(response).getAsJsonObject();
        if (responseObj.has("choices")) {
            JsonArray choices = responseObj.getAsJsonArray("choices");
            if (choices.size() > 0) {
                JsonObject firstChoice = choices.get(0).getAsJsonObject();
                JsonObject message = firstChoice.getAsJsonObject("message");
                return message.get("content").getAsString();
            }
        }
        
        throw new IOException("Invalid OpenAI response format: " + response);
    }
    
    /**
     * Ensure a character exists for the deity, create if needed
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
     * Check if character already exists
     */
    private String findExistingCharacter(String characterId) throws IOException {
        // Player2AI uses character names as identifiers
        // We'll use the deity name as the character identifier
        return null; // For now, always create new characters
    }
    
    /**
     * Create a new Player2AI character for a deity
     */
    private String createCharacter(String characterId, String personality) throws IOException {
        JsonObject request = new JsonObject();
        
        // Character creation payload
        JsonObject character = new JsonObject();
        character.addProperty("name", characterId);
        character.addProperty("description", personality);
        character.addProperty("personality", personality);
        character.addProperty("context", "You are a divine entity in a magical Minecraft world. " +
            "Players interact with you through sacred rituals and prayers. " +
            "Remember past conversations and build relationships over time.");
        
        // Gaming-specific settings
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
            LOGGER.info("Created Player2AI character: {} with ID: {}", characterId, npcId);
            return npcId;
        } else {
            throw new IOException("Failed to create character: " + response);
        }
    }
    
    /**
     * Send message to Player2AI character
     */
    private String sendMessageToCharacter(String npcId, String message, String playerUUID) throws IOException {
        JsonObject request = new JsonObject();
        request.addProperty("npc_id", npcId);
        request.addProperty("message", message);
        request.addProperty("user_id", playerUUID); // Track individual players
        
        // Add context for better responses
        JsonObject context = new JsonObject();
        context.addProperty("environment", "minecraft_world");
        context.addProperty("interaction_type", "prayer");
        context.addProperty("setting", "divine_shrine");
        request.add("context", context);
        
        String response = sendRequest("chat", "POST", request.toString());
        
        JsonObject responseObj = JsonParser.parseString(response).getAsJsonObject();
        if (responseObj.has("response")) {
            return responseObj.get("response").getAsString();
        } else if (responseObj.has("message")) {
            return responseObj.get("message").getAsString();
        } else {
            throw new IOException("Invalid response format: " + response);
        }
    }
    
    /**
     * Send HTTP request to Player2AI API (OpenAI-compatible format)
     */
    private String sendRequest(String urlString, String method, String jsonBody) throws IOException {
        URL url = URI.create(urlString).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        // Configure connection
        connection.setRequestMethod(method);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("player2-game-key", GAME_CLIENT_ID);
        
        // Set authentication based on instance type
        if (!useLocalInstance && apiKey != null && !apiKey.trim().isEmpty()) {
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        }
        connection.setDoOutput(true);
        connection.setConnectTimeout(timeoutSeconds * 1000);
        connection.setReadTimeout(timeoutSeconds * 1000);
        
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
            LOGGER.error("Player2AI API error {}: {}", responseCode, response.toString());
            throw new IOException("Player2AI API error: " + responseCode + " - " + response.toString());
        }
        
        return response.toString();
    }
    
    /**
     * Get character memory/conversation history
     */
    public CompletableFuture<String> getCharacterMemory(String characterId, String playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String npcId = characterCache.get(characterId);
                if (npcId == null) {
                    return "No previous interactions found.";
                }
                
                String response = sendRequest("memory/" + npcId + "/" + playerUUID, "GET", null);
                JsonObject responseObj = JsonParser.parseString(response).getAsJsonObject();
                
                if (responseObj.has("history")) {
                    return responseObj.get("history").getAsString();
                } else {
                    return "No previous interactions found.";
                }
                
            } catch (Exception e) {
                LOGGER.error("Failed to get character memory", e);
                return "Memory access failed.";
            }
        }, EXECUTOR);
    }
    
    /**
     * Clear character memory for a specific player (admin function)
     */
    public CompletableFuture<Boolean> clearPlayerMemory(String characterId, String playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String npcId = characterCache.get(characterId);
                if (npcId == null) {
                    return false;
                }
                
                JsonObject request = new JsonObject();
                request.addProperty("user_id", playerUUID);
                
                sendRequest("memory/" + npcId + "/clear", "POST", request.toString());
                return true;
                
            } catch (Exception e) {
                LOGGER.error("Failed to clear player memory", e);
                return false;
            }
        }, EXECUTOR);
    }
    
    /**
     * Update character personality (for deity progression changes)
     */
    public CompletableFuture<Boolean> updateCharacterPersonality(String characterId, String newPersonality) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String npcId = characterCache.get(characterId);
                if (npcId == null) {
                    return false;
                }
                
                JsonObject request = new JsonObject();
                request.addProperty("personality", newPersonality);
                
                sendRequest("characters/" + npcId + "/update", "PATCH", request.toString());
                return true;
                
            } catch (Exception e) {
                LOGGER.error("Failed to update character personality", e);
                return false;
            }
        }, EXECUTOR);
    }
}
