package com.bluelotuscoding.eidolonunchained.ai;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import com.bluelotuscoding.eidolonunchained.deity.DatapackDeity;
import com.bluelotuscoding.eidolonunchained.capability.CapabilityHandler;
import com.google.gson.JsonObject;
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
    public List<String> mod_context_ids = new ArrayList<>(); // 🔥 NEW: Mod namespaces for dynamic registry context
    
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

    // Natural-language triggers (JSON-driven)
    public List<NLTrigger> naturalLanguageTriggers = new ArrayList<>();

    // TTS (Text-to-Speech) configuration - populated from JSON only
    public TTSConfig tts_config = new TTSConfig();

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

        // Team/Faction System
        public boolean assignsPlayersToTeam = true; // Whether this deity creates faction teams
        public String teamName = ""; // Name of the faction team (e.g., "Shadows", "Nature's Guard")
        public String teamColor = ""; // Team color for display (e.g., "dark_purple", "green")
        public boolean friendlyFire = false; // Whether team members can damage each other (default: false)

        // Supported Entities
        public List<String> supportedMobIds = new ArrayList<>(); // Entities this deity supports/controls
    }

    /**
     * JSON-driven natural language trigger definition.
     */
    public static class NLTrigger {
        public String id;
        public List<String> contains = new ArrayList<>(); // simple keyword list (case-insensitive)
        public List<String> regex = new ArrayList<>(); // optional regex patterns
        public int minReputation = 0; // optional
        public long cooldownSeconds = 0; // optional
        public String action; // e.g., "offer_fate", "run_commands", "send_message"
        public JsonObject params; // action-specific parameters
    }

    /**
     * TTS (Text-to-Speech) configuration for deity voice synthesis
     */
    public static class TTSConfig {
        // Primary voice configuration
        public String voice_id = "auto"; // Voice ID or "auto" for deity-appropriate voice
        public String backup_voice = "neutral-1"; // Fallback voice if primary fails

        // Voice characteristics
        public float pitch = 1.0f; // Voice pitch adjustment (0.5-2.0)
        public float speed = 1.0f; // Speech speed (0.5-2.0)
        public float volume = 1.0f; // Volume level (0.0-2.0)

        // Advanced voice settings
        public String emotion = "neutral"; // Voice emotion: neutral, happy, sad, angry, calm, etc.
        public String accent = "default"; // Voice accent if supported
        public int emphasis_level = 0; // Speech emphasis (0-2): 0=normal, 1=moderate, 2=strong

        // TTS behavior configuration
        public boolean enabled = true; // Whether TTS is enabled for this deity
        public boolean allow_player_override = true; // Allow players to change voice settings
        public String funding_preference = "player_first"; // "player_first", "server_only", "player_only"

    // Optional per-deity overrides for provider expectations
    // If set in JSON, these take precedence over global config defaults
    public String audio_format = null;     // e.g., mp3, opus, flac, wav, pcm
    public String voice_gender = null;     // male | female | other
    public String voice_language = null;   // e.g., en_US, ja_JP

        // Custom voice files (for modpack creators)
        public String custom_voice_file = ""; // Path to custom voice file (if supported)
        public Map<String, String> voice_aliases = new HashMap<>(); // Custom voice name mappings

        // Context-aware voice changes
        public Map<String, String> reputation_voices = new HashMap<>(); // Different voices by reputation
        public Map<String, String> time_voices = new HashMap<>(); // Different voices by time of day
        public Map<String, String> biome_voices = new HashMap<>(); // Different voices by biome

        // Voice generation parameters (for advanced TTS systems)
        public Map<String, Object> advanced_params = new HashMap<>(); // Provider-specific parameters

        public TTSConfig() {
            // Initialize with safe defaults
            org.apache.logging.log4j.Logger logger = org.apache.logging.log4j.LogManager.getLogger();
            logger.info("🎵 TTSConfig constructor called");
            logger.info("🎵 TTSConfig initialized with empty maps: reputation_voices={}, biome_voices={}, time_voices={}", 
                reputation_voices, biome_voices, time_voices);
        }

        /**
         * Get the appropriate voice ID for current context
         */
        public String getVoiceForContext(ServerPlayer player, String currentBiome, int reputation) {
            org.apache.logging.log4j.Logger logger = org.apache.logging.log4j.LogManager.getLogger();
            logger.info("🎵 getVoiceForContext: currentBiome={}, reputation={}", currentBiome, reputation);
            logger.info("🎵 voice_id={}, reputation_voices={}, biome_voices={}, time_voices={}", 
                voice_id, reputation_voices, biome_voices, time_voices);
            
            // Check reputation-based voices first
            if (!reputation_voices.isEmpty()) {
                logger.info("🎵 checking reputation voices: {}", reputation_voices);
                for (Map.Entry<String, String> entry : reputation_voices.entrySet()) {
                    try {
                        int threshold = Integer.parseInt(entry.getKey());
                        logger.info("🎵 checking threshold {} against reputation {}", threshold, reputation);
                        if (reputation >= threshold) {
                            logger.info("🎵 reputation voice match: {} for threshold {}", entry.getValue(), threshold);
                            return entry.getValue();
                        }
                    } catch (NumberFormatException ignored) {
                        logger.warn("🎵 invalid reputation threshold: {}", entry.getKey());
                    }
                }
            }

            // Check biome-based voices
            if (currentBiome != null && biome_voices.containsKey(currentBiome)) {
                logger.info("🎵 biome voice match: {} for biome {}", biome_voices.get(currentBiome), currentBiome);
                return biome_voices.get(currentBiome);
            }

            // Check time-based voices
            if (!time_voices.isEmpty() && player != null) {
                long timeOfDay = player.level().getDayTime() % 24000;
                String timeContext;
                if (timeOfDay >= 0 && timeOfDay < 6000) {
                    timeContext = "day";
                } else if (timeOfDay >= 6000 && timeOfDay < 12000) {
                    timeContext = "afternoon";
                } else if (timeOfDay >= 12000 && timeOfDay < 18000) {
                    timeContext = "evening";
                } else {
                    timeContext = "night";
                }
                
                logger.info("🎵 time context: {} (timeOfDay={})", timeContext, timeOfDay);

                if (time_voices.containsKey(timeContext)) {
                    logger.info("🎵 time voice match: {} for {}", time_voices.get(timeContext), timeContext);
                    return time_voices.get(timeContext);
                }
            }

            // Return primary voice or backup
            String result = voice_id.equals("auto") ? null : voice_id;
            logger.info("🎵 returning primary/fallback voice: {}", result);
            return result;
        }

        /**
         * Apply voice aliases for custom voice names
         */
        public String resolveVoiceAlias(String voiceId) {
            org.apache.logging.log4j.Logger logger = org.apache.logging.log4j.LogManager.getLogger();
            logger.info("🎵 resolveVoiceAlias input: {}", voiceId);
            logger.info("🎵 voice_aliases: {}", voice_aliases);
            
            // Deity-local alias first
            String local = voice_aliases.getOrDefault(voiceId, null);
            if (local != null) {
                logger.info("🎵 local alias resolved: {} -> {}", voiceId, local);
                return local;
            }
            
            // Global registry fallback
            try {
                String global = com.bluelotuscoding.eidolonunchained.integration.tts.TTSVoiceRegistry
                    .getInstance()
                    .resolve(voiceId);
                logger.info("🎵 global alias resolved: {} -> {}", voiceId, global);
                return global;
            } catch (Exception e) {
                logger.warn("🎵 global alias failed: {}", e.getMessage());
            }
            
            logger.info("🎵 no alias, returning original: {}", voiceId);
            return voiceId;
        }
    }
}
