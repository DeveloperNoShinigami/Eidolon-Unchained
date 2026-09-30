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
import com.bluelotuscoding.eidolonunchained.casting.MobChantGoal;
import com.bluelotuscoding.eidolonunchained.casting.PlayerChantState;
import elucent.eidolon.api.deity.ReputationEvent;
import java.util.ArrayList;
import elucent.eidolon.api.spells.SpellCastEvent;
import elucent.eidolon.codex.CodexEvents;
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
 *   <li>{@code chantCast} (server, cancel() blocks the cast) / {@code chantCasted}: Eidolon's {@code SpellCastEvent.Pre/Post}; extra id = chant id.</li>
 *   <li>{@code chantSign} (cancelable) / {@code chantMatched} (cancelable, extra id = the matched chant, before the wind-up) / {@code chantCleared} (server): active chanting.</li>
 *   <li>{@code mobChantStarted / mobChantSign / mobChantCast (cancelable) / mobChantInterrupted} (server): mob casting; extra id = spell id.</li>
 *   <li>{@code codexPreInit / codexPostInit} (client): Eidolon's {@code CodexEvents}.</li>
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
    public static final EventHandler CHANT_CAST = GROUP.server("chantCast", () -> SpellCastEventJS.class).extra(Extra.ID).hasResult();
    public static final EventHandler CHANT_CASTED = GROUP.server("chantCasted", () -> SpellCastEventJS.class).extra(Extra.ID);
    public static final EventHandler CHANT_SIGN = GROUP.server("chantSign", () -> ChantEventJS.class).hasResult();
    public static final EventHandler CHANT_MATCHED = GROUP.server("chantMatched", () -> ChantEventJS.class).extra(Extra.ID).hasResult();
    public static final EventHandler CHANT_CLEARED = GROUP.server("chantCleared", () -> ChantEventJS.class);
    public static final EventHandler MOB_CHANT_STARTED = GROUP.server("mobChantStarted", () -> MobChantEventJS.class).extra(Extra.ID);
    public static final EventHandler MOB_CHANT_SIGN = GROUP.server("mobChantSign", () -> MobChantEventJS.class).extra(Extra.ID);
    public static final EventHandler MOB_CHANT_CAST = GROUP.server("mobChantCast", () -> MobChantEventJS.class).extra(Extra.ID).hasResult();
    public static final EventHandler MOB_CHANT_INTERRUPTED = GROUP.server("mobChantInterrupted", () -> MobChantEventJS.class).extra(Extra.ID);
    public static final EventHandler CODEX_PRE_INIT = GROUP.client("codexPreInit", () -> CodexEventJS.class);
    public static final EventHandler CODEX_POST_INIT = GROUP.client("codexPostInit", () -> CodexEventJS.class);

    public static final EventHandler IMBUE_CAST = GROUP.server("imbueCast", () -> WeaponChantEventJS.class).extra(Extra.ID).hasResult();
    public static final EventHandler PROTECTION_TRIGGERED = GROUP.server("protectionTriggered", () -> WeaponChantEventJS.class).extra(Extra.ID).hasResult();

    private EUEvents() {
    }

    /** D35: imbued right-click casts and Deity's Protection retaliations, both cancelable. */
    static void hookWeapons() {
        com.bluelotuscoding.eidolonunchained.imbue.ImbueCasting.onImbueCast = (player, weapon, chant) ->
                !IMBUE_CAST.hasListeners() || !IMBUE_CAST.post(ScriptType.SERVER, chant, new WeaponChantEventJS(player, null, weapon, chant.toString(), 0)).interruptFalse();
        com.bluelotuscoding.eidolonunchained.imbue.ImbueCasting.onProtection = (wearer, attacker, piece, chant, level) ->
                !PROTECTION_TRIGGERED.hasListeners() || !PROTECTION_TRIGGERED.post(ScriptType.SERVER, chant, new WeaponChantEventJS(wearer, attacker, piece, chant.toString(), level)).interruptFalse();
    }

    /** Wires the server chant state's script hooks to the events above (called once by the plugin). */
    static void hookChant() {
        PlayerChantState.onSign = (player, sign) -> {
            if (!CHANT_SIGN.hasListeners()) return true;
            var seq = new ArrayList<>(PlayerChantState.of(player).sequence()); seq.add(sign);
            return !CHANT_SIGN.post(ScriptType.SERVER, new ChantEventJS(player, sign, seq, null)).interruptFalse();
        };
        PlayerChantState.onCast = (player, signs) -> {
            if (!CHANT_MATCHED.hasListeners()) return true;
            var spell = PlayerChantState.of(player).resolved();
            var id = spell == null ? null : spell.getRegistryName();
            return !CHANT_MATCHED.post(ScriptType.SERVER, id, new ChantEventJS(player, null, signs, id == null ? null : id.toString())).interruptFalse();
        };
        PlayerChantState.onCleared = player -> {
            if (CHANT_CLEARED.hasListeners()) CHANT_CLEARED.post(ScriptType.SERVER, new ChantEventJS(player, null, java.util.List.of(), null));
            return null;
        };
    }

    static void hookMobChant() {
        MobChantGoal.onStarted = (m, s, t, sg, ss, r) -> { if (MOB_CHANT_STARTED.hasListeners()) MOB_CHANT_STARTED.post(ScriptType.SERVER, s.getRegistryName(), new MobChantEventJS(m, s.getRegistryName().toString(), t, sg, ss, r)); return true; };
        MobChantGoal.onSign = (m, s, t, sg, ss, r) -> { if (MOB_CHANT_SIGN.hasListeners()) MOB_CHANT_SIGN.post(ScriptType.SERVER, s.getRegistryName(), new MobChantEventJS(m, s.getRegistryName().toString(), t, sg, ss, r)); return true; };
        MobChantGoal.onCast = (m, s, t, sg, ss, r) -> !MOB_CHANT_CAST.hasListeners() || !MOB_CHANT_CAST.post(ScriptType.SERVER, s.getRegistryName(), new MobChantEventJS(m, s.getRegistryName().toString(), t, sg, ss, r)).interruptFalse();
        MobChantGoal.onInterrupted = (m, s, t, sg, ss, r) -> { if (MOB_CHANT_INTERRUPTED.hasListeners() && s != null) MOB_CHANT_INTERRUPTED.post(ScriptType.SERVER, s.getRegistryName(), new MobChantEventJS(m, s.getRegistryName().toString(), t, sg, ss, r)); return true; };
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

    @SubscribeEvent
    public static void onSpellCastPre(SpellCastEvent.Pre event) {
        if (!CHANT_CAST.hasListeners() || event.world.isClientSide()) return;
        var result = CHANT_CAST.post(ScriptType.SERVER, event.spell.getRegistryName(), new SpellCastEventJS(event));
        if (result.interruptFalse()) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onSpellCastPost(SpellCastEvent.Post event) {
        if (CHANT_CASTED.hasListeners() && !event.world.isClientSide()) CHANT_CASTED.post(ScriptType.SERVER, event.spell.getRegistryName(), new SpellCastEventJS(event));
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

        @SubscribeEvent
        public static void onCodexPreInit(CodexEvents.PreInit event) {
            if (CODEX_PRE_INIT.hasListeners()) CODEX_PRE_INIT.post(ScriptType.CLIENT, new CodexEventJS(event));
        }

        @SubscribeEvent
        public static void onCodexPostInit(CodexEvents.PostInit event) {
            if (CODEX_POST_INIT.hasListeners()) CODEX_POST_INIT.post(ScriptType.CLIENT, new CodexEventJS(event));
        }
    }

    private static void log(String name, ScriptType type) {
        if (EUConfig.LOG_SCRIPT_EVENTS.get()) {
            EidolonUnchained.LOGGER.info("Firing EidolonUnchainedEvents.{} ({} scripts)", name, type.name);
        }
    }
}
