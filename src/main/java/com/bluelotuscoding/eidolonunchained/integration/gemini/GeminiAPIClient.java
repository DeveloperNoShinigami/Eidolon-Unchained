package com.bluelotuscoding.eidolonunchained.integration.gemini;

import com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig;
import com.bluelotuscoding.eidolonunchained.ai.GenerationConfig;
import com.bluelotuscoding.eidolonunchained.ai.PrayerAIConfig;
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
        if (apiKey == null || apiKey.trim().isEmpty()) {
            LOGGER.error("Gemini API key is null or empty. AI features will not work.");
        }
        
        // Validate and normalize model name
        this.model = validateAndNormalizeModelName(model);
        this.apiKey = apiKey;
        this.timeoutSeconds = timeoutSeconds;
        LOGGER.debug("Created GeminiAPIClient with model: {}, timeout: {}s, apiKey present: {}", 
            this.model, timeoutSeconds, (apiKey != null && !apiKey.trim().isEmpty()));
    }
    
    /**
     * Validate and normalize the Gemini model name
     */
    private String validateAndNormalizeModelName(String model) {
        if (model == null || model.trim().isEmpty()) {
            LOGGER.warn("Model name is empty, defaulting to gemini-1.5-flash");
            return "gemini-1.5-flash";
        }
        
        String normalizedModel = model.toLowerCase().trim();
        
        // Handle common variations and fix them
        if (normalizedModel.contains("gemini") && normalizedModel.contains("2.5")) {
            LOGGER.warn("Gemini 2.5 models don't exist yet, using gemini-1.5-flash instead");
            return "gemini-1.5-flash";
        }
        
        if (normalizedModel.contains("flash") && normalizedModel.contains("lite")) {
            LOGGER.warn("Flash-Lite model name corrected to gemini-1.5-flash");
            return "gemini-1.5-flash";
        }
        
        // Valid model names for Gemini API
        if (normalizedModel.equals("gemini-1.5-flash") || 
            normalizedModel.equals("gemini-1.5-pro") ||
            normalizedModel.equals("gemini-pro") ||
            normalizedModel.equals("gemini-pro-vision")) {
            return normalizedModel;
        }
        
        // Try to fix common model name formats
        if (normalizedModel.contains("1.5") && normalizedModel.contains("flash")) {
            return "gemini-1.5-flash";
        }
        if (normalizedModel.contains("1.5") && normalizedModel.contains("pro")) {
            return "gemini-1.5-pro";
        }
        if (normalizedModel.contains("pro")) {
            return "gemini-pro";
        }
        
        LOGGER.warn("Unknown model name '{}', defaulting to gemini-1.5-flash", model);
        return "gemini-1.5-flash";
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
                String response = sendRequestWithRetry(requestBody);
                return parseResponse(response);
            } catch (IOException e) {
                // Enhanced error handling with specific messages for different scenarios
                if (e.getMessage() != null && e.getMessage().contains("503")) {
                    LOGGER.error("API overloaded even after retries: {}", e.getMessage());
                    return new AIResponse(false, "The divine channels are overwhelmed. Please try again in a moment.", List.of());
                } else if (e.getMessage() != null && e.getMessage().contains("401")) {
                    LOGGER.error("API authentication failed: {}", e.getMessage());
                    return new AIResponse(false, "The divine connection is misconfigured. Please check your sacred keys.", List.of());
                } else if (e.getMessage() != null && e.getMessage().contains("429")) {
                    LOGGER.error("Rate limit exceeded: {}", e.getMessage());
                    return new AIResponse(false, "You have prayed too often. Please wait before seeking divine wisdom again.", List.of());
                } else {
                    LOGGER.error("Failed to generate AI response", e);
                    return new AIResponse(false, "I cannot hear your prayer clearly right now. Please try again later.", List.of());
                }
            } catch (Exception e) {
                LOGGER.error("Unexpected error generating AI response", e);
                return new AIResponse(false, "The divine realm is experiencing disturbances. Please try again later.", List.of());
            }
        }, EXECUTOR);
    }
    
    /**
     * Send request with automatic retry logic for overloaded servers
     */
    private String sendRequestWithRetry(JsonObject requestBody) throws IOException {
        // Get retry configuration from config
        boolean retryEnabled = com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig.COMMON.enableApiRetry.get();
        if (!retryEnabled) {
            return sendRequest(requestBody);
        }
        
        int maxRetries = com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig.COMMON.maxRetryAttempts.get();
        long baseDelayMs = com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig.COMMON.retryBaseDelayMs.get();
        double backoffMultiplier = com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig.COMMON.retryBackoffMultiplier.get();
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return sendRequest(requestBody);
            } catch (IOException e) {
                // Check if this is a 503 (Service Unavailable) error
                boolean isOverloadError = e.getMessage() != null && 
                    (e.getMessage().contains("503") || e.getMessage().toLowerCase().contains("overloaded"));
                
                if (isOverloadError && attempt < maxRetries) {
                    long delayMs = Math.round(baseDelayMs * Math.pow(backoffMultiplier, attempt - 1));
                    LOGGER.warn("API overloaded (attempt {}/{}). Retrying in {}ms...", attempt, maxRetries, delayMs);
                    
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Retry interrupted", ie);
                    }
                } else {
                    // Not an overload error, or we've exhausted retries
                    if (isOverloadError) {
                        LOGGER.error("API still overloaded after {} attempts. Giving up.", maxRetries);
                    }
                    throw e;
                }
            }
        }
        
        throw new IOException("Unexpected error: retry loop should not reach here");
    }
    
    private JsonObject buildRequestBody(
            String prompt, 
            String personality,
            GenerationConfig genConfig,
            SafetySettings safetySettings) {
        
        JsonObject request = new JsonObject();
        
        // Contents array - this is the key format for Gemini API
        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        
        // Set role to 'user' explicitly
        content.addProperty("role", "user");
        
        JsonArray parts = new JsonArray();
        JsonObject textPart = new JsonObject();
        
        // Combine personality and prompt properly
        String fullPrompt = personality + "\n\nHuman: " + prompt + "\n\nAssistant:";
        textPart.addProperty("text", fullPrompt);
        parts.add(textPart);
        
        content.add("parts", parts);
        contents.add(content);
        request.add("contents", contents);
        
        // Generation config - use correct field names for Gemini
        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", genConfig.temperature);
        generationConfig.addProperty("maxOutputTokens", Math.min(genConfig.max_output_tokens, 300));
        generationConfig.addProperty("topP", 0.8);
        generationConfig.addProperty("topK", 10);
        request.add("generationConfig", generationConfig);
        
        // Safety settings - use proper Gemini format
        JsonArray safetyArray = new JsonArray();
        addSafetySetting(safetyArray, "HARM_CATEGORY_HARASSMENT", "BLOCK_MEDIUM_AND_ABOVE");
        addSafetySetting(safetyArray, "HARM_CATEGORY_HATE_SPEECH", "BLOCK_MEDIUM_AND_ABOVE");
        addSafetySetting(safetyArray, "HARM_CATEGORY_SEXUALLY_EXPLICIT", "BLOCK_MEDIUM_AND_ABOVE");
        addSafetySetting(safetyArray, "HARM_CATEGORY_DANGEROUS_CONTENT", "BLOCK_MEDIUM_AND_ABOVE");
        request.add("safetySettings", safetyArray);
        
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
        
        // Debug logging - be more careful with API key masking
        String maskedUrl = urlString.replaceAll("key=[^&]*", "key=***");
        LOGGER.debug("Making API request to: {}", maskedUrl);
        LOGGER.debug("Request body: {}", requestBody.toString());
        
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "EidolonUnchained/1.0");
        connection.setDoOutput(true);
        connection.setConnectTimeout(timeoutSeconds * 1000);
        connection.setReadTimeout(timeoutSeconds * 1000);
        
        // Send request
        String requestBodyStr = requestBody.toString();
        try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8)) {
            writer.write(requestBodyStr);
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
        
        String responseBody = response.toString();
        
        if (responseCode >= 400) {
            LOGGER.error("API request failed. Response code: {}", responseCode);
            LOGGER.error("Request URL: {}", maskedUrl);
            LOGGER.error("Request body: {}", requestBodyStr);
            LOGGER.error("Response body: {}", responseBody);
            
            // Enhanced error handling for different response codes
            if (responseCode == 503) {
                LOGGER.warn("Google Gemini API is currently overloaded (503). This is temporary - retrying...");
                throw new IOException("503 Service Unavailable - API overloaded");
            } else if (responseCode == 429) {
                LOGGER.warn("Rate limit exceeded (429). Response: {}", responseBody);
                throw new IOException("429 Too Many Requests - Rate limit exceeded");
            } else if (responseCode == 401) {
                LOGGER.error("Invalid API key (401). Response: {}", responseBody);
                throw new IOException("401 Unauthorized - Invalid API key");
            } else if (responseCode == 400) {
                LOGGER.error("Bad Request - Check API key validity and request format");
                // Try to extract actual error message from JSON response
                String specificError = "Bad request format";
                try {
                    JsonObject errorResponse = JsonParser.parseString(responseBody).getAsJsonObject();
                    if (errorResponse.has("error")) {
                        JsonObject error = errorResponse.getAsJsonObject("error");
                        specificError = error.has("message") ? error.get("message").getAsString() : "Unknown error";
                        LOGGER.error("API Error: {}", specificError);
                    }
                } catch (Exception e) {
                    LOGGER.error("Could not parse error response as JSON, got HTML error page instead");
                }
                throw new IOException("400 Bad Request - " + specificError);
            } else {
                LOGGER.error("Unexpected API error with code {}: {}", responseCode, responseBody);
                throw new IOException("HTTP " + responseCode + " - " + (responseBody.isEmpty() ? "Unknown error" : responseBody));
            }
        }
        
        LOGGER.debug("API response received successfully, length: {}", responseBody.length());
        return responseBody;
    }
    
    private AIResponse parseResponse(String jsonResponse) {
        try {
            JsonObject response = JsonParser.parseString(jsonResponse).getAsJsonObject();
            
            if (response.has("candidates") && response.getAsJsonArray("candidates").size() > 0) {
                JsonObject candidate = response.getAsJsonArray("candidates").get(0).getAsJsonObject();
                
                // Check for finish reason issues
                if (candidate.has("finishReason")) {
                    String finishReason = candidate.get("finishReason").getAsString();
                    if ("MAX_TOKENS".equals(finishReason)) {
                        LOGGER.warn("AI response truncated due to token limit");
                        return new AIResponse(false, "The deity's message was too complex. Please try a simpler prayer.", List.of());
                    }
                    if ("SAFETY".equals(finishReason)) {
                        LOGGER.warn("AI response blocked by safety filters");
                        return new AIResponse(false, "The deity does not wish to discuss such matters.", List.of());
                    }
                }
                
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
     * Build comprehensive context information for AI prompts with player status analysis
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
        
        // Health and status analysis
        float health = player.getHealth();
        float maxHealth = player.getMaxHealth();
        float healthPercentage = (health / maxHealth) * 100;
        context.append("Health: ").append((int)health).append("/").append((int)maxHealth);
        
        String healthStatus = "";
        if (healthPercentage <= 25) {
            healthStatus = " (CRITICALLY INJURED)";
        } else if (healthPercentage <= 50) {
            healthStatus = " (BADLY HURT)";
        } else if (healthPercentage <= 75) {
            healthStatus = " (WOUNDED)";
        }
        context.append(healthStatus).append("\n");
        
        // Food level analysis
        int foodLevel = player.getFoodData().getFoodLevel();
        float saturation = player.getFoodData().getSaturationLevel();
        context.append("Hunger: ").append(foodLevel).append("/20");
        
        String hungerStatus = "";
        if (foodLevel <= 6) {
            hungerStatus = " (STARVING)";
        } else if (foodLevel <= 12) {
            hungerStatus = " (HUNGRY)";
        } else if (foodLevel <= 17) {
            hungerStatus = " (PECKISH)";
        }
        context.append(hungerStatus).append("\n");
        
        // Experience Level
        context.append("Experience Level: ").append(player.experienceLevel).append("\n");
        
        // Active effects analysis
        String activeEffects = getActiveEffectsAnalysis(player);
        if (!activeEffects.isEmpty()) {
            context.append("Active Effects: ").append(activeEffects).append("\n");
        }
        
        // Equipment analysis
        String equipmentAnalysis = getEquipmentAnalysis(player);
        if (!equipmentAnalysis.isEmpty()) {
            context.append("Equipment: ").append(equipmentAnalysis).append("\n");
        }
        
        // Inventory summary - highlight notable magical items
        String inventorySummary = getInventorySummary(player);
        if (!inventorySummary.isEmpty()) {
            context.append("Notable Items: ").append(inventorySummary).append("\n");
        }
        
        // Environmental dangers
        String environmentalStatus = getEnvironmentalStatus(player);
        if (!environmentalStatus.isEmpty()) {
            context.append("Environmental Status: ").append(environmentalStatus).append("\n");
        }
        
        // Player needs assessment (AI can use this to offer proactive help)
        String needsAssessment = assessPlayerNeeds(player, healthPercentage, foodLevel);
        if (!needsAssessment.isEmpty()) {
            context.append("Apparent Needs: ").append(needsAssessment).append("\n");
        }
        
        // Ritual history
        String ritualHistory = getRitualHistory(player);
        if (!ritualHistory.isEmpty()) {
            context.append("Recent Rituals: ").append(ritualHistory).append("\n");
        }
        
        return context.toString();
    }
    
    /**
     * Build enhanced context with real-time world data and universal AI guidance
     */
    public static String buildEnhancedPlayerContext(ServerPlayer player, PrayerAIConfig prayerConfig) {
        StringBuilder context = new StringBuilder();
        
        // Start with comprehensive world analysis instead of basic context
        context.append(com.bluelotuscoding.eidolonunchained.integration.world.AdvancedWorldAnalyzer.buildComprehensiveWorldContext(player));
        
        // Add universal AI behavior guidance
        context.append("\n=== AI BEHAVIOR GUIDANCE ===\n");
        context.append(com.bluelotuscoding.eidolonunchained.ai.UniversalAIPrompts.UNIVERSAL_BASE_PROMPT);
        context.append("\n");
        context.append(com.bluelotuscoding.eidolonunchained.ai.UniversalAIPrompts.COMMAND_GUIDANCE);
        
        // Add command markup instructions
        context.append("\n=== COMMAND EXECUTION SYSTEM ===\n");
        context.append(com.bluelotuscoding.eidolonunchained.integration.ai.AIResponseProcessor.getCommandMarkupGuide());
        
        // Add prayer-specific configuration if available
        if (prayerConfig != null) {
            context.append("\n=== PRAYER CONFIGURATION ===\n");
            if (!prayerConfig.referenceCommands.isEmpty()) {
                context.append("Available Reference Commands:\n");
                for (String refCmd : prayerConfig.referenceCommands) {
                    context.append("- ").append(refCmd).append("\n");
                }
                context.append("Remember: NEVER show these commands to players. Use them silently with immersive messages.\n");
            }
            
            if (!prayerConfig.additionalPrompts.isEmpty()) {
                context.append("\nAdditional Guidance:\n");
                for (String prompt : prayerConfig.additionalPrompts) {
                    context.append(prompt).append("\n");
                }
            }
        }
        
        return context.toString();
    }
    
    private static String getPlayerHealthAnalysis(ServerPlayer player) {
        StringBuilder analysis = new StringBuilder();
        
        float health = player.getHealth();
        float maxHealth = player.getMaxHealth();
        float healthPercentage = (health / maxHealth) * 100;
        
        analysis.append("Health Status: ").append(String.format("%.1f", health)).append("/").append(String.format("%.1f", maxHealth));
        
        if (healthPercentage <= 25) {
            analysis.append(" (CRITICAL - needs immediate healing)\n");
        } else if (healthPercentage <= 50) {
            analysis.append(" (INJURED - would benefit from healing)\n");
        } else if (healthPercentage <= 75) {
            analysis.append(" (WOUNDED - minor healing helpful)\n");
        } else {
            analysis.append(" (HEALTHY)\n");
        }
        
        return analysis.toString();
    }
    
    private static String getPlayerInventoryAnalysis(ServerPlayer player) {
        StringBuilder analysis = new StringBuilder();
        
        int emptySlots = 0;
        int totalSlots = player.getInventory().getContainerSize();
        
        for (int i = 0; i < totalSlots; i++) {
            if (player.getInventory().getItem(i).isEmpty()) {
                emptySlots++;
            }
        }
        
        analysis.append("Inventory: ").append(totalSlots - emptySlots).append("/").append(totalSlots).append(" slots used");
        
        if (emptySlots <= 3) {
            analysis.append(" (FULL - may not have space for gifts)\n");
        } else if (emptySlots <= 9) {
            analysis.append(" (MOSTLY FULL - limited space)\n");
        } else {
            analysis.append(" (SPACE AVAILABLE)\n");
        }
        
        return analysis.toString();
    }
    
    private static String getPlayerEffectsAnalysis(ServerPlayer player) {
        StringBuilder analysis = new StringBuilder();
        
        var activeEffects = player.getActiveEffects();
        if (activeEffects.isEmpty()) {
            analysis.append("Active Effects: None\n");
        } else {
            analysis.append("Active Effects: ");
            activeEffects.forEach(effect -> {
                String effectName = effect.getEffect().getDescriptionId();
                int duration = effect.getDuration();
                int amplifier = effect.getAmplifier();
                
                analysis.append(effectName).append(" (").append(duration).append("t, lvl ").append(amplifier + 1).append("), ");
            });
            analysis.append("\n");
        }
        
        return analysis.toString();
    }
    
    /**
     * Get active effects analysis for context
     */
    private static String getActiveEffectsAnalysis(ServerPlayer player) {
        java.util.List<String> effects = new java.util.ArrayList<>();
        
        player.getActiveEffects().forEach(effectInstance -> {
            String effectName = effectInstance.getEffect().getDisplayName().getString();
            int amplifier = effectInstance.getAmplifier();
            int duration = effectInstance.getDuration();
            
            // Only include notable effects (positive, negative, or magical)
            if (effectInstance.getEffect().isBeneficial() || !effectInstance.getEffect().isBeneficial()) {
                String effectInfo = effectName;
                if (amplifier > 0) {
                    effectInfo += " " + (amplifier + 1);
                }
                if (duration < 1200) { // Less than 1 minute
                    effectInfo += " (expiring soon)";
                }
                effects.add(effectInfo);
            }
        });
        
        return String.join(", ", effects);
    }
    
    /**
     * Analyze player's equipment status
     */
    private static String getEquipmentAnalysis(ServerPlayer player) {
        java.util.List<String> equipmentStatus = new java.util.ArrayList<>();
        
        // Check armor
        int armorCount = 0;
        boolean hasEnchantedArmor = false;
        for (net.minecraft.world.item.ItemStack armorPiece : player.getArmorSlots()) {
            if (!armorPiece.isEmpty()) {
                armorCount++;
                if (armorPiece.isEnchanted()) {
                    hasEnchantedArmor = true;
                }
            }
        }
        
        if (armorCount == 0) {
            equipmentStatus.add("no armor (vulnerable)");
        } else if (armorCount < 4) {
            equipmentStatus.add("partial armor");
        } else {
            equipmentStatus.add(hasEnchantedArmor ? "full enchanted armor" : "full armor");
        }
        
        // Check main hand weapon
        net.minecraft.world.item.ItemStack mainHand = player.getMainHandItem();
        if (!mainHand.isEmpty()) {
            String itemName = mainHand.getDisplayName().getString();
            if (mainHand.isEnchanted()) {
                equipmentStatus.add("enchanted " + itemName.toLowerCase());
            } else if (isWeaponItem(mainHand)) {
                equipmentStatus.add(itemName.toLowerCase());
            }
        } else {
            equipmentStatus.add("bare hands (defenseless)");
        }
        
        return String.join(", ", equipmentStatus);
    }
    
    /**
     * Check if item is a weapon
     */
    private static boolean isWeaponItem(net.minecraft.world.item.ItemStack stack) {
        String itemId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
        return itemId.contains("sword") || itemId.contains("axe") || itemId.contains("bow") || 
               itemId.contains("crossbow") || itemId.contains("trident") || itemId.contains("wand");
    }
    
    /**
     * Assess environmental status and dangers
     */
    private static String getEnvironmentalStatus(ServerPlayer player) {
        java.util.List<String> environmental = new java.util.ArrayList<>();
        
        // Check if player is in dangerous locations
        if (player.isInLava()) {
            environmental.add("IN LAVA");
        }
        if (player.isUnderWater() && player.getAirSupply() < player.getMaxAirSupply()) {
            environmental.add("drowning");
        }
        if (player.isOnFire()) {
            environmental.add("on fire");
        }
        
        // Check dimension
        String dimension = player.level().dimension().location().toString();
        if (dimension.equals("minecraft:the_nether")) {
            environmental.add("in the Nether");
        } else if (dimension.equals("minecraft:the_end")) {
            environmental.add("in the End");
        }
        
        // Check height dangers
        int y = player.blockPosition().getY();
        if (y < 10) {
            environmental.add("very deep underground");
        } else if (y > 200) {
            environmental.add("very high up");
        }
        
        return String.join(", ", environmental);
    }
    
    /**
     * Assess what the player might need based on their current state
     */
    private static String assessPlayerNeeds(ServerPlayer player, float healthPercentage, int foodLevel) {
        java.util.List<String> needs = new java.util.ArrayList<>();
        
        // Health needs
        if (healthPercentage <= 25) {
            needs.add("URGENT HEALING");
        } else if (healthPercentage <= 50) {
            needs.add("healing");
        }
        
        // Food needs
        if (foodLevel <= 6) {
            needs.add("URGENT FOOD");
        } else if (foodLevel <= 12) {
            needs.add("food");
        }
        
        // Equipment needs
        boolean hasArmor = false;
        for (net.minecraft.world.item.ItemStack armorPiece : player.getArmorSlots()) {
            if (!armorPiece.isEmpty()) {
                hasArmor = true;
                break;
            }
        }
        if (!hasArmor) {
            needs.add("protection/armor");
        }
        
        // Weapon needs
        net.minecraft.world.item.ItemStack mainHand = player.getMainHandItem();
        if (mainHand.isEmpty() || !isWeaponItem(mainHand)) {
            needs.add("weapons");
        }
        
        // Environmental protection needs
        if (player.isOnFire() || player.isInLava()) {
            needs.add("fire protection");
        }
        
        return String.join(", ", needs);
    }

    /**
     * Get a summary of notable items in player's inventory
     */
    private static String getInventorySummary(ServerPlayer player) {
        StringBuilder summary = new StringBuilder();
        java.util.List<String> notableItems = new java.util.ArrayList<>();
        
        // Check main inventory and hotbar
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            net.minecraft.world.item.ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && isNotableItem(stack)) {
                String itemName = stack.getDisplayName().getString();
                int count = stack.getCount();
                if (count > 1) {
                    notableItems.add(itemName + " x" + count);
                } else {
                    notableItems.add(itemName);
                }
                
                // Limit to prevent token overflow
                if (notableItems.size() >= 8) break;
            }
        }
        
        return String.join(", ", notableItems);
    }
    
    /**
     * Check if an item is notable for AI context
     */
    private static boolean isNotableItem(net.minecraft.world.item.ItemStack stack) {
        String itemId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
        
        // Magical/ritual items
        if (itemId.contains("eidolon") || itemId.contains("soul") || itemId.contains("crystal") || 
            itemId.contains("wand") || itemId.contains("ritual") || itemId.contains("altar") ||
            itemId.contains("rune") || itemId.contains("enchant") || itemId.contains("potion")) {
            return true;
        }
        
        // Valuable materials
        if (itemId.contains("diamond") || itemId.contains("emerald") || itemId.contains("gold") || 
            itemId.contains("netherite") || itemId.contains("nether_star")) {
            return true;
        }
        
        // Enchanted items
        return stack.isEnchanted();
    }
    
    /**
     * Get recent ritual/chant history for this player
     */
    private static String getRitualHistory(ServerPlayer player) {
        try {
            return com.bluelotuscoding.eidolonunchained.ai.PlayerContextTracker.getRitualHistorySummary(player);
        } catch (Exception e) {
            return "";
        }
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
