package com.bluelotuscoding.eidolonunchained.research.tasks;

import net.minecraft.resources.ResourceLocation;

/**
 * Task requiring performing a specific ritual multiple times.
 */
public class UseRitualTask extends ResearchTask {
    private final ResourceLocation ritual;
    private final int count;

    public UseRitualTask(ResourceLocation ritual, int count) {
        super(ResearchTaskTypes.USE_RITUAL);
        this.ritual = ritual;
        this.count = count;
    }

    public ResourceLocation getRitual() {
        return ritual;
    }

    public int getCount() {
        return count;
    }
}

