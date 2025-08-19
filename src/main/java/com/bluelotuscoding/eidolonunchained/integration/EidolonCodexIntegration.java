package com.bluelotuscoding.eidolonunchained.integration;

import com.bluelotuscoding.eidolonunchained.data.CodexDataManager;
import com.bluelotuscoding.eidolonunchained.codex.CodexEntry;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.slf4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * Handles integration with Eidolon's codex system to inject custom entries.
 * Uses direct access to Eidolon's public static Chapter fields for clean integration.
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class EidolonCodexIntegration {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private static boolean integrationAttempted = false;
    private static boolean integrationSuccessful = false;
    
    // Cached Eidolon classes and constructors
    private static Class<?> textPageClass;
    private static Class<?> titlePageClass;
    private static Constructor<?> textPageConstructor;
    private static Constructor<?> titlePageConstructor;
    private static Method addPageMethod;

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            if (!integrationAttempted) {
                attemptEidolonIntegration();
                if (integrationSuccessful) {
                    injectCustomEntries();
                }
            }
        });
    }

    /**
     * Attempts to integrate with Eidolon's codex system using direct class access
     */
    public static void attemptIntegrationIfNeeded() {
        if (!integrationAttempted) {
            LOGGER.info("Starting Eidolon codex integration attempt...");
            attemptEidolonIntegration();
        }
        
        // Always try to inject entries if integration was successful and we have data
        if (integrationSuccessful) {
            injectCustomEntries();
        }
    }

    /**
     * Attempts to integrate with Eidolon's codex system using direct class access
     */
    private static void attemptEidolonIntegration() {
        if (integrationAttempted) return;
        integrationAttempted = true;
        
        LOGGER.info("Starting Eidolon codex integration attempt...");
        
        try {
            LOGGER.info("Attempting to integrate with Eidolon's codex system...");
            
            // Find core Eidolon classes
            LOGGER.info("Loading Eidolon classes...");
            Class<?> codexChaptersClass = Class.forName("elucent.eidolon.codex.CodexChapters");
            Class<?> chapterClass = Class.forName("elucent.eidolon.codex.Chapter");
            Class<?> pageClass = Class.forName("elucent.eidolon.codex.Page");
            LOGGER.info("Successfully loaded core Eidolon classes");
            
            // Find page types we can create
            LOGGER.info("Loading page classes...");
            textPageClass = Class.forName("elucent.eidolon.codex.TextPage");
            titlePageClass = Class.forName("elucent.eidolon.codex.TitlePage");
            LOGGER.info("Successfully loaded page classes");
            
            // Get constructors for creating pages
            LOGGER.info("Getting page constructors...");
            textPageConstructor = textPageClass.getConstructor(String.class);
            titlePageConstructor = titlePageClass.getConstructor(String.class);
            LOGGER.info("Successfully obtained page constructors");
            
            // Get the addPage method
            LOGGER.info("Getting addPage method...");
            addPageMethod = chapterClass.getMethod("addPage", pageClass);
            LOGGER.info("Successfully obtained addPage method");
            
            // Initialize the page converter
            LOGGER.info("Initializing page converter...");
            EidolonPageConverter.initialize();
            LOGGER.info("Page converter initialized");
            
            // Test access to a known chapter (just for logging, doesn't affect success)
            LOGGER.info("Testing chapter access...");
            Field testField = codexChaptersClass.getField("ARCANE_GOLD");
            Object testChapter = testField.get(null);
            LOGGER.info("Successfully accessed test chapter: {}", testChapter);
            
            // Integration is successful if we can access the classes and methods
            LOGGER.info("✓ Successfully found Eidolon CodexChapters class");
            LOGGER.info("✓ Found page constructors and addPage method");
            LOGGER.info("✓ Supported page types: {}", String.join(", ", EidolonPageConverter.getSupportedPageTypes()));
            integrationSuccessful = true;
            
        } catch (Exception e) {
            LOGGER.warn("Could not integrate with Eidolon: {}", e.getMessage());
            LOGGER.debug("Integration error details:", e);
        }
    }

    /**
     * Injects our custom entries into the appropriate Eidolon chapters
     */
    private static void injectCustomEntries() {
        LOGGER.info("=== Starting custom entry injection ===");
        
        if (!integrationSuccessful) {
            LOGGER.warn("Cannot inject entries - integration was not successful");
            return;
        }

        try {
            Class<?> codexChaptersClass = Class.forName("elucent.eidolon.codex.CodexChapters");
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
                    Field chapterField = codexChaptersClass.getField(fieldName);
                    Object chapterObj = chapterField.get(null);
                    
                    if (chapterObj != null) {
                        LOGGER.info("✓ Injecting {} entries into chapter {}", entries.size(), fieldName);
                        
                        for (CodexEntry entry : entries) {
                            injectEntryIntoChapter(chapterObj, entry);
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
            
        } catch (Exception e) {
            LOGGER.error("Failed to inject custom entries", e);
        }
    }

    /**
     * Converts our CodexEntry into Eidolon Page objects and adds them to the chapter
     */
    private static void injectEntryIntoChapter(Object chapterObj, CodexEntry entry) {
        try {
            // Only add pages from the JSON definition; do not always add a TitlePage
            for (JsonObject pageJson : entry.getPages()) {
                Object eidolonPage = EidolonPageConverter.convertPage(pageJson);
                if (eidolonPage != null) {
                    addPageMethod.invoke(chapterObj, eidolonPage);
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

    public static boolean isIntegrationSuccessful() {
        return integrationSuccessful;
    }
}
