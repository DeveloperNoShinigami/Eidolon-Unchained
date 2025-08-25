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
            
            // Find chant files that link to this deity instead of using AI config chant sequences
            List<com.bluelotuscoding.eidolonunchained.chant.DatapackChant> linkedChants = 
                com.bluelotuscoding.eidolonunchained.chant.DatapackChantManager.getChantsForDeity(deityId);
            if (linkedChants.isEmpty()) {
                LOGGER.warn("No chant files linked to AI deity: {}", deityId);
                continue;
            }
            
            // Register spells for each linked chant
            for (com.bluelotuscoding.eidolonunchained.chant.DatapackChant chant : linkedChants) {
                // Convert ResourceLocation signs to Sign objects
                Sign[] signs = chant.getSignSequence().stream()
                    .map(signLoc -> Signs.find(signLoc))
                    .filter(java.util.Objects::nonNull)
                    .toArray(Sign[]::new);
            
                if (signs.length == 0) {
                    LOGGER.warn("No valid signs found for chant: {} of deity: {}", chant.getName(), deityId);
                    continue;
                }
                
                // Create and register the AI prayer spell for this specific chant
                ResourceLocation spellId = new ResourceLocation(EidolonUnchained.MODID, 
                    deityId.getPath() + "_" + chant.getId().getPath() + "_ai_prayer");
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
                    LOGGER.info("Registered AI prayer spell for deity: {} with chant: {} ({})", 
                        deityId, chant.getName(), chant.getSignSequence());
                } else {
                    LOGGER.error("Failed to register AI prayer spell for deity: {} chant: {}", deityId, chant.getName());
                }
            }
        }
        
        LOGGER.info("Successfully registered {} AI deity prayer spells", registered);
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
