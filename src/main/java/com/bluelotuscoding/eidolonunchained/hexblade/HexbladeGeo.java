package com.bluelotuscoding.eidolonunchained.hexblade;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.keyframe.event.data.ParticleKeyframeData;

import java.util.function.Consumer;

/**
 * The optional GeckoLib look of a scripted hexblade ({@code .geoModel('ns:name')}). The files, all under the item's
 * resource pack namespace {@code ns}:
 * <ul>
 *   <li>{@code geo/item/<name>.geo.json} and {@code animations/item/<name>.animation.json} with the clips
 *       {@code animation.<name>.awaken|awakened|sleep|dormant};</li>
 *   <li>{@code textures/item/<name>_geo.png}, plus {@code <name>_geo_glowmask.png} when parts glow;</li>
 *   <li>{@code models/item/<name>_geo.json}: the hand/world display transforms ({@code parent: builtin/entity}).</li>
 * </ul>
 * The item's own {@code texture(...)} stays the flat icon in GUI slots.
 */
public final class HexbladeGeo {
    private HexbladeGeo() {
    }

    /**
     * Receives the particle keyframes ({@code particle_effects}: effect + locator) of the clip being played. GeckoLib fires
     * them while the client renderer animates a stack, so the renderer sets this and spawns them at the locator.
     */
    public static Consumer<ParticleKeyframeData> particleSink = data -> {
    };

    public static ResourceLocation geo(ResourceLocation id) {
        return new ResourceLocation(id.getNamespace(), "geo/item/" + id.getPath() + ".geo.json");
    }

    public static ResourceLocation animations(ResourceLocation id) {
        return new ResourceLocation(id.getNamespace(), "animations/item/" + id.getPath() + ".animation.json");
    }

    public static ResourceLocation texture(ResourceLocation id) {
        return new ResourceLocation(id.getNamespace(), "textures/item/" + id.getPath() + "_geo.png");
    }

    public static ResourceLocation glowmask(ResourceLocation id) {
        return new ResourceLocation(id.getNamespace(), "textures/item/" + id.getPath() + "_geo_glowmask.png");
    }

    /** The item model that carries the display transforms (models/item/<name>_geo.json). */
    public static ResourceLocation displayModel(ResourceLocation id) {
        return new ResourceLocation(id.getNamespace(), "item/" + id.getPath() + "_geo");
    }

    /**
     * One controller per stack (GeckoLib keeps a manager per stack id), driven by the awakened flag of the stack being
     * rendered: dormant→awakened plays {@code awaken} then loops {@code awakened}; awakened→dormant plays {@code sleep}
     * then loops {@code dormant}. The first time a stack is seen it starts in its loop, so an already awakened blade
     * does not replay the transformation when it comes into view.
     */
    public static AnimationController<HexbladeItem> controller(HexbladeItem item, ResourceLocation id) {
        String name = id.getPath().substring(id.getPath().lastIndexOf('/') + 1);
        String clip = "animation." + name + ".";
        RawAnimation awaken = RawAnimation.begin().thenPlay(clip + "awaken").thenLoop(clip + "awakened");
        RawAnimation sleep = RawAnimation.begin().thenPlay(clip + "sleep").thenLoop(clip + "dormant");
        RawAnimation awakened = RawAnimation.begin().thenLoop(clip + "awakened");
        RawAnimation dormant = RawAnimation.begin().thenLoop(clip + "dormant");
        Boolean[] first = {null};
        boolean[] changed = {false};
        return new AnimationController<>(item, "awakening", 3, state -> {
            ItemStack stack = state.getData(DataTickets.ITEMSTACK);
            boolean awake = stack != null && HexbladeItem.isAwakened(stack);
            if (first[0] == null) first[0] = awake;
            else if (awake != first[0]) changed[0] = true;
            return state.setAndContinue(changed[0] ? (awake ? awaken : sleep) : (awake ? awakened : dormant));
        }).setParticleKeyframeHandler(event -> particleSink.accept(event.getKeyframeData()));
    }
}
