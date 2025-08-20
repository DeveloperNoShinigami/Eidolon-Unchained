package com.bluelotuscoding.eidolonunchained.research.tasks;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Task requiring the player to possess a specified number of an item with matching NBT data.
 */
public class HasItemWithNbtTask extends ResearchTask {
    private final ResourceLocation item;
    private final int count;
    private final CompoundTag nbt;

    public HasItemWithNbtTask(ResourceLocation item, int count, CompoundTag nbt) {
        super(TaskType.HAS_ITEM_NBT);
        this.item = item;
        this.count = count;
        this.nbt = nbt;
    }

    public ResourceLocation getItem() {
        return item;
    }

    public int getCount() {
        return count;
    }

    public CompoundTag getNbt() {
        return nbt;
    }

    @Override
    public boolean isComplete(Player player) {
        if (player == null) return false;
        Item mcItem = ForgeRegistries.ITEMS.getValue(item);
        if (mcItem == null) return false;
        int found = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(mcItem)) {
                if (nbt != null && !nbt.isEmpty()) {
                    if (!stack.hasTag() || !nbt.equals(stack.getTag())) continue;
                }
                found += stack.getCount();
                if (found >= count) return true;
            }
        }
        return false;
    }
}
