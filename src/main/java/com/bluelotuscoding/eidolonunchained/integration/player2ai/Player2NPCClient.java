package com.bluelotuscoding.eidolonunchained.integration.player2ai;

import com.bluelotuscoding.eidolonunchained.config.APIKeyManager;
import com.google.gson.*;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Player2 NPC API client for persistent deity conversations
 * Uses the proper Player2 NPC workflow: spawn -> chat -> responses
 */
public class Player2NPCClient {
    private static final Logger LOGGER = LogManager.getLogger();

    // Player2 NPC API endpoints
    private static final String PLAYER2_NPC_SPAWN_API = "https://api.player2.game/v1/npcs/spawn";
    private static final String PLAYER2_NPC_CHAT_API = "https://api.player2.game/v1/npcs/{npc_id}/chat";
    private static final String PLAYER2_NPC_RESPONSES_API = "https://api.player2.game/v1/npcs/responses";

    private static final Executor EXECUTOR = Executors.newCachedThreadPool();

    // Cache deity -> NPC ID mappings to maintain persistent conversations
    private final Map<String, String> npcIdCache = new ConcurrentHashMap<>();

    public Player2NPCClient() {
        LOGGER.info("Player2NPCClient initialized - using NPC API for persistent deity conversations");
    }

    /**
     * Generate AI response using Player2 NPC API
     */
    public CompletableFuture<String> generateResponse(String characterId, String message, String contextPrompt,
                                                     String playerUUID, String playerName, Object generationConfig) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get or create NPC for this deity
                String npcId = ensureNPCExists(characterId, contextPrompt, playerUUID);

                // Send message to NPC
                sendNPCMessage(npcId, message, playerName, playerUUID);

