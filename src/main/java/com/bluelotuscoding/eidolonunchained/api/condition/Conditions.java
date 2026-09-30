package com.bluelotuscoding.eidolonunchained.api.condition;

import com.bluelotuscoding.eidolonunchained.api.Ids;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Predicate;

/**
 * {@code EidolonUnchained.conditions} (Phase 4, spec §7.2): the condition library. Location conditions read the
 * event's level and position, {@code entity} its subject, {@code item} its item, {@code player} its player, and
 * {@code chant / ritual / deity} its ids. Combine with {@code all / any / not} or {@code a.and(b)}; {@code test(fn)}
 * wraps any script function.
 */
public final class Conditions {
    public static final Conditions INSTANCE = new Conditions();

    private Conditions() {
    }

    // ---- location ----

    @Info("In this dimension, e.g. 'minecraft:the_nether'")
    public Condition dimension(String dimensionId) {
        var rl = Ids.of(dimensionId, "dimension");
        return new Condition("in dimension " + rl, ctx -> ctx.getLevel() != null && ctx.getLevel().dimension().location().equals(rl));
    }

    @Info("In this biome: id or '#tag', e.g. 'minecraft:deep_dark' or '#minecraft:is_forest'")
    public Condition biome(String biomeOrTag) {
        if (biomeOrTag.startsWith("#")) {
            var tag = TagKey.create(Registries.BIOME, Ids.of(biomeOrTag.substring(1), "biome tag"));
            return new Condition("in biome " + biomeOrTag, ctx -> ctx.getLevel() != null && ctx.getPos() != null && ctx.getLevel().getBiome(ctx.getPos()).is(tag));
        }
        var key = ResourceKey.create(Registries.BIOME, Ids.of(biomeOrTag, "biome"));
        return new Condition("in biome " + key.location(), ctx -> ctx.getLevel() != null && ctx.getPos() != null && ctx.getLevel().getBiome(ctx.getPos()).is(key));
    }

    @Info("Inside a piece of this structure: id or '#tag', e.g. 'minecraft:fortress'")
    public Condition structure(String structureOrTag) {
        if (structureOrTag.startsWith("#")) {
            TagKey<Structure> tag = TagKey.create(Registries.STRUCTURE, Ids.of(structureOrTag.substring(1), "structure tag"));
            return new Condition("in structure " + structureOrTag, ctx -> ctx.getLevel() instanceof ServerLevel sl && ctx.getPos() != null
                    && sl.structureManager().getStructureWithPieceAt(ctx.getPos(), tag).isValid());
        }
        var rl = Ids.of(structureOrTag, "structure");
        return new Condition("in structure " + rl, ctx -> {
            if (!(ctx.getLevel() instanceof ServerLevel sl) || ctx.getPos() == null) return false;
            var structure = sl.registryAccess().registryOrThrow(Registries.STRUCTURE).get(rl);
            return structure != null && sl.structureManager().getStructureWithPieceAt(ctx.getPos(), structure).isValid();
        });
    }

    @Info("A block (id or '#tag') within this many blocks (max 16)")
    public Condition near(String blockOrTag, int radius) {
        int r = Math.max(0, Math.min(16, radius));
        Predicate<net.minecraft.world.level.block.state.BlockState> is;
        if (blockOrTag.startsWith("#")) {
            var tag = TagKey.create(Registries.BLOCK, Ids.of(blockOrTag.substring(1), "block tag"));
            is = s -> s.is(tag);
        } else {
            var rl = Ids.of(blockOrTag, "block");
            is = s -> rl.equals(ForgeRegistries.BLOCKS.getKey(s.getBlock()));
        }
        var test = is;
        return new Condition("near " + blockOrTag + " within " + r, ctx -> {
            if (ctx.getLevel() == null || ctx.getPos() == null) return false;
            for (var p : BlockPos.betweenClosed(ctx.getPos().offset(-r, -r, -r), ctx.getPos().offset(r, r, r)))
                if (test.test(ctx.getLevel().getBlockState(p))) return true;
            return false;
        });
    }

    @Info("Daytime (sun up) in the event's level")
    public Condition day() {
        return new Condition("day", ctx -> ctx.getLevel() != null && ctx.getLevel().isDay());
    }

