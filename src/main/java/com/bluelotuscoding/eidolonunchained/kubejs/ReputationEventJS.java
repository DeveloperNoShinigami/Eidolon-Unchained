package com.bluelotuscoding.eidolonunchained.kubejs;

import dev.latvian.mods.kubejs.player.PlayerEventJS;
import dev.latvian.mods.kubejs.typings.Info;
import elucent.eidolon.api.deity.ReputationEvent;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * {@code EidolonUnchainedEvents.reputationChanged / stageUnlocked / stageLocked}: Eidolon's {@link ReputationEvent}s
 * forwarded to scripts. Extra id is the deity id, so {@code stageUnlocked('eidolon:dark', e => …)} filters by deity.
 */
public class ReputationEventJS extends PlayerEventJS {
    private final ReputationEvent event;

    public ReputationEventJS(ReputationEvent event) {
        this.event = event;
    }

    @Override
    public Player getEntity() {
        return event.player;
    }

    @Info("Deity id, e.g. 'eidolon:dark'")
    public String getDeity() {
        return event.deity.getId().toString();
    }

    @Info("Reputation before the change (change events only; otherwise current reputation)")
    public double getOldRep() {
        return event instanceof ReputationEvent.Change c ? c.oldRep : 0;
    }

    @Info("Reputation after the change (change events only)")
    public double getNewRep() {
        return event instanceof ReputationEvent.Change c ? c.newRep : 0;
    }

    @Info("Stage id (unlock/lock events only)")
    @Nullable
    public String getStage() {
        return event.stage == null ? null : event.stage.id().toString();
    }

    @Info("Whether the stage is a major one (unlock/lock events only)")
    public boolean isMajor() {
        return event.stage != null && event.stage.major();
    }
}
