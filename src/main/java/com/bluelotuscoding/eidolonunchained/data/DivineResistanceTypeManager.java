package com.bluelotuscoding.eidolonunchained.data;

import com.bluelotuscoding.eidolonunchained.util.JsonUtils;
import com.bluelotuscoding.eidolonunchained.util.UnifiedDynamicSystemLoader;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Datapack loader for mapping damage types to named divine resistance channels.
 *
 * Path: data/<namespace>/divine_resistance_types/<file>.json
 * Schema:
 * {
 *   "damage_type": "<namespace>:<damage_type>",
 *   "resistance_key": "<key>",
 *   "default_resistance": 0.0
 * }
 */
public class DivineResistanceTypeManager extends UnifiedDynamicSystemLoader {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<ResourceLocation, ResistanceTypeInfo> DAMAGE_TYPE_TO_RESISTANCE = new HashMap<>();

    public DivineResistanceTypeManager() {
        super(JsonUtils.GSON, "divine_resistance_types", "eidolonunchained");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profiler) {
        DAMAGE_TYPE_TO_RESISTANCE.clear();
        super.apply(map, resourceManager, profiler);
        LOGGER.info("Loaded {} divine resistance type mappings", DAMAGE_TYPE_TO_RESISTANCE.size());
    }

    @Override
    protected void handleEntry(ResourceLocation id, JsonObject json) {
        if (json == null || !json.has("damage_type") || !json.has("resistance_key")) {
            LOGGER.warn("Skipping divine resistance mapping {}: missing damage_type or resistance_key", id);
            return;
        }

        ResourceLocation damageType = ResourceLocation.tryParse(json.get("damage_type").getAsString());
        if (damageType == null) {
            LOGGER.warn("Skipping divine resistance mapping {}: invalid damage_type '{}'", id, json.get("damage_type").getAsString());
            return;
        }

        String resistanceKey = json.get("resistance_key").getAsString();
        if (resistanceKey == null || resistanceKey.isBlank()) {
            LOGGER.warn("Skipping divine resistance mapping {}: blank resistance_key", id);
            return;
        }

        double defaultResistance = json.has("default_resistance") ? json.get("default_resistance").getAsDouble() : 0.0d;
        DAMAGE_TYPE_TO_RESISTANCE.put(damageType, new ResistanceTypeInfo(resistanceKey, defaultResistance));
    }

    public static Optional<ResistanceTypeInfo> getInfo(ResourceLocation damageType) {
        return Optional.ofNullable(DAMAGE_TYPE_TO_RESISTANCE.get(damageType));
    }

    public static Map<ResourceLocation, ResistanceTypeInfo> getAllMappings() {
        return Collections.unmodifiableMap(DAMAGE_TYPE_TO_RESISTANCE);
    }

    public static Set<String> getAllResistanceKeys() {
        Set<String> keys = new LinkedHashSet<>();
        for (ResistanceTypeInfo info : DAMAGE_TYPE_TO_RESISTANCE.values()) {
            if (info != null && info.resistanceKey() != null && !info.resistanceKey().isBlank()) {
                keys.add(info.resistanceKey());
            }
        }
        return Collections.unmodifiableSet(keys);
    }

    public record ResistanceTypeInfo(String resistanceKey, double defaultResistance) {
    }
}
