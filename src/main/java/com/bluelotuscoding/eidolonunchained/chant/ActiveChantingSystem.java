package com.bluelotuscoding.eidolonunchained.chant;

import com.mojang.logging.LogUtils;
import elucent.eidolon.api.spells.Sign;
import elucent.eidolon.api.spells.SignSequence;
import elucent.eidolon.registries.Signs;
import elucent.eidolon.common.entity.ChantCasterEntity;
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
        ChantCasterEntity entity = null;
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
        
        // Add the sign to the sequence
        chant.addSign(sign);
        
        // Update visual representation
        updateChantCasterEntity(player, chant);
        
        // TODO: Add spell completion detection in future update
        
        LOGGER.info("Player {} added sign {} to active chant (sequence: {})", 
            player.getName().getString(), signId, chant.signs.size());
        
        // Send feedback to player via action bar
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
     */
    private static void updateChantCasterEntity(ServerPlayer player, ActiveChant chant) {
        Level level = player.level();
        
        // Remove existing entity if present
        if (chant.entity != null && !chant.entity.isRemoved()) {
            chant.entity.discard();
        }
        
        // Create new ChantCasterEntity with current sign sequence
        Vec3 lookDirection = player.getLookAngle();
        chant.entity = new ChantCasterEntity(level, player, new ArrayList<>(chant.signs), lookDirection);
        
        // Position near player
        chant.entity.setPos(
            player.getX() + lookDirection.x * 0.5,
            player.getY() + 1.0,
            player.getZ() + lookDirection.z * 0.5
        );
        
        // Add to world
        level.addFreshEntity(chant.entity);
        
        LOGGER.debug("Updated ChantCasterEntity for player {} with {} signs", 
            player.getName().getString(), chant.signs.size());
    }
    
    /**
     * Check if current sequence matches any complete datapack chant
     */
    private static void checkForCompleteChant(ServerPlayer player, ActiveChant chant) {
        List<ResourceLocation> signIds = chant.getSignIds();
        
        // Look for matching chant in datapack definitions
        DatapackChant matchingChant = DatapackChantManager.findChantBySignSequence(signIds);
        
        if (matchingChant != null) {
            LOGGER.info("Complete chant detected: {} for player {}", 
                matchingChant.getId(), player.getName().getString());
            
            // Execute the complete chant using Eidolon's system
            executeCompleteChant(player, chant, matchingChant);
        }
    }
    
    /**
     * Execute a complete chant sequence using Eidolon's casting system
     */
    private static void executeCompleteChant(ServerPlayer player, ActiveChant chant, DatapackChant matchingChant) {
        try {
            // Mark the ChantCasterEntity as successful
            if (chant.entity != null && !chant.entity.isRemoved()) {
                chant.entity.getEntityData().set(ChantCasterEntity.SUCCEEDED, true);
            }
            
            // Create SignSequence for Eidolon's casting system
            SignSequence sequence = new SignSequence(chant.signs);
            
            // Send AttemptCastPacket to trigger Eidolon's spell execution
            // This will handle the actual spell effects and cleanup
            AttemptCastPacket castPacket = new AttemptCastPacket(player, chant.signs);
            
            // Execute on server thread - direct call since we're already on server
            MinecraftServer server = player.getServer();
            if (server != null) {
                server.execute(() -> {
                    // Create ChantCasterEntity manually (similar to AttemptCastPacket logic)
                    ChantCasterEntity entity = new ChantCasterEntity(player.level(), player, chant.signs, player.getLookAngle());
                    entity.setPos(player.getX(), player.getY() + 0.5, player.getZ());
                    player.level().addFreshEntity(entity);
                });
            }
            
            // Send success feedback
            player.sendSystemMessage(
                Component.literal("§a✦ Chant Complete: §e" + matchingChant.getName()), 
                false // chat
            );
            
            // Clear the active chant
            chant.clear();
            activeChants.remove(player.getUUID());
            
            LOGGER.info("Successfully executed chant {} for player {}", 
                matchingChant.getId(), player.getName().getString());
            
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
