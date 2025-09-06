package com.bluelotuscoding.eidolonunchained.chant;

import com.mojang.logging.LogUtils;
import elucent.eidolon.api.spells.Sign;
import elucent.eidolon.api.spells.SignSequence;
import elucent.eidolon.common.entity.ChantCasterEntity;
import elucent.eidolon.registries.EidolonEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

/**
 * Manages active chanting entities that provide real-time visual feedback
 * as players build up spell sequences with keybinds.
 */
@OnlyIn(Dist.CLIENT)
public class ActiveChantingManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Track active chanting entities per player
    private static final WeakHashMap<Player, ChantCasterEntity> activeChantingEntities = new WeakHashMap<>();
    private static final WeakHashMap<Player, List<Sign>> currentSequences = new WeakHashMap<>();
    
    /**
     * Start or update an active chanting sequence for a player
     */
    public static void addSignToActiveChant(Player player, Sign sign) {
        if (player == null || sign == null) return;
        
        Level level = player.level();
        if (level == null) return;
        
        // Get or create the current sequence for this player
        List<Sign> sequence = currentSequences.computeIfAbsent(player, k -> new ArrayList<>());
        sequence.add(sign);
        
        LOGGER.debug("Adding sign {} to active chant for player {}. Sequence now: {}", 
                    sign.getRegistryName(), player.getName().getString(), sequence.size());
        
        // Get or create the chanting entity
        ChantCasterEntity chantingEntity = activeChantingEntities.get(player);
        
        if (chantingEntity == null || chantingEntity.isRemoved()) {
            // Create new chanting entity
            chantingEntity = createChantingEntity(player, sequence);
            activeChantingEntities.put(player, chantingEntity);
            
            // Spawn the entity in the world
            level.addFreshEntity(chantingEntity);
            
            LOGGER.info("Created new active chanting entity for player {}", player.getName().getString());
        } else {
            // Update existing entity with new sign sequence
            updateChantingEntity(chantingEntity, sequence);
            
            LOGGER.debug("Updated existing chanting entity for player {}", player.getName().getString());
        }
    }
    
    /**
     * Create a new ChantCasterEntity for the player with the current sequence
     */
    private static ChantCasterEntity createChantingEntity(Player player, List<Sign> sequence) {
        // Position entity slightly in front of the player
        Vec3 playerPos = player.position();
        Vec3 lookDirection = player.getLookAngle();
        Vec3 entityPos = playerPos.add(lookDirection.scale(1.5));
        
        // Create the entity
        ChantCasterEntity entity = new ChantCasterEntity(player.level(), player, sequence, lookDirection);
        entity.setPos(entityPos.x, entityPos.y + 1.0, entityPos.z);
        
        return entity;
    }
    
    /**
     * Update an existing ChantCasterEntity with a new sign sequence
     */
    private static void updateChantingEntity(ChantCasterEntity entity, List<Sign> sequence) {
        try {
            // Use reflection to access the protected setChantTag method
            java.lang.reflect.Method setChantTagMethod = ChantCasterEntity.class.getDeclaredMethod("setChantTag", List.class);
            setChantTagMethod.setAccessible(true);
            setChantTagMethod.invoke(entity, sequence);
            
            LOGGER.debug("Updated chanting entity with {} signs", sequence.size());
        } catch (Exception e) {
            LOGGER.error("Failed to update chanting entity: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Complete the active chanting sequence for a player
     */
    public static void completeActiveChant(Player player) {
        ChantCasterEntity entity = activeChantingEntities.get(player);
        if (entity != null && !entity.isRemoved()) {
            // Let the entity complete its casting naturally
            // The entity will handle spell resolution and removal
            LOGGER.info("Completing active chant for player {}", player.getName().getString());
        }
        
        // Clean up our tracking
        clearActiveChant(player);
    }
    
    /**
     * Clear the active chanting sequence for a player (cancel/timeout)
     */
    public static void clearActiveChant(Player player) {
        ChantCasterEntity entity = activeChantingEntities.remove(player);
        if (entity != null && !entity.isRemoved()) {
            // Remove the entity gracefully
            entity.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            LOGGER.debug("Removed active chanting entity for player {}", player.getName().getString());
        }
        
        // Clear the sequence
        currentSequences.remove(player);
    }
    
    /**
     * Get the current sign sequence for a player
     */
    public static List<Sign> getCurrentSequence(Player player) {
        return currentSequences.getOrDefault(player, new ArrayList<>());
    }
    
    /**
     * Check if a player has an active chanting sequence
     */
    public static boolean hasActiveChant(Player player) {
        ChantCasterEntity entity = activeChantingEntities.get(player);
        return entity != null && !entity.isRemoved() && !currentSequences.getOrDefault(player, new ArrayList<>()).isEmpty();
    }
    
    /**
     * Clean up removed or invalid entities
     */
    public static void cleanup() {
        activeChantingEntities.entrySet().removeIf(entry -> {
            ChantCasterEntity entity = entry.getValue();
            return entity == null || entity.isRemoved();
        });
        
        // Also clean up sequences for players that no longer have entities
        currentSequences.entrySet().removeIf(entry -> {
            Player player = entry.getKey();
            return !activeChantingEntities.containsKey(player);
        });
    }
}
