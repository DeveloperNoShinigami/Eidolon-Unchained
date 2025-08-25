package com.bluelotuscoding.eidolonunchained.spells;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager;
import com.bluelotuscoding.eidolonunchained.deity.DatapackDeity;
import com.bluelotuscoding.eidolonunchained.ai.AIDeityManager;
import com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig;
import elucent.eidolon.api.spells.Sign;
import elucent.eidolon.api.spells.Spell;
import elucent.eidolon.registries.Signs;
import elucent.eidolon.registries.Spells;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * Registers AI deity prayer spells that integrate with the existing Eidolon chant system.
 * These spells use specific sign sequences to trigger AI conversations instead of 
 * standard prayer effects.
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AIDeitySpells {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Map of AI deity IDs to their prayer spells
    private static final Map<ResourceLocation, AIDeityPrayerSpell> aiPrayerSpells = new HashMap<>();
    
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        // Register spells only when the first player joins to ensure the server is fully ready
        // and handshake is complete - this prevents config registration interference
        if (aiPrayerSpells.isEmpty()) {
            registerAIDeitySpells();
        }
    }
    
    /**
     * Register prayer spells for all AI-enabled deities.
     * Called after datapacks are loaded to ensure all deities are available.
     */
    public static void registerAIDeitySpells() {
        LOGGER.info("Registering AI deity prayer spells...");
        
        // Get all loaded deities
        var deities = DatapackDeityManager.getAllDeities();
        if (deities.isEmpty()) {
            LOGGER.warn("No deities loaded, skipping AI spell registration");
            return;
        }
        
        int registered = 0;
        for (var entry : deities.entrySet()) {
            ResourceLocation deityId = entry.getKey();
            DatapackDeity deity = entry.getValue();
            
            // Check if this deity has AI configuration
            AIDeityConfig aiConfig = AIDeityManager.getInstance().getAIConfig(deityId);
            if (aiConfig == null) {
                continue; // Skip non-AI deities
            }
            
            // Get chant configuration from AI config
            List<String> chantSigns = getChantSequence(aiConfig);
            if (chantSigns.isEmpty()) {
                LOGGER.warn("No chant sequence configured for AI deity: {}", deityId);
                continue;
            }
            
            // Convert string signs to Sign objects
            Sign[] signs = chantSigns.stream()
                .map(signName -> Signs.find(new ResourceLocation("eidolon", signName)))
                .filter(java.util.Objects::nonNull)
                .toArray(Sign[]::new);
            
            if (signs.length == 0) {
                LOGGER.warn("No valid signs found for AI deity: {}", deityId);
                continue;
            }
            
            // Create and register the AI prayer spell
            ResourceLocation spellId = new ResourceLocation(EidolonUnchained.MODID, deityId.getPath() + "_ai_prayer");
            AIDeityPrayerSpell spell = new AIDeityPrayerSpell(
                spellId,
                deityId,
                0, // Base reputation requirement
                1.0, // Power multiplier
                signs
            );
            
            // Register with Eidolon's spell system
            Spell registeredSpell = Spells.register(spell);
            if (registeredSpell != null) {
                aiPrayerSpells.put(deityId, spell);
                registered++;
                LOGGER.info("Registered AI prayer spell for deity: {} with chant: {}", 
                    deityId, String.join(", ", chantSigns));
            } else {
                LOGGER.error("Failed to register AI prayer spell for deity: {}", deityId);
            }
        }
        
        LOGGER.info("Successfully registered {} AI deity prayer spells", registered);
    }
    
    /**
     * Get the chant sequence for an AI deity from its configuration.
     * Falls back to default sequences if not specified.
     */
    private static List<String> getChantSequence(AIDeityConfig aiConfig) {
        // Check if the AI config specifies a custom chant sequence
        if (aiConfig.apiSettings != null && aiConfig.apiSettings.chantSequence != null) {
            return aiConfig.apiSettings.chantSequence;
        }
        
        // Use default chant sequence based on deity personality/theme
        String personality = aiConfig.personality.toLowerCase();
        
        if (personality.contains("dark") || personality.contains("shadow") || personality.contains("death")) {
            // Dark deity chant: WICKED_SIGN x3
            return List.of("wicked", "wicked", "wicked");
        } else if (personality.contains("light") || personality.contains("holy") || personality.contains("sacred")) {
            // Light deity chant: SACRED_SIGN x3  
            return List.of("sacred", "sacred", "sacred");
        } else if (personality.contains("nature") || personality.contains("forest") || personality.contains("earth")) {
            // Nature deity chant: HARMONY_SIGN x3 (closest to earth/nature)
            return List.of("harmony", "harmony", "harmony");
        } else if (personality.contains("water") || personality.contains("ocean") || personality.contains("sea")) {
            // Water deity chant: WINTER_SIGN x3 (ice/water related)
            return List.of("winter", "winter", "winter");
        } else if (personality.contains("fire") || personality.contains("flame") || personality.contains("sun")) {
            // Fire deity chant: FLAME_SIGN x3
            return List.of("flame", "flame", "flame");
        } else if (personality.contains("air") || personality.contains("wind") || personality.contains("sky")) {
            // Air deity chant: WARDING_SIGN x3 (protection/air related)
            return List.of("warding", "warding", "warding");
        } else {
            // Default generic deity chant: MAGIC_SIGN x3
            return List.of("magic", "magic", "magic");
        }
    }
    
    /**
     * Get the AI prayer spell for a specific deity
     */
    public static AIDeityPrayerSpell getAIPrayerSpell(ResourceLocation deityId) {
        return aiPrayerSpells.get(deityId);
    }
    
    /**
     * Check if a deity has an AI prayer spell registered
     */
    public static boolean hasAIPrayerSpell(ResourceLocation deityId) {
        return aiPrayerSpells.containsKey(deityId);
    }
    
    /**
     * Get all registered AI prayer spells
     */
    public static Map<ResourceLocation, AIDeityPrayerSpell> getAllAIPrayerSpells() {
        return new HashMap<>(aiPrayerSpells);
    }
}
