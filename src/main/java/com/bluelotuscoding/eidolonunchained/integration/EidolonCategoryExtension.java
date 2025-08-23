package com.bluelotuscoding.eidolonunchained.integration;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.data.ResearchDataManager;
import com.bluelotuscoding.eidolonunchained.research.ResearchChapter;
import elucent.eidolon.codex.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

// ⚠️ REFLECTION IMPORTS - Will be removed when CodexEvents become available
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * Handles creation of custom categories and chapters using REFLECTION (temporary).
 * 
 * 🔄 CURRENT IMPLEMENTATION: REFLECTION-BASED (Fully Functional)
 * 🚀 FUTURE MIGRATION: EVENT-BASED (Clean Imports)
 * 
 * This implementation uses reflection to provide FULL datapack functionality NOW,
 * with clear comments showing where to switch to imports when CodexEvents become available.
 * 
 * FEATURES IMPLEMENTED:
 * ✅ Custom category creation via reflection
 * ✅ JSON datapack-driven category loading
 * ✅ Full chapter creation and injection
 * ✅ Item mapping and GUI integration
 * 
 * MIGRATION PLAN: Replace reflection with event system when available
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class EidolonCategoryExtension {
    private static final Logger LOGGER = LogUtils.getLogger();

    // ---------------------------------------------------------------------
    // Reflection targets - update these when Eidolon internals change.
    // See docs/REFLECTION_TARGETS.md for guidance on field names and paths.
    // ---------------------------------------------------------------------
    private static final String CLASS_CODEX_CHAPTERS = "elucent.eidolon.codex.CodexChapters";
    private static final String FIELD_CATEGORIES = "categories";
    private static final String FIELD_CATEGORY_KEY = "key";
    private static final String FIELD_CATEGORY_CHAPTER = "chapter";
    private static final String FIELD_CHAPTER_PAGES = "pages";
    private static final String FIELD_INDEX_ENTRIES = "entries";
    
    // Eidolon Repraised 0.3.8.15: Only categories list is needed for integration
    private static List<Category> eidolonCategories = null;

    // Keep references to dynamically created chapters so they aren't garbage collected
    private static final List<Chapter> registeredChapters = new ArrayList<>();

    // ---------------------------------------------------------------------
    // Helper methods for safe reflection access. These log informative
    // messages when targets change and allow the mod to fail gracefully.
    // ---------------------------------------------------------------------

    private static Class<?> safeForName(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            LOGGER.error("Missing class '{}' - Eidolon may have changed. See docs/REFLECTION_TARGETS.md", className, e);
            return null;
        }
    }

    private static Field safeGetField(Class<?> clazz, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            if (!safeSetAccessible(field)) {
                return null;
            }
            return field;
        } catch (NoSuchFieldException e) {
            LOGGER.error("Missing field '{}' in class '{}' - Eidolon may have changed. See docs/REFLECTION_TARGETS.md", fieldName, clazz.getName(), e);
        } catch (Exception e) {
            LOGGER.error("Unable to access field '{}' in class '{}'", fieldName, clazz.getName(), e);
        }
        return null;
    }

    private static boolean safeSetAccessible(Field field) {
        try {
            field.setAccessible(true);
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to make field '{}' accessible", field.getName(), e);
            return false;
        }
    }

    /**
     * Initialize custom categories using reflection at the correct mod loading phase (FMLLoadCompleteEvent)
     * This ensures Eidolon is fully loaded before we access its internals.
     */
    @SubscribeEvent
    public static void onFMLLoadCompleteEvent(net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent event) {
        LOGGER.info("🎯 EidolonCategoryExtension - FMLLoadCompleteEvent received");
        LOGGER.info("⏳ Category scanning will be triggered later when resources are loaded via CodexDataManager");
        LOGGER.info("🚀 Ready for migration to event system when CodexEvents become available");
    }
    
    /**
     * Initialize access to Eidolon's category system using reflection
     * 
     * 🔄 MIGRATION NOTE: This entire method will be DELETED when CodexEvents available.
     * Event system will provide direct access to categories and itemToEntryMap.
     */
    @SuppressWarnings("unchecked")
    private static boolean initializeEidolonCategoriesAccess() {
        try {
            LOGGER.info("🔍 Using reflection to access Eidolon's category system...");
            Class<?> chaptersClass = safeForName(CLASS_CODEX_CHAPTERS);
            if (chaptersClass == null) {
                return false;
            }

            Field categoriesField = safeGetField(chaptersClass, FIELD_CATEGORIES);
            if (categoriesField == null) {
                return false;
            }

            eidolonCategories = (List<Category>) categoriesField.get(null);
            LOGGER.info("✅ Successfully accessed Eidolon internals via reflection");
            LOGGER.info("   Categories list: {} entries", eidolonCategories.size());
            return true;
        } catch (Exception e) {
            LOGGER.error("❌ Failed to access Eidolon internals via reflection", e);
            return false;
        }
    }

    /**
     * Add custom chapters to existing categories using reflection
     * 
     * 🔄 MIGRATION NOTE: When CodexEvents available, replace reflection with:
     * - category.getKey() instead of reflection field access
     * - Direct category.addChapter() instead of reflection manipulation
     */
    private static void addChaptersToExistingCategories(List<Category> categories) {
        
        LOGGER.info("🔍 Adding custom chapters to existing categories via reflection...");
        
        // Get loaded research chapters
        Map<ResourceLocation, ResearchChapter> customChapters = ResearchDataManager.getLoadedResearchChapters();
        LOGGER.info("Found {} custom research chapters to integrate", customChapters.size());
        
        // ⚠️ TEMPORARILY DISABLED: Research chapters should only appear when prerequisites are met
        // This prevents research chapters from showing up immediately in categories
        LOGGER.info("⚠️ Research chapter auto-integration disabled - chapters will appear only when prerequisites are met");
        
        // TODO: Implement conditional chapter visibility based on research prerequisites
        // For now, research chapters are handled by the research system itself, not the codex system
        
        LOGGER.info("🔍 Custom chapter integration complete (research chapters excluded)");
    }
    
    /**
     * Add a chapter to an existing category using reflection
     * 
     * 🔄 MIGRATION NOTE: When CodexEvents available, replace with:
     * - category.addChapter(chapter) direct method call
     * - Enhanced TitlePage(title, icon) constructor
     */
    private static void addChapterToCategory(Category category, String chapterTitle, 
                                           ItemStack iconItem) {
        try {
            // Create custom chapter
            String titleKey = "eidolonunchained.codex.chapter." + chapterTitle.toLowerCase().replace(" ", "_");
            Chapter customChapter = new Chapter(titleKey);
            
            // No automatic title page - entries will add their own title pages
            
            // Add sample content
            customChapter.addPage(new TextPage("This is a custom chapter created by Eidolon Unchained!"));
            customChapter.addPage(new TextPage("You can add any content here using our JSON datapack system."));
            customChapter.addPage(new TextPage("§6This chapter was added to an existing category using reflection."));
            
            // Create index entry
            IndexPage.IndexEntry newEntry = new IndexPage.IndexEntry(customChapter, iconItem);
            
            // ⚠️ REFLECTION: Add to category's index
            // FUTURE: Replace with category.addChapter(customChapter) when available
            Field chapterField = safeGetField(category.getClass(), FIELD_CATEGORY_CHAPTER);
            if (chapterField == null) {
                LOGGER.warn("Category structure changed; cannot add chapter '{}'", chapterTitle);
                return;
            }
            Index categoryIndex = (Index) chapterField.get(category);

            // Locate the first IndexPage within the category's index
            Field pagesField = safeGetField(Chapter.class, FIELD_CHAPTER_PAGES);
            if (pagesField == null) {
                LOGGER.warn("Chapter structure changed; cannot add chapter '{}'", chapterTitle);
                return;
            }
            @SuppressWarnings("unchecked")
            List<Page> pages = (List<Page>) pagesField.get(categoryIndex);

            IndexPage indexPage = null;
            for (Page page : pages) {
                if (page instanceof IndexPage) {
                    indexPage = (IndexPage) page;
                    break;
                }
            }

            if (indexPage == null) {
                LOGGER.warn("Category missing IndexPage; cannot add chapter '{}'", chapterTitle);
                return;
            }

            // Append the new entry to the IndexPage's list via reflection
            Field entriesField = safeGetField(IndexPage.class, FIELD_INDEX_ENTRIES);
            if (entriesField == null) {
                LOGGER.warn("IndexPage structure changed; cannot add chapter '{}'", chapterTitle);
                return;
            }
            @SuppressWarnings("unchecked")
            List<IndexPage.IndexEntry> entries = (List<IndexPage.IndexEntry>) entriesField.get(indexPage);
            entries.add(newEntry);

            // Keep track of the chapter to ensure it remains registered with the category
            registeredChapters.add(customChapter);

            // ⚠️ REFLECTION: Get category key for logging
            Field keyField = safeGetField(category.getClass(), FIELD_CATEGORY_KEY);
            if (keyField != null) {
                String categoryKey = (String) keyField.get(category);
                LOGGER.info("✅ Added chapter '{}' to category '{}' via reflection", chapterTitle, categoryKey);
            } else {
                LOGGER.info("✅ Added chapter '{}' to category via reflection", chapterTitle);
            }
            
        } catch (Exception e) {
            LOGGER.error("❌ Failed to add chapter '{}' to category via reflection", chapterTitle, e);
        }
    }

    /**
     * Add a research chapter to an existing category using reflection
     */
    private static void addResearchChapterToCategory(Category category, ResearchChapter researchChapter) {
        try {
            // Create codex chapter from research chapter
            String titleKey = "eidolonunchained.codex.chapter." + researchChapter.getId().getPath();
            Chapter codexChapter = new Chapter(titleKey);
            
            // No automatic title page - entries will add their own title pages
            
            // Add description if available
            if (!researchChapter.getDescription().getString().isEmpty()) {
                codexChapter.addPage(new TextPage(researchChapter.getDescription().getString()));
            }
            
            // Add placeholder content - this will be populated by the codex entries
            codexChapter.addPage(new TextPage("This chapter contains custom content loaded from datapacks."));
            codexChapter.addPage(new TextPage("§6Research Chapter: " + researchChapter.getId()));
            
            // Create index entry
            IndexPage.IndexEntry newEntry = new IndexPage.IndexEntry(codexChapter, researchChapter.getIcon());

            // ⚠️ REFLECTION: Add to category's index
            Field chapterField = safeGetField(category.getClass(), FIELD_CATEGORY_CHAPTER);
            if (chapterField == null) {
                LOGGER.warn("Category structure changed; cannot add research chapter '{}'", researchChapter.getId());
                return;
            }
            Index categoryIndex = (Index) chapterField.get(category);

            Field pagesField = safeGetField(Chapter.class, FIELD_CHAPTER_PAGES);
            if (pagesField == null) {
                LOGGER.warn("Chapter structure changed; cannot add research chapter '{}'", researchChapter.getId());
                return;
            }
            @SuppressWarnings("unchecked")
            List<Page> pages = (List<Page>) pagesField.get(categoryIndex);

            IndexPage indexPage = null;
            for (Page page : pages) {
                if (page instanceof IndexPage) {
                    indexPage = (IndexPage) page;
                    break;
                }
            }

            if (indexPage == null) {
                LOGGER.warn("Category missing IndexPage; cannot add research chapter '{}'", researchChapter.getId());
                return;
            }

            Field entriesField = safeGetField(IndexPage.class, FIELD_INDEX_ENTRIES);
            if (entriesField == null) {
                LOGGER.warn("IndexPage structure changed; cannot add research chapter '{}'", researchChapter.getId());
                return;
            }
            @SuppressWarnings("unchecked")
            List<IndexPage.IndexEntry> entries = (List<IndexPage.IndexEntry>) entriesField.get(indexPage);
            entries.add(newEntry);

            // Keep track of the chapter to ensure it remains registered with the category
            registeredChapters.add(codexChapter);

            // Log success
            Field keyField = safeGetField(category.getClass(), FIELD_CATEGORY_KEY);
            if (keyField != null) {
                String categoryKey = (String) keyField.get(category);
                LOGGER.info("✅ Added research chapter '{}' to category '{}' via reflection",
                       researchChapter.getTitle().getString(), categoryKey);
            } else {
                LOGGER.info("✅ Added research chapter '{}' via reflection", researchChapter.getTitle().getString());
            }
            
        } catch (Exception e) {
            LOGGER.error("❌ Failed to add research chapter '{}' to category via reflection",
                        researchChapter.getId(), e);
        }
    }

    /**
     * Attach an already constructed chapter to a category via reflection.
     * Used by EidolonCodexIntegration after creating chapters dynamically.
     */
    public static void attachChapterToCategory(String categoryKey, Chapter chapter, ItemStack iconItem) {
        try {
            if (eidolonCategories == null && !initializeEidolonCategoriesAccess()) {
                LOGGER.warn("Categories not initialized; cannot attach chapter '{}'", categoryKey);
                return;
            }

            for (Category category : eidolonCategories) {
                Field keyField = category.getClass().getDeclaredField("key");
                keyField.setAccessible(true);
                String key = (String) keyField.get(category);

                if (!categoryKey.equals(key)) continue;

                IndexPage.IndexEntry entry = new IndexPage.IndexEntry(chapter, iconItem);

                Field chapterField = category.getClass().getDeclaredField("chapter");
                chapterField.setAccessible(true);
                Index categoryIndex = (Index) chapterField.get(category);

                Field pagesField = Chapter.class.getDeclaredField("pages");
                pagesField.setAccessible(true);
                @SuppressWarnings("unchecked")
                List<Page> pages = (List<Page>) pagesField.get(categoryIndex);

                IndexPage indexPage = null;
                for (Page page : pages) {
                    if (page instanceof IndexPage) {
                        indexPage = (IndexPage) page;
                        break;
                    }
                }

                if (indexPage == null) {
                    LOGGER.warn("Category '{}' missing IndexPage; cannot attach chapter", categoryKey);
                    return;
                }

                Field entriesField = IndexPage.class.getDeclaredField("entries");
                entriesField.setAccessible(true);
                @SuppressWarnings("unchecked")
                List<IndexPage.IndexEntry> entries = (List<IndexPage.IndexEntry>) entriesField.get(indexPage);
                entries.add(entry);

                registeredChapters.add(chapter);
                LOGGER.info("✅ Attached chapter to category '{}' via reflection", categoryKey);
                return;
            }

            LOGGER.warn("Category '{}' not found; unable to attach chapter", categoryKey);

        } catch (Exception e) {
            LOGGER.error("❌ Failed to attach chapter to category '{}' via reflection", categoryKey, e);
        }
    }

    /* COMMENTED OUT: Hardcoded category creation examples
     * We prefer JSON datapack-driven approach for maximum flexibility
     *
    /**
     * Add custom categories to the codex using the builder pattern
     * NOTE: This is commented out in favor of JSON datapack approach
     *
    private static void addCustomCategories(java.util.List<Category> categories, 
                                          java.util.Map<Item, IndexPage.IndexEntry> itemToEntryMap) {
        
        // Example: Create a "MODDED" category with custom chapters
        Category moddedCategory = new CustomCategoryBuilder("modded")
            .icon(new ItemStack(Items.COMMAND_BLOCK))
            .color(0xFF9900) // Orange color
            .addChapter("eidolonunchained.codex.chapter.custom_monsters", 
                       "Custom Monsters Guide", 
                       new ItemStack(Items.ZOMBIE_SPAWN_EGG))
            .addChapter("eidolonunchained.codex.chapter.advanced_techniques", 
                       "Advanced Techniques", 
                       new ItemStack(Items.ENCHANTED_BOOK))
            .addChapter("eidolonunchained.codex.chapter.datapack_guide", 
                       "Datapack Creation Guide", 
                       new ItemStack(Items.WRITABLE_BOOK))
            .build();
        
        categories.add(moddedCategory);
        
        // Example: Create an "EXPANSIONS" category
        Category expansionsCategory = new CustomCategoryBuilder("expansions")
            .icon(new ItemStack(Items.END_CRYSTAL))
            .color(0x9966FF) // Purple color
            .addChapter("eidolonunchained.codex.chapter.new_spells", 
                       "New Spells & Rituals", 
                       new ItemStack(Items.BLAZE_ROD))
            .addChapter("eidolonunchained.codex.chapter.custom_items", 
                       "Custom Artifacts", 
                       new ItemStack(Items.DIAMOND))
            .build();
        
        categories.add(expansionsCategory);
        
        LOGGER.info("✅ Added {} custom categories to Eidolon codex!", 2);
    }
    */

    /* � FUTURE MIGRATION GUIDE:
     * 
     * When CodexEvents become available, replace this ENTIRE file with:
     * 
     * @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE) // Change from MOD to FORGE
     * 
     * @SubscribeEvent
     * public static void onCodexPreInit(CodexEvents.PreInit event) {
     *     LOGGER.info("🎯 Using event system - no reflection needed!");
     *     
     *     // Direct access - NO reflection required!
     *     DatapackCategoryExample.addDatapackCategories(event.categories, event.itemToEntryMap);
     * }
     * 
     * @SubscribeEvent 
     * public static void onCodexPostInit(CodexEvents.PostInit event) {
     *     // Add chapters to existing categories with direct access
     *     for (Category category : event.categories) {
     *         String categoryKey = category.getKey(); // Direct access, no reflection
     *         // Add chapters based on category type
     *     }
     * }
     * 
     * BENEFITS AFTER MIGRATION:
     * ✅ No reflection usage at all
     * ✅ Type-safe direct API access  
     * ✅ Better performance
     * ✅ IDE support and refactoring
     * ✅ Forward compatibility
     */
    
    /**
     * Trigger category scanning after resources are loaded (called from CodexDataManager)
     */
    public static void triggerCategoryScanningWithResources(net.minecraft.server.packs.resources.ResourceManager resourceManager) {
        LOGGER.info("🎯 Category scanning triggered with loaded resources!");
        
        try {
            LOGGER.info("🔄 Using reflection to access Eidolon's category system...");
            Class<?> codexChaptersClass = Class.forName(CLASS_CODEX_CHAPTERS);
            Field categoriesField = codexChaptersClass.getDeclaredField(FIELD_CATEGORIES);
            categoriesField.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            java.util.List<Category> categories = (java.util.List<Category>) categoriesField.get(null);
            
            LOGGER.info("✅ Successfully accessed Eidolon internals via reflection");
            LOGGER.info("   Categories list: {} entries", categories.size());
            
            // Create custom categories from JSON datapacks
            LOGGER.info("🎯 Creating custom categories from JSON datapacks...");
            DatapackCategoryExample.addDatapackCategories(categories, resourceManager);
            
            // Register custom research chapters
            LOGGER.info("🔬 Registering custom research chapters...");
            DatapackResearchExample.addDatapackResearchChapters(resourceManager);
            
            // Add custom chapters to existing categories
            addChaptersToExistingCategories(categories);
            
            LOGGER.info("✅ Successfully completed category scanning with loaded resources!");
            
        } catch (Exception e) {
            LOGGER.error("❌ Failed category scanning with reflection", e);
        }
    }
}
