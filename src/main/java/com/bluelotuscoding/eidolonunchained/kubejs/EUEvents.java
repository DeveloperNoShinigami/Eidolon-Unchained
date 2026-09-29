package com.bluelotuscoding.eidolonunchained.kubejs;

import com.bluelotuscoding.eidolonunchained.EUConfig;
import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import dev.latvian.mods.kubejs.client.ClientEventJS;
import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.event.StartupEventJS;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.server.ServerEventJS;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * {@code EidolonUnchainedEvents}: one event per script type so a script pack can confirm all three script managers
 * load this mod's plugin. Later phases add the real events (reputation, spells, damage, AI) to this group.
 * <ul>
 *   <li>{@code EidolonUnchainedEvents.init} (startup): after startup scripts load.</li>
 *   <li>{@code EidolonUnchainedEvents.serverReady} (server): when the server has started (and server scripts are loaded).</li>
 *   <li>{@code EidolonUnchainedEvents.clientReady} (client): when the local player has joined a world.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MOD_ID)
public final class EUEvents {
    public static final EventGroup GROUP = EventGroup.of("EidolonUnchainedEvents");
    public static final EventHandler INIT = GROUP.startup("init", () -> StartupEventJS.class);
    public static final EventHandler SERVER_READY = GROUP.server("serverReady", () -> ServerEventJS.class);
    public static final EventHandler CLIENT_READY = GROUP.client("clientReady", () -> ClientEventJS.class);

    private EUEvents() {
    }

    static void postStartup() {
        log("init", ScriptType.STARTUP);
        INIT.post(ScriptType.STARTUP, new StartupEventJS());
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        log("serverReady", ScriptType.SERVER);
        SERVER_READY.post(ScriptType.SERVER, new ServerEventJS(event.getServer()));
    }

    @Mod.EventBusSubscriber(modid = EidolonUnchained.MOD_ID, value = Dist.CLIENT)
    public static final class Client {
        private Client() {
        }

        @SubscribeEvent
        public static void onLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
            log("clientReady", ScriptType.CLIENT);
            CLIENT_READY.post(ScriptType.CLIENT, new ClientEventJS());
        }
    }

    private static void log(String name, ScriptType type) {
        if (EUConfig.LOG_SCRIPT_EVENTS.get()) {
            EidolonUnchained.LOGGER.info("Firing EidolonUnchainedEvents.{} ({} scripts)", name, type.name);
        }
    }
}
