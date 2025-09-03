package com.bluelotuscoding.eidolonunchained.ai;

import com.bluelotuscoding.eidolonunchained.chat.ConversationHistoryManager;
import net.minecraft.resources.ResourceLocation;

/**
 * Utility class for getting effective AI configuration values.
 * Implements priority system: Server Override > JSON Default > System Default
 */
public class EffectiveAIConfig {
    
    /**
     * Get the effective cooldown for a prayer type.
     * Server settings override JSON values when configured.
     * 
     * @param deityId The deity's resource location
     * @param prayerType Type of prayer ("conversation", "blessing", "knowledge", etc.)
     * @param jsonConfig The original JSON configuration (may be null)
     * @return Effective cooldown in minutes
     */
    public static int getCooldown(ResourceLocation deityId, String prayerType, PrayerAIConfig jsonConfig) {
        return ConversationHistoryManager.getEffectiveCooldownStatic(deityId, prayerType, jsonConfig);
    }
    
    /**
     * Get the effective maximum number of commands for a prayer type.
     * Server settings override JSON values, global server limit acts as ceiling.
     * 
     * @param deityId The deity's resource location
     * @param prayerType Type of prayer ("conversation", "blessing", "knowledge", etc.)
     * @param jsonConfig The original JSON configuration (may be null)
     * @return Effective maximum commands
     */
    public static int getMaxCommands(ResourceLocation deityId, String prayerType, PrayerAIConfig jsonConfig) {
        return ConversationHistoryManager.getEffectiveMaxCommandsStatic(deityId, prayerType, jsonConfig);
    }
    
    /**
     * Get the effective reputation requirement for a prayer type.
     * Server settings override JSON values when configured.
     * 
     * @param deityId The deity's resource location
     * @param prayerType Type of prayer ("conversation", "blessing", "knowledge", etc.)
     * @param jsonConfig The original JSON configuration (may be null)
     * @return Effective reputation requirement
     */
    public static int getReputationRequired(ResourceLocation deityId, String prayerType, PrayerAIConfig jsonConfig) {
        return ConversationHistoryManager.getEffectiveReputationRequiredStatic(deityId, prayerType, jsonConfig);
    }
    
    /**
     * Get the effective auto-judge commands setting.
     * Server settings override JSON values when configured.
     * 
     * @param deityId The deity's resource location
     * @param prayerType Type of prayer ("conversation", "blessing", "knowledge", etc.)
     * @param jsonConfig The original JSON configuration (may be null)
     * @return Effective auto-judge setting
     */
    public static boolean getAutoJudgeCommands(ResourceLocation deityId, String prayerType, PrayerAIConfig jsonConfig) {
        return ConversationHistoryManager.getEffectiveAutoJudgeCommandsStatic(deityId, prayerType, jsonConfig);
    }
    
    /**
     * Get the effective judgment threshold for blessing/curse decisions.
     * Server settings override JSON values when configured.
     * 
     * @param deityId The deity's resource location
     * @param thresholdType Type of threshold ("blessing", "curse", "proactiveAssistance", "emergencyAssistance")
     * @param jsonJudgment The original JSON judgment configuration (may be null)
     * @return Effective threshold value
     */
    public static int getJudgmentThreshold(ResourceLocation deityId, String thresholdType, JudgmentConfig jsonJudgment) {
        return ConversationHistoryManager.getEffectiveJudgmentThresholdStatic(deityId, thresholdType, jsonJudgment);
    }
    
    /**
     * Get the effective AI provider for a deity.
     * Server settings override JSON values when configured.
     * 
     * @param deityId The deity's resource location
     * @param jsonProvider The original JSON provider setting (may be null)
     * @return Effective AI provider
     */
    public static String getAIProvider(ResourceLocation deityId, String jsonProvider) {
        ConversationHistoryManager manager = ConversationHistoryManager.get();
        if (manager != null) {
            String serverProvider = manager.getDeityAIProvider(deityId);
            if (serverProvider != null && !serverProvider.isEmpty()) {
                return serverProvider;
            }
        }
        return jsonProvider != null ? jsonProvider : "gemini";
    }
    
