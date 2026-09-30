package com.bluelotuscoding.eidolonunchained.network;

import com.bluelotuscoding.eidolonunchained.client.ChantClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Server -> tracking clients: the signs a mob has built so far (empty = ring gone); the client renders the chant ring. */
public record MobChantStatePacket(int entityId, List<String> signs) {
    public static void encode(MobChantStatePacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.entityId);
        buf.writeCollection(p.signs, (b, s) -> b.writeUtf(s, 255));
    }

    public static MobChantStatePacket decode(FriendlyByteBuf buf) {
        int id = buf.readVarInt();
        return new MobChantStatePacket(id, buf.readCollection(ArrayList::new, b -> b.readUtf(255)));
    }

    public static void handle(MobChantStatePacket p, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ChantClient.acceptMob(p));
        ctx.get().setPacketHandled(true);
    }
}
