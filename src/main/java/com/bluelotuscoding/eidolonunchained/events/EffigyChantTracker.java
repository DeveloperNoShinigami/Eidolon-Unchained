package com.bluelotuscoding.eidolonunchained.events;

import com.bluelotuscoding.eidolonunchained.chant.DatapackChant;
import net.minecraft.core.BlockPos;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Tracks which chant was last performed near each effigy.
 * This allows effigy right-click to know what chant context to use for the conversation.
 */
public class EffigyChantTracker {
    private static final Logger LOGGER = LogManager.getLogger();
    
    // Map: effigy position → last chant performed near it
    private static final Map<BlockPos, DatapackChant> effigyLastChants = new ConcurrentHashMap<>();
    
    /**
     * Store which chant was performed near a specific effigy
     */
    public static void setEffigyLastChant(BlockPos effigyPos, DatapackChant chant) {
        effigyLastChants.put(effigyPos, chant);
        LOGGER.info("🔮 Stored chant {} for effigy at {}", chant.getId(), effigyPos);
    }
    
    /**
     * Get the last chant performed near a specific effigy
     */
    public static DatapackChant getEffigyLastChant(BlockPos effigyPos) {
        return effigyLastChants.get(effigyPos);
    }
    
    /**
     * Clear the stored chant for an effigy (after use)
     */
    public static void clearEffigyLastChant(BlockPos effigyPos) {
        DatapackChant removed = effigyLastChants.remove(effigyPos);
        if (removed != null) {
            LOGGER.info("🧹 Cleared chant {} from effigy at {}", removed.getId(), effigyPos);
        }
    }
    
    /**
     * Check if an effigy has a stored chant
     */
    public static boolean hasStoredChant(BlockPos effigyPos) {
        return effigyLastChants.containsKey(effigyPos);
    }
    
    /**
     * Clear all stored chants (for cleanup)
     */
    public static void clearAll() {
        effigyLastChants.clear();
        LOGGER.info("🧹 Cleared all effigy chant tracking");
    }
}
