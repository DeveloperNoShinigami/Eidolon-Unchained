package com.bluelotuscoding.eidolonunchained.api;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import elucent.eidolon.api.spells.Rune;
import elucent.eidolon.api.spells.SignSequence;
import elucent.eidolon.registries.Runes;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

/**
 * {@code EidolonUnchained.rune(id).sprite(rl).effect(seq => 'pass' | 'fail')} — ends in
 * {@code Runes.register(new ScriptedRune(...))}. Eidolon's {@code Rune} is abstract: its {@code doEffect(SignSequence)}
 * decides whether the sequence passes the rune, so a scripted rune needs an effect. Without one it always passes.
 */
public final class RuneBuilder {
    final ResourceLocation id;
    ResourceLocation sprite;
    Function<SignSequence, Object> effect;

    RuneBuilder(ResourceLocation id) {
        this.id = id;
        EURegistry.declare(EURegistry.Stage.RUNES, id, "rune", this::register);
        SpriteSources.add(this::spriteOrNull);
    }

    /** Block-atlas sprite id. Defaults to {@code <ns>:rune/<path>}, as Eidolon's own runes do. */
    public RuneBuilder sprite(String spriteId) {
        this.sprite = Ids.of(spriteId, "rune sprite");
        return this;
    }

    /** Called with the sign sequence; return {@code 'pass'} / {@code true} or {@code 'fail'} / {@code false}. */
    public RuneBuilder effect(Function<SignSequence, Object> effect) {
        this.effect = effect;
        return this;
    }

    ResourceLocation spriteOrNull() {
        return sprite != null ? sprite : new ResourceLocation(id.getNamespace(), "rune/" + id.getPath());
    }

    private void register(ResourceLocation id) {
        if (Runes.find(id) != null) throw new IllegalStateException("a rune with id '" + id + "' already exists");
        Runes.register(new ScriptedRune(id, spriteOrNull(), effect));
    }

    /** EU abstraction: a Rune whose effect is a script function. */
    public static final class ScriptedRune extends Rune {
        private final Function<SignSequence, Object> effect;

        ScriptedRune(ResourceLocation id, ResourceLocation sprite, Function<SignSequence, Object> effect) {
            super(id, sprite);
            this.effect = effect;
        }

        @Override
        public RuneResult doEffect(SignSequence seq) {
            if (effect == null) return RuneResult.PASS;
            try {
                var r = effect.apply(seq);
                if (r instanceof Boolean b) return b ? RuneResult.PASS : RuneResult.FAIL;
                return r != null && r.toString().equalsIgnoreCase("fail") ? RuneResult.FAIL : RuneResult.PASS;
            } catch (RuntimeException e) {
                EidolonUnchained.LOGGER.error("rune '{}' effect threw: {}", getRegistryName(), e.toString());
                return RuneResult.FAIL;
            }
        }
    }
}
