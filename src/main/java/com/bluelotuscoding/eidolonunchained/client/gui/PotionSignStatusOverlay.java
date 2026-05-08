package com.bluelotuscoding.eidolonunchained.client.gui;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.mojang.blaze3d.systems.RenderSystem;
import elucent.eidolon.api.spells.Sign;
import elucent.eidolon.registries.Signs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Renders sign icons in the same screen region as vanilla potion effects.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class PotionSignStatusOverlay implements IGuiOverlay {

    private static final class SignBinding {
        private final ResourceLocation signId;
        private long expiresAtMs;
        private final boolean positive;

        private SignBinding(ResourceLocation signId, long expiresAtMs, boolean positive) {
            this.signId = signId;
            this.expiresAtMs = expiresAtMs;
            this.positive = positive;
        }
    }

    private static final Map<ResourceLocation, SignBinding> EFFECT_SIGN_BINDINGS = new HashMap<>();

    public static void bindSignToEffect(ResourceLocation effectId, ResourceLocation signId, int durationTicks, boolean positive) {
        if (durationTicks <= 0) {
            EFFECT_SIGN_BINDINGS.remove(effectId);
            return;
        }

        long expiresAt = System.currentTimeMillis() + Math.max(20, durationTicks) * 50L;
        EFFECT_SIGN_BINDINGS.put(effectId, new SignBinding(signId, expiresAt, positive));
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || EFFECT_SIGN_BINDINGS.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        Iterator<Map.Entry<ResourceLocation, SignBinding>> iter = EFFECT_SIGN_BINDINGS.entrySet().iterator();
        while (iter.hasNext()) {
            if (iter.next().getValue().expiresAtMs <= now) {
                iter.remove();
            }
        }

        if (EFFECT_SIGN_BINDINGS.isEmpty()) {
            return;
        }

        // Match the vanilla potion effect HUD slot: 20x20 box at screenWidth-25, icon 16x16 centred inside (2px pad)
        final int boxSize = 20;
        final int iconSize = 16;
        final int pad = (boxSize - iconSize) / 2; // 2
        int boxX = screenWidth - 25;
        int y = 1;

        for (SignBinding binding : EFFECT_SIGN_BINDINGS.values()) {

            Sign sign = Signs.find(binding.signId);
            if (sign == null) {
                continue;
            }

            // Dark backing matching vanilla effect slot
            guiGraphics.fill(boxX, y, boxX + boxSize, y + boxSize, 0xAA111111);

            RenderSystem.enableBlend();
            RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);

            if (binding.positive) {
                // Tint with the sign's own colour
                int c = sign.getColor();
                float r = ((c >> 16) & 0xFF) / 255.0f;
                float g = ((c >>  8) & 0xFF) / 255.0f;
                float b = ( c        & 0xFF) / 255.0f;
                RenderSystem.setShaderColor(r, g, b, 1.0f);
            } else {
                // Greyed-out for debuffs
                RenderSystem.setShaderColor(0.5f, 0.5f, 0.5f, 1.0f);
            }

            guiGraphics.blit(
                boxX + pad,
                y + pad,
                0,
                iconSize,
                iconSize,
                mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(sign.getSprite())
            );

            // Always reset shader colour after each icon
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();

            y += 24;
        }
    }
}
