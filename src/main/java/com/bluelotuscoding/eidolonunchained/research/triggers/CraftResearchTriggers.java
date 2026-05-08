package com.bluelotuscoding.eidolonunchained.research.triggers;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.data.ResearchDataManager;
import com.bluelotuscoding.eidolonunchained.network.EidolonUnchainedNetworking;
import com.bluelotuscoding.eidolonunchained.network.RitualTaskProgressPacket;
import com.bluelotuscoding.eidolonunchained.research.tasks.CraftItemsTask;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

/**
 * Tracks craft/smelt events for CraftItemsTask progress.
 * Fires on any crafting station that calls ForgeEventFactory.firePlayerCraftingEvent()
 * — vanilla crafting table, stonecutter, Eidolon worktable, furnace output, etc.
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CraftResearchTriggers {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack result = event.getCrafting();
        if (result.isEmpty()) return;
        handleCraft(player, result);
    }

    @SubscribeEvent
    public static void onItemSmelted(PlayerEvent.ItemSmeltedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack result = event.getSmelting();
        if (result.isEmpty()) return;
        handleCraft(player, result);
    }

    private static void handleCraft(ServerPlayer player, ItemStack result) {
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(result.getItem());
        if (itemId == null) return;

        CraftItemsTask.recordCraft(player, itemId, result.getCount());
        ResearchDataManager.runCraftTaskCommands(player, itemId);
        EidolonUnchainedNetworking.sendToPlayer(player, RitualTaskProgressPacket.create(player));

        LOGGER.trace("Player {} crafted {} x{}", player.getName().getString(), itemId, result.getCount());
    }
}
