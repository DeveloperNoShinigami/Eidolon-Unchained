package com.bluelotuscoding.eidolonunchained.api.condition;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The live set of discoveries: the ones startup scripts declared ({@code research(id).discoveredBy(...)}, permanent) plus
 * the ones server scripts declare in {@code EidolonUnchainedEvents.discoveries} (replaced on every server start and
 * {@code /reload}). {@link #handle} is called by EU's event detection for every event.
 */
public final class Discoveries {
    private static final Map<ResourceLocation, Discovery> STARTUP = new LinkedHashMap<>();
    private static final Map<ResourceLocation, Discovery> SCRIPTED = new LinkedHashMap<>();
    private static volatile List<Discovery> active = List.of();

    private Discoveries() {
    }

    public static Discovery declareStartup(ResourceLocation id) {
        var d = new Discovery(id);
        STARTUP.put(id, d);
        rebuild();
        return d;
    }

    /** Server scripts: begin a fresh scripted set (the event fires, then {@link #endScripted}). */
    public static void beginScripted() {
        SCRIPTED.clear();
    }

    public static Discovery create(ResourceLocation id) {
        if (SCRIPTED.containsKey(id) || STARTUP.containsKey(id)) EidolonUnchained.LOGGER.warn("discovery '{}' declared twice; the last one wins", id);
        var d = new Discovery(id);
        SCRIPTED.put(id, d);
        return d;
    }

    public static void endScripted() {
        rebuild();
        EidolonUnchained.LOGGER.info("Discoveries active: {} ({} from startup scripts)", active.size(), STARTUP.size());
    }

    private static void rebuild() {
        var list = new ArrayList<Discovery>();
        list.addAll(STARTUP.values());
        for (var d : SCRIPTED.values()) if (!STARTUP.containsKey(d.id)) list.add(d);
        active = List.copyOf(list);
    }

    public static List<Discovery> all() {
        return active;
    }

    public static void handle(EventContext ctx) {
        for (var d : active) {
            if (d.kind == null) continue;
            if (d.accepts(ctx)) d.apply(ctx);
        }
    }
}
