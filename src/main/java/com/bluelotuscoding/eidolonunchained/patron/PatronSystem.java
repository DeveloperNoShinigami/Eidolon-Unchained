package com.bluelotuscoding.eidolonunchained.patron;

import com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig;
import com.bluelotuscoding.eidolonunchained.ai.AIDeityManager;
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
 * Simplified Patron System that works with existing IPatronData interface
 */
public class PatronSystem {
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
    
    // Configuration constants
    private static final double MIN_REPUTATION_FOR_PATRON = 25.0;
    private static final double PATRON_ABANDON_PENALTY = 0.3; // 30% reputation loss
    
    /**
     * Attempts to set a player's patron deity.
     */
    public static boolean choosePatron(ServerPlayer player, ResourceLocation deityId) {
        try {
            // Get player's patron data
            IPatronData patronData = player.getCapability(IPatronData.PATRON_DATA).orElse(null);
            if (patronData == null) {
                sendError(player, "Failed to access patron data");
                return false;
            }
            
            // Validate deity exists
            DatapackDeity deity = DatapackDeityManager.getDeity(deityId);
            if (deity == null) {
                sendError(player, "Unknown deity: " + deityId);
                return false;
            }
            
            // Get AI configuration for patron requirements
            AIDeityConfig aiConfig = AIDeityManager.getInstance().getAIConfig(deityId);
            if (aiConfig == null || aiConfig.patronConfig == null) {
                sendError(player, "Deity does not accept patrons");
                return false;
            }
            
            // Check if already patron of this deity
            ResourceLocation currentPatron = patronData.getPatron(player);
            if (deityId.equals(currentPatron)) {
                sendInfo(player, "You are already a follower of " + deity.getDisplayName());
                return true;
            }
            
            // Check minimum reputation requirement
            IReputation reputation = player.getCapability(IReputation.INSTANCE).orElse(null);
            if (reputation != null) {
                double currentRep = reputation.getReputation(player.getUUID(), deityId);
                if (currentRep < MIN_REPUTATION_FOR_PATRON) {
                    sendError(player, "You need at least " + MIN_REPUTATION_FOR_PATRON + " reputation with " + 
                             deity.getDisplayName() + " to become their follower (current: " + (int)currentRep + ")");
                    return false;
                }
            }
            
            // Apply patron switch
            patronData.setPatron(player, deityId);
            sendSuccess(player, "You are now a follower of " + deity.getDisplayName());
            
            return true;
            
        } catch (Exception e) {
            LOGGER.error("Error choosing patron for player {}", player.getName().getString(), e);
            sendError(player, "An error occurred while choosing patron");
            return false;
        }
    }
    
    /**
     * Removes a player's patron.
     */
    public static boolean abandonPatron(ServerPlayer player) {
        try {
            IPatronData patronData = player.getCapability(IPatronData.PATRON_DATA).orElse(null);
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
                // Apply reputation penalty
                IReputation reputation = player.getCapability(IReputation.INSTANCE).orElse(null);
                if (reputation != null) {
                    double currentRep = reputation.getReputation(player.getUUID(), currentPatron);
                    double penalty = currentRep * PATRON_ABANDON_PENALTY;
                    reputation.subtractReputation(player.getUUID(), currentPatron, penalty);
                    sendWarning(player, "Lost " + (int)penalty + " reputation with " + deity.getDisplayName());
                }
            }
            
            patronData.setPatron(player, null);
            sendInfo(player, "You have abandoned your patron");
            
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
            IPatronData patronData = player.getCapability(IPatronData.PATRON_DATA).orElse(null);
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
            IPatronData patronData = serverPlayer.getCapability(IPatronData.PATRON_DATA).orElse(null);
            if (patronData == null) return;
            
            ResourceLocation patronDeity = patronData.getPatron(serverPlayer);
            if (patronDeity == null) return;
            
            // Check for conflicts with patron deity
            if (!patronDeity.equals(gainedRepDeity) && 
                patronData.areOpposingDeities(patronDeity, gainedRepDeity)) {
                
                // Apply penalty to patron relationship
                IReputation reputation = serverPlayer.getCapability(IReputation.INSTANCE).orElse(null);
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
