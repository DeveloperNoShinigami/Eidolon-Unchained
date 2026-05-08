package com.bluelotuscoding.eidolonunchained.network;

import com.bluelotuscoding.eidolonunchained.client.gui.PotionSignStatusOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server -> client packet that maps an active status effect to a sign icon rendered
 * in the potion-effects HUD area.
 */
public class AttributeSignStatusIconPacket {
    private final ResourceLocation effectId;
    private final ResourceLocation signId;
    private final int durationTicks;
    /** True = buff (show with sign colour), false = debuff (show greyed out). */
    private final boolean positive;

    public AttributeSignStatusIconPacket(ResourceLocation effectId, ResourceLocation signId, int durationTicks, boolean positive) {
        this.effectId = effectId;
        this.signId = signId;
        this.durationTicks = durationTicks;
        this.positive = positive;
    }

    public static void encode(AttributeSignStatusIconPacket packet, FriendlyByteBuf buf) {
        buf.writeResourceLocation(packet.effectId);
        buf.writeResourceLocation(packet.signId);
        buf.writeInt(packet.durationTicks);
        buf.writeBoolean(packet.positive);
    }

    public static AttributeSignStatusIconPacket decode(FriendlyByteBuf buf) {
        return new AttributeSignStatusIconPacket(
            buf.readResourceLocation(), buf.readResourceLocation(), buf.readInt(), buf.readBoolean());
    }

    public static void handle(AttributeSignStatusIconPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isClient()) {
                handleClient(packet);
            }
        });
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(AttributeSignStatusIconPacket packet) {
        PotionSignStatusOverlay.bindSignToEffect(packet.effectId, packet.signId, packet.durationTicks, packet.positive);
    }
}
