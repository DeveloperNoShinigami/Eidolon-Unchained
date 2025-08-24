package com.bluelotuscoding.eidolonunchained.config;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Simple API key manager for testing and configuration
 */
public class APIKeyManager {
    private static final String CONFIG_DIR = "config/eidolonunchained";
    private static final String API_CONFIG_FILE = CONFIG_DIR + "/api-keys.properties";
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
    public static void setAPIKey(String provider, String key) {
        apiKeys.setProperty(provider.toLowerCase() + ".api_key", key);
        saveAPIKeys();
    }
    
    /**
     * Check if API key is configured for provider
     */
    public static boolean hasAPIKey(String provider) {
        String key = getAPIKey(provider);
        return key != null && !key.trim().isEmpty();
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
