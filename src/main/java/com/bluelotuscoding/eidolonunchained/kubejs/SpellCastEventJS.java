package com.bluelotuscoding.eidolonunchained.kubejs;

import dev.latvian.mods.kubejs.level.BlockContainerJS;
import dev.latvian.mods.kubejs.player.PlayerEventJS;
import dev.latvian.mods.kubejs.typings.Info;
import elucent.eidolon.api.spells.SpellCastEvent;
import net.minecraft.world.entity.player.Player;

import java.util.Arrays;
import java.util.List;

/** {@code EidolonUnchainedEvents.chantCast} (before, cancelable) / {@code chantCasted} (after): Eidolon's {@link SpellCastEvent}. */
public class SpellCastEventJS extends PlayerEventJS {
    private final SpellCastEvent event;

    public SpellCastEventJS(SpellCastEvent event) {
        this.event = event;
    }

    @Override
    public Player getEntity() {
        return event.player;
    }

    @Info("Chant id")
    public String getChant() {
        return event.spell.getRegistryName().toString();
    }

    @Info("The signs that were chanted, in order")
    public List<String> getSigns() {
        return Arrays.stream(event.signs.toArray()).map(s -> s.getRegistryName().toString()).toList();
    }

    public BlockContainerJS getBlock() {
        return new BlockContainerJS(event.world, event.pos);
    }

    public int getCost() {
        return event.spell.getCost();
    }
}
