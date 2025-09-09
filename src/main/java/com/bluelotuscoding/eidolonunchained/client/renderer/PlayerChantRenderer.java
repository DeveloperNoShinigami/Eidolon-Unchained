package com.bluelotuscoding.eidolonunchained.client.renderer;

import com.bluelotuscoding.eidolonunchained.chant.PlayerChantingSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import elucent.eidolon.Eidolon;
import elucent.eidolon.api.spells.Sign;
import elucent.eidolon.event.ClientEvents;
import elucent.eidolon.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
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
import org.joml.Vector3f;

import java.util.List;

/**
 * Client-side renderer that creates floating sign visuals around the player
 * during active chanting, emulating Eidolon's ChantCasterEntity renderer.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "eidolonunchained", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class PlayerChantRenderer {
    
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        
        // Get current chant signs for this player
        List<Sign> signs = PlayerChantingSystem.getPlayerChantSigns(player.getUUID());
        if (signs.isEmpty()) return;
        
        // Render floating signs around player (like ChantCasterEntity)
        renderFloatingSignsAroundPlayer(event.getPoseStack(), 
            player, signs, event.getPartialTick());
    }
    
    /**
     * Render floating signs in a circle around the player, emulating ChantCasterRenderer
     */
    private static void renderFloatingSignsAroundPlayer(PoseStack poseStack, 
                                                       Player player, List<Sign> signs, float partialTick) {
        
        Minecraft mc = Minecraft.getInstance();
        VertexConsumer buffer = ClientEvents.getDelayedRender().getBuffer(RenderUtil.GLOWING_BLOCK_PARTICLE);
        
        poseStack.pushPose();
        
        // Calculate player position with interpolation
        double px = Mth.lerp(partialTick, player.xOld, player.getX());
        double py = Mth.lerp(partialTick, player.yOld, player.getY());
        double pz = Mth.lerp(partialTick, player.zOld, player.getZ());
        
        // Camera position for relative rendering
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        
        // Circle configuration (like ChantCasterEntity)
        int signCount = signs.size();
        float radius = Math.max(0.3f, Mth.sqrt(signCount) / 4f);
        
        // Player's look direction (use camera look for consistency)
        Vec3 lookDirection = player.getLookAngle();
        double yaw = Mth.atan2(lookDirection.x, lookDirection.z);
        Vec3 left = new Vec3(Math.cos(yaw), 0, -Math.sin(yaw));
        Vec3 up = left.cross(lookDirection);
        
        // Center point for the sign circle (above player's head)
        Vec3 center = new Vec3(px, py + 2.2, pz).add(lookDirection.scale(1.5));
        
        // Render each sign in the circle
        for (int i = 0; i < signCount; i++) {
            Sign sign = signs.get(i);
            
            // Calculate position in circle
            float angle = -Mth.PI / 2 - i * 2 * Mth.PI / signCount;
            float sinA = Mth.sin(angle);
            float cosA = Mth.cos(angle);
            
            Vec3 signPos = center.add(left.scale(radius * cosA)).add(up.scale(radius * sinA));
            Vec3 dx = left.scale(0.175);
            Vec3 dy = up.scale(0.175);
            
            Vector3f pos = new Vector3f((float)signPos.x, (float)signPos.y, (float)signPos.z);
            Vector3f dxVec = new Vector3f((float)dx.x, (float)dx.y, (float)dx.z);
            Vector3f dyVec = new Vector3f((float)dy.x, (float)dy.y, (float)dy.z);
            
            // Get sign texture
            TextureAtlasSprite sprite = mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(sign.getSprite());
            
            // Brightness modulation (pulsing effect)
            float brightMod = Mth.clamp(Mth.sin(angle + Mth.TWO_PI * mc.level.getGameTime() / 20), 0, 1);
            brightMod *= brightMod;
            brightMod = 0.6f + 0.4f * brightMod;
            
            // Render sign quad (front and back faces)
            renderSignQuad(buffer, poseStack, pos, dxVec, dyVec, sprite, sign, brightMod);
        }
        
        poseStack.popPose();
    }
    
    /**
     * Render a single sign quad with proper UV mapping and colors
     */
    private static void renderSignQuad(VertexConsumer buffer, PoseStack poseStack, 
                                     Vector3f pos, Vector3f dx, Vector3f dy, 
                                     TextureAtlasSprite sprite, Sign sign, float brightness) {
        
        // Front face
        buffer.vertex(poseStack.last().pose(), pos.x() - dx.x() + dy.x(), pos.y() - dx.y() + dy.y(), pos.z() - dx.z() + dy.z())
            .uv(sprite.getU1(), sprite.getV1())
            .color(sign.getRed(), sign.getGreen(), sign.getBlue(), brightness)
            .uv2(0).endVertex();
            
        buffer.vertex(poseStack.last().pose(), pos.x() - dx.x() - dy.x(), pos.y() - dx.y() - dy.y(), pos.z() - dx.z() - dy.z())
            .uv(sprite.getU1(), sprite.getV0())
            .color(sign.getRed(), sign.getGreen(), sign.getBlue(), brightness)
            .uv2(0).endVertex();
            
        buffer.vertex(poseStack.last().pose(), pos.x() + dx.x() - dy.x(), pos.y() + dx.y() - dy.y(), pos.z() + dx.z() - dy.z())
            .uv(sprite.getU0(), sprite.getV0())
            .color(sign.getRed(), sign.getGreen(), sign.getBlue(), brightness)
            .uv2(0).endVertex();
            
        buffer.vertex(poseStack.last().pose(), pos.x() + dx.x() + dy.x(), pos.y() + dx.y() + dy.y(), pos.z() + dx.z() + dy.z())
            .uv(sprite.getU0(), sprite.getV1())
            .color(sign.getRed(), sign.getGreen(), sign.getBlue(), brightness)
            .uv2(0).endVertex();
        
        // Back face (for visibility from all angles)
        buffer.vertex(poseStack.last().pose(), pos.x() + dx.x() + dy.x(), pos.y() + dx.y() + dy.y(), pos.z() + dx.z() + dy.z())
            .uv(sprite.getU1(), sprite.getV1())
            .color(sign.getRed(), sign.getGreen(), sign.getBlue(), brightness)
            .uv2(0).endVertex();
            
        buffer.vertex(poseStack.last().pose(), pos.x() + dx.x() - dy.x(), pos.y() + dx.y() - dy.y(), pos.z() + dx.z() - dy.z())
            .uv(sprite.getU1(), sprite.getV0())
            .color(sign.getRed(), sign.getGreen(), sign.getBlue(), brightness)
            .uv2(0).endVertex();
            
        buffer.vertex(poseStack.last().pose(), pos.x() - dx.x() - dy.x(), pos.y() - dx.y() - dy.y(), pos.z() - dx.z() - dy.z())
            .uv(sprite.getU0(), sprite.getV0())
            .color(sign.getRed(), sign.getGreen(), sign.getBlue(), brightness)
            .uv2(0).endVertex();
            
        buffer.vertex(poseStack.last().pose(), pos.x() - dx.x() + dy.x(), pos.y() - dx.y() + dy.y(), pos.z() - dx.z() + dy.z())
            .uv(sprite.getU0(), sprite.getV1())
            .color(sign.getRed(), sign.getGreen(), sign.getBlue(), brightness)
            .uv2(0).endVertex();
    }
}
