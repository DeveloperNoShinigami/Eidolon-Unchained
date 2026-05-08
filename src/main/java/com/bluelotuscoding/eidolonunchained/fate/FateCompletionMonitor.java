package com.bluelotuscoding.eidolonunchained.fate;

import com.bluelotuscoding.eidolonunchained.ai.PlayerContextTracker;
import com.bluelotuscoding.eidolonunchained.data.FateDataLoader;
import com.bluelotuscoding.eidolonunchained.chat.DeityChat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

/**
 * Monitors player actions and automatically detects when fate requirements are completed
 */
@Mod.EventBusSubscriber(modid = "eidolonunchained", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FateCompletionMonitor {

    private static final org.apache.logging.log4j.Logger LOGGER =
        org.apache.logging.log4j.LogManager.getLogger(FateCompletionMonitor.class);

    // Track players who have been checked recently to avoid spam
    private static final Map<String, Long> lastCheckTime = new HashMap<>();
    private static final long CHECK_COOLDOWN_MS = 2000; // Check every 2 seconds max

    /**
     * Check all active fates for completion when player inventory changes
     */
    @SubscribeEvent
    public static void onPlayerInventoryChange(PlayerEvent.ItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Throttle checks to avoid performance issues
        String playerId = player.getUUID().toString();
        long now = System.currentTimeMillis();
        Long lastCheck = lastCheckTime.get(playerId);
        if (lastCheck != null && now - lastCheck < CHECK_COOLDOWN_MS) {
            return;
        }
        lastCheckTime.put(playerId, now);

        checkPlayerFateCompletion(player);
    }

    /**
     * Also check on world tick for time-based requirements
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // Check every 100 ticks (5 seconds) for time-based completions
        if (event.getServer().getTickCount() % 100 != 0) return;

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            String playerId = player.getUUID().toString();
            long now = System.currentTimeMillis();
            Long lastCheck = lastCheckTime.get(playerId);
            if (lastCheck != null && now - lastCheck < CHECK_COOLDOWN_MS) {
                continue;
            }
            lastCheckTime.put(playerId, now);

            checkPlayerFateCompletion(player);
        }
    }

    /**
     * Check if any of the player's active fates are now completed
     */
    public static void checkPlayerFateCompletion(ServerPlayer player) {
        try {
            PlayerContextTracker.EnhancedPlayerContext context =
                PlayerContextTracker.getOrCreateContext(player.getUUID(), player);

            if (context.activeTasks.isEmpty()) return;

            // Check each active task for completion
            Set<String> completedTasks = new HashSet<>();
            for (Map.Entry<String, PlayerContextTracker.PlayerTask> entry : context.activeTasks.entrySet()) {
                String taskId = entry.getKey();
                PlayerContextTracker.PlayerTask task = entry.getValue();

                if (isFateCompleted(player, taskId, task)) {
                    completedTasks.add(taskId);
                }
            }

            // Process completions
            for (String taskId : completedTasks) {
                triggerFateCompletion(player, taskId);
            }

        } catch (Exception e) {
            LOGGER.error("Error checking fate completion for player {}: {}", player.getName().getString(), e.getMessage());
        }
    }

    /**
     * Check if a specific fate is completed based on its requirements
     */
    private static boolean isFateCompleted(ServerPlayer player, String taskId, PlayerContextTracker.PlayerTask task) {
        try {
            // Get fate data from FateDataLoader
            JsonObject fateData = FateDataLoader.getFateData(taskId);
            if (fateData == null || !fateData.has("requirements")) {
                return false;
            }

            JsonArray requirements = fateData.getAsJsonArray("requirements");

            // Check all requirements
            for (JsonElement reqElement : requirements) {
                if (!isRequirementMet(player, reqElement)) {
                    return false; // If any requirement fails, fate is not complete
                }
            }

            return true; // All requirements met

        } catch (Exception e) {
            LOGGER.error("Error checking fate completion for task {}: {}", taskId, e.getMessage());
            return false;
        }
    }

    /**
     * Check if a single requirement is met
     */
    private static boolean isRequirementMet(ServerPlayer player, JsonElement requirement) {
        try {
            if (requirement.isJsonPrimitive()) {
                String reqString = requirement.getAsString();
                return checkStringRequirement(player, reqString);
            } else if (requirement.isJsonObject()) {
                JsonObject reqObject = requirement.getAsJsonObject();
                return checkObjectRequirement(player, reqObject);
            }
            return false;
        } catch (Exception e) {
            LOGGER.error("Error checking requirement {}: {}", requirement, e.getMessage());
            return false;
        }
    }

    /**
     * Check string-based requirements like "item:minecraft:coal:16"
     */
    private static boolean checkStringRequirement(ServerPlayer player, String requirement) {
        if (requirement.startsWith("item:")) {
            // Format: "item:namespace:item_name:count"
            String[] parts = requirement.split(":");
            if (parts.length >= 4) {
                String namespace = parts[1];
                String itemName = parts[2];
                int requiredCount = Integer.parseInt(parts[3]);

                ResourceLocation itemId = new ResourceLocation(namespace, itemName);
                return playerHasItem(player, itemId, requiredCount);
            }
        }
        // Add more requirement types as needed
        return false;
    }

    /**
     * Check object-based requirements like {"type": "time", "value": "night"}
     */
    private static boolean checkObjectRequirement(ServerPlayer player, JsonObject requirement) {
        if (!requirement.has("type")) return false;

        String type = requirement.get("type").getAsString();

        switch (type) {
            case "time":
                if (requirement.has("value")) {
                    String timeValue = requirement.get("value").getAsString();
                    return checkTimeRequirement(player, timeValue);
                }
                break;
            case "location":
                if (requirement.has("dimension") || requirement.has("biome")) {
                    return checkLocationRequirement(player, requirement);
                }
                break;
            // Add more requirement types as needed
        }

        return false;
    }

    /**
     * Check if player has the required item count
     */
    private static boolean playerHasItem(ServerPlayer player, ResourceLocation itemId, int requiredCount) {
        int totalCount = 0;

        // Check inventory
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).equals(itemId)) {
                totalCount += stack.getCount();
                if (totalCount >= requiredCount) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Check time-based requirements
     */
    private static boolean checkTimeRequirement(ServerPlayer player, String timeValue) {
        long dayTime = player.level().getDayTime() % 24000;

        switch (timeValue.toLowerCase()) {
            case "night":
                return dayTime >= 13000 && dayTime <= 23000;
            case "day":
                return dayTime >= 1000 && dayTime <= 13000;
            case "dawn":
                return dayTime >= 23000 || dayTime <= 1000;
            case "dusk":
                return dayTime >= 12000 && dayTime <= 14000;
            default:
                return false;
        }
    }

    /**
     * Check location-based requirements
     */
    private static boolean checkLocationRequirement(ServerPlayer player, JsonObject requirement) {
        if (requirement.has("dimension")) {
            String requiredDimension = requirement.get("dimension").getAsString();
            String currentDimension = player.level().dimension().location().toString();
            if (!currentDimension.equals(requiredDimension)) {
                return false;
            }
        }

        if (requirement.has("underground")) {
            boolean requireUnderground = requirement.get("underground").getAsBoolean();
            boolean isUnderground = player.getY() < 64; // Simple underground check
            if (requireUnderground && !isUnderground) {
                return false;
            }
        }

        // Add more location checks as needed
        return true;
    }

    /**
     * Trigger the completion of a fate and notify the AI deity
     */
    private static void triggerFateCompletion(ServerPlayer player, String taskId) {
        try {
            LOGGER.info("Fate {} completed for player {}", taskId, player.getName().getString());

            // Get fate data for completion phrases and rewards
            JsonObject fateData = FateDataLoader.getFateData(taskId);

            // Mark task as completed (this also awards reputation)
            boolean completed = PlayerContextTracker.completeTask(player, taskId);

            if (completed) {
                // Notify player of completion
                player.sendSystemMessage(Component.literal("§6[Divine Fate] §aCompleted: " +
                    (fateData != null && fateData.has("description") ?
                        fateData.get("description").getAsString() : taskId)));

                // Process additional rewards if specified in fate data
                if (fateData != null && fateData.has("rewards")) {
                    processCompletionRewards(player, fateData.getAsJsonObject("rewards"));
                }

                // Trigger AI deity conversation for completion confirmation
                triggerAICompletionResponse(player, taskId, fateData);
            }

        } catch (Exception e) {
            LOGGER.error("Error triggering fate completion for {}: {}", taskId, e.getMessage());
        }
    }

    /**
     * Process additional rewards like items or commands
     */
    private static void processCompletionRewards(ServerPlayer player, JsonObject rewards) {
        try {
            if (rewards.has("commands")) {
                JsonArray commands = rewards.getAsJsonArray("commands");
                for (JsonElement cmdElement : commands) {
                    String command = cmdElement.getAsString();
                    // Replace {player} placeholder
                    command = command.replace("{player}", player.getName().getString());

                    // Execute command as server
                    player.getServer().getCommands().performPrefixedCommand(
                        player.getServer().createCommandSourceStack(), command);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error processing completion rewards: {}", e.getMessage());
        }
    }

    /**
     * Trigger AI deity to give completion confirmation and celebrate
     */
    private static void triggerAICompletionResponse(ServerPlayer player, String taskId, JsonObject fateData) {
        try {
            // Get the deity associated with this fate
            ResourceLocation deityId = null;
            if (fateData != null && fateData.has("linked_deity")) {
                deityId = new ResourceLocation(fateData.get("linked_deity").getAsString());
            }

            if (deityId != null) {
                // Create final variables for lambda usage
                final ResourceLocation finalDeityId = deityId;
                final String completionMessage = "FATE_COMPLETED:" + taskId;
                final ServerPlayer finalPlayer = player;

                // Trigger AI conversation with special completion context
                java.util.concurrent.CompletableFuture.runAsync(() -> {
                    try {
                        // Small delay to ensure completion processing is done
                        Thread.sleep(1000);

                        // Start conversation, then send completion message
                        DeityChat.startConversation(finalPlayer, finalDeityId);

                        // Send the completion message through chat to trigger AI response
                        finalPlayer.getServer().execute(() -> {
                            try {
                                // Create callback that executes AFTER AI action bar message completes
                                Runnable completionCallback = () -> {
                                    try {
                                        LOGGER.info("🎁 [POST-MESSAGE] Fate completion callback executing for fate: {}", taskId);

                                        // Check if auto-close is configured
                                        boolean shouldAutoClose = true; // Default to true
                                        int delaySeconds = 3; // Default 3 seconds

                                        if (fateData != null && fateData.has("ai_assignment_context")) {
                                            JsonObject aiContext = fateData.getAsJsonObject("ai_assignment_context");
                                            if (aiContext.has("auto_close_conversation")) {
                                                shouldAutoClose = aiContext.get("auto_close_conversation").getAsBoolean();
                                            }
                                            if (aiContext.has("auto_close_delay_seconds")) {
                                                delaySeconds = aiContext.get("auto_close_delay_seconds").getAsInt();
                                            }
                                        }

                                        if (shouldAutoClose) {
                                            LOGGER.info("🔚 Auto-closing fate completion conversation for player {} in {} seconds",
                                                finalPlayer.getName().getString(), delaySeconds);

                                            // Schedule auto-close with delay to let player read the response
                                            final int finalDelaySeconds = delaySeconds;
                                            java.util.concurrent.CompletableFuture.runAsync(() -> {
                                                try {
                                                    Thread.sleep(finalDelaySeconds * 1000L);
                                                    finalPlayer.getServer().execute(() -> {
                                                        try {
                                                            // Check if player is still in conversation
                                                            boolean inConversation = DeityChat.isInConversation(finalPlayer);

                                                            if (inConversation) {
                                                                finalPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal("§6⟦ Divine Conversation Concluded ⟧"));

                                                                // End conversation
                                                                DeityChat.endConversation(finalPlayer);

                                                                LOGGER.info("🔚 Auto-closed fate completion conversation for player {}",
                                                                    finalPlayer.getName().getString());
                                                            }
                                                        } catch (Exception e) {
                                                            LOGGER.error("Error during auto-close execution: {}", e.getMessage());
                                                        }
                                                    });
                                                } catch (InterruptedException e) {
                                                    Thread.currentThread().interrupt();
                                                    LOGGER.warn("Auto-close interrupted for player {}", finalPlayer.getName().getString());
                                                } catch (Exception e) {
                                                    LOGGER.error("Error during auto-close scheduling: {}", e.getMessage());
                                                }
                                            });
                                        }

                                    } catch (Exception e) {
                                        LOGGER.error("Error in fate completion callback: {}", e.getMessage());
                                    }
                                };

                                // Execute with callback (same pattern as tier progression)
                                DeityChat.processSystemConversation(finalPlayer, finalDeityId, completionMessage, completionCallback);
                                LOGGER.info("AI completion response triggered for fate {} with auto-close callback", taskId);

                            } catch (Exception ex) {
                                LOGGER.error("Error sending completion message: {}", ex.getMessage());
                                // Fallback: just notify player of completion
                                finalPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                    "§6⟦ Divine Fate Completed ⟧ §aThe deity acknowledges your accomplishment"));
                            }
                        });

                    } catch (Exception e) {
                        LOGGER.error("Error triggering AI completion response: {}", e.getMessage());
                    }
                });
            }

        } catch (Exception e) {
            LOGGER.error("Error setting up AI completion response: {}", e.getMessage());
        }
    }

}