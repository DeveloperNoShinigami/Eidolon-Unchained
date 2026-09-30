package com.bluelotuscoding.eidolonunchained.api.codex;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Codex declarations (decision D31). Startup scripts run on both sides and Eidolon's codex classes are client-only, so
 * builders record plain data here and the client turns it into Eidolon pages after {@code CodexEvents.PostInit}.
 */
public final class CodexDecls {
    /** One page: its kind and the arguments the kind needs (ids and lang keys as strings). */
    public record PageDecl(String kind, List<String> args) {
    }

    public static final class ChapterDecl {
        public final ResourceLocation id;
        public String titleKey;
        public ResourceLocation category;          // where its index entry goes (scripted or Eidolon category id)
        public ResourceLocation appendTo;          // an existing chapter to append the pages to instead
        public String icon;                        // item id for the index entry
        public final List<PageDecl> pages = new ArrayList<>();

        ChapterDecl(ResourceLocation id) {
            this.id = id;
            this.titleKey = id.getNamespace() + ".codex.chapter." + id.getPath();
        }
    }

    public static final class CategoryDecl {
        public final ResourceLocation id;
        public String nameKey;
        public String icon = "minecraft:book";
        public int color = 0xFFFFFFFF;
        public final List<ResourceLocation> entries = new ArrayList<>();

        CategoryDecl(ResourceLocation id) {
            this.id = id;
            this.nameKey = id.getNamespace() + ".codex.category." + id.getPath();
        }
    }

    static final Map<ResourceLocation, ChapterDecl> CHAPTERS = new LinkedHashMap<>();
    static final Map<ResourceLocation, CategoryDecl> CATEGORIES = new LinkedHashMap<>();
    /** Signs some script placed on a sign-index page (so the fallback skips them). */
    static final Set<ResourceLocation> PLACED_SIGNS = new LinkedHashSet<>();
    static final Set<ResourceLocation> HIDDEN_SIGNS = new LinkedHashSet<>();

    private CodexDecls() {
    }

    public static Map<ResourceLocation, ChapterDecl> chapters() {
        return CHAPTERS;
    }

    public static Map<ResourceLocation, CategoryDecl> categories() {
        return CATEGORIES;
    }

    public static boolean isPlaced(ResourceLocation sign) {
        return PLACED_SIGNS.contains(sign);
    }

    public static boolean isHidden(ResourceLocation sign) {
        return HIDDEN_SIGNS.contains(sign);
    }

    public static void hideSign(ResourceLocation sign) {
        HIDDEN_SIGNS.add(sign);
    }
}