    /**
     * Get the effective AI model for a deity.
     * Server settings override JSON values when configured.
     * 
     * @param deityId The deity's resource location
     * @param jsonModel The original JSON model setting (may be null)
     * @return Effective AI model
     */
    public static String getAIModel(ResourceLocation deityId, String jsonModel) {
        ConversationHistoryManager manager = ConversationHistoryManager.get();
        if (manager != null) {
            String serverModel = manager.getDeityAIModel(deityId);
            if (serverModel != null && !serverModel.isEmpty()) {
                return serverModel;
            }
        }
        return jsonModel != null ? jsonModel : "gemini-1.5-pro";
    }
    
    /**
     * Create a configuration summary showing which values come from server vs JSON.
     * Useful for debugging and admin information.
     * 
     * @param deityId The deity to check
     * @param prayerType The prayer type to check
     * @param jsonConfig The JSON configuration
     * @return Summary string showing effective values and their sources
     */
    public static String getConfigurationSummary(ResourceLocation deityId, String prayerType, PrayerAIConfig jsonConfig) {
        StringBuilder summary = new StringBuilder();
        summary.append("=== Configuration Summary for ").append(deityId).append(" (").append(prayerType).append(") ===\n");
        
        ConversationHistoryManager manager = ConversationHistoryManager.get();
        boolean hasServerOverrides = ConversationHistoryManager.hasServerOverridesStatic(deityId);
        
        // Cooldown
        int effectiveCooldown = getCooldown(deityId, prayerType, jsonConfig);
        int jsonCooldown = jsonConfig != null ? jsonConfig.cooldown_minutes : 30;
        summary.append("Cooldown: ").append(effectiveCooldown).append(" minutes ");
        if (hasServerOverrides && effectiveCooldown != jsonCooldown) {
            summary.append("(Server Override - JSON: ").append(jsonCooldown).append(")");
        } else {
            summary.append("(JSON Default)");
        }
        summary.append("\n");
        
        // Max Commands
        int effectiveMaxCommands = getMaxCommands(deityId, prayerType, jsonConfig);
        int jsonMaxCommands = jsonConfig != null ? jsonConfig.max_commands : 3;
        summary.append("Max Commands: ").append(effectiveMaxCommands).append(" ");
        if (hasServerOverrides && effectiveMaxCommands != jsonMaxCommands) {
            summary.append("(Server Override - JSON: ").append(jsonMaxCommands).append(")");
        } else {
            summary.append("(JSON Default)");
        }
        summary.append("\n");
        
        // Reputation Required
        int effectiveRepRequired = getReputationRequired(deityId, prayerType, jsonConfig);
        int jsonRepRequired = jsonConfig != null ? jsonConfig.reputation_required : 0;
        summary.append("Reputation Required: ").append(effectiveRepRequired).append(" ");
        if (hasServerOverrides && effectiveRepRequired != jsonRepRequired) {
            summary.append("(Server Override - JSON: ").append(jsonRepRequired).append(")");
        } else {
            summary.append("(JSON Default)");
        }
        summary.append("\n");
        
        // Auto Judge Commands
        boolean effectiveAutoJudge = getAutoJudgeCommands(deityId, prayerType, jsonConfig);
        boolean jsonAutoJudge = jsonConfig != null ? jsonConfig.auto_judge_commands : false;
        summary.append("Auto Judge Commands: ").append(effectiveAutoJudge).append(" ");
        if (hasServerOverrides && effectiveAutoJudge != jsonAutoJudge) {
            summary.append("(Server Override - JSON: ").append(jsonAutoJudge).append(")");
        } else {
            summary.append("(JSON Default)");
        }
        summary.append("\n");
        
        return summary.toString();
    }
}
