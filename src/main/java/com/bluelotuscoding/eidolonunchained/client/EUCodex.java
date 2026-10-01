package com.bluelotuscoding.eidolonunchained.client;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.api.EURegistry;
import com.bluelotuscoding.eidolonunchained.api.Ids;
import com.bluelotuscoding.eidolonunchained.api.codex.CodexDecls;
import elucent.eidolon.api.spells.Sign;
import elucent.eidolon.codex.Category;
import elucent.eidolon.codex.ChantPage;
import elucent.eidolon.codex.Chapter;
import elucent.eidolon.codex.CodexChapters;
import elucent.eidolon.codex.CodexEvents;
import elucent.eidolon.codex.CraftingPage;
import elucent.eidolon.codex.CruciblePage;
import elucent.eidolon.codex.Index;
import elucent.eidolon.codex.IndexPage;
import elucent.eidolon.codex.Page;
import elucent.eidolon.codex.RitualPage;
import elucent.eidolon.codex.SignIndexPage;
import elucent.eidolon.codex.SignPage;
import elucent.eidolon.codex.SmeltingPage;
import elucent.eidolon.codex.TextPage;
import elucent.eidolon.codex.TitlePage;
import elucent.eidolon.codex.WorktablePage;
import elucent.eidolon.registries.Signs;
import elucent.eidolon.registries.Spells;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Applies the scripts' codex declarations (decision D31) after Eidolon builds its codex ({@code CodexEvents.PostInit}):
 * scripted categories, chapters (into a category's index or appended to an existing chapter) and, last, the fallback
 * sign-index page for scripted signs no script placed. Every page is one of Eidolon's own page classes.
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MOD_ID, value = Dist.CLIENT)
public final class EUCodex {
    private EUCodex() {
    }

    @SubscribeEvent
    public static void onCodexPostInit(CodexEvents.PostInit event) {
        try {
            apply();
        } catch (RuntimeException e) {
            EidolonUnchained.LOGGER.error("Codex: could not apply scripted codex content: {}", e.toString());
        }
    }

