package com.bluelotuscoding.eidolonunchained.chant;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Handles timed typed divine resistance tag modifiers (eu_divine_resistances.<key>).
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TimedTypedResistanceManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int PARTICLE_INTERVAL_TICKS = 5;
    private static final String TAG_DIVINE_RESISTANCES = "eu_divine_resistances";

    private static final Map<ModifierKey, TimedTypedEntry> ACTIVE = new HashMap<>();

    private TimedTypedResistanceManager() {
    }

    public static void applyPermanent(LivingEntity target, String resistanceKey, UUID modifierId, double amount) {
        if (resistanceKey == null || resistanceKey.isBlank() || amount == 0.0d) {
            return;
        }

        cancel(target, modifierId);
        addResistanceDelta(target, resistanceKey, amount);
    }

    public static void registerOrRefresh(
            LivingEntity target,
            String resistanceKey,
            UUID modifierId,
            double amount,
            int durationTicks,
            TimedAttributeModifierManager.ParticleSpec particleSpec) {
        if (durationTicks <= 0 || amount == 0.0d || resistanceKey == null || resistanceKey.isBlank()) {
            return;
        }
        if (!(target.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        ModifierKey key = new ModifierKey(target.getUUID(), serverLevel.dimension(), modifierId);
        TimedTypedEntry previous = ACTIVE.remove(key);
        if (previous != null) {
            addResistanceDelta(target, previous.resistanceKey(), -previous.amount());
        }

        addResistanceDelta(target, resistanceKey, amount);

        long expiresAtTick = serverLevel.getServer().getTickCount() + (long) durationTicks;
        ACTIVE.put(key, new TimedTypedEntry(resistanceKey, amount, expiresAtTick, particleSpec));
    }

    public static void cancel(LivingEntity target, UUID modifierId) {
        if (!(target.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        ModifierKey key = new ModifierKey(target.getUUID(), serverLevel.dimension(), modifierId);
        TimedTypedEntry removed = ACTIVE.remove(key);
        if (removed != null) {
            addResistanceDelta(target, removed.resistanceKey(), -removed.amount());
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ACTIVE.isEmpty()) {
            return;
        }

        long nowTick = event.getServer().getTickCount();
        Iterator<Map.Entry<ModifierKey, TimedTypedEntry>> iterator = ACTIVE.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<ModifierKey, TimedTypedEntry> entry = iterator.next();
            ModifierKey key = entry.getKey();
            TimedTypedEntry timed = entry.getValue();

            ServerLevel level = event.getServer().getLevel(key.dimension());
            if (level == null) {
                iterator.remove();
                continue;
            }

            Entity entity = level.getEntity(key.entityId());
            if (!(entity instanceof LivingEntity target)) {
                iterator.remove();
                continue;
            }

            if (nowTick < timed.expiresAtTick()) {
                if (timed.particleSpec() != null && nowTick % PARTICLE_INTERVAL_TICKS == 0) {
                    emitParticles(level, target, timed.particleSpec());
                }
                continue;
            }

            addResistanceDelta(target, timed.resistanceKey(), -timed.amount());
            LOGGER.info(
                "typed divine resistance removed: target='{}' key='{}' amount={} modifierId={} currentTick={} scheduledExpiry={}",
                target.getName().getString(),
                timed.resistanceKey(),
                timed.amount(),
                key.modifierId(),
                nowTick,
                timed.expiresAtTick()
            );
            iterator.remove();
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ACTIVE.clear();
    }

    private static void addResistanceDelta(LivingEntity target, String resistanceKey, double delta) {
        CompoundTag persisted = target.getPersistentData();
        CompoundTag typed = persisted.contains(TAG_DIVINE_RESISTANCES, Tag.TAG_COMPOUND)
            ? persisted.getCompound(TAG_DIVINE_RESISTANCES)
            : new CompoundTag();

        double before = typed.contains(resistanceKey, Tag.TAG_ANY_NUMERIC) ? typed.getDouble(resistanceKey) : 0.0d;
        double updated = before + delta;

        if (Math.abs(updated) < 1.0e-9d) {
            typed.remove(resistanceKey);
        } else {
            typed.putDouble(resistanceKey, updated);
        }

        if (typed.isEmpty()) {
            persisted.remove(TAG_DIVINE_RESISTANCES);
        } else {
            persisted.put(TAG_DIVINE_RESISTANCES, typed);
        }
    }

    private static void emitParticles(
            ServerLevel level,
            LivingEntity target,
            TimedAttributeModifierManager.ParticleSpec spec) {
        var particleType = net.minecraftforge.registries.ForgeRegistries.PARTICLE_TYPES.getValue(spec.particleId());
        if (particleType instanceof net.minecraft.core.particles.SimpleParticleType simpleParticle) {
            level.sendParticles(
                simpleParticle,
                target.getX(),
                target.getY() + 1.0,
                target.getZ(),
                Math.max(1, spec.count()),
                spec.spread(),
                spec.spread(),
                spec.spread(),
                spec.speed()
            );
        }
    }

    private record ModifierKey(UUID entityId, ResourceKey<Level> dimension, UUID modifierId) {
    }

    private record TimedTypedEntry(
            String resistanceKey,
            double amount,
            long expiresAtTick,
            TimedAttributeModifierManager.ParticleSpec particleSpec) {
    }
}
