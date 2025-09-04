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
    
    public static class TaskTemplate {
        public String taskId;
        public String displayName;
        public String description;
        public String progressionTier;
        public List<String> requirements = new ArrayList<>();
        public int reputationReward = 5; // Reputation points awarded on completion
        public int reputationRequired = 0;
        public long cooldownHours = 24;
        public boolean repeatable = false;
        public List<String> rewardCommands = new ArrayList<>();
        public String aiAssignmentContext; // JSON string for complex AI assignment rules
        
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
}
