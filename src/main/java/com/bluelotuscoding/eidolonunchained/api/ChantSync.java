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
    private static final Map<net.minecraft.resources.ResourceLocation, java.util.List<elucent.eidolon.api.spells.Sign>> SEQUENCES = new HashMap<>();

    private ChantSync() {
    }

    /** True when some chant recipe's sequence is longer than {@code prefix} and starts with it (active chanting waits for it). */
    public static boolean hasLongerSequenceStartingWith(java.util.List<elucent.eidolon.api.spells.Sign> prefix) {
        for (var seq : SEQUENCES.values()) {
            if (seq.size() <= prefix.size()) continue;
            boolean ok = true;
            for (int i = 0; i < prefix.size(); i++) if (!seq.get(i).getRegistryName().equals(prefix.get(i).getRegistryName())) { ok = false; break; }
            if (ok) return true;
        }
        return false;
    }

    /** The current chant recipe's signs for a spell id (empty when it has none). */
    public static java.util.List<elucent.eidolon.api.spells.Sign> sequenceOf(net.minecraft.resources.ResourceLocation spell) {
        return SEQUENCES.getOrDefault(spell, java.util.List.of());
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
        SEQUENCES.clear();
        Map<net.minecraft.resources.ResourceLocation, SignSequence> byId = new HashMap<>();
        for (var type : Spells.chantTypes) {
            for (ChantRecipe recipe : server.getRecipeManager().getAllRecipesFor(type)) {
                byId.put(recipe.getId(), new SignSequence(recipe.signs()));
                SEQUENCES.put(recipe.getId(), java.util.List.of(recipe.signs()));
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
        warnClashes();
    }

    /**
     * Two chants with the same signs: Eidolon resolves a sequence to the first match, so the other one can never be
     * chanted or written to a scroll (a scroll of it reads as the first). Checks recipe against recipe, and recipes
     * against Eidolon's fixed-sign spells (its prayers and sacrifices, and scripted prayers).
     */
    private static void warnClashes() {
        Map<String, java.util.List<net.minecraft.resources.ResourceLocation>> byKey = new HashMap<>();
        for (var e : SEQUENCES.entrySet()) byKey.computeIfAbsent(key(e.getValue()), k -> new java.util.ArrayList<>()).add(e.getKey());
        for (var e : byKey.entrySet()) {
            if (e.getValue().size() > 1) EidolonUnchained.LOGGER.warn("Chants {} share the signs {}: only one of them can be chanted", e.getValue(), e.getKey());
        }
        for (var e : SEQUENCES.entrySet()) {
            var seq = new SignSequence(e.getValue().toArray(new elucent.eidolon.api.spells.Sign[0]));
            for (var spell : Spells.getSpellMap().values()) {
                if (spell instanceof ScriptedSpell || spell.getRegistryName().equals(e.getKey()) || SEQUENCES.containsKey(spell.getRegistryName())) continue;
                try {
                    if (spell.matches(seq)) EidolonUnchained.LOGGER.warn("Chant {} has the same signs ({}) as {}: a scroll or chant of them is read as {}",
                            e.getKey(), key(e.getValue()), spell.getRegistryName(), spell.getRegistryName());
                } catch (RuntimeException ignored) {
                    // a spell whose signs are only set once a recipe resolves it
                }
            }
        }
    }

    private static String key(java.util.List<elucent.eidolon.api.spells.Sign> signs) {
        var sb = new StringBuilder();
        for (var s : signs) sb.append(sb.length() == 0 ? "" : ", ").append(s.getRegistryName());
        return sb.toString();
    }
}
