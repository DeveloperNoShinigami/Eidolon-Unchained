package com.bluelotuscoding.eidolonunchained.research.conditions;

import net.minecraft.world.entity.player.Player;

/**
 * Requires the world time to fall within a specific range.
 */
public class TimeCondition implements ResearchCondition {
    private final long min;
    private final long max;

    public TimeCondition(long min, long max) {
        this.min = min;
        this.max = max;
    }

    public long getMin() {
        return min;
    }

    public long getMax() {
        return max;
    }

    @Override
    public boolean test(Player player) {
        if (player == null) return true;
        long time = player.level().getDayTime() % 24000L;
        if (min <= max) {
            return time >= min && time <= max;
        } else {
            // wrap around midnight
            return time >= min || time <= max;
        }
    }
}
