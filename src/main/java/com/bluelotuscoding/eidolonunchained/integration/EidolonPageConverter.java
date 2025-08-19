package com.bluelotuscoding.eidolonunchained.integration;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import elucent.eidolon.codex.CraftingPage;
import elucent.eidolon.codex.CruciblePage;
import elucent.eidolon.codex.EntityPage;
import elucent.eidolon.codex.Page;
import elucent.eidolon.codex.RitualPage;
import elucent.eidolon.codex.TextPage;
import elucent.eidolon.codex.TitlePage;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Converts JSON page definitions to Eidolon Page objects using the exact same structure as Eidolon.
 * Based on decompiled Eidolon classes: EntityPage, TextPage, CraftingPage, etc.
 */
public class EidolonPageConverter {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static Map<String, String> cachedTranslations = new HashMap<>();
    private static boolean translationsLoaded = false;

    /**
     * Initialize the converter - for compatibility with EidolonCodexIntegration
     */
    public static void initialize() {
        LOGGER.info("EidolonPageConverter initialized - using direct Eidolon class constructors");
        // Pre-load translations for better performance
        loadTranslationsFromFile();
        LOGGER.info("Translation cache loaded with {} entries", cachedTranslations.size());
        
        // Log some sample translations for debugging
        String testKey = "eidolonunchained.codex.entry.crystal_rituals.title";
        String testTranslation = getDirectTranslation(testKey);
        LOGGER.info("Test translation for '{}': '{}'", testKey, testTranslation);
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
    public static Page convertPage(JsonObject pageJson) {
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
    }

    /**
     * Create a TextPage - takes just a String parameter
     */
    private static Page createTextPage(JsonObject pageJson) {
        String text = pageJson.has("text") ? pageJson.get("text").getAsString() : "";
        // Translate the text if it's a translation key
        String translatedText = translateText(text);
        LOGGER.debug("TextPage: {} -> {}", text, translatedText);
        return new TextPage(translatedText);
    }

    /**
     * Create a TitlePage - takes just a String parameter like TextPage but renders differently
     * IMPORTANT: TitlePage expects raw translation keys, NOT translated text!
     * TitlePage will automatically append ".title" to get the title and use base key for content
     */
    private static Page createTitlePage(JsonObject pageJson) {
        String text = pageJson.has("text") ? pageJson.get("text").getAsString() : "";

        // For TitlePage, we pass the RAW key, not translated text
        // TitlePage will handle translation internally:
        // - Uses base key for content
        // - Automatically appends ".title" for title
        LOGGER.debug("TitlePage using raw key: {}", text);
        return new TitlePage(text);
    }

    /**
     * Translate text if it's a translation key, otherwise return as-is
     */
    private static String translateText(String text) {
        // If text looks like a translation key (contains dots and starts with mod name)
        if (text.contains(".") && (text.startsWith("eidolonunchained.") || text.startsWith("eidolon."))) {
            LOGGER.info("[DEBUG] Requested translation for key: {}", text);
            // Try direct translation first since it's more reliable
            String directTranslation = getDirectTranslation(text);
            if (directTranslation != null && !directTranslation.equals(text)) {
                LOGGER.info("[DEBUG] Direct translation found for '{}': '{}'", text, directTranslation);
                return directTranslation;
            } else {
                LOGGER.info("[DEBUG] No direct translation found for '{}'.", text);
            }
            // Fallback to Component.translatable
            try {
                Component translated = Component.translatable(text);
                String result = translated.getString();
                LOGGER.info("[DEBUG] Component translation attempt: '{}' -> '{}'", text, result);
                // Check if translation actually occurred
                if (!result.equals(text) && !result.contains("translation.key.not.found")) {
                    LOGGER.info("[DEBUG] Successfully translated via Component: {} -> {}", text, result);
                    return result;
                }
            } catch (Exception e) {
                LOGGER.info("[DEBUG] Component translation failed for: {}, error: {}", text, e.getMessage());
            }
            // Last resort: create readable fallback
            LOGGER.warn("[DEBUG] No translation found for key: {}, creating fallback", text);
            return createFallbackFromKey(text);
        }
        return text;
    }
    
    /**
     * Creates a human-readable fallback from a translation key
     */
    private static String createFallbackFromKey(String key) {
        try {
            // Extract the last part of the key (after the last dot)
            String[] parts = key.split("\\.");
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
                String fallback = titleCase.toString().trim();
                LOGGER.info("Created fallback for key '{}': '{}'", key, fallback);
                return fallback;
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to create fallback for key: {}", key, e);
        }
        
        // Last resort: return the key as-is
        return key;
    }
    
    /**
     * Load translations directly from the language file as a backup
     */
    private static void loadTranslationsFromFile() {
        if (translationsLoaded) {
            return;
        }
        
        try {
            // Try to load the language file directly
            InputStream langStream = EidolonPageConverter.class.getResourceAsStream("/assets/eidolonunchained/lang/en_us.json");
            if (langStream != null) {
                JsonObject langJson = JsonParser.parseReader(new InputStreamReader(langStream)).getAsJsonObject();
                
                for (Map.Entry<String, com.google.gson.JsonElement> entry : langJson.entrySet()) {
                    cachedTranslations.put(entry.getKey(), entry.getValue().getAsString());
                }
                
                translationsLoaded = true;
                LOGGER.info("Loaded {} translation keys from language file", cachedTranslations.size());
                langStream.close();
            } else {
                LOGGER.warn("Could not find language file: /assets/eidolonunchained/lang/en_us.json");
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load translations from file: {}", e.getMessage());
        }
    }
    
    /**
     * Get translation from cached translations or fallback
     */
    private static String getDirectTranslation(String key) {
        loadTranslationsFromFile();
        LOGGER.info("[DEBUG] Looking up direct translation for key: {}", key);
        String translation = cachedTranslations.get(key);
        if (translation != null) {
            LOGGER.info("[DEBUG] Found direct translation for '{}': '{}'", key, translation);
            return translation;
        }
        LOGGER.info("[DEBUG] No direct translation found for '{}'.", key);
        return null;
    }

    /**
     * Create an EntityPage - takes just an EntityType parameter
     */
    private static Page createEntityPage(JsonObject pageJson) {
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
        return new EntityPage(entityType);
    }

    /**
     * Create a CraftingPage - takes an ItemStack parameter
     */
    private static Page createCraftingPage(JsonObject pageJson) {
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
        ItemStack itemStack = new ItemStack(item);
        LOGGER.info("Successfully creating CraftingPage with item: {} ({})", item, itemStack);
        return new CraftingPage(itemStack);
    }

    /**
     * Create a RitualPage - takes a ResourceLocation parameter
     */
    private static Page createRitualPage(JsonObject pageJson) {
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
        return new RitualPage(ritualResource);
    }

    /**
     * Create a CruciblePage - takes a ResourceLocation parameter
     */
    private static Page createCruciblePage(JsonObject pageJson) {
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

        Item item = ForgeRegistries.ITEMS.getValue(recipeResource);
        if (item == null) {
            LOGGER.warn("Item not found for crucible recipe {}, using fallback", recipeId);
            return createFallbackTextPage(pageJson);
        }

        return new CruciblePage(new ItemStack(item), recipeResource);
    }

    /**
     * Create a fallback text page when conversion fails
     */
    private static Page createFallbackTextPage(JsonObject pageJson) {
        String fallbackText = pageJson.has("text") ? pageJson.get("text").getAsString() :
                            "Failed to load page content";
        // Also translate fallback text if it's a translation key
        String translatedText = translateText(fallbackText);
        return new TextPage(translatedText);
    }
}
