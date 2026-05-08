package com.bluelotuscoding.eidolonunchained.integration.player2ai;

import com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Player2AI Health Signal System
 * Required by Player2AI Jam submission rules:
 * "Your game must send a health signal once every 60 seconds"
 * 
 * This ensures our game meets the API compliance requirements for the Player2AI game jam.
 * Uses local Player2AI desktop app connection only.
 */
public class Player2HealthSignal {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String HEALTH_ENDPOINT = "http://127.0.0.1:4315/v1/health"; // Local health endpoint
    private static final int HEALTH_SIGNAL_INTERVAL = 60; // seconds
    
    private static transient ScheduledExecutorService healthSignalExecutor;
    private static boolean healthSignalActive = false;
    private static int consecutiveFailures = 0;
    private static boolean warnedOnce = false;
    
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

        // Do not start if the local Player2 app is not available to avoid log spam
        try {
            if (!Player2AIClient.isPlayer2AppAvailable()) {
                LOGGER.info("Player2AI app not detected locally; health signal will not start (will start when Player2 becomes available)");
                return;
            }
        } catch (Throwable t) {
            // If any unexpected error occurs during availability check, do not start
            LOGGER.debug("Skipping health signal due to availability check error: {}", t.getMessage());
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
                // Rare path; sendHealthSignal already handles logging/backoff
                LOGGER.debug("Health signal runnable error: {}", e.getMessage());
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
    warnedOnce = false;
    consecutiveFailures = 0;
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
            
            // Configure request - Use GET for health endpoints (most common pattern)
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            // Apply standard game headers (X-Game-Client-ID, optional X-Player-UUID)
            Player2SharedConfig.applyGameHeaders(connection, null);
            connection.setConnectTimeout(10000); // 10 second timeout
            connection.setReadTimeout(10000);
            
            // Add API key if we have one
            String apiKey = com.bluelotuscoding.eidolonunchained.config.APIKeyManager.getAPIKey("player2ai");
            if (apiKey != null && !apiKey.trim().isEmpty() && 
                !apiKey.contains("localhost") && !apiKey.contains("127.0.0.1")) {
                connection.setRequestProperty("X-API-Key", apiKey);
            }
            
            // For GET request, no body is needed - just connect
            
            // Check response
            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                LOGGER.debug("Health signal sent successfully (response: {})", responseCode);
                // Reset failure tracking on success
                consecutiveFailures = 0;
                warnedOnce = false;
            } else {
                consecutiveFailures++;
                if (!warnedOnce) {
                    LOGGER.warn("Health signal failed with response code: {}", responseCode);
                    warnedOnce = true;
                } else {
                    LOGGER.debug("Health signal failed ({}), consecutiveFailures={}", responseCode, consecutiveFailures);
                }
            }
            
        } catch (Exception e) {
            consecutiveFailures++;
            String msg = e.getMessage() == null ? e.toString() : e.getMessage();
            if (!warnedOnce) {
                LOGGER.warn("Failed to send health signal: {}", msg);
                warnedOnce = true;
            } else {
                LOGGER.debug("Failed to send health signal: {} (suppressed)", msg);
            }
            // If repeated failures, stop the health signal to avoid log spam and retry cost
            if (consecutiveFailures >= 3) {
                LOGGER.info("Player2AI health signal disabled after {} consecutive failures; will remain off until Player2 is available and provider is activated again.", consecutiveFailures);
                stopHealthSignal();
            }
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
