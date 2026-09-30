package com.bluelotuscoding.eidolonunchained.imbue;

import com.bluelotuscoding.eidolonunchained.EUConfig;
import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.api.ChantSync;
import com.bluelotuscoding.eidolonunchained.api.ScriptedSpell;
import com.google.gson.JsonObject;
import elucent.eidolon.api.spells.Sign;
import elucent.eidolon.api.spells.SignSequence;
import elucent.eidolon.api.spells.Spell;
import elucent.eidolon.common.item.ChantScrollItem;
import elucent.eidolon.recipe.WorktableRecipe;
import elucent.eidolon.recipe.WorktableRegistry;
import elucent.eidolon.registries.Registry;
import elucent.eidolon.registries.Spells;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Dynamic worktable recipes (D35), Eidolon's way: the written chant scroll (from the Scriptorium) is always the source
 * of the chant; the table reads it off the scroll's NBT. Eidolon's worktable asks every {@code eidolon:worktable} recipe
 * {@code matches(core, extras)} and then takes {@code getResult()}, so the output is computed while matching: the item
 * in the <b>centre</b> of the 3×3 with the chant added. Two forms, same serializers
 * ({@code eidolonunchained:imbue} / {@code eidolonunchained:protect}):
 * <ul>
 *   <li><b>Generic</b>, the two recipes this mod ships (the global default): any imbuable weapon (or Deity's Protection
 *   piece) in the centre, a written chant scroll in any other slot, soul shards in the rest, at least the chant's
 *   cost ({@code .imbueCost} / {@code .protectionCost}, 1–4; Eidolon's own chants: config default).</li>
 *   <li><b>Scripted</b>: {@code event.recipes.eidolonunchained.imbue(chant, weapon, shards)} names the chant and an
 *   ingredient for the centre (an item or a tag such as {@code #minecraft:swords}); the table then only accepts the
 *   scroll of <em>that</em> chant for that weapon, and a weapon a scripted recipe names is taken out of the generic
 *   recipe, so the scroll of any other chant does nothing on it.
 *   JSON: {@code {"type":"eidolonunchained:imbue","chant":"…","weapon":{…},"shards":2}}.</li>
 * </ul>
 * One item is taken from every filled slot (the worktable's rule), so shards beyond the cost are spent too.
 */
public class ImbueRecipe extends WorktableRecipe {
    public enum Mode { IMBUE, PROTECT }

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, EidolonUnchained.MOD_ID);
    public static final RegistryObject<RecipeSerializer<?>> IMBUE = SERIALIZERS.register("imbue", () -> new Serializer(Mode.IMBUE));
    public static final RegistryObject<RecipeSerializer<?>> PROTECT = SERIALIZERS.register("protect", () -> new Serializer(Mode.PROTECT));

    public static void register(IEventBus modBus) {
        SERIALIZERS.register(modBus);
    }

    private final Mode mode;
    private final @Nullable ResourceLocation chant;      // scripted form: the chant; null = read it from the scroll
    private final @Nullable Ingredient weapon;           // scripted form: what the centre must match; null = any imbuable
    private final int shards;                            // scripted form: shard slots; -1 = the chant's own cost
    private ItemStack last = ItemStack.EMPTY;

    public ImbueRecipe(ResourceLocation id, Mode mode) {
        this(id, mode, null, null, -1);
    }

    public ImbueRecipe(ResourceLocation id, Mode mode, @Nullable ResourceLocation chant, @Nullable Ingredient weapon, int shards) {
        super(empties(9), empties(4), ItemStack.EMPTY);
        this.mode = mode;
        this.chant = chant;
        this.weapon = weapon;
        this.shards = shards;
        setRegistryName(id);
    }

    private static Ingredient[] empties(int n) {
        var arr = new Ingredient[n];
        java.util.Arrays.fill(arr, Ingredient.EMPTY);
        return arr;
    }

    public Mode mode() {
        return mode;
    }

    public @Nullable ResourceLocation chant() {
        return chant;
    }

    @Override
    public boolean matches(Container core, Container extras) {
        last = compute(core, extras);
        return !last.isEmpty();
    }

    @Override
    public ItemStack getResult() {
        return last.copy();
    }

    @Override
    public @NotNull ItemStack getResultItem(@NotNull RegistryAccess registryAccess) {
        return last.isEmpty() ? display() : last;
    }

    /** What recipe viewers show: a sample weapon with the chant, or nothing for the generic recipe. */
    private ItemStack display() {
        if (chant == null) return ItemStack.EMPTY;
        var sample = weapon != null && weapon.getItems().length > 0 ? weapon.getItems()[0].copy() : new ItemStack(Items.IRON_SWORD);
        if (mode == Mode.IMBUE) ImbueNbt.imbue(sample, chant); else ImbueNbt.setProtection(sample, chant, null);
        return sample;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return mode == Mode.IMBUE ? IMBUE.get() : PROTECT.get();
    }

    // ---- the dynamic match: weapon in the centre, scroll in a reagent slot, shards anywhere else ----

    private ItemStack compute(Container core, Container extras) {
        if (core.getContainerSize() < 9 || extras.getContainerSize() < 4) return ItemStack.EMPTY;
        var target = core.getItem(4);
        if (target.isEmpty()) return ItemStack.EMPTY;
        if (weapon != null) {
            if (!weapon.test(target)) return ItemStack.EMPTY;
        } else if (mode == Mode.IMBUE ? !ImbueNbt.isImbuable(target) : ImbueNbt.protectionLevel(target) <= 0) {
            return reject("centre item is not " + (mode == Mode.IMBUE ? "an imbuable weapon" : "a Deity's Protection piece"));
        } else if (hasScriptedRecipeFor(target)) {
            return reject("this item has its own scripted " + mode.name().toLowerCase() + " recipe(s); the generic scroll recipe does not apply");
        }
        if (mode == Mode.PROTECT && ImbueNbt.protectionLevel(target) <= 0) return reject("centre item has no Deity's Protection");
        ItemStack scroll = ItemStack.EMPTY;
        int shardSlots = 0;
        for (int i = 0; i < 4; i++) {
            var s = extras.getItem(i);
            if (s.isEmpty()) continue;
            if (s.getItem() instanceof ChantScrollItem) {
                if (!scroll.isEmpty()) return reject("more than one scroll");
                scroll = s;
            } else if (s.is(Registry.SOUL_SHARD.get())) {
                shardSlots++;
            } else {
                return reject("reagent slot holds " + s.getItem());
            }
        }
        for (int i = 0; i < 9; i++) {
            if (i == 4) continue;
            var s = core.getItem(i);
            if (s.isEmpty()) continue;
            if (s.getItem() instanceof ChantScrollItem) {                  // the scroll may sit in the 3x3 as well
                if (!scroll.isEmpty()) return reject("more than one scroll");
                scroll = s;
            } else if (s.is(Registry.SOUL_SHARD.get())) {
                shardSlots++;
            } else {
                return reject("3x3 slot " + i + " holds " + s.getItem() + " (only the centre item, the scroll and soul shards)");
            }
        }
        if (scroll.isEmpty()) return reject("no chant scroll (put a written one in any slot around the weapon)");

        ResourceLocation id;
        Spell spell;
        if (chant != null) {
            id = chant;
            spell = Spells.find(id);
            if (spell == null) return reject("unknown chant " + id);
            var signs = ChantScrollItem.getSpell(scroll);
            var written = signs.isEmpty() || signs.contains(null) ? null : resolve(signs);
            if (written == null || !id.equals(written.getRegistryName())) return reject("the scroll is not a scroll of " + id + " (this weapon only takes that chant)");
        } else {
            var signs = ChantScrollItem.getSpell(scroll);
            if (signs.isEmpty() || signs.contains(null)) return reject("the scroll is blank (write it at the Scriptorium or use /eu scroll <chant>)");
            spell = resolve(signs);
            if (spell == null) return reject("the scroll's signs match no chant");
            id = spell.getRegistryName();
        }

        int cost;
        ItemStack out = target.copy();
        out.setCount(1);
        if (mode == Mode.IMBUE) {
            if (spell instanceof ScriptedSpell ss) {
                if (!ss.isImbuable()) return reject(id + " is not imbuable");
                cost = ss.imbueCost();
            } else {
                cost = EUConfig.IMBUE_SHARDS_DEFAULT.get();
            }
            if (!ImbueNbt.imbue(out, id)) return reject(id + " is already on the weapon, or the weapon is full");
        } else {
            if (!(spell instanceof ScriptedSpell ss) || ss.deity() == null) return reject(id + " is not deity-bound");
            cost = ss.protectionCost();
            if (id.equals(ImbueNbt.protectionChant(out))) return reject(id + " is already bound to the piece");
            ImbueNbt.setProtection(out, id, ss.deity());
        }
        if (shards >= 0) cost = shards;
        cost = Math.max(0, Math.min(12, cost));
        if (shardSlots < cost) return reject(id + " needs " + cost + " soul shard slot(s), found " + shardSlots);
        return out;
    }

    /** The generic (scroll) recipe steps aside for items a scripted recipe of the same mode names. */
    private boolean hasScriptedRecipeFor(ItemStack target) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return false;
        for (var r : server.getRecipeManager().getAllRecipesFor(elucent.eidolon.registries.EidolonRecipes.WORKTABLE_TYPE.get())) {
            if (r instanceof ImbueRecipe ir && ir != this && ir.mode == mode && ir.weapon != null && ir.weapon.test(target)) return true;
        }
        return false;
    }

    private ItemStack reject(String why) {
        EidolonUnchained.LOGGER.debug("worktable {} ({}): no result: {}", mode.name().toLowerCase(), getRegistryName(), why);
        return ItemStack.EMPTY;
    }

    private static @Nullable Spell resolve(List<Sign> signs) {
        Level level = null;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) level = server.overworld();
        if (level == null) return null;             // the worktable only computes results on the server
        return Spells.find(new SignSequence(signs), level);
    }

    /** A written chant scroll for a chant (its current recipe's signs), or empty when the chant has no recipe. */
    public static ItemStack scrollFor(ResourceLocation chant) {
        var signs = ChantSync.sequenceOf(chant);
        if (signs.isEmpty()) return ItemStack.EMPTY;
        var stack = new ItemStack(Registry.CHANT_SCROLL.get());
        ChantScrollItem.setSpell(stack, signs);
        return stack;
    }

    // ---- serializer ----

    public static final class Serializer implements RecipeSerializer<ImbueRecipe> {
        private final Mode mode;

        Serializer(Mode mode) {
            this.mode = mode;
        }

        @Override
        public @NotNull ImbueRecipe fromJson(@NotNull ResourceLocation id, @NotNull JsonObject json) {
            var chantStr = GsonHelper.getAsString(json, "chant", "");
            ResourceLocation chant = chantStr.isEmpty() ? null : new ResourceLocation(chantStr);
            Ingredient weapon = json.has("weapon") ? Ingredient.fromJson(json.get("weapon")) : null;
            int shards = GsonHelper.getAsInt(json, "shards", -1);
            return (ImbueRecipe) WorktableRegistry.register(new ImbueRecipe(id, mode, chant, weapon, shards));
        }

        @Override
        public ImbueRecipe fromNetwork(@NotNull ResourceLocation id, @NotNull FriendlyByteBuf buf) {
            ResourceLocation chant = buf.readBoolean() ? buf.readResourceLocation() : null;
            Ingredient weapon = buf.readBoolean() ? Ingredient.fromNetwork(buf) : null;
            int shards = buf.readVarInt() - 1;
            return (ImbueRecipe) WorktableRegistry.register(new ImbueRecipe(id, mode, chant, weapon, shards));
        }

        @Override
        public void toNetwork(@NotNull FriendlyByteBuf buf, @NotNull ImbueRecipe r) {
            buf.writeBoolean(r.chant != null);
            if (r.chant != null) buf.writeResourceLocation(r.chant);
            buf.writeBoolean(r.weapon != null);
            if (r.weapon != null) r.weapon.toNetwork(buf);
            buf.writeVarInt(r.shards + 1);
        }
    }
}
