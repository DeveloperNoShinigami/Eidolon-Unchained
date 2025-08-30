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
     * 🎯 PLAYER LOGIN PROGRESSION CHECK
     * 
     * When a player logs in, check their current progression status.
     * This ensures any missed progression events are caught up.
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        
        try {
            LOGGER.info("🔍 Checking progression for player {} on login", player.getName().getString());
            
            // Trigger a progression check to catch up any missed events
            ReputationProgressionHandler.checkPlayerProgression(player);
            
        } catch (Exception e) {
            LOGGER.error("🚨 Error checking progression on player login", e);
        }
    }
    
    /**
     * 🧹 PLAYER LOGOUT CLEANUP
     * 
     * Clean up progression tracking data when player disconnects.
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        
        try {
            LOGGER.debug("🧹 Cleaning up progression data for player {}", player.getName().getString());
            
            // Clean up tracking data to prevent memory leaks
            ReputationProgressionHandler.cleanupPlayer(player.getUUID());
            
        } catch (Exception e) {
            LOGGER.error("🚨 Error cleaning up progression data on logout", e);
        }
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