    public Condition night() {
        return new Condition("night", ctx -> ctx.getLevel() != null && !ctx.getLevel().isDay());
    }

    public Condition raining() {
        return new Condition("raining", ctx -> ctx.getLevel() != null && ctx.getLevel().isRaining());
    }

    // ---- entity, item, player ----

    @Info("The event's subject entity: id or '#tag'; narrow with .named .nbt .boss .category .tamed .enthralled .undead .healthBelow")
    public EntityCondition entity(String entityOrTag) {
        if (entityOrTag.startsWith("#")) {
            TagKey<EntityType<?>> tag = TagKey.create(Registries.ENTITY_TYPE, Ids.of(entityOrTag.substring(1), "entity tag"));
            return new EntityCondition("entity " + entityOrTag, ctx -> ctx.getEntity() != null && ctx.getEntity().getType().is(tag));
        }
        var rl = Ids.of(entityOrTag, "entity");
        return new EntityCondition("entity " + rl, ctx -> ctx.getEntity() != null && rl.equals(ForgeRegistries.ENTITY_TYPES.getKey(ctx.getEntity().getType())));
    }

    @Info("Any living subject entity, to narrow further")
    public EntityCondition entity() {
        return new EntityCondition("entity", ctx -> ctx.getEntity() != null);
    }

    @Info("The event's item: id or '#tag'; narrow with .named .nbt .enchanted .count")
    public ItemCondition item(String itemOrTag) {
        if (itemOrTag.startsWith("#")) {
            var tag = TagKey.create(Registries.ITEM, Ids.of(itemOrTag.substring(1), "item tag"));
            return new ItemCondition("item " + itemOrTag, s -> !s.isEmpty() && s.is(tag));
        }
        var rl = Ids.of(itemOrTag, "item");
        return new ItemCondition("item " + rl, s -> !s.isEmpty() && rl.equals(ForgeRegistries.ITEMS.getKey(s.getItem())));
    }

    @Info("The event's player; narrow with .reputation .stage .knowsResearch .knowsFact .knowsSign .knowsRune .advancement .effect .holding .wearing")
    public PlayerCondition player() {
        return new PlayerCondition();
    }

    // ---- magic ----

    @Info("The chant involved (the one cast, or the chant being checked)")
    public Condition chant(String chantId) {
        var rl = Ids.of(chantId, "chant");
        return new Condition("chant " + rl, ctx -> rl.equals(ctx.idOf("chant")));
    }

    public Condition ritual(String ritualId) {
        var rl = Ids.of(ritualId, "ritual");
        return new Condition("ritual " + rl, ctx -> rl.equals(ctx.idOf("ritual")));
    }

    public Condition deity(String deityId) {
        var rl = Ids.of(deityId, "deity");
        return new Condition("deity " + rl, ctx -> rl.equals(ctx.idOf("deity")));
    }

    // ---- combining ----

    @Info("All of these hold")
    public Condition all(Condition... conditions) {
        var sb = new StringBuilder("all(");
        for (int i = 0; i < conditions.length; i++) sb.append(i > 0 ? ", " : "").append(conditions[i].describe());
        return new Condition(sb.append(')').toString(), ctx -> {
            for (var c : conditions) if (!c.matches(ctx)) return false;
            return true;
        });
    }

    @Info("At least one of these holds")
    public Condition any(Condition... conditions) {
        var sb = new StringBuilder("any(");
        for (int i = 0; i < conditions.length; i++) sb.append(i > 0 ? ", " : "").append(conditions[i].describe());
        return new Condition(sb.append(')').toString(), ctx -> {
            for (var c : conditions) if (c.matches(ctx)) return true;
            return false;
        });
    }

    public Condition not(Condition condition) {
        return condition.negate();
    }

    public interface ContextTest {
        boolean test(EventContext ctx);
    }

    @Info("Any script function: ctx => boolean (ctx.player, ctx.entity, ctx.item, ctx.level, ctx.pos, ctx.id, ctx.kind)")
    public Condition test(ContextTest fn) {
        return new Condition("script test", fn::test);
    }

    @Info("Always true (a placeholder)")
    public Condition always() {
        return new Condition("always", ctx -> true);
    }

    /** Resolves a stack for a player-less item check. */
    static boolean isEmpty(ItemStack s) {
        return s == null || s.isEmpty();
    }

}