    private static void apply() {
        var categories = new LinkedHashMap<ResourceLocation, Index>();     // scripted category id -> its index chapter
        var chapters = new LinkedHashMap<ResourceLocation, Chapter>();

        // 1. scripted categories (their index pages are filled below, once chapters exist)
        for (var c : CodexDecls.categories().values()) {
            var index = new Index(c.nameKey);
            categories.put(c.id, index);
            var cat = new Category(c.id.getNamespace() + "." + c.id.getPath(), stack(c.icon), c.color, index);
            if (c.order != null) CodexChapters.categories.add(Math.min(c.order, CodexChapters.categories.size()), cat);
            else CodexChapters.categories.add(cat);
        }

        // 2. chapters
        var entriesByCategory = new LinkedHashMap<ResourceLocation, List<Entry>>();
        for (var d : CodexDecls.chapters().values()) {
            var pages = new ArrayList<Page>();
            var placed = new ArrayList<Object[]>();               // {position, page} for pages with .at(n)
            for (var p : d.pages) {
                try {
                    var built = page(p, d.id);
                    if (p.at == null && p.entity != null && p.entity.text() != null) {
                        // Eidolon's bestiary layout: the name and text on the left page, the model facing it on the right
                        if (pages.size() % 2 == 1) pages.add(new TextPage(""));
                        pages.add(new TitlePage(p.entity.text(), ((AnimatedEntityPage) built).descriptionId()));
                    }
                    if (p.at != null) placed.add(new Object[]{p.at, built}); else pages.add(built);
                } catch (RuntimeException e) {
                    EidolonUnchained.LOGGER.error("Codex: chapter '{}' page {} {} skipped: {}", d.id, p.kind(), p.args(), e.getMessage());
                }
            }
            placed.sort(java.util.Comparator.comparingInt(o -> (Integer) o[0]));
            for (var o : placed) pages.add(Math.min((Integer) o[0], pages.size()), (Page) o[1]);
            if (d.appendTo != null) {
                var target = eidolonChapter(d.appendTo);
                if (target == null) { EidolonUnchained.LOGGER.error("Codex: chapter '{}': unknown appendTo target '{}'", d.id, d.appendTo); continue; }
                for (var p : pages) target.addPage(p);
                continue;
            }
            if (pages.isEmpty()) { EidolonUnchained.LOGGER.warn("Codex: chapter '{}' has no pages; skipped", d.id); continue; }
            var chapter = new Chapter(d.titleKey, pages.toArray(new Page[0]));
            chapters.put(d.id, chapter);
            ResourceLocation category = d.category;
            if (category == null) {
                for (var c : CodexDecls.categories().values()) if (c.entries.contains(d.id)) category = c.id;
            }
            if (category == null) { EidolonUnchained.LOGGER.warn("Codex: chapter '{}' is in no category and no category lists it; unreachable", d.id); continue; }
            entriesByCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(new Entry(d, new IndexPage.IndexEntry(chapter, stack(d.icon))));
        }
        // explicit chapter order (D36): .order(n) entries first by n, then the rest in builder order, honouring .before/.after
        for (var list : entriesByCategory.values()) {
            var ordered = new ArrayList<Entry>();
            var rest = new ArrayList<Entry>();
            for (var e : list) (e.decl.order != null ? ordered : rest).add(e);
            ordered.sort(java.util.Comparator.comparingInt(e -> e.decl.order));
            for (var e : rest) {
                int idx = -1;
                if (e.decl.before != null) idx = indexOf(ordered, e.decl.before);
                if (e.decl.after != null) { int j = indexOf(ordered, e.decl.after); if (j >= 0) idx = j + 1; }
                if (idx >= 0) ordered.add(idx, e); else ordered.add(e);
            }
            list.clear();
            list.addAll(ordered);
        }

        // 3. index entries: a scripted category's own index, or an IndexPage appended to an Eidolon index
        for (var e : entriesByCategory.entrySet()) {
            Index index = categories.get(e.getKey());
            if (index == null) index = eidolonIndex(e.getKey());
            if (index == null) { EidolonUnchained.LOGGER.error("Codex: unknown category '{}' for {} chapter(s)", e.getKey(), e.getValue().size()); continue; }
            var list = e.getValue().stream().map(en -> en.entry).toList();
            // a scripted category's first index page carries its title, as Eidolon's own categories do
            // (heading from lang 'eidolon.codex.category.<ns>.<path>.title', the tab's key plus '.title')
            boolean titled = categories.containsKey(e.getKey());
            for (int i = 0; i < list.size(); i += 6) {
                var entries = list.subList(i, Math.min(list.size(), i + 6)).toArray(new IndexPage.IndexEntry[0]);
                index.addPage(titled && i == 0
                        ? new elucent.eidolon.codex.TitledIndexPage("eidolon.codex.category." + e.getKey().getNamespace() + "." + e.getKey().getPath(), entries)
                        : new IndexPage(entries));
            }
        }

        // 4. fallback tiles for scripted signs nobody placed on a sign-index page
        var fallback = new ArrayList<SignIndexPage.SignEntry>();
        for (var id : EURegistry.declared(EURegistry.Stage.SIGNS)) {
            if (CodexDecls.isPlaced(id) || CodexDecls.isHidden(id)) continue;
            Sign sign = Signs.find(id);
            if (sign == null) continue;
            fallback.add(new SignIndexPage.SignEntry(signInfo(id, id.getNamespace() + ".codex.chapter." + id.getPath()), sign));
            EidolonUnchained.LOGGER.info("Codex: sign '{}' is on no scripted sign-index page; added to the scripted-signs page in Eidolon's Signs category", id);
        }
        if (!fallback.isEmpty() && CodexChapters.SIGNS_INDEX != null) {
            for (int i = 0; i < fallback.size(); i += 6) {
                CodexChapters.SIGNS_INDEX.addPage(new SignIndexPage(fallback.subList(i, Math.min(fallback.size(), i + 6)).toArray(new SignIndexPage.SignEntry[0])));
            }
        }
        EidolonUnchained.LOGGER.info("Codex: applied {} scripted categor{}, {} chapter(s), {} fallback sign tile(s)",
                categories.size(), categories.size() == 1 ? "y" : "ies", chapters.size(), fallback.size());
    }

    private record Entry(CodexDecls.ChapterDecl decl, IndexPage.IndexEntry entry) {
    }

    private static int indexOf(List<Entry> list, ResourceLocation chapterId) {
        for (int i = 0; i < list.size(); i++) if (list.get(i).decl.id.equals(chapterId)) return i;
        return -1;
    }

