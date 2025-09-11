package com.bluelotuscoding.eidolonunchained.network;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.deity.DatapackDeity;
import com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager;
import com.mojang.logging.LogUtils;
import elucent.eidolon.client.particle.Particles;
import elucent.eidolon.common.block.HorizontalBlockBase;
import elucent.eidolon.registries.EidolonParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.function.Supplier;

/**
 * Network packet for triggering effigy visual and audio effects
 * Following Eidolon's effect packet pattern but with datapack deity configuration
 */
public class EffigyEffectsPacket {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private final float x;
    private final float y;
    private final float z;
    private final ResourceLocation deityId;
    public static class SoundConfig {
        private final String sound;
        private final float volume;
        private final float pitch;
        
        public SoundConfig(String sound, float volume, float pitch) {
            this.sound = sound != null ? sound : "minecraft:ambient_cave";
            this.volume = Math.max(0.0f, volume);
            this.pitch = Math.max(0.1f, pitch);
        }
        
        public String getSound() { return sound; }
        public float getVolume() { return volume; }
        public float getPitch() { return pitch; }
    }
    
    private final SoundConfig soundConfig;
    private final EffectMode mode;
    private final EffectIntensity intensity;
    
    public EffigyEffectsPacket(BlockPos effigyPos, ResourceLocation deityId, SoundConfig soundConfig) {
        this(effigyPos.getX() + 0.5, effigyPos.getY() + 0.5, effigyPos.getZ() + 0.5, deityId, soundConfig, EffectMode.ONESHOT, EffectIntensity.NORMAL);
    }
    
    public EffigyEffectsPacket(BlockPos effigyPos, ResourceLocation deityId, SoundConfig soundConfig, EffectMode mode, EffectIntensity intensity) {
        this(effigyPos.getX() + 0.5, effigyPos.getY() + 0.5, effigyPos.getZ() + 0.5, deityId, soundConfig, mode, intensity);
    }
    
    public EffigyEffectsPacket(double x, double y, double z, ResourceLocation deityId, SoundConfig soundConfig, EffectMode mode, EffectIntensity intensity) {
        this.x = (float) x;
        this.y = (float) y;
        this.z = (float) z;
        this.deityId = deityId;
        this.soundConfig = soundConfig != null ? soundConfig : new SoundConfig("minecraft:ambient_cave", 1.0f, 1.0f);
        this.mode = mode != null ? mode : EffectMode.ONESHOT;
        this.intensity = intensity != null ? intensity : EffectIntensity.NORMAL;
    }
    
