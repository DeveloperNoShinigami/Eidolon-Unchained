package com.bluelotuscoding.eidolonunchained.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Configuration for AI deity system features
 */
public class AIDeityConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;
    
    // Configuration options
    public static final ForgeConfigSpec.BooleanValue ENABLE_EFFIGY_INTERACTION;
    public static final ForgeConfigSpec.BooleanValue ENABLE_CHANT_SYSTEM;
    public static final ForgeConfigSpec.IntValue DEFAULT_CONVERSATION_TIMEOUT;
    public static final ForgeConfigSpec.IntValue MAX_CONVERSATION_LENGTH;
    
    static {
        BUILDER.push("AI Deity System");
        
        ENABLE_EFFIGY_INTERACTION = BUILDER
            .comment("Enable AI deity responses when right-clicking effigies",
                    "WARNING: This is deprecated. Use chant system instead.",
                    "Default: false (disabled)")
            .define("enableEffigyInteraction", false);
            
        ENABLE_CHANT_SYSTEM = BUILDER
            .comment("Enable AI deity responses through chant sequences",
                    "This is the preferred method for AI deity interaction",
                    "Default: true (enabled)")
            .define("enableChantSystem", true);
            
        DEFAULT_CONVERSATION_TIMEOUT = BUILDER
            .comment("Default timeout for AI conversations in seconds",
                    "Default: 30 seconds")
            .defineInRange("defaultConversationTimeout", 30, 10, 300);
            
        MAX_CONVERSATION_LENGTH = BUILDER
            .comment("Maximum length of AI responses in characters",
                    "Default: 1000 characters")
            .defineInRange("maxConversationLength", 1000, 100, 5000);
        
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
