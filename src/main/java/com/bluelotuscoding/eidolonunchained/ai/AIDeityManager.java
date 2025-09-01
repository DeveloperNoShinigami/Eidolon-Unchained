package com.bluelotuscoding.eidolonunchained.ai;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.data.DatapackDeitiesLoadedEvent;
import com.bluelotuscoding.eidolonunchained.deity.DatapackDeity;
import com.google.gson.*;
import com.mojang.logging.LogUtils;
import elucent.eidolon.api.deity.Deity;
import elucent.eidolon.common.deity.Deities;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages AI configurations that link to existing datapack deities.
 * Loads from data/modid/ai_deities/ folder.
 * 
 * AI Configuration JSON structure:
 * {
 *   "deity_id": "modid:deity_name",
 *   "ai_provider": "gemini",
 *   "model": "gemini-1.5-pro", 
 *   "personality": "You are...",
 *   "behavior_rules": {
 *     "reputation_thresholds": {...},
 *     "research_requirements": {...},
 *     "dynamic_responses": {...}
 *   },
 *   "prayer_configs": {...}
 * }
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AIDeityManager extends SimpleJsonResourceReloadListener {
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static AIDeityManager INSTANCE;
    
    // Map deity IDs to their AI configurations
    private final Map<ResourceLocation, AIDeityConfig> aiConfigs = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, JsonObject> pendingConfigs = new ConcurrentHashMap<>();
    private boolean deitiesLoaded = false;
    
    public AIDeityManager() {
        super(GSON, "ai_deities");
        INSTANCE = this;
    }
    
    public static AIDeityManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AIDeityManager();
        }
        return INSTANCE;
    }
    
    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(getInstance());
        LOGGER.info("Registered AI Deity reload listener");
    }
    
    @SubscribeEvent
    public static void onDeitiesLoaded(DatapackDeitiesLoadedEvent event) {
        getInstance().deitiesLoaded = true;
        LOGGER.info("Deities loaded, linking AI configurations...");
        getInstance().linkPendingConfigs();
    }
    
    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resourceMap,
                        ResourceManager resourceManager, ProfilerFiller profiler) {
        LOGGER.info("Loading AI deity configurations...");
        
        aiConfigs.clear();
        pendingConfigs.clear();
        int loaded = 0;
        int errors = 0;
        
        for (Map.Entry<ResourceLocation, JsonElement> entry : resourceMap.entrySet()) {
            ResourceLocation location = entry.getKey();
            JsonElement element = entry.getValue();
            
            if (!element.isJsonObject()) {
                LOGGER.warn("Skipping non-object JSON at {}", location);
                continue;
            }
            
            try {
                JsonObject json = element.getAsJsonObject();
                
                // Get the deity this AI config applies to
                if (!json.has("deity")) {
                    LOGGER.error("AI config {} is missing required 'deity' field", location);
                    continue;
                }
                
                JsonElement deityIdElement = json.get("deity");
                if (deityIdElement == null || deityIdElement.isJsonNull()) {
                    LOGGER.error("AI config {} has null 'deity' field", location);
                    continue;
                }
                
                String deityIdString = deityIdElement.getAsString();
                ResourceLocation deityId = ResourceLocation.tryParse(deityIdString);
                if (deityId == null) {
                    throw new IllegalArgumentException("Invalid deity ID: " + deityIdString);
                }
                
                // Store for later linking when deities are loaded
                pendingConfigs.put(deityId, json);
                loaded++;
                
                LOGGER.debug("Queued AI config for deity: {}", deityId);
                
            } catch (Exception e) {
                LOGGER.error("Failed to load AI config from {}", location, e);
                errors++;
            }
        }
        
        LOGGER.info("Queued {} AI deity configurations with {} errors", loaded, errors);
        
        // If deities are already loaded, link immediately
        if (deitiesLoaded) {
            linkPendingConfigs();
        }
    }
    
    private void loadAIConfig(ResourceLocation location, JsonObject json) {
        // Get the deity this AI config applies to
        String deityIdString = json.get("deity_id").getAsString();
        ResourceLocation deityId = ResourceLocation.tryParse(deityIdString);
        if (deityId == null) {
            throw new IllegalArgumentException("Invalid deity ID: " + deityIdString);
        }
        
        // Verify the deity exists
        Deity deity = Deities.find(deityId);
        if (deity == null) {
            LOGGER.warn("AI config references non-existent deity: {}", deityId);
            return;
        }
        
        if (!(deity instanceof DatapackDeity)) {
            LOGGER.warn("AI config can only be applied to DatapackDeity instances: {}", deityId);
            return;
        }
        
        // Parse AI configuration
        AIDeityConfig config = new AIDeityConfig();
        config.deityId = deityId;
        config.aiProvider = json.get("ai_provider").getAsString();
        config.model = json.get("model").getAsString();
        config.personality = json.get("personality").getAsString();
        
        // Parse behavior rules
        if (json.has("behavior_rules")) {
            loadBehaviorRules(config, json.getAsJsonObject("behavior_rules"));
        }
        
        // Parse prayer configurations
        if (json.has("prayer_configs")) {
            loadPrayerConfigs(config, json.getAsJsonObject("prayer_configs"));
        }
        
        // Parse API settings
        if (json.has("api_settings")) {
            loadAPISettings(config, json.getAsJsonObject("api_settings"));
        }
        
        // Store the configuration
        aiConfigs.put(deityId, config);
        LOGGER.info("Loaded AI configuration for deity: {}", deityId);
    }
    
    /**
     * Links pending AI configurations to loaded deities
     */
    private void linkPendingConfigs() {
        LOGGER.info("Linking {} pending AI configurations to loaded deities", pendingConfigs.size());
        LOGGER.info("Available deities: {}", Deities.getDeities().stream()
            .map(d -> d.getId()).toList());
        
        int linked = 0;
        int failed = 0;
        
        for (Map.Entry<ResourceLocation, JsonObject> entry : pendingConfigs.entrySet()) {
            ResourceLocation deityId = entry.getKey();
            JsonObject json = entry.getValue();
            
            try {
                // Verify the deity exists now that deities are loaded
                Deity deity = Deities.find(deityId);
                LOGGER.info("Looking for deity {} - found: {}", deityId, deity != null);
                if (deity == null) {
                    LOGGER.warn("AI config references non-existent deity: {} (available: {})", 
                        deityId, Deities.getDeities().stream().map(d -> d.getId()).toList());
                    failed++;
                    continue;
                }
                
                if (!(deity instanceof DatapackDeity)) {
                    LOGGER.warn("AI config can only be applied to DatapackDeity instances: {} (type: {})", 
                        deityId, deity.getClass().getSimpleName());
                    failed++;
                    continue;
                }
                
                // Parse and store AI configuration
                AIDeityConfig config = new AIDeityConfig();
                config.deityId = deityId;
                config.aiProvider = json.get("ai_provider").getAsString();
                // Model field is optional - some providers like Player2AI don't need it
                config.model = json.has("model") ? json.get("model").getAsString() : null;
                config.personality = json.get("personality").getAsString();
                
                // Parse behavior rules
                if (json.has("behavior_rules")) {
                    loadBehaviorRules(config, json.getAsJsonObject("behavior_rules"));
                }
                
                // Parse prayer configurations
                if (json.has("prayer_configs")) {
                    loadPrayerConfigs(config, json.getAsJsonObject("prayer_configs"));
                }
                
                // Parse API settings
                if (json.has("api_settings")) {
                    loadAPISettings(config, json.getAsJsonObject("api_settings"));
                }
                
                // Store the configuration
                aiConfigs.put(deityId, config);
                linked++;
                LOGGER.info("Successfully linked AI configuration for deity: {}", deityId);
                
            } catch (Exception e) {
                LOGGER.error("Failed to link AI config for deity: {}", deityId, e);
                failed++;
            }
        }
        
        LOGGER.info("Successfully linked {} AI configurations, {} failed", linked, failed);
        pendingConfigs.clear();
    }
    
    private void loadBehaviorRules(AIDeityConfig config, JsonObject rules) {
        // Reputation-based behavior changes
        if (rules.has("reputation_thresholds")) {
            JsonObject thresholds = rules.getAsJsonObject("reputation_thresholds");
            for (Map.Entry<String, JsonElement> entry : thresholds.entrySet()) {
                int threshold = Integer.parseInt(entry.getKey());
                String behavior = entry.getValue().getAsString();
                config.addReputationBehavior(threshold, behavior);
            }
        }
        
        // Research count requirements
        if (rules.has("research_requirements")) {
            JsonObject requirements = rules.getAsJsonObject("research_requirements");
            for (Map.Entry<String, JsonElement> entry : requirements.entrySet()) {
                int researchCount = Integer.parseInt(entry.getKey());
                String behavior = entry.getValue().getAsString();
                config.addResearchBehavior(researchCount, behavior);
            }
        }
        
        // Dynamic personality shifts
        if (rules.has("personality_shifts")) {
            JsonObject shifts = rules.getAsJsonObject("personality_shifts");
            for (Map.Entry<String, JsonElement> entry : shifts.entrySet()) {
                String condition = entry.getKey();
                String personality = entry.getValue().getAsString();
                config.addPersonalityShift(condition, personality);
            }
        }
        
        // Blessing behavior rules
        if (rules.has("blessings")) {
            JsonObject blessings = rules.getAsJsonObject("blessings");
            for (Map.Entry<String, JsonElement> entry : blessings.entrySet()) {
                String condition = entry.getKey();
                String behavior = entry.getValue().getAsString();
                config.addBlessingBehavior(condition, behavior);
            }
        }
        
        // Curse behavior rules
        if (rules.has("curses")) {
            JsonObject curses = rules.getAsJsonObject("curses");
            for (Map.Entry<String, JsonElement> entry : curses.entrySet()) {
                String condition = entry.getKey();
                String behavior = entry.getValue().getAsString();
                config.addCurseBehavior(condition, behavior);
            }
        }
        
        // Gift behavior rules  
        if (rules.has("gifts")) {
            JsonObject gifts = rules.getAsJsonObject("gifts");
            for (Map.Entry<String, JsonElement> entry : gifts.entrySet()) {
                String condition = entry.getKey();
                String behavior = entry.getValue().getAsString();
                config.addGiftBehavior(condition, behavior);
            }
        }
        
        // Time-based behaviors
        if (rules.has("time_behaviors")) {
            JsonObject timeBehaviors = rules.getAsJsonObject("time_behaviors");
            for (Map.Entry<String, JsonElement> entry : timeBehaviors.entrySet()) {
                String timeCondition = entry.getKey(); // e.g., "dawn", "midnight"
                String behavior = entry.getValue().getAsString();
                config.addTimeBehavior(timeCondition, behavior);
            }
        }
        
        // Biome-specific behaviors
        if (rules.has("biome_behaviors")) {
            JsonObject biomeBehaviors = rules.getAsJsonObject("biome_behaviors");
            for (Map.Entry<String, JsonElement> entry : biomeBehaviors.entrySet()) {
                String biome = entry.getKey();
                String behavior = entry.getValue().getAsString();
                config.addBiomeBehavior(biome, behavior);
            }
        }
    }
    
    private void loadPrayerConfigs(AIDeityConfig config, JsonObject prayers) {
        for (Map.Entry<String, JsonElement> entry : prayers.entrySet()) {
            String prayerType = entry.getKey();
            JsonObject prayerConfig = entry.getValue().getAsJsonObject();
            
            PrayerAIConfig prayer = new PrayerAIConfig();
            prayer.type = prayerType;
            
            // Base prompt is optional, use default if not present
            if (prayerConfig.has("base_prompt")) {
                prayer.base_prompt = prayerConfig.get("base_prompt").getAsString();
                prayer.basePrompt = prayer.base_prompt; // Keep both for compatibility
            } else {
                // Use a sensible default based on prayer type
                prayer.base_prompt = generateDefaultPrompt(prayerType);
                prayer.basePrompt = prayer.base_prompt; // Keep both for compatibility
            }
            
            if (prayerConfig.has("max_commands")) {
                prayer.maxCommands = prayerConfig.get("max_commands").getAsInt();
                prayer.max_commands = prayer.maxCommands; // Keep both for compatibility
            }
            
            if (prayerConfig.has("cooldown_minutes")) {
                prayer.cooldownMinutes = prayerConfig.get("cooldown_minutes").getAsInt();
            }
            
            if (prayerConfig.has("reputation_required")) {
                prayer.reputationRequired = prayerConfig.get("reputation_required").getAsInt();
            }
            
            if (prayerConfig.has("allowed_commands")) {
                JsonArray commands = prayerConfig.getAsJsonArray("allowed_commands");
                prayer.allowedCommands.clear(); // Clear defaults
                prayer.allowed_commands.clear(); // Clear defaults
                for (JsonElement cmd : commands) {
                    prayer.allowedCommands.add(cmd.getAsString());
                    prayer.allowed_commands.add(cmd.getAsString()); // Keep both for compatibility
                }
            }
            
            // Load additional prompts for extra developer guidance
            if (prayerConfig.has("additional_prompts")) {
                JsonArray prompts = prayerConfig.getAsJsonArray("additional_prompts");
                prayer.additionalPrompts.clear();
                prayer.additional_prompts.clear();
                for (JsonElement prompt : prompts) {
                    prayer.additionalPrompts.add(prompt.getAsString());
                    prayer.additional_prompts.add(prompt.getAsString());
                }
            }
            
            // Load reference commands for AI templates
            if (prayerConfig.has("reference_commands")) {
                JsonArray refCmds = prayerConfig.getAsJsonArray("reference_commands");
                prayer.referenceCommands.clear(); // Clear defaults
                prayer.reference_commands.clear(); // Clear defaults
                for (JsonElement cmd : refCmds) {
                    prayer.referenceCommands.add(cmd.getAsString());
                    prayer.reference_commands.add(cmd.getAsString());
                }
            }
            
            if (prayerConfig.has("auto_judge_commands")) {
                prayer.autoJudgeCommands = prayerConfig.get("auto_judge_commands").getAsBoolean();
            }
            
            if (prayerConfig.has("judgment_config")) {
                loadJudgmentConfig(prayer.judgmentConfig, prayerConfig.getAsJsonObject("judgment_config"));
            }
            
            config.addPrayerConfig(prayer);
        }
    }
    
    private void loadJudgmentConfig(JudgmentConfig judgmentConfig, JsonObject judgmentJson) {
        if (judgmentJson.has("blessing_threshold")) {
            judgmentConfig.blessingThreshold = judgmentJson.get("blessing_threshold").getAsInt();
        }
        
        if (judgmentJson.has("curse_threshold")) {
            judgmentConfig.curseThreshold = judgmentJson.get("curse_threshold").getAsInt();
        }
        
        if (judgmentJson.has("blessing_commands")) {
            judgmentConfig.blessingCommands.clear();
            JsonArray commands = judgmentJson.getAsJsonArray("blessing_commands");
            for (JsonElement cmd : commands) {
                judgmentConfig.blessingCommands.add(cmd.getAsString());
            }
        }
        
        if (judgmentJson.has("curse_commands")) {
            judgmentConfig.curseCommands.clear();
            JsonArray commands = judgmentJson.getAsJsonArray("curse_commands");
            for (JsonElement cmd : commands) {
                judgmentConfig.curseCommands.add(cmd.getAsString());
            }
        }
        
        if (judgmentJson.has("neutral_commands")) {
            judgmentConfig.neutralCommands.clear();
            JsonArray commands = judgmentJson.getAsJsonArray("neutral_commands");
            for (JsonElement cmd : commands) {
                judgmentConfig.neutralCommands.add(cmd.getAsString());
            }
        }
    }
    
    private void loadAPISettings(AIDeityConfig config, JsonObject apiSettings) {
        if (apiSettings.has("api_key_env")) {
            config.apiKeyEnv = apiSettings.get("api_key_env").getAsString();
        }
        
        if (apiSettings.has("model")) {
            config.apiSettings.model = apiSettings.get("model").getAsString();
        }
        
        if (apiSettings.has("timeout_seconds")) {
            config.timeoutSeconds = apiSettings.get("timeout_seconds").getAsInt();
        }
        
        if (apiSettings.has("safety_settings")) {
            JsonObject safety = apiSettings.getAsJsonObject("safety_settings");
            for (Map.Entry<String, JsonElement> entry : safety.entrySet()) {
                String category = entry.getKey();
                String threshold = entry.getValue().getAsString();
                config.addSafetySetting(category, threshold);
            }
        }
        
        if (apiSettings.has("generation_config")) {
            JsonObject genConfig = apiSettings.getAsJsonObject("generation_config");
            if (genConfig.has("temperature")) {
                config.temperature = genConfig.get("temperature").getAsFloat();
            }
            if (genConfig.has("max_output_tokens")) {
                config.maxOutputTokens = genConfig.get("max_output_tokens").getAsInt();
            }
        }
    }
    
    /**
     * Get AI configuration for a specific deity
     */
    public AIDeityConfig getAIConfig(ResourceLocation deityId) {
        return aiConfigs.get(deityId);
    }
    
    /**
     * Check if a deity has AI enabled
     */
    public boolean hasAI(ResourceLocation deityId) {
        return aiConfigs.containsKey(deityId);
    }
    
    /**
     * Get all AI-enabled deity IDs
     */
    public java.util.Set<ResourceLocation> getAIEnabledDeities() {
        return aiConfigs.keySet();
    }
    
    /**
     * Get all AI configurations
     */
    public java.util.Collection<AIDeityConfig> getAllConfigs() {
        return aiConfigs.values();
    }
    
    /**
     * Performance optimization: Check if ANY deity has ritual integration
     * Used to avoid expensive iteration when no ritual integration exists
     */
    public boolean hasAnyRitualIntegration() {
        return aiConfigs.values().stream()
            .anyMatch(config -> config.ritual_integration != null && !config.ritual_integration.isEmpty());
    }
    
    /**
     * Register an AI configuration programmatically.
     * Used by DatapackDeityManager for consolidated deity files.
     */
    public void registerAIConfig(ResourceLocation deityId, AIDeityConfig config) {
        if (config == null) {
            LOGGER.warn("Attempted to register null AI config for deity: {}", deityId);
            return;
        }
        
        // Ensure deity ID is set correctly
        config.deityId = deityId;
        
        // Store the configuration
        aiConfigs.put(deityId, config);
        
        LOGGER.debug("Registered AI configuration for deity: {}", deityId);
    }
    
    /**
     * Generate a default prompt for a prayer type
     */
    private String generateDefaultPrompt(String prayerType) {
        return switch (prayerType.toLowerCase()) {
            case "conversation" -> "Player {player} approaches your sacred altar and speaks with you. They have {reputation} reputation with you. Respond as the deity in character, considering their standing and the current context.";
            case "blessing" -> "Player {player} requests your blessing. Their reputation with you is {reputation}. Consider their worthiness and respond with appropriate divine favor or guidance.";
            case "knowledge" -> "Player {player} seeks wisdom and knowledge from you. They have {reputation} reputation. Share divine insights appropriate to their standing with you.";
            case "guidance" -> "Player {player} asks for your guidance on their mystical journey. Their reputation: {reputation}. Offer wisdom befitting your divine nature.";
            case "ritual", "nature_ritual" -> "Player {player} wishes to perform a sacred ritual. Their standing with you: {reputation}. Guide them through an appropriate ceremonial experience.";
            case "balance" -> "Player {player} seeks to restore balance in their life or surroundings. Reputation: {reputation}. Help them achieve harmony through your divine power.";
            default -> "Player {player} prays to you seeking {prayer_type}. Their reputation with you is {reputation}. Respond as befits your divine nature and their standing.";
        };
    }
}
