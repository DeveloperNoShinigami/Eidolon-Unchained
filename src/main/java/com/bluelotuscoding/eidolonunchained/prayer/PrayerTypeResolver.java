package com.bluelotuscoding.eidolonunchained.prayer;

import com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig;
import com.bluelotuscoding.eidolonunchained.ai.AIDeityManager;
import com.bluelotuscoding.eidolonunchained.chant.DatapackChant;
import com.bluelotuscoding.eidolonunchained.integration.ai.EnhancedCommandExtractor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.Set;

/**
 * Dedicated utility class for resolving prayer types from various sources.
 * Handles the priority system: chant-based > message-based > default fallback.
 */
public class PrayerTypeResolver {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * Resolve prayer type using the priority system:
     * 1. Chant-based determination (if chant was performed)
     * 2. Message-based determination (fallback)
     * 3. Default to "conversation"
     */
    public static String resolve(ServerPlayer player, ResourceLocation deityId, String playerMessage, String aiResponse) {
        // Get deity AI configuration
        AIDeityConfig aiConfig = AIDeityManager.getInstance().getAIConfig(deityId);
        if (aiConfig == null) {
            LOGGER.warn("No AI config found for deity {}, defaulting to conversation", deityId);
            return "conversation";
        }
        
        // Priority 1: Check for chant context
        DatapackChant triggeringChant = EnhancedCommandExtractor.getLastPerformedChant(player);
        if (triggeringChant != null) {
            String chantPrayerType = determinePrayerTypeFromChant(triggeringChant, aiConfig);
            LOGGER.info("🔥 Determined prayer type from chant {}: {}", triggeringChant.getId(), chantPrayerType);
            return chantPrayerType;
        }
        
        // Priority 2: Fallback to message-based detection
        String messagePrayerType = determinePrayerTypeFromMessage(playerMessage, aiResponse, aiConfig);
        LOGGER.info("🔥 Determined prayer type from message (fallback): {}", messagePrayerType);
        return messagePrayerType;
    }
    
    /**
     * Determine prayer type from chant configuration.
     * This is the PRIMARY and most accurate method.
     */
    public static String determinePrayerTypeFromChant(DatapackChant chant, AIDeityConfig aiConfig) {
        if (chant == null || aiConfig == null) {
            LOGGER.warn("🔥 determinePrayerTypeFromChant called with null chant or config - returning 'conversation'");
            return "conversation";
        }
        
        // Check if the chant has a prayer_effect_type property configured
        String chantPrayerType = chant.getPrayerEffectType();
        if (chantPrayerType != null && !chantPrayerType.isEmpty()) {
            // Validate that this prayer type exists in the deity's configuration
            if (aiConfig.prayer_configs.containsKey(chantPrayerType)) {
                LOGGER.info("🔥 Using chant-configured prayer type: '{}' for chant: {}", 
                    chantPrayerType, chant.getId());
                return chantPrayerType;
            } else {
                LOGGER.warn("🔥 Chant {} specifies prayer type '{}' but deity {} doesn't have this type. Available: {}", 
                    chant.getId(), chantPrayerType, aiConfig.deity_id, aiConfig.prayer_configs.keySet());
            }
        }
        
        // Fallback: Try to infer prayer type from chant properties
        String inferredType = inferPrayerTypeFromChant(chant, aiConfig);
        if (inferredType != null && aiConfig.prayer_configs.containsKey(inferredType)) {
            LOGGER.info("🔥 Inferred prayer type: '{}' for chant: {}", inferredType, chant.getId());
            return inferredType;
        }
        
        // Ultimate fallback: conversation (all deities should have this)
        LOGGER.info("🔥 Using fallback prayer type 'conversation' for chant: {}", chant.getId());
        return "conversation";
    }
    
