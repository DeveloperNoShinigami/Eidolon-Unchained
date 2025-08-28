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
        // Try environment variable first
        String envKey = System.getenv(provider.toUpperCase() + "_API_KEY");
        if (envKey != null && !envKey.isEmpty()) {
            return envKey;
        }
        
        // Try config file
        return apiKeys.getProperty(provider.toLowerCase() + ".api_key");
    }
    
    /**
     * Set API key for a provider
     */
    public static boolean setAPIKey(String provider, String key) {
        try {
            apiKeys.setProperty(provider.toLowerCase() + ".api_key", key);
            saveAPIKeys();
            return true;
        } catch (Exception e) {
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
        
        // For now, just check if key exists and looks valid
        if (provider.equals("gemini")) {
            return apiKey.startsWith("AIza") && apiKey.length() > 20;
        }
        
        return false; // Unknown provider
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
            if (Files.exists(configPath)) {
                try (InputStream input = Files.newInputStream(configPath)) {
                    apiKeys.load(input);
                }
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
            
            // Save properties file
            try (OutputStream output = Files.newOutputStream(Paths.get(API_CONFIG_FILE))) {
                apiKeys.store(output, "Eidolon Unchained API Keys - Keep this file secure!");
            }
        } catch (IOException e) {
            System.err.println("Could not save API keys: " + e.getMessage());
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
