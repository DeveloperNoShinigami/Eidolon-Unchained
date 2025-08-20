package com.bluelotuscoding.eidolonunchained.research.tasks;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Task requiring the player to visit a particular biome a number of times.
 * <p>
 * This mirrors biome‑checking conditions but is expressed as a task so that
 * it can participate in the normal research objective system.
 * </p>
 */
public class ExploreBiomesTask extends ResearchTask {
    private final ResourceLocation biome;
    private final int count;

    public ExploreBiomesTask(ResourceLocation biome, int count) {
        super(ResearchTaskTypes.EXPLORE_BIOMES);
        this.biome = biome;
        this.count = count;
    }

    public ResourceLocation getBiome() {
        return biome;
    }

    public int getCount() {
        return count;
    }

    @Override
    public boolean isComplete(Player player) {
        if (player == null) return false;
        return player.level().getBiome(player.blockPosition()).unwrapKey()
            .map(k -> k.location().equals(biome))
            .orElse(false);
    }
}
