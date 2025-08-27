package com.bluelotuscoding.eidolonunchained.research.triggers.data;

import com.google.gson.annotations.SerializedName;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * Represents an item requirement for research triggers
 */
public class ItemRequirement {
    @SerializedName("item")
    private String itemId;
    
    @SerializedName("nbt")
    private CompoundTag nbt;
    
    @SerializedName("count")
    private int count = 1;
    
    public ResourceLocation getItemId() {
        return new ResourceLocation(itemId);
    }
    
    public CompoundTag getNbt() {
        return nbt != null ? nbt : new CompoundTag();
    }
    
    public int getCount() {
        return count;
    }
    
    public void setItemId(String itemId) {
        this.itemId = itemId;
    }
    
    public void setNbt(CompoundTag nbt) {
        this.nbt = nbt;
    }
    
    public void setCount(int count) {
        this.count = count;
    }
}
