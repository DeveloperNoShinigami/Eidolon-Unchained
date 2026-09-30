package com.bluelotuscoding.eidolonunchained.api;

import elucent.eidolon.api.deity.Deity;
import elucent.eidolon.api.spells.Rune;
import elucent.eidolon.api.spells.Sign;
import elucent.eidolon.common.deity.Deities;
import elucent.eidolon.registries.Researches;
import elucent.eidolon.registries.Runes;
import elucent.eidolon.registries.Signs;
import elucent.eidolon.registries.Spells;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/** The builders and lookups behind the {@code EidolonUnchained} script global. Kept free of KubeJS types. */
public final class EUApi {
    private EUApi() {
    }

    // ---- declare (startup) ----

    public static SignBuilder sign(String id) {
        return new SignBuilder(Ids.newId(id, "sign"));
    }

    public static RuneBuilder rune(String id) {
        return new RuneBuilder(Ids.newId(id, "rune"));
    }

    public static ResearchBuilder research(String id) {
        return new ResearchBuilder(Ids.newId(id, "research"));
    }

    public static DeityBuilder deity(String id) {
        return new DeityBuilder(Ids.newId(id, "deity"), false);
    }

    public static DeityBuilder extendDeity(String id) {
        return new DeityBuilder(Ids.of(id, "deity"), true);
    }

    public static SpellBuilder spell(String id) {
        return new SpellBuilder(Ids.newId(id, "spell"));
    }

    public static SpellBuilder.Prayer prayer(String id) {
        return new SpellBuilder.Prayer(Ids.newId(id, "prayer"));
    }

    public static List<String> spells() {
        return Spells.getSpells().stream().map(sp -> sp.getRegistryName().toString()).toList();
    }

    public static RitualBuilder ritual(String id) {
        return new RitualBuilder(Ids.newId(id, "ritual"));
    }

    /** Ids of the rituals scripts declared (Eidolon keeps its own ritual map private). */
    public static List<String> rituals() {
        return EURegistry.declared(EURegistry.Stage.RITUALS).stream().map(ResourceLocation::toString).toList();
    }

    // ---- helpers ----

    /** The same helper KubeJS attaches as {@code player.data.eidolon}. */
    public static PlayerHelper player(Player player) {
        if (player == null) throw new IllegalArgumentException("Eidolon Unchained: player(p) needs a player");
        return new PlayerHelper(player);
    }

    public static SoulHelper soul(LivingEntity entity) {
        return SoulHelper.of(entity);
    }

    // ---- look up (any time after registration) ----

    public static List<String> signs() {
        return Signs.getSigns().stream().map(s -> s.getRegistryName().toString()).toList();
    }

    public static Sign findSign(String id) {
        return Signs.find(Ids.of(id, "sign"));
    }

    public static List<String> runes() {
        return Runes.getRunes().stream().map(r -> r.getRegistryName().toString()).toList();
    }

    public static Rune findRune(String id) {
        return Runes.find(Ids.of(id, "rune"));
    }

    public static List<String> researches() {
        return Researches.getResearches().stream().map(r -> r.getRegistryName().toString()).toList();
    }

    public static List<String> deities() {
        return Deities.getDeities().stream().map(d -> d.getId().toString()).toList();
    }

    public static Deity findDeity(String id) {
        return Deities.find(Ids.of(id, "deity"));
    }

    public static List<String> stages(String deityId) {
        var d = findDeity(deityId);
        if (d == null) return List.of();
        return d.getProgression().getSteps().values().stream().map(s -> s.id().toString()).toList();
    }

    public static String deityModel(String deityId) {
        var m = DeityHooks.model(Ids.of(deityId, "deity"));
        return m == null ? null : m.toString();
    }

    public static ResourceLocation id(String id) {
        return Ids.of(id, "id");
    }
}
