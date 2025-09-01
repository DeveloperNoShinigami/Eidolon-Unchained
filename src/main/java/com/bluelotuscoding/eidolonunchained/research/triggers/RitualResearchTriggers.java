package com.bluelotuscoding.eidolonunchained.research.triggers;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.research.triggers.data.ResearchTrigger;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Set;

/**
 * Handles ritual-based research triggers loaded from JSON
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RitualResearchTriggers {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Track triggered research per player to prevent infinite loops
    private static final Map<String, Set<String>> PLAYER_TRIGGERED_RESEARCH = new HashMap<>();
    
    /**
     * Manually trigger ritual research when a ritual is completed
     * This should be called from ritual completion code
     */
    public static void onRitualCompleted(ServerPlayer player, ResourceLocation ritualId) {
        String playerKey = player.getUUID().toString();
        Set<String> triggeredResearch = PLAYER_TRIGGERED_RESEARCH.getOrDefault(playerKey, new HashSet<>());
        
        // Get all ritual triggers from research files
        for (Map.Entry<String, List<ResearchTrigger>> entry : ResearchTriggerLoader.getTriggersForAllResearch().entrySet()) {
            String researchId = entry.getKey();
            
            for (ResearchTrigger trigger : entry.getValue()) {
                if ("ritual".equals(trigger.getType()) && matchesRitualTrigger(player, ritualId, trigger)) {
                    // Check max_found limit
                    String trackingPrefix = researchId + ":";
                    long currentCount = triggeredResearch.stream()
                        .filter(key -> key.contains(trackingPrefix))
                        .count();
                    
                    if (currentCount >= trigger.getMaxFound()) {
                        LOGGER.debug("Player {} already triggered ritual research '{}' {} times (max: {})", 
                            player.getName().getString(), researchId, currentCount, trigger.getMaxFound());
                        continue; // Skip if already triggered enough times
                    }
                    
                    // Create research notes instead of directly granting research
                    try {
                        // First check if this research exists in Eidolon's system
                        elucent.eidolon.api.research.Research research = elucent.eidolon.registries.Researches.find(
                            new ResourceLocation("eidolonunchained", researchId));
                        
                        if (research != null) {
                            // Create research notes item like NotetakingToolsItem does
                            ItemStack notes = new ItemStack(elucent.eidolon.registries.Registry.RESEARCH_NOTES.get(), 1);
                            var tag = notes.getOrCreateTag();
                            tag.putString("research", research.getRegistryName().toString());
                            tag.putInt("stepsDone", 0);
                            tag.putLong("worldSeed", elucent.eidolon.common.tile.ResearchTableTileEntity.SEED + 
                                978060631 * ((net.minecraft.server.level.ServerLevel)player.level()).getSeed());
                            
                            // Give the research notes to the player
                            if (!player.getInventory().add(notes)) {
                                player.drop(notes, false);
                            }
                            
                            // Track this trigger
                            triggeredResearch.add(researchId + ":" + System.currentTimeMillis());
                            PLAYER_TRIGGERED_RESEARCH.put(playerKey, triggeredResearch);
                            
                            LOGGER.info("Gave research notes '{}' to player '{}' for completing ritual '{}' ({}/{} times)", 
                                researchId, player.getName().getString(), ritualId, currentCount + 1, trigger.getMaxFound());
                        } else {
                            LOGGER.warn("Research '{}' not found in Eidolon's research registry", researchId);
                        }
                    } catch (Exception e) {
                        LOGGER.error("Failed to give research notes for ritual trigger: {}", e.getMessage());
                    }
                }
            }
        }
    }
    
    private static boolean matchesRitualTrigger(ServerPlayer player, ResourceLocation ritualId, ResearchTrigger trigger) {
        // Check ritual type
        if (!ritualId.equals(trigger.getRitual())) {
            return false;
        }
        
        // Check item requirements
        return ItemRequirementChecker.checkItemRequirements(player, trigger.getItemRequirements());
    }
}
