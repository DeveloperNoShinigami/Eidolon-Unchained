package com.bluelotuscoding.eidolonunchained.client;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.imbue.ImbueNbt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.IItemDecorator;
import net.minecraftforge.client.event.RegisterItemDecorationsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * D51: a stack's chant cooldown ({@link ImbueNbt#cooldownFraction}) shows as the white sweep vanilla draws for item
 * cooldowns, but per stack. Registered on every item, since any weapon, armour piece or shield can carry a chant.
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ChantCooldownDecorator implements IItemDecorator {
    private static final ChantCooldownDecorator INSTANCE = new ChantCooldownDecorator();

    private ChantCooldownDecorator() {
    }

    @SubscribeEvent
    public static void onRegisterDecorations(RegisterItemDecorationsEvent event) {
        for (var item : ForgeRegistries.ITEMS) {
            if (item != Items.AIR) event.register(item, INSTANCE);
        }
    }

    @Override
    public boolean render(GuiGraphics graphics, Font font, ItemStack stack, int x, int y) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || !stack.hasTag()) return false;
        float f = ImbueNbt.cooldownFraction(stack, mc.level.getGameTime() + mc.getFrameTime());
        if (f <= 0) return false;
        // the same fill as the cooldown part of vanilla's GuiGraphics.renderItemDecorations
        int top = y + Mth.floor(16.0F * (1.0F - f));
        int bottom = top + Mth.ceil(16.0F * f);
        graphics.fill(RenderType.guiOverlay(), x, top, x + 16, bottom, Integer.MAX_VALUE);
        return false;
    }
}
