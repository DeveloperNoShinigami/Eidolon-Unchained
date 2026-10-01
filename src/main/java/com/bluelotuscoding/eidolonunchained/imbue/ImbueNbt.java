package com.bluelotuscoding.eidolonunchained.imbue;

import com.bluelotuscoding.eidolonunchained.EUConfig;
import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Item NBT of imbued weapons and Deity's Protection pieces (rule C6, v1):
 * <pre>
 * eidolonunchained:{v:1,
 *   caster_grant:{chants:["ns:id", …]},     // the imbued chants; a mob holding the weapon casts them (rule C4)
 *   imbue:{active:0},                       // which chant the player's right-click casts
 *   protection:{chant:"ns:id", deity:"ns:id"},   // Deity's Protection: the bound chant
 *   cooldown:{until:L, total:I}}            // chant cooldown of this stack (D51): ends at level game time 'until'
 * </pre>
 */
public final class ImbueNbt {
    public static final String ROOT = EidolonUnchained.MOD_ID;
    public static final TagKey<Item> IMBUABLE = ItemTags.create(new ResourceLocation(EidolonUnchained.MOD_ID, "imbuable"));

    private ImbueNbt() {
    }

    private static CompoundTag root(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getCompound(ROOT) : new CompoundTag();
    }

    private static CompoundTag editRoot(ItemStack stack) {
        var root = stack.getOrCreateTag().getCompound(ROOT);
        root.putInt("v", 1);
        stack.getOrCreateTag().put(ROOT, root);
        return root;
    }

    // ---- imbued chants ----

    /** Any weapon: the item tag {@code eidolonunchained:imbuable}, or a tiered (melee) item or trident by default. */
    public static boolean isImbuable(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.is(IMBUABLE) || stack.getItem() instanceof TieredItem || stack.getItem() instanceof TridentItem;
    }

    public static boolean isImbued(ItemStack stack) {
        return !chants(stack).isEmpty();
    }

    public static List<ResourceLocation> chants(ItemStack stack) {
        var grant = root(stack).getCompound("caster_grant");
        var list = grant.getList("chants", Tag.TAG_STRING);
        var out = new ArrayList<ResourceLocation>(list.size());
        for (int i = 0; i < list.size(); i++) {
            var rl = ResourceLocation.tryParse(list.getString(i));
            if (rl != null) out.add(rl);
        }
        return out;
    }

    public static int active(ItemStack stack) {
        int n = chants(stack).size();
        if (n == 0) return -1;
        return Math.floorMod(root(stack).getCompound("imbue").getInt("active"), n);
    }

    public static @Nullable ResourceLocation activeChant(ItemStack stack) {
        var chants = chants(stack);
        return chants.isEmpty() ? null : chants.get(active(stack));
    }

    public static void setActive(ItemStack stack, int index) {
        var root = editRoot(stack);
        var imbue = root.getCompound("imbue");
        imbue.putInt("active", Math.max(0, index));
        root.put("imbue", imbue);
    }

    /** Adds a chant (no duplicates, up to the configured maximum). */
    public static boolean imbue(ItemStack stack, ResourceLocation chant) {
        var chants = chants(stack);
        if (chants.contains(chant) || chants.size() >= EUConfig.MAX_IMBUED_CHANTS.get()) return false;
        chants.add(chant);
        writeChants(stack, chants);
        return true;
    }

    public static boolean removeChant(ItemStack stack, ResourceLocation chant) {
        var chants = chants(stack);
        if (!chants.remove(chant)) return false;
        writeChants(stack, chants);
        return true;
    }

    private static void writeChants(ItemStack stack, List<ResourceLocation> chants) {
        var root = editRoot(stack);
        var grant = root.getCompound("caster_grant");
        var list = new ListTag();
        for (var c : chants) list.add(StringTag.valueOf(c.toString()));
        grant.put("chants", list);
        root.put("caster_grant", grant);
        if (chants.isEmpty()) { root.remove("caster_grant"); root.remove("imbue"); }
    }

    // ---- Deity's Protection ----

    public static int protectionLevel(ItemStack stack) {
        return stack.isEmpty() ? 0 : EnchantmentHelper.getItemEnchantmentLevel(EUEnchantments.DEITYS_PROTECTION.get(), stack);
    }

    public static @Nullable ResourceLocation protectionChant(ItemStack stack) {
        var p = root(stack).getCompound("protection");
        return p.contains("chant") ? ResourceLocation.tryParse(p.getString("chant")) : null;
    }

    public static @Nullable ResourceLocation protectionDeity(ItemStack stack) {
        var p = root(stack).getCompound("protection");
        return p.contains("deity") ? ResourceLocation.tryParse(p.getString("deity")) : null;
    }

    public static void setProtection(ItemStack stack, @Nullable ResourceLocation chant, @Nullable ResourceLocation deity) {
        var root = editRoot(stack);
        if (chant == null) { root.remove("protection"); return; }
        var p = new CompoundTag();
        p.putString("chant", chant.toString());
        if (deity != null) p.putString("deity", deity.toString());
        root.put("protection", p);
    }

    // ---- chant cooldown (D51): per stack, never vanilla's per-item-type cooldown, so a shield on it still blocks ----

    public static void setCooldown(ItemStack stack, long now, int ticks) {
        if (stack.isEmpty()) return;
        var root = editRoot(stack);
        if (ticks <= 0) { root.remove("cooldown"); return; }
        var cd = new CompoundTag();
        cd.putLong("until", now + ticks);
        cd.putInt("total", ticks);
        root.put("cooldown", cd);
    }

    /** Ticks left on the stack's chant cooldown at game time {@code now} (fractional for render smoothing). */
    public static double cooldownRemaining(ItemStack stack, double now) {
        var cd = root(stack).getCompound("cooldown");
        return cd.contains("until") ? Math.max(0, cd.getLong("until") - now) : 0;
    }

    /** 1 just after the cooldown starts, 0 when it is over. */
    public static float cooldownFraction(ItemStack stack, double now) {
        var cd = root(stack).getCompound("cooldown");
        int total = cd.getInt("total");
        if (total <= 0) return 0;
        return (float) Math.min(1, cooldownRemaining(stack, now) / total);
    }

    public static boolean isOnCooldown(ItemStack stack, long now) {
        return cooldownRemaining(stack, now) > 0;
    }

    // ---- names ----

    /** "Call Storm" for {@code mypack:call_storm}, unless the pack provides {@code chant.mypack.call_storm}. */
    public static Component chantName(ResourceLocation id) {
        var words = id.getPath().replace('/', ' ').replace('_', ' ').trim().split("\\s+");
        var sb = new StringBuilder();
        for (var w : words) {
            if (w.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return Component.translatableWithFallback("chant." + id.getNamespace() + "." + id.getPath(), sb.toString());
    }
}
