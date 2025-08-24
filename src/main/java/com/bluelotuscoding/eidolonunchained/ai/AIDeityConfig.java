package com.bluelotuscoding.eidolonunchained.ai;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import com.bluelotuscoding.eidolonunchained.deity.DatapackDeity;
import java.util.*;

/**
 * Complete AI configuration for a deity.
 * Contains all behavioral rules, API settings, and prayer configurations.
 */
public class AIDeityConfig {
    // Basic AI settings
    public ResourceLocation deityId;
    public String aiProvider = "gemini";
    public String model = "gemini-1.5-pro";
    public String personality = "You are a mystical deity in Minecraft.";
    
    // API configuration
    public String apiKeyEnv = "GEMINI_API_KEY";
    public int timeoutSeconds = 30;
    public float temperature = 0.7f;
    public int maxOutputTokens = 1000;
    
    // Safety settings for Gemini
    private final Map<String, String> safetySettings = new HashMap<>();
    
    // Behavioral rules based on player progression
    private final Map<Integer, String> reputationBehaviors = new TreeMap<>();
    private final Map<Integer, String> researchBehaviors = new TreeMap<>();
    private final Map<String, String> personalityShifts = new HashMap<>();
    private final Map<String, String> timeBehaviors = new HashMap<>();
    private final Map<String, String> biomeBehaviors = new HashMap<>();
    
    // Prayer configurations
    public final Map<String, PrayerAIConfig> prayerConfigs = new HashMap<>();
    
    // API settings
    public APISettings apiSettings = new APISettings();
    
    public AIDeityConfig() {
        // Default safety settings
        safetySettings.put("harassment", "BLOCK_MEDIUM_AND_ABOVE");
        safetySettings.put("hate_speech", "BLOCK_MEDIUM_AND_ABOVE");
        safetySettings.put("sexually_explicit", "BLOCK_MEDIUM_AND_ABOVE");
        safetySettings.put("dangerous_content", "BLOCK_MEDIUM_AND_ABOVE");
    }
    
    // Reputation-based behavior
    public void addReputationBehavior(int threshold, String behavior) {
        reputationBehaviors.put(threshold, behavior);
    }
    
    public String getReputationBehavior(double reputation) {
        String behavior = null;
        for (Map.Entry<Integer, String> entry : reputationBehaviors.entrySet()) {
            if (reputation >= entry.getKey()) {
                behavior = entry.getValue();
            } else {
                break;
            }
        }
        return behavior;
    }
    
    // Research count-based behavior
    public void addResearchBehavior(int researchCount, String behavior) {
        researchBehaviors.put(researchCount, behavior);
    }
    
    public String getResearchBehavior(int researchCount) {
        String behavior = null;
        for (Map.Entry<Integer, String> entry : researchBehaviors.entrySet()) {
            if (researchCount >= entry.getKey()) {
                behavior = entry.getValue();
            } else {
                break;
            }
        }
        return behavior;
    }
    
    // Conditional personality shifts
    public void addPersonalityShift(String condition, String personality) {
        personalityShifts.put(condition, personality);
    }
    
    public String getPersonalityShift(String condition) {
        return personalityShifts.get(condition);
    }
    
    // Time-based behaviors
    public void addTimeBehavior(String timeCondition, String behavior) {
        timeBehaviors.put(timeCondition, behavior);
    }
    
    public String getTimeBehavior(String timeCondition) {
        return timeBehaviors.get(timeCondition);
    }
    
    // Biome-specific behaviors
    public void addBiomeBehavior(String biome, String behavior) {
        biomeBehaviors.put(biome, behavior);
    }
    
    public String getBiomeBehavior(String biome) {
        return biomeBehaviors.get(biome);
    }
    
    // Safety settings
    public void addSafetySetting(String category, String threshold) {
        safetySettings.put(category, threshold);
    }
    
    public Map<String, String> getSafetySettings() {
        return new HashMap<>(safetySettings);
    }
    
    // Prayer configurations
    public void addPrayerConfig(PrayerAIConfig config) {
        prayerConfigs.put(config.type, config);
    }
    
    public PrayerAIConfig getPrayerConfig(String prayerType) {
        return prayerConfigs.get(prayerType);
    }
    
    public Set<String> getPrayerTypes() {
        return prayerConfigs.keySet();
    }
    
    /**
     * Build dynamic personality based on player state
     */
    public String buildDynamicPersonality(PlayerContext playerContext) {
        StringBuilder personality = new StringBuilder(this.personality);
        
        // Add reputation-based personality modification
        String repBehavior = getReputationBehavior(playerContext.reputation);
        if (repBehavior != null) {
            personality.append(" ").append(repBehavior);
        }
        
        // Add research-based personality modification
        String researchBehavior = getResearchBehavior(playerContext.researchCount);
        if (researchBehavior != null) {
            personality.append(" ").append(researchBehavior);
        }
        
        // Add time-based behavior
        String timeBehavior = getTimeBehavior(playerContext.timeOfDay);
        if (timeBehavior != null) {
            personality.append(" ").append(timeBehavior);
        }
        
        // Add biome-based behavior
        String biomeBehavior = getBiomeBehavior(playerContext.biome);
        if (biomeBehavior != null) {
            personality.append(" ").append(biomeBehavior);
        }
        
        // Check for special personality shifts
        if (playerContext.progressionLevel.equals("master")) {
            String masterPersonality = getPersonalityShift("master_level");
            if (masterPersonality != null) {
                personality.append(" ").append(masterPersonality);
            }
        }
        
        return personality.toString();
    }
}
