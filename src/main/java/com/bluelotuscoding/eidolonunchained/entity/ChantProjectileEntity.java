package com.bluelotuscoding.eidolonunchained.entity;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * EU's generic chant projectile (D39): look (colour, size, trail) and hit effect come from the script that shot it.
 * Built on vanilla's {@link ThrowableProjectile}, so flight, block and entity collision, water drag and rotation are
 * vanilla's. The callbacks live only on the server instance that fired it; after a save/load it simply expires.
 */
public class ChantProjectileEntity extends ThrowableProjectile {
    public interface Hit {
        void on(Level level, ChantProjectileEntity projectile, @Nullable LivingEntity hitEntity, Vec3 hitPos);
    }

    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(ChantProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(ChantProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> TRAIL = SynchedEntityData.defineId(ChantProjectileEntity.class, EntityDataSerializers.BOOLEAN);

    private int lifetime = 100;
    private float gravity = 0f;
    private float homing = 0f;
    private @Nullable LivingEntity homingTarget;
    private @Nullable Hit onHit;
    private @Nullable Hit onExpire;
    private @Nullable String chantId;
    private boolean pierce = false;

    public ChantProjectileEntity(EntityType<? extends ThrowableProjectile> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(COLOR, 0xFFFFFF);
        entityData.define(SIZE, 0.35f);
        entityData.define(TRAIL, true);
    }

    // ---- configuration (server, before spawning) ----

    public void configure(int color, float size, int lifetime, float gravity, float homing, boolean trail, boolean pierce,
                          @Nullable LivingEntity homingTarget, @Nullable Hit onHit, @Nullable Hit onExpire, @Nullable String chantId) {
        entityData.set(COLOR, color & 0xFFFFFF);
        entityData.set(SIZE, Math.max(0.05f, size));
        entityData.set(TRAIL, trail);
        this.lifetime = Math.max(1, lifetime);
        this.gravity = gravity;
        this.homing = Math.max(0f, homing);
        this.homingTarget = homingTarget;
        this.onHit = onHit;
        this.onExpire = onExpire;
        this.chantId = chantId;
        this.pierce = pierce;
    }

    public int getColor() {
        return entityData.get(COLOR);
    }

    public float getSize() {
        return entityData.get(SIZE);
    }

    public @Nullable String getChantId() {
        return chantId;
    }

    // ---- flight (vanilla moves it; we add lifetime, homing and the trail) ----

    @Override
    protected float getGravity() {
        return gravity;
    }

    @Override
    public void tick() {
        if (!level().isClientSide) {
            if (tickCount > lifetime) { expire(); return; }
            if (homing > 0 && homingTarget != null && homingTarget.isAlive()) {
                var want = homingTarget.getEyePosition().subtract(position()).normalize().scale(getDeltaMovement().length());
                setDeltaMovement(getDeltaMovement().lerp(want, homing));
            }
        }
        super.tick();
        if (level().isClientSide && entityData.get(TRAIL)) {
            int c = getColor();
            level().addParticle(new DustParticleOptions(new Vector3f(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f), getSize() * 2f),
                    getX() + (random.nextDouble() - 0.5) * getSize(), getY() + (random.nextDouble() - 0.5) * getSize(), getZ() + (random.nextDouble() - 0.5) * getSize(), 0, 0, 0);
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && target instanceof LivingEntity;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (level().isClientSide) return;
        fire(onHit, result.getEntity() instanceof LivingEntity le ? le : null, result.getLocation());
        if (!pierce) discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (level().isClientSide) return;
        fire(onHit, null, result.getLocation());
        discard();
    }

    private void expire() {
        fire(onExpire, null, position());
        discard();
    }

    private void fire(@Nullable Hit hook, @Nullable LivingEntity entity, Vec3 pos) {
        if (level() instanceof ServerLevel sl) {
            int c = getColor();
            sl.sendParticles(new DustParticleOptions(new Vector3f(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f), getSize() * 3f),
                    pos.x, pos.y, pos.z, 8, 0.15, 0.15, 0.15, 0.0);
        }
        if (hook == null) return;
        try {
            hook.on(level(), this, entity, pos);
        } catch (RuntimeException e) {
            EidolonUnchained.LOGGER.error("chant projectile ({}) hit callback threw: {}", chantId, e.toString());
        }
    }

    // ---- persistence: transient by design ----

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        lifetime = 0;      // no callbacks survive a reload; expire on the next tick
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
