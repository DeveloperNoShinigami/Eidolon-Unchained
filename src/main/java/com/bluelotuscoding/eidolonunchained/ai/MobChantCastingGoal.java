package com.bluelotuscoding.eidolonunchained.ai;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.capability.CapabilityHandler;
import com.bluelotuscoding.eidolonunchained.capability.IPatronData;
import com.bluelotuscoding.eidolonunchained.chant.ChantableMobManager;
import com.bluelotuscoding.eidolonunchained.chant.DatapackChant;
import com.bluelotuscoding.eidolonunchained.chant.DatapackChantManager;
import com.bluelotuscoding.eidolonunchained.network.EidolonUnchainedNetworking;
import com.bluelotuscoding.eidolonunchained.network.MobChantBuildStatePacket;
import com.bluelotuscoding.eidolonunchained.registries.EidolonUnchainedAttributes;
import com.mojang.logging.LogUtils;
import elucent.eidolon.capability.ISoul;
import elucent.eidolon.common.entity.ai.FollowOwnerGoal;
import elucent.eidolon.common.entity.ai.ThrallTargetGoal;
import elucent.eidolon.registries.EidolonAttributes;
import elucent.eidolon.util.EntityUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedCrossbowAttackGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mob AI goal that drives sign-by-sign chant casting from the mob's rotation tags.
 * Uses canUse() / canContinueToUse() for natural target gating via the vanilla goal system.
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MobChantCastingGoal extends Goal {

    private static final Logger LOGGER = LogUtils.getLogger();

    // Rotation / cast state
    public static final String ROTATION_IDS_TAG        = "eu_chant_rotation_ids";
    public static final String ROTATION_INDEX_TAG      = "eu_chant_rotation_index";
    public static final String ROTATION_INTERVAL_TAG   = "eu_chant_rotation_interval_ticks";
    public static final String ROTATION_MIN_RANGE_TAG  = "eu_chant_rotation_min_range";
    public static final String ROTATION_MAX_RANGE_TAG  = "eu_chant_rotation_max_range";
    public static final String ROTATION_REQUIRE_LOS_TAG = "eu_chant_rotation_require_los";
    public static final String ROTATION_ROLE_ORDER_TAG = "eu_chant_role_order";
    public static final String ROTATION_ROLE_ORDER_INDEX_TAG = "eu_chant_role_order_index";
    public static final String ALLOW_NATIVE_COMBAT_AI_TAG = "eu_allow_native_combat_ai";
    public static final String COMBAT_MOVEMENT_SPEED_TAG = "eu_combat_movement_speed";
    public static final String MAX_THRESHOLD_CASTS_PER_CYCLE_TAG = "eu_max_threshold_casts_per_cycle";
    public static final String ROTATION_COOLDOWNS_TAG  = "eu_chant_rotation_cooldowns";
    public static final String CHANT_BUILD_ID_TAG      = "eu_mob_chant_build_id";
    public static final String CHANT_BUILD_PROGRESS_TAG = "eu_mob_chant_build_progress";
    public static final String CHANT_BUILD_NEXT_TICK_TAG = "eu_mob_chant_build_next_tick";

    // Resource / faith state
    public static final String MANA_CURRENT_TAG        = "eu_mob_mana_current";
    public static final String MANA_MAX_TAG            = "eu_mob_mana_max";
    public static final String FAITH_DEITY_TAG         = "eu_mob_faith_deity";
    public static final String FAITH_TITLE_TAG         = "eu_mob_faith_title";
    public static final String SOUL_MANA_INIT_TAG      = "eu_soul_mana_initialized";

    private static final int DEFAULT_INTERVAL_TICKS = 20;
    private static final int BASE_SIGN_STEP_TICKS   = 5;
    private static final int FULL_SEQUENCE_HOLD_TICKS = 10;
    private static final int MIN_SIGN_STEP_TICKS    = 4;
    private static final double DEFAULT_MIN_RANGE   = 2.5d;
    private static final double DEFAULT_MAX_RANGE   = 16.0d;
    private static final List<String> DEFAULT_ROLE_ORDER = List.of("defense", "support", "cc", "movement", "offense", "melee");
    private static final double FACING_DOT_THRESHOLD = 0.6d;
    private static final double DEFAULT_MOB_MANA    = 100.0d;
    private static final UUID MOB_MAGIC_POWER_UUID  = UUID.fromString("d4899ef5-6f4e-4628-b6cc-4cd6d26ef0cd");
    private static final UUID COMBAT_MOBILITY_UUID  = UUID.fromString("4da0ff37-5d93-4c0d-96e6-0de8e8d15df0");
    private static final double DEFAULT_COMBAT_MOBILITY_BONUS = 0.35d;

    // Summon lifecycle tracking: owner UUID → set of summoned entity UUIDs
    public static final String SUMMON_NO_LOOT_TAG = "eu_no_loot_drops";
    private static final ConcurrentHashMap<UUID, Set<UUID>> OWNER_SUMMONS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, UUID>      SUMMON_OWNER  = new ConcurrentHashMap<>();

    /** Called by DatapackChant after spawning a summoned entity. */
    public static void registerSummon(UUID ownerUUID, UUID summonUUID) {
        OWNER_SUMMONS.computeIfAbsent(ownerUUID, k -> ConcurrentHashMap.newKeySet()).add(summonUUID);
        SUMMON_OWNER.put(summonUUID, ownerUUID);
    }

    private final Mob mob;

    // Active cast state (reset on stop)
    private ResourceLocation activeCastId       = null;
    private DatapackChant    activeChant         = null;
    private int              buildProgress       = 0;
    private long             nextStepTick        = 0;
    private int              intervalCooldown    = 0;
    private LivingEntity     castTarget          = null;
    private boolean          awaitingExecution   = false;
    private int              activeRotationIndex = -1;
    private int              activeRoleOrderIndex = -1;

    public MobChantCastingGoal(Mob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel) || !(event.getEntity() instanceof Mob mob)) {
            return;
        }

        applyChantableMobConfig(mob);
        MobResourceProfile resourceProfile = resolveMobResourceProfile(mob);
        ensureMobManaPool(mob, resolveDesiredMaxMana(resourceProfile.maxMana));
        applyMobMagicPowerBonus(mob, resourceProfile.magicPowerBonus);
        syncFollowerTeamMembership(mob, resourceProfile);
        injectEnthrallGoals(mob);

        if (!hasRotationData(mob)) {
            return;
        }

        // Strip vanilla attack goals before adding ours so the mob
        // does not melee/bow-attack independently of the chant system.
        if (!mob.getPersistentData().getBoolean(ALLOW_NATIVE_COMBAT_AI_TAG)) {
            stripNativeCombatGoals(mob);
        }

        boolean alreadyHasGoal = mob.goalSelector.getAvailableGoals().stream()
            .anyMatch(w -> w.getGoal() instanceof MobChantCastingGoal);
        if (!alreadyHasGoal) {
            mob.goalSelector.addGoal(0, new MobChantCastingGoal(mob));
        }
    }

    /**
     * Removes all native melee and ranged attack goals from the mob's goal selector.
     * Called when allow_native_combat_ai=false so vanilla attacks cannot fire alongside chants.
     */
    private static void stripNativeCombatGoals(Mob mob) {
        List<Goal> toRemove = mob.goalSelector.getAvailableGoals().stream()
            .map(WrappedGoal::getGoal)
            .filter(g -> g instanceof MeleeAttackGoal
                      || g instanceof RangedBowAttackGoal
                      || g instanceof RangedCrossbowAttackGoal)
            .toList();
        toRemove.forEach(mob.goalSelector::removeGoal);
        if (!toRemove.isEmpty()) {
            LOGGER.info("[MobChant] Stripped {} native combat goal(s) from {}",
                toRemove.size(), mob.getType().toShortString());
        }
    }

    // -----------------------------------------------------------------------
    // Goal lifecycle
    // -----------------------------------------------------------------------

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        setCombatMobilityState(isValidTarget(target));
        if (isValidTarget(target)) {
            maintainCombatPosture(target);
        }
        if (intervalCooldown > 0) {
            intervalCooldown--;
            return false;
        }

        NextChant next = findNextCastableChant();
        if (next == null) {
            if (isValidTarget(target) && !isNativeCombatFallbackAllowed()) {
                // No castable chant right now but native AI must stay suppressed.
                // Apply a short recheck delay so we don't busy-loop every tick.
                intervalCooldown = Math.max(5, getInterval() / 4);
            }
            return false;
        }

        // Target-requiring chants still need range/LoS checks.
        if (next.chant.requiresTarget()) {
            if (!isValidTarget(target)) return false;
            if (!isWithinCastingLeash(target)) return false;
            if (requiresLos() && !mob.getSensing().hasLineOfSight(target)) return false;
        }

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        setCombatMobilityState(isValidTarget(mob.getTarget()));
        LivingEntity target = castTarget;
        if (isValidTarget(target)) {
            maintainCombatPosture(target);
        }
        if (activeChant == null) return false;
        if (!hasEnoughMana(Math.max(0.0d, activeChant.getManaCost()))) return false;
        // No-target chants don't need a combat target to continue.
        if (!activeChant.requiresTarget()) return true;
        if (!isValidTarget(target)) return false;
        if (!isWithinCastingLeash(target)) return false;
        if (requiresLos() && !mob.getSensing().hasLineOfSight(target)) return false;
        return true;
    }

    @Override
    public void start() {
        NextChant next = findNextCastableChant();
        if (next == null) {
            stop();
            return;
        }
        // No-target chants don't need a combat target.
        if (next.chant.requiresTarget()) {
            castTarget = mob.getTarget();
            if (!isValidTarget(castTarget)) {
                stop();
                return;
            }
        } else {
            castTarget = mob.getTarget(); // may be null; that's fine
        }
        setCombatMobilityState(true);
        activeCastId         = next.id;
        activeChant          = next.chant;
        activeRotationIndex  = next.index;
        activeRoleOrderIndex = next.roleIndex;
        buildProgress = 0;
        nextStepTick  = mob.level().getGameTime();
        awaitingExecution = false;

        CompoundTag data = mob.getPersistentData();
        if (activeCastId != null) {
            data.putString(CHANT_BUILD_ID_TAG, activeCastId.toString());
        }
        data.putInt(CHANT_BUILD_PROGRESS_TAG, 0);
        data.putLong(CHANT_BUILD_NEXT_TICK_TAG, nextStepTick);
    }

    @Override
    public void stop() {
        activeCastId         = null;
        activeChant          = null;
        buildProgress        = 0;
        nextStepTick         = 0;
        awaitingExecution    = false;
        activeRotationIndex  = -1;
        activeRoleOrderIndex = -1;
        setCombatMobilityState(isValidTarget(mob.getTarget()));
        clearBuildNbt();
        castTarget = null;
    }

    @Override
    public void tick() {
        if (activeChant == null || activeCastId == null) return;

        LivingEntity target = castTarget;
        if (target != null) {
            boolean isMovementSpell = activeChant.getCombatRole() == DatapackChant.CombatRole.MOVEMENT;
            // Combat should always be target-locked while this goal is active.
            lockFacingTarget(target, true);
            updateCastingMovement(target, isMovementSpell);
        }

        long now = mob.level().getGameTime();
        if (now < nextStepTick) return;

        int signCount = Math.max(1, activeChant.getSignSequence().size());
        CompoundTag data = mob.getPersistentData();

        if (!awaitingExecution) {
            // Movement-role spells and no-target chants don't require facing.
            // All other roles must face the target before building signs.
            boolean isMovementSpell = activeChant != null
                && activeChant.getCombatRole() == DatapackChant.CombatRole.MOVEMENT;
            boolean skipFacing = isMovementSpell || (activeChant != null && !activeChant.requiresTarget());
            if (!skipFacing && (target == null || !isFacingTarget(target))) {
                nextStepTick = now + 1;
                data.putLong(CHANT_BUILD_NEXT_TICK_TAG, nextStepTick);
                return;
            }

            buildProgress++;
            data.putInt(CHANT_BUILD_PROGRESS_TAG, buildProgress);
            syncBuildState();

            if (buildProgress < signCount) {
                // Still building signs
                nextStepTick = now + resolveStepTicks();
                data.putLong(CHANT_BUILD_NEXT_TICK_TAG, nextStepTick);
                return;
            }

            // Full sequence is visible now; hold it briefly before executing.
            awaitingExecution = true;
            nextStepTick = now + FULL_SEQUENCE_HOLD_TICKS;
            data.putLong(CHANT_BUILD_NEXT_TICK_TAG, nextStepTick);
            return;
        }

        // Full sequence has been displayed long enough — now execute.
        double manaCost = Math.max(0.0d, activeChant.getManaCost());
        ResourceLocation castId = activeCastId;
        int castIndex = activeRotationIndex;
        try {
            activeChant.execute(mob);
            LOGGER.info("[MobChant] {} cast '{}' (index {})", mob.getType().toShortString(), castId, castIndex);
        } catch (Exception e) {
            LOGGER.error("[MobChant] Effect error for '{}' on {}: {}", castId, mob.getType(), e.getMessage());
        }
        // Always advance rotation and set cooldown regardless of effect errors
        consumeMana(manaCost);
        setCooldown(castId.toString(), now, Math.max(0, activeChant.getCooldown()) * 20L);
        advanceRotationIndex(castIndex);
        int nextRoleIdx = advanceRoleOrderIndex(activeRoleOrderIndex);
        LOGGER.info("[MobChant] {} cast '{}' role='{}' → next role_order_idx={}",
            mob.getType().toShortString(), castId,
            activeChant != null ? activeChant.getCombatRole().serializedName() : "?",
            nextRoleIdx);

        // Start inter-cast interval (always respected between casts)
        intervalCooldown = getInterval();
        stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private boolean isValidTarget(LivingEntity target) {
        return target != null && target.isAlive() && target != mob && !mob.isAlliedTo(target);
    }

    private void setCombatMobilityState(boolean active) {
        AttributeInstance movementSpeed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        double mobilityBonus = getConfiguredCombatMobilityBonus();
        if (movementSpeed != null) {
            AttributeModifier existing = movementSpeed.getModifier(COMBAT_MOBILITY_UUID);
            if (active) {
                if (existing == null) {
                    movementSpeed.addTransientModifier(new AttributeModifier(
                        COMBAT_MOBILITY_UUID,
                        "eu_combat_mobility",
                        mobilityBonus,
                        AttributeModifier.Operation.MULTIPLY_TOTAL
                    ));
                } else if (Math.abs(existing.getAmount() - mobilityBonus) > 1.0E-6d) {
                    movementSpeed.removeModifier(existing);
                    movementSpeed.addTransientModifier(new AttributeModifier(
                        COMBAT_MOBILITY_UUID,
                        "eu_combat_mobility",
                        mobilityBonus,
                        AttributeModifier.Operation.MULTIPLY_TOTAL
                    ));
                }
            } else if (existing != null) {
                movementSpeed.removeModifier(existing);
            }
        }

        mob.setSprinting(active);
    }

    private boolean isInRange(LivingEntity target) {
        double min = getMinRange();
        double max = getMaxRange();
        if (max < min) max = min;
        double distSq = mob.distanceToSqr(target);
        return distSq >= min * min && distSq <= max * max;
    }

    private boolean isWithinCastingLeash(LivingEntity target) {
        double max = Math.max(getMinRange(), getMaxRange());
        double leash = Math.max(max + 4.0d, max * 1.5d);
        return mob.distanceToSqr(target) <= leash * leash;
    }

    private boolean requiresLos() {
        return true;
    }

    private double getMinRange() {
        CompoundTag data = mob.getPersistentData();
        return data.contains(ROTATION_MIN_RANGE_TAG)
            ? Math.max(0.0d, data.getDouble(ROTATION_MIN_RANGE_TAG))
            : DEFAULT_MIN_RANGE;
    }

    private double getMaxRange() {
        CompoundTag data = mob.getPersistentData();
        return data.contains(ROTATION_MAX_RANGE_TAG)
            ? Math.max(0.5d, data.getDouble(ROTATION_MAX_RANGE_TAG))
            : DEFAULT_MAX_RANGE;
    }

    private void maintainCombatPosture(LivingEntity target) {
        lockFacingTarget(target, true);

        double min = getMinRange();
        double max = Math.max(min, getMaxRange());
        double distance = Math.sqrt(mob.distanceToSqr(target));

        if (distance > max) {
            mob.getNavigation().moveTo(target, 1.35d);
            lockFacingTarget(target, true);
            return;
        }

        applyCastingStrafe(target);
        lockFacingTarget(target, true);
    }

    private void lockFacingTarget(LivingEntity target, boolean hardLock) {
        if (!hardLock) {
            mob.getLookControl().setLookAt(target, 180.0f, 180.0f);
            return;
        }

        Vec3 toTarget = target.getEyePosition().subtract(mob.getEyePosition());
        double horiz = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);
        if (horiz < 1.0E-6d) {
            return;
        }

        float yaw = (float) (Math.atan2(toTarget.z, toTarget.x) * (180.0d / Math.PI)) - 90.0f;
        float pitch = (float) (-(Math.atan2(toTarget.y, horiz) * (180.0d / Math.PI)));
        mob.setYRot(yaw);
        mob.setYHeadRot(yaw);
        mob.yBodyRot = yaw;
        mob.yRotO = yaw;
        mob.yHeadRotO = yaw;
        mob.yBodyRotO = yaw;
        mob.setXRot(pitch);
        mob.getLookControl().setLookAt(target, 180.0f, 180.0f);
    }

    private void updateCastingMovement(LivingEntity target, boolean isMovementSpell) {
        if (!isMovementSpell) {
            applyCastingStrafe(target);
            lockFacingTarget(target, true);
            return;
        }

        mob.setSprinting(true);

        double min = getMinRange();
        double max = Math.max(min, getMaxRange());
        double distSq = mob.distanceToSqr(target);

        if (distSq > max * max) {
            mob.getNavigation().moveTo(target, 1.35d);
            lockFacingTarget(target, true);
            return;
        }

        if (distSq < min * min) {
            Vec3 away = mob.position().subtract(target.position());
            if (away.lengthSqr() < 1.0E-4d) {
                away = new Vec3(1.0d, 0.0d, 0.0d);
            }
            Vec3 retreat = mob.position().add(away.normalize().scale(Math.max(1.5d, min - Math.sqrt(distSq) + 1.0d)));
            mob.getNavigation().moveTo(retreat.x, mob.getY(), retreat.z, 1.35d);
            lockFacingTarget(target, true);
            return;
        }

        mob.getNavigation().stop();
        lockFacingTarget(target, true);
    }

    private double getConfiguredCombatMobilityBonus() {
        CompoundTag data = mob.getPersistentData();
        if (data.contains(COMBAT_MOVEMENT_SPEED_TAG, Tag.TAG_DOUBLE)) {
            return Math.max(0.0d, data.getDouble(COMBAT_MOVEMENT_SPEED_TAG));
        }
        return DEFAULT_COMBAT_MOBILITY_BONUS;
    }

    private void applyCastingStrafe(LivingEntity target) {
        mob.getNavigation().stop();
        mob.setSprinting(true);

        double min = getMinRange();
        double max = Math.max(min, getMaxRange());
        double distance = Math.sqrt(mob.distanceToSqr(target));
        boolean hasMeleeRole = getRoleOrder(mob.getPersistentData()).contains("melee");

        float forward = 0.0f;
        if (hasMeleeRole) {
            if (distance > max) {
                forward = 0.9f;
            } else if (distance < min) {
                forward = -0.6f;
            } else {
                forward = 0.25f;
            }
        } else {
            // Pure ranged: hold the band and orbit. Retreat hard if pressured,
            // close only when target breaks max range.
            if (distance > max) {
                forward = 0.7f;
            } else if (distance < min) {
                forward = -1.0f;
            } else {
                forward = 0.0f;
            }
        }

        int strafePhase = (int) ((mob.level().getGameTime() / (hasMeleeRole ? 8L : 14L) + mob.getId()) & 1L);
        float sideways = hasMeleeRole
            ? (strafePhase == 0 ? 0.95f : -0.95f)
            : (strafePhase == 0 ? 1.25f : -1.25f);
        mob.getMoveControl().strafe(forward, sideways);
    }

    private NextChant findNextCastableChant() {
        CompoundTag data = mob.getPersistentData();
        ListTag ids = data.getList(ROTATION_IDS_TAG, Tag.TAG_STRING);
        if (ids.isEmpty()) return null;

        LivingEntity target = mob.getTarget();
        boolean hasTarget = isValidTarget(target);

        int size = ids.size();
        int chantStart = Math.floorMod(data.getInt(ROTATION_INDEX_TAG), size);

        List<String> roles = getRoleOrder(data);
        int roleStart = Math.floorMod(data.getInt(ROTATION_ROLE_ORDER_INDEX_TAG), roles.size());

        for (int ri = 0; ri < roles.size(); ri++) {
            int roleIdx = (roleStart + ri) % roles.size();
            String role = roles.get(roleIdx);

            // Only check range eligibility for roles that need a target.
            if (hasTarget && !isRoleEligible(role, target)) {
                continue;
            }

            // Scan pool from current chant index for the next ready chant of this role.
            for (int i = 0; i < size; i++) {
                int index = (chantStart + i) % size;
                ResourceLocation id = ResourceLocation.tryParse(ids.getString(index));
                if (id == null) continue;

                DatapackChant chant = DatapackChantManager.getChant(id);
                if (chant == null) continue;

                if (!chant.getCombatRole().serializedName().equals(role)) continue;
                if (isOnCooldown(id.toString())) continue;
                if (!hasEnoughMana(Math.max(0.0d, chant.getManaCost()))) continue;
                // Skip target-requiring chants when there is no valid target.
                if (chant.requiresTarget() && !hasTarget) continue;

                return new NextChant(roleIdx, index, id, chant);
            }
        }

        return null;
    }

    private List<String> getRoleOrder(CompoundTag data) {
        ListTag configured = data.getList(ROTATION_ROLE_ORDER_TAG, Tag.TAG_STRING);
        if (configured.isEmpty()) {
            return DEFAULT_ROLE_ORDER;
        }

        List<String> ordered = new ArrayList<>();
        for (Tag value : configured) {
            String role = value.getAsString().trim().toLowerCase(Locale.ROOT);
            if (!role.isEmpty()) {
                ordered.add(role);
            }
        }

        return ordered.isEmpty() ? DEFAULT_ROLE_ORDER : ordered;
    }

    private boolean isNativeCombatFallbackAllowed() {
        return mob.getPersistentData().contains(ALLOW_NATIVE_COMBAT_AI_TAG)
            && mob.getPersistentData().getBoolean(ALLOW_NATIVE_COMBAT_AI_TAG);
    }

    private boolean isRoleEligible(String role, LivingEntity target) {
        double distance = Math.sqrt(mob.distanceToSqr(target));
        double min = getMinRange();
        double max = Math.max(min, getMaxRange());

        // Range semantics:
        //   min_range = max melee reach
        //   max_range = max chase distance
        //
        // Role mode inference from role_order:
        //   pure melee  = has melee and no ranged-combat roles
        //   hybrid      = has melee and at least one ranged-combat role
        //   pure ranged = no melee role
        List<String> roleOrder = getRoleOrder(mob.getPersistentData());
        boolean hasMeleeRole = roleOrder.contains("melee");
        boolean hasRangedCombatRole = roleOrder.contains("offense")
            || roleOrder.contains("cc")
            || roleOrder.contains("support")
            || roleOrder.contains("defense");
        int meleeIndex = roleOrder.indexOf("melee");
        int firstRangedIndex = roleOrder.size();
        for (String rangedRole : List.of("offense", "cc", "support", "defense")) {
            int idx = roleOrder.indexOf(rangedRole);
            if (idx >= 0) {
                firstRangedIndex = Math.min(firstRangedIndex, idx);
            }
        }
        boolean meleeFocusedHybrid = hasMeleeRole
            && hasRangedCombatRole
            && meleeIndex >= 0
            && meleeIndex < firstRangedIndex;

        return switch (role) {
            case "melee"    -> distance <= min;
            case "movement" -> {
                if (hasMeleeRole && !hasRangedCombatRole) {
                    // Pure melee: chase whenever target is outside melee reach.
                    yield distance > min;
                }
                if (meleeFocusedHybrid) {
                    // Melee-focused hybrid: close distance first, then melee, then utility/ranged.
                    yield distance > min;
                }
                if (hasMeleeRole) {
                    // Ranged-focused hybrid: only chase when target escapes max chase distance.
                    // In-band distance is handled by ranged roles, close distance by melee.
                    yield distance > max;
                }
                // Pure ranged: movement spells are gap-closers only.
                // When a target is too close, combat posture retreat/strafe handles spacing
                // so offense/cc/support can still cycle instead of movement spamming.
                yield distance > max;
            }
            default         -> distance >= min && distance <= max;
        };
    }

    /**
     * Advances the role_order index by one after each cast so the mob cycles through
     * behavioral roles in sequence rather than always re-selecting the top-priority role.
     * Returns the new index for logging.
     */
    private int advanceRoleOrderIndex(int currentRoleIdx) {
        CompoundTag data = mob.getPersistentData();
        List<String> roles = getRoleOrder(data);
        if (roles.isEmpty()) return 0;
        int next = Math.floorMod(currentRoleIdx + 1, roles.size());
        data.putInt(ROTATION_ROLE_ORDER_INDEX_TAG, next);
        return next;
    }

    private void advanceRotationIndex(int currentIndex) {
        CompoundTag data = mob.getPersistentData();
        ListTag ids = data.getList(ROTATION_IDS_TAG, Tag.TAG_STRING);
        if (ids.isEmpty()) return;
        int index = currentIndex >= 0
            ? Math.floorMod(currentIndex, ids.size())
            : Math.floorMod(data.getInt(ROTATION_INDEX_TAG), ids.size());
        data.putInt(ROTATION_INDEX_TAG, (index + 1) % ids.size());
    }

    private int getInterval() {
        CompoundTag data = mob.getPersistentData();
        return data.contains(ROTATION_INTERVAL_TAG)
            ? Math.max(1, data.getInt(ROTATION_INTERVAL_TAG))
            : DEFAULT_INTERVAL_TICKS;
    }

    private int resolveStepTicks() {
        AttributeInstance attr = mob.getAttribute(EidolonAttributes.CHANTING_SPEED.get());
        double speed = attr != null ? Math.max(0.1d, Math.min(1.0d, attr.getValue())) : 1.0d;
        return Math.max(MIN_SIGN_STEP_TICKS, (int) Math.floor(BASE_SIGN_STEP_TICKS / speed));
    }

    private boolean isFacingTarget(LivingEntity target) {
        Vec3 toTarget = target.getEyePosition().subtract(mob.getEyePosition());
        if (toTarget.lengthSqr() < 1.0E-6d) {
            return true;
        }

        Vec3 look = mob.getViewVector(1.0f);
        return look.normalize().dot(toTarget.normalize()) >= FACING_DOT_THRESHOLD;
    }

    private boolean isOnCooldown(String chantId) {
        CompoundTag cooldowns = mob.getPersistentData().getCompound(ROTATION_COOLDOWNS_TAG);
        if (!cooldowns.contains(chantId, Tag.TAG_LONG)) return false;
        return mob.level().getGameTime() < cooldowns.getLong(chantId);
    }

    private void setCooldown(String chantId, long now, long durationTicks) {
        if (durationTicks <= 0L) return;
        CompoundTag data = mob.getPersistentData();
        CompoundTag cooldowns = data.getCompound(ROTATION_COOLDOWNS_TAG);
        cooldowns.putLong(chantId, now + durationTicks);
        data.put(ROTATION_COOLDOWNS_TAG, cooldowns);
    }

    private boolean hasEnoughMana(double cost) {
        if (cost <= 0.0d) return true;
        ISoul soul = mob.getCapability(ISoul.INSTANCE).orElse(null);
        if (soul != null) return soul.getMagic() >= cost;
        return true;
    }

    private void consumeMana(double cost) {
        if (cost <= 0.0d) return;
        ISoul soul = mob.getCapability(ISoul.INSTANCE).orElse(null);
        if (soul != null) {
            soul.setMagic(Math.max(0.0f, soul.getMagic() - (float) cost));
        }
    }

    private void clearBuildNbt() {
        CompoundTag data = mob.getPersistentData();
        data.remove(CHANT_BUILD_ID_TAG);
        data.remove(CHANT_BUILD_PROGRESS_TAG);
        data.remove(CHANT_BUILD_NEXT_TICK_TAG);
        clearBuildState();
    }

    private void syncBuildState() {
        if (!(mob.level() instanceof ServerLevel) || activeCastId == null) {
            return;
        }

        EidolonUnchainedNetworking.sendToPlayersAround(
            mob.level(),
            mob.blockPosition(),
            64.0d,
            MobChantBuildStatePacket.active(mob.getId(), activeCastId, buildProgress)
        );
    }

    private void clearBuildState() {
        if (!(mob.level() instanceof ServerLevel)) {
            return;
        }

        EidolonUnchainedNetworking.sendToPlayersAround(
            mob.level(),
            mob.blockPosition(),
            64.0d,
            MobChantBuildStatePacket.clear(mob.getId())
        );
    }

    private static boolean hasRotationData(Mob mob) {
        return getCasterTag(mob).contains(ROTATION_IDS_TAG, Tag.TAG_LIST)
            && !getCasterTag(mob).getList(ROTATION_IDS_TAG, Tag.TAG_STRING).isEmpty();
    }

    private static void injectEnthrallGoals(Mob mob) {
        if (!EntityUtil.isEnthralled(mob)) {
            return;
        }

        if (!(mob.getNavigation() instanceof GroundPathNavigation)
            && !(mob.getNavigation() instanceof FlyingPathNavigation)) {
            return;
        }

        boolean hasFollowOwnerGoal = mob.goalSelector.getAvailableGoals().stream()
            .anyMatch(w -> w.getGoal() instanceof FollowOwnerGoal);
        if (!hasFollowOwnerGoal) {
            mob.goalSelector.addGoal(2, new FollowOwnerGoal(mob, 1.5F, 3.0F, 1.2F));
        }

        if (mob instanceof PathfinderMob pathfinderMob) {
            boolean hasThrallTargetGoal = pathfinderMob.targetSelector.getAvailableGoals().stream()
                .anyMatch(w -> w.getGoal() instanceof ThrallTargetGoal);
            if (!hasThrallTargetGoal) {
                pathfinderMob.targetSelector.addGoal(1, new ThrallTargetGoal(pathfinderMob));
            }
        }
    }

    private static void applyChantableMobConfig(Mob mob) {
        ChantableMobManager.ChantableMobConfig config = ChantableMobManager.getConfigForMob(mob);
        if (config == null || config.chantRotationIds().isEmpty()) {
            return;
        }

        CompoundTag data = getCasterTag(mob);

        ListTag configuredRotationIds = new ListTag();
        for (String chantId : config.chantRotationIds()) {
            ResourceLocation parsed = ResourceLocation.tryParse(chantId);
            if (parsed != null) {
                configuredRotationIds.add(net.minecraft.nbt.StringTag.valueOf(parsed.toString()));
            }
        }

        ListTag existingRotationIds = data.getList(ROTATION_IDS_TAG, Tag.TAG_STRING);
        boolean rotationMissing = existingRotationIds.isEmpty();
        boolean rotationChanged = !rotationMissing && !existingRotationIds.equals(configuredRotationIds);
        if (!configuredRotationIds.isEmpty() && (rotationMissing || rotationChanged)) {
            data.put(ROTATION_IDS_TAG, configuredRotationIds);

            int safeSize = configuredRotationIds.size();
            int existingIndex = data.getInt(ROTATION_INDEX_TAG);
            int clampedIndex = safeSize > 0 ? Math.floorMod(existingIndex, safeSize) : 0;
            data.putInt(ROTATION_INDEX_TAG, clampedIndex);

            if (rotationChanged) {
                data.remove(ROTATION_COOLDOWNS_TAG);
            }

            LOGGER.info("[MobChant] Refreshed chant rotation for {} -> {} entries (changed={})",
                mob.getType().toShortString(),
                safeSize,
                rotationChanged);
        }

        data.putInt(ROTATION_INTERVAL_TAG, Math.max(1, config.intervalTicks()));
        data.putDouble(ROTATION_MIN_RANGE_TAG, Math.max(0.0d, config.minRange()));
        data.putDouble(ROTATION_MAX_RANGE_TAG, Math.max(0.5d, config.maxRange()));
        data.putBoolean(ROTATION_REQUIRE_LOS_TAG, true);

        ChantableMobManager.MobCombatPolicy policy = config.combatPolicy() == null
            ? ChantableMobManager.MobCombatPolicy.defaults()
            : config.combatPolicy();

        data.putBoolean(ALLOW_NATIVE_COMBAT_AI_TAG, policy.allowNativeCombatAi());
        data.putInt(MAX_THRESHOLD_CASTS_PER_CYCLE_TAG, Math.max(0, policy.maxThresholdCastsPerCycle()));
        data.putDouble(COMBAT_MOVEMENT_SPEED_TAG, Math.max(0.0d, policy.movementSpeed()));

        ListTag roleOrder = new ListTag();
        for (String role : policy.roleOrder()) {
            if (role != null && !role.isBlank()) {
                roleOrder.add(net.minecraft.nbt.StringTag.valueOf(role.toLowerCase(Locale.ROOT)));
            }
        }
        if (!roleOrder.isEmpty()) {
            data.put(ROTATION_ROLE_ORDER_TAG, roleOrder);
        }

        if (!data.contains(FAITH_DEITY_TAG, Tag.TAG_STRING) && config.assignedDeityId() != null) {
            data.putString(FAITH_DEITY_TAG, config.assignedDeityId().toString());
        }
        if (!data.contains(FAITH_TITLE_TAG, Tag.TAG_STRING)
            && config.startingTitle() != null
            && !config.startingTitle().isEmpty()) {
            data.putString(FAITH_TITLE_TAG, config.startingTitle());
        }
    }

    private static double resolveDesiredMaxMana(double profileMaxMana) {
        return Math.max(0.0d, profileMaxMana);
    }

    private static CompoundTag getCasterTag(Mob mob) {
        return mob.getPersistentData();
    }

    private static void ensureMobManaPool(Mob mob, double resolvedMaxMana) {
        double maxCapacity = resolveSoulManaCapacity(mob, resolvedMaxMana);
        ISoul soul = mob.getCapability(ISoul.INSTANCE).orElse(null);
        if (soul != null) {
            float maxMana = (float) Math.max(0.0d, maxCapacity);
            float oldMax = soul.getMaxMagic();
            boolean uninitialized = !soul.hasMagic() || oldMax <= 0.0f;

            if (uninitialized || Math.abs(oldMax - maxMana) > 0.0001f) {
                soul.setMaxMagic(maxMana);
                soul.setMagic(maxMana);
            } else if (soul.getMagic() > soul.getMaxMagic()) {
                soul.setMagic(soul.getMaxMagic());
            }

            CompoundTag mirrored = getCasterTag(mob);
            mirrored.putDouble(MANA_MAX_TAG, soul.getMaxMagic());
            mirrored.putDouble(MANA_CURRENT_TAG, soul.getMagic());
            return;
        }

        CompoundTag data = getCasterTag(mob);
        double maxMana = Math.max(0.0d, maxCapacity);

        if (!data.contains(MANA_MAX_TAG, Tag.TAG_DOUBLE)) {
            data.putDouble(MANA_MAX_TAG, maxMana);
            data.putDouble(MANA_CURRENT_TAG, maxMana);
            return;
        }

        double storedMax = Math.max(0.0d, data.getDouble(MANA_MAX_TAG));
        double storedCurrent = Math.max(0.0d, data.getDouble(MANA_CURRENT_TAG));

        if (Math.abs(storedMax - maxMana) > 0.0001d) {
            data.putDouble(MANA_MAX_TAG, maxMana);
            data.putDouble(MANA_CURRENT_TAG, maxMana);
            return;
        }

        if (storedCurrent > storedMax) {
            data.putDouble(MANA_CURRENT_TAG, storedMax);
        }
    }

    private static double resolveSoulManaCapacity(Mob mob, double resolvedMaxMana) {
        AttributeInstance soulManaAttr = mob.getAttribute(EidolonUnchainedAttributes.SOUL_MANA.get());
        if (soulManaAttr == null) {
            return Math.max(0.0d, resolvedMaxMana);
        }

        CompoundTag data = getCasterTag(mob);
        if (!data.contains(SOUL_MANA_INIT_TAG, Tag.TAG_BYTE)) {
            double initial = Math.max(0.0d, resolvedMaxMana);
            if (initial > 0.0d) {
                soulManaAttr.setBaseValue(initial);
            }
            data.putBoolean(SOUL_MANA_INIT_TAG, true);
        }

        return Math.max(0.0d, soulManaAttr.getValue());
    }

    private static void applyMobMagicPowerBonus(Mob mob, double bonus) {
        AttributeInstance magicPower = mob.getAttribute(EidolonAttributes.MAGIC_POWER.get());
        if (magicPower == null) {
            return;
        }

        AttributeModifier existing = magicPower.getModifier(MOB_MAGIC_POWER_UUID);
        if (existing != null) {
            magicPower.removeModifier(MOB_MAGIC_POWER_UUID);
        }

        if (Math.abs(bonus) <= 0.0001d) {
            return;
        }

        magicPower.addTransientModifier(new AttributeModifier(
            MOB_MAGIC_POWER_UUID,
            "eu_follower_magic_power",
            bonus,
            AttributeModifier.Operation.ADDITION
        ));
    }

    private static MobResourceProfile resolveMobResourceProfile(Mob mob) {
        MobResourceProfile profile = new MobResourceProfile();
        profile.maxMana = DEFAULT_MOB_MANA;
        profile.magicPowerBonus = 0.0d;

        populateProfileFromAssignedFaith(mob, profile);

        if (!mob.getPersistentData().contains(EntityUtil.THRALL_KEY, Tag.TAG_INT_ARRAY) || mob.getServer() == null) {
            return profile;
        }

        UUID ownerId = mob.getPersistentData().getUUID(EntityUtil.THRALL_KEY);
        ServerPlayer ownerPlayer = mob.getServer().getPlayerList().getPlayer(ownerId);
        if (ownerPlayer == null) {
            return profile;
        }

        profile.ownerPlayer = ownerPlayer;

        IPatronData patronData = ownerPlayer.level()
            .getCapability(CapabilityHandler.PATRON_DATA_CAPABILITY)
            .orElse(null);
        if (patronData == null) {
            return profile;
        }

        ResourceLocation patronId = patronData.getPatron(ownerPlayer);
        if (patronId == null || IPatronData.NO_PATRON.equals(patronId)) {
            return profile;
        }

        profile.patronId = patronId;
        profile.ownerTitle = patronData.getTitle(ownerPlayer);

        AIDeityConfig aiConfig = AIDeityManager.getInstance().getAIConfig(patronId);
        applyPatronResourceProfile(profile, aiConfig, profile.ownerTitle);

        return profile;
    }

    private static void populateProfileFromAssignedFaith(Mob mob, MobResourceProfile profile) {
        CompoundTag data = mob.getPersistentData();
        if (!data.contains(FAITH_DEITY_TAG, Tag.TAG_STRING)) {
            return;
        }

        ResourceLocation patronId = ResourceLocation.tryParse(data.getString(FAITH_DEITY_TAG));
        if (patronId == null) {
            return;
        }

        profile.patronId = patronId;
        profile.ownerTitle = data.contains(FAITH_TITLE_TAG, Tag.TAG_STRING) ? data.getString(FAITH_TITLE_TAG) : "";

        AIDeityConfig aiConfig = AIDeityManager.getInstance().getAIConfig(patronId);
        applyPatronResourceProfile(profile, aiConfig, profile.ownerTitle);
    }

    private static void applyPatronResourceProfile(MobResourceProfile profile, AIDeityConfig aiConfig, String title) {
        if (aiConfig == null || aiConfig.patron_config == null) {
            return;
        }

        profile.patronConfig = aiConfig.patron_config;
        profile.maxMana = Math.max(0.0d, aiConfig.patron_config.defaultFollowerMobMana);
        profile.magicPowerBonus = aiConfig.patron_config.defaultFollowerMobMagicPower;

        if (title == null || title.isEmpty()) {
            return;
        }

        Double stageMana = lookupStageValue(aiConfig.patron_config.followerMobManaByStage, title);
        if (stageMana != null) {
            profile.maxMana = Math.max(0.0d, stageMana);
        }

        Double stageMagic = lookupStageValue(aiConfig.patron_config.followerMobMagicPowerByStage, title);
        if (stageMagic != null) {
            profile.magicPowerBonus = stageMagic;
        }
    }

    private static Double lookupStageValue(Map<String, Double> byStage, String title) {
        if (byStage == null || byStage.isEmpty() || title == null) {
            return null;
        }

        if (byStage.containsKey(title)) {
            return byStage.get(title);
        }

        for (Map.Entry<String, Double> entry : byStage.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(title)) {
                return entry.getValue();
            }
        }

        return null;
    }

    private static void syncFollowerTeamMembership(Mob mob, MobResourceProfile profile) {
        if (profile.patronId == null || profile.patronConfig == null || !profile.patronConfig.assignsPlayersToTeam) {
            return;
        }

        ResourceLocation mobId = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        if (mobId == null) {
            return;
        }

        if (profile.patronConfig.followerMobIds != null
            && !profile.patronConfig.followerMobIds.isEmpty()
            && !profile.patronConfig.followerMobIds.contains(mobId.toString())) {
            return;
        }

        Scoreboard scoreboard = profile.ownerPlayer != null
            ? profile.ownerPlayer.server.getScoreboard()
            : (mob.getServer() != null ? mob.getServer().getScoreboard() : null);
        if (scoreboard == null) {
            return;
        }

        String teamName = "deity_" + profile.patronId.getPath();
        PlayerTeam team = scoreboard.getPlayerTeam(teamName);
        if (team == null) {
            team = scoreboard.addPlayerTeam(teamName);
            if (team == null) {
                return;
            }
        }

        String displayName = profile.patronConfig.teamName == null || profile.patronConfig.teamName.isEmpty()
            ? profile.patronId.getPath()
            : profile.patronConfig.teamName;
        team.setDisplayName(Component.literal(displayName));
        team.setAllowFriendlyFire(profile.patronConfig.friendlyFire);
        team.setSeeFriendlyInvisibles(true);

        if (profile.patronConfig.teamColor != null && !profile.patronConfig.teamColor.isEmpty()) {
            ChatFormatting color = ChatFormatting.getByName(profile.patronConfig.teamColor);
            if (color != null) {
                team.setColor(color);
            }
        }

        String mobEntry = mob.getStringUUID();
        PlayerTeam current = scoreboard.getPlayersTeam(mobEntry);
        if (current != null && current != team) {
            scoreboard.removePlayerFromTeam(mobEntry);
        }

        if (scoreboard.getPlayersTeam(mobEntry) != team) {
            scoreboard.addPlayerToTeam(mobEntry, team);
        }
    }

    private static final class MobResourceProfile {
        private double maxMana;
        private double magicPowerBonus;
        private ServerPlayer ownerPlayer;
        private ResourceLocation patronId;
        private String ownerTitle;
        private AIDeityConfig.PatronConfig patronConfig;
    }

    private static final class NextChant {
        private final int roleIndex; // winning position in role_order list
        private final int index;     // winning position in chant rotation array
        private final ResourceLocation id;
        private final DatapackChant chant;

        private NextChant(int roleIndex, int index, ResourceLocation id, DatapackChant chant) {
            this.roleIndex = roleIndex;
            this.index = index;
            this.id = id;
            this.chant = chant;
        }
    }

    // -----------------------------------------------------------------------
    // Summon lifecycle events
    // -----------------------------------------------------------------------

    /** Cancel all loot drops for summoned entities. */
    @SubscribeEvent
    public static void onSummonDrops(LivingDropsEvent event) {
        if (event.getEntity().getPersistentData().getBoolean(SUMMON_NO_LOOT_TAG)) {
            event.setCanceled(true);
        }
    }

    /** Kill all summons when their owner dies. */
    @SubscribeEvent
    public static void onOwnerDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        UUID ownerUUID = dead.getUUID();
        Set<UUID> summons = OWNER_SUMMONS.remove(ownerUUID);
        if (summons == null || summons.isEmpty()) return;
        if (!(dead.level() instanceof ServerLevel level)) return;

        for (UUID summonUUID : new HashSet<>(summons)) {
            SUMMON_OWNER.remove(summonUUID);
            net.minecraft.world.entity.Entity summon = level.getEntity(summonUUID);
            if (summon instanceof LivingEntity living && living.isAlive()) {
                living.hurt(level.damageSources().genericKill(), Float.MAX_VALUE);
            }
        }
    }

    /** Clean up tracking maps when a summoned entity dies naturally. */
    @SubscribeEvent
    public static void onSummonDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (!dead.getPersistentData().getBoolean(SUMMON_NO_LOOT_TAG)) return;
        UUID summonUUID = dead.getUUID();
        // Fall back to the thrall key if our map doesn't have it (e.g. after a reload).
        UUID ownerUUID = SUMMON_OWNER.remove(summonUUID);
        if (ownerUUID == null && dead.getPersistentData().hasUUID(elucent.eidolon.util.EntityUtil.THRALL_KEY)) {
            ownerUUID = dead.getPersistentData().getUUID(elucent.eidolon.util.EntityUtil.THRALL_KEY);
        }
        if (ownerUUID != null) {
            Set<UUID> ownerSet = OWNER_SUMMONS.get(ownerUUID);
            if (ownerSet != null) ownerSet.remove(summonUUID);
        }
    }
}
