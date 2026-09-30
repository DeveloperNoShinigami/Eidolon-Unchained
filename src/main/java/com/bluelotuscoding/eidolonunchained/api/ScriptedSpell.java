package com.bluelotuscoding.eidolonunchained.api;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import elucent.eidolon.api.spells.SignSequence;
import elucent.eidolon.capability.IReputation;
import elucent.eidolon.capability.ISoul;
import elucent.eidolon.common.spell.StaticSpell;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
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

    /** The mob path (rule C3): a LivingEntity caster and its target. */
    public interface MobCast {
        void cast(Level level, BlockPos pos, LivingEntity caster, @Nullable LivingEntity target);
    }

    public interface MobCheck {
        boolean test(Level level, BlockPos pos, LivingEntity caster, @Nullable LivingEntity target);
    }

    /** The targeted path (imbued right-click at what you look at, Deity's Protection retaliation, mobs when no mobCast). */
    public interface TargetCast {
        void cast(Level level, LivingEntity caster, LivingEntity target);
    }

    @Nullable MobCast mobCast;
    @Nullable MobCheck mobCanCast;
    @Nullable TargetCast targetCast;
    boolean imbuable = true;
    int imbueCost = 4;
    int protectionCost = 2;

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

    public double minReputation() {
        return minReputation;
    }

    public boolean hasMobPath() {
        return mobCast != null || targetCast != null;
    }

    public boolean isImbuable() {
        return imbuable;
    }

    public int imbueCost() {
        return imbueCost;
    }

    public int protectionCost() {
        return protectionCost;
    }

    public boolean canMobCast(Level level, BlockPos pos, LivingEntity caster, @Nullable LivingEntity target) {
        if (mobCanCast == null) return true;
        try {
            return mobCanCast.test(level, pos, caster, target);
        } catch (RuntimeException e) {
            EidolonUnchained.LOGGER.error("spell '{}' mobCanCast threw: {}", getRegistryName(), e.toString());
            return false;
        }
    }

    public void castByMob(Level level, BlockPos pos, LivingEntity caster, @Nullable LivingEntity target) {
        try {
            if (mobCast != null) mobCast.cast(level, pos, caster, target);
            else if (targetCast != null && target != null) targetCast.cast(level, caster, target);
        } catch (RuntimeException e) {
            EidolonUnchained.LOGGER.error("spell '{}' mobCast threw: {}", getRegistryName(), e.toString());
        }
    }

    /** true when the spell has a targeted path and it ran. */
    public boolean castAt(Level level, LivingEntity caster, LivingEntity target) {
        if (targetCast == null) return false;
        try {
            targetCast.cast(level, caster, target);
            return true;
        } catch (RuntimeException e) {
            EidolonUnchained.LOGGER.error("spell '{}' targetCast threw: {}", getRegistryName(), e.toString());
            return false;
        }
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
        ISoul.expendMana(player, getCost());          // as Eidolon's own spells do in their cast (creative pays nothing)
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
