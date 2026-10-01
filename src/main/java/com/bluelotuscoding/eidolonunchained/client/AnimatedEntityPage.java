package com.bluelotuscoding.eidolonunchained.client;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.api.codex.CodexDecls;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.math.Axis;
import elucent.eidolon.codex.CodexGui;
import elucent.eidolon.codex.EntityPage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.loading.object.BakedAnimations;
import software.bernie.geckolib.renderer.GeoRenderer;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Eidolon's {@link EntityPage}, but alive. Eidolon creates a new entity every frame and renders it at tick 0, so animated
 * models (GeckoLib, and vanilla idle motion) stand frozen. This page keeps one entity while the page is in view, ticked
 * by the client's game time (the codex does not pause the game), and draws it with a <b>private renderer</b> built from
 * the type's registered renderer provider: a GeckoLib renderer owns its model and the model owns the animation clock, so
 * the page's copy shares no animation state with the same mob in the world. When the page is reached again after being
 * out of view, a fresh entity and renderer are made, so the animation starts over.
 * <p>GeckoLib models take {@link CodexDecls.EntityOptions}: a clip played on arrival (the page's copy gets one controller
 * of its own) and a clip looped after it; a smaller size while arriving (a gate is larger than the deity) that eases back
 * to the standard size; and cuts at the ground or behind the model, as terrain or a gate would hide it. The page clips
 * to its own area so a large model never spills over the book.
 */
public class AnimatedEntityPage extends EntityPage {
    /**
     * A gap this long between the end of one draw and the start of the next means the page was left and reached
     * again. Measured from the end of a draw, so the page's own first draw (building the renderer, baking a big model)
     * never reads as having left.
     */
    private static final long LEFT_AFTER_MS = 500;
    /** Ticks to ease from the arrival size to the standard size once the arrival clip ends. */
    private static final float SETTLE_TICKS = 20;

    private final EntityType<?> entityType;
    private final CodexDecls.EntityOptions options;
    private final List<GroundClipBuffers.Plane> clipPlanes = new ArrayList<>();
    private Entity entity;
    private EntityRenderer<? super Entity> renderer;
    private long lastRenderMs;
    private long arrivedAt;
    /** Length of the arrival clip in ticks; 0 when none plays. */
    private double arrivalTicks;
    private boolean warned;

    public <T extends Entity> AnimatedEntityPage(EntityType<T> type, CodexDecls.EntityOptions options) {
        super(type);
        this.entityType = type;
        this.options = options;
        if (options.hideBelowGround()) clipPlanes.add(GroundClipBuffers.Plane.ground(0));
        if (options.hideBehind() != null) clipPlanes.add(GroundClipBuffers.Plane.behind(options.hideBehind().floatValue()));
    }

    /** The lang key of the entity's name, for the facing title page. */
    public String descriptionId() {
        return entityType.getDescriptionId();
    }

    @Override
    public void render(CodexGui gui, @NotNull GuiGraphics guiGraphics, ResourceLocation bg, int x, int y, int mouseX, int mouseY) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long ms = System.currentTimeMillis();
        if (entity == null || entity.level() != mc.level || ms - lastRenderMs > LEFT_AFTER_MS) {
            entity = entityType.create(mc.level);       // new entity and renderer = new GeckoLib animation state: the clip starts over
            if (entity == null) return;
            renderer = privateRenderer(mc, entity);
            arrivedAt = mc.level.getGameTime();
            arrivalTicks = options.animation() != null ? playOnArrival() : 0;
        }
        entity.tickCount = (int) mc.level.getGameTime();
        float partial = mc.getFrameTime();

        boolean gecko = entity instanceof GeoAnimatable;
        if (options.geckoOptions() && !gecko && !warned) {
            warned = true;
            EidolonUnchained.LOGGER.warn("Codex: entityPage model options apply to GeckoLib models only; '{}' is drawn with the defaults",
                    ForgeRegistries.ENTITY_TYPES.getKey(entityType));
        }
        float yaw = gecko ? (float) options.rotate() : -30f;
        float fit = Math.min(112 / entity.getBbHeight(), 100) * (gecko ? size(mc.level.getGameTime() - arrivedAt + partial) : 1f);

