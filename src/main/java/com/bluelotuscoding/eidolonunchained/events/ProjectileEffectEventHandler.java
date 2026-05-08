package com.bluelotuscoding.eidolonunchained.events;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.chant.DatapackChant;
import com.bluelotuscoding.eidolonunchained.network.DeityDamageNumberPacket;
import com.bluelotuscoding.eidolonunchained.network.EidolonUnchainedNetworking;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Emits configurable particles for chant-spawned projectiles:
 * - while in-flight (trail particles)
 * - on impact (impact burst/ring)
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ProjectileEffectEventHandler {
    private static final Map<ResourceKey<Level>, Set<UUID>> TRAIL_PROJECTILES = new HashMap<>();

    private ProjectileEffectEventHandler() {
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof Projectile projectile)) {
            return;
        }

        if (!projectile.getPersistentData().getBoolean("eu_trail_particles_enabled")) {
            return;
        }

        TRAIL_PROJECTILES
            .computeIfAbsent(level.dimension(), ignored -> new HashSet<>())
            .add(projectile.getUUID());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || TRAIL_PROJECTILES.isEmpty()) {
            return;
        }

        long nowTick = event.getServer().getTickCount();
        Iterator<Map.Entry<ResourceKey<Level>, Set<UUID>>> mapIterator = TRAIL_PROJECTILES.entrySet().iterator();

        while (mapIterator.hasNext()) {
            Map.Entry<ResourceKey<Level>, Set<UUID>> dimensionEntry = mapIterator.next();
            ServerLevel level = event.getServer().getLevel(dimensionEntry.getKey());
            if (level == null) {
                mapIterator.remove();
                continue;
            }

            Set<UUID> tracked = dimensionEntry.getValue();
            Iterator<UUID> projectileIterator = tracked.iterator();
            while (projectileIterator.hasNext()) {
                UUID projectileId = projectileIterator.next();
                Entity entity = level.getEntity(projectileId);
                if (!(entity instanceof Projectile projectile) || !projectile.isAlive()) {
                    projectileIterator.remove();
                    continue;
                }

                var tag = projectile.getPersistentData();
                if (!tag.getBoolean("eu_trail_particles_enabled")) {
                    projectileIterator.remove();
                    continue;
                }

                int interval = Math.max(1, tag.getInt("eu_trail_particle_interval"));
                if (nowTick % interval != 0) {
                    continue;
                }

                ResourceLocation particleId = ResourceLocation.tryParse(tag.getString("eu_trail_particle"));
                if (particleId == null) {
                    continue;
                }

                ParticleType<?> particleType = ForgeRegistries.PARTICLE_TYPES.getValue(particleId);
                if (!(particleType instanceof SimpleParticleType simpleParticle)) {
                    continue;
                }

                int count = Math.max(1, tag.getInt("eu_trail_particle_count"));
                double spread = tag.getDouble("eu_trail_particle_spread");
                double speed = tag.getDouble("eu_trail_particle_speed");

                level.sendParticles(
                    simpleParticle,
                    projectile.getX(),
                    projectile.getY(),
                    projectile.getZ(),
                    count,
                    spread,
                    spread,
                    spread,
                    speed
                );
            }

            if (tracked.isEmpty()) {
                mapIterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile().level() instanceof ServerLevel level)) {
            return;
        }

        // Cleanup trail tracking for this projectile (if tracked).
        Set<UUID> tracked = TRAIL_PROJECTILES.get(level.dimension());
        if (tracked != null) {
            tracked.remove(event.getProjectile().getUUID());
            if (tracked.isEmpty()) {
                TRAIL_PROJECTILES.remove(level.dimension());
            }
        }

        var projectile = event.getProjectile();
        var tag = projectile.getPersistentData();

        applyConfiguredImpactDamage(event, projectile, tag);
        applyConfiguredOnHitEffects(event, tag);

        if (!tag.getBoolean("eu_impact_particles_enabled")) {
            return;
        }

        ResourceLocation particleId = ResourceLocation.tryParse(tag.getString("eu_impact_particle"));
        if (particleId == null) {
            return;
        }

        ParticleType<?> particleType = ForgeRegistries.PARTICLE_TYPES.getValue(particleId);
        if (!(particleType instanceof SimpleParticleType simpleParticle)) {
            return;
        }

        int count = Math.max(1, tag.getInt("eu_impact_particle_count"));
        double spread = tag.getDouble("eu_impact_particle_spread");
        double speed = tag.getDouble("eu_impact_particle_speed");
        String mode = tag.getString("eu_impact_particle_mode");

        HitResult hit = event.getRayTraceResult();
        Vec3 hitPos = hit != null ? hit.getLocation() : event.getProjectile().position();

        if ("ring".equalsIgnoreCase(mode)) {
            emitRing(level, simpleParticle, hitPos, count, spread, speed);
        } else {
            level.sendParticles(
                simpleParticle,
                hitPos.x,
                hitPos.y,
                hitPos.z,
                count,
                spread,
                spread,
                spread,
                speed
            );
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        TRAIL_PROJECTILES.clear();
    }

    private static void applyConfiguredImpactDamage(ProjectileImpactEvent event, Projectile projectile, net.minecraft.nbt.CompoundTag tag) {
        if (!tag.getBoolean("eu_impact_damage_enabled")) {
            return;
        }

        double configuredDamage = tag.getDouble("eu_impact_damage");
        if (configuredDamage <= 0.0d) {
            return;
        }

        HitResult hit = event.getRayTraceResult();
        if (!(hit instanceof EntityHitResult entityHit) || !(entityHit.getEntity() instanceof LivingEntity target)) {
            return;
        }

        Entity owner = projectile.getOwner();
        String damageType = tag.getString("eu_impact_damage_type");
        float adjustedDamage = DivineResistanceResolver.applyDivineResistance(target, (float) configuredDamage, damageType);
        DamageSource source = resolveConfiguredDamageSource(target, projectile, owner, damageType);
        boolean applied = target.hurt(source, adjustedDamage);
        if (applied
            && tag.contains("eu_impact_hit_color", net.minecraft.nbt.Tag.TAG_INT)
            && shouldEmitCustomDamageNumber(target)) {
            if (!(target.level() instanceof ServerLevel serverLevel)) {
                return;
            }
            EidolonUnchainedNetworking.sendToPlayersAround(
                serverLevel,
                target.blockPosition(),
                48.0d,
                new DeityDamageNumberPacket(target.getId(), adjustedDamage, tag.getInt("eu_impact_hit_color"))
            );
        }
    }

    private static boolean shouldEmitCustomDamageNumber(LivingEntity target) {
        ResourceLocation typeId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        return typeId == null || !"dummmmmmy".equals(typeId.getNamespace());
    }

    private static DamageSource resolveConfiguredDamageSource(
        LivingEntity target,
        Projectile projectile,
        Entity owner,
        String configuredType
    ) {
        if (configuredType == null || configuredType.isBlank()) {
            return target.damageSources().indirectMagic(projectile, owner);
        }

        if ("magic".equalsIgnoreCase(configuredType)
            || "indirect_magic".equalsIgnoreCase(configuredType)
            || "indirectmagic".equalsIgnoreCase(configuredType)) {
            return target.damageSources().indirectMagic(projectile, owner);
        }

        ResourceLocation damageTypeId = ResourceLocation.tryParse(configuredType);
        if (damageTypeId == null) {
            return target.damageSources().indirectMagic(projectile, owner);
        }

        ResourceKey<DamageType> key = ResourceKey.create(Registries.DAMAGE_TYPE, damageTypeId);
        Holder<DamageType> holder = target.level().registryAccess()
            .registryOrThrow(Registries.DAMAGE_TYPE)
            .getHolder(key)
            .orElse(null);
        if (holder == null) {
            return target.damageSources().indirectMagic(projectile, owner);
        }

        return new DamageSource(holder, projectile, owner);
    }

    private static void applyConfiguredOnHitEffects(ProjectileImpactEvent event, net.minecraft.nbt.CompoundTag tag) {
        if (!tag.contains("eu_projectile_on_hit_json", net.minecraft.nbt.Tag.TAG_STRING)) {
            return;
        }

        HitResult hit = event.getRayTraceResult();
        if (!(hit instanceof EntityHitResult entityHit) || !(entityHit.getEntity() instanceof LivingEntity target)) {
            return;
        }

        try {
            JsonElement parsed = JsonParser.parseString(tag.getString("eu_projectile_on_hit_json"));
            if (!(parsed instanceof JsonArray effectsArray)) {
                return;
            }

            for (JsonElement element : effectsArray) {
                if (element == null || !element.isJsonObject()) {
                    continue;
                }

                JsonObject effectData = element.getAsJsonObject();
                if (!effectData.has("type")) {
                    continue;
                }

                String effectType = effectData.get("type").getAsString();
                DatapackChant.ChantEffect effect = new DatapackChant.ChantEffect(effectType, effectData);
                effect.apply(target);
            }
        } catch (Exception ignored) {
            // Keep projectile impacts safe even when user-supplied JSON is malformed.
        }
    }

    private static void emitRing(ServerLevel level, SimpleParticleType particle, Vec3 center, int count, double radius, double speed) {
        int samples = Math.max(8, Math.min(64, count));
        for (int i = 0; i < samples; i++) {
            double angle = (Math.PI * 2.0d * i) / samples;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            level.sendParticles(particle, x, center.y, z, 1, 0.0d, 0.03d, 0.0d, speed);
        }
    }
}
