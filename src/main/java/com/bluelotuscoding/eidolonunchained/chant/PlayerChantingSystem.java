package com.bluelotuscoding.eidolonunchained.chant;

import com.mojang.logging.LogUtils;
import elucent.eidolon.api.spells.Sign;
import elucent.eidolon.api.spells.SignSequence;
import elucent.eidolon.registries.Signs;
import elucent.eidolon.network.AttemptCastPacket;
import elucent.eidolon.network.Networking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Player-centered chanting system that uses the player as the focal point for spell casting.
 * Instead of spawning separate entities, all visual effects and progression happen around the player.
 * 
 * Uses EVENT-DRIVEN execution like the ribbon system - NO TICKING for performance!
 * Spells execute after a 1-second delay using scheduled tasks, not continuous polling.
 */
public class PlayerChantingSystem {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Track active chant sequences per player
    private static final Map<UUID, PlayerChant> activeChants = new ConcurrentHashMap<>();
    
    // Visual configuration
    private static final int PARTICLE_COUNT_PER_SIGN = 20;
    private static final double PARTICLE_RADIUS = 2.0;
    private static final long SPELL_RESOLUTION_DELAY = 1000; // 1 second delay before casting
    
    // Scheduler for delayed spell execution (like ribbon system)
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    /**
     * Container for tracking a player's active chanting sequence
     */
    private static class PlayerChant {
        final List<Sign> signs = new ArrayList<>();
        long lastSignTime = System.currentTimeMillis();
        private CompletableFuture<Void> spellExecutionTask = null;
        
        void addSign(Sign sign) {
            signs.add(sign);
            lastSignTime = System.currentTimeMillis();
            // Cancel any pending spell execution when adding new signs
            if (spellExecutionTask != null && !spellExecutionTask.isDone()) {
                spellExecutionTask.cancel(false);
                spellExecutionTask = null;
            }
        }
        
        void scheduleSpellExecution(DatapackChant spell, ServerPlayer player) {
            // Cancel any existing scheduled execution
            if (spellExecutionTask != null && !spellExecutionTask.isDone()) {
                spellExecutionTask.cancel(false);
            }
            
            // Schedule execution after delay (like ribbon system)
            spellExecutionTask = CompletableFuture.runAsync(() -> {
                try {
                    // Execute the datapack chant effects
                    spell.execute(player);
                    
                    // Send success message
                    player.sendSystemMessage(Component.literal("§a✨ " + spell.getName() + " §acompleted!"));
                    
                    // Try to also trigger Eidolon spell casting
                    SignSequence sequence = new SignSequence(signs);
                    try {
                        Level level = player.level();
                        elucent.eidolon.api.spells.Spell eidolonSpell = elucent.eidolon.registries.Spells.find(sequence, level);
                        if (eidolonSpell != null) {
                            // Use player's position for spell casting
                            net.minecraft.core.BlockPos playerPos = player.blockPosition();
                            eidolonSpell.cast(level, playerPos, player, sequence);
                            LOGGER.info("Also triggered Eidolon spell: {}", eidolonSpell.getRegistryName());
                        }
                    } catch (Exception e) {
                        LOGGER.debug("No matching Eidolon spell found or error casting: {}", e.getMessage());
                    }
                    
                    LOGGER.info("Successfully executed spell {} for player {}", 
                        spell.getName(), player.getName().getString());
                        
                } catch (Exception e) {
                    LOGGER.error("Error executing spell {} for player {}: {}", 
                        spell.getName(), player.getName().getString(), e.getMessage());
                } finally {
                    // Clear the chant after execution (success or failure)
                    PlayerChantingSystem.clearPlayerChant(player);
                }
            }, CompletableFuture.delayedExecutor(SPELL_RESOLUTION_DELAY, TimeUnit.MILLISECONDS));
        }
        
        void clear() {
            signs.clear();
            if (spellExecutionTask != null && !spellExecutionTask.isDone()) {
                spellExecutionTask.cancel(false);
                spellExecutionTask = null;
            }
        }
        
