package com.bluelotuscoding.eidolonunchained.research.tasks;

import net.minecraft.resources.ResourceLocation;

/**
 * Task requiring a number of entities to be killed.
 */
public class KillEntitiesTask extends ResearchTask {
    private final ResourceLocation entity;
    private final int count;

    public KillEntitiesTask(ResourceLocation entity, int count) {
        super(ResearchTaskTypes.KILL_ENTITIES);
        this.entity = entity;
        this.count = count;
    }

    public ResourceLocation getEntity() {
        return entity;
    }

    public int getCount() {
        return count;
    }
}

