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
    
    public PrayerAIConfig() {
        // Default allowed commands
        allowedCommands.addAll(Arrays.asList("give", "effect", "playsound"));
        allowed_commands.addAll(Arrays.asList("give", "effect", "playsound"));
    }
}
