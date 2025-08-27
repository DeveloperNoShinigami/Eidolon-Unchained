package com.bluelotuscoding.eidolonunchained.chant;

import com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages cooldowns for chant casting to prevent spam
 * Supports per-chant cooldowns with global config as fallback
 */
public class ChantCooldownManager {
    
    // playerId -> (chantId -> lastCastTime)
    private static final Map<UUID, Map<ResourceLocation, Long>> playerChantCooldowns = new HashMap<>();
    
    /**
     * Check if a player can cast a specific chant (not on cooldown)
     */
    public static boolean canCastChant(Player player, DatapackChant chant) {
        UUID playerId = player.getUUID();
        ResourceLocation chantId = chant.getId();
        long currentTime = System.currentTimeMillis();
        
        Map<ResourceLocation, Long> playerCooldowns = playerChantCooldowns.get(playerId);
        if (playerCooldowns == null || !playerCooldowns.containsKey(chantId)) {
            return true;
        }
        
        long lastCastTime = playerCooldowns.get(chantId);
        int cooldownSeconds = chant.getCooldown(); // Use chant-specific cooldown
        long cooldownMs = cooldownSeconds * 1000L;
        
        return (currentTime - lastCastTime) >= cooldownMs;
    }
    
    /**
     * Get remaining cooldown time in seconds for a specific chant
     */
    public static int getRemainingCooldown(Player player, DatapackChant chant) {
        UUID playerId = player.getUUID();
        ResourceLocation chantId = chant.getId();
        long currentTime = System.currentTimeMillis();
        
        Map<ResourceLocation, Long> playerCooldowns = playerChantCooldowns.get(playerId);
        if (playerCooldowns == null || !playerCooldowns.containsKey(chantId)) {
            return 0;
        }
        
        long lastCastTime = playerCooldowns.get(chantId);
        int cooldownSeconds = chant.getCooldown(); // Use chant-specific cooldown
        long cooldownMs = cooldownSeconds * 1000L;
        long remainingMs = cooldownMs - (currentTime - lastCastTime);
        
        return Math.max(0, (int) (remainingMs / 1000));
    }
    
    /**
     * Set cooldown for a player after casting a specific chant
     */
    public static void setCooldown(Player player, DatapackChant chant) {
        UUID playerId = player.getUUID();
        ResourceLocation chantId = chant.getId();
        
        playerChantCooldowns.computeIfAbsent(playerId, k -> new HashMap<>())
                           .put(chantId, System.currentTimeMillis());
    }
    
    /**
     * Clear cooldown for a specific chant for a player (admin command or special circumstances)
     */
    public static void clearCooldown(Player player, ResourceLocation chantId) {
        UUID playerId = player.getUUID();
        Map<ResourceLocation, Long> playerCooldowns = playerChantCooldowns.get(playerId);
        if (playerCooldowns != null) {
            playerCooldowns.remove(chantId);
            if (playerCooldowns.isEmpty()) {
                playerChantCooldowns.remove(playerId);
            }
        }
    }
    
    /**
     * Clear all cooldowns for a player
     */
    public static void clearAllCooldowns(Player player) {
        UUID playerId = player.getUUID();
        playerChantCooldowns.remove(playerId);
    }
    
    /**
     * Clear old cooldown entries to prevent memory leaks
     */
    public static void cleanup() {
        long currentTime = System.currentTimeMillis();
        long maxAge = 24 * 60 * 60 * 1000L; // 24 hours
        
        playerChantCooldowns.entrySet().removeIf(playerEntry -> {
            Map<ResourceLocation, Long> chantCooldowns = playerEntry.getValue();
            chantCooldowns.entrySet().removeIf(chantEntry -> 
                (currentTime - chantEntry.getValue()) > maxAge
            );
            return chantCooldowns.isEmpty();
        });
    }
}
