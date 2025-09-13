package com.bluelotuscoding.eidolonunchained.chant;

import com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig;
import com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager;
import com.bluelotuscoding.eidolonunchained.deity.DatapackDeity;
import elucent.eidolon.api.spells.Sign;
import elucent.eidolon.common.spell.PrayerSpell;
import elucent.eidolon.api.deity.Deity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import com.mojang.logging.LogUtils;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import elucent.eidolon.common.tile.EffigyTileEntity;
import elucent.eidolon.api.ritual.Ritual;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;

import java.util.List;
import java.util.Comparator;

/**
 * A custom spell that represents a datapack-defined chant.
 * Extends PrayerSpell to integrate with Eidolon's existing chant/prayer system.
 * When cast, executes the custom effects defined in the chant configuration.
 */
public class DatapackChantSpell extends PrayerSpell {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private final DatapackChant chantData;
    
    public DatapackChantSpell(ResourceLocation name, DatapackChant chantData, Sign... signs) {
        // Create a deity with correct colors for the parent class
        super(name, createDeityWithColors(name, chantData), signs);
        this.chantData = chantData;
    }

    @Override
    public int getCost() {
        // Respect datapack-defined mana cost for gating
        return chantData != null ? Math.max(0, chantData.getManaCost()) : 0;
    }
    
    /**
     * Creates a deity with proper colors from datapack for the parent PrayerSpell class
     */
    private static Deity createDeityWithColors(ResourceLocation spellName, DatapackChant chantData) {
        // Get colors from linked deity or generate smart colors
        float[] colors;
        if (chantData.hasLinkedDeity()) {
            DatapackDeity deity = DatapackDeityManager.getDeity(chantData.getLinkedDeity());
            if (deity != null) {
                colors = new float[]{deity.getRed(), deity.getGreen(), deity.getBlue()};
            } else {
                colors = new float[]{0.8f, 0.4f, 1.0f}; // Default mystical purple
            }
        } else {
            colors = new float[]{1.0f, 0.6f, 0.2f}; // Default warm orange/gold
        }
        
        // Convert to 0-255 range for Deity constructor
        int red = (int)(colors[0] * 255);
        int green = (int)(colors[1] * 255);
        int blue = (int)(colors[2] * 255);
        
        return new Deity(new ResourceLocation(spellName.getNamespace(), "chant_" + spellName.getPath()), red, green, blue) {
            public String getDisplayName() {
                return "Chant: " + spellName.getPath();
            }
            
            @Override
            public void onReputationUnlock(Player player, ResourceLocation lock) {
                // No-op for chant spells
            }
            
            @Override
            public void onReputationLock(Player player, ResourceLocation lock) {
                // No-op for chant spells
            }
        };
    }
    
    @Override
    public boolean canCast(Level world, BlockPos pos, Player player) {
        // Check cooldown first
        if (!ChantCooldownManager.canCastChant(player, chantData)) {
            int remainingCooldown = ChantCooldownManager.getRemainingCooldown(player, chantData);
            player.sendSystemMessage(Component.translatable("eidolonunchained.ui.chant.cooldown", remainingCooldown));
            return false;
        }
        
        // 🔥 FIXED: Check effigy requirement using ChantCasterEntity position
        if (chantData.requiresEffigy()) {
            elucent.eidolon.common.tile.EffigyTileEntity effigy = getEffigyFromPlayer(world, player);
            if (effigy == null) {
                player.sendSystemMessage(Component.literal("§c⚠ This chant requires an Effigy nearby (within 4 blocks)."));
                player.sendSystemMessage(Component.literal("§7Build an Effigy to channel divine power for this ritual."));
                return false;
            }
            if (!effigy.ready()) {
                player.sendSystemMessage(Component.literal("§c⏰ The Effigy is cooling down. Wait for it to be ready."));
                player.sendSystemMessage(Component.literal("§7The divine channels need time to recover their energy."));
                return false;
            }
            // Success feedback
            player.sendSystemMessage(Component.literal("§a✓ Effigy detected and ready - divine power flows freely."));
        }
        
        // Check basic spell requirements (magic cost) - bypass PrayerSpell's effigy check
        if (getCost() > 0 && !player.isCreative()) {
            if (player.getCapability(elucent.eidolon.capability.ISoul.INSTANCE).isPresent()) {
                elucent.eidolon.capability.ISoul soul = player.getCapability(elucent.eidolon.capability.ISoul.INSTANCE).resolve().get();
                if (soul.getMagic() < getCost()) {
                    if (player instanceof ServerPlayer serverPlayer)
                        serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket(Component.translatable("eidolon.title.no_mana")));
                    return false;
                }
            }
        }
        
