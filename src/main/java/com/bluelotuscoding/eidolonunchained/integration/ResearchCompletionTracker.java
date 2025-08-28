package com.bluelotuscoding.eidolonunchained.integration;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.data.ResearchDataManager;
import com.bluelotuscoding.eidolonunchained.research.ResearchEntry;
import com.mojang.logging.LogUtils;
import elucent.eidolon.registries.Researches;
import elucent.eidolon.api.research.Research;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listens for research completion and triggers codex chapter updates.
 * This bridges the gap between research discovery and codex chapter visibility.
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ResearchCompletionTracker {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Track player research state to detect changes
    private static final Map<String, Set<ResourceLocation>> playerKnownResearch = new ConcurrentHashMap<>();
    
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.getServer().getTickCount() % 40 != 0) return; // Check every 2 seconds
        
        // Check all online players for research changes
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            checkPlayerResearchChanges(player);
        }
    }
    
    private static void checkPlayerResearchChanges(ServerPlayer player) {
        String playerKey = player.getStringUUID();
        Set<ResourceLocation> currentResearch = getCurrentPlayerResearch(player);
        Set<ResourceLocation> lastKnownResearch = playerKnownResearch.get(playerKey);
        
        if (lastKnownResearch == null) {
            // First time checking this player - just store current state
            playerKnownResearch.put(playerKey, new HashSet<>(currentResearch));
            return;
        }
        
        // Find newly completed research
        Set<ResourceLocation> newResearch = new HashSet<>(currentResearch);
        newResearch.removeAll(lastKnownResearch);
        
        if (!newResearch.isEmpty()) {
            LOGGER.info("Player {} completed {} new research entries", player.getName().getString(), newResearch.size());
            
            // Update stored research state
            playerKnownResearch.put(playerKey, new HashSet<>(currentResearch));
            
            // Check if any research chapters should now be visible
            checkAndUpdateCodexChapters(player, newResearch);
        }
    }
    
    private static Set<ResourceLocation> getCurrentPlayerResearch(ServerPlayer player) {
        Set<ResourceLocation> research = new HashSet<>();
        
        // Get all Eidolon research the player knows
        for (Research eidolonResearch : Researches.getResearches()) {
            // if (elucent.eidolon.util.KnowledgeUtil.knowsResearch(player, eidolonResearch.getRegistryName())) {
            if (false) {  // TODO: Fix KnowledgeUtil reference
                research.add(eidolonResearch.getRegistryName());
            }
        }
        
        return research;
    }
    
    private static void checkAndUpdateCodexChapters(ServerPlayer player, Set<ResourceLocation> newResearch) {
        Map<ResourceLocation, List<ResearchEntry>> researchExtensions = ResearchDataManager.getResearchExtensions();
        
        for (Map.Entry<ResourceLocation, List<ResearchEntry>> chapterEntry : researchExtensions.entrySet()) {
            ResourceLocation chapterId = chapterEntry.getKey();
            List<ResearchEntry> entries = chapterEntry.getValue();
            
            // Check if any entry in this chapter now has its prerequisites met
            boolean chapterShouldBeVisible = false;
            
            for (ResearchEntry entry : entries) {
                boolean prerequisitesMet = true;
                
                for (ResourceLocation prereq : entry.getPrerequisites()) {
                    Research research = Researches.find(prereq);
                    // if (research == null || !elucent.eidolon.util.KnowledgeUtil.knowsResearch(player, prereq)) {
                    if (research == null) {  // TODO: Fix KnowledgeUtil reference
                        prerequisitesMet = false;
                        break;
                    }
                }
                
                if (prerequisitesMet) {
                    chapterShouldBeVisible = true;
                    LOGGER.info("Research entry '{}' prerequisites met for chapter '{}'", entry.getId(), chapterId);
                    break;
                }
            }
            
            if (chapterShouldBeVisible) {
                // Trigger codex integration to add this chapter
                LOGGER.info("Chapter '{}' should now be visible - triggering codex integration", chapterId);
                EidolonCodexIntegration.attemptIntegrationIfNeeded();
                break; // Only need to trigger integration once
            }
        }
    }
    
    /**
     * Clear tracking data when player disconnects
     */
    @SubscribeEvent 
    public static void onPlayerLogout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            playerKnownResearch.remove(player.getStringUUID());
        }
    }
}
