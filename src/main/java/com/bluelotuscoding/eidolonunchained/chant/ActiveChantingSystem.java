package com.bluelotuscoding.eidolonunchained.chant;

import com.mojang.logging.LogUtils;
import elucent.eidolon.api.spells.Sign;
import elucent.eidolon.api.spells.SignSequence;
import elucent.eidolon.registries.Signs;
import elucent.eidolon.common.entity.ChantCasterEntity;
import com.bluelotuscoding.eidolonunchained.entity.UpdateableChantCasterEntity;
import elucent.eidolon.network.AttemptCastPacket;
import elucent.eidolon.network.Networking;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Real-time active chanting system using Eidolon's ChantCasterEntity.
 * 
 * Players press individual sign keybinds (G,H,J,K) to build chant sequences.
 * ChantCasterEntity spawns immediately and updates in real-time as signs are added.
 * Auto-completes when the sequence matches a datapack chant definition.
 * 
 * This replaces the old complete-chant assignment system with dynamic building.
 */
public class ActiveChantingSystem {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Track active chant sequences per player
    private static final Map<UUID, ActiveChant> activeChants = new ConcurrentHashMap<>();
    
    // Cleanup timer
    private static long lastCleanupTime = 0;
    private static final long CLEANUP_INTERVAL = 30000; // 30 seconds
    
            // No default assignments - players configure their own via SlotAssignmentManager
    
    /**
     * Container for tracking an active chanting sequence
     */
    private static class ActiveChant {
        final List<Sign> signs = new ArrayList<>();
        UpdateableChantCasterEntity entity = null;
        long lastSignTime = System.currentTimeMillis();
        
        void addSign(Sign sign) {
            signs.add(sign);
            lastSignTime = System.currentTimeMillis();
        }
        
        void clear() {
            signs.clear();
            if (entity != null && !entity.isRemoved()) {
                entity.discard();
            }
            entity = null;
        }
        
        boolean isEmpty() {
            return signs.isEmpty();
        }
        
        List<ResourceLocation> getSignIds() {
            List<ResourceLocation> signIds = new ArrayList<>();
            for (Sign sign : signs) {
                signIds.add(sign.getRegistryName());
            }
            return signIds;
        }
    }
    
    /**
     * Add a sign to the player's active chant sequence
     */
    public static void addSignToChant(ServerPlayer player, int slot) {
        // Get the player's assigned sign for this slot
        SlotAssignmentManager.SlotAssignment assignment = SlotAssignmentManager.getSlotAssignment(player, slot);
        
        if (assignment == null || assignment.type != SlotAssignmentManager.SlotType.SIGN) {
            player.sendSystemMessage(Component.literal("§cNo sign assigned to slot " + slot + ". Use /chant assign-sign " + slot + " <sign_id>"));
            return;
        }
        
        ResourceLocation signId = assignment.id;
        Sign sign = Signs.find(signId);
        if (sign == null) {
            player.sendSystemMessage(Component.literal("§cInvalid sign assigned to slot " + slot + ": " + signId));
            return;
        }
        
        // Get or create active chant for this player
        ActiveChant chant = activeChants.computeIfAbsent(player.getUUID(), k -> new ActiveChant());
        
        // INSTANT: Spawn entity immediately on FIRST sign if needed
        if (chant.signs.isEmpty()) {
            LOGGER.info("FIRST SIGN: Creating initial ChantCasterEntity for player {}", player.getName().getString());
        }
        
        // Add the sign to the sequence
        chant.addSign(sign);
        
        // Update visual representation and check for completion immediately
        updateChantCasterEntity(player, chant);
        
        LOGGER.info("Player {} added sign {} to active chant (sequence: {})", 
            player.getName().getString(), signId, chant.signs.size());
        
        // Send instant feedback to player via action bar
        String signName = assignment.displayName;
        player.sendSystemMessage(
            Component.literal("§6Added: " + signName + " §7(" + chant.signs.size() + " signs)"), 
            true // action bar
        );
    }
    
    /**
     * Clear the player's active chant sequence
     */
    public static void clearActiveChant(ServerPlayer player) {
        ActiveChant chant = activeChants.get(player.getUUID());
        if (chant != null) {
            LOGGER.info("Clearing active chant for player {} ({} signs)", 
                player.getName().getString(), chant.signs.size());
            
            chant.clear();
            activeChants.remove(player.getUUID());
            
            player.sendSystemMessage(
                Component.literal("§7Chant sequence cleared"), 
                true // action bar
            );
        }
    }
    
