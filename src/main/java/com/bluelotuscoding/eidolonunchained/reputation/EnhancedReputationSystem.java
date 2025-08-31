package com.bluelotuscoding.eidolonunchained.reputation;

import com.bluelotuscoding.eidolonunchained.deity.DatapackDeity;
import elucent.eidolon.capability.IReputation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Enhanced reputation system that provides natural ways to gain reputation through conversations and interactions
 */
public class EnhancedReputationSystem {
    private static final Logger LOGGER = LogManager.getLogger();
    
    // Track conversation interactions to prevent spam
    public static final Map<String, Long> lastConversationTime = new HashMap<>();
    public static final Map<String, Integer> dailyConversationCount = new HashMap<>();
    private static final long CONVERSATION_COOLDOWN = 300000; // 5 minutes
    private static final int MAX_CONVERSATIONS_PER_DAY = 10;
    
    /**
     * Award reputation for meaningful conversation interactions
     */
    public static void awardConversationReputation(ServerPlayer player, DatapackDeity deity, String interactionType) {
        try {
            String key = player.getUUID().toString() + ":" + deity.getId().toString();
            long currentTime = System.currentTimeMillis();
            
            // Check cooldown
            Long lastTime = lastConversationTime.get(key);
            if (lastTime != null && (currentTime - lastTime) < CONVERSATION_COOLDOWN) {
                return; // Too soon
            }
            
            // Check daily limit
            int todayCount = dailyConversationCount.getOrDefault(key, 0);
            if (todayCount >= MAX_CONVERSATIONS_PER_DAY) {
                return; // Reached daily limit
            }
            
            double reputationGain = calculateConversationReputation(interactionType, deity.getPlayerReputation(player));
            
            if (reputationGain > 0) {
                addReputation(player, deity, reputationGain);
                
                // Update tracking
                lastConversationTime.put(key, currentTime);
                dailyConversationCount.put(key, todayCount + 1);
                
                // Notify player of reputation gain
                player.sendSystemMessage(Component.literal(
                    "§e✨ " + deity.getDisplayName() + " §7acknowledges your devotion §e(+" + 
                    String.format("%.1f", reputationGain) + " reputation)"));
                
                LOGGER.info("🌟 REPUTATION GAIN: {} gained {} reputation with {} for {}", 
                    player.getName().getString(), reputationGain, deity.getName(), interactionType);
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to award conversation reputation: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Calculate reputation gain based on interaction type and current standing
     */
    private static double calculateConversationReputation(String interactionType, double currentReputation) {
        double baseGain = switch (interactionType.toLowerCase()) {
            case "meaningful_conversation" -> 2.0;
            case "first_contact" -> 5.0;
            case "prayer_response" -> 1.5;
            case "quest_acceptance" -> 3.0;
            case "respectful_interaction" -> 1.0;
            case "deep_conversation" -> 2.5;
            default -> 1.0;
        };
        
        // Diminishing returns as reputation increases
        if (currentReputation > 75) {
            baseGain *= 0.3; // Much slower gain at high reputation
        } else if (currentReputation > 50) {
            baseGain *= 0.5; // Slower gain at medium reputation
        } else if (currentReputation > 25) {
            baseGain *= 0.7; // Slightly slower gain
        }
        
        return baseGain;
    }
    
    /**
     * Award reputation for specific achievements
     */
    public static void awardAchievementReputation(ServerPlayer player, DatapackDeity deity, String achievement, double amount) {
        try {
            addReputation(player, deity, amount);
            
            player.sendSystemMessage(Component.literal(
                "§6✦ " + deity.getDisplayName() + " §7rewards your " + achievement + " §6(+" + 
                String.format("%.1f", amount) + " reputation)"));
            
            LOGGER.info("🏆 ACHIEVEMENT REPUTATION: {} gained {} reputation with {} for {}", 
                player.getName().getString(), amount, deity.getName(), achievement);
                
        } catch (Exception e) {
            LOGGER.error("Failed to award achievement reputation: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Award reputation for prayer offerings (integrates with existing prayer system)
     */
    public static void awardPrayerReputation(ServerPlayer player, DatapackDeity deity, String prayerType) {
        String key = player.getUUID().toString() + ":" + deity.getId().toString() + ":prayer";
        long currentTime = System.currentTimeMillis();
        
        // Check prayer cooldown (shorter than conversation)
        Long lastTime = lastConversationTime.get(key);
        if (lastTime != null && (currentTime - lastTime) < 180000) { // 3 minutes
            return;
        }
        
        double prayerGain = switch (prayerType) {
            case "blessing" -> 1.0;
            case "guidance" -> 0.8;
            case "protection" -> 1.2;
            case "curse" -> 1.5; // Cursing enemies shows dedication
            default -> 0.5;
        };
        
        if (prayerGain > 0) {
            addReputation(player, deity, prayerGain);
            lastConversationTime.put(key, currentTime);
            
            LOGGER.debug("🙏 PRAYER REPUTATION: {} gained {} reputation with {} for {} prayer", 
                player.getName().getString(), prayerGain, deity.getName(), prayerType);
        }
    }
    
    /**
     * Natural reputation gain for spending time in deity's domain
     */
    public static void awardPresenceReputation(ServerPlayer player, DatapackDeity deity, String location) {
        String key = player.getUUID().toString() + ":" + deity.getId().toString() + ":presence";
        long currentTime = System.currentTimeMillis();
        
        // Check presence cooldown (much longer)
        Long lastTime = lastConversationTime.get(key);
        if (lastTime != null && (currentTime - lastTime) < 1800000) { // 30 minutes
            return;
        }
        
        double presenceGain = 0.5; // Small but steady gain for spending time in appropriate areas
        
        addReputation(player, deity, presenceGain);
        lastConversationTime.put(key, currentTime);
        
        LOGGER.debug("🌍 PRESENCE REPUTATION: {} gained {} reputation with {} for being in {}", 
            player.getName().getString(), presenceGain, deity.getName(), location);
    }
    
    /**
     * Core reputation addition method
     */
    private static void addReputation(ServerPlayer player, DatapackDeity deity, double amount) {
        player.getCapability(IReputation.INSTANCE).ifPresent(reputation -> {
            reputation.addReputation(player.getUUID(), deity.getId(), amount);
        });
    }
    
    /**
     * Get current reputation safely
     */
    public static double getCurrentReputation(ServerPlayer player, DatapackDeity deity) {
        return deity.getPlayerReputation(player);
    }
    
    /**
     * Get current progression stage for a player with a specific deity
     */
    public static String getCurrentProgressionStage(ServerPlayer player, DatapackDeity deity) {
        if (deity == null) return "Unknown";
        
        double reputation = deity.getPlayerReputation(player);
        return deity.getCurrentProgressionStage(reputation);
    }
    
    /**
     * Get next progression stage info for a player with a specific deity
     */
    public static String getNextProgressionInfo(ServerPlayer player, DatapackDeity deity) {
        if (deity == null) return "Unknown deity";
        
        double reputation = deity.getPlayerReputation(player);
        return deity.getNextProgressionInfo(reputation);
    }
    
    /**
     * Get reputation tier name for display (fallback for when deity is unknown)
     */
    public static String getReputationTier(double reputation) {
        if (reputation >= 100) return "Divine Champion";
        if (reputation >= 75) return "High Priest";
        if (reputation >= 50) return "Trusted Acolyte";
        if (reputation >= 25) return "Devoted Follower";
        if (reputation >= 10) return "Acknowledged Seeker";
        if (reputation >= 0) return "Unknown Newcomer";
        return "Unwelcome Stranger";
    }
    
    /**
     * Reset daily counters (should be called daily)
     */
    public static void resetDailyCounters() {
        dailyConversationCount.clear();
        LOGGER.info("Reset daily conversation counters for reputation system");
    }
    
    /**
     * Get reputation progress info for player
     */
    public static void showProgressInfo(ServerPlayer player, DatapackDeity deity) {
        double current = deity.getPlayerReputation(player);
        String currentStage = deity.getCurrentProgressionStage(current);
        String nextInfo = deity.getNextProgressionInfo(current);
        
        player.sendSystemMessage(Component.literal("§e=== " + deity.getDisplayName() + " Reputation Status ==="));
        player.sendSystemMessage(Component.literal("§6Current: §f" + String.format("%.1f", current) + " §7(" + currentStage + ")"));
        player.sendSystemMessage(Component.literal("§7" + nextInfo));
        
        // Show today's conversation count
        String key = player.getUUID().toString() + ":" + deity.getId().toString();
        int todayCount = dailyConversationCount.getOrDefault(key, 0);
        player.sendSystemMessage(Component.literal("§7Conversations today: " + todayCount + "/" + MAX_CONVERSATIONS_PER_DAY));
    }
    
    /**
     * Get the current date key for daily tracking
     */
    public static String getCurrentDateKey() {
        return String.valueOf(System.currentTimeMillis() / (24 * 60 * 60 * 1000)); // Days since epoch
    }
}
