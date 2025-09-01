package com.bluelotuscoding.eidolonunchained.research.triggers;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.research.triggers.data.ResearchTrigger;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handles block interaction research triggers loaded from JSON
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class InteractionResearchTriggers {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Track triggered research per player to prevent infinite loops
    private static final Map<String, Set<String>> PLAYER_TRIGGERED_RESEARCH = new HashMap<>();
    
    @SubscribeEvent
    public static void onBlockInteraction(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        
        // Check if player has notetaking tools (required for research discovery)
        if (!hasNotetakingTools(player)) {
            return; // No tools, no research discovery
        }
        
        ResourceLocation blockType = ForgeRegistries.BLOCKS.getKey(event.getLevel().getBlockState(event.getPos()).getBlock());
        
        String playerKey = player.getUUID().toString();
        Set<String> triggeredResearch = PLAYER_TRIGGERED_RESEARCH.getOrDefault(playerKey, new HashSet<>());
        
        // Get all interaction triggers from research files
        for (Map.Entry<String, List<ResearchTrigger>> entry : ResearchTriggerLoader.getTriggersForAllResearch().entrySet()) {
            String researchId = entry.getKey();
            
            for (ResearchTrigger trigger : entry.getValue()) {
                if ("block_interaction".equals(trigger.getType()) && matchesInteractionTrigger(player, event, blockType, trigger)) {
                    // Check max_found limit
                    String trackingPrefix = researchId + ":";
                    long currentCount = triggeredResearch.stream()
                        .filter(key -> key.contains(trackingPrefix))
                        .count();
                    
                    if (currentCount >= trigger.getMaxFound()) {
                        LOGGER.debug("Player {} already triggered interaction research '{}' {} times (max: {})", 
                            player.getName().getString(), researchId, currentCount, trigger.getMaxFound());
                        continue; // Skip if already triggered enough times
                    }
                    
                    // Consume notetaking tool before creating research
                    if (!consumeNotetakingTool(player)) {
                        LOGGER.warn("Failed to consume notetaking tool for player {}, research discovery cancelled", 
                            player.getName().getString());
                        continue;
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
                                978060631 * ((net.minecraft.server.level.ServerLevel)event.getLevel()).getSeed());
                            
                            // Give the research notes to the player
                            if (!player.getInventory().add(notes)) {
                                player.drop(notes, false);
                            }
                            
                            // Track this trigger - store just researchId:timestamp for proper filtering
                            triggeredResearch.add(researchId + ":" + System.currentTimeMillis());
                            PLAYER_TRIGGERED_RESEARCH.put(playerKey, triggeredResearch);
                            
                            LOGGER.info("Gave research notes '{}' to player '{}' for interacting with '{}' ({}/{} times)", 
                                researchId, player.getName().getString(), blockType, currentCount + 1, trigger.getMaxFound());
                        } else {
                            LOGGER.warn("Research '{}' not found in Eidolon's research registry", researchId);
                        }
                    } catch (Exception e) {
                        LOGGER.error("Failed to give research notes for interaction trigger: {}", e.getMessage());
                    }
                }
            }
        }
    }
    
    private static boolean matchesInteractionTrigger(ServerPlayer player, PlayerInteractEvent.RightClickBlock event, 
                                                   ResourceLocation blockType, ResearchTrigger trigger) {
        // Check block type
        if (!blockType.equals(trigger.getBlock())) {
            return false;
        }
        
        // Check block entity NBT if specified
        CompoundTag requiredNbt = trigger.getNbt();
        if (!requiredNbt.isEmpty()) {
            BlockEntity blockEntity = event.getLevel().getBlockEntity(event.getPos());
            if (blockEntity != null) {
                CompoundTag blockNbt = blockEntity.saveWithoutMetadata();
                
                if (!containsAllTags(blockNbt, requiredNbt)) {
                    return false;
                }
            } else if (!requiredNbt.isEmpty()) {
                // Required NBT but no block entity
                return false;
            }
        }
        
        // Check item requirements
        return ItemRequirementChecker.checkItemRequirements(player, trigger.getItemRequirements());
    }
    
    private static boolean containsAllTags(CompoundTag actualNbt, CompoundTag requiredNbt) {
        for (String key : requiredNbt.getAllKeys()) {
            if (!actualNbt.contains(key)) {
                return false;
            }
            
            if (!actualNbt.get(key).equals(requiredNbt.get(key))) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Check if player has notetaking tools required for research discovery
     */
    private static boolean hasNotetakingTools(ServerPlayer player) {
        try {
            // Check for Eidolon notetaking tools only
            return player.getInventory().hasAnyMatching(stack -> {
                String itemName = stack.getItem().toString().toLowerCase();
                return itemName.contains("notetaking");
            });
        } catch (Exception e) {
            LOGGER.error("Failed to check notetaking tools: {}", e.getMessage());
            return false; // Default to no tools if check fails
        }
    }
    
    /**
     * Consume one notetaking tool from player's inventory
     */
    private static boolean consumeNotetakingTool(ServerPlayer player) {
        try {
            var inventory = player.getInventory();
            
            // Find and consume one notetaking tool
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (!stack.isEmpty()) {
                    String itemName = stack.getItem().toString().toLowerCase();
                    if (itemName.contains("notetaking")) {
                        stack.shrink(1); // Remove 1 count
                        LOGGER.debug("Consumed 1 notetaking tool from player {}", player.getName().getString());
                        return true;
                    }
                }
            }
            
            LOGGER.warn("Failed to find notetaking tool to consume for player {}", player.getName().getString());
            return false;
        } catch (Exception e) {
            LOGGER.error("Failed to consume notetaking tool: {}", e.getMessage());
            return false;
        }
    }
}