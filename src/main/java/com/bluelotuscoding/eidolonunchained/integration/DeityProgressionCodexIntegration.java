package com.bluelotuscoding.eidolonunchained.integration;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager;
import com.bluelotuscoding.eidolonunchained.deity.DatapackDeity;
import elucent.eidolon.api.deity.Deity;
import elucent.eidolon.codex.*;
import elucent.eidolon.codex.IndexPage.ReputationLockedEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Automatically creates codex entries that unlock based on deity progression stages.
 * This integrates your custom deity stages with Eidolon's ReputationLockedEntry system.
 */
public class DeityProgressionCodexIntegration {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * Creates codex entries for all major deity progression stages.
     * Called after both DatapackDeityManager and CodexDataManager have loaded.
     */
    public static void createProgressionCodexEntries() {
        try {
            LOGGER.info("Creating codex entries for deity progression stages...");
            
            for (DatapackDeity deity : DatapackDeityManager.getAllDeities().values()) {
                createProgressionEntriesForDeity(deity);
            }
            
            LOGGER.info("Deity progression codex integration complete");
        } catch (Exception e) {
            LOGGER.error("Failed to create deity progression codex entries", e);
        }
    }
    
    private static void createProgressionEntriesForDeity(DatapackDeity deity) {
        Deity.Progression progression = deity.getProgression();
        List<ReputationLockedEntry> progressionEntries = new ArrayList<>();
        
        // Get all progression stages
        Map<Integer, Deity.Stage> stages = progression.getSteps();
        
        for (Map.Entry<Integer, Deity.Stage> entry : stages.entrySet()) {
            int reputation = entry.getKey();
            Deity.Stage stage = entry.getValue();
            
            // Only create entries for major stages (following Eidolon's pattern)
            if (stage.major() && reputation > 0) { // Skip the initial stage
                Chapter progressionChapter = createProgressionChapter(deity, stage, reputation);
                ReputationLockedEntry reputationEntry = new ReputationLockedEntry(
                    progressionChapter,
                    getIconForStage(deity, stage),
                    reputation,
                    deity
                );
                
                progressionEntries.add(reputationEntry);
                LOGGER.debug("Created progression entry for deity {} stage {} (rep {})", 
                    deity.getId(), stage.id(), reputation);
            }
        }
        
        // Add progression entries to deity's codex category
        if (!progressionEntries.isEmpty()) {
            addProgressionEntriesToCodex(deity, progressionEntries);
        }
    }
    
    private static Chapter createProgressionChapter(DatapackDeity deity, Deity.Stage stage, int reputation) {
        String stageId = stage.id().getPath();
        String chapterKey = "eidolonunchained.codex.chapter." + stageId;
        
        // Create a title page for this progression stage (TitlePage only takes one String)
        TitlePage titlePage = new TitlePage("eidolonunchained.codex.page." + stageId + ".title");
        
        // Create a text page with stage description
        TextPage descriptionPage = new TextPage(
            "eidolonunchained.codex.page." + stageId + ".description"
        );
        
        // Create a text page with stage rewards/abilities
        TextPage rewardsPage = new TextPage(
            "eidolonunchained.codex.page." + stageId + ".rewards"
        );
        
        return new Chapter(chapterKey, titlePage, descriptionPage, rewardsPage);
    }
    
    private static ItemStack getIconForStage(DatapackDeity deity, Deity.Stage stage) {
        // Get stage ID to determine appropriate icon
        String stageId = stage.id().getPath();
        
        // Custom icons based on stage name patterns
        if (stageId.contains("initiate") || stageId.contains("novice")) {
            return new ItemStack(Items.PAPER);
        } else if (stageId.contains("scholar") || stageId.contains("adept")) {
            return new ItemStack(Items.BOOK);
        } else if (stageId.contains("master") || stageId.contains("lord")) {
            return new ItemStack(Items.ENCHANTED_BOOK);
        } else if (stageId.contains("shadow") || stageId.contains("dark")) {
            return new ItemStack(Items.BLACK_DYE);
        } else if (stageId.contains("light") || stageId.contains("holy")) {
            return new ItemStack(Items.WHITE_DYE);
        } else if (stageId.contains("nature") || stageId.contains("verdant")) {
            return new ItemStack(Items.GREEN_DYE);
        }
        
        // Default icon
        return new ItemStack(Items.WRITABLE_BOOK);
    }
    
    private static void addProgressionEntriesToCodex(DatapackDeity deity, List<ReputationLockedEntry> entries) {
        try {
            // Use reflection to access Eidolon's category system
            EidolonCategoryExtension categoryExtension = new EidolonCategoryExtension();
            
            // Try to find existing theurgy category (where deity content belongs)
            Category theurgyCategory = categoryExtension.findCategory("theurgy");
            
            if (theurgyCategory != null) {
                // Get the existing theurgy index and add our entries
                categoryExtension.addEntriesToCategory(theurgyCategory, entries);
                
                LOGGER.info("Added {} progression entries to theurgy category for deity {}", 
                    entries.size(), deity.getId());
            } else {
                // Create a new category specifically for datapack deities
                String categoryName = "datapack_deities";
                Category deityCategory = categoryExtension.createCategory(
                    categoryName,
                    "Datapack Deities",
                    getDeityIcon(deity),
                    getDeityColor(deity),
                    entries
                );
                
                if (deityCategory != null) {
                    LOGGER.info("Created new deity category with {} progression entries for deity {}", 
                        entries.size(), deity.getId());
                } else {
                    LOGGER.warn("Could not create codex category for deity {}", deity.getId());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to add progression entries to codex for deity {}", deity.getId(), e);
        }
    }
    
    private static ItemStack getDeityIcon(DatapackDeity deity) {
        String deityId = deity.getId().getPath();
        
        if (deityId.contains("dark") || deityId.contains("shadow")) {
            return new ItemStack(Items.OBSIDIAN);
        } else if (deityId.contains("light") || deityId.contains("holy")) {
            return new ItemStack(Items.GLOWSTONE);
        } else if (deityId.contains("nature") || deityId.contains("verdant")) {
            return new ItemStack(Items.OAK_SAPLING);
        }
        
        return new ItemStack(Items.BEACON);
    }
    
    private static int getDeityColor(DatapackDeity deity) {
        // Use deity's configured color values (convert float back to int)
        int red = (int)(deity.getRed() * 255);
        int green = (int)(deity.getGreen() * 255);
        int blue = (int)(deity.getBlue() * 255);
        return (red << 16) | (green << 8) | blue;
    }
}
