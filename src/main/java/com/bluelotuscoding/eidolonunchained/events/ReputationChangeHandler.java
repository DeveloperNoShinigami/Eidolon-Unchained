package com.bluelotuscoding.eidolonunchained.events;

import com.bluelotuscoding.eidolonunchained.capability.CapabilityHandler;
import com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager;
import com.bluelotuscoding.eidolonunchained.deity.DatapackDeity;
import elucent.eidolon.capability.IReputation;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ⚠️ DEPRECATED: This tick-based reputation monitoring system has been replaced
 * with native Eidolon integration via DatapackDeity.onReputationChange()
 * 
 * The onReputationChange() method in DatapackDeity now handles real-time
 * title updates directly when reputation changes, which is more efficient
 * and proper integration with Eidolon's built-in reputation system.
 * 
 * Keeping this class for reference but it's no longer needed.
 * 
 * Handles real-time reputation changes and triggers title updates
 */
//@Mod.EventBusSubscriber(modid = "eidolonunchained") // DISABLED - replaced with native integration
public class ReputationChangeHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Track last known reputation values to detect changes
    private static final Map<UUID, Map<ResourceLocation, Double>> lastKnownReputation = new ConcurrentHashMap<>();
    
    // Reduced frequency checking - check every 20 ticks (1 second)
    private static int tickCounter = 0;
    private static final int CHECK_INTERVAL = 20;
    
    /**
     * Monitor reputation changes via server tick events
     * This is a lightweight approach that checks for changes periodically
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        tickCounter++;
        if (tickCounter < CHECK_INTERVAL) return;
        tickCounter = 0;
        
        // Check all online players for reputation changes
        if (event.getServer() != null) {
            for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                checkPlayerReputationChanges(player);
            }
        }
    }
    
    /**
     * Check if a player's reputation has changed since last check
     */
    private static void checkPlayerReputationChanges(ServerPlayer player) {
        try {
            UUID playerId = player.getUUID();
            
            // Get current reputation data
            player.level().getCapability(IReputation.INSTANCE).ifPresent(reputation -> {
                Map<ResourceLocation, Double> currentRep = new ConcurrentHashMap<>();
                
                // Check reputation for all our custom deities
                for (DatapackDeity deity : DatapackDeityManager.getAllDeities().values()) {
                    double currentValue = reputation.getReputation(player, deity.getId());
                    currentRep.put(deity.getId(), currentValue);
                }
                
                // Compare with last known values
                Map<ResourceLocation, Double> lastRep = lastKnownReputation.get(playerId);
                if (lastRep != null) {
                    for (Map.Entry<ResourceLocation, Double> entry : currentRep.entrySet()) {
                        ResourceLocation deityId = entry.getKey();
                        double currentValue = entry.getValue();
                        double lastValue = lastRep.getOrDefault(deityId, 0.0);
                        
                        // If reputation changed, trigger title update
                        if (Math.abs(currentValue - lastValue) > 0.1) { // Small threshold to avoid floating point issues
                            LOGGER.debug("Reputation change detected for player {} with deity {}: {} -> {}", 
                                player.getName().getString(), deityId, lastValue, currentValue);
                            
                            triggerTitleUpdate(player, deityId, lastValue, currentValue);
                        }
                    }
                }
                
                // Update last known values
                lastKnownReputation.put(playerId, currentRep);
            });
            
        } catch (Exception e) {
            LOGGER.warn("Error checking reputation changes for player {}: {}", 
                player.getName().getString(), e.getMessage());
        }
    }
    
    /**
     * Trigger title update when reputation changes
     */
    private static void triggerTitleUpdate(ServerPlayer player, ResourceLocation deityId, double oldRep, double newRep) {
        try {
            // Check if this deity is the player's patron
            player.level().getCapability(CapabilityHandler.PATRON_DATA_CAPABILITY).ifPresent(patronData -> {
                ResourceLocation patron = patronData.getPatron(player);
                if (deityId.equals(patron)) {
                    // This is the player's patron deity - update title
                    DatapackDeity deity = DatapackDeityManager.getDeity(deityId);
                    if (deity != null) {
                        String oldTitle = patronData.getTitle(player);
                        deity.updatePatronTitle(player);
                        String newTitle = patronData.getTitle(player);
                        
                        // Notify player if title changed
                        if (!Objects.equals(oldTitle, newTitle)) {
                            LOGGER.info("Title updated for player {} ({}): '{}' -> '{}'", 
                                player.getName().getString(), deityId, oldTitle, newTitle);
                            
                            // Send notification to player
                            if (newTitle != null && !newTitle.isEmpty()) {
                                player.sendSystemMessage(Component.literal(
                                    "§6✨ Your devotion has earned you a new title: §e" + newTitle));
                            }
                        }
                    }
                }
            });
            
        } catch (Exception e) {
            LOGGER.error("Error updating title for reputation change: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Clear tracking data when player leaves
     */
    public static void onPlayerLogout(ServerPlayer player) {
        lastKnownReputation.remove(player.getUUID());
    }
    
    /**
     * Force update all reputation tracking for a player (useful for manual commands)
     */
    public static void forceUpdatePlayer(ServerPlayer player) {
        lastKnownReputation.remove(player.getUUID());
        checkPlayerReputationChanges(player);
    }
}
