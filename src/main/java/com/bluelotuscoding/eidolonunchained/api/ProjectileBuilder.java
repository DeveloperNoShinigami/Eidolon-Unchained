package com.bluelotuscoding.eidolonunchained.api;

import com.bluelotuscoding.eidolonunchained.entity.EUEntities;
import com.bluelotuscoding.eidolonunchained.entity.ChantProjectileEntity;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * {@code EidolonUnchained.projectile(caster)} (D39): inside any cast body. {@code .entity(id)} shoots an existing
 * projectile entity type (Eidolon's soulfire/bonechill through their own {@code shoot}, anything else by spawning it with
 * the caster as owner); without it, EU's scripted {@code chant_projectile} with colour, size, speed, gravity, lifetime,
 * homing and hit callbacks. Aims along the caster's look unless {@code .at(target)} / {@code .towards(x, y, z)} is given.
 */
public final class ProjectileBuilder {
    private final LivingEntity caster;
    private @Nullable Vec3 aim;
    private @Nullable LivingEntity target;
    private @Nullable EntityType<?> entityType;
    private int color = 0xFFFFFF;
    private float size = 0.35f;
    private double speed = 1.2;
    private float inaccuracy = 0f;
    private float gravity = 0f;
    private int lifetime = 60;
    private float homing = 0f;
    private boolean trail = true;
    private boolean pierce = false;
    private @Nullable ChantProjectileEntity.Hit onHit;
    private @Nullable ChantProjectileEntity.Hit onExpire;
    private @Nullable String chantId;

    ProjectileBuilder(LivingEntity caster) {
        if (caster == null) throw new IllegalArgumentException("Eidolon Unchained: projectile(caster) needs a living entity");
        this.caster = caster;
    }

    @Info("Aim at an entity (and home on it when .homing is set)")
    public ProjectileBuilder at(LivingEntity target) {
        this.target = target;
        this.aim = target.getEyePosition().subtract(caster.getEyePosition()).normalize();
        return this;
    }

    @Info("Aim at a point")
    public ProjectileBuilder towards(double x, double y, double z) {
        this.aim = new Vec3(x, y, z).subtract(caster.getEyePosition()).normalize();
        return this;
    }

    @Info("Shoot an existing projectile entity type instead of the scripted one, e.g. 'eidolon:soulfire_projectile' or 'minecraft:small_fireball'")
    public ProjectileBuilder entity(String entityTypeId) {
        var rl = Ids.of(entityTypeId, "entity");
        this.entityType = ForgeRegistries.ENTITY_TYPES.getValue(rl);
        if (entityType == null || !ForgeRegistries.ENTITY_TYPES.containsKey(rl)) throw new IllegalArgumentException("Eidolon Unchained: unknown entity type '" + entityTypeId + "'");
        return this;
    }

    public ProjectileBuilder color(int r, int g, int b) {
        this.color = Ids.rgb(r, g, b) & 0xFFFFFF;
        return this;
    }

    public ProjectileBuilder color(int packedRgb) {
        this.color = packedRgb & 0xFFFFFF;
        return this;
    }

    public ProjectileBuilder size(float size) {
        this.size = size;
        return this;
    }

    @Info("Blocks per tick (default 1.2)")
    public ProjectileBuilder speed(double speed) {
        this.speed = speed;
        return this;
    }

    public ProjectileBuilder inaccuracy(float spread) {
        this.inaccuracy = spread;
        return this;
    }

    @Info("Downward pull per tick (0 = straight)")
    public ProjectileBuilder gravity(float gravity) {
        this.gravity = gravity;
        return this;
    }

    @Info("Ticks before it expires (default 60)")
    public ProjectileBuilder lifetime(int ticks) {
        this.lifetime = ticks;
        return this;
    }

    @Info("0..1 steering strength toward the .at target each tick")
    public ProjectileBuilder homing(float strength) {
        this.homing = strength;
        return this;
    }

    public ProjectileBuilder trail(boolean trail) {
        this.trail = trail;
        return this;
    }

    @Info("Pass through entities it hits instead of vanishing")
    public ProjectileBuilder pierce(boolean pierce) {
        this.pierce = pierce;
        return this;
    }

    @Info("(level, projectile, hitEntity | null, hitPos) => …")
    public ProjectileBuilder onHit(ChantProjectileEntity.Hit fn) {
        this.onHit = fn;
        return this;
    }

    @Info("(level, projectile, null, pos) => … when the lifetime runs out")
    public ProjectileBuilder onExpire(ChantProjectileEntity.Hit fn) {
        this.onExpire = fn;
        return this;
    }

    @Info("Tag the projectile with the chant that fired it (for logs and events)")
    public ProjectileBuilder chant(String chantId) {
        this.chantId = chantId;
        return this;
    }

    @Info("Fire it; returns the spawned entity")
    public Entity shoot() {
        var level = caster.level();
        var dir = aim != null ? aim : caster.getLookAngle();
        if (inaccuracy > 0) {
            var r = caster.getRandom();
            dir = dir.add(r.nextGaussian() * 0.0075 * inaccuracy, r.nextGaussian() * 0.0075 * inaccuracy, r.nextGaussian() * 0.0075 * inaccuracy).normalize();
        }
        var start = caster.getEyePosition().add(dir.scale(0.6));
        var velocity = dir.scale(speed);

        if (entityType != null) {
            var e = entityType.create(level);
            if (e == null) throw new IllegalStateException("Eidolon Unchained: could not create '" + ForgeRegistries.ENTITY_TYPES.getKey(entityType) + "'");
            if (e instanceof elucent.eidolon.common.entity.SpellProjectileEntity esp) {
                esp.shoot(start.x, start.y, start.z, velocity.x, velocity.y, velocity.z, caster, ItemStack.EMPTY);
                if (!esp.isAddedToWorld()) level.addFreshEntity(esp);
                return esp;
            }
            e.setPos(start.x, start.y, start.z);
            if (e instanceof Projectile p) { p.setOwner(caster); p.shoot(dir.x, dir.y, dir.z, (float) speed, 0f); }
            else e.setDeltaMovement(velocity);
            level.addFreshEntity(e);
            return e;
        }

        var p = new ChantProjectileEntity(EUEntities.CHANT_PROJECTILE.get(), level);
        p.setOwner(caster);
        p.setPos(start.x, start.y, start.z);
        p.configure(color, size, lifetime, gravity, homing, trail, pierce, target, onHit, onExpire, chantId);
        p.setDeltaMovement(velocity);
        level.addFreshEntity(p);
        return p;
    }
}
