package com.bluelotuscoding.eidolonunchained.mixins;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeManager;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * Debug mixin to track recipe loading and failures
 * TEMPORARILY DISABLED - Compilation issues with mixin obfuscation
 */
//@Mixin(RecipeManager.class)
public class RecipeManagerDebugMixin {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    //@Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At("HEAD"))
    private void logRecipeLoadingStart(Map<ResourceLocation, JsonElement> recipes, ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfo ci) {
        LOGGER.info("📚 RECIPE MANAGER: Starting to load {} recipes", recipes.size());
        
        // Log all recipe files being loaded
        for (ResourceLocation recipeId : recipes.keySet()) {
            if (recipeId.getNamespace().equals("eidolonunchained")) {
                LOGGER.info("📚 FOUND RECIPE: {}", recipeId);
                
                // Log the JSON content for our recipes
                JsonElement json = recipes.get(recipeId);
                if (json != null && json.isJsonObject()) {
                    String type = json.getAsJsonObject().has("type") ? 
                        json.getAsJsonObject().get("type").getAsString() : "unknown";
                    LOGGER.info("📚 RECIPE TYPE: {} -> {}", recipeId, type);
                    
                    if (type.equals("eidolon:ritual_brazier_command")) {
                        LOGGER.info("📚 COMMAND RITUAL: {} content: {}", recipeId, json.toString());
                    }
                }
            }
        }
    }
    
    //@Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At("RETURN"))
    private void logRecipeLoadingEnd(Map<ResourceLocation, JsonElement> recipes, ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfo ci) {
        LOGGER.info("📚 RECIPE MANAGER: Finished loading recipes");
    }
}
