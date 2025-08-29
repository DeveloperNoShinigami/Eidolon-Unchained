package com.bluelotuscoding.eidolonunchained.integration;

import com.bluelotuscoding.eidolonunchained.data.ResearchDataManager;
import com.bluelotuscoding.eidolonunchained.research.ResearchChapter;
import com.bluelotuscoding.eidolonunchained.research.ResearchEntry;
import com.bluelotuscoding.eidolonunchained.research.conditions.ResearchCondition;
import com.mojang.logging.LogUtils;
import elucent.eidolon.api.research.Research;
import elucent.eidolon.registries.Researches;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.Minecraft;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.slf4j.Logger;

// No reflection imports needed

import java.util.List;
import java.util.Map;

/**
 * Handles integration with Eidolon's research system to inject custom research entries.
 * Fully datapack-driven, no reflection or legacy API usage.
 * 
 * CRITICAL: Integration is now called from ResearchDataManager after resource loading
 * completes to ensure proper timing - research entries must be loaded before injection.
 */
public class EidolonResearchIntegration {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Injects our custom research entries into Eidolon's research system.
     * Now called from ResearchDataManager after resource loading completes.
     */
    public static void injectCustomResearch() {
        try {
            Map<ResourceLocation, ResearchChapter> customChapters = ResearchDataManager.getLoadedResearchChapters();
            LOGGER.info("Attempting to register {} custom research chapters", customChapters.size());
            for (Map.Entry<ResourceLocation, ResearchChapter> chapterEntry : customChapters.entrySet()) {
                ResearchChapter chapter = chapterEntry.getValue();
                if (chapter == null) {
                    LOGGER.warn("✗ Skipping null chapter {}", chapterEntry.getKey());
                    continue;
                }
                registerChapterWithEidolon(chapter);
            }

            Map<ResourceLocation, ResearchEntry> customResearch = ResearchDataManager.getLoadedResearchEntries();
            LOGGER.info("Attempting to inject {} custom research entries", customResearch.size());

            for (Map.Entry<ResourceLocation, ResearchEntry> entry : customResearch.entrySet()) {
                ResourceLocation researchId = entry.getKey();
                ResearchEntry data = entry.getValue();
                if (data == null) {
                    LOGGER.warn("✗ Skipping null research entry {}", researchId);
                    continue;
                }
                ResearchChapter chapter = customChapters.get(data.getChapter());
                if (chapter == null) {
                    LOGGER.warn("✗ Missing chapter {} for research {} - skipping", data.getChapter(), researchId);
                    continue;
                }
                boolean conditionsMet = true;
                if (!data.getConditions().isEmpty()) {
                    conditionsMet = data.getConditions().stream().allMatch(c -> c.test(Minecraft.getInstance().player));
                }
                if (conditionsMet) {
                    Research research = createResearchFromEntry(data);
                    Researches.register(research);
                    LOGGER.info("✓ Injected research entry: {}", researchId);
                } else {
                    LOGGER.info("✗ Skipping research entry {} due to unmet conditions", researchId);
                }
            }

            LOGGER.info("Research integration complete!");

        } catch (Exception e) {
            LOGGER.error("Failed to inject custom research", e);
        }
    }

    /**
     * No-op: Research chapters are handled purely via datapack and category extension system.
     */
    private static void registerChapterWithEidolon(ResearchChapter chapter) {
        LOGGER.info("✓ Research chapter prepared for integration: {}", chapter.getId());
    }

    /**
     * Creates a Research object from our ResearchEntry data.
     */
    private static Research createResearchFromEntry(ResearchEntry entry) {
        int stars = entry.getRequiredStars();
        if (stars < 0) {
            switch (entry.getType()) {
                case ADVANCED -> stars = 1;
                case FORBIDDEN -> stars = 2;
                case RITUAL -> stars = 1;
                case CRAFTING, BASIC -> stars = 0;
                default -> stars = 0;
            }
        }
        return new CustomResearch(entry, stars);
    }

    /**
     * Lightweight extension of Eidolon's Research class that keeps additional metadata
     * for potential future use.
     */
    private static class CustomResearch extends Research {
        private final ResourceLocation chapter;
        private final Component title;
        private final ItemStack icon;
        private final ResearchEntry.ResearchType type;
        private final List<ResourceLocation> prerequisites;

        protected CustomResearch(ResearchEntry entry, int stars) {
            super(entry.getId(), stars);
            this.chapter = entry.getChapter();
            this.title = entry.getTitle();
            this.icon = entry.getIcon();
            this.type = entry.getType();
            this.prerequisites = entry.getPrerequisites();
        }
    }
}
