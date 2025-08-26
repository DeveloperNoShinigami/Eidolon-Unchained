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
    
    // Use Eidolon's actual codex background texture for authentic red ribbon UI
    private static final ResourceLocation CHANT_TEXTURE = new ResourceLocation("eidolon", "textures/gui/codex_bg.png");
    
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
        
        // Check if overlay should timeout (extended to 10 seconds for better visibility)
        long timeSinceLastSign = System.currentTimeMillis() - lastSignAddTime;
        if (timeSinceLastSign > 10000) { // 10 seconds timeout
            isActive = false;
            activeChant.clear();
            floatingSignsData.clear();
            return;
        }
        
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
        RenderSystem.setShaderTexture(0, CHANT_TEXTURE);
        
        // Render chant ribbon using Eidolon's exact UV coordinates
        int bgx = baseX;
        
        // Left cap (256, 208, 16, 32)
        guiGraphics.blit(CHANT_TEXTURE, bgx, baseY, 256, 208, 16, 32, 512, 512);
        bgx += 16;
        
        // Middle segments for each sign (272, 208, 24, 32)
        for (int i = 0; i < activeChant.size(); i++) {
            guiGraphics.blit(CHANT_TEXTURE, bgx, baseY, 272, 208, 24, 32, 512, 512);
            // Sign overlay (312, 208, 24, 24)
            guiGraphics.blit(CHANT_TEXTURE, bgx, baseY, 312, 208, 24, 24, 512, 512);
            bgx += 24;
        }
        
        // Right cap (296, 208, 16, 32)
        guiGraphics.blit(CHANT_TEXTURE, bgx, baseY, 296, 208, 16, 32, 512, 512);
        
        // Render signs with glow effects
        RenderSystem.enableBlend();
        RenderSystem.setShader(ClientRegistry::getGlowingSpriteShader);
        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
        
        bgx = baseX + 16;
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        
        // Approximate client ticks for animation
        int clientTicks = (int)(System.currentTimeMillis() / 50);
        
        // Render base signs
        for (int i = 0; i < activeChant.size(); i++) {
            Sign sign = activeChant.get(i);
            RenderUtil.litQuad(guiGraphics.pose(), bufferSource, bgx + 4, baseY + 4, 16, 16,
                sign.getRed(), sign.getGreen(), sign.getBlue(), 
                Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(sign.getSprite()));
            bufferSource.endBatch();
            bgx += 24;
        }
        
        // Render flickering glow overlay
        bgx = baseX + 16;
        RenderSystem.blendFunc(770, 1); // SRC_ALPHA, ONE for additive blending
        for (int i = 0; i < activeChant.size(); i++) {
            float flicker = 0.75f + 0.25f * (float)Math.sin(Math.toRadians(12 * clientTicks - 360.0f * i / activeChant.size()));
            Sign sign = activeChant.get(i);
            RenderUtil.litQuad(guiGraphics.pose(), bufferSource, bgx + 4, baseY + 4, 16, 16,
                sign.getRed() * flicker, sign.getGreen() * flicker, sign.getBlue() * flicker, 
                Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(sign.getSprite()));
            bufferSource.endBatch();
            bgx += 24;
        }
        
        // Reset blend function
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
    
    /**
     * Render floating signs in 3D world space (called from ClientEvents)
     */
    public static void renderFloatingSignsIn3D(GuiGraphics guiGraphics, float partialTicks) {
        if (!isActive || floatingSignsData.isEmpty()) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        
        // Get camera position for proper positioning
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
        Vec3 playerPos = mc.player.position();
        
        // Render each floating sign
        RenderSystem.enableBlend();
        RenderSystem.setShader(ClientRegistry::getGlowingSpriteShader);
        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
        
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        
        for (int i = 0; i < floatingSignsData.size(); i++) {
            FloatingSignData signData = floatingSignsData.get(i);
            
            // Calculate animation progress (signs float up and orbit)
            long currentTime = System.currentTimeMillis();
            float ageInSeconds = (currentTime - signData.spawnTime) / 1000.0f;
            
            // Remove old signs after 8 seconds
            if (ageInSeconds > 8.0f) {
                floatingSignsData.remove(i);
                i--;
                continue;
            }
            
            // Calculate position relative to player
            float orbitRadius = signData.radius + ageInSeconds * 0.2f; // Slowly expand
            float height = 1.5f + ageInSeconds * 0.3f; // Float upward
            float angle = signData.angle + ageInSeconds * 30.0f; // Slow rotation
            
            float x = (float)(playerPos.x + Math.cos(Math.toRadians(angle)) * orbitRadius);
            float y = (float)(playerPos.y + height);
            float z = (float)(playerPos.z + Math.sin(Math.toRadians(angle)) * orbitRadius);
            
            // Calculate screen position from world position
            // This is a simplified approach - for proper 3D rendering we'd need WorldRenderEvent
            float alpha = Math.max(0, 1.0f - ageInSeconds / 8.0f); // Fade out over time
            float glow = 0.8f + 0.2f * (float)Math.sin(ageInSeconds * 4.0f); // Pulsing effect
            
            // For now, just spawn particles at the calculated position
            if (mc.level.random.nextFloat() < 0.3f) {
                mc.level.addParticle(ParticleTypes.ENCHANT, x, y, z, 0, 0.05, 0);
                
                // Add sign-colored glow particles
                Sign sign = signData.sign;
                if (mc.level.random.nextFloat() < 0.1f) {
                    // Convert sign colors to particle velocity for colored effect
                    double velX = (sign.getRed() - 0.5) * 0.1;
                    double velY = 0.1;
                    double velZ = (sign.getBlue() - 0.5) * 0.1;
                    mc.level.addParticle(ParticleTypes.GLOW, x, y, z, velX, velY, velZ);
                }
            }
        }
        
        bufferSource.endBatch();
        RenderSystem.defaultBlendFunc();
    }
}
