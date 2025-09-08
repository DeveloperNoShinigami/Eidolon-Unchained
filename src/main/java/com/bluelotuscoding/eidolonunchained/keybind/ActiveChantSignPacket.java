package com.bluelotuscoding.eidolonunchained.keybind;

import com.bluelotuscoding.eidolonunchained.chant.ActiveChantingSystem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Network packet for active chanting sign additions.
 * Sent when player presses individual sign keybinds (G,H,J,K).
 */
public class ActiveChantSignPacket {
    private final int signSlot;
    private final Action action;
    
    public enum Action {
        ADD_SIGN,    // G,H,J,K keys - add individual signs
        CLEAR_CHANT  // C key - clear current sequence
    }
    
    public ActiveChantSignPacket(int signSlot, Action action) {
        this.signSlot = signSlot;
        this.action = action;
    }
    
    public static void encode(ActiveChantSignPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.signSlot);
        buf.writeEnum(msg.action);
    }
    
    public static ActiveChantSignPacket decode(FriendlyByteBuf buf) {
        return new ActiveChantSignPacket(buf.readInt(), buf.readEnum(Action.class));
    }
    
    public static void handle(ActiveChantSignPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                switch (msg.action) {
                    case ADD_SIGN -> ActiveChantingSystem.addSignToChant(player, msg.signSlot);
                    case CLEAR_CHANT -> ActiveChantingSystem.clearActiveChant(player);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
