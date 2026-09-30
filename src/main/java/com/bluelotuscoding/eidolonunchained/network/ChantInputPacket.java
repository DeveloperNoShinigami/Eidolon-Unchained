package com.bluelotuscoding.eidolonunchained.network;

import com.bluelotuscoding.eidolonunchained.casting.PlayerChantState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client → server: a chant input. The server owns the state (rule C1); the client only reports what was pressed. */
public record ChantInputPacket(Action action, int slot, String signId) {
    public enum Action { SIGN, CAST, CLEAR, ASSIGN, UNASSIGN }

    public static ChantInputPacket sign(int slot) {
        return new ChantInputPacket(Action.SIGN, slot, "");
    }

    public static ChantInputPacket cast() {
        return new ChantInputPacket(Action.CAST, 0, "");
    }

    public static ChantInputPacket clear() {
        return new ChantInputPacket(Action.CLEAR, 0, "");
    }

    public static ChantInputPacket assign(int slot, String signId) {
        return new ChantInputPacket(Action.ASSIGN, slot, signId);
    }

    public static void encode(ChantInputPacket p, FriendlyByteBuf buf) {
        buf.writeEnum(p.action);
        buf.writeVarInt(p.slot);
        buf.writeUtf(p.signId, 255);
    }

    public static ChantInputPacket decode(FriendlyByteBuf buf) {
        return new ChantInputPacket(buf.readEnum(Action.class), buf.readVarInt(), buf.readUtf(255));
    }

    public static void handle(ChantInputPacket p, Supplier<NetworkEvent.Context> ctx) {
        var player = ctx.get().getSender();
        if (player == null) return;
        var state = PlayerChantState.of(player);
        switch (p.action) {
            case SIGN -> state.pressSlot(p.slot);
            case CAST -> state.cast();                         // scripts/commands only; the keys never send it (D37)
            case CLEAR -> state.clear(PlayerChantState.ClearReason.PLAYER);
            case ASSIGN -> state.assign(p.slot, p.signId);
            case UNASSIGN -> state.assign(p.slot, null);
        }
        ctx.get().setPacketHandled(true);
    }
}
