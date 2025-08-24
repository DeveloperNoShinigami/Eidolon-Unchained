package com.bluelotuscoding.eidolonunchained.chant;

import com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig;
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
import org.slf4j.Logger;

import java.util.List;

/**
 * A custom spell that represents a datapack-defined chant.
 * Extends PrayerSpell to integrate with Eidolon's existing chant/prayer system.
 * When cast, executes the custom effects defined in the chant configuration.
 */
public class DatapackChantSpell extends PrayerSpell {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private final DatapackChant chantData;
    
    public DatapackChantSpell(ResourceLocation name, DatapackChant chantData, Sign... signs) {
        // Create a dummy deity for the parent class
        super(name, createDummyDeity(name), signs);
        this.chantData = chantData;
    }
    
    /**
     * Creates a dummy deity for the parent PrayerSpell class
     */
    private static Deity createDummyDeity(ResourceLocation spellName) {
        return new Deity(new ResourceLocation(spellName.getNamespace(), "chant_" + spellName.getPath()), 100, 100, 100) {
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
    public void cast(Level world, BlockPos pos, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        
        // Check if chant system is enabled
        if (!EidolonUnchainedConfig.COMMON.enableChantSystem.get()) {
            serverPlayer.sendSystemMessage(Component.literal("§cChant system is disabled"));
            return;
        }
        
        // Execute the custom chant effects
        executeChantEffects(serverPlayer, world, pos);
        
        // Show success message
        serverPlayer.sendSystemMessage(Component.literal("§6✨ " + chantData.getName() + " completed successfully!"));
        
        LOGGER.info("Player {} successfully performed chant: {}", 
                   serverPlayer.getName().getString(), chantData.getId());
    }
    
    /**
     * Executes the custom effects defined in the chant configuration
     */
    private void executeChantEffects(ServerPlayer player, Level world, BlockPos pos) {
        for (DatapackChant.ChantEffect effect : chantData.getEffects()) {
            try {
                effect.apply(player);
            } catch (Exception e) {
                LOGGER.error("Failed to execute chant effect for {}: {}", chantData.getId(), e.getMessage());
                player.sendSystemMessage(Component.literal("§cChant effect failed to execute"));
            }
        }
    }
}
