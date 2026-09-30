package com.bluelotuscoding.eidolonunchained.client;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.casting.PlayerChantState;
import com.bluelotuscoding.eidolonunchained.network.ChantInputPacket;
import com.bluelotuscoding.eidolonunchained.network.ChantStatePacket;
import com.bluelotuscoding.eidolonunchained.network.EUNetwork;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Client side of active chanting (D37): sign slot keys (1–9) and a cancel key; no cast key and no HUD ribbon. The
 * signs appear in the world as they are chanted (server particles) and the chant fires when it matches. Defaults per
 * D34: slots 1–4 on G/H/J/K, cancel X; everything rebindable.
 */
public final class ChantClient {
    public static final String CATEGORY = "key.categories.eidolonunchained";
    public static final KeyMapping[] SLOT_KEYS = new KeyMapping[PlayerChantState.SLOTS];
    public static final KeyMapping CLEAR = key("clear", GLFW.GLFW_KEY_X);

    // last synced state (for client scripts and future presentation)
    public static List<String> signs = new ArrayList<>();
    public static String spell = "";
    public static int cost = 0;
    public static List<String> slots = new ArrayList<>();

    private static final int[] SLOT_DEFAULTS = {GLFW.GLFW_KEY_G, GLFW.GLFW_KEY_H, GLFW.GLFW_KEY_J, GLFW.GLFW_KEY_K,
            InputConstants.UNKNOWN.getValue(), InputConstants.UNKNOWN.getValue(), InputConstants.UNKNOWN.getValue(),
            InputConstants.UNKNOWN.getValue(), InputConstants.UNKNOWN.getValue()};

    static {
        for (int i = 0; i < SLOT_KEYS.length; i++) SLOT_KEYS[i] = key("slot_" + (i + 1), SLOT_DEFAULTS[i]);
    }

    private ChantClient() {
    }

    private static KeyMapping key(String name, int defaultKey) {
        return new KeyMapping("key." + EidolonUnchained.MOD_ID + "." + name, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, defaultKey, CATEGORY);
    }

    /** Signs currently shown around an entity (players and mobs), by entity id: what the ring renderer draws. */
    public static final java.util.Map<Integer, List<String>> RINGS = new java.util.HashMap<>();
    public static final java.util.Set<Integer> WINDING = new java.util.HashSet<>();

    public static void accept(ChantStatePacket p) {
        var me = Minecraft.getInstance().player;
        if (me != null && me.getId() == p.entityId()) {
            signs = p.signs();
            spell = p.spell();
            cost = p.cost();
            slots = p.slots();
        }
        setRing(p.entityId(), p.signs(), p.winding());
    }

    public static void acceptMob(com.bluelotuscoding.eidolonunchained.network.MobChantStatePacket p) {
        setRing(p.entityId(), p.signs(), false);
    }

    private static void setRing(int entityId, List<String> ringSigns, boolean winding) {
        if (ringSigns.isEmpty()) { RINGS.remove(entityId); WINDING.remove(entityId); }
        else { RINGS.put(entityId, ringSigns); if (winding) WINDING.add(entityId); else WINDING.remove(entityId); }
    }

    @Mod.EventBusSubscriber(modid = EidolonUnchained.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        private ModBus() {
        }

        @SubscribeEvent
        public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
            for (var k : SLOT_KEYS) event.register(k);
            event.register(CLEAR);
        }
    }

    @Mod.EventBusSubscriber(modid = EidolonUnchained.MOD_ID, value = Dist.CLIENT)
    public static final class ForgeBus {
        private ForgeBus() {
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END || Minecraft.getInstance().player == null || Minecraft.getInstance().screen != null) return;
            for (int i = 0; i < SLOT_KEYS.length; i++) {
                while (SLOT_KEYS[i].consumeClick()) EUNetwork.sendToServer(ChantInputPacket.sign(i));
            }
            while (CLEAR.consumeClick()) EUNetwork.sendToServer(ChantInputPacket.clear());
        }
    }
}
