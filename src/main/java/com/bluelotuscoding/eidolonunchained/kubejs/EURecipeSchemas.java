package com.bluelotuscoding.eidolonunchained.kubejs;

import dev.latvian.mods.kubejs.item.InputItem;
import dev.latvian.mods.kubejs.item.OutputItem;
import dev.latvian.mods.kubejs.recipe.RecipeJS;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.BooleanComponent;
import dev.latvian.mods.kubejs.recipe.component.ItemComponents;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.component.StringComponent;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import dev.latvian.mods.kubejs.recipe.schema.RegisterRecipeSchemasEvent;
import net.minecraft.resources.ResourceLocation;

/**
 * Timing rule T2 (D26): sign sequences are Eidolon recipes, and these schemas let server scripts create them without a
 * datapack. Each schema's fields are exactly what the matching Eidolon serializer reads (Repraised 0.3.13):
 * <ul>
 *   <li>{@code eidolon:chant}: {@code signs[]} — {@code ChantRecipe.Serializer}</li>
 *   <li>{@code eidolon:command_chant}: {@code signs[]}, {@code commands[]}, {@code mana_cost} — {@code CommandChantRecipe.Serializer}</li>
 *   <li>{@code eidolon:conversion_chant}: {@code signs[]}, {@code mana_cost} — {@code BaseConversionChantRecipe.Serializer}</li>
 *   <li>{@code eidolon:chant_conversion}: {@code input}, {@code output}, {@code min_devotion}, {@code conversion_cost}, {@code deity} — {@code ChantConversionRecipe.Serializer}</li>
 * </ul>
 * For the three chant types the recipe id <em>is</em> the spell id ({@code ChantRecipe.getChant()} = {@code Spells.find(id)}),
 * so the first script argument is the spell id and {@link ChantRecipeJS} uses it as the recipe id.
 */
public final class EURecipeSchemas {
    /** Optional so KubeJS can also parse Eidolon's own chant recipes (which carry no "spell": their id is the spell). */
    public static final RecipeKey<String> SPELL = StringComponent.ID.key("spell").optional("");
    public static final RecipeKey<String[]> SIGNS = StringComponent.ID.asArray().key("signs");
    public static final RecipeKey<String[]> COMMANDS = StringComponent.NON_BLANK.asArray().key("commands");
    public static final RecipeKey<Integer> MANA_COST = NumberComponent.INT.key("mana_cost").optional(0);
    public static final RecipeKey<InputItem> INPUT = ItemComponents.INPUT.key("input");
    public static final RecipeKey<OutputItem> OUTPUT = ItemComponents.OUTPUT.key("output");
    public static final RecipeKey<Float> MIN_DEVOTION = NumberComponent.FLOAT.key("min_devotion").optional(0f);
    public static final RecipeKey<Float> CONVERSION_COST = NumberComponent.FLOAT.key("conversion_cost").optional(-1f);
    public static final RecipeKey<String> DEITY = StringComponent.ID.key("deity").optional("");

    public static final RecipeSchema CHANT = new RecipeSchema(ChantRecipeJS.class, ChantRecipeJS::new, SPELL, SIGNS)
            .constructor(SPELL, SIGNS);
    public static final RecipeSchema COMMAND_CHANT = new RecipeSchema(ChantRecipeJS.class, ChantRecipeJS::new, SPELL, SIGNS, COMMANDS, MANA_COST)
            .constructor(SPELL, SIGNS, COMMANDS)
            .constructor(SPELL, SIGNS, COMMANDS, MANA_COST);
    public static final RecipeSchema CONVERSION_CHANT = new RecipeSchema(ChantRecipeJS.class, ChantRecipeJS::new, SPELL, SIGNS, MANA_COST)
            .constructor(SPELL, SIGNS)
            .constructor(SPELL, SIGNS, MANA_COST);
    public static final RecipeSchema CHANT_CONVERSION = new RecipeSchema(OUTPUT, INPUT, MIN_DEVOTION, CONVERSION_COST, DEITY)
            .uniqueOutputId(OUTPUT)
            .constructor(OUTPUT, INPUT)
            .constructor(OUTPUT, INPUT, MIN_DEVOTION, CONVERSION_COST, DEITY);

    // Ritual recipes (RitualRecipe.getPedestalItems: every item list is a JSON array of ingredients; the reagent too).
    public static final RecipeKey<String> RITUAL = StringComponent.ID.key("ritual");
    public static final RecipeKey<InputItem[]> REAGENT = ItemComponents.INPUT.asArray().key("reagent");   // Eidolon reads it with getAsJsonArray
    public static final RecipeKey<InputItem[]> PEDESTAL_ITEMS = ItemComponents.INPUT.asArray().key("pedestalItems").optional(new InputItem[0]).alwaysWrite();
    public static final RecipeKey<InputItem[]> FOCUS_ITEMS = ItemComponents.INPUT.asArray().key("focusItems").optional(new InputItem[0]).alwaysWrite();
    public static final RecipeKey<InputItem[]> INVARIANT_ITEMS = ItemComponents.INPUT.asArray().key("invariantItems").optional(new InputItem[0]);
    public static final RecipeKey<Float> HEALTH = NumberComponent.FLOAT.key("healthRequirement").optional(0f);
    public static final RecipeKey<Boolean> KEEP_NBT = BooleanComponent.BOOLEAN.key("keepNbtOfReagent").optional(false);

