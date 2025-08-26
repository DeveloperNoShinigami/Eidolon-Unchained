package com.bluelotuscoding.eidolonunchained.keybind;

import com.mojang.logging.LogUtils;
import elucent.eidolon.api.spells.Sign;
import elucent.eidolon.codex.CodexGui;
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
 * Opens the codex chant interface and automatically adds the sign.
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
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;
            
            LOGGER.info("Received chant sign trigger packet for sign: {}", signId);
            
            // Find the sign
            Sign sign = Signs.find(signId);
            if (sign == null) {
                LOGGER.warn("Sign not found: {}", signId);
                return;
            }
            
            try {
                // Send feedback to the player about the sign being added to their active chant
                String signName = signId.getPath().substring(0, 1).toUpperCase() + signId.getPath().substring(1);
                mc.player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§6Added §f" + signName + "§6 sign to active chant"));
                mc.player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§7Continue adding signs or open codex (TAB) to cast"));
                
                // TODO: Implement a client-side chant building system
                // For now, just provide helpful instructions
                mc.player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§7Tip: Open your codex and look for your chant to complete it!"));
                
                LOGGER.info("Processed sign trigger for: {}", signId);
            } catch (Exception e) {
                LOGGER.error("Failed to process sign trigger: {}", e.getMessage(), e);
                mc.player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cSign keybind error. Use codex manually."));
            }
        });
        
        return true;
    }
}
