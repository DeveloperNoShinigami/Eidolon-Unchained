package com.bluelotuscoding.eidolonunchained.data;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig;
import com.bluelotuscoding.eidolonunchained.ai.AIDeityManager;
import com.bluelotuscoding.eidolonunchained.ai.TaskSystemConfig;
import com.google.gson.*;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.*;
import com.bluelotuscoding.eidolonunchained.util.UnifiedDynamicLoader;
import com.bluelotuscoding.eidolonunchained.util.UnifiedDynamicSystemLoader;

/**
 * Loads deity fates (tasks) from data/<namespace>/fates/<deity_id>/*.json
 * Each fate file must include a "linked_deity" field to associate with an AI deity id.
 * Parsed fates are appended to the corresponding AIDeityConfig.task_config.availableTasks list.
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FateDataLoader extends UnifiedDynamicSystemLoader {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = com.bluelotuscoding.eidolonunchained.util.JsonUtils.GSON;
    private static FateDataLoader INSTANCE;

    // Pending fates if AI configs are not yet linked
    private static final Map<ResourceLocation, List<TaskSystemConfig.TaskTemplate>> PENDING = new HashMap<>();

    // Store original JSON data for fate lookup
    private static final Map<String, JsonObject> FATE_DATA_CACHE = new HashMap<>();

    public FateDataLoader() {
        super(GSON, "fates", "eidolonunchained");
        INSTANCE = this;
    }

    public static FateDataLoader getInstance() {
        if (INSTANCE == null) INSTANCE = new FateDataLoader();
        return INSTANCE;
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(getInstance());
        LOGGER.info("Registered Fate (task) datapack reload listener");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager rm, ProfilerFiller profiler) {
        int loaded = 0;
        int errors = 0;
        PENDING.clear();
        FATE_DATA_CACHE.clear();

        super.apply(map, rm, profiler);

        // After processing entries, try to attach any pending fates
        if (!PENDING.isEmpty()) {
            int attached = 0;
            for (Map.Entry<ResourceLocation, List<TaskSystemConfig.TaskTemplate>> e : new ArrayList<>(PENDING.entrySet())) {
                AIDeityConfig cfg = AIDeityManager.getInstance().getAIConfig(e.getKey());
                if (cfg != null) {
                    cfg.task_config.availableTasks.addAll(e.getValue());
                    attached += e.getValue().size();
                    PENDING.remove(e.getKey());
                }
            }
            if (attached > 0) LOGGER.info("Attached {} pending fates to AI configs", attached);
        }

        LOGGER.info("Loaded fate definitions (pending attached: {})", PENDING.isEmpty() ? 0 : 1);
    }

    @Override
    protected void handleEntry(ResourceLocation location, JsonObject json) {
        try {
            if (json == null || !json.isJsonObject()) {
                LOGGER.warn("Skipping non-object fate JSON: {}", location);
                return;
            }

            // Linked deity is required
            if (!json.has("linked_deity")) {
                LOGGER.warn("Fate {} missing 'linked_deity' field, skipping", location);
                return;
            }
            ResourceLocation deityId = ResourceLocation.tryParse(json.get("linked_deity").getAsString());
            if (deityId == null) {
                LOGGER.warn("Fate {} has invalid 'linked_deity' id", location);
                return;
            }

            TaskSystemConfig.TaskTemplate t = parseFate(json);
            if (t == null) {
                LOGGER.warn("Fate {} could not be parsed", location);
                return;
            }

            // Cache the original JSON data for later lookup
            if (t.taskId != null) {
                FATE_DATA_CACHE.put(t.taskId, json);
            }

            // Try to attach to existing AI config
            AIDeityConfig cfg = AIDeityManager.getInstance().getAIConfig(deityId);
            if (cfg != null) {
                cfg.task_config.availableTasks.add(t);
            } else {
                // Store pending until AI configs are linked
                PENDING.computeIfAbsent(deityId, k -> new ArrayList<>()).add(t);
            }
        } catch (Exception e) {
            LOGGER.error("Error loading fate from {}", location, e);
        }
    }

    private TaskSystemConfig.TaskTemplate parseFate(JsonObject json) {
        TaskSystemConfig.TaskTemplate task = new TaskSystemConfig.TaskTemplate();

        if (json.has("task_id")) task.taskId = json.get("task_id").getAsString();
        if (json.has("display_name")) task.displayName = json.get("display_name").getAsString();
        if (json.has("description")) task.description = json.get("description").getAsString();
        if (json.has("progression_tier")) task.progressionTier = json.get("progression_tier").getAsString();

        // Requirements: accept strings or objects; store as type:JSON for objects
        if (json.has("requirements") && json.get("requirements").isJsonArray()) {
            for (JsonElement el : json.getAsJsonArray("requirements")) {
                if (el.isJsonPrimitive()) {
                    task.requirements.add(el.getAsString());
                } else if (el.isJsonObject()) {
                    JsonObject obj = el.getAsJsonObject();
                    String type = obj.has("type") ? obj.get("type").getAsString() : "unknown";
                    task.requirements.add(type + ":" + obj.toString());
                }
            }
        }

        if (json.has("rewards") && json.get("rewards").isJsonObject()) {
            JsonObject rewards = json.getAsJsonObject("rewards");
            if (rewards.has("reputation")) task.reputationReward = rewards.get("reputation").getAsInt();
            if (rewards.has("commands") && rewards.get("commands").isJsonArray()) {
                for (JsonElement cmd : rewards.getAsJsonArray("commands")) {
                    task.rewardCommands.add(cmd.getAsString());
                }
            }
            if (rewards.has("progression_unlock")) {
                task.progressionUnlock = rewards.get("progression_unlock").getAsString();
            }
        }

        if (json.has("cooldown_hours")) task.cooldownHours = json.get("cooldown_hours").getAsLong();
        if (json.has("repeatable")) task.repeatable = json.get("repeatable").getAsBoolean();
        if (json.has("ai_assignment_context") && json.get("ai_assignment_context").isJsonObject()) {
            task.aiAssignmentContext = json.getAsJsonObject("ai_assignment_context").toString();
        }

        return task;
    }

    /**
     * Get the original JSON data for a fate by task ID
     */
    public static JsonObject getFateData(String taskId) {
        return FATE_DATA_CACHE.get(taskId);
    }

    /**
     * Clear the fate data cache
     */
    public static void clearCache() {
        FATE_DATA_CACHE.clear();
    }
}
