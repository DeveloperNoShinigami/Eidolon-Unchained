package com.bluelotuscoding.eidolonunchained.command;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.casting.PlayerChantState;
import com.bluelotuscoding.eidolonunchained.patron.Patrons;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import elucent.eidolon.capability.ISoul;
import elucent.eidolon.common.deity.Deities;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import elucent.eidolon.registries.Signs;

/**
 * {@code /eu} (decision D23). Phase 3 adds the player-facing chant subcommands (level 0: a player manages their own
 * slots); the admin subcommands from P6 (level 2) arrive with their phases. Phase 5: {@code patron get|set|revoke}
 * and {@code mana get|set|add|take|max} (level 2).
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MOD_ID)
public final class EUCommands {
    private EUCommands() {
    }

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("eu")
                // /eu scroll <chant>: a written chant scroll (what the Scriptorium would write), for testing and admins.
                .then(Commands.literal("scroll").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("chant", net.minecraft.commands.arguments.ResourceLocationArgument.id())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(elucent.eidolon.registries.Spells.getSpellMap().keySet().stream().map(Object::toString), b))
                                .executes(c -> {
                                    var player = c.getSource().getPlayerOrException();
                                    var id = net.minecraft.commands.arguments.ResourceLocationArgument.getId(c, "chant");
                                    var scroll = com.bluelotuscoding.eidolonunchained.imbue.ImbueRecipe.scrollFor(id);
                                    if (scroll.isEmpty()) {
                                        c.getSource().sendFailure(Component.literal("No chant recipe (sign sequence) for " + id));
                                        return 0;
                                    }
                                    if (!player.getInventory().add(scroll)) player.drop(scroll, false);
                                    c.getSource().sendSuccess(() -> Component.literal("Scroll of " + id), false);
                                    return 1;
                                })))
                // /eu discoveries list | reset [id]: the active discoveries; forget them for yourself (testing)
                .then(Commands.literal("discoveries")
                        .then(Commands.literal("list").executes(c -> {
                            var all = com.bluelotuscoding.eidolonunchained.api.condition.Discoveries.all();
                            c.getSource().sendSuccess(() -> Component.literal(all.size() + " discoveries"), false);
                            for (var d : all) c.getSource().sendSuccess(() -> Component.literal(" " + d), false);
                            return all.size();
                        }))
                        .then(Commands.literal("reset").requires(s -> s.hasPermission(2))
                                .executes(c -> {
                                    com.bluelotuscoding.eidolonunchained.api.condition.Discovery.forget(c.getSource().getPlayerOrException(), null);
                                    com.bluelotuscoding.eidolonunchained.patron.Callings.forget(c.getSource().getPlayerOrException());
                                    c.getSource().sendSuccess(() -> Component.literal("Your discoveries are forgotten"), false);
                                    return 1;
                                })
                                .then(Commands.argument("id", net.minecraft.commands.arguments.ResourceLocationArgument.id())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(com.bluelotuscoding.eidolonunchained.api.condition.Discoveries.all().stream().map(d -> d.id().toString()), b))
                                        .executes(c -> {
                                            var id = net.minecraft.commands.arguments.ResourceLocationArgument.getId(c, "id");
                                            com.bluelotuscoding.eidolonunchained.api.condition.Discovery.forget(c.getSource().getPlayerOrException(), id);
                                            c.getSource().sendSuccess(() -> Component.literal("Forgotten: " + id), false);
                                            return 1;
                                        }))))
                .then(patron())
                .then(mana())
                .then(Commands.literal("chant")
                        .then(Commands.literal("assign")
                                .then(Commands.argument("slot", IntegerArgumentType.integer(1, PlayerChantState.SLOTS))
                                        .then(Commands.argument("sign", net.minecraft.commands.arguments.ResourceLocationArgument.id())
                                                .suggests((c, b) -> SharedSuggestionProvider.suggest(Signs.getSigns().stream().map(s -> s.getRegistryName().toString()), b))
                                                .executes(c -> {
                                                    var player = c.getSource().getPlayerOrException();
                                                    int slot = IntegerArgumentType.getInteger(c, "slot");
                                                    var sign = net.minecraft.commands.arguments.ResourceLocationArgument.getId(c, "sign").toString();
                                                    PlayerChantState.of(player).assign(slot - 1, sign);
                                                    var now = PlayerChantState.of(player).slotIds().get(slot - 1);
                                                    c.getSource().sendSuccess(() -> Component.translatable(now.isEmpty() ? "command.eidolonunchained.chant.assign_failed" : "command.eidolonunchained.chant.assigned", slot, now), false);
                                                    return now.isEmpty() ? 0 : 1;
                                                }))))
                        .then(Commands.literal("unassign")
                                .then(Commands.argument("slot", IntegerArgumentType.integer(1, PlayerChantState.SLOTS))
                                        .executes(c -> {
                                            var player = c.getSource().getPlayerOrException();
                                            int slot = IntegerArgumentType.getInteger(c, "slot");
                                            PlayerChantState.of(player).assign(slot - 1, null);
                                            c.getSource().sendSuccess(() -> Component.translatable("command.eidolonunchained.chant.unassigned", slot), false);
                                            return 1;
                                        })))
                        .then(Commands.literal("slots").executes(c -> {
                            var player = c.getSource().getPlayerOrException();
                            var slots = PlayerChantState.of(player).slotIds();
                            for (int i = 0; i < slots.size(); i++) {
                                final int n = i + 1; final String s = slots.get(i);
                                c.getSource().sendSuccess(() -> Component.literal(n + ": " + (s.isEmpty() ? "-" : s)), false);
                            }
                            return 1;
                        }))
                        .then(Commands.literal("clear").executes(c -> {
                            var player = c.getSource().getPlayerOrException();
                            PlayerChantState.of(player).clear(PlayerChantState.ClearReason.PLAYER);
                            return 1;
                        }))));
    }

    // ---- /eu patron get|set|revoke (D47, level 2) ----

    private static final SuggestionProvider<CommandSourceStack> DEITIES = (c, b) ->
            SharedSuggestionProvider.suggest(Deities.getDeities().stream().map(d -> d.getId().toString()), b);

    // Players pledge (major patron, minor pledges, reputation); a mob simply has a patron, saved on it (D55).
    private static LiteralArgumentBuilder<CommandSourceStack> patron() {
        return Commands.literal("patron").requires(s -> s.hasPermission(2))
                .then(Commands.literal("get")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(c -> {
                                    var target = EntityArgument.getEntity(c, "target");
                                    var none = Component.translatable("command.eidolonunchained.patron.none");
                                    if (target instanceof net.minecraft.server.level.ServerPlayer player) {
                                        var major = Patrons.majorPatron(player);
                                        var minor = Patrons.minorPledges(player);
                                        c.getSource().sendSuccess(() -> Component.translatable("command.eidolonunchained.patron.get", player.getDisplayName(),
                                                major == null ? none : Component.literal(major.toString()),
                                                minor.isEmpty() ? none : Component.literal(String.join(", ", minor.stream().map(Object::toString).toList()))), false);
                                        return major == null ? 0 : 1;
                                    }
                                    var patron = Patrons.patronOf(target);
                                    c.getSource().sendSuccess(() -> Component.translatable("command.eidolonunchained.patron.get_mob", target.getDisplayName(),
                                            patron == null ? none : Component.literal(patron.toString())), false);
                                    return patron == null ? 0 : 1;
                                })))
                .then(Commands.literal("set")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .then(Commands.argument("deity", ResourceLocationArgument.id()).suggests(DEITIES)
                                        .executes(c -> {
                                            var target = EntityArgument.getEntity(c, "target");
                                            var deity = ResourceLocationArgument.getId(c, "deity");
                                            if (target instanceof net.minecraft.server.level.ServerPlayer player) {
                                                var failure = Patrons.pledge(player, deity);
                                                if (failure != null) {
                                                    c.getSource().sendFailure(failure);
                                                    return 0;
                                                }
                                            } else if (target instanceof net.minecraft.world.entity.LivingEntity mob) {
                                                if (Deities.find(deity) == null) {
                                                    c.getSource().sendFailure(Component.translatable("eidolonunchained.patron.unknown_deity", deity.toString()));
                                                    return 0;
                                                }
                                                Patrons.setMobPatron(mob, deity);
                                            } else {
                                                c.getSource().sendFailure(Component.translatable("command.eidolonunchained.patron.not_living", target.getDisplayName()));
                                                return 0;
                                            }
                                            c.getSource().sendSuccess(() -> Component.translatable("command.eidolonunchained.patron.set", target.getDisplayName(), deity.toString()), true);
                                            return 1;
                                        }))))
                .then(Commands.literal("revoke")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(c -> revoke(c, null, 0))
                                .then(Commands.argument("deity", ResourceLocationArgument.id()).suggests(DEITIES)
                                        .executes(c -> revoke(c, ResourceLocationArgument.getId(c, "deity"), 0))
                                        .then(Commands.argument("reputation", DoubleArgumentType.doubleArg())
                                                .executes(c -> revoke(c, ResourceLocationArgument.getId(c, "deity"), DoubleArgumentType.getDouble(c, "reputation")))))));
    }

    private static int revoke(CommandContext<CommandSourceStack> c, ResourceLocation deity, double reputation) throws CommandSyntaxException {
        var target = EntityArgument.getEntity(c, "target");
        if (!(target instanceof net.minecraft.server.level.ServerPlayer player)) {
            // a mob: clears the patron saved on it (its type or caster profile may still give it one)
            if (!(target instanceof net.minecraft.world.entity.LivingEntity mob)) {
                c.getSource().sendFailure(Component.translatable("command.eidolonunchained.patron.not_living", target.getDisplayName()));
                return 0;
            }
            Patrons.setMobPatron(mob, null);
            var now = Patrons.patronOf(mob);
            c.getSource().sendSuccess(() -> Component.translatable("command.eidolonunchained.patron.mob_cleared", target.getDisplayName(),
                    now == null ? Component.translatable("command.eidolonunchained.patron.none") : Component.literal(now.toString())), true);
            return 1;
        }
        var revoked = Patrons.revoke(player, deity, reputation);
        if (revoked.isEmpty()) {
            c.getSource().sendFailure(deity == null
                    ? Component.translatable("command.eidolonunchained.patron.revoke_none", player.getDisplayName())
                    : Component.translatable("eidolonunchained.patron.not_pledged", player.getDisplayName(), deity.toString()));
            return 0;
        }
        for (var d : revoked)
            c.getSource().sendSuccess(() -> Component.translatable("command.eidolonunchained.patron.revoked", player.getDisplayName(), d.toString(), reputation), true);
        return revoked.size();
    }

    // ---- /eu mana get|set|add|take|max (D50, level 2): Eidolon's ISoul magic, called mana as in game ----

    private enum ManaOp { SET, ADD, TAKE, MAX }

    private static LiteralArgumentBuilder<CommandSourceStack> mana() {
        var root = Commands.literal("mana").requires(s -> s.hasPermission(2))
                .then(Commands.literal("get")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .executes(c -> mana(c, null, 0))));
        for (var op : ManaOp.values()) {
            root.then(Commands.literal(op.name().toLowerCase(java.util.Locale.ROOT))
                    .then(Commands.argument("targets", EntityArgument.entities())
                            .then(Commands.argument("amount", FloatArgumentType.floatArg(0))
                                    .executes(c -> mana(c, op, FloatArgumentType.getFloat(c, "amount"))))));
        }
        return root;
    }

    private static int mana(CommandContext<CommandSourceStack> c, ManaOp op, float amount) throws CommandSyntaxException {
        int n = 0;
        for (var entity : EntityArgument.getEntities(c, "targets")) {
            if (!(entity instanceof LivingEntity living)) continue;
            var soul = living.getCapability(ISoul.INSTANCE).resolve().orElse(null);
            if (soul == null) continue;
            if (op != null) {
                switch (op) {
                    case SET -> soul.setMagic(amount);
                    case ADD -> soul.giveMagic(amount);
                    case TAKE -> soul.takeMagic(amount);
                    case MAX -> soul.setMaxMagic(amount);
                }
                // a player's max is re-floored by their stages (Patrons); the value set here becomes Eidolon's own max
                if (op == ManaOp.MAX && living instanceof ServerPlayer sp) Patrons.refreshMana(sp);
                Patrons.syncSoul(living);
            }
            n++;
            c.getSource().sendSuccess(() -> Component.translatable("command.eidolonunchained.mana.show", living.getDisplayName(),
                    String.format(java.util.Locale.ROOT, "%.1f", soul.getMagic()), String.format(java.util.Locale.ROOT, "%.1f", soul.getMaxMagic())), op != null);
        }
        if (n == 0) c.getSource().sendFailure(Component.translatable("command.eidolonunchained.mana.no_targets"));
        return n;
    }
}
