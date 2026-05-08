package com.bluelotuscoding.eidolonunchained.events;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.registries.EidolonUnchainedAttributes;
import elucent.eidolon.registries.EidolonAttributes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Ensures Eidolon custom attributes are available on living entities
 * so datapack chants and mob casters can use them consistently.
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class AttributeBridgeEventHandler {
    private AttributeBridgeEventHandler() {
    }

    @SubscribeEvent
    public static void onEntityAttributeModification(EntityAttributeModificationEvent event) {
        Attribute magicPower = EidolonAttributes.MAGIC_POWER.get();
        Attribute chantingSpeed = EidolonAttributes.CHANTING_SPEED.get();
        Attribute soulMana = EidolonUnchainedAttributes.SOUL_MANA.get();
        Attribute divineResistance = EidolonUnchainedAttributes.DIVINE_RESISTANCE.get();

        for (EntityType<? extends LivingEntity> type : event.getTypes()) {
            // Only patch entity types that are real living entities with base health attributes.
            if (!event.has(type, Attributes.MAX_HEALTH)) {
                continue;
            }

            if (!event.has(type, magicPower)) {
                event.add(type, magicPower);
            }
            if (!event.has(type, chantingSpeed)) {
                event.add(type, chantingSpeed);
            }
            if (!event.has(type, soulMana)) {
                event.add(type, soulMana);
            }
            if (!event.has(type, divineResistance)) {
                event.add(type, divineResistance);
            }
        }
    }
}
