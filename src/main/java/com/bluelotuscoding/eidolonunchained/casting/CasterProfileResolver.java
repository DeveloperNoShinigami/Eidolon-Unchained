package com.bluelotuscoding.eidolonunchained.casting;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Rule C4 / spec §3.8.5: one effective {@link CasterProfile} per mob, from (in order) the base profile the entity's NBT
 * names → entity NBT overrides → equipment grants (main hand, off hand, armour) → nothing else yet. Namespaced,
 * versioned NBT (rule C6):
 * <pre>
 * entity: eidolonunchained:{v:1, caster:{profile:"ns:id", spells:["…"], deity:"ns:id", max_mana:60f, cast_interval:40, sign_delay:6, min_range:2d, max_range:18d, line_of_sight:1b, target:"nearest_player"}}
 * item:   eidolonunchained:{v:1, caster_grant:{profile:"ns:id", spells:["…"], deity:"ns:id", mana_add:0f, max_mana_add:20f}}
 * </pre>
 * A mob with no sources resolves to null (a plain mob). Results are cached per mob and re-resolved by {@link MobCasting}
 * on join, equipment change and script request.
 */
public final class CasterProfileResolver {
    public static final String ROOT = EidolonUnchained.MOD_ID;

    private static final Map<Mob, CasterProfile> CACHE = new WeakHashMap<>();

    private CasterProfileResolver() {
    }

    public static @Nullable CasterProfile cached(Mob mob) {
        return CACHE.get(mob);
    }

    public static @Nullable CasterProfile resolve(Mob mob) {
        var result = compute(mob);
        if (result == null) CACHE.remove(mob); else CACHE.put(mob, result);
        return result;
    }

    private static @Nullable CasterProfile compute(Mob mob) {
        var entityTag = mob.getPersistentData().getCompound(ROOT).getCompound("caster");
        var grants = new ArrayList<CompoundTag>();
        for (var slot : EquipmentSlot.values()) {
            var stack = mob.getItemBySlot(slot);
            var g = grantOf(stack);
            if (g != null) grants.add(g);
        }
        if (entityTag.isEmpty() && grants.isEmpty()) return null;

        // base profile: the entity's, else the first grant's
        CasterProfile base = null;
        if (entityTag.contains("profile")) base = CasterProfile.find(ResourceLocation.tryParse(entityTag.getString("profile")));
        if (base == null) for (var g : grants) if (g.contains("profile")) { base = CasterProfile.find(ResourceLocation.tryParse(g.getString("profile"))); if (base != null) break; }
        var effective = base != null ? base.copy(new ResourceLocation(ROOT, "resolved")) : new CasterProfile(new ResourceLocation(ROOT, "resolved"));
        if (base == null) effective.spells.clear();

        // entity overrides
        applyList(entityTag, "spells", effective.spells);
        if (entityTag.contains("deity")) effective.deity = ResourceLocation.tryParse(entityTag.getString("deity"));
        if (entityTag.contains("max_mana")) effective.maxMana = entityTag.getFloat("max_mana");
        if (entityTag.contains("regen")) effective.regenPerSecond = entityTag.getFloat("regen");
        if (entityTag.contains("cast_interval")) effective.castInterval = entityTag.getInt("cast_interval");
        if (entityTag.contains("sign_delay")) effective.signDelay = entityTag.getInt("sign_delay");
        if (entityTag.contains("min_range")) effective.minRange = entityTag.getDouble("min_range");
        if (entityTag.contains("max_range")) effective.maxRange = entityTag.getDouble("max_range");
        if (entityTag.contains("line_of_sight")) effective.requireLineOfSight = entityTag.getBoolean("line_of_sight");
        if (entityTag.contains("target")) {
            try { effective.targetPolicy = CasterProfile.TargetPolicy.valueOf(entityTag.getString("target").toUpperCase()); } catch (IllegalArgumentException ignored) { }
        }

        // equipment grants: add spells, deity if none, mana
        for (var g : grants) {
            applyList(g, "spells", effective.spells);
            if (effective.deity == null && g.contains("deity")) effective.deity = ResourceLocation.tryParse(g.getString("deity"));
            if (g.contains("max_mana_add")) effective.maxMana += g.getFloat("max_mana_add");
        }
        return effective.spells.isEmpty() ? null : effective;
    }

    private static void applyList(CompoundTag tag, String key, List<ResourceLocation> into) {
        if (!tag.contains(key, Tag.TAG_LIST)) return;
        var list = tag.getList(key, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            var rl = ResourceLocation.tryParse(list.getString(i));
            if (rl != null && !into.contains(rl)) into.add(rl);
        }
    }

    /** The {@code caster_grant} compound of an item, or null. */
    public static @Nullable CompoundTag grantOf(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) return null;
        var root = stack.getTag().getCompound(ROOT);
        return root.contains("caster_grant") ? root.getCompound("caster_grant") : null;
    }

    /** Writes a caster profile reference onto an entity (scripts: {@code EidolonUnchained.caster(entity).setProfile(id)}). */
    public static void setEntityProfile(Mob mob, @Nullable ResourceLocation profile) {
        var root = mob.getPersistentData().getCompound(ROOT);
        root.putInt("v", 1);
        var caster = root.getCompound("caster");
        if (profile == null) caster.remove("profile"); else caster.putString("profile", profile.toString());
        root.put("caster", caster);
        mob.getPersistentData().put(ROOT, root);
    }

    public static void addEntitySpell(Mob mob, ResourceLocation spell) {
        var root = mob.getPersistentData().getCompound(ROOT);
        root.putInt("v", 1);
        var caster = root.getCompound("caster");
        var list = caster.getList("spells", Tag.TAG_STRING);
        list.add(net.minecraft.nbt.StringTag.valueOf(spell.toString()));
        caster.put("spells", list);
        root.put("caster", caster);
        mob.getPersistentData().put(ROOT, root);
    }

    /** Writes a {@code caster_grant} onto an item (imbuing, scripts). */
    public static void writeGrant(ItemStack stack, @Nullable ResourceLocation profile, List<ResourceLocation> spells, @Nullable ResourceLocation deity) {
        var root = stack.getOrCreateTag().getCompound(ROOT);
        root.putInt("v", 1);
        var grant = root.getCompound("caster_grant");
        if (profile != null) grant.putString("profile", profile.toString());
        var list = grant.getList("spells", Tag.TAG_STRING);
        for (var s : spells) list.add(net.minecraft.nbt.StringTag.valueOf(s.toString()));
        grant.put("spells", list);
        if (deity != null) grant.putString("deity", deity.toString());
        root.put("caster_grant", grant);
        stack.getOrCreateTag().put(ROOT, root);
    }
}
