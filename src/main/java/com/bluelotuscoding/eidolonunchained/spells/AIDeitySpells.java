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
 * Registers AI deity prayer spells that integrate with the existing Eidolon
 * chant system.
 * These spells use specific sign sequences to trigger AI conversations instead
 * of
 * standard prayer effects.
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AIDeitySpells {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Map of AI deity IDs to their prayer spells
    private static final Map<ResourceLocation, AIDeityPrayerSpell> aiPrayerSpells = new HashMap<>();

    @SubscribeEvent
    public static void onAIConfigsLinked(com.bluelotuscoding.eidolonunchained.events.AIConfigsLinkedEvent event) {
        if (event.getLinkedCount() > 0) {
            LOGGER.info("AI configs linked ({} success, {} failed), registering prayer spells...",
                event.getLinkedCount(), event.getFailedCount());
            registerAIDeitySpells();
        } else {
            LOGGER.warn("No AI configs linked, skipping prayer spell registration");
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        // Ensure AI deity configs are linked before attempting to register spells.
        // This helps when datapacks/deities were reloaded after startup.
        try {
            AIDeityManager.getInstance().linkPendingConfigs();
        } catch (Exception e) {
            LOGGER.warn("Failed to link AI deity configs on player login: {}", e.getMessage());
        }

        // Also ensure other datapack-driven systems are linked/registered on player
        // login
        try {
            var chantMgr = com.bluelotuscoding.eidolonunchained.chant.DatapackChantManager.getInstance();
            if (chantMgr != null)
                chantMgr.registerChantsWithEidolon();
        } catch (Exception e) {
            LOGGER.warn("Failed to register chants on login: {}", e.getMessage());
        }

        try {
            // Codex chant integration may need re-registration after datapack reloads
            com.bluelotuscoding.eidolonunchained.integration.CodexChantIntegration.registerChants();
        } catch (Exception e) {
            LOGGER.warn("Failed to run CodexChantIntegration on login: {}", e.getMessage());
        }

        try {
            // Attempt ritual registration as well (deferred registration may have been
            // pending)
            var ritualMgr = com.bluelotuscoding.eidolonunchained.data.RitualDataManager.getInstance();
            if (ritualMgr != null)
                ritualMgr.registerRitualsWithEidolon();
        } catch (Exception e) {
            LOGGER.warn("Failed to register rituals on login: {}", e.getMessage());
        }

        // Client-side chant registration (if on client) - best-effort
        try {
            if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
                com.bluelotuscoding.eidolonunchained.chant.DatapackChantManager.registerClientChantsWithEidolon();
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to register client chants on login: {}", e.getMessage());
        }

        // Only attempt registration if we have no spells registered
        // The main registration should happen via AIConfigsLinkedEvent
        if (aiPrayerSpells.isEmpty()) {
            LOGGER.info("No AI prayer spells registered on login, attempting late registration...");
            try {
                registerAIDeitySpells();
            } catch (Exception e) {
                LOGGER.warn("Failed to register AI prayer spells on login: {}", e.getMessage());
            }
        }
    }

    /**
     * Register prayer spells for all AI-enabled deities.
     * Called after datapacks are loaded to ensure all deities are available.
     */
    public static void registerAIDeitySpells() {
        LOGGER.info("=== AI Prayer Spell Registration Start ===");

        // Get all loaded deities
        var deities = DatapackDeityManager.getAllDeities();
        LOGGER.info("Step 1: Found {} total deities loaded in DatapackDeityManager", deities.size());
        
        if (deities.isEmpty()) {
            LOGGER.error("REGISTRATION BLOCKED: No deities loaded");
            LOGGER.error("Check: DatapackDeityManager.apply() should run before this");
            return;
        }
        
        int aiConfigCount = AIDeityManager.getInstance().getAIEnabledDeities().size();
        LOGGER.info("Step 2: Found {} AI-enabled deities", aiConfigCount);
        
        if (aiConfigCount == 0) {
            LOGGER.error("REGISTRATION BLOCKED: No AI configs linked");
            LOGGER.error("Check: AIDeityManager.linkPendingConfigs() should run before this");
            return;
        }

        int registered = 0;
        int deitiesWithAI = 0;
        int deitiesWithChants = 0;

        for (var entry : deities.entrySet()) {
            ResourceLocation deityId = entry.getKey();
            DatapackDeity deity = entry.getValue();

            // Check if this deity has AI configuration
            AIDeityConfig aiConfig = AIDeityManager.getInstance().getAIConfig(deityId);
            if (aiConfig == null) {
                LOGGER.debug("Deity {} has no AI config, skipping", deityId);
                continue; // Skip non-AI deities
            }
            // If we've already registered spells for this deity, skip to avoid
            // duplicate registration which can create conflicting ModConfig files
            if (aiPrayerSpells.containsKey(deityId)) {
                LOGGER.info("AI prayer spells already registered for deity {}, skipping duplicate registration", deityId);
                continue;
            }
            deitiesWithAI++;
            LOGGER.info("Deity {} has AI config, checking for linked chants", deityId);

            // Find chant files that link to this deity instead of using AI config chant
            // sequences
            List<com.bluelotuscoding.eidolonunchained.chant.DatapackChant> linkedChants = com.bluelotuscoding.eidolonunchained.chant.DatapackChantManager
                    .getChantsForDeity(deityId);
            LOGGER.info("Found {} chants linked to deity {}", linkedChants.size(), deityId);
            if (linkedChants.isEmpty()) {
                LOGGER.warn("No chant files linked to AI deity: {}", deityId);
                continue;
            }
            deitiesWithChants++;

            // Register spells for each linked chant
            for (com.bluelotuscoding.eidolonunchained.chant.DatapackChant chant : linkedChants) {
                // Only create an AI prayer spell for chants that explicitly declare a prayer
                // effect
                String pet = chant.getPrayerEffectType();
                LOGGER.info("Chant {} has prayer_effect_type: '{}'", chant.getId(), pet);
                if (pet == null || pet.isEmpty()) {
                    LOGGER.debug("Skipping AI prayer spell for chant {} (no prayer_effect_type)", chant.getId());
                    continue;
                }

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
                        signs);

                // Register with Eidolon's spell system
                Spell registeredSpell = Spells.register(spell);
                if (registeredSpell != null) {
                    aiPrayerSpells.put(deityId, spell);
                    registered++;
                    LOGGER.info("Registered AI prayer spell for deity: {} with chant: {} ({})",
                            deityId, chant.getName(), chant.getSignSequence());
                } else {
                    LOGGER.error("Failed to register AI prayer spell for deity: {} chant: {}", deityId,
                            chant.getName());
                }
            }
        }

        LOGGER.info(
                "AI Prayer Spell Registration Summary: {} deities total, {} with AI config, {} with linked chants, {} spells registered",
                deities.size(), deitiesWithAI, deitiesWithChants, registered);
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
