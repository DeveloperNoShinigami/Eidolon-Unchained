package com.bluelotuscoding.eidolonunchained.client.renderer;

import com.bluelotuscoding.eidolonunchained.client.gui.ChantOverlay;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import elucent.eidolon.api.spells.Sign;
import elucent.eidolon.client.ClientRegistry;
import elucent.eidolon.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;

import java.util.List;

/**
 * Render layer that displays floating signs around the player during chanting.
 * Integrates with ChantOverlay to show signs in a circular formation around the caster.
 */
public class ChantingSignsLayer<T extends Player> extends RenderLayer<T, PlayerModel<T>> {
    
    public ChantingSignsLayer(RenderLayerParent<T, PlayerModel<T>> rendererIn) {
        super(rendererIn);
    }
    
    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T player, 
                      float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, 
                      float netHeadYaw, float headPitch) {
        
        // Only render if this is the local player and chant is active
        if (!ChantOverlay.isActive() || !player.equals(Minecraft.getInstance().player)) {
            return;
        }
        
        List<Sign> activeChant = ChantOverlay.getActiveChant();
        if (activeChant.isEmpty()) return;
        
        renderFloatingSignsAroundPlayer(poseStack, buffer, packedLight, player, activeChant, 
                                      partialTicks, ageInTicks);
    }
    
    /**
     * Render floating signs in a circular formation around the player
     */
    private void renderFloatingSignsAroundPlayer(PoseStack poseStack, MultiBufferSource buffer, 
                                               int packedLight, Player player, List<Sign> signs, 
                                               float partialTicks, float ageInTicks) {
        
        poseStack.pushPose();
        
        // Position relative to player center
        poseStack.translate(0.0, player.getBbHeight() * 0.75, 0.0);
        
        // Render each sign in circular formation
        for (int i = 0; i < signs.size(); i++) {
            Sign sign = signs.get(i);
            
            poseStack.pushPose();
            
            // Calculate position in circle
            float angle = (float) (2.0 * Math.PI * i / Math.max(1, signs.size()));
            float radius = 1.5f + (signs.size() * 0.1f); // Expand circle as more signs are added
            
            // Position sign around player
            float x = (float) Math.cos(angle + ageInTicks * 0.01f) * radius;
            float z = (float) Math.sin(angle + ageInTicks * 0.01f) * radius;
            float y = (float) Math.sin(ageInTicks * 0.02f + i) * 0.2f; // Gentle bobbing
            
            poseStack.translate(x, y, z);
            
            // Face the player
            poseStack.mulPose(Axis.YP.rotationDegrees(-angle * 180.0f / (float)Math.PI + 90.0f));
            
            // Scale the sign
            float scale = 0.5f;
            poseStack.scale(scale, scale, scale);
            
            // Calculate glow effect
            float glow = 0.8f + 0.2f * (float)Math.sin(ageInTicks * 0.1f + i);
            
            // Render the sign with glow effect
            RenderUtil.litQuad(poseStack, buffer, -8, -8, 16, 16,
                sign.getRed() * glow, sign.getGreen() * glow, sign.getBlue() * glow, 
                Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(sign.getSprite()));
            
            poseStack.popPose();
        }
        
        poseStack.popPose();
    }
}
