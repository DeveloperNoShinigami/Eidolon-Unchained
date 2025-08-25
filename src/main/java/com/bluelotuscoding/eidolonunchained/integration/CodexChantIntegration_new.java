package com.bluelotuscoding.eidolonunchained.integration;

import com.bluelotuscoding.eidolonunchained.chant.DatapackChant;
import com.bluelotuscoding.eidolonunchained.chant.DatapackChantManager;
import com.bluelotuscoding.eidolonunchained.chant.DatapackChantSpell;
import elucent.eidolon.codex.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Integrates datapack chants with Eidolon's codex system
 * Adds chant chapters to existing Eidolon categories (like research system does)
 */
public class CodexChantIntegration {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CodexChantIntegration.class);
    private static final Map<String, Chapter> chantChapters = new HashMap<>();
    
    /**
     * Called after datapacks load to register chants in the codex
     * Adds chant chapters to existing Eidolon categories (like research system does)
     */
    public static void registerChants() {
        try {
            Map<String, DatapackChant> chants = DatapackChantManager.getAllChants();
            
            if (chants.isEmpty()) {
                LOGGER.info("No datapack chants found for codex integration");
                return;
            }
            
            LOGGER.info("Registering {} datapack chants with codex", chants.size());
            
            // Create individual chapter for each chant and attach to existing categories
            for (DatapackChant chant : chants.values()) {
                if (chant.shouldShowInCodex()) {
                    addChantToExistingCategory(chant);
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to register datapack chants with codex", e);
        }
    }
    
    /**
     * Create a chapter for the chant and attach it to the specified category
     * Category can be ANY category (existing Eidolon category or custom category)
     */
    private static void addChantToExistingCategory(DatapackChant chant) {
        try {
            // Create individual chapter for this chant (like DARK_PRAYER, LIGHT_PRAYER in Eidolon)
            Chapter chantChapter = createChantChapter(chant);
            
            // Store chapter for reference
            String chantId = chant.getId().getPath();
            chantChapters.put(chantId, chantChapter);
            
            // Get the target category from the chant's codex_category field (completely flexible)
            String targetCategory = chant.getCodexCategory();
            
            // Get icon from chant configuration (datapack-driven, no hardcoding)
            ItemStack icon = getChantIcon(chant);
            
            // Use the same method that research uses to attach to ANY category
            EidolonCategoryExtension.attachChapterToCategory(targetCategory, chantChapter, icon);
            
            LOGGER.info("✅ Added chant '{}' to '{}' category", chant.getName(), targetCategory);
            
        } catch (Exception e) {
            LOGGER.error("❌ Failed to add chant '{}' to category: {}", chant.getName(), e.getMessage(), e);
        }
    }
    
    private static Chapter createChantChapter(DatapackChant chant) {
        String chantId = chant.getId().getPath();
        List<Page> pages = new ArrayList<>();
        
        // Add title page
        pages.add(new TitlePage("eidolonunchained.codex.page." + chantId));
        
        // The key: create a proper ChantPage that shows signs like Eidolon does
        try {
            DatapackChantSpell spell = DatapackChantManager.getSpellForChant(chant.getId());
            if (spell != null) {
                // ChantPage automatically displays the sign sequence with visual icons!
                pages.add(new ChantPage("eidolonunchained.codex.page." + chantId + ".chant", spell));
            } else {
                LOGGER.warn("No spell found for chant {}, creating text page instead", chantId);
                pages.add(new TextPage("eidolonunchained.codex.page." + chantId + ".chant"));
            }
        } catch (Exception e) {
            LOGGER.warn("Could not create ChantPage for {}: {}, using text page", chantId, e.getMessage());
            pages.add(new TextPage("eidolonunchained.codex.page." + chantId + ".chant"));
        }
        
        // Add description page if there's extra lore
        if (!chant.getDescription().isEmpty()) {
            pages.add(new TextPage("eidolonunchained.codex.page." + chantId + ".description"));
        }
        
        return new Chapter(
            "eidolonunchained.codex.chapter." + chantId,
            pages.toArray(new Page[0])
        );
    }
    
    private static ItemStack getChantIcon(DatapackChant chant) {
        // Use icon specified in chant JSON (completely datapack-driven)
        if (chant.getCodexIcon() != null) {
            try {
                // Get item from registry using the ResourceLocation from JSON
                var item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(chant.getCodexIcon());
                if (item != null) {
                    return new ItemStack(item);
                }
            } catch (Exception e) {
                LOGGER.warn("Could not create icon from '{}' for chant '{}': {}", 
                    chant.getCodexIcon(), chant.getName(), e.getMessage());
            }
        }
        
        // Fallback to default book if no icon specified or icon invalid
        return new ItemStack(Items.ENCHANTED_BOOK);
    }
}
