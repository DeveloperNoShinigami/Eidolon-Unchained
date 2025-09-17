package com.bluelotuscoding.eidolonunchained.integration.player2ai;

import com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig;
import com.google.gson.*;
import net.minecraft.server.level.ServerPlayer;
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
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Player2 TTS API client with player-funded and server fallback support
 * Uses Player2.game TTS API with smart cost distribution
 */
public class Player2TTSClient {
    private static final Logger LOGGER = LogManager.getLogger();

    // Player2 Web API endpoints (OpenAPI: https://api.player2.game/v1)
    // TTS speak endpoint returns TTSSpeakResponse { data: base64 } or may include url
    private static final String PLAYER2_TTS_SPEAK = "https://api.player2.game/v1/tts/speak";
    private static final String PLAYER2_AUTH_API = "https://api.player2.game/v1/auth";

    // Local Player2 App endpoints for client-side requests
    private static final String PLAYER2_LOCAL_TTS = "http://127.0.0.1:4315/v1/tts";
    private static final String PLAYER2_LOCAL_AUTH = "http://127.0.0.1:4315/v1/login/web/0198fed4-2d7d-7acf-aaf0-2bdb36a74eba";

    // Eidolon Unchained verified game client ID
    private static final String GAME_CLIENT_ID = "0198fed4-2d7d-7acf-aaf0-2bdb36a74eba";

    private static final transient Executor EXECUTOR = Executors.newCachedThreadPool();

    /**
     * TTS Request configuration
     */
    public static class TTSRequest {
        public final String text;
        public final String voice;
        public final String deityId;
        public final ServerPlayer player;
        public final boolean usePlayerFunding;
        public final boolean allowServerFallback;

        public TTSRequest(String text, String voice, String deityId, ServerPlayer player) {
            this(text, voice, deityId, player, true, true);
        }

        public TTSRequest(String text, String voice, String deityId, ServerPlayer player,
                           boolean usePlayerFunding, boolean allowServerFallback) {
            this.text = text;
            this.voice = voice;
            this.deityId = deityId;
            this.player = player;
            this.usePlayerFunding = usePlayerFunding;
            this.allowServerFallback = allowServerFallback;
        }
    }

    /**
     * TTS Response with audio data or URL
     */
    public static class TTSResponse {
        public final boolean success;
        public final String audioUrl;
        public final byte[] audioData;
        public final String error;
        public final boolean usedPlayerFunding;

        public TTSResponse(boolean success, String audioUrl, byte[] audioData, String error, boolean usedPlayerFunding) {
            this.success = success;
            this.audioUrl = audioUrl;
            this.audioData = audioData;
            this.error = error;
            this.usedPlayerFunding = usedPlayerFunding;
        }

        public static TTSResponse success(String audioUrl, byte[] audioData, boolean usedPlayerFunding) {
            return new TTSResponse(true, audioUrl, audioData, null, usedPlayerFunding);
        }

        public static TTSResponse failure(String error) {
            return new TTSResponse(false, null, null, error, false);
        }
    }

