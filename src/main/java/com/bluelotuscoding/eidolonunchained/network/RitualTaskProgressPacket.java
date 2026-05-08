package com.bluelotuscoding.eidolonunchained.network;

import com.bluelotuscoding.eidolonunchained.research.tasks.CraftItemsTask;
import com.bluelotuscoding.eidolonunchained.research.tasks.KillEntitiesTask;
import com.bluelotuscoding.eidolonunchained.research.tasks.UseRitualTask;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Synchronizes ritual-completion counts and kill counts so task progress can be evaluated client-side.
 */
public class RitualTaskProgressPacket {
    private final Map<ResourceLocation, Integer> ritualCounts;
    private final Map<ResourceLocation, Integer> killCounts;
    private final Map<ResourceLocation, Integer> craftCounts;

    public RitualTaskProgressPacket(Map<ResourceLocation, Integer> ritualCounts,
                                    Map<ResourceLocation, Integer> killCounts,
                                    Map<ResourceLocation, Integer> craftCounts) {
        this.ritualCounts = ritualCounts;
        this.killCounts = killCounts;
        this.craftCounts = craftCounts;
    }

    public static RitualTaskProgressPacket create(ServerPlayer player) {
        return new RitualTaskProgressPacket(
            UseRitualTask.getCompletionCounts(player),
            KillEntitiesTask.getKillCounts(player),
            CraftItemsTask.getCraftCounts(player)
        );
    }

    private static void writeMap(FriendlyByteBuf buffer, Map<ResourceLocation, Integer> map) {
        buffer.writeInt(map.size());
        for (Map.Entry<ResourceLocation, Integer> entry : map.entrySet()) {
            buffer.writeResourceLocation(entry.getKey());
            buffer.writeInt(entry.getValue());
        }
    }

    private static Map<ResourceLocation, Integer> readMap(FriendlyByteBuf buffer) {
        int size = buffer.readInt();
        Map<ResourceLocation, Integer> map = new HashMap<>();
        for (int i = 0; i < size; i++) {
            map.put(buffer.readResourceLocation(), buffer.readInt());
        }
        return map;
    }

    public static void encode(RitualTaskProgressPacket packet, FriendlyByteBuf buffer) {
        writeMap(buffer, packet.ritualCounts);
        writeMap(buffer, packet.killCounts);
        writeMap(buffer, packet.craftCounts);
    }

    public static RitualTaskProgressPacket decode(FriendlyByteBuf buffer) {
        return new RitualTaskProgressPacket(readMap(buffer), readMap(buffer), readMap(buffer));
    }

    public static boolean handle(RitualTaskProgressPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
                UseRitualTask.syncClientProgress(packet.ritualCounts);
                KillEntitiesTask.syncClientProgress(packet.killCounts);
                CraftItemsTask.syncClientProgress(packet.craftCounts);
            }
        });
        context.setPacketHandled(true);
        return true;
    }
}
