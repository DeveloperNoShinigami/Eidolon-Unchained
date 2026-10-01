package com.bluelotuscoding.eidolonunchained.mixin;

import com.bluelotuscoding.eidolonunchained.imbue.ImbueNbt;
import elucent.eidolon.client.ClientRegistry;
import elucent.eidolon.registries.Registry;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * D51: Eidolon's mana bar shows only while a hand holds one of its mana items. It also shows while a held item (weapon,
 * shield) carries a chant or a worn armour piece does: the two {@code getItem()} calls of the overlay's hand check then
 * see Eidolon's chant scroll. Eidolon's drawing and its position presets are untouched. Eidolon's names are not
 * remapped; the Minecraft call is (refmap).
 */
@Mixin(value = ClientRegistry.EidolonManaBar.class, remap = false)
public abstract class EidolonManaBarMixin {
    @Redirect(method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getItem()Lnet/minecraft/world/item/Item;", remap = true))
    private Item eidolonunchained$chantItemsShowBar(ItemStack held) {
        var player = Minecraft.getInstance().player;
        if (carriesChant(held)) return Registry.CHANT_SCROLL.get();
        if (player != null) {
            for (var armor : player.getArmorSlots()) if (carriesChant(armor)) return Registry.CHANT_SCROLL.get();
        }
        return held.getItem();
    }

    private static boolean carriesChant(ItemStack stack) {
        return !stack.isEmpty() && (ImbueNbt.isImbued(stack)
                || (ImbueNbt.protectionLevel(stack) > 0 && ImbueNbt.protectionChant(stack) != null));
    }
}
