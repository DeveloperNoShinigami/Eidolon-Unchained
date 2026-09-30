package com.bluelotuscoding.eidolonunchained.api;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Sign and rune sprites live in the block atlas. Every builder that declares one contributes a supplier here, and the
 * KubeJS plugin turns the set into a generated {@code eidolonunchained:atlases/blocks.json} so the sprites get stitched
 * without the script pack writing atlas JSON by hand.
 */
public final class SpriteSources {
    private static final List<Supplier<ResourceLocation>> SOURCES = new ArrayList<>();

    private SpriteSources() {
    }

    static void add(Supplier<ResourceLocation> sprite) {
        SOURCES.add(sprite);
    }

    public static Set<ResourceLocation> all() {
        var out = new LinkedHashSet<ResourceLocation>();
        for (var s : SOURCES) {
            var rl = s.get();
            if (rl != null) out.add(rl);
        }
        return out;
    }
}
