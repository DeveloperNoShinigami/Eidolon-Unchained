package com.bluelotuscoding.eidolonunchained.research.tasks;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

/**
 * Task requiring the player to possess an item that matches a given NBT filter.
 */
public class HasItemWithNbtTask extends ResearchTask {
    private final ResourceLocation item;
    @Nullable
    private final CompoundTag filter;
    private final int count;

    public HasItemWithNbtTask(ResourceLocation item, @Nullable CompoundTag filter, int count) {
        super(ResearchTaskTypes.HAS_ITEM_NBT);
        this.item = item;
        this.filter = filter;
        this.count = count;
    }

    public ResourceLocation getItem() {
        return item;
    }

    @Nullable
    public CompoundTag getFilter() {
        return filter;
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
                if (filter != null) {
                    CompoundTag tag = stack.getTag();
                    if (tag == null || !NbtUtils.compareNbt(filter, tag, true)) continue;
                }
                found += stack.getCount();
                if (found >= count) return true;
            }
        }
        return false;
    }
}
