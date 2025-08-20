package com.bluelotuscoding.eidolonunchained.research.tasks;

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
}


