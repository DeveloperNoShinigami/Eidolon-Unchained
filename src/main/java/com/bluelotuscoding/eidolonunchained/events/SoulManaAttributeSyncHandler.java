package com.bluelotuscoding.eidolonunchained.events;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.registries.EidolonUnchainedAttributes;
import elucent.eidolon.capability.ISoul;
import elucent.eidolon.network.Networking;
import elucent.eidolon.network.SoulUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Keeps Eidolon's soul capability max magic in sync with soul_mana attribute.
 * This makes soul_mana the authoritative max mana value for all entities that have ISoul.
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SoulManaAttributeSyncHandler {
    private SoulManaAttributeSyncHandler() {
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity living = event.getEntity();
        if (living.level().isClientSide) {
            return;
        }

        AttributeInstance soulManaAttr = living.getAttribute(EidolonUnchainedAttributes.SOUL_MANA.get());
        if (soulManaAttr == null) {
            return;
        }

        ISoul soul = living.getCapability(ISoul.INSTANCE).orElse(null);
        if (soul == null) {
            return;
        }

        float maxMana = (float) Math.max(0.0d, soulManaAttr.getValue());
        float oldMax = soul.getMaxMagic();
        float oldCurrent = soul.getMagic();
        boolean changed = false;

        if (!soul.hasMagic() || Math.abs(oldMax - maxMana) > 0.0001f) {
            soul.setMaxMagic(maxMana);
            changed = true;

            if (!soul.hasMagic() || oldMax <= 0.0f) {
                soul.setMagic(maxMana);
                changed = true;
            }
        }

        if (soul.getMagic() > soul.getMaxMagic()) {
            soul.setMagic(soul.getMaxMagic());
            changed = true;
        }

        if (living instanceof ServerPlayer player && changed
            && (Math.abs(oldMax - soul.getMaxMagic()) > 0.0001f || Math.abs(oldCurrent - soul.getMagic()) > 0.0001f)) {
            Networking.sendToTracking(player.level(), player.getOnPos(), new SoulUpdatePacket(player));
        }
    }
}
