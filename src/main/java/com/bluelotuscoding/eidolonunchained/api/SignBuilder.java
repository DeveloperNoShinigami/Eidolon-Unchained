package com.bluelotuscoding.eidolonunchained.api;

import elucent.eidolon.api.spells.Sign;
import elucent.eidolon.registries.Signs;
import net.minecraft.resources.ResourceLocation;

/**
 * {@code EidolonUnchained.sign(id).sprite(rl).color(...)} — ends in {@code Signs.register(new Sign(id, sprite, color))}.
 * <p>
 * The sprite is a block-atlas sprite id (Eidolon draws signs from {@code InventoryMenu.BLOCK_ATLAS}). Eidolon Unchained
 * adds every declared sprite to the atlas through KubeJS's generated {@code atlases/blocks.json}, so a script pack only
 * needs the PNG at {@code kubejs/assets/<ns>/textures/<path>.png}.
 */
public final class SignBuilder {
    final ResourceLocation id;
    ResourceLocation sprite;
    Integer color;

    SignBuilder(ResourceLocation id) {
        this.id = id;
        EURegistry.declare(EURegistry.Stage.SIGNS, id, "sign", this::register);
        SpriteSources.add(this::spriteOrNull);
    }

    /** Block-atlas sprite id, e.g. {@code 'mypack:particle/storm_sign'} (texture at textures/particle/storm_sign.png). */
    public SignBuilder sprite(String spriteId) {
        this.sprite = Ids.of(spriteId, "sign sprite");
        return this;
    }

    public SignBuilder color(int r, int g, int b) {
        this.color = Ids.rgb(r, g, b);
        return this;
    }

    public SignBuilder color(int packedRgb) {
        this.color = Ids.rgb(packedRgb);
        return this;
    }

    ResourceLocation spriteOrNull() {
        return sprite;
    }

    private void register(ResourceLocation id) {
        if (sprite == null) throw new IllegalStateException("sign '" + id + "' has no .sprite(...)");
        if (color == null) throw new IllegalStateException("sign '" + id + "' has no .color(...)");
        if (Signs.find(id) != null) throw new IllegalStateException("a sign with id '" + id + "' already exists");
        Signs.register(new Sign(id, sprite, color));
    }
}