        boolean isEmpty() {
            return signs.isEmpty();
        }
    }
    
    /**
     * Add a sign to the player's active chant sequence with immediate visual feedback
     * Uses EVENT-DRIVEN execution like ribbon system - no ticking needed!
     */
    public static void addSignToActiveChant(ServerPlayer player, Sign sign, String signDisplayName) {
        // Get or create active chant for this player
        PlayerChant chant = activeChants.computeIfAbsent(player.getUUID(), k -> new PlayerChant());
        
        // Add the sign to the sequence
        chant.addSign(sign);
        
        // Immediate visual and audio feedback
        spawnSignParticles(player, sign, chant.signs.size());
        playSignSound(player, sign);
        
        // Check for spell completion IMMEDIATELY (like ribbon system)
        checkAndHandleSpellCompletion(player, chant);
        
        LOGGER.info("Player {} added sign {} to chant sequence (total: {})", 
            player.getName().getString(), sign.getRegistryName(), chant.signs.size());
        
        // Send action bar feedback
        player.sendSystemMessage(
            Component.literal("§6" + signDisplayName + " §7(" + chant.signs.size() + " signs)"), 
            true // action bar
        );
    }
    
    /**
     * Check if current sequence matches any complete datapack chant
     * IMMEDIATE execution like ribbon system - no polling needed!
     */
    private static void checkAndHandleSpellCompletion(ServerPlayer player, PlayerChant chant) {
        if (chant.signs.isEmpty()) return;
        
        // Check for valid spell sequences
        List<ResourceLocation> signIds = new ArrayList<>();
        for (Sign sign : chant.signs) {
            signIds.add(sign.getRegistryName());
        }
        
        // Look for matching datapack chants
        for (DatapackChant datapackChant : DatapackChantManager.getAllChantsCollection()) {
            if (datapackChant.getSignSequence().equals(signIds)) {
                // Found exact match! Schedule execution after delay (like ribbon)
                
                // Enhanced completion particles
                spawnCompletionParticles(player);
                
                // Play completion sound
                player.level().playSound(
                    null,
                    player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_LEVELUP,
                    SoundSource.PLAYERS,
                    0.5f, 1.2f
                );
                
                player.sendSystemMessage(
                    Component.literal("§a✓ " + datapackChant.getName() + " §7(casting in 1s...)"), 
                    true
                );
                
                // Schedule spell execution after delay (EVENT-DRIVEN like ribbon)
                chant.scheduleSpellExecution(datapackChant, player);
                
                LOGGER.info("Player {} completed chant: {} - scheduled for execution", 
                    player.getName().getString(), datapackChant.getName());
                break;
            }
        }
    }
    
    /**
     * Spawn immersive particle effects around the player for the new sign
     */
    private static void spawnSignParticles(ServerPlayer player, Sign sign, int signCount) {
        Level level = player.level();
        Vec3 playerPos = player.position();
        
        // Ensure we're on the server side for particles
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }
        
        // Base particles around player
        for (int i = 0; i < PARTICLE_COUNT_PER_SIGN; i++) {
            double angle = (i * 2.0 * Math.PI) / PARTICLE_COUNT_PER_SIGN;
            double radius = PARTICLE_RADIUS * (0.5 + 0.5 * Math.random());
            
            double x = playerPos.x + Math.cos(angle) * radius;
            double y = playerPos.y + 1.0 + Math.random() * 2.0;
            double z = playerPos.z + Math.sin(angle) * radius;
            
            // Enchantment particles for magical effect
            serverLevel.sendParticles(
                ParticleTypes.ENCHANT,
                x, y, z,
                1, // count
                0.1, 0.1, 0.1, // offset
                0.05 // speed
            );
        }
        
