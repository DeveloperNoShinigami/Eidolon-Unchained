package com.bluelotuscoding.eidolonunchained.integration;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.lang.reflect.Constructor;

/**
 * Converts JSON page definitions to Eidolon Page objects using the exact same structure as Eidolon.
 * Based on decompiled Eidolon classes: EntityPage, TextPage, CraftingPage, etc.
 */
public class EidolonPageConverter {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Initialize the converter - for compatibility with EidolonCodexIntegration
     */
    public static void initialize() {
        LOGGER.info("EidolonPageConverter initialized - using direct Eidolon class constructors");
    }

    /**
     * Get list of supported page types - for compatibility with EidolonCodexIntegration
     */
    public static String[] getSupportedPageTypes() {
        return new String[]{"text", "title", "entity", "crafting", "ritual", "crucible"};
    }

    /**
     * Convert a JSON page definition to an Eidolon Page object
     */
    public static Object convertPage(JsonObject pageJson) {
        try {
            String type = pageJson.has("type") ? pageJson.get("type").getAsString().toLowerCase() : "text";
            
            switch (type) {
                case "text":
                    return createTextPage(pageJson);
                case "title":
                    return createTitlePage(pageJson);
                case "entity":
                    return createEntityPage(pageJson);
                case "crafting":
                    return createCraftingPage(pageJson);
                case "ritual":
                    return createRitualPage(pageJson);
                case "crucible":
                    return createCruciblePage(pageJson);
                default:
                    LOGGER.warn("Unknown page type: {}, falling back to text", type);
                    return createTextPage(pageJson);
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to convert page: {}", pageJson, e);
            return createFallbackTextPage(pageJson);
        }
    }

    /**
     * Create a TextPage - takes just a String parameter
     */
    private static Object createTextPage(JsonObject pageJson) {
        try {
            Class<?> textPageClass = Class.forName("elucent.eidolon.codex.TextPage");
            Constructor<?> constructor = textPageClass.getConstructor(String.class);
            
            String text = pageJson.has("text") ? pageJson.get("text").getAsString() : "";
            // Translate the text if it's a translation key
            String translatedText = translateText(text);
            LOGGER.debug("TextPage: {} -> {}", text, translatedText);
            return constructor.newInstance(translatedText);
            
        } catch (Exception e) {
            LOGGER.error("Failed to create TextPage", e);
            return createFallbackTextPage(pageJson);
        }
    }

    /**
     * Create a TitlePage - takes just a String parameter like TextPage but renders differently
     */
    private static Object createTitlePage(JsonObject pageJson) {
        try {
            Class<?> titlePageClass = Class.forName("elucent.eidolon.codex.TitlePage");
            Constructor<?> constructor = titlePageClass.getConstructor(String.class);
            
            String text = pageJson.has("text") ? pageJson.get("text").getAsString() : "";
            // Translate the text if it's a translation key
            String translatedText = translateText(text);
            LOGGER.debug("TitlePage: {} -> {}", text, translatedText);
            return constructor.newInstance(translatedText);
            
        } catch (Exception e) {
            LOGGER.error("Failed to create TitlePage", e);
            return createFallbackTextPage(pageJson);
        }
    }

    /**
     * Translate text if it's a translation key, otherwise return as-is
     */
    private static String translateText(String text) {
        // If text looks like a translation key (contains dots and starts with mod name)
        if (text.contains(".") && (text.startsWith("eidolonunchained.") || text.startsWith("eidolon."))) {
            try {
                Component translated = Component.translatable(text);
                String result = translated.getString();
                
                // If translation failed, the result will be the same as the input key
                if (result.equals(text)) {
                    LOGGER.warn("Translation key not found: {}, using fallback", text);
                    // Try to create a fallback from the key structure
                    String[] parts = text.split("\\.");
                    if (parts.length > 0) {
                        String lastPart = parts[parts.length - 1];
                        // Convert from snake_case to Title Case
                        String[] words = lastPart.split("_");
                        StringBuilder titleCase = new StringBuilder();
                        for (String word : words) {
                            if (word.length() > 0) {
                                titleCase.append(Character.toUpperCase(word.charAt(0)));
                                if (word.length() > 1) {
                                    titleCase.append(word.substring(1).toLowerCase());
                                }
                                titleCase.append(" ");
                            }
                        }
                        return titleCase.toString().trim();
                    }
                    return text;
                } else {
                    LOGGER.debug("Successfully translated: {} -> {}", text, result);
                    return result;
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to translate key: {}, using as-is: {}", text, e.getMessage());
                return text;
            }
        }
        return text;
    }

    /**
     * Create an EntityPage - takes just an EntityType parameter
     */
    private static Object createEntityPage(JsonObject pageJson) {
        try {
            String entityId = pageJson.has("entity") ? pageJson.get("entity").getAsString() : "";
            if (entityId.isEmpty()) {
                LOGGER.warn("Entity page missing entity ID");
                return createFallbackTextPage(pageJson);
            }
            
            // Get EntityType from registry
            ResourceLocation entityResource = ResourceLocation.tryParse(entityId);
            if (entityResource == null) {
                LOGGER.warn("Invalid entity ID: {}", entityId);
                return createFallbackTextPage(pageJson);
            }
            
            EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(entityResource);
            if (entityType == null) {
                LOGGER.warn("Entity type not found: {}", entityId);
                return createFallbackTextPage(pageJson);
            }
            
            // Create EntityPage with EntityType parameter
            Class<?> entityPageClass = Class.forName("elucent.eidolon.codex.EntityPage");
            Constructor<?> constructor = entityPageClass.getConstructor(EntityType.class);
            return constructor.newInstance(entityType);
            
        } catch (Exception e) {
            LOGGER.error("Failed to create EntityPage for: {}", pageJson.get("entity"), e);
            return createFallbackTextPage(pageJson);
        }
    }

    /**
     * Create a CraftingPage - takes an ItemStack parameter
     */
    private static Object createCraftingPage(JsonObject pageJson) {
        try {
            // Support both "recipe" and "item" properties
            String itemId = "";
            if (pageJson.has("recipe")) {
                itemId = pageJson.get("recipe").getAsString();
                LOGGER.info("Creating crafting page for recipe: {}", itemId);
            } else if (pageJson.has("item")) {
                itemId = pageJson.get("item").getAsString();
                LOGGER.info("Creating crafting page for item: {}", itemId);
            }
            
            if (itemId.isEmpty()) {
                LOGGER.warn("Crafting page missing both 'recipe' and 'item' properties");
                return createFallbackTextPage(pageJson);
            }
            
            Item item = null;
            
            // Try to parse as direct item ID first
            ResourceLocation itemResource = ResourceLocation.tryParse(itemId);
            if (itemResource != null) {
                item = ForgeRegistries.ITEMS.getValue(itemResource);
                LOGGER.debug("Direct item lookup for {}: {}", itemId, item);
            }
            
            // If direct lookup failed, try some common recipe->item mappings
            if (item == null) {
                LOGGER.info("Direct item lookup failed for {}, trying recipe mappings", itemId);
                
                // Common Eidolon recipe mappings
                if (itemId.equals("eidolon:arcane_gold_ingot")) {
                    // This is a regular crafting recipe that produces arcane gold ingots
                    item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("eidolon", "arcane_gold_ingot"));
                    LOGGER.info("Mapped crafting recipe to result: {} -> eidolon:arcane_gold_ingot", itemId);
                } else if (itemId.equals("eidolon:arcane_gold_ingot_alchemy")) {
                    // This is a crucible recipe that produces arcane gold ingots
                    item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("eidolon", "arcane_gold_ingot"));
                    LOGGER.info("Mapped crucible recipe to result: {} -> eidolon:arcane_gold_ingot", itemId);
                } else if (itemId.equals("eidolon:crystallization")) {
                    // Try several possible result items for crystallization
                    String[] candidates = {
                        "eidolon:arcane_gold_ingot",
                        "eidolon:arcane_gold_block", 
                        "eidolon:soul_gem",
                        "eidolon:crystalized_void",
                        "minecraft:gold_ingot"
                    };
                    
                    for (String candidate : candidates) {
                        ResourceLocation candidateResource = ResourceLocation.tryParse(candidate);
                        if (candidateResource != null) {
                            Item candidateItem = ForgeRegistries.ITEMS.getValue(candidateResource);
                            if (candidateItem != null) {
                                item = candidateItem;
                                LOGGER.info("Found recipe result item: {} -> {}", itemId, candidate);
                                break;
                            }
                        }
                    }
                } else if (itemId.startsWith("eidolon:")) {
                    // For other eidolon recipes, try to guess the result item
                    String recipeName = itemId.substring(8); // Remove "eidolon:" prefix
                    String[] guesses = {
                        "eidolon:" + recipeName,
                        "eidolon:" + recipeName + "_ingot",
                        "eidolon:" + recipeName + "_gem",
                        "eidolon:" + recipeName + "_crystal"
                    };
                    
                    for (String guess : guesses) {
                        ResourceLocation guessResource = ResourceLocation.tryParse(guess);
                        if (guessResource != null) {
                            Item guessItem = ForgeRegistries.ITEMS.getValue(guessResource);
                            if (guessItem != null) {
                                item = guessItem;
                                LOGGER.info("Guessed recipe result item: {} -> {}", itemId, guess);
                                break;
                            }
                        }
                    }
                }
            }
            
            if (item == null) {
                LOGGER.warn("Could not resolve item for recipe/item: {}, using fallback", itemId);
                return createFallbackTextPage(pageJson);
            }
            
            // Create CraftingPage with ItemStack parameter
            Class<?> craftingPageClass = Class.forName("elucent.eidolon.codex.CraftingPage");
            Constructor<?> constructor = craftingPageClass.getConstructor(ItemStack.class);
            ItemStack itemStack = new ItemStack(item);
            LOGGER.info("Successfully creating CraftingPage with item: {} ({})", item, itemStack);
            return constructor.newInstance(itemStack);
            
        } catch (Exception e) {
            LOGGER.error("Failed to create CraftingPage for: {}", pageJson, e);
            return createFallbackTextPage(pageJson);
        }
    }