        return true;
    }
    
    @Override
    public void cast(Level world, BlockPos pos, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        
        // Check if chant system is enabled
        if (!EidolonUnchainedConfig.COMMON.enableChantSystem.get()) {
            serverPlayer.sendSystemMessage(Component.translatable("eidolonunchained.ui.chant.system_disabled"));
            return;
        }
        
        // Record the chant for AI context tracking
        try {
            if (chantData.hasLinkedDeity()) {
                com.bluelotuscoding.eidolonunchained.ai.PlayerContextTracker.recordChant(
                    serverPlayer, chantData.getId(), chantData.getLinkedDeity(), true);
                
                // Grant Facts integration for chant completion
                // TODO: Implement Facts integration when class is available
                // com.bluelotuscoding.eidolonunchained.integration.FactsIntegration.onChantPerformed(
                //     serverPlayer, chantData.getId());
            } else {
                com.bluelotuscoding.eidolonunchained.ai.PlayerContextTracker.recordChant(
                    serverPlayer, chantData.getId(), null, true);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to record chant for AI context: {}", e.getMessage());
        }
        
        // 🔥 Only trigger deity conversation if prayer_effect_type is specified
        // This allows chants to be linked to deities for lore/context without auto-triggering conversations
        if (chantData.hasLinkedDeity() && chantData.getPrayerEffectType() != null && !chantData.getPrayerEffectType().isEmpty()) {
            // Import the necessary classes for deity interaction
            try {
                var aiDeityManager = com.bluelotuscoding.eidolonunchained.ai.AIDeityManager.getInstance();
                var deityChat = com.bluelotuscoding.eidolonunchained.chat.DeityChat.class;
                
                // Execute chant effects first
                executeChantEffects(serverPlayer, world, pos);
                
                // 🔥 Store this chant for prayer type detection
                com.bluelotuscoding.eidolonunchained.integration.ai.EnhancedCommandExtractor
                    .setLastPerformedChant(serverPlayer, chantData);
                
                // Then trigger deity conversation using the prayer_effect_type
                java.lang.reflect.Method startConversation = deityChat.getDeclaredMethod("startConversation", 
                    ServerPlayer.class, net.minecraft.resources.ResourceLocation.class);
                startConversation.invoke(null, serverPlayer, chantData.getLinkedDeity());
                
                serverPlayer.sendSystemMessage(Component.translatable("eidolonunchained.ui.chant.deity_listening", chantData.getName()));
                
            } catch (Exception e) {
                LOGGER.error("Failed to trigger deity conversation for chant: {}", chantData.getId(), e);
                // Fall back to normal chant execution
                executeChantEffects(serverPlayer, world, pos);
                serverPlayer.sendSystemMessage(Component.translatable("eidolonunchained.ui.chant.success", chantData.getName()));
            }
        } else if (chantData.hasLinkedDeity()) {
            // Chant is linked to deity but no prayer_effect_type - just execute effects and show lore
            executeChantEffects(serverPlayer, world, pos);
            serverPlayer.sendSystemMessage(Component.translatable("eidolonunchained.ui.chant.deity_acknowledgment", chantData.getName()));
            LOGGER.info("Chant {} linked to deity {} but no prayer_effect_type - no conversation triggered", 
                chantData.getId(), chantData.getLinkedDeity());
        } else {
            // Execute normal chant effects
            executeChantEffects(serverPlayer, world, pos);
            serverPlayer.sendSystemMessage(Component.translatable("eidolonunchained.ui.chant.success", chantData.getName()));
        }
        
        // Consume mana based on datapack cost (if any)
        try {
            if (getCost() > 0 && !player.isCreative()) {
                var capOpt = player.getCapability(elucent.eidolon.capability.ISoul.INSTANCE);
                if (capOpt.isPresent()) {
                    elucent.eidolon.capability.ISoul soul = capOpt.resolve().get();
                    double current = 0;
                    try {
                        current = ((Number)soul.getClass().getMethod("getMagic").invoke(soul)).doubleValue();
                    } catch (Exception e0) {
                        try { current = ((Number)soul.getMagic()).doubleValue(); } catch (Exception ignored2) {}
                    }
                    double newVal = Math.max(0, current - (double)getCost());
                    try {
                        soul.getClass().getMethod("setMagic", double.class).invoke(soul, newVal);
                    } catch (Exception e1) {
                        try {
                            soul.getClass().getMethod("setMagic", int.class).invoke(soul, (int)newVal);
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to consume mana for chant {}: {}", chantData.getId(), e.getMessage());
        }

        // Set cooldown after successful cast
        ChantCooldownManager.setCooldown(serverPlayer, chantData);
        
        // 🔥 FIXED: If chant requires effigy, trigger Eidolon's effigy mechanics using correct position
        if (chantData.requiresEffigy()) {
            EffigyTileEntity effigy = getEffigyFromPlayer(world, player);
            if (effigy != null) {
                // Trigger effigy cooldown (like Eidolon's PrayerSpell does)
                effigy.pray();
                LOGGER.info("🔮 Triggered effigy cooldown for chant: {}", chantData.getId());
            }
        }
        
        // Effigy effects are now handled by EffigyEffectsManager
        
        LOGGER.info("Player {} successfully performed chant: {}", 
                   serverPlayer.getName().getString(), chantData.getId());
    }
    
    /**
     * 🔥 CRITICAL FIX: Calculate ChantCasterEntity position for effigy detection
     * This is the EXACT position calculation that Eidolon uses internally
     */
    protected static BlockPos getChantCasterPosition(Player player) {
        double rad = Math.toRadians(player.yHeadRot);
        net.minecraft.world.phys.Vec3 entityPos = player.getEyePosition().add(-Math.sin(rad) / 2, -0.75, Math.cos(rad) / 2);
        return new BlockPos((int)entityPos.x, (int)entityPos.y, (int)entityPos.z);
    }
    
    /**
     * Get nearby effigy using ChantCasterEntity position (FIXED - matches Eidolon's real method)
     */
    protected static EffigyTileEntity getEffigy(Level world, BlockPos pos) {
        List<EffigyTileEntity> effigies = Ritual.getTilesWithinAABB(EffigyTileEntity.class, world, new AABB(pos.offset(-4, -4, -4), pos.offset(5, 5, 5)));
        if (effigies.isEmpty()) return null;
        return effigies.stream().min(Comparator.comparingDouble((e) -> e.getBlockPos().distSqr(pos))).get();
    }
    
    /**
     * Get nearby effigy using the correct ChantCasterEntity position (RECOMMENDED)
     */
    protected static EffigyTileEntity getEffigyFromPlayer(Level world, Player player) {
        BlockPos chantCasterPos = getChantCasterPosition(player);
        return getEffigy(world, chantCasterPos);
    }

    
    /**
     * Executes the custom effects defined in the chant configuration
     */
    private void executeChantEffects(ServerPlayer player, Level world, BlockPos pos) {
        for (DatapackChant.ChantEffect effect : chantData.getEffects()) {
            try {
                // 🔧 FIX: Set parent chant context before applying effect
                // This is needed for effigy effects that require access to linked deity
                effect.setParentChant(chantData);
                effect.apply(player);
            } catch (Exception e) {
                LOGGER.error("Failed to execute chant effect for {}: {}", chantData.getId(), e.getMessage());
                player.sendSystemMessage(Component.translatable("eidolonunchained.ui.chant.effect_failed"));
            }
        }
    }
    
}
