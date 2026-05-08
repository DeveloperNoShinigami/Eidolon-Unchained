package com.bluelotuscoding.eidolonunchained.chant;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.util.UnifiedDynamicLoader;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loads chantable mob configs from datapack chantable_mobs directories.
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ChantableMobManager extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = com.bluelotuscoding.eidolonunchained.util.JsonUtils.GSON;

    private static ChantableMobManager INSTANCE;
    private static final Map<ResourceLocation, ChantableMobConfig> CONFIGS = new HashMap<>();

    private static final List<String> DEFAULT_ROLE_ORDER = List.of("defense", "support", "cc", "movement", "offense", "melee");
    private static final Set<String> ALLOWED_ROLES = Set.of("offense", "defense", "support", "cc", "movement", "melee");
    private static final double DEFAULT_MOVEMENT_SPEED = 0.35d;

    private ChantableMobManager() {
        super(GSON, "chantable_mobs");
    }

    public static ChantableMobManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ChantableMobManager();
        }
        return INSTANCE;
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(getInstance());
        LOGGER.info("Registered ChantableMobManager reload listener");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resourceMap, ResourceManager resourceManager, ProfilerFiller profiler) {
        CONFIGS.clear();

        Map<ResourceLocation, JsonObject> entries = UnifiedDynamicLoader.normalizeResourceMap(resourceMap, EidolonUnchained.MODID);
        int loaded = 0;

        for (Map.Entry<ResourceLocation, JsonObject> entry : entries.entrySet()) {
            ChantableMobConfig config = parseConfig(entry.getKey(), entry.getValue());
            if (config == null) {
                continue;
            }

            CONFIGS.put(config.mobId(), config);
            loaded++;
        }

        LOGGER.info("Loaded {} chantable mob configs", loaded);
    }

    private ChantableMobConfig parseConfig(ResourceLocation entryId, JsonObject json) {
        if (json == null) {
            return null;
        }

        boolean enabled = !json.has("enabled") || json.get("enabled").getAsBoolean();
        if (!enabled) {
            return null;
        }

        String mobIdString = json.has("mob") ? json.get("mob").getAsString() : null;

        ResourceLocation mobId;
        if (mobIdString == null || mobIdString.isEmpty()) {
            mobId = entryId;
        } else {
            mobId = ResourceLocation.tryParse(mobIdString);
        }

        if (mobId == null) {
            LOGGER.warn("chantable_mobs: invalid mob id in {}", entryId);
            return null;
        }

        if (!ForgeRegistries.ENTITY_TYPES.containsKey(mobId)) {
            LOGGER.warn("chantable_mobs: unknown entity type '{}' in {}", mobId, entryId);
            return null;
        }

        JsonArray chants = null;
        if (json.has("chant_rotation_ids") && json.get("chant_rotation_ids").isJsonArray()) {
            chants = json.getAsJsonArray("chant_rotation_ids");
        }

        List<String> chantIds = new ArrayList<>();
        if (chants != null) {
            for (JsonElement element : chants) {
                if (element == null || element.isJsonNull()) {
                    continue;
                }
                String chantId = element.getAsString();
                ResourceLocation parsed = ResourceLocation.tryParse(chantId);
                if (parsed != null) {
                    chantIds.add(parsed.toString());
                }
            }
        }

        if (chantIds.isEmpty()) {
            LOGGER.warn("chantable_mobs: no valid chant ids for '{}' in {}", mobId, entryId);
            return null;
        }

        int intervalTicks = json.has("chant_interval_ticks")
            ? Math.max(1, json.get("chant_interval_ticks").getAsInt())
            : 20;

        double minRange = json.has("min_range")
            ? Math.max(0.0d, json.get("min_range").getAsDouble())
            : 2.5d;

        double maxRange = json.has("max_range")
            ? Math.max(0.5d, json.get("max_range").getAsDouble())
            : 16.0d;

        if (maxRange < minRange) {
            maxRange = minRange;
        }

        String deityIdString = json.has("assigned_deity")
            ? json.get("assigned_deity").getAsString()
            : null;

        ResourceLocation assignedDeityId = deityIdString == null ? null : ResourceLocation.tryParse(deityIdString);
        if (assignedDeityId == null) {
            LOGGER.warn("chantable_mobs: missing or invalid assigned deity for '{}' in {}", mobId, entryId);
            return null;
        }

        String startingTitle = json.has("starting_title")
            ? json.get("starting_title").getAsString()
            : null;

        if (startingTitle == null || startingTitle.trim().isEmpty()) {
            LOGGER.warn("chantable_mobs: missing starting_title for '{}' in {}", mobId, entryId);
            return null;
        }

        MobCombatPolicy combatPolicy = parseCombatPolicy(entryId, json.getAsJsonObject("mob_combat_policy"));

        return new ChantableMobConfig(
            mobId,
            chantIds,
            intervalTicks,
            minRange,
            maxRange,
            assignedDeityId,
            startingTitle.trim(),
            combatPolicy
        );
    }

    private MobCombatPolicy parseCombatPolicy(ResourceLocation entryId, JsonObject policyJson) {
        if (policyJson == null) {
            return MobCombatPolicy.defaults();
        }

        boolean allowNativeCombatAi = policyJson.has("allow_native_combat_ai")
            && policyJson.get("allow_native_combat_ai").getAsBoolean();

        int maxThresholdCastsPerCycle = policyJson.has("max_threshold_casts_per_cycle")
            ? Math.max(0, policyJson.get("max_threshold_casts_per_cycle").getAsInt())
            : 2;

        double movementSpeed = policyJson.has("movement_speed")
            ? Math.max(0.0d, policyJson.get("movement_speed").getAsDouble())
            : DEFAULT_MOVEMENT_SPEED;

        List<String> roleOrder = parseRoleOrder(entryId, policyJson.getAsJsonArray("role_order"));

        SupportThresholds supportHpThresholds = parseSupportThresholds(policyJson.getAsJsonObject("support_hp_thresholds"));
        SupportThresholds supportManaThresholds = parseSupportThresholds(policyJson.getAsJsonObject("support_mana_thresholds"));

        return new MobCombatPolicy(
            allowNativeCombatAi,
            maxThresholdCastsPerCycle,
            movementSpeed,
            roleOrder,
            supportHpThresholds,
            supportManaThresholds
        );
    }

    private List<String> parseRoleOrder(ResourceLocation entryId, JsonArray roleArray) {
        if (roleArray == null || roleArray.isEmpty()) {
            return DEFAULT_ROLE_ORDER;
        }

        Set<String> seen = new HashSet<>();
        List<String> parsed = new ArrayList<>();
        for (JsonElement element : roleArray) {
            if (element == null || element.isJsonNull()) {
                continue;
            }

            String rawRole = element.getAsString();
            if (rawRole == null) {
                continue;
            }

            String role = rawRole.trim().toLowerCase();
            if (!ALLOWED_ROLES.contains(role)) {
                LOGGER.warn("chantable_mobs: unknown role '{}' in {}. Ignoring.", rawRole, entryId);
                continue;
            }

            if (seen.add(role)) {
                parsed.add(role);
            }
        }

        return parsed.isEmpty() ? DEFAULT_ROLE_ORDER : List.copyOf(parsed);
    }

    private SupportThresholds parseSupportThresholds(JsonObject thresholdsJson) {
        if (thresholdsJson == null) {
            return new SupportThresholds(null, null);
        }

        ThresholdRule self = parseThresholdRule(thresholdsJson.getAsJsonObject("self"));
        ThresholdRule ally = parseThresholdRule(thresholdsJson.getAsJsonObject("ally"));
        return new SupportThresholds(self, ally);
    }

    private ThresholdRule parseThresholdRule(JsonObject ruleJson) {
        if (ruleJson == null) {
            return null;
        }

        if (!ruleJson.has("below") || !ruleJson.has("chant")) {
            return null;
        }

        double below = Math.max(0.0d, Math.min(1.0d, ruleJson.get("below").getAsDouble()));
        ResourceLocation chantId = ResourceLocation.tryParse(ruleJson.get("chant").getAsString());
        if (chantId == null) {
            return null;
        }

        return new ThresholdRule(below, chantId.toString());
    }

    public static ChantableMobConfig getConfigForMob(Mob mob) {
        ResourceLocation mobId = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        if (mobId == null) {
            return null;
        }
        return CONFIGS.get(mobId);
    }

    public record ChantableMobConfig(
        ResourceLocation mobId,
        List<String> chantRotationIds,
        int intervalTicks,
        double minRange,
        double maxRange,
        ResourceLocation assignedDeityId,
        String startingTitle,
        MobCombatPolicy combatPolicy
    ) {}

    public record MobCombatPolicy(
        boolean allowNativeCombatAi,
        int maxThresholdCastsPerCycle,
        double movementSpeed,
        List<String> roleOrder,
        SupportThresholds supportHpThresholds,
        SupportThresholds supportManaThresholds
    ) {
        public static MobCombatPolicy defaults() {
            return new MobCombatPolicy(false, 2, DEFAULT_MOVEMENT_SPEED, DEFAULT_ROLE_ORDER, new SupportThresholds(null, null), new SupportThresholds(null, null));
        }
    }

    public record SupportThresholds(
        ThresholdRule self,
        ThresholdRule ally
    ) {}

    public record ThresholdRule(
        double below,
        String chantId
    ) {}
}
