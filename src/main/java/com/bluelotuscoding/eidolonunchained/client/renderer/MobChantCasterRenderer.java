package com.bluelotuscoding.eidolonunchained.client.renderer;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.chant.DatapackChant;
import com.bluelotuscoding.eidolonunchained.chant.DatapackChantManager;
import elucent.eidolon.api.spells.Sign;
import elucent.eidolon.registries.Signs;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Renders chant-ring visuals for chant-capable mobs in world space.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MobChantCasterRenderer {
    private static final String CHANT_BUILD_ID_TAG = "eu_mob_chant_build_id";
    private static final String CHANT_BUILD_PROGRESS_TAG = "eu_mob_chant_build_progress";
    private static final int SEARCH_RADIUS = 64;

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }

        List<Mob> nearbyMobs = mc.level.getEntitiesOfClass(
            Mob.class,
            mc.player.getBoundingBox().inflate(SEARCH_RADIUS),
            mob -> mob.isAlive() && !mob.isSpectator()
        );

        for (Mob mob : nearbyMobs) {
            List<Sign> signs = resolveMobChantSigns(mob);
            if (signs.isEmpty()) {
                continue;
            }

            PlayerChantCasterRenderer.renderChantSigns(
                event.getPoseStack(),
                event.getPartialTick(),
                mob,
                signs,
                resolveMobHeadLook(mob, event.getPartialTick())
            );
        }
    }

    private static Vec3 resolveMobHeadLook(Mob mob, float partialTick) {
        float yawDegrees = Mth.lerp(partialTick, mob.yHeadRotO, mob.yHeadRot);
        float pitchDegrees = Mth.lerp(partialTick, mob.xRotO, mob.getXRot());
        return Vec3.directionFromRotation(pitchDegrees, yawDegrees).normalize();
    }

    private static List<Sign> resolveMobChantSigns(Mob mob) {
        CompoundTag data = mob.getPersistentData();
        if (!data.contains(CHANT_BUILD_ID_TAG, Tag.TAG_STRING)) {
            return Collections.emptyList();
        }

        int progress = Math.max(0, data.getInt(CHANT_BUILD_PROGRESS_TAG));
        if (progress <= 0) {
            return Collections.emptyList();
        }

        String buildChantId = data.getString(CHANT_BUILD_ID_TAG);
        if (buildChantId == null || buildChantId.isEmpty()) {
            return Collections.emptyList();
        }

        ResourceLocation chantId = ResourceLocation.tryParse(buildChantId);
        if (chantId == null) {
            return Collections.emptyList();
        }

        DatapackChant chant = DatapackChantManager.getChant(chantId);
        if (chant == null) {
            return Collections.emptyList();
        }

        List<Sign> signs = new ArrayList<>();
        int maxSigns = Math.min(progress, chant.getSignSequence().size());
        for (int i = 0; i < maxSigns; i++) {
            ResourceLocation signId = chant.getSignSequence().get(i);
            Sign sign = Signs.find(signId);
            if (sign != null) {
                signs.add(sign);
            }
        }

        return signs;
    }
}
