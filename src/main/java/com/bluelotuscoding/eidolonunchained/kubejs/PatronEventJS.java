package com.bluelotuscoding.eidolonunchained.kubejs;

import dev.latvian.mods.kubejs.player.PlayerEventJS;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * {@code EidolonUnchainedEvents.patronChanged} (server): a player pledged to a deity or a pledge was revoked (D47).
 * Extra id is the deity id, so {@code patronChanged('eu_examples:myrkul', e => …)} filters by deity.
 */
public class PatronEventJS extends PlayerEventJS {
    private final Player player;
    private final ResourceLocation deity;
    private final ResourceLocation previous;
    private final boolean granted;

    public PatronEventJS(Player player, ResourceLocation deity, @Nullable ResourceLocation previous, boolean granted) {
        this.player = player;
        this.deity = deity;
        this.previous = previous;
        this.granted = granted;
    }

    @Override
    public Player getEntity() {
        return player;
    }

    @Info("The deity pledged to or revoked, e.g. 'eu_examples:myrkul'")
    public String getDeity() {
        return deity.toString();
    }

    @Info("The player's major patron before this change, or null")
    @Nullable
    public String getPrevious() {
        return previous == null ? null : previous.toString();
    }

    @Info("True for a pledge, false for a revoke")
    public boolean isGranted() {
        return granted;
    }
}
