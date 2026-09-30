package com.bluelotuscoding.eidolonunchained.api;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import elucent.eidolon.api.spells.SignSequence;
import elucent.eidolon.recipe.ChantRecipe;
import elucent.eidolon.registries.Spells;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

/**
 * Timing rule T3 (D26): after every datapack (re)load, each scripted spell takes its sign sequence from the current
 * {@code eidolon:chant} recipe with the same id, so an edited sequence works after {@code /reload} without a restart.
 * Eidolon caches recipe-resolved spells in its own static list; because a scripted spell's {@code matches} reads the
 * refreshed sequence from the same instance, that cache does not go stale for scripted spells. (Eidolon's own static
 * spells keep Eidolon's behaviour.)
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MOD_ID)
public final class ChantSync {
    private ChantSync() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        refresh(event.getServer());                     // recipes are loaded by now
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) return;          // a single player joining; recipes did not change
        refresh(event.getPlayerList().getServer());     // /reload
    }

    public static void refresh(MinecraftServer server) {
        Map<net.minecraft.resources.ResourceLocation, SignSequence> byId = new HashMap<>();
        for (var type : Spells.chantTypes) {
            for (ChantRecipe recipe : server.getRecipeManager().getAllRecipesFor(type)) {
                byId.put(recipe.getId(), new SignSequence(recipe.signs()));
            }
        }
        int linked = 0, orphaned = 0;
        for (var spell : Spells.getSpellMap().values()) {          // getSpells() is only Eidolon's recipe-resolved cache
            if (spell instanceof ScriptedSpell ss) {
                var seq = byId.get(ss.getRegistryName());
                ss.refreshSigns(seq);
                if (seq != null) linked++; else orphaned++;
            }
        }
        if (linked + orphaned > 0) {
            EidolonUnchained.LOGGER.info("Chant recipes linked to scripted spells: {} linked{}", linked,
                    orphaned == 0 ? "" : ", " + orphaned + " spell(s) have no eidolon:chant recipe with their id and cannot be chanted");
        }
    }
}
