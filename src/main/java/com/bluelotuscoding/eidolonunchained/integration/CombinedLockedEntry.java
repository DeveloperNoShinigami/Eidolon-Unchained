package com.bluelotuscoding.eidolonunchained.integration;

import elucent.eidolon.Eidolon;
import elucent.eidolon.codex.Chapter;
import elucent.eidolon.codex.IndexPage;
import elucent.eidolon.capability.IReputation;
import elucent.eidolon.util.KnowledgeUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Locks a chapter behind multiple conditions at once (facts AND reputation).
 * - All listed facts must be known
 * - Reputation must be >= required value for the specified deity (if provided)
 */
public class CombinedLockedEntry extends IndexPage.IndexEntry {
    private final java.util.List<ResourceLocation> requiredFacts;
    private final java.util.List<ResourceLocation> requiredResearch;
    private final Integer requiredReputation;
    private final ResourceLocation deityId;

    public CombinedLockedEntry(Chapter chapter,
                               ItemStack icon,
                               java.util.List<ResourceLocation> requiredFacts,
                               java.util.List<ResourceLocation> requiredResearch,
                               Integer requiredReputation,
                               ResourceLocation deityId) {
        super(chapter, icon);
        this.requiredFacts = requiredFacts;
        this.requiredResearch = requiredResearch;
        this.requiredReputation = requiredReputation;
        this.deityId = deityId;
    }

    @Override
    public boolean isUnlocked() {
        // Check facts (AND semantics)
        if (requiredFacts != null && !requiredFacts.isEmpty()) {
            for (ResourceLocation fact : requiredFacts) {
                if (!KnowledgeUtil.knowsFact(Eidolon.proxy.getPlayer(), fact)) {
                    return false;
                }
            }
        }

        // Check research (AND semantics)
        if (requiredResearch != null && !requiredResearch.isEmpty()) {
            for (ResourceLocation r : requiredResearch) {
                if (!KnowledgeUtil.knowsResearch(Eidolon.proxy.getPlayer(), r)) {
                    return false;
                }
            }
        }

        // Check reputation requirement if specified
        if (requiredReputation != null && deityId != null) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return true; // match Eidolon behavior
            double rep = server.overworld().getCapability(IReputation.INSTANCE).resolve().get()
                .getReputation(Eidolon.proxy.getPlayer(), deityId);
            if (rep < requiredReputation) return false;
        }

        return true;
    }
}
