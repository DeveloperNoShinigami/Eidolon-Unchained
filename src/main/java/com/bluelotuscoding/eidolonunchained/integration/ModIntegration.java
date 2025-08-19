package com.bluelotuscoding.eidolonunchained.integration;

import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration handler for Eidolon Repraised and Curios API
 */
public class ModIntegration {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModIntegration.class);
    
    public static final String CURIOS_MODID = "curios";

    private static boolean curiosLoaded = false;

    public static void init() {
        // Eidolon is a required dependency; assume it's always present
        LOGGER.info("Eidolon Repraised detected! Enabling Eidolon integrations...");

        curiosLoaded = ModList.get().isLoaded(CURIOS_MODID);

        if (curiosLoaded) {
            LOGGER.info("Curios API detected! Enabling Curios integrations...");
            // Initialize Curios-specific features here
        } else {
            LOGGER.error("Curios API not found! This mod requires Curios API to function.");
        }
    }

    public static boolean isCuriosLoaded() {
        return curiosLoaded;
    }
}
