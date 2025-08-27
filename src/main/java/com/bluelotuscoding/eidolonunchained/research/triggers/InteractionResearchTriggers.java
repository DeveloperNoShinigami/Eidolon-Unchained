package com.bluelotuscoding.eidolonunchained.research.triggers;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.research.triggers.data.ResearchTrigger;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
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
                    // Grant research using Eidolon's system
                    try {
                        elucent.eidolon.util.KnowledgeUtil.grantResearchNoToast(player, 
                            new ResourceLocation("eidolonunchained", researchId));
                        LOGGER.info("Granted research '{}' to player '{}' for interacting with '{}'", 
                            researchId, player.getName().getString(), blockType);
                    } catch (Exception e) {
                        LOGGER.error("Failed to grant research for interaction trigger: {}", e.getMessage());
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