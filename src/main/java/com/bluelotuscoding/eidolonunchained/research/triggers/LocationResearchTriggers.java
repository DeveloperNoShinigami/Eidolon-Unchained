package com.bluelotuscoding.eidolonunchained.research.triggers;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.research.triggers.data.ResearchTrigger;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles location-based research triggers (dimension, biome, structure) loaded from JSON
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LocationResearchTriggers {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<UUID, Integer> PLAYER_CHECK_TIMERS = new HashMap<>();
    private static final int CHECK_INTERVAL = 60; // Check every 3 seconds (60 ticks)
    
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        
        // Rate limit the checks
        UUID playerId = player.getUUID();
        int timer = PLAYER_CHECK_TIMERS.getOrDefault(playerId, 0);
        
        if (timer++ >= CHECK_INTERVAL) {
            PLAYER_CHECK_TIMERS.put(playerId, 0);
            checkLocationTriggers(player);
        } else {
            PLAYER_CHECK_TIMERS.put(playerId, timer);
        }
    }
    
    private static void checkLocationTriggers(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos playerPos = player.blockPosition();
        
        // Get all location triggers from research files
        for (Map.Entry<String, List<ResearchTrigger>> entry : ResearchTriggerLoader.getTriggersForAllResearch().entrySet()) {
            String researchId = entry.getKey();
            
            for (ResearchTrigger trigger : entry.getValue()) {
                String triggerType = trigger.getType();
                if (("dimension".equals(triggerType) || "biome".equals(triggerType) || "structure".equals(triggerType)) && 
                    matchesLocationTrigger(player, level, playerPos, trigger)) {
                    
                    // Grant research using Eidolon's system
                    try {
                        elucent.eidolon.util.KnowledgeUtil.grantResearchNoToast(player, 
                            new ResourceLocation("eidolonunchained", researchId));
                        LOGGER.info("Granted research '{}' to player '{}' for location trigger", 
                            researchId, player.getName().getString());
                    } catch (Exception e) {
                        LOGGER.error("Failed to grant research for location trigger: {}", e.getMessage());
                    }
                }
            }
        }
    }
    
    private static boolean matchesLocationTrigger(ServerPlayer player, ServerLevel level, BlockPos playerPos, ResearchTrigger trigger) {
        // Check item requirements first
        if (!ItemRequirementChecker.checkItemRequirements(player, trigger.getItemRequirements())) {
            return false;
        }
        
        String triggerType = trigger.getType();
        
        switch (triggerType) {
            case "dimension":
                return checkDimensionTrigger(level, trigger);
            case "biome":
                return checkBiomeTrigger(level, playerPos, trigger);
            case "structure":
                return checkStructureTrigger(level, playerPos, trigger);
            default:
                return false;
        }
    }
    
    private static boolean checkDimensionTrigger(ServerLevel level, ResearchTrigger trigger) {
        ResourceLocation currentDimension = level.dimension().location();
        return currentDimension.equals(trigger.getDimension());
    }
    
    private static boolean checkBiomeTrigger(ServerLevel level, BlockPos playerPos, ResearchTrigger trigger) {
        Holder<Biome> biomeHolder = level.getBiome(playerPos);
        ResourceLocation currentBiome = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME)
            .getKey(biomeHolder.value());
        
        boolean biomeMatches = currentBiome != null && currentBiome.equals(trigger.getBiome());
        
        // Check coordinates if specified
        if (trigger.getCoordinates() != null) {
            ResearchTrigger.Coordinates coords = trigger.getCoordinates();
            if (coords.getX() != null && coords.getZ() != null) {
                double distance = Math.sqrt(Math.pow(playerPos.getX() - coords.getX(), 2) + 
                                          Math.pow(playerPos.getZ() - coords.getZ(), 2));
                boolean inRange = distance <= coords.getRange();
                
                // If coordinates are specified, both biome and location must match
                return biomeMatches && inRange;
            }
        }
        
        return biomeMatches;
    }
    
    private static boolean checkStructureTrigger(ServerLevel level, BlockPos playerPos, ResearchTrigger trigger) {
        try {
            // Get structure registry
            var structureRegistry = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
            
            // Find the target structure
            Structure targetStructure = structureRegistry.get(trigger.getStructure());
            if (targetStructure == null) {
                return false;
            }
            
            // Check coordinates if specified
            if (trigger.getCoordinates() != null) {
                ResearchTrigger.Coordinates coords = trigger.getCoordinates();
                if (coords.getX() != null && coords.getZ() != null) {
                    double distance = Math.sqrt(Math.pow(playerPos.getX() - coords.getX(), 2) + 
                                              Math.pow(playerPos.getZ() - coords.getZ(), 2));
                    
                    // For coordinate-based structure triggers, just check if we're in range of the coordinates
                    return distance <= coords.getRange();
                }
            }
            
            // If no coordinates specified, check if structure exists at this position
            var structureResult = level.structureManager().getStructureWithPieceAt(playerPos, targetStructure);
            return structureResult.isValid();
            
        } catch (Exception e) {
            LOGGER.error("Failed to check structure trigger: {}", e.getMessage());
            return false;
        }
    }
}