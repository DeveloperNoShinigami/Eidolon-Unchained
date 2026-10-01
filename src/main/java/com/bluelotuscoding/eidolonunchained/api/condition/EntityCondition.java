package com.bluelotuscoding.eidolonunchained.api.condition;

import dev.latvian.mods.kubejs.typings.Info;
import elucent.eidolon.util.EntityUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraftforge.common.Tags;

/** {@code C.entity('minecraft:zombie' | '#tag')} and its narrowing calls; tested against the event's subject entity. */
public final class EntityCondition extends Condition {
    EntityCondition(String description, java.util.function.Predicate<EventContext> base) {
        super(description, base);
    }

    private static LivingEntity e(EventContext ctx) {
        return ctx.getEntity();
    }

    @Info("Custom name equals (as shown)")
    public EntityCondition named(String name) {
        return narrow("named '" + name + "'", ctx -> e(ctx) != null && e(ctx).hasCustomName() && name.equals(e(ctx).getCustomName().getString()));
    }

    @Info("Its NBT contains this SNBT, e.g. '{eu_ritual_spawned:1b}' (Forge data under ForgeData)")
    public EntityCondition nbt(String snbt) {
        CompoundTag expected;
        try {
            expected = TagParser.parseTag(snbt);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Eidolon Unchained: bad SNBT '" + snbt + "': " + ex.getMessage());
        }
        return narrow("nbt " + snbt, ctx -> e(ctx) != null && NbtUtils.compareNbt(expected, e(ctx).saveWithoutId(new CompoundTag()), true));
    }

    @Info("A boss (entity type tag forge:bosses)")
    public EntityCondition boss() {
        return narrow("boss", ctx -> e(ctx) != null && e(ctx).getType().is(Tags.EntityTypes.BOSSES));
    }

    @Info("Mob category: 'monster', 'creature', 'ambient', 'water_creature', 'misc' …")
    public EntityCondition category(String category) {
        return narrow("category " + category, ctx -> e(ctx) != null && e(ctx).getType().getCategory().getName().equalsIgnoreCase(category));
    }

    public EntityCondition tamed() {
        return narrow("tamed", ctx -> e(ctx) instanceof OwnableEntity o && o.getOwnerUUID() != null
                || e(ctx) instanceof AbstractHorse h && h.isTamed());
    }

    @Info("Enthralled by anyone (Eidolon's thrall marker)")
    public EntityCondition enthralled() {
        return narrow("enthralled", ctx -> e(ctx) != null && EntityUtil.isEnthralled(e(ctx)));
    }

    public EntityCondition undead() {
        return narrow("undead", ctx -> e(ctx) != null && e(ctx).getMobType() == MobType.UNDEAD);
    }

    @Info("Follows this god: a player's major patron, or a mob's patron (D55)")
    public EntityCondition patron(String deityId) {
        var id = net.minecraft.resources.ResourceLocation.tryParse(deityId);
        if (id == null) throw new IllegalArgumentException("Eidolon Unchained: bad deity id '" + deityId + "'");
        return narrow("patron " + id, ctx -> e(ctx) != null && id.equals(com.bluelotuscoding.eidolonunchained.patron.Patrons.patronOf(e(ctx))));
    }

    @Info("Health below this fraction of max (0..1)")
    public EntityCondition healthBelow(double fraction) {
        return narrow("health < " + fraction, ctx -> e(ctx) != null && e(ctx).getHealth() < e(ctx).getMaxHealth() * fraction);
    }
}