    /**
     * Determine prayer type from player message content.
     * This is the FALLBACK method when no chant context is available.
     */
    public static String determinePrayerTypeFromMessage(String playerMessage, String aiResponse, AIDeityConfig aiConfig) {
        if (aiConfig == null || aiConfig.prayer_configs == null) {
            return "conversation"; // Safe fallback
        }
        
        String lowerMessage = playerMessage.toLowerCase();
        Set<String> availablePrayerTypes = aiConfig.prayer_configs.keySet();
        
        LOGGER.info("🔥 Available prayer types for deity {}: {}", aiConfig.deity_id, availablePrayerTypes);
        
        // Match player message against available prayer types
        for (String prayerType : availablePrayerTypes) {
            String lowerPrayerType = prayerType.toLowerCase();
            
            // Direct mention of prayer type
            if (lowerMessage.contains(lowerPrayerType)) {
                LOGGER.info("🔥 Direct match: '{}' contains prayer type '{}'", lowerMessage, prayerType);
                return prayerType;
            }
            
            // Contextual matching based on prayer type semantics
            switch (lowerPrayerType) {
                case "blessing":
                    if (lowerMessage.contains("bless") || lowerMessage.contains("help") || 
                        lowerMessage.contains("aid") || lowerMessage.contains("boost")) {
                        LOGGER.info("🔥 Contextual match: '{}' → blessing", lowerMessage);
                        return prayerType;
                    }
                    break;
                case "growth":
                    if (lowerMessage.contains("grow") || lowerMessage.contains("plant") || 
                        lowerMessage.contains("harvest") || lowerMessage.contains("fertility")) {
                        LOGGER.info("🔥 Contextual match: '{}' → growth", lowerMessage);
                        return prayerType;
                    }
                    break;
                case "curse":
                    if (lowerMessage.contains("curse") || lowerMessage.contains("punish") || 
                        lowerMessage.contains("revenge") || lowerMessage.contains("harm")) {
                        LOGGER.info("🔥 Contextual match: '{}' → curse", lowerMessage);
                        return prayerType;
                    }
                    break;
                case "protection":
                    if (lowerMessage.contains("protect") || lowerMessage.contains("shield") || 
                        lowerMessage.contains("defend") || lowerMessage.contains("guard")) {
                        LOGGER.info("🔥 Contextual match: '{}' → protection", lowerMessage);
                        return prayerType;
                    }
                    break;
                case "guidance":
                case "communion":
                    if (lowerMessage.contains("wisdom") || lowerMessage.contains("knowledge") || 
                        lowerMessage.contains("teach") || lowerMessage.contains("guide")) {
                        LOGGER.info("🔥 Contextual match: '{}' → {}", lowerMessage, prayerType);
                        return prayerType;
                    }
                    break;
            }
        }
        
        // Default to conversation (all deities have this)
        LOGGER.info("🔥 No specific prayer type match - defaulting to 'conversation'");
        return "conversation";
    }
    
    /**
     * Infer prayer type from chant properties when not explicitly configured
     */
    private static String inferPrayerTypeFromChant(DatapackChant chant, AIDeityConfig aiConfig) {
        String chantName = chant.getName().toLowerCase();
        String chantCategory = chant.getCategory().toLowerCase();
        
        // Check available prayer types in order of specificity
        Set<String> availableTypes = aiConfig.prayer_configs.keySet();
        
        // Direct name/category matches
        for (String prayerType : availableTypes) {
            String lowerType = prayerType.toLowerCase();
            if (chantName.contains(lowerType) || chantCategory.contains(lowerType)) {
                return prayerType;
            }
        }
        
        // Semantic matching based on chant name/category
        if (availableTypes.contains("blessing")) {
            if (chantName.contains("bless") || chantName.contains("help") || 
                chantCategory.contains("blessing") || chantCategory.contains("aid")) {
                return "blessing";
            }
        }
        
        if (availableTypes.contains("growth")) {
            if (chantName.contains("grow") || chantName.contains("nature") || 
                chantCategory.contains("growth") || chantCategory.contains("nature")) {
                return "growth";
            }
        }
        
        if (availableTypes.contains("curse")) {
            if (chantName.contains("curse") || chantName.contains("harm") || 
                chantCategory.contains("curse") || chantCategory.contains("dark")) {
                return "curse";
            }
        }
        
        if (availableTypes.contains("protection")) {
            if (chantName.contains("protect") || chantName.contains("shield") || 
                chantCategory.contains("protection") || chantCategory.contains("ward")) {
                return "protection";
            }
        }
        
        if (availableTypes.contains("guidance") || availableTypes.contains("communion")) {
            if (chantName.contains("wisdom") || chantName.contains("knowledge") || 
                chantCategory.contains("guidance") || chantCategory.contains("communion")) {
                return availableTypes.contains("guidance") ? "guidance" : "communion";
            }
        }
        
        // No match found
        return null;
    }
}
