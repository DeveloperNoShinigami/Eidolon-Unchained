package com.bluelotuscoding.eidolonunchained.client;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.api.ChantSync;
import com.bluelotuscoding.eidolonunchained.imbue.ImbueNbt;
import com.bluelotuscoding.eidolonunchained.network.EUNetwork;
import com.bluelotuscoding.eidolonunchained.network.ImbueInputPacket;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Either;
import elucent.eidolon.api.spells.Sign;
import elucent.eidolon.client.ClientRegistry;
import elucent.eidolon.registries.Spells;
import elucent.eidolon.util.ClientInfo;
import elucent.eidolon.util.RenderUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Client side of imbued weapons (D35): right-click on an imbued weapon casts its active chant (the item's own
 * right-click use moves to sneak + right-click), the "next chant" key cycles the chant, the "awaken" key awakens or
 * puts to sleep a held hexblade, and the tooltip says "Hold [key] to view imbued chants" until the key is held, when it
 * lists the chants with their signs. Every key is rebindable in Controls.
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MOD_ID, value = Dist.CLIENT)
public final class ImbueClient {
    public static final KeyMapping VIEW = new KeyMapping("key." + EidolonUnchained.MOD_ID + ".imbued", KeyConflictContext.UNIVERSAL,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, ChantClient.CATEGORY);
    public static final KeyMapping NEXT_CHANT = new KeyMapping("key." + EidolonUnchained.MOD_ID + ".next_chant", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, ChantClient.CATEGORY);
    public static final KeyMapping AWAKEN = new KeyMapping("key." + EidolonUnchained.MOD_ID + ".awaken", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, ChantClient.CATEGORY);

    private ImbueClient() {
    }

