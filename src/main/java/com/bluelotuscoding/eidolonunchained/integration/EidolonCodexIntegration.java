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

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * Handles integration with Eidolon's codex system to inject custom entries.
 * Uses direct access to Eidolon's public static Chapter fields for clean integration.
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class EidolonCodexIntegration {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // No runtime reflection caching needed now that Eidolon is a compile-time dependency

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

            // Convert "eidolon:arcane_gold" to "ARCANE_GOLD"
            String fieldName = convertChapterIdToFieldName(chapterId.toString());

            try {
                Field chapterField = CodexChapters.class.getField(fieldName);
                Chapter chapter = (Chapter) chapterField.get(null);

                if (chapter != null) {
                    LOGGER.info("✓ Injecting {} entries into chapter {}", entries.size(), fieldName);

                    for (CodexEntry entry : entries) {
                        injectEntryIntoChapter(chapter, entry);
                    }
                } else {
                    LOGGER.warn("✗ Chapter {} is null - may need to defer injection", fieldName);
                }

            } catch (NoSuchFieldException e) {
                LOGGER.warn("✗ Could not find chapter field {} for target {}", fieldName, chapterId);
            } catch (Exception e) {
                LOGGER.error("Failed to inject entries into chapter {}", chapterId, e);
            }
        }

        LOGGER.info("Codex integration complete!");
    }

    /**
     * Converts our CodexEntry into Eidolon Page objects and adds them to the chapter
     */
    private static void injectEntryIntoChapter(Chapter chapter, CodexEntry entry) {
        try {
            // Only add pages from the JSON definition; do not always add a TitlePage
            for (JsonObject pageJson : entry.getPages()) {
                Page eidolonPage = EidolonPageConverter.convertPage(pageJson);
                if (eidolonPage != null) {
                    chapter.addPage(eidolonPage);
                }
            }
            LOGGER.debug("Successfully injected entry '{}' with {} pages", entry.getId(), entry.getPages().size());
        } catch (Exception e) {
            LOGGER.error("Failed to inject entry '{}' into chapter", entry.getId(), e);
        }
    }

    /**
     * Converts "eidolon:arcane_gold" to "ARCANE_GOLD" OR just returns "ARCANE_GOLD" if already uppercase
     */
    private static String convertChapterIdToFieldName(String chapterId) {
        // If it's already uppercase and looks like a field name, use it directly
        if (chapterId.equals(chapterId.toUpperCase()) && !chapterId.contains(":")) {
            return chapterId;
        }
        
        // Otherwise convert from resource location format
        if (chapterId.startsWith("eidolon:")) {
            return chapterId.substring(8).toUpperCase();
        } else {
            // Handle cases like "void_amulet" -> "VOID_AMULET"
            return chapterId.toUpperCase();
        }
    }

}