    /** {@code eidolon:ritual_brazier}: reagent + pedestal/focus items trigger a registered ritual ({@code GenericRitualRecipe.Serializer}). */
    public static final RecipeSchema RITUAL_BRAZIER = new RecipeSchema(RITUAL, REAGENT, PEDESTAL_ITEMS, FOCUS_ITEMS, HEALTH, INVARIANT_ITEMS)
            .uniqueId(r -> r.getValue(RITUAL).replace(':', '_').replace('/', '_'))
            .constructor(RITUAL, REAGENT)
            .constructor(RITUAL, REAGENT, PEDESTAL_ITEMS, FOCUS_ITEMS)
            .constructor(RITUAL, REAGENT, PEDESTAL_ITEMS, FOCUS_ITEMS, HEALTH)
            .constructor(RITUAL, REAGENT, PEDESTAL_ITEMS, FOCUS_ITEMS, HEALTH, INVARIANT_ITEMS);
    /** {@code eidolon:ritual_brazier_crafting}: Eidolon's crafting ritual ({@code ItemRitualRecipe.Serializer}). */
    public static final RecipeSchema RITUAL_BRAZIER_CRAFTING = new RecipeSchema(OUTPUT, REAGENT, PEDESTAL_ITEMS, FOCUS_ITEMS, HEALTH, KEEP_NBT)
            .uniqueOutputId(OUTPUT)
            .constructor(OUTPUT, REAGENT)
            .constructor(OUTPUT, REAGENT, PEDESTAL_ITEMS, FOCUS_ITEMS)
            .constructor(OUTPUT, REAGENT, PEDESTAL_ITEMS, FOCUS_ITEMS, HEALTH, KEEP_NBT);

    // Scripted imbue / protect recipes (ImbueRecipe.Serializer): the chant, what the centre item must match, shard slots.
    /** Optional so KubeJS can also parse the two generic recipes this mod ships (no chant: read from the scroll). */
    public static final RecipeKey<String> CHANT_ID = StringComponent.ID.key("chant").optional("");
    public static final RecipeKey<InputItem> WEAPON = ItemComponents.INPUT.key("weapon").optional(InputItem.EMPTY).alwaysWrite();
    public static final RecipeKey<Integer> SHARDS = NumberComponent.INT.key("shards").optional(-1).alwaysWrite();

    /**
     * {@code event.recipes.eidolonunchained.imbue('mypack:call_storm', '#minecraft:swords', 2)}: at the worktable, an item the
     * ingredient matches in the centre + the written scroll of that chant in a reagent slot (Eidolon's way) + that many
     * soul shard slots → the same item with the chant. {@code shards} -1 = the chant's own cost. A weapon a scripted
     * recipe names is taken out of the generic scroll recipe: another chant's scroll does nothing on it.
     */
    public static final RecipeSchema IMBUE = new RecipeSchema(CHANT_ID, WEAPON, SHARDS)
            .uniqueId(r -> {
                var w = r.getValue(WEAPON);
                String weapon = "any";
                if (w != null && !w.isEmpty()) {
                    var json = w.ingredient.toJson();
                    if (json.isJsonObject() && json.getAsJsonObject().has("item")) weapon = json.getAsJsonObject().get("item").getAsString();
                    else if (json.isJsonObject() && json.getAsJsonObject().has("tag")) weapon = "tag_" + json.getAsJsonObject().get("tag").getAsString();
                    else weapon = Integer.toHexString(json.toString().hashCode());
                }
                return (r.getValue(CHANT_ID) + "_" + weapon).replaceAll("[^a-z0-9_./-]", "_");
            })
            .constructor(CHANT_ID)
            .constructor(CHANT_ID, WEAPON)
            .constructor(CHANT_ID, WEAPON, SHARDS);

    private EURecipeSchemas() {
    }

    public static void register(RegisterRecipeSchemasEvent event) {
        event.namespace("eidolonunchained")
                .register("imbue", IMBUE)
                .register("protect", IMBUE);
        event.namespace("eidolon")
                .register("chant", CHANT)
                .register("command_chant", COMMAND_CHANT)
                .register("conversion_chant", CONVERSION_CHANT)
                .register("chant_conversion", CHANT_CONVERSION)
                .register("ritual_brazier", RITUAL_BRAZIER)
                .register("ritual_brazier_crafting", RITUAL_BRAZIER_CRAFTING);
    }

    /** A chant recipe whose id is the spell id it names. */
    public static class ChantRecipeJS extends RecipeJS {
        @Override
        public ResourceLocation getOrCreateId() {
            if (id == null) {
                var spell = getValue(SPELL);
                if (spell != null && !spell.isEmpty() && ResourceLocation.isValidResourceLocation(spell)) {
                    id = new ResourceLocation(spell);
                }
            }
            return super.getOrCreateId();
        }
    }
}
