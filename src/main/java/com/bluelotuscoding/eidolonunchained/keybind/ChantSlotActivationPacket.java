package com.bluelotuscoding.eidolonunchained.keybind;

import com.bluelotuscoding.eidolonunchained.chant.ChantSlotManager;
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
    
    public ChantSlotActivationPacket(int slotNumber) {
        this.slotNumber = slotNumber;
    }
    
    public ChantSlotActivationPacket(FriendlyByteBuf buf) {
        this.slotNumber = buf.readInt();
    }
    
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.slotNumber);
    }
    
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                LOGGER.info("Processing chant slot activation for player: {} slot: {}", 
                           player.getName().getString(), slotNumber);
                
                // Get the chant assigned to this slot
                boolean success = ChantSlotManager.activateChantSlot(player, slotNumber);
                if (!success) {
                    player.sendSystemMessage(Component.literal("§cNo chant assigned to slot " + slotNumber + ". Use /chant assign <slot> <chant_id> to configure."));
                }
            }
        });
        
        return true;
    }
}
