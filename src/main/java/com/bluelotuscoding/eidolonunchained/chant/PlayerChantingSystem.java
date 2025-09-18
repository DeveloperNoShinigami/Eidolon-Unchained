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
import net.minecraft.core.BlockPos;
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
    private static final transient ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private static volatile boolean warnedEmptySpellRegistry = false;
    
    /**
     * Container for tracking a player's active chanting sequence
     */
    private static class PlayerChant {
        final List<Sign> signs = new ArrayList<>();
        long lastSignTime = System.currentTimeMillis();
        long startTime = System.currentTimeMillis();
        private CompletableFuture<Void> spellExecutionTask = null;
        
        void addSign(Sign sign) {
            if (signs.isEmpty()) {
                startTime = System.currentTimeMillis();
            }
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
                // Execute on server thread to ensure proper effigy synchronization
                net.minecraft.server.MinecraftServer server = player.getServer();
                if (server != null) {
                    server.execute(() -> {
                        try {
                            // CRITICAL FIX: Call the Eidolon spell's cast() method, not DatapackChant.execute()!
                            // The cast() method contains the AI deity communication logic
                            DatapackChantSpell eidolonSpell = DatapackChantManager.getSpellForChant(spell.getId());
                            if (eidolonSpell != null) {
                                // Call the spell's cast method with player's position
                                BlockPos playerPos = player.blockPosition();
                                eidolonSpell.cast(player.level(), playerPos, player);
                                
                                LOGGER.info("Successfully cast spell {} for player {} with AI deity communication", 
                                    spell.getName(), player.getName().getString());
                            } else {
                                LOGGER.warn("No Eidolon spell found for chant {}, falling back to direct execution", spell.getId());
                                // Fallback to direct execution if spell not found
                                spell.execute(player);
                            }
                                
                        } catch (Exception e) {
                            LOGGER.error("Error executing spell {} for player {}: {}", 
                                spell.getName(), player.getName().getString(), e.getMessage());
                        } finally {
                            // Clear the chant after execution (success or failure)
                            PlayerChantingSystem.clearPlayerChant(player);
                        }
                    });
                }
            }, CompletableFuture.delayedExecutor(SPELL_RESOLUTION_DELAY, TimeUnit.MILLISECONDS));
        }
        
        void scheduleEidolonSpellExecution(elucent.eidolon.api.spells.Spell eidolonSpell, ServerPlayer player, SignSequence sequence) {
            // Cancel any existing scheduled execution
            if (spellExecutionTask != null && !spellExecutionTask.isDone()) {
                spellExecutionTask.cancel(false);
            }
            
            // Schedule NATIVE EIDOLON spell execution after delay
            spellExecutionTask = CompletableFuture.runAsync(() -> {
                // Ensure execution happens on the main server thread
                net.minecraft.server.MinecraftServer server = player.getServer();
                if (server != null) {
                    server.execute(() -> {
                        try {
                            // Execute native Eidolon spell using their casting system
                            BlockPos playerPos = player.blockPosition();
                            eidolonSpell.cast(player.level(), playerPos, player, sequence);

                            LOGGER.info("Successfully cast Eidolon spell {} for player {}",
                                eidolonSpell.getRegistryName(), player.getName().getString());
                        } catch (Exception e) {
                            LOGGER.error("Error executing Eidolon spell {} for player {}: {}",
                                eidolonSpell.getRegistryName(), player.getName().getString(), e.getMessage());
                        } finally {
                            // Clear the chant after execution (success or failure)
                            PlayerChantingSystem.clearPlayerChant(player);
                        }
                    });
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
        
        // Immediate audio feedback for keybind presses
        playSignSound(player, sign);
        
        // Run configurable sign effects (datapack-driven) instead of built-in visuals/sounds
        try {
            // Determine if current input matches the prefix of any known chant
            ResourceLocation matchedChant = findMatchingChantPrefix(chant.signs);
            int signIndex = Math.max(0, chant.signs.size() - 1);
            KeybindSignEffectsManager.runSignEffects(player, sign.getRegistryName(), signIndex, matchedChant);
        } catch (Exception ignored) {}
        
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

    // Determine if current sequence matches the prefix of any datapack chant
    private static ResourceLocation findMatchingChantPrefix(java.util.List<Sign> signs) {
        if (signs == null || signs.isEmpty()) return null;
        java.util.List<ResourceLocation> current = new java.util.ArrayList<>();
        for (Sign s : signs) current.add(s.getRegistryName());
        for (DatapackChant chant : DatapackChantManager.getAllChantsCollection()) {
            java.util.List<ResourceLocation> seq = chant.getSignSequence();
            if (current.size() <= seq.size()) {
                boolean prefix = true;
                for (int i = 0; i < current.size(); i++) {
                    if (!current.get(i).equals(seq.get(i))) { prefix = false; break; }
                }
                if (prefix) return chant.getId();
            }
        }
        return null;
    }
    
    /**
     * Check if current sequence matches any complete datapack chant OR Eidolon spell
     * IMMEDIATE execution like ribbon system - no polling needed!
     */
    private static void checkAndHandleSpellCompletion(ServerPlayer player, PlayerChant chant) {
        if (chant.signs.isEmpty()) return;
        
        // Check for valid spell sequences
        List<ResourceLocation> signIds = new ArrayList<>();
        for (Sign sign : chant.signs) {
            signIds.add(sign.getRegistryName());
        }
        
        // FIRST: Check for Eidolon native spells
        try {
            SignSequence eidolonSequence = new SignSequence(chant.signs);
            elucent.eidolon.api.spells.Spell eidolonSpell = elucent.eidolon.registries.Spells.find(eidolonSequence, player.level());

            // Fallback: some Eidolon builds don't seed the internal 'spells' cache; scan the spellMap directly
            if (eidolonSpell == null) {
                try {
                    // First scan the map values
                    for (elucent.eidolon.api.spells.Spell s : elucent.eidolon.registries.Spells.getSpellMap().values()) {
                        if (s != null && s.matches(eidolonSequence)) {
                            eidolonSpell = s;
                            LOGGER.debug("Resolved Eidolon spell via spellMap fallback: {}", s.getRegistryName());
                            break;
                        }
                    }
                    // If still not found, also scan the registered spells list as a safety net
                    if (eidolonSpell == null) {
                        for (elucent.eidolon.api.spells.Spell s : elucent.eidolon.registries.Spells.getSpells()) {
                            if (s != null && s.matches(eidolonSequence)) {
                                eidolonSpell = s;
                                LOGGER.debug("Resolved Eidolon spell via spells list fallback: {}", s.getRegistryName());
                                break;
                            }
                        }
                    }
                    // One-time diagnostic if registry appears empty
                    if (eidolonSpell == null
                        && !warnedEmptySpellRegistry
                        && elucent.eidolon.registries.Spells.getSpellMap().isEmpty()
                        && elucent.eidolon.registries.Spells.getSpells().isEmpty()) {
                        warnedEmptySpellRegistry = true;
                        LOGGER.warn("Eidolon spell registry is empty when resolving chants. Check loading order and client sync.");
                    }
                } catch (Throwable t) {
                    LOGGER.debug("Eidolon spellMap fallback error: {}", t.getMessage());
                }
            }

            if (eidolonSpell != null) {
                // Found Eidolon spell! Execute it using Eidolon's system
                
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
                
                // Show casting message
                String spellName = eidolonSpell.getRegistryName().getPath().replace("_", " ");
                spellName = spellName.substring(0, 1).toUpperCase() + spellName.substring(1);
                
                player.sendSystemMessage(
                    Component.literal("§d✓ " + spellName + " §7(Eidolon spell - casting in 1s...)"), 
                    true
                );
                
                // Schedule EIDOLON spell execution after delay
                chant.scheduleEidolonSpellExecution(eidolonSpell, player, eidolonSequence);
                
                LOGGER.info("Player {} completed Eidolon spell: {} - scheduled for execution", 
                    player.getName().getString(), eidolonSpell.getRegistryName());
                return; // Found match, done
            }
        } catch (Exception e) {
            LOGGER.debug("Error checking Eidolon spells: {}", e.getMessage());
        }
        
        // SECOND: Look for matching datapack chants  
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
                
                // Show casting message with fixed translation
                String translatedName = datapackChant.getName();
                if (translatedName.startsWith("eidolonunchained.chant.")) {
                    // Extract readable name from translation key  
                    String[] parts = translatedName.split("\\.");
                    if (parts.length >= 3) {
                        translatedName = parts[2].replace("_", " ");
                        translatedName = translatedName.substring(0, 1).toUpperCase() + translatedName.substring(1);
                    }
                }
                player.sendSystemMessage(
                    Component.literal("§a✓ " + translatedName + " §7(casting in 1s...)"), 
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
        Vec3 look = player.getLookAngle();
        double forward = com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig.COMMON.chantVisualForwardOffset.get();
        double vertical = com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig.COMMON.chantVisualVerticalBase.get();
        Vec3 center = new Vec3(
            playerPos.x + look.x * forward,
            playerPos.y + vertical,
            playerPos.z + look.z * forward
        );
        
        // Ensure we're on the server side for particles
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }
        
        // Base particles around chant center (in front of player)
        for (int i = 0; i < PARTICLE_COUNT_PER_SIGN; i++) {
            double angle = (i * 2.0 * Math.PI) / PARTICLE_COUNT_PER_SIGN;
            double radius = PARTICLE_RADIUS * (0.5 + 0.5 * Math.random());
            
            double x = center.x + Math.cos(angle) * radius;
            double y = center.y + (Math.random() * 1.5 - 0.25);
            double z = center.z + Math.sin(angle) * radius;
            
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
            
            double x = center.x + Math.cos(angle) * radius;
            double y = (center.y - 0.3) + height;
            double z = center.z + Math.sin(angle) * radius;
            
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
            double offsetX = (level.random.nextDouble() - 0.5) * 1.2;
            double offsetY = (level.random.nextDouble() - 0.3) * 1.2;
            double offsetZ = (level.random.nextDouble() - 0.5) * 1.2;
            
            serverLevel.sendParticles(
                ParticleTypes.END_ROD,
                center.x + offsetX,
                center.y + offsetY,
                center.z + offsetZ,
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
        // Prefer Eidolon's SELECT_RUNE sound if available; fallback to vanilla enchantment sound
        try {
            player.level().playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                elucent.eidolon.registries.EidolonSounds.SELECT_RUNE.get(),
                SoundSource.PLAYERS,
                0.6f,
                0.9f + (player.level().random.nextFloat() * 0.2f)
            );
        } catch (Throwable t) {
            player.level().playSound(
                null, // played to all players near the position
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.PLAYERS,
                0.7f, // volume
                1.0f + (player.level().random.nextFloat() - 0.5f) * 0.4f // pitch variation
            );
        }
    }
    
    /**
     * Spawn dramatic completion particles when spell is ready to cast
     */
    private static void spawnCompletionParticles(ServerPlayer player) {
        Level level = player.level();
        Vec3 playerPos = player.position();
        Vec3 look = player.getLookAngle();
        double forward = com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig.COMMON.chantVisualForwardOffset.get();
        double vertical = com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig.COMMON.chantVisualVerticalBase.get();
        Vec3 center = new Vec3(
            playerPos.x + look.x * forward,
            playerPos.y + vertical,
            playerPos.z + look.z * forward
        );
        
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
                center.x, center.y, center.z,
                1,
                vx, vy, vz,
                0.1
            );
        }
        
        // Ring of enchantment particles
        for (int i = 0; i < 30; i++) {
            double angle = (i * 2.0 * Math.PI) / 30;
            double radius = 2.5;
            
            double x = center.x + Math.cos(angle) * radius;
            double y = center.y - 1.1;
            double z = center.z + Math.sin(angle) * radius;
            
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
        List<Sign> result = chant != null ? new ArrayList<>(chant.signs) : new ArrayList<>();
        return result;
    }
    
    /**
     * Get the timing information for progressive rendering
     */
    public static long getChantStartTime(UUID playerId) {
        PlayerChant chant = activeChants.get(playerId);
        return chant != null ? chant.startTime : 0;
    }
    
    /**
     * Get how long the chant has been active (for animation timing)
     */
    public static long getChantDuration(UUID playerId) {
        PlayerChant chant = activeChants.get(playerId);
        if (chant == null) return 0;
        return System.currentTimeMillis() - chant.startTime;
    }
    
    /**
     * Check if a player has an active chant sequence
     */
    public static boolean hasActiveChant(UUID playerId) {
        PlayerChant chant = activeChants.get(playerId);
        return chant != null && !chant.isEmpty();
    }
    
    /**
     * Get the current chant status for a player (for debugging)
     */
    public static String getChantStatus(UUID playerId) {
        PlayerChant chant = activeChants.get(playerId);
        if (chant == null || chant.isEmpty()) {
            return "No active chant";
        }
        return chant.signs.size() + " signs in sequence";
    }
    
    /**
     * Execute a complete chant sequence by adding signs and auto-completing
     * Used when ActiveChantingSystem delegates to PlayerChantingSystem
     */
    public static void executeChantFromSequence(ServerPlayer player, List<ResourceLocation> signSequence) {
        // Clear any existing chant first
        clearPlayerChant(player);
        
        // Convert ResourceLocations to Sign objects and add them
        for (ResourceLocation signRL : signSequence) {
            Sign sign = Signs.find(signRL);
            if (sign != null) {
                // Add each sign - this will automatically trigger completion checking
                addSignToActiveChant(player, sign, sign.getRegistryName().getPath());
            } else {
                LOGGER.warn("Could not find sign for ResourceLocation: {}", signRL);
            }
        }
        
        LOGGER.info("Executed complete chant sequence for player {} with {} signs", 
            player.getName().getString(), signSequence.size());
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
