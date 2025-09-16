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
import net.minecraftforge.server.ServerLifecycleHooks;
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
        public int pulsingStartTick = -1;
        
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
     * Drive visual updates each server tick for all active effigy effects
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        if (activeEffects.isEmpty()) return;

        // Iterate a copy of keys to allow safe removal
        for (var entry : new java.util.ArrayList<>(activeEffects.entrySet())) {
            UUID playerId = entry.getKey();
            ActiveEffigyEffect effect = entry.getValue();

            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null) {
                activeEffects.remove(playerId);
                continue;
            }

            ServerLevel world = player.serverLevel();
            DatapackDeity deity = DatapackDeityManager.getDeity(effect.deityId);

            // Try to resolve the effigy BE at the stored position (fallback uses pos overload)
            EffigyTileEntity effigy = null;
            BlockEntity be = world.getBlockEntity(effect.effigyPos);
            if (be instanceof EffigyTileEntity e) effigy = e;

            // Advance time and smooth intensity
            effect.tickCounter++;
            adjustIntensity(effect);

            // Spawn particles based on state
            switch (effect.state) {
                case STARTING -> createStartingEffects(world, effigy, deity, effect);
                case ACTIVE -> createSteadyEffects(world, effigy, deity, effect);
                case PULSING -> createPulsingEffects(world, effigy, deity, effect);
                case ENDING -> createEndingEffects(world, effigy, deity, effect);
            }
        }
    }

    /**
     * Start effigy effects when AI conversation begins
     */
    public static void startConversationEffects(ServerPlayer player, BlockPos effigyPos, ResourceLocation deityId) {
        UUID playerId = player.getUUID();
        // Stop any existing effects for this player
        stopEffects(player);
        // Register new active effect
        activeEffects.put(playerId, new ActiveEffigyEffect(effigyPos, deityId));
        // Optional configurable start sound
        try {
            String id = com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig.COMMON.effigyConversationSoundId.get();
            if (id != null && !id.isBlank() && !"none".equalsIgnoreCase(id)) {
                net.minecraft.resources.ResourceLocation rl = net.minecraft.resources.ResourceLocation.tryParse(id);
                net.minecraft.sounds.SoundEvent sound = SoundEvents.BEACON_ACTIVATE;
                if (rl != null) {
                    net.minecraft.sounds.SoundEvent cfg = net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS.getValue(rl);
                    if (cfg != null) sound = cfg;
                }
                float vol = com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig.COMMON.effigyConversationSoundVolume.get().floatValue();
                float pit = com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig.COMMON.effigyConversationSoundPitch.get().floatValue();
                player.serverLevel().playSound(null, effigyPos, sound, SoundSource.BLOCKS, vol, pit);
            }
        } catch (Exception ignored) {}
    }

    /**
     * Intensify effects when AI starts responding (pulsing mode)
     */
    public static void startAIResponseEffects(ServerPlayer player) {
        ActiveEffigyEffect effect = activeEffects.get(player.getUUID());
        if (effect != null) {
            effect.state = ActiveEffigyEffect.EffectState.PULSING;
            effect.targetIntensity = 1.5f;
            effect.pulsingStartTick = effect.tickCounter;
        }
    }

    /**
     * Return to steady effects when AI finishes responding
     */
    public static void endAIResponseEffects(ServerPlayer player) {
        ActiveEffigyEffect effect = activeEffects.get(player.getUUID());
        if (effect != null) {
            effect.state = ActiveEffigyEffect.EffectState.ACTIVE;
            effect.targetIntensity = 1.0f;
            effect.pulsingStartTick = -1;
        }
    }

    /**
     * Stop all effigy effects when conversation ends
     */
    public static void stopEffects(ServerPlayer player) {
        ActiveEffigyEffect effect = activeEffects.remove(player.getUUID());
        if (effect != null) {
            createCompletionFlash(player.serverLevel(), effect.effigyPos, effect.deityId);
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
            if (effigy != null) createFlameParticles(world, effigy, deity, effect.currentIntensity * 0.7f);
            else createFlameParticles(world, effect.effigyPos, deity, effect.currentIntensity * 0.7f);
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
            if (effigy != null) createFlameParticles(world, effigy, deity, effect.currentIntensity);
            else createFlameParticles(world, effect.effigyPos, deity, effect.currentIntensity);
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
            if (effigy != null) createFlameParticles(world, effigy, deity, effect.currentIntensity * pulseMultiplier);
            else createFlameParticles(world, effect.effigyPos, deity, effect.currentIntensity * pulseMultiplier);
        }
        
        // No extra sparkle effects during pulses (user preference)
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

    float red = deity != null ? deity.getRed() : 1.0f;
    float green = deity != null ? deity.getGreen() : 0.7f;
    float blue = deity != null ? deity.getBlue() : 0.3f;

        // Place two colored flame clusters at the effigy front corners (matches PrayerSpell look)
        var state = world.getBlockState(pos);
        if (state.hasProperty(elucent.eidolon.common.block.HorizontalBlockBase.HORIZONTAL_FACING)) {
            var dir = state.getValue(elucent.eidolon.common.block.HorizontalBlockBase.HORIZONTAL_FACING);
            var tangent = dir.getClockWise();
            float x0 = pos.getX() + 0.5f + dir.getStepX() * 0.21875f;
            float y0 = pos.getY() + 0.8125f;
            float z0 = pos.getZ() + 0.5f + dir.getStepZ() * 0.21875f;

            // Make both eyes the same size and strengthen with intensity
            int eyeRepeats = Math.max(3, Math.round(4 * intensity));

            elucent.eidolon.client.particle.Particles.create(EidolonParticles.FLAME_PARTICLE.get())
                .setColor(red, green, blue)
                .setAlpha(0.5f, 0f)
                .setScale(0.14f, 0.09f)
                .randomOffset(0.01f)
                .randomVelocity(0.0025f)
                .addVelocity(0, 0.005f, 0)
                .repeat(world, x0 + 0.09375f * tangent.getStepX(), y0, z0 + 0.09375f * tangent.getStepZ(), eyeRepeats);

            elucent.eidolon.client.particle.Particles.create(EidolonParticles.FLAME_PARTICLE.get())
                .setColor(red, green, blue)
                .setAlpha(0.5f, 0f)
                .setScale(0.14f, 0.09f)
                .randomOffset(0.01f)
                .randomVelocity(0.0025f)
                .addVelocity(0, 0.005f, 0)
                .repeat(world, x0 - 0.09375f * tangent.getStepX(), y0, z0 - 0.09375f * tangent.getStepZ(), eyeRepeats);
        }

        // Soft colored ring above the effigy for ambiance
        int particleCount = Math.max(1, (int)(intensity * 4));
        for (int i = 0; i < particleCount; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double radius = 0.6 + random.nextDouble() * 0.5;
            double height = random.nextDouble() * 0.6;
            double x = center.x + Math.cos(angle) * radius;
            double y = center.y + 0.6 + height;
            double z = center.z + Math.sin(angle) * radius;

            elucent.eidolon.client.particle.Particles.create(EidolonParticles.FLAME_PARTICLE.get())
                .setColor(red, green, blue)
                .setAlpha(0.4f, 0f)
                .setScale(0.1f, 0.06f)
                .randomOffset(0.02f)
                .randomVelocity(0.003f)
                .spawn(world, x, y, z);
        }
    }

    // Overload: render at a raw position when effigy BE type is unavailable
    private static void createFlameParticles(ServerLevel world, BlockPos pos, DatapackDeity deity, float intensity) {
        Vec3 center = Vec3.atCenterOf(pos);
        RandomSource random = world.getRandom();
    float red = deity != null ? deity.getRed() : 1.0f;
    float green = deity != null ? deity.getGreen() : 0.7f;
    float blue = deity != null ? deity.getBlue() : 0.3f;
        int particleCount = Math.max(1, (int)(intensity * 4));
        for (int i = 0; i < particleCount; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double radius = 0.8 + random.nextDouble() * 0.7;
            double height = random.nextDouble() * 1.0;
            double x = center.x + Math.cos(angle) * radius;
            double y = center.y + 0.4 + height;
            double z = center.z + Math.sin(angle) * radius;
            elucent.eidolon.client.particle.Particles.create(EidolonParticles.FLAME_PARTICLE.get())
                .setColor(red, green, blue)
                .setAlpha(0.4f, 0f)
                .setScale(0.1f, 0.06f)
                .randomOffset(0.02f)
                .randomVelocity(0.003f)
                .spawn(world, x, y, z);
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
        
        // Play completion thunder instead of beacon sound
        world.playSound(null, effigyPos, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.NEUTRAL, 8.0f, 0.8f);
        world.playSound(null, effigyPos, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.NEUTRAL, 1.4f, 0.9f);
        
        // Visual flash disabled per request
        for (int i = 0; i < 0; i++) {
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
        
        // CRITICAL: Use ChantCasterEntity's position calculation (elevated + offset)
        double rad = Math.toRadians(player.yHeadRot);
        net.minecraft.world.phys.Vec3 entityPos = player.getEyePosition().add(-Math.sin(rad) / 2, -0.75, Math.cos(rad) / 2);
        BlockPos searchPos = new BlockPos((int)entityPos.x, (int)entityPos.y, (int)entityPos.z);
        
        LOGGER.info("🔍 Searching for effigy using ChantCasterEntity position calculation", 
            player.getName().getString());
        LOGGER.info("🔍 Player at: {} → ChantCaster equivalent at: {}", 
            player.blockPosition(), searchPos);
        
        // Use Eidolon's proven method - create AABB search area from ChantCaster position
        int range = (int) Math.ceil(maxDistance);
        AABB searchArea = new AABB(
            searchPos.offset(-range, -range, -range), 
            searchPos.offset(range + 1, range + 1, range + 1)
        );
        
        LOGGER.info("🔎 Search area: {} to {} (range: {})", 
            searchPos.offset(-range, -range, -range),
            searchPos.offset(range + 1, range + 1, range + 1), range);
        
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
                            if (searchPos.distSqr(pos) <= maxDistance * maxDistance) {
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
                                            pos, Math.sqrt(searchPos.distSqr(pos)));
                                        return effigy;
                                    }
                                }
                            }
                        }
                    }
                }
                
                LOGGER.warn("🔮 No effigy found within {} blocks of player {} at search pos {} (both methods failed)", 
                    maxDistance, player.getName().getString(), searchPos);
                return null;
            }
            
            // Filter effigies that are properly placed on altar structures (CRITICAL)
            java.util.List<EffigyTileEntity> validEffigies = effigies.stream()
                .filter(effigy -> {
                    // Check if effigy is placed on a proper altar block (TableBlockBase)
                    net.minecraft.world.level.block.state.BlockState below = world.getBlockState(effigy.getBlockPos().below());
                    boolean hasAltar = below.getBlock() instanceof elucent.eidolon.common.block.TableBlockBase;
                    boolean isReady = effigy.ready();
                    
                    if (!hasAltar) {
                        LOGGER.warn("🚫 Effigy at {} not on altar (below: {})", 
                            effigy.getBlockPos(), below.getBlock().getName().getString());
                    }
                    if (!isReady) {
                        LOGGER.warn("🚫 Effigy at {} not ready (cooldown)", effigy.getBlockPos());
                    }
                    
                    return hasAltar && isReady;
                })
                .collect(java.util.stream.Collectors.toList());
            
            LOGGER.info("🔍 Found {} total effigies, {} valid (on altar + ready)", 
                effigies.size(), validEffigies.size());
            
            if (validEffigies.isEmpty()) {
                LOGGER.warn("🔮 No valid effigy found (must be placed on altar and ready)");
                return null;
            }
            
            // Return closest valid effigy
            EffigyTileEntity closestEffigy = validEffigies.stream()
                .min(java.util.Comparator.comparingDouble((e) -> e.getBlockPos().distSqr(searchPos)))
                .orElse(null);
            
            if (closestEffigy != null) {
                double distance = Math.sqrt(closestEffigy.getBlockPos().distSqr(searchPos));
                LOGGER.info("🎆 Found effigy at {} (distance: {:.1f} blocks) using ChantCaster position", 
                    closestEffigy.getBlockPos(), distance);
            }
            
            return closestEffigy;
            
        } catch (Exception e) {
            LOGGER.error("❌ Error using Eidolon's effigy detection: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Robust effigy position detection: returns the nearest effigy BlockPos even if the tile class differs.
     */
    public static BlockPos findNearbyEffigyPos(ServerPlayer player, double maxDistance) {
        EffigyTileEntity effigy = findNearbyEffigy(player, maxDistance);
        if (effigy != null) return effigy.getBlockPos();

        // Fallback: scan for the effigy block by registry id
        try {
            ServerLevel world = player.serverLevel();
            double rad = Math.toRadians(player.yHeadRot);
            net.minecraft.world.phys.Vec3 entityPos = player.getEyePosition().add(-Math.sin(rad) / 2, -0.75, Math.cos(rad) / 2);
            BlockPos searchPos = new BlockPos((int)entityPos.x, (int)entityPos.y, (int)entityPos.z);
            int range = (int)Math.ceil(maxDistance);
            BlockPos nearest = null;
            double best = Double.MAX_VALUE;
            for (int x = -range; x <= range; x++)
                for (int y = -range; y <= range; y++)
                    for (int z = -range; z <= range; z++) {
                        BlockPos pos = searchPos.offset(x, y, z);
                        if (searchPos.distSqr(pos) > maxDistance * maxDistance) continue;
                        var state = world.getBlockState(pos);
                        var key = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock());
                        if (key != null && key.getNamespace().equals("eidolon") && key.getPath().contains("effigy")) {
                            double d = pos.distSqr(searchPos);
                            if (d < best) { best = d; nearest = pos; }
                        }
                    }
            return nearest;
        } catch (Exception ignored) {}
        return null;
    }
}



