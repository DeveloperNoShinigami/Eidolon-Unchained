package com.bluelotuscoding.eidolonunchained.validation;

import com.bluelotuscoding.eidolonunchained.ai.AIDeityManager;
import com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig;
import com.bluelotuscoding.eidolonunchained.ai.PrayerAIConfig;
import com.bluelotuscoding.eidolonunchained.config.APIKeyManager;
import com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager;
import net.minecraft.resources.ResourceLocation;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.*;

/**
 * Comprehensive validation system for AI deity configurations
 * Checks every aspect to ensure the system is properly configured
 */
public class AIConfigValidator {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static class ValidationResult {
        public final boolean isValid;
        public final List<String> errors;
        public final List<String> warnings;
        public final Map<String, Object> details;
        
        public ValidationResult(boolean isValid, List<String> errors, List<String> warnings, Map<String, Object> details) {
            this.isValid = isValid;
            this.errors = errors;
            this.warnings = warnings;
            this.details = details;
        }
    }
    
    /**
     * Perform comprehensive validation of the entire AI system
     */
    public static ValidationResult validateSystem() {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Map<String, Object> details = new HashMap<>();
        
        LOGGER.info("🔍 Starting comprehensive AI system validation...");
        
        // 1. Check API Key Configuration
        validateAPIKeys(errors, warnings, details);
        
        // 2. Check Deity Loading
        validateDeityLoading(errors, warnings, details);
        
        // 3. Check AI Configuration Loading
        validateAIConfigurations(errors, warnings, details);
        
        // 4. Check AI-Deity Linkage
        validateAIDeityLinkage(errors, warnings, details);
        
        // 5. Check Command Configuration
        validateCommandConfiguration(errors, warnings, details);
        
        // 6. Check Token Limits
        validateTokenLimits(errors, warnings, details);
        
        // 7. Check Prayer Configuration
        validatePrayerConfiguration(errors, warnings, details);
        
        boolean isValid = errors.isEmpty();
        
        LOGGER.info("🔍 Validation complete. Valid: {}, Errors: {}, Warnings: {}", 
            isValid, errors.size(), warnings.size());
            
        return new ValidationResult(isValid, errors, warnings, details);
    }
    
    private static void validateAPIKeys(List<String> errors, List<String> warnings, Map<String, Object> details) {
        LOGGER.debug("Validating API keys...");
        
        boolean hasGeminiKey = APIKeyManager.hasAPIKey("gemini");
        details.put("gemini_api_key_present", hasGeminiKey);
        
        if (!hasGeminiKey) {
            errors.add("❌ Gemini API key is missing. Set GEMINI_API_KEY environment variable or use /eidolon-config");
            details.put("gemini_api_key_error", "Missing API key");
        } else {
            String apiKey = APIKeyManager.getAPIKey("gemini");
            if (apiKey.length() < 30) {
                warnings.add("⚠️ Gemini API key seems unusually short. Verify it's correct.");
            }
            
            if (!apiKey.startsWith("AIza")) {
                warnings.add("⚠️ Gemini API key doesn't start with 'AIza' - may be invalid format.");
            }
            
            details.put("gemini_api_key_length", apiKey.length());
            details.put("gemini_api_key_format_ok", apiKey.startsWith("AIza"));
        }
    }
    
    private static void validateDeityLoading(List<String> errors, List<String> warnings, Map<String, Object> details) {
        LOGGER.debug("Validating deity loading...");
        
        // Set<ResourceLocation> deities = DatapackDeityManager.getAllDeityIds();
        Set<ResourceLocation> deities = DatapackDeityManager.getAllDeities().keySet();
        details.put("loaded_deities_count", deities.size());
        details.put("loaded_deities", deities);
        
        if (deities.isEmpty()) {
            errors.add("❌ No deities loaded. Check datapack deity definitions.");
        } else if (deities.size() < 3) {
            warnings.add("⚠️ Only " + deities.size() + " deities loaded. Expected at least 3 (light, dark, nature).");
        }
        
        // Check for expected deities
        boolean hasLight = deities.contains(new ResourceLocation("eidolonunchained", "light_deity"));
        boolean hasDark = deities.contains(new ResourceLocation("eidolonunchained", "dark_deity"));
        boolean hasNature = deities.contains(new ResourceLocation("eidolonunchained", "nature_deity"));
        
        details.put("has_light_deity", hasLight);
        details.put("has_dark_deity", hasDark);
        details.put("has_nature_deity", hasNature);
        
        if (!hasLight) warnings.add("⚠️ Light deity not found");
        if (!hasDark) warnings.add("⚠️ Dark deity not found");
        if (!hasNature) warnings.add("⚠️ Nature deity not found");
    }
    
