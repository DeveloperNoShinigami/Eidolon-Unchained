package com.bluelotuscoding.eidolonunchained.events;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import elucent.eidolon.capability.ISoul;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Bridges Eidolon's soul capability to non-player mobs so they can use
 * the same mana system as players.
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MobSoulCapabilityEventHandler {
    private static final ResourceLocation MOB_SOUL_CAP_ID = ResourceLocation.tryParse(EidolonUnchained.MODID + ":mob_soul");

    private MobSoulCapabilityEventHandler() {
    }

    @SubscribeEvent
    public static void onAttachEntityCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (MOB_SOUL_CAP_ID == null) {
            return;
        }
        if (event.getObject() instanceof Mob) {
            event.addCapability(MOB_SOUL_CAP_ID, new ISoul.Provider());
        }
    }
}
