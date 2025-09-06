package com.bluelotuscoding.eidolonunchained.keybind;

import com.bluelotuscoding.eidolonunchained.chant.DatapackChantManager;
import com.mojang.logging.LogUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.function.Supplier;

/**
 * Network packet for casting chants via keybinds.
 * Sent from client to server when a chant keybind is pressed.
 */
public class ChantCastPacket {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private final ResourceLocation chantId;
    
    public ChantCastPacket(ResourceLocation chantId) {
        this.chantId = chantId;
    }
    
    public ChantCastPacket(FriendlyByteBuf buf) {
        this.chantId = buf.readResourceLocation();
    }
    
    public static void encode(ChantCastPacket packet, FriendlyByteBuf buf) {
        buf.writeResourceLocation(packet.chantId);
    }
    
    public static ChantCastPacket decode(FriendlyByteBuf buf) {
        return new ChantCastPacket(buf.readResourceLocation());
    }
    
    public static void consume(ChantCastPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                LOGGER.info("Processing chant cast packet for player: {} chant: {}", 
                           player.getName().getString(), packet.chantId);
                
                boolean success = DatapackChantManager.executeChant(packet.chantId, player);
                if (success) {
                    player.sendSystemMessage(Component.literal("§6✨ Chant cast successfully via keybind!"));
                } else {
                    player.sendSystemMessage(Component.literal("§cFailed to cast chant: " + packet.chantId.getPath()));
                }
            }
        });
        context.setPacketHandled(true);
    }
}
