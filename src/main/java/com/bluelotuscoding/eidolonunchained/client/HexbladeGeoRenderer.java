package com.bluelotuscoding.eidolonunchained.client;

import com.bluelotuscoding.eidolonunchained.hexblade.HexbladeGeo;
import com.bluelotuscoding.eidolonunchained.hexblade.HexbladeItem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * Renders a scripted hexblade that has {@code .geoModel(...)} in hand, on the ground, in item frames and on other
 * players and mobs. GUI slots never reach it: the generated item model shows the flat icon there (see
 * {@code HexbladeItemBuilder#generateAssetJsons}). Parts in {@code <name>_geo_glowmask.png} glow when that file exists.
 */
public class HexbladeGeoRenderer extends GeoItemRenderer<HexbladeItem> {
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
    }
}
