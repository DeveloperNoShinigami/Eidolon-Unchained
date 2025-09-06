package com.bluelotuscoding.eidolonunchained.keybind;

import com.bluelotuscoding.eidolonunchained.client.gui.ChantOverlay;
import com.mojang.logging.LogUtils;
import elucent.eidolon.api.spells.Sign;
import elucent.eidolon.registries.Signs;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.function.Supplier;

/**
 * Network packet to trigger the chant interface with a specific sign.
 * Sent from server to client when a sign keybind is pressed.
 * Opens the independent chant overlay and automatically adds the sign.
 */
public class ChantSignTriggerPacket {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private final ResourceLocation signId;
    
    public ChantSignTriggerPacket(ResourceLocation signId) {
        this.signId = signId;
    }
    
    // Constructor for decoding from network
    public ChantSignTriggerPacket(FriendlyByteBuf buffer) {
        this.signId = buffer.readResourceLocation();
    }
    
    // Encode method for sending over network
    public static void encode(ChantSignTriggerPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.signId);
    }
    
    // Decode method for receiving from network
    public static ChantSignTriggerPacket decode(FriendlyByteBuf buffer) {
        return new ChantSignTriggerPacket(buffer);
    }
    
    // CORRECT HANDLER METHOD - Fixed signature for Forge 1.20.1
    public static void handle(ChantSignTriggerPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Handle on client side for overlay
            if (context.getDirection().getReceptionSide().isClient()) {
                handleClientSide(packet);
            }
        });
        context.setPacketHandled(true);
    }
    
    @OnlyIn(Dist.CLIENT)
    private static void handleClientSide(ChantSignTriggerPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        
        LOGGER.debug("Received chant sign trigger packet for sign: {}", packet.signId);
        
        // Find the sign
        Sign sign = Signs.find(packet.signId);
        if (sign == null) {
            LOGGER.warn("Sign not found: {}", packet.signId);
            return;
        }
        
        try {
            // Add the sign to the chant overlay (new independent system)
            ChantOverlay.addSignToChant(sign);
            
            LOGGER.debug("Successfully added sign {} to chant overlay", packet.signId);
        } catch (Exception e) {
            LOGGER.error("Failed to process sign trigger: {}", e.getMessage(), e);
        }
    }
}
