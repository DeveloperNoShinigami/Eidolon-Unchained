package com.bluelotuscoding.eidolonunchained.network;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Handles networking events for Eidolon Unchained
 * Ensures proper synchronization between client and server
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID)
public class NetworkEventHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * Send sync packets when players join the world
     * Uses PlayerLoggedInEvent which fires AFTER login is complete
     * This prevents networking issues during the login phase
     */
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            // Log player join for debugging
            LOGGER.info("Player joined: {}", serverPlayer.getName().getString());
            
            // Wait a few ticks before sending custom packets to ensure login is fully complete
            serverPlayer.getServer().execute(() -> {
                try {
                    // Verify the player is still connected and in play phase
                    if (serverPlayer.connection != null && !serverPlayer.hasDisconnected()) {
                        LOGGER.info("Sending comprehensive datapack sync to player: {}", serverPlayer.getName().getString());
                        
                        // Send comprehensive datapack sync packet (includes deities, chants, codex)
                        DatapackSyncPacket comprehensiveSync = new DatapackSyncPacket();
                        EidolonUnchainedNetworking.INSTANCE.send(
                            PacketDistributor.PLAYER.with(() -> serverPlayer), 
                            comprehensiveSync
                        );
                        LOGGER.info("Sent comprehensive datapack sync to player: {}", serverPlayer.getName().getString());
                        
                        // Keep legacy deity sync for compatibility
                        DeitySyncPacket legacySync = DeitySyncPacket.createFromServer();
                        if (legacySync != null) {
                            EidolonUnchainedNetworking.INSTANCE.send(
                                PacketDistributor.PLAYER.with(() -> serverPlayer), 
                                legacySync
                            );
                            LOGGER.info("Sent legacy deity sync packet to player: {}", serverPlayer.getName().getString());
                        }
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to send sync packets to player {}: {}", 
                        serverPlayer.getName().getString(), e.getMessage());
                    e.printStackTrace(); // Add stack trace for debugging
                    // Don't throw exception - allow player to stay connected
                }
            });
        }
    }
}
