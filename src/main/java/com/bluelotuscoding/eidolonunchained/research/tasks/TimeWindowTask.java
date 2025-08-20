package com.bluelotuscoding.eidolonunchained.research.tasks;

import net.minecraft.world.entity.player.Player;

/**
 * Task requiring the world time to fall within a specific range.
 */
public class TimeWindowTask extends ResearchTask {
    private final long min;
    private final long max;

    public TimeWindowTask(long min, long max) {
        super(TaskType.TIME_WINDOW);
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
    public boolean isComplete(Player player) {
        if (player == null) return false;
        long time = player.level().getDayTime() % 24000L;
        if (min <= max) {
            return time >= min && time <= max;
        } else {
            // wrap around midnight
            return time >= min || time <= max;
        }
    }
}

