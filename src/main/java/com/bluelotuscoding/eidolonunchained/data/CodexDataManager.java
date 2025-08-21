package com.bluelotuscoding.eidolonunchained.data;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.codex.CodexEntry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Manages loading and registration of custom codex categories, chapters and
 * entries supplied through datapacks.
 * <p>
 * Files follow the format described in {@code docs/datapack_overview.md}:
 * categories reside in {@code codex_categories/}, chapters in
 * {@code codex_chapters/} and individual entries in
 * {@code codex_entries/}. Translation keys should use the pattern
 * {@code <namespace>.codex.<category>.<chapter>.<suffix>} so that names and
 * page text can be localised. Research requirements can be declared through
 * each entry's {@code prerequisites} array.</p>
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CodexDataManager extends SimpleJsonResourceReloadListener {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CodexDataManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    // Storage for loaded codex entries and custom chapters
    private static final Map<ResourceLocation, List<CodexEntry>> CHAPTER_EXTENSIONS = new HashMap<>();
    private static final Map<ResourceLocation, CodexEntry> ALL_ENTRIES = new HashMap<>();
    private static final Map<ResourceLocation, ChapterDefinition> CUSTOM_CHAPTERS = new HashMap<>();
    
    private static CodexDataManager INSTANCE;
    
    public CodexDataManager() {
        super(GSON, "codex_entries");
        INSTANCE = this;
    }
    
    public static CodexDataManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new CodexDataManager();
        }
        return INSTANCE;
    }
    
    /**
     * Initialize the CodexDataManager. Called during mod setup.
     */
    public static void init() {
        LOGGER.info("Initializing CodexDataManager...");
        // The actual registration happens via @SubscribeEvent methods
        // This method is mainly for logging and ensuring the class is loaded
    }
    
    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(getInstance());
        LOGGER.info("Registered Eidolon Unchained codex data reload listener");
    }
    
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        LOGGER.info("Server started - loaded {} custom codex entries extending {} chapters and {} new chapters",
                   ALL_ENTRIES.size(), CHAPTER_EXTENSIONS.size(), CUSTOM_CHAPTERS.size());

        // Log all loaded content for debugging
        if (!CHAPTER_EXTENSIONS.isEmpty()) {
            CHAPTER_EXTENSIONS.forEach((chapter, entries) -> {
                LOGGER.info("Chapter '{}' extended with {} entries: {}",
                           chapter, entries.size(),
                           entries.stream().map(e -> e.getId().toString()).toList());
            });
        }

        if (!CUSTOM_CHAPTERS.isEmpty()) {
            LOGGER.info("Custom chapters: {}", CUSTOM_CHAPTERS.keySet());
        }
    }
    
    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resourceLocationJsonObjectMap, 
                        ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        
        LOGGER.info("CodexDataManager.apply() called with {} resources", resourceLocationJsonObjectMap.size());
        
        // Log all detected resources for debugging
        resourceLocationJsonObjectMap.keySet().forEach(location -> {
            LOGGER.info("Found resource: {}", location);
        });
        
        CHAPTER_EXTENSIONS.clear();
        ALL_ENTRIES.clear();
        CUSTOM_CHAPTERS.clear();

        // Load chapter definitions first so entries can reference them
        loadCustomChapters(resourceManager);
        
        int loadedEntries = 0;
        int errors = 0;
        
        for (Map.Entry<ResourceLocation, JsonElement> entry : resourceLocationJsonObjectMap.entrySet()) {
            ResourceLocation resourceLocation = entry.getKey();
            JsonElement jsonElement = entry.getValue();
            
            LOGGER.info("Processing codex entry file: {}", resourceLocation);
            
            if (!jsonElement.isJsonObject()) {
                LOGGER.warn("Skipping non-object JSON at {}", resourceLocation);
                continue;
            }
            
            JsonObject json = jsonElement.getAsJsonObject();
            
            try {
                String path = resourceLocation.getPath();
                
                LOGGER.info("Resource path: {}, processing codex entry...", path);
                
                // All files from this listener are already from codex_entries directory
                LOGGER.info("Attempting to load codex entry...");
                loadCodexEntry(resourceLocation, json);
                loadedEntries++;
                LOGGER.info("Successfully loaded entry! Total loaded: {}", loadedEntries);
                
            } catch (Exception e) {
                LOGGER.error("Error loading codex data from {}: {}", resourceLocation, e.getMessage(), e);
                errors++;
            }
        }
        
        LOGGER.info("Loaded {} codex entries with {} errors", loadedEntries, errors);
    }
    
    /**
     * Loads a codex entry from JSON data
     */
    private void loadCodexEntry(ResourceLocation location, JsonObject json) {
        try {
            LOGGER.info("Loading codex entry from {}", location);
            
            // Log the JSON content for debugging
            LOGGER.info("JSON content keys: {}", json.keySet());
            
            // Validate required fields
            if (!json.has("target_chapter")) {
                throw new JsonParseException("Codex entry missing 'target_chapter' field");
            }
            if (!json.has("pages")) {
                throw new JsonParseException("Codex entry missing 'pages' field");
            }
            
            LOGGER.info("Required fields present, proceeding with parsing...");
            
            // Use the file location as the entry ID
            ResourceLocation entryId = location;
            
            // Parse target chapter - support both direct field names and resource locations
            String targetChapterStr = json.get("target_chapter").getAsString();
            LOGGER.info("Entry {} targets chapter: {}", entryId, targetChapterStr);
            
            ResourceLocation targetChapter;
            
            if (targetChapterStr.contains(":")) {
                // It's a resource location like "eidolon:void_amulet"
                targetChapter = ResourceLocation.tryParse(targetChapterStr);
                LOGGER.info("Parsed as resource location: {}", targetChapter);
            } else {
                // It's a direct field name like "VOID_AMULET" - convert to our namespace
                // First try our namespace for custom chapters
                targetChapter = ResourceLocation.tryParse("eidolonunchained:" + targetChapterStr.toLowerCase());
                LOGGER.info("Converted field name to our custom resource location: {}", targetChapter);
            }
            
            if (targetChapter == null) {
                throw new JsonParseException("Invalid target_chapter format: " + targetChapterStr);
            }
            
            LOGGER.info("About to create CodexEntry...");

            // Basic fields
            String titleStr = json.has("title") ? json.get("title").getAsString() : location.getPath();
            Component title = (titleStr.contains(":") || titleStr.contains(".") || titleStr.startsWith("eidolonunchained:"))
                ? Component.translatable(titleStr)
                : Component.literal(titleStr);
            String descStr = json.has("description") ? json.get("description").getAsString() : "";
            Component description = (descStr.contains(":") || descStr.contains(".") || descStr.startsWith("eidolonunchained:"))
                ? Component.translatable(descStr)
                : Component.literal(descStr);

            // Icon parsing
            ItemStack icon = ItemStack.EMPTY;
            if (json.has("icon")) {
                JsonElement iconElem = json.get("icon");
                if (iconElem.isJsonPrimitive()) {
                    ResourceLocation itemId = ResourceLocation.tryParse(iconElem.getAsString());
                    if (itemId != null) {
                        Item item = ForgeRegistries.ITEMS.getValue(itemId);
                        if (item != null) icon = new ItemStack(item);
                    }
                } else if (iconElem.isJsonObject()) {
                    JsonObject iconObj = iconElem.getAsJsonObject();
                    if (iconObj.has("item")) {
                        ResourceLocation itemId = ResourceLocation.tryParse(iconObj.get("item").getAsString());
                        Item item = itemId != null ? ForgeRegistries.ITEMS.getValue(itemId) : null;
                        if (item != null) {
                            int count = iconObj.has("count") ? iconObj.get("count").getAsInt() : 1;
                            icon = new ItemStack(item, count);
                            if (iconObj.has("nbt")) {
                                try {
                                    CompoundTag tag = TagParser.parseTag(iconObj.get("nbt").getAsString());
                                    icon.setTag(tag);
                                } catch (Exception e) {
                                    LOGGER.warn("Failed to parse icon NBT for {}: {}", entryId, e.getMessage());
                                }
                            }
                        }
                    }
                }
            }

            // Prerequisites
            List<ResourceLocation> prerequisites = new ArrayList<>();
            if (json.has("prerequisites")) {
                JsonArray prereqArray = json.getAsJsonArray("prerequisites");
                for (JsonElement elem : prereqArray) {
                    ResourceLocation prereq = ResourceLocation.tryParse(elem.getAsString());
                    if (prereq != null) prerequisites.add(prereq);
                }
            }

            // Pages
            List<JsonObject> pages = new ArrayList<>();
            JsonArray pagesArray = json.getAsJsonArray("pages");
            for (JsonElement elem : pagesArray) {
                pages.add(elem.getAsJsonObject());
            }

            // Type
            CodexEntry.EntryType type = CodexEntry.EntryType.TEXT;
            if (json.has("type")) {
                String typeStr = json.get("type").getAsString();
                for (CodexEntry.EntryType t : CodexEntry.EntryType.values()) {
                    if (t.getName().equalsIgnoreCase(typeStr)) {
                        type = t;
                        break;
                    }
                }
            }

            // Additional custom data (any unrecognized fields)
            JsonObject additional = new JsonObject();
            Set<String> known = Set.of("target_chapter", "pages", "title", "description", "icon", "prerequisites", "type");
            for (Map.Entry<String, JsonElement> e : json.entrySet()) {
                if (!known.contains(e.getKey())) {
                    additional.add(e.getKey(), e.getValue());
                }
            }

            CodexEntry entry = new CodexEntry(entryId, title, description, targetChapter, icon,
                                              prerequisites, pages, type, additional);

            LOGGER.info("CodexEntry created successfully with {} pages", entry.getPages().size());
            
            // Store in both maps
            ALL_ENTRIES.put(entryId, entry);
            CHAPTER_EXTENSIONS.computeIfAbsent(targetChapter, k -> new ArrayList<>()).add(entry);
            
            LOGGER.info("✓ Successfully loaded codex entry '{}' with {} pages for chapter '{}'", 
                        entryId, entry.getPages().size(), targetChapter);
            
        } catch (Exception e) {
            LOGGER.error("Exception in loadCodexEntry for {}: {}", location, e.getMessage(), e);
            throw new RuntimeException("Failed to load codex entry from " + location, e);
        }
    }

    /**
     * Loads custom chapter definitions from datapacks
     */
    private void loadCustomChapters(ResourceManager resourceManager) {
        LOGGER.info("Searching for custom codex chapters in 'codex_chapters'...");
        resourceManager.listResources("codex_chapters", path -> path.getPath().endsWith(".json"))
            .forEach((resLoc, resource) -> {
                LOGGER.info("Found codex chapter resource: {} (namespace: {}, path: {})", resLoc, resLoc.getNamespace(), resLoc.getPath());
                try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                    JsonObject json = GSON.fromJson(reader, JsonObject.class);
                    if (json == null || !json.has("title")) {
                        LOGGER.warn("Skipping invalid chapter definition at {}", resLoc);
                        return;
                    }

                    String titleStr = json.get("title").getAsString();
                    String iconStr = json.has("icon") ? json.get("icon").getAsString() : "minecraft:book";
                    ResourceLocation icon = ResourceLocation.tryParse(iconStr);

                    String path = resLoc.getPath();
                    path = path.substring("codex_chapters/".length(), path.length() - 5); // remove directory and .json
                    ResourceLocation chapterId = new ResourceLocation(resLoc.getNamespace(), path);

                    // Store the translation key or literal for the chapter title
                    String chapterTitle = (titleStr.contains(":") || titleStr.contains(".") || titleStr.startsWith("eidolonunchained:"))
                        ? Component.translatable(titleStr).getString()
                        : titleStr;

                    LOGGER.info("Registering custom chapter: {} (title: {}, icon: {})", chapterId, chapterTitle, icon);
                    CUSTOM_CHAPTERS.put(chapterId, new ChapterDefinition(chapterTitle, icon));
                    LOGGER.info("Loaded custom chapter definition {}", chapterId);
                } catch (IOException e) {
                    LOGGER.error("Failed to load chapter definition at {}", resLoc, e);
                }
            });
        LOGGER.info("Custom chapter loading complete. Total loaded: {}", CUSTOM_CHAPTERS.size());
    }
    
    /**
     * Gets all loaded codex entries for a specific chapter
     */
    public static List<CodexEntry> getEntriesForChapter(ResourceLocation chapterId) {
        return CHAPTER_EXTENSIONS.getOrDefault(chapterId, new ArrayList<>());
    }
    
    /**
     * Gets all loaded codex entries
     */
    public static Map<ResourceLocation, CodexEntry> getAllEntries() {
        return new HashMap<>(ALL_ENTRIES);
    }
    
    /**
     * Gets all chapter extensions
     */
    public static Map<ResourceLocation, List<CodexEntry>> getAllChapterExtensions() {
        return new HashMap<>(CHAPTER_EXTENSIONS);
    }
    
    /**
     * Gets a specific codex entry by ID
     */
    public static CodexEntry getEntry(ResourceLocation id) {
        return ALL_ENTRIES.get(id);
    }
    
    /**
     * Checks if a codex entry exists
     */
    public static boolean hasEntry(ResourceLocation id) {
        return ALL_ENTRIES.containsKey(id);
    }
    
    /**
     * Checks if a chapter has any extensions
     */
    public static boolean hasExtensions(ResourceLocation chapterId) {
        return CHAPTER_EXTENSIONS.containsKey(chapterId) && !CHAPTER_EXTENSIONS.get(chapterId).isEmpty();
    }

    /**
     * Gets a datapack-defined chapter definition
     */
    public static ChapterDefinition getCustomChapter(ResourceLocation id) {
        return CUSTOM_CHAPTERS.get(id);
    }

    /**
     * Gets all custom chapter definitions
     */
    public static Map<ResourceLocation, ChapterDefinition> getAllCustomChapters() {
        return new HashMap<>(CUSTOM_CHAPTERS);
    }
    
    /**
     * Log loaded data for debugging
     */
    public static void logLoadedData() {
        LOGGER.info("Server started - loaded {} custom codex entries extending {} chapters and {} new chapters",
                   ALL_ENTRIES.size(), CHAPTER_EXTENSIONS.size(), CUSTOM_CHAPTERS.size());

        for (Map.Entry<ResourceLocation, List<CodexEntry>> entry : CHAPTER_EXTENSIONS.entrySet()) {
            LOGGER.info("Chapter '{}' extended with {} entries: {}",
                       entry.getKey(), entry.getValue().size(),
                       entry.getValue().stream().map(e -> e.getId().toString()).toList());
        }

        if (!CUSTOM_CHAPTERS.isEmpty()) {
            LOGGER.info("Custom chapters: {}", CUSTOM_CHAPTERS.keySet());
        }
    }

    /**
     * Represents a datapack-defined chapter with title and icon
     */
    public static class ChapterDefinition {
        private final String title;
        private final ResourceLocation icon;

        public ChapterDefinition(String title, ResourceLocation icon) {
            this.title = title;
            this.icon = icon;
        }

        public String getTitle() {
            return title;
        }

        public ResourceLocation getIcon() {
            return icon;
        }
    }
    
    /**
     * Simplified entry structure for datapack-driven category creation
     * This is used specifically for the JSON datapack system
     */
    public static class DatapackEntry {
        public String title_key;
        public String title;
        public Item icon;
        public List<PageData> pages;

        public DatapackEntry() {
            this.pages = new ArrayList<>();
        }

        public DatapackEntry(String title_key, String title, Item icon) {
            this.title_key = title_key;
            this.title = title;
            this.icon = icon;
            this.pages = new ArrayList<>();
        }
    }
    
    /**
     * Represents page data from JSON files
     */
    public static class PageData {
        public String type;
        public String content;
        public JsonObject data;
        
        public PageData() {}
        
        public PageData(String type, String content) {
            this.type = type;
            this.content = content;
            this.data = new JsonObject();
        }
    }
    
    /**
     * Load DatapackEntry objects from a specific directory by reading actual JSON files
     * This method supports the datapack-driven category system
     */
    public List<DatapackEntry> loadEntriesFromDirectory(ResourceManager resourceManager, String directory) {
        List<DatapackEntry> entries = new ArrayList<>();

        try {
            LOGGER.info("Loading entries from directory: {}", directory);

            Map<ResourceLocation, Resource> resources = resourceManager.listResources(directory,
                loc -> loc.getPath().endsWith(".json"));

            for (Map.Entry<ResourceLocation, Resource> resEntry : resources.entrySet()) {
                ResourceLocation resLoc = resEntry.getKey();
                if (resLoc.getPath().endsWith("_category.json")) continue;

                try (InputStreamReader reader = new InputStreamReader(resEntry.getValue().open(), StandardCharsets.UTF_8)) {
                    JsonObject json = GSON.fromJson(reader, JsonObject.class);
                    if (json == null) continue;

                    String path = resLoc.getPath();
                    String fileName = path.substring(path.lastIndexOf('/') + 1, path.length() - 5);

                    String title = json.has("title") ? json.get("title").getAsString() : fileName;
                    String titleKey = json.has("title_key") ? json.get("title_key").getAsString() : fileName;

                    Item icon = Items.BOOK;
                    if (json.has("icon")) {
                        ResourceLocation iconLoc = ResourceLocation.tryParse(json.get("icon").getAsString());
                        if (iconLoc != null) {
                            Item item = ForgeRegistries.ITEMS.getValue(iconLoc);
                            if (item != null) icon = item;
                        }
                    }

                    DatapackEntry entry = new DatapackEntry(titleKey, title, icon);

                    if (json.has("pages") && json.get("pages").isJsonArray()) {
                        JsonArray pages = json.getAsJsonArray("pages");
                        for (JsonElement elem : pages) {
                            if (!elem.isJsonObject()) continue;
                            JsonObject pageObj = elem.getAsJsonObject();
                            String type = pageObj.has("type") ? pageObj.get("type").getAsString() : "text";
                            String content = pageObj.has("text") ? pageObj.get("text").getAsString() : "";
                            PageData pageData = new PageData(type, content);
                            pageData.data = pageObj;
                            entry.pages.add(pageData);
                        }
                    }

                    entries.add(entry);
                } catch (Exception ex) {
                    LOGGER.error("Error reading codex entry {}", resLoc, ex);
                }
            }

            LOGGER.info("Loaded {} entries from {}", entries.size(), directory);

        } catch (Exception e) {
            LOGGER.error("Failed to load entries from directory: {}", directory, e);
        }

        return entries;
    }
}
