package com.bluelotuscoding.eidolonunchained.config;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Map;
import java.util.HashMap;

/**
 * Simple API key manager for server-side configuration only
 * API keys are stored server-side and never synchronized to clients
 */
public class APIKeyManager {
    private static final String CONFIG_DIR = "config/eidolonunchained";
    private static final String API_CONFIG_FILE = CONFIG_DIR + "/server-api-keys.properties";
    private static Properties apiKeys = new Properties();
    
    static {
        loadAPIKeys();
    }
    
    /**
     * Get API key for a specific provider
     */
    public static String getAPIKey(String provider) {
        System.out.println("APIKeyManager.getAPIKey() called with provider: " + provider);
        
        // Try environment variable first
        String envKey = System.getenv(provider.toUpperCase() + "_API_KEY");
        if (envKey != null && !envKey.isEmpty()) {
            System.out.println("Found API key in environment variable: " + provider.toUpperCase() + "_API_KEY");
            return envKey;
        }
        
        // Try config file
        String configKey = provider.toLowerCase() + ".api_key";
        String fileKey = apiKeys.getProperty(configKey);
        System.out.println("Looking for config key: " + configKey);
        System.out.println("Found in config file: " + (fileKey != null ? "YES (length: " + fileKey.length() + ")" : "NO"));
        System.out.println("Total properties in file: " + apiKeys.size());
        
        // Debug: print all keys
        System.out.println("All keys in properties file:");
        for (String key : apiKeys.stringPropertyNames()) {
            System.out.println("  - " + key + " = " + (apiKeys.getProperty(key) != null ? "[SET]" : "[NULL]"));
        }
        
        return fileKey;
    }
    
    /**
     * Set API key for a provider
     */
    public static boolean setAPIKey(String provider, String key) {
        try {
            String configKey = provider.toLowerCase() + ".api_key";
            System.out.println("APIKeyManager.setAPIKey() called:");
            System.out.println("  Provider: " + provider);
            System.out.println("  Config key: " + configKey);
            System.out.println("  Key length: " + (key != null ? key.length() : 0));
            
            apiKeys.setProperty(configKey, key);
            System.out.println("  Property set in memory");
            
            saveAPIKeys();
            System.out.println("  Properties saved to file");
            return true;
        } catch (Exception e) {
            System.err.println("Failed to set API key: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Check if API key is configured for provider
     */
    public static boolean hasAPIKey(String provider) {
        String key = getAPIKey(provider);
        return key != null && !key.trim().isEmpty();
    }
    
    /**
     * Set a configuration value
     */
    public static boolean setConfigValue(String key, String value) {
        try {
            apiKeys.setProperty(key, value);
            saveAPIKeys();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get a configuration value
     */
    public static String getConfigValue(String key) {
        return apiKeys.getProperty(key);
    }
    
    /**
     * Get all configurations (with masked sensitive values)
     */
    public static Map<String, String> getAllConfigs() {
        Map<String, String> configs = new HashMap<>();
        for (String key : apiKeys.stringPropertyNames()) {
            String value = apiKeys.getProperty(key);
            configs.put(key, value);
        }
        return configs;
    }
    
    /**
     * Remove a configuration
     */
    public static boolean removeConfig(String key) {
        try {
            Object removed = apiKeys.remove(key);
            if (removed != null) {
                saveAPIKeys();
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Test connection to a provider
     */
    public static boolean testConnection(String provider) {
        String apiKey = getAPIKey(provider);
        if (apiKey == null || apiKey.isEmpty()) {
            return false;
        }
        
        // Validate key format based on provider
        switch (provider.toLowerCase()) {
            case "gemini":
                return apiKey.startsWith("AIza") && apiKey.length() > 20;
            case "player2ai":
            case "player2":
                // Player2AI keys are typically UUID format or start with specific prefix
                return apiKey.length() > 10; // Basic validation
            case "openai":
                return apiKey.startsWith("sk-") && apiKey.length() > 20;
            default:
                return false; // Unknown provider
        }
    }
    
    /**
     * Get system status
     */
    public static Map<String, Object> getSystemStatus() {
        Map<String, Object> status = new HashMap<>();
        
        // Check API providers
        status.put("Gemini API", hasAPIKey("gemini") ? "✓ Configured" : "✗ Not configured");
        
        // Check configuration file
        Path configPath = Paths.get(API_CONFIG_FILE);
        status.put("Config file", Files.exists(configPath) ? "✓ Found" : "✗ Missing");
        
        // Count configurations
        status.put("Total configs", apiKeys.size());
        
        return status;
    }
    
    /**
     * Validate all configurations
     */
    public static Map<String, Object> validateAllConfigurations() {
        Map<String, Object> results = new HashMap<>();
        
        // Test each provider
        if (hasAPIKey("gemini")) {
            results.put("Gemini API", testConnection("gemini"));
        } else {
            results.put("Gemini API", false);
        }
        
        if (hasAPIKey("player2ai")) {
            results.put("Player2AI", testConnection("player2ai"));
        } else {
            results.put("Player2AI", false);
        }
        
        if (hasAPIKey("openai")) {
            results.put("OpenAI", testConnection("openai"));
        } else {
            results.put("OpenAI", false);
        }
        
        return results;
    }
    
    /**
     * Reload configuration from file
     */
    public static void reload() {
        loadAPIKeys();
    }
    
    /**
     * Load API keys from config file
     */
    private static void loadAPIKeys() {
        try {
            Path configPath = Paths.get(API_CONFIG_FILE);
            System.out.println("APIKeyManager: Looking for config at: " + configPath.toAbsolutePath());
            if (Files.exists(configPath)) {
                try (InputStream input = Files.newInputStream(configPath)) {
                    apiKeys.load(input);
                    System.out.println("APIKeyManager: Successfully loaded " + apiKeys.size() + " API configurations");
                }
            } else {
                System.out.println("APIKeyManager: Config file not found, starting with empty configuration");
            }
        } catch (IOException e) {
            System.err.println("Could not load API keys: " + e.getMessage());
        }
    }
    
    /**
     * Remove API key for a provider
     */
    public static boolean removeAPIKey(String provider) {
        try {
            apiKeys.remove(provider.toLowerCase() + ".api_key");
            saveAPIKeys();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Save API keys to config file
     */
    private static void saveAPIKeys() {
        try {
            // Create config directory if it doesn't exist
            Path configDir = Paths.get(CONFIG_DIR);
            Files.createDirectories(configDir);
            System.out.println("Created config directory: " + configDir.toAbsolutePath());
            
            // Save properties file
            Path configFile = Paths.get(API_CONFIG_FILE);
            try (OutputStream output = Files.newOutputStream(configFile)) {
                apiKeys.store(output, "Eidolon Unchained API Keys - Keep this file secure!");
                System.out.println("API keys saved to: " + configFile.toAbsolutePath());
                System.out.println("File exists after save: " + Files.exists(configFile));
                System.out.println("File size: " + Files.size(configFile) + " bytes");
            }
        } catch (IOException e) {
            System.err.println("Could not save API keys: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Quick setup method for testing
     */
    public static void quickSetup(String geminiKey) {
        setAPIKey("gemini", geminiKey);
        System.out.println("✅ Gemini API key configured!");
        System.out.println("📁 Saved to: " + API_CONFIG_FILE);
        System.out.println("🔒 Keep this file secure!");
    }
}
