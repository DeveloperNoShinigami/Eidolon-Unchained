package com.bluelotuscoding.eidolonunchained.ai;

import java.util.List;
import java.util.ArrayList;

/**
 * Configuration for the deity task system
 */
public class TaskSystemConfig {
    public boolean enabled = true;
    public int maxActiveTasks = 3;
    public List<TaskTemplate> availableTasks = new ArrayList<>();
    public TaskAssignmentBehavior taskAssignmentBehavior = new TaskAssignmentBehavior();
    
    public enum AssignmentMode {
        AI,              // AI assigns during conversation (default, existing behavior)
        DETERMINISTIC,   // Only deterministic conditions trigger assignment
        BOTH,            // AI can assign OR deterministic conditions fire (first wins)
        MANUAL           // Only assigned via command (modpack scripted events)
    }

    public static class DeterministicAssign {
        public int reputationMin = 0;
        public int reputationMax = Integer.MAX_VALUE;
        public double probability = 1.0;
        public String trigger = "on_reputation_gain"; // on_reputation_gain, on_prayer, on_kill
        public List<String> completedFates = new ArrayList<>(); // prerequisite fate IDs
        public String timeOfDay; // "day" or "night"
        public String minStage;  // minimum stage_id
    }

    public static class TaskTemplate {
        public String taskId;
        public String displayName;
        public String description;
        public String progressionTier;
        public String progressionUnlock;
        public List<String> requirements = new ArrayList<>();
        public int reputationReward = 5;
        public int reputationRequired = 0;
        public long cooldownHours = 24;
        public boolean repeatable = false;
        public List<String> rewardCommands = new ArrayList<>();
        public String aiAssignmentContext; // JSON string for complex AI assignment rules
        public AssignmentMode assignmentMode = AssignmentMode.AI;
        public DeterministicAssign deterministicAssign;

        public TaskTemplate() {}

        public TaskTemplate(String taskId, String description, int reputationReward) {
            this.taskId = taskId;
            this.description = description;
            this.reputationReward = reputationReward;
        }
    }
    
    public TaskSystemConfig() {
        // Empty constructor - all tasks should come from JSON configuration
        // No hardcoded tasks to ensure complete JSON-driven configuration
    }

    /**
     * Deity-level assignment behavior for offering tasks/fates during conversation
     */
    public static class TaskAssignmentBehavior {
        /** Probability [0.0-1.0] to offer a fate on each eligible player message */
        public float autoAssignProbability = 0.0f;
        /** Minimum reputation with the deity required to auto-offer a fate */
        public int minReputationForAutoAssign = 0;
        /** Cooldown in hours between auto-offers for the same deity */
        public long cooldownBetweenAssignmentsHours = 24L;
    }
}
