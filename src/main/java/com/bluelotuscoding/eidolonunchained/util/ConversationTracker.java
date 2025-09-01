package com.bluelotuscoding.eidolonunchained.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple conversation tracking utility for rate limiting
 * Separated from reputation system to keep concerns distinct
 */
public class ConversationTracker {
    private static final Logger LOGGER = LogManager.getLogger();
    
    // Track conversation timing for rate limiting (not reputation)
    private static final Map<String, Long> lastConversationTime = new HashMap<>();
    private static final Map<String, Integer> dailyConversationCount = new HashMap<>();
    private static final long CONVERSATION_COOLDOWN = 300000; // 5 minutes
    private static final int MAX_CONVERSATIONS_PER_DAY = 10;
    
    /**
     * Check if player can have a conversation with deity based on cooldowns
     */
    public static boolean canConverse(ServerPlayer player, ResourceLocation deityId) {
        String key = player.getUUID().toString() + ":" + deityId.toString();
        long currentTime = System.currentTimeMillis();
        
        // Check cooldown
        Long lastTime = lastConversationTime.get(key);
        if (lastTime != null && (currentTime - lastTime) < CONVERSATION_COOLDOWN) {
            return false; // Too soon
        }
        
        // Check daily limit
        String dailyKey = key + "_" + getCurrentDateKey();
        int todayCount = dailyConversationCount.getOrDefault(dailyKey, 0);
        if (todayCount >= MAX_CONVERSATIONS_PER_DAY) {
            return false; // Reached daily limit
        }
        
        return true;
    }
    
    /**
     * Record a conversation for tracking purposes
     */
    public static void recordConversation(ServerPlayer player, ResourceLocation deityId) {
        String key = player.getUUID().toString() + ":" + deityId.toString();
        long currentTime = System.currentTimeMillis();
        
        // Update tracking
        lastConversationTime.put(key, currentTime);
        
        String dailyKey = key + "_" + getCurrentDateKey();
        int todayCount = dailyConversationCount.getOrDefault(dailyKey, 0);
        dailyConversationCount.put(dailyKey, todayCount + 1);
        
        LOGGER.debug("Recorded conversation: {} with {} (daily count: {})", 
            player.getName().getString(), deityId, todayCount + 1);
    }
    
    /**
     * Get time until player can converse again
     */
    public static long getTimeUntilNextConversation(ServerPlayer player, ResourceLocation deityId) {
        String key = player.getUUID().toString() + ":" + deityId.toString();
        Long lastTime = lastConversationTime.get(key);
        if (lastTime == null) return 0;
        
        long timeSince = System.currentTimeMillis() - lastTime;
        return Math.max(0, CONVERSATION_COOLDOWN - timeSince);
    }
    
    /**
     * Get today's conversation count
     */
    public static int getTodayConversationCount(ServerPlayer player, ResourceLocation deityId) {
        String key = player.getUUID().toString() + ":" + deityId.toString();
        String dailyKey = key + "_" + getCurrentDateKey();
        return dailyConversationCount.getOrDefault(dailyKey, 0);
    }
    
    /**
     * Get the current date key for daily tracking
     */
    private static String getCurrentDateKey() {
        return LocalDate.now().toString();
    }
    
    /**
     * Reset daily counters (should be called daily or on server restart)
     */
    public static void resetDailyCounters() {
        String today = getCurrentDateKey();
        // Remove old entries, keep today's
        dailyConversationCount.entrySet().removeIf(entry -> !entry.getKey().endsWith(today));
        LOGGER.info("Reset old daily conversation counters, kept entries for {}", today);
    }
}