    private static void validateAIConfigurations(List<String> errors, List<String> warnings, Map<String, Object> details) {
        LOGGER.debug("Validating AI configurations...");
        
        AIDeityManager manager = AIDeityManager.getInstance();
        // Map<ResourceLocation, AIDeityConfig> configs = manager.getAllConfigs();
        Collection<AIDeityConfig> configs = manager.getAllConfigs();
        
        details.put("ai_configs_count", configs.size());
        // details.put("ai_configs", configs.keySet());
        details.put("ai_configs", configs.stream().map(c -> c.deity_id).collect(java.util.stream.Collectors.toSet()));
        
        if (configs.isEmpty()) {
            errors.add("❌ No AI configurations loaded. Check ai_deities/*.json files.");
            return;
        }
        
        // for (Map.Entry<ResourceLocation, AIDeityConfig> entry : configs.entrySet()) {
        for (AIDeityConfig config : configs) {
            // ResourceLocation deityId = entry.getKey();
            ResourceLocation deityId = config.deity_id;
            // AIDeityConfig config = entry.getValue();
            
            validateSingleAIConfig(deityId, config, errors, warnings, details);
        }
    }
    
    private static void validateSingleAIConfig(ResourceLocation deityId, AIDeityConfig config, 
                                              List<String> errors, List<String> warnings, Map<String, Object> details) {
        String prefix = "ai_config_" + deityId.getPath() + "_";
        
        // Check basic config
        if (config.personality == null || config.personality.trim().isEmpty()) {
            errors.add("❌ " + deityId + ": Missing personality configuration");
        }
        
        if (config.api_settings == null) {
            errors.add("❌ " + prefix + "API settings are missing");
            return;
        }
        
        if (config.api_settings.generationConfig == null) {
            errors.add("❌ " + prefix + "API generation config is missing");
        } else {
            if (config.api_settings.generationConfig.max_output_tokens > 500) {
                warnings.add("⚠️ " + prefix + "High token limit (" + 
                    config.api_settings.generationConfig.max_output_tokens + ") may cause issues");
            }
            details.put(prefix + "max_tokens", config.api_settings.generationConfig.max_output_tokens);
        }
        
        // Validate prayer configurations
        if (config.prayer_configs == null || config.prayer_configs.isEmpty()) {
            errors.add("❌ " + prefix + "No prayer configurations found");
        } else {
            details.put(prefix + "prayer_configs_count", config.prayer_configs.size());
            
            for (Map.Entry<String, PrayerAIConfig> prayerEntry : config.prayer_configs.entrySet()) {
                String prayerType = prayerEntry.getKey();
                PrayerAIConfig prayerConfig = prayerEntry.getValue();
                validatePrayerConfig(deityId, prayerType, prayerConfig, errors, warnings);
            }
        }
    }
    
    private static void validatePrayerConfig(ResourceLocation deityId, String prayerType, PrayerAIConfig prayerConfig,
                                           List<String> errors, List<String> warnings) {
        String configName = deityId + ":" + prayerType;
        
        if (prayerConfig.base_prompt == null || prayerConfig.base_prompt.trim().isEmpty()) {
            errors.add("❌ " + configName + ": Missing base prompt");
        }
        
        if (prayerConfig.auto_judge_commands) {
            if (prayerConfig.judgment_config == null) {
                errors.add("❌ " + configName + ": Auto-judge enabled but no judgment config");
            } else {
                if (prayerConfig.judgment_config.blessingCommands == null || prayerConfig.judgment_config.blessingCommands.isEmpty()) {
                    warnings.add("⚠️ " + configName + ": No blessing commands defined");
                }
                if (prayerConfig.judgment_config.curseCommands == null || prayerConfig.judgment_config.curseCommands.isEmpty()) {
                    warnings.add("⚠️ " + configName + ": No curse commands defined");
                }
            }
        }
        
        if (prayerConfig.allowed_commands != null && !prayerConfig.allowed_commands.isEmpty()) {
            for (String command : prayerConfig.allowed_commands) {
                if (!isValidCommand(command)) {
                    warnings.add("⚠️ " + configName + ": Potentially unsafe command: " + command);
                }
            }
        }
    }
    
