package com.bluelotuscoding.eidolonunchained.client;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.entity.EUEntities;
import com.bluelotuscoding.eidolonunchained.entity.ChantProjectileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** A camera-facing glowing quad tinted with the projectile's colour (full-bright, additive-looking). */
public final class ChantProjectileRenderer extends EntityRenderer<ChantProjectileEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(EidolonUnchained.MOD_ID, "textures/entity/chant_projectile.png");

    public ChantProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(ChantProjectileEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int packedLight) {
        pose.pushPose();
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        pose.mulPose(Axis.YP.rotationDegrees(180f));
        float s = e.getSize() * 2f;
        pose.scale(s, s, s);
        int c = e.getColor();
        int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
        var last = pose.last();
        Matrix4f m = last.pose();
        Matrix3f n = last.normal();
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));
        int light = 0xF000F0;
        vertex(vc, m, n, -0.5f, -0.5f, 0f, 1f, r, g, b, light);
        vertex(vc, m, n, 0.5f, -0.5f, 1f, 1f, r, g, b, light);
        vertex(vc, m, n, 0.5f, 0.5f, 1f, 0f, r, g, b, light);
        vertex(vc, m, n, -0.5f, 0.5f, 0f, 0f, r, g, b, light);
        pose.popPose();
        super.render(e, yaw, partialTick, pose, buffers, packedLight);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float u, float v, int r, int g, int b, int light) {
        vc.vertex(m, x, y, 0f).color(r, g, b, 255).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0f, 1f, 0f).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(ChantProjectileEntity entity) {
        return TEXTURE;
    }

    @Mod.EventBusSubscriber(modid = EidolonUnchained.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(EUEntities.CHANT_PROJECTILE.get(), ChantProjectileRenderer::new);
        }
    }
}
