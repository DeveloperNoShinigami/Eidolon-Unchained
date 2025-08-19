package com.bluelotuscoding.eidolonunchained.data;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.research.ResearchEntry;
import com.bluelotuscoding.eidolonunchained.research.ResearchChapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Set;

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
    private static final Map<ResourceLocation, ResourceLocation> ENTRIES_WITH_MISSING_CHAPTER = new HashMap<>();
    
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
        ENTRIES_WITH_MISSING_CHAPTER.clear();
        
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

        if (!ENTRIES_WITH_MISSING_CHAPTER.isEmpty()) {
            ENTRIES_WITH_MISSING_CHAPTER.forEach((entryId, chapterId) ->
                LOGGER.warn("Research entry {} references missing chapter {}", entryId, chapterId)
            );
        }
    }

    /**
     * Loads a custom research chapter from JSON
     */
    private void loadResearchChapter(ResourceLocation location, JsonObject json) {
        try {
            // This would parse the JSON and create ResearchChapter objects
            // For now, just store the location as a placeholder
            if (!json.has("id")) {
                LOGGER.warn("Research chapter at {} missing required 'id' field", location);
                return;
            }
            
            ResourceLocation chapterId = ResourceLocation.tryParse(json.get("id").getAsString());
            LOADED_RESEARCH_CHAPTERS.put(chapterId, null); // Placeholder for now
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to load research chapter from " + location, e);
        }
    }

    /**
     * Loads a custom research entry from JSON
     */
    private void loadResearchEntry(ResourceLocation location, JsonObject json) {
        try {
            if (!json.has("id")) {
                LOGGER.warn("Research entry at {} missing required 'id' field", location);
                return;
            }

            ResourceLocation entryId = ResourceLocation.tryParse(json.get("id").getAsString());
            if (entryId == null) {
                LOGGER.warn("Invalid research entry id '{}' at {}", json.get("id").getAsString(), location);
                return;
            }

            // Basic fields
            Component title = json.has("title")
                ? Component.literal(json.get("title").getAsString())
                : Component.literal(entryId.toString());

            Component description = json.has("description")
                ? Component.literal(json.get("description").getAsString())
                : Component.literal("");

            ResourceLocation chapter = null;
            if (json.has("chapter")) {
                chapter = ResourceLocation.tryParse(json.get("chapter").getAsString());
            }
            if (chapter == null) {
                LOGGER.warn("Research entry {} missing or has invalid 'chapter' field", entryId);
                return;
            }

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

            // Unlocks
            List<ResourceLocation> unlocks = new ArrayList<>();
            if (json.has("unlocks")) {
                JsonArray unlocksArray = json.getAsJsonArray("unlocks");
                for (JsonElement elem : unlocksArray) {
                    ResourceLocation unlock = ResourceLocation.tryParse(elem.getAsString());
                    if (unlock != null) unlocks.add(unlock);
                }
            }

            int x = json.has("x") ? json.get("x").getAsInt() : 0;
            int y = json.has("y") ? json.get("y").getAsInt() : 0;

            ResearchEntry.ResearchType type = ResearchEntry.ResearchType.BASIC;
            if (json.has("type")) {
                String typeStr = json.get("type").getAsString();
                for (ResearchEntry.ResearchType t : ResearchEntry.ResearchType.values()) {
                    if (t.getName().equalsIgnoreCase(typeStr)) {
                        type = t;
                        break;
                    }
                }
            }

            // Additional custom fields
            JsonObject additional = new JsonObject();
            Set<String> known = Set.of("id", "title", "description", "chapter", "icon",
                                       "prerequisites", "unlocks", "x", "y", "type");
            for (Map.Entry<String, JsonElement> e : json.entrySet()) {
                if (!known.contains(e.getKey())) {
                    additional.add(e.getKey(), e.getValue());
                }
            }

            ResearchEntry entry = new ResearchEntry(entryId, title, description, chapter, icon,
                                                    prerequisites, unlocks, x, y, type, additional);

            LOADED_RESEARCH_ENTRIES.put(entryId, entry);
            RESEARCH_EXTENSIONS.computeIfAbsent(chapter, k -> new ArrayList<>()).add(entry);

            if (!LOADED_RESEARCH_CHAPTERS.containsKey(chapter)) {
                ENTRIES_WITH_MISSING_CHAPTER.put(entryId, chapter);
            }

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
