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
    
    // Constructor for decoding from buffer
    public ChantCastPacket(FriendlyByteBuf buf) {
        this.chantId = buf.readResourceLocation();
    }
    
    // Encode method for writing to buffer
    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(this.chantId);
    }
    
    // Handle method with correct signature for Forge 1.20.1
    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Handle the packet on the main thread
            ServerPlayer player = context.getSender();
            if (player != null) {
                handleChantCast(player, this.chantId);
            }
        });
        context.setPacketHandled(true);
    }
    
    private void handleChantCast(ServerPlayer player, ResourceLocation chantId) {
        LOGGER.info("Processing chant cast packet for player: {} chant: {}", 
                   player.getName().getString(), chantId);
        
        boolean success = DatapackChantManager.executeChant(chantId, player);
        if (success) {
            player.sendSystemMessage(Component.literal("§6✨ Chant cast successfully via keybind!"));
        } else {
            player.sendSystemMessage(Component.literal("§cFailed to cast chant: " + chantId.getPath()));
        }
    }
    
    public ResourceLocation getChantId() {
        return chantId;
    }
}
