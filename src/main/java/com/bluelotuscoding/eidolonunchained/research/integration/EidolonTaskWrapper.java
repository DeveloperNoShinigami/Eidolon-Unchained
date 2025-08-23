package com.bluelotuscoding.eidolonunchained.research.integration;

import com.bluelotuscoding.eidolonunchained.research.tasks.EnterDimensionTask;
import com.bluelotuscoding.eidolonunchained.research.tasks.ExploreBiomesTask;
import com.bluelotuscoding.eidolonunchained.research.tasks.HasNbtTask;
import com.bluelotuscoding.eidolonunchained.research.tasks.WeatherTask;
import com.bluelotuscoding.eidolonunchained.research.conditions.WeatherCondition;
import elucent.eidolon.api.research.ResearchTask;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Wraps advanced task types to provide proper display integration with Eidolon's research UI.
 */
public abstract class EidolonTaskWrapper extends ResearchTask {
    
    public static class DimensionTaskWrapper extends EidolonTaskWrapper {
        private final EnterDimensionTask task;
        
        public DimensionTaskWrapper(EnterDimensionTask task) {
            this.task = task;
        }
        
        @Override
        public CompoundTag write() {
            CompoundTag tag = new CompoundTag();
            tag.putString("dimension", task.getDimension().toString());
            return tag;
        }
        
        @Override
        public void read(CompoundTag tag) {
            // Read-only wrapper, original task data is preserved
        }
        
        @Override
        public CompletenessResult isComplete(AbstractContainerMenu menu, Player player, int slotStart) {
            boolean complete = task.isComplete(player);
            return new CompletenessResult(slotStart, complete);
        }
        
        @Override
        public void onComplete(AbstractContainerMenu menu, Player player, int slotStart) {
            // No special completion action needed
        }
        
        @Override
        @OnlyIn(Dist.CLIENT)
        public int getWidth() {
            return 120; // Width for text display
        }
        
        @Override
        @OnlyIn(Dist.CLIENT)
        public void drawIcon(GuiGraphics stack, ResourceLocation texture, int x, int y) {
            // Draw dimension icon - you could add custom icons here
            stack.blit(texture, x, y, 0, 128, 32, 32, 256, 256);
        }
        
        @Override
        @OnlyIn(Dist.CLIENT)
        public void drawTooltip(@NotNull GuiGraphics stack, AbstractContainerScreen<?> gui, double mouseX, double mouseY) {
            List<Component> tooltip = new ArrayList<>();
            
            String dimensionKey = task.getDimension().toString();
            String translationKey = "task.eidolonunchained.enter_dimension." + 
                dimensionKey.replace(":", ".").replace("/", ".");
            
            // Try specific dimension translation first, fall back to generic
            Component description;
            try {
                description = Component.translatable(translationKey);
                if (description.getString().equals(translationKey)) {
                    // Translation not found, use generic format
                    String dimensionName = dimensionKey.substring(dimensionKey.indexOf(':') + 1)
                        .replace("_", " ");
                    description = Component.translatable("task.eidolonunchained.enter_dimension.generic", dimensionName);
                }
            } catch (Exception e) {
                description = Component.literal("Enter " + dimensionKey);
            }
            
            tooltip.add(description);
            stack.renderComponentTooltip(Minecraft.getInstance().font, tooltip, (int) mouseX, (int) mouseY);
        }
    }
    
    public static class BiomeTaskWrapper extends EidolonTaskWrapper {
        private final ExploreBiomesTask task;
        
        public BiomeTaskWrapper(ExploreBiomesTask task) {
            this.task = task;
        }
        
        @Override
        public CompoundTag write() {
            CompoundTag tag = new CompoundTag();
            tag.putString("biome", task.getBiome().toString());
            tag.putInt("count", task.getCount());
            return tag;
        }
        
        @Override
        public void read(CompoundTag tag) {
            // Read-only wrapper
        }
        
        @Override
        public CompletenessResult isComplete(AbstractContainerMenu menu, Player player, int slotStart) {
            boolean complete = task.isComplete(player);
            return new CompletenessResult(slotStart, complete);
        }
        
        @Override
        public void onComplete(AbstractContainerMenu menu, Player player, int slotStart) {
            // No special completion action
        }
        
        @Override
        @OnlyIn(Dist.CLIENT)
        public int getWidth() {
            return 120;
        }
        
        @Override
        @OnlyIn(Dist.CLIENT)
        public void drawIcon(GuiGraphics stack, ResourceLocation texture, int x, int y) {
            // Draw biome exploration icon
            stack.blit(texture, x, y, 32, 128, 32, 32, 256, 256);
        }
        
