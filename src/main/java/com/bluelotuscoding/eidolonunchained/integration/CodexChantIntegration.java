package com.bluelotuscoding.eidolonunchained.integration;

import com.bluelotuscoding.eidolonunchained.chant.DatapackChant;
import com.bluelotuscoding.eidolonunchained.chant.DatapackChantManager;
import com.bluelotuscoding.eidolonunchained.chant.DatapackChantSpell;
import com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig;
import elucent.eidolon.api.spells.Sign;
import elucent.eidolon.codex.*;
import elucent.eidolon.util.ColorUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Integrates datapack chants with Eidolon's codex system
 * Creates proper categories, chapters, and pages that match Eidolon's format
 */
public class CodexChantIntegration {
    
    private static final Map<String, Category> customCategories = new HashMap<>();
    private static final Map<String, List<Chapter>> categoryChapters = new HashMap<>();
    
    /**
     * Called after datapacks load to register chants in the codex
     */
    public static void registerChants() {
        try {
            Map<String, DatapackChant> chants = DatapackChantManager.getAllChants();
            
            if (chants.isEmpty()) {
                com.mojang.logging.LogUtils.getLogger().info("No chants loaded, skipping codex integration");
                return;
            }
            
            com.mojang.logging.LogUtils.getLogger().info("Registering {} chants with codex", chants.size());
            
            // Group chants by category based on configuration
            Map<String, List<DatapackChant>> chantsByCategory = new HashMap<>();
            
            for (DatapackChant chant : chants.values()) {
                try {
                    if (chant.shouldShowInCodex()) {
                        String categoryToUse;
                        
                        if (EidolonUnchainedConfig.COMMON.useIndividualCategories.get()) {
                            // Use individual category from chant JSON
                            categoryToUse = chant.getCategory();
                        } else {
                            // Use default category from config
                            categoryToUse = EidolonUnchainedConfig.COMMON.chantCodexCategory.get();
                        }
                        
                        chantsByCategory.computeIfAbsent(categoryToUse, k -> new ArrayList<>()).add(chant);
                    }
                } catch (Exception e) {
                    com.mojang.logging.LogUtils.getLogger().error("Failed to process chant {}: {}", chant.getId(), e.getMessage());
                }
            }
            
            // Create codex entries for each category
            for (Map.Entry<String, List<DatapackChant>> entry : chantsByCategory.entrySet()) {
                String categoryName = entry.getKey();
                List<DatapackChant> categoryChants = entry.getValue();
                
                try {
                    createCodexCategory(categoryName, categoryChants);
                    com.mojang.logging.LogUtils.getLogger().debug("Created codex category '{}' with {} chants", categoryName, categoryChants.size());
                } catch (Exception e) {
                    com.mojang.logging.LogUtils.getLogger().error("Failed to create codex category '{}': {}", categoryName, e.getMessage());
                }
            }
            
            com.mojang.logging.LogUtils.getLogger().info("Chant codex integration completed successfully");
            
        } catch (Exception e) {
            com.mojang.logging.LogUtils.getLogger().error("Failed during chant codex integration: {}", e.getMessage());
        }
    }
    
    private static void createCodexCategory(String categoryName, List<DatapackChant> chants) {
        List<Chapter> chapters = new ArrayList<>();
        List<IndexPage.IndexEntry> indexEntries = new ArrayList<>();
        
        // Create a chapter for each chant
        for (DatapackChant chant : chants) {
            Chapter chapter = createChantChapter(chant);
            chapters.add(chapter);
            
            // Create index entry with appropriate icon and requirements
            ItemStack icon = getChantIcon(chant);
            IndexPage.IndexEntry entry;
            
            if (chant.getRequirements().isEmpty()) {
                entry = new IndexPage.IndexEntry(chapter, icon);
            } else {
                // TODO: Add requirement-locked entries based on chant requirements
                entry = new IndexPage.IndexEntry(chapter, icon);
            }
            
            indexEntries.add(entry);
        }
        
        // Create category index
        Index categoryIndex = new Index(
            "eidolonunchained.codex.chapter." + categoryName,
            new IndexPage(
                indexEntries.toArray(new IndexPage.IndexEntry[0])
            )
        );
        
        // Create category with appropriate color scheme
        int categoryColor = getCategoryColor(categoryName);
        ItemStack categoryIcon = getCategoryIcon(categoryName);
        
        Category category = new Category(
            categoryName,
            categoryIcon,
            categoryColor,
            categoryIndex
        );
        
        // Add to Eidolon's category list
        CodexChapters.categories.add(category);
        customCategories.put(categoryName, category);
        categoryChapters.put(categoryName, chapters);
    }
    
