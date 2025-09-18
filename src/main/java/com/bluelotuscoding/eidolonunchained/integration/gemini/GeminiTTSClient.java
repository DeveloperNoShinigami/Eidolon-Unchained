package com.bluelotuscoding.eidolonunchained.integration.gemini;

import com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig;
import com.bluelotuscoding.eidolonunchained.integration.webtts.WebTTSClient;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Google Gemini TTS Client using the Gemini API Speech Generation
 * https://ai.google.dev/gemini-api/docs/speech-generation
 */
public class GeminiTTSClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(GeminiTTSClient.class);

    private static final String GEMINI_TTS_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";

    // Supported Gemini voices (All 30 available voices)
    public enum GeminiVoice {
        // Original 5 voices (most commonly used)
        PUCK("Puck"),           // Upbeat
        CHARON("Charon"),       // Informative
        KORE("Kore"),           // Firm
        FENRIR("Fenrir"),       // Excitable
        AOEDE("Aoede"),         // Breezy

        // Additional 25 voices
        ZEPHYR("Zephyr"),               // Bright
        LEDA("Leda"),                   // Youthful
        ORUS("Orus"),                   // Firm
        CALLIRRHOE("Callirrhoe"),       // Easy-going
        AUTONOE("Autonoe"),             // Bright
        ENCELADUS("Enceladus"),         // Breathy
        IAPETUS("Iapetus"),             // Clear
        UMBRIEL("Umbriel"),             // Easy-going
        ALGIEBA("Algieba"),             // Smooth
        DESPINA("Despina"),             // Smooth
        ERINOME("Erinome"),             // Clear
        ALGENIB("Algenib"),             // Gravelly
        RASALGETHI("Rasalgethi"),       // Informative
        LAOMEDEIA("Laomedeia"),         // Upbeat
        ACHERNAR("Achernar"),           // Soft
        ALNILAM("Alnilam"),             // Firm
        SCHEDAR("Schedar"),             // Even
        GACRUX("Gacrux"),               // Mature
        PULCHERRIMA("Pulcherrima"),     // Forward
        ACHIRD("Achird"),               // Friendly
        ZUBENELGENUBI("Zubenelgenubi"), // Casual
        VINDEMIATRIX("Vindemiatrix"),   // Gentle
        SADACHBIA("Sadachbia"),         // Lively
        SADALTAGER("Sadaltager"),       // Knowledgeable
        SULAFAT("Sulafat");             // Warm

        private final String voiceName;

        GeminiVoice(String voiceName) {
            this.voiceName = voiceName;
        }

        public String getVoiceName() {
            return voiceName;
        }

        public static GeminiVoice fromString(String voice) {
            if (voice == null || voice.isEmpty() || "auto".equalsIgnoreCase(voice)) {
                return CHARON; // Default dark voice for deities
            }

            for (GeminiVoice v : values()) {
                if (v.voiceName.equalsIgnoreCase(voice) || v.name().equalsIgnoreCase(voice)) {
                    return v;
                }
            }

            LOGGER.warn("Unknown Gemini voice '{}', using Charon", voice);
            return CHARON;
        }
    }

    public CompletableFuture<WebTTSClient.TTSResponse> generateTTS(String text, String voice, String model) {
        return generateTTS(text, voice, model, null);
    }

    public CompletableFuture<WebTTSClient.TTSResponse> generateTTS(String text, String voice, String model, com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig.TTSConfig ttsConfig) {
        return generateTTS(text, voice, model, ttsConfig, null, null);
    }

    public CompletableFuture<WebTTSClient.TTSResponse> generateTTS(String text, String voice, String model, com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig.TTSConfig ttsConfig, Integer playerReputation, String playerBiome) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String apiKey = com.bluelotuscoding.eidolonunchained.config.APIKeyManager.getAPIKey("gemini");
                if (apiKey == null || apiKey.trim().isEmpty()) {
                    LOGGER.error("Gemini API key not configured");
                    return WebTTSClient.TTSResponse.failure("Gemini API key not configured");
                }

                // Use specified model or default to TTS-capable model
                String ttsModel = (model != null && !model.isEmpty())
                    ? model
                    : "gemini-2.5-flash-preview-tts"; // Default TTS model

                String url = String.format(GEMINI_TTS_BASE_URL, ttsModel) + "?key=" + apiKey;

                // Enhanced voice selection using TTS config with context
                String selectedVoice = selectVoiceFromConfig(voice, ttsConfig, playerReputation, playerBiome);
                GeminiVoice geminiVoice = GeminiVoice.fromString(selectedVoice);

                // Create enhanced Gemini TTS request payload
                String jsonPayload = createTTSRequest(text, geminiVoice, ttsConfig);

                LOGGER.info("Gemini TTS request - Model: {}, Voice: {}, Text length: {}",
                    ttsModel, geminiVoice.getVoiceName(), text.length());

                HttpsURLConnection connection = (HttpsURLConnection) URI.create(url).toURL().openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(60000);

                // Send request
                try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream())) {
                    writer.write(jsonPayload);
                    writer.flush();
                }

                int responseCode = connection.getResponseCode();

                if (responseCode == 200) {
                    // Read response
                    StringBuilder response = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                    }

                    // Parse Gemini response and extract audio
                    byte[] audioData = parseGeminiResponse(response.toString());
                    if (audioData != null) {
                        LOGGER.info("Gemini TTS success - Audio size: {} bytes", audioData.length);
                        return WebTTSClient.TTSResponse.successData(audioData);
                    } else {
                        LOGGER.error("Failed to parse Gemini TTS response");
                        return WebTTSClient.TTSResponse.failure("Failed to parse audio from response");
                    }
                } else {
                    // Read error response
                    String errorResponse = "";
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                        String line;
                        StringBuilder errorBuilder = new StringBuilder();
                        while ((line = reader.readLine()) != null) {
                            errorBuilder.append(line);
                        }
                        errorResponse = errorBuilder.toString();
                    } catch (Exception e) {
                        // Ignore error reading error stream
                    }

                    LOGGER.error("Gemini TTS failed - HTTP {}: {}", responseCode, errorResponse);
                    return WebTTSClient.TTSResponse.failure("HTTP " + responseCode + ": " + errorResponse);
                }

            } catch (Exception e) {
                LOGGER.error("Gemini TTS exception", e);
                return WebTTSClient.TTSResponse.failure("Exception: " + e.getMessage());
            }
        });
    }

    private String selectVoiceFromConfig(String defaultVoice, com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig.TTSConfig ttsConfig, Integer playerReputation, String playerBiome) {
        if (ttsConfig == null) return defaultVoice;

        // Check voice aliases first
        if (ttsConfig.voice_aliases != null && ttsConfig.voice_aliases.containsKey(defaultVoice)) {
            return ttsConfig.voice_aliases.get(defaultVoice);
        }

        // Reputation-based voice selection
        if (playerReputation != null && ttsConfig.reputation_voices != null) {
            String repVoice = null;
            int bestMatch = -1;
            for (Map.Entry<String, String> entry : ttsConfig.reputation_voices.entrySet()) {
                try {
                    int threshold = Integer.parseInt(entry.getKey());
                    if (playerReputation >= threshold && threshold > bestMatch) {
                        bestMatch = threshold;
                        repVoice = entry.getValue();
                    }
                } catch (NumberFormatException ignored) {}
            }
            if (repVoice != null) return repVoice;
        }

        // Biome-based voice selection
        if (playerBiome != null && ttsConfig.biome_voices != null && ttsConfig.biome_voices.containsKey(playerBiome)) {
            return ttsConfig.biome_voices.get(playerBiome);
        }

        // Use backup voice if primary voice is not available
        if (ttsConfig.backup_voice != null && !ttsConfig.backup_voice.isEmpty()) {
            GeminiVoice primary = GeminiVoice.fromString(defaultVoice);
            if (primary == GeminiVoice.CHARON && !defaultVoice.equals(primary.getVoiceName())) {
                return ttsConfig.backup_voice;
            }
        }

        return defaultVoice;
    }

    private String createTTSRequest(String text, GeminiVoice voice, com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig.TTSConfig ttsConfig) {
        // Enhanced Gemini TTS request with style prompting based on TTS config
        String enhancedText = text;

        if (ttsConfig != null) {
            // Add style instructions based on config using natural language prompting
            StringBuilder styleInstructions = new StringBuilder();

            // Enhanced emotion mapping
            if (ttsConfig.emotion != null && !ttsConfig.emotion.isEmpty() && !"neutral".equals(ttsConfig.emotion)) {
                String emotionPrompt = mapEmotionToPrompt(ttsConfig.emotion);
                if (emotionPrompt != null) {
                    styleInstructions.append(" ").append(emotionPrompt);
                }
            }

            // Enhanced accent mapping
            if (ttsConfig.accent != null && !ttsConfig.accent.isEmpty() && !"default".equals(ttsConfig.accent)) {
                String accentPrompt = mapAccentToPrompt(ttsConfig.accent);
                if (accentPrompt != null) {
                    styleInstructions.append(" ").append(accentPrompt);
                }
            }

            // Enhanced speed control
            if (ttsConfig.speed < 0.7f) {
                styleInstructions.append(" Speak very slowly and deliberately with long pauses.");
            } else if (ttsConfig.speed < 0.85f) {
                styleInstructions.append(" Speak slowly and thoughtfully.");
            } else if (ttsConfig.speed > 1.3f) {
                styleInstructions.append(" Speak rapidly with intense urgency.");
            } else if (ttsConfig.speed > 1.15f) {
                styleInstructions.append(" Speak with energy and pace.");
            }

            // Enhanced pitch control
            if (ttsConfig.pitch < 0.7f) {
                styleInstructions.append(" Use a deep, resonant, otherworldly voice.");
            } else if (ttsConfig.pitch < 0.85f) {
                styleInstructions.append(" Use a deeper, more authoritative voice.");
            } else if (ttsConfig.pitch > 1.3f) {
                styleInstructions.append(" Use a higher, ethereal, mystical voice.");
            } else if (ttsConfig.pitch > 1.15f) {
                styleInstructions.append(" Use a slightly higher, more spiritual voice.");
            }

            // Volume/intensity mapping
            if (ttsConfig.volume < 0.7f) {
                styleInstructions.append(" Speak in a quiet whisper.");
            } else if (ttsConfig.volume > 1.3f) {
                styleInstructions.append(" Speak with powerful, booming projection.");
            }

            // Emphasis level mapping
            if (ttsConfig.emphasis_level == 1) {
                styleInstructions.append(" Use moderate dramatic emphasis.");
            } else if (ttsConfig.emphasis_level >= 2) {
                styleInstructions.append(" Use strong dramatic emphasis with theatrical delivery.");
            }

            if (styleInstructions.length() > 0) {
                enhancedText = "[Style:" + styleInstructions.toString() + "] " + text;
            }
        }

        // Determine audio format from TTS config, default to compressed format for bandwidth efficiency
        String audioFormat = "MP3"; // Default to MP3 - conversion handled by VoiceChatIntegration
        if (ttsConfig != null && ttsConfig.audio_format != null && !ttsConfig.audio_format.isEmpty()) {
            // Map deity config audio format to Gemini API format
            String configFormat = ttsConfig.audio_format.toUpperCase();
            audioFormat = switch (configFormat) {
                case "MP3" -> "MP3"; // Request MP3 for file size efficiency
                case "WAV" -> "LINEAR16"; // Gemini's WAV format - better for sample rate handling
                case "FLAC" -> "FLAC";
                case "OGG" -> "OGG_OPUS";
                case "OPUS" -> "OGG_OPUS";
                case "PCM", "LINEAR16" -> "LINEAR16";
                default -> {
                    LOGGER.warn("Unknown audio format '{}' in TTS config, using MP3", ttsConfig.audio_format);
                    yield "MP3";
                }
            };
        }

        LOGGER.debug("Requesting audio format: {}", audioFormat);

        return String.format("""
            {
              "contents": [{
                "parts": [{
                  "text": "%s"
                }]
              }],
              "generationConfig": {
                "responseModalities": ["AUDIO"],
                "speechConfig": {
                  "voiceConfig": {
                    "prebuiltVoiceConfig": {
                      "voiceName": "%s"
                    }
                  }
                }
              }
            }
            """,
            enhancedText.replace("\"", "\\\"").replace("\n", "\\n"),
            voice.getVoiceName());
    }

    /**
     * Maps emotion keywords to natural language prompts for Gemini TTS
     */
    private String mapEmotionToPrompt(String emotion) {
        return switch (emotion.toLowerCase()) {
            // Primary emotions
            case "happy", "joyful", "cheerful" -> "Speak with a happy, joyful tone.";
            case "sad", "melancholy", "sorrowful" -> "Speak with a sad, melancholy tone.";
            case "angry", "furious", "wrathful" -> "Speak with an angry, wrathful tone.";
            case "fearful", "scared", "terrified" -> "Speak with a fearful, trembling tone.";
            case "excited", "enthusiastic", "energetic" -> "Speak with excited enthusiasm.";
            case "calm", "peaceful", "serene" -> "Speak with a calm, peaceful tone.";

            // Deity-specific emotions
            case "menacing", "threatening", "ominous" -> "Speak in a menacing, threatening whisper.";
            case "divine", "holy", "sacred" -> "Speak with divine, holy reverence.";
            case "ancient", "wise", "timeless" -> "Speak with ancient wisdom and gravitas.";
            case "mysterious", "enigmatic", "cryptic" -> "Speak mysteriously with cryptic undertones.";
            case "ethereal", "otherworldly", "spectral" -> "Speak with an ethereal, otherworldly quality.";
            case "commanding", "authoritative", "imperial" -> "Speak with commanding authority.";
            case "gentle", "kind", "compassionate" -> "Speak with gentle compassion.";
            case "dark", "sinister", "malevolent" -> "Speak with dark, sinister undertones.";
            case "playful", "mischievous", "impish" -> "Speak with playful mischief.";
            case "noble", "regal", "majestic" -> "Speak with noble majesty.";

            // Intensity levels
            case "whispered", "hushed", "secretive" -> "Speak in a secretive whisper.";
            case "booming", "thunderous", "powerful" -> "Speak with booming, thunderous power.";
            case "sultry", "seductive", "alluring" -> "Speak with sultry allure.";
            case "weary", "tired", "exhausted" -> "Speak with weary exhaustion.";
            case "eager", "impatient", "restless" -> "Speak with eager impatience.";

            default -> null;
        };
    }

    /**
     * Maps accent keywords to natural language prompts for Gemini TTS
     */
    private String mapAccentToPrompt(String accent) {
        return switch (accent.toLowerCase()) {
            // Mystical/Fantasy accents
            case "ancient", "archaic", "old" -> "Use an ancient, archaic speaking style.";
            case "ethereal", "otherworldly", "mystical" -> "Use an ethereal, otherworldly accent.";
            case "divine", "celestial", "heavenly" -> "Use a divine, celestial accent.";
            case "demonic", "infernal", "hellish" -> "Use a demonic, infernal accent.";
            case "draconic", "draconian", "dragon" -> "Use a draconic, powerful accent.";
            case "elven", "elvish", "fae" -> "Use an elegant, elven accent.";
            case "dwarven", "dwarvish", "gruff" -> "Use a gruff, dwarven accent.";
            case "orcish", "brutish", "savage" -> "Use a brutish, savage accent.";

            // Regional/Cultural accents
            case "british", "english", "posh" -> "Use a refined British accent.";
            case "scottish", "highland" -> "Use a Scottish Highland accent.";
            case "irish", "gaelic" -> "Use an Irish accent.";
            case "american", "western" -> "Use an American accent.";
            case "southern", "drawl" -> "Use a Southern drawl.";
            case "northern", "yankee" -> "Use a Northern accent.";

            // Vocal qualities
            case "gravelly", "rough", "hoarse" -> "Use a gravelly, rough vocal quality.";
            case "smooth", "silky", "refined" -> "Use a smooth, refined vocal quality.";
            case "breathy", "wispy", "airy" -> "Use a breathy, wispy vocal quality.";
            case "nasally", "sharp", "piercing" -> "Use a sharp, piercing vocal quality.";

            // Character types
            case "scholarly", "academic", "learned" -> "Use a scholarly, academic accent.";
            case "noble", "aristocratic", "royal" -> "Use a noble, aristocratic accent.";
            case "peasant", "common", "rustic" -> "Use a rustic, common accent.";
            case "military", "commanding", "stern" -> "Use a stern, military accent.";

            default -> null;
        };
    }

    private byte[] parseGeminiResponse(String jsonResponse) {
        try {
            // Debug: Log the response structure to understand the format
            LOGGER.info("Gemini TTS Response length: {} characters", jsonResponse.length());
            if (jsonResponse.length() > 500) {
                LOGGER.info("Gemini TTS Response (first 500 chars): {}", jsonResponse.substring(0, 500));
            } else {
                LOGGER.info("Gemini TTS Response (full): {}", jsonResponse);
            }

            // First try JSON-based parsing for robustness
            byte[] jsonResult = parseWithJsonLibrary(jsonResponse);
            if (jsonResult != null) {
                return jsonResult;
            }
            
            // Fallback to string-based parsing if JSON parsing fails
            LOGGER.info("JSON parsing failed, falling back to string-based parsing");

            // Gemini TTS returns audio data in candidates[0].content.parts[0].inlineData.data
            // Look for the correct structure: "inlineData":{"data":"base64-data","mimeType":"audio/format"}
            if (jsonResponse.contains("\"inlineData\"") && jsonResponse.contains("\"data\"")) {
                // Find the inlineData section
                int inlineDataStart = jsonResponse.indexOf("\"inlineData\"");
                if (inlineDataStart != -1) {
                    // Find the mimeType to understand the format
                    String mimeType = "unknown";
                    int mimeStart = jsonResponse.indexOf("\"mimeType\":\"", inlineDataStart);
                    if (mimeStart != -1) {
                        mimeStart += 12; // Skip past "mimeType":"
                        int mimeEnd = jsonResponse.indexOf("\"", mimeStart);
                        if (mimeEnd > mimeStart) {
                            mimeType = jsonResponse.substring(mimeStart, mimeEnd);
                            LOGGER.info("Found mimeType: {}", mimeType);
                        }
                    }

                    // More robust search for the data field within inlineData
                    // Look for data field AFTER the inlineData start but before the closing brace
                    int searchStart = inlineDataStart;
                    int searchEnd = jsonResponse.indexOf("}", searchStart);
                    if (searchEnd == -1) searchEnd = jsonResponse.length();
                    
                    // Find "data":" within the inlineData object
                    String searchArea = jsonResponse.substring(searchStart, searchEnd);
                    int dataFieldPos = searchArea.indexOf("\"data\":\"");
                    
                    if (dataFieldPos != -1) {
                        int dataStart = searchStart + dataFieldPos + 8; // Skip past "data":"
                        
                        // Find the end of the data value (look for the closing quote)
                        // Need to be careful of escaped quotes in base64 data
                        int dataEnd = -1;
                        int searchPos = dataStart;
                        while (searchPos < jsonResponse.length()) {
                            int quotePos = jsonResponse.indexOf("\"", searchPos);
                            if (quotePos == -1) break;
                            
                            // Check if this quote is escaped
                            if (quotePos > 0 && jsonResponse.charAt(quotePos - 1) != '\\') {
                                dataEnd = quotePos;
                                break;
                            }
                            searchPos = quotePos + 1;
                        }

                        if (dataEnd > dataStart) {
                            String base64Audio = jsonResponse.substring(dataStart, dataEnd);
                            LOGGER.info("Found inlineData.data, base64 length: {}, mimeType: {}", base64Audio.length(), mimeType);

                            // Decode the audio data (format depends on mimeType)
                            byte[] audioData = Base64.getDecoder().decode(base64Audio);
                            LOGGER.info("Decoded audio data, byte length: {}, format: {}", audioData.length, mimeType);

                            // Log format details for debugging
                            if (mimeType.contains("L16") || mimeType.contains("pcm")) {
                                LOGGER.info("Audio format: PCM (may need conversion for playback)");
                            } else if (mimeType.contains("wav")) {
                                LOGGER.info("Audio format: WAV (ready for playback)");
                            } else if (mimeType.contains("mp3")) {
                                LOGGER.info("Audio format: MP3 (ready for playback)");
                            }

                            return audioData;
                        }
                    }
                }
            }

            // Legacy fallback: Look for "audioData" field (older format)
            if (jsonResponse.contains("\"audioData\"")) {
                int start = jsonResponse.indexOf("\"audioData\":\"") + 13;
                int end = jsonResponse.indexOf("\"", start);
                if (start > 12 && end > start) {
                    String base64Audio = jsonResponse.substring(start, end);
                    LOGGER.info("Found legacy audioData, base64 length: {}", base64Audio.length());
                    return Base64.getDecoder().decode(base64Audio);
                }
            }

            // Debug: Check what fields are present
            if (jsonResponse.contains("\"candidates\"")) {
                LOGGER.info("Found 'candidates' field in response");
            }
            if (jsonResponse.contains("\"content\"")) {
                LOGGER.info("Found 'content' field in response");
            }
            if (jsonResponse.contains("\"parts\"")) {
                LOGGER.info("Found 'parts' field in response");
            }

            LOGGER.error("No audio data found in Gemini response structure");
            return null;
        } catch (Exception e) {
            LOGGER.error("Error parsing Gemini response", e);
            return null;
        }
    }

    /**
     * Parse Gemini response using proper JSON library for robustness
     */
    private byte[] parseWithJsonLibrary(String jsonResponse) {
        try {
            Gson gson = new Gson();
            JsonObject response = gson.fromJson(jsonResponse, JsonObject.class);
            
            // Navigate the JSON structure: candidates[0].content.parts[0].inlineData.data
            if (response.has("candidates")) {
                JsonArray candidates = response.getAsJsonArray("candidates");
                if (candidates.size() > 0) {
                    JsonObject candidate = candidates.get(0).getAsJsonObject();
                    
                    if (candidate.has("content")) {
                        JsonObject content = candidate.getAsJsonObject("content");
                        
                        if (content.has("parts")) {
                            JsonArray parts = content.getAsJsonArray("parts");
                            if (parts.size() > 0) {
                                JsonObject part = parts.get(0).getAsJsonObject();
                                
                                if (part.has("inlineData")) {
                                    JsonObject inlineData = part.getAsJsonObject("inlineData");
                                    
                                    if (inlineData.has("data") && inlineData.has("mimeType")) {
                                        String base64Data = inlineData.get("data").getAsString();
                                        String mimeType = inlineData.get("mimeType").getAsString();
                                        
                                        LOGGER.info("JSON parsing: Found audio data, base64 length: {}, mimeType: {}", base64Data.length(), mimeType);
                                        
                                        // Decode the audio data
                                        byte[] audioData = Base64.getDecoder().decode(base64Data);
                                        LOGGER.info("JSON parsing: Decoded audio data, byte length: {}, format: {}", audioData.length, mimeType);
                                        
                                        return audioData;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            LOGGER.warn("JSON parsing: No audio data found in expected structure");
            return null;
            
        } catch (Exception e) {
            LOGGER.warn("JSON parsing failed: {}", e.getMessage());
            return null;
        }
    }

    public CompletableFuture<Boolean> testConnection() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String apiKey = com.bluelotuscoding.eidolonunchained.config.APIKeyManager.getAPIKey("gemini");
                if (apiKey == null || apiKey.trim().isEmpty()) {
                    LOGGER.warn("Gemini API key not configured for TTS test");
                    return false;
                }

                // Test with a simple request
                return generateTTS("Test", "Charon", "gemini-2.5-flash-preview-tts")
                    .get().audioData != null;

            } catch (Exception e) {
                LOGGER.error("Gemini TTS connection test failed", e);
                return false;
            }
        });
    }
}