                // Get response from NPC responses stream
                return getNPCResponse(npcId, playerUUID);

            } catch (Exception e) {
                LOGGER.warn("Player2 NPC API error for deity {}: {}", characterId, e.getMessage());
                throw new RuntimeException("AI generation failed: " + e.getMessage(), e);
            }
        }, EXECUTOR);
    }

    /**
     * Ensure NPC exists for this deity, create if needed
     */
    private String ensureNPCExists(String deityId, String personality, String playerUUID) throws Exception {
        // Check cache first
        String existingNpcId = npcIdCache.get(deityId);
        if (existingNpcId != null) {
            LOGGER.debug("Using cached NPC ID {} for deity {}", existingNpcId, deityId);
            return existingNpcId;
        }

        // Create new NPC
        String npcId = spawnNPC(deityId, personality, playerUUID);
        npcIdCache.put(deityId, npcId);
        LOGGER.info("Created new NPC {} for deity {}", npcId, deityId);
        return npcId;
    }

    /**
     * Spawn a new NPC character for a deity
     */
    private String spawnNPC(String deityId, String personality, String playerUUID) throws Exception {
        String p2Key = getP2Key(playerUUID);

        JsonObject request = new JsonObject();
        request.addProperty("name", deityId + "_deity");
        request.addProperty("short_name", deityId);
        request.addProperty("character_description", "An ancient deity from the realm of " + deityId);
        request.addProperty("system_prompt", personality);

        // Add TTS configuration
        JsonObject tts = new JsonObject();
        tts.addProperty("speed", 1.0);
        tts.addProperty("audio_format", "wav");
        JsonArray voiceIds = new JsonArray();
        voiceIds.add("01955d76-ed5b-74de-83e5-800a44fee0d1"); // Default voice
        tts.add("voice_ids", voiceIds);
        request.add("tts", tts);

        String response = sendHttpRequest(PLAYER2_NPC_SPAWN_API, request.toString(), p2Key);
        JsonObject responseObj = JsonParser.parseString(response).getAsJsonObject();

        if (responseObj.has("npc_id")) {
            return responseObj.get("npc_id").getAsString();
        } else {
            throw new Exception("Failed to spawn NPC: " + response);
        }
    }

    /**
     * Send message to NPC
     */
    private void sendNPCMessage(String npcId, String message, String playerName, String playerUUID) throws Exception {
        String p2Key = getP2Key(playerUUID);
        String url = PLAYER2_NPC_CHAT_API.replace("{npc_id}", npcId);

        JsonObject request = new JsonObject();
        request.addProperty("sender_name", playerName);
        request.addProperty("sender_message", message);
        request.addProperty("game_state_info", "Player is in Minecraft world interacting with deity");
        request.addProperty("tts", "player"); // Request TTS in response

        sendHttpRequest(url, request.toString(), p2Key);
        LOGGER.debug("Sent message to NPC {}: {}", npcId, message);
    }

    /**
     * Get response from NPC responses stream
     */
    private String getNPCResponse(String npcId, String playerUUID) throws Exception {
        String p2Key = getP2Key(playerUUID);

        // For now, implement simple polling - in production could use streaming
        URL url = URI.create(PLAYER2_NPC_RESPONSES_API).toURL();
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "Bearer " + p2Key);
        connection.setRequestProperty("Accept", "application/json");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(30000);

        int responseCode = connection.getResponseCode();
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                responseCode >= 200 && responseCode < 300 ? connection.getInputStream() : connection.getErrorStream(),
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }

        if (responseCode < 200 || responseCode >= 300) {
            throw new Exception("NPC responses API error: " + responseCode + " - " + response.toString());
        }

        // Parse response and extract message for our NPC
        JsonObject responseObj = JsonParser.parseString(response.toString()).getAsJsonObject();
        if (responseObj.has("message")) {
            return responseObj.get("message").getAsString();
        }

        // If no direct message, look for responses array
        if (responseObj.has("responses")) {
            JsonArray responses = responseObj.getAsJsonArray("responses");
            for (JsonElement element : responses) {
                JsonObject resp = element.getAsJsonObject();
                if (resp.has("npc_id") && resp.get("npc_id").getAsString().equals(npcId)) {
                    if (resp.has("message")) {
                        return resp.get("message").getAsString();
                    }
                }
            }
        }

        throw new Exception("No response found for NPC " + npcId);
    }

    /**
     * Get Player2 authentication key
     */
    private String getP2Key(String playerUUID) throws Exception {
        // Try player-specific key first
        java.util.UUID uuid = java.util.UUID.fromString(playerUUID);
        String p2Key = Player2AuthManager.getCachedP2Key(uuid);

        if (p2Key == null || p2Key.isEmpty()) {
            // Try server-level key
            try {
                p2Key = APIKeyManager.getAPIKey("player2ai");
            } catch (Throwable ignored) {}
        }

        if (p2Key == null || p2Key.isEmpty()) {
            throw new Exception("No Player2 authentication available - use /tts login device or configure server key");
        }

        return p2Key;
    }

    /**
     * Send HTTP request to Player2 API
     */
    private String sendHttpRequest(String urlString, String jsonBody, String p2Key) throws Exception {
        URL url = URI.create(urlString).toURL();
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + p2Key);
        connection.setDoOutput(true);
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(15000);

        try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8)) {
            writer.write(jsonBody);
            writer.flush();
        }

        int responseCode = connection.getResponseCode();
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                responseCode >= 200 && responseCode < 300 ? connection.getInputStream() : connection.getErrorStream(),
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }

        if (responseCode < 200 || responseCode >= 300) {
            LOGGER.warn("Player2 API error {}: {}", responseCode, response.toString());
            throw new Exception("Player2 API error: " + responseCode + " - " + response.toString());
        }

        return response.toString();
    }

    /**
     * Check if Player2 API is available
     */
    public boolean isAvailable() {
        try {
            // Quick test with any available key
            String testKey = null;
            try {
                testKey = APIKeyManager.getAPIKey("player2ai");
            } catch (Throwable ignored) {}

            return testKey != null && !testKey.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Clear NPC cache for a deity (forces recreation)
     */
    public void clearNPCCache(String deityId) {
        String removedNpcId = npcIdCache.remove(deityId);
        if (removedNpcId != null) {
            LOGGER.info("Cleared NPC cache for deity {} (NPC ID: {})", deityId, removedNpcId);
        }
    }

    /**
     * Clear all NPC cache
     */
    public void clearAllNPCCache() {
        npcIdCache.clear();
        LOGGER.info("Cleared all NPC cache");
    }
}