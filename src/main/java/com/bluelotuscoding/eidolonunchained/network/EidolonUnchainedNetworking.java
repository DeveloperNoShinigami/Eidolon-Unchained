package com.bluelotuscoding.eidolonunchained.network;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Networking system for Eidolon Unchained
 * Handles client-server synchronization of custom data
 */
public class EidolonUnchainedNetworking {
    private static final String PROTOCOL_VERSION = "1.0";
    
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(EidolonUnchained.MODID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );
    
    private static int packetId = 0;
    
    public static void register() {
        // Register deity sync packet for proper client-server synchronization
        INSTANCE.registerMessage(
            ++packetId,
            DeitySyncPacket.class,
            DeitySyncPacket::encode,
            DeitySyncPacket::decode,
            DeitySyncPacket::handle
        );
        
        // Add more packets here as needed
    }
}
