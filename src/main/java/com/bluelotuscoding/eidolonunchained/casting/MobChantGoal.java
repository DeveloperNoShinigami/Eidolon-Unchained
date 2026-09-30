package com.bluelotuscoding.eidolonunchained.casting;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.api.ChantSync;
import com.bluelotuscoding.eidolonunchained.api.ScriptedSpell;
import elucent.eidolon.api.spells.Sign;
import elucent.eidolon.capability.ISoul;
import com.bluelotuscoding.eidolonunchained.network.EUNetwork;
import com.bluelotuscoding.eidolonunchained.network.MobChantStatePacket;
import elucent.eidolon.registries.EidolonSounds;
import elucent.eidolon.registries.Spells;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Mob chant AI (rule C3 / spec §3.8.6). With a valid target in range (and sight, if required), picks the next rotation
 * spell whose cooldown is ready, whose mana the mob can pay and whose {@code mobCanCast} agrees, builds its sign
 * sequence sign by sign (Eidolon's sign particles and chant sound over the mob), then runs the spell's mob path and
 * pays the mana. Interrupted by a big hit (config) or by losing the target. Script hooks are static so the KubeJS
 * layer can attach without this class knowing it.
 */
public final class MobChantGoal extends Goal {
    public interface Hook {
        /** return false to cancel (only honoured for CAST) */
        boolean on(Mob mob, ScriptedSpell spell, @Nullable LivingEntity target, @Nullable Sign sign, List<Sign> signs, String reason);
    }

    public static Hook onStarted = (m, s, t, sg, ss, r) -> true;
    public static Hook onSign = (m, s, t, sg, ss, r) -> true;
    public static Hook onCast = (m, s, t, sg, ss, r) -> true;
    public static Hook onInterrupted = (m, s, t, sg, ss, r) -> true;

    private final Mob mob;
    private final Map<ResourceLocation, Long> readyAt = new HashMap<>();
    private long nextCastAt = 0;
    private int rotationIndex = 0;

    private @Nullable ScriptedSpell current;
    private @Nullable LivingEntity target;
    private final List<Sign> built = new ArrayList<>();
    private List<Sign> sequence = List.of();
    private int timer;
    private int windup = -1;                    // ticks left before the built chant releases (-1 = still building)
    private boolean interrupted;

    public MobChantGoal(Mob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.LOOK));
    }

    private @Nullable CasterProfile profile() {
        return CasterProfileResolver.cached(mob);
    }

    @Override
    public boolean canUse() {
        var p = profile();
        if (p == null || mob.level().isClientSide) return false;
        if (mob.level().getGameTime() < nextCastAt) return false;
        var t = pickTarget(p);
        if (t == null || !inRange(p, t)) return false;
        var spell = pickSpell(p, t);
        if (spell == null) return false;
        current = spell;
        target = t;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return !interrupted && current != null && target != null && target.isAlive() && mob.isAlive()
                && profile() != null && inRange(profile(), target);
    }

    @Override
    public void start() {
        var p = profile();
        if (p != null && p.suppressMeleeWhileCasting) setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE)); else setFlags(EnumSet.of(Flag.LOOK));
        built.clear();
        sequence = current == null ? List.of() : ChantSync.sequenceOf(current.getRegistryName());
        timer = 0;
        windup = -1;
        interrupted = false;
        if (current != null) onStarted.on(mob, current, target, null, sequence, "");
    }

    @Override
    public void tick() {
        var p = profile();
        if (p == null || current == null || target == null) return;
        mob.getLookControl().setLookAt(target, 30f, 30f);
        if (built.size() < sequence.size()) {
            if (timer-- > 0) return;
            var sign = sequence.get(built.size());
            built.add(sign);
            timer = p.signDelay;
            syncRing();
            if (mob.level() instanceof ServerLevel sl) {
                sl.playSound(null, mob.blockPosition(), EidolonSounds.CHANT_WORD.get(), SoundSource.HOSTILE, 0.7f, mob.getRandom().nextFloat() * 0.375f + 0.625f);
            }
            onSign.on(mob, current, target, sign, List.copyOf(built), "");
            return;
        }
        // sequence complete: wind up for the spell's delay with the signs circling the mob (D38, same as players), then cast from the mob
        if (windup < 0) windup = Math.max(0, current.getDelay());
        if (windup > 0) {
            windup--;                                   // the ring renderer shows the wind-up (D41)
            return;
        }
        var soul = mob.getCapability(ISoul.INSTANCE).resolve().orElse(null);
        int cost = current.getCost();
        if (soul != null && soul.getMagic() < cost) { finish("no_mana"); return; }
        if (!current.canMobCast(mob.level(), mob.blockPosition(), mob, target)) { finish("refused"); return; }
        if (!onCast.on(mob, current, target, null, List.copyOf(built), "")) { finish("cancelled"); return; }
        current.castByMob(mob.level(), mob.blockPosition(), mob, target);
        if (soul != null) soul.takeMagic(cost);
        long now = mob.level().getGameTime();
        readyAt.put(current.getRegistryName(), now + p.cooldowns.getOrDefault(current.getRegistryName(), 0));
        nextCastAt = now + p.castInterval;
        rotationIndex = (rotationIndex + 1) % Math.max(1, p.spells.size());
        finish("cast");
    }

    @Override
    public void stop() {
        if (current != null && !built.isEmpty() && (built.size() < sequence.size() || windup > 0)) {
            onInterrupted.on(mob, current, target, null, List.copyOf(built), interrupted ? "hit" : "target_lost");
        }
        current = null;
        target = null;
        built.clear();
        syncRing();
        setFlags(EnumSet.of(Flag.LOOK));
    }

    private void syncRing() {
        EUNetwork.sendToTracking(mob, new MobChantStatePacket(mob.getId(), built.stream().map(s -> s.getRegistryName().toString()).toList()));
    }

    /** Called by the hurt handler when the mob takes a big hit. */
    public void interrupt() {
        if (current != null && (built.size() < sequence.size() || windup > 0)) interrupted = true;
    }

    private void finish(String reason) {
        if (!"cast".equals(reason)) onInterrupted.on(mob, current, target, null, List.copyOf(built), reason);
        current = null;
    }

    // ---- selection ----

    private @Nullable LivingEntity pickTarget(CasterProfile p) {
        return switch (p.targetPolicy) {
            case ATTACK_TARGET -> mob.getTarget();
            case NEAREST_PLAYER -> {
                Predicate<LivingEntity> ok = e -> !(e instanceof Player pl && (pl.isCreative() || pl.isSpectator()));
                var pl = mob.level().getNearestPlayer(mob, p.maxRange);
                yield pl != null && ok.test(pl) ? pl : null;
            }
            case ALLIES -> mob.getTarget();      // thrall support arrives with the enthrall integration
        };
    }

    private boolean inRange(CasterProfile p, LivingEntity t) {
        double d = mob.distanceTo(t);
        if (d < p.minRange || d > p.maxRange) return false;
        return !p.requireLineOfSight || mob.hasLineOfSight(t);
    }

    private @Nullable ScriptedSpell pickSpell(CasterProfile p, LivingEntity t) {
        if (p.spells.isEmpty()) return null;
        long now = mob.level().getGameTime();
        var soul = mob.getCapability(ISoul.INSTANCE).resolve().orElse(null);
        for (int i = 0; i < p.spells.size(); i++) {
            var id = p.spells.get((rotationIndex + i) % p.spells.size());
            if (!(Spells.find(id) instanceof ScriptedSpell ss) || !ss.hasMobPath()) continue;
            if (readyAt.getOrDefault(id, 0L) > now) continue;
            if (ChantSync.sequenceOf(id).isEmpty()) continue;                          // no chant recipe: not chantable
            if (soul != null && soul.getMagic() < ss.getCost()) continue;
            if (!ss.canMobCast(mob.level(), mob.blockPosition(), mob, t)) continue;
            return ss;
        }
        return null;
    }
}
