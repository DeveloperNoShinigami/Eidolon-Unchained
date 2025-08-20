package com.bluelotuscoding.eidolonunchained.research.tasks;

import java.util.Locale;

/**
 * Basic representation of a research task parsed from JSON.
 * Concrete implementations store task-specific data.
 */
public abstract class ResearchTask {
    private final TaskType type;

    protected ResearchTask(TaskType type) {
        this.type = type;
    }

    public TaskType getType() {
        return type;
    }

    /**
     * Supported task types.
     */
    public enum TaskType {
        KILL_ENTITIES("kill_entities"),
        CRAFT_ITEMS("craft_items"),
        USE_RITUAL("use_ritual"),
        COLLECT_ITEMS("collect_items");

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

