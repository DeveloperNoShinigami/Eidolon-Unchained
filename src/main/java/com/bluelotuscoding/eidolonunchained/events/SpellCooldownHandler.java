package com.bluelotuscoding.eidolonunchained.events;

import elucent.eidolon.api.spells.Spell;
import elucent.eidolon.capability.IReputation;
import elucent.eidolon.common.spell.PrayerSpell;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.util.LazyOptional;

import java.util.UUID;

/**
 * Handles spell cooldown modifications to prevent non-prayer spells from being affected
 * by Eidolon's prayer cooldown system, while preserving prayer cooldowns.
 */
@Mod.EventBusSubscriber(modid = "eidolonunchained", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SpellCooldownHandler {

    /**
     * Event handler to clear non-prayer spell cooldowns immediately after they're set.
     * This allows normal spells to be cast without the 17.5 minute prayer cooldown,
     * while preserving actual prayer spell cooldowns.
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        // This event is used as a hook point, but we'll actually handle this in runtime
        // by modifying the reputation capability behavior
    }

    /**
     * Checks if a spell should be exempt from prayer cooldowns
     * @param spell The spell being cast
     * @return true if the spell should bypass prayer cooldowns
     */
    public static boolean shouldBypassPrayerCooldown(Spell spell) {
        // Only actual PrayerSpells should have cooldowns
        // All other spells (regular chants, basic spells) should bypass the cooldown
        return !(spell instanceof PrayerSpell);
    }

    /**
     * Clear cooldown for non-prayer spells
     * @param level The world level
     * @param player The player casting the spell
     * @param spell The spell being cast
     */
    public static void clearNonPrayerCooldown(Level level, Player player, Spell spell) {
        if (shouldBypassPrayerCooldown(spell)) {
            LazyOptional<IReputation> iReputationLazyOptional = level.getCapability(IReputation.INSTANCE);
            if (iReputationLazyOptional.resolve().isPresent()) {
                IReputation iReputation = iReputationLazyOptional.resolve().get();
                
                // If this is not a prayer spell, remove any cooldown that was just set
                // We do this by checking if the spell was just cast and clearing its cooldown
                UUID playerId = player.getUUID();
                if (spell instanceof PrayerSpell) {
                    // Keep prayer cooldowns intact
                    return;
                }
                
                // For non-prayer spells, we need to remove the cooldown from the reputation system
                // This requires accessing the internal prayer times map
                try {
                    // Use reflection to clear the cooldown for non-prayer spells
                    var prayerTimesField = iReputation.getClass().getDeclaredField("prayerTimes");
                    prayerTimesField.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    var prayerTimes = (java.util.Map<UUID, java.util.Map<net.minecraft.resources.ResourceLocation, Long>>) prayerTimesField.get(iReputation);
                    
                    if (prayerTimes.containsKey(playerId)) {
                        var playerTimes = prayerTimes.get(playerId);
                        // Remove the spell's cooldown entry if it's not a prayer
                        playerTimes.remove(spell.getRegistryName());
                    }
                } catch (Exception e) {
                    // Fallback: log the issue but don't crash
                    com.bluelotuscoding.eidolonunchained.EidolonUnchained.LOGGER.warn("Could not clear non-prayer spell cooldown for {}: {}", spell.getRegistryName(), e.getMessage());
                }
            }
        }
    }
}
