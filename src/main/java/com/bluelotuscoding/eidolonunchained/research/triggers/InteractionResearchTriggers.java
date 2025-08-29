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

import java.util.List;
import java.util.Map;

/**
 * Handles block interaction research triggers loaded from JSON
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class InteractionResearchTriggers {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    @SubscribeEvent
    public static void onBlockInteraction(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        
        ResourceLocation blockType = ForgeRegistries.BLOCKS.getKey(event.getLevel().getBlockState(event.getPos()).getBlock());
        
        // Get all interaction triggers from research files
        for (Map.Entry<String, List<ResearchTrigger>> entry : ResearchTriggerLoader.getTriggersForAllResearch().entrySet()) {
            String researchId = entry.getKey();
            
            for (ResearchTrigger trigger : entry.getValue()) {
                if ("block_interaction".equals(trigger.getType()) && matchesInteractionTrigger(player, event, blockType, trigger)) {
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
                            
                            LOGGER.info("Gave research notes '{}' to player '{}' for interacting with '{}'", 
                                researchId, player.getName().getString(), blockType);
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
}