    /**
     * Create a RitualPage - takes a ResourceLocation parameter
     */
    private static Object createRitualPage(JsonObject pageJson) {
        try {
            String ritualId = pageJson.has("ritual") ? pageJson.get("ritual").getAsString() : "";
            if (ritualId.isEmpty()) {
                LOGGER.warn("Ritual page missing ritual ID");
                return createFallbackTextPage(pageJson);
            }
            
            ResourceLocation ritualResource = ResourceLocation.tryParse(ritualId);
            if (ritualResource == null) {
                LOGGER.warn("Invalid ritual ID: {}", ritualId);
                return createFallbackTextPage(pageJson);
            }
            
            // Create RitualPage with ResourceLocation parameter
            Class<?> ritualPageClass = Class.forName("elucent.eidolon.codex.RitualPage");
            Constructor<?> constructor = ritualPageClass.getConstructor(ResourceLocation.class);
            return constructor.newInstance(ritualResource);
            
        } catch (Exception e) {
            LOGGER.error("Failed to create RitualPage for: {}", pageJson.get("ritual"), e);
            return createFallbackTextPage(pageJson);
        }
    }

    /**
     * Create a CruciblePage - takes a ResourceLocation parameter
     */
    private static Object createCruciblePage(JsonObject pageJson) {
        try {
            String recipeId = pageJson.has("recipe") ? pageJson.get("recipe").getAsString() : "";
            if (recipeId.isEmpty()) {
                LOGGER.warn("Crucible page missing recipe ID");
                return createFallbackTextPage(pageJson);
            }
            
            ResourceLocation recipeResource = ResourceLocation.tryParse(recipeId);
            if (recipeResource == null) {
                LOGGER.warn("Invalid recipe ID: {}", recipeId);
                return createFallbackTextPage(pageJson);
            }
            
            // Create CruciblePage with ResourceLocation parameter
            Class<?> cruciblePageClass = Class.forName("elucent.eidolon.codex.CruciblePage");
            Constructor<?> constructor = cruciblePageClass.getConstructor(ResourceLocation.class);
            return constructor.newInstance(recipeResource);
            
        } catch (Exception e) {
            LOGGER.error("Failed to create CruciblePage for: {}", pageJson.get("recipe"), e);
            return createFallbackTextPage(pageJson);
        }
    }

    /**
     * Create a fallback text page when conversion fails
     */
    private static Object createFallbackTextPage(JsonObject pageJson) {
        try {
            Class<?> textPageClass = Class.forName("elucent.eidolon.codex.TextPage");
            Constructor<?> constructor = textPageClass.getConstructor(String.class);
            
            String fallbackText = pageJson.has("text") ? pageJson.get("text").getAsString() : 
                                "Failed to load page content";
            // Also translate fallback text if it's a translation key
            String translatedText = translateText(fallbackText);
            return constructor.newInstance(translatedText);
            
        } catch (Exception e) {
            LOGGER.error("Failed to create fallback text page", e);
            return null;
        }
    }
}
