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
 * Task requiring crafting a number of specific items.
 * Auto-completes when the player crafts or smelts the item — no manual slot placement needed.
 */
public class CraftItemsTask extends ResearchTask {
    private static final String CRAFT_COUNTS_TAG = "craft_item_counts";
    private static final Map<ResourceLocation, Integer> CLIENT_CRAFT_COUNTS = new ConcurrentHashMap<>();

    private final ResourceLocation item;
    private final int count;
    /** Optional crafting station hint used for the UI icon. */
    private final String station;

    public CraftItemsTask(ResourceLocation item, int count, String station) {
        super(ResearchTaskTypes.CRAFT_ITEMS);
        this.item = item;
        this.count = count;
        this.station = station != null ? station : "crafting_table";
    }

    public ResourceLocation getItem() { return item; }
    public int getCount() { return count; }
    public String getStation() { return station; }

    public static void recordCraft(ServerPlayer player, ResourceLocation itemId, int amount) {
        if (player == null || itemId == null) return;
        CompoundTag modData = player.getPersistentData().getCompound(EidolonUnchained.MODID);
        CompoundTag craftCounts = modData.getCompound(CRAFT_COUNTS_TAG);
        String key = itemId.toString();
        craftCounts.putInt(key, craftCounts.getInt(key) + amount);
        modData.put(CRAFT_COUNTS_TAG, craftCounts);
        player.getPersistentData().put(EidolonUnchained.MODID, modData);
    }

    public static void clearProgress(ServerPlayer player) {
        if (player == null) return;
        CompoundTag modData = player.getPersistentData().getCompound(EidolonUnchained.MODID);
        modData.remove(CRAFT_COUNTS_TAG);
        player.getPersistentData().put(EidolonUnchained.MODID, modData);
    }

    public static Map<ResourceLocation, Integer> getCraftCounts(ServerPlayer player) {
        Map<ResourceLocation, Integer> counts = new HashMap<>();
        if (player == null) return counts;
        CompoundTag craftCounts = player.getPersistentData()
            .getCompound(EidolonUnchained.MODID)
            .getCompound(CRAFT_COUNTS_TAG);
        for (String key : craftCounts.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id != null) counts.put(id, craftCounts.getInt(key));
        }
        return counts;
    }

    public static void syncClientProgress(Map<ResourceLocation, Integer> counts) {
        CLIENT_CRAFT_COUNTS.clear();
        CLIENT_CRAFT_COUNTS.putAll(counts);
    }

    public static int getClientCraftCount(ResourceLocation itemId) {
        return CLIENT_CRAFT_COUNTS.getOrDefault(itemId, 0);
    }

    public int getCraftCount(ServerPlayer player) {
        return getCraftCounts(player).getOrDefault(item, 0);
    }

    @Override
    public boolean isComplete(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            return getCraftCount(serverPlayer) >= count;
        }
        return getClientCraftCount(item) >= count;
    }
}
