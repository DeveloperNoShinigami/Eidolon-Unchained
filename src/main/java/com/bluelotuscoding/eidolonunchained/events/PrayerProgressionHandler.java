package com.bluelotuscoding.eidolonunchained.events;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager;
import com.bluelotuscoding.eidolonunchained.deity.DatapackDeity;
import com.mojang.logging.LogUtils;
import elucent.eidolon.capability.IReputation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🎯 EVENT-DRIVEN PRAYER PROGRESSION HANDLER
 * 
 * This replaces the emergency-disabled server tick polling system with an
 * event-driven approach that checks progression only during prayer interactions.
 * 
 * KEY PRINCIPLES:
 * - Only checks progression when players actively pray to deities
 * - Uses Eidolon's existing reputation system as the source of truth
 * - Fires progression events when reputation thresholds are crossed
 * - No continuous polling - completely event-driven
 * 
 * This solves the freezing issues while maintaining proper progression tracking.
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PrayerProgressionHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Store last known reputation during prayer interactions only
    private static final Map<UUID, Map<ResourceLocation, Double>> lastKnownPrayerReputation = new ConcurrentHashMap<>();
    
    /**
     * 🙏 CHECK PROGRESSION DURING PRAYER
     * 
     * This is called during prayer interactions to check if the player
     * has progressed since their last prayer. Event-driven, not polling.
     * 
     * @param player The player praying
     * @param deityId The deity being prayed to
     * @param currentReputation Current reputation from Eidolon system
     */
    public static void checkProgressionDuringPrayer(ServerPlayer player, ResourceLocation deityId, double currentReputation) {
        try {
            UUID playerId = player.getUUID();
            
            // Get last known reputation from prayer interactions
            Map<ResourceLocation, Double> playerPrayerRep = lastKnownPrayerReputation.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
            Double lastReputation = playerPrayerRep.get(deityId);
            
            // If this is the first prayer, just store current value
            if (lastReputation == null) {
                playerPrayerRep.put(deityId, currentReputation);
                LOGGER.debug("🙏 First prayer reputation recorded for {}/{}: {}", 
                    player.getName().getString(), deityId, currentReputation);
                return;
            }
            
            // Check if reputation has changed significantly
            if (Math.abs(currentReputation - lastReputation) > 0.1) {
                LOGGER.info("🙏 Prayer reputation change detected for {}/{}: {} -> {}", 
                    player.getName().getString(), deityId, lastReputation, currentReputation);
                
                // Get deity and check for progression threshold crossings
                DatapackDeity deity = DatapackDeityManager.getDeity(deityId);
                if (deity != null) {
                    checkProgressionThresholds(player, deity, lastReputation, currentReputation);
                }
                
                // Update stored reputation
                playerPrayerRep.put(deityId, currentReputation);
            }
            
        } catch (Exception e) {
            LOGGER.error("🚨 Error checking prayer progression for {}/{}", 
                player.getName().getString(), deityId, e);
        }
    }
    
    /**
     * 🎯 CHECK FOR PROGRESSION THRESHOLD CROSSINGS
     * 
     * Analyzes reputation change to determine if any progression thresholds were crossed.
     * Uses JSON-defined stages from deity configurations.
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
            
            // Check each stage for threshold crossings
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
     * Fires when a player reaches a new progression level during prayer.
     */
    private static void triggerProgressionUnlock(ServerPlayer player, DatapackDeity deity, 
                                               String stageName, double reputation) {
        try {
            LOGGER.info("🎉 PROGRESSION UNLOCK: {} reached {} with {} ({}rep)", 
                player.getName().getString(), deity.getName(), stageName, (int)reputation);
            
            // Get stage data for title and rewards
            Map<String, Object> stagesMap = deity.getProgressionStages();
            @SuppressWarnings("unchecked")
            Map<String, Object> stageData = (Map<String, Object>) stagesMap.get(stageName);
            
            if (stageData != null) {
                // Get progression title
                String title = (String) stageData.get("title");
                if (title != null) {
                    player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("§6✨ " + title + " ✨§r"), 
                        true // action bar
                    );
                }
                
                // Send progression message
                player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal(
                        "§e⟨ " + deity.getDisplayName() + " ⟩ §7recognizes your devotion: §6" + title
                    )
                );
            }
            
        } catch (Exception e) {
            LOGGER.error("🚨 Error triggering progression unlock", e);
        }
    }
    
    /**
     * 🔒 TRIGGER PROGRESSION LOCK
     * 
     * Fires when a player falls below a progression level during prayer.
     */
    private static void triggerProgressionLock(ServerPlayer player, DatapackDeity deity, 
                                             String stageName, double reputation) {
        try {
            LOGGER.info("📉 PROGRESSION LOCK: {} lost {} with {} ({}rep)", 
                player.getName().getString(), deity.getName(), stageName, (int)reputation);
            
            // Send progression loss message
            player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal(
                    "§c⟨ " + deity.getDisplayName() + " ⟩ §7Your standing has diminished..."
                )
            );
            
        } catch (Exception e) {
            LOGGER.error("🚨 Error triggering progression lock", e);
        }
    }
    
    /**
     * 🧹 CLEANUP PRAYER REPUTATION DATA
     * 
     * Called periodically to clean up old prayer reputation data.
     * Only keeps data for online players.
     */
    public static void cleanupPrayerReputationData(ServerPlayer[] onlinePlayers) {
        try {
            // Get list of online player UUIDs
            var onlinePlayerIds = java.util.Arrays.stream(onlinePlayers)
                .map(ServerPlayer::getUUID)
                .collect(java.util.stream.Collectors.toSet());
            
            // Remove data for offline players
            lastKnownPrayerReputation.entrySet().removeIf(entry -> 
                !onlinePlayerIds.contains(entry.getKey()));
                
            LOGGER.debug("🧹 Cleaned prayer reputation data, {} players tracked", 
                lastKnownPrayerReputation.size());
                
        } catch (Exception e) {
            LOGGER.error("🚨 Error cleaning prayer reputation data", e);
        }
    }
    
    /**
     * 🎯 FORCE PROGRESSION CHECK FOR SPECIFIC PLAYER
     * 
     * Public method to manually trigger progression checks during prayer.
     * This is the main entry point called by the prayer system.
     */
    public static void forceProgressionCheckDuringPrayer(ServerPlayer player, ResourceLocation deityId) {
        try {
            player.level().getCapability(IReputation.INSTANCE).ifPresent(reputation -> {
                double currentReputation = reputation.getReputation(player.getUUID(), deityId);
                checkProgressionDuringPrayer(player, deityId, currentReputation);
            });
        } catch (Exception e) {
            LOGGER.error("🚨 Error in forced prayer progression check", e);
        }
    }
}
