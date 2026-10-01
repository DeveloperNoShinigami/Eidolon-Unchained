package com.bluelotuscoding.eidolonunchained.api.condition;

import com.bluelotuscoding.eidolonunchained.api.Ids;
import com.bluelotuscoding.eidolonunchained.api.PlayerHelper;
import com.bluelotuscoding.eidolonunchained.patron.Patrons;
import dev.latvian.mods.kubejs.typings.Info;
import elucent.eidolon.common.deity.Deities;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

/** {@code C.player()} and its narrowing calls; tested against the event's player (false when there is none). */
public final class PlayerCondition extends Condition {
    PlayerCondition() {
        super("player", ctx -> ctx.getPlayer() != null);
    }

    private static Player p(EventContext ctx) {
        return ctx.getPlayer();
    }

    private static PlayerHelper h(EventContext ctx) {
        return new PlayerHelper(ctx.getPlayer());
    }

    @Info("Reputation with the deity is at least this")
    public PlayerCondition reputation(String deityId, double min) {
        return narrow("reputation " + deityId + " >= " + min, ctx -> h(ctx).reputation(deityId) >= min);
    }

    @Info("Has reached this stage of the deity's progression (its reputation threshold)")
    public PlayerCondition stage(String deityId, String stageId) {
        var stage = Ids.of(stageId, "stage");
        return narrow("stage " + deityId + " " + stage, ctx -> {
            var deity = Deities.find(Ids.of(deityId, "deity"));
            if (deity == null) return false;
            double rep = h(ctx).reputation(deityId);
            for (var s : deity.getProgression().getSteps().values()) if (s.id().equals(stage)) return rep >= s.rep();
            return false;
        });
    }

    @Info("The player's major patron is this deity")
    public PlayerCondition patron(String deityId) {
        var deity = Ids.of(deityId, "deity");
        return narrow("patron " + deity, ctx -> deity.equals(Patrons.majorPatron(p(ctx))));
    }

    @Info("The player has a major patron")
    public PlayerCondition hasPatron() {
        return narrow("has a patron", ctx -> Patrons.majorPatron(p(ctx)) != null);
    }

    @Info("The player is pledged to this deity (as major patron or a minor pledge)")
    public PlayerCondition pledged(String deityId) {
        var deity = Ids.of(deityId, "deity");
        return narrow("pledged " + deity, ctx -> Patrons.pledged(p(ctx), deity));
    }

    public PlayerCondition knowsResearch(String id) {
        return narrow("knows research " + id, ctx -> h(ctx).knowsResearch(id));
    }

    public PlayerCondition knowsFact(String id) {
        return narrow("knows fact " + id, ctx -> h(ctx).knowsFact(id));
    }

    public PlayerCondition knowsSign(String id) {
        return narrow("knows sign " + id, ctx -> h(ctx).knowsSign(id));
    }

    public PlayerCondition knowsRune(String id) {
        return narrow("knows rune " + id, ctx -> h(ctx).knowsRune(id));
    }

    @Info("Has completed this advancement")
    public PlayerCondition advancement(String advancementId) {
        var rl = Ids.of(advancementId, "advancement");
        return narrow("advancement " + rl, ctx -> {
            if (!(p(ctx) instanceof ServerPlayer sp)) return false;
            var adv = sp.server.getAdvancements().getAdvancement(rl);
            return adv != null && sp.getAdvancements().getOrStartProgress(adv).isDone();
        });
    }

    @Info("Has this potion effect")
    public PlayerCondition effect(String effectId) {
        var rl = Ids.of(effectId, "effect");
        return narrow("effect " + rl, ctx -> {
            var effect = ForgeRegistries.MOB_EFFECTS.getValue(rl);
            return effect != null && p(ctx).hasEffect(effect);
        });
    }

    @Info("Holds a matching item in either hand")
    public PlayerCondition holding(ItemCondition item) {
        return narrow("holding (" + item.describe() + ")", ctx -> item.testStack(p(ctx).getMainHandItem()) || item.testStack(p(ctx).getOffhandItem()));
    }

    @Info("Wears a matching item in any armour slot")
    public PlayerCondition wearing(ItemCondition item) {
        return narrow("wearing (" + item.describe() + ")", ctx -> {
            for (var slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET})
                if (item.testStack(p(ctx).getItemBySlot(slot))) return true;
            return false;
        });
    }

    public PlayerCondition creative() {
        return narrow("creative", ctx -> p(ctx).isCreative());
    }
}
