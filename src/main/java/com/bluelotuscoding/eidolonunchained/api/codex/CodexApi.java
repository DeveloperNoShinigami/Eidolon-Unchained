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

        @Info("EntityPage: an entity rendered on the page; animated models play from the start each time the page is opened")
        public ChapterBuilder entityPage(String entityId) {
            return entityPage(entityId, java.util.Map.of());
        }

        @Info("EntityPage with options: { text: 'lang.key' (a facing page titled with the entity's name, as Eidolon's bestiary does), and for GeckoLib models scale: 1.0 (standard size, 1 fills the frame), rotate: -30 (yaw), animation: 'manifest' (played from the start when the page is reached), then: 'idle' (looped afterwards; without it the last frame holds), arrivalScale: 0.6 (size while the arrival clip plays, easing back to scale), hideBelowGround: true, hideBehind: 2.0 (blocks behind the model past which nothing is drawn) }")
        public ChapterBuilder entityPage(String entityId, java.util.Map<String, Object> options) {
            double scale = 1.0, rotate = -30;
            Double arrivalScale = null, hideBehind = null;
            String animation = null, then = null, text = null;
            boolean hideBelowGround = false, gecko = false;
            for (var e : options.entrySet()) {
                switch (e.getKey()) {
                    case "text" -> text = String.valueOf(e.getValue());
                    case "scale" -> { scale = number(e.getValue(), "scale"); gecko = true; }
                    case "arrivalScale" -> { arrivalScale = number(e.getValue(), "arrivalScale"); gecko = true; }
                    case "rotate" -> { rotate = number(e.getValue(), "rotate"); gecko = true; }
                    case "animation" -> { animation = String.valueOf(e.getValue()); gecko = true; }
                    case "then" -> { then = String.valueOf(e.getValue()); gecko = true; }
                    case "hideBelowGround" -> {
                        if (!(e.getValue() instanceof Boolean flag)) throw new IllegalArgumentException("entityPage option 'hideBelowGround' must be true or false");
                        hideBelowGround = flag;
                        gecko = true;
                    }
                    case "hideBehind" -> { hideBehind = number(e.getValue(), "hideBehind"); gecko = true; }
                    default -> throw new IllegalArgumentException("entityPage '" + entityId + "': unknown option '" + e.getKey()
                            + "' (text, scale, arrivalScale, rotate, animation, then, hideBelowGround, hideBehind)");
                }
            }
            if (scale <= 0 || (arrivalScale != null && arrivalScale <= 0)) throw new IllegalArgumentException("entityPage '" + entityId + "': scales must be > 0");
            if (animation == null && (then != null || arrivalScale != null)) throw new IllegalArgumentException("entityPage '" + entityId + "': 'then' and 'arrivalScale' need an 'animation'");
            page("entity", entityId);
            decl.pages.get(decl.pages.size() - 1).entity = new CodexDecls.EntityOptions(scale, arrivalScale, rotate, animation, then, hideBelowGround, hideBehind, text, gecko);
            return this;
        }

        private static double number(Object value, String key) {
            if (value instanceof Number n) return n.doubleValue();
            throw new IllegalArgumentException("entityPage option '" + key + "' must be a number");
        }

        @Info("RitualPage: the ritual brazier recipe id; for scripted rituals that is the ritual id itself, e.g. 'mypack:storm_rite'")
        public ChapterBuilder ritualPage(String ritualRecipeId) {
            return page("ritual", ritualRecipeId);
        }

        @Info("ChantPage: text plus the signs of a spell (needs its chant recipe to be linked)")
        public ChapterBuilder chantPage(String textKey, String spellId) {
            return page("chant", textKey, spellId);
        }

        @Info("Put the last added page at this position in the chapter (0 = first); default is builder order")
        public ChapterBuilder at(int position) {
            if (decl.pages.isEmpty()) throw new IllegalStateException("Eidolon Unchained: .at() needs a page before it");
            decl.pages.get(decl.pages.size() - 1).at = Math.max(0, position);
            return this;
        }

        @Info("Position of this chapter in its category index (0 = first); default is builder order after existing entries")
        public ChapterBuilder order(int position) {
            decl.order = Math.max(0, position);
            return this;
        }

        @Info("List this chapter right before another scripted chapter in the same category")
        public ChapterBuilder before(String chapterId) {
            decl.before = Ids.of(chapterId, "codex chapter");
            return this;
        }

        @Info("List this chapter right after another scripted chapter in the same category")
        public ChapterBuilder after(String chapterId) {
            decl.after = Ids.of(chapterId, "codex chapter");
            return this;
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

        @Info("Tab position (0 = first tab, before Eidolon's); default is after Eidolon's tabs in builder order")
        public CategoryBuilder order(int position) {
            decl.order = Math.max(0, position);
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
