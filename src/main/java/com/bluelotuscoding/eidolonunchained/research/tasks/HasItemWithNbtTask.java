package com.bluelotuscoding.eidolonunchained.research.tasks;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Task requiring the player to possess a specific item with optional NBT data.
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
        Item target = ForgeRegistries.ITEMS.getValue(item);
        if (target == null) return false;
        int found = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.is(target)) continue;
            if (filter != null && !NbtUtils.compareNbt(filter, stack.getTag(), true)) continue;
            found += stack.getCount();
            if (found >= count) return true;

        }
        return false;
    }
}
