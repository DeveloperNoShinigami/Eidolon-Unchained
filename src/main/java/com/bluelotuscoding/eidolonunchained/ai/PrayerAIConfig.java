package com.bluelotuscoding.eidolonunchained.ai;

import java.util.*;

/**
 * Configuration for a specific prayer type
 */
public class PrayerAIConfig {
    public String type;
    public String basePrompt = "Player {player} seeks divine guidance.";
    public String base_prompt = "Player {player} seeks divine guidance."; // Alternative field name for compatibility
    public int maxCommands = 2; // CONFIGURABLE: Let JSON datapacks set this value
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
        // Default allowed commands - keep simple and focused
        allowedCommands.addAll(Arrays.asList("give", "effect", "playsound"));
        allowed_commands.addAll(Arrays.asList("give", "effect", "playsound"));
        
        // Enhanced reference commands - more divine and contextual
        referenceCommands.addAll(Arrays.asList(
            // Divine gifts for those who prove worthy
            "give {player} minecraft:golden_apple 1", // Divine sustenance
            "give {player} eidolon:soul_shard 2", // Spiritual power
            "give {player} minecraft:diamond 1", // Blessed gem
            "give {player} minecraft:enchanted_golden_apple 1", // Greatest blessing
            // Divine effects for protection and aid  
            "effect give {player} minecraft:regeneration 120 1", // Divine healing
            "effect give {player} minecraft:resistance 300 0", // Divine protection
            "effect give {player} minecraft:night_vision 1200 0", // Divine sight
            "effect give {player} minecraft:water_breathing 600 0", // Divine breath
            "effect give {player} minecraft:fire_resistance 600 0", // Divine immunity
            // Divine sounds for atmosphere
            "playsound minecraft:block.bell.use master {player} ~ ~ ~ 1.0 0.8" // Divine chime
        ));
        reference_commands.addAll(referenceCommands);
    }
}
