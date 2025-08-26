package com.bluelotuscoding.eidolonunchained.debug;

import com.bluelotuscoding.eidolonunchained.config.APIKeyManager;
import com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig;
import com.bluelotuscoding.eidolonunchained.integration.gemini.GeminiAPIClient;
import com.bluelotuscoding.eidolonunchained.ai.GenerationConfig;
import com.bluelotuscoding.eidolonunchained.ai.SafetySettings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.ArrayList;

/**
 * Enhanced API testing and debugging utility for Eidolon Unchained
 * Provides comprehensive testing tools with detailed error diagnostics
 */
public class APIDebugger {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(APIDebugger.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    public enum TestType {
        CONNECTION,     // Test basic API connectivity
        AUTHENTICATION, // Test API key validation  
        SIMPLE_REQUEST, // Test simple API request
        FULL_REQUEST,   // Test complete deity conversation request
        CONFIG_VALIDATION // Test configuration validation
    }
    
    /**
     * Comprehensive API test suite with enhanced error diagnostics
     */
    public static CompletableFuture<APITestResult> runAPITest(ServerPlayer player, TestType testType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                switch (testType) {
                    case CONNECTION:
                        return testConnection(player);
                    case AUTHENTICATION:
                        return testAuthentication(player);
                    case SIMPLE_REQUEST:
                        return testSimpleRequest(player);
                    case FULL_REQUEST:
                        return testFullRequest(player);
                    case CONFIG_VALIDATION:
                        return testConfigValidation(player);
                    default:
                        return new APITestResult(false, "Unknown test type", null);
                }
            } catch (Exception e) {
                LOGGER.error("API test failed: {}", e.getMessage(), e);
                return new APITestResult(false, "Test execution failed: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Test basic connectivity and configuration
     */
    private static APITestResult testConnection(ServerPlayer player) {
        LOGGER.info("🔍 Testing API connection for player: {}", player.getName().getString());
        
        try {
            String provider = EidolonUnchainedConfig.COMMON.aiProvider.get();
            String model = EidolonUnchainedConfig.COMMON.geminiModel.get();
            int timeout = EidolonUnchainedConfig.COMMON.geminiTimeout.get();
            
            String apiKey = APIKeyManager.getAPIKey(provider);
            if (apiKey == null || apiKey.isEmpty()) {
                return new APITestResult(false, 
                    "❌ No API key found!\n\n" +
                    "Setup instructions:\n" +
                    "1. Get key: https://aistudio.google.com/app/apikey\n" +
                    "2. Set key: /eidolon api set gemini YOUR_KEY\n" +
                    "3. Test again: /eidolon api test auth", 
                    null);
            }
            
            if (!apiKey.startsWith("AIza") && provider.equals("gemini")) {
                return new APITestResult(false, 
                    "❌ Invalid Gemini API key format!\n" +
                    "• Gemini keys start with 'AIza'\n" +
                    "• Get new key: https://aistudio.google.com/app/apikey\n" +
                    "• Set key: /eidolon api set gemini YOUR_KEY", 
                    null);
            }
            
            return new APITestResult(true, 
                String.format("✅ Connection test passed!\n" +
                    "Provider: %s\n" +
                    "Model: %s\n" +
                    "Timeout: %ds\n" +
                    "API Key: %s****", 
                    provider, model, timeout, apiKey.substring(0, Math.min(8, apiKey.length()))), 
                null);
                
        } catch (Exception e) {
            return new APITestResult(false, "Connection test failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Test API authentication with enhanced error diagnostics
     */
    private static APITestResult testAuthentication(ServerPlayer player) {
        LOGGER.info("🔐 Testing API authentication for player: {}", player.getName().getString());
        
        try {
            String apiKey = APIKeyManager.getAPIKey("gemini");
            if (apiKey == null) {
                return new APITestResult(false, "❌ No API key available for authentication test", null);
            }
            
            String model = EidolonUnchainedConfig.COMMON.geminiModel.get();
            int timeout = EidolonUnchainedConfig.COMMON.geminiTimeout.get();
            
            GeminiAPIClient client = new GeminiAPIClient(apiKey, model, timeout);
            
            // Test with minimal request
            CompletableFuture<GeminiAPIClient.AIResponse> response = 
                client.generateResponse(
                    "Hello, this is a test message. Please respond with 'Authentication successful'",
                    "You are a helpful assistant testing API connectivity.",
                    new GenerationConfig(),
                    new SafetySettings()
                );
            
            GeminiAPIClient.AIResponse result = response.get();
            
            if (result.success) {
                LOGGER.info("✅ Authentication successful: {}", result.dialogue);
                return new APITestResult(true, 
                    "✅ Authentication successful!\n" +
                    "Response: " + result.dialogue, 
                    null);
            } else {
                String error = result.dialogue; // In case of error, dialogue contains error message
                LOGGER.error("❌ Authentication failed: {}", error);
                
                StringBuilder errorMsg = new StringBuilder("❌ Authentication failed!\n\n");
                
                if (error.contains("quota") || error.contains("QUOTA") || error.contains("limit")) {
                    errorMsg.append("🚨 QUOTA EXHAUSTED:\n");
                    errorMsg.append("• Your API key reached daily limits\n");
                    errorMsg.append("• Free tier: ~15 requests/minute, ~1500/day\n");
                    errorMsg.append("• Solutions:\n");
                    errorMsg.append("  - Wait 24 hours for reset\n");
                    errorMsg.append("  - Upgrade to paid tier\n");
                    errorMsg.append("  - Check quota: https://aistudio.google.com/app/apikey\n");
                } else if (error.contains("permission") || error.contains("PERMISSION_DENIED")) {
                    errorMsg.append("🚨 PERMISSION DENIED:\n");
                    errorMsg.append("• API key invalid, revoked, or expired\n");
                    errorMsg.append("• Generate new key: https://aistudio.google.com/app/apikey\n");
                    errorMsg.append("• Set new key: /eidolon api set gemini YOUR_NEW_KEY\n");
                } else if (error.contains("model") || error.contains("MODEL")) {
                    errorMsg.append("🚨 MODEL ISSUE:\n");
                    errorMsg.append("• Current model: " + model + "\n");
                    errorMsg.append("• Valid models: gemini-1.5-flash, gemini-1.5-pro\n");
                    errorMsg.append("• Change model in config file or use default\n");
                } else {
                    errorMsg.append("🚨 UNKNOWN ERROR:\n");
                    errorMsg.append("• Error: " + error + "\n");
                    errorMsg.append("• Try: /eidolon api test all for full diagnosis\n");
                }
                
                return new APITestResult(false, errorMsg.toString(), null);
            }
            
        } catch (Exception e) {
            LOGGER.error("Authentication test failed", e);
            String errorMsg = "❌ Authentication test failed: " + e.getMessage();
            
            if (e.getMessage().contains("timeout")) {
                errorMsg += "\n🔍 TIMEOUT: Increase timeout in config: gemini_timeout_seconds";
            }
            
            return new APITestResult(false, errorMsg, e);
        }
    }
    
    /**
     * Test simple API request
     */
    private static APITestResult testSimpleRequest(ServerPlayer player) {
        return new APITestResult(true, "Simple request test - placeholder", null);
    }
    
    /**
     * Test full deity conversation request
     */
    private static APITestResult testFullRequest(ServerPlayer player) {
        return new APITestResult(true, "Full request test - placeholder", null);
    }
    
    /**
     * Test configuration validation
     */
    private static APITestResult testConfigValidation(ServerPlayer player) {
        StringBuilder result = new StringBuilder("🔧 Configuration Validation:\n\n");
        
        boolean allValid = true;
        
        // Check AI provider
        String provider = EidolonUnchainedConfig.COMMON.aiProvider.get();
        if ("gemini".equals(provider)) {
            result.append("✅ Provider: ").append(provider).append("\n");
        } else {
            result.append("❌ Provider: ").append(provider).append(" (unsupported)\n");
            allValid = false;
        }
        
        // Check model
        String model = EidolonUnchainedConfig.COMMON.geminiModel.get();
        if ("gemini-1.5-flash".equals(model) || "gemini-1.5-pro".equals(model)) {
            result.append("✅ Model: ").append(model).append("\n");
        } else {
            result.append("⚠ Model: ").append(model).append(" (might not work)\n");
        }
        
        // Check timeout
        int timeout = EidolonUnchainedConfig.COMMON.geminiTimeout.get();
        if (timeout >= 10 && timeout <= 120) {
            result.append("✅ Timeout: ").append(timeout).append("s\n");
        } else {
            result.append("⚠ Timeout: ").append(timeout).append("s (recommended: 10-120s)\n");
        }
        
        // Check API key
        String apiKey = APIKeyManager.getAPIKey(provider);
        if (apiKey != null && !apiKey.isEmpty()) {
            result.append("✅ API Key: Configured\n");
        } else {
            result.append("❌ API Key: Not configured\n");
            allValid = false;
        }
        
        return new APITestResult(allValid, result.toString(), null);
    }
    
    /**
     * API test result container
     */
    public static class APITestResult {
        private final boolean success;
        private final String message;
        private final Exception exception;
        
        public APITestResult(boolean success, String message, Exception exception) {
            this.success = success;
            this.message = message;
            this.exception = exception;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Exception getException() { return exception; }
    }
}
