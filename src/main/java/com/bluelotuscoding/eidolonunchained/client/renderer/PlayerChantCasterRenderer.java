package com.bluelotuscoding.eidolonunchained.client.renderer;

import com.bluelotuscoding.eidolonunchained.chant.PlayerChantingSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import elucent.eidolon.api.spells.Sign;
import elucent.eidolon.client.renderer.ChantCasterRenderer;
import elucent.eidolon.common.entity.ChantCasterEntity;
import elucent.eidolon.event.ClientEvents;
import elucent.eidolon.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.List;

/**
 * Player-centered chant renderer that extends Eidolon's ChantCasterRenderer
 * Uses the same rendering logic but works with PlayerChantingSystem instead of entities
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "eidolonunchained", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerChantCasterRenderer extends ChantCasterRenderer {
    
    public PlayerChantCasterRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
    
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        // Try AFTER_ENTITIES stage like many other mod renderers
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        
        // Get current chant signs for this player
        List<Sign> signs = PlayerChantingSystem.getPlayerChantSigns(player.getUUID());
        
        // DEBUG: Log when we're checking for signs
        if (mc.level.getGameTime() % 20 == 0) { // Every second
            System.out.println("DEBUG: PlayerChantCasterRenderer checking for signs. Found: " + signs.size());
        }
        
        if (signs.isEmpty()) return;
        
        System.out.println("DEBUG: Rendering " + signs.size() + " chant signs for player");
        
        // Render using Eidolon's ChantCasterRenderer logic
        renderPlayerChantSigns(event.getPoseStack(), event.getPartialTick(), player, signs);
    }
    
    /**
     * Render floating signs around player using ChantCasterRenderer's exact logic
     */
    private static void renderPlayerChantSigns(PoseStack mStack, float partialTick, Player player, List<Sign> signs) {
        // Use the same buffer as ChantCasterRenderer
        VertexConsumer sb = ClientEvents.getDelayedRender().getBuffer(RenderUtil.GLOWING_BLOCK_PARTICLE);
        
        mStack.pushPose();
        
        // Get camera position for proper world-to-screen rendering
        Minecraft mc = Minecraft.getInstance();
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
        
        // Get the ring texture like ChantCasterRenderer
        TextureAtlasSprite ring = mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
            .apply(new ResourceLocation("eidolon", "particle/ring"));
        
        // Player position with interpolation
        double px = Mth.lerp(partialTick, player.xOld, player.getX());
        double py = Mth.lerp(partialTick, player.yOld, player.getY()) + 0.5; // Eye level, not above
        double pz = Mth.lerp(partialTick, player.zOld, player.getZ());
        
        // Translate relative to camera (like entity rendering)
        mStack.translate(px - cameraPos.x, py - cameraPos.y, pz - cameraPos.z);
        
        // Player's look direction (like ChantCasterRenderer)
        Vec3 look = player.getLookAngle();
        double yaw = Mth.atan2(look.x, look.z);
        Vec3 left = new Vec3(Math.cos(yaw), 0, -Math.sin(yaw));
        Vec3 up = left.cross(look);
        
        float time = player.tickCount + partialTick;
        
        // Setup circle parameters (like ChantCasterRenderer)
        int sz = Math.max(0, signs.size() - 1);
        float r = Mth.sqrt(sz) / 4f;
        if (sz > 0) r = Math.max(0.3f, r);
        Vec3 center = look.add(0, 0.5f, 0);
        
        // Render each sign in a circle (exact ChantCasterRenderer logic)
        for (int i = 0; i < signs.size(); i++) {
            Sign sign = signs.get(i);
            
            // Position calculation like ChantCasterRenderer
            float a = -Mth.PI / 2 - i * 2 * Mth.PI / signs.size();
            float sa = Mth.sin(a), ca = Mth.cos(a);
            
            Vec3 od = center.add(left.scale(r * ca)).add(up.scale(r * sa));
            Vec3 dxd = left.scale(0.175), dyd = up.scale(0.175);
            Vector3f o = new Vector3f((float)od.x, (float)od.y, (float)od.z);
            Vector3f dx = new Vector3f((float)dxd.x, (float)dxd.y, (float)dxd.z);
            Vector3f dy = new Vector3f((float)dyd.x, (float)dyd.y, (float)dyd.z);
            
            // Get sign sprite (exactly like ChantCasterRenderer)
            TextureAtlasSprite spr = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(sign.getSprite());
            
            // Brightness calculation (like ChantCasterRenderer)
            float brightMod = Mth.clamp(Mth.sin(a + Mth.TWO_PI * player.tickCount / 20), 0, 1);
            brightMod *= brightMod;
            brightMod = 0.6f + 0.4f * brightMod;
            float alphaMod = 1.0f;
            
            // Render sign sprite using exact ChantCasterRenderer vertex calls
            renderSignSprite(mStack, sb, spr, ring, o, dx, dy, sign, brightMod, alphaMod);
        }
        
        mStack.popPose();
    }
    
    /**
     * Render sign sprite using exact ChantCasterRenderer logic
     */
    private static void renderSignSprite(PoseStack mStack, VertexConsumer sb, TextureAtlasSprite spr, 
                                        TextureAtlasSprite ring, Vector3f o, Vector3f dx, Vector3f dy, 
                                        Sign sign, float brightMod, float alphaMod) {
        // Front and back faces (exactly like ChantCasterRenderer)
        for (int j = 0; j < 2; j++) {
            sb.vertex(mStack.last().pose(), o.x() - dx.x() + dy.x(), o.y() - dx.y() + dy.y(), o.z() - dx.z() + dy.z())
                .uv(spr.getU1(), spr.getV1())
                .color(sign.getRed(), sign.getGreen(), sign.getBlue(), brightMod * alphaMod)
                .uv2(0).endVertex();
            sb.vertex(mStack.last().pose(), o.x() - dx.x() - dy.x(), o.y() - dx.y() - dy.y(), o.z() - dx.z() - dy.z())
                .uv(spr.getU1(), spr.getV0())
                .color(sign.getRed(), sign.getGreen(), sign.getBlue(), brightMod * alphaMod)
                .uv2(0).endVertex();
            sb.vertex(mStack.last().pose(), o.x() + dx.x() - dy.x(), o.y() + dx.y() - dy.y(), o.z() + dx.z() - dy.z())
                .uv(spr.getU0(), spr.getV0())
                .color(sign.getRed(), sign.getGreen(), sign.getBlue(), brightMod * alphaMod)
                .uv2(0).endVertex();
            sb.vertex(mStack.last().pose(), o.x() + dx.x() + dy.x(), o.y() + dx.y() + dy.y(), o.z() + dx.z() + dy.z())
                .uv(spr.getU0(), spr.getV1())
                .color(sign.getRed(), sign.getGreen(), sign.getBlue(), brightMod * alphaMod)
                .uv2(0).endVertex();

            // Second face (reverse winding)
            sb.vertex(mStack.last().pose(), o.x() + dx.x() + dy.x(), o.y() + dx.y() + dy.y(), o.z() + dx.z() + dy.z())
                .uv(spr.getU1(), spr.getV1())
                .color(sign.getRed(), sign.getGreen(), sign.getBlue(), brightMod * alphaMod)
                .uv2(0).endVertex();
            sb.vertex(mStack.last().pose(), o.x() + dx.x() - dy.x(), o.y() + dx.y() - dy.y(), o.z() + dx.z() - dy.z())
                .uv(spr.getU1(), spr.getV0())
                .color(sign.getRed(), sign.getGreen(), sign.getBlue(), brightMod * alphaMod)
                .uv2(0).endVertex();
            sb.vertex(mStack.last().pose(), o.x() - dx.x() - dy.x(), o.y() - dx.y() - dy.y(), o.z() - dx.z() - dy.z())
                .uv(spr.getU0(), spr.getV0())
                .color(sign.getRed(), sign.getGreen(), sign.getBlue(), brightMod * alphaMod)
                .uv2(0).endVertex();
            sb.vertex(mStack.last().pose(), o.x() - dx.x() + dy.x(), o.y() - dx.y() + dy.y(), o.z() - dx.z() + dy.z())
                .uv(spr.getU0(), spr.getV1())
                .color(sign.getRed(), sign.getGreen(), sign.getBlue(), brightMod * alphaMod)
                .uv2(0).endVertex();
        }
        
        // RING RENDERING (the missing visual element!)
        // Create copies of dx/dy for ring rendering so we don't modify originals
        Vector3f ringDx = new Vector3f(dx).mul(1.75f);
        Vector3f ringDy = new Vector3f(dy).mul(1.75f);
        
        // Front ring face
        sb.vertex(mStack.last().pose(), o.x() - ringDx.x() + ringDy.x(), o.y() - ringDx.y() + ringDy.y(), o.z() - ringDx.z() + ringDy.z())
            .uv(ring.getU1(), ring.getV1())
            .color(sign.getRed(), sign.getGreen(), sign.getBlue(), alphaMod * 0.5f)
            .uv2(0).endVertex();
        sb.vertex(mStack.last().pose(), o.x() - ringDx.x() - ringDy.x(), o.y() - ringDx.y() - ringDy.y(), o.z() - ringDx.z() - ringDy.z())
            .uv(ring.getU1(), ring.getV0())
            .color(sign.getRed(), sign.getGreen(), sign.getBlue(), alphaMod * 0.5f)
            .uv2(0).endVertex();
        sb.vertex(mStack.last().pose(), o.x() + ringDx.x() - ringDy.x(), o.y() + ringDx.y() - ringDy.y(), o.z() + ringDx.z() - ringDy.z())
            .uv(ring.getU0(), ring.getV0())
            .color(sign.getRed(), sign.getGreen(), sign.getBlue(), alphaMod * 0.5f)
            .uv2(0).endVertex();
        sb.vertex(mStack.last().pose(), o.x() + ringDx.x() + ringDy.x(), o.y() + ringDx.y() + ringDy.y(), o.z() + ringDx.z() + ringDy.z())
            .uv(ring.getU0(), ring.getV1())
            .color(sign.getRed(), sign.getGreen(), sign.getBlue(), alphaMod * 0.5f)
            .uv2(0).endVertex();

        // Back ring face
        sb.vertex(mStack.last().pose(), o.x() + ringDx.x() + ringDy.x(), o.y() + ringDx.y() + ringDy.y(), o.z() + ringDx.z() + ringDy.z())
            .uv(ring.getU1(), ring.getV1())
            .color(sign.getRed(), sign.getGreen(), sign.getBlue(), alphaMod * 0.5f)
            .uv2(0).endVertex();
        sb.vertex(mStack.last().pose(), o.x() + ringDx.x() - ringDy.x(), o.y() + ringDx.y() - ringDy.y(), o.z() + ringDx.z() - ringDy.z())
            .uv(ring.getU1(), ring.getV0())
            .color(sign.getRed(), sign.getGreen(), sign.getBlue(), alphaMod * 0.5f)
            .uv2(0).endVertex();
        sb.vertex(mStack.last().pose(), o.x() - ringDx.x() - ringDy.x(), o.y() - ringDx.y() - ringDy.y(), o.z() - ringDx.z() - ringDy.z())
            .uv(ring.getU0(), ring.getV0())
            .color(sign.getRed(), sign.getGreen(), sign.getBlue(), alphaMod * 0.5f)
            .uv2(0).endVertex();
        sb.vertex(mStack.last().pose(), o.x() - ringDx.x() + ringDy.x(), o.y() - ringDx.y() + ringDy.y(), o.z() - ringDx.z() + ringDy.z())
            .uv(ring.getU0(), ring.getV1())
            .color(sign.getRed(), sign.getGreen(), sign.getBlue(), alphaMod * 0.5f)
            .uv2(0).endVertex();
    }
}
