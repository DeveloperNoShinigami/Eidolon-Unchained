package com.bluelotuscoding.eidolonunchained.api;

import com.bluelotuscoding.eidolonunchained.casting.PlayerChantState;
import dev.latvian.mods.kubejs.typings.Info;
import elucent.eidolon.registries.Signs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/** {@code player.data.eidolon.chant}: the player's server-side chant state (slots and the in-progress sequence). */
public final class ChantHelper {
    private final Player player;

    ChantHelper(Player player) {
        this.player = player;
    }

    private PlayerChantState state() {
        if (!(player instanceof ServerPlayer sp)) throw new IllegalStateException("Eidolon Unchained: chant state lives on the server");
        return PlayerChantState.of(sp);
    }

    @Info("Assign a known sign to a slot (0-8); null clears the slot")
    public void assign(int slot, String signId) {
        state().assign(slot, signId);
    }

    @Info("Sign id per slot ('' when unassigned)")
    public List<String> slots() {
        return state().slotIds();
    }

    @Info("The signs chanted so far")
    public List<String> sequence() {
        return state().sequence().stream().map(s -> s.getRegistryName().toString()).toList();
    }

    @Info("Append a sign the player knows")
    public void addSign(String signId) {
        var sign = Signs.find(Ids.of(signId, "sign"));
        if (sign == null) throw new IllegalArgumentException("Eidolon Unchained: unknown sign '" + signId + "'");
        state().addSign(sign);
    }

    public void clear() {
        state().clear(PlayerChantState.ClearReason.SCRIPT);
    }

    @Info("Cast the current sequence through Eidolon's executor; false when empty or cancelled")
    public boolean cast() {
        return state().cast();
    }

    @Info("Id of the chant the current sequence resolves to, or null")
    public String resolved() {
        var s = state().resolved();
        return s == null ? null : s.getRegistryName().toString();
    }
}
