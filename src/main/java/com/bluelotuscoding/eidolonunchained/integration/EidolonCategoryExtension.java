package com.bluelotuscoding.eidolonunchained.integration;

import com.bluelotuscoding.eidolonunchained.data.CodexDataManager;
import elucent.eidolon.codex.Category;
import elucent.eidolon.codex.Chapter;
import elucent.eidolon.codex.CodexChapters;
import elucent.eidolon.codex.Index;
import elucent.eidolon.codex.IndexPage;
import elucent.eidolon.codex.Page;
import elucent.eidolon.codex.TitledIndexPage;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import net.minecraftforge.fml.common.Mod;
import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Codex category helper that uses direct Eidolon APIs where available and
 * only falls back to reflection for package-private Category/IndexPage fields.
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class EidolonCategoryExtension {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String FIELD_CATEGORY_KEY = "key";
    private static final String FIELD_CATEGORY_CHAPTER = "chapter";
    private static final String FIELD_CHAPTER_PAGES = "pages";
    private static final String FIELD_INDEX_ENTRIES = "entries";

    private static Field getField(Class<?> c, String n) throws Exception { Field f=c.getDeclaredField(n); f.setAccessible(true); return f; }

    private static List<Category> getCategories() {
        return CodexChapters.categories;
    }

    private static ItemStack toItemStack(ResourceLocation itemId) {
        if (itemId == null) {
            return new ItemStack(Items.BOOK);
        }

        var item = ForgeRegistries.ITEMS.getValue(itemId);
        return item != null ? new ItemStack(item) : new ItemStack(Items.BOOK);
    }

    /**
     * Finds a category by its key (the name used when Category was constructed).
     */
    public Category findCategory(String categoryKey) {
        try {
            for (Category c : getCategories()) {
                Field keyF = getField(Category.class, FIELD_CATEGORY_KEY);
                String key = (String) keyF.get(c);
                if (categoryKey.equals(key)) return c;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to find category '{}'", categoryKey, e);
        }
        return null;
    }

    /**
     * Creates a new category with a titled index page and registers it with Eidolon.
     */
    public Category createCategory(String categoryKey, String displayName, ItemStack icon, int color, List<IndexPage.IndexEntry> entries) {
        try {
            IndexPage indexPage = new TitledIndexPage("eidolonunchained.codex.category." + categoryKey, entries.toArray(new IndexPage.IndexEntry[0]));
            Index index = new Index("eidolon.codex.category." + categoryKey, indexPage);
            Category category = new Category(categoryKey, icon, color, index);
            getCategories().add(category);
            return category;
        } catch (Exception e) {
            LOGGER.error("Failed to create category '{}'", categoryKey, e);
            return null;
        }
    }

    /**
     * Adds entries to an existing category's index page.
     */
    public void addEntriesToCategory(Category category, List<IndexPage.IndexEntry> newEntries) {
        try {
            Field chapterF = getField(Category.class, FIELD_CATEGORY_CHAPTER);
            Index idx = (Index) chapterF.get(category);
            Field pagesF = getField(Chapter.class, FIELD_CHAPTER_PAGES);
            @SuppressWarnings("unchecked")
            List<Page> pages = (List<Page>) pagesF.get(idx);
            for (Page p : pages) {
                if (p instanceof IndexPage ip) {
                    Field entriesF = getField(IndexPage.class, FIELD_INDEX_ENTRIES);
                    @SuppressWarnings("unchecked")
                    List<IndexPage.IndexEntry> entries = (List<IndexPage.IndexEntry>) entriesF.get(ip);
                    entries.addAll(newEntries);
                    return;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed adding entries to category", e);
        }
    }

    /**
     * Attaches a chapter to a category by adding an index entry pointing to it.
     * Automatically creates new IndexPage objects when pages get too full (like Theurgy does).
     */
    public static void attachChapterToCategory(String categoryKey, Chapter chapter, ItemStack icon) {
        attachChapterToCategory(categoryKey, new IndexPage.IndexEntry(chapter, icon), icon);
    }

    /**
     * Attaches a pre-built index entry to a category. This supports Eidolon's
     * existing locked entry types so custom chapters can mirror base codex
     * visibility behavior.
     */
    public static void attachChapterToCategory(String categoryKey, IndexPage.IndexEntry entry, ItemStack icon) {
        try {
            Category target = null;
            for (Category c : getCategories()) {
                Field keyF = getField(Category.class, FIELD_CATEGORY_KEY);
                String key = (String) keyF.get(c);
                if (categoryKey.equals(key)) { target = c; break; }
            }

            // If the category doesn't exist, create it on-demand with a basic titled index
            if (target == null) {
                LOGGER.info("Category '{}' not found; creating it to attach chapter", categoryKey);
                CodexDataManager.CategoryDefinition categoryDefinition = CodexDataManager.getCategoryDefinition(categoryKey);
                ItemStack categoryIcon = categoryDefinition != null ? toItemStack(categoryDefinition.getIcon()) : icon;
                int categoryColor = categoryDefinition != null && categoryDefinition.getColor() != null
                    ? categoryDefinition.getColor()
                    : 0xFFFFFF;

                // Create an empty titled index page for this category
                IndexPage indexPage = new TitledIndexPage(
                    "eidolonunchained.codex.category." + categoryKey,
                    new IndexPage.IndexEntry[0]
                );
                Index idx = new Index("eidolon.codex.category." + categoryKey, indexPage);
                Category created = new Category(categoryKey, categoryIcon, categoryColor, idx);
                getCategories().add(created);
                target = created;
                LOGGER.info("✅ Created missing category '{}' with a basic index page", categoryKey);
            }

            Field chapterF = getField(Category.class, FIELD_CATEGORY_CHAPTER);
            Index idx = (Index) chapterF.get(target);
            Field pagesF = getField(Chapter.class, FIELD_CHAPTER_PAGES);
            @SuppressWarnings("unchecked")
            List<Page> pages = (List<Page>) pagesF.get(idx);

            // Find the last IndexPage or create first one
            IndexPage lastIndexPage = null;
            for (Page p : pages) {
                if (p instanceof IndexPage) {
                    lastIndexPage = (IndexPage) p;
                }
            }

            if (lastIndexPage == null) {
                LOGGER.info("Category '{}' missing IndexPage; creating a titled index page", categoryKey);
                lastIndexPage = new TitledIndexPage(
                    "eidolonunchained.codex.category." + categoryKey,
                    new IndexPage.IndexEntry[0]
                );
                pages.add(lastIndexPage);
            }

            // Check if current page is full (6 entries max like Theurgy does)
            Field entriesF = getField(IndexPage.class, FIELD_INDEX_ENTRIES);
            @SuppressWarnings("unchecked")
            List<IndexPage.IndexEntry> entries = (List<IndexPage.IndexEntry>) entriesF.get(lastIndexPage);

            // If the last IndexPage has 6 or more entries, create a new one
            if (entries.size() >= 6) {
                LOGGER.info("IndexPage for category '{}' is full ({} entries), creating new page", categoryKey, entries.size());
                IndexPage newPage = new IndexPage(new IndexPage.IndexEntry[0]);
                pages.add(newPage);
                lastIndexPage = newPage;

                // Get entries list for the new page
                @SuppressWarnings("unchecked")
                List<IndexPage.IndexEntry> newEntries = (List<IndexPage.IndexEntry>) entriesF.get(newPage);
                newEntries.add(entry);
            } else {
                // Add to existing page
                entries.add(entry);
            }

        } catch (Exception e) {
            LOGGER.error("Failed to attach chapter to category '{}'", categoryKey, e);
        }
    }

    /**
     * Reload hook retained for compatibility; live codex injection now happens on CodexEvents.PostInit.
     */
    public static void triggerCategoryScanningWithResources(ResourceManager resourceManager) {
        try {
            LOGGER.info("Category scanning hook invoked after resource reload (codex injection deferred to CodexEvents.PostInit)");
        } catch (Exception e) {
            LOGGER.error("Error during category scanning hook", e);
        }
    }
}
