package com.bluelotuscoding.eidolonunchained.research.triggers;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.research.triggers.data.ResearchTrigger;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads research triggers from research files (not separate files)
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ResearchTriggerLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static final Map<String, List<ResearchTrigger>> TRIGGERS_BY_TYPE = new HashMap<>();
    private static final Map<String, List<ResearchTrigger>> TRIGGERS_BY_RESEARCH = new HashMap<>();
    
    /**
     * Load research triggers from research files
     */
    public static void loadTriggers(ResourceManager resourceManager) {
        TRIGGERS_BY_TYPE.clear();
        TRIGGERS_BY_RESEARCH.clear();
        
        try {
            // Load from both "research" (preferred) and "eidolon_research" (legacy) directories
            Map<ResourceLocation, Resource> resources = new HashMap<>();
            
            // Load from "research" folder first (preferred)
            Map<ResourceLocation, Resource> researchResources = resourceManager.listResources("research", 
                location -> location.getNamespace().equals(EidolonUnchained.MODID) && location.getPath().endsWith(".json"));
            resources.putAll(researchResources);
            
            // Load from "eidolon_research" folder (legacy support)
            Map<ResourceLocation, Resource> eidolonResearchResources = resourceManager.listResources("eidolon_research", 
                location -> location.getNamespace().equals(EidolonUnchained.MODID) && location.getPath().endsWith(".json"));
            resources.putAll(eidolonResearchResources);
            
            for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
                loadTriggersFromResearchFile(entry.getKey(), entry.getValue());
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to load research trigger files: {}", e.getMessage());
        }
        
        LOGGER.info("Loaded {} research trigger types with {} total triggers from {} research files", 
            TRIGGERS_BY_TYPE.size(), 
            TRIGGERS_BY_TYPE.values().stream().mapToInt(List::size).sum(),
            TRIGGERS_BY_RESEARCH.size());
    }
    
    private static void loadTriggersFromResearchFile(ResourceLocation location, Resource resource) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.open(), StandardCharsets.UTF_8))) {
            
            JsonObject researchJson = GSON.fromJson(reader, JsonObject.class);
            if (researchJson != null && researchJson.has("triggers")) {
                String researchId = researchJson.has("id") ? researchJson.get("id").getAsString() : location.getPath();
                JsonArray triggers = researchJson.getAsJsonArray("triggers");
                
                List<ResearchTrigger> researchTriggers = new ArrayList<>();
                
                for (JsonElement triggerElement : triggers) {
                    ResearchTrigger trigger = parseComplexTrigger(triggerElement);
                    if (trigger != null) {
                        researchTriggers.add(trigger);
                        TRIGGERS_BY_TYPE.computeIfAbsent(trigger.getType(), k -> new ArrayList<>()).add(trigger);
                    }
                }
                
                if (!researchTriggers.isEmpty()) {
                    TRIGGERS_BY_RESEARCH.put(researchId, researchTriggers);
                    LOGGER.debug("Loaded {} triggers from research {}", researchTriggers.size(), researchId);
                }
            }
        } catch (IOException | JsonSyntaxException e) {
            LOGGER.error("Failed to load research triggers from {}: {}", location, e.getMessage());
        }
    }
    
    private static ResearchTrigger parseComplexTrigger(JsonElement triggerElement) {
        if (triggerElement.isJsonObject()) {
            try {
                return GSON.fromJson(triggerElement, ResearchTrigger.class);
            } catch (JsonSyntaxException e) {
                LOGGER.error("Failed to parse complex trigger: {}", e.getMessage());
                return null;
            }
        } else if (triggerElement.isJsonPrimitive()) {
            // Handle legacy simple triggers (entity names)
            String triggerStr = triggerElement.getAsString();
            ResearchTrigger trigger = new ResearchTrigger();
            trigger.setType("kill_entity");
            trigger.setEntity(triggerStr);
            return trigger;
        }
        
        return null;
    }
    
    /**
     * Get all triggers of a specific type
     */
    public static List<ResearchTrigger> getTriggersOfType(String type) {
        return TRIGGERS_BY_TYPE.getOrDefault(type, new ArrayList<>());
    }
    
    /**
     * Get all triggers for a specific research
     */
    public static List<ResearchTrigger> getTriggersForResearch(String researchId) {
        return TRIGGERS_BY_RESEARCH.getOrDefault(researchId, new ArrayList<>());
    }
    
    /**
     * Get all triggers for all research (research-based mapping)
     */
    public static Map<String, List<ResearchTrigger>> getTriggersForAllResearch() {
        return new HashMap<>(TRIGGERS_BY_RESEARCH);
    }
    
    @SubscribeEvent
    public static void onResourceReload(AddReloadListenerEvent event) {
        event.addListener((preparationBarrier, resourceManager, profilerFiller, profilerFiller2, backgroundExecutor, gameExecutor) -> {
            return preparationBarrier.wait(null).thenRunAsync(() -> {
                loadTriggers(resourceManager);
            }, gameExecutor);
        });
    }
}
