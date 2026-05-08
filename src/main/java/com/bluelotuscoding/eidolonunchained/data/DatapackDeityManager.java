package com.bluelotuscoding.eidolonunchained.data;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.deity.DatapackDeity;
import com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig;
import com.bluelotuscoding.eidolonunchained.ai.AIDeityManager;
import com.google.gson.*;
import com.mojang.logging.LogUtils;
import elucent.eidolon.api.deity.Deity;
import elucent.eidolon.common.deity.Deities;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import com.bluelotuscoding.eidolonunchained.util.UnifiedDynamicSystemLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;

import java.util.Map;
import java.util.HashMap;

/**
 * Manages loading datapack deity definitions from JSON files.
 * Loads from data/modid/deities/ folder.
 * 
 * Deity JSON structure:
 * {
 *   "id": "modid:deity_name",
 *   "name": "Display Name",
 *   "description": "Description text",
 *   "colors": {"red": 255, "green": 100, "blue": 50},
 *   "progression": {
 *     "max_reputation": 100,
 *     "stages": [...]
 *   },
 *   "unlock_rewards": {...}
 * }
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DatapackDeityManager extends UnifiedDynamicSystemLoader {
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = com.bluelotuscoding.eidolonunchained.util.JsonUtils.GSON;
    private static DatapackDeityManager INSTANCE;
    
    // Store loaded deities
    private static final Map<ResourceLocation, DatapackDeity> deities = new HashMap<>();
    
    public DatapackDeityManager() {
        super(GSON, "deities", "eidolonunchained");
        INSTANCE = this;
    }
    
    public static DatapackDeityManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DatapackDeityManager();
        }
        return INSTANCE;
    }
    
    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(getInstance());
        LOGGER.info("Registered Datapack Deity reload listener");
    }
    
    // Static methods for accessing deities
    public static DatapackDeity getDeity(ResourceLocation id) {
        return deities.get(id);
    }
    
    public static boolean hasDeity(ResourceLocation id) {
        return deities.containsKey(id);
    }
    
    public static Map<ResourceLocation, DatapackDeity> getAllDeities() {
        return new HashMap<>(deities);
    }
    
    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resourceMap,
                        ResourceManager resourceManager, ProfilerFiller profiler) {
        LOGGER.info("Loading datapack deities...");

        // Prepare AI deity manager for a deity reload without wiping already-linked configs.
        // This avoids clearing server-side AI configs before AI configs are re-linked,
        // which caused "AI config not found" behavior during gameplay.
        com.bluelotuscoding.eidolonunchained.ai.AIDeityManager.getInstance().prepareForDeityReload();

        // Use UnifiedDynamicSystemLoader to normalize entries and process each via handleEntry
        deities.clear();
        super.apply(resourceMap, resourceManager, profiler);

        LOGGER.info("Loaded {} datapack deities", deities.size());

        // Notify that deities have been loaded - AI system can now link to them
        MinecraftForge.EVENT_BUS.post(new DatapackDeitiesLoadedEvent(deities));
    }

    @Override
    protected void handleEntry(ResourceLocation location, JsonObject json) throws Exception {
        if (json == null || !json.isJsonObject()) {
            LOGGER.warn("Skipping non-object JSON at {}", location);
            return;
        }
        loadDeity(location, json);
    }
    
    private void loadDeity(ResourceLocation location, JsonObject json) {
        // Parse basic deity information
        String id = json.get("id").getAsString();
        ResourceLocation deityId = ResourceLocation.tryParse(id);
        if (deityId == null) {
            throw new IllegalArgumentException("Invalid deity ID: " + id);
        }
        
        String name = json.get("name").getAsString();
        String description = json.has("description") ? json.get("description").getAsString() : "";
        
        // Parse colors
        JsonObject colors = json.getAsJsonObject("colors");
        int red = colors.get("red").getAsInt();
        int green = colors.get("green").getAsInt();
        int blue = colors.get("blue").getAsInt();
        
        // Create the deity instance
        DatapackDeity deity = new DatapackDeity(deityId, name, description, red, green, blue);
        
        // Load progression system
        if (json.has("progression")) {
            loadProgression(deity, json.getAsJsonObject("progression"));
        }
        
        // Load unlock rewards
        if (json.has("unlock_rewards")) {
            loadUnlockRewards(deity, json.getAsJsonObject("unlock_rewards"));
        }
        
        // Load prayer types (basic structure, AI will enhance these)
        if (json.has("prayer_types")) {
            loadPrayerTypes(deity, json.getAsJsonArray("prayer_types"));
        }
        
        // Load abandon configuration
        if (json.has("abandon")) {
            loadAbandonConfiguration(deity, json.getAsJsonObject("abandon"));
        }

        // Link to a native Eidolon deity for reputation mirroring
        if (json.has("linked_eidolon_deity")) {
            deity.setLinkedEidolonDeity(new ResourceLocation(json.get("linked_eidolon_deity").getAsString()));
        }

        // Optional base deity damage type used by deity-linked combat chants.
        if (json.has("deity_damage_type")) {
            ResourceLocation deityDamageType = ResourceLocation.tryParse(json.get("deity_damage_type").getAsString());
            if (deityDamageType != null) {
                deity.setDeityDamageType(deityDamageType);
            } else {
                LOGGER.warn("Invalid deity_damage_type '{}' for deity {}", json.get("deity_damage_type").getAsString(), deityId);
            }
        }
        
        // Extract and register AI configuration if present
        if (json.has("ai_configuration")) {
            try {
                loadAIConfiguration(deityId, json.getAsJsonObject("ai_configuration"));
                LOGGER.debug("Loaded AI configuration for deity: {}", deityId);
            } catch (Exception e) {
                LOGGER.error("Failed to load AI configuration for deity {}", deityId, e);
            }
        }
        
        // Store in our static map for easy access
        deities.put(deityId, deity);
        
        // Register with Eidolon's deity system IMMEDIATELY
        // This ensures the deity is available in Deities.find() before DatapackDeitiesLoadedEvent fires
        Deities.register(deity);
        
        LOGGER.info("Registered datapack deity: {} ({})", deityId, name);
    }
    
    private void loadProgression(DatapackDeity deity, JsonObject progression) {
        int maxReputation = progression.has("max_reputation") ? 
            progression.get("max_reputation").getAsInt() : 100;
        
        if (progression.has("stages")) {
            JsonArray stages = progression.getAsJsonArray("stages");
            for (JsonElement stageElement : stages) {
                if (!stageElement.isJsonObject()) continue;
                
                JsonObject stage = stageElement.getAsJsonObject();
                String stageId = stage.get("id").getAsString();
                int reputation = stage.get("reputation").getAsInt();
                boolean major = stage.has("major") ? stage.get("major").getAsBoolean() : false;
                
                // Extract and store stage title from JSON
                if (stage.has("title")) {
                    String title = stage.get("title").getAsString();
                    deity.setStageTitle(stageId, title);
                    LOGGER.debug("Set title for stage {}: '{}'", stageId, title);
                }
                
                ResourceLocation stageRL = ResourceLocation.tryParse(stageId);
                if (stageRL == null) {
                    LOGGER.warn("Invalid stage ID: {}", stageId);
                    continue;
                }
                
                // Create stage
                Deity.Stage deityStage = new Deity.Stage(stageRL, reputation, major);
                
                // Add requirements
                if (stage.has("requirements")) {
                    JsonArray requirements = stage.getAsJsonArray("requirements");
                    for (JsonElement reqElement : requirements) {
                        String requirement = reqElement.getAsString();
                        if (requirement.startsWith("research:")) {
                            String researchId = requirement.substring(9);
                            ResourceLocation researchRL = ResourceLocation.tryParse(researchId);
                            if (researchRL != null) {
                                deityStage.requirement(new Deity.ResearchRequirement(researchRL));
                            }
                        } else if (requirement.startsWith("sign:")) {
                            // Handle sign requirements
                            String signName = requirement.substring(5);
                            // Will need to resolve sign from name
                            LOGGER.debug("Sign requirement: {}", signName);
                        }
                    }
                }
                
                // 🎁 PROCESS STAGE REWARDS (THIS WAS MISSING!)
                if (stage.has("rewards")) {
                    JsonArray rewards = stage.getAsJsonArray("rewards");
                    for (JsonElement rewardElement : rewards) {
                        // Handle both formats: objects and direct command strings
                        if (rewardElement.isJsonObject()) {
                            // Original object format: {"type": "item", "data": "...", "count": 1}
                            JsonObject reward = rewardElement.getAsJsonObject();
                            String type = reward.get("type").getAsString();
                            String data = reward.get("data").getAsString();
                            
                            if ("item".equals(type)) {
                                int count = reward.has("count") ? reward.get("count").getAsInt() : 1;
                                deity.addStageReward(stageId, "item", data + ":" + count);
                                LOGGER.debug("Added item reward for stage {}: {}x{}", stageId, count, data);
                            } else if ("effect".equals(type)) {
                                int duration = reward.has("duration") ? reward.get("duration").getAsInt() : 200;
                                int amplifier = reward.has("amplifier") ? reward.get("amplifier").getAsInt() : 0;
                                deity.addStageReward(stageId, "effect", data + ":" + duration + ":" + amplifier);
                                LOGGER.debug("Added effect reward for stage {}: {} {}s level {}", stageId, data, duration/20, amplifier);
                            } else if ("sign".equals(type)) {
                                deity.addStageReward(stageId, "sign", data);
                                LOGGER.debug("Added sign reward for stage {}: {}", stageId, data);
                            }
                        } else if (rewardElement.isJsonPrimitive()) {
                            // New direct command format: "give @s item 1"
                            String command = rewardElement.getAsString();
                            deity.addStageReward(stageId, "command", command);
                            LOGGER.debug("Added command reward for stage {}: {}", stageId, command);
                        }
                    }
                }
                
                deity.addProgressionStage(deityStage);
            }
        }
        
        deity.setMaxReputation(maxReputation);
    }
    
    private void loadUnlockRewards(DatapackDeity deity, JsonObject rewards) {
        for (Map.Entry<String, JsonElement> entry : rewards.entrySet()) {
            String stageId = entry.getKey();
            JsonObject reward = entry.getValue().getAsJsonObject();
            
            // Parse signs
            if (reward.has("signs")) {
                JsonArray signs = reward.getAsJsonArray("signs");
                for (JsonElement signElement : signs) {
                    String signName = signElement.getAsString();
                    deity.addStageReward(stageId, "sign", signName);
                }
            }
            
            // Parse items
            if (reward.has("items")) {
                JsonArray items = reward.getAsJsonArray("items");
                for (JsonElement itemElement : items) {
                    if (itemElement.isJsonObject()) {
                        JsonObject item = itemElement.getAsJsonObject();
                        String itemId = item.get("item").getAsString();
                        int count = item.has("count") ? item.get("count").getAsInt() : 1;
                        deity.addStageReward(stageId, "item", itemId + ":" + count);
                    }
                }
            }
            
            // Parse effects
            if (reward.has("effects")) {
                JsonArray effects = reward.getAsJsonArray("effects");
                for (JsonElement effectElement : effects) {
                    if (effectElement.isJsonObject()) {
                        JsonObject effect = effectElement.getAsJsonObject();
                        String effectId = effect.get("effect").getAsString();
                        int duration = effect.has("duration") ? effect.get("duration").getAsInt() : 200;
                        int amplifier = effect.has("amplifier") ? effect.get("amplifier").getAsInt() : 0;
                        deity.addStageReward(stageId, "effect", effectId + ":" + duration + ":" + amplifier);
                    }
                }
            }
        }
    }
    
    private void loadPrayerTypes(DatapackDeity deity, JsonArray prayerTypes) {
        for (JsonElement typeElement : prayerTypes) {
            String prayerType = typeElement.getAsString();
            deity.addPrayerType(prayerType);
        }
    }
    
    private void loadAbandonConfiguration(DatapackDeity deity, JsonObject abandon) {
        double penalty = abandon.has("reputation_penalty") ? 
            abandon.get("reputation_penalty").getAsDouble() : 1.0; // Default: lose all
        
        boolean resetReputation = abandon.has("reset_reputation") ? 
            abandon.get("reset_reputation").getAsBoolean() : true; // Default: reset to 0
        
        String message = abandon.has("message") ? 
            abandon.get("message").getAsString() : 
            "You have abandoned your patron and lost all divine favor.";
        
        deity.setAbandonConfiguration(penalty, resetReputation, message);
        
        LOGGER.debug("Loaded abandon configuration for {}: penalty={}, reset={}, message='{}'", 
            deity.getId(), penalty, resetReputation, message);
    }
    
    /**
     * Extract AI configuration from consolidated deity JSON and register it with AIDeityManager.
     * This allows single-file deity definitions that include both basic deity data and AI behavior.
     */
    private void loadAIConfiguration(ResourceLocation deityId, JsonObject aiConfig) {
        try {
            // Convert the JSON to an AIDeityConfig object
            AIDeityConfig config = GSON.fromJson(aiConfig, AIDeityConfig.class);

            // WORKAROUND: Manual TTS config parsing if GSON failed
            if (config.tts_config == null && aiConfig.has("tts_config")) {
                try {
                    JsonObject ttsJson = aiConfig.getAsJsonObject("tts_config");
                    config.tts_config = GSON.fromJson(ttsJson, AIDeityConfig.TTSConfig.class);
                } catch (Exception e) {
                    LOGGER.error("Failed to manually parse TTS config for {}: {}", deityId, e.getMessage());
                }
            }

            // Ensure the deity ID matches
            if (config.deity_id == null) {
                config.deity_id = deityId;
            }
            
            // Register with AIDeityManager for unified access
            AIDeityManager.getInstance().registerAIConfig(deityId, config);
            
            LOGGER.debug("Successfully registered AI configuration for deity: {}", deityId);
            
        } catch (Exception e) {
            LOGGER.error("Failed to parse AI configuration for deity {}: {}", deityId, e.getMessage());
            throw e;
        }
    }
    
    // CLIENT-SIDE METHODS FOR MULTIPLAYER SYNC
    
    /**
     * Clears client-side deity data for multiplayer sync
     */
    public static void clearClientDeities() {
        deities.clear();
        LOGGER.info("Cleared client-side deity data for sync");
    }
    
    /**
     * Adds a deity to client-side storage during multiplayer sync
     */
    public static void addClientDeity(ResourceLocation id, DatapackDeity deity) {
        deities.put(id, deity);
        LOGGER.debug("Added client deity: {}", id);
    }
}

/**
 * Event fired when all datapack deities have been loaded.
 * AI system listens for this to link AI configurations to deities.
 */
// Event class moved to separate file DatapackDeitiesLoadedEvent.java
