package com.bluelotuscoding.eidolonunchained.integration;

import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration handler for Eidolon Repraised and Curios API
 */
public class ModIntegration {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModIntegration.class);
    
    public static final String EIDOLON_MODID = "eidolon";
    public static final String CURIOS_MODID = "curios";
    
    private static boolean eidolonLoaded = false;
    private static boolean curiosLoaded = false;
    
    public static void init() {
        eidolonLoaded = ModList.get().isLoaded(EIDOLON_MODID);
        curiosLoaded = ModList.get().isLoaded(CURIOS_MODID);
        
        if (eidolonLoaded) {
            LOGGER.info("Eidolon Repraised detected! Enabling Eidolon integrations...");
            // Initialize Eidolon-specific features here
        } else {
            LOGGER.error("Eidolon Repraised not found! This mod requires Eidolon Repraised to function.");
        }
        
        if (curiosLoaded) {
            LOGGER.info("Curios API detected! Enabling Curios integrations...");
            // Initialize Curios-specific features here
        } else {
            LOGGER.error("Curios API not found! This mod requires Curios API to function.");
        }
    }
    
    public static boolean isEidolonLoaded() {
        return eidolonLoaded;
    }
    
    public static boolean isCuriosLoaded() {
        return curiosLoaded;
    }
}
