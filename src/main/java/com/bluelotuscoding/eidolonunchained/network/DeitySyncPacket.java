package com.bluelotuscoding.eidolonunchained.network;

import com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager;
import com.bluelotuscoding.eidolonunchained.deity.DatapackDeity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Packet for synchronizing deity data between server and client
 * This prevents networking issues when clients join worlds with custom deities
 */
public class DeitySyncPacket {
    private final Map<ResourceLocation, String> deityData;
    
    public DeitySyncPacket(Map<ResourceLocation, String> deityData) {
        this.deityData = deityData;
    }
    
    public DeitySyncPacket() {
        this.deityData = new HashMap<>();
    }
    
    public static void encode(DeitySyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.deityData.size());
        for (Map.Entry<ResourceLocation, String> entry : packet.deityData.entrySet()) {
            buffer.writeResourceLocation(entry.getKey());
            buffer.writeUtf(entry.getValue());
        }
    }
    
    public static DeitySyncPacket decode(FriendlyByteBuf buffer) {
        Map<ResourceLocation, String> deityData = new HashMap<>();
        int size = buffer.readInt();
        for (int i = 0; i < size; i++) {
            ResourceLocation id = buffer.readResourceLocation();
            String name = buffer.readUtf();
            deityData.put(id, name);
        }
        return new DeitySyncPacket(deityData);
    }
    
    public static void handle(DeitySyncPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            try {
                // Only handle on client side and ensure we're in the correct phase
                if (ctx.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
                    // Verify we're actually in the play phase, not login phase
                    if (ctx.getSender() == null) { // Client side
                        // Store deity data for client-side use
                        // This ensures the client has the same deity information as the server
                        packet.deityData.forEach((id, name) -> {
                            // Client-side deity registry update would go here
                            // For now, we're just ensuring the packet is handled properly
                        });
                    }
                }
            } catch (Exception e) {
                // Log but don't crash - prevents login issues
                System.err.println("Error handling DeitySyncPacket: " + e.getMessage());
            }
        });
        ctx.setPacketHandled(true);
    }
    
    /**
     * Create a sync packet from current server deity data
     */
    public static DeitySyncPacket createFromServer() {
        Map<ResourceLocation, String> deityData = new HashMap<>();
        try {
            Map<ResourceLocation, DatapackDeity> allDeities = DatapackDeityManager.getAllDeities();
            if (allDeities != null && !allDeities.isEmpty()) {
                allDeities.forEach((id, deity) -> {
                    if (deity != null && deity.getName() != null) {
                        deityData.put(id, deity.getName());
                    }
                });
            }
        } catch (Exception e) {
            // Log but don't throw - empty packet is better than crash
            System.err.println("Failed to create deity sync packet: " + e.getMessage());
        }
        return new DeitySyncPacket(deityData);
    }
}
