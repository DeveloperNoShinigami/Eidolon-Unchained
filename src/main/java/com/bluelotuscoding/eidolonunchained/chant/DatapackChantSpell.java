package com.bluelotuscoding.eidolonunchained.chant;

import elucent.eidolon.common.spell.PrayerSpell;
import net.minecraft.resources.ResourceLocation;

/**
 * Custom spell implementation for datapack-defined chants.
 * Extends PrayerSpell to integrate with Eidolon's spell system.
 */
public class DatapackChantSpell extends PrayerSpell {
    
    private final DatapackChant chantData;
    
    public DatapackChantSpell(ResourceLocation name, DatapackChant chantData) {
        super(name);
        this.chantData = chantData;
    }
    
    public DatapackChant getChantData() {
        return chantData;
    }
}
}
