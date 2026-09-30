package com.bluelotuscoding.eidolonunchained.kubejs;

import dev.latvian.mods.kubejs.player.PlayerEventJS;
import dev.latvian.mods.kubejs.typings.Info;
import elucent.eidolon.api.spells.Sign;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** {@code EidolonUnchainedEvents.chantSign / chantCast / chantCleared}: a player's out-of-codex chant (rule C1). */
public class ChantEventJS extends PlayerEventJS {
    private final Player player;
    private final @Nullable Sign sign;
    private final List<Sign> signs;
    private final @Nullable String spell;

    public ChantEventJS(Player player, @Nullable Sign sign, List<Sign> signs, @Nullable String spell) {
        this.player = player;
        this.sign = sign;
        this.signs = signs;
        this.spell = spell;
    }

    @Override
    public Player getEntity() {
        return player;
    }

    @Info("The sign just pressed (chantSign only)")
    public @Nullable String getSign() {
        return sign == null ? null : sign.getRegistryName().toString();
    }

    @Info("The sequence (after the press for chantSign; the full chant for chantCast)")
    public List<String> getSigns() {
        return signs.stream().map(s -> s.getRegistryName().toString()).toList();
    }

    @Info("The chant the sequence resolves to, or null (chantMatched only)")
    public @Nullable String getChant() {
        return spell;
    }
}
