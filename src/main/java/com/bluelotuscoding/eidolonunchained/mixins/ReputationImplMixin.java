package com.bluelotuscoding.eidolonunchained.mixins;

import com.bluelotuscoding.eidolonunchained.chant.DatapackChant;
import com.bluelotuscoding.eidolonunchained.chant.DatapackChantManager;
import com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig;
import elucent.eidolon.capability.IReputation;
import elucent.eidolon.capability.ReputationImpl;
import elucent.eidolon.common.spell.PrayerSpell;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.resources.ResourceLocation;
import java.util.Map;
import java.util.UUID;

/**
 * Mixin to modify Eidolon's reputation system to properly handle spell cooldowns.
 * - Bypasses cooldowns for non-prayer spells when configured
 * - Reads individual spell cooldowns from JSON datapack definitions
 * - Provides global cooldown control while respecting individual spell settings
 */
@Mixin(value = ReputationImpl.class, remap = false)
public class ReputationImplMixin {

    @Shadow
    private Map<UUID, Map<ResourceLocation, Long>> prayerTimes;

    /**
     * Inject into the canPray method to handle custom spell cooldown logic.
     * This allows for:
     * - Global bypass of prayer cooldowns for regular spells
     * - Individual spell cooldowns from JSON definitions
     * - Proper prayer cooldown preservation
     * 
     * @param player The player UUID
     * @param spell The prayer spell being checked
     * @param time The current game time
     * @param cir Callback info returnable
     */
    @Inject(method = "canPray(Ljava/util/UUID;Lelucent/eidolon/common/spell/PrayerSpell;J)Z", at = @At("HEAD"), cancellable = true)
    public void handleSpellCooldowns(UUID player, PrayerSpell spell, long time, CallbackInfoReturnable<Boolean> cir) {
        // Check if spell cooldown bypass is enabled in config
        if (!EidolonUnchainedConfig.COMMON.bypassSpellCooldowns.get()) {
            return; // If disabled, let original method handle everything
        }
        
        ResourceLocation spellId = spell.getRegistryName();
        if (spellId == null) return;
        
        // Check if this is a custom datapack chant with individual cooldown
        DatapackChant customChant = DatapackChantManager.getChant(spellId);
        if (customChant != null) {
            // Handle custom chant cooldown from JSON
            int customCooldownSeconds = customChant.getCooldown();
            
            if (customCooldownSeconds <= 0) {
                // No cooldown defined, allow immediate casting
                cir.setReturnValue(true);
                return;
            }
            
            // Check custom cooldown timing
            long customCooldownTicks = customCooldownSeconds * 20L; // Convert seconds to ticks
            Map<ResourceLocation, Long> playerTimes = prayerTimes.get(player);
            
            if (playerTimes == null || !playerTimes.containsKey(spellId)) {
                // No previous cast time, allow casting
                cir.setReturnValue(true);
                return;
            }
            
            long lastCastTime = playerTimes.get(spellId);
            boolean canCast = (time - lastCastTime) >= customCooldownTicks;
            cir.setReturnValue(canCast);
            return;
        }
        
        // Check if this is actually a prayer spell by examining its class and properties
        boolean isActualPrayer = isActualPrayerSpell(spell);
        
        if (!isActualPrayer) {
            // For non-prayer spells without custom cooldowns, bypass Eidolon's prayer cooldown
            cir.setReturnValue(true);
        }
        // For actual prayers, let the original method handle cooldowns normally
    }

    /**
     * Determines if a spell is an actual prayer that should have cooldowns.
     * This checks various criteria to distinguish real prayers from regular spells
     * that happen to inherit from PrayerSpell.
     * 
     * @param spell The spell to check
     * @return true if this is an actual prayer that should have cooldowns
     */
    private boolean isActualPrayerSpell(PrayerSpell spell) {
        if (spell == null) return false;
        
        String spellName = spell.getRegistryName().toString().toLowerCase();
        String spellPath = spell.getRegistryName().getPath().toLowerCase();
        
        // Check if this is an actual prayer by name/path
        if (spellName.contains("prayer") || spellPath.contains("prayer")) {
            return true;
        }
        
        // Check if this is a deity-related spell
        if (spellName.contains("deity") || spellPath.contains("deity")) {
            return true;
        }
        
        // Check for specific prayer-related keywords
        if (spellName.contains("blessing") || spellName.contains("divine") || 
            spellName.contains("sacred") || spellName.contains("holy")) {
            return true;
        }
        
        // Check the cooldown value - if it's the default prayer cooldown (21000), it's likely a prayer
        int cooldown = spell.getCooldown();
        if (cooldown >= 20000) { // High cooldown suggests it's a prayer
            return true;
        }
        
        // If none of the above conditions are met, treat it as a regular spell
        return false;
    }
}
