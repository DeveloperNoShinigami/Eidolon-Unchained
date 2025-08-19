package com.bluelotuscoding.eidolonunchained.data;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.research.ResearchEntry;
import com.bluelotuscoding.eidolonunchained.research.ResearchChapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * Manages loading and registration of custom research entries and chapters from datapacks.
 * This extends Eidolon's research system (separate from the codex system).
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ResearchDataManager extends SimpleJsonResourceReloadListener {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ResearchDataManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    // Storage for loaded research data
    private static final Map<ResourceLocation, ResearchChapter> LOADED_RESEARCH_CHAPTERS = new HashMap<>();
    private static final Map<ResourceLocation, List<ResearchEntry>> RESEARCH_EXTENSIONS = new HashMap<>();
    private static final Map<ResourceLocation, ResearchEntry> LOADED_RESEARCH_ENTRIES = new HashMap<>();
    
    private static ResearchDataManager INSTANCE;
    
    public ResearchDataManager() {
        super(GSON, "research_entries");
        INSTANCE = this;
    }
    
    public static ResearchDataManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ResearchDataManager();
        }
        return INSTANCE;
    }
    
    /**
     * Initialize the ResearchDataManager. Called during mod setup.
     */
    public static void init() {
        LOGGER.info("Initializing ResearchDataManager...");
        // The actual registration happens via @SubscribeEvent methods
        // This method is mainly for logging and ensuring the class is loaded
    }
    
    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(getInstance());
        LOGGER.info("Registered Eidolon Unchained research data reload listener");
    }
    
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        LOGGER.info("Server started - loaded {} custom research chapters and {} research entries", 
                   LOADED_RESEARCH_CHAPTERS.size(), LOADED_RESEARCH_ENTRIES.size());
        
        // Log all loaded content for debugging
        if (!LOADED_RESEARCH_CHAPTERS.isEmpty()) {
            LOGGER.info("Custom research chapters: {}", LOADED_RESEARCH_CHAPTERS.keySet());
        }
        if (!LOADED_RESEARCH_ENTRIES.isEmpty()) {
            LOGGER.info("Custom research entries: {}", LOADED_RESEARCH_ENTRIES.keySet());
        }
    }
    
    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resourceLocationJsonObjectMap, 
                        ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        
        LOADED_RESEARCH_CHAPTERS.clear();
        RESEARCH_EXTENSIONS.clear();
        LOADED_RESEARCH_ENTRIES.clear();
        
        int loadedChapters = 0;
        int loadedEntries = 0;
        int errors = 0;
        
        for (Map.Entry<ResourceLocation, JsonElement> entry : resourceLocationJsonObjectMap.entrySet()) {
            ResourceLocation resourceLocation = entry.getKey();
            JsonElement jsonElement = entry.getValue();
            
            if (!jsonElement.isJsonObject()) {
                LOGGER.warn("Skipping non-object JSON at {}", resourceLocation);
                continue;
            }
            
            JsonObject json = jsonElement.getAsJsonObject();
            
            try {
                String path = resourceLocation.getPath();
                
                if (path.startsWith("research_chapters/")) {
                    loadResearchChapter(resourceLocation, json);
                    loadedChapters++;
                } else if (path.startsWith("research_entries/")) {
                    loadResearchEntry(resourceLocation, json);
                    loadedEntries++;
                } else {
                    LOGGER.debug("Skipping unrecognized research data at {}", resourceLocation);
                }
                
            } catch (Exception e) {
                LOGGER.error("Failed to load research data from {}", resourceLocation, e);
                errors++;
            }
        }
        
        LOGGER.info("Loaded {} research chapters, {} research entries with {} errors", 
                   loadedChapters, loadedEntries, errors);
    }

    /**
     * Loads a custom research chapter from JSON
     */
    private void loadResearchChapter(ResourceLocation location, JsonObject json) {
        try {
            if (!json.has("id")) {
                LOGGER.warn("Research chapter at {} missing required 'id' field", location);
                return;
            }

            ResourceLocation chapterId = ResourceLocation.tryParse(json.get("id").getAsString());
            Component title = json.has("title")
                ? Component.literal(json.get("title").getAsString())
                : Component.literal(chapterId.getPath());
            Component description = json.has("description")
                ? Component.literal(json.get("description").getAsString())
                : Component.literal("");

            // Icon parsing
            ItemStack iconStack = ItemStack.EMPTY;
            if (json.has("icon") && json.get("icon").isJsonObject()) {
                JsonObject iconObj = json.getAsJsonObject("icon");
                if (iconObj.has("item")) {
                    ResourceLocation itemId = ResourceLocation.tryParse(iconObj.get("item").getAsString());
                    Item item = ForgeRegistries.ITEMS.getValue(itemId);
                    if (item != null) {
                        int count = iconObj.has("count") ? iconObj.get("count").getAsInt() : 1;
                        iconStack = new ItemStack(item, count);
                    }
                }
            }

            int sortOrder = json.has("sort_order") ? json.get("sort_order").getAsInt() : 0;
            boolean isSecret = json.has("secret") && json.get("secret").getAsBoolean();
            ResourceLocation background = json.has("background")
                ? ResourceLocation.tryParse(json.get("background").getAsString())
                : null;

            ResearchChapter chapter = new ResearchChapter(
                chapterId,
                title,
                description,
                iconStack,
                sortOrder,
                isSecret,
                background,
                new JsonObject()
            );

            LOADED_RESEARCH_CHAPTERS.put(chapterId, chapter);
            LOGGER.info("Loaded research chapter {}", chapterId);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load research chapter from " + location, e);
        }
    }

    /**
     * Loads a custom research entry from JSON
     */
    private void loadResearchEntry(ResourceLocation location, JsonObject json) {
        try {
            // This would parse the JSON and create ResearchEntry objects
            // For now, just store the location as a placeholder
            if (!json.has("id")) {
                LOGGER.warn("Research entry at {} missing required 'id' field", location);
                return;
            }
            
            ResourceLocation entryId = ResourceLocation.tryParse(json.get("id").getAsString());
            LOADED_RESEARCH_ENTRIES.put(entryId, null); // Placeholder for now
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to load research entry from " + location, e);
        }
    }

    // Public API methods for accessing loaded research data
    
    /**
     * Gets all loaded research chapters
     */
    public static Map<ResourceLocation, ResearchChapter> getLoadedResearchChapters() {
        return new HashMap<>(LOADED_RESEARCH_CHAPTERS);
    }
    
    /**
     * Gets all loaded research entries
     */
    public static Map<ResourceLocation, ResearchEntry> getLoadedResearchEntries() {
        return new HashMap<>(LOADED_RESEARCH_ENTRIES);
    }
    
    /**
     * Gets research extensions by chapter
     */
    public static Map<ResourceLocation, List<ResearchEntry>> getResearchExtensions() {
        return new HashMap<>(RESEARCH_EXTENSIONS);
    }
    
    /**
     * Gets a specific research chapter by ID
     */
    public static ResearchChapter getResearchChapter(ResourceLocation id) {
        return LOADED_RESEARCH_CHAPTERS.get(id);
    }
    
    /**
     * Gets a specific research entry by ID
     */
    public static ResearchEntry getResearchEntry(ResourceLocation id) {
        return LOADED_RESEARCH_ENTRIES.get(id);
    }
    
    /**
     * Checks if a research chapter exists
     */
    public static boolean hasResearchChapter(ResourceLocation id) {
        return LOADED_RESEARCH_CHAPTERS.containsKey(id);
    }
    
    /**
     * Checks if a research entry exists
     */
    public static boolean hasResearchEntry(ResourceLocation id) {
        return LOADED_RESEARCH_ENTRIES.containsKey(id);
    }
    
    /**
     * Log loaded data for debugging
     */
    public static void logLoadedData() {
        LOGGER.info("Server started - loaded {} custom research chapters and {} research entries", 
                   LOADED_RESEARCH_CHAPTERS.size(), LOADED_RESEARCH_ENTRIES.size());
    }
}
