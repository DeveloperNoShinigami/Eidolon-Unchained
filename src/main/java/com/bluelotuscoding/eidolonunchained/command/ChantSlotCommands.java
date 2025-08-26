package com.bluelotuscoding.eidolonunchained.command;

import com.bluelotuscoding.eidolonunchained.chant.ChantSlotManager;
import com.bluelotuscoding.eidolonunchained.chant.DatapackChantManager;
import com.bluelotuscoding.eidolonunchained.chant.DatapackChant;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

/**
 * Commands for managing flexible chant slot assignments.
 * Allows players to assign, clear, and view their chant configurations.
 */
public class ChantSlotCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("chant")
            .then(Commands.literal("assign")
                .then(Commands.argument("slot", IntegerArgumentType.integer(1, 4))
                    .then(Commands.argument("chant_id", ResourceLocationArgument.id())
                        .executes(ChantSlotCommands::assignChant)
                    )
                )
            )
            .then(Commands.literal("clear")
                .then(Commands.argument("slot", IntegerArgumentType.integer(1, 4))
                    .executes(ChantSlotCommands::clearChant)
                )
            )
            .then(Commands.literal("list")
                .executes(ChantSlotCommands::listAssignments)
            )
            .then(Commands.literal("available")
                .executes(ChantSlotCommands::listAvailableChants)
            )
            .then(Commands.literal("info")
                .then(Commands.argument("chant_id", ResourceLocationArgument.id())
                    .executes(ChantSlotCommands::showChantInfo)
                )
            )
        );
    }
    
    private static int assignChant(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        int slot = IntegerArgumentType.getInteger(context, "slot");
        ResourceLocation chantId = ResourceLocationArgument.getId(context, "chant_id");
        
        boolean success = ChantSlotManager.assignChantToSlot(player, slot, chantId);
        return success ? 1 : 0;
    }
    
    private static int clearChant(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        int slot = IntegerArgumentType.getInteger(context, "slot");
        boolean success = ChantSlotManager.clearChantSlot(player, slot);
        return success ? 1 : 0;
    }
    
    private static int listAssignments(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        String assignments = ChantSlotManager.getPlayerChantAssignments(player);
        player.sendSystemMessage(Component.literal("§6=== Your Chant Assignments ==="));
        player.sendSystemMessage(Component.literal("§7" + assignments));
        
        return 1;
    }
    
    private static int listAvailableChants(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        Collection<ResourceLocation> chantIds = DatapackChantManager.getAllChantIds();
        player.sendSystemMessage(Component.literal("§6=== Available Chants ==="));
        
        for (ResourceLocation chantId : chantIds) {
            DatapackChant chant = DatapackChantManager.getChant(chantId);
            if (chant != null) {
                String deityInfo = chant.getLinkedDeity() != null ? " §c[Deity: " + chant.getLinkedDeity().getPath() + "]" : " §a[No Deity Required]";
                player.sendSystemMessage(Component.literal("§7- " + chantId + " - §f" + chant.getName() + deityInfo));
            }
        }
        
        return 1;
    }
    
    private static int showChantInfo(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        ResourceLocation chantId = ResourceLocationArgument.getId(context, "chant_id");
        DatapackChant chant = DatapackChantManager.getChant(chantId);
        
        if (chant == null) {
            player.sendSystemMessage(Component.literal("§cChant not found: " + chantId));
            return 0;
        }
        
        player.sendSystemMessage(Component.literal("§6=== Chant Information ==="));
        player.sendSystemMessage(Component.literal("§7Name: §f" + chant.getName()));
        player.sendSystemMessage(Component.literal("§7Description: §f" + chant.getDescription()));
        
        if (chant.getLinkedDeity() != null) {
            player.sendSystemMessage(Component.literal("§7Linked Deity: §c" + chant.getLinkedDeity()));
            player.sendSystemMessage(Component.literal("§7Requires Effigy: §cYes"));
        } else {
            player.sendSystemMessage(Component.literal("§7Linked Deity: §aNone"));
            player.sendSystemMessage(Component.literal("§7Requires Effigy: §aNo"));
        }
        
        // Show sign sequence
        if (chant.getSignSequence() != null && !chant.getSignSequence().isEmpty()) {
            StringBuilder signs = new StringBuilder();
            for (int i = 0; i < chant.getSignSequence().size(); i++) {
                if (i > 0) signs.append(" → ");
                signs.append(chant.getSignSequence().get(i).getPath().replace("_", " "));
            }
            player.sendSystemMessage(Component.literal("§7Sign Sequence: §f" + signs.toString()));
        }
        
        player.sendSystemMessage(Component.literal("§7Difficulty: §f" + chant.getDifficulty() + "/5"));
        
        return 1;
    }
}
