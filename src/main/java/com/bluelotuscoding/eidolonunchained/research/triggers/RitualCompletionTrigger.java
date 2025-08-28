package com.bluelotuscoding.eidolonunchained.research.triggers;

import com.bluelotuscoding.eidolonunchained.research.triggers.data.ResearchTrigger;
import elucent.eidolon.util.KnowledgeUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

/**
 * Handles ritual completion triggers for research progression.
 * Detects when players complete rituals and grants appropriate research.
 */
@Mod.EventBusSubscriber(modid = "eidolonunchained", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RitualCompletionTrigger {

    /**
     * Event handler for ritual completion.
     * This should be called when a ritual is completed.
     */
    public static void onRitualCompleted(ServerPlayer player, ResourceLocation ritualId) {
        if (player == null || ritualId == null) return;

        // Get all research entries that have ritual triggers
        Map<String, Map<String, ResearchTrigger>> allTriggers = ResearchTriggerLoader.getTriggersForAllResearch();
        
        for (Map.Entry<String, Map<String, ResearchTrigger>> researchEntry : allTriggers.entrySet()) {
            String researchId = researchEntry.getKey();
            Map<String, ResearchTrigger> triggers = researchEntry.getValue();
            
            for (ResearchTrigger trigger : triggers.values()) {
                if ("ritual".equals(trigger.getType()) && ritualId.toString().equals(trigger.getRitualString())) {
                    // Grant the research to the player
                    KnowledgeUtil.grantResearchNoToast(player, new ResourceLocation("eidolonunchained", researchId));
                    
                    // Log the research grant
                    com.bluelotuscoding.eidolonunchained.EidolonUnchained.LOGGER.info(
                        "Granted research '{}' to player '{}' for completing ritual '{}'", 
                        researchId, player.getName().getString(), ritualId
                    );
                    break;
                }
            }
        }
    }

    /**
     * Manual trigger for testing purposes - can be called via commands
     */
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        // This is just a placeholder event - ritual completion should be detected
        // through the actual ritual system integration
    }
}