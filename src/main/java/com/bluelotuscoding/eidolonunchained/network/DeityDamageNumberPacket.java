package com.bluelotuscoding.eidolonunchained.network;

import com.bluelotuscoding.eidolonunchained.client.renderer.DeityDamageNumberRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DeityDamageNumberPacket {
    private final int targetEntityId;
    private final float damageAmount;
    private final int rgbColor;

    public DeityDamageNumberPacket(int targetEntityId, float damageAmount, int rgbColor) {
        this.targetEntityId = targetEntityId;
        this.damageAmount = damageAmount;
        this.rgbColor = rgbColor;
    }

    public static void encode(DeityDamageNumberPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.targetEntityId);
        buffer.writeFloat(packet.damageAmount);
        buffer.writeInt(packet.rgbColor);
    }

    public static DeityDamageNumberPacket decode(FriendlyByteBuf buffer) {
        return new DeityDamageNumberPacket(buffer.readInt(), buffer.readFloat(), buffer.readInt());
    }

    public static boolean handle(DeityDamageNumberPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(packet));
            }
        });
        context.setPacketHandled(true);
        return true;
    }

    private static void handleClient(DeityDamageNumberPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        Entity target = minecraft.level.getEntity(packet.targetEntityId);
        if (target == null) {
            return;
        }

        DeityDamageNumberRenderer.spawn(target, packet.damageAmount, packet.rgbColor);
    }
}
