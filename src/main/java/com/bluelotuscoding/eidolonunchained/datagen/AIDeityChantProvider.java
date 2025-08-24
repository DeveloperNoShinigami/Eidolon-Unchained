package com.bluelotuscoding.eidolonunchained.datagen;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.spells.AIDeitySpells;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import elucent.eidolon.api.spells.Sign;
import elucent.eidolon.api.spells.Spell;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Data generator for AI deity chant recipes.
 * Creates chant recipes that integrate AI deities with Eidolon's chant system.
 */
public class AIDeityChantProvider implements DataProvider {
    private final DataGenerator generator;
    
    public AIDeityChantProvider(DataGenerator generator) {
        this.generator = generator;
    }
    
    @Override
    public @NotNull CompletableFuture<?> run(@NotNull CachedOutput output) {
        return CompletableFuture.runAsync(() -> {
            // Generate chant recipes for all registered AI deity spells
            var aiSpells = AIDeitySpells.getAllAIPrayerSpells();
        
            for (var entry : aiSpells.entrySet()) {
                ResourceLocation deityId = entry.getKey();
                var spell = entry.getValue();
                
                // Create chant recipe JSON
                JsonObject chantJson = createChantRecipe(spell);
                
                // Save to data/eidolonunchained/recipes/
                ResourceLocation recipeId = new ResourceLocation(EidolonUnchained.MODID, 
                    deityId.getPath() + "_ai_prayer_chant");
                Path recipePath = getRecipePath(recipeId);
                
                try {
                    DataProvider.saveStable(output, chantJson, recipePath);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to save chant recipe for " + deityId, e);
                }
            }
        });
    }
    
    private JsonObject createChantRecipe(Spell spell) {
        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", "eidolon:chant");
        recipe.addProperty("spell", spell.getRegistryName().toString());
        
        // For now, we'll use a default sign sequence
        // The actual sequences are defined in the AI deity configuration files
        JsonArray signs = new JsonArray();
        signs.add("eidolon:wicked");
        signs.add("eidolon:sacred");
        recipe.add("signs", signs);
        
        return recipe;
    }
    
    private Path getRecipePath(ResourceLocation recipeId) {
        return this.generator.getPackOutput().getOutputFolder()
            .resolve("data")
            .resolve(recipeId.getNamespace())
            .resolve("recipes")
            .resolve(recipeId.getPath() + ".json");
    }
    
    @Override
    public @NotNull String getName() {
        return "AI Deity Chants";
    }
}
