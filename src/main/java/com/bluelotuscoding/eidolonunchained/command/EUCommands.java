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
                .then(Commands.literal("chant")
                        .then(Commands.literal("assign")
                                .then(Commands.argument("slot", IntegerArgumentType.integer(1, PlayerChantState.SLOTS))
                                        .then(Commands.argument("sign", StringArgumentType.string())
                                                .suggests((c, b) -> SharedSuggestionProvider.suggest(Signs.getSigns().stream().map(s -> s.getRegistryName().toString()), b))
                                                .executes(c -> {
                                                    var player = c.getSource().getPlayerOrException();
                                                    int slot = IntegerArgumentType.getInteger(c, "slot");
                                                    var sign = StringArgumentType.getString(c, "sign");
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
