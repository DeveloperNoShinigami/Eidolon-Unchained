package com.bluelotuscoding.eidolonunchained.research.tasks;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;

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
    public static ResearchTaskType KILL_ENTITY_NBT;
    public static ResearchTaskType CRAFT_ITEMS;
    public static ResearchTaskType USE_RITUAL;
    public static ResearchTaskType COLLECT_ITEMS;
    public static ResearchTaskType EXPLORE_BIOMES;
    public static ResearchTaskType HAS_NBT;

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
        KILL_ENTITY_NBT = register(new ResourceLocation(EidolonUnchained.MODID, "kill_entity_nbt"), json -> {
            ResourceLocation entity = ResourceLocation.tryParse(json.get("entity").getAsString());
            CompoundTag filter = null;
            if (json.has("filter")) {
                try {
                    filter = TagParser.parseTag(json.get("filter").getAsString());
                } catch (Exception ignored) {}
            }
            int count = json.has("count") ? json.get("count").getAsInt() : 1;
            return new KillEntityWithNbtTask(entity, filter, count);
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
        HAS_NBT = register(new ResourceLocation(EidolonUnchained.MODID, "has_nbt"), json -> {
            if (!json.has("nbt")) return null;
            try {
                CompoundTag tag = TagParser.parseTag(json.get("nbt").getAsString());
                return new HasNbtTask(tag);
            } catch (Exception e) {
                return null;
            }
        });
    }
}
