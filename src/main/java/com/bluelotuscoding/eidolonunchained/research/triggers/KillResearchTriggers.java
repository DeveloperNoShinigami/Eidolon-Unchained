package com.bluelotuscoding.eidolonunchained.research.triggers;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.research.triggers.data.ResearchTrigger;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
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
        
        // Get all kill triggers from research files
        for (Map.Entry<String, List<ResearchTrigger>> entry : ResearchTriggerLoader.getTriggersForAllResearch().entrySet()) {
            String researchId = entry.getKey();
            
            for (ResearchTrigger trigger : entry.getValue()) {
                if ("kill_entity".equals(trigger.getType()) && matchesKillTrigger(player, killedEntity, entityType, trigger)) {
                    // Grant research using Eidolon's system
                    try {
                        elucent.eidolon.util.KnowledgeUtil.grantResearchNoToast(player, 
                            new ResourceLocation("eidolonunchained", researchId));
                        LOGGER.info("Granted research '{}' to player '{}' for killing '{}'", 
                            researchId, player.getName().getString(), entityType);
                    } catch (Exception e) {
                        LOGGER.error("Failed to grant research for kill trigger: {}", e.getMessage());
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