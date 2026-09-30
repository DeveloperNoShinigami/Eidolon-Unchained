package com.bluelotuscoding.eidolonunchained.kubejs;

import dev.latvian.mods.kubejs.entity.LivingEntityEventJS;
import dev.latvian.mods.kubejs.typings.Info;
import elucent.eidolon.api.spells.Sign;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** {@code EidolonUnchainedEvents.mobChantStarted / mobChantSign / mobChantCast / mobChantInterrupted}. */
public class MobChantEventJS extends LivingEntityEventJS {
    private final Mob mob;
    private final String spell;
    private final @Nullable LivingEntity target;
    private final @Nullable Sign sign;
    private final List<Sign> signs;
    private final String reason;

    public MobChantEventJS(Mob mob, String spell, @Nullable LivingEntity target, @Nullable Sign sign, List<Sign> signs, String reason) {
        this.mob = mob;
        this.spell = spell;
        this.target = target;
        this.sign = sign;
        this.signs = signs;
        this.reason = reason;
    }

    @Override
    public Mob getEntity() {
        return mob;
    }

    @Info("Chant id")
    public String getChant() {
        return spell;
    }

    public @Nullable LivingEntity getTarget() {
        return target;
    }

    @Info("The sign just chanted (mobChantSign only)")
    public @Nullable String getSign() {
        return sign == null ? null : sign.getRegistryName().toString();
    }

    public List<String> getSigns() {
        return signs.stream().map(s -> s.getRegistryName().toString()).toList();
    }

    @Info("mobChantInterrupted: 'hit' | 'target_lost' | 'no_mana' | 'refused' | 'cancelled'")
    public String getReason() {
        return reason;
    }
}
