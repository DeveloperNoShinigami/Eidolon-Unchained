package com.bluelotuscoding.eidolonunchained.keybind;

import com.bluelotuscoding.eidolonunchained.client.gui.ChantOverlay;
import com.mojang.logging.LogUtils;
import elucent.eidolon.api.spells.Sign;
import elucent.eidolon.registries.Signs;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
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
    
    public ChantSignTriggerPacket(FriendlyByteBuf buf) {
        this.signId = buf.readResourceLocation();
    }
    
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeResourceLocation(this.signId);
    }
    
    @OnlyIn(Dist.CLIENT)
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            System.out.println("DEBUG: ChantSignTriggerPacket.handle() called on client");
            
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;
            
            LOGGER.debug("Received chant sign trigger packet for sign: {}", signId);
            
            // Find the sign
            Sign sign = Signs.find(signId);
            if (sign == null) {
                LOGGER.warn("Sign not found: {}", signId);
                System.out.println("DEBUG: Sign not found: " + signId);
                return;
            }
            
            System.out.println("DEBUG: Found sign, calling ChantOverlay.addSignToChant()");
            
            try {
                // Add the sign to the chant overlay (new independent system)
                ChantOverlay.addSignToChant(sign);
                
                LOGGER.debug("Successfully added sign {} to chant overlay", signId);
            } catch (Exception e) {
                LOGGER.error("Failed to process sign trigger: {}", e.getMessage(), e);
                System.out.println("DEBUG: Exception in addSignToChant: " + e.getMessage());
            }
        });
        
        return true;
    }
}
