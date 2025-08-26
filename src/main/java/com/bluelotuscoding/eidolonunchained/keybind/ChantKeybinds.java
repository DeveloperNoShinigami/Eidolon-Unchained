package com.bluelotuscoding.eidolonunchained.keybind;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.chant.DatapackChantManager;
import com.bluelotuscoding.eidolonunchained.network.EidolonUnchainedNetworking;
import com.mojang.logging.LogUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

/**
 * Flexible chant casting system supporting any datapack-defined chants.
 * Provides 4 configurable chant slots that can be assigned to any available chants.
 * Opens an in-game chant casting interface when activated.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ChantKeybinds {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // 4 configurable chant slots
    public static final KeyMapping CHANT_SLOT_1 = new KeyMapping(
        "key.eidolonunchained.chant_slot_1",
        GLFW.GLFW_KEY_G,
        "key.categories.eidolonunchained"
    );
    
    public static final KeyMapping CHANT_SLOT_2 = new KeyMapping(
        "key.eidolonunchained.chant_slot_2",
        GLFW.GLFW_KEY_H,
        "key.categories.eidolonunchained"
    );
    
    public static final KeyMapping CHANT_SLOT_3 = new KeyMapping(
        "key.eidolonunchained.chant_slot_3",
        GLFW.GLFW_KEY_J,
        "key.categories.eidolonunchained"
    );
    
    public static final KeyMapping CHANT_SLOT_4 = new KeyMapping(
        "key.eidolonunchained.chant_slot_4",
        GLFW.GLFW_KEY_K,
        "key.categories.eidolonunchained"
    );
    
    // Open chant casting interface
    public static final KeyMapping OPEN_CHANT_INTERFACE = new KeyMapping(
        "key.eidolonunchained.open_chant_interface",
        GLFW.GLFW_KEY_C,
        "key.categories.eidolonunchained"
    );
    
    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(CHANT_SLOT_1);
        event.register(CHANT_SLOT_2);
        event.register(CHANT_SLOT_3);
        event.register(CHANT_SLOT_4);
        event.register(OPEN_CHANT_INTERFACE);
        LOGGER.info("✅ Registered flexible chant keybinds (4 slots + interface)");
    }
    
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        
        if (event.getAction() == GLFW.GLFW_PRESS) {
            int chantSlot = -1;
            
            if (CHANT_SLOT_1.consumeClick()) {
                chantSlot = 1;
            } else if (CHANT_SLOT_2.consumeClick()) {
                chantSlot = 2;
            } else if (CHANT_SLOT_3.consumeClick()) {
                chantSlot = 3;
            } else if (CHANT_SLOT_4.consumeClick()) {
                chantSlot = 4;
            } else if (OPEN_CHANT_INTERFACE.consumeClick()) {
                // Open chant management interface
                LOGGER.info("Opening chant interface");
                mc.player.sendSystemMessage(Component.literal("§6Opening Chant Interface..."));
                
                ChantInterfacePacket packet = new ChantInterfacePacket(ChantInterfacePacket.Action.OPEN_INTERFACE);
                EidolonUnchainedNetworking.INSTANCE.sendToServer(packet);
                return;
            }
            
            if (chantSlot != -1) {
                LOGGER.info("Player pressed chant slot: {}", chantSlot);
                mc.player.sendSystemMessage(Component.literal("§6Activating chant slot " + chantSlot + "..."));
                
                // Send packet to server to handle chant slot activation
                ChantSlotActivationPacket packet = new ChantSlotActivationPacket(chantSlot);
                EidolonUnchainedNetworking.INSTANCE.sendToServer(packet);
            }
        }
    }
}
