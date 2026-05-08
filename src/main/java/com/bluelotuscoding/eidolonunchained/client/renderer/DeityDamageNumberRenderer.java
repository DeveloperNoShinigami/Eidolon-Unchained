package com.bluelotuscoding.eidolonunchained.client.renderer;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DeityDamageNumberRenderer {
    private static final List<DamageNumber> ACTIVE = new ArrayList<>();
    private static final Random RANDOM = new Random();
    private static final int LIFETIME_TICKS = 18;

    private DeityDamageNumberRenderer() {
    }

    public static void spawn(Entity target, float amount, int rgbColor) {
        if (target == null || target.level() == null) {
            return;
        }

        double xJitter = (RANDOM.nextDouble() - 0.5d) * Math.max(0.3d, target.getBbWidth() * 0.6d);
        double zJitter = (RANDOM.nextDouble() - 0.5d) * Math.max(0.3d, target.getBbWidth() * 0.6d);
        String text = formatAmount(amount);
        int color = 0xFF000000 | (rgbColor & 0x00FFFFFF);

        synchronized (ACTIVE) {
            ACTIVE.add(new DamageNumber(
                target.getId(),
                target.level().dimension(),
                target.getX() + xJitter,
                target.getY() + target.getBbHeight() + 0.35d,
                target.getZ() + zJitter,
                text,
                color
            ));
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        synchronized (ACTIVE) {
            Iterator<DamageNumber> iterator = ACTIVE.iterator();
            while (iterator.hasNext()) {
                DamageNumber number = iterator.next();
                number.age++;
                if (number.age >= LIFETIME_TICKS) {
                    iterator.remove();
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || ACTIVE.isEmpty()) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Font font = mc.font;
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        var camera = mc.gameRenderer.getMainCamera();

        synchronized (ACTIVE) {
            Iterator<DamageNumber> iterator = ACTIVE.iterator();
            while (iterator.hasNext()) {
                DamageNumber number = iterator.next();

                if (!number.dimension.equals(mc.level.dimension())) {
                    continue;
                }

                Entity trackedEntity = mc.level.getEntity(number.entityId);
                if (trackedEntity != null && trackedEntity.isAlive()) {
                    number.x = trackedEntity.getX();
                    number.y = trackedEntity.getY() + trackedEntity.getBbHeight() + 0.35d;
                    number.z = trackedEntity.getZ();
                }

                float life = number.age / (float) LIFETIME_TICKS;
                float alpha = 1.0f - life;
                if (alpha <= 0.0f) {
                    iterator.remove();
                    continue;
                }

                float yLift = life * 0.45f;
                int alphaInt = Mth.clamp((int) (alpha * 255.0f), 0, 255);
                int color = (alphaInt << 24) | (number.argbColor & 0x00FFFFFF);

                poseStack.pushPose();
                poseStack.translate(
                    number.x - camera.getPosition().x,
                    number.y - camera.getPosition().y + yLift,
                    number.z - camera.getPosition().z
                );
                poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
                poseStack.scale(-0.025f, -0.025f, 0.025f);

                float halfWidth = font.width(number.text) / 2.0f;
                font.drawInBatch(
                    number.text,
                    -halfWidth,
                    0.0f,
                    color,
                    false,
                    poseStack.last().pose(),
                    buffer,
                    Font.DisplayMode.NORMAL,
                    0,
                    15728880
                );

                poseStack.popPose();
            }
        }

        buffer.endBatch();
    }

    private static String formatAmount(float amount) {
        if (Math.abs(amount - Math.round(amount)) < 0.05f) {
            return Integer.toString(Math.round(amount));
        }
        return String.format(Locale.ROOT, "%.1f", amount);
    }

    private static final class DamageNumber {
        private final int entityId;
        private final ResourceKey<Level> dimension;
        private final String text;
        private final int argbColor;
        private double x;
        private double y;
        private double z;
        private int age;

        private DamageNumber(int entityId, ResourceKey<Level> dimension, double x, double y, double z, String text, int argbColor) {
            this.entityId = entityId;
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.text = text;
            this.argbColor = argbColor;
            this.age = 0;
        }
    }
}
