package com.bluelotuscoding.eidolonunchained.casting;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A scripted caster profile (rule C4): what a mob may cast and how. Declared by {@code EidolonUnchained.caster(id)},
 * referenced by entity NBT ({@code eidolonunchained.caster.profile}) or item NBT ({@code eidolonunchained.caster_grant.profile}),
 * and merged by {@link CasterProfileResolver} into one effective profile per mob.
 */
public final class CasterProfile {
    public enum TargetPolicy { ATTACK_TARGET, NEAREST_PLAYER, ALLIES }

    public final ResourceLocation id;
    public final List<ResourceLocation> spells = new ArrayList<>();
    public @Nullable ResourceLocation deity;
    public float maxMana = 50;
    public float regenPerSecond = 1;
    public int castInterval = 60;
    public int signDelay = 8;
    public double minRange = 2;
    public double maxRange = 16;
    public boolean requireLineOfSight = true;
    public final Map<ResourceLocation, Integer> cooldowns = new HashMap<>();
    public TargetPolicy targetPolicy = TargetPolicy.ATTACK_TARGET;
    public boolean suppressMeleeWhileCasting = true;

    public CasterProfile(ResourceLocation id) {
        this.id = id;
    }

    public CasterProfile copy(ResourceLocation newId) {
        var c = new CasterProfile(newId);
        c.spells.addAll(spells);
        c.deity = deity;
        c.maxMana = maxMana;
        c.regenPerSecond = regenPerSecond;
        c.castInterval = castInterval;
        c.signDelay = signDelay;
        c.minRange = minRange;
        c.maxRange = maxRange;
        c.requireLineOfSight = requireLineOfSight;
        c.cooldowns.putAll(cooldowns);
        c.targetPolicy = targetPolicy;
        c.suppressMeleeWhileCasting = suppressMeleeWhileCasting;
        return c;
    }

    /** Registry of declared profiles (filled at registration stage CASTERS). */
    public static final Map<ResourceLocation, CasterProfile> REGISTRY = new LinkedHashMap<>();

    public static @Nullable CasterProfile find(ResourceLocation id) {
        return REGISTRY.get(id);
    }
}
