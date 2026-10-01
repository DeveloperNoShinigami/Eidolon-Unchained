package com.bluelotuscoding.eidolonunchained.network;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** Eidolon Unchained's packet channel. Phase 3: chant input (client → server) and chant state (server → client). */
public final class EUNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(EidolonUnchained.MOD_ID, "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private EUNetwork() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(ChantInputPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ChantInputPacket::encode).decoder(ChantInputPacket::decode).consumerMainThread(ChantInputPacket::handle).add();
        CHANNEL.messageBuilder(ChantStatePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ChantStatePacket::encode).decoder(ChantStatePacket::decode).consumerMainThread(ChantStatePacket::handle).add();
        CHANNEL.messageBuilder(MobChantStatePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(MobChantStatePacket::encode).decoder(MobChantStatePacket::decode).consumerMainThread(MobChantStatePacket::handle).add();
        CHANNEL.messageBuilder(ImbueInputPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ImbueInputPacket::encode).decoder(ImbueInputPacket::decode).consumerMainThread(ImbueInputPacket::handle).add();
        CHANNEL.messageBuilder(CallingPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CallingPacket::encode).decoder(CallingPacket::decode).consumerMainThread(CallingPacket::handle).add();
    }

    public static void sendTo(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToTrackingAndSelf(net.minecraft.world.entity.Entity entity, Object packet) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet);
    }

    public static void sendToTracking(net.minecraft.world.entity.Entity entity, Object packet) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), packet);
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }
}
