package com.bluelotuscoding.eidolonunchained.recipe;

import com.bluelotuscoding.eidolonunchained.registries.EidolonUnchainedRecipes;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elucent.eidolon.recipe.CrucibleRecipe;
import elucent.eidolon.registries.EidolonRecipes;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom crucible recipe that executes commands upon completion
 * Extends normal crucible functionality to run server commands
 */
public class CrucibleCommandRecipe extends CrucibleRecipe {
    private static final Logger LOGGER = LoggerFactory.getLogger(CrucibleCommandRecipe.class);
    
    private final List<String> commands;
    
    public CrucibleCommandRecipe(ResourceLocation id, List<Step> steps, ItemStack result, List<String> commands) {
        super(steps, result);
        this.setRegistryName(id);
        this.commands = commands != null ? commands : new ArrayList<>();
    }
    
    public List<String> getCommands() {
        return commands;
    }
    
    /**
     * Execute the commands when the recipe is completed
     * This should be called by the crucible when the recipe finishes
     */
    public void executeCommands(Level level, double x, double y, double z, @Nullable ServerPlayer player) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        
        LOGGER.info("Executing {} commands for crucible recipe: {}", commands.size(), getId());
        
        for (String command : commands) {
            try {
                // Replace player placeholder if a player is nearby
                String processedCommand = command;
                if (player != null) {
                    processedCommand = command.replace("@p", player.getGameProfile().getName());
                }
                
                // Execute the command at the crucible location
                var commandSourceStack = serverLevel.getServer().createCommandSourceStack()
                    .withPosition(new net.minecraft.world.phys.Vec3(x, y, z))
                    .withLevel(serverLevel);
                
                LOGGER.debug("Executing command: {}", processedCommand);
                serverLevel.getServer().getCommands().performPrefixedCommand(commandSourceStack, processedCommand);
                
            } catch (Exception e) {
                LOGGER.error("Failed to execute crucible command '{}': {}", command, e.getMessage());
            }
        }
    }
    
    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return EidolonUnchainedRecipes.CRUCIBLE_COMMAND_RECIPE.get();
    }
    
    @Override
    public @NotNull RecipeType<?> getType() {
        return EidolonUnchainedRecipes.CRUCIBLE_COMMAND_TYPE.get();
    }
    
    /**
     * Custom serializer for crucible command recipes
     */
    public static class Serializer implements RecipeSerializer<CrucibleCommandRecipe> {
        
        @Override
        public @NotNull CrucibleCommandRecipe fromJson(@NotNull ResourceLocation recipeId, @NotNull JsonObject json) {
            // Parse normal crucible recipe parts
            List<Step> steps = parseSteps(json);
            ItemStack result = parseResult(json);
            
            // Parse commands array
            List<String> commands = new ArrayList<>();
            if (json.has("commands")) {
                JsonArray commandsArray = json.getAsJsonArray("commands");
                for (JsonElement element : commandsArray) {
                    commands.add(element.getAsString());
                }
            }
            
            return new CrucibleCommandRecipe(recipeId, steps, result, commands);
        }
        
        @Override
        public @Nullable CrucibleCommandRecipe fromNetwork(@NotNull ResourceLocation recipeId, @NotNull FriendlyByteBuf buffer) {
            // Read steps
            int stepCount = buffer.readInt();
            List<Step> steps = new ArrayList<>();
            for (int i = 0; i < stepCount; i++) {
                int stirs = buffer.readInt();
                int ingredientCount = buffer.readInt();
                List<Ingredient> ingredients = new ArrayList<>();
                for (int j = 0; j < ingredientCount; j++) {
                    ingredients.add(Ingredient.fromNetwork(buffer));
                }
                steps.add(new Step(stirs, ingredients));
            }
            
            // Read result
            ItemStack result = buffer.readItem();
            
            // Read commands
            int commandCount = buffer.readInt();
            List<String> commands = new ArrayList<>();
            for (int i = 0; i < commandCount; i++) {
                commands.add(buffer.readUtf());
            }
            
            return new CrucibleCommandRecipe(recipeId, steps, result, commands);
        }
        
        @Override
        public void toNetwork(@NotNull FriendlyByteBuf buffer, @NotNull CrucibleCommandRecipe recipe) {
            // Write steps
            buffer.writeInt(recipe.getSteps().size());
            for (Step step : recipe.getSteps()) {
                buffer.writeInt(step.stirs);
                buffer.writeInt(step.matches.size());
                for (Ingredient ingredient : step.matches) {
                    ingredient.toNetwork(buffer);
                }
            }
            
            // Write result
            buffer.writeItem(recipe.getResult());
            
            // Write commands
            buffer.writeInt(recipe.commands.size());
            for (String command : recipe.commands) {
                buffer.writeUtf(command);
            }
        }
        
        // Helper methods to parse crucible recipe format
        private List<Step> parseSteps(JsonObject json) {
            List<Step> steps = new ArrayList<>();
            JsonArray stepsArray = json.getAsJsonArray("steps");
            
            for (JsonElement stepElement : stepsArray) {
                JsonObject stepObject = stepElement.getAsJsonObject();
                
                int stirs = stepObject.has("stirs") ? stepObject.get("stirs").getAsInt() : 0;
                List<Ingredient> ingredients = new ArrayList<>();
                
                if (stepObject.has("items")) {
                    JsonArray itemsArray = stepObject.getAsJsonArray("items");
                    for (JsonElement itemElement : itemsArray) {
                        ingredients.add(Ingredient.fromJson(itemElement));
                    }
                }
                
                steps.add(new Step(stirs, ingredients));
            }
            
            return steps;
        }
        
        private ItemStack parseResult(JsonObject json) {
            JsonObject resultObject = json.getAsJsonObject("result");
            String itemName = resultObject.get("item").getAsString();
            int count = resultObject.has("count") ? resultObject.get("count").getAsInt() : 1;
            
            return new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                new ResourceLocation(itemName)), count);
        }
    }
}
