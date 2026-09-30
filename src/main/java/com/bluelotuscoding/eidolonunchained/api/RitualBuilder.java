package com.bluelotuscoding.eidolonunchained.api;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import dev.latvian.mods.kubejs.typings.Info;
import elucent.eidolon.api.ritual.FocusItemRequirement;
import elucent.eidolon.api.ritual.HealthRequirement;
import elucent.eidolon.api.ritual.IRequirement;
import elucent.eidolon.api.ritual.ItemRequirement;
import elucent.eidolon.api.ritual.Ritual;
import elucent.eidolon.registries.RitualRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * {@code EidolonUnchained.ritual(id).symbol(rl).color(rgb).require(...).invariant(...).onComplete(fn)} — ends in
 * {@code RitualRegistry.register(id, new ScriptedRitual(...))}. Requirements are Eidolon's {@code ItemRequirement},
 * {@code HealthRequirement} and {@code FocusItemRequirement}. The brazier reaches a ritual through a
 * {@code eidolon:ritual_brazier} recipe naming it (see the recipe schemas), which is how Eidolon's own rituals work.
 * <p>
 * Script hooks: {@code onStart(level, pos)} runs when the requirements are all met; {@code onTick(level, pos)} runs
 * every tick after that and returns {@code 'pass'} to keep going or {@code 'terminate'} to end; {@code onComplete}
 * is the one-shot form: run the effect on start and terminate.
 */
public final class RitualBuilder {
    public interface RitualHook extends BiConsumer<Level, BlockPos> {
    }

    public interface RitualTick extends BiFunction<Level, BlockPos, Object> {
    }

    final ResourceLocation id;
    ResourceLocation symbol;
    Integer color;
    final List<Supplier<IRequirement>> requirements = new ArrayList<>();
    final List<Supplier<IRequirement>> invariants = new ArrayList<>();
    RitualHook onStart;
    RitualTick onTick;
    RitualHook onComplete;

    RitualBuilder(ResourceLocation id) {
        this.id = id;
        EURegistry.declare(EURegistry.Stage.RITUALS, id, "ritual", this::register);
    }

    @Info("Symbol texture id shown over the brazier, e.g. 'mypack:textures/rituals/storm.png'")
    public RitualBuilder symbol(String textureId) {
        this.symbol = Ids.of(textureId, "ritual symbol");
        return this;
    }

    public RitualBuilder color(int r, int g, int b) {
        this.color = Ids.rgb(r, g, b);
        return this;
    }

    public RitualBuilder color(int packedRgb) {
        this.color = Ids.rgb(packedRgb);
        return this;
    }

    @Info("An item on a pedestal: item id or '#tag'; count is how many pedestals must hold it")
    public RitualBuilder require(String itemOrTag, int count) {
        for (int i = 0; i < Math.max(1, count); i++) requirements.add(itemRequirement(itemOrTag));
        return this;
    }

    public RitualBuilder require(String itemOrTag) {
        return require(itemOrTag, 1);
    }

    @Info("Health the performing player sacrifices")
    public RitualBuilder requireHealth(float health) {
        requirements.add(() -> new HealthRequirement(health));
        return this;
    }

    @Info("An item that must be on the ritual focus: item id or '#tag'")
    public RitualBuilder requireFocus(String itemOrTag) {
        requirements.add(focusRequirement(itemOrTag));
        return this;
    }

    @Info("An item that must stay present for the whole ritual")
    public RitualBuilder invariant(String itemOrTag) {
        invariants.add(itemRequirement(itemOrTag));
        return this;
    }

    public RitualBuilder onStart(RitualHook fn) {
        this.onStart = fn;
        return this;
    }

    @Info("(level, pos) => 'pass' | 'terminate', every tick after start")
    public RitualBuilder onTick(RitualTick fn) {
        this.onTick = fn;
        return this;
    }

    @Info("One-shot: (level, pos) => …, then the ritual ends")
    public RitualBuilder onComplete(RitualHook fn) {
        this.onComplete = fn;
        return this;
    }

