package com.bluelotuscoding.eidolonunchained.client.renderer;

import com.bluelotuscoding.eidolonunchained.chant.PlayerChantingSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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

import java.util.Iterator;
import java.util.List;

/**
 * Player chant renderer that replicates the exact ChantCasterEntity behavior
 * but renders around the player instead of an entity.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "eidolonunchained", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerChantCasterRenderer {
    
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        // Use AFTER_ENTITIES like the original ChantCasterRenderer
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        
        // Get current chant signs for this player
        List<Sign> signs = PlayerChantingSystem.getPlayerChantSigns(player.getUUID());
        if (signs.isEmpty()) return;
        
        // Render the chant using exact ChantCasterEntity logic
        renderPlayerChantSigns(event.getPoseStack(), event.getPartialTick(), player, signs);
    }
    
    /**
     * Render floating signs around player using exact ChantCasterRenderer logic
     */
    private static void renderPlayerChantSigns(PoseStack mStack, float pticks, Player player, List<Sign> signs) {
        mStack.pushPose();
        
        // Use the same buffers as ChantCasterRenderer
        VertexConsumer sb = ClientEvents.getDelayedRender().getBuffer(RenderUtil.GLOWING_BLOCK_PARTICLE);

        Minecraft mc = Minecraft.getInstance();
        
        // Get textures (exactly like ChantCasterRenderer)
        TextureAtlasSprite beam = mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
            .apply(new ResourceLocation("eidolon", "particle/beam"));
        TextureAtlasSprite ring = mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
            .apply(new ResourceLocation("eidolon", "particle/ring"));

        // Camera-relative positioning for world rendering
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
        double px = Mth.lerp(pticks, player.xOld, player.getX());
        double py = Mth.lerp(pticks, player.yOld, player.getY());
        double pz = Mth.lerp(pticks, player.zOld, player.getZ());
        
        // Translate to player position relative to camera (at eye level)
        mStack.translate(px - cameraPos.x, py - cameraPos.y + 1.0, pz - cameraPos.z);

        // Player's look direction (like ChantCasterRenderer)
        Vec3 look = player.getLookAngle();
        double yaw = Mth.atan2(look.x, look.z);
        Vec3 left = new Vec3(Math.cos(yaw), 0, -Math.sin(yaw));
        Vec3 up = left.cross(look);

        // Circle parameters (exactly like ChantCasterRenderer)
        int sz = Math.max(0, signs.size() - 1);
        float r = Mth.sqrt(sz) / 4f;
        if (sz > 0) r = Math.max(0.3f, r);
        Vec3 center = look.add(0, 0.5f, 0);
        
        // Animation parameters (like ChantCasterRenderer)
        int nreps = 1; // No death animation for players
        float alphaMod = 1.0f;
        
        // PHASE 1: Render sign sprites and individual rings (like ChantCasterRenderer lines 70-112)
        for (int k = 0; k < nreps; k++) {
            int i = 0;
            for (Sign s : signs) {
                float a = -Mth.PI / 2 - i * 2 * Mth.PI / signs.size();
                float sa = Mth.sin(a), ca = Mth.cos(a);

                Vec3 od = center.add(left.scale(r * ca)).add(up.scale(r * sa));
                Vec3 dxd = left.scale(0.175), dyd = up.scale(0.175);
                Vector3f o = new Vector3f((float)od.x, (float)od.y, (float)od.z);
                Vector3f dx = new Vector3f((float)dxd.x, (float)dxd.y, (float)dxd.z);
                Vector3f dy = new Vector3f((float)dyd.x, (float)dyd.y, (float)dyd.z);

                TextureAtlasSprite spr = mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(s.getSprite());

                // Brightness animation (exactly like ChantCasterRenderer)
                float brightMod = Mth.clamp(Mth.sin(a + Mth.TWO_PI * player.tickCount / 20), 0, 1);
                brightMod *= brightMod;
                brightMod = 0.6f + 0.4f * brightMod;

                // Render sign sprite (front and back faces - exactly like ChantCasterRenderer lines 87-97)
                for (int j = 0; j < 2; j++) {
                    sb.vertex(mStack.last().pose(), o.x() - dx.x() + dy.x(), o.y() - dx.y() + dy.y(), o.z() - dx.z() + dy.z())
                        .uv(spr.getU1(), spr.getV1())
                        .color(s.getRed(), s.getGreen(), s.getBlue(), brightMod * alphaMod)
                        .uv2(0).endVertex();
                    sb.vertex(mStack.last().pose(), o.x() - dx.x() - dy.x(), o.y() - dx.y() - dy.y(), o.z() - dx.z() - dy.z())
                        .uv(spr.getU1(), spr.getV0())
                        .color(s.getRed(), s.getGreen(), s.getBlue(), brightMod * alphaMod)
                        .uv2(0).endVertex();
                    sb.vertex(mStack.last().pose(), o.x() + dx.x() - dy.x(), o.y() + dx.y() - dy.y(), o.z() + dx.z() - dy.z())
                        .uv(spr.getU0(), spr.getV0())
                        .color(s.getRed(), s.getGreen(), s.getBlue(), brightMod * alphaMod)
                        .uv2(0).endVertex();
                    sb.vertex(mStack.last().pose(), o.x() + dx.x() + dy.x(), o.y() + dx.y() + dy.y(), o.z() + dx.z() + dy.z())
                        .uv(spr.getU0(), spr.getV1())
                        .color(s.getRed(), s.getGreen(), s.getBlue(), brightMod * alphaMod)
                        .uv2(0).endVertex();

                    sb.vertex(mStack.last().pose(), o.x() + dx.x() + dy.x(), o.y() + dx.y() + dy.y(), o.z() + dx.z() + dy.z())
                        .uv(spr.getU1(), spr.getV1())
                        .color(s.getRed(), s.getGreen(), s.getBlue(), brightMod * alphaMod)
                        .uv2(0).endVertex();
                    sb.vertex(mStack.last().pose(), o.x() + dx.x() - dy.x(), o.y() + dx.y() - dy.y(), o.z() + dx.z() - dy.z())
                        .uv(spr.getU1(), spr.getV0())
                        .color(s.getRed(), s.getGreen(), s.getBlue(), brightMod * alphaMod)
                        .uv2(0).endVertex();
                    sb.vertex(mStack.last().pose(), o.x() - dx.x() - dy.x(), o.y() - dx.y() - dy.y(), o.z() - dx.z() - dy.z())
                        .uv(spr.getU0(), spr.getV0())
                        .color(s.getRed(), s.getGreen(), s.getBlue(), brightMod * alphaMod)
                        .uv2(0).endVertex();
                    sb.vertex(mStack.last().pose(), o.x() - dx.x() + dy.x(), o.y() - dx.y() + dy.y(), o.z() - dx.z() + dy.z())
                        .uv(spr.getU0(), spr.getV1())
                        .color(s.getRed(), s.getGreen(), s.getBlue(), brightMod * alphaMod)
                        .uv2(0).endVertex();
                }

                // Render individual ring around sign (exactly like ChantCasterRenderer lines 99-109)
                dx.mul(1.75f);
                dy.mul(1.75f);
                sb.vertex(mStack.last().pose(), o.x() - dx.x() + dy.x(), o.y() - dx.y() + dy.y(), o.z() - dx.z() + dy.z())
                    .uv(ring.getU1(), ring.getV1())
                    .color(s.getRed(), s.getGreen(), s.getBlue(), alphaMod * 0.5f)
                    .uv2(0).endVertex();
                sb.vertex(mStack.last().pose(), o.x() - dx.x() - dy.x(), o.y() - dx.y() - dy.y(), o.z() - dx.z() - dy.z())
                    .uv(ring.getU1(), ring.getV0())
                    .color(s.getRed(), s.getGreen(), s.getBlue(), alphaMod * 0.5f)
                    .uv2(0).endVertex();
                sb.vertex(mStack.last().pose(), o.x() + dx.x() - dy.x(), o.y() + dx.y() - dy.y(), o.z() + dx.z() - dy.z())
                    .uv(ring.getU0(), ring.getV0())
                    .color(s.getRed(), s.getGreen(), s.getBlue(), alphaMod * 0.5f)
                    .uv2(0).endVertex();
                sb.vertex(mStack.last().pose(), o.x() + dx.x() + dy.x(), o.y() + dx.y() + dy.y(), o.z() + dx.z() + dy.z())
                    .uv(ring.getU0(), ring.getV1())
                    .color(s.getRed(), s.getGreen(), s.getBlue(), alphaMod * 0.5f)
                    .uv2(0).endVertex();

                sb.vertex(mStack.last().pose(), o.x() + dx.x() + dy.x(), o.y() + dx.y() + dy.y(), o.z() + dx.z() + dy.z())
                    .uv(ring.getU1(), ring.getV1())
                    .color(s.getRed(), s.getGreen(), s.getBlue(), alphaMod * 0.5f)
                    .uv2(0).endVertex();
                sb.vertex(mStack.last().pose(), o.x() + dx.x() - dy.x(), o.y() + dx.y() - dy.y(), o.z() + dx.z() - dy.z())
                    .uv(ring.getU1(), ring.getV0())
                    .color(s.getRed(), s.getGreen(), s.getBlue(), alphaMod * 0.5f)
                    .uv2(0).endVertex();
                sb.vertex(mStack.last().pose(), o.x() - dx.x() - dy.x(), o.y() - dx.y() - dy.y(), o.z() - dx.z() - dy.z())
                    .uv(ring.getU0(), ring.getV0())
                    .color(s.getRed(), s.getGreen(), s.getBlue(), alphaMod * 0.5f)
                    .uv2(0).endVertex();
                sb.vertex(mStack.last().pose(), o.x() - dx.x() + dy.x(), o.y() - dx.y() + dy.y(), o.z() - dx.z() + dy.z())
                    .uv(ring.getU0(), ring.getV1())
                    .color(s.getRed(), s.getGreen(), s.getBlue(), alphaMod * 0.5f)
                    .uv2(0).endVertex();

                i++;
            }
            
            // PHASE 2: Render connecting beam ring (THIS IS THE MISSING PIECE!)
            // This is what makes the ring "build up" - exactly like ChantCasterRenderer lines 114-178
            i = 0;
            if (!signs.isEmpty()) {
                Iterator<Sign> iter = signs.iterator();
                Sign cur = null, next = signs.get(signs.size() - 1); // Start with last sign
                float rr = r + 0.1375f;   // Outer radius
                float rr2 = rr + 0.2f;    // Even more outer
                float rs = r - 0.3f;      // Inner radius  
                float rs2 = rs + 0.2f;    // Less inner
                int steps = Math.max(24, signs.size() * 4);
                if (steps % signs.size() != 0) steps += signs.size() - steps % signs.size();
                int periodicity = Math.max(4, steps / signs.size());
                
                for (i = 0; i < steps; i++) {
                    if (i % periodicity == 0) {
                        cur = next;
                        next = iter.next();
                        if (!iter.hasNext()) iter = signs.iterator();
                    }
                    float a1 = -Mth.PI / 2 - (i - periodicity) * Mth.TWO_PI / steps;
                    float a2 = -Mth.PI / 2 - (i - periodicity + 1) * Mth.TWO_PI / steps;
                    float sa1 = Mth.sin(a1), ca1 = Mth.cos(a1);
                    float sa2 = Mth.sin(a2), ca2 = Mth.cos(a2);

                    // Color interpolation between signs
                    float r1 = Mth.lerp((i % periodicity) / (float)periodicity, cur.getRed(), next.getRed());
                    float r2 = Mth.lerp((i % periodicity + 1) / (float)periodicity, cur.getRed(), next.getRed());
                    float g1 = Mth.lerp((i % periodicity) / (float)periodicity, cur.getGreen(), next.getGreen());
                    float g2 = Mth.lerp((i % periodicity + 1) / (float)periodicity, cur.getGreen(), next.getGreen());
                    float b1 = Mth.lerp((i % periodicity) / (float)periodicity, cur.getBlue(), next.getBlue());
                    float b2 = Mth.lerp((i % periodicity + 1) / (float)periodicity, cur.getBlue(), next.getBlue());

                    // Outer ring beam
                    Vec3 id1 = center.add(left.scale(rr * ca1)).add(up.scale(rr * sa1));
                    Vec3 id2 = center.add(left.scale(rr * ca2)).add(up.scale(rr * sa2));
                    Vec3 od1 = center.add(left.scale(rr2 * ca1)).add(up.scale(rr2 * sa1));
                    Vec3 od2 = center.add(left.scale(rr2 * ca2)).add(up.scale(rr2 * sa2));
                    Vector3f i1 = new Vector3f((float)id1.x, (float)id1.y, (float)id1.z);
                    Vector3f i2 = new Vector3f((float)id2.x, (float)id2.y, (float)id2.z);
                    Vector3f o1 = new Vector3f((float)od1.x, (float)od1.y, (float)od1.z);
                    Vector3f o2 = new Vector3f((float)od2.x, (float)od2.y, (float)od2.z);

                    sb.vertex(mStack.last().pose(), o1.x(), o1.y(), o1.z()).uv(beam.getU1(), beam.getV1()).color(r1, g1, b1, alphaMod * 0.5f).uv2(0).endVertex();
                    sb.vertex(mStack.last().pose(), o2.x(), o2.y(), o2.z()).uv(beam.getU0(), beam.getV1()).color(r2, g2, b2, alphaMod * 0.5f).uv2(0).endVertex();
                    sb.vertex(mStack.last().pose(), i2.x(), i2.y(), i2.z()).uv(beam.getU0(), beam.getV0()).color(r2, g2, b2, alphaMod * 0.5f).uv2(0).endVertex();
                    sb.vertex(mStack.last().pose(), i1.x(), i1.y(), i1.z()).uv(beam.getU1(), beam.getV0()).color(r1, g1, b1, alphaMod * 0.5f).uv2(0).endVertex();

                    sb.vertex(mStack.last().pose(), o2.x(), o2.y(), o2.z()).uv(beam.getU1(), beam.getV1()).color(r2, g2, b2, alphaMod * 0.5f).uv2(0).endVertex();
                    sb.vertex(mStack.last().pose(), o1.x(), o1.y(), o1.z()).uv(beam.getU0(), beam.getV1()).color(r1, g1, b1, alphaMod * 0.5f).uv2(0).endVertex();
                    sb.vertex(mStack.last().pose(), i1.x(), i1.y(), i1.z()).uv(beam.getU0(), beam.getV0()).color(r1, g1, b1, alphaMod * 0.5f).uv2(0).endVertex();
                    sb.vertex(mStack.last().pose(), i2.x(), i2.y(), i2.z()).uv(beam.getU1(), beam.getV0()).color(r2, g2, b2, alphaMod * 0.5f).uv2(0).endVertex();

                    // Inner ring beam
                    id1 = center.add(left.scale(rs * ca1)).add(up.scale(rs * sa1));
                    id2 = center.add(left.scale(rs * ca2)).add(up.scale(rs * sa2));
                    od1 = center.add(left.scale(rs2 * ca1)).add(up.scale(rs2 * sa1));
                    od2 = center.add(left.scale(rs2 * ca2)).add(up.scale(rs2 * sa2));
                    i1 = new Vector3f((float)id1.x, (float)id1.y, (float)id1.z);
                    i2 = new Vector3f((float)id2.x, (float)id2.y, (float)id2.z);
                    o1 = new Vector3f((float)od1.x, (float)od1.y, (float)od1.z);
                    o2 = new Vector3f((float)od2.x, (float)od2.y, (float)od2.z);

                    sb.vertex(mStack.last().pose(), o1.x(), o1.y(), o1.z()).uv(beam.getU1(), beam.getV1()).color(r1, g1, b1, alphaMod * 0.5f).uv2(0).endVertex();
                    sb.vertex(mStack.last().pose(), o2.x(), o2.y(), o2.z()).uv(beam.getU0(), beam.getV1()).color(r2, g2, b2, alphaMod * 0.5f).uv2(0).endVertex();
                    sb.vertex(mStack.last().pose(), i2.x(), i2.y(), i2.z()).uv(beam.getU0(), beam.getV0()).color(r2, g2, b2, alphaMod * 0.5f).uv2(0).endVertex();
                    sb.vertex(mStack.last().pose(), i1.x(), i1.y(), i1.z()).uv(beam.getU1(), beam.getV0()).color(r1, g1, b1, alphaMod * 0.5f).uv2(0).endVertex();

                    sb.vertex(mStack.last().pose(), o2.x(), o2.y(), o2.z()).uv(beam.getU1(), beam.getV1()).color(r2, g2, b2, alphaMod * 0.5f).uv2(0).endVertex();
                    sb.vertex(mStack.last().pose(), o1.x(), o1.y(), o1.z()).uv(beam.getU0(), beam.getV1()).color(r1, g1, b1, alphaMod * 0.5f).uv2(0).endVertex();
                    sb.vertex(mStack.last().pose(), i1.x(), i1.y(), i1.z()).uv(beam.getU0(), beam.getV0()).color(r1, g1, b1, alphaMod * 0.5f).uv2(0).endVertex();
                    sb.vertex(mStack.last().pose(), i2.x(), i2.y(), i2.z()).uv(beam.getU1(), beam.getV0()).color(r2, g2, b2, alphaMod * 0.5f).uv2(0).endVertex();
                }
            }
        }
        
        mStack.popPose();
    }
}