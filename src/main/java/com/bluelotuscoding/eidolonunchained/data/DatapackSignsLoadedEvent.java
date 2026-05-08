package com.bluelotuscoding.eidolonunchained.data;

import net.minecraftforge.eventbus.api.Event;
import net.minecraft.resources.ResourceLocation;
import elucent.eidolon.api.spells.Sign;

import java.util.Map;

/**
 * Event fired when datapack signs are loaded/reloaded.
 * Consumed by CodexSignIntegration (client-side) to rebuild sign index and categories.
 */
public class DatapackSignsLoadedEvent extends Event {
    private final Map<ResourceLocation, Sign> signs;

    public DatapackSignsLoadedEvent(Map<ResourceLocation, Sign> signs) {
        this.signs = signs;
    }

    public Map<ResourceLocation, Sign> getSigns() {
        return signs;
    }
}
