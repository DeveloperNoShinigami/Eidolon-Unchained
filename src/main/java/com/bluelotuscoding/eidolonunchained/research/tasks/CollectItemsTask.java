package com.bluelotuscoding.eidolonunchained.research.tasks;

import net.minecraft.resources.ResourceLocation;

/**
 * Task requiring collecting a number of specific items.
 */
public class CollectItemsTask extends ResearchTask {
    private final ResourceLocation item;
    private final int count;

    public CollectItemsTask(ResourceLocation item, int count) {
        super(TaskType.COLLECT_ITEMS);
        this.item = item;
        this.count = count;
    }

    public ResourceLocation getItem() {
        return item;
    }

    public int getCount() {
        return count;
    }
}

