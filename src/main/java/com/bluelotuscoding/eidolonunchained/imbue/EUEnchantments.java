package com.bluelotuscoding.eidolonunchained.imbue;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Deity's Protection (D35): armour and shields, levels I–III, rare. Not treasure-only, so it comes out of the
 * enchanting table as well as loot chests ({@code enchant_randomly} picks any discoverable enchantment). A piece does
 * nothing until a deity-bound chant is attached at the worktable ({@link ImbueRecipe}); then it casts that chant's
 * targeted path at whatever hurt the wearer ({@link ImbueCasting}). Level shortens the cooldown and lowers the mana
 * share, both server config.
 */
public final class EUEnchantments {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, EidolonUnchained.MOD_ID);

    public static final EnchantmentCategory PROTECTION_CATEGORY = EnchantmentCategory.create("EU_DEITYS_PROTECTION",
            item -> item instanceof ArmorItem || item instanceof ShieldItem);

    public static final RegistryObject<Enchantment> DEITYS_PROTECTION = ENCHANTMENTS.register("deitys_protection", DeitysProtection::new);

    private EUEnchantments() {
    }

    public static void register(IEventBus modBus) {
        ENCHANTMENTS.register(modBus);
    }

    public static final class DeitysProtection extends Enchantment {
        DeitysProtection() {
            super(Rarity.RARE, PROTECTION_CATEGORY, new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND});
        }

        @Override
        public int getMaxLevel() {
            return 3;
        }

        @Override
        public int getMinCost(int level) {
            return 10 + (level - 1) * 12;
        }

        @Override
        public int getMaxCost(int level) {
            return getMinCost(level) + 40;
        }

        @Override
        public boolean isTreasureOnly() {
            return false;          // D35: chests and the enchanting table
        }

        @Override
        public boolean canEnchant(ItemStack stack) {
            return PROTECTION_CATEGORY.canEnchant(stack.getItem());
        }
    }
}
