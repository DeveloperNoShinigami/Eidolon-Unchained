package com.bluelotuscoding.eidolonunchained.integration.webtts;

import com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig;
import com.bluelotuscoding.eidolonunchained.integration.http.HttpTransport;
import com.google.gson.*;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Generic Web TTS API client supporting /tts/voices and /tts/speak.
 */
public class WebTTSClient {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final transient Executor EXECUTOR = Executors.newCachedThreadPool();

    public static class VoiceInfo {
        public final String id;
        public VoiceInfo(String id) { this.id = id; }
    }

    public static class TTSRequest {
        public final String text;
        public final List<String> voiceIds;
        public final double speed;
        public final String audioFormat;
        public final String voiceGender;
        public final String voiceLanguage;
        public final ServerPlayer player;
        public final String deityId;

        public TTSRequest(String text, List<String> voiceIds, double speed, String audioFormat,
                          String voiceGender, String voiceLanguage, ServerPlayer player, String deityId) {
            this.text = text;
            this.voiceIds = voiceIds;
            this.speed = speed;
            this.audioFormat = audioFormat;
            this.voiceGender = voiceGender;
            this.voiceLanguage = voiceLanguage;
            this.player = player;
            this.deityId = deityId;
        }
    }

    public static class TTSResponse {
        public final boolean success;
        public final String audioUrl;  // if server returns URL
        public final byte[] audioData; // if server returns base64 data
        public final String error;
        public TTSResponse(boolean success, String audioUrl, byte[] audioData, String error) {
            this.success = success; this.audioUrl = audioUrl; this.audioData = audioData; this.error = error;
        }
        public static TTSResponse successUrl(String url){ return new TTSResponse(true, url, null, null);} 
        public static TTSResponse successData(byte[] data){ return new TTSResponse(true, null, data, null);} 
        public static TTSResponse failure(String error){ return new TTSResponse(false, null, null, error);} 
    }

    private static String getBaseUrl() {
        String base = EidolonUnchainedConfig.WEBTTS_BASE_URL.get();
        if (base == null || base.isEmpty()) base = "http://localhost:3000"; // sensible default
        return base;
    }

    private static String getApiKey() {
        return com.bluelotuscoding.eidolonunchained.config.APIKeyManager.getAPIKey("webtts");
    }

    public CompletableFuture<List<VoiceInfo>> listVoices() {
        return CompletableFuture.supplyAsync(() -> {
            List<VoiceInfo> voices = new ArrayList<>();
            try {
                String urlStr = getBaseUrl() + "/tts/voices";
                Map<String, String> headers = new HashMap<>();
                String apiKey = getApiKey();
                if (apiKey != null && !apiKey.isEmpty()) {
                    headers.put("Authorization", "Bearer " + apiKey);
                }
                HttpTransport.Response response = HttpTransport.get(urlStr, headers, 30);
                int code = response.statusCode();
                if (code == 200) {
                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    JsonArray arr = json.getAsJsonArray("voices");
                    if (arr != null) {
                        for (JsonElement el : arr) {
                            if (el.isJsonObject()) {
                                JsonObject v = el.getAsJsonObject();
                                String id = v.has("id") ? v.get("id").getAsString() : v.toString();
                                voices.add(new VoiceInfo(id));
                            } else {
                                voices.add(new VoiceInfo(el.getAsString()));
                            }
                        }
                    }
                } else {
                    LOGGER.warn("/tts/voices returned status {}", code);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to fetch voices: {}", e.getMessage());
            }
            return voices;
        }, EXECUTOR);
    }

    public CompletableFuture<TTSResponse> speak(TTSRequest req) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String urlStr = getBaseUrl() + "/tts/speak";
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                String apiKey = getApiKey();
                if (apiKey != null && !apiKey.isEmpty()) {
                    headers.put("Authorization", "Bearer " + apiKey);
                }

                JsonObject payload = new JsonObject();
                payload.addProperty("text", req.text);
                if (req.voiceIds != null && !req.voiceIds.isEmpty()) {
                    JsonArray ids = new JsonArray();
                    for (String id : req.voiceIds) ids.add(id);
                    payload.add("voice_ids", ids);
                }
                payload.addProperty("speed", req.speed);
                payload.addProperty("audio_format", req.audioFormat);
                if (req.voiceGender != null) payload.addProperty("voice_gender", req.voiceGender);
                if (req.voiceLanguage != null) payload.addProperty("voice_language", req.voiceLanguage);

                HttpTransport.Response response = HttpTransport.postJson(urlStr, headers, payload.toString(), 60);
                int code = response.statusCode();
                if (code == 200) {
                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    if (json.has("data")) {
                        String data = json.get("data").getAsString();
                        // Assume base64 audio data string
                        byte[] audio = Base64.getDecoder().decode(data);
                        return TTSResponse.successData(audio);
                    }
                    // Fallback: maybe returned a URL
                    if (json.has("url")) {
                        return TTSResponse.successUrl(json.get("url").getAsString());
                    }
                    return TTSResponse.failure("No audio data in response");
                } else if (code == 401) {
                    return TTSResponse.failure("Unauthorized (401) - check API key");
                } else if (code == 402) {
                    return TTSResponse.failure("Insufficient credits (402)");
                } else if (code == 400) {
                    return TTSResponse.failure("Invalid request (400): " + response.body());
                } else {
                    return TTSResponse.failure("TTS server error: " + code);
                }
            } catch (Exception e) {
                return TTSResponse.failure("Speak error: " + e.getMessage());
            }
        }, EXECUTOR);
    }
}
