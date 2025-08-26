package com.bluelotuscoding.eidolonunchained.keybind;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.data.DatapackChantManager;
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
 * Handles keybind-based chant casting as an alternative to codex-based casting.
 * Provides quick access to deity communion spells.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ChantKeybinds {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static final KeyMapping SHADOW_COMMUNION = new KeyMapping(
        "key.eidolonunchained.shadow_communion",
        GLFW.GLFW_KEY_G,
        "key.categories.eidolonunchained"
    );
    
    public static final KeyMapping DIVINE_COMMUNION = new KeyMapping(
        "key.eidolonunchained.divine_communion",
        GLFW.GLFW_KEY_H,
        "key.categories.eidolonunchained"
    );
    
    public static final KeyMapping NATURES_COMMUNION = new KeyMapping(
        "key.eidolonunchained.natures_communion",
        GLFW.GLFW_KEY_J,
        "key.categories.eidolonunchained"
    );
    
    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(SHADOW_COMMUNION);
        event.register(DIVINE_COMMUNION);
        event.register(NATURES_COMMUNION);
        LOGGER.info("Registered chant keybinds");
    }
    
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        
        if (event.getAction() == GLFW.GLFW_PRESS) {
            ResourceLocation chantId = null;
            
            if (SHADOW_COMMUNION.consumeClick()) {
                chantId = new ResourceLocation(EidolonUnchained.MODID, "shadow_communion");
            } else if (DIVINE_COMMUNION.consumeClick()) {
                chantId = new ResourceLocation(EidolonUnchained.MODID, "divine_communion");
            } else if (NATURES_COMMUNION.consumeClick()) {
                chantId = new ResourceLocation(EidolonUnchained.MODID, "natures_communion");
            }
            
            if (chantId != null) {
                LOGGER.info("Player pressed keybind for chant: {}", chantId);
                mc.player.sendSystemMessage(Component.literal("§6Attempting to cast: " + chantId.getPath().replace("_", " ")));
                
                // Send packet to server to execute chant
                ChantCastPacket packet = new ChantCastPacket(chantId);
                EidolonUnchainedNetworking.INSTANCE.sendToServer(packet);
            }
        }
    }
}
