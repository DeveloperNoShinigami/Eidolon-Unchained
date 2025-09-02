package com.bluelotuscoding.eidolonunchained.ai;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import com.bluelotuscoding.eidolonunchained.deity.DatapackDeity;
import com.bluelotuscoding.eidolonunchained.capability.CapabilityHandler;
import java.util.*;

/**
 * Complete AI configuration for a deity.
 * Contains all behavioral rules, API settings, prayer configurations, and patron allegiance rules.
 */
public class AIDeityConfig {
    // Basic AI settings - set automatically by DatapackDeityManager
    public ResourceLocation deityId;
    public String aiProvider = "gemini";
    public String model = "gemini-1.5-pro";
    public String personality = "You are a mystical deity in Minecraft.";
    
    // Patron allegiance configuration
    public PatronConfig patronConfig = new PatronConfig();
    
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
    
    // Enhanced behavior rules
    private final Map<String, String> blessingBehaviors = new HashMap<>();
    private final Map<String, String> curseBehaviors = new HashMap<>(); 
    private final Map<String, String> giftBehaviors = new HashMap<>();
    
    // Prayer configurations
    public final Map<String, PrayerAIConfig> prayerConfigs = new HashMap<>();
    
    // Task system configuration
    public TaskSystemConfig taskConfig = new TaskSystemConfig();
    
    // API settings
    public APISettings apiSettings = new APISettings();
    
    // Ritual integration configuration for patron selection
    public Map<String, Object> ritual_integration = new HashMap<>();
    
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
    
