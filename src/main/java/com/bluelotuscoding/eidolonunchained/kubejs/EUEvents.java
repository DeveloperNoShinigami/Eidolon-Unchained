package com.bluelotuscoding.eidolonunchained.kubejs;

import com.bluelotuscoding.eidolonunchained.EUConfig;
import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import dev.latvian.mods.kubejs.client.ClientEventJS;
import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.event.Extra;
import dev.latvian.mods.kubejs.event.StartupEventJS;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.server.ServerEventJS;
import elucent.eidolon.api.deity.ReputationEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * {@code EidolonUnchainedEvents}. Phase 1: one event per script type. Phase 2: Eidolon's reputation events.
 * <ul>
 *   <li>{@code init} (startup): after startup scripts load.</li>
 *   <li>{@code serverReady} (server): when the server has started.</li>
 *   <li>{@code clientReady} (client): when the local player has joined a world.</li>
 *   <li>{@code reputationChanged / stageUnlocked / stageLocked} (server): Eidolon's {@link ReputationEvent}s; the
 *   optional extra id is a deity id, e.g. {@code stageUnlocked('eidolon:dark', e => …)}.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MOD_ID)
public final class EUEvents {
    public static final EventGroup GROUP = EventGroup.of("EidolonUnchainedEvents");
    public static final EventHandler INIT = GROUP.startup("init", () -> StartupEventJS.class);
    public static final EventHandler SERVER_READY = GROUP.server("serverReady", () -> ServerEventJS.class);
    public static final EventHandler CLIENT_READY = GROUP.client("clientReady", () -> ClientEventJS.class);
    public static final EventHandler REPUTATION_CHANGED = GROUP.server("reputationChanged", () -> ReputationEventJS.class).extra(Extra.ID);
    public static final EventHandler STAGE_UNLOCKED = GROUP.server("stageUnlocked", () -> ReputationEventJS.class).extra(Extra.ID);
    public static final EventHandler STAGE_LOCKED = GROUP.server("stageLocked", () -> ReputationEventJS.class).extra(Extra.ID);

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

    @SubscribeEvent
    public static void onReputationChange(ReputationEvent.Change event) {
        if (REPUTATION_CHANGED.hasListeners()) REPUTATION_CHANGED.post(ScriptType.SERVER, event.deity.getId(), new ReputationEventJS(event));
    }

    @SubscribeEvent
    public static void onStageUnlock(ReputationEvent.Unlock event) {
        if (STAGE_UNLOCKED.hasListeners()) STAGE_UNLOCKED.post(ScriptType.SERVER, event.deity.getId(), new ReputationEventJS(event));
    }

    @SubscribeEvent
    public static void onStageLock(ReputationEvent.Lock event) {
        if (STAGE_LOCKED.hasListeners()) STAGE_LOCKED.post(ScriptType.SERVER, event.deity.getId(), new ReputationEventJS(event));
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
