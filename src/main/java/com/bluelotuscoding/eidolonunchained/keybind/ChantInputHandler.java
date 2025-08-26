package com.bluelotuscoding.eidolonunchained.keybind;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.config.ChantCastingConfig;
import com.bluelotuscoding.eidolonunchained.network.EidolonUnchainedNetworking;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

/**
 * Handles input events for chant keybinds
 * Separate from registration to use FORGE bus for runtime events
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ChantInputHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        
        if (event.getAction() == GLFW.GLFW_PRESS) {
            int chantSlot = -1;
            
            if (ChantKeybinds.CHANT_SLOT_1.consumeClick()) {
                chantSlot = 1;
            } else if (ChantKeybinds.CHANT_SLOT_2.consumeClick()) {
                chantSlot = 2;
            } else if (ChantKeybinds.CHANT_SLOT_3.consumeClick()) {
                chantSlot = 3;
            } else if (ChantKeybinds.CHANT_SLOT_4.consumeClick()) {
                chantSlot = 4;
            } else if (ChantKeybinds.OPEN_CHANT_INTERFACE.consumeClick()) {
                // Open chant management interface
                LOGGER.info("Opening chant interface");
                mc.player.sendSystemMessage(Component.literal("§6Opening Chant Interface..."));
                
                ChantInterfacePacket packet = new ChantInterfacePacket(ChantInterfacePacket.Action.OPEN_INTERFACE);
                EidolonUnchainedNetworking.INSTANCE.sendToServer(packet);
                return;
            }
            
            if (chantSlot != -1) {
                handleChantSlotActivation(chantSlot);
            }
        }
    }
    
    private static void handleChantSlotActivation(int chantSlot) {
        Minecraft mc = Minecraft.getInstance();
        ChantCastingConfig.CastingMode mode = ChantCastingConfig.getCurrentMode();
        
        LOGGER.info("Player pressed chant slot: {} (mode: {})", chantSlot, mode);
        
        String modeMessage = switch (mode) {
            case FULL_CHANT -> "§6Casting full chant from slot " + chantSlot + "...";
            case INDIVIDUAL_SIGNS -> "§6Casting next sign from slot " + chantSlot + "...";
            case HYBRID -> "§6Activating chant slot " + chantSlot + " (hybrid mode)...";
        };
        
        if (ChantCastingConfig.isFeedbackEnabled()) {
            mc.player.sendSystemMessage(Component.literal(modeMessage));
        }
        
        // Send packet to server with casting mode information
        ChantSlotActivationPacket packet = new ChantSlotActivationPacket(chantSlot, mode.name());
        EidolonUnchainedNetworking.INSTANCE.sendToServer(packet);
    }
}
