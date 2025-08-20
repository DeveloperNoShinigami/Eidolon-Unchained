package com.bluelotuscoding.eidolonunchained.research.tasks;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Registry for {@link ResearchTaskType} instances.
 */
public class ResearchTaskTypes {
    private static final Map<ResourceLocation, ResearchTaskType> REGISTRY = new HashMap<>();

    public static ResearchTaskType register(ResourceLocation id, Function<JsonObject, ResearchTask> factory) {
        ResearchTaskType type = new ResearchTaskType(id, factory);
        REGISTRY.put(id, type);
        return type;
    }

    public static ResearchTaskType get(ResourceLocation id) {
        return REGISTRY.get(id);
    }

    // Built-in task types
    public static ResearchTaskType KILL_ENTITIES;
    public static ResearchTaskType CRAFT_ITEMS;
    public static ResearchTaskType USE_RITUAL;
    public static ResearchTaskType COLLECT_ITEMS;
    public static ResearchTaskType EXPLORE_BIOMES;

    /**
     * Registers the built-in task types. Should be called during mod
     * initialization.
     */
    public static void registerBuiltins() {
        KILL_ENTITIES = register(new ResourceLocation(EidolonUnchained.MODID, "kill_entities"), json -> {
            ResourceLocation entity = ResourceLocation.tryParse(json.get("entity").getAsString());
            int count = json.has("count") ? json.get("count").getAsInt() : 1;
            return new KillEntitiesTask(entity, count);
        });
        CRAFT_ITEMS = register(new ResourceLocation(EidolonUnchained.MODID, "craft_items"), json -> {
            ResourceLocation item = ResourceLocation.tryParse(json.get("item").getAsString());
            int count = json.has("count") ? json.get("count").getAsInt() : 1;
            return new CraftItemsTask(item, count);
        });
        USE_RITUAL = register(new ResourceLocation(EidolonUnchained.MODID, "use_ritual"), json -> {
            ResourceLocation ritual = ResourceLocation.tryParse(json.get("ritual").getAsString());
            int count = json.has("count") ? json.get("count").getAsInt() : 1;
            return new UseRitualTask(ritual, count);
        });
        COLLECT_ITEMS = register(new ResourceLocation(EidolonUnchained.MODID, "collect_items"), json -> {
            ResourceLocation item = ResourceLocation.tryParse(json.get("item").getAsString());
            int count = json.has("count") ? json.get("count").getAsInt() : 1;
            return new CollectItemsTask(item, count);
        });
        EXPLORE_BIOMES = register(new ResourceLocation(EidolonUnchained.MODID, "explore_biomes"), json -> {
            ResourceLocation biome = ResourceLocation.tryParse(json.get("biome").getAsString());
            int count = json.has("count") ? json.get("count").getAsInt() : 1;
            return new ExploreBiomesTask(biome, count);
        });
    }
}
