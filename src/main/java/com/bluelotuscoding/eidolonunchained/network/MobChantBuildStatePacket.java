package com.bluelotuscoding.eidolonunchained.network;

import com.bluelotuscoding.eidolonunchained.ai.MobChantCastingGoal;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MobChantBuildStatePacket {
    private final int entityId;
    private final boolean active;
    private final String chantId;
    private final int progress;

    private MobChantBuildStatePacket(int entityId, boolean active, String chantId, int progress) {
        this.entityId = entityId;
        this.active = active;
        this.chantId = chantId;
        this.progress = progress;
    }

    public static MobChantBuildStatePacket active(int entityId, ResourceLocation chantId, int progress) {
        return new MobChantBuildStatePacket(entityId, true, chantId.toString(), progress);
    }

    public static MobChantBuildStatePacket clear(int entityId) {
        return new MobChantBuildStatePacket(entityId, false, "", 0);
    }

    public static void encode(MobChantBuildStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.entityId);
        buffer.writeBoolean(packet.active);
        buffer.writeUtf(packet.chantId);
        buffer.writeInt(packet.progress);
    }

    public static MobChantBuildStatePacket decode(FriendlyByteBuf buffer) {
        return new MobChantBuildStatePacket(
            buffer.readInt(),
            buffer.readBoolean(),
            buffer.readUtf(),
            buffer.readInt()
        );
    }

    public static boolean handle(MobChantBuildStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(packet));
            }
        });
        context.setPacketHandled(true);
        return true;
    }

    private static void handleClient(MobChantBuildStatePacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        Entity entity = minecraft.level.getEntity(packet.entityId);
        if (!(entity instanceof Mob mob)) {
            return;
        }

        if (packet.active) {
            mob.getPersistentData().putString(MobChantCastingGoal.CHANT_BUILD_ID_TAG, packet.chantId);
            mob.getPersistentData().putInt(MobChantCastingGoal.CHANT_BUILD_PROGRESS_TAG, packet.progress);
            return;
        }

        mob.getPersistentData().remove(MobChantCastingGoal.CHANT_BUILD_ID_TAG);
        mob.getPersistentData().remove(MobChantCastingGoal.CHANT_BUILD_PROGRESS_TAG);
        mob.getPersistentData().remove(MobChantCastingGoal.CHANT_BUILD_NEXT_TICK_TAG);
    }
}