    private static Supplier<IRequirement> itemRequirement(String itemOrTag) {
        if (itemOrTag.startsWith("#")) {
            var tag = TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(), Ids.of(itemOrTag.substring(1), "item tag"));
            return () -> new ItemRequirement(tag);
        }
        var rl = Ids.of(itemOrTag, "item");
        return () -> {
            var item = ForgeRegistries.ITEMS.getValue(rl);
            if (item == null || !ForgeRegistries.ITEMS.containsKey(rl)) throw new IllegalStateException("unknown item '" + rl + "'");
            return new ItemRequirement(item);
        };
    }

    private static Supplier<IRequirement> focusRequirement(String itemOrTag) {
        if (itemOrTag.startsWith("#")) {
            TagKey<Item> tag = TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(), Ids.of(itemOrTag.substring(1), "item tag"));
            return () -> new FocusItemRequirement(tag);
        }
        var rl = Ids.of(itemOrTag, "item");
        return () -> {
            var item = ForgeRegistries.ITEMS.getValue(rl);
            if (item == null || !ForgeRegistries.ITEMS.containsKey(rl)) throw new IllegalStateException("unknown item '" + rl + "'");
            return new FocusItemRequirement(item);
        };
    }

    private void register(ResourceLocation id) {
        if (symbol == null) throw new IllegalStateException("ritual '" + id + "' has no .symbol(...)");
        if (color == null) throw new IllegalStateException("ritual '" + id + "' has no .color(...)");
        if (onStart == null && onTick == null && onComplete == null) throw new IllegalStateException("ritual '" + id + "' has no .onComplete / .onStart / .onTick");
        if (RitualRegistry.find(id) != null) throw new IllegalStateException("a ritual with id '" + id + "' already exists");
        var ritual = new ScriptedRitual(symbol, color, onStart, onTick, onComplete);
        for (var r : requirements) ritual.addRequirement(r.get());
        for (var r : invariants) ritual.addInvariant(r.get());
        RitualRegistry.register(id, ritual);
    }

    /** EU abstraction: a Ritual whose start/tick bodies are script functions. Clones share the functions. */
    public static final class ScriptedRitual extends Ritual {
        private final RitualHook onStart;
        private final RitualTick onTick;
        private final RitualHook onComplete;

        ScriptedRitual(ResourceLocation symbol, int color, RitualHook onStart, RitualTick onTick, RitualHook onComplete) {
            super(symbol, color);
            this.onStart = onStart;
            this.onTick = onTick;
            this.onComplete = onComplete;
        }

        @Override
        public Ritual cloneRitual() {
            var copy = new ScriptedRitual(getSymbol(), getColor(), onStart, onTick, onComplete);
            copy.addRequirements(getRequirements());
            return copy;
        }

        @Override
        public RitualResult start(Level level, BlockPos pos) {
            try {
                if (onComplete != null) {
                    onComplete.accept(level, pos);
                    return RitualResult.TERMINATE;
                }
                if (onStart != null) onStart.accept(level, pos);
                return onTick == null ? RitualResult.TERMINATE : RitualResult.PASS;
            } catch (RuntimeException e) {
                EidolonUnchained.LOGGER.error("ritual '{}' start threw: {}", getRegistryName(), e.toString());
                return RitualResult.TERMINATE;
            }
        }

        @Override
        public RitualResult tick(Level level, BlockPos pos) {
            if (onTick == null) return RitualResult.TERMINATE;
            try {
                var r = onTick.apply(level, pos);
                if (r instanceof Boolean b) return b ? RitualResult.PASS : RitualResult.TERMINATE;
                return r != null && r.toString().equalsIgnoreCase("terminate") ? RitualResult.TERMINATE : RitualResult.PASS;
            } catch (RuntimeException e) {
                EidolonUnchained.LOGGER.error("ritual '{}' tick threw: {}", getRegistryName(), e.toString());
                return RitualResult.TERMINATE;
            }
        }
    }
}
