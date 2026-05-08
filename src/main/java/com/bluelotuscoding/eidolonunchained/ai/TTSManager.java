package com.bluelotuscoding.eidolonunchained.ai;

import com.bluelotuscoding.eidolonunchained.integration.player2ai.Player2TTSClient;
import com.bluelotuscoding.eidolonunchained.integration.webtts.WebTTSClient;
import com.bluelotuscoding.eidolonunchained.integration.gemini.GeminiTTSClient;
import com.bluelotuscoding.eidolonunchained.integration.tts.TTSVoiceRegistry;
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
    private final GeminiTTSClient geminiClient;

    // Cache TTS requests to avoid duplicate processing
    private final Map<String, CompletableFuture<Object>> activeRequests = new ConcurrentHashMap<>();

    // Per-player TTS settings
    private final Map<String, TTSSettings> playerSettings = new ConcurrentHashMap<>();
    // Track last used path per player (e.g., player2-local, player2-web, server-funded, webapi)
    private final Map<String, String> lastUsedPath = new ConcurrentHashMap<>();

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
        this.geminiClient = new GeminiTTSClient();
        // Preload global TTS voice aliases once at startup using the game directory
        try {
            java.io.File gameDir = net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get().toFile();
            TTSVoiceRegistry.getInstance().loadIfNeeded(gameDir);
            LOGGER.info("Loaded TTS voice aliases at startup");
        } catch (Throwable t) {
            LOGGER.debug("Could not preload TTS voice aliases: {}", t.getMessage());
        }
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
        // Per-player opt-in is the sole gate. The global TOML flag (tts.enabled) is intentionally
        // NOT checked here — /eu tts enable sets the per-player flag, not the TOML.
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
        String aiProvider = null;
        try {
            AIDeityManager aiManager = AIDeityManager.getInstance();
            net.minecraft.resources.ResourceLocation deityRL =
                new net.minecraft.resources.ResourceLocation(deityId.contains(":") ? deityId : "eidolonunchained:" + deityId);
            AIDeityConfig cfg = aiManager.getAIConfig(deityRL);
            if (cfg != null) {
                deityTTS = cfg.tts_config;
                aiProvider = cfg.ai_provider; // Also get the AI provider for TTS inheritance

                // Additional validation logging
                LOGGER.info("🎵 TTS Config resolution for {}: deityTTS={}, aiProvider={}",
                    deityId, deityTTS != null ? "present" : "null", aiProvider);

                if (deityTTS != null) {
                    LOGGER.info("🎵 TTS Config details: provider={}, model={}, enabled={}",
                        deityTTS.tts_provider, deityTTS.model, deityTTS.enabled);
                }
            } else {
                LOGGER.warn("🎵 No AI deity config found for {}, TTS will use defaults", deityId);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to resolve deity TTS config for {}: {}", deityId, e.getMessage());
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
            // Apply alias resolution for explicit player preference as well
            if (voice != null && !"auto".equals(voice)) {
                String originalVoice = voice;
                if (deityTTS != null) {
                    voice = deityTTS.resolveVoiceAlias(voice);
                } else {
                    try {
                        voice = TTSVoiceRegistry.getInstance().resolve(voice);
                    } catch (Exception ignored) {}
                }
                LOGGER.info("🎵 TTS Voice Resolution (player preference): {} -> {}", originalVoice, voice);
            }
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

        // Determine provider from deity config with AI provider inheritance. TOML is intentionally ignored.
        String provider;
        if (deityTTS != null && deityTTS.tts_provider != null && !deityTTS.tts_provider.trim().isEmpty()) {
            // Explicit TTS provider specified
            provider = deityTTS.tts_provider.trim();
            LOGGER.info("🎵 TTS provider (explicit tts_config) for {}: {}", deityId, provider);
        } else if (aiProvider != null && !aiProvider.trim().isEmpty()) {
            // Inherit TTS provider from AI provider
            provider = aiProvider.trim();
            LOGGER.info("🎵 TTS provider (inherited from ai_provider) for {}: {}", deityId, provider);
        } else {
            // Safe default when deity doesn't specify anything
            provider = "gemini";
            LOGGER.warn("🎵 No tts_provider or ai_provider in deity config for {} — defaulting to 'gemini'", deityId);
        }
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
                        lastUsedPath.put(player.getUUID().toString(), "webapi");
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
        } else if ("google".equalsIgnoreCase(provider) || "gemini".equalsIgnoreCase(provider)) {
            // Use Gemini TTS
            String geminiModel = (deityTTS != null && deityTTS.model != null && !deityTTS.model.trim().isEmpty())
                ? deityTTS.model.trim()
                : "gemini-2.5-flash-preview-tts";

            LOGGER.info("🎵 Using Gemini TTS model: {} for deity: {}", geminiModel, deityId);
            
            // Get player context for voice selection
            final Integer[] playerReputationRef = {null};
            final String[] playerBiomeRef = {null};
            
            try {
                // Get player reputation for context-aware voice selection
                player.level().getCapability(elucent.eidolon.capability.IReputation.INSTANCE).ifPresent(reputation -> {
                    playerReputationRef[0] = (int) reputation.getReputation(player.getUUID(), 
                        new net.minecraft.resources.ResourceLocation(deityId.contains(":") ? deityId : "eidolonunchained:" + deityId));
                });
                
                // Get current biome for context-aware voice selection
                playerBiomeRef[0] = player.level().getBiome(player.blockPosition())
                    .unwrapKey()
                    .map(key -> key.location().toString())
                    .orElse(null);
            } catch (Exception e) {
                LOGGER.debug("Could not get player context for TTS: {}", e.getMessage());
            }
            
            future = geminiClient.generateTTS(text, voice, geminiModel, deityTTS, playerReputationRef[0], playerBiomeRef[0])
                .thenApply(response -> {
                    activeRequests.remove(cacheKey);
                    if (response.success) {
                        // Send with effective playback parameters
                        sendTTSToPlayer(player, response, sendVolume, sendSpeed);
                        lastUsedPath.put(player.getUUID().toString(), "gemini");
                    } else {
                        LOGGER.warn("Gemini TTS failed for {}: {}", player.getName().getString(), response.error);
                    }
                    return (Object) response;
                })
                .exceptionally(throwable -> {
                    activeRequests.remove(cacheKey);
                    LOGGER.error("Gemini TTS error for player {}: {}", player.getName().getString(), throwable.getMessage());
                    return (Object) com.bluelotuscoding.eidolonunchained.integration.webtts.WebTTSClient.TTSResponse.failure("Gemini TTS generation error");
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
                        String path = (response.path != null && !response.path.isEmpty()) ? response.path
                            : (response.usedPlayerFunding ? "player2-unknown" : "server-funded");
                        lastUsedPath.put(player.getUUID().toString(), path);
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
                        // Brief toast to indicate path used (client-side chat)
                        trySendPathToast(player);
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
                trySendPathToast(player);

            } else if (audioData != null) {
                LOGGER.info("Attempting VoiceChat spatial playback with audio data for player: {}", player.getName().getString());
                // Try spatial playback first (Simple Voice Chat), fall back to packet
                try {
                    boolean played = com.bluelotuscoding.eidolonunchained.integration.voicechat.VoiceChatIntegration
                        .tryPlaySpatial(player, null, audioData, volume, speed);
                    if (played) {
                        LOGGER.info("SUCCESS: Played TTS audio via VoiceChat (bytes) for player: {}", player.getName().getString());
                        trySendPathToast(player);
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
                trySendPathToast(player);

            } else {
                LOGGER.error("TTS response has no audio URL or data for player: {}", player.getName().getString());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to send TTS audio to player {}: {}", player.getName().getString(), e.getMessage(), e);
        }
    }

    private void trySendPathToast(ServerPlayer player) {
        try {
            String key = player.getUUID().toString();
            String path = lastUsedPath.get(key);
            if (path == null || path.isEmpty()) return;
            String label = switch (path) {
                case "player2-local" -> "§bPlayer2 Local";
                case "player2-web" -> "§bPlayer2 Web";
                case "server-funded" -> "§dServer Funded";
                case "webapi" -> "§aWeb API";
                case "gemini" -> "§eGemini TTS";
                default -> "§7" + path;
            };
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§8[TTS] Path: " + label));
        } catch (Throwable t) {
            LOGGER.debug("Failed to send path toast: {}", t.getMessage());
        }
    }

    public String getLastUsedPath(ServerPlayer player) {
        return lastUsedPath.getOrDefault(player.getUUID().toString(), "");
    }

    /**
     * List available TTS voices for the current provider.
     */
    public CompletableFuture<java.util.List<WebTTSClient.VoiceInfo>> listVoices() {
        // Do not read provider from TOML; return a union of commonly available voices.
        // This is used for suggestions/UI and not bound to a single provider.
        try {
            java.util.List<WebTTSClient.VoiceInfo> combined = new java.util.ArrayList<>();
            combined.addAll(getGeminiVoices());
            combined.addAll(getPlayer2Voices());
            return CompletableFuture.completedFuture(combined);
        } catch (Exception e) {
            LOGGER.debug("Failed to assemble voice list: {}", e.getMessage());
            return CompletableFuture.completedFuture(java.util.Collections.emptyList());
        }
    }

    /**
     * Get list of available Player2 voices
     */
    private java.util.List<WebTTSClient.VoiceInfo> getPlayer2Voices() {
        java.util.List<WebTTSClient.VoiceInfo> voices = new java.util.ArrayList<>();

        // American English Female Voices
        voices.add(new WebTTSClient.VoiceInfo("sophia"));
        voices.add(new WebTTSClient.VoiceInfo("madison"));
        voices.add(new WebTTSClient.VoiceInfo("harper"));
        voices.add(new WebTTSClient.VoiceInfo("olivia"));
        voices.add(new WebTTSClient.VoiceInfo("ava"));
        voices.add(new WebTTSClient.VoiceInfo("amelia"));
        voices.add(new WebTTSClient.VoiceInfo("charlotte"));
        voices.add(new WebTTSClient.VoiceInfo("evelyn"));
        voices.add(new WebTTSClient.VoiceInfo("abigail"));
        voices.add(new WebTTSClient.VoiceInfo("mia"));
        voices.add(new WebTTSClient.VoiceInfo("chloe"));

        // American English Male Voices
        voices.add(new WebTTSClient.VoiceInfo("ethan"));
        voices.add(new WebTTSClient.VoiceInfo("noah"));
        voices.add(new WebTTSClient.VoiceInfo("mason"));
        voices.add(new WebTTSClient.VoiceInfo("logan"));
        voices.add(new WebTTSClient.VoiceInfo("benjamin"));
        voices.add(new WebTTSClient.VoiceInfo("lucas"));
        voices.add(new WebTTSClient.VoiceInfo("jackson"));
        voices.add(new WebTTSClient.VoiceInfo("caleb"));
        voices.add(new WebTTSClient.VoiceInfo("nicholas"));

        // British English Voices
        voices.add(new WebTTSClient.VoiceInfo("eleanor"));
        voices.add(new WebTTSClient.VoiceInfo("poppy"));
        voices.add(new WebTTSClient.VoiceInfo("florence"));
        voices.add(new WebTTSClient.VoiceInfo("amelia_british"));
        voices.add(new WebTTSClient.VoiceInfo("oliver"));
        voices.add(new WebTTSClient.VoiceInfo("harry"));
        voices.add(new WebTTSClient.VoiceInfo("william"));
        voices.add(new WebTTSClient.VoiceInfo("charles"));

        return voices;
    }

    /**
     * Get list of available Gemini voices
     */
    private java.util.List<WebTTSClient.VoiceInfo> getGeminiVoices() {
        java.util.List<WebTTSClient.VoiceInfo> voices = new java.util.ArrayList<>();

        // All 30 Gemini TTS voices (from GeminiTTSClient.GeminiVoice enum)
        voices.add(new WebTTSClient.VoiceInfo("Aoede"));        // Divine muse
        voices.add(new WebTTSClient.VoiceInfo("Archer"));       // Noble warrior
        voices.add(new WebTTSClient.VoiceInfo("Charon"));       // Death's ferryman
        voices.add(new WebTTSClient.VoiceInfo("Euterpe"));      // Music and joy
        voices.add(new WebTTSClient.VoiceInfo("Fenrir"));       // Fierce wolf
        voices.add(new WebTTSClient.VoiceInfo("Helios"));       // Sun god
        voices.add(new WebTTSClient.VoiceInfo("Hermes"));       // Swift messenger
        voices.add(new WebTTSClient.VoiceInfo("Kore"));         // Maiden of spring
        voices.add(new WebTTSClient.VoiceInfo("Puck"));         // Mischievous sprite
        voices.add(new WebTTSClient.VoiceInfo("River"));        // Flowing stream
        voices.add(new WebTTSClient.VoiceInfo("Rouge"));        // Bold and daring
        voices.add(new WebTTSClient.VoiceInfo("Sage"));         // Wise elder
        voices.add(new WebTTSClient.VoiceInfo("Seeker"));       // Curious wanderer
        voices.add(new WebTTSClient.VoiceInfo("Solo"));         // Independent spirit
        voices.add(new WebTTSClient.VoiceInfo("Thalia"));       // Comedy muse
        voices.add(new WebTTSClient.VoiceInfo("Terpsichore"));  // Dance muse
        voices.add(new WebTTSClient.VoiceInfo("Urania"));       // Astronomy muse
        voices.add(new WebTTSClient.VoiceInfo("Zephyr"));       // Gentle wind
        voices.add(new WebTTSClient.VoiceInfo("Erebus"));       // Primordial darkness
        voices.add(new WebTTSClient.VoiceInfo("Florian"));      // Flower bearer
        voices.add(new WebTTSClient.VoiceInfo("Orion"));        // Hunter constellation
        voices.add(new WebTTSClient.VoiceInfo("Nova"));         // Stellar explosion
        voices.add(new WebTTSClient.VoiceInfo("Pixie"));        // Playful fairy
        voices.add(new WebTTSClient.VoiceInfo("Anthem"));       // Triumphant song
        voices.add(new WebTTSClient.VoiceInfo("Journey"));      // Epic adventure
        voices.add(new WebTTSClient.VoiceInfo("Legacy"));       // Ancient wisdom
        voices.add(new WebTTSClient.VoiceInfo("Quest"));        // Bold seeker
        voices.add(new WebTTSClient.VoiceInfo("Spirit"));       // Ethereal essence
        voices.add(new WebTTSClient.VoiceInfo("Lore"));         // Ancient knowledge
        voices.add(new WebTTSClient.VoiceInfo("Echo"));         // Resounding voice

        return voices;
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
        String apiKey = com.bluelotuscoding.eidolonunchained.config.APIKeyManager.getAPIKey("player2ai");
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
