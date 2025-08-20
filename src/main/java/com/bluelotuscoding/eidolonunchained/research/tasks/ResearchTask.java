package com.bluelotuscoding.eidolonunchained.research.tasks;

import java.util.Locale;
import net.minecraft.world.entity.player.Player;

/**
 * Basic representation of a research task parsed from JSON.
 * Concrete implementations store task-specific data.
 */
public abstract class ResearchTask {
    private final ResearchTaskType type;

    protected ResearchTask(ResearchTaskType type) {
        this.type = type;
    }

    public ResearchTaskType getType() {
        return type;
    }

    /**
     * Checks whether this task is considered complete for the given player.
     *
     * <p>Most tasks track progress through the base Eidolon research system and
     * thus always return {@code false} here.  Tasks that mirror
     * {@link com.bluelotuscoding.eidolonunchained.research.conditions.ResearchCondition}
     * implementations can override this to provide on‑the‑fly evaluation and
     * automatically mark themselves complete.</p>
     *
     * @param player the player to test against
     * @return {@code true} if the task has been satisfied
     */
    public boolean isComplete(Player player) {
        return false;
    }

    /**
     * Supported task types.
     */
    public enum TaskType {
        KILL_ENTITIES("kill_entities"),
        CRAFT_ITEMS("craft_items"),
        USE_RITUAL("use_ritual"),
        COLLECT_ITEMS("collect_items"),
        ENTER_DIMENSION("enter_dimension"),
        TIME_WINDOW("time_window"),
        WEATHER("weather"),
        INVENTORY("inventory");

        private final String id;

        TaskType(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        /**
         * Looks up a task type by its string identifier.
         *
         * @param id The identifier from JSON.
         * @return Matching TaskType or null if unknown.
         */
        public static TaskType byId(String id) {
            for (TaskType t : values()) {
                if (t.id.equals(id)) return t;
            }
            return null;
        }

        @Override
        public String toString() {
            return id.toLowerCase(Locale.ROOT);
        }
    }
}


