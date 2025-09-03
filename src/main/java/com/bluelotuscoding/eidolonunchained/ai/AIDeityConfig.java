package com.bluelotuscoding.eidolonunchained.ai;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import com.bluelotuscoding.eidolonunchained.deity.DatapackDeity;
import com.bluelotuscoding.eidolonunchained.capability.CapabilityHandler;
import java.util.*;

/**
 * Complete AI configuration for a deity.
 * All values must be provided via JSON or commands - no defaults.
 */
public class AIDeityConfig {
    // Basic AI settings - must be set by JSON loading or commands
    public ResourceLocation deity_id;
    public String ai_provider; // No default - must come from JSON/command
    public String model; // No default - must come from JSON/command  
    public String personality; // No default - must come from JSON/command
    
    // Patron allegiance configuration
    public PatronConfig patron_config = new PatronConfig();
    
    // API configuration - no defaults, must be configured
    public String api_key_env; // No default - must come from JSON/command
    public int timeout_seconds; // No default - must come from JSON/command
    public float temperature; // No default - must come from JSON/command
    public int max_output_tokens; // No default - must come from JSON/command
    public String item_context_id; // Mob ID context for AI item commands - must come from JSON/command
    
    // Safety settings for Gemini - must be configured via JSON
    private final Map<String, String> safety_settings = new HashMap<>();
    
    // Behavioral rules based on player progression - populated from JSON only
    private final Map<Integer, String> reputation_behaviors = new TreeMap<>();
    private final Map<Integer, String> research_behaviors = new TreeMap<>();
    private final Map<String, String> personality_shifts = new HashMap<>();
    private final Map<String, String> time_behaviors = new HashMap<>();
    private final Map<String, String> biome_behaviors = new HashMap<>();
    
    // Enhanced behavior rules - populated from JSON only
    private final Map<String, String> blessing_behaviors = new HashMap<>();
    private final Map<String, String> curse_behaviors = new HashMap<>(); 
    private final Map<String, String> gift_behaviors = new HashMap<>();
    
    // Prayer configurations - populated from JSON only
    public final Map<String, PrayerAIConfig> prayer_configs = new HashMap<>();
    
    // Task system configuration - must be configured via JSON
    public TaskSystemConfig task_config = new TaskSystemConfig();
    
    // API settings - must be configured via JSON
    public APISettings api_settings = new APISettings();
    
    // Ritual integration configuration for patron selection - populated from JSON only
    public Map<String, Object> ritual_integration = new HashMap<>();
    
    public AIDeityConfig() {
        // No defaults - safety settings must come from JSON configuration
        // Initialize empty collections to prevent null pointer exceptions
    }
    
    // Reputation-based behavior
    public void addReputationBehavior(int threshold, String behavior) {
        reputation_behaviors.put(threshold, behavior);
    }
    
    public String getReputationBehavior(double reputation) {
        String behavior = null;
        for (Map.Entry<Integer, String> entry : reputation_behaviors.entrySet()) {
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
        return new HashMap<>(reputation_behaviors);
    }
    
    // Research count-based behavior
    public void addResearchBehavior(int researchCount, String behavior) {
        research_behaviors.put(researchCount, behavior);
    }
    
    public String getResearchBehavior(int researchCount) {
        String behavior = null;
        for (Map.Entry<Integer, String> entry : research_behaviors.entrySet()) {
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
        personality_shifts.put(condition, personality);
    }
    
    public String getPersonalityShift(String condition) {
        return personality_shifts.get(condition);
    }
    
    // Time-based behaviors
    public void addTimeBehavior(String timeCondition, String behavior) {
        time_behaviors.put(timeCondition, behavior);
    }
    
    public String getTimeBehavior(String timeCondition) {
        return time_behaviors.get(timeCondition);
    }
    
    // Biome-specific behaviors
    public void addBiomeBehavior(String biome, String behavior) {
        biome_behaviors.put(biome, behavior);
    }
    
    public String getBiomeBehavior(String biome) {
        return biome_behaviors.get(biome);
    }
    
    // Blessing behaviors
    public void addBlessingBehavior(String condition, String behavior) {
        blessing_behaviors.put(condition, behavior);
    }
    
    public String getBlessingBehavior(String condition) {
        return blessing_behaviors.get(condition);
    }
    
    // Curse behaviors  
    public void addCurseBehavior(String condition, String behavior) {
        curse_behaviors.put(condition, behavior);
    }
    
    public String getCurseBehavior(String condition) {
        return curse_behaviors.get(condition);
    }
    
    // Gift behaviors
    public void addGiftBehavior(String condition, String behavior) {
        gift_behaviors.put(condition, behavior);
    }
    
    public String getGiftBehavior(String condition) {
        return gift_behaviors.get(condition);
    }
    
    // Safety settings
    public void addSafetySetting(String category, String threshold) {
        safety_settings.put(category, threshold);
    }
    
    public Map<String, String> getSafetySettings() {
        return new HashMap<>(safety_settings);
    }
    
    // Prayer configurations
    public void addPrayerConfig(PrayerAIConfig config) {
        prayer_configs.put(config.type, config);
    }
    
    public PrayerAIConfig getPrayerConfig(String prayerType) {
        return prayer_configs.get(prayerType);
    }
    
    public Set<String> getPrayerTypes() {
        return prayer_configs.keySet();
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
        
        if (playerPatron.equals(this.deity_id)) {
            return PatronRelationship.FOLLOWER;
        }
        
        if (patron_config.opposingDeities.contains(playerPatron.toString())) {
            return PatronRelationship.ENEMY;
        }
        
        if (patron_config.alliedDeities.contains(playerPatron.toString())) {
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
                return patron_config.followerPersonalityModifiers.getOrDefault(
                    playerTitle, patron_config.followerPersonalityModifiers.get("default"));
            case ENEMY:
                return patron_config.enemyPersonalityModifier;
            case NEUTRAL:
                return patron_config.neutralPersonalityModifier;
            case NO_PATRON:
                return patron_config.noPatronPersonalityModifier;
            case ALLIED:
                return patron_config.alliedPersonalityModifier;
            default:
                return null;
        }
    }
    
    /**
     * Check if this deity can respond to the player based on patron rules
     */
    public boolean canRespondToPlayer(ServerPlayer player) {
        if (!patron_config.acceptsFollowers) {
            return false;
        }
        
        try {
            return player.level().getCapability(CapabilityHandler.PATRON_DATA_CAPABILITY)
                .map(patronData -> {
                    ResourceLocation playerPatron = patronData.getPatron(player);
                    PatronRelationship relationship = determinePatronRelationship(playerPatron);
                    
                    switch (patron_config.requiresPatronStatus) {
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
