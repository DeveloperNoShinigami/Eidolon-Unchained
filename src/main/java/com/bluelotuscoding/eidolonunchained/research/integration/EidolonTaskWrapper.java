package com.bluelotuscoding.eidolonunchained.research.integration;

import com.bluelotuscoding.eidolonunchained.research.tasks.EnterDimensionTask;
import com.bluelotuscoding.eidolonunchained.research.tasks.ExploreBiomesTask;
import com.bluelotuscoding.eidolonunchained.research.tasks.HasItemWithNbtTask;
import com.bluelotuscoding.eidolonunchained.research.tasks.HasNbtTask;
import com.bluelotuscoding.eidolonunchained.research.tasks.CraftItemsTask;
import com.bluelotuscoding.eidolonunchained.research.tasks.KillEntitiesTask;
import com.bluelotuscoding.eidolonunchained.research.tasks.KillEntityWithNbtTask;
import com.bluelotuscoding.eidolonunchained.research.tasks.TimeWindowTask;
import com.bluelotuscoding.eidolonunchained.research.tasks.UseRitualTask;
import com.bluelotuscoding.eidolonunchained.research.tasks.WeatherTask;
import elucent.eidolon.api.research.ResearchTask;
import elucent.eidolon.registries.Registry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
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
    protected int getSingleSlotTaskWidth() {
        return getDefaultWidth() + 8 + 17;
    }

    @OnlyIn(Dist.CLIENT)
    protected int drawSingleEmptySlotRail(@NotNull GuiGraphics stack, ResourceLocation texture, int x, int y) {
        stack.blit(texture, x, y, 0, 88, 224, 1, 32, 256, 256);
        stack.blit(texture, x + 1, y, 0, 192, 0, 22, 32, 256, 256);
        stack.blit(texture, x + 23, y, 0, 88, 224, 2, 32, 256, 256);
        return 25;
    }

    @OnlyIn(Dist.CLIENT)
    protected void drawItemIcon(@NotNull GuiGraphics stack, @NotNull ItemStack icon, int x, int y) {
        stack.renderItem(icon, x, y);
        stack.renderItemDecorations(Minecraft.getInstance().font, icon, x, y, null);
    }

    protected ItemStack getDimensionIcon(ResourceLocation dimension) {
        String key = dimension.toString();
        if (key.contains("the_nether") || key.contains("nether")) return new ItemStack(Items.NETHER_WART);
        if (key.contains("the_end") || key.endsWith(":end") || key.contains("/end")) return new ItemStack(Items.ENDER_PEARL);
        return new ItemStack(Items.GRASS_BLOCK);
    }

    protected ItemStack getBiomeIcon(ResourceLocation biome) {
        String key = biome.getPath();
        if (key.contains("desert") || key.contains("beach") || key.contains("badlands")) return new ItemStack(Items.SAND);
        if (key.contains("snow") || key.contains("frozen") || key.contains("ice")) return new ItemStack(Items.SNOW_BLOCK);
        if (key.contains("swamp") || key.contains("mangrove")) return new ItemStack(Items.LILY_PAD);
        if (key.contains("ocean") || key.contains("river")) return new ItemStack(Items.WATER_BUCKET);
        if (key.contains("nether")) return new ItemStack(Items.NETHERRACK);
        if (key.contains("end")) return new ItemStack(Items.END_STONE);
        return new ItemStack(Items.GRASS_BLOCK);
    }

    protected ItemStack getEntityIcon(ResourceLocation entityId) {
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(entityId);
        if (type != null) {
            SpawnEggItem egg = SpawnEggItem.byId(type);
            if (egg != null) return new ItemStack(egg);
        }
        return new ItemStack(Items.IRON_SWORD);
    }

    protected ItemStack getItemIcon(ResourceLocation itemId, ItemStack fallback) {
        var item = ForgeRegistries.ITEMS.getValue(itemId);
        return item == null ? fallback : new ItemStack(item);
    }
    
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
            return getSingleSlotTaskWidth();
        }
        
        @Override
        @OnlyIn(Dist.CLIENT)
        public void drawIcon(GuiGraphics stack, ResourceLocation texture, int x, int y) {
            drawItemIcon(stack, getDimensionIcon(task.getDimension()), x, y);
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public int drawCustom(@NotNull GuiGraphics stack, ResourceLocation texture, int x, int y) {
            return drawSingleEmptySlotRail(stack, texture, x, y);
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
            return getSingleSlotTaskWidth();
        }
        
        @Override
        @OnlyIn(Dist.CLIENT)
        public void drawIcon(GuiGraphics stack, ResourceLocation texture, int x, int y) {
            drawItemIcon(stack, getBiomeIcon(task.getBiome()), x, y);
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public int drawCustom(@NotNull GuiGraphics stack, ResourceLocation texture, int x, int y) {
            return drawSingleEmptySlotRail(stack, texture, x, y);
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
            return getSingleSlotTaskWidth();
        }
        
        @Override
        @OnlyIn(Dist.CLIENT)
        public void drawIcon(GuiGraphics stack, ResourceLocation texture, int x, int y) {
            drawItemIcon(stack, new ItemStack(Items.CLOCK), x, y);
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public int drawCustom(@NotNull GuiGraphics stack, ResourceLocation texture, int x, int y) {
            return drawSingleEmptySlotRail(stack, texture, x, y);
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
            return getSingleSlotTaskWidth();
        }
        
        @Override
        @OnlyIn(Dist.CLIENT)
        public void drawIcon(GuiGraphics stack, ResourceLocation texture, int x, int y) {
            drawItemIcon(stack, new ItemStack(Items.COMMAND_BLOCK), x, y);
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public int drawCustom(@NotNull GuiGraphics stack, ResourceLocation texture, int x, int y) {
            return drawSingleEmptySlotRail(stack, texture, x, y);
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

    public static class TimeWindowTaskWrapper extends EidolonTaskWrapper {
        private final TimeWindowTask task;

        public TimeWindowTaskWrapper(TimeWindowTask task) {
            this.task = task;
        }

        @Override
        public CompoundTag write() {
            CompoundTag tag = new CompoundTag();
            tag.putLong("min", task.getMin());
            tag.putLong("max", task.getMax());
            return tag;
        }

        @Override
        public void read(CompoundTag tag) {
        }

        @Override
        public CompletenessResult isComplete(AbstractContainerMenu menu, Player player, int slotStart) {
            return new CompletenessResult(slotStart, task.isComplete(player));
        }

        @Override
        public void onComplete(AbstractContainerMenu menu, Player player, int slotStart) {
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public int getWidth() {
            return getSingleSlotTaskWidth();
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public void drawIcon(GuiGraphics stack, ResourceLocation texture, int x, int y) {
            drawItemIcon(stack, new ItemStack(Items.CLOCK), x, y);
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public int drawCustom(@NotNull GuiGraphics stack, ResourceLocation texture, int x, int y) {
            return drawSingleEmptySlotRail(stack, texture, x, y);
        }
    }

    public static class CraftItemsTaskWrapper extends EidolonTaskWrapper {
        private final CraftItemsTask task;

        public CraftItemsTaskWrapper(CraftItemsTask task) {
            this.task = task;
        }

        private ItemStack getStationIcon() {
            return switch (task.getStation().toLowerCase()) {
                // Vanilla stations
                case "furnace"           -> new ItemStack(Items.FURNACE);
                case "blast_furnace"     -> new ItemStack(Items.BLAST_FURNACE);
                case "smoker"            -> new ItemStack(Items.SMOKER);
                case "smithing_table"    -> new ItemStack(Items.SMITHING_TABLE);
                case "anvil"             -> new ItemStack(Items.ANVIL);
                case "loom"              -> new ItemStack(Items.LOOM);
                case "cartography_table" -> new ItemStack(Items.CARTOGRAPHY_TABLE);
                case "grindstone"        -> new ItemStack(Items.GRINDSTONE);
                case "stonecutter"       -> new ItemStack(Items.STONECUTTER);
                case "enchanting_table"  -> new ItemStack(Items.ENCHANTING_TABLE);
                case "brewing_stand"     -> new ItemStack(Items.BREWING_STAND);
                // Eidolon stations
                case "wooden_altar", "altar"    -> getItemIcon(new ResourceLocation("eidolon", "wooden_altar"), new ItemStack(Items.OAK_SLAB));
                case "stone_altar"              -> getItemIcon(new ResourceLocation("eidolon", "stone_altar"), new ItemStack(Items.STONE_SLAB));
                case "worktable"                -> getItemIcon(new ResourceLocation("eidolon", "worktable"), new ItemStack(Items.CRAFTING_TABLE));
                case "research_table"           -> getItemIcon(new ResourceLocation("eidolon", "research_table"), new ItemStack(Items.BOOK));
                case "scriptorium"              -> getItemIcon(new ResourceLocation("eidolon", "scriptorium"), new ItemStack(Items.BOOKSHELF));
                case "crucible"                 -> getItemIcon(new ResourceLocation("eidolon", "crucible"), new ItemStack(Items.CAULDRON));
                case "soul_enchanter"           -> getItemIcon(new ResourceLocation("eidolon", "soul_enchanter"), new ItemStack(Items.ENCHANTING_TABLE));
                default                         -> new ItemStack(Items.CRAFTING_TABLE);
            };
        }

        @Override
        public CompoundTag write() {
            CompoundTag tag = new CompoundTag();
            tag.putString("item", task.getItem().toString());
            tag.putInt("count", task.getCount());
            tag.putString("station", task.getStation());
            return tag;
        }

        @Override
        public void read(CompoundTag tag) {
        }

        @Override
        public CompletenessResult isComplete(AbstractContainerMenu menu, Player player, int slotStart) {
            return new CompletenessResult(slotStart, task.isComplete(player));
        }

        @Override
        public void onComplete(AbstractContainerMenu menu, Player player, int slotStart) {
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public int getWidth() {
            return getSingleSlotTaskWidth();
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public void drawIcon(GuiGraphics stack, ResourceLocation texture, int x, int y) {
            drawItemIcon(stack, getStationIcon(), x, y);
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public int drawCustom(@NotNull GuiGraphics stack, ResourceLocation texture, int x, int y) {
            return drawSingleEmptySlotRail(stack, texture, x, y);
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public void drawTooltip(@NotNull GuiGraphics stack, AbstractContainerScreen<?> gui, double mouseX, double mouseY) {
            List<Component> tooltip = new ArrayList<>();
            String itemName = task.getItem().getPath().replace('_', ' ');
            String stationName = task.getStation().replace('_', ' ');
            int crafted = CraftItemsTask.getClientCraftCount(task.getItem());
            if (task.getCount() > 1) {
                tooltip.add(Component.literal("Craft " + task.getCount() + "x " + itemName));
                tooltip.add(Component.literal("Progress: " + crafted + "/" + task.getCount()));
            } else {
                tooltip.add(Component.literal("Craft: " + itemName));
                tooltip.add(Component.literal("Progress: " + crafted + "/1"));
            }
            tooltip.add(Component.literal("Station: " + stationName));
            stack.renderComponentTooltip(Minecraft.getInstance().font, tooltip, (int) mouseX, (int) mouseY);
        }
    }

    public static class KillEntitiesTaskWrapper extends EidolonTaskWrapper {
        private final KillEntitiesTask task;

        public KillEntitiesTaskWrapper(KillEntitiesTask task) {
            this.task = task;
        }

        @Override
        public CompoundTag write() {
            CompoundTag tag = new CompoundTag();
            tag.putString("entity", task.getEntity().toString());
            tag.putInt("count", task.getCount());
            return tag;
        }

        @Override
        public void read(CompoundTag tag) {
        }

        @Override
        public CompletenessResult isComplete(AbstractContainerMenu menu, Player player, int slotStart) {
            return new CompletenessResult(slotStart, task.isComplete(player));
        }

        @Override
        public void onComplete(AbstractContainerMenu menu, Player player, int slotStart) {
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public int getWidth() {
            return getSingleSlotTaskWidth();
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public void drawIcon(GuiGraphics stack, ResourceLocation texture, int x, int y) {
            drawItemIcon(stack, getEntityIcon(task.getEntity()), x, y);
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public int drawCustom(@NotNull GuiGraphics stack, ResourceLocation texture, int x, int y) {
            return drawSingleEmptySlotRail(stack, texture, x, y);
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public void drawTooltip(@NotNull GuiGraphics stack, AbstractContainerScreen<?> gui, double mouseX, double mouseY) {
            List<Component> tooltip = new ArrayList<>();
            String entityName = task.getEntity().getPath().replace('_', ' ');
            int killed = KillEntitiesTask.getClientKillCount(task.getEntity());

            if (task.getCount() > 1) {
                tooltip.add(Component.literal("Kill " + task.getCount() + "x " + entityName));
                tooltip.add(Component.literal("Progress: " + killed + "/" + task.getCount()));
            } else {
                tooltip.add(Component.literal("Kill: " + entityName));
                tooltip.add(Component.literal("Progress: " + killed + "/1"));
            }

            stack.renderComponentTooltip(Minecraft.getInstance().font, tooltip, (int) mouseX, (int) mouseY);
        }
    }

    public static class KillEntityNbtTaskWrapper extends EidolonTaskWrapper {
        private final KillEntityWithNbtTask task;

        public KillEntityNbtTaskWrapper(KillEntityWithNbtTask task) {
            this.task = task;
        }

        @Override
        public CompoundTag write() {
            CompoundTag tag = new CompoundTag();
            tag.putString("entity", task.getEntity().toString());
            tag.putInt("count", task.getCount());
            if (task.getFilter() != null) tag.put("filter", task.getFilter().copy());
            return tag;
        }

        @Override
        public void read(CompoundTag tag) {
        }

        @Override
        public CompletenessResult isComplete(AbstractContainerMenu menu, Player player, int slotStart) {
            return new CompletenessResult(slotStart, task.isComplete(player));
        }

        @Override
        public void onComplete(AbstractContainerMenu menu, Player player, int slotStart) {
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public int getWidth() {
            return getSingleSlotTaskWidth();
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public void drawIcon(GuiGraphics stack, ResourceLocation texture, int x, int y) {
            drawItemIcon(stack, getEntityIcon(task.getEntity()), x, y);
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public int drawCustom(@NotNull GuiGraphics stack, ResourceLocation texture, int x, int y) {
            return drawSingleEmptySlotRail(stack, texture, x, y);
        }
    }

    public static class HasItemNbtTaskWrapper extends EidolonTaskWrapper {
        private final HasItemWithNbtTask task;

        public HasItemNbtTaskWrapper(HasItemWithNbtTask task) {
            this.task = task;
        }

        @Override
        public CompoundTag write() {
            CompoundTag tag = new CompoundTag();
            tag.putString("item", task.getItem().toString());
            tag.putInt("count", task.getCount());
            if (task.getFilter() != null) tag.put("filter", task.getFilter().copy());
            return tag;
        }

        @Override
        public void read(CompoundTag tag) {
        }

        @Override
        public CompletenessResult isComplete(AbstractContainerMenu menu, Player player, int slotStart) {
            return new CompletenessResult(slotStart, task.isComplete(player));
        }

        @Override
        public void onComplete(AbstractContainerMenu menu, Player player, int slotStart) {
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public int getWidth() {
            return getSingleSlotTaskWidth();
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public void drawIcon(GuiGraphics stack, ResourceLocation texture, int x, int y) {
            drawItemIcon(stack, getItemIcon(task.getItem(), new ItemStack(Items.COMMAND_BLOCK)), x, y);
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public int drawCustom(@NotNull GuiGraphics stack, ResourceLocation texture, int x, int y) {
            return drawSingleEmptySlotRail(stack, texture, x, y);
        }
    }

    public static class RitualTaskWrapper extends EidolonTaskWrapper {
        private final UseRitualTask task;

        public RitualTaskWrapper(UseRitualTask task) {
            this.task = task;
        }

        @Override
        public CompoundTag write() {
            CompoundTag tag = new CompoundTag();
            tag.putString("ritual", task.getRitual().toString());
            tag.putInt("count", task.getCount());
            return tag;
        }

        @Override
        public void read(CompoundTag tag) {
            // Read-only wrapper
        }

        @Override
        public CompletenessResult isComplete(AbstractContainerMenu menu, Player player, int slotStart) {
            return new CompletenessResult(slotStart, task.isComplete(player));
        }

        @Override
        public void onComplete(AbstractContainerMenu menu, Player player, int slotStart) {
            // Ritual completion is tracked outside the research table.
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public int getWidth() {
            return getSingleSlotTaskWidth();
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public void drawIcon(GuiGraphics stack, ResourceLocation texture, int x, int y) {
            drawItemIcon(stack, new ItemStack(Registry.BRAZIER.get()), x, y);
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public int drawCustom(@NotNull GuiGraphics stack, ResourceLocation texture, int x, int y) {
            return drawSingleEmptySlotRail(stack, texture, x, y);
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public void drawTooltip(@NotNull GuiGraphics stack, AbstractContainerScreen<?> gui, double mouseX, double mouseY) {
            List<Component> tooltip = new ArrayList<>();
            String ritualName = task.getRitual().getPath().replace('_', ' ');
            int completed = UseRitualTask.getClientCompletionCount(task.getRitual());

            if (task.getCount() > 1) {
                tooltip.add(Component.literal("Complete ritual " + task.getCount() + "x: " + ritualName));
                tooltip.add(Component.literal("Progress: " + completed + "/" + task.getCount()));
            } else {
                tooltip.add(Component.literal("Complete ritual: " + ritualName));
                tooltip.add(Component.literal("Progress: " + completed + "/1"));
            }

            stack.renderComponentTooltip(Minecraft.getInstance().font, tooltip, (int) mouseX, (int) mouseY);
        }
    }
}
