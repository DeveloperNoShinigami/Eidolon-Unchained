package com.bluelotuscoding.eidolonunchained.api;

import com.bluelotuscoding.eidolonunchained.hexblade.HexbladeItem;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * {@code EidolonUnchained.hexblade(stack)}: reads a scripted hexblade ({@link HexbladeItem}) or, when Hexblades Renewed
 * is loaded, one of its blades (through the NBT keys its {@code IHexblade} writes: {@code awakened},
 * {@code hexElementalDamage}, {@code hexSwordDamage}); Hexblades' blades are all bound to {@code hexblades:blade}.
 */
public final class HexbladeHelper {
    public static final ResourceLocation HEXBLADES_DEITY = new ResourceLocation("hexblades", "blade");
    private final ItemStack stack;

    HexbladeHelper(ItemStack stack) {
        this.stack = stack == null ? ItemStack.EMPTY : stack;
    }

    public static boolean hexbladesLoaded() {
        return ModList.get().isLoaded("hexblades");
    }

    private boolean isMod() {
        if (stack.isEmpty() || !hexbladesLoaded()) return false;
        var key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return key != null && key.getNamespace().equals("hexblades") && stack.hasTag() && stack.getTag().contains("hexSwordDamage");
    }

    @Info("A scripted hexblade or a Hexblades Renewed blade")
    public boolean isHexblade() {
        return stack.getItem() instanceof HexbladeItem || isMod();
    }

    public boolean isScripted() {
        return stack.getItem() instanceof HexbladeItem;
    }

    public boolean isAwakened() {
        if (stack.getItem() instanceof HexbladeItem) return HexbladeItem.isAwakened(stack);
        return isMod() && stack.getTag().getBoolean("awakened");
    }

    public double elementalPower() {
        if (stack.getItem() instanceof HexbladeItem) return HexbladeItem.elementalPower(stack);
        return isMod() ? stack.getTag().getFloat("hexElementalDamage") : 0;
    }

    public double awakenedDamage() {
        if (stack.getItem() instanceof HexbladeItem) return HexbladeItem.awakenedDamage(stack);
        return isMod() ? stack.getTag().getDouble("hexSwordDamage") : 0;
    }

    @Info("The bound deity id (Hexblades Renewed blades: 'hexblades:blade'), or null")
    public @Nullable String deity() {
        if (stack.getItem() instanceof HexbladeItem h) return h.deity() == null ? null : h.deity().toString();
        return isMod() ? HEXBLADES_DEITY.toString() : null;
    }

    @Info("Awaken or sleep a scripted hexblade (server); false when it is not one or not charged")
    public boolean setAwakened(Player player, boolean awakened) {
        if (!(stack.getItem() instanceof HexbladeItem h) || player.level().isClientSide()) return false;
        return h.setAwakened(stack, player, awakened);
    }

    @Info("Make the blade say one of its lines")
    public void talk(Player player) {
        if (stack.getItem() instanceof HexbladeItem h) h.talk(player);
    }
}
