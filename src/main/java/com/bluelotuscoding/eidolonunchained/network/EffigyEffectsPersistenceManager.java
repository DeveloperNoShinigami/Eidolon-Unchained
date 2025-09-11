package com.bluelotuscoding.eidolonunchained.network;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Manages persistent effigy effects that continue during deity conversations
 * Effects can be started, intensified, dimmed, and stopped based on conversation state
 */
@Mod.EventBusSubscriber(modid = "eidolonunchained", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EffigyEffectsPersistenceManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Track active effigy effects per player
    private static final Map<UUID, ActiveEffigyEffect> activeEffects = new ConcurrentHashMap<>();
    
    /**
     * Start persistent effigy effects for a player
     */
    public static void startEffects(ServerPlayer player, BlockPos effigyPos, ResourceLocation deityId, 
                                   EffigyEffectsPacket.SoundConfig soundConfig) {
        UUID playerId = player.getUUID();
        
        // Stop any existing effects
        stopEffects(player);
        
        // Create new persistent effect
        ActiveEffigyEffect effect = new ActiveEffigyEffect(effigyPos, deityId, soundConfig, 
                                                          System.currentTimeMillis());
        activeEffects.put(playerId, effect);
        
        // Send initial effect packet
        sendEffectPacket(player, EffigyEffectsPacket.EffectMode.START, effect);
        
        LOGGER.info("🔮 Started persistent effigy effects for player {} with deity {}", 
                   player.getName().getString(), deityId);
    }
    
    /**
     * Intensify effects when deity is speaking
     */
    public static void intensifyEffects(ServerPlayer player) {
        UUID playerId = player.getUUID();
        ActiveEffigyEffect effect = activeEffects.get(playerId);
        
        if (effect != null) {
            effect.intensity = EffigyEffectsPacket.EffectIntensity.HIGH;
            sendEffectPacket(player, EffigyEffectsPacket.EffectMode.INTENSIFY, effect);
            
            LOGGER.debug("🔮 Intensified effigy effects for player {}", player.getName().getString());
        }
    }
    
    /**
     * Dim effects when deity is idle
     */
    public static void dimEffects(ServerPlayer player) {
        UUID playerId = player.getUUID();
        ActiveEffigyEffect effect = activeEffects.get(playerId);
        
        if (effect != null) {
            effect.intensity = EffigyEffectsPacket.EffectIntensity.LOW;
            sendEffectPacket(player, EffigyEffectsPacket.EffectMode.DIM, effect);
            
            LOGGER.debug("🔮 Dimmed effigy effects for player {}", player.getName().getString());
        }
    }
    
    /**
     * Stop persistent effigy effects for a player
     */
    public static void stopEffects(ServerPlayer player) {
        UUID playerId = player.getUUID();
        ActiveEffigyEffect effect = activeEffects.remove(playerId);
        
        if (effect != null) {
            sendEffectPacket(player, EffigyEffectsPacket.EffectMode.STOP, effect);
            
            LOGGER.info("🔮 Stopped persistent effigy effects for player {}", 
                       player.getName().getString());
        }
    }
    
    /**
     * Check if player has active effigy effects
     */
    public static boolean hasActiveEffects(ServerPlayer player) {
        return activeEffects.containsKey(player.getUUID());
    }
    
    /**
     * Get active effect info for a player
     */
    public static ActiveEffigyEffect getActiveEffect(ServerPlayer player) {
        return activeEffects.get(player.getUUID());
    }
    
    /**
     * Send effect packet to player
     */
    private static void sendEffectPacket(ServerPlayer player, EffigyEffectsPacket.EffectMode mode, 
                                        ActiveEffigyEffect effect) {
        try {
            EffigyEffectsPacket packet = new EffigyEffectsPacket(
                effect.effigyPos, effect.deityId, effect.soundConfig, mode, effect.intensity
            );
            
            // TODO: Send packet via networking system
            // EidolonUnchainedNetworking.INSTANCE.sendTo(packet, player.connection.connection, 
            //     net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT);
            
            LOGGER.debug("🔮 Sent {} effigy effect packet to {}", mode, player.getName().getString());
        } catch (Exception e) {
            LOGGER.error("🔮 Failed to send effigy effect packet: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Server tick event to maintain persistent effects
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            // Clean up expired effects and send periodic updates
            long currentTime = System.currentTimeMillis();
            
            activeEffects.entrySet().removeIf(entry -> {
                ActiveEffigyEffect effect = entry.getValue();
                
                // Remove effects older than 10 minutes (failsafe)
                if (currentTime - effect.startTime > 600000) {
                    LOGGER.warn("🔮 Removing expired effigy effect for player {}", entry.getKey());
                    return true;
                }
                
                return false;
            });
        }
    }
    
    /**
     * Clean up effects when player disconnects
     */
    public static void onPlayerDisconnect(ServerPlayer player) {
        UUID playerId = player.getUUID();
        if (activeEffects.remove(playerId) != null) {
            LOGGER.info("🔮 Cleaned up effigy effects for disconnecting player {}", 
                       player.getName().getString());
        }
    }
    
    /**
     * Data class for active effigy effect tracking
     */
    public static class ActiveEffigyEffect {
        public final BlockPos effigyPos;
        public final ResourceLocation deityId;
        public final EffigyEffectsPacket.SoundConfig soundConfig;
        public final long startTime;
        public EffigyEffectsPacket.EffectIntensity intensity;
        
        public ActiveEffigyEffect(BlockPos effigyPos, ResourceLocation deityId, 
                                 EffigyEffectsPacket.SoundConfig soundConfig, long startTime) {
            this.effigyPos = effigyPos;
            this.deityId = deityId;
            this.soundConfig = soundConfig;
            this.startTime = startTime;
            this.intensity = EffigyEffectsPacket.EffectIntensity.NORMAL;
        }
    }
    
}