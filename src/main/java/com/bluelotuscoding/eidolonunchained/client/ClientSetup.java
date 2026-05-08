package com.bluelotuscoding.eidolonunchained.client;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.client.gui.ChantOverlay;
import com.bluelotuscoding.eidolonunchained.client.gui.PotionSignStatusOverlay;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Client-side setup for Eidolon Unchained
 * Handles registration of GUI overlays and other client-only components
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("chant_overlay", new ChantOverlay());
        event.registerAboveAll("potion_sign_status_overlay", new PotionSignStatusOverlay());
        LOGGER.info("Registered chant overlay for independent chant interface");
    }
}
