package com.bluelotuscoding.eidolonunchained.api;

import com.bluelotuscoding.eidolonunchained.damage.DivineDamages;
import dev.latvian.mods.kubejs.typings.Info;

/**
 * {@code EidolonUnchained.divineDamage('eu_examples:necrotic').deity('eu_examples:myrkul')} (startup scripts): a named
 * divine damage owned by a god. EU generates its damage type and the attributes {@code <id>_damage} and
 * {@code <id>_resistance}; dealing it is plain KubeJS: {@code target.hurt(EidolonUnchained.damageSource(id, caster), n)}.
 */
public final class DivineDamageBuilder {
    private final DivineDamages.Declared declared;

    DivineDamageBuilder(DivineDamages.Declared declared) {
        this.declared = declared;
    }

    @Info("The god this damage belongs to (required): its followers deal it at full power, scaled by its devotion curve")
    public DivineDamageBuilder deity(String deityId) {
        declared.owner = Ids.of(deityId, "deity");
        return this;
    }

    @Info("Display name for the generated death messages and attribute names (default: the id's path in title case, e.g. 'Necrotic')")
    public DivineDamageBuilder name(String name) {
        declared.name = name;
        return this;
    }
}
