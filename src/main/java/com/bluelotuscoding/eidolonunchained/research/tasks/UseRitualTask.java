package com.bluelotuscoding.eidolonunchained.research.tasks;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Task requiring performing a specific ritual multiple times.
 */
public class UseRitualTask extends ResearchTask {
    private static final String COMPLETED_RITUALS_TAG = "completed_rituals";
    private static final Map<ResourceLocation, Integer> CLIENT_COMPLETION_COUNTS = new ConcurrentHashMap<>();

    private final ResourceLocation ritual;
    private final int count;

    public UseRitualTask(ResourceLocation ritual, int count) {
        super(ResearchTaskTypes.USE_RITUAL);
        this.ritual = ritual;
        this.count = count;
    }

    public ResourceLocation getRitual() {
        return ritual;
    }

    public int getCount() {
        return count;
    }

    public static void recordCompletion(ServerPlayer player, ResourceLocation ritualId) {
        if (player == null || ritualId == null) {
            return;
        }

        CompoundTag modData = player.getPersistentData().getCompound(EidolonUnchained.MODID);
        CompoundTag completedRituals = modData.getCompound(COMPLETED_RITUALS_TAG);
        String ritualKey = ritualId.toString();

        completedRituals.putInt(ritualKey, completedRituals.getInt(ritualKey) + 1);
        modData.put(COMPLETED_RITUALS_TAG, completedRituals);
        player.getPersistentData().put(EidolonUnchained.MODID, modData);
    }

    public static void clearProgress(ServerPlayer player) {
        if (player == null) return;
        CompoundTag modData = player.getPersistentData().getCompound(EidolonUnchained.MODID);
        modData.remove(COMPLETED_RITUALS_TAG);
        player.getPersistentData().put(EidolonUnchained.MODID, modData);
    }

    public static Map<ResourceLocation, Integer> getCompletionCounts(ServerPlayer player) {
        Map<ResourceLocation, Integer> counts = new HashMap<>();
        if (player == null) {
            return counts;
        }

        CompoundTag completedRituals = player.getPersistentData()
            .getCompound(EidolonUnchained.MODID)
            .getCompound(COMPLETED_RITUALS_TAG);

        for (String key : completedRituals.getAllKeys()) {
            ResourceLocation ritualId = ResourceLocation.tryParse(key);
            if (ritualId != null) {
                counts.put(ritualId, completedRituals.getInt(key));
            }
        }

        return counts;
    }

    public static void syncClientProgress(Map<ResourceLocation, Integer> counts) {
        CLIENT_COMPLETION_COUNTS.clear();
        CLIENT_COMPLETION_COUNTS.putAll(counts);
    }

    public static int getClientCompletionCount(ResourceLocation ritualId) {
        return CLIENT_COMPLETION_COUNTS.getOrDefault(ritualId, 0);
    }

    public int getCompletionCount(ServerPlayer player) {
        return getCompletionCounts(player).getOrDefault(ritual, 0);
    }

    @Override
    public boolean isComplete(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            return getCompletionCount(serverPlayer) >= count;
        }

        return getClientCompletionCount(ritual) >= count;
    }
}

