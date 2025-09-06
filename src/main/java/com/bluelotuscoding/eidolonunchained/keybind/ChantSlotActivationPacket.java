package com.bluelotuscoding.eidolonunchained.keybind;

import com.bluelotuscoding.eidolonunchained.chant.SlotAssignmentManager;
import com.mojang.logging.LogUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.function.Supplier;

/**
 * Network packet for activating a specific chant slot.
 * Handles the flexible chant casting system where players can assign any chant to any slot.
 */
public class ChantSlotActivationPacket {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private final int slotNumber;
    private final String castingMode;
    
    public ChantSlotActivationPacket(int slotNumber) {
        this.slotNumber = slotNumber;
        this.castingMode = "FULL_CHANT"; // Default for backward compatibility
    }
    
    public ChantSlotActivationPacket(int slotNumber, String castingMode) {
        this.slotNumber = slotNumber;
        this.castingMode = castingMode;
    }
    
    public ChantSlotActivationPacket(FriendlyByteBuf buf) {
        this.slotNumber = buf.readInt();
        this.castingMode = buf.readUtf();
    }
    
    // Encode method for writing to buffer
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(this.slotNumber);
        buf.writeUtf(this.castingMode);
    }
    
    // Handle method with correct signature for Forge 1.20.1
    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Handle the packet on the main thread
            ServerPlayer player = context.getSender();
            if (player != null) {
                handleSlotActivation(player, this.slotNumber, this.castingMode);
            }
        });
        context.setPacketHandled(true);
    }
    
    private void handleSlotActivation(ServerPlayer player, int slotNumber, String castingMode) {
        LOGGER.info("Processing slot activation for player: {} slot: {} mode: {}", 
                   player.getName().getString(), slotNumber, castingMode);
        
        // Get the assignment for this slot and activate with the specified mode
        boolean success = SlotAssignmentManager.activateSlot(player, slotNumber, castingMode);
        if (!success) {
            player.sendSystemMessage(Component.literal("§cNo assignment in slot " + slotNumber + ". Use /chant assign-sign <slot> <sign> or /chant assign-chant <slot> <chant> to configure."));
        }
    }
    
    public int getSlotNumber() {
        return slotNumber;
    }
    
    public String getCastingMode() {
        return castingMode;
    }
}
