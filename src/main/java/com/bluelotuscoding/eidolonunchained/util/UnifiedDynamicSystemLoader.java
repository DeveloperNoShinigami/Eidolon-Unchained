package com.bluelotuscoding.eidolonunchained.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.Map;

/**
 * Abstract base for simple datapack-driven loaders that follow the unified dynamic format.
 * Subclasses implement {@link #handleEntry(ResourceLocation, JsonObject)} to process each entry.
 */
public abstract class UnifiedDynamicSystemLoader extends SimpleJsonResourceReloadListener {
    protected static final Logger LOGGER = LogUtils.getLogger();

    private final String defaultNamespace;
    private final String entriesKey; // optional top-level key to read entries from

    public UnifiedDynamicSystemLoader(Gson gson, String folder) {
        super(gson, folder);
        this.defaultNamespace = "eidolonunchained";
        this.entriesKey = null;
    }

    public UnifiedDynamicSystemLoader(Gson gson, String folder, String defaultNamespace) {
        super(gson, folder);
        this.defaultNamespace = defaultNamespace == null ? "eidolonunchained" : defaultNamespace;
        this.entriesKey = null;
    }

    public UnifiedDynamicSystemLoader(Gson gson, String folder, String defaultNamespace, String entriesKey) {
        super(gson, folder);
        this.defaultNamespace = defaultNamespace == null ? "eidolonunchained" : defaultNamespace;
        this.entriesKey = entriesKey;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resourceMap, ResourceManager resourceManager, ProfilerFiller profiler) {
        try {
            Map<ResourceLocation, JsonObject> entries;
            if (this.entriesKey == null) entries = UnifiedDynamicLoader.normalizeResourceMap(resourceMap, this.defaultNamespace);
            else entries = UnifiedDynamicLoader.normalizeResourceMap(resourceMap, this.defaultNamespace, this.entriesKey);
            processEntries(entries);
        } catch (Exception ex) {
            LOGGER.error("UnifiedDynamicSystemLoader failed to apply resources for {}: {}", this, ex.getMessage(), ex);
        }
    }

    protected void processEntries(Map<ResourceLocation, JsonObject> entries) {
        if (entries == null) return;
        for (Map.Entry<ResourceLocation, JsonObject> e : entries.entrySet()) {
            try {
                handleEntry(e.getKey(), e.getValue());
            } catch (Exception ex) {
                onEntryError(e.getKey(), ex);
            }
        }
    }

    protected void onEntryError(ResourceLocation id, Exception ex) {
        LOGGER.error("Error processing entry {}: {}", id, ex.getMessage(), ex);
    }

    protected abstract void handleEntry(ResourceLocation id, JsonObject json) throws Exception;
}
