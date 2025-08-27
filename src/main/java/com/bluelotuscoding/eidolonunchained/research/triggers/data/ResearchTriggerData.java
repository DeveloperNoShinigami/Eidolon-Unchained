package com.bluelotuscoding.eidolonunchained.research.triggers.data;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Container for research trigger definitions
 */
public class ResearchTriggerData {
    @SerializedName("triggers")
    private List<ResearchTrigger> triggers;
    
    public List<ResearchTrigger> getTriggers() {
        return triggers;
    }
    
    public void setTriggers(List<ResearchTrigger> triggers) {
        this.triggers = triggers;
    }
}
