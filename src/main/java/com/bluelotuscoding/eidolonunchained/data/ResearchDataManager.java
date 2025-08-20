package com.bluelotuscoding.eidolonunchained.data;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.research.ResearchEntry;
import com.bluelotuscoding.eidolonunchained.research.ResearchChapter;
import com.bluelotuscoding.eidolonunchained.research.tasks.*;
import com.bluelotuscoding.eidolonunchained.research.conditions.*;
import elucent.eidolon.api.research.ResearchTask;
import elucent.eidolon.registries.Researches;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Set;

import com.bluelotuscoding.eidolonunchained.research.conditions.DimensionCondition;
import com.bluelotuscoding.eidolonunchained.research.conditions.InventoryCondition;
import com.bluelotuscoding.eidolonunchained.research.conditions.ResearchCondition;
import com.bluelotuscoding.eidolonunchained.research.conditions.TimeCondition;
import com.bluelotuscoding.eidolonunchained.research.conditions.WeatherCondition;

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
        // Register built-in research task types
        ResearchTaskTypes.registerBuiltins();
        // The actual data registration happens via @SubscribeEvent methods
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

        // First load all research chapters from their own directory
        for (Map.Entry<ResourceLocation, JsonObject> chapter : loadResearchChapters(resourceManager).entrySet()) {
            try {
                loadResearchChapter(chapter.getKey(), chapter.getValue());
                loadedChapters++;
            } catch (Exception e) {
                LOGGER.error("Failed to load research chapter from {}", chapter.getKey(), e);
                errors++;
            }
        }

        // Now load research entries passed in by the reload listener
        for (Map.Entry<ResourceLocation, JsonElement> entry : resourceLocationJsonObjectMap.entrySet()) {
            ResourceLocation resourceLocation = entry.getKey();
            JsonElement jsonElement = entry.getValue();

            if (!jsonElement.isJsonObject()) {
                LOGGER.warn("Skipping non-object JSON at {}", resourceLocation);
                continue;
            }

            try {
                loadResearchEntry(resourceLocation, jsonElement.getAsJsonObject());
                loadedEntries++;
            } catch (Exception e) {
                LOGGER.error("Failed to load research entry from {}", resourceLocation, e);
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
     * Scans the resource manager for research chapter JSON files.
     */
    private Map<ResourceLocation, JsonObject> loadResearchChapters(ResourceManager resourceManager) {
        Map<ResourceLocation, JsonObject> chapters = new HashMap<>();

        resourceManager.listResources("research_chapters", path -> path.getPath().endsWith(".json"))
            .forEach((resLoc, resource) -> {
                try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                    JsonObject json = GSON.fromJson(reader, JsonObject.class);
                    if (json != null) {
                        chapters.put(resLoc, json);
                    }
                } catch (IOException e) {
                    LOGGER.error("Failed to read research chapter at {}", resLoc, e);
                }
            });

        return chapters;
    }

    /**
     * Loads a custom research chapter from JSON
     */
    private void loadResearchChapter(ResourceLocation location, JsonObject json) {
        try {
            LOGGER.info("Attempting to load research chapter from resource: {} (namespace: {}, path: {})", location, location.getNamespace(), location.getPath());
            if (!json.has("id")) {
                throw new JsonParseException("Missing required 'id' field");
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
            String category = json.has("category") ? json.get("category").getAsString() : "nature";

            LOGGER.info("Registering research chapter: {} (title: {}, icon: {}, sortOrder: {}, secret: {}, background: {}, category: {})", chapterId, title, iconStack, sortOrder, isSecret, background, category);
            ResearchChapter chapter = new ResearchChapter(
                chapterId,
                title,
                description,
                iconStack,
                sortOrder,
                isSecret,
                background,
                category,
                new JsonObject()
            );

            LOADED_RESEARCH_CHAPTERS.put(chapterId, chapter);
            LOGGER.info("Loaded research chapter {}", chapterId);
        } catch (Exception e) {
            LOGGER.error("Failed to load research chapter from {}: {}", location, e.getMessage(), e);
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

            int requiredStars = -1;
            if (json.has("required_stars")) {
                requiredStars = json.get("required_stars").getAsInt();
            } else if (json.has("additional") && json.get("additional").isJsonObject()) {
                JsonObject addObj = json.getAsJsonObject("additional");
                if (addObj.has("required_stars")) {
                    requiredStars = addObj.get("required_stars").getAsInt();
                    addObj.remove("required_stars");
                }
            }

            // Tasks parsing
            Map<Integer, List<com.bluelotuscoding.eidolonunchained.research.tasks.ResearchTask>> tasks = new HashMap<>();
            JsonObject tasksContainer = null;
            if (json.has("tasks") && json.get("tasks").isJsonObject()) {
                tasksContainer = json.getAsJsonObject("tasks");
            } else if (json.has("additional") && json.get("additional").isJsonObject()) {
                JsonObject addObj = json.getAsJsonObject("additional");
                if (addObj.has("tasks") && addObj.get("tasks").isJsonObject()) {
                    tasksContainer = addObj.getAsJsonObject("tasks");
                    addObj.remove("tasks");
                }
            }

            if (tasksContainer != null) {
                for (Map.Entry<String, JsonElement> tierEntry : tasksContainer.entrySet()) {
                    String tierKey = tierEntry.getKey();
                    int tier;
                    try {
                        tier = Integer.parseInt(tierKey.replace("tier_", ""));
                    } catch (NumberFormatException ex) {
                        LOGGER.warn("Invalid task tier '{}' in research {}", tierKey, entryId);
                        continue;
                    }
                    List<com.bluelotuscoding.eidolonunchained.research.tasks.ResearchTask> tierTasks = new ArrayList<>();
                    JsonArray arr = tierEntry.getValue().getAsJsonArray();
                    for (JsonElement el : arr) {
                        if (!el.isJsonObject()) continue;
                        JsonObject tobj = el.getAsJsonObject();
                        if (!tobj.has("type")) {
                            LOGGER.warn("Task in research {} missing type", entryId);
                            continue;
                        }
                        String typeStr = tobj.get("type").getAsString();
                        ResourceLocation typeId = ResourceLocation.tryParse(typeStr.contains(":") ? typeStr : EidolonUnchained.MODID + ":" + typeStr);
                        if (typeId == null) {
                            LOGGER.warn("Invalid task type '{}' in research {}", typeStr, entryId);
                            continue;
                        }
                        com.bluelotuscoding.eidolonunchained.research.tasks.ResearchTaskType taskType = com.bluelotuscoding.eidolonunchained.research.tasks.ResearchTaskTypes.get(typeId);
                        if (taskType == null) {
                            LOGGER.warn("Unknown task type '{}' in research {}", typeStr, entryId);
                        } else {
                            switch (taskType) {
                                case KILL_ENTITIES -> {
                                    ResourceLocation entity = ResourceLocation.tryParse(tobj.get("entity").getAsString());
                                    int count = tobj.has("count") ? tobj.get("count").getAsInt() : 1;
                                    task = new KillEntitiesTask(entity, count);
                                }
                                case CRAFT_ITEMS -> {
                                    ResourceLocation item = ResourceLocation.tryParse(tobj.get("item").getAsString());
                                    int count = tobj.has("count") ? tobj.get("count").getAsInt() : 1;
                                    task = new CraftItemsTask(item, count);
                                }
                                case USE_RITUAL -> {
                                    ResourceLocation ritual = ResourceLocation.tryParse(tobj.get("ritual").getAsString());
                                    int count = tobj.has("count") ? tobj.get("count").getAsInt() : 1;
                                    task = new UseRitualTask(ritual, count);
                                }
                                case COLLECT_ITEMS -> {
                                    ResourceLocation item = ResourceLocation.tryParse(tobj.get("item").getAsString());
                                    int count = tobj.has("count") ? tobj.get("count").getAsInt() : 1;
                                    task = new CollectItemsTask(item, count);
                                }
                                case ENTER_DIMENSION -> {
                                    ResourceLocation dim = ResourceLocation.tryParse(tobj.get("dimension").getAsString());
                                    task = new EnterDimensionTask(dim);
                                }
                                case TIME_WINDOW -> {
                                    long min = tobj.has("min") ? tobj.get("min").getAsLong() : 0;
                                    long max = tobj.has("max") ? tobj.get("max").getAsLong() : 24000;
                                    task = new TimeWindowTask(min, max);
                                }
                                case WEATHER -> {
                                    String weather = tobj.get("weather").getAsString();
                                    task = new WeatherTask(weather);
                                }
                                case INVENTORY -> {
                                    ResourceLocation item = ResourceLocation.tryParse(tobj.get("item").getAsString());
                                    int count = tobj.has("count") ? tobj.get("count").getAsInt() : 1;
                                    task = new InventoryTask(item, count);
                                }
                            }
                        }
                        try {
                            com.bluelotuscoding.eidolonunchained.research.tasks.ResearchTask task = taskType.decoder().apply(tobj);
                            if (task != null) {
                                tierTasks.add(task);
                                integrateTask(task);
                            }
                        } catch (Exception e) {
                            LOGGER.warn("Failed to parse task of type '{}' in research {}", typeStr, entryId, e);
                        }
                    }
                    if (!tierTasks.isEmpty()) tasks.put(tier, tierTasks);
                }
            }

            // Additional custom fields
            JsonObject additional = new JsonObject();
            Set<String> known = Set.of("id", "title", "description", "chapter", "icon",
                                       "prerequisites", "unlocks", "x", "y", "type", "required_stars", "tasks", "additional");
            for (Map.Entry<String, JsonElement> e : json.entrySet()) {
                if (!known.contains(e.getKey())) {
                    additional.add(e.getKey(), e.getValue());
                }
            }
            if (json.has("additional") && json.get("additional").isJsonObject()) {
                JsonObject addObj = json.getAsJsonObject("additional");
                for (Map.Entry<String, JsonElement> e : addObj.entrySet()) {
                    additional.add(e.getKey(), e.getValue());
                }
            }

            // Conditional requirements
            // This block is maintained for backwards compatibility but datapacks
            // should prefer expressing these as dedicated tasks instead.
            List<ResearchCondition> conditions = new ArrayList<>();
            if (json.has("conditional_requirements") && json.get("conditional_requirements").isJsonObject()) {
                JsonObject cond = json.getAsJsonObject("conditional_requirements");
                if (cond.has("dimension")) {
                    ResourceLocation dim = ResourceLocation.tryParse(cond.get("dimension").getAsString());
                    if (dim != null) conditions.add(new DimensionCondition(dim));
                }
                if (cond.has("time_range") && cond.get("time_range").isJsonObject()) {
                    JsonObject time = cond.getAsJsonObject("time_range");
                    long min = time.has("min") ? time.get("min").getAsLong() : 0;
                    long max = time.has("max") ? time.get("max").getAsLong() : 24000;
                    conditions.add(new TimeCondition(min, max));
                }
                if (cond.has("weather")) {
                    conditions.add(new WeatherCondition(cond.get("weather").getAsString()));
                }
                if (cond.has("inventory") && cond.get("inventory").isJsonArray()) {
                    JsonArray inv = cond.getAsJsonArray("inventory");
                    for (JsonElement el : inv) {
                        if (!el.isJsonObject()) continue;
                        JsonObject obj = el.getAsJsonObject();
                        if (!obj.has("item")) continue;
                        ResourceLocation itemId = ResourceLocation.tryParse(obj.get("item").getAsString());
                        if (itemId == null) continue;
                        int count = obj.has("count") ? obj.get("count").getAsInt() : 1;
                        Item item = ForgeRegistries.ITEMS.getValue(itemId);
                        if (item != null) {
                            conditions.add(new InventoryCondition(item, count));
                        }
                    }
                }
            }

            ResearchEntry entry = new ResearchEntry(entryId, title, description, chapter, icon,
                                                    prerequisites, unlocks, x, y, type, requiredStars, additional, tasks, conditions);

            LOADED_RESEARCH_ENTRIES.put(entryId, entry);
            RESEARCH_EXTENSIONS.computeIfAbsent(chapter, k -> new ArrayList<>()).add(entry);

            if (!LOADED_RESEARCH_CHAPTERS.containsKey(chapter)) {
                ENTRIES_WITH_MISSING_CHAPTER.put(entryId, chapter);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to load research entry from " + location, e);
        }
    }

    private static void integrateTask(com.bluelotuscoding.eidolonunchained.research.tasks.ResearchTask task) {
        if (task instanceof CollectItemsTask collect) {
            Item item = ForgeRegistries.ITEMS.getValue(collect.getItem());
            if (item != null) {
                Researches.addTask(rand -> new ResearchTask.TaskItems(new ItemStack(item, collect.getCount())));
                return;
            }
        }
        // Fallback placeholder task to ensure registration
        Researches.addTask(rand -> new ResearchTask.XP(1));
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
