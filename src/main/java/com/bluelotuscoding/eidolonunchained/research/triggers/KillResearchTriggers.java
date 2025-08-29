package com.bluelotuscoding.eidolonunchained.research.triggers;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.research.triggers.data.ResearchTrigger;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

/**
 * Handles kill-based research triggers loaded from JSON
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class KillResearchTriggers {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    @SubscribeEvent
    public static void onEntityKilled(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        
        LivingEntity killedEntity = event.getEntity();
        ResourceLocation entityType = ForgeRegistries.ENTITY_TYPES.getKey(killedEntity.getType());
        
        LOGGER.debug("Player {} killed entity: {}", player.getName().getString(), entityType);
        
        // Get all kill triggers from research files
        Map<String, List<ResearchTrigger>> allTriggers = ResearchTriggerLoader.getTriggersForAllResearch();
        LOGGER.debug("Checking {} research entries for kill triggers", allTriggers.size());
        
        for (Map.Entry<String, List<ResearchTrigger>> entry : allTriggers.entrySet()) {
            String researchId = entry.getKey();
            
            for (ResearchTrigger trigger : entry.getValue()) {
                if ("kill_entity".equals(trigger.getType()) && matchesKillTrigger(player, killedEntity, entityType, trigger)) {
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
                            
                            LOGGER.info("Gave research notes '{}' to player '{}' for killing '{}'", 
                                researchId, player.getName().getString(), entityType);
                        } else {
                            LOGGER.warn("Research '{}' not found in Eidolon's research registry", researchId);
                        }
                    } catch (Exception e) {
                        LOGGER.error("Failed to give research notes for kill trigger: {}", e.getMessage());
                    }
                }
            }
        }
    }
    
    private static boolean matchesKillTrigger(ServerPlayer player, LivingEntity killedEntity, 
                                            ResourceLocation entityType, ResearchTrigger trigger) {
        // Check entity type
        if (!entityType.equals(trigger.getEntity())) {
            return false;
        }
        
        // Check entity NBT if specified
        CompoundTag requiredNbt = trigger.getNbt();
        if (!requiredNbt.isEmpty()) {
            CompoundTag entityNbt = new CompoundTag();
            killedEntity.saveWithoutId(entityNbt);
            
            if (!containsAllTags(entityNbt, requiredNbt)) {
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