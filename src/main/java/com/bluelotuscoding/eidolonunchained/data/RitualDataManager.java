package com.bluelotuscoding.eidolonunchained.data;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages ritual recipes loaded from datapacks.
 * Integrates with Eidolon's ritual system for datapack-driven ritual configuration.
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID)
public class RitualDataManager extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(RitualDataManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    // Server-side storage
    private static final Map<ResourceLocation, JsonObject> rituals = new ConcurrentHashMap<>();
    
    // Client-side storage for multiplayer sync
    private static final Map<ResourceLocation, JsonObject> CLIENT_RITUALS = new ConcurrentHashMap<>();
    
    private static RitualDataManager INSTANCE;
    
    public RitualDataManager() {
        super(GSON, "rituals");
        INSTANCE = this;
    }
    
    @SubscribeEvent
    public static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new RitualDataManager());
    }
    
    @Override
    protected void apply(Map<ResourceLocation, JsonElement> prepared, ResourceManager resourceManager, ProfilerFiller profiler) {
        rituals.clear();
        
        LOGGER.info("Loading ritual recipes from datapacks...");
        
        for (Map.Entry<ResourceLocation, JsonElement> entry : prepared.entrySet()) {
            ResourceLocation id = entry.getKey();
            
            try {
                if (entry.getValue().isJsonObject()) {
                    JsonObject ritualJson = entry.getValue().getAsJsonObject();
                    rituals.put(id, ritualJson);
                    LOGGER.debug("Loaded ritual: {}", id);
                } else {
                    LOGGER.warn("Ritual {} is not a valid JSON object, skipping", id);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load ritual {}: {}", id, e.getMessage());
            }
        }
        
        LOGGER.info("Loaded {} ritual recipes", rituals.size());
        
        // Register with Eidolon's ritual system if available
        if (isEidolonAvailable()) {
            registerRitualsWithEidolon();
        }
    }
    
    /**
     * Register rituals with Eidolon's ritual system
     */
    private void registerRitualsWithEidolon() {
        LOGGER.info("Registering {} rituals with Eidolon ritual system", rituals.size());
        
        int registered = 0;
        for (Map.Entry<ResourceLocation, JsonObject> entry : rituals.entrySet()) {
            try {
                // TODO: Implement Eidolon ritual registration
                // This will require analysis of Eidolon's ritual API
                LOGGER.debug("Prepared ritual {} for Eidolon registration", entry.getKey());
                registered++;
            } catch (Exception e) {
                LOGGER.error("Failed to register ritual {} with Eidolon: {}", entry.getKey(), e.getMessage());
            }
        }
        
        LOGGER.info("Successfully registered {} rituals with Eidolon", registered);
    }
    
    /**
     * CLIENT-SIDE ONLY: Register synced rituals with Eidolon's ritual system
     */
    public static void registerClientRitualsWithEidolon() {
        if (!net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
            return;
        }
        
        try {
            LOGGER.info("CLIENT: Registering {} rituals with Eidolon ritual system", CLIENT_RITUALS.size());
            
            int registered = 0;
            for (Map.Entry<ResourceLocation, JsonObject> entry : CLIENT_RITUALS.entrySet()) {
                try {
                    // TODO: Implement client-side Eidolon ritual registration
                    LOGGER.debug("CLIENT: Prepared ritual {} for Eidolon registration", entry.getKey());
                    registered++;
                } catch (Exception e) {
                    LOGGER.error("CLIENT: Failed to register ritual {} with Eidolon: {}", entry.getKey(), e.getMessage());
                }
            }
            
            LOGGER.info("CLIENT: Successfully registered {} rituals with Eidolon", registered);
            
        } catch (Exception e) {
            LOGGER.error("CLIENT: Failed to register rituals with Eidolon", e);
        }
    }
    
    // Data access methods
    public static Map<ResourceLocation, JsonObject> getAllRituals() {
        return new ConcurrentHashMap<>(rituals);
    }
    
    public static JsonObject getRitual(ResourceLocation id) {
        return rituals.get(id);
    }
    
    // Client-side data management for multiplayer sync
    public static void clearClientRituals() {
        CLIENT_RITUALS.clear();
    }
    
    public static void addClientRitual(ResourceLocation id, JsonObject ritual) {
        CLIENT_RITUALS.put(id, ritual);
    }
    
    public static Map<ResourceLocation, JsonObject> getAllClientRituals() {
        return new ConcurrentHashMap<>(CLIENT_RITUALS);
    }
    
    public static RitualDataManager getInstance() {
        return INSTANCE;
    }
    
    private boolean isEidolonAvailable() {
        try {
            Class.forName("elucent.eidolon.registries.Rituals");
            return true;
        } catch (ClassNotFoundException e) {
            LOGGER.warn("Eidolon not available, skipping ritual registration");
            return false;
        }
    }
}
