package com.bluelotuscoding.eidolonunchained.ai;

import com.bluelotuscoding.eidolonunchained.chat.ConversationHistoryManager;
import net.minecraft.resources.ResourceLocation;

/**
 * Utility class for resolving effective AI configuration values.
 * Implements priority system: Server Override > JSON Default > System Default
 */
public class AIConfigResolver {

    public static int getCooldown(ResourceLocation deityId, String prayerType, PrayerAIConfig jsonConfig) {
        return ConversationHistoryManager.getEffectiveCooldownStatic(deityId, prayerType, jsonConfig);
    }

    public static int getMaxCommands(ResourceLocation deityId, String prayerType, PrayerAIConfig jsonConfig) {
        return ConversationHistoryManager.getEffectiveMaxCommandsStatic(deityId, prayerType, jsonConfig);
    }

    public static int getReputationRequired(ResourceLocation deityId, String prayerType, PrayerAIConfig jsonConfig) {
        return ConversationHistoryManager.getEffectiveReputationRequiredStatic(deityId, prayerType, jsonConfig);
    }

    public static boolean getAutoJudgeCommands(ResourceLocation deityId, String prayerType, PrayerAIConfig jsonConfig) {
        return ConversationHistoryManager.getEffectiveAutoJudgeCommandsStatic(deityId, prayerType, jsonConfig);
    }

    public static int getJudgmentThreshold(ResourceLocation deityId, String thresholdType, JudgmentConfig jsonJudgment) {
        return ConversationHistoryManager.getEffectiveJudgmentThresholdStatic(deityId, thresholdType, jsonJudgment);
    }

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

    public static String getConfigurationSummary(ResourceLocation deityId, String prayerType, PrayerAIConfig jsonConfig) {
        StringBuilder summary = new StringBuilder();
        summary.append("=== Configuration Summary for ").append(deityId).append(" (").append(prayerType).append(") ===\n");

        boolean hasServerOverrides = ConversationHistoryManager.hasServerOverridesStatic(deityId);

        int effectiveCooldown = getCooldown(deityId, prayerType, jsonConfig);
        int jsonCooldown = jsonConfig != null ? jsonConfig.cooldown_seconds : 1800;
        summary.append("Cooldown: ").append(effectiveCooldown).append(" seconds ");
        if (hasServerOverrides && effectiveCooldown != jsonCooldown) {
            summary.append("(Server Override - JSON: ").append(jsonCooldown).append(")");
        } else {
            summary.append("(JSON Default)");
        }
        summary.append("\n");

        int effectiveMaxCommands = getMaxCommands(deityId, prayerType, jsonConfig);
        int jsonMaxCommands = jsonConfig != null ? jsonConfig.max_commands : 3;
        summary.append("Max Commands: ").append(effectiveMaxCommands).append(" ");
        if (hasServerOverrides && effectiveMaxCommands != jsonMaxCommands) {
            summary.append("(Server Override - JSON: ").append(jsonMaxCommands).append(")");
        } else {
            summary.append("(JSON Default)");
        }
        summary.append("\n");

        int effectiveRepRequired = getReputationRequired(deityId, prayerType, jsonConfig);
        int jsonRepRequired = jsonConfig != null ? jsonConfig.reputation_required : 0;
        summary.append("Reputation Required: ").append(effectiveRepRequired).append(" ");
        if (hasServerOverrides && effectiveRepRequired != jsonRepRequired) {
            summary.append("(Server Override - JSON: ").append(jsonRepRequired).append(")");
        } else {
            summary.append("(JSON Default)");
        }
        summary.append("\n");

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
