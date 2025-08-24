package com.bluelotuscoding.eidolonunchained.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class AIProviderConfig {
    public static final ForgeConfigSpec SPEC;
    public static final AIProviderConfig INSTANCE;
    
    static {
        Pair<AIProviderConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(AIProviderConfig::new);
        SPEC = specPair.getRight();
        INSTANCE = specPair.getLeft();
    }
    
    // AI Provider Settings
    public final ForgeConfigSpec.ConfigValue<String> providerType;
    public final ForgeConfigSpec.ConfigValue<String> apiEndpoint;
    public final ForgeConfigSpec.ConfigValue<String> apiKeySource;
    public final ForgeConfigSpec.BooleanValue enableAI;
    public final ForgeConfigSpec.BooleanValue requireOpForSetup;
    
    // Proxy Service Settings
    public final ForgeConfigSpec.ConfigValue<String> proxyServiceUrl;
    public final ForgeConfigSpec.ConfigValue<String> proxyServiceKey;
    
    private AIProviderConfig(ForgeConfigSpec.Builder builder) {
        builder.comment("AI Deity Provider Configuration")
               .push("ai_provider");
        
        enableAI = builder
            .comment("Enable AI-powered deity interactions")
            .define("enable_ai", false);
            
        requireOpForSetup = builder
            .comment("Require server operator permissions to configure AI settings")
            .define("require_op_for_setup", true);
        
        providerType = builder
            .comment("AI Provider: 'direct' (OpenAI/Gemini), 'proxy' (Player2.game), or 'hybrid'")
            .define("provider_type", "proxy");
            
        apiEndpoint = builder
            .comment("Direct API endpoint (for direct mode)")
            .define("api_endpoint", "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro:generateContent");
            
        apiKeySource = builder
            .comment("API key source: 'env' (environment variable), 'file' (config file), or 'command' (set via command)")
            .define("api_key_source", "env");
        
        builder.comment("Proxy Service Settings (for proxy/hybrid mode)")
               .push("proxy");
               
        proxyServiceUrl = builder
            .comment("Proxy service URL (e.g., Player2.game API)")
            .define("proxy_url", "https://api.player2.game/v1/chat");
            
        proxyServiceKey = builder
            .comment("Proxy service API key (set via command for security)")
            .define("proxy_key", "");
            
        builder.pop().pop();
    }
}
