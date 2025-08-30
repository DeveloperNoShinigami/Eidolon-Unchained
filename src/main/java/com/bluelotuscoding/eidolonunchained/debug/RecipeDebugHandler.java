package com.bluelotuscoding.eidolonunchained.debug;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.mojang.logging.LogUtils;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * 🚨 EMERGENCY DISABLED: Recipe Debug Handler
 * 
 * This handler was causing game freezing due to async processing.
 * Disabled until we can implement a safer version.
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RecipeDebugHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * 🚨 EMERGENCY DISABLE: Recipe reload listener causing potential freezing
     */
    // @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onReloadListeners_DISABLED(AddReloadListenerEvent event) {
        // DISABLED - This async processing might be causing the game freezing
        LOGGER.info("📚 RECIPE DEBUG: Handler disabled to prevent freezing");
        return;
    }
}
