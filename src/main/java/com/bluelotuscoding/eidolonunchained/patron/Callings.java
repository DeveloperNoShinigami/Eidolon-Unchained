package com.bluelotuscoding.eidolonunchained.patron;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.network.CallingPacket;
import com.bluelotuscoding.eidolonunchained.network.EUNetwork;
import elucent.eidolon.common.deity.Deities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Callings (D49): a god offers itself and the player answers in their own words. A discovery's
 * {@code .offersPatronage(god)} starts it: the greeting and question are typed out on the player's action bar
 * ({@link CallingPacket}); the player answers in chat (the message is kept private); yes pledges on the spot, no or
 * silence until the wait runs out declines, and the god won't call again for its cooldown (or never). A player bound to
 * another major god is told so once per rival god, then left alone.
 * Per player: {@code eidolonunchained.callings {called:[...], next:{god: game time, -1 = never}, bound_told:{god: rival}}}.
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MOD_ID)
public final class Callings {
    /** What a god says and which answers it accepts; lines are lang keys or plain text ({@code %s} = the player). */
    public record Settings(String greeting, String question, String accept, String decline, String silence, String bound,
                           List<String> yes, List<String> no, int waitTicks, long callAgainTicks, ResourceLocation voice) {
        public static final Settings DEFAULT = new Settings("eidolonunchained.calling.greeting", "eidolonunchained.calling.question",
                "eidolonunchained.calling.accept", "eidolonunchained.calling.decline", "eidolonunchained.calling.silence",
                "eidolonunchained.calling.bound", List.of("yes", "y", "i will", "i accept"), List.of("no", "n", "never"), 60 * 20, -1,
                new ResourceLocation("eidolon", "chant_word"));
    }

    private record Active(ResourceLocation god, long expiresAt) {
    }

    /** Typing the greeting and question takes a few seconds before the wait proper begins. */
    private static final int TYPING_GRACE_TICKS = 120;
    private static final String ROOT = EidolonUnchained.MOD_ID, NBT = "callings";

    private static final Map<ResourceLocation, Settings> SETTINGS = new HashMap<>();
    private static final Map<UUID, Active> ACTIVE = new HashMap<>();

    private Callings() {
    }

    public static void declare(ResourceLocation god, Settings settings) {
        SETTINGS.put(god, settings);
    }

    private static Settings settings(ResourceLocation god) {
        return SETTINGS.getOrDefault(god, Settings.DEFAULT);
    }

    /** True once the god has called this player (for {@code .requiresCalling(true)} gods' pledge rites). */
    public static boolean wasCalled(Player player, ResourceLocation god) {
        var list = data(player).getList("called", net.minecraft.nbt.Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) if (list.getString(i).equals(god.toString())) return true;
        return false;
    }

