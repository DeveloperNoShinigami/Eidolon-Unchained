package com.bluelotuscoding.eidolonunchained.api;

import dev.latvian.mods.kubejs.typings.Info;
import elucent.eidolon.api.spells.Sign;
import elucent.eidolon.common.deity.Deities;
import elucent.eidolon.common.spell.PrayerSpell;
import elucent.eidolon.registries.Signs;
import elucent.eidolon.registries.Spells;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code EidolonUnchained.spell(id).cost(n).delay(t).canCast(fn).cast(fn)} — ends in
 * {@code Spells.register(new ScriptedSpell(...))} under T1. {@code .deity(id).minReputation(n)} makes it deity-bound.
 * {@code EidolonUnchained.prayer(id).deity(id).signs(...)} builds Eidolon's own {@code PrayerSpell} (effigy prayers are
 * static, so they take their signs here and need no chant recipe).
 */
public final class SpellBuilder {
    final ResourceLocation id;
    int cost = 0;
    int delay = 10;
    ScriptedSpell.CastCheck canCast;
    ScriptedSpell.CastAction cast;
    ResourceLocation deity;
    double minReputation = 0;
    ScriptedSpell.MobCast mobCast;
    ScriptedSpell.MobCheck mobCanCast;
    ScriptedSpell.TargetCast targetCast;
    boolean imbuable = true;
    int imbueCost = 4;

    SpellBuilder(ResourceLocation id) {
        this.id = id;
        EURegistry.declare(EURegistry.Stage.SPELLS, id, "spell", this::register);
    }

    @Info("Mana cost (Eidolon: cost); also editable in the generated server config")
    public SpellBuilder cost(int cost) {
        if (cost < 0) throw new IllegalArgumentException("Eidolon Unchained: spell '" + id + "' cost must be >= 0");
        this.cost = cost;
        return this;
    }

    @Info("Ticks between chant completion and the cast (Eidolon: delay, default 10)")
    public SpellBuilder delay(int ticks) {
        if (ticks < 0) throw new IllegalArgumentException("Eidolon Unchained: spell '" + id + "' delay must be >= 0");
        this.delay = ticks;
        return this;
    }

    @Info("(level, pos, player) => boolean; runs after Eidolon's mana check")
    public SpellBuilder canCast(ScriptedSpell.CastCheck fn) {
        this.canCast = fn;
        return this;
    }

    @Info("(level, pos, player) => …; the effect")
    public SpellBuilder cast(ScriptedSpell.CastAction fn) {
        this.cast = fn;
        return this;
    }

    @Info("Bind to a deity: casting needs reputation with it (see .minReputation)")
    public SpellBuilder deity(String deityId) {
        this.deity = Ids.of(deityId, "deity");
        return this;
    }

    public SpellBuilder minReputation(double reputation) {
        this.minReputation = reputation;
        return this;
    }

    @Info("Mob path (rule C3): (level, pos, caster, target) => …; without it (or targetCast) mobs skip the chant")
    public SpellBuilder mobCast(ScriptedSpell.MobCast fn) {
        this.mobCast = fn;
        return this;
    }

    @Info("(level, pos, caster, target) => boolean, checked before a mob casts")
    public SpellBuilder mobCanCast(ScriptedSpell.MobCheck fn) {
        this.mobCanCast = fn;
        return this;
    }

    @Info("Targeted path: (level, caster, target) => …; used by imbued weapons, Deity's Protection, and mobs when mobCast is absent")
    public SpellBuilder targetCast(ScriptedSpell.TargetCast fn) {
        this.targetCast = fn;
        return this;
    }

    @Info("Keep this chant off imbued weapons (every chant is imbuable by default, D35)")
    public SpellBuilder notImbuable() {
        this.imbuable = false;
        return this;
    }

    @Info("Soul shards needed to imbue a weapon with this chant (default 4)")
    public SpellBuilder imbueCost(int shards) {
        this.imbueCost = Math.max(0, shards);
        return this;
    }

    private void register(ResourceLocation id) {
        if (cast == null) throw new IllegalStateException("spell '" + id + "' has no .cast(...)");
        if (Spells.find(id) != null) throw new IllegalStateException("a spell with id '" + id + "' already exists");
        if (deity != null && Deities.find(deity) == null) throw new IllegalStateException("spell '" + id + "': unknown deity '" + deity + "'");
        var spell = new ScriptedSpell(id, cost, delay, canCast, cast, deity, minReputation);
        spell.mobCast = mobCast;
        spell.mobCanCast = mobCanCast;
        spell.targetCast = targetCast;
        spell.imbuable = imbuable;
        spell.imbueCost = imbueCost;
        Spells.register(spell);
    }

    /** {@code EidolonUnchained.prayer(id)}: Eidolon's {@code PrayerSpell}, cast at an effigy with fixed signs. */
    public static final class Prayer {
        final ResourceLocation id;
        ResourceLocation deity;
        int cost = 0;
        int reputation = 0;
        double power = 1.0;
        final List<ResourceLocation> signs = new ArrayList<>();

        Prayer(ResourceLocation id) {
            this.id = id;
            EURegistry.declare(EURegistry.Stage.SPELLS, id, "prayer", this::register);
        }

        public Prayer deity(String deityId) {
            this.deity = Ids.of(deityId, "deity");
            return this;
        }

        public Prayer cost(int cost) {
            this.cost = cost;
            return this;
        }

        @Info("Reputation granted by the prayer")
        public Prayer reputation(int reputation) {
            this.reputation = reputation;
            return this;
        }

        @Info("Power multiplier of the prayer's effect")
        public Prayer power(double power) {
            this.power = power;
            return this;
        }

        @Info("The sign sequence, in order")
        public Prayer signs(String... signIds) {
            for (var s : signIds) signs.add(Ids.of(s, "sign"));
            return this;
        }

        private void register(ResourceLocation id) {
            if (deity == null) throw new IllegalStateException("prayer '" + id + "' has no .deity(...)");
            var d = Deities.find(deity);
            if (d == null) throw new IllegalStateException("prayer '" + id + "': unknown deity '" + deity + "'");
            if (signs.isEmpty()) throw new IllegalStateException("prayer '" + id + "' has no .signs(...)");
            var resolved = new Sign[signs.size()];
            for (int i = 0; i < resolved.length; i++) {
                resolved[i] = Signs.find(signs.get(i));
                if (resolved[i] == null) throw new IllegalStateException("prayer '" + id + "': unknown sign '" + signs.get(i) + "'");
            }
            if (Spells.find(id) != null) throw new IllegalStateException("a spell with id '" + id + "' already exists");
            Spells.register(new PrayerSpell(id, d, cost, reputation, power, resolved));
        }
    }
}