    private static void validateAIDeityLinkage(List<String> errors, List<String> warnings, Map<String, Object> details) {
        LOGGER.debug("Validating AI-deity linkage...");
        
        // Set<ResourceLocation> deities = DatapackDeityManager.getAllDeityIds();
        Set<ResourceLocation> deities = DatapackDeityManager.getAllDeities().keySet();
        AIDeityManager manager = AIDeityManager.getInstance();
        // Map<ResourceLocation, AIDeityConfig> configs = manager.getAllConfigs();
        Collection<AIDeityConfig> configs = manager.getAllConfigs();
        
        int linkedCount = 0;
        for (ResourceLocation deityId : deities) {
            // if (configs.containsKey(deityId)) {
            if (configs.stream().anyMatch(c -> c.deity_id.equals(deityId))) {
                linkedCount++;
            } else {
                warnings.add("⚠️ Deity " + deityId + " has no AI configuration");
            }
        }
        
        details.put("linked_ai_configs", linkedCount);
        details.put("unlinked_deities", deities.size() - linkedCount);
        
        if (linkedCount == 0) {
            errors.add("❌ No AI configurations linked to deities");
        }
    }
    
    private static void validateCommandConfiguration(List<String> errors, List<String> warnings, Map<String, Object> details) {
        LOGGER.debug("Validating command configuration...");
        
        // This is already covered in prayer config validation
        details.put("command_validation", "Covered in prayer configuration validation");
    }
    
    private static void validateTokenLimits(List<String> errors, List<String> warnings, Map<String, Object> details) {
        LOGGER.debug("Validating token limits...");
        
        AIDeityManager manager = AIDeityManager.getInstance();
        // Map<ResourceLocation, AIDeityConfig> configs = manager.getAllConfigs();
        Collection<AIDeityConfig> configs = manager.getAllConfigs();
        
        int highTokenCount = 0;
        // for (Map.Entry<ResourceLocation, AIDeityConfig> entry : configs.entrySet()) {
        for (AIDeityConfig config : configs) {
            // AIDeityConfig config = entry.getValue();
            if (config.api_settings != null && config.api_settings.generationConfig != null) {
                int tokens = config.api_settings.generationConfig.max_output_tokens;
                if (tokens > 400) {
                    highTokenCount++;
                    // warnings.add("⚠️ " + entry.getKey() + ": High token limit (" + tokens + ") may cause API errors");
                    warnings.add("⚠️ " + config.deity_id + ": High token limit (" + tokens + ") may cause API errors");
                }
            }
        }
        
        details.put("high_token_configs", highTokenCount);
    }
    
    private static void validatePrayerConfiguration(List<String> errors, List<String> warnings, Map<String, Object> details) {
        LOGGER.debug("Validating prayer configuration...");
        
        // This is covered in the AI configuration validation
        details.put("prayer_validation", "Covered in AI configuration validation");
    }
    
    private static boolean isValidCommand(String command) {
        // Basic command safety check
        String[] dangerousCommands = {"op", "deop", "ban", "kick", "stop", "whitelist", "reload"};
        String lowerCommand = command.toLowerCase();
        
        for (String dangerous : dangerousCommands) {
            if (lowerCommand.contains(dangerous)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Print validation results in a formatted way
     */
    public static void printValidationResults(ValidationResult result) {
        LOGGER.info("🔍 AI System Validation Results:");
        LOGGER.info("📊 Overall Status: {}", result.isValid ? "✅ VALID" : "❌ INVALID");
        
        if (!result.errors.isEmpty()) {
            LOGGER.error("🚨 ERRORS ({}):", result.errors.size());
            for (String error : result.errors) {
                LOGGER.error("   {}", error);
            }
        }
        
        if (!result.warnings.isEmpty()) {
            LOGGER.warn("⚠️  WARNINGS ({}):", result.warnings.size());
            for (String warning : result.warnings) {
                LOGGER.warn("   {}", warning);
            }
        }
        
        if (result.isValid) {
            LOGGER.info("✅ AI system is properly configured and ready to use!");
        } else {
            LOGGER.error("❌ AI system has configuration issues that need to be resolved.");
        }
    }
}
