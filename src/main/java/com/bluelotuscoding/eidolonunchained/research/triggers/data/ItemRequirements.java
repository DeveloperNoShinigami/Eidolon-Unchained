package com.bluelotuscoding.eidolonunchained.research.triggers.data;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Represents item requirements for research triggers
 */
public class ItemRequirements {
    @SerializedName("check_inventory")
    private boolean checkInventory = true;
    
    @SerializedName("items")
    private List<ItemRequirement> items;
    
    public boolean shouldCheckInventory() {
        return checkInventory;
    }
    
    public List<ItemRequirement> getItems() {
        return items;
    }
    
    public void setCheckInventory(boolean checkInventory) {
        this.checkInventory = checkInventory;
    }
    
    public void setItems(List<ItemRequirement> items) {
        this.items = items;
    }
}