    private static Chapter createChantChapter(DatapackChant chant) {
        // Use the sanitized ID path instead of display name for ResourceLocations
        String chantId = chant.getId().getPath(); // This comes from the file name, already valid
        ResourceLocation chantLocation = new ResourceLocation("eidolonunchained", chantId);
        
        List<Page> pages = new ArrayList<>();
        
        // Add title page - use sanitized chant ID for translation keys
        pages.add(new TitlePage("eidolonunchained.codex.page." + chantId));
        
        // Try to get spell for chant page, but don't fail if it doesn't exist
        try {
            DatapackChantSpell spell = DatapackChantManager.getSpellForChant(chantLocation);
            if (spell != null) {
                pages.add(new CustomChantPage("eidolonunchained.codex.page." + chantId + ".chant", chant, spell));
            } else {
                // Add a text page if spell creation fails
                pages.add(new TextPage("eidolonunchained.codex.page." + chantId + ".chant"));
            }
        } catch (Exception e) {
            com.mojang.logging.LogUtils.getLogger().warn("Failed to create spell page for chant {}: {}", chantId, e.getMessage());
            // Add a text page as fallback
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
        // Return appropriate icon based on chant category/effects
        return switch (chant.getCategory().toLowerCase()) {
            case "basic" -> new ItemStack(Items.PAPER);
            case "dark" -> new ItemStack(Items.WITHER_SKELETON_SKULL);
            case "light" -> new ItemStack(Items.GLOWSTONE);
            case "nature" -> new ItemStack(Items.OAK_SAPLING);
            case "healing" -> new ItemStack(Items.GOLDEN_APPLE);
            case "combat" -> new ItemStack(Items.DIAMOND_SWORD);
            default -> new ItemStack(Items.ENCHANTED_BOOK);
        };
    }
    
    private static ItemStack getCategoryIcon(String categoryName) {
        return switch (categoryName.toLowerCase()) {
            case "basic" -> new ItemStack(Items.BOOK);
            case "dark" -> new ItemStack(Items.OBSIDIAN);
            case "light" -> new ItemStack(Items.BEACON);
            case "nature" -> new ItemStack(Items.GRASS_BLOCK);
            case "healing" -> new ItemStack(Items.POTION);
            case "combat" -> new ItemStack(Items.NETHERITE_SWORD);
            case "examples" -> new ItemStack(Items.KNOWLEDGE_BOOK);
            default -> new ItemStack(Items.ENCHANTED_BOOK);
        };
    }
    
    private static int getCategoryColor(String categoryName) {
        return switch (categoryName.toLowerCase()) {
            case "basic" -> ColorUtil.packColor(255, 100, 100, 100);
            case "dark" -> ColorUtil.packColor(255, 80, 40, 120);
            case "light" -> ColorUtil.packColor(255, 255, 230, 117);
            case "nature" -> ColorUtil.packColor(255, 34, 139, 34);
            case "healing" -> ColorUtil.packColor(255, 255, 100, 100);
            case "combat" -> ColorUtil.packColor(255, 200, 50, 50);
            case "examples" -> ColorUtil.packColor(255, 70, 130, 180);
            default -> ColorUtil.packColor(255, 150, 150, 150);
        };
    }
    
    /**
     * Custom chant page that displays datapack chants properly
     */
    public static class CustomChantPage extends ChantPage {
        private final DatapackChant chant;
        private final DatapackChantSpell spell;
        
        public CustomChantPage(String textKey, DatapackChant chant, DatapackChantSpell spell) {
            super(textKey, spell);
            this.chant = chant;
            this.spell = spell;
        }
        
        @Override
        public void render(CodexGui gui, net.minecraft.client.gui.GuiGraphics graphics, ResourceLocation bg, int x, int y, int mouseX, int mouseY) {
            super.render(gui, graphics, bg, x, y, mouseX, mouseY);
            
            // Add custom rendering for datapack chant information
            // This would show the effects, requirements, difficulty, etc.
            int yOffset = 120;
            
            // Show difficulty
            graphics.drawString(gui.getMinecraft().font, "Difficulty: " + "★".repeat(chant.getDifficulty()), 
                x + 20, y + yOffset, 0x000000);
            yOffset += 15;
            
            // Show requirements
            if (!chant.getRequirements().isEmpty()) {
                graphics.drawString(gui.getMinecraft().font, "Requirements:", x + 20, y + yOffset, 0x000000);
                yOffset += 12;
                for (String req : chant.getRequirements()) {
                    graphics.drawString(gui.getMinecraft().font, "• " + req, x + 30, y + yOffset, 0x666666);
                    yOffset += 10;
                }
            }
        }
    }
}
