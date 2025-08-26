package com.bluelotuscoding.eidolonunchained.network;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.keybind.ChantCastPacket;
import com.bluelotuscoding.eidolonunchained.keybind.ChantSlotActivationPacket;
import com.bluelotuscoding.eidolonunchained.keybind.ChantInterfacePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Networking system for Eidolon Unchained
 * Handles client-server synchronization of custom data
 * Fixed to prevent login phase packet conflicts
 */
public class EidolonUnchainedNetworking {
    private static final String PROTOCOL_VERSION = "1";
    
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(EidolonUnchained.MODID, "main"),
        () -> PROTOCOL_VERSION,
        // Accept any version on client - prevents login issues
        version -> true,
        // Accept any version on server - prevents login issues  
        version -> true
    );
    
    private static int packetId = 0;
    
    public static void register() {
        // Register deity sync packet for proper client-server synchronization
        // Only register if both client and server support our protocol
        try {
            INSTANCE.registerMessage(
                ++packetId,
                DeitySyncPacket.class,
                DeitySyncPacket::encode,
                DeitySyncPacket::decode,
                DeitySyncPacket::handle
            );
            
            // Register chant cast packet for keybind-based casting
            INSTANCE.registerMessage(
                ++packetId,
                ChantCastPacket.class,
                ChantCastPacket::toBytes,
                ChantCastPacket::new,
                ChantCastPacket::handle
            );
            
            // Register chant slot activation packet
            INSTANCE.registerMessage(
                ++packetId,
                ChantSlotActivationPacket.class,
                ChantSlotActivationPacket::toBytes,
                ChantSlotActivationPacket::new,
                ChantSlotActivationPacket::handle
            );
            
            // Register chant interface packet
            INSTANCE.registerMessage(
                ++packetId,
                ChantInterfacePacket.class,
                ChantInterfacePacket::toBytes,
                ChantInterfacePacket::new,
                ChantInterfacePacket::handle
            );
            
            // Add more packets here as needed
        } catch (Exception e) {
            // Log but don't crash - networking is optional for core functionality
            System.err.println("Failed to register Eidolon Unchained packets: " + e.getMessage());
        }
    }
}
