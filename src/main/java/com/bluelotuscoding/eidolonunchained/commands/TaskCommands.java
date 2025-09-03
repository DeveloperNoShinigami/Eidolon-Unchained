package com.bluelotuscoding.eidolonunchained.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.ai.PlayerContextTracker;
import com.bluelotuscoding.eidolonunchained.ai.AIDeityManager;
import com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig;
import com.bluelotuscoding.eidolonunchained.ai.TaskSystemConfig;

import java.util.List;
import java.util.Map;

/**
 * Command handlers for the deity task system
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TaskCommands {
    
    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        dispatcher.register(Commands.literal("dtask")
            .then(Commands.literal("assign")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("deity", StringArgumentType.string())
                        .then(Commands.argument("taskId", StringArgumentType.string())
                            .executes(TaskCommands::assignTask)))))
            .then(Commands.literal("assignany")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("taskId", StringArgumentType.string())
                        .executes(TaskCommands::assignAnyTask))))
            .then(Commands.literal("complete")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("taskId", StringArgumentType.string())
                        .executes(TaskCommands::completeTask))))
            .then(Commands.literal("list")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(TaskCommands::listTasks)))
            .then(Commands.literal("reputation")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("deity", StringArgumentType.string())
                        .executes(TaskCommands::checkSpecificReputation))))
            .then(Commands.literal("repall")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(TaskCommands::checkAllReputation)))
            .then(Commands.literal("ritual")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("ritualId", StringArgumentType.string())
                        .executes(TaskCommands::markRitualComplete))))
        );
    }
    
    private static int assignTask(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            String taskId = StringArgumentType.getString(context, "taskId");
            String deityIdStr = StringArgumentType.getString(context, "deity");
            ResourceLocation deityId = new ResourceLocation(deityIdStr);
            
            // Get AI config to find task template
            AIDeityConfig config = AIDeityManager.getInstance().getAIConfig(deityId);
            if (config == null) {
                context.getSource().sendFailure(Component.literal("Unknown deity: " + deityIdStr));
                return 0;
            }
            
            // Find the task template
            TaskSystemConfig.TaskTemplate taskTemplate = null;
            for (TaskSystemConfig.TaskTemplate template : config.task_config.availableTasks) {
                if (template.taskId.equals(taskId)) {
                    taskTemplate = template;
                    break;
                }
            }
            
            if (taskTemplate == null) {
                context.getSource().sendFailure(Component.literal("Unknown task: " + taskId));
                return 0;
            }
            
            // Check if player meets reputation requirement
            double playerReputation = com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager.getDeity(deityId).getPlayerReputation(targetPlayer);
            if (playerReputation < taskTemplate.reputationRequired) {
                context.getSource().sendFailure(Component.literal("Player needs at least " + taskTemplate.reputationRequired + " reputation"));
                return 0;
            }
            
            // Assign the task
            PlayerContextTracker.assignTask(targetPlayer, taskId, taskTemplate.description, deityId, taskTemplate.reputationReward);
            
            targetPlayer.sendSystemMessage(Component.translatable("eidolonunchained.task.assigned", taskTemplate.description));
            targetPlayer.sendSystemMessage(Component.translatable("eidolonunchained.task.reward", taskTemplate.reputationReward));
            
            context.getSource().sendSuccess(() -> Component.literal("Assigned task '" + taskId + "' to " + targetPlayer.getName().getString()), true);
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error assigning task: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int assignAnyTask(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            String taskId = StringArgumentType.getString(context, "taskId");
            
            // Search all deities for this task
            TaskSystemConfig.TaskTemplate taskTemplate = null;
            ResourceLocation foundDeityId = null;
            
            for (AIDeityConfig config : AIDeityManager.getInstance().getAllConfigs()) {
                for (TaskSystemConfig.TaskTemplate template : config.task_config.availableTasks) {
                    if (template.taskId.equals(taskId)) {
                        taskTemplate = template;
                        foundDeityId = config.deity_id;
                        break;
                    }
                }
            }
            
            if (taskTemplate == null) {
                context.getSource().sendFailure(Component.literal("Unknown task: " + taskId));
                return 0;
            }
            
            // Assign the task
            PlayerContextTracker.assignTask(targetPlayer, taskId, taskTemplate.description, foundDeityId, taskTemplate.reputationReward);
            
            targetPlayer.sendSystemMessage(Component.translatable("eidolonunchained.task.assigned", taskTemplate.description));
            targetPlayer.sendSystemMessage(Component.translatable("eidolonunchained.task.reward", taskTemplate.reputationReward));
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error assigning task: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int completeTask(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            String taskId = StringArgumentType.getString(context, "taskId");
            
            PlayerContextTracker.EnhancedPlayerContext playerContext = PlayerContextTracker.getContext(player.getUUID());
            if (playerContext == null || !playerContext.activeTasks.containsKey(taskId)) {
                context.getSource().sendFailure(Component.literal("Player does not have this task active"));
                return 0;
            }
            
            // Validate task requirements
            PlayerContextTracker.PlayerTask task = playerContext.activeTasks.get(taskId);
            boolean canComplete = true;
            for (String requirement : getTaskRequirements(taskId)) {
                if (!validateRequirement(player, requirement)) {
                    context.getSource().sendFailure(Component.literal("Requirement not met: " + requirement));
                    canComplete = false;
                    break;
                }
            }
            
            if (canComplete) {
                PlayerContextTracker.completeTask(player, taskId);
                player.sendSystemMessage(Component.translatable("eidolonunchained.task.completed_reputation", task.reputationReward));
                
                // Execute task rewards
                executeTaskRewards(player, taskId);
                
                return 1;
            } else {
                context.getSource().sendFailure(Component.literal("Task requirements not met"));
                return 0;
            }
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error completing task: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int listTasks(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            PlayerContextTracker.EnhancedPlayerContext playerContext = PlayerContextTracker.getContext(player.getUUID());
            
            if (playerContext == null || playerContext.activeTasks.isEmpty()) {
                player.sendSystemMessage(Component.translatable("eidolonunchained.task.no_active"));
                return 1;
            }
            
            player.sendSystemMessage(Component.translatable("eidolonunchained.task.header"));
            for (PlayerContextTracker.PlayerTask task : playerContext.activeTasks.values()) {
                player.sendSystemMessage(Component.literal("§e" + task.description));
                player.sendSystemMessage(Component.literal("  §7Reward: §6" + task.reputationReward + " reputation points"));
            }
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error listing tasks: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int checkSpecificReputation(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            String deityIdStr = StringArgumentType.getString(context, "deity");
            ResourceLocation deityId = new ResourceLocation(deityIdStr);
            
            com.bluelotuscoding.eidolonunchained.deity.DatapackDeity deity = 
                com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager.getDeity(deityId);
            
            if (deity != null) {
                double reputation = deity.getPlayerReputation(player);
                player.sendSystemMessage(Component.literal("§6Reputation with " + deityIdStr + ": §e" + (int)reputation + " points"));
            } else {
                // Try Eidolon reputation system
                try {
                    elucent.eidolon.capability.IReputation reputationCap = player.level().getCapability(elucent.eidolon.capability.IReputation.INSTANCE).orElse(null);
                    if (reputationCap != null) {
                        double reputation = reputationCap.getReputation(player, deityId);
                        player.sendSystemMessage(Component.literal("§6Reputation with " + deityIdStr + ": §e" + (int)reputation + " points"));
                    } else {
                        player.sendSystemMessage(Component.literal("§cCould not access reputation data"));
                    }
                } catch (Exception e) {
                    player.sendSystemMessage(Component.literal("§cUnknown deity: " + deityIdStr));
                }
            }
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error checking reputation: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int checkAllReputation(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            PlayerContextTracker.EnhancedPlayerContext playerContext = PlayerContextTracker.getContext(player.getUUID());
            
            if (playerContext == null) {
                context.getSource().sendFailure(Component.literal("No player context found"));
                return 0;
            }
            
            player.sendSystemMessage(Component.translatable("eidolonunchained.task.reputation_header"));
            
            // Check datapack deities
            for (com.bluelotuscoding.eidolonunchained.deity.DatapackDeity deity : com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager.getAllDeities().values()) {
                double reputation = deity.getPlayerReputation(player);
                if (reputation != 0) {
                    player.sendSystemMessage(Component.literal("§e" + deity.getId().getPath() + ": §6" + (int)reputation + " points"));
                }
            }
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error checking reputation: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int markRitualComplete(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            String ritualIdString = StringArgumentType.getString(context, "ritualId");
            
            ResourceLocation ritualId = new ResourceLocation(ritualIdString);
            
            // Fire our custom ritual completion event
            com.bluelotuscoding.eidolonunchained.events.RitualEventHandler.fireRitualCompletion(targetPlayer, ritualId);
            
            context.getSource().sendSuccess(() -> Component.literal("Marked ritual '" + ritualIdString + "' as completed for " + targetPlayer.getName().getString()), true);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error marking ritual complete: " + e.getMessage()));
            return 0;
        }
    }
    
    // Helper methods
    private static List<String> getTaskRequirements(String taskId) {
        for (AIDeityConfig config : AIDeityManager.getInstance().getAllConfigs()) {
            for (TaskSystemConfig.TaskTemplate template : config.task_config.availableTasks) {
                if (template.taskId.equals(taskId)) {
                    return template.requirements;
                }
            }
        }
        return List.of();
    }
    
    private static boolean validateRequirement(ServerPlayer player, String requirement) {
        String[] parts = requirement.split(":");
        if (parts.length >= 2) {
            String type = parts[0];
            
            if (type.equals("item") && parts.length >= 4) {
                // Item count requirement: "item:minecraft:wheat:16"
                String itemName = parts[1] + ":" + parts[2];
                int requiredCount = Integer.parseInt(parts[3]);
                
                int playerCount = 0;
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    net.minecraft.world.item.ItemStack stack = player.getInventory().getItem(i);
                    if (net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).toString().equals(itemName)) {
                        playerCount += stack.getCount();
                    }
                }
                
                return playerCount >= requiredCount;
            } else if (type.equals("ritual") && parts.length >= 3) {
                // Ritual requirement: "ritual:eidolonunchained:nature_blessing"
                ResourceLocation ritualId = new ResourceLocation(parts[1] + ":" + parts[2]);
                PlayerContextTracker.EnhancedPlayerContext context = PlayerContextTracker.getContext(player.getUUID());
                
                if (context != null) {
                    return context.completedRituals.contains(ritualId.toString());
                }
            }
        }
        
        return false;
    }
    
    private static void executeTaskRewards(ServerPlayer player, String taskId) {
        for (AIDeityConfig config : AIDeityManager.getInstance().getAllConfigs()) {
            for (TaskSystemConfig.TaskTemplate template : config.task_config.availableTasks) {
                if (template.taskId.equals(taskId)) {
                    for (String command : template.rewardCommands) {
                        String processedCommand = command.replace("{player}", player.getName().getString());
                        try {
                            player.getServer().getCommands().performPrefixedCommand(
                                player.getServer().createCommandSourceStack(),
                                processedCommand
                            );
                        } catch (Exception e) {
                            System.err.println("Failed to execute reward command: " + processedCommand + " - " + e.getMessage());
                        }
                    }
                    break;
                }
            }
        }
    }
}
