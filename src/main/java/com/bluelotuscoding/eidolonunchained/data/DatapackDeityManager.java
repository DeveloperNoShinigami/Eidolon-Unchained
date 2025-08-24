package com.bluelotuscoding.eidolonunchained.data;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.deity.DatapackDeity;
import com.google.gson.*;
import com.mojang.logging.LogUtils;
import elucent.eidolon.api.deity.Deity;
import elucent.eidolon.common.deity.Deities;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
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
public class DatapackDeityManager extends SimpleJsonResourceReloadListener {
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static DatapackDeityManager INSTANCE;
    
    // Store loaded deities
    private static final Map<ResourceLocation, DatapackDeity> deities = new HashMap<>();
    
    public DatapackDeityManager() {
        super(GSON, "deities");
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
                loadDeity(location, element.getAsJsonObject());
                loaded++;
            } catch (Exception e) {
                LOGGER.error("Failed to load deity from {}", location, e);
                errors++;
            }
        }
        
        LOGGER.info("Loaded {} datapack deities with {} errors", loaded, errors);
        
        // Notify that deities have been loaded - AI system can now link to them
        MinecraftForge.EVENT_BUS.post(new DatapackDeitiesLoadedEvent(deities));
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
        
        // Register with Eidolon's deity system
        Deities.register(deity);
        
        // Store in our static map for easy access
        deities.put(deityId, deity);
        
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
}

/**
 * Event fired when all datapack deities have been loaded.
 * AI system listens for this to link AI configurations to deities.
 */
// Event class moved to separate file DatapackDeitiesLoadedEvent.java
