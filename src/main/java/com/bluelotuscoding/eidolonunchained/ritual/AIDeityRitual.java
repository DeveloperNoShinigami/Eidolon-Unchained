package com.bluelotuscoding.eidolonunchained.ritual;

import com.bluelotuscoding.eidolonunchained.chat.DeityChat;
import com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager;
import com.bluelotuscoding.eidolonunchained.deity.DatapackDeity;
import elucent.eidolon.api.ritual.Ritual;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ritual entry point for AI deity conversations.
 * When performed at a brazier, finds the nearest player and starts an AI conversation
 * with the linked deity. Terminates immediately after starting (async conversation handles itself).
 */
public class AIDeityRitual extends Ritual {
    private static final Logger LOGGER = LoggerFactory.getLogger(AIDeityRitual.class);

    /** The registered ritual resource location that triggered this instance. */
    private final ResourceLocation ritualId;

    /** The ai_deity resource location this ritual invokes. */
    private final ResourceLocation deityId;

    /**
     * @param ritualId The registered ritual ID
     * @param symbol   Eidolon particle symbol resource (e.g. {@code eidolon:particle/daylight_ritual})
     * @param r        Ritual circle red component (0–1)
     * @param g        Ritual circle green component (0–1)
     * @param b        Ritual circle blue component (0–1)
     * @param deityId The deity to start a conversation with
     */
    public AIDeityRitual(ResourceLocation ritualId, ResourceLocation symbol, float r, float g, float b, ResourceLocation deityId) {
        super(symbol, r, g, b);
        this.ritualId = ritualId;
        this.deityId = deityId;
    }

    @Override
    public Ritual cloneRitual() {
        return new AIDeityRitual(ritualId, getSymbol(), getRed(), getGreen(), getBlue(), deityId);
    }

    @Override
    public RitualResult start(Level world, BlockPos pos) {
        if (world.isClientSide || !(world instanceof ServerLevel serverLevel)) {
            return RitualResult.TERMINATE;
        }

        // Find the nearest server player within 16 blocks of the brazier
        ServerPlayer nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (ServerPlayer p : serverLevel.players()) {
            double dist = p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (dist < 256 && dist < nearestDist) { // 16 blocks squared
                nearestDist = dist;
                nearest = p;
            }
        }

        if (nearest == null) {
            LOGGER.warn("AIDeityRitual for {} fired but no player within range of brazier at {}", deityId, pos);
            return RitualResult.TERMINATE;
        }

        DatapackDeity deity = DatapackDeityManager.getDeity(deityId);
        String deityName = deity != null ? deity.getDisplayName() : deityId.toString();

        DeityChat.startConversation(nearest, deityId);
        nearest.sendSystemMessage(Component.translatable("eidolonunchained.ritual.deity_listening", deityName));

        LOGGER.debug("AIDeityRitual started conversation with {} for player {}", deityId, nearest.getName().getString());
        return RitualResult.TERMINATE; // conversation is async; ritual completes immediately
    }

    @Override
    public RitualResult tick(Level world, BlockPos pos) {
        return RitualResult.TERMINATE; // never ticks — terminated in start()
    }

    public ResourceLocation getDeityId() {
        return deityId;
    }

    public ResourceLocation getRitualId() {
        return ritualId;
    }
}
