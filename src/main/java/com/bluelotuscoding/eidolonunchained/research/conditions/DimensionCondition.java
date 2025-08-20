package com.bluelotuscoding.eidolonunchained.research.conditions;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Requires the player to be in a specific dimension.
 */
public class DimensionCondition implements ResearchCondition {
    private final ResourceLocation dimension;

    public DimensionCondition(ResourceLocation dimension) {
        this.dimension = dimension;
    }

    public ResourceLocation getDimension() {
        return dimension;
    }

    @Override
    public boolean test(Player player) {
        if (player == null) return true;
        return player.level().dimension().location().equals(dimension);
    }
}
