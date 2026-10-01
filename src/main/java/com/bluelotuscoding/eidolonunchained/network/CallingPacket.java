package com.bluelotuscoding.eidolonunchained.network;

import com.bluelotuscoding.eidolonunchained.client.CallingClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Server -> one client: a god speaks (callings, step 4). The client types the lines out on the action bar letter by
 * letter (the server has no client language files, so it can't type translated text itself). {@code awaitAnswer} keeps
 * the last line and the "(answer in chat)" hint up until the next packet; an empty packet ends the conversation.
 */
public record CallingPacket(Component speaker, int color, net.minecraft.resources.ResourceLocation voice, List<Component> lines, boolean awaitAnswer) {
    public static void encode(CallingPacket p, FriendlyByteBuf buf) {
        buf.writeComponent(p.speaker);
        buf.writeInt(p.color);
        buf.writeResourceLocation(p.voice);
        buf.writeCollection(p.lines, FriendlyByteBuf::writeComponent);
        buf.writeBoolean(p.awaitAnswer);
    }

    public static CallingPacket decode(FriendlyByteBuf buf) {
        var speaker = buf.readComponent();
        int color = buf.readInt();
        var voice = buf.readResourceLocation();
        var lines = buf.readCollection(ArrayList::new, FriendlyByteBuf::readComponent);
        return new CallingPacket(speaker, color, voice, lines, buf.readBoolean());
    }

    public static void handle(CallingPacket p, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> CallingClient.accept(p));
        ctx.get().setPacketHandled(true);
    }
}
