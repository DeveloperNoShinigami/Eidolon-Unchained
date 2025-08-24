package com.bluelotuscoding.eidolonunchained.integration.gemini;

import com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig;
import com.bluelotuscoding.eidolonunchained.ai.GenerationConfig;
import com.bluelotuscoding.eidolonunchained.ai.SafetySettings;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Google Gemini API client for AI deity interactions
 */
public class GeminiAPIClient {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String GEMINI_API_BASE = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final Executor EXECUTOR = Executors.newCachedThreadPool();
    
    private final String apiKey;
    private final String model;
    private final int timeoutSeconds;
    
    public GeminiAPIClient(String apiKey, String model, int timeoutSeconds) {
        this.apiKey = apiKey;
        this.model = model;
        this.timeoutSeconds = timeoutSeconds;
    }
    
    /**
     * Generate AI response for deity prayer
     */
    public CompletableFuture<AIResponse> generateResponse(
            String prompt, 
            String personality,
            GenerationConfig genConfig,
            SafetySettings safetySettings) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject requestBody = buildRequestBody(prompt, personality, genConfig, safetySettings);
                String response = sendRequest(requestBody);
                return parseResponse(response);
            } catch (Exception e) {
                LOGGER.error("Failed to generate AI response", e);
                return new AIResponse(false, "I cannot hear your prayer clearly right now. Please try again later.", List.of());
            }
        }, EXECUTOR);
    }
    
    private JsonObject buildRequestBody(
            String prompt, 
            String personality,
            GenerationConfig genConfig,
            SafetySettings safetySettings) {
        
        JsonObject request = new JsonObject();
        
        // Contents array
        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        JsonArray parts = new JsonArray();
        
        JsonObject textPart = new JsonObject();
        String fullPrompt = personality + "\n\n" + prompt;
        textPart.addProperty("text", fullPrompt);
        parts.add(textPart);
        
        content.add("parts", parts);
        contents.add(content);
        request.add("contents", contents);
        
        // Safety settings
        JsonArray safetyArray = new JsonArray();
        addSafetySetting(safetyArray, "HARM_CATEGORY_HARASSMENT", safetySettings.harassment);
        addSafetySetting(safetyArray, "HARM_CATEGORY_HATE_SPEECH", safetySettings.hate_speech);
        addSafetySetting(safetyArray, "HARM_CATEGORY_SEXUALLY_EXPLICIT", safetySettings.sexually_explicit);
        addSafetySetting(safetyArray, "HARM_CATEGORY_DANGEROUS_CONTENT", safetySettings.dangerous_content);
        request.add("safetySettings", safetyArray);
        
        // Generation config
        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", genConfig.temperature);
        generationConfig.addProperty("maxOutputTokens", genConfig.max_output_tokens);
        request.add("generationConfig", generationConfig);
        
        return request;
    }
    
    private void addSafetySetting(JsonArray array, String category, String threshold) {
        JsonObject setting = new JsonObject();
        setting.addProperty("category", category);
        setting.addProperty("threshold", threshold);
        array.add(setting);
    }
    
    private String sendRequest(JsonObject requestBody) throws IOException {
        String urlString = GEMINI_API_BASE + model + ":generateContent?key=" + apiKey;
        @SuppressWarnings("deprecation")
        URL url = new URL(urlString);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        connection.setConnectTimeout(timeoutSeconds * 1000);
        connection.setReadTimeout(timeoutSeconds * 1000);
        
        // Send request
        try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8)) {
            writer.write(requestBody.toString());
            writer.flush();
        }
        
        // Read response
        int responseCode = connection.getResponseCode();
        InputStream inputStream = responseCode >= 200 && responseCode < 300 
            ? connection.getInputStream() 
            : connection.getErrorStream();
            
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        
        if (responseCode >= 400) {
            throw new IOException("API request failed with code " + responseCode + ": " + response.toString());
        }
        
        return response.toString();
    }
    
    private AIResponse parseResponse(String jsonResponse) {
        try {
            JsonObject response = JsonParser.parseString(jsonResponse).getAsJsonObject();
            
            if (response.has("candidates") && response.getAsJsonArray("candidates").size() > 0) {
                JsonObject candidate = response.getAsJsonArray("candidates").get(0).getAsJsonObject();
                
                if (candidate.has("content")) {
                    JsonObject content = candidate.getAsJsonObject("content");
                    if (content.has("parts") && content.getAsJsonArray("parts").size() > 0) {
                        JsonObject part = content.getAsJsonArray("parts").get(0).getAsJsonObject();
                        if (part.has("text")) {
                            String text = part.get("text").getAsString();
                            return parseAIResponseText(text);
                        }
                    }
                }
            }
            
            LOGGER.warn("Unexpected API response format: {}", jsonResponse);
            return new AIResponse(false, "The deity's voice is unclear at this moment.", List.of());
            
        } catch (Exception e) {
            LOGGER.error("Failed to parse AI response", e);
            return new AIResponse(false, "The divine message was lost in translation.", List.of());
        }
    }
    
    /**
     * Parse the AI text response to extract dialogue and commands
     */
    private AIResponse parseAIResponseText(String text) {
        List<String> commands = new java.util.ArrayList<>();
        StringBuilder dialogue = new StringBuilder();
        
        String[] lines = text.split("\n");
        boolean inCommandSection = false;
        
        for (String line : lines) {
            line = line.trim();
            
            // Look for command markers
            if (line.toLowerCase().contains("command:") || line.startsWith("/")) {
                inCommandSection = true;
                // Extract the actual command
                if (line.startsWith("/")) {
                    commands.add(line);
                } else if (line.contains(":")) {
                    String command = line.substring(line.indexOf(":") + 1).trim();
                    if (command.startsWith("/")) {
                        commands.add(command);
                    }
                }
            } else if (line.startsWith("*") || line.toLowerCase().contains("end") && inCommandSection) {
                inCommandSection = false;
            } else if (!inCommandSection && !line.isEmpty()) {
                if (dialogue.length() > 0) {
                    dialogue.append(" ");
                }
                dialogue.append(line);
            }
        }
        
        // Clean up dialogue
        String finalDialogue = dialogue.toString().trim();
        if (finalDialogue.isEmpty()) {
            finalDialogue = "The deity acknowledges your prayer.";
        }
        
        return new AIResponse(true, finalDialogue, commands);
    }
    
    /**
     * Build context information for AI prompts
     */
    public static String buildPlayerContext(ServerPlayer player) {
        StringBuilder context = new StringBuilder();
        
        // Basic info
        context.append("Player: ").append(player.getName().getString()).append("\n");
        
        // Location
        BlockPos pos = player.blockPosition();
        context.append("Location: ").append(pos.getX()).append(", ").append(pos.getY()).append(", ").append(pos.getZ()).append("\n");
        
        // Biome
        String biomeId = player.level().getBiome(pos).unwrapKey()
            .map(resourceKey -> resourceKey.location().toString())
            .orElse("minecraft:plains");
        context.append("Biome: ").append(biomeId).append("\n");
        
        // Time and weather
        long time = player.level().getDayTime() % 24000;
        String timeOfDay = getTimeOfDay(time);
        context.append("Time: ").append(timeOfDay).append("\n");
        
        boolean isRaining = player.level().isRaining();
        boolean isThundering = player.level().isThundering();
        String weather = isThundering ? "thundering" : (isRaining ? "raining" : "clear");
        context.append("Weather: ").append(weather).append("\n");
        
        // Health and experience
        context.append("Health: ").append((int)player.getHealth()).append("/").append((int)player.getMaxHealth()).append("\n");
        context.append("Experience Level: ").append(player.experienceLevel).append("\n");
        
        return context.toString();
    }
    
    private static String getTimeOfDay(long time) {
        if (time < 1000) return "dawn";
        else if (time < 6000) return "morning";
        else if (time < 12000) return "day";
        else if (time < 13000) return "dusk";
        else if (time < 18000) return "night";
        else if (time < 22000) return "late night";
        else return "midnight";
    }
    
    /**
     * Response container for AI-generated content
     */
    public static class AIResponse {
        public final boolean success;
        public final String dialogue;
        public final List<String> commands;
        
        public AIResponse(boolean success, String dialogue, List<String> commands) {
            this.success = success;
            this.dialogue = dialogue;
            this.commands = commands;
        }
    }
}
