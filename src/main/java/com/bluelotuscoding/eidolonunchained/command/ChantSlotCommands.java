package com.bluelotuscoding.eidolonunchained.command;

import com.bluelotuscoding.eidolonunchained.chant.SlotAssignmentManager;
import com.bluelotuscoding.eidolonunchained.chant.DatapackChantManager;
import com.bluelotuscoding.eidolonunchained.chant.DatapackChant;
import com.bluelotuscoding.eidolonunchained.config.ChantCastingConfig;
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
 * Commands for managing flexible slot assignments.
 * Supports both individual sign assignment and full chant assignment.
 */
public class ChantSlotCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("chant")
            .then(Commands.literal("assign-sign")
                .then(Commands.argument("slot", IntegerArgumentType.integer(1, 4))
                    .then(Commands.argument("sign_id", ResourceLocationArgument.id())
                        .executes(ChantSlotCommands::assignSign)
                    )
                )
            )
            .then(Commands.literal("assign-chant")
                .then(Commands.argument("slot", IntegerArgumentType.integer(1, 4))
                    .then(Commands.argument("chant_id", ResourceLocationArgument.id())
                        .executes(ChantSlotCommands::assignChant)
                    )
                )
            )
            .then(Commands.literal("clear")
                .then(Commands.argument("slot", IntegerArgumentType.integer(1, 4))
                    .executes(ChantSlotCommands::clearSlot)
                )
            )
            .then(Commands.literal("list")
                .executes(ChantSlotCommands::listAssignments)
            )
            .then(Commands.literal("available-signs")
                .executes(ChantSlotCommands::listAvailableSigns)
            )
            .then(Commands.literal("available-chants")
                .executes(ChantSlotCommands::listAvailableChants)
            )
            .then(Commands.literal("info")
                .then(Commands.argument("chant_id", ResourceLocationArgument.id())
                    .executes(ChantSlotCommands::showChantInfo)
                )
            )
            .then(Commands.literal("mode")
                .executes(ChantSlotCommands::showCurrentMode)
            )
        );
    }
    
    private static int assignSign(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        int slot = IntegerArgumentType.getInteger(context, "slot");
        ResourceLocation signId = ResourceLocationArgument.getId(context, "sign_id");
        
        boolean success = SlotAssignmentManager.assignSignToSlot(player, slot, signId);
        return success ? 1 : 0;
    }
    
    private static int assignChant(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        int slot = IntegerArgumentType.getInteger(context, "slot");
        ResourceLocation chantId = ResourceLocationArgument.getId(context, "chant_id");
        
        boolean success = SlotAssignmentManager.assignChantToSlot(player, slot, chantId);
        return success ? 1 : 0;
    }
    
    private static int clearSlot(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        int slot = IntegerArgumentType.getInteger(context, "slot");
        boolean success = SlotAssignmentManager.clearSlot(player, slot);
        return success ? 1 : 0;
    }
    
    private static int listAssignments(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        player.sendSystemMessage(Component.literal("§6=== Your Slot Assignments ==="));
        player.sendSystemMessage(Component.literal("§7Current mode: " + ChantCastingConfig.getCurrentMode()));
        
        for (int i = 1; i <= 4; i++) {
            SlotAssignmentManager.SlotAssignment assignment = SlotAssignmentManager.getSlotAssignment(player, i);
            if (assignment != null) {
                String typeDisplay = assignment.type == SlotAssignmentManager.SlotType.SIGN ? "Sign" : "Chant";
                player.sendSystemMessage(Component.literal("§f• Slot " + i + ": " + typeDisplay + " - " + assignment.displayName));
            } else {
                player.sendSystemMessage(Component.literal("§7• Slot " + i + ": (empty)"));
            }
        }
        
        player.sendSystemMessage(Component.literal("§7Use /chant assign-sign or /chant assign-chant to configure slots"));
        return 1;
    }
    
    private static int listAvailableSigns(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        SlotAssignmentManager.listAvailableSigns(player);
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
    
    private static int showCurrentMode(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        ChantCastingConfig.CastingMode mode = ChantCastingConfig.getCurrentMode();
        player.sendSystemMessage(Component.literal("§6=== Current Casting Mode ==="));
        player.sendSystemMessage(Component.literal("§7Mode: §f" + mode));
        
        switch (mode) {
            case INDIVIDUAL_SIGNS -> {
                player.sendSystemMessage(Component.literal("§7Description: Cast individual signs assigned to keybinds"));
                player.sendSystemMessage(Component.literal("§7Usage: Assign signs to slots, press keys to cast signs"));
                player.sendSystemMessage(Component.literal("§7Example: /chant assign-sign 1 eidolon:wicked"));
            }
            case FULL_CHANT -> {
                player.sendSystemMessage(Component.literal("§7Description: Cast full chant sequences with one key"));
                player.sendSystemMessage(Component.literal("§7Usage: Assign chants to slots, press keys to cast full sequence"));
                player.sendSystemMessage(Component.literal("§7Example: /chant assign-chant 1 eidolonunchained:shadow_communion"));
            }
            case HYBRID -> {
                player.sendSystemMessage(Component.literal("§7Description: Support both individual signs and full chants"));
                player.sendSystemMessage(Component.literal("§7Usage: Assign either signs or chants to different slots"));
                player.sendSystemMessage(Component.literal("§7Mixed example: Signs in slots 1-2, chants in slots 3-4"));
            }
        }
        
        return 1;
    }
}
