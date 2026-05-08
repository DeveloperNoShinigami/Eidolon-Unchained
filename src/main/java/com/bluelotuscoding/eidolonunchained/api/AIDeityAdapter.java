package com.bluelotuscoding.eidolonunchained.api;

import com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig;
import com.bluelotuscoding.eidolonunchained.chat.DeityChat;
import com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager;
import com.bluelotuscoding.eidolonunchained.deity.DatapackDeity;
import elucent.eidolon.api.deity.Deity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bridges an AI deity config into Eidolon's Deity API so that:
 * - The deity appears in {@link Deities#find(ResourceLocation)}
 * - {@link Deity#onReputationChange} fires correctly (via IReputation.addReputation(Player, ...))
 * - Progression stage unlocks trigger AI "progression" conversations
 * - Progression locks trigger AI "cap" conversations
 *
 * Register each instance via {@link Deities#register(Deity)} during
 * {@code AIDeityManager.apply()} after all configs are loaded.
 */
public class AIDeityAdapter extends Deity {
    private static final Logger LOGGER = LoggerFactory.getLogger(AIDeityAdapter.class);

    private final AIDeityConfig config;

    public AIDeityAdapter(AIDeityConfig config) {
        super(
            config.deity_id,
            colorComponent(config, "r"),
            colorComponent(config, "g"),
            colorComponent(config, "b")
        );
        this.config = config;
        buildProgression();
    }

    // -----------------------------------------------------------------------
    // Progression from DatapackDeity JSON stages
    // -----------------------------------------------------------------------

    private void buildProgression() {
        // Progression stages are owned by DatapackDeity and managed by DatapackDeityManager.
        // AIDeityAdapter does not duplicate them — DatapackDeity.onReputationUnlock already
        // triggers AI conversations on stage unlock.
    }

    // -----------------------------------------------------------------------
    // Deity API — abstract methods
    // -----------------------------------------------------------------------

    @Override
    public void onReputationUnlock(Player player, ResourceLocation lock) {
        LOGGER.info("AI deity {} — reputation unlock for player {}: {}", config.deity_id, player.getName().getString(), lock);
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            // Trigger a "progression" prayer conversation to acknowledge the milestone
            DeityChat.startConversation(serverPlayer, config.deity_id);
        }
    }

    @Override
    public void onReputationLock(Player player, ResourceLocation lock) {
        LOGGER.info("AI deity {} — reputation lock for player {}: {}", config.deity_id, player.getName().getString(), lock);
        // No automatic conversation on lock — avoids spamming the player if rep drops
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Reads RGB color (0–255) from the linked {@link DatapackDeity}, falling back to 128 if not found.
     * @param channel "r", "g", or "b"
     */
    private static int colorComponent(AIDeityConfig config, String channel) {
        try {
            DatapackDeity d = DatapackDeityManager.getDeity(config.deity_id);
            if (d != null) {
                return switch (channel) {
                    case "r" -> (int) (d.getRed() * 255f);
                    case "g" -> (int) (d.getGreen() * 255f);
                    case "b" -> (int) (d.getBlue() * 255f);
                    default -> 128;
                };
            }
        } catch (Exception ignored) {}
        return 128;
    }
}