    public static boolean viewHeld() {
        var key = VIEW.getKey();
        if (key.getType() != InputConstants.Type.KEYSYM || key.getValue() == InputConstants.UNKNOWN.getValue()) return false;
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), key.getValue());
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide()) return;
        var stack = event.getItemStack();
        var chants = ImbueNbt.chants(stack);
        if (chants.isEmpty()) return;
        if (event.getEntity().isShiftKeyDown()) return;                               // sneak + right-click: the weapon's own use
        EUNetwork.sendToServer(new ImbueInputPacket(ImbueInputPacket.Action.CAST));   // right-click: cast
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    @SubscribeEvent
    public static void onClientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        var player = Minecraft.getInstance().player;
        while (NEXT_CHANT.consumeClick()) {
            if (player != null && ImbueNbt.chants(player.getMainHandItem()).size() > 1)
                EUNetwork.sendToServer(new ImbueInputPacket(ImbueInputPacket.Action.CYCLE));
        }
        while (AWAKEN.consumeClick()) {
            if (player != null && player.getMainHandItem().getItem() instanceof com.bluelotuscoding.eidolonunchained.hexblade.HexbladeItem)
                EUNetwork.sendToServer(new ImbueInputPacket(ImbueInputPacket.Action.AWAKEN));
        }
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        var stack = event.getItemStack();
        if (stack.getItem() instanceof com.bluelotuscoding.eidolonunchained.hexblade.HexbladeItem hb) divineAttackLine(hb, stack, event.getToolTip());
        var chants = ImbueNbt.chants(stack);
        var lines = event.getToolTip();
        if (!chants.isEmpty()) {
            int active = ImbueNbt.active(stack);
            lines.add(Component.translatable("eidolonunchained.imbue.tooltip.imbued", chants.size()).withStyle(ChatFormatting.LIGHT_PURPLE));
            if (viewHeld()) {
                for (int i = 0; i < chants.size(); i++) {
                    var spell = Spells.find(chants.get(i));
                    var name = ImbueNbt.chantName(chants.get(i));
                    var line = Component.literal(i == active ? " ▶ " : "    ").withStyle(ChatFormatting.DARK_PURPLE)
                            .append(name.copy().withStyle(i == active ? ChatFormatting.WHITE : ChatFormatting.GRAY));
                    if (spell != null) line.append(Component.literal("  " + spell.getCost() + " mana").withStyle(ChatFormatting.DARK_AQUA));
                    lines.add(line);
                }
                lines.add(Component.translatable("eidolonunchained.imbue.tooltip.how", NEXT_CHANT.getTranslatedKeyMessage()).withStyle(ChatFormatting.DARK_GRAY));
            } else {
                lines.add(Component.translatable("eidolonunchained.imbue.tooltip.hold", VIEW.getTranslatedKeyMessage()).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        var protection = ImbueNbt.protectionChant(stack);
        if (protection != null && ImbueNbt.protectionLevel(stack) > 0) {
            lines.add(Component.translatable("eidolonunchained.protection.tooltip.bound", ImbueNbt.chantName(protection)).withStyle(ChatFormatting.AQUA));
        } else if (ImbueNbt.protectionLevel(stack) > 0) {
            lines.add(Component.translatable("eidolonunchained.protection.tooltip.unbound").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    /** An awakened divine hexblade's hit is wholly divine: its " N Attack Damage" line reads " N Necrotic Damage" in its god's colour. */
    private static void divineAttackLine(com.bluelotuscoding.eidolonunchained.hexblade.HexbladeItem hb, net.minecraft.world.item.ItemStack stack, List<Component> lines) {
        var d = com.bluelotuscoding.eidolonunchained.damage.DivineDamages.get(hb.awakenedDivineDamage(stack));
        if (d == null || d.damageAttribute() == null) return;
        int color = com.bluelotuscoding.eidolonunchained.patron.Patrons.deityColor(d.owner);
        for (int i = 0; i < lines.size(); i++) {
            var line = lines.get(i);                       // vanilla: literal " " + translatable("attribute.modifier.equals.0", value, name)
            var part = line.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents ? line
                    : line.getSiblings().size() == 1 ? line.getSiblings().get(0) : null;
            if (part == null || !(part.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t) || !t.getKey().startsWith("attribute.modifier.")) continue;
            var args = t.getArgs();
            if (args.length == 2 && args[1] instanceof Component name && name.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents n
                    && n.getKey().equals(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE.getDescriptionId())) {
                var renamed = Component.translatable(t.getKey(), args[0], Component.translatable(d.damageAttribute().getDescriptionId()));
                lines.set(i, (part == line ? renamed : Component.literal(" ").append(renamed)).withStyle(st -> st.withColor(color)));
            }
        }
    }

    /** The sign rows (one per chant) under the text while the view key is held. */
    @SubscribeEvent
    public static void onGatherTooltip(RenderTooltipEvent.GatherComponents event) {
        var chants = ImbueNbt.chants(event.getItemStack());
        if (chants.isEmpty() || !viewHeld()) return;
        var rows = new ArrayList<List<Sign>>();
        for (var id : chants) rows.add(ChantSync.sequenceOf(id));
        event.getTooltipElements().add(Either.right(new SignRowsInfo(rows, ImbueNbt.active(event.getItemStack()))));
    }

    public record SignRowsInfo(List<List<Sign>> rows, int active) implements TooltipComponent {
    }

    /** Eidolon's chant-scroll tooltip drawing, one row per chant; the active row full brightness. */
    public static final class SignRows implements ClientTooltipComponent {
        private final SignRowsInfo info;

        public SignRows(SignRowsInfo info) {
            this.info = info;
        }

        @Override
        public int getHeight() {
            int h = 0;
            for (var row : info.rows) h += row.isEmpty() ? 0 : 12;
            return h == 0 ? 0 : h + 2;
        }

        @Override
        public int getWidth(@NotNull Font font) {
            int w = 0;
            for (var row : info.rows) w = Math.max(w, 17 * row.size());
            return w;
        }

        @Override
        public void renderImage(@NotNull Font font, int x, int y, @NotNull GuiGraphics gfx) {
            var mStack = gfx.pose();
            MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.setShader(ClientRegistry::getGlowingSpriteShader);
            RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
            var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
            int rowY = y;
            for (int r = 0; r < info.rows.size(); r++) {
                var row = info.rows.get(r);
                if (row.isEmpty()) continue;
                float dim = r == info.active ? 1.0f : 0.45f;
                for (int i = 0; i < row.size(); i++) {
                    var sign = row.get(i);
                    if (sign == null) continue;
                    float flicker = dim * (0.75f + 0.05f * (float) Math.sin(Math.toRadians(12 * ClientInfo.getClientPartialTicks() - 360.0f * i / row.size())));
                    for (int j = 0; j < 2; j++) {
                        RenderUtil.litQuad(mStack, buffers, 2 + x + 17 * i, rowY, 10, 10,
                                sign.getRed() * flicker, sign.getGreen() * flicker, sign.getBlue() * flicker, atlas.apply(sign.getSprite()));
                        buffers.endBatch();
                    }
                }
                rowY += 12;
            }
            RenderSystem.disableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
        }
    }

    @Mod.EventBusSubscriber(modid = EidolonUnchained.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        private ModBus() {
        }

        @SubscribeEvent
        public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
            event.register(VIEW);
            event.register(NEXT_CHANT);
            event.register(AWAKEN);
        }

        @SubscribeEvent
        public static void onTooltipFactories(RegisterClientTooltipComponentFactoriesEvent event) {
            event.register(SignRowsInfo.class, SignRows::new);
        }
    }
}
