package com.bluelotuscoding.eidolonunchained.patron;

import com.bluelotuscoding.eidolonunchained.capability.IPatronData;
import com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager;
import com.bluelotuscoding.eidolonunchained.deity.DatapackDeity;
import com.mojang.logging.LogUtils;
import elucent.eidolon.capability.IReputation;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Dynamic Patron System that uses deity-specific configuration from JSON
 * All values come from deity configuration - NO HARDCODING
 */
public class PatronSystem {
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
    
    /**
     * Attempts to set a player's patron deity using deity-specific minimum reputation.
     */
    public static boolean choosePatron(ServerPlayer player, ResourceLocation deityId) {
        try {
            // Get player's patron data from level capability (like other systems)
            IPatronData patronData = player.level().getCapability(com.bluelotuscoding.eidolonunchained.capability.CapabilityHandler.PATRON_DATA_CAPABILITY).orElse(null);
            if (patronData == null) {
                sendError(player, "Failed to access patron data");
                return false;
            }
            
            // Validate deity exists in base configuration
            DatapackDeity deity = DatapackDeityManager.getDeity(deityId);
            if (deity == null) {
                sendError(player, "Unknown deity: " + deityId);
                return false;
            }
            
            // Check if already patron of this deity
            ResourceLocation currentPatron = patronData.getPatron(player);
            if (deityId.equals(currentPatron)) {
                sendInfo(player, "You are already a follower of " + deity.getDisplayName());
                return true;
            }
            
            // Get reputation capability for potential initial grant
            IReputation reputation = player.level().getCapability(IReputation.INSTANCE).orElse(null);
            
            // Apply patron switch
            patronData.setPatron(player, deityId);
            
            // 🎁 DYNAMIC INITIAL REPUTATION - Use deity's minimum requirement
            if (reputation != null) {
                double currentRep = reputation.getReputation(player.getUUID(), deityId);
                double minimumRequired = deity.getMinimumPatronReputation();
                
                if (currentRep < minimumRequired) {
                    // Grant minimum reputation to unlock first stage
                    reputation.setReputation(player.getUUID(), deityId, minimumRequired);
                    LOGGER.info("🎁 Granted initial reputation {} to new patron {} for deity {} (minimum: {})", 
                        minimumRequired, player.getName().getString(), deityId, minimumRequired);
                    
                    // This will trigger onReputationChange and unlock the first stage rewards
                    sendSuccess(player, "You gain divine favor as you pledge yourself to " + deity.getDisplayName());
                } else {
                    LOGGER.info("🎯 Player {} already has sufficient reputation ({}) for deity {} (minimum: {})", 
                        player.getName().getString(), currentRep, deityId, minimumRequired);
                }
            }
            
            sendSuccess(player, "You are now a follower of " + deity.getDisplayName());
            
            return true;
            
        } catch (Exception e) {
            LOGGER.error("Error choosing patron for player {}", player.getName().getString(), e);
            sendError(player, "An error occurred while choosing patron");
            return false;
        }
    }
    
