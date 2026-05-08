package com.bluelotuscoding.eidolonunchained.chant;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Centralized lifecycle manager for timed attribute modifiers.
 *
 * This avoids per-cast scheduled task races by storing only the latest expiry
 * for each target+modifier pair and removing expired modifiers from one server tick loop.
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TimedAttributeModifierManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int PARTICLE_INTERVAL_TICKS = 5;

    private static final Map<ModifierKey, TimedModifierEntry> ACTIVE = new HashMap<>();

    private TimedAttributeModifierManager() {
    }

    public static void registerOrRefresh(
            LivingEntity target,
            ResourceLocation attributeId,
            UUID modifierId,
            int durationTicks,
            ParticleSpec particleSpec) {
        if (durationTicks <= 0 || !(target.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        long expiresAtTick = serverLevel.getServer().getTickCount() + (long) durationTicks;
        ModifierKey key = new ModifierKey(target.getUUID(), serverLevel.dimension(), modifierId);
        ACTIVE.put(key, new TimedModifierEntry(attributeId, expiresAtTick, particleSpec));
    }

    public static void cancel(LivingEntity target, UUID modifierId) {
        if (!(target.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        ModifierKey key = new ModifierKey(target.getUUID(), serverLevel.dimension(), modifierId);
        ACTIVE.remove(key);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ACTIVE.isEmpty()) {
            return;
        }

        long nowTick = event.getServer().getTickCount();
        Iterator<Map.Entry<ModifierKey, TimedModifierEntry>> iterator = ACTIVE.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<ModifierKey, TimedModifierEntry> entry = iterator.next();
            ModifierKey key = entry.getKey();
            TimedModifierEntry timed = entry.getValue();

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

            Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(timed.attributeId());
            if (attribute == null) {
                iterator.remove();
                continue;
            }

            AttributeInstance instance = target.getAttribute(attribute);
            if (instance == null) {
                iterator.remove();
                continue;
            }

            AttributeModifier existing = instance.getModifier(key.modifierId());
            if (existing != null) {
                double beforeRemoval = instance.getValue();
                instance.removeModifier(existing);

                LOGGER.info(
                    "modify_attribute removed: target='{}' attribute='{}' before={} after={} modifierId={} currentTick={} scheduledExpiry={}",
                    target.getName().getString(),
                    timed.attributeId(),
                    beforeRemoval,
                    instance.getValue(),
                    key.modifierId(),
                    nowTick,
                    timed.expiresAtTick()
                );

                if (target instanceof ServerPlayer sp) {
                    sp.connection.send(new net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket(
                        sp.getId(), java.util.List.of(instance)));
                }
            }

            iterator.remove();
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ACTIVE.clear();
    }

    private record ModifierKey(UUID entityId, ResourceKey<Level> dimension, UUID modifierId) {
    }

    private record TimedModifierEntry(ResourceLocation attributeId, long expiresAtTick, ParticleSpec particleSpec) {
    }

    public record ParticleSpec(ResourceLocation particleId, int count, double spread, double speed) {
    }

    private static void emitParticles(ServerLevel level, LivingEntity target, ParticleSpec spec) {
        var particleType = ForgeRegistries.PARTICLE_TYPES.getValue(spec.particleId());
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
}
