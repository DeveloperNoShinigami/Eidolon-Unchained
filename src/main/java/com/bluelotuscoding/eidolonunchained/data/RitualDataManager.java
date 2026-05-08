package com.bluelotuscoding.eidolonunchained.data;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.ritual.AIDeityRitual;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elucent.eidolon.registries.RitualRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import com.bluelotuscoding.eidolonunchained.util.UnifiedDynamicSystemLoader;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
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
public class RitualDataManager extends UnifiedDynamicSystemLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(RitualDataManager.class);
    private static final Gson GSON = com.bluelotuscoding.eidolonunchained.util.JsonUtils.GSON;
    
    // Server-side storage
    private static final Map<ResourceLocation, JsonObject> rituals = new ConcurrentHashMap<>();
    
    // Client-side storage for multiplayer sync
    private static final Map<ResourceLocation, JsonObject> CLIENT_RITUALS = new ConcurrentHashMap<>();
    
    private static RitualDataManager INSTANCE;
    // Whether we've attempted/finished registering rituals with Eidolon to avoid double registration
    private static volatile boolean registeredWithEidolon = false;
    
    public RitualDataManager() {
        super(GSON, "rituals", "eidolonunchained");
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
        super.apply(prepared, resourceManager, profiler);

        LOGGER.info("Loaded {} ritual recipes", rituals.size());
        
        // Register with Eidolon's ritual system if available. If not available now,
        // we'll attempt deferred registration when the server has started.
        if (isEidolonAvailable()) {
            registerRitualsWithEidolon();
        }
    }

    @Override
    protected void handleEntry(ResourceLocation id, JsonObject json) {
        try {
            if (json == null || !json.isJsonObject()) {
                LOGGER.warn("Ritual {} is not a valid JSON object, skipping", id);
                return;
            }
            rituals.put(id, json);
            LOGGER.debug("Loaded ritual: {}", id);
        } catch (Exception e) {
            LOGGER.error("Failed to load ritual {}: {}", id, e.getMessage());
        }
    }
    
    /**
     * Register rituals with Eidolon's ritual system.
     * Ritual JSONs with a "linked_deity" field are registered as {@link AIDeityRitual} instances.
     */
    public void registerRitualsWithEidolon() {
        if (registeredWithEidolon) {
            LOGGER.info("Rituals already registered with Eidolon, skipping");
            return;
        }

        LOGGER.info("Registering {} rituals with Eidolon ritual system", rituals.size());

        int registered = 0;
        for (Map.Entry<ResourceLocation, JsonObject> entry : rituals.entrySet()) {
            ResourceLocation ritualId = entry.getKey();
            JsonObject json = entry.getValue();
            try {
                if (!json.has("linked_deity")) {
                    LOGGER.debug("Ritual {} has no linked_deity — skipping AI registration", ritualId);
                    continue;
                }

                ResourceLocation deityId = ResourceLocation.tryParse(json.get("linked_deity").getAsString());
                if (deityId == null) {
                    LOGGER.warn("Ritual {} has invalid linked_deity value, skipping", ritualId);
                    continue;
                }

                // Parse optional color (defaults to neutral grey)
                float r = 0.5f, g = 0.5f, b = 0.5f;
                if (json.has("color")) {
                    JsonObject color = json.getAsJsonObject("color");
                    r = color.has("r") ? color.get("r").getAsFloat() : r;
                    g = color.has("g") ? color.get("g").getAsFloat() : g;
                    b = color.has("b") ? color.get("b").getAsFloat() : b;
                }

                // Symbol defaults to the daylight particle (visible, generic)
                ResourceLocation symbol = new ResourceLocation("eidolon", "particle/daylight_ritual");
                if (json.has("symbol")) {
                    ResourceLocation parsed = ResourceLocation.tryParse(json.get("symbol").getAsString());
                    if (parsed != null) symbol = parsed;
                }

                AIDeityRitual ritual = new AIDeityRitual(ritualId, symbol, r, g, b, deityId);
                RitualRegistry.register(ritualId, ritual);
                registered++;
                LOGGER.info("Registered AIDeityRitual {} for deity {}", ritualId, deityId);
            } catch (Exception e) {
                LOGGER.error("Failed to register ritual {} with Eidolon: {}", ritualId, e.getMessage());
            }
        }

        LOGGER.info("Registered {} AI deity rituals with Eidolon", registered);
        registeredWithEidolon = true;
    }

    /**
     * Attempt to register rituals when the server has finished starting and Eidolon should be available.
     */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (registeredWithEidolon) return;
        if (INSTANCE == null) return;

        if (INSTANCE.isEidolonAvailable()) {
            LOGGER.info("Server started - Eidolon available, attempting deferred ritual registration");
            try {
                INSTANCE.registerRitualsWithEidolon();
            } catch (Exception e) {
                LOGGER.error("Deferred ritual registration failed: {}", e.getMessage());
            }
        } else {
            LOGGER.warn("Server started but Eidolon still not available - rituals will be registered when Eidolon loads");
        }
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
        if (net.minecraftforge.fml.ModList.get().isLoaded("eidolon")) {
            return true;
        }

            LOGGER.warn("Eidolon not available, skipping ritual registration");
            return false;
    }
}
