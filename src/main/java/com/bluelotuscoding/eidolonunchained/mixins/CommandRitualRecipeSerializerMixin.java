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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Mixin to add invariantItems support to CommandRitualRecipe
 */
@Mixin(value = CommandRitualRecipe.Serializer.class, remap = false)
public class CommandRitualRecipeSerializerMixin {
    
    @Inject(method = "fromJson", at = @At("RETURN"), cancellable = true)
    private void addInvariantItemsSupport(ResourceLocation pRecipeId, JsonObject json, CallbackInfoReturnable<CommandRitualRecipe> cir) {
        CommandRitualRecipe originalRecipe = cir.getReturnValue();
        
        // Check if the JSON has invariantItems
        if (json.has("invariantItems")) {
            JsonArray invariantItems = GsonHelper.getAsJsonArray(json, "invariantItems");
            List<Ingredient> invariants = StreamSupport.stream(invariantItems.spliterator(), false)
                .map(Ingredient::fromJson)
                .collect(Collectors.toList());
            
            if (!invariants.isEmpty()) {
                // Get commands from JSON directly since there's no getter
                List<String> commands = new ArrayList<>();
                if (json.has("commands")) {
                    JsonArray commandsJson = GsonHelper.getAsJsonArray(json, "commands");
                    for (JsonElement element : commandsJson) {
                        commands.add(element.getAsString());
                    }
                } else if (json.has("command")) {
                    commands.add(GsonHelper.getAsString(json, "command"));
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
                
                cir.setReturnValue(enhancedRecipe);
            }
        }
    }
}
