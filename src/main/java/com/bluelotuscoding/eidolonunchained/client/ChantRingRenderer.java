package com.bluelotuscoding.eidolonunchained.client;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import elucent.eidolon.Eidolon;
import elucent.eidolon.api.spells.Sign;
import elucent.eidolon.event.ClientEvents;
import elucent.eidolon.registries.Signs;
import elucent.eidolon.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The chant ring (D41), drawn around every caster whose signs the server synced ({@link ChantClient#RINGS}): the same
 * picture Eidolon's {@code ChantCasterRenderer} draws for its chanter entity, and the old Eidolon Unchained branch's
 * {@code PlayerChantCasterRenderer} drew around the player — sign sprites on a circle in front of the caster, a small
 * ring behind each sign, and two colour-blended beam rings that grow with the sequence. Uses Eidolon's delayed render
 * buffer and its glowing particle render type, so it blends like Eidolon's own effects.
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MOD_ID, value = Dist.CLIENT)
public final class ChantRingRenderer {
    private ChantRingRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || ChantClient.RINGS.isEmpty()) return;
        var pose = event.getPoseStack();
        var cam = mc.gameRenderer.getMainCamera().getPosition();
        float pt = event.getPartialTick();
        for (var e : new ArrayList<>(ChantClient.RINGS.entrySet())) {
            var entity = mc.level.getEntity(e.getKey());
            if (!(entity instanceof LivingEntity caster) || !caster.isAlive()) { ChantClient.RINGS.remove(e.getKey()); continue; }
            var signs = new ArrayList<Sign>();
            for (var id : e.getValue()) { var s = Signs.find(ResourceLocation.tryParse(id)); if (s != null) signs.add(s); }
            if (signs.isEmpty()) continue;
            boolean self = caster == mc.player && mc.options.getCameraType().isFirstPerson();
            Vec3 look = caster instanceof Mob m ? Vec3.directionFromRotation(Mth.lerp(pt, m.xRotO, m.getXRot()), Mth.lerp(pt, m.yHeadRotO, m.yHeadRot)).normalize()
                    : caster.getViewVector(pt);
            double x = Mth.lerp(pt, caster.xOld, caster.getX()) - cam.x;
            double y = Mth.lerp(pt, caster.yOld, caster.getY()) - cam.y;
            double z = Mth.lerp(pt, caster.zOld, caster.getZ()) - cam.z;
            float pulse = ChantClient.WINDING.contains(e.getKey()) ? 0.75f + 0.25f * Mth.sin(caster.tickCount * 0.6f) : 1f;
            pose.pushPose();
            pose.translate(x, y + caster.getBbHeight() * 0.55, z);
            render(pose, caster, signs, look, self ? 0.55f * pulse : pulse);
            pose.popPose();
        }
    }

    /** Port of Eidolon's ChantCasterRenderer geometry, centred on the caster. */
    public static void render(PoseStack pose, LivingEntity caster, List<Sign> signs, Vec3 look, float alphaMod) {
        var mc = Minecraft.getInstance();
        VertexConsumer sb = ClientEvents.getDelayedRender().getBuffer(RenderUtil.GLOWING_BLOCK_PARTICLE);
        TextureAtlasSprite beam = mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(new ResourceLocation(Eidolon.MODID, "particle/beam"));
        TextureAtlasSprite ring = mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(new ResourceLocation(Eidolon.MODID, "particle/ring"));
        float yaw = (float) Mth.atan2(look.x, look.z);
        Vec3 left = new Vec3(Math.cos(yaw), 0, -Math.sin(yaw));
        Vec3 up = left.cross(look);
        int sz = Math.max(0, signs.size() - 1);
        float r = Mth.sqrt(sz) / 4f;
        if (sz > 0) r = Math.max(0.3f, r);
        Vec3 center = look.add(0, 0.5f, 0);
        var m = pose.last().pose();

        int i = 0;
        for (Sign s : signs) {
            float a = -Mth.PI / 2 - i * 2 * Mth.PI / signs.size();
            float sa = Mth.sin(a), ca = Mth.cos(a);
            Vec3 od = center.add(left.scale(r * ca)).add(up.scale(r * sa));
            Vec3 dxd = left.scale(0.175), dyd = up.scale(0.175);
            Vector3f o = new Vector3f((float) od.x, (float) od.y, (float) od.z);
            Vector3f dx = new Vector3f((float) dxd.x, (float) dxd.y, (float) dxd.z);
            Vector3f dy = new Vector3f((float) dyd.x, (float) dyd.y, (float) dyd.z);
            TextureAtlasSprite spr = mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(s.getSprite());
            float bright = Mth.clamp(Mth.sin(a + Mth.TWO_PI * caster.tickCount / 20), 0, 1);
            bright *= bright;
            bright = 0.6f + 0.4f * bright;
            for (int j = 0; j < 2; j++) quad(sb, m, o, dx, dy, spr, s.getRed(), s.getGreen(), s.getBlue(), bright * alphaMod, j == 1);
            dx.mul(1.75f);
            dy.mul(1.75f);
            quad(sb, m, o, dx, dy, ring, s.getRed(), s.getGreen(), s.getBlue(), alphaMod * 0.5f, false);
            quad(sb, m, o, dx, dy, ring, s.getRed(), s.getGreen(), s.getBlue(), alphaMod * 0.5f, true);
            i++;
        }

        // the two beam rings, colour-blended between neighbouring signs (Eidolon's "building" look)
        Iterator<Sign> iter = signs.iterator();
        Sign cur = null, next = signs.get(signs.size() - 1);
        float rr = r + 0.1375f, rr2 = rr + 0.2f, rs = r - 0.3f, rs2 = rs + 0.2f;
        int steps = Math.max(24, signs.size() * 4);
        if (steps % signs.size() != 0) steps += signs.size() - steps % signs.size();
        int periodicity = Math.max(4, steps / signs.size());
        for (i = 0; i < steps; i++) {
            if (i % periodicity == 0) {
                cur = next;
                next = iter.next();
                if (!iter.hasNext()) iter = signs.iterator();
            }
            float a1 = -Mth.PI / 2 - (i - periodicity) * Mth.TWO_PI / steps, a2 = -Mth.PI / 2 - (i - periodicity + 1) * Mth.TWO_PI / steps;
            float f1 = (i % periodicity) / (float) periodicity, f2 = (i % periodicity + 1) / (float) periodicity;
            float r1 = Mth.lerp(f1, cur.getRed(), next.getRed()), r2 = Mth.lerp(f2, cur.getRed(), next.getRed());
            float g1 = Mth.lerp(f1, cur.getGreen(), next.getGreen()), g2 = Mth.lerp(f2, cur.getGreen(), next.getGreen());
            float b1 = Mth.lerp(f1, cur.getBlue(), next.getBlue()), b2 = Mth.lerp(f2, cur.getBlue(), next.getBlue());
            band(sb, m, center, left, up, rr, rr2, a1, a2, beam, r1, g1, b1, r2, g2, b2, alphaMod * 0.5f);
            band(sb, m, center, left, up, rs, rs2, a1, a2, beam, r1, g1, b1, r2, g2, b2, alphaMod * 0.5f);
        }
    }

    private static void quad(VertexConsumer sb, org.joml.Matrix4f m, Vector3f o, Vector3f dx, Vector3f dy, TextureAtlasSprite spr,
                             float r, float g, float b, float a, boolean back) {
        if (!back) {
            v(sb, m, o.x() - dx.x() + dy.x(), o.y() - dx.y() + dy.y(), o.z() - dx.z() + dy.z(), spr.getU1(), spr.getV1(), r, g, b, a);
            v(sb, m, o.x() - dx.x() - dy.x(), o.y() - dx.y() - dy.y(), o.z() - dx.z() - dy.z(), spr.getU1(), spr.getV0(), r, g, b, a);
            v(sb, m, o.x() + dx.x() - dy.x(), o.y() + dx.y() - dy.y(), o.z() + dx.z() - dy.z(), spr.getU0(), spr.getV0(), r, g, b, a);
            v(sb, m, o.x() + dx.x() + dy.x(), o.y() + dx.y() + dy.y(), o.z() + dx.z() + dy.z(), spr.getU0(), spr.getV1(), r, g, b, a);
        } else {
            v(sb, m, o.x() + dx.x() + dy.x(), o.y() + dx.y() + dy.y(), o.z() + dx.z() + dy.z(), spr.getU1(), spr.getV1(), r, g, b, a);
            v(sb, m, o.x() + dx.x() - dy.x(), o.y() + dx.y() - dy.y(), o.z() + dx.z() - dy.z(), spr.getU1(), spr.getV0(), r, g, b, a);
            v(sb, m, o.x() - dx.x() - dy.x(), o.y() - dx.y() - dy.y(), o.z() - dx.z() - dy.z(), spr.getU0(), spr.getV0(), r, g, b, a);
            v(sb, m, o.x() - dx.x() + dy.x(), o.y() - dx.y() + dy.y(), o.z() - dx.z() + dy.z(), spr.getU0(), spr.getV1(), r, g, b, a);
        }
    }

    private static void band(VertexConsumer sb, org.joml.Matrix4f m, Vec3 center, Vec3 left, Vec3 up, float ri, float ro, float a1, float a2,
                             TextureAtlasSprite beam, float r1, float g1, float b1, float r2, float g2, float b2, float a) {
        float sa1 = Mth.sin(a1), ca1 = Mth.cos(a1), sa2 = Mth.sin(a2), ca2 = Mth.cos(a2);
        Vec3 id1 = center.add(left.scale(ri * ca1)).add(up.scale(ri * sa1));
        Vec3 id2 = center.add(left.scale(ri * ca2)).add(up.scale(ri * sa2));
        Vec3 od1 = center.add(left.scale(ro * ca1)).add(up.scale(ro * sa1));
        Vec3 od2 = center.add(left.scale(ro * ca2)).add(up.scale(ro * sa2));
        v(sb, m, (float) od1.x, (float) od1.y, (float) od1.z, beam.getU1(), beam.getV1(), r1, g1, b1, a);
        v(sb, m, (float) od2.x, (float) od2.y, (float) od2.z, beam.getU0(), beam.getV1(), r2, g2, b2, a);
        v(sb, m, (float) id2.x, (float) id2.y, (float) id2.z, beam.getU0(), beam.getV0(), r2, g2, b2, a);
        v(sb, m, (float) id1.x, (float) id1.y, (float) id1.z, beam.getU1(), beam.getV0(), r1, g1, b1, a);
        v(sb, m, (float) od2.x, (float) od2.y, (float) od2.z, beam.getU1(), beam.getV1(), r2, g2, b2, a);
        v(sb, m, (float) od1.x, (float) od1.y, (float) od1.z, beam.getU0(), beam.getV1(), r1, g1, b1, a);
        v(sb, m, (float) id1.x, (float) id1.y, (float) id1.z, beam.getU0(), beam.getV0(), r1, g1, b1, a);
        v(sb, m, (float) id2.x, (float) id2.y, (float) id2.z, beam.getU1(), beam.getV0(), r2, g2, b2, a);
    }

    private static void v(VertexConsumer sb, org.joml.Matrix4f m, float x, float y, float z, float u, float vv, float r, float g, float b, float a) {
        sb.vertex(m, x, y, z).uv(u, vv).color(r, g, b, a).uv2(0).endVertex();
    }
}
