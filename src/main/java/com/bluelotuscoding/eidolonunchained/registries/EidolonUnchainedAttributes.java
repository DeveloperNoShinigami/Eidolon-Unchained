package com.bluelotuscoding.eidolonunchained.registries;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Custom attributes owned by Eidolon Unchained.
 */
public final class EidolonUnchainedAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES =
        DeferredRegister.create(ForgeRegistries.ATTRIBUTES, EidolonUnchained.MODID);

    /**
     * Soul mana capacity used by autonomous mob chanting.
     */
    public static final RegistryObject<Attribute> SOUL_MANA = ATTRIBUTES.register(
        "soul_mana",
        () -> new RangedAttribute("attribute.eidolonunchained.soul_mana", 100.0d, 0.0d, 100000.0d).setSyncable(true)
    );

    /**
     * Global scalar multiplier applied to all deity-scoped divine damage.
     *
     * 1.0 = no change, values above 1.0 increase resistance, values below 1.0 increase damage taken.
     */
    public static final RegistryObject<Attribute> DIVINE_RESISTANCE = ATTRIBUTES.register(
        "divine_resistance",
        () -> new RangedAttribute("attribute.eidolonunchained.divine_resistance", 1.0d, -10.0d, 10.0d).setSyncable(true)
    );

    private EidolonUnchainedAttributes() {
    }
}
