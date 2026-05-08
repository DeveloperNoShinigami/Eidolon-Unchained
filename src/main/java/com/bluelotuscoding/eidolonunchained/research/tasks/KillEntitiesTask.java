package com.bluelotuscoding.eidolonunchained.research.tasks;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Task requiring a number of entities to be killed.
 */
public class KillEntitiesTask extends ResearchTask {
    private static final String KILL_COUNTS_TAG = "kill_entity_counts";
    private static final Map<ResourceLocation, Integer> CLIENT_KILL_COUNTS = new ConcurrentHashMap<>();

    private final ResourceLocation entity;
    private final int count;

    public KillEntitiesTask(ResourceLocation entity, int count) {
        super(ResearchTaskTypes.KILL_ENTITIES);
        this.entity = entity;
        this.count = count;
    }

    public ResourceLocation getEntity() {
        return entity;
    }

    public int getCount() {
        return count;
    }

    public static void recordKill(ServerPlayer player, ResourceLocation entityId) {
        if (player == null || entityId == null) return;
        CompoundTag modData = player.getPersistentData().getCompound(EidolonUnchained.MODID);
        CompoundTag killCounts = modData.getCompound(KILL_COUNTS_TAG);
        String key = entityId.toString();
        killCounts.putInt(key, killCounts.getInt(key) + 1);
        modData.put(KILL_COUNTS_TAG, killCounts);
        player.getPersistentData().put(EidolonUnchained.MODID, modData);
    }

    public static void clearProgress(ServerPlayer player) {
        if (player == null) return;
        CompoundTag modData = player.getPersistentData().getCompound(EidolonUnchained.MODID);
        modData.remove(KILL_COUNTS_TAG);
        player.getPersistentData().put(EidolonUnchained.MODID, modData);
    }

    public static Map<ResourceLocation, Integer> getKillCounts(ServerPlayer player) {
        Map<ResourceLocation, Integer> counts = new HashMap<>();
        if (player == null) return counts;
        CompoundTag killCounts = player.getPersistentData()
            .getCompound(EidolonUnchained.MODID)
            .getCompound(KILL_COUNTS_TAG);
        for (String key : killCounts.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id != null) counts.put(id, killCounts.getInt(key));
        }
        return counts;
    }

    public static void syncClientProgress(Map<ResourceLocation, Integer> counts) {
        CLIENT_KILL_COUNTS.clear();
        CLIENT_KILL_COUNTS.putAll(counts);
    }

    public static int getClientKillCount(ResourceLocation entityId) {
        return CLIENT_KILL_COUNTS.getOrDefault(entityId, 0);
    }

    public int getKillCount(ServerPlayer player) {
        return getKillCounts(player).getOrDefault(entity, 0);
    }

    @Override
    public boolean isComplete(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            return getKillCount(serverPlayer) >= count;
        }
        return getClientKillCount(entity) >= count;
    }
}
