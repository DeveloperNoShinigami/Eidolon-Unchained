package com.bluelotuscoding.eidolonunchained.api.condition;

import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * What a condition is tested against (Phase 4, spec §7.2): the player the event is about, its subject entity (a victim,
 * a tamed or enthralled mob, a chant's target), an item, a place, and the ids involved (chant, ritual, deity, the
 * event's own id). Built by EU for every event and discovery; scripts see it in {@code C.test(ctx => …)} and
 * {@code .run(ctx => …)}.
 */
public final class EventContext {
    private final String kind;
    private @Nullable Player player;
    private @Nullable LivingEntity entity;
    private ItemStack item = ItemStack.EMPTY;
    private @Nullable Level level;
    private @Nullable BlockPos pos;
    private @Nullable ResourceLocation id;
    private final Map<String, ResourceLocation> ids = new HashMap<>();

    public EventContext(String kind) {
        this.kind = kind;
    }

    // ---- building (Java side) ----

    public EventContext player(@Nullable Player p) {
        this.player = p;
        if (p != null) { if (level == null) level = p.level(); if (pos == null) pos = p.blockPosition(); }
        return this;
    }

    public EventContext entity(@Nullable LivingEntity e) {
        this.entity = e;
        if (e != null) { if (level == null) level = e.level(); if (pos == null) pos = e.blockPosition(); }
        return this;
    }

    public EventContext item(@Nullable ItemStack stack) {
        this.item = stack == null ? ItemStack.EMPTY : stack;
        return this;
    }

    public EventContext at(Level level, BlockPos pos) {
        this.level = level;
        this.pos = pos;
        return this;
    }

    public EventContext id(@Nullable ResourceLocation id) {
        this.id = id;
        if (id != null) ids.put(kind, id);
        return this;
    }

    public EventContext with(String key, @Nullable ResourceLocation value) {
        if (value != null) ids.put(key, value);
        return this;
    }

    /** A context from whatever a script handed to {@code condition.test(x)}. */
    public static EventContext of(Object o) {
        if (o instanceof EventContext c) return c;
        var ctx = new EventContext("test");
        if (o instanceof Player p) ctx.player(p);
        else if (o instanceof LivingEntity le) ctx.entity(le);
        else if (o instanceof ItemStack s) ctx.item(s);
        else if (o instanceof Entity e) ctx.at(e.level(), e.blockPosition());
        return ctx;
    }

    // ---- reading (scripts and conditions) ----

    @Info("What happened: 'kill', 'biome', 'structure', 'research', 'ritual', 'chant', …")
    public String getKind() {
        return kind;
    }

    public @Nullable Player getPlayer() {
        return player;
    }

    @Info("The subject: the victim, the tamed or enthralled mob, the chant's target (null when none)")
    public @Nullable LivingEntity getEntity() {
        return entity;
    }

    public ItemStack getItem() {
        return item;
    }

    public @Nullable Level getLevel() {
        return level;
    }

    public @Nullable BlockPos getPos() {
        return pos;
    }

    @Info("The event's own id: the biome, structure, research, ritual, chant … id")
    public @Nullable String getId() {
        return id == null ? null : id.toString();
    }

    public @Nullable ResourceLocation idOf(String key) {
        return ids.get(key);
    }

    @Override
    public String toString() {
        return "EventContext{" + kind + ", id=" + id + ", player=" + (player == null ? "-" : player.getName().getString())
                + ", entity=" + (entity == null ? "-" : entity.getType().toShortString()) + ", item=" + item + "}";
    }
}
