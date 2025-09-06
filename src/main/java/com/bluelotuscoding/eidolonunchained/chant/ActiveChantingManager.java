package com.bluelotuscoding.eidolonunchained.chant;

import com.mojang.logging.LogUtils;
import elucent.eidolon.api.spells.Sign;
import elucent.eidolon.common.entity.ChantCasterEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages active chanting with real-time ChantCasterEntity spawning
 * This creates the immersive visual chanting experience where entities 
 * spawn immediately and update as signs are added
 */
@OnlyIn(Dist.CLIENT)
public class ActiveChantingManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private static ChantCasterEntity activeChantingEntity = null;
    private static List<Sign> currentSigns = new ArrayList<>();
    private static Player currentCaster = null;
    
    /**
     * Start active chanting - spawn ChantCasterEntity immediately
     */
    public static void startActiveChanting(Player player, Sign firstSign) {
        if (activeChantingEntity != null) {
            // Clear existing chant first
            stopActiveChanting();
        }
        
        LOGGER.info("Starting active chanting for player {} with sign {}", player.getName().getString(), firstSign.getRegistryName());
        
        try {
            // Create initial sign list
            List<Sign> initialSigns = new ArrayList<>();
            initialSigns.add(firstSign);
            
            // Get player's look direction
            Vec3 lookDirection = player.getLookAngle();
            
            // Spawn ChantCasterEntity using the correct constructor
            // Constructor: ChantCasterEntity(Level world, Player caster, List<Sign> runes, Vec3 look)
            activeChantingEntity = new ChantCasterEntity(player.level(), player, initialSigns, lookDirection);
            
            // Position the entity near the player
            activeChantingEntity.setPos(player.getX(), player.getY() + 0.5, player.getZ());
            
            // Add entity to world
            player.level().addFreshEntity(activeChantingEntity);
            
            // Store current signs and caster
            currentSigns.clear();
            currentSigns.add(firstSign);
            currentCaster = player;
            
            LOGGER.info("Successfully spawned ChantCasterEntity for active chanting");
            
        } catch (Exception e) {
            LOGGER.error("Failed to start active chanting: {}", e.getMessage(), e);
            activeChantingEntity = null;
        }
    }
    
    /**
     * Add a sign to the active chanting sequence
     */
    public static void addSignToActiveChant(Sign sign) {
        if (activeChantingEntity == null || currentCaster == null) {
            LOGGER.warn("Attempted to add sign to non-existent active chant");
            return;
        }
        
        LOGGER.info("Adding sign {} to active chant", sign.getRegistryName());
        
        try {
            // Add sign to our tracking list
            currentSigns.add(sign);
            
            // Update the ChantCasterEntity's sign sequence using reflection
            updateChantingEntity(currentSigns);
            
            LOGGER.info("Successfully added sign to active chant. Total signs: {}", currentSigns.size());
            
        } catch (Exception e) {
            LOGGER.error("Failed to add sign to active chant: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Update the ChantCasterEntity with new sign sequence
     */
    private static void updateChantingEntity(List<Sign> signs) {
        if (activeChantingEntity == null) return;
        
        try {
            // Use reflection to access and update the runes field in ChantCasterEntity
            Field runesField = ChantCasterEntity.class.getDeclaredField("runes");
            runesField.setAccessible(true);
            
            // Create new list for the entity
            List<Sign> entitySigns = new ArrayList<>(signs);
            runesField.set(activeChantingEntity, entitySigns);
            
            LOGGER.debug("Updated ChantCasterEntity with {} signs", signs.size());
            
        } catch (Exception e) {
            LOGGER.warn("Failed to update ChantCasterEntity signs via reflection: {}", e.getMessage());
            // Fallback: Try to recreate the entity with updated signs
            recreateChantingEntity(signs);
        }
    }
    
    /**
     * Recreate the chanting entity if reflection fails
     */
    private static void recreateChantingEntity(List<Sign> signs) {
        if (currentCaster == null) return;
        
        try {
            // Remove old entity
            if (activeChantingEntity != null) {
                activeChantingEntity.discard();
            }
            
            // Create new entity with updated signs
            Vec3 lookDirection = currentCaster.getLookAngle();
            activeChantingEntity = new ChantCasterEntity(currentCaster.level(), currentCaster, new ArrayList<>(signs), lookDirection);
            
            // Position and add to world
            activeChantingEntity.setPos(currentCaster.getX(), currentCaster.getY() + 0.5, currentCaster.getZ());
            currentCaster.level().addFreshEntity(activeChantingEntity);
            
            LOGGER.debug("Recreated ChantCasterEntity with {} signs", signs.size());
            
        } catch (Exception e) {
            LOGGER.error("Failed to recreate ChantCasterEntity: {}", e.getMessage(), e);
            activeChantingEntity = null;
        }
    }
    
    /**
     * Complete the active chanting - let the entity finish its casting
     */
    public static void completeActiveChanting() {
        if (activeChantingEntity == null) {
            LOGGER.warn("Attempted to complete non-existent active chant");
            return;
        }
        
        LOGGER.info("Completing active chant with {} signs", currentSigns.size());
        
        try {
            // The ChantCasterEntity should handle completion automatically
            // We just need to let it finish its casting sequence
            
            // Clear our tracking but let the entity complete naturally
            currentSigns.clear();
            currentCaster = null;
            activeChantingEntity = null; // It will remove itself when done
            
            LOGGER.info("Active chant completion initiated");
            
        } catch (Exception e) {
            LOGGER.error("Failed to complete active chant: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Stop active chanting immediately
     */
    public static void stopActiveChanting() {
        if (activeChantingEntity != null) {
            LOGGER.info("Stopping active chant");
            activeChantingEntity.discard();
            activeChantingEntity = null;
        }
        
        currentSigns.clear();
        currentCaster = null;
    }
    
    /**
     * Check if active chanting is in progress
     */
    public static boolean isActiveChanting() {
        return activeChantingEntity != null && !activeChantingEntity.isRemoved();
    }
    
    /**
     * Get current sign count
     */
    public static int getCurrentSignCount() {
        return currentSigns.size();
    }
    
    /**
     * Get current signs (read-only)
     */
    public static List<Sign> getCurrentSigns() {
        return new ArrayList<>(currentSigns);
    }
}
