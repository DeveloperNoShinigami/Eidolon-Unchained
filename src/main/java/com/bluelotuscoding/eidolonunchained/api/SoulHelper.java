package com.bluelotuscoding.eidolonunchained.api;

import dev.latvian.mods.kubejs.typings.Info;
import elucent.eidolon.capability.ISoul;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * {@code EidolonUnchained.soul(entity)} / {@code player.eidolon.soul()}: Eidolon's {@link ISoul} capability with the
 * script names the user chose (decision D30): <em>mana</em> in scripts, "magic" in Eidolon's Java. Ethereal (soul)
 * hearts keep Eidolon's names.
 */
public final class SoulHelper {
    private final LivingEntity entity;

    SoulHelper(LivingEntity entity) {
        this.entity = entity;
    }

    public static SoulHelper of(LivingEntity entity) {
        if (entity == null) throw new IllegalArgumentException("Eidolon Unchained: soul(entity) needs a living entity");
        return new SoulHelper(entity);
    }

    private ISoul soul() {
        return entity.getCapability(ISoul.INSTANCE).resolve()
                .orElseThrow(() -> new IllegalStateException("Eidolon Unchained: " + entity.getName().getString() + " has no Eidolon soul"));
    }

    /** True when Eidolon attached a soul to this entity (players always; other entities per Eidolon's rules). */
    public boolean isPresent() {
        return entity.getCapability(ISoul.INSTANCE).isPresent();
    }

    @Info("Current mana (Eidolon: getMagic)")
    public float getMana() {
        return soul().getMagic();
    }

    @Info("Maximum mana (Eidolon: getMaxMagic)")
    public float getMaxMana() {
        return soul().getMaxMagic();
    }

    public boolean getHasMana() {
        return soul().hasMagic();
    }

    public void setMana(float mana) {
        soul().setMagic(mana);
    }

    public void setMaxMana(float max) {
        soul().setMaxMagic(max);
    }

    @Info("Remove mana, never below zero handling is Eidolon's (takeMagic)")
    public void takeMana(float amount) {
        soul().takeMagic(amount);
    }

    public void giveMana(float amount) {
        soul().giveMagic(amount);
    }

    @Info("Players only: spend mana as a spell would (ISoul.expendMana); creative players pay nothing")
    public void expendMana(int amount) {
        if (!(entity instanceof Player player)) throw new IllegalStateException("Eidolon Unchained: expendMana is for players; use takeMana for other entities");
        ISoul.expendMana(player, amount);
    }

    public float getEtherealHealth() {
        return soul().getEtherealHealth();
    }

    public float getMaxEtherealHealth() {
        return soul().getMaxEtherealHealth();
    }

    public void setEtherealHealth(float health) {
        soul().setEtherealHealth(health);
    }

    public void setMaxEtherealHealth(float max) {
        soul().setMaxEtherealHealth(max);
    }
}
