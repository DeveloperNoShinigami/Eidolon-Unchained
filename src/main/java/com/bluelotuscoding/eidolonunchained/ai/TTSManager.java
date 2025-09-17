package com.bluelotuscoding.eidolonunchained.ai;

import com.bluelotuscoding.eidolonunchained.integration.player2ai.Player2TTSClient;
import com.bluelotuscoding.eidolonunchained.integration.webtts.WebTTSClient;
import com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig;
import com.bluelotuscoding.eidolonunchained.network.EidolonUnchainedNetworking;
import com.bluelotuscoding.eidolonunchained.network.TTSAudioPacket;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

/**
 * Manages TTS (Text-To-Speech) for AI deity interactions
 * Supports player-funded and server-funded TTS with smart fallback
 */
public class TTSManager {
    private static final Logger LOGGER = LogManager.getLogger();

    private static TTSManager instance;
    private final Player2TTSClient player2Client;
    private final WebTTSClient webClient;

    // Cache TTS requests to avoid duplicate processing
    private final Map<String, CompletableFuture<Object>> activeRequests = new ConcurrentHashMap<>();

    // Per-player TTS settings
    private final Map<String, TTSSettings> playerSettings = new ConcurrentHashMap<>();

    public static class TTSSettings {
        public boolean enabled = false; // TTS disabled by default - user must enable
        public boolean usePlayerFunding = true; // Prefer player funding when available
        public boolean allowServerFallback = true; // Allow server funding as fallback
        public String preferredVoice = "auto"; // "auto" means use deity-appropriate voice
        public float volume = 1.0f;
        public float speed = 1.0f;
        public boolean ttsOnly = false; // If true, skip LLM and use TTS-only mode (saves money!)

        public TTSSettings() {}

        public TTSSettings(boolean enabled, boolean usePlayerFunding, boolean allowServerFallback) {
            this.enabled = enabled;
            this.usePlayerFunding = usePlayerFunding;
            this.allowServerFallback = allowServerFallback;
        }
    }

    private TTSManager() {
    this.player2Client = new Player2TTSClient();
    this.webClient = new WebTTSClient();
    }

    public static TTSManager getInstance() {
        if (instance == null) {
            instance = new TTSManager();
        }
        return instance;
    }

