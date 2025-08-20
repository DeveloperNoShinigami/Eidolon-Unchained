package com.bluelotuscoding.eidolonunchained.research.conditions;

import net.minecraft.world.entity.player.Player;

/**
 * Base interface for conditional requirements that gate research entries.
 */
public interface ResearchCondition {
    /**
     * Returns true if the condition is satisfied for the given player.
     *
     * @param player the player to test against, may be null when no player context is available
     * @return true if the condition is met
     */
    boolean test(Player player);
}
