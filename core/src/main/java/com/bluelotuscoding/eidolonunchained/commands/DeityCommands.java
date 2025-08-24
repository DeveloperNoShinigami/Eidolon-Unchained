package com.bluelotuscoding.eidolonunchained.commands;

import com.bluelotuscoding.eidolonunchained.chat.DeityChat;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Commands for deity interaction system
 */
@Mod.EventBusSubscriber
public class DeityCommands {
    
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        // Register deity conversation commands
        dispatcher.register(Commands.literal("deity_accept_commands")
            .executes(DeityCommands::acceptCommands));
        
        dispatcher.register(Commands.literal("deity_decline_commands")
            .executes(DeityCommands::declineCommands));
    }
    
    private static int acceptCommands(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }
        
        // Execute pending commands through the chat system
        DeityChat.executePlayerCommands(player, true);
        return 1;
    }
    
    private static int declineCommands(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }
        
        // Decline pending commands through the chat system
        DeityChat.executePlayerCommands(player, false);
        return 1;
    }
}
