package com.bluelotuscoding.eidolonunchained.events;

import com.bluelotuscoding.eidolonunchained.chant.DatapackChantManager;
import com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig;
import com.bluelotuscoding.eidolonunchained.chant.DatapackChant;
import elucent.eidolon.api.spells.Spell;
import elucent.eidolon.common.spell.PrayerSpell;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Handles spell cooldown calculations for custom datapack chants and prayers.
 * Integrates with ReputationImplMixin to provide proper cooldown values.
 */
@Mod.EventBusSubscriber(modid = "eidolonunchained", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SpellCooldownHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Get the cooldown for a specific spell in ticks.
     * This method is called by ReputationImplMixin to determine cooldowns.
     * 
     * @param spell The spell to get cooldown for
     * @param player The player casting the spell (for future reputation-based scaling)
     * @return Cooldown in ticks (20 ticks = 1 second)
     */
    public static long getSpellCooldown(Spell spell, Player player) {
        if (spell == null) {
            return 0L;
        }

        // For datapack chants, check if we have custom cooldown data
        ResourceLocation spellId = spell.getRegistryName();
        if (spellId != null) {
            DatapackChant chantData = DatapackChantManager.getChant(spellId);
            if (chantData != null && chantData.getCooldown() > 0) {
                // Use the custom cooldown from JSON (convert seconds to ticks)
                long cooldownTicks = chantData.getCooldown() * 20L;
                LOGGER.debug("Using custom cooldown {} ticks for spell {}", cooldownTicks, spellId);
                return cooldownTicks;
            }
        }

        // For prayer spells, use config-based cooldown
        if (spell instanceof PrayerSpell) {
            int configCooldownMinutes = EidolonUnchainedConfig.COMMON.prayerCooldownMinutes.get();
            long cooldownTicks = configCooldownMinutes * 60L * 20L; // Convert minutes to ticks
            LOGGER.debug("Using config prayer cooldown {} ticks ({} minutes) for spell {}", cooldownTicks, configCooldownMinutes, spellId);
            return cooldownTicks;
        }

        // For non-prayer spells without custom data, no cooldown
        LOGGER.debug("No cooldown for non-prayer spell {}", spellId);
        return 0L;
    }

    /**
     * Check if a spell should have any cooldown at all.
     * 
     * @param spell The spell to check
     * @return true if the spell should have cooldown, false otherwise
     */
    public static boolean shouldHaveCooldown(Spell spell) {
        if (spell == null) {
            return false;
        }

        // Check if it's a datapack chant with cooldown
        ResourceLocation spellId = spell.getRegistryName();
        if (spellId != null) {
            DatapackChant chantData = DatapackChantManager.getChant(spellId);
            if (chantData != null && chantData.getCooldown() > 0) {
                return true;
            }
        }

        // Prayer spells always have cooldown (unless config disables it)
        if (spell instanceof PrayerSpell) {
            return EidolonUnchainedConfig.COMMON.prayerCooldownMinutes.get() > 0;
        }

        // Other spells don't have cooldown by default
        return false;
    }

    /**
     * Get a human-readable cooldown description for display purposes.
     * 
     * @param spell The spell to describe
     * @return String description of the cooldown
     */
    public static String getCooldownDescription(Spell spell, Player player) {
        long cooldownTicks = getSpellCooldown(spell, player);
        if (cooldownTicks <= 0) {
            return "No cooldown";
        }

        long seconds = cooldownTicks / 20;
        if (seconds < 60) {
            return seconds + " seconds";
        } else {
            long minutes = seconds / 60;
            long remainingSeconds = seconds % 60;
            if (remainingSeconds == 0) {
                return minutes + " minutes";
            } else {
                return minutes + "m " + remainingSeconds + "s";
            }
        }
    }
}
