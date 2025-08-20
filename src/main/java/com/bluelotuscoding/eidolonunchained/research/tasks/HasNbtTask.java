package com.bluelotuscoding.eidolonunchained.research.tasks;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

/**
 * Task requiring the player's persistent NBT data to contain a specific tag.
 */
public class HasNbtTask extends ResearchTask {
    private final CompoundTag required;

    public HasNbtTask(CompoundTag required) {
        super(ResearchTaskTypes.HAS_NBT);
        this.required = required;
    }

    /**
     * @return the NBT tag that must be present on the player
     */
    public CompoundTag getRequired() {
        return required;
    }

    /**
     * Checks completion against a server player by comparing their persistent
     * data to the required tag.
     */
    public boolean isComplete(ServerPlayer player) {
        return player != null && NbtUtils.compareNbt(required, player.getPersistentData(), true);
    }

    @Override
    public boolean isComplete(Player player) {
        return player instanceof ServerPlayer sp && isComplete(sp);
    }
}

