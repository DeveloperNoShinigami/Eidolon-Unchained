package com.bluelotuscoding.eidolonunchained.research;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elucent.eidolon.api.research.Research;
import elucent.eidolon.api.research.ResearchTask;
import elucent.eidolon.registries.Researches;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Registers JSON-defined research with Eidolon's research system
 * This bridges our custom JSON research format with Eidolon's Java-based research registry
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ResearchRegistrar {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();

    /**
     * Load and register research from JSON files
     */
    public static void registerResearch(ResourceManager resourceManager) {
        try {
            // Load from eidolon_research directory  
            Map<ResourceLocation, Resource> resources = resourceManager.listResources("eidolon_research", 
                location -> location.getNamespace().equals(EidolonUnchained.MODID) && location.getPath().endsWith(".json"));
            
            for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
                ResourceLocation location = entry.getKey();
                Resource resource = entry.getValue();
                
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(resource.open(), StandardCharsets.UTF_8))) {
                    
                    JsonObject json = GSON.fromJson(reader, JsonObject.class);
                    registerSingleResearch(location, json);
                    
                } catch (Exception e) {
                    LOGGER.error("Failed to parse research file: {}", location, e);
                }
            }
            
            LOGGER.info("Registered {} custom research entries with Eidolon", resources.size());
            
        } catch (Exception e) {
            LOGGER.error("Failed to register research with Eidolon", e);
        }
    }
    
    /**
     * Register a single research from JSON with Eidolon's system
     */
    private static void registerSingleResearch(ResourceLocation location, JsonObject json) {
        try {
            String id = json.get("id").getAsString();
            int stars = json.get("stars").getAsInt();
            
            // Create the research with special tasks based on JSON tasks
            Research research = new Research(new ResourceLocation(EidolonUnchained.MODID, id), stars);
            
            // Parse the tasks from JSON and add them as special tasks
            if (json.has("tasks")) {
                JsonObject tasks = json.getAsJsonObject("tasks");
                
                for (String stepKey : tasks.keySet()) {
                    int step = Integer.parseInt(stepKey);
                    JsonArray stepTasks = tasks.getAsJsonArray(stepKey);
                    
                    ResearchTask[] researchTasks = new ResearchTask[stepTasks.size()];
                    for (int i = 0; i < stepTasks.size(); i++) {
                        JsonObject taskJson = stepTasks.get(i).getAsJsonObject();
                        researchTasks[i] = parseResearchTask(taskJson);
                    }
                    
                    research.addSpecialTasks(step, researchTasks);
                }
            }
            
            // Parse discovery triggers and register with appropriate sources
            Object[] sources = parseDiscoverySources(json);
            
            // Register with Eidolon
            Researches.register(research, sources);
            
            LOGGER.debug("Registered research '{}' with {} stars and {} discovery sources", 
                id, stars, sources.length);
            
        } catch (Exception e) {
            LOGGER.error("Failed to register research from {}: {}", location, e.getMessage());
        }
    }
    
    /**
     * Parse Eidolon ResearchTask from JSON task definition
     */
    private static ResearchTask parseResearchTask(JsonObject taskJson) {
        String type = taskJson.get("type").getAsString();
        
        switch (type) {
            case "item":
                String itemId = taskJson.get("item").getAsString();
                int count = taskJson.get("count").getAsInt();
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
                if (item != null) {
                    return new ResearchTask.TaskItems(new ItemStack(item, count));
                }
                break;
                
            // Add more task types as needed (XP, etc.)
            default:
                LOGGER.warn("Unknown research task type: {}", type);
                break;
        }
        
        // Fallback to simple item task
        return new ResearchTask.TaskItems(new ItemStack(net.minecraft.world.item.Items.DIRT, 1));
    }
    
    /**
     * Parse discovery sources from triggers array for Eidolon registration
     */
    private static Object[] parseDiscoverySources(JsonObject json) {
        if (!json.has("triggers")) return new Object[0];
        
        JsonArray triggers = json.getAsJsonArray("triggers");
        java.util.List<Object> sources = new java.util.ArrayList<>();
        
        for (JsonElement triggerElement : triggers) {
            // Handle simple string triggers (entity/block names)
            if (triggerElement.isJsonPrimitive()) {
                String triggerStr = triggerElement.getAsString();
                
                // Try to parse as entity
                EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(triggerStr));
                if (entityType != null) {
                    sources.add(entityType);
                    continue;
                }
                
                // Try to parse as block
                Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(triggerStr));
                if (block != null) {
                    sources.add(block);
                }
            }
            // Ignore complex triggers - those are handled by our custom system
        }
        
        return sources.toArray();
    }
    
    @SubscribeEvent
    public static void onResourceReload(AddReloadListenerEvent event) {
        event.addListener((preparationBarrier, resourceManager, profilerFiller, profilerFiller2, backgroundExecutor, gameExecutor) -> {
            return preparationBarrier.wait(null).thenRunAsync(() -> {
                registerResearch(resourceManager);
            }, gameExecutor);
        });
    }
}
