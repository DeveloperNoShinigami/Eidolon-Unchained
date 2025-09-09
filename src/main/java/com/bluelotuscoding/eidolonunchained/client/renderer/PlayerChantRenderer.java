package com.bluelotuscoding.eidolonunchained.client.renderer;

import com.bluelotuscoding.eidolonunchained.chant.PlayerChantingSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import elucent.eidolon.Eidolon;
import elucent.eidolon.api.spells.Sign;
import elucent.eidolon.event.ClientEvents;
import elucent.eidolon.util.RenderUtil;
import net.minecraft.client.Minecraft;
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
    private static void renderFloatingSignsAroundPlayer(PoseStack mStack, 
                                                       Player player, List<Sign> signs, float partialTick) {
        
        Minecraft mc = Minecraft.getInstance();
        VertexConsumer sb = ClientEvents.getDelayedRender().getBuffer(RenderUtil.GLOWING_SPRITE);
        
        mStack.pushPose();
        
        // Calculate player position with interpolation (like entity renderer)
        double px = Mth.lerp(partialTick, player.xOld, player.getX());
        double py = Mth.lerp(partialTick, player.yOld, player.getY());
        double pz = Mth.lerp(partialTick, player.zOld, player.getZ());
        
        // Don't translate by camera - work in world coordinates
        // (ChantCasterRenderer doesn't translate by camera either)
        
        // Player's look direction (like ChantCasterEntity)
        Vec3 look = player.getLookAngle();
        double yaw = Mth.atan2(look.x, look.z);
        Vec3 left = new Vec3(Math.cos(yaw), 0, -Math.sin(yaw));
        Vec3 up = left.cross(look);
        
        // Circle configuration (exactly like ChantCasterEntity)
        int sz = Math.max(0, signs.size() - 1);
        float r = Mth.sqrt(sz) / 4f;
        if (sz > 0) r = Math.max(0.3f, r);
        
        // Center point for the sign circle (like ChantCasterEntity: look + 0.5 up)
        Vec3 center = new Vec3(px, py, pz).add(look).add(0, 0.5f, 0);
        
        // Render each sign in the circle
        for (int i = 0; i < signs.size(); i++) {
            Sign s = signs.get(i);
            
            // Calculate position in circle (exactly like ChantCasterEntity)
            float a = -Mth.PI / 2 - i * 2 * Mth.PI / signs.size();
            float sa = Mth.sin(a), ca = Mth.cos(a);
            
            Vec3 od = center.add(left.scale(r * ca)).add(up.scale(r * sa));
            Vec3 dxd = left.scale(0.175), dyd = up.scale(0.175);
            Vector3f o = new Vector3f((float)od.x, (float)od.y, (float)od.z);
            Vector3f dx = new Vector3f((float)dxd.x, (float)dxd.y, (float)dxd.z);
            Vector3f dy = new Vector3f((float)dyd.x, (float)dyd.y, (float)dyd.z);
            
            // Get sign texture (like ChantCasterEntity)
            TextureAtlasSprite spr = mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(s.getSprite());
            
            // Brightness modulation with pulsing effect (like ChantCasterEntity)
            float brightMod = Mth.clamp(Mth.sin(a + Mth.TWO_PI * mc.level.getGameTime() / 20), 0, 1);
            brightMod *= brightMod;
            brightMod = 0.6f + 0.4f * brightMod;
            
            // Render sign quad (front and back faces, like ChantCasterEntity)
            for (int j = 0; j < 2; j++) {
                sb.vertex(mStack.last().pose(), o.x() - dx.x() + dy.x(), o.y() - dx.y() + dy.y(), o.z() - dx.z() + dy.z())
                    .uv(spr.getU1(), spr.getV1())
                    .color(s.getRed(), s.getGreen(), s.getBlue(), brightMod)
                    .uv2(0).endVertex();
                    
                sb.vertex(mStack.last().pose(), o.x() - dx.x() - dy.x(), o.y() - dx.y() - dy.y(), o.z() - dx.z() - dy.z())
                    .uv(spr.getU1(), spr.getV0())
                    .color(s.getRed(), s.getGreen(), s.getBlue(), brightMod)
                    .uv2(0).endVertex();
                    
                sb.vertex(mStack.last().pose(), o.x() + dx.x() - dy.x(), o.y() + dx.y() - dy.y(), o.z() + dx.z() - dy.z())
                    .uv(spr.getU0(), spr.getV0())
                    .color(s.getRed(), s.getGreen(), s.getBlue(), brightMod)
                    .uv2(0).endVertex();
                    
                sb.vertex(mStack.last().pose(), o.x() + dx.x() + dy.x(), o.y() + dx.y() + dy.y(), o.z() + dx.z() + dy.z())
                    .uv(spr.getU0(), spr.getV1())
                    .color(s.getRed(), s.getGreen(), s.getBlue(), brightMod)
                    .uv2(0).endVertex();
                
                // Back face
                sb.vertex(mStack.last().pose(), o.x() + dx.x() + dy.x(), o.y() + dx.y() + dy.y(), o.z() + dx.z() + dy.z())
                    .uv(spr.getU1(), spr.getV1())
                    .color(s.getRed(), s.getGreen(), s.getBlue(), brightMod)
                    .uv2(0).endVertex();
                    
                sb.vertex(mStack.last().pose(), o.x() + dx.x() - dy.x(), o.y() + dx.y() - dy.y(), o.z() + dx.z() - dy.z())
                    .uv(spr.getU1(), spr.getV0())
                    .color(s.getRed(), s.getGreen(), s.getBlue(), brightMod)
                    .uv2(0).endVertex();
                    
                sb.vertex(mStack.last().pose(), o.x() - dx.x() - dy.x(), o.y() - dx.y() - dy.y(), o.z() - dx.z() - dy.z())
                    .uv(spr.getU0(), spr.getV0())
                    .color(s.getRed(), s.getGreen(), s.getBlue(), brightMod)
                    .uv2(0).endVertex();
                    
                sb.vertex(mStack.last().pose(), o.x() - dx.x() + dy.x(), o.y() - dx.y() + dy.y(), o.z() - dx.z() + dy.z())
                    .uv(spr.getU0(), spr.getV1())
                    .color(s.getRed(), s.getGreen(), s.getBlue(), brightMod)
                    .uv2(0).endVertex();
            }
        }
        
        mStack.popPose();
    }
}