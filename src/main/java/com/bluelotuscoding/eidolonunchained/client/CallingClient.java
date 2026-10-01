package com.bluelotuscoding.eidolonunchained.client;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.network.CallingPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Types a god's lines out on the action bar, one letter per tick with the god's voice every few letters, holds each
 * finished line a moment, then the next. While the god waits for an answer the last line stays up with a hint.
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MOD_ID, value = Dist.CLIENT)
public final class CallingClient {
    private static final int HOLD_TICKS = 50;
    private static final int SOUND_EVERY = 3;

    private static Component speaker = Component.empty();
    private static int color = 0xFFFFFF;
    private static net.minecraft.sounds.SoundEvent voice;             // set by each packet (never at class load: sounds register later)
    private static final Deque<String> queue = new ArrayDeque<>();
    private static String line = null;
    private static int shown, hold;
    private static boolean awaitAnswer;

    private CallingClient() {
    }

    public static void accept(CallingPacket p) {
        queue.clear();
        speaker = p.speaker();
        color = p.color();
        voice = net.minecraft.sounds.SoundEvent.createVariableRangeEvent(p.voice());   // any sound id, registered or only in a sounds.json
        for (var l : p.lines()) queue.add(l.getString());       // translated here, on the client
        awaitAnswer = p.awaitAnswer();
        line = null;
        if (queue.isEmpty() && !awaitAnswer) Minecraft.getInstance().gui.setOverlayMessage(Component.empty(), false);
        next();
    }

    private static void next() {
        line = queue.poll();
        shown = 0;
        hold = 0;
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || line == null) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null) { line = null; queue.clear(); return; }
        if (shown < line.length()) {
            shown++;
            if (shown % SOUND_EVERY == 1 && !Character.isWhitespace(line.charAt(shown - 1)))
                mc.player.level().playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(), voice,
                        SoundSource.PLAYERS, 0.25f, 0.9f + mc.player.getRandom().nextFloat() * 0.3f, false);
            show(line.substring(0, shown), false);
            return;
        }
        boolean last = queue.isEmpty();
        if (last && awaitAnswer) {
            if (hold++ % 40 == 0) show(line, true);                 // keep the question up (the action bar fades)
            return;
        }
        if (hold++ >= HOLD_TICKS) {
            if (last) line = null; else next();
        }
    }

    private static void show(String text, boolean hint) {
        var msg = Component.empty()
                .append(speaker.copy().withStyle(s -> s.withColor(TextColor.fromRgb(color)).withBold(true)))
                .append(Component.literal(": ").withStyle(s -> s.withColor(TextColor.fromRgb(color))))
                .append(Component.literal(text).withStyle(s -> s.withColor(TextColor.fromRgb(color))));
        if (hint) msg.append(Component.literal(" ")).append(Component.translatable("eidolonunchained.calling.hint").withStyle(s -> s.withColor(0xAAAAAA).withItalic(true)));
        Minecraft.getInstance().gui.setOverlayMessage(msg, false);
    }
}
