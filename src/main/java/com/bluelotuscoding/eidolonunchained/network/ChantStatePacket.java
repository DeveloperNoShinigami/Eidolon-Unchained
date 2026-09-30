package com.bluelotuscoding.eidolonunchained.network;

import com.bluelotuscoding.eidolonunchained.client.ChantClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Server → client: the player's chant state for the overlay. {@code signs} is the current sequence, {@code spell} the
 * spell it resolves to ("" when none), {@code cost} its mana cost, {@code slots} the sign id per slot ("" = unassigned).
 */
public record ChantStatePacket(int entityId, boolean winding, List<String> signs, String spell, int cost, List<String> slots) {
    public static void encode(ChantStatePacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.entityId);
        buf.writeBoolean(p.winding);
        buf.writeCollection(p.signs, (b, s) -> b.writeUtf(s, 255));
        buf.writeUtf(p.spell, 255);
        buf.writeVarInt(p.cost);
        buf.writeCollection(p.slots, (b, s) -> b.writeUtf(s, 255));
    }

    public static ChantStatePacket decode(FriendlyByteBuf buf) {
        int id = buf.readVarInt();
        boolean winding = buf.readBoolean();
        var signs = buf.readCollection(ArrayList::new, b -> b.readUtf(255));
        var spell = buf.readUtf(255);
        var cost = buf.readVarInt();
        var slots = buf.readCollection(ArrayList::new, b -> b.readUtf(255));
        return new ChantStatePacket(id, winding, signs, spell, cost, slots);
    }

    public static void handle(ChantStatePacket p, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ChantClient.accept(p));
        ctx.get().setPacketHandled(true);
    }
}
