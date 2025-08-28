package com.bluelotuscoding.eidolonunchained.research;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import elucent.eidolon.api.research.Research;
import elucent.eidolon.api.research.ResearchTask;
import elucent.eidolon.registries.Researches;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bridges JSON research definitions with Eidolon's research system.
 * Loads research from both "research" and "eidolon_research" folders.
 */
public class ResearchRegistrar {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static final String MODID = EidolonUnchained.MODID;

    /**
     * Load and register research from JSON files
     */
    public static void loadAndRegisterResearch(ResourceManager resourceManager) {
        LOGGER.info("Loading research from JSON files...");
        
        try {
            // Try both folder names: "research" (preferred) and "eidolon_research" (legacy)
            Map<ResourceLocation, Resource> resources = new HashMap<>();
            
            // Load from "research" folder first (preferred)
            Map<ResourceLocation, Resource> researchResources = resourceManager.listResources("research", 
                location -> location.getNamespace().equals(MODID) && location.getPath().endsWith(".json"));
            resources.putAll(researchResources);
            LOGGER.info("Found {} research files in 'research' folder", researchResources.size());
            
            // Load from "eidolon_research" folder (legacy support)
            Map<ResourceLocation, Resource> eidolonResearchResources = resourceManager.listResources("eidolon_research", 
                location -> location.getNamespace().equals(MODID) && location.getPath().endsWith(".json"));
            resources.putAll(eidolonResearchResources);
            LOGGER.info("Found {} research files in 'eidolon_research' folder", eidolonResearchResources.size());
            
            for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
                ResourceLocation location = entry.getKey();
                Resource resource = entry.getValue();
                
                try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.open(), StandardCharsets.UTF_8))) {
                    
                    JsonObject json = GSON.fromJson(reader, JsonObject.class);
                    if (json != null) {
                        registerSingleResearch(location, json);
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to load research from {}: {}", location, e.getMessage());
                }
            }
            
            LOGGER.info("Completed research registration. Total files processed: {}", resources.size());
            
        } catch (Exception e) {
            LOGGER.error("Failed to load research files: {}", e.getMessage(), e);
        }
    }

    private static void registerSingleResearch(ResourceLocation location, JsonObject json) {
        try {
            String researchId = json.get("id").getAsString();
            int stars = json.has("stars") ? json.get("stars").getAsInt() : 1;
            
            // Create the research
            Research research = new Research(new ResourceLocation(MODID, researchId), stars);
            
            // Register with Eidolon
            Researches.register(research);
            LOGGER.debug("Registered research: {} with {} stars", researchId, stars);
            
        } catch (Exception e) {
            LOGGER.error("Failed to register research from {}: {}", location, e.getMessage());
        }
    }

    // Simplified - no task parsing for now, handled by trigger system
    private static ResearchTask parseResearchTask(JsonElement element) {
        return null;
    }

    private static void parseDiscoverySources(Research research, JsonElement element) {
        // Simplified - no discovery sources for now
    }
}
