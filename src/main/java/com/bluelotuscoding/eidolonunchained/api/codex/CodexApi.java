package com.bluelotuscoding.eidolonunchained.api.codex;

import com.bluelotuscoding.eidolonunchained.api.Ids;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * {@code EidolonUnchained.codex}: script-authored codex content (decision D31).
 * <pre>
 * EidolonUnchained.codex.chapter('mypack:storm_lore').category('eidolon:signs').title('mypack.codex.chapter.storm_lore')
 *     .titlePage('mypack.codex.storm.intro').textPage('mypack.codex.storm.body').signPage('mypack:storm')
 *     .signIndexPage('mypack:storm')
 * EidolonUnchained.codex.category('mypack:necromancy').icon('minecraft:wither_skeleton_skull').color(0x6FF5D8)
 *     .entry('mypack:storm_lore')
 * </pre>
 * Every page kind ends in one of Eidolon's page classes; the client builds them after Eidolon builds its codex.
 */
public final class CodexApi {
    public static final CodexApi INSTANCE = new CodexApi();

    private CodexApi() {
    }

    @Info("Declare (or continue) a chapter: .category(id) | .appendTo(existingChapterId), .title(key), .icon(item), then pages")
    public ChapterBuilder chapter(String id) {
        var rl = Ids.newId(id, "codex chapter");
        return new ChapterBuilder(CodexDecls.CHAPTERS.computeIfAbsent(rl, CodexDecls.ChapterDecl::new));
    }

    @Info("Declare a codex category (a tab): .name(key).icon(item).color(rgb).entry(chapterId)…")
    public CategoryBuilder category(String id) {
        var rl = Ids.newId(id, "codex category");
        return new CategoryBuilder(CodexDecls.CATEGORIES.computeIfAbsent(rl, CodexDecls.CategoryDecl::new));
    }

    @Info("Ids of Eidolon's categories and index chapters scripts can target")
    public List<String> eidolonTargets() {
        return List.of("eidolon:nature", "eidolon:rituals", "eidolon:artifice", "eidolon:theurgy", "eidolon:signs", "eidolon:spells",
                "eidolon:nature_index", "eidolon:rituals_index", "eidolon:artifice_index", "eidolon:theurgy_index", "eidolon:signs_index", "eidolon:spells_index");
    }

    public static final class ChapterBuilder {
        private final CodexDecls.ChapterDecl decl;

        ChapterBuilder(CodexDecls.ChapterDecl decl) {
            this.decl = decl;
        }

        @Info("Category whose index lists this chapter: a scripted one or eidolon:nature|rituals|artifice|theurgy|signs|spells")
        public ChapterBuilder category(String categoryId) {
            decl.category = Ids.of(categoryId, "codex category");
            return this;
        }

        @Info("Instead of a new chapter, append these pages to an existing chapter (e.g. 'eidolon:signs_index')")
        public ChapterBuilder appendTo(String chapterId) {
            decl.appendTo = Ids.of(chapterId, "codex chapter");
            return this;
        }

        @Info("Lang key of the chapter title (default '<ns>.codex.chapter.<path>')")
        public ChapterBuilder title(String langKey) {
            decl.titleKey = langKey;
            return this;
        }

        @Info("Item shown for this chapter in the category index")
        public ChapterBuilder icon(String itemId) {
            decl.icon = itemId;
            return this;
        }

        @Info("TitlePage: lang key of the text (the chapter title is shown above it)")
        public ChapterBuilder titlePage(String textKey) {
            return page("title", textKey);
        }

        @Info("TitlePage with an item shown next to the title")
        public ChapterBuilder titlePage(String textKey, String itemId) {
            return page("title_item", textKey, itemId);
        }

        @Info("TextPage: lang key of the text")
        public ChapterBuilder textPage(String textKey) {
            return page("text", textKey);
        }

        @Info("SignPage: the sign, its sprite and name")
        public ChapterBuilder signPage(String signId) {
            return page("sign", signId);
        }

        @Info("SignIndexPage: chantable tiles for up to six signs (this is what makes a sign chantable)")
        public ChapterBuilder signIndexPage(String... signIds) {
            if (signIds.length == 0 || signIds.length > 6) throw new IllegalArgumentException("Eidolon Unchained: signIndexPage takes 1 to 6 signs");
            for (var s : signIds) CodexDecls.PLACED_SIGNS.add(Ids.of(s, "sign"));
            return page("sign_index", signIds);
        }

        @Info("CraftingPage: the crafting recipe of an item ('4x minecraft:torch' style counts are allowed)")
        public ChapterBuilder craftingPage(String itemId) {
            return page("crafting", itemId);
        }

        @Info("CraftingPage for a specific recipe id")
        public ChapterBuilder craftingPage(String itemId, String recipeId) {
            return page("crafting_recipe", itemId, recipeId);
        }

        @Info("WorktablePage (Eidolon's worktable recipe of an item)")
        public ChapterBuilder worktablePage(String itemId) {
            return page("worktable", itemId);
        }

        @Info("CruciblePage (Eidolon's crucible recipe of an item)")
        public ChapterBuilder cruciblePage(String itemId) {
            return page("crucible", itemId);
        }

        @Info("SmeltingPage: result item and the default input shown")
        public ChapterBuilder smeltingPage(String resultId, String inputId) {
            return page("smelting", resultId, inputId);
        }

        @Info("EntityPage: an entity rendered on the page")
        public ChapterBuilder entityPage(String entityId) {
            return page("entity", entityId);
        }

        @Info("RitualPage: an Eidolon ritual recipe id (eidolon:ritual_brazier… recipe), as the brazier shows it")
        public ChapterBuilder ritualPage(String ritualRecipeId) {
            return page("ritual", ritualRecipeId);
        }

        @Info("ChantPage: text plus the signs of a spell (needs its chant recipe to be linked)")
        public ChapterBuilder chantPage(String textKey, String spellId) {
            return page("chant", textKey, spellId);
        }

        private ChapterBuilder page(String kind, String... args) {
            decl.pages.add(new CodexDecls.PageDecl(kind, List.of(args)));
            return this;
        }
    }

    public static final class CategoryBuilder {
        private final CodexDecls.CategoryDecl decl;

        CategoryBuilder(CodexDecls.CategoryDecl decl) {
            this.decl = decl;
        }

        @Info("Lang key of the category name (default '<ns>.codex.category.<path>')")
        public CategoryBuilder name(String langKey) {
            decl.nameKey = langKey;
            return this;
        }

        @Info("Item drawn on the tab")
        public CategoryBuilder icon(String itemId) {
            decl.icon = itemId;
            return this;
        }

        public CategoryBuilder color(int r, int g, int b) {
            decl.color = Ids.rgb(r, g, b);
            return this;
        }

        public CategoryBuilder color(int packedRgb) {
            decl.color = Ids.rgb(packedRgb);
            return this;
        }

        @Info("A scripted chapter listed in this category's index (its .category(...) is then optional)")
        public CategoryBuilder entry(String chapterId) {
            decl.entries.add(Ids.of(chapterId, "codex chapter"));
            return this;
        }
    }

    static ResourceLocation rl(String s, String what) {
        return Ids.of(s, what);
    }
}
