package com.bluelotuscoding.eidolonunchained.kubejs;

import dev.latvian.mods.kubejs.entity.LivingEntityEventJS;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** {@code EidolonUnchainedEvents.imbueCast} (player, weapon, chant) and {@code protectionTriggered} (wearer, attacker, piece, chant, level). */
public class WeaponChantEventJS extends LivingEntityEventJS {
    private final LivingEntity entity;
    private final @Nullable LivingEntity attacker;
    private final ItemStack item;
    private final String chant;
    private final int level;

    public WeaponChantEventJS(LivingEntity entity, @Nullable LivingEntity attacker, ItemStack item, String chant, int level) {
        this.entity = entity;
        this.attacker = attacker;
        this.item = item;
        this.chant = chant;
        this.level = level;
    }

    @Override
    public LivingEntity getEntity() {
        return entity;
    }

    @Info("protectionTriggered: what hurt the wearer")
    public @Nullable LivingEntity getAttacker() {
        return attacker;
    }

    @Info("The weapon (imbueCast) or the enchanted piece (protectionTriggered)")
    public ItemStack getItem() {
        return item;
    }

    public String getChant() {
        return chant;
    }

    @Info("Deity's Protection enchantment level (protectionTriggered only)")
    public int getEnchantmentLevel() {
        return level;
    }
}
