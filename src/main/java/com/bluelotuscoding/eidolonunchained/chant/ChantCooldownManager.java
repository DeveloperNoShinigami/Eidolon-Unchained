package com.bluelotuscoding.eidolonunchained.chant;

import com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages cooldowns for chant casting to prevent spam
 */
public class ChantCooldownManager {
    
    private static final Map<UUID, Long> playerCooldowns = new HashMap<>();
    
    /**
     * Check if a player can cast a chant (not on cooldown)
     */
    public static boolean canCastChant(Player player) {
        UUID playerId = player.getUUID();
        long currentTime = System.currentTimeMillis();
        
        if (!playerCooldowns.containsKey(playerId)) {
            return true;
        }
        
        long lastCastTime = playerCooldowns.get(playerId);
        int cooldownSeconds = EidolonUnchainedConfig.COMMON.chantCooldownSeconds.get();
        long cooldownMs = cooldownSeconds * 1000L;
        
        return (currentTime - lastCastTime) >= cooldownMs;
    }
    
    /**
     * Get remaining cooldown time in seconds for a player
     */
    public static int getRemainingCooldown(Player player) {
        UUID playerId = player.getUUID();
        long currentTime = System.currentTimeMillis();
        
        if (!playerCooldowns.containsKey(playerId)) {
            return 0;
        }
        
        long lastCastTime = playerCooldowns.get(playerId);
        int cooldownSeconds = EidolonUnchainedConfig.COMMON.chantCooldownSeconds.get();
        long cooldownMs = cooldownSeconds * 1000L;
        long remainingMs = cooldownMs - (currentTime - lastCastTime);
        
        return Math.max(0, (int) (remainingMs / 1000));
    }
    
    /**
     * Set cooldown for a player after casting a chant
     */
    public static void setCooldown(Player player) {
        UUID playerId = player.getUUID();
        playerCooldowns.put(playerId, System.currentTimeMillis());
    }
    
    /**
     * Clear cooldown for a player (admin command or special circumstances)
     */
    public static void clearCooldown(Player player) {
        UUID playerId = player.getUUID();
        playerCooldowns.remove(playerId);
    }
    
    /**
     * Clear old cooldown entries to prevent memory leaks
     */
    public static void cleanup() {
        long currentTime = System.currentTimeMillis();
        int maxCooldownSeconds = EidolonUnchainedConfig.COMMON.chantCooldownSeconds.get();
        long maxCooldownMs = maxCooldownSeconds * 1000L;
        
        playerCooldowns.entrySet().removeIf(entry -> 
            (currentTime - entry.getValue()) > (maxCooldownMs * 2) // Remove after 2x cooldown time
        );
    }
}
