package com.bluelotuscoding.eidolonunchained.research.triggers;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.research.triggers.data.ResearchTrigger;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

/**
 * Handles ritual-based research triggers loaded from JSON
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RitualResearchTriggers {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * Manually trigger ritual research when a ritual is completed
     * This should be called from ritual completion code
     */
    public static void onRitualCompleted(ServerPlayer player, ResourceLocation ritualId) {
        // Get all ritual triggers from research files
        for (Map.Entry<String, List<ResearchTrigger>> entry : ResearchTriggerLoader.getTriggersForAllResearch().entrySet()) {
            String researchId = entry.getKey();
            
            for (ResearchTrigger trigger : entry.getValue()) {
                if ("ritual".equals(trigger.getType()) && matchesRitualTrigger(player, ritualId, trigger)) {
                    // Grant research using Eidolon's system
                    try {
                        elucent.eidolon.util.KnowledgeUtil.grantResearchNoToast(player, 
                            new ResourceLocation("eidolonunchained", researchId));
                        LOGGER.info("Granted research '{}' to player '{}' for completing ritual '{}'", 
                            researchId, player.getName().getString(), ritualId);
                    } catch (Exception e) {
                        LOGGER.error("Failed to grant research for ritual trigger: {}", e.getMessage());
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