        // Rising spiral particles based on sign count (more signs = more dramatic)
        int spiralParticles = Math.min(signCount * 5, 25);
        for (int i = 0; i < spiralParticles; i++) {
            double t = (double) i / spiralParticles;
            double angle = t * 4.0 * Math.PI; // Two full rotations
            double height = t * 3.0; // Rise 3 blocks
            double radius = 1.0 + t * 0.5; // Expanding spiral
            
            double x = playerPos.x + Math.cos(angle) * radius;
            double y = playerPos.y + 0.5 + height;
            double z = playerPos.z + Math.sin(angle) * radius;
            
            // Glow particles for the spiral
            serverLevel.sendParticles(
                ParticleTypes.GLOW,
                x, y, z,
                1, // count
                0.05, 0.05, 0.05, // offset
                0.02 // speed
            );
        }
        
        // Sign-specific colored particles (if we can determine sign color)
        for (int i = 0; i < 10; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 1.5;
            double offsetY = level.random.nextDouble() * 1.5;
            double offsetZ = (level.random.nextDouble() - 0.5) * 1.5;
            
            serverLevel.sendParticles(
                ParticleTypes.END_ROD,
                playerPos.x + offsetX,
                playerPos.y + 1.2 + offsetY,
                playerPos.z + offsetZ,
                1, // count
                0, 0.1, 0, // velocity
                0.1 // speed
            );
        }
    }
    
    /**
     * Play appropriate sound for sign addition
     */
    private static void playSignSound(ServerPlayer player, Sign sign) {
        // Use Eidolon's spell-related sounds if available, otherwise use enchantment sounds
        player.level().playSound(
            null, // played to all players near the position
            player.getX(), player.getY(), player.getZ(),
            SoundEvents.ENCHANTMENT_TABLE_USE,
            SoundSource.PLAYERS,
            0.7f, // volume
            1.0f + (player.level().random.nextFloat() - 0.5f) * 0.4f // pitch variation
        );
    }
    
    /**
     * Spawn dramatic completion particles when spell is ready to cast
     */
    private static void spawnCompletionParticles(ServerPlayer player) {
        Level level = player.level();
        Vec3 playerPos = player.position();
        
        // Ensure we're on the server side for particles
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }
        
        // Burst of golden particles
        for (int i = 0; i < 50; i++) {
            double angle = level.random.nextDouble() * 2.0 * Math.PI;
            double velocity = 0.5 + level.random.nextDouble() * 0.5;
            
            double vx = Math.cos(angle) * velocity;
            double vy = 0.2 + level.random.nextDouble() * 0.3;
            double vz = Math.sin(angle) * velocity;
            
            serverLevel.sendParticles(
                ParticleTypes.FIREWORK,
                playerPos.x, playerPos.y + 1.0, playerPos.z,
                1,
                vx, vy, vz,
                0.1
            );
        }
        
        // Ring of enchantment particles
        for (int i = 0; i < 30; i++) {
            double angle = (i * 2.0 * Math.PI) / 30;
            double radius = 2.5;
            
            double x = playerPos.x + Math.cos(angle) * radius;
            double y = playerPos.y + 0.1;
            double z = playerPos.z + Math.sin(angle) * radius;
            
            serverLevel.sendParticles(
                ParticleTypes.ENCHANT,
                x, y, z,
                1,
                0, 0.5, 0, // upward velocity
                0.1
            );
        }
    }
    
    /**
     * Clear a player's active chant (called when spell is cast or cancelled)
     */
    public static void clearPlayerChant(ServerPlayer player) {
        PlayerChant chant = activeChants.remove(player.getUUID());
        if (chant != null) {
            chant.clear(); // This will cancel any pending execution
            LOGGER.info("Cleared chant sequence for player {}", player.getName().getString());
        }
    }
    
    /**
     * Get current chant sequence for a player (for UI display)
     */
    public static List<Sign> getPlayerChantSigns(UUID playerId) {
        PlayerChant chant = activeChants.get(playerId);
        return chant != null ? new ArrayList<>(chant.signs) : new ArrayList<>();
    }
    
    /**
     * Cleanup method for server shutdown
     */
    public static void shutdown() {
        // Cancel all pending executions
        for (PlayerChant chant : activeChants.values()) {
            chant.clear();
        }
        activeChants.clear();
        scheduler.shutdown();
    }
}
