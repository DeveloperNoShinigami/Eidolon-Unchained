package com.bluelotuscoding.eidolonunchained.events;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.data.DeityScriptManager;
import com.bluelotuscoding.eidolonunchained.patron.PatronState;
import elucent.eidolon.api.research.Research;
import elucent.eidolon.registries.Researches;
import elucent.eidolon.util.KnowledgeUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Bridges knowledge progression to deity rule events.
 * Detects newly learned research/facts and fires:
 * - on_research_completed (with research_id)
 * - on_fact_gained (with fact_id)
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class KnowledgeEventBridge {
    private static final int CHECK_INTERVAL_TICKS = 20;
    private static int tickCounter = 0;

    private static final Map<UUID, Set<ResourceLocation>> LAST_KNOWN_RESEARCH = new HashMap<>();
    private static final Map<UUID, Set<ResourceLocation>> LAST_KNOWN_FACTS = new HashMap<>();

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer() == null) {
            return;
        }

        tickCounter++;
        if (tickCounter < CHECK_INTERVAL_TICKS) {
            return;
        }
        tickCounter = 0;

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            checkKnowledgeChanges(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID playerId = player.getUUID();
            LAST_KNOWN_RESEARCH.remove(playerId);
            LAST_KNOWN_FACTS.remove(playerId);
        }
    }

    private static void checkKnowledgeChanges(ServerPlayer player) {
        ResourceLocation patron = PatronState.getPatron(player);
        if (patron == null) {
            initializeSnapshotIfNeeded(player);
            return;
        }

        UUID playerId = player.getUUID();
        Set<ResourceLocation> currentResearch = getKnownResearch(player);
        Set<ResourceLocation> currentFacts = getKnownFacts(player);

        Set<ResourceLocation> previousResearch = LAST_KNOWN_RESEARCH.get(playerId);
        Set<ResourceLocation> previousFacts = LAST_KNOWN_FACTS.get(playerId);

        // First snapshot: do not emit backfilled events.
        if (previousResearch == null || previousFacts == null) {
            LAST_KNOWN_RESEARCH.put(playerId, currentResearch);
            LAST_KNOWN_FACTS.put(playerId, currentFacts);
            return;
        }

        Set<ResourceLocation> gainedResearch = new HashSet<>(currentResearch);
        gainedResearch.removeAll(previousResearch);

        Set<ResourceLocation> gainedFacts = new HashSet<>(currentFacts);
        gainedFacts.removeAll(previousFacts);

        for (ResourceLocation researchId : gainedResearch) {
            HashMap<String, String> filters = new HashMap<>();
            filters.put("research_id", researchId.toString());
            DeityScriptManager.fireRuleEvent(player, patron, "on_research_completed", 0, filters);
        }

        for (ResourceLocation factId : gainedFacts) {
            HashMap<String, String> filters = new HashMap<>();
            filters.put("fact_id", factId.toString());
            DeityScriptManager.fireRuleEvent(player, patron, "on_fact_gained", 0, filters);
        }

        LAST_KNOWN_RESEARCH.put(playerId, currentResearch);
        LAST_KNOWN_FACTS.put(playerId, currentFacts);
    }

    private static void initializeSnapshotIfNeeded(ServerPlayer player) {
        UUID playerId = player.getUUID();
        LAST_KNOWN_RESEARCH.putIfAbsent(playerId, getKnownResearch(player));
        LAST_KNOWN_FACTS.putIfAbsent(playerId, getKnownFacts(player));
    }

    private static Set<ResourceLocation> getKnownResearch(ServerPlayer player) {
        Set<ResourceLocation> known = new HashSet<>();
        for (Research research : Researches.getResearches()) {
            ResourceLocation id = research.getRegistryName();
            if (id != null && KnowledgeUtil.knowsResearch(player, id)) {
                known.add(id);
            }
        }
        return known;
    }

    private static Set<ResourceLocation> getKnownFacts(ServerPlayer player) {
        return player.getCapability(elucent.eidolon.capability.IKnowledge.INSTANCE)
            .map(k -> new HashSet<>(k.getKnownFacts()))
            .orElseGet(HashSet::new);
    }
}
