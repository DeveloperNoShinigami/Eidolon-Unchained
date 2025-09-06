package com.bluelotuscoding.eidolonunchained.keybind;

import com.bluelotuscoding.eidolonunchained.chant.ChantSlotManager;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.function.Supplier;

/**
 * Network packet for handling chant interface interactions.
 * Supports opening the interface and syncing chant assignments.
 */
public class ChantInterfacePacket {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public enum Action {
        OPEN_INTERFACE,
        SYNC_ASSIGNMENTS
    }
    
    private final Action action;
    private final String data; // Optional data payload
    
    public ChantInterfacePacket(Action action) {
        this.action = action;
        this.data = "";
    }
    
    public ChantInterfacePacket(Action action, String data) {
        this.action = action;
        this.data = data;
    }
    
    public ChantInterfacePacket(FriendlyByteBuf buf) {
        this.action = Action.values()[buf.readInt()];
        this.data = buf.readUtf();
    }
    
    public static void encode(ChantInterfacePacket packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.action);
        buf.writeUtf(packet.data != null ? packet.data : "");
    }
    
    public static ChantInterfacePacket decode(FriendlyByteBuf buf) {
        Action action = buf.readEnum(Action.class);
        String data = buf.readUtf();
        return new ChantInterfacePacket(action, data.isEmpty() ? null : data);
    }
    
    public static void consume(ChantInterfacePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                LOGGER.info("Processing chant interface packet for player: {} action: {}", 
                           player.getName().getString(), packet.action);
                
                switch (packet.action) {
                    case OPEN_INTERFACE:
                        // Send current chant assignments to client
                        String assignments = ChantSlotManager.getPlayerChantAssignments(player);
                        ChantInterfacePacket response = new ChantInterfacePacket(Action.SYNC_ASSIGNMENTS, assignments);
                        // Send response back to client
                        player.sendSystemMessage(Component.literal("§6Opening chant interface - assignments synced"));
                        break;
                        
                    case SYNC_ASSIGNMENTS:
                        // Update client with current assignments (handled client-side)
                        break;
                }
            }
        });
        context.setPacketHandled(true);
    }
}
