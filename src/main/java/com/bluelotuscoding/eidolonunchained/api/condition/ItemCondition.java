package com.bluelotuscoding.eidolonunchained.api.condition;

import com.bluelotuscoding.eidolonunchained.api.Ids;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Predicate;

/** {@code C.item('minecraft:diamond_sword' | '#tag')} and its narrowing calls; tested against the event's item. */
public final class ItemCondition extends Condition {
    final Predicate<ItemStack> stackTest;

    ItemCondition(String description, Predicate<ItemStack> base) {
        super(description);
        this.stackTest = base;
        parts.add(ctx -> base.test(ctx.getItem()));
    }

    /** Tests a stack directly (for player().holding / wearing). */
    public boolean testStack(ItemStack stack) {
        var ctx = new EventContext("test").item(stack);
        return matches(ctx);
    }

    @Info("Hover name equals")
    public ItemCondition named(String name) {
        return narrow("named '" + name + "'", ctx -> ctx.getItem().hasCustomHoverName() && name.equals(ctx.getItem().getHoverName().getString()));
    }

    @Info("Its NBT contains this SNBT")
    public ItemCondition nbt(String snbt) {
        CompoundTag expected;
        try {
            expected = TagParser.parseTag(snbt);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Eidolon Unchained: bad SNBT '" + snbt + "': " + ex.getMessage());
        }
        return narrow("nbt " + snbt, ctx -> ctx.getItem().hasTag() && NbtUtils.compareNbt(expected, ctx.getItem().getTag(), true));
    }

    @Info("Has this enchantment at this level or higher")
    public ItemCondition enchanted(String enchantmentId, int minLevel) {
        var rl = Ids.of(enchantmentId, "enchantment");
        return narrow("enchanted " + rl + " " + minLevel, ctx -> {
            var ench = ForgeRegistries.ENCHANTMENTS.getValue(rl);
            return ench != null && EnchantmentHelper.getItemEnchantmentLevel(ench, ctx.getItem()) >= minLevel;
        });
    }

    public ItemCondition enchanted(String enchantmentId) {
        return enchanted(enchantmentId, 1);
    }

    @Info("At least this many in the stack")
    public ItemCondition count(int min) {
        return narrow("count >= " + min, ctx -> ctx.getItem().getCount() >= min);
    }
}
