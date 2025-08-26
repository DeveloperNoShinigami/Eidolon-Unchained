package com.bluelotuscoding.eidolonunchained.client.gui;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig;
import elucent.eidolon.api.spells.Sign;
import elucent.eidolon.api.spells.SignSequence;
import elucent.eidolon.common.entity.ChantCasterEntity;
import elucent.eidolon.network.AttemptCastPacket;
import elucent.eidolon.network.Networking;
import elucent.eidolon.registries.EidolonSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
    private static ChantCasterEntity activeEntity = null;
    
    // Configuration - accessed dynamically to avoid early config loading issues
    
    /**
     * Add a sign to the active chant with Eidolon's casting animation
     */
    public static void addSignToChant(Sign sign) {
        if (sign == null) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        
        // Activate the overlay if not already active
        if (!isActive) {
            isActive = true;
            activeChant.clear();
            activeEntity = null;
        }
        
        // Add the sign
        activeChant.add(sign);
        lastSignAddTime = System.currentTimeMillis();
        autoCompleteTriggered = false;
        
        // Create or update the ChantCasterEntity
        updateChantCasterEntity();
        
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
     * Clear the active chant and remove ChantCasterEntity
     */
    public static void clearChant() {
        activeChant.clear();
        isActive = false;
        autoCompleteTriggered = false;
        autoCompleteStartTime = 0;
        
        // Remove the ChantCasterEntity
        if (activeEntity != null && !activeEntity.isRemoved()) {
            activeEntity.discard();
        }
        activeEntity = null;
    }
    
    /**
     * Create or update the ChantCasterEntity with current sign sequence
     */
    private static void updateChantCasterEntity() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        
        // Remove old entity if it exists
        if (activeEntity != null && !activeEntity.isRemoved()) {
            activeEntity.discard();
        }
        
        // Create new ChantCasterEntity with current signs
        Vec3 placement = mc.player.position().add(0, mc.player.getBbHeight() * 2 / 3, 0).add(mc.player.getLookAngle().scale(0.5f));
        activeEntity = new ChantCasterEntity(mc.level, mc.player, new ArrayList<>(activeChant), mc.player.getLookAngle());
        activeEntity.setPos(placement.x, placement.y, placement.z);
        mc.level.addFreshEntity(activeEntity);
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
        
        // Render simple UI feedback (sign count and auto-complete progress)
        renderChantInfo(guiGraphics, screenWidth, screenHeight);
    }
    
    /**
     * Render minimal UI information about the active chant
     */
    private void renderChantInfo(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        
        // Show sign count
        Component chantInfo = Component.literal("§6✨ Chant Active: §f" + activeChant.size() + " signs");
        int infoX = screenWidth / 2 - mc.font.width(chantInfo) / 2;
        int infoY = screenHeight - 60;
        guiGraphics.drawString(mc.font, chantInfo, infoX, infoY, 0xFFFFFF);
        
        // Show auto-complete progress if triggered
        int autoCompleteDelayMs = EidolonUnchainedConfig.COMMON.chantAutoCompleteDelay.get() * 50; // Convert to milliseconds
        if (autoCompleteTriggered && autoCompleteDelayMs > 0) {
            long elapsed = System.currentTimeMillis() - autoCompleteStartTime;
            float progress = Math.min(1.0f, elapsed / (float)autoCompleteDelayMs);
            
            // Progress bar
            int barWidth = 200;
            int barX = screenWidth / 2 - barWidth / 2;
            int barY = screenHeight - 40;
            
            // Background
            guiGraphics.fill(barX, barY, barX + barWidth, barY + 4, 0x80000000);
            
            // Fill
            int fillWidth = (int)(barWidth * progress);
            guiGraphics.fill(barX, barY, barX + fillWidth, barY + 4, 0xFF00FF00);
            
            // Progress text
            Component progressText = Component.literal("§eCasting... " + (int)(progress * 100) + "%");
            int textX = screenWidth / 2 - mc.font.width(progressText) / 2;
            guiGraphics.drawString(mc.font, progressText, textX, barY - 12, 0xFFFFFF);
        }
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
        
        // Provide feedback
        mc.player.sendSystemMessage(
            Component.literal("§6✨ Chant completed: " + activeChant.size() + " signs cast!")
        );
        
        // Clear the chant (this will also remove the ChantCasterEntity)
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
