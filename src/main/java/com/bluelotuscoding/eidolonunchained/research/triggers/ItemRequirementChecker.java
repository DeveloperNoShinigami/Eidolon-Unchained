package com.bluelotuscoding.eidolonunchained.research.triggers;

import com.bluelotuscoding.eidolonunchained.research.triggers.data.ItemRequirement;
import com.bluelotuscoding.eidolonunchained.research.triggers.data.ItemRequirements;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Utility class for checking item requirements across player inventory
 */
public class ItemRequirementChecker {
    
    /**
     * Check if player meets item requirements
     */
    public static boolean checkItemRequirements(Player player, ItemRequirements requirements) {
        if (requirements == null || requirements.getItems() == null || requirements.getItems().isEmpty()) {
            return true; // No requirements means always pass
        }
        
        for (ItemRequirement requirement : requirements.getItems()) {
            if (!hasRequiredItem(player, requirement, requirements.shouldCheckInventory())) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Check if player has a specific item with NBT matching
     */
    private static boolean hasRequiredItem(Player player, ItemRequirement requirement, boolean checkInventory) {
        // Check mainhand first
        if (matchesItem(player.getMainHandItem(), requirement)) {
            return true;
        }
        
        // Check offhand
        if (matchesItem(player.getOffhandItem(), requirement)) {
            return true;
        }
        
        // Check full inventory if enabled
        if (checkInventory) {
            for (ItemStack stack : player.getInventory().items) {
                if (matchesItem(stack, requirement)) {
                    return true;
                }
            }
            
            // Check armor slots
            for (ItemStack stack : player.getInventory().armor) {
                if (matchesItem(stack, requirement)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Check if an ItemStack matches the requirement including NBT
     */
    private static boolean matchesItem(ItemStack stack, ItemRequirement requirement) {
        if (stack.isEmpty()) {
            return false;
        }
        
        // Check item type
        if (!ForgeRegistries.ITEMS.getKey(stack.getItem()).equals(requirement.getItemId())) {
            return false;
        }
        
        // Check count
        if (stack.getCount() < requirement.getCount()) {
            return false;
        }
        
        // Check NBT if specified
        CompoundTag requiredNbt = requirement.getNbt();
        if (!requiredNbt.isEmpty()) {
            CompoundTag stackNbt = stack.getTag();
            if (stackNbt == null) {
                return false;
            }
            
            // Check if all required NBT tags are present and match
            return containsAllTags(stackNbt, requiredNbt);
        }
        
        return true;
    }
    
    /**
     * Check if actualNbt contains all tags from requiredNbt with matching values
     */
    private static boolean containsAllTags(CompoundTag actualNbt, CompoundTag requiredNbt) {
        for (String key : requiredNbt.getAllKeys()) {
            if (!actualNbt.contains(key)) {
                return false;
            }
            
            // Compare tag values
            if (!actualNbt.get(key).equals(requiredNbt.get(key))) {
                return false;
            }
        }
        
        return true;
    }
}
