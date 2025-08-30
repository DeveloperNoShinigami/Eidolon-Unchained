package com.bluelotuscoding.eidolonunchained.events;

import com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig;
import com.bluelotuscoding.eidolonunchained.ai.AIDeityManager;
import com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager;
import com.bluelotuscoding.eidolonunchained.deity.DatapackDeity;
import com.bluelotuscoding.eidolonunchained.patron.PatronSystem;
import com.bluelotuscoding.eidolonunchained.capability.CapabilityHandler;
import elucent.eidolon.capability.IReputation;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.Map;

import java.util.List;
import java.util.Map;

/**
 * Handles ritual-based patron selection using AI deity configurations
 */
@Mod.EventBusSubscriber(modid = "eidolonunchained", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RitualPatronHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // EMERGENCY DISABLED: Potential cause of game freezing due to excessive deity iteration
    // @SubscribeEvent
    public static void onRitualComplete(RitualCompleteEvent event) {
        ServerPlayer player = event.getPlayer();
        ResourceLocation ritualId = event.getRitualId();
        
        // Check if this is a patron selection ritual
        if (isPatronSelectionRitual(ritualId)) {
            handlePatronSelectionRitual(player, ritualId);
        }
    }
    
    /**
     * Check if the completed ritual is a patron selection ritual
     */
    private static boolean isPatronSelectionRitual(ResourceLocation ritualId) {
        // Check all AI deity configurations for ritual integration
        for (Map.Entry<ResourceLocation, DatapackDeity> entry : DatapackDeityManager.getAllDeities().entrySet()) {
            ResourceLocation deityId = entry.getKey();
            DatapackDeity deity = entry.getValue();
            AIDeityConfig aiConfig = AIDeityManager.getInstance().getAIConfig(deityId);
            if (aiConfig != null && aiConfig.ritual_integration != null) {
                Map<String, Object> patronRitual = (Map<String, Object>) aiConfig.ritual_integration.get("patron_selection_ritual");
                if (patronRitual != null) {
                    Boolean enabled = (Boolean) patronRitual.get("enabled");
                    String configRitualId = (String) patronRitual.get("ritual_id");
                    
                    if (enabled != null && enabled && configRitualId != null && 
                        ritualId.toString().equals(configRitualId)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * Handle patron selection ritual completion
     */
    private static void handlePatronSelectionRitual(ServerPlayer player, ResourceLocation ritualId) {
        try {
            // Find the deity associated with this ritual
            DatapackDeity targetDeity = null;
            AIDeityConfig targetConfig = null;
            
            for (Map.Entry<ResourceLocation, DatapackDeity> entry : DatapackDeityManager.getAllDeities().entrySet()) {
                ResourceLocation deityId = entry.getKey();
                DatapackDeity deity = entry.getValue();
                AIDeityConfig aiConfig = AIDeityManager.getInstance().getAIConfig(deityId);
                if (aiConfig != null && aiConfig.ritual_integration != null) {
                    Map<String, Object> patronRitual = (Map<String, Object>) aiConfig.ritual_integration.get("patron_selection_ritual");
                    if (patronRitual != null) {
                        String configRitualId = (String) patronRitual.get("ritual_id");
                        if (ritualId.toString().equals(configRitualId)) {
                            targetDeity = deity;
                            targetConfig = aiConfig;
                            break;
                        }
                    }
                }
            }
            
            if (targetDeity == null || targetConfig == null) {
                LOGGER.warn("Could not find deity configuration for patron ritual: {}", ritualId);
                return;
            }
            
            Map<String, Object> patronRitual = (Map<String, Object>) targetConfig.ritual_integration.get("patron_selection_ritual");
            Map<String, Object> requirements = (Map<String, Object>) patronRitual.get("requirements");
            
            // Check requirements
            if (!checkRitualRequirements(player, targetDeity.getId(), requirements)) {
                executeFailureCommands(player, patronRitual);
                return;
            }
            
            // Execute patron selection
            boolean success = PatronSystem.choosePatron(player, targetDeity.getId());
            
            if (success) {
                executeCompletionCommands(player, patronRitual);
                LOGGER.info("Player {} successfully chose {} as patron through ritual", 
                           player.getName().getString(), targetDeity.getId());
            } else {
                executeFailureCommands(player, patronRitual);
                LOGGER.warn("Failed to set patron {} for player {} through ritual", 
                           targetDeity.getId(), player.getName().getString());
            }
            
        } catch (Exception e) {
            LOGGER.error("Error handling patron selection ritual for player {}", 
                        player.getName().getString(), e);
            player.sendSystemMessage(Component.literal("§cAn error occurred during the ritual. The gods are displeased."));
        }
    }
    
    /**
     * Check if player meets ritual requirements
     */
    private static boolean checkRitualRequirements(ServerPlayer player, ResourceLocation deityId, Map<String, Object> requirements) {
        if (requirements == null) {
            return true;
        }
        
        // Check minimum reputation
        if (requirements.containsKey("min_reputation")) {
            int minRep = (Integer) requirements.get("min_reputation");
            double currentRep = player.level().getCapability(IReputation.INSTANCE)
                .map(rep -> rep.getReputation(player, deityId))
                .orElse(0.0);
            
            if (currentRep < minRep) {
                player.sendSystemMessage(Component.literal("§cYou need at least " + minRep + 
                    " reputation with this deity (current: " + (int)currentRep + ")"));
                return false;
            }
        }
        
        // Check cooldown
        if (requirements.containsKey("cooldown_hours")) {
            return player.level().getCapability(CapabilityHandler.PATRON_DATA_CAPABILITY)
                .map(patronData -> {
                    if (patronData.getPatron(player) != null) {
                        // TODO: Check patron switch cooldown
                        return true; // For now, allow switching
                    }
                    return true;
                }).orElse(false);
        }
        
        // Check forbidden patrons
        if (requirements.containsKey("forbidden_patrons")) {
            return player.level().getCapability(CapabilityHandler.PATRON_DATA_CAPABILITY)
                .map(patronData -> {
                    ResourceLocation currentPatron = patronData.getPatron(player);
                    if (currentPatron != null) {
                        List<String> forbidden = (List<String>) requirements.get("forbidden_patrons");
                        if (forbidden.contains(currentPatron.toString())) {
                            player.sendSystemMessage(Component.literal("§cYou cannot serve this deity while following " + currentPatron));
                            return false;
                        }
                    }
                    return true;
                }).orElse(true);
        }
        
        return true;
    }
    
    /**
     * Execute completion commands from ritual config
     */
    private static void executeCompletionCommands(ServerPlayer player, Map<String, Object> patronRitual) {
        List<String> commands = (List<String>) patronRitual.get("completion_commands");
        if (commands != null && !commands.isEmpty()) {
            executeCommands(player, commands);
        }
    }
    
    /**
     * Execute failure commands from ritual config
     */
    private static void executeFailureCommands(ServerPlayer player, Map<String, Object> patronRitual) {
        List<String> commands = (List<String>) patronRitual.get("failure_commands");
        if (commands != null && !commands.isEmpty()) {
            executeCommands(player, commands);
        }
    }
    
    /**
     * Execute a list of commands with the player as the source
     */
    private static void executeCommands(ServerPlayer player, List<String> commands) {
        try {
            ServerLevel level = (ServerLevel) player.level();
            CommandSourceStack commandSource = player.createCommandSourceStack().withPermission(2);
            
            for (String command : commands) {
                if (command != null && !command.trim().isEmpty()) {
                    // Replace @p with the player's name for targeting
                    String processedCommand = command.replace("@p", player.getName().getString());
                    
                    try {
                        level.getServer().getCommands().performPrefixedCommand(commandSource, processedCommand);
                    } catch (Exception e) {
                        LOGGER.warn("Failed to execute ritual command '{}' for player {}: {}", 
                                   command, player.getName().getString(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error executing ritual commands for player {}", player.getName().getString(), e);
        }
    }
}
