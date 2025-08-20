package com.bluelotuscoding.eidolonunchained.research.tasks;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Task requiring a number of entities with matching NBT to be killed.
 */
public class KillEntityWithNbtTask extends ResearchTask {
    private final ResourceLocation entity;
    @Nullable
    private final CompoundTag filter;
    private final int count;

    public KillEntityWithNbtTask(ResourceLocation entity, @Nullable CompoundTag filter, int count) {
        super(ResearchTaskTypes.KILL_ENTITY_NBT);
        this.entity = entity;
        this.filter = filter;
        this.count = count;
    }

    public ResourceLocation getEntity() {
        return entity;
    }

    @Nullable
    public CompoundTag getFilter() {
        return filter;
    }

    public int getCount() {
        return count;
    }

    @Override
    public boolean isComplete(Player player) {
        return KillEntityWithNbtTaskHandler.getKillCount(player, this) >= count;
    }

    String getKey() {
        String key = entity.toString();
        if (filter != null) key += filter.toString();
        return key;
    }
}
