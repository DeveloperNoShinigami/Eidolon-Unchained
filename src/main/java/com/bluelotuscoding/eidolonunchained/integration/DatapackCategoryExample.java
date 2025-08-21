package com.bluelotuscoding.eidolonunchained.integration;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.data.CodexDataManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import elucent.eidolon.codex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * JSON Datapack-driven category creation system
 * 
 * 🔄 CURRENT STATUS: FULLY IMPLEMENTED USING REFLECTION
 * 
 * CURRENT VERSION (0.3.8+): 
 * ✅ Uses reflection to access Eidolon internals
 * ✅ Full JSON datapack functionality enabled
 * ✅ Creates custom categories with chapters from JSON
 * 
 * FUTURE VERSION MIGRATION: When CodexEvents become available:
 * - Replace reflection with direct event API access
 * - Keep all JSON datapack functionality as-is
 * - Simply change internal implementation, not external behavior
 * 
 * DETECTION: Check if class "elucent.eidolon.codex.CodexEvents" exists
 * BENEFIT AFTER MIGRATION: Same functionality, cleaner code, no reflection!
 */
public class DatapackCategoryExample {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * Create custom categories populated from JSON datapack files
     * 
     * ✅ TRULY DATAPACK-DRIVEN: Reads category definitions from _category.json files
     * 🔄 FUTURE MIGRATION: Replace reflection with direct CodexEvents API access
     */
    public static void addDatapackCategories(java.util.List<Category> categories) {
        
        LOGGER.info("🎯 Scanning for datapack category definitions...");
        
        try {
            CodexDataManager dataManager = new CodexDataManager();
            ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();

            Map<ResourceLocation, Resource> categoryFiles = resourceManager.listResources("codex",
                loc -> loc.getPath().endsWith("_category.json"));

            int categoriesCreated = 0;

            for (Map.Entry<ResourceLocation, Resource> entry : categoryFiles.entrySet()) {
                ResourceLocation resLoc = entry.getKey();
                if (!resLoc.getNamespace().equals(EidolonUnchained.MODID)) continue;

                String path = resLoc.getPath();
                String categoryKey = path.substring("codex/".length(), path.length() - "/_category.json".length());

                CategoryDefinition categoryDef = loadCategoryDefinition(entry.getValue(), categoryKey);

                if (categoryDef != null) {
                    LOGGER.info("📁 Found category definition: {}", categoryDef.name);

                    createCategoryFromDatapack(categories, dataManager,
                        categoryDef.key,
                        categoryDef.getIconStack(),
                        categoryDef.getColorInt(),
                        "codex/" + categoryDef.key
                    );

                    categoriesCreated++;
                } else {
                    LOGGER.warn("⚠️ No _category.json found for: {}", categoryKey);
                }
            }

            LOGGER.info("✅ Created {} datapack categories from JSON definitions!", categoriesCreated);

        } catch (Exception e) {
            LOGGER.error("❌ Failed to create datapack categories", e);
        }
    }
    
