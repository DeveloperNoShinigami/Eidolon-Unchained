package com.bluelotuscoding.eidolonunchained.api;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import elucent.eidolon.api.spells.SignSequence;
import elucent.eidolon.capability.IReputation;
import elucent.eidolon.common.spell.StaticSpell;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * EU abstraction: a {@link StaticSpell} whose {@code canCast} / {@code cast} bodies are script functions. Extending
 * Eidolon's own {@code StaticSpell} keeps its behaviour: the cancelable {@code SpellCastEvent.Pre}, the mana check,
 * the per-spell config (delay, cost) and {@code SpellCastEvent.Post}. A deity-bound spell additionally needs the
 * caster's reputation with that deity to be at least {@code minReputation} (spec §12).
 * <p>
 * The sign sequence is <em>not</em> stored here at declaration time: it comes from the {@code eidolon:chant} recipe
 * with the same id (T2), which Eidolon sets through {@code setSigns} when it first resolves the recipe, and which
 * {@link #refreshSigns} re-reads after a datapack reload (T3).
 */
public final class ScriptedSpell extends StaticSpell {
    public interface CastCheck {
        boolean test(Level level, BlockPos pos, Player player);
    }

    public interface CastAction {
        void cast(Level level, BlockPos pos, Player player);
    }

    private final @Nullable CastCheck canCast;
    private final CastAction cast;
    private final @Nullable ResourceLocation deity;
    private final double minReputation;

    ScriptedSpell(ResourceLocation id, int cost, int delay, @Nullable CastCheck canCast, CastAction cast,
                  @Nullable ResourceLocation deity, double minReputation) {
        super(id, cost, delay);
        this.canCast = canCast;
        this.cast = cast;
        this.deity = deity;
        this.minReputation = minReputation;
    }

    public @Nullable ResourceLocation deity() {
        return deity;
    }

    @Override
    public boolean matches(SignSequence sequence) {
        return signs != null && signs.equals(sequence);
    }

    @Override
    public boolean canCast(Level level, BlockPos pos, Player player) {
        if (deity != null && player instanceof ServerPlayer sp) {
            var rep = sp.server.overworld().getCapability(IReputation.INSTANCE).resolve()
                    .map(r -> r.getReputation(player, deity)).orElse(0.0);
            if (rep < minReputation) {
                sp.displayClientMessage(Component.translatable("eidolonunchained.spell.reputation_too_low"), true);
                return false;
            }
        }
        if (canCast == null) return true;
        try {
            return canCast.test(level, pos, player);
        } catch (RuntimeException e) {
            EidolonUnchained.LOGGER.error("spell '{}' canCast threw: {}", getRegistryName(), e.toString());
            return false;
        }
    }

    @Override
    public void cast(Level level, BlockPos pos, Player player) {
        try {
            cast.cast(level, pos, player);
        } catch (RuntimeException e) {
            EidolonUnchained.LOGGER.error("spell '{}' cast threw: {}", getRegistryName(), e.toString());
        }
    }

    /** T3: take the sign sequence from the current chant recipe of the same id (null when there is none). */
    public void refreshSigns(@Nullable SignSequence fromRecipe) {
        this.signs = fromRecipe;
    }
}
