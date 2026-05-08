package com.bluelotuscoding.eidolonunchained.events;

import net.minecraftforge.eventbus.api.Event;

/**
 * Event fired when AI deity configurations have been successfully linked to their deities.
 * This is the safe time to register systems that depend on both deities and AI configs,
 * such as AI prayer spells.
 */
public class AIConfigsLinkedEvent extends Event {
    private final int linkedCount;
    private final int failedCount;

    public AIConfigsLinkedEvent(int linkedCount, int failedCount) {
        this.linkedCount = linkedCount;
        this.failedCount = failedCount;
    }

    public int getLinkedCount() {
        return linkedCount;
    }

    public int getFailedCount() {
        return failedCount;
    }
}
