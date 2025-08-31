package com.bluelotuscoding.eidolonunchained.ai;

import java.util.*;

/**
 * Configuration for a specific prayer type
 */
public class PrayerAIConfig {
    public String type;
    public String basePrompt = "Player {player} is praying to you.";
    public String base_prompt = "Player {player} is praying to you."; // Alternative field name for compatibility
    public int maxCommands = 2;
    public int max_commands = 2; // Alternative field name for compatibility
    public int cooldownMinutes = 30;
    public int reputationRequired = 0;
    public List<String> allowedCommands = new ArrayList<>();
    public List<String> allowed_commands = new ArrayList<>(); // Alternative field name for compatibility
    public boolean autoJudgeCommands = false;
    public JudgmentConfig judgmentConfig = new JudgmentConfig();
    
    // Enhanced configuration options
    public List<String> additionalPrompts = new ArrayList<>(); // Developer guidance prompts
    public List<String> additional_prompts = new ArrayList<>(); // Alternative field name for compatibility
    public List<String> referenceCommands = new ArrayList<>(); // AI reference command templates
    public List<String> reference_commands = new ArrayList<>(); // Alternative field name for compatibility
    
    public PrayerAIConfig() {
        // Default allowed commands
        allowedCommands.addAll(Arrays.asList("give", "effect", "playsound"));
        allowed_commands.addAll(Arrays.asList("give", "effect", "playsound"));
        
        // Default reference commands - AI can use these as templates
        referenceCommands.addAll(Arrays.asList(
            "give {player} minecraft:diamond 1",
            "effect give {player} minecraft:regeneration 30 1",
            "give {player} eidolon:soul_shard 3",
            "effect give {player} minecraft:night_vision 600 0",
            "give {player} minecraft:golden_apple 1"
        ));
        reference_commands.addAll(referenceCommands);
    }
}
