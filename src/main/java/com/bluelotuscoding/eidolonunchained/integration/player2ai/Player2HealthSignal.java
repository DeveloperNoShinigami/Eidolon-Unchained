package com.bluelotuscoding.eidolonunchained.integration.player2ai;

import com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Player2AI Health Signal System
 * Required by Player2AI Jam submission rules:
 * "Your game must send a health signal once every 60 seconds"
 * 
 * This ensures our game meets the API compliance requirements for the Player2AI game jam.
 */
public class Player2HealthSignal {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String HEALTH_ENDPOINT = "https://api.player2.game/v1/health";
    private static final String GAME_CLIENT_ID = "eidolon-unchained"; // Player2AI game client ID
    private static final int HEALTH_SIGNAL_INTERVAL = 60; // seconds
    
    private static ScheduledExecutorService healthSignalExecutor;
    private static boolean healthSignalActive = false;
    
    /**
     * Start the health signal system
     * Called when Player2AI is activated as the AI provider
     */
    public static synchronized void startHealthSignal() {
        if (healthSignalActive) {
            LOGGER.debug("Health signal already active");
            return;
        }
        
        // Only start if Player2AI is the active provider
        String aiProvider = EidolonUnchainedConfig.COMMON.aiProvider.get();
        if (!"player2ai".equals(aiProvider)) {
            LOGGER.debug("Player2AI not active, skipping health signal");
            return;
        }
        
        healthSignalExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Player2AI-HealthSignal");
            t.setDaemon(true);
            return t;
        });
        
        // Send initial health signal immediately
        sendHealthSignal();
        
        // Schedule recurring health signals every 60 seconds
        healthSignalExecutor.scheduleAtFixedRate(() -> {
            try {
                sendHealthSignal();
            } catch (Exception e) {
                LOGGER.warn("Health signal failed: {}", e.getMessage());
            }
        }, HEALTH_SIGNAL_INTERVAL, HEALTH_SIGNAL_INTERVAL, TimeUnit.SECONDS);
        
        healthSignalActive = true;
        LOGGER.info("Player2AI health signal started (60 second interval)");
    }
    
    /**
     * Stop the health signal system
     * Called when Player2AI is deactivated or mod shuts down
     */
    public static synchronized void stopHealthSignal() {
        if (!healthSignalActive) {
            return;
        }
        
        if (healthSignalExecutor != null && !healthSignalExecutor.isShutdown()) {
            healthSignalExecutor.shutdown();
            try {
                if (!healthSignalExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    healthSignalExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                healthSignalExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        healthSignalActive = false;
        LOGGER.info("Player2AI health signal stopped");
    }
    
    /**
     * Send a single health signal to Player2AI API
     * This tells Player2AI that our game is actively using their service
     */
    private static void sendHealthSignal() {
        try {
            URL url = URI.create(HEALTH_ENDPOINT).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // Configure request
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("player2-game-key", GAME_CLIENT_ID);
            connection.setDoOutput(true);
            connection.setConnectTimeout(10000); // 10 second timeout
            connection.setReadTimeout(10000);
            
            // Add API key if we have one
            String apiKey = EidolonUnchainedConfig.COMMON.player2aiApiKey.get();
            if (apiKey != null && !apiKey.trim().isEmpty() && 
                !apiKey.contains("localhost") && !apiKey.contains("127.0.0.1")) {
                connection.setRequestProperty("X-API-Key", apiKey);
            }
            
            // Send health signal payload
            JsonObject payload = new JsonObject();
            payload.addProperty("game_client_id", GAME_CLIENT_ID);
            payload.addProperty("status", "active");
            payload.addProperty("timestamp", System.currentTimeMillis());
            
            try (OutputStreamWriter writer = new OutputStreamWriter(
                connection.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(payload.toString());
                writer.flush();
            }
            
            // Check response
            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                LOGGER.debug("Health signal sent successfully (response: {})", responseCode);
            } else {
                LOGGER.warn("Health signal failed with response code: {}", responseCode);
            }
            
        } catch (Exception e) {
            LOGGER.warn("Failed to send health signal: {}", e.getMessage());
        }
    }
    
    /**
     * Check if health signal is currently active
     */
    public static boolean isHealthSignalActive() {
        return healthSignalActive;
    }
    
    /**
     * Get the configured health signal interval
     */
    public static int getHealthSignalInterval() {
        return HEALTH_SIGNAL_INTERVAL;
    }
}
