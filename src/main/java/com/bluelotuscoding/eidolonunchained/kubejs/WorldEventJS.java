package com.bluelotuscoding.eidolonunchained.kubejs;

import com.bluelotuscoding.eidolonunchained.api.condition.EventContext;
import dev.latvian.mods.kubejs.player.PlayerEventJS;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Phase 4 events ({@code enteredBiome, leftBiome, enteredStructure, leftStructure, learnedResearch, learnedFact,
 * learnedSign, learnedRune, ritualCompleted, enthralled, tamed}): {@code e.player}, {@code e.target} (the mob),
 * {@code e.item}, {@code e.pos}, {@code e.id} (the biome, structure, research … id) and {@code e.context} for conditions.
 */
public class WorldEventJS extends PlayerEventJS {
    private final EventContext ctx;

    public WorldEventJS(EventContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public @Nullable Player getEntity() {
        return ctx.getPlayer();
    }

    @Info("The biome, structure, research, fact, sign, rune, ritual or entity type id this event is about")
    public @Nullable String getId() {
        return ctx.getId();
    }

    @Info("The mob tamed or enthralled (null otherwise)")
    public @Nullable LivingEntity getTarget() {
        return ctx.getEntity();
    }

    public ItemStack getItem() {
        return ctx.getItem();
    }

    public @Nullable BlockPos getPos() {
        return ctx.getPos();
    }

    @Info("The event as a condition context: someCondition.test(e.context)")
    public EventContext getContext() {
        return ctx;
    }
}
