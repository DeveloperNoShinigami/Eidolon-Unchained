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
 * <p>
 * Patron rites (D47): {@code .grantsPatron(...)} pledges the performer (the nearest player) and {@code .revokePatron(...)}
 * ends a pledge when the ritual completes. A refusal ({@code .requires} failing, or a pledge the performer can't make)
 * happens at the first setup step, before any pedestal item is taken, and the burned reagent is given back.
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
    final List<ResourceLocation> grants = new ArrayList<>();
    boolean revoke;
    ResourceLocation revokeDeity;
    double revokeReputation;

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

    @Info("Pledges the performer (the nearest player) to these deities when the ritual completes. At most one may be a major (patronRequired) god; if the performer already follows a different major god the ritual is refused and the reagents are kept")
    public RitualBuilder grantsPatron(String... deityIds) {
        if (deityIds.length == 0) throw new IllegalArgumentException("Eidolon Unchained: ritual '" + id + "': grantsPatron needs at least one deity");
        for (var d : deityIds) grants.add(Ids.of(d, "deity"));
        return this;
    }

    @Info("Ends every pledge of the performer when the ritual completes; their reputation with each is left at 0")
    public RitualBuilder revokePatron() {
        this.revoke = true;
        this.revokeDeity = null;
        this.revokeReputation = 0;
        return this;
    }

    @Info("Ends the performer's pledge to this deity; their reputation with it is left at 0")
    public RitualBuilder revokePatron(String deityId) {
        return revokePatron(deityId, 0);
    }

    @Info("Ends the performer's pledge to this deity, leaving their reputation with it at this value (negative allowed: a grudge)")
    public RitualBuilder revokePatron(String deityId, double reputation) {
        this.revoke = true;
        this.revokeDeity = Ids.of(deityId, "deity");
        this.revokeReputation = reputation;
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
        if (color == null) throw new IllegalStateException("ritual '" + id + "' has no .color(...)");
        boolean patronRite = !grants.isEmpty() || revoke;
        if (onStart == null && onTick == null && onComplete == null && !patronRite)
            throw new IllegalStateException("ritual '" + id + "' has no .onComplete / .onStart / .onTick / .grantsPatron / .revokePatron");
        if (RitualRegistry.find(id) != null) throw new IllegalStateException("a ritual with id '" + id + "' already exists");
        int major = 0;
        for (var d : grants) {
            if (elucent.eidolon.common.deity.Deities.find(d) == null) throw new IllegalStateException("ritual '" + id + "': grantsPatron: unknown deity '" + d + "'");
            if (com.bluelotuscoding.eidolonunchained.patron.Patrons.isRequired(d)) major++;
        }
        if (major > 1) throw new IllegalStateException("ritual '" + id + "': grantsPatron lists " + major + " major gods; a player can follow only one");
        if (revokeDeity != null && elucent.eidolon.common.deity.Deities.find(revokeDeity) == null)
            throw new IllegalStateException("ritual '" + id + "': revokePatron: unknown deity '" + revokeDeity + "'");
        var rite = patronRite ? new PatronRite(List.copyOf(grants), revoke, revokeDeity, revokeReputation) : null;
        var ritual = new ScriptedRitual(symbolSprite(), color, onStart, onTick, onComplete, requires, rite);
        for (var r : requirements) ritual.addRequirement(r.get());
        for (var r : invariants) ritual.addInvariant(r.get());
        RitualRegistry.register(id, ritual);
    }

    /** What a ritual does to the performer's pledges: revoke first, then grant (so one rite can also convert). */
    record PatronRite(List<ResourceLocation> grants, boolean revoke, ResourceLocation revokeDeity, double revokeReputation) {
        private boolean revokes(net.minecraft.server.level.ServerPlayer player, ResourceLocation deity) {
            return revoke && (revokeDeity == null || revokeDeity.equals(deity))
                    && com.bluelotuscoding.eidolonunchained.patron.Patrons.pledged(player, deity);
        }

        /** Why the performer can't go through with this rite, or null. Nothing changes. */
        net.minecraft.network.chat.Component refusal(net.minecraft.server.level.ServerPlayer player) {
            var major = com.bluelotuscoding.eidolonunchained.patron.Patrons.majorPatron(player);
            var majorAfter = major != null && revokes(player, major) ? null : major;
            for (var d : grants) {
                if (com.bluelotuscoding.eidolonunchained.patron.Patrons.requiresCalling(d) && !com.bluelotuscoding.eidolonunchained.patron.Callings.wasCalled(player, d))
                    return net.minecraft.network.chat.Component.translatable("eidolonunchained.ritual.not_called", com.bluelotuscoding.eidolonunchained.patron.Patrons.deityName(d));
                if (com.bluelotuscoding.eidolonunchained.patron.Patrons.pledged(player, d) && !revokes(player, d))
                    return net.minecraft.network.chat.Component.translatable("eidolonunchained.ritual.already_follower", com.bluelotuscoding.eidolonunchained.patron.Patrons.deityName(d));
                if (com.bluelotuscoding.eidolonunchained.patron.Patrons.isRequired(d) && majorAfter != null && !majorAfter.equals(d))
                    return net.minecraft.network.chat.Component.translatable("eidolonunchained.ritual.jealous",
                            com.bluelotuscoding.eidolonunchained.patron.Patrons.deityName(majorAfter), com.bluelotuscoding.eidolonunchained.patron.Patrons.deityName(d));
            }
            if (revoke && grants.isEmpty()) {
                boolean any = revokeDeity == null ? !com.bluelotuscoding.eidolonunchained.patron.Patrons.pledges(player).isEmpty()
                        : com.bluelotuscoding.eidolonunchained.patron.Patrons.pledged(player, revokeDeity);
                if (!any) return revokeDeity == null
                        ? net.minecraft.network.chat.Component.translatable("eidolonunchained.ritual.no_patron")
                        : net.minecraft.network.chat.Component.translatable("eidolonunchained.ritual.not_follower", com.bluelotuscoding.eidolonunchained.patron.Patrons.deityName(revokeDeity));
            }
            return null;
        }

        void apply(net.minecraft.server.level.ServerPlayer player) {
            if (revoke) {
                for (var d : com.bluelotuscoding.eidolonunchained.patron.Patrons.revoke(player, revokeDeity, revokeReputation))
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("eidolonunchained.ritual.revoked", com.bluelotuscoding.eidolonunchained.patron.Patrons.deityName(d)));
            }
            for (var d : grants) {
                var why = com.bluelotuscoding.eidolonunchained.patron.Patrons.pledge(player, d);
                player.sendSystemMessage(why != null ? why : net.minecraft.network.chat.Component.translatable("eidolonunchained.ritual.pledged", com.bluelotuscoding.eidolonunchained.patron.Patrons.deityName(d)));
            }
        }
    }

    /** EU abstraction: a Ritual whose start/tick bodies are script functions. Clones share the functions. */
    public static final class ScriptedRitual extends Ritual {
        private final RitualHook onStart;
        private final RitualTick onTick;
        private final RitualHook onComplete;
        private final com.bluelotuscoding.eidolonunchained.api.condition.Condition requires;
        private final PatronRite rite;
        private static final java.util.Set<String> REFUSED = java.util.concurrent.ConcurrentHashMap.newKeySet();

        ScriptedRitual(ResourceLocation symbol, int color, RitualHook onStart, RitualTick onTick, RitualHook onComplete,
                       com.bluelotuscoding.eidolonunchained.api.condition.Condition requires, PatronRite rite) {
            super(symbol, color);
            this.onStart = onStart;
            this.onTick = onTick;
            this.onComplete = onComplete;
            this.requires = requires;
            this.rite = rite;
        }

        private static net.minecraft.world.entity.player.Player performer(Level level, BlockPos pos) {
            return level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 16, false);
        }

        private static String key(Level level, BlockPos pos) {
            return level.dimension().location() + "|" + pos.asLong();
        }

        /** The brazier mixin asks this before reporting a completion: true (once) when this ritual refused to start there. */
        public static boolean consumeRefusal(Level level, BlockPos pos) {
            return REFUSED.remove(key(level, pos));
        }

        /** Why this ritual won't start for the performer (conditions, then the patron rite), or null. */
        private net.minecraft.network.chat.Component refusal(Level level, BlockPos pos, net.minecraft.world.entity.player.Player player) {
            if (requires != null) {
                var ctx = new com.bluelotuscoding.eidolonunchained.api.condition.EventContext("ritual").at(level, pos)
                        .id(getRegistryName()).with("ritual", getRegistryName()).player(player);
                if (!requires.matches(ctx)) return net.minecraft.network.chat.Component.translatable("eidolonunchained.ritual.refused");
            }
            if (rite != null) {
                if (!(player instanceof net.minecraft.server.level.ServerPlayer sp)) return net.minecraft.network.chat.Component.translatable("eidolonunchained.ritual.no_performer");
                return rite.refusal(sp);
            }
            return null;
        }

        /**
         * Refusals happen here, at the first setup step: Eidolon takes a pedestal item at each step and burned the reagent
         * when the recipe matched, so refusing later (at start) would cost the player everything. The pedestals stay as
         * they are, the reagent is given back as an item over the brazier, and the brazier goes out.
         */
        @Override
        public SetupResult setup(Level level, BlockPos pos, int step) {
            if (step == 0 && !level.isClientSide() && (requires != null || rite != null)) {
                var player = performer(level, pos);
                var why = refusal(level, pos, player);
                if (why != null) {
                    if (player != null) player.displayClientMessage(why, true);
                    refundReagent(level, pos);
                    return SetupResult.FAIL;
                }
            }
            return super.setup(level, pos, step);
        }

        /** The ritual's brazier recipe carries the ritual's id (EU's recipe schema), so its reagent is known. */
        private void refundReagent(Level level, BlockPos pos) {
            level.getRecipeManager().byKey(getRegistryName()).ifPresent(r -> {
                if (r instanceof elucent.eidolon.recipe.RitualRecipe rr && !rr.reagent.isEmpty()) {
                    var items = rr.reagent.getItems();
                    if (items.length > 0) {
                        var drop = new net.minecraft.world.entity.item.ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, items[0].copy());
                        drop.setDefaultPickUpDelay();
                        level.addFreshEntity(drop);
                    }
                }
            });
        }

        @Override
        public Ritual cloneRitual() {
            var copy = new ScriptedRitual(getSymbol(), getColor(), onStart, onTick, onComplete, requires, rite);
            copy.addRequirements(getRequirements());
            return copy;
        }

        @Override
        public RitualResult start(Level level, BlockPos pos) {
            if (!level.isClientSide() && rite != null) {
                // checked again: the performer may have changed their pledges while the ritual set up
                var player = performer(level, pos);
                var why = refusal(level, pos, player);
                if (why != null) {
                    if (player != null) player.displayClientMessage(why, true);
                    REFUSED.add(key(level, pos));
                    return RitualResult.TERMINATE;
                }
                rite.apply((net.minecraft.server.level.ServerPlayer) player);
            }
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
