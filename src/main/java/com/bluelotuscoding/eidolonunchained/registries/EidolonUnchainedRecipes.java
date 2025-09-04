package com.bluelotuscoding.eidolonunchained.registries;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.recipe.CrucibleCommandRecipe;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

/**
 * Registry for custom recipe types and serializers
 */
public class EidolonUnchainedRecipes {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = 
        DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, EidolonUnchained.MODID);
        
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = 
        DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, EidolonUnchained.MODID);
    
    // Custom crucible recipe that executes commands
    public static final RegistryObject<RecipeSerializer<CrucibleCommandRecipe>> CRUCIBLE_COMMAND_RECIPE = 
        RECIPE_SERIALIZERS.register("crucible_command", () -> {
            LOGGER.info("🍲 Registering CrucibleCommandRecipe serializer");
            return new CrucibleCommandRecipe.Serializer();
        });
        
    public static final RegistryObject<RecipeType<CrucibleCommandRecipe>> CRUCIBLE_COMMAND_TYPE = 
        RECIPE_TYPES.register("crucible_command", () -> {
            LOGGER.info("🔧 Registering CrucibleCommandRecipe type");
            return new RecipeType<>() {
                @Override
                public String toString() {
                    return "eidolonunchained:crucible_command";
                }
            };
        });
        
    public static void init() {
        LOGGER.info("🎯 EidolonUnchainedRecipes registry initialized");
    }
}
