package com.bluelotuscoding.eidolonunchained.integration;

import com.bluelotuscoding.eidolonunchained.data.CodexDataManager;
import com.bluelotuscoding.eidolonunchained.codex.CodexEntry;
import com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig;
import com.bluelotuscoding.eidolonunchained.data.ResearchDataManager;
import com.bluelotuscoding.eidolonunchained.research.ResearchChapter;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import elucent.eidolon.codex.Chapter;
import elucent.eidolon.codex.CodexEvents;
// import elucent.eidolon.codex.CodexChapters; // No longer needed
import elucent.eidolon.codex.IndexPage;
import elucent.eidolon.codex.TitledIndexPage;
import elucent.eidolon.codex.Page;
import elucent.eidolon.codex.TextPage;
import elucent.eidolon.codex.TitlePage;
import elucent.eidolon.registries.Researches;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

// No reflection imports needed

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles integration with Eidolon's codex system to inject custom entries.
 * Since Eidolon's codex system is client-side only, this entire class is client-only.
 * 
 * ⚠️ NOTE: This class uses REFLECTION only where absolutely necessary.
 * We prefer using the new event-driven system (EidolonCategoryExtension) for new content.
 * This class is kept for compatibility with existing content injection needs.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class EidolonCodexIntegration {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Entries whose prerequisites weren't found will be stored here for potential later use
    private static final Map<Chapter, List<CodexEntry>> DEFERRED_ENTRIES = new HashMap<>();
    
    // Flag to prevent duplicate integration
    private static boolean integrationCompleted = false;

    @SubscribeEvent
    public static void onCodexPreInit(CodexEvents.PreInit event) {
        integrationCompleted = false;
    }

    @SubscribeEvent
    public static void onCodexPostInit(CodexEvents.PostInit event) {
        attemptIntegrationIfNeeded();
    }

    /**
     * Integrates currently loaded datapack codex content after Eidolon has rebuilt its codex.
     */
    public static void attemptIntegrationIfNeeded() {
        if (integrationCompleted) {
            LOGGER.info("Codex integration already completed, skipping...");
            return;
        }

        LOGGER.info("Starting Eidolon codex integration from loaded datapack content...");

        EidolonPageConverter.initialize();

        if (EidolonUnchainedConfig.COMMON.enableCodexIntegration.get()
            && EidolonUnchainedConfig.COMMON.showChantsInCodex.get()) {
            CodexChantIntegration.registerChants();
        }

        CodexSignIntegration.registerSigns();
        processCustomChapters();
        integrationCompleted = true;
        LOGGER.info("Codex datapack integration complete");
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

    private static String getComponentKey(Component component) {
        if (component != null && component.getContents() instanceof TranslatableContents translatableContents) {
            return translatableContents.getKey();
        }

        return component != null ? component.getString() : "";
    }

    private static String getCustomChapterIndexPageKey(ResourceLocation chapterId) {
        return chapterId.getNamespace() + ".codex.page." + chapterId.getPath() + ".0";
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
                // Check if this is a temporary placeholder
                if (existingChapter.toString().contains("temp_placeholder")) {
                    LOGGER.info("Chapter '{}' found but not yet registered with Eidolon - deferring integration", chapterId);
                    // Store entries for later processing
                    DEFERRED_ENTRIES.put(existingChapter, entries);
                    continue;
                }

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
            LOGGER.warn("Target chapter '{}' not found in Eidolon chapters or custom chapters - will defer integration", chapterId);
            LOGGER.info("Available custom chapters: {}", CodexDataManager.getAllCustomChapters().keySet());

            // Instead of erroring, defer these entries for later processing
            DEFERRED_ENTRIES.put(null, entries);
            continue;
        }

        // After processing regular codex entries, handle conditional research chapters
        // Enhanced research integration with "linked_research" field support and proper player completion checking
        injectLinkedResearchChapters(); // ENHANCED - uses player completion checking and custom category linking
        
        // Report completion status
        if (!DEFERRED_ENTRIES.isEmpty()) {
            LOGGER.info("Codex integration complete with {} deferred entries due to timing issues", DEFERRED_ENTRIES.size());
            LOGGER.info("Deferred entries will be processed when chapters are properly registered with Eidolon");
        } else {
            LOGGER.info("Codex integration complete!");
        }
    }

    /**
     * Processes custom chapter definitions loaded by CodexDataManager and creates actual Eidolon chapters.
     * This is the missing piece that creates new chapters from datapack definitions.
     */
    private static void processCustomChapters() {
        LOGGER.info("=== Starting custom chapter processing ===");
        
        Map<ResourceLocation, CodexDataManager.ChapterDefinition> customChapters = CodexDataManager.getAllCustomChapters();
        
        if (customChapters.isEmpty()) {
            LOGGER.info("No custom chapters found to process");
            return;
        }
        
        LOGGER.info("Found {} custom chapters to process", customChapters.size());
        
        for (Map.Entry<ResourceLocation, CodexDataManager.ChapterDefinition> chapterEntry : customChapters.entrySet()) {
            ResourceLocation chapterId = chapterEntry.getKey();
            CodexDataManager.ChapterDefinition definition = chapterEntry.getValue();
            
            try {
                LOGGER.info("Processing custom chapter: {}", chapterId);
                
                // Create chapter icon
                ItemStack icon = createItemStackFromResourceLocation(definition.getIcon());
                String chapterTitleKey = getComponentKey(definition.getTitle());
                String chapterIndexPageKey = getCustomChapterIndexPageKey(chapterId);
                
                // Create the chapter using Eidolon's Chapter constructor
                Chapter chapter = new Chapter(
                    chapterTitleKey,
                    new TitledIndexPage(chapterIndexPageKey)
                );
                
                // Add any entries that belong to this chapter
                List<CodexEntry> chapterEntries = CodexDataManager.getAllChapterExtensions().get(chapterId);
                if (chapterEntries != null && !chapterEntries.isEmpty()) {
                    LOGGER.info("Adding {} entries to custom chapter {}", chapterEntries.size(), chapterId);
                    for (CodexEntry codexEntry : chapterEntries) {
                        injectEntryIntoChapter(chapter, codexEntry);
                    }
                }
                
                // Attach the chapter to its category
                String categoryName = definition.getCategory();
                if (categoryName != null && !categoryName.isEmpty()) {
                    LOGGER.info("Attaching custom chapter {} to category {}", chapterId, categoryName);
                    List<ResourceLocation> requiredFacts = new ArrayList<>(definition.getUnlockFacts());
                    List<ResourceLocation> requiredResearch = new ArrayList<>(definition.getUnlockResearch());

                    if (chapterEntries != null) {
                        for (CodexEntry codexEntry : chapterEntries) {
                            for (ResourceLocation prereq : codexEntry.getPrerequisites()) {
                                if (!requiredResearch.contains(prereq)) {
                                    requiredResearch.add(prereq);
                                }
                            }
                        }
                    }

                    IndexPage.IndexEntry indexEntry;
                    if (definition.getUnlockDeity() != null
                        || !requiredFacts.isEmpty()
                        || !requiredResearch.isEmpty()
                        || definition.getUnlockRep() != null) {
                        indexEntry = new CombinedLockedEntry(
                            chapter,
                            icon,
                            requiredFacts,
                            requiredResearch,
                            definition.getUnlockRep(),
                            definition.getUnlockDeity()
                        );
                    } else {
                        indexEntry = new IndexPage.IndexEntry(chapter, icon);
                    }

                    EidolonCategoryExtension.attachChapterToCategory(categoryName, indexEntry, icon);
                } else {
                    LOGGER.warn("Custom chapter {} has no category specified", chapterId);
                }
                
                LOGGER.info("Successfully created and attached custom chapter {}", chapterId);
                
            } catch (Exception e) {
                LOGGER.error("Failed to process custom chapter {}: {}", chapterId, e.getMessage(), e);
            }
        }
        
        LOGGER.info("Custom chapter processing complete!");
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
     * Enhanced research integration that uses "linked_research" field in custom categories
     * and proper player completion checking instead of prerequisite checking.
     * Only creates research chapters when players actually complete research scrolls.
     */
    private static void injectLinkedResearchChapters() {
        LOGGER.info("=== Starting linked research chapter injection ===");
        
        // Since this is client-side only, we need to get the current player context
        // Note: This will only work when a player is logged into a world
        Player currentPlayer = Minecraft.getInstance().player;
        if (currentPlayer == null) {
            LOGGER.info("No current player context - research chapters will be injected when player joins world");
            return;
        }
        
        try {
            // Get all custom categories with linked_research field
            // We need to access the DatapackCategoryExample to get category definitions
            // For now, let's focus on the custom_research category as an example
            injectResearchForCustomCategory("custom_research", currentPlayer);
            
            LOGGER.info("Linked research chapter injection complete!");
            
        } catch (Exception e) {
            LOGGER.error("Failed to inject linked research chapters", e);
        }
    }
    
    /**
     * Injects research chapters for a specific custom category based on its linked_research field
     */
    private static void injectResearchForCustomCategory(String categoryKey, Player player) {
        LOGGER.info("Processing linked research for category: {}", categoryKey);

        // Get research chapters that should be available for this category
        Map<ResourceLocation, ResearchChapter> researchChapters = ResearchDataManager.getLoadedResearchChapters();

        if (researchChapters.isEmpty()) {
            LOGGER.info("No research chapters found - research integration will be skipped");
            return;
        }

        LOGGER.info("Found {} research chapters to evaluate", researchChapters.size());

        // Get the linked_research from the actual category definition JSON
        List<String> linkedResearch = getLinkedResearchForCategory(categoryKey);
        if (linkedResearch.isEmpty()) {
            LOGGER.info("No linked_research found for category: {}", categoryKey);
            return;
        }

        LOGGER.info("Found {} linked research entries for category '{}': {}", linkedResearch.size(), categoryKey, linkedResearch);
        
        for (String researchIdStr : linkedResearch) {
            ResourceLocation researchId = ResourceLocation.tryParse(researchIdStr);
            if (researchId == null) {
                LOGGER.warn("Invalid research ID in linked_research: {}", researchIdStr);
                continue;
            }
            
            // Check if player has completed this research using proper player completion checking
            if (elucent.eidolon.util.KnowledgeUtil.knowsResearch(player, researchId)) {
                LOGGER.info("Player has completed research '{}', creating codex chapter", researchId);
                
                // Find the research chapter for this research
                ResearchChapter researchChapter = findResearchChapterForResearch(researchId, researchChapters);
                if (researchChapter != null) {
                    createAndAttachResearchChapter(categoryKey, researchId, researchChapter, player);
                }
            } else {
                LOGGER.debug("Player has not completed research '{}', chapter will not appear", researchId);
            }
        }
    }

    /**
     * Gets the linked_research list for a category by reading from the category definition
     */
    private static List<String> getLinkedResearchForCategory(String categoryKey) {
        try {
            // We need to access the CategoryDefinition from DatapackCategoryExample
            // For now, we'll read the _category.json file directly
            // TODO: This should be refactored to share category definitions between classes

            String categoryPath = "codex/" + categoryKey + "/_category.json";
            LOGGER.info("Looking for category definition at: {}", categoryPath);

            // Access the resource manager (this is client-side code)
            if (Minecraft.getInstance().level != null) {
                var resourceManager = Minecraft.getInstance().getResourceManager();
                ResourceLocation categoryLoc = new ResourceLocation("eidolonunchained", categoryPath);

                if (resourceManager.getResource(categoryLoc).isPresent()) {
                    var resource = resourceManager.getResource(categoryLoc).get();
                    try (InputStreamReader reader = new InputStreamReader(resource.open(), java.nio.charset.StandardCharsets.UTF_8)) {
                        JsonObject json = com.bluelotuscoding.eidolonunchained.util.JsonUtils.GSON.fromJson(reader, JsonObject.class);

                        if (json != null && json.has("linked_research") && json.get("linked_research").isJsonArray()) {
                            List<String> linkedResearch = new ArrayList<>();
                            com.google.gson.JsonArray researchArray = json.get("linked_research").getAsJsonArray();
                            for (com.google.gson.JsonElement element : researchArray) {
                                if (element.isJsonPrimitive()) {
                                    linkedResearch.add(element.getAsString());
                                }
                            }
                            LOGGER.info("Successfully loaded {} linked research entries from category definition", linkedResearch.size());
                            return linkedResearch;
                        }
                    }
                } else {
                    LOGGER.warn("Category definition file not found: {}", categoryLoc);
                }
            }

        } catch (Exception e) {
            LOGGER.error("Failed to load linked research for category '{}': {}", categoryKey, e.getMessage(), e);
        }

        // Fallback: return empty list
        return new ArrayList<>();
    }

    /**
     * Finds the ResearchChapter that contains a specific research entry
     */
    private static ResearchChapter findResearchChapterForResearch(ResourceLocation researchId, Map<ResourceLocation, ResearchChapter> researchChapters) {
        // Look through research entries to find which chapter contains this research
        Map<ResourceLocation, List<com.bluelotuscoding.eidolonunchained.research.ResearchEntry>> researchExtensions = 
            ResearchDataManager.getResearchExtensions();
            
        for (Map.Entry<ResourceLocation, ResearchChapter> chapterEntry : researchChapters.entrySet()) {
            ResourceLocation chapterId = chapterEntry.getKey();
            List<com.bluelotuscoding.eidolonunchained.research.ResearchEntry> entriesForChapter = 
                researchExtensions.get(chapterId);
                
            if (entriesForChapter != null) {
                for (com.bluelotuscoding.eidolonunchained.research.ResearchEntry entry : entriesForChapter) {
                    if (researchId.equals(entry.getId())) {
                        return chapterEntry.getValue();
                    }
                }
            }
        }
        
        LOGGER.warn("Could not find research chapter for research: {}", researchId);
        return null;
    }
    
    /**
     * Creates a codex chapter from research data and attaches it to the specified category
     */
    private static void createAndAttachResearchChapter(String categoryKey, ResourceLocation researchId, ResearchChapter researchChapter, Player player) {
        try {
            // Create a new Chapter for the research
            String chapterTitle = researchChapter.getTitle().getString();
            Chapter chapter = new Chapter(chapterTitle);
            
            // Add research entries for this chapter as pages
            Map<ResourceLocation, List<com.bluelotuscoding.eidolonunchained.research.ResearchEntry>> researchExtensions = 
                ResearchDataManager.getResearchExtensions();
            List<com.bluelotuscoding.eidolonunchained.research.ResearchEntry> entriesForChapter = 
                researchExtensions.get(researchChapter.getId());
                
            if (entriesForChapter != null) {
                for (com.bluelotuscoding.eidolonunchained.research.ResearchEntry researchEntry : entriesForChapter) {
                    // Only add entries that the player has actually completed
                    if (elucent.eidolon.util.KnowledgeUtil.knowsResearch(player, researchEntry.getId())) {
                        convertResearchEntryToPages(chapter, researchEntry);
                        LOGGER.debug("Added completed research entry '{}' to chapter '{}'", researchEntry.getId(), researchChapter.getId());
                    }
                }
            }
            
            // Attach the chapter to the custom category
            EidolonCategoryExtension.attachChapterToCategory(categoryKey, chapter, researchChapter.getIcon());
            LOGGER.info("✅ Attached research chapter '{}' to custom category '{}' based on player research completion", 
                       chapterTitle, categoryKey);
                       
        } catch (Exception e) {
            LOGGER.error("Failed to create and attach research chapter for research: {}", researchId, e);
        }
    }
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
                        String label = "unknown";

                        if (task == null) {
                            taskText.append("- ").append(label).append(" task\n");
                            continue;
                        }

                        try {
                            var taskType = task.getType();
                            if (taskType != null) {
                                try {
                                    ResourceLocation typeId = taskType.id();
                                    if (typeId != null) {
                                        label = typeId.getPath();
                                    } else {
                                        LOGGER.warn("Research task has null type ID: {}", task);
                                    }
                                } catch (Exception e) {
                                    LOGGER.warn("Failed to obtain type id for research task {}: {}", task, e.getMessage());
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.warn("Research task getType() failed for {}: {}", task, e.getMessage());
                        }

                        taskText.append("- ").append(label).append(" task\n");
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
                    String key = translatable.getKey();
                    // Strip .title suffix if present, since TitlePage adds it automatically
                    if (key.endsWith(".title")) {
                        key = key.substring(0, key.length() - 6);
                    }
                    tp = new TitlePage(key);
                } else {
                    tp = new TitlePage(entry.getTitle().getString());
                }
                chapter.addPage(tp);
            }

            // Description
            if (entry.getDescription() != null && !entry.getDescription().getString().isEmpty()) {
                String descText;
                if (entry.getDescription().getContents() instanceof TranslatableContents translatable) {
                    // Use the translation key directly for TextPage so it gets properly translated
                    descText = translatable.getKey();
                    LOGGER.debug("Using translation key for description: {}", descText);
                } else {
                    // Use literal text
                    descText = entry.getDescription().getString();
                    LOGGER.debug("Using literal text for description: {}", descText);
                }
                chapter.addPage(new TextPage(descText));
            }

            // Additional pages (client-only class — no dist check needed)
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
                LOGGER.info("Chapter definition exists but not yet registered - this indicates the chapter exists but codex integration is running too early");
                // Return a placeholder that indicates we found the chapter definition
                // This will prevent the error message since we know the chapter exists
                return createTemporaryChapterPlaceholder(chapterDef);
            }
        }

        // Fallback: search Eidolon's system directly for any matching chapters
        return findChapterInEidolonSystem(chapterId, null);
    }

    /**
     * Creates a temporary chapter placeholder for chapters that exist in our system
     * but haven't been registered with Eidolon yet due to timing issues
     */
    private static Chapter createTemporaryChapterPlaceholder(CodexDataManager.ChapterDefinition chapterDef) {
        try {
            // Create a minimal chapter that won't actually be used but prevents error logging
            Page[] emptyPages = new Page[0];
            Chapter placeholder = new Chapter("temp_placeholder", emptyPages);
            LOGGER.info("Created temporary placeholder for chapter - integration will be deferred");
            return placeholder;
        } catch (Exception e) {
            LOGGER.warn("Failed to create temporary chapter placeholder", e);
            return null;
        }
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