        @Override
        @OnlyIn(Dist.CLIENT)
        public void drawTooltip(@NotNull GuiGraphics stack, AbstractContainerScreen<?> gui, double mouseX, double mouseY) {
            List<Component> tooltip = new ArrayList<>();
            
            String biomeKey = task.getBiome().toString();
            String translationKey = "task.eidolonunchained.explore_biome." + 
                biomeKey.replace(":", ".").replace("/", ".");
            
            Component description;
            try {
                description = Component.translatable(translationKey);
                if (description.getString().equals(translationKey)) {
                    // Use generic format
                    String biomeName = biomeKey.substring(biomeKey.indexOf(':') + 1)
                        .replace("_", " ");
                    if (task.getCount() > 1) {
                        description = Component.translatable("task.eidolonunchained.explore_biome.multiple", task.getCount(), biomeName);
                    } else {
                        description = Component.translatable("task.eidolonunchained.explore_biome.generic", biomeName);
                    }
                }
            } catch (Exception e) {
                description = Component.literal("Explore " + biomeKey + (task.getCount() > 1 ? " (" + task.getCount() + " times)" : ""));
            }
            
            tooltip.add(description);
            stack.renderComponentTooltip(Minecraft.getInstance().font, tooltip, (int) mouseX, (int) mouseY);
        }
    }
    
    public static class WeatherTaskWrapper extends EidolonTaskWrapper {
        private final WeatherTask task;
        
        public WeatherTaskWrapper(WeatherTask task) {
            this.task = task;
        }
        
        @Override
        public CompoundTag write() {
            CompoundTag tag = new CompoundTag();
            tag.putString("weather", task.getWeather().name());
            return tag;
        }
        
        @Override
        public void read(CompoundTag tag) {
            // Read-only wrapper
        }
        
        @Override
        public CompletenessResult isComplete(AbstractContainerMenu menu, Player player, int slotStart) {
            boolean complete = task.isComplete(player);
            return new CompletenessResult(slotStart, complete);
        }
        
        @Override
        public void onComplete(AbstractContainerMenu menu, Player player, int slotStart) {
            // No special completion action
        }
        
        @Override
        @OnlyIn(Dist.CLIENT)
        public int getWidth() {
            return 100;
        }
        
        @Override
        @OnlyIn(Dist.CLIENT)
        public void drawIcon(GuiGraphics stack, ResourceLocation texture, int x, int y) {
            // Draw weather icon based on type
            int iconOffset = switch (task.getWeather()) {
                case RAIN -> 64;
                case THUNDER -> 96;
                default -> 128; // CLEAR
            };
            stack.blit(texture, x, y, iconOffset, 128, 32, 32, 256, 256);
        }
        
        @Override
        @OnlyIn(Dist.CLIENT)
        public void drawTooltip(@NotNull GuiGraphics stack, AbstractContainerScreen<?> gui, double mouseX, double mouseY) {
            List<Component> tooltip = new ArrayList<>();
            
            String weatherKey = task.getWeather().name().toLowerCase();
            Component description = Component.translatable("task.eidolonunchained.weather." + weatherKey);
            
            tooltip.add(description);
            stack.renderComponentTooltip(Minecraft.getInstance().font, tooltip, (int) mouseX, (int) mouseY);
        }
    }
    
    public static class NbtTaskWrapper extends EidolonTaskWrapper {
        private final HasNbtTask task;
        
        public NbtTaskWrapper(HasNbtTask task) {
            this.task = task;
        }
        
        @Override
        public CompoundTag write() {
            CompoundTag tag = new CompoundTag();
            tag.put("required", task.getRequired());
            return tag;
        }
        
        @Override
        public void read(CompoundTag tag) {
            // Read-only wrapper
        }
        
        @Override
        public CompletenessResult isComplete(AbstractContainerMenu menu, Player player, int slotStart) {
            boolean complete = task.isComplete(player);
            return new CompletenessResult(slotStart, complete);
        }
        
        @Override
        public void onComplete(AbstractContainerMenu menu, Player player, int slotStart) {
            // No special completion action
        }
        
        @Override
        @OnlyIn(Dist.CLIENT)
        public int getWidth() {
            return 140;
        }
        
        @Override
        @OnlyIn(Dist.CLIENT)
        public void drawIcon(GuiGraphics stack, ResourceLocation texture, int x, int y) {
            // Draw NBT/data icon
            stack.blit(texture, x, y, 160, 128, 32, 32, 256, 256);
        }
        
        @Override
        @OnlyIn(Dist.CLIENT)
        public void drawTooltip(@NotNull GuiGraphics stack, AbstractContainerScreen<?> gui, double mouseX, double mouseY) {
            List<Component> tooltip = new ArrayList<>();
            
            // Try to create a more readable description from NBT
            CompoundTag required = task.getRequired();
            if (required.size() == 1) {
                String key = required.getAllKeys().iterator().next();
                Component description = Component.translatable("task.eidolonunchained.has_nbt.single", key);
                tooltip.add(description);
            } else {
                Component description = Component.translatable("task.eidolonunchained.has_nbt.multiple", required.size());
                tooltip.add(description);
            }
            
            stack.renderComponentTooltip(Minecraft.getInstance().font, tooltip, (int) mouseX, (int) mouseY);
        }
    }
}
