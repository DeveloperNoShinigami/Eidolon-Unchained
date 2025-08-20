package com.bluelotuscoding.eidolonunchained.research.conditions;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Requires the player to have a certain number of a specific item in their inventory.
 */
public class InventoryCondition implements ResearchCondition {
    private final Item item;
    private final int count;

    public InventoryCondition(Item item, int count) {
        this.item = item;
        this.count = count;
    }

    public Item getItem() {
        return item;
    }

    public int getCount() {
        return count;
    }

    @Override
    public boolean test(Player player) {
        if (player == null) return true;
        int found = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                found += stack.getCount();
                if (found >= count) return true;
            }
        }
        return false;
    }
}
