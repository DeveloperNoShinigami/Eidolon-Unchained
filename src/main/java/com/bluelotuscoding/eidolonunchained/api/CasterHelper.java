package com.bluelotuscoding.eidolonunchained.api;

import com.bluelotuscoding.eidolonunchained.casting.CasterProfile;
import com.bluelotuscoding.eidolonunchained.casting.CasterProfileResolver;
import com.bluelotuscoding.eidolonunchained.casting.MobCasting;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.List;

/** {@code EidolonUnchained.caster(entity)}: a mob's resolved caster profile and ways to change it (rule C4). */
public final class CasterHelper {
    private final Mob mob;

    CasterHelper(LivingEntity entity) {
        if (!(entity instanceof Mob m)) throw new IllegalArgumentException("Eidolon Unchained: caster(entity) needs a mob (players cast through chants)");
        this.mob = m;
    }

    public boolean isCaster() {
        return CasterProfileResolver.cached(mob) != null;
    }

    @Info("Effective profile: spells, deity, mana, ranges… (null when the mob is no caster)")
    public CasterProfile profile() {
        return CasterProfileResolver.cached(mob);
    }

    public List<String> chants() {
        var p = profile();
        return p == null ? List.of() : p.spells.stream().map(Object::toString).toList();
    }

    @Info("Write a profile reference into the mob's NBT and re-resolve")
    public CasterHelper setProfile(String profileId) {
        var rl = Ids.of(profileId, "caster profile");
        if (CasterProfile.find(rl) == null) throw new IllegalArgumentException("Eidolon Unchained: unknown caster profile '" + profileId + "'");
        CasterProfileResolver.setEntityProfile(mob, rl);
        MobCasting.refresh(mob);
        return this;
    }

    @Info("Remove the profile reference (equipment grants still count) and re-resolve")
    public CasterHelper clearProfile() {
        CasterProfileResolver.setEntityProfile(mob, null);
        MobCasting.refresh(mob);
        return this;
    }

    @Info("Add a chant to this mob's own list (needs a mob path) and re-resolve")
    public CasterHelper grantChant(String spellId) {
        CasterProfileResolver.addEntitySpell(mob, Ids.of(spellId, "spell"));
        MobCasting.refresh(mob);
        return this;
    }

    @Info("Re-resolve from NBT and equipment now")
    public CasterHelper refresh() {
        MobCasting.refresh(mob);
        return this;
    }
}