    /**
     * Get all reputation behavior thresholds for external access
     */
    public Map<Integer, String> getReputationBehaviors() {
        return new HashMap<>(reputationBehaviors);
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
    
    // Blessing behaviors
    public void addBlessingBehavior(String condition, String behavior) {
        blessingBehaviors.put(condition, behavior);
    }
    
    public String getBlessingBehavior(String condition) {
        return blessingBehaviors.get(condition);
    }
    
    // Curse behaviors  
    public void addCurseBehavior(String condition, String behavior) {
        curseBehaviors.put(condition, behavior);
    }
    
    public String getCurseBehavior(String condition) {
        return curseBehaviors.get(condition);
    }
    
    // Gift behaviors
    public void addGiftBehavior(String condition, String behavior) {
        giftBehaviors.put(condition, behavior);
    }
    
    public String getGiftBehavior(String condition) {
        return giftBehaviors.get(condition);
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
        if (playerContext.progressionLevel != null && playerContext.progressionLevel.equals("master")) {
            String masterPersonality = getPersonalityShift("master_level");
            if (masterPersonality != null) {
                personality.append(" ").append(masterPersonality);
            }
        }
        
        return personality.toString();
    }
    
    /**
     * Build dynamic personality with patron context awareness
     */
    public String buildDynamicPersonalityWithPatron(PlayerContext playerContext, ServerPlayer player) {
        StringBuilder personality = new StringBuilder(this.personality);
        
        // Check patron status and modify personality accordingly
        try {
            player.level().getCapability(CapabilityHandler.PATRON_DATA_CAPABILITY)
                .ifPresent(patronData -> {
                    ResourceLocation playerPatron = patronData.getPatron(player);
                    PatronRelationship relationship = determinePatronRelationship(playerPatron);
                    
                    // Apply patron-specific personality modifiers
                    String patronModifier = getPatronPersonalityModifier(relationship, patronData.getTitle(player));
                    if (patronModifier != null) {
                        personality.append(" ").append(patronModifier);
                    }
                });
        } catch (Exception e) {
            // Fallback to basic personality if patron system fails
        }
        
        // Add existing dynamic modifiers
        String repBehavior = getReputationBehavior(playerContext.reputation);
        if (repBehavior != null) {
            personality.append(" ").append(repBehavior);
        }
        
        String researchBehavior = getResearchBehavior(playerContext.researchCount);
        if (researchBehavior != null) {
            personality.append(" ").append(researchBehavior);
        }
        
        String timeBehavior = getTimeBehavior(playerContext.timeOfDay);
        if (timeBehavior != null) {
            personality.append(" ").append(timeBehavior);
        }
        
        String biomeBehavior = getBiomeBehavior(playerContext.biome);
        if (biomeBehavior != null) {
            personality.append(" ").append(biomeBehavior);
        }
        
        if (playerContext.progressionLevel != null && playerContext.progressionLevel.equals("master")) {
            String masterPersonality = getPersonalityShift("master_level");
            if (masterPersonality != null) {
                personality.append(" ").append(masterPersonality);
            }
        }
        
        return personality.toString();
    }
    
    /**
     * Determine patron relationship between player and this deity
     */
    public PatronRelationship determinePatronRelationship(ResourceLocation playerPatron) {
        if (playerPatron == null) {
            return PatronRelationship.NO_PATRON;
        }
        
        if (playerPatron.equals(this.deityId)) {
            return PatronRelationship.FOLLOWER;
        }
        
        if (patronConfig.opposingDeities.contains(playerPatron.toString())) {
            return PatronRelationship.ENEMY;
        }
        
        if (patronConfig.alliedDeities.contains(playerPatron.toString())) {
            return PatronRelationship.ALLIED;
        }
        
        return PatronRelationship.NEUTRAL;
    }
    
    /**
     * Get personality modifier based on patron relationship
     */
    private String getPatronPersonalityModifier(PatronRelationship relationship, String playerTitle) {
        switch (relationship) {
            case FOLLOWER:
                return patronConfig.followerPersonalityModifiers.getOrDefault(
                    playerTitle, patronConfig.followerPersonalityModifiers.get("default"));
            case ENEMY:
                return patronConfig.enemyPersonalityModifier;
            case NEUTRAL:
                return patronConfig.neutralPersonalityModifier;
            case NO_PATRON:
                return patronConfig.noPatronPersonalityModifier;
            case ALLIED:
                return patronConfig.alliedPersonalityModifier;
            default:
                return null;
        }
    }
    
    /**
     * Check if this deity can respond to the player based on patron rules
     */
    public boolean canRespondToPlayer(ServerPlayer player) {
        if (!patronConfig.acceptsFollowers) {
            return false;
        }
        
        try {
            return player.level().getCapability(CapabilityHandler.PATRON_DATA_CAPABILITY)
                .map(patronData -> {
                    ResourceLocation playerPatron = patronData.getPatron(player);
                    PatronRelationship relationship = determinePatronRelationship(playerPatron);
                    
                    switch (patronConfig.requiresPatronStatus) {
                        case "follower_only":
                            return relationship == PatronRelationship.FOLLOWER;
                        case "no_enemies":
                            return relationship != PatronRelationship.ENEMY;
                        case "any":
                            return true;
                        default:
                            return true;
                    }
                }).orElse(true);
        } catch (Exception e) {
            return true; // Default to allowing response if patron system fails
        }
    }
    
    /**
     * Patron relationship types
     */
    public enum PatronRelationship {
        FOLLOWER,    // Player serves this deity
        ENEMY,       // Player serves opposing deity
        NEUTRAL,     // Player serves unrelated deity
        ALLIED,      // Player serves allied deity
        NO_PATRON    // Player has no patron
    }
    
    /**
     * Patron configuration data class
     */
    public static class PatronConfig {
        public boolean acceptsFollowers = true;
        public String requiresPatronStatus = "any"; // "follower_only", "no_enemies", "any"
        public List<String> opposingDeities = new ArrayList<>();
        public List<String> alliedDeities = new ArrayList<>();
        public String neutralResponseMode = "normal"; // "normal", "cautious", "cold"
        public String enemyResponseMode = "hostile"; // "hostile", "reject", "mock"
        
        // Personality modifiers
        public Map<String, String> followerPersonalityModifiers = new HashMap<>();
        public String neutralPersonalityModifier = "";
        public String enemyPersonalityModifier = "";
        public String noPatronPersonalityModifier = "";
        public String alliedPersonalityModifier = "";
        
        // Response rules
        public Map<String, Object> conversationRules = new HashMap<>();
    }
}