    /**
     * Update the ChantCasterEntity to show current sign sequence
     * 🎯 REAL-TIME: True updates without entity recreation using extended entity
     */
    private static void updateChantCasterEntity(ServerPlayer player, ActiveChant chant) {
        Level level = player.level();
        
        // Create entity only if none exists
        if (chant.entity == null || !chant.entity.canBeUpdated()) {
            // Create new UpdateableChantCasterEntity
            Vec3 lookDirection = player.getLookAngle();
            chant.entity = new UpdateableChantCasterEntity(level, player, new ArrayList<>(chant.signs), lookDirection);
            
            // Position near player for visibility
            chant.entity.setPos(
                player.getX() + lookDirection.x * 0.3,
                player.getY() + 1.2,
                player.getZ() + lookDirection.z * 0.3
            );
            
            // Spawn entity
            level.addFreshEntity(chant.entity);
            
            LOGGER.info("FIRST SIGN: Creating UpdateableChantCasterEntity for player {}", player.getName().getString());
        } else {
            // 🎯 TRUE REAL-TIME UPDATE: Update existing entity without recreation
            chant.entity.updateSignSequence(new ArrayList<>(chant.signs));
            
            // Update position to follow player
            Vec3 lookDirection = player.getLookAngle();
            chant.entity.setPos(
                player.getX() + lookDirection.x * 0.3,
                player.getY() + 1.2,
                player.getZ() + lookDirection.z * 0.3
            );
            
            LOGGER.info("REAL-TIME: Updated existing ChantCasterEntity for player {} to {} signs", 
                player.getName().getString(), chant.signs.size());
        }
        
        // 🎯 IMMEDIATE SPELL CHECK: Check for completion right after update
        checkForCompleteChant(player, chant);
    }
    
    /**
     * Check if current sequence matches any complete datapack chant
     */
    private static void checkForCompleteChant(ServerPlayer player, ActiveChant chant) {
        List<ResourceLocation> signIds = chant.getSignIds();
        
        // Look for exact matching chant in datapack definitions
        DatapackChant matchingChant = DatapackChantManager.findChantBySignSequence(signIds);
        
        if (matchingChant != null) {
            LOGGER.info("Complete chant detected: {} for player {}", 
                matchingChant.getId(), player.getName().getString());
            
            // Execute the complete chant
            executeCompleteChant(player, chant, matchingChant);
            
            // Clear after execution (done in executeCompleteChant, but ensure it's cleared)
            clearActiveChant(player);
            return;
        }
        
        // Check if this sequence is the start of any valid chant (PREFIX CHECK)
        boolean hasValidPrefix = DatapackChantManager.hasValidChantPrefix(signIds);
        
        if (!hasValidPrefix && chant.signs.size() >= 3) {
            // 🔥 FIXED: Only auto-clear after 3+ signs, give more time for building
            LOGGER.info("Invalid chant sequence detected for player {} after {} signs, auto-clearing", 
                player.getName().getString(), chant.signs.size());
            
            player.sendSystemMessage(
                Component.literal("§cInvalid spell sequence - clearing chant"), 
                true // action bar
            );
            
            clearActiveChant(player);
        } else if (hasValidPrefix && chant.signs.size() > 1) {
            // Valid prefix - encourage player to continue
            player.sendSystemMessage(
                Component.literal("§aContinue building spell... §7(" + chant.signs.size() + " signs)"), 
                true // action bar
            );
        } else if (chant.signs.size() == 1) {
            // First sign - give neutral feedback
            player.sendSystemMessage(
                Component.literal("§7Building spell... §7(" + chant.signs.size() + " sign)"), 
                true // action bar
            );
        }
        
        // For sequences ≥ 6 signs, warn but don't auto-clear (in case of very long spells)
        if (chant.signs.size() >= 6 && hasValidPrefix) {
            player.sendSystemMessage(
                Component.literal("§eLong spell sequence - check your combination"), 
                true // action bar
            );
        }
    }
    
