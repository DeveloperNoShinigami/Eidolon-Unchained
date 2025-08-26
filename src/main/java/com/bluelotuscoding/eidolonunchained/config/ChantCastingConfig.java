package com.bluelotuscoding.eidolonunchained.config;

/**
 * Configuration and utilities for chant casting modes
 */
public class ChantCastingConfig {
    
    /**
     * Available chant casting modes
     */
    public enum CastingMode {
        /**
         * Cast the entire chant sequence with one key press (default)
         * User presses G/H/J/K and the full chant is executed instantly
         */
        FULL_CHANT,
        
        /**
         * Cast signs one by one like in the codex interface
         * User presses G/H/J/K repeatedly to advance through each sign
         */
        INDIVIDUAL_SIGNS,
        
        /**
         * Support both approaches - hold key for full chant, tap for individual signs
         * Tap = individual sign, Hold = full chant
         */
        HYBRID
    }
    
    /**
     * Get the current casting mode from config
     */
    public static CastingMode getCurrentMode() {
        String modeStr = EidolonUnchainedConfig.COMMON.chantCastingMode.get();
        try {
            return CastingMode.valueOf(modeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Fallback to default if config is invalid
            return CastingMode.FULL_CHANT;
        }
    }
    
    /**
     * Get the timeout for individual sign casting
     */
    public static int getIndividualSignTimeout() {
        return EidolonUnchainedConfig.COMMON.individualSignTimeoutMs.get();
    }
    
    /**
     * Check if chant feedback is enabled
     */
    public static boolean isFeedbackEnabled() {
        return EidolonUnchainedConfig.COMMON.enableChantFeedback.get();
    }
    
    /**
     * Check if the mode supports individual sign casting
     */
    public static boolean supportsIndividualSigns() {
        CastingMode mode = getCurrentMode();
        return mode == CastingMode.INDIVIDUAL_SIGNS || mode == CastingMode.HYBRID;
    }
    
    /**
     * Check if the mode supports full chant casting
     */
    public static boolean supportsFullChant() {
        CastingMode mode = getCurrentMode();
        return mode == CastingMode.FULL_CHANT || mode == CastingMode.HYBRID;
    }
}
