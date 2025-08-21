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
import net.minecraft.resources.ResourceLocation;
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
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class EidolonCodexIntegration {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Entries whose prerequisites weren't found will be stored here for potential later use
    private static final Map<Chapter, List<CodexEntry>> DEFERRED_ENTRIES = new HashMap<>();

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(EidolonCodexIntegration::attemptIntegrationIfNeeded);
    }

    /**
     * Integrates custom entries into Eidolon's codex.
     */
    public static void attemptIntegrationIfNeeded() {
        LOGGER.info("Starting Eidolon codex integration...");
        EidolonPageConverter.initialize();
        injectCustomEntries();
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

        for (Map.Entry<ResourceLocation, List<CodexEntry>> chapterEntry : chapterExtensions.entrySet()) {
            ResourceLocation chapterId = chapterEntry.getKey();
            List<CodexEntry> entries = chapterEntry.getValue();

            LOGGER.info("Processing chapter {} with {} entries", chapterId, entries.size());

            ResearchChapter research = ResearchDataManager.getResearchChapter(chapterId);
            CodexDataManager.ChapterDefinition metadata = CodexDataManager.getCustomChapter(chapterId);

            Component titleComponent;
            if (metadata != null) {
                titleComponent = metadata.getTitle();
            } else if (research != null) {
                titleComponent = research.getTitle();
            } else {
                titleComponent = Component.literal(chapterId.getPath());
            }
            String title = titleComponent.getString();

            if (research == null) {
                LOGGER.info("No research chapter for {} - using fallback metadata", chapterId);
            }

            String renderedTitle = title.getString();
            Chapter chapter = new Chapter(renderedTitle, new TitlePage(renderedTitle));
            LOGGER.info("Created chapter {} for codex integration", chapterId);

            LOGGER.info("✓ Injecting {} entries into chapter {}", entries.size(), chapterId);
            for (CodexEntry entry : entries) {
                injectEntryIntoChapter(chapter, entry);
            }
        }

        LOGGER.info("Codex integration complete!");
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
                TitlePage tp = new TitlePage(entry.getTitle().getString());
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

}