    private static Page page(CodexDecls.PageDecl p, ResourceLocation chapter) {
        var a = p.args();
        return switch (p.kind()) {
            case "title" -> new TitlePage(a.get(0));
            case "title_item" -> new TitlePage(a.get(0), stack(a.get(1)));
            case "text" -> new TextPage(a.get(0));
            case "sign" -> new SignPage(sign(a.get(0)));
            case "sign_index" -> {
                var entries = new ArrayList<SignIndexPage.SignEntry>();
                for (var s : a) {
                    var sign = sign(s);
                    entries.add(new SignIndexPage.SignEntry(signInfo(sign.getRegistryName(), s.replace(':', '.') + ".codex.sign"), sign));
                }
                yield new SignIndexPage(entries.toArray(new SignIndexPage.SignEntry[0]));
            }
            case "crafting" -> new CraftingPage(stack(a.get(0)));
            case "crafting_recipe" -> new CraftingPage(stack(a.get(0)), Ids.of(a.get(1), "recipe"));
            case "worktable" -> new WorktablePage(stack(a.get(0)));
            case "crucible" -> new CruciblePage(stack(a.get(0)));
            case "smelting" -> new SmeltingPage(stack(a.get(0)), stack(a.get(1)));
            case "entity" -> {
                var rl = Ids.of(a.get(0), "entity");
                var type = ForgeRegistries.ENTITY_TYPES.getValue(rl);
                if (type == null || !ForgeRegistries.ENTITY_TYPES.containsKey(rl)) throw new IllegalArgumentException("unknown entity '" + a.get(0) + "'");
                yield new AnimatedEntityPage(type, p.entity == null ? CodexDecls.EntityOptions.DEFAULT : p.entity);   // Eidolon's page, animated
            }
            case "ritual" -> new RitualPage(Ids.of(a.get(0), "ritual recipe"));
            case "chant" -> {
                var spell = Spells.find(Ids.of(a.get(1), "spell"));
                if (spell == null) throw new IllegalArgumentException("unknown spell '" + a.get(1) + "'");
                yield new ChantPage(a.get(0), spell);
            }
            default -> throw new IllegalArgumentException("unknown page kind '" + p.kind() + "'");
        };
    }

    /**
     * A sign's info chapter, opened from its sign-index tile. Shaped like Eidolon's own (title page with the sign's
     * description, then the sign): text {@code <ns>.codex.sign.<path>}, heading {@code <ns>.codex.sign.<path>.title}.
     */
    private static Chapter signInfo(ResourceLocation id, String chapterTitleKey) {
        return new Chapter(chapterTitleKey, new TitlePage(id.getNamespace() + ".codex.sign." + id.getPath()), new SignPage(Signs.find(id)));
    }

    private static Sign sign(String id) {
        var sign = Signs.find(Ids.of(id, "sign"));
        if (sign == null) throw new IllegalArgumentException("unknown sign '" + id + "'");
        return sign;
    }

    /** {@code 'minecraft:torch'} or {@code '4x minecraft:torch'}. */
    private static ItemStack stack(String spec) {
        if (spec == null || spec.isBlank()) return ItemStack.EMPTY;
        int count = 1;
        var s = spec.trim();
        var m = java.util.regex.Pattern.compile("^(\\d+)x\\s+(.+)$").matcher(s);
        if (m.matches()) { count = Integer.parseInt(m.group(1)); s = m.group(2); }
        var rl = Ids.of(s, "item");
        var item = ForgeRegistries.ITEMS.getValue(rl);
        if (item == null || !ForgeRegistries.ITEMS.containsKey(rl)) throw new IllegalArgumentException("unknown item '" + spec + "'");
        return new ItemStack(item, count);
    }

    private static final Map<String, java.util.function.Supplier<Index>> EIDOLON_INDEXES = Map.of(
            "nature", () -> CodexChapters.NATURE_INDEX, "rituals", () -> CodexChapters.RITUALS_INDEX, "artifice", () -> CodexChapters.ARTIFICE_INDEX,
            "theurgy", () -> CodexChapters.THEURGY_INDEX, "signs", () -> CodexChapters.SIGNS_INDEX, "spells", () -> CodexChapters.SPELLS_INDEX);

    /** {@code eidolon:signs} (a category) → its index chapter. */
    private static Index eidolonIndex(ResourceLocation category) {
        if (!category.getNamespace().equals("eidolon")) return null;
        var s = EIDOLON_INDEXES.get(category.getPath());
        return s == null ? null : s.get();
    }

    /** {@code eidolon:signs_index} → the index chapter; other Eidolon chapters are not addressable yet. */
    private static Chapter eidolonChapter(ResourceLocation chapter) {
        if (!chapter.getNamespace().equals("eidolon") || !chapter.getPath().endsWith("_index")) return null;
        var s = EIDOLON_INDEXES.get(chapter.getPath().substring(0, chapter.getPath().length() - "_index".length()));
        return s == null ? null : s.get();
    }
}
