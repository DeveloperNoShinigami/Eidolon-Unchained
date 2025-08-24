package com.bluelotuscoding.eidolonunchained.command;

import com.bluelotuscoding.eidolonunchained.prayer.PrayerSystem;
import com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

/**
 * Commands for interacting with AI deities through prayer
 */
public class PrayerCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("pray")
            .then(Commands.argument("deity", ResourceLocationArgument.id())
                .then(Commands.argument("type", StringArgumentType.word())
                    .executes(PrayerCommands::executePrayer)
                    .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(PrayerCommands::executePrayerWithMessage)
                    )
                )
            )
        );
        
        dispatcher.register(Commands.literal("deities")
            .executes(PrayerCommands::listDeities)
        );
        
        dispatcher.register(Commands.literal("deity")
            .then(Commands.argument("deity", ResourceLocationArgument.id())
                .executes(PrayerCommands::showDeityInfo)
            )
        );
    }
    
    private static int executePrayer(CommandContext<CommandSourceStack> context) {
        return executePrayerInternal(context, null);
    }
    
    private static int executePrayerWithMessage(CommandContext<CommandSourceStack> context) {
        String message = StringArgumentType.getString(context, "message");
        return executePrayerInternal(context, message);
    }
    
    private static int executePrayerInternal(CommandContext<CommandSourceStack> context, String message) {
        CommandSourceStack source = context.getSource();
        
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only players can pray to deities"));
            return 0;
        }
        
        try {
            ResourceLocation deityId = ResourceLocationArgument.getId(context, "deity");
            String prayerType = StringArgumentType.getString(context, "type");
            
            // Validate deity exists
            if (!DatapackDeityManager.hasDeity(deityId)) {
                player.sendSystemMessage(Component.literal("§cUnknown deity: " + deityId));
                return 0;
            }
            
            // Execute prayer
            if (message != null) {
                PrayerSystem.handlePrayer(player, deityId, prayerType, message);
            } else {
                PrayerSystem.handlePrayer(player, deityId, prayerType);
            }
            
            return 1;
            
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("§cFailed to commune with deity: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int listDeities(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        var deities = DatapackDeityManager.getAllDeities();
        if (deities.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No deities are currently available."), false);
            return 0;
        }
        
        source.sendSuccess(() -> Component.literal("§6Available Deities:"), false);
        for (var deity : deities.values()) {
            source.sendSuccess(() -> Component.literal("§e- " + deity.getId() + "§r: " + deity.getDisplayName()), false);
        }
        
        return deities.size();
    }
    
    private static int showDeityInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ResourceLocation deityId = ResourceLocationArgument.getId(context, "deity");
        
        var deity = DatapackDeityManager.getDeity(deityId);
        if (deity == null) {
            source.sendFailure(Component.literal("Unknown deity: " + deityId));
            return 0;
        }
        
        source.sendSuccess(() -> Component.literal("§6" + deity.getDisplayName()), false);
        source.sendSuccess(() -> Component.literal("§7ID: " + deity.getId()), false);
        source.sendSuccess(() -> Component.literal("§7Description: " + deity.getDescription()), false);
        
        if (source.getEntity() instanceof ServerPlayer player) {
            int reputation = (int) Math.round(deity.getPlayerReputation(player));
            source.sendSuccess(() -> Component.literal("§7Your Reputation: " + reputation + "/100"), false);
            
            // Show basic progression info
            String level = getProgressionLevel(reputation);
            source.sendSuccess(() -> Component.literal("§7Progression: " + level), false);
        }
        
        return 1;
    }
    
    private static String getProgressionLevel(int reputation) {
        if (reputation >= 75) return "§6Master";
        if (reputation >= 50) return "§eAdvanced";
        if (reputation >= 25) return "§aIntermediate";
        if (reputation >= 10) return "§2Novice";
        return "§7Beginner";
    }
}
