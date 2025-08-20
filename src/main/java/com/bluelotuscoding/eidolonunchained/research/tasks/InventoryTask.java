package com.bluelotuscoding.eidolonunchained.research.tasks;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Task requiring the player to possess a specified number of an item.
 */
public class InventoryTask extends ResearchTask {
    private final ResourceLocation item;
    private final int count;

    public InventoryTask(ResourceLocation item, int count) {
        super(TaskType.INVENTORY);
        this.item = item;
        this.count = count;
    }

    public ResourceLocation getItem() {
        return item;
    }

    public int getCount() {
        return count;
    }

    @Override
    public boolean isComplete(Player player) {
        if (player == null) return false;
        Item mcItem = ForgeRegistries.ITEMS.getValue(item);
        if (mcItem == null) return false;
        int found = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(mcItem)) {
                found += stack.getCount();
                if (found >= count) return true;
            }
        }
        return false;
    }
}

