package com.bluelotuscoding.eidolonunchained.entity;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** EU entity types. Phase 3: the generic scripted chant projectile (D39). */
public final class EUEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, EidolonUnchained.MOD_ID);

    public static final RegistryObject<EntityType<ChantProjectileEntity>> CHANT_PROJECTILE = ENTITIES.register("chant_projectile",
            () -> EntityType.Builder.<ChantProjectileEntity>of(ChantProjectileEntity::new, MobCategory.MISC)
                    .sized(0.35f, 0.35f).clientTrackingRange(6).updateInterval(2).build("chant_projectile"));

    private EUEntities() {
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
    }
}
