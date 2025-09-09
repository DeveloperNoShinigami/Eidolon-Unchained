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
        private boolean validSpellDetected = false;
        private long spellValidationTime = 0;
        private DatapackChant pendingSpell = null;
        private static final long SPELL_RESOLUTION_DELAY = 1000; // 1 second like ribbon
        
        void addSign(Sign sign) {
            signs.add(sign);
            lastSignTime = System.currentTimeMillis();
            // Reset validation when adding new signs (still building)
            validSpellDetected = false;
            pendingSpell = null;
        }
        
        void markValidSpellDetected(DatapackChant spell) {
            validSpellDetected = true;
            spellValidationTime = System.currentTimeMillis();
            pendingSpell = spell;
        }
        
        boolean shouldResolveSpell() {
            if (!validSpellDetected || pendingSpell == null) return false;
            
            // Resolve after delay (like ribbon system)
            long timeSinceValidation = System.currentTimeMillis() - spellValidationTime;
            return timeSinceValidation >= SPELL_RESOLUTION_DELAY;
        }
        
        DatapackChant getPendingSpell() {
            return pendingSpell;
        }
        
        void clear() {
            signs.clear();
            if (entity != null && !entity.isRemoved()) {
                entity.discard();
            }
            entity = null;
            validSpellDetected = false;
            pendingSpell = null;
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
     * Add a sign to the player's active chant sequence using player-centered approach
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
        
        // OPTION 1: CURRENT - Use PlayerChantingSystem (player-centered with custom renderer)
        PlayerChantingSystem.addSignToActiveChant(player, sign, assignment.displayName);
        
        /* OPTION 2: REVERT TO ENTITY-BASED - Uncomment this block and comment out PlayerChantingSystem call above
        // Original entity-based approach using ChantCasterEntity
        ActiveChant chant = activeChants.computeIfAbsent(player.getUUID(), k -> new ActiveChant());
        chant.addSign(sign);
        
        // Update ChantCasterEntity to show current sequence
        updateChantCasterEntity(player, chant);
        
        // Check for complete chant after adding sign
        checkForCompleteChant(player, chant);
        
        player.sendSystemMessage(
            Component.literal("§6" + assignment.displayName + " §7(" + chant.signs.size() + " signs)"), 
            true // action bar
        );
        */
        
        LOGGER.info("Player {} added sign {} using player-centered chanting system", 
            player.getName().getString(), signId);
    }
    
    /**
     * Clear the player's active chant sequence
     */
    public static void clearActiveChant(ServerPlayer player) {
        // OPTION 1: CURRENT - Use PlayerChantingSystem
        PlayerChantingSystem.clearPlayerChant(player);
        
        /* OPTION 2: REVERT TO ENTITY-BASED - Uncomment this block and comment out PlayerChantingSystem call above
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
        */
    }

    /* 
    // ===== ENTITY-BASED CHANTING METHODS (COMMENTED OUT FOR OPTION 1) =====
    // REVERT INSTRUCTIONS: To go back to entity-based chanting:
    // 1. Uncomment this entire block
    // 2. Comment out PlayerChantingSystem calls in addSignToChant() and clearActiveChant() 
    // 3. Uncomment the ActiveChant usage in addSignToChant()
    // 4. Delete/disable PlayerChantCasterRenderer
    
    /**
     * Update the ChantCasterEntity to show current sign sequence
     * 🎯 FIXED: Update existing entity instead of constantly recreating it
     *//*
    private static void updateChantCasterEntity(ServerPlayer player, ActiveChant chant) {
        Level level = player.level();
        Vec3 lookDirection = player.getLookAngle();
        
        if (chant.entity == null || chant.entity.isRemoved()) {
            // Create new ChantCasterEntity only if we don't have one
            chant.entity = new ChantCasterEntity(level, player, new ArrayList<>(chant.signs), lookDirection);
            
            // Position near player for visibility
            chant.entity.setPos(
                player.getX() + lookDirection.x * 0.3,
                player.getY() + 1.2,
                player.getZ() + lookDirection.z * 0.3
            );
            
            // Spawn entity
            level.addFreshEntity(chant.entity);
            
            LOGGER.info("ENTITY: Created new ChantCasterEntity for player {} with {} signs", 
                player.getName().getString(), chant.signs.size());
        } else {
            // Update existing entity with new sign sequence
            chant.entity.setChantTag(new ArrayList<>(chant.signs));
            
            // Update position to follow player
            chant.entity.setPos(
                player.getX() + lookDirection.x * 0.3,
                player.getY() + 1.2,
                player.getZ() + lookDirection.z * 0.3
            );
            
            LOGGER.info("ENTITY: Updated existing ChantCasterEntity for player {} with {} signs", 
                player.getName().getString(), chant.signs.size());
        }
    }*/
    
    /*
    /**
     * Check if current sequence matches any complete datapack chant (like ribbon validation)
     * COMMENTED OUT FOR OPTION 1 - PlayerChantingSystem handles this
     *//*
    private static void checkForCompleteChant(ServerPlayer player, ActiveChant chant) {
        List<ResourceLocation> signIds = chant.getSignIds();
        
        // Look for exact matching chant in datapack definitions
        DatapackChant matchingChant = DatapackChantManager.findChantBySignSequence(signIds);
        
        if (matchingChant != null) {
            LOGGER.info("Valid spell detected: {} for player {} - starting resolution delay", 
                matchingChant.getId(), player.getName().getString());
            
            // 🎯 RIBBON PATTERN: Mark valid spell detected, start delay
            chant.markValidSpellDetected(matchingChant);
            
            // Entity stays visible during delay (like ribbon)
            LOGGER.info("Entity will resolve spell after 1 second delay (like ribbon system)");
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
    }*/
    
    /*
    // OPTION 2: Entity-Based Execution (Commented for testing Option 1 - Player-Centered)
    // Execute a complete chant sequence using Eidolon's casting system
    // 🔥 FIXED: Proper Eidolon spell execution that actually casts the spell
    */
    private static void executeCompleteChant(ServerPlayer player, ActiveChant chant, DatapackChant matchingChant) {
        /*try {
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
        }*/
        
        // OPTION 1: Player-Centered Execution (Current Active Implementation)
        // Use PlayerChantingSystem for actual spell execution with proper AI deity communication
        PlayerChantingSystem.executeChantFromSequence(player, matchingChant.getSignSequence());
        
        // Clear the active chant after delegation to PlayerChantingSystem
        clearActiveChant(player);
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
     * Clean up old/abandoned chant sequences and handle spell checking using player-centered system
     */
    public static void tick(MinecraftServer server) {
        // NO TICKING NEEDED: Delegate to player-centered system which uses event-driven execution
        // PlayerChantingSystem uses CompletableFuture.delayedExecutor for spell delays like ribbon system
        
        // Legacy cleanup for any remaining ActiveChant entries (transition period)
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - lastCleanupTime < CLEANUP_INTERVAL) {
            return; // Too early for cleanup cycle
        }
        lastCleanupTime = currentTime;
        
        List<UUID> playersToRemove = new ArrayList<>();
        
        for (Map.Entry<UUID, ActiveChant> entry : activeChants.entrySet()) {
            UUID playerId = entry.getKey();
            ActiveChant chant = entry.getValue();
            
            // Clean up old entries - player-centered system handles active chanting now
            if (currentTime - chant.lastSignTime > 30000) { // 30 second timeout
                playersToRemove.add(playerId);
            }
        }
        
        // Clean up legacy entries
        for (UUID playerId : playersToRemove) {
            ActiveChant chant = activeChants.remove(playerId);
            if (chant != null) {
                chant.clear(); // Clean up any remaining entities
                LOGGER.info("Cleaned up legacy ActiveChant for player {}", playerId);
            }
        }
    }    /**
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
     * Check if player has an active chant in progress (updated for player-centered system)
     */
    public static boolean hasActiveChant(Player player) {
        // Check new system first
        List<Sign> signs = PlayerChantingSystem.getPlayerChantSigns(player.getUUID());
        if (!signs.isEmpty()) {
            return true;
        }
        
        // Check legacy system as fallback
        ActiveChant chant = activeChants.get(player.getUUID());
        return chant != null && !chant.isEmpty();
    }
    
    /**
     * Clear a player's active chant sequence (delegated to player-centered system)
     */
    public static void clearPlayerChant(ServerPlayer player) {
        PlayerChantingSystem.clearPlayerChant(player);
        
        // Also clear legacy system if present
        ActiveChant legacyChant = activeChants.remove(player.getUUID());
        if (legacyChant != null) {
            legacyChant.clear();
            LOGGER.info("Cleared legacy chant for player {}", player.getName().getString());
        }
    }
    
    /**
     * Get current chant signs for a player (for UI display)
     */
    public static List<Sign> getPlayerChantSigns(UUID playerId) {
        return PlayerChantingSystem.getPlayerChantSigns(playerId);
    }
}
