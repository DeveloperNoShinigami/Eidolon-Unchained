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
        public String description;
        public List<String> requirements = new ArrayList<>();
        public int favorReward = 5;
        public int reputationRequired = 0;
        public long cooldownHours = 24;
        public List<String> rewardCommands = new ArrayList<>();
        
        public TaskTemplate() {}
        
        public TaskTemplate(String taskId, String description, int favorReward) {
            this.taskId = taskId;
            this.description = description;
            this.favorReward = favorReward;
        }
    }
    
    public TaskSystemConfig() {
        // Add default tasks
        TaskTemplate gatherHerbs = new TaskTemplate("gather_herbs", "Collect 16 mystical herbs from the forest", 3);
        gatherHerbs.requirements.add("item:minecraft:wheat:16");
        gatherHerbs.rewardCommands.add("give {player} eidolon:lesser_soul_gem 1");
        availableTasks.add(gatherHerbs);
        
        TaskTemplate performRituals = new TaskTemplate("perform_rituals", "Successfully perform 3 different chants", 5);
        performRituals.reputationRequired = 10;
        performRituals.rewardCommands.add("give {player} minecraft:experience_bottle 5");
        availableTasks.add(performRituals);
        
        TaskTemplate exploreNether = new TaskTemplate("explore_nether", "Visit the Nether and gather soul sand", 8);
        exploreNether.requirements.add("item:minecraft:soul_sand:8");
        exploreNether.reputationRequired = 25;
        exploreNether.rewardCommands.add("give {player} eidolon:soul_shard 2");
        availableTasks.add(exploreNether);
    }
}