        guiGraphics.enableScissor(x, y, x + 128, y + 160);              // keep big models (gates, rifts) inside the page
        var pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(x + 64, y + 136, 64);
        pose.mulPose(Axis.XP.rotationDegrees(-15));
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.scale(fit, -fit, fit);
        MultiBufferSource.BufferSource buf = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        Lighting.setupForFlatItems();
        MultiBufferSource target = gecko && !clipPlanes.isEmpty() ? new GroundClipBuffers(buf, pose.last().pose(), clipPlanes) : buf;
        renderer.render(entity, entity.getYRot(), partial, pose, target, 0xf000f0);
        buf.endBatch();
        Lighting.setupFor3DItems();
        pose.popPose();
        guiGraphics.disableScissor();
        lastRenderMs = System.currentTimeMillis();
    }

    /** The arrival size while the arrival clip plays, then a smooth ease to the standard size. */
    private float size(double ticksSinceArrival) {
        double standard = options.scale();
        if (options.arrivalScale() == null || arrivalTicks <= 0) return (float) standard;
        double arrival = options.arrivalScale();
        double t = (ticksSinceArrival - arrivalTicks) / SETTLE_TICKS;
        if (t <= 0) return (float) arrival;
        if (t >= 1) return (float) standard;
        double eased = t * t * (3 - 2 * t);
        return (float) (arrival + (standard - arrival) * eased);
    }

    /** A renderer of this page's own, from the provider the type registered; the shared one if that fails. */
    @SuppressWarnings("unchecked")
    private EntityRenderer<? super Entity> privateRenderer(Minecraft mc, Entity entity) {
        try {
            var provider = (EntityRendererProvider<Entity>) providers().get(entityType);
            if (provider != null) {
                var dispatcher = mc.getEntityRenderDispatcher();
                var context = new EntityRendererProvider.Context(dispatcher, mc.getItemRenderer(), mc.getBlockRenderer(),
                        dispatcher.getItemInHandRenderer(), mc.getResourceManager(), mc.getEntityModels(), mc.font);
                return provider.create(context);
            }
        } catch (RuntimeException | ReflectiveOperationException e) {
            if (!warned) {
                warned = true;
                EidolonUnchained.LOGGER.warn("Codex: no private renderer for '{}', using the shared one: {}", ForgeRegistries.ENTITY_TYPES.getKey(entityType), e.toString());
            }
        }
        return mc.getEntityRenderDispatcher().getRenderer(entity);
    }

    /**
     * Swap the page copy's controllers for one that plays the arrival clip from the start, then loops {@code then} (or
     * holds the last frame). Returns the arrival clip's length in ticks, 0 if it could not be set up.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private double playOnArrival() {
        if (!(entity instanceof GeoAnimatable geo) || !(renderer instanceof GeoRenderer geoRenderer)) return 0;   // GeckoLib-only (warned in render)
        var clips = GeckoLibCache.getBakedAnimations().get(geoRenderer.getGeoModel().getAnimationResource(geo));
        String main = clip(clips, options.animation()), next = options.then() == null ? null : clip(clips, options.then());
        if (main == null || (options.then() != null && next == null)) {
            if (!warned) {
                warned = true;
                EidolonUnchained.LOGGER.warn("Codex: '{}' has no clip '{}'; its own animation plays. Clips: {}", ForgeRegistries.ENTITY_TYPES.getKey(entityType),
                        main == null ? options.animation() : options.then(), clips == null ? "none" : clips.animations().keySet());
            }
            return 0;
        }
        RawAnimation raw = next == null ? RawAnimation.begin().thenPlayAndHold(main) : RawAnimation.begin().thenPlay(main).thenLoop(next);
        AnimatableManager manager = geo.getAnimatableInstanceCache().getManagerForId(entity.getId());
        for (var name : new ArrayList<>(((Map<String, ?>) manager.getAnimationControllers()).keySet())) manager.removeController(name);
        manager.addController(new AnimationController(geo, "eu_codex_page", 0, state -> ((AnimationState) state).setAndContinue(raw)));
        return clips.getAnimation(main).length();
    }

    /** {@code 'manifest'} finds {@code 'animation.myrkul.manifest'}; a full clip name is used as is. */
    private static String clip(BakedAnimations clips, String name) {
        if (clips == null) return null;
        if (clips.animations().containsKey(name)) return name;
        for (var key : clips.animations().keySet()) if (key.endsWith("." + name)) return key;
        return null;
    }

    private static Map<EntityType<?>, EntityRendererProvider<?>> providers;

    /** {@code EntityRenderers.PROVIDERS}, found by type rather than by name so it works with any mappings. */
    @SuppressWarnings("unchecked")
    private static Map<EntityType<?>, EntityRendererProvider<?>> providers() throws ReflectiveOperationException {
        if (providers != null) return providers;
        for (Field f : EntityRenderers.class.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers()) || !Map.class.isAssignableFrom(f.getType())) continue;
            f.setAccessible(true);
            var map = (Map<?, ?>) f.get(null);
            if (!map.isEmpty() && map.keySet().iterator().next() instanceof EntityType<?>) {
                return providers = (Map<EntityType<?>, EntityRendererProvider<?>>) map;
            }
        }
        throw new NoSuchFieldException("EntityRenderers provider map");
    }
}
