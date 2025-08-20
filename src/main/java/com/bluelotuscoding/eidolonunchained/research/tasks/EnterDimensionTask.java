package com.bluelotuscoding.eidolonunchained.research.tasks;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Task requiring the player to be present in a specific dimension.
 * <p>This mirrors {@link com.bluelotuscoding.eidolonunchained.research.conditions.DimensionCondition}
 * but is expressed as a task so that it can participate in the normal research
 * objective system.</p>
 */
public class EnterDimensionTask extends ResearchTask {
    private final ResourceLocation dimension;

    public EnterDimensionTask(ResourceLocation dimension) {
        super(TaskType.ENTER_DIMENSION);
        this.dimension = dimension;
    }

    public ResourceLocation getDimension() {
        return dimension;
    }

    @Override
    public boolean isComplete(Player player) {
        if (player == null) return false;
        return player.level().dimension().location().equals(dimension);
    }
}

