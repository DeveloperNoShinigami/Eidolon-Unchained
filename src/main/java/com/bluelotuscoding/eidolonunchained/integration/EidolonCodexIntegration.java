package com.bluelotuscoding.eidolonunchained.integration;

import com.bluelotuscoding.eidolonunchained.data.CodexDataManager;
import com.bluelotuscoding.eidolonunchained.codex.CodexEntry;
import com.bluelotuscoding.eidolonunchained.data.ResearchDataManager;
import com.bluelotuscoding.eidolonunchained.research.ResearchChapter;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import elucent.eidolon.codex.Chapter;
// import elucent.eidolon.codex.CodexChapters; // No longer needed
import elucent.eidolon.codex.Page;
import elucent.eidolon.codex.TextPage;
import elucent.eidolon.codex.TitlePage;
import elucent.eidolon.registries.Researches;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.slf4j.Logger;

// No reflection imports needed

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles integration with Eidolon's codex system to inject custom entries.
 * 
 * ⚠️ NOTE: This class uses REFLECTION only where absolutely necessary.
 * We prefer using the new event-driven system (EidolonCategoryExtension) for new content.
 * This class is kept for compatibility with existing content injection needs.
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EidolonCodexIntegration {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Entries whose prerequisites weren't found will be stored here for potential later use
    private static final Map<Chapter, List<CodexEntry>> DEFERRED_ENTRIES = new HashMap<>();
    
    // Flag to prevent duplicate integration
    private static boolean integrationCompleted = false;

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        // Run integration after all data has been loaded
        attemptIntegrationIfNeeded();
    }

    /**
     * Integrates custom entries into Eidolon's codex.
     */
    public static void attemptIntegrationIfNeeded() {
        if (integrationCompleted) {
            LOGGER.info("Codex integration already completed, skipping...");
            return;
        }
        
        LOGGER.info("Starting Eidolon codex integration...");
        EidolonPageConverter.initialize();
        injectCustomEntries();
        integrationCompleted = true;
    }

    /**
     * Helper method to convert ResourceLocation to ItemStack
     */
    private static ItemStack createItemStackFromResourceLocation(ResourceLocation resourceLocation) {
        if (resourceLocation == null) {
            return new ItemStack(Items.BOOK);
        }
        
        var item = ForgeRegistries.ITEMS.getValue(resourceLocation);
        if (item == null) {
            LOGGER.warn("Could not find item for resource location: {}, falling back to book", resourceLocation);
            return new ItemStack(Items.BOOK);
        }
        
        return new ItemStack(item);
    }

    /**
     * Injects our custom entries into the appropriate Eidolon chapters
     */
    private static void injectCustomEntries() {
        LOGGER.info("=== Starting custom entry injection ===");

        Map<ResourceLocation, List<CodexEntry>> chapterExtensions = CodexDataManager.getAllChapterExtensions();

        LOGGER.info("Found {} loaded chapter extensions from CodexDataManager", chapterExtensions.size());

        if (chapterExtensions.isEmpty()) {
            LOGGER.warn("No chapter extensions found! Check if CodexDataManager is loading data correctly.");
            return;
        }

        LOGGER.info("Injecting entries for {} chapters", chapterExtensions.size());

        // Process each unique chapter (not each entry)
        for (Map.Entry<ResourceLocation, List<CodexEntry>> chapterEntry : chapterExtensions.entrySet()) {
            ResourceLocation chapterId = chapterEntry.getKey();
            List<CodexEntry> entries = chapterEntry.getValue();

            LOGGER.info("Processing chapter {} with {} entries", chapterId, entries.size());

            ResearchChapter research = ResearchDataManager.getResearchChapter(chapterId);
            CodexDataManager.ChapterDefinition metadata = CodexDataManager.getCustomChapter(chapterId);

            // Try to find existing Eidolon chapter first
            Chapter existingChapter = findExistingEidolonChapter(chapterId);
            
            // If not found, try to find existing custom chapter
            if (existingChapter == null) {
                existingChapter = findExistingCustomChapter(chapterId);
            }
            
            if (existingChapter != null) {
                LOGGER.info("Found existing chapter for {} - adding {} entries as pages", chapterId, entries.size());
                
                // Add ALL entries for this chapter as pages within the existing chapter
                for (CodexEntry entry : entries) {
                    LOGGER.info("Adding entry '{}' as pages to existing chapter", entry.getId());
                    injectEntryIntoChapter(existingChapter, entry);
                }
                
                LOGGER.info("Successfully extended existing chapter with {} entries", entries.size());
                continue; // Skip creating new chapter since we extended existing one
            }
            
            // If we reach here, the target_chapter doesn't exist - this is a configuration error
            LOGGER.error("Target chapter '{}' not found in Eidolon chapters or custom chapters. Check chapter definitions.", chapterId);
            LOGGER.error("Available custom chapters: {}", CodexDataManager.getAllCustomChapters().keySet());
            continue; // Skip this chapter entirely rather than create incorrect new chapter
        }

        // After processing regular codex entries, handle conditional research chapters
        injectConditionalResearchChapters();

        LOGGER.info("Codex integration complete!");
    }

    /**
     * Injects research chapters into categories when their research entries' prerequisites are met.
     * This allows research chapters to appear conditionally based on player progress.
     */
    private static void injectConditionalResearchChapters() {
        LOGGER.info("=== Starting conditional research chapter injection ===");
        
        Map<ResourceLocation, ResearchChapter> researchChapters = ResearchDataManager.getLoadedResearchChapters();
        
        if (researchChapters.isEmpty()) {
            LOGGER.info("No research chapters found to inject conditionally");
            return;
        }
        
        LOGGER.info("Found {} research chapters to evaluate for conditional injection", researchChapters.size());
        
        for (Map.Entry<ResourceLocation, ResearchChapter> entry : researchChapters.entrySet()) {
            ResourceLocation chapterId = entry.getKey();
            ResearchChapter researchChapter = entry.getValue();
            
            // Get research entries for this chapter
            Map<ResourceLocation, List<com.bluelotuscoding.eidolonunchained.research.ResearchEntry>> researchExtensions = 
                ResearchDataManager.getResearchExtensions();
            List<com.bluelotuscoding.eidolonunchained.research.ResearchEntry> entriesForChapter = 
                researchExtensions.get(chapterId);
            
            if (entriesForChapter == null || entriesForChapter.isEmpty()) {
                LOGGER.debug("No research entries found for chapter '{}', skipping conditional injection", chapterId);
                continue;
            }
            
            // Check if any research entry in this chapter has its prerequisites met
            boolean anyEntryVisible = false;
            for (com.bluelotuscoding.eidolonunchained.research.ResearchEntry researchEntry : entriesForChapter) {
                boolean prerequisitesMet = true;
                
                // Check prerequisites for this specific research entry
                for (ResourceLocation prereq : researchEntry.getPrerequisites()) {
                    if (Researches.find(prereq) == null) {
                        prerequisitesMet = false;
                        break;
                    }
                }
                
                if (prerequisitesMet) {
                    anyEntryVisible = true;
                    LOGGER.debug("Research entry '{}' prerequisites met in chapter '{}'", 
                               researchEntry.getId(), chapterId);
                    break;
                }
            }
            
            if (!anyEntryVisible) {
                LOGGER.debug("No research entries have prerequisites met for chapter '{}', skipping injection", chapterId);
                continue;
            }
            
            // At least one entry is visible - create and attach chapter to category
            LOGGER.info("Research chapter '{}' has visible entries, injecting into category '{}'", 
                       chapterId, researchChapter.getCategory());
                       
            String renderedTitle = researchChapter.getTitle().getString();
            Chapter chapter = new Chapter(renderedTitle);
            
            // Add research entries with met prerequisites as pages to the chapter
            for (com.bluelotuscoding.eidolonunchained.research.ResearchEntry researchEntry : entriesForChapter) {
                boolean prerequisitesMet = true;
                for (ResourceLocation prereq : researchEntry.getPrerequisites()) {
                    if (Researches.find(prereq) == null) {
                        prerequisitesMet = false;
                        break;
                    }
                }
                
                if (prerequisitesMet) {
                    // Convert research entry to codex pages
                    convertResearchEntryToPages(chapter, researchEntry);
                    LOGGER.debug("Added research entry '{}' to chapter '{}'", researchEntry.getId(), chapterId);
                }
            }
            
            // Attach to category
            EidolonCategoryExtension.attachChapterToCategory(researchChapter.getCategory(), chapter, researchChapter.getIcon());
            LOGGER.info("✅ Conditionally attached research chapter '{}' to category '{}'", 
                       renderedTitle, researchChapter.getCategory());
        }
        
        LOGGER.info("Conditional research chapter injection complete!");
    }

    /**
     * Converts a research entry to codex pages and adds them to the chapter
     */
    private static void convertResearchEntryToPages(Chapter chapter, com.bluelotuscoding.eidolonunchained.research.ResearchEntry researchEntry) {
        try {
            // Add title page for the research entry
            if (researchEntry.getTitle() != null && !researchEntry.getTitle().getString().isEmpty()) {
                TitlePage titlePage = new TitlePage(researchEntry.getTitle().getString());
                chapter.addPage(titlePage);
            }
            
            // Add description page
            if (researchEntry.getDescription() != null && !researchEntry.getDescription().getString().isEmpty()) {
                TextPage descPage = new TextPage(researchEntry.getDescription().getString());
                chapter.addPage(descPage);
            }
            
            // Add tasks as pages (simplified version)
            Map<Integer, List<com.bluelotuscoding.eidolonunchained.research.tasks.ResearchTask>> tasks = researchEntry.getTasks();
            if (!tasks.isEmpty()) {
                StringBuilder taskText = new StringBuilder("Required Tasks:\n\n");
                for (Map.Entry<Integer, List<com.bluelotuscoding.eidolonunchained.research.tasks.ResearchTask>> taskEntry : tasks.entrySet()) {
                    taskText.append("Stage ").append(taskEntry.getKey()).append(":\n");
                    for (com.bluelotuscoding.eidolonunchained.research.tasks.ResearchTask task : taskEntry.getValue()) {
                        // Use task type for description since getDescription() doesn't exist
                        taskText.append("- ").append(task.getType().id().getPath()).append(" task\n");
                    }
                    taskText.append("\n");
                }
                TextPage taskPage = new TextPage(taskText.toString());
                chapter.addPage(taskPage);
            }
            
            LOGGER.debug("Successfully converted research entry '{}' to {} pages", 
                        researchEntry.getId(), chapter.size());
        } catch (Exception e) {
            LOGGER.error("Failed to convert research entry '{}' to pages", researchEntry.getId(), e);
        }
    }

    /**
     * Converts our CodexEntry into Eidolon Page objects and adds them to the chapter
     */
    private static void injectEntryIntoChapter(Chapter chapter, CodexEntry entry) {
        try {
            // If the entry has prerequisites, ensure they are all registered
            if (!entry.getPrerequisites().isEmpty()) {
                for (ResourceLocation prereq : entry.getPrerequisites()) {
                    if (Researches.find(prereq) == null) {
                        LOGGER.debug("Deferring entry '{}' due to unmet prerequisite {}", entry.getId(), prereq);
                        DEFERRED_ENTRIES.computeIfAbsent(chapter, c -> new ArrayList<>()).add(entry);
                        return; // Skip injecting pages until prerequisites exist
                    }
                }
            }

            // Title and icon
            if (entry.getTitle() != null && !entry.getTitle().getString().isEmpty()) {
                TitlePage tp;
                if (entry.getTitle().getContents() instanceof TranslatableContents translatable) {
                    tp = new TitlePage(translatable.getKey());
                } else {
                    tp = new TitlePage(entry.getTitle().getString());
                }
                chapter.addPage(tp);
            }

            // Description
            if (entry.getDescription() != null && !entry.getDescription().getString().isEmpty()) {
                chapter.addPage(new TextPage(entry.getDescription().getString()));
            }

            // Additional pages
            for (JsonObject pageJson : entry.getPages()) {
                Page eidolonPage = EidolonPageConverter.convertPage(pageJson);
                if (eidolonPage != null) {
                    chapter.addPage(eidolonPage);
                }
            }

            // Log prerequisites (visibility gating could be implemented client-side)
            if (!entry.getPrerequisites().isEmpty()) {
                LOGGER.debug("Entry {} prerequisites: {}", entry.getId(), entry.getPrerequisites());
            }

            LOGGER.debug("Successfully injected entry '{}' with {} total pages", entry.getId(), chapter.size());
        } catch (Exception e) {
            LOGGER.error("Failed to inject entry '{}' into chapter", entry.getId(), e);
        }
    }

    /**
     * Attempts to find an existing Eidolon chapter by ResourceLocation
     */
    private static Chapter findExistingEidolonChapter(ResourceLocation chapterId) {
        try {
            // Access Eidolon's chapter registry via reflection
            Class<?> codexChaptersClass = Class.forName("elucent.eidolon.codex.CodexChapters");
            
            // Get the static field by name mapping
            String fieldName = getEidolonChapterFieldName(chapterId);
            if (fieldName != null) {
                try {
                    java.lang.reflect.Field chapterField = codexChaptersClass.getDeclaredField(fieldName);
                    chapterField.setAccessible(true);
                    Object chapterObj = chapterField.get(null);
                    
                    if (chapterObj instanceof Chapter) {
                        LOGGER.info("Found existing Eidolon chapter '{}' for ResourceLocation {}", fieldName, chapterId);
                        return (Chapter) chapterObj;
                    }
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    LOGGER.debug("Failed to access Eidolon chapter field '{}': {}", fieldName, e.getMessage());
                }
            }
            
            return null;
        } catch (ClassNotFoundException e) {
            LOGGER.warn("Could not find Eidolon CodexChapters class");
            return null;
        }
    }

    /**
     * Attempts to find an existing custom chapter by ResourceLocation
     * First checks our CodexDataManager for chapter definitions, then searches Eidolon's system
     */
    private static Chapter findExistingCustomChapter(ResourceLocation chapterId) {
        // First, check if we have a chapter definition in our system
        CodexDataManager.ChapterDefinition chapterDef = CodexDataManager.getCustomChapter(chapterId);
        if (chapterDef != null) {
            LOGGER.info("Found custom chapter definition for '{}', checking if already registered in Eidolon system", chapterId);
            
            // Check if this chapter has already been registered with Eidolon
            String titleString = chapterDef.getTitle().getString(); // Convert Component to String
            Chapter existingChapter = findChapterInEidolonSystem(chapterId, titleString);
            if (existingChapter != null) {
                LOGGER.info("Found existing chapter '{}' already registered in Eidolon system", chapterId);
                return existingChapter;
            } else {
                LOGGER.info("Chapter definition exists but not yet registered - will create new chapter");
                return null; // Will trigger chapter creation
            }
        }
        
        // Fallback: search Eidolon's system directly for any matching chapters
        return findChapterInEidolonSystem(chapterId, null);
    }
    
    /**
     * Searches Eidolon's category system for a chapter matching the given ID
     */
    private static Chapter findChapterInEidolonSystem(ResourceLocation chapterId, String expectedTitle) {
        try {
            // Access Eidolon's category system via reflection
            Class<?> codexClass = Class.forName("elucent.eidolon.codex.Codex");
            java.lang.reflect.Field categoriesField = codexClass.getDeclaredField("categories");
            categoriesField.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            java.util.List<Object> categories = (java.util.List<Object>) categoriesField.get(null);
            
            // Search through all categories
            for (Object categoryObj : categories) {
                // Get the chapters from this category
                Class<?> categoryClass = categoryObj.getClass();
                java.lang.reflect.Field chaptersField = categoryClass.getDeclaredField("chapters");
                chaptersField.setAccessible(true);
                
                @SuppressWarnings("unchecked")
                java.util.List<Chapter> chapters = (java.util.List<Chapter>) chaptersField.get(categoryObj);
                
                // Search chapters in this category
                for (Chapter chapter : chapters) {
                    // Get chapter title to compare
                    java.lang.reflect.Field titleField = Chapter.class.getDeclaredField("title");
                    titleField.setAccessible(true);
                    String chapterTitle = (String) titleField.get(chapter);
                    
                    // Check if this chapter matches our target
                    if (chapterTitle != null) {
                        // If we have an expected title, check for exact match first
                        if (expectedTitle != null && chapterTitle.equals(expectedTitle)) {
                            LOGGER.info("Found existing chapter with exact title match: '{}'", chapterTitle);
                            return chapter;
                        }
                        
                        // Check if the chapter title corresponds to our chapter ID
                        String expectedTitleKey = chapterId.getNamespace() + ".codex.chapter." + chapterId.getPath();
                        if (chapterTitle.equals(expectedTitleKey) || 
                            chapterTitle.equals(chapterId.getPath()) ||
                            chapterTitle.toLowerCase().replace(" ", "_").equals(chapterId.getPath())) {
                            
                            LOGGER.info("Found existing chapter '{}' with title '{}' for ResourceLocation {}", 
                                      chapterTitle, chapterTitle, chapterId);
                            return chapter;
                        }
                    }
                }
            }
            
            LOGGER.debug("No existing chapter found in Eidolon system for ResourceLocation {}", chapterId);
            return null;
        } catch (Exception e) {
            LOGGER.warn("Failed to search for chapter '{}' in Eidolon system: {}", chapterId, e.getMessage());
            return null;
        }
    }

    /**
     * Maps ResourceLocation to Eidolon chapter field names
     */
    private static String getEidolonChapterFieldName(ResourceLocation chapterId) {
        // Map common chapter names to Eidolon's static field names
        String path = chapterId.getPath();
        
        switch (path) {
            case "wooden_stand": return "WOODEN_STAND";
            case "tallow": return "TALLOW";
            case "crucible": return "CRUCIBLE";
            case "arcane_gold": return "ARCANE_GOLD";
            case "reagents": return "REAGENTS";
            case "soul_gems": return "SOUL_GEMS";
            case "shadow_gem": return "SHADOW_GEM";
            case "warped_sprouts": return "WARPED_SPROUTS";
            case "basic_alchemy": return "BASIC_ALCHEMY";
            case "inlays": return "INLAYS";
            case "basic_baubles": return "BASIC_BAUBLES";
            case "magic_workbench": return "MAGIC_WORKBENCH";
            case "void_amulet": return "VOID_AMULET";
            case "warded_mail": return "WARDED_MAIL";
            case "soulfire_wand": return "SOULFIRE_WAND";
            case "bonechill_wand": return "BONECHILL_WAND";
            case "reaper_scythe": return "REAPER_SCYTHE";
            case "cleaving_axe": return "CLEAVING_AXE";
            case "soul_enchanter": return "SOUL_ENCHANTER";
            case "reversal_pick": return "REVERSAL_PICK";
            case "warlock_armor": return "WARLOCK_ARMOR";
            case "gravity_belt": return "GRAVITY_BELT";
            case "prestigious_palm": return "PRESTIGIOUS_PALM";
            case "mind_shielding_plate": return "MIND_SHIELDING_PLATE";
            case "resolute_belt": return "RESOLUTE_BELT";
            case "glass_hand": return "GLASS_HAND";
            case "soulbone": return "SOULBONE";
            case "raven_cloak": return "RAVEN_CLOAK";
            case "necromancer_staff": return "NECROMANCER_STAFF";
            case "arrow_ring": return "ARROW_RING";
            default:
                LOGGER.debug("No Eidolon chapter field mapping for: {}", path);
                return null;
        }
    }

}
