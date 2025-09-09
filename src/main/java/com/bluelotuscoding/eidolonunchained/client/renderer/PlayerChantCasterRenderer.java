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
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        
        // Get current chant signs for this player
        List<Sign> signs = PlayerChantingSystem.getPlayerChantSigns(player.getUUID());
        if (signs.isEmpty()) return;
        
        // Render using Eidolon's ChantCasterRenderer logic
        renderPlayerChantSigns(event.getPoseStack(), event.getPartialTick(), player, signs);
    }
    
    /**
     * Render floating signs around player using ChantCasterRenderer's exact logic
     */
    private static void renderPlayerChantSigns(PoseStack mStack, float partialTick, Player player, List<Sign> signs) {
        // This is copied from ChantCasterRenderer.render() but adapted for player positioning
        VertexConsumer sb = ClientEvents.getDelayedRender().getBuffer(RenderUtil.GLOWING_SPRITE);
        
        mStack.pushPose();
        
        // Player position with interpolation
        double px = Mth.lerp(partialTick, player.xOld, player.getX());
        double py = Mth.lerp(partialTick, player.yOld, player.getY()) + 1.5; // Above player
        double pz = Mth.lerp(partialTick, player.zOld, player.getZ());
        
        mStack.translate(px, py, pz);
        
        // Player's look direction
        Vec3 look = player.getLookAngle();
        double yaw = Mth.atan2(look.x, look.z);
        Vec3 left = new Vec3(Math.cos(yaw), 0, -Math.sin(yaw));
        Vec3 up = left.cross(look);
        
        float time = player.tickCount + partialTick;
        
        // Render each sign in a circle (exact ChantCasterRenderer logic)
        for (int i = 0; i < signs.size(); i++) {
            Sign sign = signs.get(i);
            
            mStack.pushPose();
            
            // Circular positioning
            float angle = (float)(i * 2 * Math.PI / Math.max(3, signs.size()));
            float radius = 1.5f;
            float x = Mth.cos(angle + time * 0.05f) * radius;
            float z = Mth.sin(angle + time * 0.05f) * radius;
            float y = Mth.sin(time * 0.1f + i) * 0.2f;
            
            mStack.translate(x, y, z);
            
            // Billboard to face camera (like ChantCasterRenderer)
            mStack.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
            
            // Get sign sprite
            TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(new ResourceLocation("eidolon", "block/sign_" + sign.getRegistryName().getPath()));
            
            if (sprite != null) {
                // Render sign sprite (exact ChantCasterRenderer method)
                renderSignSprite(mStack, sb, sprite, time, i);
            }
            
            mStack.popPose();
        }
        
        mStack.popPose();
    }
    
    /**
     * Render individual sign sprite (copied from ChantCasterRenderer)
     */
    private static void renderSignSprite(PoseStack mStack, VertexConsumer sb, TextureAtlasSprite sprite, float time, int index) {
        float size = 0.5f;
        float alpha = 0.8f + 0.2f * Mth.sin(time * 0.1f + index);
        
        var matrix = mStack.last().pose();
        var normal = mStack.last().normal();
        
        int brightness = 15728880; // Full brightness
        
        // Front face
        sb.vertex(matrix, -size, -size, 0).color(1f, 1f, 1f, alpha).uv(sprite.getU0(), sprite.getV1()).overlayCoords(0).uv2(brightness).normal(normal, 0, 0, 1).endVertex();
        sb.vertex(matrix, size, -size, 0).color(1f, 1f, 1f, alpha).uv(sprite.getU1(), sprite.getV1()).overlayCoords(0).uv2(brightness).normal(normal, 0, 0, 1).endVertex();
        sb.vertex(matrix, size, size, 0).color(1f, 1f, 1f, alpha).uv(sprite.getU1(), sprite.getV0()).overlayCoords(0).uv2(brightness).normal(normal, 0, 0, 1).endVertex();
        sb.vertex(matrix, -size, size, 0).color(1f, 1f, 1f, alpha).uv(sprite.getU0(), sprite.getV0()).overlayCoords(0).uv2(brightness).normal(normal, 0, 0, 1).endVertex();
        
        // Back face
        sb.vertex(matrix, -size, size, 0).color(1f, 1f, 1f, alpha).uv(sprite.getU0(), sprite.getV0()).overlayCoords(0).uv2(brightness).normal(normal, 0, 0, -1).endVertex();
        sb.vertex(matrix, size, size, 0).color(1f, 1f, 1f, alpha).uv(sprite.getU1(), sprite.getV0()).overlayCoords(0).uv2(brightness).normal(normal, 0, 0, -1).endVertex();
        sb.vertex(matrix, size, -size, 0).color(1f, 1f, 1f, alpha).uv(sprite.getU1(), sprite.getV1()).overlayCoords(0).uv2(brightness).normal(normal, 0, 0, -1).endVertex();
        sb.vertex(matrix, -size, -size, 0).color(1f, 1f, 1f, alpha).uv(sprite.getU0(), sprite.getV1()).overlayCoords(0).uv2(brightness).normal(normal, 0, 0, -1).endVertex();
    }
}
