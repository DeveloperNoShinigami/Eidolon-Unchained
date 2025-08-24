package com.bluelotuscoding.eidolonunchained.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Unified configuration for Eidolon Unchained
 * Consolidates all mod settings into a single, well-organized configuration file
 */
public class EidolonUnchainedConfig {
    
    public static final CommonConfig COMMON;
    public static final ForgeConfigSpec COMMON_SPEC;
    
    static {
        final Pair<CommonConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(CommonConfig::new);
        COMMON_SPEC = specPair.getRight();
        COMMON = specPair.getLeft();
    }
    
    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, COMMON_SPEC, "eidolonunchained-common.toml");
    }
    
    public static class CommonConfig {
        
        // ===========================================
        // AI DEITY SYSTEM CONFIGURATION
        // ===========================================
        
        public final ForgeConfigSpec.ConfigValue<String> aiProvider;
        public final ForgeConfigSpec.ConfigValue<String> geminiApiKey;
        public final ForgeConfigSpec.ConfigValue<String> geminiModel;
        public final ForgeConfigSpec.IntValue geminiTimeout;
        public final ForgeConfigSpec.DoubleValue aiTemperature;
        public final ForgeConfigSpec.IntValue maxTokens;
        public final ForgeConfigSpec.BooleanValue enableAIDeities;
        public final ForgeConfigSpec.BooleanValue logAIInteractions;
        
        // ===========================================
        // CHANT SYSTEM CONFIGURATION
        // ===========================================
        
        public final ForgeConfigSpec.BooleanValue enableChantSystem;
        public final ForgeConfigSpec.BooleanValue enableDatapackChants;
        public final ForgeConfigSpec.BooleanValue showChantsInCodex;
        public final ForgeConfigSpec.ConfigValue<String> chantCodexCategory;
        public final ForgeConfigSpec.BooleanValue useIndividualCategories;
        public final ForgeConfigSpec.BooleanValue requireExactSignOrder;
        public final ForgeConfigSpec.IntValue chantCooldownSeconds;
        public final ForgeConfigSpec.BooleanValue allowChantCancellation;
        
        // ===========================================
        // DEITY INTERACTION CONFIGURATION
        // ===========================================
        
        public final ForgeConfigSpec.BooleanValue enableEffigyRightClick;
        public final ForgeConfigSpec.BooleanValue enableChatInteraction;
        public final ForgeConfigSpec.BooleanValue requireChantCompletion;
        public final ForgeConfigSpec.IntValue conversationTimeoutMinutes;
        public final ForgeConfigSpec.BooleanValue enableReputationMessages;
        public final ForgeConfigSpec.BooleanValue enableDeityNotifications;
        
        // ===========================================
        // PRAYER SYSTEM CONFIGURATION
        // ===========================================
        
        public final ForgeConfigSpec.BooleanValue enablePrayerSystem;
        public final ForgeConfigSpec.IntValue prayerCooldownMinutes;
        public final ForgeConfigSpec.BooleanValue enablePrayerEffects;
        public final ForgeConfigSpec.BooleanValue enablePrayerCommands;
        public final ForgeConfigSpec.IntValue maxPrayersPerDay;
        public final ForgeConfigSpec.BooleanValue enablePrayerHistory;
        
        // ===========================================
        // INTEGRATION CONFIGURATION
        // ===========================================
        
        public final ForgeConfigSpec.BooleanValue enableEidolonIntegration;
        public final ForgeConfigSpec.BooleanValue enableCodexIntegration;
        public final ForgeConfigSpec.BooleanValue enableResearchIntegration;
        public final ForgeConfigSpec.BooleanValue enableKubeJSIntegration;
        public final ForgeConfigSpec.BooleanValue enableCustomDeities;
        
        // ===========================================
        // SECURITY AND PERMISSIONS
        // ===========================================
        
        public final ForgeConfigSpec.IntValue requiredOpLevel;
        public final ForgeConfigSpec.BooleanValue enablePermissionChecks;
        public final ForgeConfigSpec.BooleanValue logSecurityEvents;
        public final ForgeConfigSpec.BooleanValue encryptApiKeys;
        
        // ===========================================
        // DEBUG AND DEVELOPMENT
        // ===========================================
        
        public final ForgeConfigSpec.BooleanValue enableDebugMode;
        public final ForgeConfigSpec.BooleanValue verboseLogging;
        public final ForgeConfigSpec.BooleanValue enableDataGeneration;
        public final ForgeConfigSpec.BooleanValue validateJsonFiles;
        
        CommonConfig(ForgeConfigSpec.Builder builder) {
            
            // ===========================================
            // AI DEITY SYSTEM CONFIGURATION
            // ===========================================
            builder.comment(
                "═══════════════════════════════════════════════════════════════════════",
                " AI DEITY SYSTEM CONFIGURATION",
                " Configure AI-powered deity interactions and responses",
                "═══════════════════════════════════════════════════════════════════════"
            ).push("ai_deities");
            
            enableAIDeities = builder
                .comment("Enable AI-powered deity conversations and interactions")
                .define("enable_ai_deities", true);
            
            aiProvider = builder
                .comment("AI provider to use (gemini, openai, proxy)")
                .define("ai_provider", "gemini");
            
            geminiApiKey = builder
                .comment("Google Gemini API key (leave empty to use environment variable EIDOLON_GEMINI_API_KEY)")
                .define("gemini_api_key", "");
            
            geminiModel = builder
                .comment("Gemini model to use (gemini-1.5-flash, gemini-1.5-pro)")
                .define("gemini_model", "gemini-1.5-flash");
            
            geminiTimeout = builder
                .comment("API request timeout in seconds")
                .defineInRange("gemini_timeout_seconds", 30, 5, 120);
            
            aiTemperature = builder
                .comment("AI response creativity (0.0 = predictable, 1.0 = creative)")
                .defineInRange("ai_temperature", 0.7, 0.0, 1.0);
            
            maxTokens = builder
                .comment("Maximum tokens in AI responses")
                .defineInRange("max_tokens", 500, 50, 2000);
            
            logAIInteractions = builder
                .comment("Log AI interactions for debugging (API keys are never logged)")
                .define("log_ai_interactions", false);
            
            builder.pop();
            
            // ===========================================
            // CHANT SYSTEM CONFIGURATION
            // ===========================================
            builder.comment(
                "═══════════════════════════════════════════════════════════════════════",
                " CHANT SYSTEM CONFIGURATION", 
                " Configure the datapack chant system and sign combinations",
                "═══════════════════════════════════════════════════════════════════════"
            ).push("chant_system");
            
            enableChantSystem = builder
                .comment("Enable the custom chant system")
                .define("enable_chant_system", true);
            
            enableDatapackChants = builder
                .comment("Allow chants to be defined in datapacks (data/modid/chants/)")
                .define("enable_datapack_chants", true);
            
            showChantsInCodex = builder
                .comment("Show custom chants in the Eidolon codex")
                .define("show_chants_in_codex", true);
            
            chantCodexCategory = builder
                .comment("Default codex category for custom chants (used when use_individual_categories is false)")
                .define("chant_codex_category", "examples");
            
            useIndividualCategories = builder
                .comment("Allow chants to specify their own category in JSON (if false, all use default category)")
                .define("use_individual_categories", true);
            
            requireExactSignOrder = builder
                .comment("Require exact sign order for chants (disable for more flexible casting)")
                .define("require_exact_sign_order", true);
            
            chantCooldownSeconds = builder
                .comment("Cooldown between chant attempts in seconds")
                .defineInRange("chant_cooldown_seconds", 5, 0, 60);
            
            allowChantCancellation = builder
                .comment("Allow players to cancel chants mid-sequence")
                .define("allow_chant_cancellation", true);
            
            builder.pop();
            
            // ===========================================
            // DEITY INTERACTION CONFIGURATION
            // ===========================================
            builder.comment(
                "═══════════════════════════════════════════════════════════════════════",
                " DEITY INTERACTION CONFIGURATION",
                " Configure how players interact with deities",
                "═══════════════════════════════════════════════════════════════════════"
            ).push("deity_interaction");
            
            enableEffigyRightClick = builder
                .comment("Enable deity interaction by right-clicking effigies (legacy mode)")
                .define("enable_effigy_right_click", false);
            
            enableChatInteraction = builder
                .comment("Enable deity conversation through chat after chant completion")
                .define("enable_chat_interaction", true);
            
            requireChantCompletion = builder
                .comment("Require chant completion before deity interaction")
                .define("require_chant_completion", true);
            
            conversationTimeoutMinutes = builder
                .comment("How long conversations stay active in minutes")
                .defineInRange("conversation_timeout_minutes", 10, 1, 60);
            
            enableReputationMessages = builder
                .comment("Show reputation change messages to players")
                .define("enable_reputation_messages", true);
            
            enableDeityNotifications = builder
                .comment("Show deity unlock and progression notifications")
                .define("enable_deity_notifications", true);
            
            builder.pop();
            
            // ===========================================
            // PRAYER SYSTEM CONFIGURATION
            // ===========================================
            builder.comment(
                "═══════════════════════════════════════════════════════════════════════",
                " PRAYER SYSTEM CONFIGURATION",
                " Configure prayer types, cooldowns, and effects",
                "═══════════════════════════════════════════════════════════════════════"
            ).push("prayer_system");
            
            enablePrayerSystem = builder
                .comment("Enable the prayer system for deity interactions")
                .define("enable_prayer_system", true);
            
            prayerCooldownMinutes = builder
                .comment("Cooldown between prayers in minutes")
                .defineInRange("prayer_cooldown_minutes", 5, 0, 60);
            
            enablePrayerEffects = builder
                .comment("Allow prayers to grant effects and buffs")
                .define("enable_prayer_effects", true);
            
            enablePrayerCommands = builder
                .comment("Allow prayers to execute commands (server admin configured)")
                .define("enable_prayer_commands", true);
            
            maxPrayersPerDay = builder
                .comment("Maximum prayers per player per day (0 = unlimited)")
                .defineInRange("max_prayers_per_day", 10, 0, 100);
            
            enablePrayerHistory = builder
                .comment("Track prayer history for reputation calculations")
                .define("enable_prayer_history", true);
            
            builder.pop();
            
            // ===========================================
            // INTEGRATION CONFIGURATION
            // ===========================================
            builder.comment(
                "═══════════════════════════════════════════════════════════════════════",
                " INTEGRATION CONFIGURATION",
                " Configure integrations with other mods and systems",
                "═══════════════════════════════════════════════════════════════════════"
            ).push("integrations");
            
            enableEidolonIntegration = builder
                .comment("Enable integration with Eidolon mod systems")
                .define("enable_eidolon_integration", true);
            
            enableCodexIntegration = builder
                .comment("Integrate custom content with Eidolon codex")
                .define("enable_codex_integration", true);
            
            enableResearchIntegration = builder
                .comment("Integrate with Eidolon research system")
                .define("enable_research_integration", true);
            
            enableKubeJSIntegration = builder
                .comment("Enable KubeJS integration for custom scripting")
                .define("enable_kubejs_integration", true);
            
            enableCustomDeities = builder
                .comment("Allow custom deities from datapacks")
                .define("enable_custom_deities", true);
            
            builder.pop();
            
            // ===========================================
            // SECURITY AND PERMISSIONS
            // ===========================================
            builder.comment(
                "═══════════════════════════════════════════════════════════════════════",
                " SECURITY AND PERMISSIONS",
                " Configure security settings and permission requirements",
                "═══════════════════════════════════════════════════════════════════════"
            ).push("security");
            
            requiredOpLevel = builder
                .comment("Required OP level for configuration commands (0-4)")
                .defineInRange("required_op_level", 4, 0, 4);
            
            enablePermissionChecks = builder
                .comment("Enable permission checks for sensitive operations")
                .define("enable_permission_checks", true);
            
            logSecurityEvents = builder
                .comment("Log security-related events and permission checks")
                .define("log_security_events", true);
            
            encryptApiKeys = builder
                .comment("Encrypt API keys when stored in config files")
                .define("encrypt_api_keys", true);
            
            builder.pop();
            
            // ===========================================
            // DEBUG AND DEVELOPMENT
            // ===========================================
            builder.comment(
                "═══════════════════════════════════════════════════════════════════════",
                " DEBUG AND DEVELOPMENT",
                " Configuration for debugging and development features",
                "═══════════════════════════════════════════════════════════════════════"
            ).push("debug");
            
            enableDebugMode = builder
                .comment("Enable debug mode with additional logging and validation")
                .define("enable_debug_mode", false);
            
            verboseLogging = builder
                .comment("Enable verbose logging for troubleshooting")
                .define("verbose_logging", false);
            
            enableDataGeneration = builder
                .comment("Enable data generation for chants and recipes")
                .define("enable_data_generation", true);
            
            validateJsonFiles = builder
                .comment("Validate JSON files on load and show detailed errors")
                .define("validate_json_files", true);
            
            builder.pop();
        }
    }
}
