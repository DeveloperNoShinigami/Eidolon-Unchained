package com.bluelotuscoding.eidolonunchained.research.triggers;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.research.triggers.data.ResearchTrigger;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
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
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        
        UUID playerId = serverPlayer.getUUID();
        int timer = PLAYER_CHECK_TIMERS.getOrDefault(playerId, 0);
        
        if (timer >= CHECK_INTERVAL) {
            checkLocationTriggers(serverPlayer);
            PLAYER_CHECK_TIMERS.put(playerId, 0);
        } else {
            PLAYER_CHECK_TIMERS.put(playerId, timer + 1);
        }
    }
    
    /**
     * Check all location-based triggers for a player
     */
    private static void checkLocationTriggers(ServerPlayer player) {
        Map<String, List<ResearchTrigger>> allTriggers = ResearchTriggerLoader.getTriggersForAllResearch();
        
        for (Map.Entry<String, List<ResearchTrigger>> entry : allTriggers.entrySet()) {
            String researchId = entry.getKey();
            
            for (ResearchTrigger trigger : entry.getValue()) {
                if (shouldCheckTrigger(trigger, player)) {
                    giveResearchNote(player, researchId);
                }
            }
        }
    }
    
    /**
     * Check if a trigger should activate based on location conditions
     */
    private static boolean shouldCheckTrigger(ResearchTrigger trigger, ServerPlayer player) {
        try {
            // Early exit if no location conditions
            if (trigger.getEntity() != null || trigger.getBlock() != null || trigger.getRitual() != null) {
                return false; // These are handled by other trigger classes
            }
            
            // Check dimension triggers
            if (trigger.getDimension() != null) {
                boolean dimensionMatches = checkDimensionTrigger(player, trigger.getDimension().toString());
                if (dimensionMatches) return true;
            }
            
            // Check biome triggers  
            if (trigger.getBiome() != null) {
                boolean biomeMatches = checkBiomeTrigger(player, trigger.getBiome().toString());
                if (biomeMatches) return true;
            }
            
            // Check structure triggers
            if (trigger.getStructure() != null) {
                boolean structureMatches = checkStructureTrigger(player, trigger.getStructure().toString());
                if (structureMatches) return true;
            }
            
            return false;
            
        } catch (Exception e) {
            LOGGER.error("Failed to check trigger conditions for research: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Give a research note to the player instead of directly granting research
     */
    private static void giveResearchNote(ServerPlayer player, String researchId) {
        try {
            // Get the research note item from Eidolon's registry
            var researchNoteItem = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
                new ResourceLocation("eidolon", "research_notes"));
            
            if (researchNoteItem == null) {
                LOGGER.error("Could not find research_notes item from Eidolon");
                return;
            }
            
            // Create ItemStack with proper NBT
            ItemStack researchNote = new ItemStack(researchNoteItem);
            CompoundTag tag = researchNote.getOrCreateTag();
            tag.putString("research", researchId);
            tag.putInt("stepsDone", 0);
            
            // Add worldSeed for research table compatibility
            ServerLevel serverLevel = (ServerLevel) player.level();
            long worldSeed = serverLevel.getSeed();
            tag.putLong("worldSeed", worldSeed);
            
            // Give to player
            if (!player.addItem(researchNote)) {
                player.drop(researchNote, false); // Drop if inventory full
            }
            
            LOGGER.info("Gave research note for '{}' to player {}", researchId, player.getName().getString());
            
        } catch (Exception e) {
            LOGGER.error("Failed to give research note for '{}': {}", researchId, e.getMessage());
        }
    }
    
    /**
     * Check dimension condition
     */
    private static boolean checkDimensionTrigger(ServerPlayer player, String dimension) {
        try {
            String currentDimension = player.level().dimension().location().toString();
            return currentDimension.equals(dimension);
        } catch (Exception e) {
            LOGGER.error("Failed to check dimension trigger: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Check biome condition  
     */
    private static boolean checkBiomeTrigger(ServerPlayer player, String biome) {
        try {
            BlockPos playerPos = player.blockPosition();
            Holder<Biome> biomeHolder = player.level().getBiome(playerPos);
            ResourceLocation biomeLocation = biomeHolder.unwrapKey()
                .map(key -> key.location())
                .orElse(null);
                
            if (biomeLocation == null) {
                return false;
            }
            
            String biomeName = biomeLocation.toString();
            return biomeName.equals(biome);
        } catch (Exception e) {
            LOGGER.error("Failed to check biome trigger: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Check structure condition
     */
    private static boolean checkStructureTrigger(ServerPlayer player, String structure) {
        try {
            BlockPos playerPos = player.blockPosition();
            ServerLevel level = (ServerLevel) player.level();
            
            ResourceLocation structureLocation = new ResourceLocation(structure);
            Structure targetStructure = level.registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
                .get(structureLocation);
                
            if (targetStructure == null) {
                return false;
            }
            
            var structureResult = level.structureManager().getStructureWithPieceAt(playerPos, targetStructure);
            return structureResult.isValid();
            
        } catch (Exception e) {
            LOGGER.error("Failed to check structure trigger: {}", e.getMessage());
            return false;
        }
    }
}