    /**
     * Removes a player's patron using deity-specific abandon configuration.
     */
    public static boolean abandonPatron(ServerPlayer player) {
        try {
            IPatronData patronData = player.level().getCapability(com.bluelotuscoding.eidolonunchained.capability.CapabilityHandler.PATRON_DATA_CAPABILITY).orElse(null);
            if (patronData == null) {
                sendError(player, "Failed to access patron data");
                return false;
            }
            
            ResourceLocation currentPatron = patronData.getPatron(player);
            if (currentPatron == null) {
                sendInfo(player, "You don't have a patron to abandon");
                return false;
            }
            
            DatapackDeity deity = DatapackDeityManager.getDeity(currentPatron);
            if (deity != null) {
                // 🎯 DYNAMIC ABANDON BEHAVIOR - Use deity configuration
                IReputation reputation = player.level().getCapability(IReputation.INSTANCE).orElse(null);
                if (reputation != null) {
                    double currentRep = reputation.getReputation(player.getUUID(), currentPatron);
                    
                    if (deity.shouldResetReputationOnAbandon()) {
                        // Complete reputation reset
                        reputation.setReputation(player.getUUID(), currentPatron, 0.0);
                        LOGGER.info("🔄 Reset reputation to 0 for {} abandoning deity {}", 
                            player.getName().getString(), currentPatron);
                        sendWarning(player, "All reputation with " + deity.getDisplayName() + " has been lost");
                    } else {
                        // Apply penalty percentage
                        double penalty = currentRep * deity.getAbandonPenalty();
                        reputation.subtractReputation(player.getUUID(), currentPatron, penalty);
                        LOGGER.info("🔻 Applied {} penalty ({} lost) for {} abandoning deity {}", 
                            deity.getAbandonPenalty(), penalty, player.getName().getString(), currentPatron);
                        sendWarning(player, "Lost " + (int)penalty + " reputation with " + deity.getDisplayName());
                    }
                }
                
                // Show deity-specific abandon message
                sendError(player, deity.getAbandonMessage());
            }
            
            patronData.setPatron(player, null);
            
            return true;
            
        } catch (Exception e) {
            LOGGER.error("Error abandoning patron for player {}", player.getName().getString(), e);
            sendError(player, "An error occurred while abandoning patron");
            return false;
        }
    }
    
    /**
     * Gets patron status information for a player.
     */
    public static void getPatronStatus(ServerPlayer player) {
        try {
            IPatronData patronData = player.level().getCapability(com.bluelotuscoding.eidolonunchained.capability.CapabilityHandler.PATRON_DATA_CAPABILITY).orElse(null);
            if (patronData == null) {
                sendError(player, "Failed to access patron data");
                return;
            }
            
            ResourceLocation currentPatron = patronData.getPatron(player);
            if (currentPatron == null) {
                sendInfo(player, "You don't have a patron deity");
                return;
            }
            
            DatapackDeity deity = DatapackDeityManager.getDeity(currentPatron);
            if (deity != null) {
                String title = patronData.getTitle(player);
                sendInfo(player, "Patron: " + deity.getDisplayName());
                if (title != null && !title.isEmpty()) {
                    sendInfo(player, "Title: " + title);
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Error getting patron status for player {}", player.getName().getString(), e);
            sendError(player, "An error occurred while getting patron status");
        }
    }
    
    /**
     * Handles reputation changes with conflict detection.
     */
    public static void handleReputationChange(Player player, ResourceLocation gainedRepDeity, double amount) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        
        try {
            IPatronData patronData = serverPlayer.level().getCapability(com.bluelotuscoding.eidolonunchained.capability.CapabilityHandler.PATRON_DATA_CAPABILITY).orElse(null);
            if (patronData == null) return;
            
            ResourceLocation patronDeity = patronData.getPatron(serverPlayer);
            if (patronDeity == null) return;
            
            // Check for conflicts with patron deity
            if (!patronDeity.equals(gainedRepDeity) && 
                patronData.areOpposingDeities(patronDeity, gainedRepDeity)) {
                
                // Apply penalty to patron relationship
                IReputation reputation = serverPlayer.level().getCapability(IReputation.INSTANCE).orElse(null);
                if (reputation != null) {
                    double penalty = amount * 0.5; // 50% of gained reputation as penalty
                    reputation.subtractReputation(serverPlayer.getUUID(), patronDeity, penalty);
                    
                    DatapackDeity patron = DatapackDeityManager.getDeity(patronDeity);
                    DatapackDeity gainedRep = DatapackDeityManager.getDeity(gainedRepDeity);
                    if (patron != null && gainedRep != null) {
                        sendWarning(serverPlayer, "§c" + patron.getDisplayName() + " is displeased by your actions with " + 
                                  gainedRep.getDisplayName());
                        sendWarning(serverPlayer, "§cLost " + (int)penalty + " reputation with your patron");
                    }
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Error handling reputation change for player {}", player.getName().getString(), e);
        }
    }
    
    // Utility methods for messaging
    private static void sendSuccess(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal("§a" + message));
    }
    
    private static void sendInfo(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal("§b" + message));
    }
    
    private static void sendWarning(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal("§e" + message));
    }
    
    private static void sendError(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal("§c" + message));
    }
}
