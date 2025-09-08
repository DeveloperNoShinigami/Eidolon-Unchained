package com.bluelotuscoding.eidolonunchained.network;

import com.bluelotuscoding.eidolonunchained.chant.ActiveChantingSystem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Network packet for Active Chanting System communication
 * Handles adding signs to active chant sequences and chant management
 */
public class ActiveChantSignPacket {
    private final int signSlot;
    private final Action action;
    
    public enum Action {
        ADD_SIGN,
        CLEAR_CHANT
    }
    
    public ActiveChantSignPacket(int signSlot, Action action) {
        this.signSlot = signSlot;
        this.action = action;
    }
    
    public static void encode(ActiveChantSignPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.signSlot);
        buffer.writeEnum(packet.action);
    }
    
    public static ActiveChantSignPacket decode(FriendlyByteBuf buffer) {
        int signSlot = buffer.readInt();
        Action action = buffer.readEnum(Action.class);
        return new ActiveChantSignPacket(signSlot, action);
    }
    
    public static void handle(ActiveChantSignPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            
            switch (packet.action) {
                case ADD_SIGN -> {
                    // Add sign to player's active chant sequence
                    ActiveChantingSystem.addSignToChant(player, packet.signSlot);
                }
                case CLEAR_CHANT -> {
                    // Clear player's active chant sequence
                    ActiveChantingSystem.clearActiveChant(player);
                }
            }
        });
        context.setPacketHandled(true);
    }
}
