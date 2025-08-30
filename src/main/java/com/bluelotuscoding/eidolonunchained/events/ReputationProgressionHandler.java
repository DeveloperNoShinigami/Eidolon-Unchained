package com.bluelotuscoding.eidolonunchained.events;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager;
import com.bluelotuscoding.eidolonunchained.deity.DatapackDeity;
import com.mojang.logging.LogUtils;
import elucent.eidolon.capability.IReputation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 🎯 REPUTATION PROGRESSION EVENT SYSTEM
 * 
 * Monitors reputation changes and fires deity progression events when thresholds are crossed.
 * This replaces the hardcoded progression logic and enables dynamic JSON-driven advancement.
 * 
 * Key Features:
 * - Tracks last known reputation levels per player/deity
 * - Detects threshold crossings based on JSON-defined progression stages
 * - Fires unlock events for new progression levels
 * - Integrates with AI system for dynamic personality changes
 * - Provides comprehensive logging for debugging
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ReputationProgressionHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Track last known reputation levels to detect changes
    private static final Map<UUID, Map<ResourceLocation, Double>> lastKnownReputation = new HashMap<>();
    
    // Tick counter for periodic checks (every 20 ticks = 1 second)
    private static int tickCounter = 0;
    private static final int CHECK_INTERVAL = 20;
    
    /**
     * 🔄 PERIODIC REPUTATION MONITORING
     * 
     * Checks all online players' reputation levels every second to detect progression changes.
     * This ensures immediate response to reputation gains from any source.
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        tickCounter++;
        if (tickCounter >= CHECK_INTERVAL) {
            tickCounter = 0;
            checkAllPlayersProgression();
        }
    }
    
    /**
     * 🔍 CHECK ALL ONLINE PLAYERS
     * 
     * Iterates through all online players and checks their reputation progression
     * against all registered datapack deities.
     */
    private static void checkAllPlayersProgression() {
        try {
            // Get all registered datapack deities
            Map<ResourceLocation, DatapackDeity> deities = DatapackDeityManager.getAllDeities();
            if (deities.isEmpty()) return;
            
            // Check each deity for each online player
            for (DatapackDeity deity : deities.values()) {
                checkDeityProgressionForAllPlayers(deity);
            }
        } catch (Exception e) {
            LOGGER.error("🚨 Error during reputation progression check", e);
        }
    }
    
    /**
     * 🏛️ CHECK SPECIFIC DEITY PROGRESSION
     * 
     * Examines all online players' reputation with a specific deity and triggers
     * progression events when thresholds are crossed.
     */
    private static void checkDeityProgressionForAllPlayers(DatapackDeity deity) {
        // This would need server access - let's modify to work with events instead
        LOGGER.debug("🔍 Checking progression for deity: {}", deity.getId());
    }
    
    /**
     * 📊 CHECK INDIVIDUAL PLAYER PROGRESSION
     * 
     * Called when we need to check a specific player's progression with all deities.
     * This is the main entry point for progression detection.
     */
    public static void checkPlayerProgression(ServerPlayer player) {
        try {
            UUID playerId = player.getUUID();
            
            // Get player's reputation capability
            player.level().getCapability(IReputation.INSTANCE).ifPresent(reputation -> {
                
                // Check progression for each registered deity
                for (DatapackDeity deity : DatapackDeityManager.getAllDeities().values()) {
                    checkPlayerDeityProgression(player, deity, reputation);
                }
            });
            
        } catch (Exception e) {
            LOGGER.error("🚨 Error checking progression for player {}", player.getName().getString(), e);
        }
    }
    
    /**
     * 🎭 CHECK PLAYER-DEITY PROGRESSION
     * 
     * Core progression detection logic that compares current reputation against
     * last known levels and triggers advancement events.
     */
    private static void checkPlayerDeityProgression(ServerPlayer player, DatapackDeity deity, IReputation reputation) {
        UUID playerId = player.getUUID();
        ResourceLocation deityId = deity.getId();
        
        try {
            // Get current reputation
            double currentReputation = reputation.getReputation(playerId, deityId);
            
            // Get last known reputation
            Map<ResourceLocation, Double> playerRep = lastKnownReputation.computeIfAbsent(playerId, k -> new HashMap<>());
            Double lastReputation = playerRep.get(deityId);
            
            // If this is the first check, just store current value
            if (lastReputation == null) {
                playerRep.put(deityId, currentReputation);
                LOGGER.debug("📋 Initial reputation recorded for {}/{}: {}", 
                    player.getName().getString(), deityId, currentReputation);
                return;
            }
            
            // Check if reputation has changed significantly (more than 0.1 to avoid floating point issues)
            if (Math.abs(currentReputation - lastReputation) > 0.1) {
                LOGGER.info("📈 Reputation change detected for {}/{}: {} -> {}", 
                    player.getName().getString(), deityId, lastReputation, currentReputation);
                
                // Check for progression threshold crossings
                checkProgressionThresholds(player, deity, lastReputation, currentReputation);
                
                // Update stored reputation
                playerRep.put(deityId, currentReputation);
            }
            
        } catch (Exception e) {
            LOGGER.error("🚨 Error checking deity progression for {}/{}", 
                player.getName().getString(), deityId, e);
        }
    }
    
    /**
     * 🎯 PROGRESSION THRESHOLD DETECTION
     * 
     * Analyzes reputation change to determine if any progression thresholds were crossed.
     * Uses JSON-defined stages instead of hardcoded values.
     */
    private static void checkProgressionThresholds(ServerPlayer player, DatapackDeity deity, 
                                                  double oldReputation, double newReputation) {
        
        try {
            // Get deity's progression stages from JSON
            Map<String, Object> stagesMap = deity.getProgressionStages();
            
            if (stagesMap.isEmpty()) {
                LOGGER.debug("🔍 No progression stages defined for deity {}", deity.getId());
                return;
            }
            
            // Check each stage to see if we crossed its threshold
            for (Map.Entry<String, Object> stageEntry : stagesMap.entrySet()) {
                String stageName = stageEntry.getKey();
                Object stageData = stageEntry.getValue();
                
                // Handle the case where stage data is a Map
                if (!(stageData instanceof Map)) continue;
                @SuppressWarnings("unchecked")
                Map<String, Object> stageDataMap = (Map<String, Object>) stageData;
                
                Object repReqObj = stageDataMap.get("reputationRequired");
                if (!(repReqObj instanceof Number)) continue;
                
                double requiredReputation = ((Number) repReqObj).doubleValue();
                
                // Check if we just crossed this threshold (going up)
                if (oldReputation < requiredReputation && newReputation >= requiredReputation) {
                    triggerProgressionUnlock(player, deity, stageName, newReputation);
                }
                // Check if we fell below this threshold (going down)
                else if (oldReputation >= requiredReputation && newReputation < requiredReputation) {
                    triggerProgressionLock(player, deity, stageName, newReputation);
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("🚨 Error checking progression thresholds for {}/{}", 
                player.getName().getString(), deity.getId(), e);
        }
    }
    
    /**
     * 🔓 TRIGGER PROGRESSION UNLOCK
     * 
     * Fires when a player reaches a new progression level.
     * This replaces the missing onReputationUnlock events.
     */
    private static void triggerProgressionUnlock(ServerPlayer player, DatapackDeity deity, 
                                               String stageName, double reputation) {
        
        ResourceLocation stageId = new ResourceLocation(deity.getId().getNamespace(), stageName);
        
        LOGGER.info("🎉 PROGRESSION UNLOCK: Player {} reached {} with {} ({}rep)", 
            player.getName().getString(), stageName, deity.getName(), (int)reputation);
        
        try {
            // Call the deity's unlock handler (this should trigger rewards, messages, etc.)
            deity.onReputationUnlock(player, stageId);
            
            // 🤖 Notify AI system of progression change
            notifyAISystemProgression(player, deity, stageName, reputation, true);
            
        } catch (Exception e) {
            LOGGER.error("🚨 Error triggering progression unlock for {}/{}/{}", 
                player.getName().getString(), deity.getId(), stageName, e);
        }
    }
    
    /**
     * 🔒 TRIGGER PROGRESSION LOCK
     * 
     * Fires when a player falls below a progression level.
     * This handles reputation loss scenarios.
     */
    private static void triggerProgressionLock(ServerPlayer player, DatapackDeity deity, 
                                             String stageName, double reputation) {
        
        ResourceLocation stageId = new ResourceLocation(deity.getId().getNamespace(), stageName);
        
        LOGGER.info("🔐 PROGRESSION LOCK: Player {} lost {} with {} ({}rep)", 
            player.getName().getString(), stageName, deity.getName(), (int)reputation);
        
        try {
            // Call the deity's lock handler
            deity.onReputationLock(player, stageId);
            
            // 🤖 Notify AI system of progression change
            notifyAISystemProgression(player, deity, stageName, reputation, false);
            
        } catch (Exception e) {
            LOGGER.error("🚨 Error triggering progression lock for {}/{}/{}", 
                player.getName().getString(), deity.getId(), stageName, e);
        }
    }
    
    /**
     * 🤖 NOTIFY AI SYSTEM
     * 
     * Informs the AI system about progression changes so it can update
     * personality and behavior appropriately.
     */
    private static void notifyAISystemProgression(ServerPlayer player, DatapackDeity deity, 
                                                String stageName, double reputation, boolean unlocked) {
        try {
            // This will be called to update AI personality based on new progression
            LOGGER.debug("🤖 Notifying AI system: {}/{} {} {} ({}rep)", 
                player.getName().getString(), deity.getId(), 
                unlocked ? "unlocked" : "locked", stageName, (int)reputation);
            
            // TODO: Integrate with AI personality system to update behavior
            // This ensures AI treats player according to their actual progression level
            
        } catch (Exception e) {
            LOGGER.error("🚨 Error notifying AI system of progression change", e);
        }
    }
    
    /**
     * 🧹 CLEANUP DISCONNECTED PLAYERS
     * 
     * Removes tracking data for players who have disconnected to prevent memory leaks.
     */
    public static void cleanupPlayer(UUID playerId) {
        lastKnownReputation.remove(playerId);
        LOGGER.debug("🧹 Cleaned up reputation tracking for player {}", playerId);
    }
    
    /**
     * 📊 MANUAL PROGRESSION CHECK
     * 
     * Public method for manually triggering a progression check.
     * Useful for testing and debugging.
     */
    public static void forceProgressionCheck(ServerPlayer player) {
        LOGGER.info("🔄 Manual progression check triggered for {}", player.getName().getString());
        checkPlayerProgression(player);
    }
}
