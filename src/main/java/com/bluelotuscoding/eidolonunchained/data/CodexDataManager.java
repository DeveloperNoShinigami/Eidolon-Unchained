package com.bluelotuscoding.eidolonunchained.data;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.codex.CodexEntry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Manages loading and registration of custom codex entries that extend existing Eidolon chapters.
 * This allows addon developers and users to add new entries to existing chapters via JSON files.
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CodexDataManager extends SimpleJsonResourceReloadListener {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CodexDataManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    // Storage for loaded codex entries, grouped by target chapter
    private static final Map<ResourceLocation, List<CodexEntry>> CHAPTER_EXTENSIONS = new HashMap<>();
    private static final Map<ResourceLocation, CodexEntry> ALL_ENTRIES = new HashMap<>();
    
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
        LOGGER.info("Server started - loaded {} custom codex entries extending {} chapters", 
                   ALL_ENTRIES.size(), CHAPTER_EXTENSIONS.size());
        
        // Log all loaded content for debugging
        if (!CHAPTER_EXTENSIONS.isEmpty()) {
            CHAPTER_EXTENSIONS.forEach((chapter, entries) -> {
                LOGGER.info("Chapter '{}' extended with {} entries: {}", 
                           chapter, entries.size(), 
                           entries.stream().map(e -> e.getId().toString()).toList());
            });
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
                // It's a direct field name like "VOID_AMULET" - convert to dummy resource location
                targetChapter = ResourceLocation.tryParse("eidolon:" + targetChapterStr.toLowerCase());
                LOGGER.info("Converted field name to resource location: {}", targetChapter);
            }
            
            if (targetChapter == null) {
                throw new JsonParseException("Invalid target_chapter format: " + targetChapterStr);
            }
            
            LOGGER.info("About to create CodexEntry...");
            
            // Create the CodexEntry object
            String title = json.has("title") ? json.get("title").getAsString() : location.getPath();
            CodexEntry entry = CodexEntry.fromDatapack(entryId, title, json.getAsJsonArray("pages"));
            
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
     * Log loaded data for debugging
     */
    public static void logLoadedData() {
        LOGGER.info("Server started - loaded {} custom codex entries extending {} chapters", 
                   ALL_ENTRIES.size(), CHAPTER_EXTENSIONS.size());
        
        for (Map.Entry<ResourceLocation, List<CodexEntry>> entry : CHAPTER_EXTENSIONS.entrySet()) {
            LOGGER.info("Chapter '{}' extended with {} entries: {}", 
                       entry.getKey(), entry.getValue().size(), 
                       entry.getValue().stream().map(e -> e.getId().toString()).toList());
        }
    }
}
