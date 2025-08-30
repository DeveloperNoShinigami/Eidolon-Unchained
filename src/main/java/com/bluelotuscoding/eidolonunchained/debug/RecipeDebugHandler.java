package com.bluelotuscoding.eidolonunchained.debug;

import com.mojang.logging.LogUtils;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

/**
 * Debug handler to track recipe loading
 */
@Mod.EventBusSubscriber(modid = "eidolonunchained")
public class RecipeDebugHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onReloadListeners(AddReloadListenerEvent event) {
        LOGGER.info("📚 RECIPE DEBUG: Recipe reload listeners are being added");
        
        // Add a reload listener that runs after recipes are loaded
        event.addListener((pPreparations, pResourceManager, pPreparationsProfiler, pReloadProfiler, pBackgroundExecutor, pGameExecutor) -> {
            return CompletableFuture.runAsync(() -> {
                // This runs after all recipe loading is complete
                try {
                    RecipeManager recipeManager = event.getServerResources().getRecipeManager();
                    if (recipeManager != null) {
                        LOGGER.info("📚 RECIPE DEBUG: Recipe manager available after reload");
                        
                        // Count recipes by type
                        int totalRecipes = recipeManager.getRecipes().size();
                        int commandRituals = 0;
                        int eidolonUnchainedRecipes = 0;
                        
                        for (Recipe<?> recipe : recipeManager.getRecipes()) {
                            if (recipe.getId().getNamespace().equals("eidolonunchained")) {
                                eidolonUnchainedRecipes++;
                                LOGGER.info("📚 FOUND EU RECIPE: {} of type {}", recipe.getId(), recipe.getType());
                                
                                // Check if it's a command ritual
                                if (recipe.getClass().getSimpleName().contains("CommandRitual")) {
                                    commandRituals++;
                                    LOGGER.info("📚 COMMAND RITUAL FOUND: {}", recipe.getId());
                                }
                            }
                        }
                        
                        LOGGER.info("📚 RECIPE SUMMARY: Total={}, EidolonUnchained={}, CommandRituals={}", 
                            totalRecipes, eidolonUnchainedRecipes, commandRituals);
                    } else {
                        LOGGER.error("📚 RECIPE DEBUG: Recipe manager is null after reload!");
                    }
                } catch (Exception e) {
                    LOGGER.error("📚 RECIPE DEBUG: Error during recipe analysis: {}", e.getMessage(), e);
                }
            }, pGameExecutor);
        });
    }
}
