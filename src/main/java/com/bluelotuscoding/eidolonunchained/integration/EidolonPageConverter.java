package com.bluelotuscoding.eidolonunchained.integration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import elucent.eidolon.codex.CraftingPage;
import elucent.eidolon.codex.CruciblePage;
import elucent.eidolon.codex.EntityPage;
import elucent.eidolon.codex.ListPage;
import elucent.eidolon.codex.ChantPage;
import elucent.eidolon.codex.RuneDescPage;
import elucent.eidolon.codex.RuneIndexPage;
import elucent.eidolon.codex.SignPage;
import elucent.eidolon.codex.Page;
import elucent.eidolon.codex.TitledRitualPage;
import elucent.eidolon.codex.SmeltingPage;
import elucent.eidolon.codex.RitualPage;
import elucent.eidolon.codex.TextPage;
import elucent.eidolon.codex.TitlePage;
import elucent.eidolon.codex.WorktablePage;
import elucent.eidolon.codex.CodexGui;
import elucent.eidolon.api.spells.Spell;
import elucent.eidolon.api.spells.Sign;
import elucent.eidolon.registries.Spells;
import elucent.eidolon.registries.Signs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
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
        return new String[]{"text", "title", "entity", "crafting", "crafting_recipe", "ritual", "ritual_recipe", "crucible", "list", "image", "item_showcase", "workbench", "smelting", "sign", "chant", "rune_desc", "rune_index"};
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
            case "crafting_recipe":
                return createCraftingRecipePage(pageJson);
            case "ritual":
                return createRitualPage(pageJson);
            case "ritual_recipe":
                return createRitualRecipePage(pageJson);
            case "crucible":
                return createCruciblePage(pageJson);
            case "list":
                return createListPage(pageJson);
            case "image":
                return createImagePage(pageJson);
            case "item_showcase":
                return createItemShowcasePage(pageJson);
            case "workbench":
                return createWorkbenchPage(pageJson);
            case "smelting":
                return createSmeltingPage(pageJson);
            case "sign":
                return createSignPage(pageJson);
            case "chant":
                return createChantPage(pageJson);
            case "rune_desc":
                return createRuneDescPage(pageJson);
            case "rune_index":
                return createRuneIndexPage(pageJson);
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
        // If text contains a namespace, treat it as a translation key
        if (text.contains(":")) {
            LOGGER.info("[DEBUG] Requested translation for key: {}", text);
            try {
                String result = Component.translatable(text).getString();
                LOGGER.info("[DEBUG] Component translation attempt: '{}' -> '{}'", text, result);
                if (!result.equals(text) && !result.contains("translation.key.not.found")) {
                    return result;
                }
            } catch (Exception e) {
                LOGGER.info("[DEBUG] Component translation failed for: {}, error: {}", text, e.getMessage());
            }
            return text;
        }

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
            JsonElement recipeElement = pageJson.get("recipe");
            if (recipeElement.isJsonObject()) {
                LOGGER.error("Crafting page 'recipe' must be a string recipe ID, not a JSON object");
                return createFallbackTextPage(pageJson);
            }
            itemId = recipeElement.getAsString();
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
     * Create a CraftingPage tied to a specific recipe ID
     */
    private static Page createCraftingRecipePage(JsonObject pageJson) {
        String recipeId = pageJson.has("recipe") ? pageJson.get("recipe").getAsString() : "";
        if (recipeId.isEmpty()) {
            LOGGER.warn("Crafting recipe page missing recipe ID");
            return createFallbackTextPage(pageJson);
        }

        ResourceLocation recipeResource = ResourceLocation.tryParse(recipeId);
        if (recipeResource == null) {
            LOGGER.warn("Invalid crafting recipe ID: {}", recipeId);
            return createFallbackTextPage(pageJson);
        }

        Item item = ForgeRegistries.ITEMS.getValue(recipeResource);
        if (item == null) {
            LOGGER.warn("Item not found for crafting recipe {}, using fallback", recipeId);
            return createFallbackTextPage(pageJson);
        }

        return new CraftingPage(new ItemStack(item), recipeResource);
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

        // Use TitledRitualPage with a title translation key
        String title = pageJson.has("text") ? pageJson.get("text").getAsString() : "";
        if (title.isEmpty()) {
            LOGGER.warn("Ritual page missing title text");
            return createFallbackTextPage(pageJson);
        }

        // Create TitledRitualPage with translation key and ritual ID
        return new TitledRitualPage(title, ritualResource);
    }

    /**
     * Create a RitualPage tied to a specific ritual recipe ID
     */
    private static Page createRitualRecipePage(JsonObject pageJson) {
        String ritualId = pageJson.has("ritual") ? pageJson.get("ritual").getAsString() : "";
        if (ritualId.isEmpty()) {
            LOGGER.warn("Ritual recipe page missing ritual ID");
            return createFallbackTextPage(pageJson);
        }

        ResourceLocation ritualResource = ResourceLocation.tryParse(ritualId);
        if (ritualResource == null) {
            LOGGER.warn("Invalid ritual recipe ID: {}", ritualId);
            return createFallbackTextPage(pageJson);
        }

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
     * Create a ListPage - takes a key and a list of entries with items
     */
    private static Page createListPage(JsonObject pageJson) {
        if (!pageJson.has("entries") || !pageJson.get("entries").isJsonArray()) {
            LOGGER.warn("List page missing entries array");
            return createFallbackTextPage(pageJson);
        }

        JsonArray entriesArray = pageJson.getAsJsonArray("entries");
        ListPage.ListEntry[] entries = new ListPage.ListEntry[entriesArray.size()];
        for (int i = 0; i < entriesArray.size(); i++) {
            JsonObject entryObj = entriesArray.get(i).getAsJsonObject();
            String itemId = entryObj.has("item") ? entryObj.get("item").getAsString() : "";
            Item item = null;
            if (!itemId.isEmpty()) {
                ResourceLocation itemRes = ResourceLocation.tryParse(itemId);
                if (itemRes != null) {
                    item = ForgeRegistries.ITEMS.getValue(itemRes);
                }
            }
            ItemStack stack = item != null ? new ItemStack(item) : ItemStack.EMPTY;
            String key = entryObj.has("text") ? entryObj.get("text").getAsString() : (item != null ? item.getDescriptionId() : "");
            entries[i] = new ListPage.ListEntry(key, stack);
        }

        String text = pageJson.has("text") ? pageJson.get("text").getAsString() : "";
        return new ListPage(text, entries);
    }

    /**
     * Create an ImagePage that renders a texture in the codex
     */
    private static Page createImagePage(JsonObject pageJson) {
        String image = pageJson.has("image") ? pageJson.get("image").getAsString() : "";
        int width = pageJson.has("width") ? pageJson.get("width").getAsInt() : 128;
        int height = pageJson.has("height") ? pageJson.get("height").getAsInt() : 128;
        if (image.isEmpty()) {
            LOGGER.warn("Image page missing image path");
            return createFallbackTextPage(pageJson);
        }

        ResourceLocation imageRes = ResourceLocation.tryParse(image);
        if (imageRes == null) {
            LOGGER.warn("Invalid image resource: {}", image);
            return createFallbackTextPage(pageJson);
        }

        return new ImagePage(imageRes, width, height);
    }

    /**
     * Create an ItemShowcasePage displaying an item with accompanying text
     */
    private static Page createItemShowcasePage(JsonObject pageJson) {
        String itemId = pageJson.has("item") ? pageJson.get("item").getAsString() : "";
        if (itemId.isEmpty()) {
            LOGGER.warn("Item showcase page missing item ID");
            return createFallbackTextPage(pageJson);
        }

        Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(itemId));
        if (item == null) {
            LOGGER.warn("Invalid item for item showcase: {}", itemId);
            return createFallbackTextPage(pageJson);
        }

        String title = pageJson.has("title") ? translateText(pageJson.get("title").getAsString()) : "";
        String text = pageJson.has("text") ? translateText(pageJson.get("text").getAsString()) : "";

        return new ItemShowcasePage(new ItemStack(item), title, text);
    }

    /**
     * Create a WorkbenchPage (WorktablePage) - takes an item and optional recipe ID
     */
    private static Page createWorkbenchPage(JsonObject pageJson) {
        String itemId = pageJson.has("item") ? pageJson.get("item").getAsString() : "";
        String recipeId = pageJson.has("recipe") ? pageJson.get("recipe").getAsString() : "";

        Item item = null;
        if (!itemId.isEmpty()) {
            ResourceLocation itemRes = ResourceLocation.tryParse(itemId);
            if (itemRes != null) {
                item = ForgeRegistries.ITEMS.getValue(itemRes);
            }
        } else if (!recipeId.isEmpty()) {
            ResourceLocation recipeRes = ResourceLocation.tryParse(recipeId);
            if (recipeRes != null) {
                item = ForgeRegistries.ITEMS.getValue(recipeRes);
            }
        }

        if (item == null) {
            LOGGER.warn("Workbench page missing valid item or recipe");
            return createFallbackTextPage(pageJson);
        }

        if (!recipeId.isEmpty()) {
            ResourceLocation recipeRes = ResourceLocation.tryParse(recipeId);
            if (recipeRes != null) {
                return new WorktablePage(new ItemStack(item), recipeRes);
            }
        }

        return new WorktablePage(new ItemStack(item));
    }

    /**
     * Create a SmeltingPage showing input and result items, with optional recipe ID
     */
    private static Page createSmeltingPage(JsonObject pageJson) {
        String resultId = pageJson.has("result") ? pageJson.get("result").getAsString() : "";
        String inputId = pageJson.has("input") ? pageJson.get("input").getAsString() : "";
        String recipeId = pageJson.has("recipe") ? pageJson.get("recipe").getAsString() : "";

        if (resultId.isEmpty() || inputId.isEmpty()) {
            LOGGER.warn("Smelting page missing result or input item");
            return createFallbackTextPage(pageJson);
        }

        Item resultItem = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(resultId));
        Item inputItem = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(inputId));
        if (resultItem == null || inputItem == null) {
            LOGGER.warn("Invalid items for smelting page: result={}, input={}", resultId, inputId);
            return createFallbackTextPage(pageJson);
        }

        ItemStack resultStack = new ItemStack(resultItem);
        ItemStack inputStack = new ItemStack(inputItem);
        if (!recipeId.isEmpty()) {
            ResourceLocation recipeRes = ResourceLocation.tryParse(recipeId);
            if (recipeRes != null) {
                return new SmeltingPage(resultStack, inputStack, recipeRes);
            }
        }
        return new SmeltingPage(resultStack, inputStack);
    }

    /**
     * Create a SignPage showing a specific spell sign
     */
    private static Page createSignPage(JsonObject pageJson) {
        String signId = pageJson.has("sign") ? pageJson.get("sign").getAsString() : "";
        if (signId.isEmpty()) {
            LOGGER.warn("Sign page missing sign ID");
            return createFallbackTextPage(pageJson);
        }

        ResourceLocation signRes = ResourceLocation.tryParse(signId);
        Sign sign = signRes != null ? Signs.find(signRes) : null;
        if (sign == null) {
            LOGGER.warn("Invalid sign for sign page: {}", signId);
            return createFallbackTextPage(pageJson);
        }

        return new SignPage(sign);
    }

    /**
     * Create a ChantPage showing a spell chant and its signs
     */
    private static Page createChantPage(JsonObject pageJson) {
        String textKey = pageJson.has("text") ? pageJson.get("text").getAsString() : "";
        String spellId = pageJson.has("spell") ? pageJson.get("spell").getAsString() : "";
        if (textKey.isEmpty() || spellId.isEmpty()) {
            LOGGER.warn("Chant page missing text key or spell ID");
            return createFallbackTextPage(pageJson);
        }

        ResourceLocation spellRes = ResourceLocation.tryParse(spellId);
        Spell spell = spellRes != null ? Spells.find(spellRes) : null;
        if (spell == null) {
            LOGGER.warn("Invalid spell for chant page: {}", spellId);
            return createFallbackTextPage(pageJson);
        }

        return new ChantPage(textKey, spell);
    }

    /**
     * Create a RuneDescPage which displays details about a selected rune
     */
    private static Page createRuneDescPage(JsonObject pageJson) {
        return new RuneDescPage();
    }

    /**
     * Create a RuneIndexPage listing available runes
     */
    private static Page createRuneIndexPage(JsonObject pageJson) {
        return new RuneIndexPage();
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

    /**
     * Simple page that displays a centered image
     */
    private static class ImagePage extends Page {
        private final ResourceLocation image;
        private final int width;
        private final int height;

        public ImagePage(ResourceLocation image, int width, int height) {
            super(new ResourceLocation("eidolon", "textures/gui/codex_blank_page.png"));
            this.image = image;
            this.width = width;
            this.height = height;
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public void render(CodexGui gui, GuiGraphics mStack, ResourceLocation bg, int x, int y, int mouseX, int mouseY) {
            int drawX = x + (128 - width) / 2;
            int drawY = y + (160 - height) / 2;
            mStack.blit(image, drawX, drawY, 0, 0, width, height, width, height);
        }
    }

    /**
     * Page that shows an item with a title and description
     */
    private static class ItemShowcasePage extends Page {
        private final ItemStack item;
        private final String title;
        private final String text;

        public ItemShowcasePage(ItemStack item, String title, String text) {
            super(new ResourceLocation("eidolon", "textures/gui/codex_title_page.png"));
            this.item = item;
            this.title = title;
            this.text = text;
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public void render(CodexGui gui, GuiGraphics mStack, ResourceLocation bg, int x, int y, int mouseX, int mouseY) {
            if (!title.isEmpty()) {
                int titleWidth = Minecraft.getInstance().font.width(title);
                drawText(mStack, title, x + 64 - titleWidth / 2, y + 15 - Minecraft.getInstance().font.lineHeight);
            }
            drawItem(mStack, item, x + 56, y + 32, mouseX, mouseY);
            if (!text.isEmpty()) {
                drawWrappingText(mStack, text, x + 4, y + 72, 120);
            }
        }
    }
}
