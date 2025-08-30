package com.bluelotuscoding.eidolonunchained.mixins;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elucent.eidolon.api.ritual.FocusItemPresentRequirement;
import elucent.eidolon.api.ritual.Ritual;
import elucent.eidolon.recipe.CommandRitualRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Mixin to add invariantItems support to CommandRitualRecipe
 */
@Mixin(value = CommandRitualRecipe.Serializer.class, remap = false)
public class CommandRitualRecipeSerializerMixin {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    @Inject(method = "fromJson", at = @At("HEAD"))
    private void logRecipeLoading(ResourceLocation pRecipeId, JsonObject json, CallbackInfoReturnable<CommandRitualRecipe> cir) {
        try {
            LOGGER.info("🔮 RITUAL LOADING: Attempting to load CommandRitualRecipe: {}", pRecipeId);
            LOGGER.info("🔮 RITUAL JSON: {}", json.toString());
            
            // Log specific fields that are critical for parsing
            if (json.has("commands")) {
                LOGGER.info("🔮 RITUAL COMMANDS: Found commands array");
            } else if (json.has("command")) {
                LOGGER.info("🔮 RITUAL COMMANDS: Found single command field");
            } else {
                LOGGER.warn("🔮 RITUAL WARNING: No commands/command field found in {}", pRecipeId);
            }
            
            if (json.has("reagent")) {
                LOGGER.info("🔮 RITUAL REAGENT: Found reagent field");
            } else {
                LOGGER.warn("🔮 RITUAL WARNING: No reagent field found in {}", pRecipeId);
            }
        } catch (Exception e) {
            LOGGER.error("🔮 RITUAL ERROR: Exception in recipe loading logging for {}: {}", pRecipeId, e.getMessage());
        }
    }
    
    @Inject(method = "fromJson", at = @At("RETURN"), cancellable = true)
    private void addInvariantItemsSupportWithLogging(ResourceLocation pRecipeId, JsonObject json, CallbackInfoReturnable<CommandRitualRecipe> cir) {
        try {
            CommandRitualRecipe originalRecipe = cir.getReturnValue();
            
            if (originalRecipe == null) {
                LOGGER.error("🔮 RITUAL FAILED: Original recipe is null for {}", pRecipeId);
                return;
            }
            
            LOGGER.info("🔮 RITUAL SUCCESS: Basic CommandRitualRecipe loaded successfully: {}", pRecipeId);
            
            // Check if the JSON has invariantItems
            if (json.has("invariantItems")) {
                LOGGER.info("🔮 RITUAL ENHANCE: Adding invariantItems support to {}", pRecipeId);
                JsonArray invariantItems = GsonHelper.getAsJsonArray(json, "invariantItems");
                List<Ingredient> invariants = StreamSupport.stream(invariantItems.spliterator(), false)
                    .map(Ingredient::fromJson)
                    .collect(Collectors.toList());
                
                if (!invariants.isEmpty()) {
                    LOGGER.info("🔮 RITUAL ENHANCE: Found {} invariant items for {}", invariants.size(), pRecipeId);
                    // Get commands from JSON directly since there's no getter
                    List<String> commands = new ArrayList<>();
                    if (json.has("commands")) {
                        JsonArray commandsJson = GsonHelper.getAsJsonArray(json, "commands");
                        for (JsonElement element : commandsJson) {
                            commands.add(element.getAsString());
                        }
                        LOGGER.info("🔮 RITUAL ENHANCE: Found {} commands from 'commands' array", commands.size());
                    } else if (json.has("command")) {
                        commands.add(GsonHelper.getAsString(json, "command"));
                        LOGGER.info("🔮 RITUAL ENHANCE: Found 1 command from 'command' field");
                    }
                    
                    // Create an enhanced version that supports invariant items
                    CommandRitualRecipe enhancedRecipe = new CommandRitualRecipe(
                        originalRecipe.getId(),
                        commands,
                        originalRecipe.reagent,
                        originalRecipe.pedestalItems,
                        originalRecipe.focusItems,
                        0.0f
                    ) {
                    @Override
                    public Ritual getRitualWithRequirements() {
                        Ritual ritual = super.getRitualWithRequirements();
                        // Add invariant items as non-consuming focus requirements
                        ritual.addInvariants(invariants.stream()
                            .map(FocusItemPresentRequirement::new)
                            .collect(Collectors.toList()));
                        return ritual;
                    }
                };
                
                    LOGGER.info("🔮 RITUAL ENHANCE: Enhanced recipe created successfully for {}", pRecipeId);
                    cir.setReturnValue(enhancedRecipe);
                } else {
                    LOGGER.info("🔮 RITUAL ENHANCE: No invariant items found for {}", pRecipeId);
                }
            } else {
                LOGGER.info("🔮 RITUAL BASIC: Recipe {} loaded without invariantItems field", pRecipeId);
            }
        } catch (Exception e) {
            LOGGER.error("🔮 RITUAL ERROR: Exception during enhancement of {}: {}", pRecipeId, e.getMessage(), e);
        }
    }
}
