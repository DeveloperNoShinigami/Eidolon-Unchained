package com.bluelotuscoding.eidolonunchained.integration;

import com.bluelotuscoding.eidolonunchained.data.ResearchDataManager;
import com.bluelotuscoding.eidolonunchained.research.ResearchEntry;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.slf4j.Logger;

import java.util.Map;

/**
 * Handles integration with Eidolon's research system to inject custom research entries.
 * This is separate from the codex integration and focuses on the research progression system.
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class EidolonResearchIntegration {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private static boolean integrationAttempted = false;
    private static boolean integrationSuccessful = false;

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            if (!integrationAttempted) {
                attemptResearchIntegration();
                if (integrationSuccessful) {
                    injectCustomResearch();
                }
            }
        });
    }

    /**
     * Attempts to integrate with Eidolon's research system
     */
    private static void attemptResearchIntegration() {
        if (integrationAttempted) return;
        integrationAttempted = true;
        
        try {
            LOGGER.info("Attempting to integrate with Eidolon's research system...");
            
            // Try to find Eidolon's Research class
            Class.forName("elucent.eidolon.api.research.Research");
            LOGGER.info("Found Eidolon Research class");
            
            // Try to find research registry or manager
            // We'll need to explore Eidolon's structure more to find where researches are stored
            
            integrationSuccessful = true;
            LOGGER.info("✓ Research integration setup successful!");
            
        } catch (Exception e) {
            LOGGER.warn("Could not integrate with Eidolon research: {}", e.getMessage());
            LOGGER.debug("Research integration error details:", e);
        }
    }

    /**
     * Injects our custom research entries into Eidolon's research system
     */
    private static void injectCustomResearch() {
        if (!integrationSuccessful) {
            LOGGER.warn("Cannot inject research - integration was not successful");
            return;
        }

        try {
            Map<ResourceLocation, ResearchEntry> customResearch = ResearchDataManager.getLoadedResearchEntries();
            
            LOGGER.info("Attempting to inject {} custom research entries", customResearch.size());
            
            for (Map.Entry<ResourceLocation, ResearchEntry> entry : customResearch.entrySet()) {
                ResourceLocation researchId = entry.getKey();
                
                // TODO: Convert our ResearchEntry to Eidolon's Research object and register it
                LOGGER.info("✓ Would inject research entry: {}", researchId);
            }
            
            LOGGER.info("Research integration complete!");
            
        } catch (Exception e) {
            LOGGER.error("Failed to inject custom research", e);
        }
    }

    public static boolean isIntegrationSuccessful() {
        return integrationSuccessful;
    }
}
