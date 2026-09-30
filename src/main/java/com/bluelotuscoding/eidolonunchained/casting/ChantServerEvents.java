package com.bluelotuscoding.eidolonunchained.casting;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Keeps {@link PlayerChantState} alive: sync on join, idle timeout each second, forget on logout. */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MOD_ID)
public final class ChantServerEvents {
    private ChantServerEvents() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) PlayerChantState.of(sp).sync();
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        var st = PlayerChantState.peek(event.getEntity().getUUID());
        if (st != null) st.clear(PlayerChantState.ClearReason.LOGOUT);
        PlayerChantState.forget(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            PlayerChantState.forget(sp.getUUID());      // the ServerPlayer instance changed
            PlayerChantState.of(sp).sync();
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer sp)) return;
        var st = PlayerChantState.peek(sp.getUUID());
        if (st != null) st.tick();
    }
}
