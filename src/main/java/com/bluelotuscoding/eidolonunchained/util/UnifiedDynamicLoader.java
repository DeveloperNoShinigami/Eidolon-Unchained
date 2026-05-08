package com.bluelotuscoding.eidolonunchained.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility to normalize datapack resource maps into a consistent map of ResourceLocation -> JsonObject
 * Supports two common patterns:
 * 1) Single file containing an "entries" object: { "entries": { "key": { ... }, ... } }
 * 2) Per-file entries where each json file represents one entry; filename becomes the key
 */
public final class UnifiedDynamicLoader {
    private UnifiedDynamicLoader() {}

    public static Map<ResourceLocation, JsonObject> normalizeResourceMap(Map<ResourceLocation, JsonElement> resourceMap, String defaultNamespace) {
        return normalizeResourceMap(resourceMap, defaultNamespace, null);
    }

    public static Map<ResourceLocation, JsonObject> normalizeResourceMap(Map<ResourceLocation, JsonElement> resourceMap, String defaultNamespace, String entriesKey) {
        Map<ResourceLocation, JsonObject> out = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> e : resourceMap.entrySet()) {
            ResourceLocation res = e.getKey();
            JsonElement el = e.getValue();
            if (el == null || !el.isJsonObject()) continue;
            JsonObject root = el.getAsJsonObject();

            // Pattern A: top-level entries object (configurable key)
            String keyToCheck = entriesKey == null ? "entries" : entriesKey;
            if (root.has(keyToCheck) && root.get(keyToCheck).isJsonObject()) {
                JsonObject entries = root.getAsJsonObject(keyToCheck);
                for (Map.Entry<String, JsonElement> ent : entries.entrySet()) {
                    String key = ent.getKey();
                    JsonElement val = ent.getValue();
                    if (val == null) continue;
                    JsonObject valueObj;
                    if (val.isJsonObject()) {
                        valueObj = val.getAsJsonObject();
                    } else {
                        // wrap non-object entry as { "commands": <value> }
                        valueObj = new JsonObject();
                        valueObj.add("commands", val);
                    }
                    ResourceLocation id = parseIdWithDefault(key, defaultNamespace);
                    out.put(id, valueObj);
                }
                continue;
            }

            // Pattern B: file-per-entry — use filename as key
            String path = res.getPath(); // e.g., facts/foo.json or fates/air_deity/bar.json
            String name = path.substring(path.lastIndexOf('/') + 1);
            if (name.endsWith(".json")) name = name.substring(0, name.length() - 5);
            if (!name.isEmpty()) {
                ResourceLocation id = new ResourceLocation(res.getNamespace(), name);
                out.put(id, root);
            }
        }
        return out;
    }

    private static ResourceLocation parseIdWithDefault(String key, String defaultNamespace) {
        if (key == null) return null;
        key = key.trim();
        if (key.contains(":")) return ResourceLocation.tryParse(key);
        return new ResourceLocation(defaultNamespace, key);
    }
}
