package com.bluelotuscoding.eidolonunchained.integration;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Utility class to detect Eidolon version features and capabilities.
 * This helps us automatically switch between compatibility mode and modern features.
 */
public class EidolonVersionDetection {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private static Boolean hasCodexEvents = null;
    private static Boolean hasEnhancedTitlePage = null;
    private static Boolean hasPublicCategoryFields = null;
    
    /**
     * Check if this Eidolon version supports CodexEvents
     * This is the key feature that enables modern category creation
     */
    public static boolean hasCodexEvents() {
        if (hasCodexEvents == null) {
            try {
                Class.forName("elucent.eidolon.codex.CodexEvents");
                hasCodexEvents = true;
                LOGGER.info("✅ CodexEvents detected - modern Eidolon version available");
            } catch (ClassNotFoundException e) {
                hasCodexEvents = false;
                LOGGER.info("ℹ️ CodexEvents not found - using legacy compatibility mode");
            }
        }
        return hasCodexEvents;
    }
    
    /**
     * Check if TitlePage constructor accepts ItemStack parameter
     */
    public static boolean hasEnhancedTitlePage() {
        if (hasEnhancedTitlePage == null) {
            try {
                Class.forName("elucent.eidolon.codex.TitlePage")
                    .getConstructor(String.class, net.minecraft.world.item.ItemStack.class);
                hasEnhancedTitlePage = true;
                LOGGER.info("✅ Enhanced TitlePage constructor available");
            } catch (NoSuchMethodException | ClassNotFoundException e) {
                hasEnhancedTitlePage = false;
                LOGGER.info("ℹ️ Enhanced TitlePage constructor not available");
            }
        }
        return hasEnhancedTitlePage;
    }
    
    /**
     * Check if Category fields are publicly accessible
     */
    public static boolean hasPublicCategoryFields() {
        if (hasPublicCategoryFields == null) {
            try {
                java.lang.reflect.Field keyField = Class.forName("elucent.eidolon.codex.Category")
                    .getDeclaredField("key");
                hasPublicCategoryFields = java.lang.reflect.Modifier.isPublic(keyField.getModifiers());
                LOGGER.info("✅ Public Category fields available");
            } catch (NoSuchFieldException | ClassNotFoundException e) {
                hasPublicCategoryFields = false;
                LOGGER.info("ℹ️ Public Category fields not available");
            }
        }
        return hasPublicCategoryFields;
    }
    
    /**
     * Get the current Eidolon integration mode
     */
    public static EidolonMode getEidolonMode() {
        if (hasCodexEvents()) {
            return EidolonMode.MODERN;
        } else {
            return EidolonMode.LEGACY;
        }
    }
    
    /**
     * Log all detected features
     */
    public static void logFeatureDetection() {
        LOGGER.info("🔍 Eidolon Feature Detection Results:");
        LOGGER.info("   Mode: {}", getEidolonMode());
        LOGGER.info("   CodexEvents: {}", hasCodexEvents() ? "✅ Available" : "❌ Not Available");
        LOGGER.info("   Enhanced TitlePage: {}", hasEnhancedTitlePage() ? "✅ Available" : "❌ Not Available");
        LOGGER.info("   Public Category Fields: {}", hasPublicCategoryFields() ? "✅ Available" : "❌ Not Available");
        
        if (getEidolonMode() == EidolonMode.LEGACY) {
            LOGGER.info("🔧 Running in Legacy Mode - using reflection where necessary");
            LOGGER.info("📋 Migration Guide: See EIDOLON_VERSION_MIGRATION_GUIDE.md");
        } else {
            LOGGER.info("🚀 Running in Modern Mode - using event system and direct imports");
        }
    }
    
    /**
     * Eidolon integration modes
     */
    public enum EidolonMode {
        LEGACY("Legacy - Reflection Required"),
        MODERN("Modern - Event System Available");
        
        private final String description;
        
        EidolonMode(String description) {
            this.description = description;
        }
        
        @Override
        public String toString() {
            return description;
        }
    }
}
