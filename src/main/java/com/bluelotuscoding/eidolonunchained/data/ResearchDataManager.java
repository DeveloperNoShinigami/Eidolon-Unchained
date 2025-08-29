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
import net.minecraft.world.item.Items;
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
        super(GSON, "research");
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

        // Also convert chapters from CodexDataManager to research chapters
        Map<ResourceLocation, CodexDataManager.ChapterDefinition> codexChapters = CodexDataManager.getAllCustomChapters();
        LOGGER.info("Converting {} codex chapters to research chapters", codexChapters.size());
        for (Map.Entry<ResourceLocation, CodexDataManager.ChapterDefinition> entry : codexChapters.entrySet()) {
            try {
                ResourceLocation chapterId = entry.getKey();
                CodexDataManager.ChapterDefinition def = entry.getValue();
                
                // Create a ResearchChapter from the CodexDataManager chapter
                ResearchChapter researchChapter = convertCodexChapterToResearchChapter(chapterId, def);
                LOADED_RESEARCH_CHAPTERS.put(chapterId, researchChapter);
                loadedChapters++;
                LOGGER.info("Converted codex chapter {} to research chapter", chapterId);
            } catch (Exception e) {
                LOGGER.error("Failed to convert codex chapter {} to research chapter", entry.getKey(), e);
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

            ResourceLocation chapterId;
            String idStr = json.get("id").getAsString();
            if (idStr.contains(":")) {
                chapterId = ResourceLocation.tryParse(idStr);
            } else {
                // Default to our mod namespace if no namespace specified
                chapterId = new ResourceLocation(EidolonUnchained.MODID, idStr);
            }
            String titleStr = json.has("title") ? json.get("title").getAsString() : chapterId.getPath();
            Component title = (titleStr.contains(":") || titleStr.contains(".") || titleStr.startsWith("eidolonunchained:"))
                ? Component.translatable(titleStr)
                : Component.literal(titleStr);
            String descStr = json.has("description") ? json.get("description").getAsString() : "";
            Component description = (descStr.contains(":") || descStr.contains(".") || descStr.startsWith("eidolonunchained:"))
                ? Component.translatable(descStr)
                : Component.literal(descStr);

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
     * Loads a custom research entry from JSON - handles both trigger-based and codex-style research
     */
    private void loadResearchEntry(ResourceLocation location, JsonObject json) {
        try {
            if (!json.has("id")) {
                LOGGER.warn("Research entry at {} missing required 'id' field", location);
                return;
            }

            ResourceLocation entryId;
            String idStr = json.get("id").getAsString();
            if (idStr.contains(":")) {
                entryId = ResourceLocation.tryParse(idStr);
            } else {
                // Default to our mod namespace if no namespace specified
                entryId = new ResourceLocation(EidolonUnchained.MODID, idStr);
            }
            if (entryId == null) {
                LOGGER.warn("Invalid research entry id '{}' at {}", json.get("id").getAsString(), location);
                return;
            }

            // Check if this is a trigger-based research (has "triggers" and "stars") or codex-style (has "chapter")
            if (json.has("triggers") && json.has("stars")) {
                // This is a trigger-based research - convert to ResearchEntry format
                loadTriggerBasedResearch(entryId, json);
            } else if (json.has("chapter") || json.has("target_chapter")) {
                // This is a codex-style research entry
                loadCodexStyleResearch(entryId, location, json);
            } else {
                LOGGER.warn("Research entry {} at {} is neither trigger-based nor codex-style - missing required fields", entryId, location);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to load research entry from " + location, e);
        }
    }

    /**
     * Loads a trigger-based research (the original working format)
     */
    private void loadTriggerBasedResearch(ResourceLocation entryId, JsonObject json) {
        // Generate basic research entry data from trigger-based research
        Component title = Component.translatable("eidolonunchained.research." + entryId.getPath() + ".title");
        Component description = Component.translatable("eidolonunchained.research." + entryId.getPath() + ".description");
        
        // Use a default chapter for trigger-based research
        ResourceLocation chapter = new ResourceLocation(EidolonUnchained.MODID, "trigger_research");
        
        // Default icon
        ItemStack icon = new ItemStack(Items.BOOK);
        
        // Parse stars for required_stars field
        int requiredStars = json.has("stars") ? json.get("stars").getAsInt() : 1;
        
        // Parse tasks from the trigger-based format
        Map<Integer, List<com.bluelotuscoding.eidolonunchained.research.tasks.ResearchTask>> tasks = new HashMap<>();
        
        if (json.has("tasks") && json.get("tasks").isJsonObject()) {
            JsonObject tasksObj = json.getAsJsonObject("tasks");
            for (Map.Entry<String, JsonElement> tierEntry : tasksObj.entrySet()) {
                String tierKey = tierEntry.getKey();
                int tier;
                try {
                    tier = Integer.parseInt(tierKey);
                } catch (NumberFormatException ex) {
                    LOGGER.warn("Invalid task tier '{}' in research {}", tierKey, entryId);
                    continue;
                }
                
                List<com.bluelotuscoding.eidolonunchained.research.tasks.ResearchTask> tierTasks = new ArrayList<>();
                if (tierEntry.getValue().isJsonArray()) {
                    JsonArray taskArray = tierEntry.getValue().getAsJsonArray();
                    for (JsonElement taskElement : taskArray) {
                        if (taskElement.isJsonObject()) {
                            JsonObject taskObj = taskElement.getAsJsonObject();
                            com.bluelotuscoding.eidolonunchained.research.tasks.ResearchTask task = parseTask(taskObj, entryId);
                            if (task != null) {
                                tierTasks.add(task);
                                integrateTask(task);
                            }
                        }
                    }
                }
                if (!tierTasks.isEmpty()) {
                    tasks.put(tier, tierTasks);
                }
            }
        }
        
        // Create the research entry
        ResearchEntry entry = new ResearchEntry(
            entryId, title, description, chapter, icon,
            new ArrayList<>(), // prerequisites
            new ArrayList<>(), // unlocks  
            0, 0, // x, y coordinates
            ResearchEntry.ResearchType.BASIC,
            requiredStars,
            new JsonObject(), // additional
            tasks,
            new ArrayList<>() // conditions
        );

        LOADED_RESEARCH_ENTRIES.put(entryId, entry);
        RESEARCH_EXTENSIONS.computeIfAbsent(chapter, k -> new ArrayList<>()).add(entry);
        
        // Ensure the default chapter exists
        if (!LOADED_RESEARCH_CHAPTERS.containsKey(chapter)) {
            // Create a default chapter for trigger-based research
            ResearchChapter defaultChapter = new ResearchChapter(
                chapter,
                Component.translatable("eidolonunchained.research.chapter.trigger_research.title"),
                Component.translatable("eidolonunchained.research.chapter.trigger_research.description"),
                new ItemStack(Items.BOOK),
                100, // sortOrder
                false, // isSecret
                new ResourceLocation("eidolon", "textures/gui/research_bg.png"), // backgroundTexture
                "basics", // category - use a default Eidolon category
                new JsonObject() // additionalData
            );
            LOADED_RESEARCH_CHAPTERS.put(chapter, defaultChapter);
        }

        LOGGER.info("Loaded trigger-based research: {}", entryId);
    }

    /**
     * Parse a task from JSON object
     */
    private com.bluelotuscoding.eidolonunchained.research.tasks.ResearchTask parseTask(JsonObject taskObj, ResourceLocation researchId) {
        if (!taskObj.has("type")) {
            LOGGER.warn("Task in research {} missing type", researchId);
            return null;
        }
        
        String type = taskObj.get("type").getAsString();
        
        // Handle the trigger-based research task format
        switch (type) {
            case "item":
                if (taskObj.has("item")) {
                    ResourceLocation itemId = ResourceLocation.tryParse(taskObj.get("item").getAsString());
                    int count = taskObj.has("count") ? taskObj.get("count").getAsInt() : 1;
                    if (itemId != null) {
                        return new CollectItemsTask(itemId, count);
                    }
                }
                break;
            case "kill":
            case "kill_entity":
                if (taskObj.has("entity")) {
                    ResourceLocation entityId = ResourceLocation.tryParse(taskObj.get("entity").getAsString());
                    int count = taskObj.has("count") ? taskObj.get("count").getAsInt() : 1;
                    if (entityId != null) {
                        return new KillEntitiesTask(entityId, count);
                    }
                }
                break;
            case "craft":
            case "craft_item":
                if (taskObj.has("item")) {
                    ResourceLocation itemId = ResourceLocation.tryParse(taskObj.get("item").getAsString());
                    int count = taskObj.has("count") ? taskObj.get("count").getAsInt() : 1;
                    if (itemId != null) {
                        return new CraftItemsTask(itemId, count);
                    }
                }
                break;
            default:
                LOGGER.warn("Unknown task type '{}' in research {}", type, researchId);
                break;
        }
        
        return null;
    }

    /**
     * Loads a codex-style research entry (the new format)
     */
    private void loadCodexStyleResearch(ResourceLocation entryId, ResourceLocation location, JsonObject json) {
        // Basic fields
        String titleStr = json.has("title") ? json.get("title").getAsString() : entryId.toString();
        Component title = (titleStr.contains(":") || titleStr.contains(".") || titleStr.startsWith("eidolonunchained:"))
            ? Component.translatable(titleStr)
            : Component.literal(titleStr);

        String descStr = json.has("description") ? json.get("description").getAsString() : "";
        Component description = (descStr.contains(":") || descStr.contains(".") || descStr.startsWith("eidolonunchained:"))
            ? Component.translatable(descStr)
            : Component.literal(descStr);

        ResourceLocation chapter = null;
        if (json.has("chapter")) {
            chapter = ResourceLocation.tryParse(json.get("chapter").getAsString());
        } else if (json.has("target_chapter")) {
            // Support legacy "target_chapter" field name
            String chapterStr = json.get("target_chapter").getAsString();
            if (chapterStr.contains(":")) {
                chapter = ResourceLocation.tryParse(chapterStr);
            } else {
                // Default to our mod namespace if no namespace specified
                chapter = new ResourceLocation(EidolonUnchained.MODID, chapterStr);
            }
        }
        if (chapter == null) {
            LOGGER.warn("Research entry {} missing or has invalid 'chapter'/'target_chapter' field", entryId);
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
        }

        // Create the research entry
        ResearchEntry entry = new ResearchEntry(entryId, title, description, chapter, icon,
                                                prerequisites, unlocks, x, y, type, requiredStars, 
                                                new JsonObject(), new HashMap<>(), new ArrayList<>());

        LOADED_RESEARCH_ENTRIES.put(entryId, entry);
        RESEARCH_EXTENSIONS.computeIfAbsent(chapter, k -> new ArrayList<>()).add(entry);

        if (!LOADED_RESEARCH_CHAPTERS.containsKey(chapter)) {
            ENTRIES_WITH_MISSING_CHAPTER.put(entryId, chapter);
        }

        LOGGER.info("Loaded codex-style research: {}", entryId);
    }

    private static void integrateTask(com.bluelotuscoding.eidolonunchained.research.tasks.ResearchTask task) {
        if (task instanceof CollectItemsTask collect) {
            Item item = ForgeRegistries.ITEMS.getValue(collect.getItem());
            if (item != null) {
                Researches.addTask(rand -> new ResearchTask.TaskItems(new ItemStack(item, collect.getCount())));
                return;
            }
        }
        if (task instanceof ExploreBiomesTask explore) {
            Researches.addTask(rand -> new com.bluelotuscoding.eidolonunchained.research.integration.EidolonTaskWrapper.BiomeTaskWrapper(explore));
            return;
        }
        if (task instanceof EnterDimensionTask dimension) {
            Researches.addTask(rand -> new com.bluelotuscoding.eidolonunchained.research.integration.EidolonTaskWrapper.DimensionTaskWrapper(dimension));
            return;
        }
        if (task instanceof WeatherTask weather) {
            Researches.addTask(rand -> new com.bluelotuscoding.eidolonunchained.research.integration.EidolonTaskWrapper.WeatherTaskWrapper(weather));
            return;
        }
        if (task instanceof HasNbtTask nbt) {
            Researches.addTask(rand -> new com.bluelotuscoding.eidolonunchained.research.integration.EidolonTaskWrapper.NbtTaskWrapper(nbt));
            return;
        }
        if (task instanceof KillEntitiesTask kill) {
            // Keep using XP for kill tasks since they're tracked separately
            Researches.addTask(rand -> new ResearchTask.XP(1));
            return;
        }
        if (task instanceof KillEntityWithNbtTask killNbt) {
            // Keep using XP for kill tasks since they're tracked separately  
            Researches.addTask(rand -> new ResearchTask.XP(1));
            return;
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

    /**
     * Converts a CodexDataManager ChapterDefinition to a ResearchChapter
     */
    private static ResearchChapter convertCodexChapterToResearchChapter(ResourceLocation chapterId, CodexDataManager.ChapterDefinition def) {
        // Create a simple research chapter with basic properties
        return new ResearchChapter(
            chapterId,
            def.getTitle(),
            Component.literal("Datapack chapter"), // Default description
            new ItemStack(Items.BOOK), // Default icon
            100, // Default sort order
            false, // Not secret
            new ResourceLocation("eidolon", "textures/gui/research_bg.png"), // Default background
            def.getCategory(), // Use the category from the definition
            new JsonObject() // Empty extra data
        );
    }
}