    /**
     * Generate TTS for deity speech and send to player
     * This is the main method called by DeityChat
     */
    public CompletableFuture<Boolean> generateAndSendTTS(ServerPlayer player, String text, String deityId) {
        // Check global TTS enable flag first
        if (!com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig.ENABLE_TTS.get()) {
            LOGGER.debug("TTS globally disabled");
            return CompletableFuture.completedFuture(false);
        }

    TTSSettings settings = getPlayerSettings(player);

        if (!settings.enabled) {
            LOGGER.debug("TTS disabled for player: {}", player.getName().getString());
            return CompletableFuture.completedFuture(false);
        }

        // Create cache key to avoid duplicate requests
        String cacheKey = player.getUUID().toString() + ":" + text.hashCode();

        // Check if we're already processing this request
        CompletableFuture<Object> activeRequest = activeRequests.get(cacheKey);
        if (activeRequest != null) {
            LOGGER.debug("TTS request already in progress for player: {}", player.getName().getString());
            return activeRequest.thenApply(response -> {
                if (response instanceof Player2TTSClient.TTSResponse p2) return p2.success;
                if (response instanceof WebTTSClient.TTSResponse web) return web.success;
                return false;
            });
        }

        // Resolve AI deity config for per-deity TTS behavior
        AIDeityConfig.TTSConfig deityTTS = null;
        try {
            AIDeityManager aiManager = AIDeityManager.getInstance();
            net.minecraft.resources.ResourceLocation deityRL =
                new net.minecraft.resources.ResourceLocation(deityId.contains(":") ? deityId : "eidolonunchained:" + deityId);
            AIDeityConfig cfg = aiManager.getAIConfig(deityRL);
            if (cfg != null) {
                deityTTS = cfg.tts_config;
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to resolve deity TTS config for {}: {}", deityId, e.getMessage());
        }

        // Honor per-deity TTS enable flag
        if (deityTTS != null && !deityTTS.enabled) {
            LOGGER.debug("TTS disabled by deity config for {}", deityId);
            return CompletableFuture.completedFuture(false);
        }

        // Determine voice to use with context awareness (deity config + fallback mapping)
        String voice;
        if (settings.preferredVoice.equals("auto")) {
            // Get current biome for context-aware voice selection
            String currentBiome = null;
            try {
                currentBiome = player.level().getBiome(player.blockPosition())
                    .unwrapKey()
                    .map(key -> key.location().toString())
                    .orElse(null);
            } catch (Exception e) {
                LOGGER.debug("Failed to get player biome for TTS: {}", e.getMessage());
            }
            LOGGER.info("🎵 TTS Voice Resolution: deityId={}, currentBiome={}", deityId, currentBiome);
            voice = Player2TTSClient.getVoiceForDeity(deityId, player, currentBiome);
            LOGGER.info("🎵 TTS Voice Resolution: getVoiceForDeity returned: {}", voice);
            // Apply alias from deity config if specified
            if (deityTTS != null && voice != null) {
                String originalVoice = voice;
                voice = deityTTS.resolveVoiceAlias(voice);
                LOGGER.info("🎵 TTS Voice Resolution: after resolveVoiceAlias: {} -> {}", originalVoice, voice);
            }
        } else {
            voice = settings.preferredVoice;
        }

        // Compute effective playback parameters, respecting allow_player_override
    float effectiveVolume = settings.volume;
    float effectiveSpeed = settings.speed;
        if (deityTTS != null && !deityTTS.allow_player_override) {
            effectiveVolume = deityTTS.volume;
            effectiveSpeed = deityTTS.speed;
        }
        // Clamp to safe ranges
    effectiveVolume = Math.max(0.0f, Math.min(2.0f, effectiveVolume));
    effectiveSpeed = Math.max(0.5f, Math.min(2.0f, effectiveSpeed));
    final float sendVolume = effectiveVolume;
    final float sendSpeed = effectiveSpeed;

        String provider = EidolonUnchainedConfig.TTS_PROVIDER.get();
        CompletableFuture<Object> future;
    if ("webapi".equalsIgnoreCase(provider)) {
            // Map to WebTTS request
            java.util.List<String> voiceIds = voice != null && !voice.equals("auto") ? java.util.Arrays.asList(voice) : java.util.Collections.emptyList();
            // Use effective speed for generation when using web API
            double speed = (double) sendSpeed;
            // Prefer deity overrides if provided, else use global defaults
            String fmt = deityTTS != null && deityTTS.audio_format != null && !deityTTS.audio_format.isEmpty()
                ? deityTTS.audio_format
                : EidolonUnchainedConfig.TTS_DEFAULT_AUDIO_FORMAT.get();
            String gender = deityTTS != null && deityTTS.voice_gender != null && !deityTTS.voice_gender.isEmpty()
                ? deityTTS.voice_gender
                : EidolonUnchainedConfig.TTS_DEFAULT_GENDER.get();
            String lang = deityTTS != null && deityTTS.voice_language != null && !deityTTS.voice_language.isEmpty()
                ? deityTTS.voice_language
                : EidolonUnchainedConfig.TTS_DEFAULT_LANGUAGE.get();
            final WebTTSClient.TTSRequest req = new WebTTSClient.TTSRequest(text, voiceIds, speed, fmt, gender, lang, player, deityId);
            future = webClient.speak(req)
                .thenApply(response -> {
                    activeRequests.remove(cacheKey);
                    if (response.success) {
                        // Send with effective playback parameters
                        sendTTSToPlayer(player, response, sendVolume, sendSpeed);
                    } else {
                        LOGGER.warn("Web TTS failed for {}: {}", player.getName().getString(), response.error);
                    }
                    return (Object) response;
                })
                .exceptionally(throwable -> {
                    activeRequests.remove(cacheKey);
                    LOGGER.error("Web TTS error for player {}: {}", player.getName().getString(), throwable.getMessage());
                    return (Object) WebTTSClient.TTSResponse.failure("TTS generation error");
                });
        } else {
            // Default to Player2 client
            // Resolve funding preference constraints from deity config
            boolean usePlayerFunding = settings.usePlayerFunding;
            boolean allowServerFallback = settings.allowServerFallback;
            if (deityTTS != null && deityTTS.funding_preference != null) {
                switch (deityTTS.funding_preference.toLowerCase()) {
                    case "player_only":
                        usePlayerFunding = true;
                        allowServerFallback = false;
                        break;
                    case "server_only":
                        usePlayerFunding = false;
                        allowServerFallback = true;
                        break;
                    case "player_first":
                    default:
                        usePlayerFunding = true;
                        allowServerFallback = true;
                        break;
                }
            }

            final Player2TTSClient.TTSRequest request = new Player2TTSClient.TTSRequest(text, voice, deityId, player, usePlayerFunding, allowServerFallback);
            future = player2Client.generateTTS(request)
                .thenApply(response -> {
                    activeRequests.remove(cacheKey);
                    if (response.success) {
                        // Send with effective playback parameters
                        sendTTSToPlayer(player, response, sendVolume, sendSpeed);
                        if (response.usedPlayerFunding) {
                            LOGGER.info("TTS generated using player funding for: {}", player.getName().getString());
                        } else {
                            LOGGER.info("TTS generated using server funding for: {}", player.getName().getString());
                        }
                    } else {
                        LOGGER.warn("TTS generation failed for player {}: {}", player.getName().getString(), response.error);
                    }
                    return (Object) response;
                })
                .exceptionally(throwable -> {
                    activeRequests.remove(cacheKey);
                    LOGGER.error("TTS generation error for player {}: {}", player.getName().getString(), throwable.getMessage());
                    return (Object) Player2TTSClient.TTSResponse.failure("TTS generation error");
                });
        }

        activeRequests.put(cacheKey, future);

        return future.thenApply(response -> {
            if (response instanceof Player2TTSClient.TTSResponse p2) return p2.success;
            if (response instanceof WebTTSClient.TTSResponse web) return web.success;
            return false;
        });
    }

    /**
     * Send TTS audio data to the player's client
     */
    private void sendTTSToPlayer(ServerPlayer player, Object responseObj, float volume, float speed) {
        try {
            LOGGER.info("sendTTSToPlayer called for player: {} with volume: {}, speed: {}",
                       player.getName().getString(), volume, speed);

            String audioUrl = null; byte[] audioData = null;
            if (responseObj instanceof Player2TTSClient.TTSResponse r1) {
                audioUrl = r1.audioUrl; audioData = r1.audioData;
                LOGGER.info("Player2TTSClient response - URL: {}, audioData length: {}",
                           audioUrl != null, audioData != null ? audioData.length : 0);
            } else if (responseObj instanceof WebTTSClient.TTSResponse r2) {
                audioUrl = r2.audioUrl; audioData = r2.audioData;
                LOGGER.info("WebTTSClient response - URL: {}, audioData length: {}",
                           audioUrl != null, audioData != null ? audioData.length : 0);
            }

            if (audioUrl != null) {
                LOGGER.info("Attempting VoiceChat spatial playback with URL for player: {}", player.getName().getString());
                // Try spatial playback first (Simple Voice Chat), fall back to packet
                try {
                    boolean played = com.bluelotuscoding.eidolonunchained.integration.voicechat.VoiceChatIntegration
                        .tryPlaySpatial(player, audioUrl, null, volume, speed);
                    if (played) {
                        LOGGER.info("SUCCESS: Played TTS audio via VoiceChat (URL) for player: {}", player.getName().getString());
                        return;
                    } else {
                        LOGGER.warn("VoiceChat spatial playback failed, falling back to packet for player: {}", player.getName().getString());
                    }
                } catch (Throwable t) {
                    LOGGER.error("Exception during VoiceChat spatial playback: {}", t.getMessage(), t);
                }
                // Send URL for client to download and play
                TTSAudioPacket packet = new TTSAudioPacket(audioUrl, null, volume, speed);
                EidolonUnchainedNetworking.sendToPlayer(player, packet);
                LOGGER.info("Sent TTS audio URL packet to player: {}", player.getName().getString());

            } else if (audioData != null) {
                LOGGER.info("Attempting VoiceChat spatial playback with audio data for player: {}", player.getName().getString());
                // Try spatial playback first (Simple Voice Chat), fall back to packet
                try {
                    boolean played = com.bluelotuscoding.eidolonunchained.integration.voicechat.VoiceChatIntegration
                        .tryPlaySpatial(player, null, audioData, volume, speed);
                    if (played) {
                        LOGGER.info("SUCCESS: Played TTS audio via VoiceChat (bytes) for player: {}", player.getName().getString());
                        return;
                    } else {
                        LOGGER.warn("VoiceChat spatial playback failed, falling back to packet for player: {}", player.getName().getString());
                    }
                } catch (Throwable t) {
                    LOGGER.error("Exception during VoiceChat spatial playback: {}", t.getMessage(), t);
                }
                // Send audio data directly
                TTSAudioPacket packet = new TTSAudioPacket(null, audioData, volume, speed);
                EidolonUnchainedNetworking.sendToPlayer(player, packet);
                LOGGER.info("Sent TTS audio data packet to player: {}", player.getName().getString());

            } else {
                LOGGER.error("TTS response has no audio URL or data for player: {}", player.getName().getString());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to send TTS audio to player {}: {}", player.getName().getString(), e.getMessage(), e);
        }
    }

    /**
     * List available TTS voices when using the Web API provider. Returns empty list otherwise.
     */
    public CompletableFuture<java.util.List<WebTTSClient.VoiceInfo>> listVoices() {
        String provider = EidolonUnchainedConfig.TTS_PROVIDER.get();
        if ("webapi".equalsIgnoreCase(provider)) {
            return webClient.listVoices();
        }
        return CompletableFuture.completedFuture(java.util.Collections.emptyList());
    }

    /**
     * Get TTS settings for a player, creating defaults if needed
     */
    public TTSSettings getPlayerSettings(ServerPlayer player) {
        return playerSettings.computeIfAbsent(player.getUUID().toString(), k -> new TTSSettings());
    }

    /**
     * Update TTS settings for a player
     */
    public void updatePlayerSettings(ServerPlayer player, TTSSettings settings) {
        playerSettings.put(player.getUUID().toString(), settings);
        LOGGER.debug("Updated TTS settings for player: {}", player.getName().getString());
    }

    /**
     * Enable/disable TTS for a player
     */
    public void setTTSEnabled(ServerPlayer player, boolean enabled) {
        TTSSettings settings = getPlayerSettings(player);
        settings.enabled = enabled;
        updatePlayerSettings(player, settings);
        LOGGER.info("TTS {} for player: {}", enabled ? "enabled" : "disabled", player.getName().getString());
    }

    /**
     * Set player funding preference
     */
    public void setPlayerFundingPreference(ServerPlayer player, boolean usePlayerFunding, boolean allowServerFallback) {
        TTSSettings settings = getPlayerSettings(player);
        settings.usePlayerFunding = usePlayerFunding;
        settings.allowServerFallback = allowServerFallback;
        updatePlayerSettings(player, settings);

        String fundingMode;
        if (usePlayerFunding && allowServerFallback) {
            fundingMode = "player-first with server fallback";
        } else if (usePlayerFunding) {
            fundingMode = "player-only";
        } else if (allowServerFallback) {
            fundingMode = "server-only";
        } else {
            fundingMode = "disabled";
        }

        LOGGER.info("Set TTS funding mode to '{}' for player: {}", fundingMode, player.getName().getString());
    }

    /**
     * Check if TTS is available for a player (either player or server funded)
     */
    public boolean isTTSAvailable(ServerPlayer player) {
        TTSSettings settings = getPlayerSettings(player);
        if (!settings.enabled) {
            return false;
        }

        // Check if any funding method is available
        boolean playerFundingAvailable = settings.usePlayerFunding && isPlayerFundingAvailable(player);
        boolean serverFundingAvailable = settings.allowServerFallback && isServerFundingAvailable();

        return playerFundingAvailable || serverFundingAvailable;
    }

    /**
     * Check if player funding is available (Player2 App running and authenticated)
     */
    private boolean isPlayerFundingAvailable(ServerPlayer player) {
        // This would check if Player2 App is running and player is authenticated
        // For now, delegate to the TTS client's logic
        return true; // The TTS client will handle the actual check
    }

    /**
     * Check if server funding is available (API key configured)
     */
    private boolean isServerFundingAvailable() {
        String apiKey = com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig.PLAYER2_API_KEY.get();
        return apiKey != null && !apiKey.isEmpty();
    }

    /**
     * Clear player settings when they log out
     */
    public void clearPlayerSettings(String playerUUID) {
        playerSettings.remove(playerUUID);

        // Cancel any active requests for this player
        activeRequests.entrySet().removeIf(entry -> entry.getKey().startsWith(playerUUID + ":"));
    }

    /**
     * Get TTS statistics for cost tracking
     */
    public static class TTSStats {
        public int playerFundedRequests = 0;
        public int serverFundedRequests = 0;
        public int failedRequests = 0;
        public long totalCharacters = 0;
    }

    private final TTSStats stats = new TTSStats();

    public TTSStats getStats() {
        return stats;
    }

    /**
     * Reset TTS statistics
     */
    public void resetStats() {
        stats.playerFundedRequests = 0;
        stats.serverFundedRequests = 0;
        stats.failedRequests = 0;
        stats.totalCharacters = 0;
    }
}