    /** The god reaches out, unless the player already follows it, is mid-conversation, or the god's cooldown runs. */
    public static void offer(ServerPlayer player, ResourceLocation god) {
        if (Deities.find(god) == null) { skip(player, god, "unknown god"); return; }
        if (Patrons.pledged(player, god)) { skip(player, god, "already a follower"); return; }
        if (ACTIVE.containsKey(player.getUUID())) { skip(player, god, "already in a conversation"); return; }
        long now = player.level().getGameTime();
        var data = data(player);
        var next = data.getCompound("next");
        if (next.contains(god.toString()) && (next.getLong(god.toString()) < 0 || next.getLong(god.toString()) > now)) {
            skip(player, god, next.getLong(god.toString()) < 0 ? "declined; this god does not call again" : "on cooldown");
            return;
        }
        var s = settings(god);
        markCalled(player, god);
        var major = Patrons.majorPatron(player);
        if (Patrons.isRequired(god) && major != null && !major.equals(god)) {
            // it can't have them while another major god does: it says so once per rival, then keeps silent
            var told = data(player).getCompound("bound_told");
            if (major.toString().equals(told.getString(god.toString()))) { skip(player, god, "bound to " + major + " (already told)"); return; }
            speak(player, god, false, line(s.greeting, player), line(s.bound, player, Patrons.deityName(major)));
            var d = data(player);
            var t = d.getCompound("bound_told");
            t.putString(god.toString(), major.toString());
            d.put("bound_told", t);
            save(player, d);
            return;
        }
        ACTIVE.put(player.getUUID(), new Active(god, now + TYPING_GRACE_TICKS + s.waitTicks));
        speak(player, god, true, line(s.greeting, player), line(s.question, player));
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onChat(ServerChatEvent event) {
        var player = event.getPlayer();
        var active = ACTIVE.get(player.getUUID());
        if (active == null) return;
        event.setCanceled(true);                                      // a private exchange: nobody else sees the answer
        player.sendSystemMessage(Component.translatable("eidolonunchained.calling.you", event.getRawText()).withStyle(s -> s.withColor(0xAAAAAA)));
        var s = settings(active.god);
        var answer = normalize(event.getRawText());
        if (matches(answer, s.yes)) {
            ACTIVE.remove(player.getUUID());
            var why = Patrons.pledge(player, active.god);
            if (why != null) { speak(player, active.god, false, why); return; }
            speak(player, active.god, false, line(s.accept, player));
        } else if (matches(answer, s.no)) {
            ACTIVE.remove(player.getUUID());
            cooldown(player, active.god, s);
            speak(player, active.god, false, line(s.decline, player));
        } else {
            player.displayClientMessage(Component.translatable("eidolonunchained.calling.unclear"), false);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ACTIVE.isEmpty() || event.getServer().getTickCount() % 20 != 0) return;
        long now = event.getServer().overworld().getGameTime();
        for (var it = ACTIVE.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            if (e.getValue().expiresAt > now) continue;
            it.remove();
            var player = event.getServer().getPlayerList().getPlayer(e.getKey());
            if (player == null) continue;
            var s = settings(e.getValue().god);
            cooldown(player, e.getValue().god, s);
            speak(player, e.getValue().god, false, line(s.silence, player));
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ACTIVE.remove(event.getEntity().getUUID());                    // leaving mid-conversation: it simply ends
    }

    /** Forgets every calling for the player: who called, cooldowns, a pending answer ({@code /eu discoveries reset}). */
    public static void forget(ServerPlayer player) {
        ACTIVE.remove(player.getUUID());
        save(player, new CompoundTag());
    }

    // ---- internals ----

    private static void skip(ServerPlayer player, ResourceLocation god, String why) {
        EidolonUnchained.LOGGER.info("Calling of {} by {} skipped: {}", player.getName().getString(), god, why);
    }

    private static void speak(ServerPlayer player, ResourceLocation god, boolean awaitAnswer, Component... lines) {
        var deity = Deities.find(god);
        int color = deity == null ? 0xFFFFFF
                : ((int) (deity.getRed() * 255) << 16) | ((int) (deity.getGreen() * 255) << 8) | (int) (deity.getBlue() * 255);
        EUNetwork.sendTo(player, new CallingPacket(Patrons.deityName(god), color, settings(god).voice(), List.of(lines), awaitAnswer));
    }

    private static Component line(String keyOrText, ServerPlayer player, Object... more) {
        var args = new Object[more.length + 1];
        args[0] = player.getDisplayName();
        System.arraycopy(more, 0, args, 1, more.length);
        return Component.translatableWithFallback(keyOrText, keyOrText, args);
    }

    private static String normalize(String text) {
        return text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9' ]", " ").replaceAll("\\s+", " ").trim();
    }

    /** An answer matches a phrase when it is the phrase, or starts with it as whole words ("yes, my lord"). */
    private static boolean matches(String answer, List<String> phrases) {
        for (var p : phrases) {
            var n = normalize(p);
            if (!n.isEmpty() && (answer.equals(n) || answer.startsWith(n + " "))) return true;
        }
        return false;
    }

    private static void cooldown(ServerPlayer player, ResourceLocation god, Settings s) {
        var data = data(player);
        var next = data.getCompound("next");
        next.putLong(god.toString(), s.callAgainTicks < 0 ? -1 : player.level().getGameTime() + s.callAgainTicks);
        data.put("next", next);
        save(player, data);
    }

    private static void markCalled(ServerPlayer player, ResourceLocation god) {
        if (wasCalled(player, god)) return;
        var data = data(player);
        var list = data.getList("called", net.minecraft.nbt.Tag.TAG_STRING);
        list.add(net.minecraft.nbt.StringTag.valueOf(god.toString()));
        data.put("called", list);
        save(player, data);
    }

    private static CompoundTag data(Player player) {
        return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getCompound(ROOT).getCompound(NBT);
    }

    private static void save(Player player, CompoundTag data) {
        data.putInt("v", 1);
        var persisted = player.getPersistentData();
        var tag = persisted.getCompound(Player.PERSISTED_NBT_TAG);
        var root = tag.getCompound(ROOT);
        root.put(NBT, data);
        tag.put(ROOT, root);
        persisted.put(Player.PERSISTED_NBT_TAG, tag);
    }
}
