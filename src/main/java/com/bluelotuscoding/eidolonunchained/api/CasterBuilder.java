package com.bluelotuscoding.eidolonunchained.api;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.casting.CasterProfile;
import dev.latvian.mods.kubejs.typings.Info;
import elucent.eidolon.common.deity.Deities;
import elucent.eidolon.registries.Spells;
import net.minecraft.resources.ResourceLocation;

/** {@code EidolonUnchained.caster(id)…} — a mob caster profile (rule C4). Registered at stage CASTERS, after spells. */
public final class CasterBuilder {
    final CasterProfile profile;

    CasterBuilder(ResourceLocation id) {
        this.profile = new CasterProfile(id);
        EURegistry.declare(EURegistry.Stage.CASTERS, id, "caster profile", this::register);
    }

    @Info("Chant rotation, in order; each needs a mob path (.mobCast or .targetCast)")
    public CasterBuilder chants(String... spellIds) {
        for (var s : spellIds) profile.spells.add(Ids.of(s, "spell"));
        return this;
    }

    public CasterBuilder deity(String deityId) {
        profile.deity = Ids.of(deityId, "deity");
        return this;
    }

    @Info("Max mana and regeneration per second (Eidolon's ISoul)")
    public CasterBuilder mana(float max, float regenPerSecond) {
        profile.maxMana = Math.max(0, max);
        profile.regenPerSecond = Math.max(0, regenPerSecond);
        return this;
    }

    @Info("Ticks between casts")
    public CasterBuilder castInterval(int ticks) {
        profile.castInterval = Math.max(0, ticks);
        return this;
    }

    @Info("Ticks between signs while the mob builds a chant")
    public CasterBuilder signDelay(int ticks) {
        profile.signDelay = Math.max(1, ticks);
        return this;
    }

    public CasterBuilder range(double min, double max) {
        profile.minRange = Math.max(0, min);
        profile.maxRange = Math.max(profile.minRange, max);
        return this;
    }

    public CasterBuilder requireLineOfSight(boolean required) {
        profile.requireLineOfSight = required;
        return this;
    }

    @Info("Per-chant cooldown in ticks")
    public CasterBuilder cooldown(String spellId, int ticks) {
        profile.cooldowns.put(Ids.of(spellId, "spell"), Math.max(0, ticks));
        return this;
    }

    @Info("'attack_target' (default) | 'nearest_player' | 'allies'")
    public CasterBuilder targetPolicy(String policy) {
        profile.targetPolicy = switch (policy.toLowerCase()) {
            case "attack_target" -> CasterProfile.TargetPolicy.ATTACK_TARGET;
            case "nearest_player" -> CasterProfile.TargetPolicy.NEAREST_PLAYER;
            case "allies" -> CasterProfile.TargetPolicy.ALLIES;
            default -> throw new IllegalArgumentException("Eidolon Unchained: unknown target policy '" + policy + "'");
        };
        return this;
    }

    public CasterBuilder suppressMeleeWhileCasting(boolean suppress) {
        profile.suppressMeleeWhileCasting = suppress;
        return this;
    }

    private void register(ResourceLocation id) {
        if (profile.spells.isEmpty()) throw new IllegalStateException("caster profile '" + id + "' has no .spells(...)");
        for (var s : profile.spells) {
            var spell = Spells.find(s);
            if (spell == null) throw new IllegalStateException("caster profile '" + id + "': unknown spell '" + s + "'");
            if (!(spell instanceof ScriptedSpell ss) || !ss.hasMobPath()) {
                EidolonUnchained.LOGGER.warn("caster profile '{}': spell '{}' has no mob path (.mobCast / .targetCast); mobs will skip it", id, s);
            }
        }
        if (profile.deity != null && Deities.find(profile.deity) == null) throw new IllegalStateException("caster profile '" + id + "': unknown deity '" + profile.deity + "'");
        CasterProfile.REGISTRY.put(id, profile);
    }
}
