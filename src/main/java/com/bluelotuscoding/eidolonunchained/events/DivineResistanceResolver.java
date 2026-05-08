package com.bluelotuscoding.eidolonunchained.events;

import com.bluelotuscoding.eidolonunchained.data.DivineResistanceTypeManager;
import com.bluelotuscoding.eidolonunchained.registries.EidolonUnchainedAttributes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Shared divine damage resistance resolver used by chant combat handlers.
 */
public final class DivineResistanceResolver {
    private static final double BASE_MULTIPLIER = 1.0d;
    private static final double MIN_PERCENT_VALUE = -10.0d;
    private static final double MAX_PERCENT_VALUE = 10.0d;
    private static final String TAG_ITEM_GLOBAL_RESISTANCE = "eu_divine_resistance";
    private static final String TAG_DIVINE_RESISTANCES = "eu_divine_resistances";

    private DivineResistanceResolver() {
    }

    /**
     * Applies divine resistance to a base damage value.
     *
     * Formula:
     * finalDamage = baseDamage * max(0, 1 - clamp((globalMultiplier - 1.0) + typedResistance, -10.0, 10.0))
     * where globalMultiplier is the divine_resistance attribute (base 1.0)
     * and typedResistance comes from entity tag + worn gear tags for the mapped channel.
     */
    public static float applyDivineResistance(LivingEntity target, float baseDamage, String configuredDamageType) {
        if (baseDamage <= 0.0f) {
            return baseDamage;
        }

        ResourceLocation damageTypeId = ResourceLocation.tryParse(configuredDamageType == null ? "" : configuredDamageType);
        if (damageTypeId == null) {
            return baseDamage;
        }

        // Keep vanilla magic / indirect_magic behavior unchanged unless explicitly mapped.
        String normalized = configuredDamageType.toLowerCase(java.util.Locale.ROOT);
        if ("magic".equals(normalized) || "indirect_magic".equals(normalized) || "indirectmagic".equals(normalized)) {
            return baseDamage;
        }

        double globalMultiplier = BASE_MULTIPLIER;
        var globalAttr = target.getAttribute(EidolonUnchainedAttributes.DIVINE_RESISTANCE.get());
        if (globalAttr != null) {
            globalMultiplier = globalAttr.getValue();
        }

        // Gear can push multiplier up/down directly (e.g. armor grants +0.10 resistance).
        globalMultiplier += getGlobalGearResistanceBonus(target);

        double typedResistance = DivineResistanceTypeManager.getInfo(damageTypeId)
            .map(info -> getTypedResistance(target, info.resistanceKey(), info.defaultResistance())
                + getTypedGearResistanceBonus(target, info.resistanceKey()))
            .orElse(0.0d);

        double normalizedGlobalResistance = globalMultiplier - BASE_MULTIPLIER;
        double totalResistance = clamp(normalizedGlobalResistance + typedResistance, MIN_PERCENT_VALUE, MAX_PERCENT_VALUE);
        double multiplier = Math.max(0.0d, BASE_MULTIPLIER - totalResistance);
        return (float) (baseDamage * multiplier);
    }

    private static double getTypedResistance(LivingEntity target, String resistanceKey, double defaultResistance) {
        CompoundTag persisted = target.getPersistentData();
        if (!persisted.contains(TAG_DIVINE_RESISTANCES, CompoundTag.TAG_COMPOUND)) {
            return defaultResistance;
        }

        CompoundTag typed = persisted.getCompound(TAG_DIVINE_RESISTANCES);
        if (!typed.contains(resistanceKey, CompoundTag.TAG_ANY_NUMERIC)) {
            return defaultResistance;
        }

        return typed.getDouble(resistanceKey);
    }

    private static double getGlobalGearResistanceBonus(LivingEntity target) {
        double bonus = 0.0d;
        for (ItemStack stack : target.getArmorSlots()) {
            bonus += readGlobalGearResistance(stack);
        }
        bonus += readGlobalGearResistance(target.getItemBySlot(EquipmentSlot.MAINHAND));
        bonus += readGlobalGearResistance(target.getItemBySlot(EquipmentSlot.OFFHAND));
        return bonus;
    }

    private static double getTypedGearResistanceBonus(LivingEntity target, String resistanceKey) {
        double bonus = 0.0d;
        for (ItemStack stack : target.getArmorSlots()) {
            bonus += readTypedGearResistance(stack, resistanceKey);
        }
        bonus += readTypedGearResistance(target.getItemBySlot(EquipmentSlot.MAINHAND), resistanceKey);
        bonus += readTypedGearResistance(target.getItemBySlot(EquipmentSlot.OFFHAND), resistanceKey);
        return bonus;
    }

    private static double readGlobalGearResistance(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0.0d;
        }

        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_ITEM_GLOBAL_RESISTANCE, CompoundTag.TAG_ANY_NUMERIC)) {
            return 0.0d;
        }

        return tag.getDouble(TAG_ITEM_GLOBAL_RESISTANCE);
    }

    private static double readTypedGearResistance(ItemStack stack, String resistanceKey) {
        if (stack == null || stack.isEmpty()) {
            return 0.0d;
        }

        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_DIVINE_RESISTANCES, CompoundTag.TAG_COMPOUND)) {
            return 0.0d;
        }

        CompoundTag typed = tag.getCompound(TAG_DIVINE_RESISTANCES);
        if (!typed.contains(resistanceKey, CompoundTag.TAG_ANY_NUMERIC)) {
            return 0.0d;
        }

        return typed.getDouble(resistanceKey);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
