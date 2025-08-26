package com.bluelotuscoding.eidolonunchained.client.gui;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import elucent.eidolon.api.spells.Sign;
import elucent.eidolon.api.spells.SignSequence;
import elucent.eidolon.client.ClientRegistry;
import elucent.eidolon.common.entity.ChantCasterEntity;
import elucent.eidolon.network.AttemptCastPacket;
import elucent.eidolon.network.Networking;
import elucent.eidolon.registries.EidolonSounds;
import elucent.eidolon.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * Independent chant overlay system that integrates with Eidolon's ChantCasterEntity
 * to show the actual spell casting animations with floating signs in the air.
 * Provides immersive casting with the authentic Eidolon visual effects.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ChantOverlay implements IGuiOverlay {
    
    private static final ResourceLocation CODEX_TEXTURE = new ResourceLocation("eidolon", "textures/gui/codex_gui.png");
    
    // Chant state
    private static final List<Sign> activeChant = new ArrayList<>();
    private static boolean isActive = false;
    private static long lastSignAddTime = 0;
    private static long autoCompleteStartTime = 0;
    private static boolean autoCompleteTriggered = false;
    private static ResourceLocation lastSignId = null; // Prevent duplicate additions
    
    // Custom floating sign animation state (replacing ChantCasterEntity)
    private static final List<FloatingSignData> floatingSignsData = new ArrayList<>();
    
    // Configuration - accessed dynamically to avoid early config loading issues
    
    // Custom floating sign data for our controlled animation
    private static class FloatingSignData {
        public final Sign sign;
        public final long spawnTime;
        public final float angle;
        public final float radius;
        
        public FloatingSignData(Sign sign, long spawnTime, float angle, float radius) {
            this.sign = sign;
            this.spawnTime = spawnTime;
            this.angle = angle;
            this.radius = radius;
        }
    }
    
    /**
     * Add a sign to the active chant with Eidolon's casting animation
     */
    public static void addSignToChant(Sign sign) {
        if (sign == null) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        
        // Prevent duplicate additions within 100ms
        long currentTime = System.currentTimeMillis();
        ResourceLocation signId = sign.getRegistryName();
        if (signId != null && signId.equals(lastSignId) && (currentTime - lastSignAddTime) < 100) {
            return; // Skip duplicate
        }
        
        // Activate the overlay if not already active
        if (!isActive) {
            isActive = true;
            activeChant.clear();
            floatingSignsData.clear();
        }
        
        // Add the sign
        activeChant.add(sign);
        lastSignAddTime = currentTime;
        lastSignId = signId;
        autoCompleteTriggered = false;
        
        // Add floating sign data for custom animation (immediate)
        float angle = (activeChant.size() - 1) * (360.0f / Math.max(1, activeChant.size()));
        floatingSignsData.add(new FloatingSignData(sign, currentTime, angle, 1.5f));
        
        // Play Eidolon's SELECT_RUNE sound (like selecting in codex)
        if (mc.player != null) {
            mc.player.playNotifySound(EidolonSounds.SELECT_RUNE.get(), SoundSource.NEUTRAL, 
                0.5f, mc.level.random.nextFloat() * 0.25f + 0.75f);
        }
        
        // Spawn additional particles for extra effect
        spawnSignParticles(sign);
        
        // Check if this completes a known chant
        checkForChantCompletion();
    }
    
    /**
     * Check if the current sequence matches any known chants
     */
    private static void checkForChantCompletion() {
        if (activeChant.isEmpty()) return;
        
        // Convert to SignSequence for spell matching
        SignSequence sequence = new SignSequence(activeChant);
        
        // Check against all registered spells
        // TODO: Implement spell matching logic
        // For now, auto-complete after any sequence of 3+ signs
        if (activeChant.size() >= 3) {
            triggerAutoComplete();
        }
    }
    
    /**
     * Trigger auto-completion after a delay
     */
    private static void triggerAutoComplete() {
        int autoCompleteDelayMs = EidolonUnchainedConfig.COMMON.chantAutoCompleteDelay.get() * 50; // Convert to milliseconds
        if (!autoCompleteTriggered && autoCompleteDelayMs > 0) {
            autoCompleteTriggered = true;
            autoCompleteStartTime = System.currentTimeMillis();
        }
    }
    
    /**
     * Clear the active chant and remove floating signs
     */
    public static void clearChant() {
        activeChant.clear();
        floatingSignsData.clear();
        isActive = false;
        autoCompleteTriggered = false;
        autoCompleteStartTime = 0;
        lastSignId = null; // Reset duplicate prevention
    }
    
    /**
     * Spawn particles for sign casting animation
     */
    private static void spawnSignParticles(Sign sign) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null) {
            // Spawn sparkle particles around the player
            for (int i = 0; i < 15; i++) {
                double offsetX = (mc.level.random.nextDouble() - 0.5) * 2.0;
                double offsetY = mc.level.random.nextDouble() * 2.0;
                double offsetZ = (mc.level.random.nextDouble() - 0.5) * 2.0;
                
                mc.level.addParticle(
                    ParticleTypes.ENCHANT,
                    mc.player.getX() + offsetX,
                    mc.player.getY() + 1.0 + offsetY,
                    mc.player.getZ() + offsetZ,
                    0, 0.1, 0
                );
            }
            
            // Add sign-colored particles
            for (int i = 0; i < 8; i++) {
                double offsetX = (mc.level.random.nextDouble() - 0.5) * 1.5;
                double offsetY = mc.level.random.nextDouble() * 1.5;
                double offsetZ = (mc.level.random.nextDouble() - 0.5) * 1.5;
                
                mc.level.addParticle(
                    ParticleTypes.GLOW,
                    mc.player.getX() + offsetX,
                    mc.player.getY() + 1.2 + offsetY,
                    mc.player.getZ() + offsetZ,
                    0, 0.05, 0
                );
            }
        }
    }
    
    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        if (!isActive || activeChant.isEmpty()) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        
        // Check for auto-completion
        int autoCompleteDelayMs = EidolonUnchainedConfig.COMMON.chantAutoCompleteDelay.get() * 50; // Convert to milliseconds
        if (autoCompleteTriggered && autoCompleteDelayMs > 0 && 
            (System.currentTimeMillis() - autoCompleteStartTime) >= autoCompleteDelayMs) {
            executeChant();
            return;
        }
        
        // Debug: Always render something to test if overlay is working
        Component debugText = Component.literal("§cCHANT OVERLAY ACTIVE - Signs: " + activeChant.size());
        guiGraphics.drawString(mc.font, debugText, 10, 10, 0xFFFFFF);
        
        // Render the red ribbon chant interface (like in codex)
        renderChantRibbon(guiGraphics, screenWidth, screenHeight, partialTick);
    }
    
    /**
     * Render the red ribbon chant interface (adapted from Eidolon's CodexGui.renderChant)
     */
    private void renderChantRibbon(GuiGraphics guiGraphics, int screenWidth, int screenHeight, float partialTicks) {
        // Calculate chant display position (center bottom of screen)
        int chantWidth = 32 + 24 * activeChant.size();
        int baseX = screenWidth / 2 - chantWidth / 2;
        int baseY = screenHeight - 80; // 80 pixels from bottom
        
        RenderSystem.enableBlend();
        RenderSystem.setShaderTexture(0, CODEX_TEXTURE);
        
        // Render background panels (red ribbon)
        int bgx = baseX;
        guiGraphics.blit(CODEX_TEXTURE, bgx, baseY, 256, 208, 16, 32, 512, 512);
        bgx += 16;
        
        for (int i = 0; i < activeChant.size(); i++) {
            guiGraphics.blit(CODEX_TEXTURE, bgx, baseY, 272, 208, 24, 32, 512, 512);
            guiGraphics.blit(CODEX_TEXTURE, bgx, baseY, 312, 208, 24, 24, 512, 512);
            bgx += 24;
        }
        
        guiGraphics.blit(CODEX_TEXTURE, bgx, baseY, 296, 208, 16, 32, 512, 512);
        
        // Render signs with glow effects
        RenderSystem.enableBlend();
        RenderSystem.setShader(ClientRegistry::getGlowingSpriteShader);
        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
        
        bgx = baseX + 16;
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        
        for (int i = 0; i < activeChant.size(); i++) {
            Sign sign = activeChant.get(i);
            
            // Calculate glow effect based on timing
            float glow = 1.0f;
            long timeSinceLastSign = System.currentTimeMillis() - lastSignAddTime;
            if (i == activeChant.size() - 1 && timeSinceLastSign < 500) {
                // Pulsing animation for newly added sign
                float animProgress = timeSinceLastSign / 500.0f;
                glow = 1.5f + 0.5f * (float)Math.sin(animProgress * Math.PI * 4);
            } else {
                // Normal flicker effect (from Eidolon's ClientEvents.getClientTicks())
                int clientTicks = (int)(System.currentTimeMillis() / 50); // Approximate client ticks
                float flicker = 0.75f + 0.25f * (float)Math.sin(Math.toRadians(12 * clientTicks - 360.0f * i / activeChant.size()));
                glow = flicker;
            }
            
            // Base rendering
            RenderUtil.litQuad(guiGraphics.pose(), bufferSource, bgx + 4, baseY + 4, 16, 16,
                sign.getRed(), sign.getGreen(), sign.getBlue(), 
                Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(sign.getSprite()));
            bufferSource.endBatch();
            
            // Render with glow
            RenderUtil.litQuad(guiGraphics.pose(), bufferSource, bgx + 4, baseY + 4, 16, 16,
                sign.getRed() * glow, sign.getGreen() * glow, sign.getBlue() * glow, 
                Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(sign.getSprite()));
            bufferSource.endBatch();
            
            bgx += 24;
        }
        
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        
        // Show simple text info above the ribbon
        Component chantInfo = Component.literal("§6✨ Chanting... " + activeChant.size() + " signs");
        int infoX = screenWidth / 2 - Minecraft.getInstance().font.width(chantInfo) / 2;
        int infoY = baseY - 15;
        guiGraphics.drawString(Minecraft.getInstance().font, chantInfo, infoX, infoY, 0xFFFFFF);
    }
    
    /**
     * Execute the completed chant using Eidolon's AttemptCastPacket
     */
    private void executeChant() {
        if (activeChant.isEmpty()) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        // Play completion sound (experience orb pickup as requested)
        if (mc.player != null) {
            mc.player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.NEUTRAL, 1.0f, 1.0f);
        }
        
        // Send AttemptCastPacket to server (this will handle the actual spell casting)
        AttemptCastPacket castPacket = new AttemptCastPacket(mc.player, new ArrayList<>(activeChant));
        Networking.INSTANCE.sendToServer(castPacket);
        
        // Clear the chant (this will also remove the floating signs)
        clearChant();
    }
    
    /**
     * Check if the chant overlay is currently active
     */
    public static boolean isActive() {
        return isActive;
    }
    
    /**
     * Get the current chant sequence
     */
    public static List<Sign> getActiveChant() {
        return new ArrayList<>(activeChant);
    }
}
