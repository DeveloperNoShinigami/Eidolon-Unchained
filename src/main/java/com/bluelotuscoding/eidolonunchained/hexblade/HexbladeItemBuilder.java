package com.bluelotuscoding.eidolonunchained.hexblade;

import com.bluelotuscoding.eidolonunchained.api.Ids;
import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.generator.AssetJsonGenerator;
import dev.latvian.mods.kubejs.item.custom.HandheldItemBuilder;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;

/**
 * KubeJS item type {@code 'eidolonunchained:hexblade'} (also {@code 'hexblade'}): Hexblades Renewed's weapon system
 * with any deity. Everything a sword builder has (tier, attackDamageBaseline, speedBaseline, texture, displayName…)
 * plus the hexblade parts below. Registered in {@code StartupEvents.registry('item', …)} like any KubeJS item.
 */
public class HexbladeItemBuilder extends HandheldItemBuilder {
    public transient final HexbladeItem.Settings settings = new HexbladeItem.Settings();

    public HexbladeItemBuilder(ResourceLocation id) {
        super(id, 3F, -2.4F);
        maxDamage = 1250;                 // the energy pool (Hexblades' patron tier: 1250)
    }

    @Info("""
            GeckoLib look 'ns:name': the 3D model everywhere, GUI slots included (texture() is then only the break particle).
            Files: geo/item/name.geo.json, animations/item/name.animation.json (clips awaken, awakened, sleep, dormant),
            textures/item/name_geo.png (+ name_geo_glowmask.png to glow), models/item/name_geo.json (display transforms)""")
    public HexbladeItemBuilder geoModel(String id) {
        settings.geoModel = new ResourceLocation(id);
        return this;
    }

    /** With geoModel: the item model is the display model (builtin/entity), so the GeckoLib renderer draws every view, GUI included. */
    @Override
    public void generateAssetJsons(AssetJsonGenerator generator) {
        if (settings.geoModel == null || modelJson != null) {
            super.generateAssetJsons(generator);
            return;
        }
        var model = new JsonObject();
        model.addProperty("parent", HexbladeGeo.displayModel(settings.geoModel).toString());
        var particle = new JsonObject();
        particle.addProperty("particle", textureJson.has("layer0") ? textureJson.get("layer0").getAsString()
                : settings.geoModel.getNamespace() + ":item/" + settings.geoModel.getPath() + "_geo");
        model.add("textures", particle);
        generator.json(AssetJsonGenerator.asItemModelLocation(id), model);
    }

    @Info("The deity the blade is bound to: devotion (reputation) with it scales the awakened powers")
    public HexbladeItemBuilder deity(String deityId) {
        settings.deity = Ids.of(deityId, "deity");
        return this;
    }

    @Info("Energy recharged per tick while dormant (default 5); also the reserve kept when the blade sleeps")
    public HexbladeItemBuilder rechargePerTick(int energy) {
        settings.rechargePerTick = Math.max(0, energy);
        return this;
    }

    @Info("Energy drained per tick while awakened (default 2)")
    public HexbladeItemBuilder drainPerTick(int amount) {
        settings.drainPerTick = Math.max(0, amount);
        return this;
    }

    @Info("Energy restored per hit (default 10)")
    public HexbladeItemBuilder hitEnergy(int amount) {
        settings.hitEnergy = Math.max(0, amount);
        return this;
    }

    @Info("Awakened hits deal the blade's elemental power as this divine damage (D56: opt-in), e.g. 'eu_examples:necrotic'")
    public HexbladeItemBuilder divineDamage(String damageId) {
        settings.divineDamage = Ids.of(damageId, "divine damage");
        return this;
    }

    @Info("Elemental power = devotion / ratio while awakened (default 10; 0 disables)")
    public HexbladeItemBuilder elementalRatio(double ratio) {
        settings.elementalRatio = ratio;
        return this;
    }

    @Info("(devotion) => extra attack damage while awakened (default devotion / 10)")
    public HexbladeItemBuilder awakenedDamage(HexbladeItem.DevotionFn fn) {
        settings.awakenedDamage = fn;
        return this;
    }

    @Info("(devotion) => extra attack speed while awakened (default 0)")
    public HexbladeItemBuilder awakenedSpeed(HexbladeItem.DevotionFn fn) {
        settings.awakenedSpeed = fn;
        return this;
    }

    @Info("(stack, target, attacker, awakened) => …; runs on every hit, awakened or not")
    public HexbladeItemBuilder onHit(HexbladeItem.HitFn fn) {
        settings.onHit = fn;
        return this;
    }

    @Info("(player, stack, awakened) => …; every tick while held (Hexblades' hex bonus)")
    public HexbladeItemBuilder whileHeld(HexbladeItem.HeldFn fn) {
        settings.whileHeld = fn;
        return this;
    }

    @Info("Lines the blade says (one at random) when it awakens")
    public HexbladeItemBuilder dialogue(String... lines) {
        settings.dialogue = List.of(lines);
        return this;
    }

    @Info("Italic flavour text in the blade's colour")
    public HexbladeItemBuilder flavor(String... lines) {
        var out = new ArrayList<Component>();
        for (var l : lines) out.add(Component.literal(l));
        settings.flavor = out;
        return this;
    }

    @Info("Extra lines shown while Shift is held")
    public HexbladeItemBuilder awakenedTooltip(String... lines) {
        var out = new ArrayList<Component>();
        for (var l : lines) out.add(Component.literal(l));
        settings.awakenedTooltip = out;
        return this;
    }

    @Info("Text colour for flavour and dialogue (0xRRGGBB)")
    public HexbladeItemBuilder textColor(int rgb) {
        settings.textColor = rgb & 0xFFFFFF;
        return this;
    }

    @Info("Hits ignore invulnerability frames (Hexblades' behaviour; default true)")
    public HexbladeItemBuilder noInvulnerabilityFrames(boolean value) {
        settings.noInvulnerabilityFrames = value;
        return this;
    }

    public HexbladeItemBuilder talkOnAwaken(boolean value) {
        settings.talkOnAwaken = value;
        return this;
    }

    @Override
    public Item createObject() {
        return new HexbladeItem(settings, toolTier, (int) attackDamageBaseline, speedBaseline, createItemProperties());
    }
}
