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

import java.util.Collection;

/**
 * Commands for managing the AI deity task system
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TaskCommands {
    
    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        dispatcher.register(Commands.literal("eidolon")
            .then(Commands.literal("task")
                .then(Commands.literal("assign")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("task_id", StringArgumentType.string())
                            .then(Commands.argument("deity", StringArgumentType.string())
                                .executes(TaskCommands::assignTask)))))
                .then(Commands.literal("complete")
                    .then(Commands.argument("task_id", StringArgumentType.string())
                        .executes(TaskCommands::completeTask)))
                .then(Commands.literal("list")
                    .executes(TaskCommands::listTasks))
                .then(Commands.literal("favor")
                    .then(Commands.argument("deity", StringArgumentType.string())
                        .executes(TaskCommands::checkFavor))
                    .executes(TaskCommands::checkAllFavor))));
        
        // Alternative command for AI deity task assignment
        dispatcher.register(Commands.literal("assign_task")
            .requires(source -> source.hasPermission(0)) // Allow anyone to use this
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("task_id", StringArgumentType.string())
                    .executes(TaskCommands::assignTaskSimple))));
    }
    
    private static int assignTask(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            String taskId = StringArgumentType.getString(context, "task_id");
            String deityIdStr = StringArgumentType.getString(context, "deity");
            ResourceLocation deityId = new ResourceLocation(deityIdStr);
            
            // Get AI config to find task template
            AIDeityConfig config = AIDeityManager.getInstance().getAIConfig(deityId);
            if (config == null) {
                context.getSource().sendFailure(Component.literal("Unknown deity: " + deityIdStr));
                return 0;
            }
            
            // Find task template
            TaskSystemConfig.TaskTemplate taskTemplate = null;
            for (TaskSystemConfig.TaskTemplate template : config.taskConfig.availableTasks) {
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
            PlayerContextTracker.assignTask(targetPlayer, taskId, taskTemplate.description, deityId, taskTemplate.favorReward);
            
            targetPlayer.sendSystemMessage(Component.literal("§6[Divine Task] §e" + taskTemplate.description));
            targetPlayer.sendSystemMessage(Component.literal("§7Reward: §6" + taskTemplate.favorReward + " favor points"));
            
            context.getSource().sendSuccess(() -> Component.literal("Assigned task '" + taskId + "' to " + targetPlayer.getName().getString()), true);
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error assigning task: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int assignTaskSimple(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            String taskId = StringArgumentType.getString(context, "task_id");
            
            // Try to find the task in any deity config (for AI command usage)
            TaskSystemConfig.TaskTemplate taskTemplate = null;
            ResourceLocation foundDeityId = null;
            
            for (AIDeityConfig config : AIDeityManager.getInstance().getAllConfigs()) {
                for (TaskSystemConfig.TaskTemplate template : config.taskConfig.availableTasks) {
                    if (template.taskId.equals(taskId)) {
                        taskTemplate = template;
                        foundDeityId = config.deityId;
                        break;
                    }
                }
                if (taskTemplate != null) break;
            }
            
            if (taskTemplate == null) {
                context.getSource().sendFailure(Component.literal("Unknown task: " + taskId));
                return 0;
            }
            
            // Assign the task
            PlayerContextTracker.assignTask(targetPlayer, taskId, taskTemplate.description, foundDeityId, taskTemplate.favorReward);
            
            targetPlayer.sendSystemMessage(Component.literal("§6[Divine Task] §e" + taskTemplate.description));
            targetPlayer.sendSystemMessage(Component.literal("§7Reward: §6" + taskTemplate.favorReward + " favor points"));
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error assigning task: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int completeTask(CommandContext<CommandSourceStack> context) {
        try {
            if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                context.getSource().sendFailure(Component.literal("Only players can complete tasks"));
                return 0;
            }
            
            String taskId = StringArgumentType.getString(context, "task_id");
            
            PlayerContextTracker.EnhancedPlayerContext playerContext = PlayerContextTracker.getContext(player.getUUID());
            if (playerContext == null || !playerContext.activeTasks.containsKey(taskId)) {
                context.getSource().sendFailure(Component.literal("You don't have that task active"));
                return 0;
            }
            
            PlayerContextTracker.PlayerTask task = playerContext.activeTasks.get(taskId);
            
            // Check requirements if any
            boolean canComplete = true;
            for (String requirement : getTaskRequirements(taskId)) {
                if (!checkTaskRequirement(player, requirement)) {
                    canComplete = false;
                    context.getSource().sendFailure(Component.literal("Requirement not met: " + requirement));
                    break;
                }
            }
            
            if (canComplete) {
                if (PlayerContextTracker.completeTask(player, taskId)) {
                    player.sendSystemMessage(Component.literal("§a[Task Complete] §e" + task.description));
                    player.sendSystemMessage(Component.literal("§7Gained §6" + task.favorReward + " favor points§7!"));
                    
                    // Execute reward commands
                    executeTaskRewards(player, taskId);
                    
                    return 1;
                } else {
                    context.getSource().sendFailure(Component.literal("Failed to complete task"));
                }
            }
            
            return 0;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error completing task: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int listTasks(CommandContext<CommandSourceStack> context) {
        try {
            if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                context.getSource().sendFailure(Component.literal("Only players can list tasks"));
                return 0;
            }
            
            PlayerContextTracker.EnhancedPlayerContext playerContext = PlayerContextTracker.getOrCreateContext(player.getUUID(), player);
            
            if (playerContext.activeTasks.isEmpty()) {
                player.sendSystemMessage(Component.literal("§7You have no active tasks"));
                return 1;
            }
            
            player.sendSystemMessage(Component.literal("§6=== Active Divine Tasks ==="));
            for (PlayerContextTracker.PlayerTask task : playerContext.activeTasks.values()) {
                player.sendSystemMessage(Component.literal("§e" + task.taskId + ": §f" + task.description));
                player.sendSystemMessage(Component.literal("  §7Reward: §6" + task.favorReward + " favor points"));
            }
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error listing tasks: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int checkFavor(CommandContext<CommandSourceStack> context) {
        try {
            if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                context.getSource().sendFailure(Component.literal("Only players can check favor"));
                return 0;
            }
            
            String deityIdStr = StringArgumentType.getString(context, "deity");
            ResourceLocation deityId = new ResourceLocation(deityIdStr);
            
            int favorPoints = PlayerContextTracker.getFavorPoints(player, deityId);
            player.sendSystemMessage(Component.literal("§6Favor with " + deityIdStr + ": §e" + favorPoints + " points"));
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error checking favor: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int checkAllFavor(CommandContext<CommandSourceStack> context) {
        try {
            if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                context.getSource().sendFailure(Component.literal("Only players can check favor"));
                return 0;
            }
            
            PlayerContextTracker.EnhancedPlayerContext playerContext = PlayerContextTracker.getOrCreateContext(player.getUUID(), player);
            
            if (playerContext.favorPoints.isEmpty()) {
                player.sendSystemMessage(Component.literal("§7You have no favor with any deity"));
                return 1;
            }
            
            player.sendSystemMessage(Component.literal("§6=== Divine Favor ==="));
            for (var entry : playerContext.favorPoints.entrySet()) {
                player.sendSystemMessage(Component.literal("§e" + entry.getKey().getPath() + ": §6" + entry.getValue() + " points"));
            }
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error checking favor: " + e.getMessage()));
            return 0;
        }
    }
    
    private static java.util.List<String> getTaskRequirements(String taskId) {
        // Find task requirements from configs
        for (AIDeityConfig config : AIDeityManager.getInstance().getAllConfigs()) {
            for (TaskSystemConfig.TaskTemplate template : config.taskConfig.availableTasks) {
                if (template.taskId.equals(taskId)) {
                    return template.requirements;
                }
            }
        }
        return java.util.Collections.emptyList();
    }
    
    private static boolean checkTaskRequirement(ServerPlayer player, String requirement) {
        if (requirement.startsWith("item:")) {
            // Format: "item:namespace:item_id:count"
            String[] parts = requirement.split(":");
            if (parts.length >= 4) {
                try {
                    ResourceLocation itemId = new ResourceLocation(parts[1], parts[2]);
                    int requiredCount = Integer.parseInt(parts[3]);
                    
                    int playerCount = 0;
                    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                        var stack = player.getInventory().getItem(i);
                        if (!stack.isEmpty() && net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).equals(itemId)) {
                            playerCount += stack.getCount();
                        }
                    }
                    
                    return playerCount >= requiredCount;
                } catch (Exception e) {
                    return false;
                }
            }
        }
        return true; // Default to true for unknown requirements
    }
    
    private static void executeTaskRewards(ServerPlayer player, String taskId) {
        // Find and execute reward commands
        for (AIDeityConfig config : AIDeityManager.getInstance().getAllConfigs()) {
            for (TaskSystemConfig.TaskTemplate template : config.taskConfig.availableTasks) {
                if (template.taskId.equals(taskId)) {
                    for (String command : template.rewardCommands) {
                        String processedCommand = command.replace("{player}", player.getName().getString());
                        try {
                            player.getServer().getCommands().performPrefixedCommand(
                                player.getServer().createCommandSourceStack(), processedCommand);
                        } catch (Exception e) {
                            com.mojang.logging.LogUtils.getLogger().warn("Failed to execute reward command: " + processedCommand, e);
                        }
                    }
                    break;
                }
            }
        }
    }
}
