package com.bluelotuscoding.eidolonunchained.research.triggers;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

/**
 * Shared utility for notetaking tool checks used by all research trigger classes.
 */
public class ResearchItemUtil {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static boolean hasNotetakingTools(ServerPlayer player) {
        try {
            return player.getInventory().hasAnyMatching(stack -> {
                String itemName = stack.getItem().toString().toLowerCase();
                return itemName.contains("notetaking");
            });
        } catch (Exception e) {
            LOGGER.error("Failed to check notetaking tools: {}", e.getMessage());
            return false;
        }
    }

    public static boolean consumeNotetakingTool(ServerPlayer player) {
        try {
            var inventory = player.getInventory();
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (!stack.isEmpty()) {
                    String itemName = stack.getItem().toString().toLowerCase();
                    if (itemName.contains("notetaking")) {
                        stack.shrink(1);
                        LOGGER.debug("Consumed 1 notetaking tool from player {}", player.getName().getString());
                        return true;
                    }
                }
            }
            LOGGER.warn("Failed to find notetaking tool to consume for player {}", player.getName().getString());
            return false;
        } catch (Exception e) {
            LOGGER.error("Failed to consume notetaking tool: {}", e.getMessage());
            return false;
        }
    }
}
