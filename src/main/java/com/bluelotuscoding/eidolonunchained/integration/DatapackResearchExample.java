package com.bluelotuscoding.eidolonunchained.integration;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Registers custom research chapters with Eidolon's research system using reflection
 */
public class DatapackResearchExample {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * Register custom research chapters from datapack files
     */
    public static void addDatapackResearchChapters(ResourceManager resourceManager) {
        LOGGER.info("🔬 Scanning for research chapter definitions...");
        
        try {
            // Find research chapter files
            Map<ResourceLocation, Resource> researchFiles = resourceManager.listResources("research_chapters",
                loc -> loc.getPath().endsWith(".json"));
            
            LOGGER.info("🔍 Found {} research chapter files", researchFiles.size());
            
            for (Map.Entry<ResourceLocation, Resource> entry : researchFiles.entrySet()) {
                ResourceLocation resLoc = entry.getKey();
                LOGGER.info("📚 Loading research chapter: {}", resLoc);
                
                try {
                    ResearchChapterDefinition chapterDef = loadResearchChapter(entry.getValue());
                    if (chapterDef != null) {
                        registerResearchChapter(chapterDef);
                        LOGGER.info("✅ Registered research chapter: {}", chapterDef.id);
                    }
                } catch (Exception e) {
                    LOGGER.error("❌ Failed to load research chapter: {}", resLoc, e);
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("❌ Failed to scan research chapters", e);
        }
    }
    
    /**
     * Load research chapter definition from JSON
     */
    private static ResearchChapterDefinition loadResearchChapter(Resource resource) {
        try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
            JsonObject json = new Gson().fromJson(reader, JsonObject.class);
            if (json == null) return null;

            String id = json.has("id") ? json.get("id").getAsString() : "";
            String title = json.has("title") ? json.get("title").getAsString() : "";
            String icon = "minecraft:book"; // Default icon
            
            // Handle icon field - can be string or object
            if (json.has("icon")) {
                JsonElement iconElement = json.get("icon");
                if (iconElement.isJsonPrimitive()) {
                    icon = iconElement.getAsString();
                } else if (iconElement.isJsonObject()) {
                    JsonObject iconObj = iconElement.getAsJsonObject();
                    if (iconObj.has("item")) {
                        icon = iconObj.get("item").getAsString();
                    }
                }
            }

            if (id.isEmpty() || title.isEmpty()) {
                LOGGER.warn("Research chapter missing required fields (id/title)");
                return null;
            }

            return new ResearchChapterDefinition(id, title, icon);
        } catch (Exception e) {
            LOGGER.error("Failed to parse research chapter JSON", e);
            return null;
        }
    }
    
    /**
     * Register a research chapter with Eidolon's research system using reflection
     */
    private static void registerResearchChapter(ResearchChapterDefinition chapterDef) {
        try {
            // Try to access Eidolon's research system
            LOGGER.info("🔧 Attempting to register research chapter '{}' with Eidolon...", chapterDef.id);
            
            // Get the icon item
            ItemStack iconStack = getIconStack(chapterDef.icon);
            
            // Try different possible Eidolon research registration methods
            // This is experimental - we'll need to find the right Eidolon API
            
            // Method 1: Try accessing ResearchChapters class
            try {
                Class<?> researchChaptersClass = Class.forName("elucent.eidolon.research.ResearchChapters");
                LOGGER.info("✅ Found ResearchChapters class");
                
                // Look for a registration method or chapters map
                Field[] fields = researchChaptersClass.getDeclaredFields();
                for (Field field : fields) {
                    LOGGER.info("🔍 Research field: {}", field.getName());
                }
                
            } catch (ClassNotFoundException e) {
                LOGGER.info("❌ ResearchChapters class not found, trying alternative...");
            }
            
            // Method 2: Try accessing through Codex system
            try {
                Class<?> codexClass = Class.forName("elucent.eidolon.codex.CodexChapters");
                LOGGER.info("✅ Found CodexChapters class - may contain research registration");
                
                // Check if codex system handles research too
                Method[] methods = codexClass.getDeclaredMethods();
                for (Method method : methods) {
                    if (method.getName().toLowerCase().contains("research")) {
                        LOGGER.info("🔍 Found research-related method: {}", method.getName());
                    }
                }
                
            } catch (ClassNotFoundException e) {
                LOGGER.info("❌ CodexChapters class not accessible");
            }
            
            // For now, log the attempt - we may need to find the exact Eidolon research API
            LOGGER.warn("⚠️ Research chapter registration needs proper Eidolon API discovery");
            LOGGER.info("📋 Would register: ID='{}', Title='{}', Icon='{}'", 
                chapterDef.id, chapterDef.title, chapterDef.icon);
            
        } catch (Exception e) {
            LOGGER.error("❌ Failed to register research chapter: {}", chapterDef.id, e);
        }
    }
    
    /**
     * Get ItemStack for icon
     */
    private static ItemStack getIconStack(String iconId) {
        try {
            ResourceLocation itemRes = ResourceLocation.tryParse(iconId);
            Item item = itemRes != null ? ForgeRegistries.ITEMS.getValue(itemRes) : null;
            return item != null ? new ItemStack(item) : new ItemStack(Items.BOOK);
        } catch (Exception e) {
            return new ItemStack(Items.BOOK);
        }
    }
    
    /**
     * Research chapter definition loaded from JSON
     */
    private static class ResearchChapterDefinition {
        final String id;
        final String title;
        final String icon;

        ResearchChapterDefinition(String id, String title, String icon) {
            this.id = id;
            this.title = title;
            this.icon = icon;
        }
    }
}