    /**
     * Load category definition from _category.json file
     */
    private static CategoryDefinition loadCategoryDefinition(Resource resource, String categoryKey) {
        try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
            JsonObject json = new Gson().fromJson(reader, JsonObject.class);
            if (json == null) return null;

            String name = json.has("name") ? json.get("name").getAsString() : categoryKey;
            String icon = json.has("icon") ? json.get("icon").getAsString() : "minecraft:book";
            String color = json.has("color") ? json.get("color").getAsString() : "0x555555";
            String description = json.has("description") ? json.get("description").getAsString() : "";

            return new CategoryDefinition(categoryKey, name, icon, color, description);
        } catch (Exception e) {
            LOGGER.error("Failed to load category definition for: {}", categoryKey, e);
            return null;
        }
    }
    
    /**
     * Category definition loaded from _category.json files
     */
    private static class CategoryDefinition {
        final String key;
        final String name;
        final String icon;
        final String color;
        final String description;
        
        CategoryDefinition(String key, String name, String icon, String color, String description) {
            this.key = key;
            this.name = name;
            this.icon = icon;
            this.color = color;
            this.description = description;
        }
        
        ItemStack getIconStack() {
            try {
                ResourceLocation itemId = ResourceLocation.tryParse(icon);
                Item item = itemId != null ? ForgeRegistries.ITEMS.getValue(itemId) : null;
                return item != null ? new ItemStack(item) : new ItemStack(Items.BOOK);
            } catch (Exception e) {
                return new ItemStack(Items.BOOK);
            }
        }
        
        int getColorInt() {
            try {
                return (int) Long.parseLong(color.replace("0x", ""), 16);
            } catch (Exception e) {
                return 0x555555; // Gray fallback
            }
        }
    }
    
    /**
     * Helper method to create a category from datapack files and add it to the categories list
     * ✅ FULLY FUNCTIONAL: Uses reflection-compatible approach
     */
    private static void createCategoryFromDatapack(java.util.List<Category> categories,
                                                 CodexDataManager dataManager,
                                                 String categoryKey,
                                                 ItemStack categoryIcon,
                                                 int categoryColor,
                                                 String jsonDirectory) {
        try {
            Category category = createDatapackCategory(categoryKey, categoryIcon, categoryColor, 
                                                     jsonDirectory, dataManager);
            
            if (category != null) {
                categories.add(category); // Direct addition - reflection handles the rest!
                LOGGER.info("✅ Added datapack category '{}' with JSON content", categoryKey);
            } else {
                LOGGER.warn("⚠️ Failed to create category '{}' - no valid JSON files found", categoryKey);
            }
            
        } catch (Exception e) {
            LOGGER.error("❌ Error creating datapack category '{}'", categoryKey, e);
        }
    }
    
    /**
     * Create a category populated from JSON files in a specific directory
     * ✅ FULLY FUNCTIONAL: Uses reflection-compatible approach
     */
    private static Category createDatapackCategory(String categoryKey,
                                                  ItemStack categoryIcon,
                                                  int categoryColor,
                                                  String jsonDirectory,
                                                  CodexDataManager dataManager) {
        
        try {
            // Load entries from JSON files in the directory
            ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
            List<CodexDataManager.DatapackEntry> entries = dataManager.loadEntriesFromDirectory(resourceManager, jsonDirectory);
            
            if (entries.isEmpty()) {
                LOGGER.warn("No JSON entries found in directory: {}", jsonDirectory);
                return null;
            }
            
            CustomCategoryBuilder builder = new CustomCategoryBuilder(categoryKey)
                .icon(categoryIcon)
                .color(categoryColor);
            
            // Convert JSON entries to chapters
            for (CodexDataManager.DatapackEntry entry : entries) {
                Chapter chapter = convertJsonToChapter(entry, dataManager);
                
                if (chapter != null) {
                    // Use entry icon or default to book
                    ItemStack chapterIcon = entry.icon != null ? 
                        new ItemStack(entry.icon) : 
                        new ItemStack(Items.BOOK);
                    
                    builder.addChapter(chapter, chapterIcon);
                }
            }
            
            return builder.build();
            
        } catch (Exception e) {
            LOGGER.error("Failed to create datapack category '{}'", categoryKey, e);
            return null;
        }
    }
    
    /**
     * Convert a JSON entry to an Eidolon Chapter with full page content
     * ✅ FULLY FUNCTIONAL: Uses reflection-compatible approach
     */
    private static Chapter convertJsonToChapter(CodexDataManager.DatapackEntry entry, CodexDataManager dataManager) {
        try {
            Chapter chapter = new Chapter(entry.title_key);
            
            // Add title page
            chapter.addPage(new TitlePage(entry.title));
            
            // Convert JSON pages to Eidolon pages using our converter
            EidolonPageConverter pageConverter = new EidolonPageConverter();
            
            for (CodexDataManager.PageData pageData : entry.pages) {
                // Convert PageData to JsonObject for the converter
                JsonObject pageJson = convertPageDataToJson(pageData);
                Page convertedPage = EidolonPageConverter.convertPage(pageJson);
                
                if (convertedPage != null) {
                    chapter.addPage(convertedPage);
                } else {
                    LOGGER.warn("Failed to convert page of type '{}' for chapter '{}'", 
                              pageData.type, entry.title);
                }
            }
            
            return chapter;
            
        } catch (Exception e) {
            LOGGER.error("Failed to convert JSON entry '{}' to chapter", entry.title, e);
            return null;
        }
    }
    
    /**
     * Helper method to convert PageData to JsonObject for EidolonPageConverter
     */
    private static JsonObject convertPageDataToJson(CodexDataManager.PageData pageData) {
        JsonObject pageJson = new JsonObject();
        pageJson.addProperty("type", pageData.type);
        pageJson.addProperty("content", pageData.content);
        
        // If PageData has additional JSON data, merge it
        if (pageData.data != null) {
            for (Map.Entry<String, com.google.gson.JsonElement> entry : pageData.data.entrySet()) {
                pageJson.add(entry.getKey(), entry.getValue());
            }
        }
        
        return pageJson;
    }
}
