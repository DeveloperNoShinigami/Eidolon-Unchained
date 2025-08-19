package com.bluelotuscoding.eidolonunchained.integration;

import com.bluelotuscoding.eidolonunchained.data.CodexDataManager;
import com.bluelotuscoding.eidolonunchained.codex.CodexEntry;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import elucent.eidolon.codex.Chapter;
import elucent.eidolon.codex.CodexChapters;
import elucent.eidolon.codex.Page;
import elucent.eidolon.codex.TextPage;
import elucent.eidolon.codex.TitlePage;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles integration with Eidolon's codex system to inject custom entries.
 * Uses direct access to Eidolon's public static Chapter fields for clean integration.
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class EidolonCodexIntegration {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Lookup map for Eidolon chapters keyed by their resource location
    private static final Map<ResourceLocation, Chapter> CHAPTER_LOOKUP = new HashMap<>();

    static {
        registerChapter("monsters", CodexChapters.MONSTERS);
        registerChapter("summon_ritual", CodexChapters.SUMMON_RITUAL);
        registerChapter("crystal_ritual", CodexChapters.CRYSTAL_RITUAL);
        registerChapter("void_amulet", CodexChapters.VOID_AMULET);
        // Additional chapters can be registered here as needed
    }

    private static void registerChapter(String path, Chapter chapter) {
        CHAPTER_LOOKUP.put(new ResourceLocation("eidolon", path), chapter);
    }

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

            Chapter chapter = CHAPTER_LOOKUP.get(chapterId);
            if (chapter != null) {
                LOGGER.info("✓ Injecting {} entries into chapter {}", entries.size(), chapterId);
                for (CodexEntry entry : entries) {
                    injectEntryIntoChapter(chapter, entry);
                }
            } else {
                LOGGER.warn("✗ Unknown chapter {} - skipping", chapterId);
            }
        }

        LOGGER.info("Codex integration complete!");
    }

    /**
     * Converts our CodexEntry into Eidolon Page objects and adds them to the chapter
     */
    private static void injectEntryIntoChapter(Chapter chapter, CodexEntry entry) {
        try {
            // Title and icon
            if (entry.getTitle() != null && !entry.getTitle().getString().isEmpty()) {
                TitlePage tp = entry.getIcon().isEmpty()
                    ? new TitlePage(entry.getTitle().getString())
                    : new TitlePage(entry.getTitle().getString(), entry.getIcon());
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
