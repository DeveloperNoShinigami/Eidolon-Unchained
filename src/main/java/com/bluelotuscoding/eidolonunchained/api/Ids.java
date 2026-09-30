package com.bluelotuscoding.eidolonunchained.api;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import net.minecraft.ResourceLocationException;
import net.minecraft.resources.ResourceLocation;

/** Id parsing with script-friendly errors. */
public final class Ids {
    private Ids() {
    }

    /** Parses a reference to anything ({@code 'minecraft:bone'}, {@code 'eidolon:flame'}). A bare path gets this mod's namespace. */
    public static ResourceLocation of(String id, String what) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Eidolon Unchained: " + what + " needs an id like 'mypack:name'");
        try {
            return id.indexOf(':') < 0 ? new ResourceLocation(EidolonUnchained.MOD_ID, id) : new ResourceLocation(id);
        } catch (ResourceLocationException e) {
            throw new IllegalArgumentException("Eidolon Unchained: " + what + " id '" + id + "' is not a valid resource location (" + e.getMessage() + ")");
        }
    }

    /** Parses the id of something a script <em>creates</em>: it must carry the pack's own namespace. */
    public static ResourceLocation newId(String id, String what) {
        var rl = of(id, what);
        if (rl.getNamespace().equals("minecraft") || rl.getNamespace().equals("eidolon")) {
            throw new IllegalArgumentException("Eidolon Unchained: new " + what + " '" + id + "' must use your pack's namespace, not '" + rl.getNamespace() + "'");
        }
        return rl;
    }

    public static int rgb(int r, int g, int b) {
        for (int v : new int[]{r, g, b}) {
            if (v < 0 || v > 255) throw new IllegalArgumentException("Eidolon Unchained: colour channels must be 0..255, got " + r + ", " + g + ", " + b);
        }
        return (255 << 24) | (r << 16) | (g << 8) | b;
    }

    public static int rgb(int packed) {
        return (255 << 24) | (packed & 0xFFFFFF);
    }
}
