package com.bluelotuscoding.eidolonunchained.command;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.casting.PlayerChantState;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import elucent.eidolon.registries.Signs;

/**
 * {@code /eu} (decision D23). Phase 3 adds the player-facing chant subcommands (level 0: a player manages their own
 * slots); the admin subcommands from P6 (level 2) arrive with their phases.
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
}
