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
    private static final String PLAYER2_LOCAL_API_BASE = "http://127.0.0.1:4315/v1/npc/";
    private static final String PLAYER2_AUTH_BASE = "http://localhost:4316/v1/login/web/";
    private static final String GAME_CLIENT_ID = "eidolon-unchained"; // Player2AI game client ID
    private static final Executor EXECUTOR = Executors.newCachedThreadPool();
    
    private final String apiKey;
    private final int timeoutSeconds;
    private final boolean useLocalInstance;
    private final Map<String, String> characterCache = new HashMap<>();
    
    public Player2AIClient(String apiKey, int timeoutSeconds) {
        this.apiKey = apiKey;
        this.timeoutSeconds = timeoutSeconds;
        // Determine if using local instance (no API key or localhost URL)
        this.useLocalInstance = apiKey == null || apiKey.trim().isEmpty() || 
            apiKey.contains("localhost") || apiKey.contains("127.0.0.1");
        
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
            LOGGER.debug("Player2 App not available for quick authentication: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Generate AI response for deity interaction using Player2AI character persistence
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
                // Ensure character exists
                String npcId = ensureCharacterExists(characterId, personality);
                
                // Send message to character
                String response = sendMessageToCharacter(npcId, prompt, playerUUID);
                
                return new GeminiAPIClient.AIResponse(true, response, Collections.emptyList());
                
            } catch (Exception e) {
                LOGGER.error("Player2AI request failed", e);
                return new GeminiAPIClient.AIResponse(false, 
                    "The deity's voice echoes from beyond the veil, but their words are lost in the void...", 
                    Collections.emptyList());
            }
        }, EXECUTOR);
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
     * Send HTTP request to Player2AI API
     */
    private String sendRequest(String endpoint, String method, String jsonBody) throws IOException {
        String baseUrl = useLocalInstance ? PLAYER2_LOCAL_API_BASE : PLAYER2_CLOUD_API_BASE;
        String urlString = baseUrl + endpoint;
        URL url = URI.create(urlString).toURL();
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        
        // Configure connection
        connection.setRequestMethod(method);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("player2-game-key", GAME_CLIENT_ID);
        
        // Set authentication based on instance type
        if (!useLocalInstance && apiKey != null && !apiKey.trim().isEmpty()) {
            connection.setRequestProperty("X-API-Key", apiKey);
        }
        connection.setDoOutput(true);
        connection.setConnectTimeout(timeoutSeconds * 1000);
        connection.setReadTimeout(timeoutSeconds * 1000);
        
        // Send request body
        if (jsonBody != null && !jsonBody.isEmpty()) {
            try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream())) {
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
                    : connection.getErrorStream()))) {
            
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
