package com.bluelotuscoding.eidolonunchained.network;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/**
 * Handles sending datapack synchronization to clients when they log in
 * This fixes the core issue where clients never received custom datapack content
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerLoginHandler {
    
    /**
     * Send datapack sync packet when player logs in
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            try {
                System.out.println("Player " + serverPlayer.getName().getString() + " logged in, sending datapack sync...");
                
                // Create and send comprehensive datapack sync packet
                DatapackSyncPacket syncPacket = new DatapackSyncPacket();
                EidolonUnchainedNetworking.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> serverPlayer), 
                    syncPacket
                );
                
                System.out.println("Datapack sync packet sent to " + serverPlayer.getName().getString());
                
            } catch (Exception e) {
                System.err.println("Failed to send datapack sync to player " + 
                                 serverPlayer.getName().getString() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
