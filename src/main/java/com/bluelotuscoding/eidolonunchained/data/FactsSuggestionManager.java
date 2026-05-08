package com.bluelotuscoding.eidolonunchained.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import com.bluelotuscoding.eidolonunchained.util.UnifiedDynamicLoader;
import com.bluelotuscoding.eidolonunchained.util.UnifiedDynamicSystemLoader;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Optional loader for fact suggestions: data/<ns>/facts/*.json
 * Schema (per-file):
 * {
 *   "title": "Readable title",
 *   "description": "How the fact is unlocked",
 *   "tags": ["category", "deity:light_deity"]
 * }
 * File name defines the fact id (facts/<id>.json => <ns>:<id>)
 */
public class FactsSuggestionManager extends UnifiedDynamicSystemLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final com.google.gson.Gson GSON = com.bluelotuscoding.eidolonunchained.util.JsonUtils.GSON;

    private static final Map<ResourceLocation, FactInfo> FACTS = new HashMap<>();

    public FactsSuggestionManager() {
        super(GSON, "facts", "eidolonunchained");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager rm, ProfilerFiller profiler) {
        FACTS.clear();
        super.apply(map, rm, profiler);
        LOGGER.info("Loaded {} fact suggestions", FACTS.size());
    }

    @Override
    protected void handleEntry(ResourceLocation id, JsonObject json) {
        if (json == null) return;
        String name = id.getPath();
        String title = json.has("title") ? json.get("title").getAsString() : name;
        String description = json.has("description") ? json.get("description").getAsString() : "";
        List<String> tags = new ArrayList<>();
        if (json.has("tags") && json.get("tags").isJsonArray()) {
            for (JsonElement el : json.getAsJsonArray("tags")) tags.add(el.getAsString());
        }
        FACTS.put(id, new FactInfo(id, title, description, tags));
    }

    public static Set<ResourceLocation> getKnownFactIds() {
        return new HashSet<>(FACTS.keySet());
    }

    public static Set<ResourceLocation> getAllFactIds() {
        return getKnownFactIds();
    }

    public static FactInfo getFact(ResourceLocation id) {
        return FACTS.get(id);
    }

    public static class FactInfo {
        public final ResourceLocation id;
        public final String title;
        public final String description;
        public final List<String> tags;
        public FactInfo(ResourceLocation id, String title, String description, List<String> tags) {
            this.id = id; this.title = title; this.description = description; this.tags = tags;
        }
    }
}
