package com.bluelotuscoding.eidolonunchained.registries;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.recipe.CrucibleCommandRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registry for custom recipe types and serializers
 */
public class EidolonUnchainedRecipes {
    
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = 
        DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, EidolonUnchained.MODID);
        
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = 
        DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, EidolonUnchained.MODID);
    
    // Custom crucible recipe that executes commands
    public static final RegistryObject<RecipeSerializer<CrucibleCommandRecipe>> CRUCIBLE_COMMAND_RECIPE = 
        RECIPE_SERIALIZERS.register("crucible_command", CrucibleCommandRecipe.Serializer::new);
        
    public static final RegistryObject<RecipeType<CrucibleCommandRecipe>> CRUCIBLE_COMMAND_TYPE = 
        RECIPE_TYPES.register("crucible_command", () -> new RecipeType<>() {
            @Override
            public String toString() {
                return "eidolonunchained:crucible_command";
            }
        });
}
