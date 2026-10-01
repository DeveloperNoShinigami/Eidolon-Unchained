package com.bluelotuscoding.eidolonunchained.network;

import com.bluelotuscoding.eidolonunchained.imbue.ImbueCasting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client → server: right-click on an imbued weapon (cast), the next-chant key (cycle the active chant) or the awaken key (hexblade). */
public record ImbueInputPacket(Action action) {
    public enum Action { CAST, CYCLE, AWAKEN }

    public static void encode(ImbueInputPacket p, FriendlyByteBuf buf) {
        buf.writeEnum(p.action);
    }

    public static ImbueInputPacket decode(FriendlyByteBuf buf) {
        return new ImbueInputPacket(buf.readEnum(Action.class));
    }

    public static void handle(ImbueInputPacket p, Supplier<NetworkEvent.Context> ctx) {
        var player = ctx.get().getSender();
        if (player != null && p.action == Action.AWAKEN) {
            var held = player.getMainHandItem();               // the server checks what is really in hand
            if (held.getItem() instanceof com.bluelotuscoding.eidolonunchained.hexblade.HexbladeItem hb) hb.toggle(held, player);
        } else if (player != null) {
            var weapon = ImbueCasting.heldImbued(player);         // the server checks what is really in hand
            if (!weapon.isEmpty()) {
                switch (p.action) {
                    case CAST -> ImbueCasting.castImbued(player, weapon);
                    case CYCLE -> ImbueCasting.cycleImbued(player, weapon);
                    case AWAKEN -> { }
                }
            }
        }
        ctx.get().setPacketHandled(true);
    }
}