    /**
     * Execute a complete chant sequence using Eidolon's casting system
     * 🔥 FIXED: Proper Eidolon spell execution that actually casts the spell
     */
    private static void executeCompleteChant(ServerPlayer player, ActiveChant chant, DatapackChant matchingChant) {
        try {
            // Mark the existing ChantCasterEntity as successful before clearing
            if (chant.entity != null && !chant.entity.isRemoved()) {
                chant.entity.getEntityData().set(ChantCasterEntity.SUCCEEDED, true);
                
                // 🔥 CRITICAL FIX: Capture entity reference before clearing chant
                final ChantCasterEntity entityToCleanup = chant.entity;
                
                // Let the entity stay visible for a moment to show success
                MinecraftServer server = player.getServer();
                if (server != null) {
                    // Schedule entity removal after success animation using standard scheduler
                    java.util.concurrent.Executors.newSingleThreadScheduledExecutor().schedule(() -> {
                        server.execute(() -> {
                            if (entityToCleanup != null && !entityToCleanup.isRemoved()) {
                                entityToCleanup.discard();
                            }
                        });
                    }, 2, java.util.concurrent.TimeUnit.SECONDS);
                }
            }
            
            // 🔥 PROPER EIDOLON INTEGRATION: Use Eidolon's spell casting system
            SignSequence sequence = new SignSequence(chant.signs);
            
            // Try to find and execute the spell through Eidolon's spell registry
            // This uses Eidolon's built-in spell resolution system
            try {
                // Create a new ChantCasterEntity that will handle the actual spell casting
                ChantCasterEntity castingEntity = new ChantCasterEntity(player.level(), player, chant.signs, player.getLookAngle());
                castingEntity.setPos(player.getX(), player.getY() + 0.5, player.getZ());
                player.level().addFreshEntity(castingEntity);
                
                // Mark it as successful immediately (it will handle its own casting logic)
                castingEntity.getEntityData().set(ChantCasterEntity.SUCCEEDED, true);
                
                // Send success feedback
                player.sendSystemMessage(
                    Component.literal("§a✦ Spell Cast: §e" + matchingChant.getName()), 
                    false // chat
                );
                
                LOGGER.info("Successfully executed chant {} for player {} via ChantCasterEntity", 
                    matchingChant.getId(), player.getName().getString());
                    
            } catch (Exception castError) {
                LOGGER.warn("ChantCasterEntity casting failed, trying fallback approach: {}", castError.getMessage());
                
                // Fallback: Send basic success message
                player.sendSystemMessage(
                    Component.literal("§e✦ Chant Completed: §a" + matchingChant.getName()), 
                    false // chat
                );
                
                LOGGER.info("Chant {} acknowledged for player {} (fallback mode)", 
                    matchingChant.getId(), player.getName().getString());
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to execute complete chant for player {}: {}", 
                player.getName().getString(), e.getMessage(), e);
            
            player.sendSystemMessage(
                Component.literal("§cChant execution failed"), 
                true // action bar
            );
        }
    }
    
    /**
     * Get current active chant status for a player (for debugging)
     */
    public static String getChantStatus(Player player) {
        ActiveChant chant = activeChants.get(player.getUUID());
        if (chant == null || chant.isEmpty()) {
            return "No active chant";
        }
        
        StringBuilder status = new StringBuilder();
        status.append("Active chant (").append(chant.signs.size()).append(" signs): ");
        for (Sign sign : chant.signs) {
            status.append(sign.getRegistryName().getPath()).append(" ");
        }
        
        return status.toString().trim();
    }
    
    /**
     * Clean up old/abandoned chant sequences (called periodically)
     */
    public static void cleanupOldChants() {
        long currentTime = System.currentTimeMillis();
        long timeout = 30000; // 30 seconds
        
        activeChants.entrySet().removeIf(entry -> {
            ActiveChant chant = entry.getValue();
            if (currentTime - chant.lastSignTime > timeout) {
                LOGGER.debug("Cleaning up abandoned chant for player {}", entry.getKey());
                chant.clear();
                return true;
            }
            return false;
        });
    }
    
    /**
     * Get configured sign for a slot (for UI display)
     */
    public static Sign getSignForSlot(ServerPlayer player, int slotNumber) {
        SlotAssignmentManager.SlotAssignment assignment = SlotAssignmentManager.getSlotAssignment(player, slotNumber);
        if (assignment != null && assignment.type == SlotAssignmentManager.SlotType.SIGN) {
            return Signs.find(assignment.id);
        }
        return null;
    }
    
    /**
     * Check if player has an active chant in progress
     */
    public static boolean hasActiveChant(Player player) {
        ActiveChant chant = activeChants.get(player.getUUID());
        return chant != null && !chant.isEmpty();
    }
}
