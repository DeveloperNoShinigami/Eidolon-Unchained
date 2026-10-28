package com.bluelotuscoding.eidolonunchained.ai;

import java.util.*;

/**
 * Configuration for a specific prayer type
 * All values must be provided via JSON - no defaults
 */
public class PrayerAIConfig {
    public String type;
    public String base_prompt; // No default - must come from JSON
    public int max_commands; // No default - must come from JSON
    public int cooldown_seconds; // No default - must come from JSON
    public int reputation_required; // No default - must come from JSON
    public List<String> allowed_commands = new ArrayList<>(); // Initialize empty, populate from JSON
    public boolean auto_judge_commands; // No default - must come from JSON
    public JudgmentConfig judgment_config = new JudgmentConfig();
    
    // Enhanced configuration options - no defaults
    public List<String> additional_prompts = new ArrayList<>(); // Initialize empty, populate from JSON
    public List<String> reference_commands = new ArrayList<>(); // Initialize empty, populate from JSON

    // Deterministic fallback when AI is unavailable
    public List<String> fallback_messages = new ArrayList<>(); // Random message picked when AI not available
    public List<String> fallback_commands = new ArrayList<>(); // Commands to execute as fallback

    public PrayerAIConfig() {
        // No defaults - all configuration must come from JSON
        // Initialize collections to prevent null pointer exceptions
    }

    public boolean hasFallback() {
        return !fallback_messages.isEmpty() || !fallback_commands.isEmpty();
    }
}
