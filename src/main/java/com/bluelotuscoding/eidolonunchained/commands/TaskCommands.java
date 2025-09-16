package com.bluelotuscoding.eidolonunchained.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import com.bluelotuscoding.eidolonunchained.ai.PlayerContextTracker;
import com.bluelotuscoding.eidolonunchained.ai.AIDeityManager;
import com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig;
import com.bluelotuscoding.eidolonunchained.ai.TaskSystemConfig;

import java.util.List;

/**
 * Command handlers for the deity task system
 */
public class TaskCommands {
    private static final com.google.gson.Gson GSON = com.bluelotuscoding.eidolonunchained.util.JsonUtils.GSON;

    
    public static int assignTask(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            String taskId = getTaskOrFateId(context);
            ResourceLocation deityId = ResourceLocationArgument.getId(context, "deity");
            
            // Get AI config to find task template
            AIDeityConfig config = AIDeityManager.getInstance().getAIConfig(deityId);
            if (config == null) {
                context.getSource().sendFailure(Component.literal("Unknown deity: " + deityId));
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
                context.getSource().sendFailure(Component.literal("Unknown fate/task: " + taskId));
                return 0;
            }
            
            // Check if player meets reputation requirement
            double playerReputation = com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager.getDeity(deityId).getPlayerReputation(targetPlayer);
            if (playerReputation < taskTemplate.reputationRequired) {
                context.getSource().sendFailure(Component.literal("Player needs at least " + taskTemplate.reputationRequired + " reputation"));
                return 0;
            }
            
            // Enforce basic assignment gating from ai_assignment_context (progression)
            if (!meetsAssignmentConditions(targetPlayer, taskTemplate)) {
                context.getSource().sendFailure(Component.literal("Assignment conditions not met (progression/reputation)"));
                return 0;
            }

            // Assign the task
            PlayerContextTracker.assignTask(targetPlayer, taskId, taskTemplate.description, deityId, taskTemplate.reputationReward);
            
            targetPlayer.sendSystemMessage(Component.translatable("eidolonunchained.fate.assigned", taskTemplate.description));
            targetPlayer.sendSystemMessage(Component.translatable("eidolonunchained.fate.reward", taskTemplate.reputationReward));
            
            context.getSource().sendSuccess(() -> Component.literal("Assigned fate/task '" + taskId + "' to " + targetPlayer.getName().getString()), true);
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error assigning task: " + e.getMessage()));
            return 0;
        }
    }
    
    public static int assignAnyTask(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            String taskId = getTaskOrFateId(context);
            
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
                context.getSource().sendFailure(Component.literal("Unknown fate/task: " + taskId));
                return 0;
            }
            
            // Enforce basic assignment gating from ai_assignment_context (progression)
            if (!meetsAssignmentConditions(targetPlayer, taskTemplate)) {
                context.getSource().sendFailure(Component.literal("Assignment conditions not met (progression/reputation)"));
                return 0;
            }

            // Assign the task
            PlayerContextTracker.assignTask(targetPlayer, taskId, taskTemplate.description, foundDeityId, taskTemplate.reputationReward);
            
            targetPlayer.sendSystemMessage(Component.translatable("eidolonunchained.fate.assigned", taskTemplate.description));
            targetPlayer.sendSystemMessage(Component.translatable("eidolonunchained.fate.reward", taskTemplate.reputationReward));
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error assigning task: " + e.getMessage()));
            return 0;
        }
    }
    
    public static int completeTask(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            String taskId = getTaskOrFateId(context);
            
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
                player.sendSystemMessage(Component.translatable("eidolonunchained.fate.completed_reputation", task.reputationReward));
                
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
    
    public static int listTasks(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            PlayerContextTracker.EnhancedPlayerContext playerContext = PlayerContextTracker.getContext(player.getUUID());
            
            if (playerContext == null || playerContext.activeTasks.isEmpty()) {
                player.sendSystemMessage(Component.translatable("eidolonunchained.fate.no_active"));
                return 1;
            }
            
            player.sendSystemMessage(Component.translatable("eidolonunchained.fate.header"));
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
    
    public static int checkSpecificReputation(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            ResourceLocation deityId = ResourceLocationArgument.getId(context, "deity");
            
            com.bluelotuscoding.eidolonunchained.deity.DatapackDeity deity = 
                com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager.getDeity(deityId);
            
            if (deity != null) {
                double reputation = deity.getPlayerReputation(player);
                player.sendSystemMessage(Component.literal("§6Reputation with " + deityId + ": §e" + (int)reputation + " points"));
            } else {
                // Try Eidolon reputation system
                try {
                    var repCap = player.level().getCapability(elucent.eidolon.capability.IReputation.INSTANCE);
                    if (repCap.isPresent()) {
                        double reputation = repCap.map(cap -> cap.getReputation(player, deityId)).orElse(0.0);
                        player.sendSystemMessage(Component.literal("§6Reputation with " + deityId + ": §e" + (int)reputation + " points"));
                    } else {
                        player.sendSystemMessage(Component.literal("§cCould not access reputation data"));
                    }
                } catch (Exception e) {
                    player.sendSystemMessage(Component.literal("§cUnknown deity: " + deityId));
                }
            }
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error checking reputation: " + e.getMessage()));
            return 0;
        }
    }
    
    public static int checkAllReputation(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            PlayerContextTracker.EnhancedPlayerContext playerContext = PlayerContextTracker.getContext(player.getUUID());
            
            if (playerContext == null) {
                context.getSource().sendFailure(Component.literal("No player context found"));
                return 0;
            }
            
            player.sendSystemMessage(Component.translatable("eidolonunchained.fate.reputation_header"));
            
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
    
    public static int markRitualComplete(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            ResourceLocation ritualId = ResourceLocationArgument.getId(context, "ritualId");
            
            // Fire our custom ritual completion event
            com.bluelotuscoding.eidolonunchained.events.RitualEventHandler.fireRitualCompletion(targetPlayer, ritualId);
            
            context.getSource().sendSuccess(() -> Component.literal("Marked ritual '" + ritualId + "' as completed for " + targetPlayer.getName().getString()), true);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error marking ritual complete: " + e.getMessage()));
            return 0;
        }
    }

    // Accept both "taskId" and "fateId" argument names
    private static String getTaskOrFateId(CommandContext<CommandSourceStack> context) {
        try {
            return StringArgumentType.getString(context, "taskId");
        } catch (IllegalArgumentException ignored) {
            try {
                return StringArgumentType.getString(context, "fateId");
            } catch (IllegalArgumentException e) {
                throw e;
            }
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
        // Try JSON parsing first for complex requirements
        try {
            com.google.gson.JsonObject reqJson = GSON.fromJson(requirement, com.google.gson.JsonObject.class);
            return validateJsonRequirement(player, reqJson);
        } catch (Exception e) {
            // Fall back to string parsing for simple requirements
        }
        
        String[] parts = requirement.split(":");
        if (parts.length >= 2) {
            String type = parts[0];
            
            if (type.equals("item") || type.equals("collect_items")) {
                // Item count requirement: "item:minecraft:wheat:16"
                if (parts.length >= 4) {
                    String itemName = parts[1] + ":" + parts[2];
                    int requiredCount = Integer.parseInt(parts[3]);
                    return hasItem(player, itemName, requiredCount);
                }
            } else if (type.equals("kill_entities") && parts.length >= 4) {
                // Kill entities requirement: "kill_entities:minecraft:zombie:10"
                String entityType = parts[1] + ":" + parts[2];
                int requiredKills = Integer.parseInt(parts[3]);
                return getKillCount(player, entityType) >= requiredKills;
            } else if (type.equals("mine_blocks") && parts.length >= 4) {
                // Mine blocks requirement: "mine_blocks:minecraft:stone:64"
                String blockType = parts[1] + ":" + parts[2];
                int requiredMines = Integer.parseInt(parts[3]);
                return getMineCount(player, blockType) >= requiredMines;
            } else if (type.equals("use_items") && parts.length >= 4) {
                // Use items requirement: "use_items:minecraft:ender_pearl:5"
                String itemType = parts[1] + ":" + parts[2];
                int requiredUses = Integer.parseInt(parts[3]);
                return getUseCount(player, itemType) >= requiredUses;
            } else if (type.equals("dimension") && parts.length >= 3) {
                // Dimension requirement: "dimension:minecraft:nether"
                String requiredDim = parts[1] + ":" + parts[2];
                String currentDim = player.level().dimension().location().toString();
                return currentDim.equals(requiredDim);
            } else if (type.equals("time") && parts.length >= 2) {
                // Time requirement: "time:day" or "time:night"
                String timeType = parts[1];
                long dayTime = player.level().getDayTime() % 24000;
                if (timeType.equals("day")) {
                    return dayTime >= 0 && dayTime < 12000;
                } else if (timeType.equals("night")) {
                    return dayTime >= 12000 && dayTime < 24000;
                }
            } else if (type.equals("location")) {
                // Supported forms:
                // - "location:x:y:z[:radius]" (numeric position with optional radius, default 5)
                // - "location:underground" (y < 50 OR not sky-visible)
                // - "location:y>NUMBER" or "location:y<NUMBER" (Y range)
                // - "location:<namespace>:<biome>" (biome id)
                if (parts.length >= 4) {
                    // Numeric position
                    try {
                        double reqX = Double.parseDouble(parts[1]);
                        double reqY = Double.parseDouble(parts[2]);  
                        double reqZ = Double.parseDouble(parts[3]);
                        double radius = parts.length >= 5 ? Double.parseDouble(parts[4]) : 5.0;

                        double distance = player.position().distanceTo(new net.minecraft.world.phys.Vec3(reqX, reqY, reqZ));
                        return distance <= radius;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                } else if (parts.length == 3) {
                    // Biome id
                    String biomeId = parts[1] + ":" + parts[2];
                    String currentBiome = player.level().getBiome(player.blockPosition()).unwrapKey()
                        .map(key -> key.location().toString()).orElse("");
                    return currentBiome.equals(biomeId);
                } else if (parts.length == 2) {
                    String expr = parts[1].toLowerCase();
                    // Underground shorthand
                    if ("underground".equals(expr)) {
                        boolean yThreshold = player.blockPosition().getY() < 50;
                        boolean skyVisible = player.level().canSeeSky(player.blockPosition());
                        return yThreshold || !skyVisible;
                    }
                    // Y-range expressions
                    if (expr.startsWith("y>")) {
                        try {
                            int minY = Integer.parseInt(expr.substring(2));
                            return player.blockPosition().getY() > minY;
                        } catch (NumberFormatException e) {
                            return false;
                        }
                    } else if (expr.startsWith("y<")) {
                        try {
                            int maxY = Integer.parseInt(expr.substring(2));
                            return player.blockPosition().getY() < maxY;
                        } catch (NumberFormatException e) {
                            return false;
                        }
                    }
                }
            } else if (type.equals("explore_biomes")) {
                // Explore biomes requirement: "explore_biomes:minecraft:desert:minecraft:jungle"
                for (int i = 1; i < parts.length; i += 2) {
                    if (i + 1 < parts.length) {
                        String biome = parts[i] + ":" + parts[i + 1];
                        if (!hasExploredBiome(player, biome)) {
                            return false;
                        }
                    }
                }
                return true;
            } else if (type.equals("visit_structures")) {
                // Visit structures requirement: "visit_structures:minecraft:village:minecraft:stronghold"
                for (int i = 1; i < parts.length; i += 2) {
                    if (i + 1 < parts.length) {
                        String structure = parts[i] + ":" + parts[i + 1];
                        if (!hasVisitedStructure(player, structure)) {
                            return false;
                        }
                    }
                }
                return true;
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
    
    private static boolean validateJsonRequirement(ServerPlayer player, com.google.gson.JsonObject requirement) {
        String type = requirement.get("type").getAsString();
        
        switch (type) {
            case "kill_entities":
                String entity = requirement.get("entity").getAsString();
                int killCount = requirement.get("count").getAsInt();
                return getKillCount(player, entity) >= killCount;
                
            case "mine_blocks":
                String block = requirement.get("block").getAsString();
                int mineCount = requirement.get("count").getAsInt();
                return getMineCount(player, block) >= mineCount;
                
            case "collect_items":
                String item = requirement.get("item").getAsString();
                int itemCount = requirement.get("count").getAsInt();
                return hasItem(player, item, itemCount);
                
            case "use_items":
                String useItem = requirement.get("item").getAsString();
                int useCount = requirement.get("count").getAsInt();
                return getUseCount(player, useItem) >= useCount;
                
            case "dimension":
                String dimension = requirement.get("dimension").getAsString();
                return player.level().dimension().location().toString().equals(dimension);
                
            case "time":
                String timeType = requirement.get("time").getAsString();
                long dayTime = player.level().getDayTime() % 24000;
                switch (timeType) {
                    case "day": return dayTime >= 0 && dayTime < 12000;
                    case "night": return dayTime >= 12000 && dayTime < 24000;
                    case "dawn": return dayTime >= 23000 || dayTime < 1000;
                    case "dusk": return dayTime >= 11000 && dayTime < 13000;
                }
                break;
                
            case "location":
                // JSON forms supported:
                // { "type":"location", "pos":[x,y,z], "radius":5 }
                // { "type":"location", "value":"underground" }
                // { "type":"location", "y_greater_than":60 } or { "y_less_than":20 }
                // { "type":"location", "biome":"minecraft:deep_ocean" }
                if (requirement.has("pos")) {
                    com.google.gson.JsonArray pos = requirement.getAsJsonArray("pos");
                    double x = pos.get(0).getAsDouble();
                    double y = pos.get(1).getAsDouble();
                    double z = pos.get(2).getAsDouble();
                    double radius = requirement.has("radius") ? requirement.get("radius").getAsDouble() : 5.0;

                    double distance = player.position().distanceTo(new net.minecraft.world.phys.Vec3(x, y, z));
                    return distance <= radius;
                }
                if (requirement.has("value")) {
                    String value = requirement.get("value").getAsString().toLowerCase();
                    if ("underground".equals(value)) {
                        boolean yThreshold = player.blockPosition().getY() < 50;
                        boolean skyVisible = player.level().canSeeSky(player.blockPosition());
                        return yThreshold || !skyVisible;
                    }
                }
                if (requirement.has("y_greater_than")) {
                    int minY = requirement.get("y_greater_than").getAsInt();
                    return player.blockPosition().getY() > minY;
                }
                if (requirement.has("y_less_than")) {
                    int maxY = requirement.get("y_less_than").getAsInt();
                    return player.blockPosition().getY() < maxY;
                }
                if (requirement.has("biome")) {
                    String biomeId = requirement.get("biome").getAsString();
                    String currentBiome = player.level().getBiome(player.blockPosition()).unwrapKey()
                        .map(key -> key.location().toString()).orElse("");
                    return currentBiome.equals(biomeId);
                }
                break;
                
            case "explore_biomes":
                if (requirement.has("biomes")) {
                    com.google.gson.JsonArray biomes = requirement.getAsJsonArray("biomes");
                    for (com.google.gson.JsonElement biomeEl : biomes) {
                        String biome = biomeEl.getAsString();
                        if (!hasExploredBiome(player, biome)) {
                            return false;
                        }
                    }
                    return true;
                }
                break;
                
            case "visit_structures":
                if (requirement.has("structures")) {
                    com.google.gson.JsonArray structures = requirement.getAsJsonArray("structures");
                    for (com.google.gson.JsonElement structureEl : structures) {
                        String structure = structureEl.getAsString();
                        if (!hasVisitedStructure(player, structure)) {
                            return false;
                        }
                    }
                    return true;
                }
                break;
        }
        
        return false;
    }
    
    // Helper methods for tracking statistics
    private static int getKillCount(ServerPlayer player, String entityType) {
        PlayerContextTracker.EnhancedPlayerContext context = PlayerContextTracker.getContext(player.getUUID());
        if (context != null && context.killsByEntity != null) {
            return context.killsByEntity.getOrDefault(entityType, 0);
        }
        return 0;
    }
    
    private static int getMineCount(ServerPlayer player, String blockType) {
        PlayerContextTracker.EnhancedPlayerContext context = PlayerContextTracker.getContext(player.getUUID());
        if (context != null && context.minedBlocks != null) {
            return context.minedBlocks.getOrDefault(blockType, 0);
        }
        return 0;
    }
    
    private static int getUseCount(ServerPlayer player, String itemType) {
        PlayerContextTracker.EnhancedPlayerContext context = PlayerContextTracker.getContext(player.getUUID());
        if (context != null && context.itemsUsed != null) {
            return context.itemsUsed.getOrDefault(itemType, 0);
        }
        return 0;
    }
    
    private static boolean hasExploredBiome(ServerPlayer player, String biome) {
        PlayerContextTracker.EnhancedPlayerContext context = PlayerContextTracker.getContext(player.getUUID());
        if (context != null && context.visitedBiomes != null) {
            return context.visitedBiomes.contains(biome);
        }
        return false;
    }
    
    private static boolean hasVisitedStructure(ServerPlayer player, String structure) {
        PlayerContextTracker.EnhancedPlayerContext context = PlayerContextTracker.getContext(player.getUUID());
        if (context != null && context.visitedStructures != null) {
            return context.visitedStructures.contains(structure);
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
                            var server = player.getServer();
                            if (server != null) {
                                server.getCommands().performPrefixedCommand(
                                    server.createCommandSourceStack(),
                                    processedCommand
                                );
                            }
                        } catch (Exception e) {
                            System.err.println("Failed to execute reward command: " + processedCommand + " - " + e.getMessage());
                        }
                    }
                    break;
                }
            }
        }
    }
    
    private static boolean meetsAssignmentConditions(ServerPlayer player, TaskSystemConfig.TaskTemplate taskTemplate) {
        // Check progression tier requirements (assignment gate)
        // Special-case: progression_tier == "none" means no attunement/progression required
        if (taskTemplate.progressionTier != null && !taskTemplate.progressionTier.isEmpty()) {
            if (!"none".equalsIgnoreCase(taskTemplate.progressionTier)) {
                PlayerContextTracker.EnhancedPlayerContext context = PlayerContextTracker.getContext(player.getUUID());
                if (context == null || !context.unlockedProgressions.contains(taskTemplate.progressionTier)) {
                    return false;
                }
            }
        }
        
        // Check AI assignment context if present
        if (taskTemplate.aiAssignmentContext != null && !taskTemplate.aiAssignmentContext.isEmpty()) {
            try {
                com.google.gson.JsonObject assignmentRules = GSON.fromJson(taskTemplate.aiAssignmentContext, com.google.gson.JsonObject.class);
                
                // Check minimum reputation
                if (assignmentRules.has("min_reputation")) {
                    int minRep = assignmentRules.get("min_reputation").getAsInt();
                    if (getPlayerReputationForTask(player, taskTemplate) < minRep) {
                        return false;
                    }
                }
                
                // Check required items
                if (assignmentRules.has("required_items")) {
                    com.google.gson.JsonArray items = assignmentRules.getAsJsonArray("required_items");
                    for (com.google.gson.JsonElement item : items) {
                        com.google.gson.JsonObject itemReq = item.getAsJsonObject();
                        String itemId = itemReq.get("item").getAsString();
                        int count = itemReq.has("count") ? itemReq.get("count").getAsInt() : 1;
                        
                        if (!hasItem(player, itemId, count)) {
                            return false;
                        }
                    }
                }
                
                // Check dimension requirements
                if (assignmentRules.has("required_dimension")) {
                    String reqDimension = assignmentRules.get("required_dimension").getAsString();
                    String currentDim = player.level().dimension().location().toString();
                    if (!currentDim.equals(reqDimension)) {
                        return false;
                    }
                }
                
            } catch (Exception e) {
                System.err.println("Failed to parse aiAssignmentContext: " + e.getMessage());
                return false;
            }
        }
        
        return true;
    }
    
    private static double getPlayerReputationForTask(ServerPlayer player, TaskSystemConfig.TaskTemplate taskTemplate) {
        // Try to find the deity associated with this task
        for (AIDeityConfig config : AIDeityManager.getInstance().getAllConfigs()) {
            if (config.task_config.availableTasks.contains(taskTemplate)) {
                com.bluelotuscoding.eidolonunchained.deity.DatapackDeity deity = 
                    com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager.getDeity(config.deity_id);
                if (deity != null) {
                    return deity.getPlayerReputation(player);
                }
            }
        }
        return 0.0;
    }
    
    private static boolean hasItem(ServerPlayer player, String itemId, int count) {
        int totalCount = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            net.minecraft.world.item.ItemStack stack = player.getInventory().getItem(i);
            if (net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).toString().equals(itemId)) {
                totalCount += stack.getCount();
            }
        }
        return totalCount >= count;
    }
}
