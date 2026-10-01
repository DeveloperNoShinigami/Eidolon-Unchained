package com.bluelotuscoding.eidolonunchained.client;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.hexblade.HexbladeGeo;
import com.bluelotuscoding.eidolonunchained.hexblade.HexbladeItem;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import elucent.eidolon.client.particle.Particles;
import elucent.eidolon.registries.EidolonParticles;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.keyframe.event.data.ParticleKeyframeData;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import software.bernie.geckolib.util.RenderUtils;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders a scripted hexblade that has {@code .geoModel(...)} in hand, on the ground, in item frames and on other
 * players and mobs. GUI slots never reach it: the generated item model shows the flat icon there (see
 * {@code HexbladeItemBuilder#generateAssetJsons}). Parts in {@code <name>_geo_glowmask.png} glow when that file exists.
 * <p>
 * Soul-fire: the model's locators (bone {@code "locators"} in the geo file) are placed in the world every render, and
 * the clip's particle keyframes spawn Eidolon's glowing particles there: {@code soul_burst}, {@code soul_flare},
 * {@code soul_puff}. While the blade is awakened a thin flame stream also rises from the {@code spine*} locators.
 * In first person the hand is drawn in view space with its own projection, so the particles appear at the matching
 * point in the world: close to the drawn blade, not exactly on it.
 */
public class HexbladeGeoRenderer extends GeoItemRenderer<HexbladeItem> {
    private static final float R = 0x6f / 255f, G = 0xf5 / 255f, B = 0xd8 / 255f;          // Myrkul teal #6ff5d8
    private static final float R2 = 0.10f, G2 = 0.45f, B2 = 0.40f;
    private static final List<ParticleKeyframeData> PENDING = new ArrayList<>();

    static {
        HexbladeGeo.particleSink = PENDING::add;
    }

    private record Locator(String name, float x, float y, float z) {
    }

    private final Map<String, List<Locator>> locatorsByBone = new HashMap<>();
    private final List<String> spine = new ArrayList<>();
    private final Map<String, Vec3> located = new HashMap<>();
    private final Map<Long, Long> lastAmbientTick = new HashMap<>();

    public HexbladeGeoRenderer(ResourceLocation id) {
        super(new GeoModel<>() {
            @Override
            public ResourceLocation getModelResource(HexbladeItem item) {
                return HexbladeGeo.geo(id);
            }

            @Override
            public ResourceLocation getTextureResource(HexbladeItem item) {
                return HexbladeGeo.texture(id);
            }

            @Override
            public ResourceLocation getAnimationResource(HexbladeItem item) {
                return HexbladeGeo.animations(id);
            }
        });
        if (Minecraft.getInstance().getResourceManager().getResource(HexbladeGeo.glowmask(id)).isPresent()) {
            addRenderLayer(new AutoGlowingGeoLayer<>(this));
        }
        readLocators(HexbladeGeo.geo(id));
    }

    /** GeckoLib does not keep locators, so read them from the geo file: bedrock space, x mirrored, 1/16 block. */
    private void readLocators(ResourceLocation geo) {
        var res = Minecraft.getInstance().getResourceManager().getResource(geo);
        if (res.isEmpty()) return;
        try (Reader reader = res.get().openAsReader()) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            for (var boneEl : root.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones")) {
                JsonObject bone = boneEl.getAsJsonObject();
                if (!bone.has("locators")) continue;
                for (var e : bone.getAsJsonObject("locators").entrySet()) {
                    var v = e.getValue().isJsonArray() ? e.getValue().getAsJsonArray() : e.getValue().getAsJsonObject().getAsJsonArray("offset");
                    var loc = new Locator(e.getKey(), -v.get(0).getAsFloat() / 16f, v.get(1).getAsFloat() / 16f, v.get(2).getAsFloat() / 16f);
                    locatorsByBone.computeIfAbsent(bone.get("name").getAsString(), k -> new ArrayList<>()).add(loc);
                    if (loc.name().startsWith("spine")) spine.add(loc.name());
                }
            }
        } catch (Exception e) {
            EidolonUnchained.LOGGER.warn("hexblade geo {}: could not read locators: {}", geo, e.toString());
        }
    }

    @Override
    public void renderRecursively(PoseStack poseStack, HexbladeItem animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        List<Locator> locs = isReRender ? null : locatorsByBone.get(bone.getName());
        if (locs != null) {
            poseStack.pushPose();
            RenderUtils.prepMatrixForBone(poseStack, bone);
            var pose = poseStack.last().pose();
            for (Locator l : locs) {
                Vector4f v = pose.transform(new Vector4f(l.x(), l.y(), l.z(), 1f));
                located.put(l.name(), toWorld(v.x(), v.y(), v.z()));
            }
            poseStack.popPose();
        }
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    /**
     * Render space → world. In-world rendering (third person, other players, mobs, ground, frames) is camera-relative
     * with the camera's rotation applied; first-person hands are drawn in view space. Both undo with the inverse view
     * rotation plus the camera position.
     */
    private static Vec3 toWorld(float x, float y, float z) {
        Camera cam = Minecraft.getInstance().gameRenderer.getMainCamera();
        Quaternionf view = Axis.XP.rotationDegrees(cam.getXRot()).mul(Axis.YP.rotationDegrees(cam.getYRot() + 180f));
        Vector3f p = view.conjugate().transform(new Vector3f(x, y, z));
        return cam.getPosition().add(p.x(), p.y(), p.z());
    }

    @Override
    public void renderFinal(PoseStack poseStack, HexbladeItem animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        super.renderFinal(poseStack, animatable, model, bufferSource, buffer, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && !Minecraft.getInstance().isPaused()) {
            for (ParticleKeyframeData data : PENDING) {
                Vec3 at = located.get(data.getLocator());
                if (at != null) effect(level, data.getEffect(), at);
            }
            if (currentItemStack != null && HexbladeItem.isAwakened(currentItemStack) && !spine.isEmpty()) {
                long id = GeoItem.getId(currentItemStack);
                long tick = level.getGameTime();
                Long last = lastAmbientTick.put(id, tick);
                if (last == null || last != tick) {
                    Vec3 at = located.get(spine.get(level.random.nextInt(spine.size())));
                    if (at != null) effect(level, "ambient", at);
                }
                if (lastAmbientTick.size() > 64) lastAmbientTick.clear();
            }
        }
        PENDING.clear();
        located.clear();
    }

    private static Particles.ParticleBuilder teal(RegistryObject<?> type, float alpha, float scale, int life) {
        return Particles.create(type).setColor(R, G, B, R2, G2, B2).setAlpha(alpha, 0f).setScale(scale, 0f).setLifetime(life).disableGravity();
    }

    /** Effect names from the clips' particle keyframes; unknown names do nothing. */
    private static void effect(ClientLevel level, String name, Vec3 at) {
        switch (name) {
            case "soul_burst" -> {
                teal(EidolonParticles.WISP_PARTICLE, 0.6f, 0.3f, 20).randomOffset(0.05).randomVelocity(0.035).repeat(level, at.x, at.y, at.z, 6);
                teal(EidolonParticles.SPARKLE_PARTICLE, 0.8f, 0.15f, 14).randomOffset(0.06).randomVelocity(0.05).repeat(level, at.x, at.y, at.z, 4);
            }
            case "soul_flare" -> {
                teal(EidolonParticles.FLAME_PARTICLE, 0.7f, 0.25f, 16).randomOffset(0.05).randomVelocity(0.015).addVelocity(0, 0.02, 0).repeat(level, at.x, at.y, at.z, 4);
                teal(EidolonParticles.SPARKLE_PARTICLE, 1f, 0.2f, 10).randomOffset(0.08).repeat(level, at.x, at.y, at.z, 3);
            }
            case "soul_puff" -> teal(EidolonParticles.WISP_PARTICLE, 0.45f, 0.18f, 24).randomOffset(0.04).randomVelocity(0.01).addVelocity(0, 0.012, 0).repeat(level, at.x, at.y, at.z, 2);
            case "ambient" -> teal(EidolonParticles.FLAME_PARTICLE, 0.35f, 0.12f, 18).randomOffset(0.06).randomVelocity(0.005).addVelocity(0, 0.01, 0).spawn(level, at.x, at.y, at.z);
            default -> {
            }
        }
    }
}
