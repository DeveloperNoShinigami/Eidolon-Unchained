package com.bluelotuscoding.eidolonunchained.events;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.ai.PlayerContextTracker;
import com.bluelotuscoding.eidolonunchained.network.EidolonUnchainedNetworking;
import com.bluelotuscoding.eidolonunchained.network.RitualTaskProgressPacket;
import com.bluelotuscoding.eidolonunchained.research.tasks.UseRitualTask;
import com.bluelotuscoding.eidolonunchained.research.triggers.ResearchTriggerLoader;
import com.bluelotuscoding.eidolonunchained.research.triggers.ItemRequirementChecker;
import com.bluelotuscoding.eidolonunchained.research.triggers.data.ResearchTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.MinecraftForge;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

/**
 * Handles ritual completion events and research bridging.
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RitualEventHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    @SubscribeEvent
    public static void onRitualComplete(RitualCompleteEvent event) {
        if (event.isSuccessful()) {
            UseRitualTask.recordCompletion(event.getPlayer(), event.getRitualId());
            PlayerContextTracker.onRitualComplete(event.getPlayer(), event.getRitualId(), true);
            EidolonUnchainedNetworking.sendToPlayer(event.getPlayer(), RitualTaskProgressPacket.create(event.getPlayer()));

            triggerRitualResearch(event.getPlayer(), event.getRitualId());

            event.getPlayer().sendSystemMessage(
                net.minecraft.network.chat.Component.literal(
                    "§6[Ritual Complete] §e" + event.getRitualId().getPath().replace("_", " ") + " §6completed!"
                )
            );
        }
    }

    public static void fireRitualCompletion(ServerLevel level, BlockPos pos, ResourceLocation ritualId) {
        if (ritualId == null) {
            return;
        }

        LOGGER.info("[RITUAL_COMPLETE] Ritual completed at {}: id={}", pos, ritualId);

        // Find the nearest player to the brazier (same logic as AIDeityRitual.start)
        ServerPlayer nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (ServerPlayer p : level.players()) {
            double dist = p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = p;
            }
        }

        if (nearest == null) {
            LOGGER.warn("[RITUAL_COMPLETE] No players in level for ritual {} at {}", ritualId, pos);
            return;
        }

        LOGGER.info("[RITUAL_COMPLETE] Firing for player {} for ritual {}", nearest.getName().getString(), ritualId);
        MinecraftForge.EVENT_BUS.post(new RitualCompleteEvent(nearest, ritualId, true));
    }

    /**
     * Manual method to fire ritual completion for testing/admin commands
     */
    public static void fireRitualCompletion(ServerPlayer player, ResourceLocation ritualId) {
        RitualCompleteEvent event = new RitualCompleteEvent(player, ritualId, true);
        MinecraftForge.EVENT_BUS.post(event);
    }

    /**
     * Trigger research discovery based on ritual completion.
     * Gives a research note item (consistent with kill/location triggers) rather than
     * directly granting research, so the player must use the research table.
     */
    private static void triggerRitualResearch(ServerPlayer player, ResourceLocation ritualId) {
        try {
            for (Map.Entry<String, List<ResearchTrigger>> entry : ResearchTriggerLoader.getTriggersForAllResearch().entrySet()) {
                String researchId = entry.getKey();

                for (ResearchTrigger trigger : entry.getValue()) {
                    if ("ritual".equals(trigger.getType()) &&
                        trigger.getRitual() != null &&
                        trigger.getRitual().equals(ritualId)) {

                        long currentCount = PlayerContextTracker.getTriggeredResearchCount(player, researchId);
                        if (currentCount >= trigger.getMaxFound()) {
                            LOGGER.debug("Player {} already triggered ritual research '{}' {} times (max: {})",
                                player.getName().getString(), researchId, currentCount, trigger.getMaxFound());
                            continue;
                        }

                        if (!ItemRequirementChecker.checkItemRequirements(player, trigger.getItemRequirements())) {
                            continue;
                        }

                        String namespacedId = researchId.contains(":")
                            ? researchId
                            : new ResourceLocation("eidolonunchained", researchId).toString();

                        elucent.eidolon.api.research.Research research = elucent.eidolon.registries.Researches.find(
                            new ResourceLocation(namespacedId));

                        if (research == null) {
                            LOGGER.warn("Ritual trigger: research '{}' not found in Eidolon registry — cannot give note", namespacedId);
                            continue;
                        }

                        net.minecraft.world.item.ItemStack notes = new net.minecraft.world.item.ItemStack(
                            elucent.eidolon.registries.Registry.RESEARCH_NOTES.get(), 1);
                        net.minecraft.nbt.CompoundTag tag = notes.getOrCreateTag();
                        tag.putString("research", research.getRegistryName().toString());
                        tag.putInt("stepsDone", 0);
                        tag.putLong("worldSeed", elucent.eidolon.common.tile.ResearchTableTileEntity.SEED +
                            978060631L * ((net.minecraft.server.level.ServerLevel) player.level()).getSeed());

                        if (!player.getInventory().add(notes)) {
                            player.drop(notes, false);
                        }

                        PlayerContextTracker.trackTriggeredResearch(player, researchId);
                        LOGGER.info("Gave research note '{}' to player {} after completing ritual '{}' ({}/{} times)",
                            namespacedId, player.getName().getString(), ritualId, currentCount + 1, trigger.getMaxFound());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to grant research for ritual completion", e);
        }
    }
}