    public static void encode(EffigyEffectsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.x);
        buffer.writeFloat(packet.y);
        buffer.writeFloat(packet.z);
        buffer.writeUtf(packet.deityId.toString(), 256);
        buffer.writeUtf(packet.soundConfig.getSound(), 256);
        buffer.writeFloat(packet.soundConfig.getVolume());
        buffer.writeFloat(packet.soundConfig.getPitch());
        buffer.writeEnum(packet.mode);
        buffer.writeEnum(packet.intensity);
    }
    
    public static EffigyEffectsPacket decode(FriendlyByteBuf buffer) {
        return new EffigyEffectsPacket(
            buffer.readFloat(),
            buffer.readFloat(), 
            buffer.readFloat(),
            new ResourceLocation(buffer.readUtf()),
            new SoundConfig(buffer.readUtf(), buffer.readFloat(), buffer.readFloat()),
            buffer.readEnum(EffectMode.class),
            buffer.readEnum(EffectIntensity.class)
        );
    }
    
    public static void consume(EffigyEffectsPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            assert ctx.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT;
            
            Level world = net.minecraft.client.Minecraft.getInstance().level;
            if (world != null) {
                applyEffigyEffects(world, packet);
            }
        });
        ctx.get().setPacketHandled(true);
    }
    
    /**
     * Apply effigy effects using Eidolon's exact particle logic but with datapack deity colors
     * Now supports different effect modes and intensities for persistence
     */
    private static void applyEffigyEffects(Level world, EffigyEffectsPacket packet) {
        try {
            // Get deity configuration for colors
            DatapackDeity deity = DatapackDeityManager.getDeity(packet.deityId);
            if (deity == null) {
                LOGGER.warn("🔮 Cannot apply effigy effects - deity not found: {}", packet.deityId);
                return;
            }
            
            BlockPos effigyPos = new BlockPos((int) packet.x, (int) packet.y, (int) packet.z);
            
            // Handle different effect modes
            switch (packet.mode) {
                case ONESHOT:
                case START:
                    // Apply Eidolon's flame particle effects with deity colors
                    applyEidolonFlameEffects(world, effigyPos, deity, packet.intensity);
                    
                    // Play ambient sound for oneshot/start
                    if (packet.mode == EffectMode.ONESHOT || packet.mode == EffectMode.START) {
                        playConfigurableAmbientSound(world, effigyPos, packet);
                    }
                    break;
                    
                case INTENSIFY:
                    // More intense particle effects when deity speaks
                    applyEidolonFlameEffects(world, effigyPos, deity, EffectIntensity.HIGH);
                    break;
                    
                case DIM:
                    // Dimmed particle effects when deity is idle
                    applyEidolonFlameEffects(world, effigyPos, deity, EffectIntensity.LOW);
                    break;
                    
                case STOP:
                    // TODO: Could add fadeout particles if desired
                    break;
            }
            
            LOGGER.debug("🔮 Applied {} effigy effects for deity {} (intensity: {})", 
                packet.mode, packet.deityId, packet.intensity);
                
        } catch (Exception e) {
            LOGGER.error("🔮 Failed to apply effigy effects for deity {}: {}", 
                packet.deityId, e.getMessage(), e);
        }
    }
    
    /**
     * Apply Eidolon's exact flame particle effects using datapack deity colors
     * This is copied directly from PrayerSpell.java lines 130-149
     * Now supports intensity scaling for persistence effects
     */
    private static void applyEidolonFlameEffects(Level world, BlockPos effigyPos, DatapackDeity deity, EffectIntensity intensity) {
        // Get effigy block state for positioning (same as Eidolon)
        BlockState state = world.getBlockState(effigyPos);
        Direction dir;
        if (state.hasProperty(HorizontalBlockBase.HORIZONTAL_FACING)) {
            dir = state.getValue(HorizontalBlockBase.HORIZONTAL_FACING);
        } else {
            dir = Direction.NORTH; // Fallback direction
        }
        
        Direction tangent = dir.getClockWise();
        float x = effigyPos.getX() + 0.5f + dir.getStepX() * 0.21875f;
        float y = effigyPos.getY() + 0.8125f;
        float z = effigyPos.getZ() + 0.5f + dir.getStepZ() * 0.21875f;
        
        // Calculate particle counts based on intensity
        int particleCount1 = getParticleCount(8, intensity);
        int particleCount2 = getParticleCount(8, intensity);
        float alphaMultiplier = getAlphaMultiplier(intensity);
        
        // First flame particle group (Eidolon's logic with intensity scaling)
        Particles.create(EidolonParticles.FLAME_PARTICLE.get())
                .setColor(deity.getRed(), deity.getGreen(), deity.getBlue())
                .setAlpha(0.5f * alphaMultiplier, 0)
                .setScale(0.125f, 0.0625f)
                .randomOffset(0.01f)
                .randomVelocity(0.0025f).addVelocity(0, 0.005f, 0)
                .repeat(world, x + 0.09375f * tangent.getStepX(), y, z + 0.09375f * tangent.getStepZ(), particleCount1);
        
        // Second flame particle group (Eidolon's logic with intensity scaling)
        Particles.create(EidolonParticles.FLAME_PARTICLE.get())
                .setColor(deity.getRed(), deity.getGreen(), deity.getBlue())
                .setAlpha(0.5f * alphaMultiplier, 0)
                .setScale(0.1875f, 0.125f)
                .randomOffset(0.01f)
                .randomVelocity(0.0025f).addVelocity(0, 0.005f, 0)
                .repeat(world, x - 0.09375f * tangent.getStepX(), y, z - 0.09375f * tangent.getStepZ(), particleCount2);
    }
    
    /**
     * Play configurable ambient sound with custom volume and pitch
     */
    private static void playConfigurableAmbientSound(Level world, BlockPos pos, EffigyEffectsPacket packet) {
        try {
            ResourceLocation soundLocation = new ResourceLocation(packet.soundConfig.getSound());
            SoundEvent soundEvent = ForgeRegistries.SOUND_EVENTS.getValue(soundLocation);
            
            if (soundEvent != null) {
                world.playLocalSound(
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    soundEvent, SoundSource.BLOCKS,
                    packet.soundConfig.getVolume(), packet.soundConfig.getPitch(), false
                );
            } else {
                LOGGER.warn("🔮 Unknown sound event: {}", packet.soundConfig.getSound());
            }
        } catch (Exception e) {
            LOGGER.error("🔮 Failed to play ambient sound {}: {}", packet.soundConfig.getSound(), e.getMessage());
        }
    }
    
    /**
     * Get particle count based on intensity
     */
    private static int getParticleCount(int baseCount, EffectIntensity intensity) {
        switch (intensity) {
            case LOW:
                return Math.max(1, baseCount / 2);  // Half particles
            case HIGH:
                return baseCount * 2;               // Double particles
            case NORMAL:
            default:
                return baseCount;                   // Normal count
        }
    }
    
    /**
     * Get alpha multiplier based on intensity
     */
    private static float getAlphaMultiplier(EffectIntensity intensity) {
        switch (intensity) {
            case LOW:
                return 0.5f;  // Dimmed
            case HIGH:
                return 1.5f;  // Brighter
            case NORMAL:
            default:
                return 1.0f;  // Normal
        }
    }
    
    /**
     * Effect modes for persistence control
     */
    public enum EffectMode {
        ONESHOT,    // Single effect trigger (original behavior)
        START,      // Start persistent effects
        INTENSIFY,  // Increase effect intensity (deity speaking)
        DIM,        // Decrease effect intensity (deity idle)
        STOP        // Stop persistent effects
    }
    
    /**
     * Effect intensity levels for persistence
     */
    public enum EffectIntensity {
        LOW,        // Dimmed effects
        NORMAL,     // Standard effects
        HIGH        // Intensified effects
    }
}