package com.bluelotuscoding.eidolonunchained.integration;

import com.bluelotuscoding.eidolonunchained.data.ResearchDataManager;
import com.bluelotuscoding.eidolonunchained.research.ResearchEntry;
import com.mojang.logging.LogUtils;
import elucent.eidolon.api.research.Research;
import elucent.eidolon.registries.Researches;
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
    
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(EidolonResearchIntegration::injectCustomResearch);
    }

    /**
     * Injects our custom research entries into Eidolon's research system
     */
    private static void injectCustomResearch() {
        try {
            Map<ResourceLocation, ResearchEntry> customResearch = ResearchDataManager.getLoadedResearchEntries();

            LOGGER.info("Attempting to inject {} custom research entries", customResearch.size());

            for (Map.Entry<ResourceLocation, ResearchEntry> entry : customResearch.entrySet()) {
                ResourceLocation researchId = entry.getKey();
                Research research = new Research(researchId, 0);
                Researches.register(research);
                LOGGER.info("✓ Injected research entry: {}", researchId);
            }

            LOGGER.info("Research integration complete!");

        } catch (Exception e) {
            LOGGER.error("Failed to inject custom research", e);
        }
    }
}