    /**
     * Generate TTS audio with smart funding strategy:
     * 1. Try player-funded (if player has player2ai configured)
     * 2. Fall back to server-funded (if server has API key)
     * 3. Fail gracefully if neither available
     */
    public CompletableFuture<TTSResponse> generateTTS(TTSRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Determine funding strategy based on request flags
                boolean playerAllowed = request.usePlayerFunding && isPlayerFundingAvailable(request.player);
                boolean serverAllowed = isServerFundingAvailable();

                // Case A: Player-first (player allowed)
                if (playerAllowed) {
                    LOGGER.debug("Attempting player-funded TTS for player: {}", request.player.getName().getString());
                    TTSResponse playerResponse = tryPlayerFundedTTS(request);
                    if (playerResponse.success) {
                        LOGGER.info("Successfully used player-funded TTS for {}", request.player.getName().getString());
                        return playerResponse;
                    }
                    LOGGER.debug("Player-funded TTS failed: {}", playerResponse.error);

                    // Fallback to server if allowed
                    if (request.allowServerFallback && serverAllowed) {
                        LOGGER.debug("Falling back to server-funded TTS for player: {}", request.player.getName().getString());
                        TTSResponse serverResponse = tryServerFundedTTS(request);
                        if (serverResponse.success) {
                            LOGGER.info("Successfully used server-funded TTS for {}", request.player.getName().getString());
                            return serverResponse;
                        }
                        LOGGER.warn("Server-funded TTS fallback failed: {}", serverResponse.error);
                    }
                } else {
                    // Case B: Server-first or player not allowed/available
                    if (serverAllowed) {
                        LOGGER.debug("Attempting server-funded TTS for player: {}", request.player.getName().getString());
                        TTSResponse serverResponse = tryServerFundedTTS(request);
                        if (serverResponse.success) {
                            LOGGER.info("Successfully used server-funded TTS for {}", request.player.getName().getString());
                            return serverResponse;
                        }
                        LOGGER.warn("Server-funded TTS failed: {}", serverResponse.error);
                    }
                }

                // No TTS available given constraints
                LOGGER.warn("No TTS funding available or allowed for player: {}", request.player.getName().getString());
                return TTSResponse.failure("TTS not available - funding not available or not allowed");

            } catch (Exception e) {
                LOGGER.error("TTS generation failed for player {}: {}", request.player.getName().getString(), e.getMessage());
                return TTSResponse.failure("TTS generation error: " + e.getMessage());
            }
        }, EXECUTOR);
    }

    /**
     * Check if player has Player2 App running and can get authentication token
     */
    private boolean isPlayerFundingAvailable(ServerPlayer player) {
        try {
            // Check if Player2 App is running locally
            if (!Player2AIClient.isPlayer2AppAvailable()) {
                return false;
            }

            // Try to get p2Key from Player2 App using verified game client ID
            URL authUrl = URI.create(PLAYER2_LOCAL_AUTH).toURL();
            HttpURLConnection conn = (HttpURLConnection) authUrl.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                // Parse response to get p2Key
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String response = reader.lines().reduce("", (a, b) -> a + b);
                    JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                    return json.has("p2Key") && !json.get("p2Key").isJsonNull();
                }
            }
            return false;
        } catch (Exception e) {
            LOGGER.debug("Failed to check player funding availability: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if server funding is available (uses player p2Key from Player2 App)
     */
    private boolean isServerFundingAvailable() {
        // Server funding now uses player's p2Key obtained from Player2 App
        // No separate server API key needed for verified games
        return true;
    }

    /**
     * Try player-funded TTS via local Player2 App
     */
    private TTSResponse tryPlayerFundedTTS(TTSRequest request) {
        try {
            JsonObject ttsRequest = new JsonObject();
            ttsRequest.addProperty("text", request.text);
            ttsRequest.addProperty("voice", request.voice);
            // Prefer configured defaults when available
            String localFormat = EidolonUnchainedConfig.TTS_DEFAULT_AUDIO_FORMAT.get();
            if (localFormat == null || localFormat.isEmpty()) localFormat = "mp3";
            ttsRequest.addProperty("format", localFormat);
            // Provide speed if supported by local app (ignored if unsupported)
            try {
                double spd = EidolonUnchainedConfig.TTS_DEFAULT_SPEED.get();
                ttsRequest.addProperty("speed", spd);
            } catch (Exception ignored) {}

            // Add deity context for better voice selection
            JsonObject metadata = new JsonObject();
            metadata.addProperty("game", "minecraft");
            metadata.addProperty("mod", "eidolon-unchained");
            metadata.addProperty("character", request.deityId);
            ttsRequest.add("metadata", metadata);

            URL url = URI.create(PLAYER2_LOCAL_TTS).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("X-Game-Client-ID", GAME_CLIENT_ID);
            conn.setRequestProperty("X-Player-UUID", request.player.getUUID().toString());
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);

            try (OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream())) {
                writer.write(ttsRequest.toString());
                writer.flush();
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                // Read audio data or URL response
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String response = reader.lines().reduce("", (a, b) -> a + b);
                    JsonObject json = JsonParser.parseString(response).getAsJsonObject();

                    if (json.has("audio_url")) {
                        return TTSResponse.success(json.get("audio_url").getAsString(), null, true);
                    } else if (json.has("audio_data")) {
                        String base64Audio = json.get("audio_data").getAsString();
                        byte[] audioData = Base64.getDecoder().decode(base64Audio);
                        return TTSResponse.success(null, audioData, true);
                    }
                }
            } else {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                    String error = reader.lines().reduce("", (a, b) -> a + b);
                    return TTSResponse.failure("Player TTS failed: " + error);
                }
            }
        } catch (Exception e) {
            return TTSResponse.failure("Player TTS error: " + e.getMessage());
        }

        return TTSResponse.failure("Player TTS: unknown error");
    }

    /**
     * Try server-funded TTS via Player2.game web API (uses player p2Key)
     */
    private TTSResponse tryServerFundedTTS(TTSRequest request) {
        try {
            // Get p2Key from Player2 App for web API authentication
            String p2Key = getPlayerP2Key(request.player);
            if (p2Key == null || p2Key.isEmpty()) {
                return TTSResponse.failure("Server TTS requires Player2 App authentication");
            }
            // Build TTS request with voice selection
            JsonObject ttsRequest = new JsonObject();
            ttsRequest.addProperty("text", request.text);
            
            // Get TTS configuration from AI deity config if available
            com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig.TTSConfig ttsConfig = getTTSConfigForDeity(request.deityId);
            
            // Apply enhancement parameters from deity config or use defaults
            if (ttsConfig != null) {
                ttsRequest.addProperty("speed", ttsConfig.speed);
                ttsRequest.addProperty("audio_format", ttsConfig.audio_format != null ? ttsConfig.audio_format : "wav");
                
                // TODO: Verify Player2 API supports these enhancement parameters
                // Currently setting as metadata - may need API documentation review
                JsonObject metadata = new JsonObject();
                metadata.addProperty("pitch", ttsConfig.pitch);
                metadata.addProperty("volume", ttsConfig.volume);
                metadata.addProperty("emotion", ttsConfig.emotion);
                metadata.addProperty("accent", ttsConfig.accent);
                metadata.addProperty("emphasis_level", ttsConfig.emphasis_level);
                
                // Add advanced parameters if present
                if (!ttsConfig.advanced_params.isEmpty()) {
                    JsonObject advancedParams = new JsonObject();
                    ttsConfig.advanced_params.forEach((key, value) -> {
                        if (value instanceof String) {
                            advancedParams.addProperty(key, (String) value);
                        } else if (value instanceof Number) {
                            advancedParams.addProperty(key, (Number) value);
                        } else if (value instanceof Boolean) {
                            advancedParams.addProperty(key, (Boolean) value);
                        }
                    });
                    metadata.add("advanced_params", advancedParams);
                }
                
                // TODO: Custom WAV file support
                // Check if Player2 API supports custom voice files (ttsConfig.custom_voice_file)
                // This would allow modpack creators to use custom deity voices
                if (ttsConfig.custom_voice_file != null && !ttsConfig.custom_voice_file.isEmpty()) {
                    // TODO: Implement custom voice file upload/reference to Player2 API
                    // This feature needs investigation into Player2 API capabilities
                    LOGGER.info("Custom voice file specified for deity {}: {} (TODO: implement)", request.deityId, ttsConfig.custom_voice_file);
                }
                
                ttsRequest.add("metadata", metadata);
            } else {
                ttsRequest.addProperty("speed", 1.0);
                ttsRequest.addProperty("audio_format", "wav");  // Use WAV for better compatibility with VoiceChat and Java Sound API
            }

            // Add voice selection - respect AI deity config settings
            String voiceToUse = request.voice;
            LOGGER.info("TTS Voice Selection - Initial request voice: '{}', deity: '{}'", request.voice, request.deityId);

            if (request.voice == null || request.voice.isEmpty() || request.voice.equals("auto")) {
                // For "auto" mode, use deity-specific voice from config
                if (request.deityId != null && !request.deityId.isEmpty() && request.player != null) {
                    String deityVoice = getVoiceForDeity(request.deityId, request.player, "");
                    LOGGER.info("TTS Voice Selection - Deity config returned voice: '{}'", deityVoice);
                    if (deityVoice != null && !deityVoice.equals("auto")) {
                        voiceToUse = deityVoice;
                        LOGGER.info("TTS Voice Selection - Using deity-specific voice from config: '{}'", deityVoice);
                    } else {
                        LOGGER.info("TTS Voice Selection - Deity config returned auto/null, will let Player2.game choose");
                    }
                } else {
                    LOGGER.info("TTS Voice Selection - No deity context or player, will let Player2.game choose");
                }
                // If still "auto" or null, let Player2.game choose (no voice_id parameter)
            }

            // Only set voice_id if we have a specific voice (not "auto")
            if (voiceToUse != null && !voiceToUse.isEmpty() && !voiceToUse.equals("auto")) {
                // Support both voice names and IDs
                String resolvedVoiceId = resolveVoiceNameToId(voiceToUse);
                LOGGER.info("TTS Voice Selection - Resolving '{}' to voice ID: '{}'", voiceToUse, resolvedVoiceId);
                if (resolvedVoiceId != null) {
                    ttsRequest.addProperty("voice_id", resolvedVoiceId);
                    LOGGER.info("TTS Voice Selection - FINAL: Using voice '{}' with ID: '{}'", voiceToUse, resolvedVoiceId);
                } else {
                    LOGGER.warn("TTS Voice Selection - Failed to resolve voice '{}', letting Player2.game choose default", voiceToUse);
                }
            } else {
                LOGGER.info("TTS Voice Selection - FINAL: No specific voice selected, letting Player2.game choose");
            }

            URL url = URI.create(PLAYER2_TTS_SPEAK).toURL();
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + p2Key);
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);

            // Log the request for debugging
            LOGGER.info("Player2 TTS API Request: {}", ttsRequest.toString());
            LOGGER.info("Request URL: {}", url);
            LOGGER.info("Request headers: Authorization=Bearer [REDACTED], Content-Type=application/json");

            try (OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream())) {
                writer.write(ttsRequest.toString());
                writer.flush();
            }

            int responseCode = conn.getResponseCode();
            LOGGER.info("Player2 TTS API Response Code: {}", responseCode);
            if (responseCode == 200) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String response = reader.lines().reduce("", (a, b) -> a + b);
                    JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                    // Spec: TTSSpeakResponse { data: base64 } or data URL format
                    if (json.has("data") && !json.get("data").isJsonNull()) {
                        String audioData = json.get("data").getAsString();

                        // Handle data URL format: data:audio/mp3;base64,actual_base64_data
                        if (audioData.startsWith("data:")) {
                            int commaIndex = audioData.indexOf(',');
                            if (commaIndex != -1) {
                                audioData = audioData.substring(commaIndex + 1);
                            }
                        }

                        byte[] decodedAudio = Base64.getDecoder().decode(audioData);
                        return TTSResponse.success(null, decodedAudio, false);
                    }
                    // Fallbacks observed on some implementations
                    if (json.has("url")) {
                        return TTSResponse.success(json.get("url").getAsString(), null, false);
                    }
                    if (json.has("audio_url")) {
                        return TTSResponse.success(json.get("audio_url").getAsString(), null, false);
                    }
                    if (json.has("audio_data")) {
                        String base64Audio = json.get("audio_data").getAsString();
                        byte[] audioData = Base64.getDecoder().decode(base64Audio);
                        return TTSResponse.success(null, audioData, false);
                    }
                }
            } else if (responseCode == 401) {
                return TTSResponse.failure("Unauthorized (401) - check Player2 API key");
            } else if (responseCode == 402) {
                return TTSResponse.failure("Insufficient credits (402)");
            } else if (responseCode == 400) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                    String error = reader.lines().reduce("", (a, b) -> a + b);
                    return TTSResponse.failure("Invalid request (400): " + error);
                }
            } else {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                    String error = reader.lines().reduce("", (a, b) -> a + b);
                    return TTSResponse.failure("Server TTS failed (" + responseCode + "): " + error);
                }
            }
        } catch (Exception e) {
            return TTSResponse.failure("Server TTS error: " + e.getMessage());
        }

        return TTSResponse.failure("Server TTS: unknown error");
    }

    /**
     * Get player's p2Key from Player2 App for web API authentication
     */
    private String getPlayerP2Key(ServerPlayer player) {
        try {
            // Call Player2 App login endpoint with verified game client ID
            URL authUrl = URI.create(PLAYER2_LOCAL_AUTH).toURL();
            HttpURLConnection conn = (HttpURLConnection) authUrl.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                // Parse response to get p2Key
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String response = reader.lines().reduce("", (a, b) -> a + b);
                    JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                    if (json.has("p2Key") && !json.get("p2Key").isJsonNull()) {
                        return json.get("p2Key").getAsString();
                    }
                }
            } else {
                LOGGER.debug("Failed to get p2Key from Player2 App: HTTP {}", responseCode);
            }
            return null;
        } catch (Exception e) {
            LOGGER.debug("Failed to get player p2Key: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get TTS configuration from AI deity config
     */
    private com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig.TTSConfig getTTSConfigForDeity(String deityId) {
        try {
            com.bluelotuscoding.eidolonunchained.ai.AIDeityManager aiManager =
                com.bluelotuscoding.eidolonunchained.ai.AIDeityManager.getInstance();

            net.minecraft.resources.ResourceLocation deityResource =
                new net.minecraft.resources.ResourceLocation(deityId.contains(":") ? deityId : "eidolonunchained:" + deityId);

            com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig aiConfig = aiManager.getAIConfig(deityResource);
            return aiConfig != null ? aiConfig.tts_config : null;
        } catch (Exception e) {
            LOGGER.debug("Failed to get TTS config for deity {}: {}", deityId, e.getMessage());
            return null;
        }
    }

    /**
     * Get appropriate voice for a deity using AI config or fallback to defaults
     */
    public static String getVoiceForDeity(String deityId, ServerPlayer player, String currentBiome) {
        org.apache.logging.log4j.Logger logger = org.apache.logging.log4j.LogManager.getLogger();
        logger.info("🎵 getVoiceForDeity called: deityId={}, currentBiome={}", deityId, currentBiome);
        
        try {
            // Try to get voice from AI deity configuration
            com.bluelotuscoding.eidolonunchained.ai.AIDeityManager aiManager =
                com.bluelotuscoding.eidolonunchained.ai.AIDeityManager.getInstance();

            net.minecraft.resources.ResourceLocation deityResource =
                new net.minecraft.resources.ResourceLocation(deityId.contains(":") ? deityId : "eidolonunchained:" + deityId);
            
            logger.info("🎵 deityResource={}", deityResource);

            com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig aiConfig = aiManager.getAIConfig(deityResource);
            
            logger.info("🎵 aiConfig={}", aiConfig != null ? "found" : "null");

            if (aiConfig != null && aiConfig.tts_config != null) {
                logger.info("🎵 tts_config found, voice_id={}", aiConfig.tts_config.voice_id);
                
                // Get player's reputation with this deity
                int reputation = 0;
                if (player != null) {
                    try {
                        final int[] repValue = {0}; // Array workaround for final variable access
                        player.getCapability(elucent.eidolon.capability.IReputation.INSTANCE).ifPresent(rep -> {
                            repValue[0] = (int) rep.getReputation(player.getUUID(), deityResource);
                        });
                        reputation = repValue[0];
                    } catch (Exception e) {
                        // Ignore reputation errors, use default
                        logger.debug("🎵 Failed to get reputation: {}", e.getMessage());
                    }
                }
                
                logger.info("🎵 reputation={}", reputation);

                // Get context-aware voice from deity config
                String configuredVoice = aiConfig.tts_config.getVoiceForContext(player, currentBiome, reputation);
                logger.info("🎵 getVoiceForContext returned: {}", configuredVoice);
                
                if (configuredVoice != null && !configuredVoice.equals("auto")) {
                    // Apply voice aliases
                    String finalVoice = aiConfig.tts_config.resolveVoiceAlias(configuredVoice);
                    logger.info("🎵 final voice after aliases: {}", finalVoice);
                    return finalVoice;
                }
            }
        } catch (Exception e) {
            // No fallback - if AI deity config fails, let Player2 choose default
            logger.warn("🎵 Failed to get voice from deity config: {}", e.getMessage());
        }

        // No fallback mapping - voices come exclusively from AI deity configs
        logger.info("🎵 returning null (let Player2 choose default)");
        return null; // Let Player2 choose default if no config available
    }

    /**
     * Resolve voice name to Player2 voice ID, supporting both names and direct IDs
     */
    private String resolveVoiceNameToId(String voiceNameOrId) {
        // If it's already a UUID format (Player2 voice ID), return as-is
        if (voiceNameOrId.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
            return voiceNameOrId;
        }

        // Map Player2 voice names to their UUIDs (from /v1/tts/voices endpoint)
        switch (voiceNameOrId.toLowerCase()) {
            // American English Female Voices
            case "sophia": return "01955d76-ed5b-73e0-a88d-cbeb3c5b499d";
            case "madison": return "01955d76-ed5b-7407-a03c-cdd993439ba4";
            case "harper": return "01955d76-ed5b-7416-82d8-5fc486e2f676";
            case "olivia": return "01955d76-ed5b-7426-8748-4b0e5aea1974";
            case "ava": return "01955d76-ed5b-7436-a182-c4d21aaca9fc";
            case "amelia": return "01955d76-ed5b-7441-a184-5f5ee015e4fe";
            case "charlotte": return "01955d76-ed5b-7451-92d6-5ef579d3ed28";
            case "evelyn": return "01955d76-ed5b-745d-add1-b755d440192d";
            case "abigail": return "01955d76-ed5b-7468-83a7-bfc267cf4849";
            case "mia": return "01955d76-ed5b-7474-86b2-a41b310c2a2d";
            case "chloe": return "01955d76-ed5b-7480-951c-af1dd9873e34";

            // American English Male Voices
            case "ethan": return "01955d76-ed5b-748c-8d98-0fb708ef0fbd";
            case "noah": return "01955d76-ed5b-7497-9f8e-0e7448515bf3";
            case "mason": return "01955d76-ed5b-74a3-9129-c3253d01f690";
            case "logan": return "01955d76-ed5b-74af-a2be-9302077075b8";
            case "benjamin": return "01955d76-ed5b-74ba-89e5-2b4b45e632cd";
            case "lucas": return "01955d76-ed5b-74c6-ac15-ab68ee19d560";
            case "jackson": return "01955d76-ed5b-74d2-a33c-b2b8e998658f";
            case "caleb": return "01955d76-ed5b-74de-83e5-800a44fee0d1";
            case "nicholas": return "01955d76-ed5b-74e9-9fea-1f8cad1cd9c5";

            // DEBUG: Temporary hardcoded mapping for shadow_lord to test
            case "shadow_lord": return "01955d76-ed5b-748c-8d98-0fb708ef0fbd"; // Ethan's voice ID

            // British English Voices
            case "eleanor": return "01955d76-ed5b-74f9-b54a-2d051890468d";
            case "poppy": return "01955d76-ed5b-751c-b341-0ee85dbefd92";
            case "florence": return "01955d76-ed5b-7528-86ee-3348a642af7e";
            case "amelia_british": return "01955d76-ed5b-7534-b7a6-028adcfb4e7d";
            case "oliver": return "01955d76-ed5b-753f-9f74-c0674216f0f5";
            case "harry": return "01955d76-ed5b-754f-a070-a570ddfed516";
            case "william": return "01955d76-ed5b-755b-9b43-890d73586908";
            case "charles": return "01955d76-ed5b-7566-9c0e-bce4d88ceba0";

            // Japanese Voices
            case "sakura": return "01955d76-ed5b-757a-9bdb-94fa0a2b7893";
            case "akari": return "01955d76-ed5b-7591-9d3f-f919ac645bb6";
            case "yuki": return "01955d76-ed5b-75a1-96f8-7a82e767e2c4";
            case "hana": return "01955d76-ed5b-75ad-afe3-ac5eb3d0a16e";
            case "takashi": return "01955d76-ed5b-75b8-b70f-dfaf400b7c42";

            // Mandarin Chinese Voices
            case "mei": return "01955d76-ed5b-75c8-8386-b83ff9c45856";
            case "ling": return "01955d76-ed5b-75d4-8338-3d7108137cd1";
            case "jingyi": return "01955d76-ed5b-75df-8ca5-a6f84acaff76";
            case "qiuyue": return "01955d76-ed5b-75eb-b509-e7bf29b3b530";
            case "wei": return "01955d76-ed5b-75fb-87dd-ebbed25d2585";
            case "liang": return "01955d76-ed5b-7606-9e21-8b236fbe12a8";
            case "ming": return "01955d76-ed5b-7612-bf44-f7bdcc808356";
            case "hao": return "01955d76-ed5b-761e-abac-1956f66ac089";

            // Spanish Voices
            case "carmen": return "01955d76-ed5b-762a-9a2a-0fec3b7ace8b";
            case "miguel": return "01955d76-ed5b-7649-ac1e-c56a13c3302f";
            case "javier": return "01955d76-ed5b-7655-98bb-fd7578af9617";

            // French Voices
            case "sophie": return "01955d76-ed5b-7668-877b-2fa240c1d5ee";

            // Hindi Voices
            case "priya": return "01955d76-ed5b-7678-b678-3ddc5ec8b5c4";
            case "aditi": return "01955d76-ed5b-7683-a79d-253390189fdb";
            case "arjun": return "01955d76-ed5b-768f-9e5b-8bcc89ba8f3d";
            case "vikram": return "01955d76-ed5b-769b-bd00-002a8e88dc65";

            // Italian Voices
            case "bianca": return "01955d76-ed5b-76ab-bc6b-57cc5dfeaf01";
            case "marco": return "01955d76-ed5b-76ba-898e-c65bd579a334";

            // Brazilian Portuguese Voices
            case "isabela": return "01955d76-ed5b-76c6-8b9e-b713d3f0b866";
            case "gabriel": return "01955d76-ed5b-76d2-8f05-b9a34b5f9011";
            case "rafael": return "01955d76-ed5b-76dd-bef6-37119ea2f99f";

            default:
                LOGGER.debug("Unknown voice name '{}', using default", voiceNameOrId);
                return null; // Let Player2 choose default
        }
    }
}