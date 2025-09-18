package com.bluelotuscoding.eidolonunchained.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Client for Google Gemini Speech Generation API
 * Implements Text-to-Speech using Gemini's speech generation capabilities
 * @see <a href="https://ai.google.dev/gemini-api/docs/speech-generation">Gemini Speech Generation Documentation</a>
 */
public class GeminiTTSClient {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Executor EXECUTOR = Executors.newCachedThreadPool();
    
    private final String apiKey;
    private final int timeoutSeconds;

    public GeminiTTSClient(String apiKey) {
        this(apiKey, 30);
    }

    public GeminiTTSClient(String apiKey, int timeoutSeconds) {
        this.apiKey = apiKey;
        this.timeoutSeconds = timeoutSeconds;
        if (apiKey == null || apiKey.isEmpty()) {
            LOGGER.error("Gemini API key is null or empty - TTS will not function");
        }
    }

    /**
     * Generate speech using Gemini Speech Generation API with natural language prompts
     * @param text The text to convert to speech
     * @param voiceConfig Voice configuration settings  
     * @return CompletableFuture containing the audio data as byte array
     */
    public CompletableFuture<byte[]> generateSpeech(String text, GeminiVoiceConfig voiceConfig) {
        if (apiKey == null || apiKey.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Gemini API key not configured"));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Build natural language prompt for Gemini TTS based on official documentation
                String enhancedPrompt = buildNaturalLanguagePrompt(text, voiceConfig);
                
                // Use EXACT structure from official TTS notebook
                JsonObject requestBody = new JsonObject();
                
                // Contents should be an array of content objects, not a simple string
                JsonArray contents = new JsonArray();
                JsonObject contentObj = new JsonObject();
                JsonArray parts = new JsonArray();
                JsonObject textPart = new JsonObject();
                textPart.addProperty("text", enhancedPrompt);
                parts.add(textPart);
                contentObj.add("parts", parts);
                contents.add(contentObj);
                requestBody.add("contents", contents);
                
                // Configure response for audio output - NOTE: "Audio" not "AUDIO" per official docs
                JsonObject config = new JsonObject();
                JsonArray responseModalities = new JsonArray();
                responseModalities.add("Audio");
                config.add("response_modalities", responseModalities);
                
                // Speech configuration with prebuilt voice - following official structure
                JsonObject speechConfig = new JsonObject();
                JsonObject voiceConfigObj = new JsonObject();
                JsonObject prebuiltVoiceConfig = new JsonObject();
                prebuiltVoiceConfig.addProperty("voice_name", voiceConfig.getVoiceName());
                
                voiceConfigObj.add("prebuilt_voice_config", prebuiltVoiceConfig);
                speechConfig.add("voice_config", voiceConfigObj);
                config.add("speech_config", speechConfig);
                
                requestBody.add("config", config);

                String urlString = "https://generativelanguage.googleapis.com/v1beta/models/" + 
                    voiceConfig.getModel() + ":generateContent?key=" + apiKey;

                URL apiUrl = new URL(urlString);
                HttpsURLConnection connection = (HttpsURLConnection) apiUrl.openConnection();
                
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                connection.setConnectTimeout(timeoutSeconds * 1000);
                connection.setReadTimeout(timeoutSeconds * 1000);

                // Write request body
                try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8)) {
                    writer.write(requestBody.toString());
                    writer.flush();
                }

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpsURLConnection.HTTP_OK) {
                    String errorResponse = readErrorResponse(connection);
                    throw new RuntimeException("Gemini TTS API error: " + responseCode + " - " + errorResponse);
                }

                // Read response
                String responseBody = readResponse(connection);
                JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();
                
                // Parse Gemini Speech Generation API response format
                if (responseJson.has("candidates")) {
                    JsonObject candidate = responseJson.getAsJsonArray("candidates").get(0).getAsJsonObject();
                    if (candidate.has("content")) {
                        JsonObject content = candidate.getAsJsonObject("content");
                        if (content.has("parts")) {
                            JsonObject part = content.getAsJsonArray("parts").get(0).getAsJsonObject();
                            if (part.has("inline_data")) {
                                JsonObject inlineData = part.getAsJsonObject("inline_data");
                                if (inlineData.has("data")) {
                                    String base64Audio = inlineData.get("data").getAsString();
                                    return Base64.getDecoder().decode(base64Audio);
                                }
                            }
                        }
                    }
                }
                
                throw new RuntimeException("No audio content in Gemini TTS response");
            } catch (Exception e) {
                LOGGER.error("Error generating speech with Gemini TTS: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to generate speech with Gemini TTS", e);
            }
        }, EXECUTOR);
    }

    /**
     * Build a natural language prompt for Gemini TTS (much simpler than the old approach!)
     * Examples: "Say in a menacing, ancient voice: Your soul belongs to me now, mortal..."
     */
    private String buildNaturalLanguagePrompt(String originalText, GeminiVoiceConfig voiceConfig) {
        StringBuilder prompt = new StringBuilder("Say");
        
        // Add style instructions using natural language
        String emotion = voiceConfig.getEmotion();
        String accent = voiceConfig.getAccent();
        double pitch = voiceConfig.getPitch();
        double speed = voiceConfig.getSpeakingRate();
        
        // Build natural language style description
        List<String> styles = new ArrayList<>();
        
        if (emotion != null && !emotion.isEmpty()) {
            styles.add("in a " + emotion + " tone");
        }
        
        if (accent != null && !accent.isEmpty()) {
            styles.add("with an " + accent + " accent");
        }
        
        // Add pace/pitch descriptions
        if (speed < 0.9) {
            styles.add("slowly and deliberately");
        } else if (speed > 1.1) {
            styles.add("quickly");
        }
        
        if (pitch < 0.9) {
            styles.add("in a deep, low voice");
        } else if (pitch > 1.1) {
            styles.add("in a higher pitch");
        }
        
        // Combine styles naturally
        if (!styles.isEmpty()) {
            prompt.append(" ").append(String.join(", ", styles));
        }
        
        prompt.append(": ").append(originalText);
        return prompt.toString();
    }

    private String readResponse(HttpsURLConnection connection) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    private String readErrorResponse(HttpsURLConnection connection) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        } catch (Exception e) {
            return "Unable to read error response: " + e.getMessage();
        }
    }

    /**
     * Get available voices for Gemini TTS
     * Note: Gemini Speech Generation API doesn't have a direct voice listing endpoint
     * This returns a predefined set of commonly supported voices
     * @return CompletableFuture containing list of available voices
     */
    public CompletableFuture<List<GeminiVoice>> getAvailableVoices() {
        return CompletableFuture.supplyAsync(() -> {
            List<GeminiVoice> voices = new ArrayList<>();
            
            // Official Gemini TTS voices based on the official notebook documentation
            // These are the 30 built-in voices supported by Gemini's TTS model
            voices.add(new GeminiVoice("Zephyr", "en-US", "NEUTRAL", "Prebuilt"));
            voices.add(new GeminiVoice("Puck", "en-US", "NEUTRAL", "Prebuilt"));
            voices.add(new GeminiVoice("Charon", "en-US", "NEUTRAL", "Prebuilt"));
            voices.add(new GeminiVoice("Kore", "en-US", "NEUTRAL", "Prebuilt"));
            voices.add(new GeminiVoice("Fenrir", "en-US", "NEUTRAL", "Prebuilt"));
            voices.add(new GeminiVoice("Leda", "en-US", "NEUTRAL", "Prebuilt"));
            voices.add(new GeminiVoice("Orus", "en-US", "NEUTRAL", "Prebuilt"));
            voices.add(new GeminiVoice("Aoede", "en-US", "NEUTRAL", "Prebuilt"));
            voices.add(new GeminiVoice("Callirhoe", "en-US", "NEUTRAL", "Prebuilt"));
            voices.add(new GeminiVoice("Autonoe", "en-US", "NEUTRAL", "Prebuilt"));
            voices.add(new GeminiVoice("Enceladus", "en-US", "NEUTRAL", "Prebuilt"));
            voices.add(new GeminiVoice("Iapetus", "en-US", "NEUTRAL", "Prebuilt"));
            voices.add(new GeminiVoice("Umbriel", "en-US", "NEUTRAL", "Prebuilt"));
            voices.add(new GeminiVoice("Algieba", "en-US", "NEUTRAL", "Prebuilt"));
            voices.add(new GeminiVoice("Despina", "en-US", "NEUTRAL", "Prebuilt"));
            voices.add(new GeminiVoice("Erinome", "en-US", "NEUTRAL", "Prebuilt"));
            voices.add(new GeminiVoice("Algenib", "en-US", "NEUTRAL", "Prebuilt"));
            voices.add(new GeminiVoice("Rasalgethi", "en-US", "NEUTRAL", "Prebuilt"));
            voices.add(new GeminiVoice("Laomedeia", "en-US", "NEUTRAL", "Prebuilt"));
            voices.add(new GeminiVoice("Achernar", "en-US", "NEUTRAL", "Prebuilt"));
            voices.add(new GeminiVoice("Alnilam", "en-US", "NEUTRAL", "Prebuilt"));
            voices.add(new GeminiVoice("Schedar", "en-US", "NEUTRAL", "Prebuilt"));
            voices.add(new GeminiVoice("Gacrux", "en-US", "NEUTRAL", "Prebuilt"));
            voices.add(new GeminiVoice("Pulcherrima", "en-US", "NEUTRAL", "Prebuilt"));
            voices.add(new GeminiVoice("Achird", "en-US", "NEUTRAL", "Prebuilt"));
            voices.add(new GeminiVoice("Zubenelgenubi", "en-US", "NEUTRAL", "Prebuilt"));
            voices.add(new GeminiVoice("Vindemiatrix", "en-US", "NEUTRAL", "Prebuilt"));
            voices.add(new GeminiVoice("Sadachbia", "en-US", "NEUTRAL", "Prebuilt"));
            voices.add(new GeminiVoice("Sadaltager", "en-US", "NEUTRAL", "Prebuilt"));
            voices.add(new GeminiVoice("Sulafar", "en-US", "NEUTRAL", "Prebuilt"));
            
            return voices;
        });
    }

    /**
     * Test the Gemini TTS connection
     * @return CompletableFuture indicating success or failure
     */
    public CompletableFuture<Boolean> testConnection() {
        if (apiKey == null || apiKey.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }

        // Test with a simple phrase using a valid Gemini TTS voice
        GeminiVoiceConfig testConfig = new GeminiVoiceConfig.Builder()
            .languageCode("en-US")
            .voiceName("Charon")  // Use valid Gemini TTS voice name
            .audioEncoding("MP3")
            .build();

        return generateSpeech("Test", testConfig)
            .thenApply(audioData -> audioData.length > 0)
            .exceptionally(error -> {
                LOGGER.debug("Gemini TTS connection test failed: {}", error.getMessage());
                return false;
            });
    }

    /**
     * Configuration class for Gemini voice settings
     */
    public static class GeminiVoiceConfig {
        private final String languageCode;
        private final String voiceName;
        private final String audioEncoding;
        private final double speakingRate;
        private final double pitch;
        private final double volumeGainDb;
        private final String model;
        private final String emotion;
        private final String accent;

        private GeminiVoiceConfig(Builder builder) {
            this.languageCode = builder.languageCode;
            this.voiceName = builder.voiceName;
            this.audioEncoding = builder.audioEncoding;
            this.speakingRate = builder.speakingRate;
            this.pitch = builder.pitch;
            this.volumeGainDb = builder.volumeGainDb;
            this.model = builder.model;
            this.emotion = builder.emotion;
            this.accent = builder.accent;
        }

        public String getLanguageCode() { return languageCode; }
        public String getVoiceName() { return voiceName; }
        public String getAudioEncoding() { return audioEncoding; }
        public double getSpeakingRate() { return speakingRate; }
        public double getPitch() { return pitch; }
        public double getVolumeGainDb() { return volumeGainDb; }
        public String getModel() { return model; }
        public String getEmotion() { return emotion; }
        public String getAccent() { return accent; }

        public static class Builder {
            private String languageCode = "en-US";
            private String voiceName = "Charon";
            private String audioEncoding = "MP3";
            private double speakingRate = 1.0;
            private double pitch = 0.0;
            private double volumeGainDb = 0.0;
            private String model = "gemini-2.5-flash-preview-tts";
            private String emotion = "";
            private String accent = "";

            public Builder languageCode(String languageCode) {
                this.languageCode = languageCode;
                return this;
            }

            public Builder voiceName(String voiceName) {
                this.voiceName = voiceName;
                return this;
            }

            public Builder audioEncoding(String audioEncoding) {
                this.audioEncoding = audioEncoding;
                return this;
            }

            public Builder speakingRate(double speakingRate) {
                this.speakingRate = Math.max(0.25, Math.min(4.0, speakingRate));
                return this;
            }

            public Builder pitch(double pitch) {
                this.pitch = Math.max(-20.0, Math.min(20.0, pitch));
                return this;
            }

            public Builder volumeGainDb(double volumeGainDb) {
                this.volumeGainDb = Math.max(-96.0, Math.min(16.0, volumeGainDb));
                return this;
            }

            public Builder model(String model) {
                this.model = model;
                return this;
            }

            public Builder emotion(String emotion) {
                this.emotion = emotion;
                return this;
            }

            public Builder accent(String accent) {
                this.accent = accent;
                return this;
            }

            public GeminiVoiceConfig build() {
                return new GeminiVoiceConfig(this);
            }
        }
    }

    /**
     * Voice information class for Gemini TTS
     */
    public static class GeminiVoice {
        private final String name;
        private final String languageCode;
        private final String gender;
        private final String type;

        public GeminiVoice(String name, String languageCode, String gender, String type) {
            this.name = name;
            this.languageCode = languageCode;
            this.gender = gender;
            this.type = type;
        }

        public String getName() { return name; }
        public String getLanguageCode() { return languageCode; }
        public String getGender() { return gender; }
        public String getType() { return type; }

        @Override
        public String toString() {
            return name + " (" + languageCode + ", " + gender + ", " + type + ")";
        }
    }
}
