package com.bluelotuscoding.eidolonunchained.api;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Timing rule T1 (decision D26): startup scripts <em>declare</em>; this registry <em>replays</em> every declaration into
 * Eidolon's static registries during Eidolon Unchained's own common setup, in dependency order, so Eidolon's registration
 * side effects (config specs, sign lookups, deity references) happen in one known place at one known time.
 * <p>
 * Stages run in the order they are listed in {@link Stage}. A declaration made after {@link #registerAll()} throws, so a
 * script cannot silently register nothing.
 */
public final class EURegistry {
    /** Replay order. Later stages may look up anything an earlier stage registered. */
    public enum Stage { SIGNS, RUNES, RESEARCH, DEITIES, RITUALS, SPELLS, CASTERS }

    private static final Map<Stage, LinkedHashMap<ResourceLocation, Declaration>> DECLARATIONS = new LinkedHashMap<>();
    private static boolean registered = false;

    static {
        for (var stage : Stage.values()) DECLARATIONS.put(stage, new LinkedHashMap<>());
    }

    private EURegistry() {
    }

    /** One recorded declaration: what to register and a short label for the log. */
    public record Declaration(ResourceLocation id, String label, Consumer<ResourceLocation> register) {
    }

    /** Called by builders. Duplicate ids within a stage replace the earlier declaration and warn. */
    public static void declare(Stage stage, ResourceLocation id, String label, Consumer<ResourceLocation> register) {
        if (registered) {
            throw new IllegalStateException("Eidolon Unchained: '" + id + "' was declared after registration. Declare in startup scripts only.");
        }
        var previous = DECLARATIONS.get(stage).put(id, new Declaration(id, label, register));
        if (previous != null) {
            EidolonUnchained.LOGGER.warn("{} '{}' declared twice; the later declaration wins", label, id);
        }
    }

    public static boolean isDeclared(Stage stage, ResourceLocation id) {
        return DECLARATIONS.get(stage).containsKey(id);
    }

    public static List<ResourceLocation> declared(Stage stage) {
        return new ArrayList<>(DECLARATIONS.get(stage).keySet());
    }

    public static boolean isRegistered() {
        return registered;
    }

    /** Replays every declaration into Eidolon. Runs once, from common setup. */
    public static void registerAll() {
        if (registered) return;
        registered = true;
        var summary = new ArrayList<String>();
        int failures = 0;
        for (var stage : Stage.values()) {
            var decls = DECLARATIONS.get(stage);
            int ok = 0;
            for (var d : decls.values()) {
                try {
                    d.register().accept(d.id());
                    ok++;
                    EidolonUnchained.LOGGER.debug("registered {} '{}'", d.label(), d.id());
                } catch (RuntimeException e) {
                    failures++;
                    EidolonUnchained.LOGGER.error("Could not register {} '{}': {}", d.label(), d.id(), e.toString());
                }
            }
            if (!decls.isEmpty()) summary.add(ok + " " + stage.name().toLowerCase());
        }
        EidolonUnchained.LOGGER.info("Script declarations registered with Eidolon: {}{}",
                summary.isEmpty() ? "none" : String.join(", ", summary), failures == 0 ? "" : " (" + failures + " failed, see above)");
    }
}
