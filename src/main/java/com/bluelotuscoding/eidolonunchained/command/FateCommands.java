package com.bluelotuscoding.eidolonunchained.command;

import com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig;
import com.bluelotuscoding.eidolonunchained.ai.AIDeityManager;
import com.bluelotuscoding.eidolonunchained.ai.PlayerContextTracker;
import com.bluelotuscoding.eidolonunchained.ai.TaskSystemConfig;
import com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager;
import com.bluelotuscoding.eidolonunchained.deity.DatapackDeity;
import com.bluelotuscoding.eidolonunchained.events.RitualEventHandler;
import com.bluelotuscoding.eidolonunchained.util.JsonUtils;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/**
 * Fate (task) system commands under /eu fates.
 * Canonical naming — "tasks" alias has been removed.
 */
public class FateCommands {

    /** Build the "fates" subtree to be attached to the /eu root. */
    public static LiteralArgumentBuilder<CommandSourceStack> buildNode(
            com.mojang.brigadier.suggestion.SuggestionProvider<CommandSourceStack> playerSuggestions,
            com.mojang.brigadier.suggestion.SuggestionProvider<CommandSourceStack> deitySuggestions,
            com.mojang.brigadier.suggestion.SuggestionProvider<CommandSourceStack> taskIdSuggestions,
            com.mojang.brigadier.suggestion.SuggestionProvider<CommandSourceStack> ritualSuggestions) {

        return Commands.literal("fates")
            .then(Commands.literal("assign")
                .requires(cs -> cs.hasPermission(2))
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("deity", ResourceLocationArgument.id())
                        .suggests(deitySuggestions)
                        .then(Commands.argument("fateId", StringArgumentType.greedyString())
                            .suggests(taskIdSuggestions)
                            .executes(FateCommands::assignTask)))))
            .then(Commands.literal("complete")
                .requires(cs -> cs.hasPermission(2))
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("fateId", StringArgumentType.greedyString())
                        .suggests(taskIdSuggestions)
                        .executes(FateCommands::completeTask))))
            .then(Commands.literal("list")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(FateCommands::listTasks)))
            .then(Commands.literal("ritual")
                .requires(cs -> cs.hasPermission(2))
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("ritualId", ResourceLocationArgument.id())
                        .suggests(ritualSuggestions)
                        .executes(FateCommands::markRitualComplete))));
    }

    static int assignTask(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            String fateId = StringArgumentType.getString(context, "fateId");
            ResourceLocation deityId = ResourceLocationArgument.getId(context, "deity");

            AIDeityConfig config = AIDeityManager.getInstance().getAIConfig(deityId);
            if (config == null) {
                context.getSource().sendFailure(Component.literal("Unknown deity: " + deityId));
                return 0;
            }

            TaskSystemConfig.TaskTemplate template = findTemplate(config, fateId);
            if (template == null) {
                context.getSource().sendFailure(Component.literal("Unknown fate: " + fateId));
                return 0;
            }

            DatapackDeity deity = DatapackDeityManager.getDeity(deityId);
            double reputation = deity != null ? deity.getPlayerReputation(targetPlayer) : 0;
            if (reputation < template.reputationRequired) {
                context.getSource().sendFailure(Component.literal(
                    "Player needs at least " + template.reputationRequired + " reputation with " + deityId.getPath()));
                return 0;
            }

            if (!meetsAssignmentConditions(targetPlayer, template)) {
                context.getSource().sendFailure(Component.literal("Assignment conditions not met (progression/reputation)"));
                return 0;
            }

            PlayerContextTracker.assignTask(targetPlayer, fateId, template.description, deityId, template.reputationReward);
            targetPlayer.sendSystemMessage(Component.translatable("eidolonunchained.fate.assigned", template.description));
            targetPlayer.sendSystemMessage(Component.translatable("eidolonunchained.fate.reward", template.reputationReward));
            context.getSource().sendSuccess(() -> Component.literal(
                "Assigned fate '" + fateId + "' to " + targetPlayer.getName().getString()), true);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error assigning fate: " + e.getMessage()));
            return 0;
        }
    }

    static int completeTask(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            String fateId = StringArgumentType.getString(context, "fateId");

            PlayerContextTracker.EnhancedPlayerContext playerContext = PlayerContextTracker.getContext(player.getUUID());
            if (playerContext == null || !playerContext.activeTasks.containsKey(fateId)) {
                context.getSource().sendFailure(Component.literal("Player does not have this fate active"));
                return 0;
            }

            PlayerContextTracker.PlayerTask task = playerContext.activeTasks.get(fateId);
            for (String req : getTaskRequirements(fateId)) {
                if (!validateRequirement(player, req)) {
                    context.getSource().sendFailure(Component.literal("Requirement not met: " + req));
                    return 0;
                }
            }

            PlayerContextTracker.completeTask(player, fateId);
            player.sendSystemMessage(Component.translatable("eidolonunchained.fate.completed_reputation", task.reputationReward));
            executeTaskRewards(player, fateId);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error completing fate: " + e.getMessage()));
            return 0;
        }
    }

    static int listTasks(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            PlayerContextTracker.EnhancedPlayerContext playerContext = PlayerContextTracker.getContext(player.getUUID());

            if (playerContext == null || playerContext.activeTasks.isEmpty()) {
                context.getSource().sendSuccess(() -> Component.translatable("eidolonunchained.fate.no_active"), false);
                return 1;
            }

            context.getSource().sendSuccess(() -> Component.translatable("eidolonunchained.fate.header"), false);
            for (PlayerContextTracker.PlayerTask task : playerContext.activeTasks.values()) {
                context.getSource().sendSuccess(() -> Component.literal(
                    "§e" + task.description + "\n  §7Reward: §6" + task.reputationReward + " reputation"), false);
            }
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error listing fates: " + e.getMessage()));
            return 0;
        }
    }

    static int markRitualComplete(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            ResourceLocation ritualId = ResourceLocationArgument.getId(context, "ritualId");
            RitualEventHandler.fireRitualCompletion(player, ritualId);
            context.getSource().sendSuccess(() -> Component.literal(
                "Marked ritual '" + ritualId + "' complete for " + player.getName().getString()), true);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error marking ritual: " + e.getMessage()));
            return 0;
        }
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private static TaskSystemConfig.TaskTemplate findTemplate(AIDeityConfig config, String taskId) {
        for (TaskSystemConfig.TaskTemplate t : config.task_config.availableTasks) {
            if (taskId.equals(t.taskId)) return t;
        }
        return null;
    }

    private static List<String> getTaskRequirements(String taskId) {
        for (AIDeityConfig config : AIDeityManager.getInstance().getAllConfigs()) {
            TaskSystemConfig.TaskTemplate t = findTemplate(config, taskId);
            if (t != null) return t.requirements;
        }
        return List.of();
    }

    private static boolean validateRequirement(ServerPlayer player, String requirement) {
        try {
            JsonObject json = JsonUtils.GSON.fromJson(requirement, JsonObject.class);
            return validateJsonRequirement(player, json);
        } catch (Exception ignored) {}

        String[] parts = requirement.split(":");
        if (parts.length < 2) return false;
        String type = parts[0];

        switch (type) {
            case "item": case "collect_items":
                if (parts.length >= 4) return hasItem(player, parts[1] + ":" + parts[2], Integer.parseInt(parts[3]));
                break;
            case "kill_entities":
                if (parts.length >= 4) return getKillCount(player, parts[1] + ":" + parts[2]) >= Integer.parseInt(parts[3]);
                break;
            case "dimension":
                if (parts.length >= 3) return player.level().dimension().location().toString().equals(parts[1] + ":" + parts[2]);
                break;
            case "time":
                long dt = player.level().getDayTime() % 24000;
                if ("day".equals(parts[1])) return dt < 12000;
                if ("night".equals(parts[1])) return dt >= 12000;
                break;
            case "ritual":
                if (parts.length >= 3) {
                    PlayerContextTracker.EnhancedPlayerContext ctx = PlayerContextTracker.getContext(player.getUUID());
                    return ctx != null && ctx.completedRituals.contains(parts[1] + ":" + parts[2]);
                }
                break;
        }
        return false;
    }

    private static boolean validateJsonRequirement(ServerPlayer player, JsonObject req) {
        String type = req.get("type").getAsString();
        return switch (type) {
            case "kill_entities" -> getKillCount(player, req.get("entity").getAsString()) >= req.get("count").getAsInt();
            case "collect_items" -> hasItem(player, req.get("item").getAsString(), req.get("count").getAsInt());
            case "dimension" -> player.level().dimension().location().toString().equals(req.get("dimension").getAsString());
            case "time" -> {
                long d = player.level().getDayTime() % 24000;
                yield switch (req.get("time").getAsString()) {
                    case "day" -> d < 12000;
                    case "night" -> d >= 12000;
                    default -> false;
                };
            }
            default -> false;
        };
    }

    private static int getKillCount(ServerPlayer player, String entityType) {
        PlayerContextTracker.EnhancedPlayerContext ctx = PlayerContextTracker.getContext(player.getUUID());
        return (ctx != null && ctx.killsByEntity != null) ? ctx.killsByEntity.getOrDefault(entityType, 0) : 0;
    }

    private static boolean hasItem(ServerPlayer player, String itemId, int count) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var stack = player.getInventory().getItem(i);
            var key = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (key != null && key.toString().equals(itemId)) total += stack.getCount();
        }
        return total >= count;
    }

    private static boolean meetsAssignmentConditions(ServerPlayer player, TaskSystemConfig.TaskTemplate template) {
        if (template.progressionTier != null && !template.progressionTier.isEmpty()
                && !"none".equalsIgnoreCase(template.progressionTier)) {
            PlayerContextTracker.EnhancedPlayerContext ctx = PlayerContextTracker.getContext(player.getUUID());
            if (ctx == null || !ctx.unlockedProgressions.contains(template.progressionTier)) return false;
        }
        if (template.aiAssignmentContext != null && !template.aiAssignmentContext.isEmpty()) {
            try {
                JsonObject rules = JsonUtils.GSON.fromJson(template.aiAssignmentContext, JsonObject.class);
                if (rules.has("required_items")) {
                    JsonArray items = rules.getAsJsonArray("required_items");
                    for (var el : items) {
                        JsonObject ir = el.getAsJsonObject();
                        if (!hasItem(player, ir.get("item").getAsString(), ir.has("count") ? ir.get("count").getAsInt() : 1))
                            return false;
                    }
                }
                if (rules.has("required_dimension")) {
                    if (!player.level().dimension().location().toString().equals(rules.get("required_dimension").getAsString()))
                        return false;
                }
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    private static void executeTaskRewards(ServerPlayer player, String taskId) {
        for (AIDeityConfig config : AIDeityManager.getInstance().getAllConfigs()) {
            TaskSystemConfig.TaskTemplate t = findTemplate(config, taskId);
            if (t != null) {
                for (String cmd : t.rewardCommands) {
                    String processed = cmd.replace("{player}", player.getName().getString());
                    try {
                        var server = player.getServer();
                        if (server != null)
                            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), processed);
                    } catch (Exception e) {
                        System.err.println("[EidolonUnchained] Reward command failed: " + processed + " - " + e.getMessage());
                    }
                }
                return;
            }
        }
    }
}
