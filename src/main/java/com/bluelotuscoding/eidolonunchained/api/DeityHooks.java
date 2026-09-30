package com.bluelotuscoding.eidolonunchained.api;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import elucent.eidolon.api.deity.ReputationEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Per-deity script reactions and the avatar model id. Fed by {@link DeityBuilder}; fired from Eidolon's
 * {@link ReputationEvent}s (which cover Eidolon's own deities as well as scripted ones) and, for scripted deities,
 * from {@code Deity.onReputationUnlock/Lock}. The global {@code EidolonUnchainedEvents} are posted by the KubeJS layer
 * from the same Forge events, so a script can use either.
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MOD_ID)
public final class DeityHooks {
    private record Hooks(ResourceLocation model, List<BiConsumer<Player, String>> onUnlock, List<BiConsumer<Player, String>> onLock,
                         List<DeityBuilder.ReputationListener> onChange) {
    }

    private static final Map<ResourceLocation, Hooks> HOOKS = new HashMap<>();

    private DeityHooks() {
    }

    static void register(ResourceLocation deity, ResourceLocation model, List<BiConsumer<Player, String>> onUnlock,
                         List<BiConsumer<Player, String>> onLock, List<DeityBuilder.ReputationListener> onChange) {
        var prev = HOOKS.get(deity);
        var u = new ArrayList<>(onUnlock); var l = new ArrayList<>(onLock); var c = new ArrayList<>(onChange);
        if (prev != null) { u.addAll(0, prev.onUnlock()); l.addAll(0, prev.onLock()); c.addAll(0, prev.onChange()); }
        HOOKS.put(deity, new Hooks(model != null ? model : (prev != null ? prev.model() : null), u, l, c));
    }

    /** The avatar model id a script gave this deity, or null. */
    public static ResourceLocation model(ResourceLocation deity) {
        var h = HOOKS.get(deity);
        return h == null ? null : h.model();
    }

    static void unlocked(ResourceLocation deity, Player player, ResourceLocation stage) {
        var h = HOOKS.get(deity);
        if (h != null) for (var fn : h.onUnlock()) safely(fn, player, stage, deity, "onStageUnlocked");
    }

    static void locked(ResourceLocation deity, Player player, ResourceLocation stage) {
        var h = HOOKS.get(deity);
        if (h != null) for (var fn : h.onLock()) safely(fn, player, stage, deity, "onStageLocked");
    }

    // Eidolon posts these for every deity, before it calls the deity's own unlock/lock callbacks. Scripted deities'
    // builder hooks for unlock/lock are driven from the Deity callbacks (above) so they fire once; for extended Eidolon
    // deities (which never call our callbacks) they are driven from here.
    @SubscribeEvent
    public static void onChange(ReputationEvent.Change event) {
        var h = HOOKS.get(event.deity.getId());
        if (h == null) return;
        for (var fn : h.onChange()) {
            try {
                fn.changed(event.player, event.oldRep, event.newRep);
            } catch (RuntimeException e) {
                EidolonUnchained.LOGGER.error("deity '{}' onReputationChanged threw: {}", event.deity.getId(), e.toString());
            }
        }
    }

    @SubscribeEvent
    public static void onUnlock(ReputationEvent.Unlock event) {
        if (!(event.deity instanceof DeityBuilder.ScriptedDeity) && event.stage != null) unlocked(event.deity.getId(), event.player, event.stage.id());
    }

    @SubscribeEvent
    public static void onLock(ReputationEvent.Lock event) {
        if (!(event.deity instanceof DeityBuilder.ScriptedDeity) && event.stage != null) locked(event.deity.getId(), event.player, event.stage.id());
    }

    private static void safely(BiConsumer<Player, String> fn, Player player, ResourceLocation stage, ResourceLocation deity, String what) {
        try {
            fn.accept(player, stage.toString());
        } catch (RuntimeException e) {
            EidolonUnchained.LOGGER.error("deity '{}' {} threw: {}", deity, what, e.toString());
        }
    }
}
