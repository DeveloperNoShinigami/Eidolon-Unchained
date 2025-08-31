package com.bluelotuscoding.eidolonunchained.mixins;

import com.mojang.logging.LogUtils;
import elucent.eidolon.recipe.GenericRitualRecipe;
import elucent.eidolon.recipe.RitualRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Mixin to fix invariantItems matching logic in GenericRitualRecipe
 */
@Mixin(value = GenericRitualRecipe.class, remap = false)
public class GenericRitualRecipeMixin {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * Fix the isMatch method to properly handle invariantItems
     * The issue: original method removes invariants from focusItems but then 
     * calls super.isMatch() which does strict size comparison
     */
    @Inject(method = "isMatch(Ljava/util/List;Ljava/util/List;Lnet/minecraft/world/item/ItemStack;)Z", 
            at = @At("HEAD"), cancellable = true)
    private void fixInvariantItemsMatching(List<ItemStack> pedestalItems, List<ItemStack> focusItems, 
                                         ItemStack reagent, CallbackInfoReturnable<Boolean> cir) {
        try {
            GenericRitualRecipe self = (GenericRitualRecipe) (Object) this;
            
            // Access fields via reflection
            Field reagentField = RitualRecipe.class.getDeclaredField("reagent");
            Field pedestalItemsField = RitualRecipe.class.getDeclaredField("pedestalItems");
            Field focusItemsField = RitualRecipe.class.getDeclaredField("focusItems");
            Field invariantItemsField = RitualRecipe.class.getDeclaredField("invariantItems");
            
            reagentField.setAccessible(true);
            pedestalItemsField.setAccessible(true);
            focusItemsField.setAccessible(true);
            invariantItemsField.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            Ingredient recipeReagent = (Ingredient) reagentField.get(self);
            @SuppressWarnings("unchecked")
            List<Ingredient> recipePedestalItems = (List<Ingredient>) pedestalItemsField.get(self);
            @SuppressWarnings("unchecked")
            List<Ingredient> recipeFocusItems = (List<Ingredient>) focusItemsField.get(self);
            @SuppressWarnings("unchecked")
            List<Ingredient> recipeInvariantItems = (List<Ingredient>) invariantItemsField.get(self);
            
            // Check reagent first
            if (!recipeReagent.test(reagent)) {
                LOGGER.debug("🔮 RITUAL MATCH: Reagent mismatch. Expected: {}, Found: {} ({})", 
                    recipeReagent.toJson(), reagent, reagent.getDescriptionId());
                cir.setReturnValue(false);
                return;
            }
            
            // Check pedestal items (unchanged)
            if (recipePedestalItems.size() != pedestalItems.size() || 
                !doItemsMatch(pedestalItems, recipePedestalItems)) {
                LOGGER.debug("🔮 RITUAL MATCH: Pedestal items mismatch. Expected: {}, Found: {}", 
                    recipePedestalItems.size(), pedestalItems.size());
                cir.setReturnValue(false);
                return;
            }
            
            // FIXED: Handle focus items with invariants properly
            List<ItemStack> focusItemsCopy = new ArrayList<>(focusItems);
            
            // Remove invariant items from focus items for matching
            if (!recipeInvariantItems.isEmpty()) {
                focusItemsCopy.removeIf(stack -> 
                    recipeInvariantItems.stream().anyMatch(ingredient -> ingredient.test(stack)));
                LOGGER.debug("🔮 RITUAL MATCH: Removed {} invariant items from focus items. Remaining: {}", 
                    focusItems.size() - focusItemsCopy.size(), focusItemsCopy.size());
            }
            
            // Now check if remaining focus items match expected focus items
            boolean focusMatch = recipeFocusItems.size() == focusItemsCopy.size() && 
                               doItemsMatch(focusItemsCopy, recipeFocusItems);
            
            if (!focusMatch) {
                LOGGER.debug("🔮 RITUAL MATCH: Focus items mismatch. Expected: {}, Found: {} (after removing invariants)", 
                    recipeFocusItems.size(), focusItemsCopy.size());
                cir.setReturnValue(false);
                return;
            }
            
            LOGGER.debug("🔮 RITUAL MATCH: ✅ Recipe matches! Pedestals: {}, Focus: {}, Invariants: {}", 
                pedestalItems.size(), focusItemsCopy.size(), focusItems.size() - focusItemsCopy.size());
            cir.setReturnValue(true);
            
        } catch (Exception e) {
            LOGGER.error("🔮 RITUAL MATCH: Exception during matching: {}", e.getMessage(), e);
            cir.setReturnValue(false);
        }
    }
    
    /**
     * Helper method to match items (copied from RitualRecipe)
     */
    private static boolean doItemsMatch(List<ItemStack> inputs, List<Ingredient> recipeItems) {
        if (inputs.size() != recipeItems.size()) {
            return false;
        }
        
        // Simple matching - could be more sophisticated
        for (int i = 0; i < inputs.size(); i++) {
            boolean found = false;
            for (Ingredient ingredient : recipeItems) {
                if (ingredient.test(inputs.get(i))) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }
}
