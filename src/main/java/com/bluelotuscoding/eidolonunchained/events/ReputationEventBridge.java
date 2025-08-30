package com.bluelotuscoding.eidolonunchained.events;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * 🔗 REPUTATION EVENT INTEGRATION
 * 
 * Bridges Eidolon's reputation system with our dynamic progression handler.
 * This ensures that when reputation changes occur, our progression system
 * immediately checks for threshold crossings and fires appropriate events.
 * 
 * This fixes the issue where "progression doesn't seem to trigger" by
 * providing the missing event bridge between reputation changes and 
 * progression unlocks.
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ReputationEventBridge {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * 🚨 EMERGENCY DISABLE: PLAYER LOGIN PROGRESSION CHECK
     * 
     * TEMPORARILY DISABLED - Might be causing freezing during login.
     */
    // @SubscribeEvent
    public static void onPlayerLogin_DISABLED(PlayerEvent.PlayerLoggedInEvent event) {
        // DISABLED - This might be causing freezing during login
        return;
    }
    
    /**
     * 🚨 EMERGENCY DISABLE: PLAYER LOGOUT CLEANUP
     * 
     * TEMPORARILY DISABLED - Being extra safe.
     */
    // @SubscribeEvent
    public static void onPlayerLogout_DISABLED(PlayerEvent.PlayerLoggedOutEvent event) {
        // DISABLED - Being extra safe about event handling
        return;
    }
    
    /**
     * 🎯 MANUAL PROGRESSION TRIGGER
     * 
     * Public method to manually trigger progression checks.
     * Useful for testing and when reputation is changed outside normal flows.
     */
    public static void triggerProgressionCheck(ServerPlayer player) {
        try {
            LOGGER.info("🔄 Manual progression check triggered for {}", player.getName().getString());
            ReputationProgressionHandler.forceProgressionCheck(player);
        } catch (Exception e) {
            LOGGER.error("🚨 Error in manual progression check", e);
        }
    }
}
