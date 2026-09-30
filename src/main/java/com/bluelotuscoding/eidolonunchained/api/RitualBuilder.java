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
    com.bluelotuscoding.eidolonunchained.api.condition.Condition requires;

    RitualBuilder(ResourceLocation id) {
        this.id = id;
        EURegistry.declare(EURegistry.Stage.RITUALS, id, "ritual", this::register);
    }

    /**
     * Block-atlas sprite id of the symbol. Eidolon's own atlas file stitches every pack's {@code textures/vfx/} folder
     * under {@code particle/}, so {@code assets/<ns>/textures/vfx/<name>.png} is the sprite {@code <ns>:particle/<name>}
     * with nothing to register. Defaults to {@code <ns>:particle/<ritual path>}.
     */
    ResourceLocation symbolSprite() {
        return symbol != null ? symbol : new ResourceLocation(id.getNamespace(), "particle/" + id.getPath());
    }

    @Info("The symbol drawn over the brazier and on the codex ritual page, as Eidolon names them: 'mypack:particle/storm' for the texture assets/mypack/textures/vfx/storm.png (Eidolon stitches that folder itself), or one of Eidolon's, e.g. 'eidolon:particle/allure_ritual'. Optional: defaults to '<ns>:particle/<ritual path>'")
    public RitualBuilder symbol(String spriteId) {
        var s = spriteId.trim();
        // accept a texture path by mistake: 'ns:textures/x/y.png' -> 'ns:x/y'
        int colon = s.indexOf(':');
        String ns = colon < 0 ? null : s.substring(0, colon);
        String path = colon < 0 ? s : s.substring(colon + 1);
        if (path.startsWith("textures/")) path = path.substring("textures/".length());
        if (path.endsWith(".png")) path = path.substring(0, path.length() - 4);
        if (path.startsWith("vfx/")) path = "particle/" + path.substring(4);   // the texture folder name → Eidolon's sprite prefix
        this.symbol = Ids.of(ns == null ? path : ns + ":" + path, "ritual symbol");
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

    @Info("A condition (EidolonUnchained.conditions) that must hold when the ritual starts; tested with the nearest player within 16 blocks")
    public RitualBuilder requires(com.bluelotuscoding.eidolonunchained.api.condition.Condition condition) {
        this.requires = condition;
        return this;
    }

    @Info("The same with a plain function: ctx => boolean (ctx.player = nearest player, ctx.level, ctx.pos = the brazier)")
    public RitualBuilder requires(com.bluelotuscoding.eidolonunchained.api.condition.Conditions.ContextTest fn) {
        return requires(com.bluelotuscoding.eidolonunchained.api.condition.Condition.of(fn));
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
        if (color == null) throw new IllegalStateException("ritual '" + id + "' has no .color(...)");
        if (onStart == null && onTick == null && onComplete == null) throw new IllegalStateException("ritual '" + id + "' has no .onComplete / .onStart / .onTick");
        if (RitualRegistry.find(id) != null) throw new IllegalStateException("a ritual with id '" + id + "' already exists");
        var ritual = new ScriptedRitual(symbolSprite(), color, onStart, onTick, onComplete, requires);
        for (var r : requirements) ritual.addRequirement(r.get());
        for (var r : invariants) ritual.addInvariant(r.get());
        RitualRegistry.register(id, ritual);
    }

    /** EU abstraction: a Ritual whose start/tick bodies are script functions. Clones share the functions. */
    public static final class ScriptedRitual extends Ritual {
        private final RitualHook onStart;
        private final RitualTick onTick;
        private final RitualHook onComplete;
        private final com.bluelotuscoding.eidolonunchained.api.condition.Condition requires;
        private static final java.util.Set<String> REFUSED = java.util.concurrent.ConcurrentHashMap.newKeySet();

        ScriptedRitual(ResourceLocation symbol, int color, RitualHook onStart, RitualTick onTick, RitualHook onComplete,
                       com.bluelotuscoding.eidolonunchained.api.condition.Condition requires) {
            super(symbol, color);
            this.onStart = onStart;
            this.onTick = onTick;
            this.onComplete = onComplete;
            this.requires = requires;
        }

        private static String key(Level level, BlockPos pos) {
            return level.dimension().location() + "|" + pos.asLong();
        }

        /** The brazier mixin asks this before reporting a completion: true (once) when this ritual refused to start there. */
        public static boolean consumeRefusal(Level level, BlockPos pos) {
            return REFUSED.remove(key(level, pos));
        }

        private boolean refused(Level level, BlockPos pos) {
            if (requires == null || level.isClientSide()) return false;
            var player = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 16, false);
            var ctx = new com.bluelotuscoding.eidolonunchained.api.condition.EventContext("ritual").at(level, pos)
                    .id(getRegistryName()).with("ritual", getRegistryName()).player(player);
            if (requires.matches(ctx)) return false;
            if (player != null) player.displayClientMessage(net.minecraft.network.chat.Component.translatable("eidolonunchained.ritual.refused"), true);
            REFUSED.add(key(level, pos));
            return true;
        }

        @Override
        public Ritual cloneRitual() {
            var copy = new ScriptedRitual(getSymbol(), getColor(), onStart, onTick, onComplete, requires);
            copy.addRequirements(getRequirements());
            return copy;
        }

        @Override
        public RitualResult start(Level level, BlockPos pos) {
            if (refused(level, pos)) return RitualResult.TERMINATE;
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
