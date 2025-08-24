package com.bluelotuscoding.eidolonunchained.data;

import net.minecraftforge.eventbus.api.Event;
import java.util.Map;
import com.bluelotuscoding.eidolonunchained.deity.DatapackDeity;
import net.minecraft.resources.ResourceLocation;

/**
 * Event fired when datapack deities are loaded/reloaded
 */
public class DatapackDeitiesLoadedEvent extends Event {
    private final Map<ResourceLocation, DatapackDeity> deities;
    
    public DatapackDeitiesLoadedEvent(Map<ResourceLocation, DatapackDeity> deities) {
        this.deities = deities;
    }
    
    public Map<ResourceLocation, DatapackDeity> getDeities() {
        return deities;
    }
}
