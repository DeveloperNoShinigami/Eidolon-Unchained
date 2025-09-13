package com.bluelotuscoding.eidolonunchained.effects;

import com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager;
import com.bluelotuscoding.eidolonunchained.deity.DatapackDeity;
import com.mojang.logging.LogUtils;
import elucent.eidolon.common.tile.EffigyTileEntity;
import elucent.eidolon.registries.EidolonParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Manages effigy visual effects that mimic Eidolon's ChantCasterEntity behavior
 * Handles dynamic effects during AI deity conversations with pulsing and color customization
 */
@Mod.EventBusSubscriber(modid = "eidolonunchained", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EffigyEffectsManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Track active effigy effects per player
    private static final Map<UUID, ActiveEffigyEffect> activeEffects = new ConcurrentHashMap<>();
    
    /**
     * Represents an active effigy effect for a player's conversation
     */
    public static class ActiveEffigyEffect {
        public final BlockPos effigyPos;
        public final ResourceLocation deityId;
        public final long startTime;
        public EffectState state;
        public int tickCounter = 0;
        public float currentIntensity = 0.0f;
        public float targetIntensity = 1.0f;
        
        public ActiveEffigyEffect(BlockPos effigyPos, ResourceLocation deityId) {
            this.effigyPos = effigyPos;
            this.deityId = deityId;
            this.startTime = System.currentTimeMillis();
            this.state = EffectState.STARTING;
        }
        
        public enum EffectState {
            STARTING,    // Effect is ramping up
            ACTIVE,      // Steady effect during conversation
            PULSING,     // Pulsing during AI response
            ENDING       // Effect is fading out
        }
    }
    
    /**
     * Start effigy effects when AI conversation begins
     */
    public static void startConversationEffects(ServerPlayer player, BlockPos effigyPos, ResourceLocation deityId) {
        UUID playerId = player.getUUID();
        
        // Stop any existing effects for this player
        stopEffects(player);
        
        // Create new active effect
        ActiveEffigyEffect effect = new ActiveEffigyEffect(effigyPos, deityId);
        activeEffects.put(playerId, effect);
        
        // Play initial sound effect (like Eidolon does)
        ServerLevel world = player.serverLevel();
        world.playSound(null, effigyPos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.8f, 1.2f);
        
        LOGGER.info("🔮 Started conversation effigy effects for player {} with deity {} at {}", 
            player.getName().getString(), deityId, effigyPos);
    }
    
    /**
     * Intensify effects when AI starts responding (pulsing mode)
     */
    public static void startAIResponseEffects(ServerPlayer player) {
        UUID playerId = player.getUUID();
        ActiveEffigyEffect effect = activeEffects.get(playerId);
        
        if (effect != null) {
            effect.state = ActiveEffigyEffect.EffectState.PULSING;
            effect.targetIntensity = 1.5f; // Increase intensity for response
            LOGGER.debug("🌊 Started pulsing effects for AI response - player: {}", player.getName().getString());
        }
    }
    
    /**
     * Return to steady effects when AI finishes responding
     */
    public static void endAIResponseEffects(ServerPlayer player) {
        UUID playerId = player.getUUID();
        ActiveEffigyEffect effect = activeEffects.get(playerId);
        
        if (effect != null) {
            effect.state = ActiveEffigyEffect.EffectState.ACTIVE;
            effect.targetIntensity = 1.0f; // Return to normal intensity
            LOGGER.debug("💫 Returned to steady effects after AI response - player: {}", player.getName().getString());
        }
    }
    
    /**
     * Stop all effigy effects when conversation ends
     */
    public static void stopEffects(ServerPlayer player) {
        UUID playerId = player.getUUID();
        ActiveEffigyEffect effect = activeEffects.remove(playerId);
        
        if (effect != null) {
            // Create final completion flash (like ChantCasterEntity does)
            createCompletionFlash(player.serverLevel(), effect.effigyPos, effect.deityId);
            LOGGER.info("✨ Stopped conversation effigy effects for player {}", player.getName().getString());
        }
    }
    
    /**
     * Server tick handler for updating effigy effects
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        // Update all active effects
        for (Map.Entry<UUID, ActiveEffigyEffect> entry : activeEffects.entrySet()) {
            UUID playerId = entry.getKey();
            ActiveEffigyEffect effect = entry.getValue();
            
            // Find the player and world
            ServerLevel world = null;
            for (ServerLevel serverWorld : event.getServer().getAllLevels()) {
                ServerPlayer player = serverWorld.getServer().getPlayerList().getPlayer(playerId);
                if (player != null) {
                    world = serverWorld;
                    break;
                }
            }
            
            if (world != null) {
                updateEffigyEffect(world, effect);
            }
        }
    }
    
    /**
     * Update a single effigy effect each tick (mimics ChantCasterEntity.tick())
     */
    private static void updateEffigyEffect(ServerLevel world, ActiveEffigyEffect effect) {
        effect.tickCounter++;
        
        // Find the effigy tile entity
        BlockEntity blockEntity = world.getBlockEntity(effect.effigyPos);
        if (!(blockEntity instanceof EffigyTileEntity effigy)) {
            return; // Effigy was removed
        }
        
        // Get deity information for colors
        DatapackDeity deity = DatapackDeityManager.getDeity(effect.deityId);
        if (deity == null) {
            return; // Invalid deity
        }
        
        // Smoothly adjust intensity based on state
        adjustIntensity(effect);
        
        // Create particle effects based on current state
        switch (effect.state) {
            case STARTING -> createStartingEffects(world, effigy, deity, effect);
            case ACTIVE -> createSteadyEffects(world, effigy, deity, effect);
            case PULSING -> createPulsingEffects(world, effigy, deity, effect);
            case ENDING -> createEndingEffects(world, effigy, deity, effect);
        }
    }
    
    /**
     * Smoothly adjust effect intensity
     */
    private static void adjustIntensity(ActiveEffigyEffect effect) {
        float intensityChange = 0.05f; // Smooth transition speed
        
        if (effect.currentIntensity < effect.targetIntensity) {
            effect.currentIntensity = Math.min(effect.targetIntensity, effect.currentIntensity + intensityChange);
        } else if (effect.currentIntensity > effect.targetIntensity) {
            effect.currentIntensity = Math.max(effect.targetIntensity, effect.currentIntensity - intensityChange);
        }
    }
    
    /**
     * Create starting effects (ramping up)
     */
    private static void createStartingEffects(ServerLevel world, EffigyTileEntity effigy, DatapackDeity deity, ActiveEffigyEffect effect) {
        if (effect.tickCounter % 4 == 0) { // Reduced frequency during startup
            createFlameParticles(world, effigy, deity, effect.currentIntensity * 0.7f);
        }
        
        // Transition to active after 40 ticks (2 seconds)
        if (effect.tickCounter >= 40) {
            effect.state = ActiveEffigyEffect.EffectState.ACTIVE;
        }
    }
    
    /**
     * Create steady conversation effects
     */
    private static void createSteadyEffects(ServerLevel world, EffigyTileEntity effigy, DatapackDeity deity, ActiveEffigyEffect effect) {
        if (effect.tickCounter % 3 == 0) { // Regular frequency
            createFlameParticles(world, effigy, deity, effect.currentIntensity);
        }
    }
    
    /**
     * Create pulsing effects during AI response
     */
    private static void createPulsingEffects(ServerLevel world, EffigyTileEntity effigy, DatapackDeity deity, ActiveEffigyEffect effect) {
        // Create pulsing pattern using sine wave
        double pulsePhase = (effect.tickCounter * 0.2) % (2 * Math.PI);
        float pulseMultiplier = 0.7f + 0.4f * (float)Math.sin(pulsePhase); // Oscillate between 0.7 and 1.1
        
        if (effect.tickCounter % 2 == 0) { // Higher frequency during pulsing
            createFlameParticles(world, effigy, deity, effect.currentIntensity * pulseMultiplier);
        }
        
        // Add extra sparkle effects during pulses
        if (effect.tickCounter % 10 == 0) {
            createSparkleEffects(world, effigy, deity);
        }
    }
    
    /**
     * Create ending effects (fading out)
     */
    private static void createEndingEffects(ServerLevel world, EffigyTileEntity effigy, DatapackDeity deity, ActiveEffigyEffect effect) {
        if (effect.tickCounter % 6 == 0) { // Reduced frequency during ending
            createFlameParticles(world, effigy, deity, effect.currentIntensity * 0.5f);
        }
    }
    
    /**
     * Create Eidolon-style flame particles around the effigy (mimics ChantCasterEntity)
     */
    private static void createFlameParticles(ServerLevel world, EffigyTileEntity effigy, DatapackDeity deity, float intensity) {
        BlockPos pos = effigy.getBlockPos();
        Vec3 center = Vec3.atCenterOf(pos);
        RandomSource random = world.getRandom();
        
        // Get deity colors
        float red = deity.getRed();
        float green = deity.getGreen();
        float blue = deity.getBlue();
        
        // Create flame particles around the effigy (like Eidolon does)
        int particleCount = Math.max(1, (int)(intensity * 4));
        for (int i = 0; i < particleCount; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double radius = 0.8 + random.nextDouble() * 0.7;
            double height = random.nextDouble() * 1.5;
            
            double x = center.x + Math.cos(angle) * radius;
            double y = center.y + 0.2 + height;
            double z = center.z + Math.sin(angle) * radius;
            
            // Use flame-like particles (will integrate proper Eidolon particles later)
            // Create multiple particle types for rich visual effect
            world.sendParticles(
                ParticleTypes.FLAME,
                x, y, z,
                1, 0.1, 0.1, 0.1, 0.02
            );
            
            // Add sparkle effect based on deity colors
            world.sendParticles(
                ParticleTypes.ENCHANT,
                x, y, z,
                2, 0.15, 0.15, 0.15, 0.01
            );
        }
    }
    
    /**
     * Create sparkle effects for intense moments
     */
    private static void createSparkleEffects(ServerLevel world, EffigyTileEntity effigy, DatapackDeity deity) {
        BlockPos pos = effigy.getBlockPos();
        Vec3 center = Vec3.atCenterOf(pos);
        RandomSource random = world.getRandom();
        
        // Create sparkle burst
        for (int i = 0; i < 6; i++) {
            double angle = (i * 2.0 * Math.PI) / 6;
            double radius = 1.2 + random.nextDouble() * 0.5;
            
            double x = center.x + Math.cos(angle) * radius;
            double y = center.y + 0.5 + random.nextDouble() * 0.8;
            double z = center.z + Math.sin(angle) * radius;
            
            world.sendParticles(
                ParticleTypes.GLOW,
                x, y, z,
                1, 0.1, 0.1, 0.1, 0.05
            );
        }
    }
    
    /**
     * Create completion flash when conversation ends (mimics ChantCasterEntity completion)
     */
    private static void createCompletionFlash(ServerLevel world, BlockPos effigyPos, ResourceLocation deityId) {
        Vec3 center = Vec3.atCenterOf(effigyPos);
        DatapackDeity deity = DatapackDeityManager.getDeity(deityId);
        
        // Play completion sound
        world.playSound(null, effigyPos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.6f, 0.8f);
        
        // Create flash burst
        for (int i = 0; i < 20; i++) {
            double angle = (i * 2.0 * Math.PI) / 20;
            double radius = 1.5 + world.getRandom().nextDouble() * 1.0;
            double height = world.getRandom().nextDouble() * 1.5;
            
            double x = center.x + Math.cos(angle) * radius;
            double y = center.y + 0.5 + height;
            double z = center.z + Math.sin(angle) * radius;
            
            // Flash particles
            world.sendParticles(
                ParticleTypes.FLASH,
                x, y, z,
                1, 0.1, 0.1, 0.1, 0.1
            );
            
            // Deity-themed particles (will add color support later)
            if (deity != null) {
                world.sendParticles(
                    ParticleTypes.FLAME,
                    x, y, z,
                    2, 0.15, 0.15, 0.15, 0.03
                );
                
                world.sendParticles(
                    ParticleTypes.ENCHANT,
                    x, y, z,
                    1, 0.1, 0.1, 0.1, 0.02
                );
            }
        }
        
        LOGGER.debug("✨ Created completion flash for deity {} at {}", deityId, effigyPos);
    }
    
    /**
     * Find nearby effigy for a player using Eidolon's proven detection method
     * Uses the same pattern as PrayerSpell.getEffigy() and ChantCasterEntity
     */
    public static EffigyTileEntity findNearbyEffigy(ServerPlayer player, double maxDistance) {
        ServerLevel world = player.serverLevel();
        BlockPos playerPos = player.blockPosition();
        
        LOGGER.info("🔍 Searching for effigy near player {} at {} using Eidolon's detection method", 
            player.getName().getString(), playerPos);
        
        // Use Eidolon's proven method - create AABB search area
        int range = (int) Math.ceil(maxDistance);
        AABB searchArea = new AABB(
            playerPos.offset(-range, -range, -range), 
            playerPos.offset(range + 1, range + 1, range + 1)
        );
        
        LOGGER.info("🔎 Search area: {} to {} (range: {})", 
            playerPos.offset(-range, -range, -range),
            playerPos.offset(range + 1, range + 1, range + 1), range);
        
        try {
            // Debug chunk loading and tile entity registration
            LOGGER.info("🔍 Checking chunk loading status around player...");
            for (int chunkX = (int)Math.floor(searchArea.minX / 16.0); chunkX <= (int)Math.ceil(searchArea.maxX / 16.0); chunkX++) {
                for (int chunkZ = (int)Math.floor(searchArea.minZ / 16.0); chunkZ <= (int)Math.ceil(searchArea.maxZ / 16.0); chunkZ++) {
                    net.minecraft.world.level.ChunkPos chunkPos = new net.minecraft.world.level.ChunkPos(chunkX, chunkZ);
                    boolean isLoaded = world.hasChunk(chunkX, chunkZ);
                    LOGGER.info("🔍 Chunk {} loaded: {}", chunkPos, isLoaded);
                    
                    if (isLoaded) {
                        net.minecraft.world.level.chunk.ChunkAccess chunk = world.getChunk(chunkX, chunkZ);
                        java.util.Set<BlockPos> tilePositions = chunk.getBlockEntitiesPos();
                        LOGGER.info("🔍 Chunk {} has {} tile entities: {}", chunkPos, tilePositions.size(), 
                            tilePositions.stream().limit(5).map(BlockPos::toString).collect(java.util.stream.Collectors.joining(", ")));
                        
                        // Check if any are effigies
                        int effigyCount = 0;
                        for (BlockPos tilePos : tilePositions) {
                            net.minecraft.world.level.block.entity.BlockEntity te = world.getBlockEntity(tilePos);
                            if (te instanceof EffigyTileEntity) {
                                effigyCount++;
                                LOGGER.info("🎆 Found effigy in chunk at {}: {}", tilePos, te.getClass().getSimpleName());
                            }
                        }
                        LOGGER.info("🔍 Chunk {} has {} effigy tile entities", chunkPos, effigyCount);
                    }
                }
            }
            
            // Use Eidolon's chunk-based tile entity lookup (same as PrayerSpell.getEffigy)
            java.util.List<EffigyTileEntity> effigies = elucent.eidolon.api.ritual.Ritual.getTilesWithinAABB(
                EffigyTileEntity.class, world, searchArea);
            
            LOGGER.info("🔎 Eidolon's getTilesWithinAABB found {} effigies in search area", effigies.size());
            
            if (effigies.isEmpty()) {
                LOGGER.warn("🔮 No effigy found with Eidolon's method. Attempting manual search as fallback...");
                
                // Fallback: Manual search to see what's actually in the area
                for (int x = (int) searchArea.minX; x <= searchArea.maxX; x++) {
                    for (int y = (int) searchArea.minY; y <= searchArea.maxY; y++) {
                        for (int z = (int) searchArea.minZ; z <= searchArea.maxZ; z++) {
                            BlockPos pos = new BlockPos(x, y, z);
                            if (playerPos.distSqr(pos) <= maxDistance * maxDistance) {
                                net.minecraft.world.level.block.state.BlockState blockState = world.getBlockState(pos);
                                net.minecraft.world.level.block.entity.BlockEntity blockEntity = world.getBlockEntity(pos);
                                
                                // Log interesting blocks
                                if (!blockState.isAir() && (blockState.getBlock().getName().getString().toLowerCase().contains("effigy") || 
                                    blockEntity instanceof EffigyTileEntity)) {
                                    LOGGER.info("🔍 Found block at {}: {} (BlockEntity: {})", 
                                        pos, blockState.getBlock().getName().getString(), 
                                        blockEntity != null ? blockEntity.getClass().getSimpleName() : "null");
                                    
                                    if (blockEntity instanceof EffigyTileEntity effigy) {
                                        LOGGER.info("🎆 MANUAL SEARCH: Found effigy at {} (distance: {:.1f} blocks)", 
                                            pos, Math.sqrt(playerPos.distSqr(pos)));
                                        return effigy;
                                    }
                                }
                            }
                        }
                    }
                }
                
                LOGGER.warn("🔮 No effigy found within {} blocks of player {} at {} (both methods failed)", 
                    maxDistance, player.getName().getString(), playerPos);
                return null;
            }
            
            // Return closest effigy (same logic as Eidolon's PrayerSpell)
            EffigyTileEntity closestEffigy = effigies.stream()
                .min(java.util.Comparator.comparingDouble((e) -> e.getBlockPos().distSqr(playerPos)))
                .orElse(null);
            
            if (closestEffigy != null) {
                double distance = Math.sqrt(closestEffigy.getBlockPos().distSqr(playerPos));
                LOGGER.info("🎆 Found effigy at {} (distance: {:.1f} blocks) using Eidolon's method", 
                    closestEffigy.getBlockPos(), distance);
            }
            
            return closestEffigy;
            
        } catch (Exception e) {
            LOGGER.error("❌ Error using Eidolon's effigy detection: {}", e.getMessage(), e);
            return null;
        }
    }
}