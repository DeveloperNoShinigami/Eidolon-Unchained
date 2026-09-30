package com.bluelotuscoding.eidolonunchained.network;

import com.bluelotuscoding.eidolonunchained.imbue.ImbueCasting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client → server: right-click on an imbued weapon (cast) or view-key + right-click (cycle the active chant). */
public record ImbueInputPacket(Action action) {
    public enum Action { CAST, CYCLE }

    public static void encode(ImbueInputPacket p, FriendlyByteBuf buf) {
        buf.writeEnum(p.action);
    }

    public static ImbueInputPacket decode(FriendlyByteBuf buf) {
        return new ImbueInputPacket(buf.readEnum(Action.class));
    }

    public static void handle(ImbueInputPacket p, Supplier<NetworkEvent.Context> ctx) {
        var player = ctx.get().getSender();
        if (player != null) {
            var weapon = ImbueCasting.heldImbued(player);         // the server checks what is really in hand
            if (!weapon.isEmpty()) {
                switch (p.action) {
                    case CAST -> ImbueCasting.castImbued(player, weapon);
                    case CYCLE -> ImbueCasting.cycleImbued(player, weapon);
                }
            }
        }
        ctx.get().setPacketHandled(true);
    }
}
