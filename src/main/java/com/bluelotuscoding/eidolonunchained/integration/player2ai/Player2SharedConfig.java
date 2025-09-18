package com.bluelotuscoding.eidolonunchained.integration.player2ai;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.HttpURLConnection;
import java.util.UUID;

/**
 * Shared constants and helpers for Player2 integrations (AI + TTS + Auth).
 * Ensures a single source of truth for client ID and header wiring.
 */
public final class Player2SharedConfig {
    private static final Logger LOGGER = LogManager.getLogger();

    // Canonical verified Game Client ID for Eidolon Unchained
    public static final String GAME_CLIENT_ID = "0198fed4-2d7d-7acf-aaf0-2bdb36a74eba";

    // Standard header names supported by Player2 for game header auth
    public static final String HEADER_GAME_CLIENT_ID = "X-Game-Client-ID";
    public static final String HEADER_PLAYER_UUID = "X-Player-UUID";

    // Legacy header some local apps may still accept; keep for compatibility on localhost only
    public static final String HEADER_LEGACY_GAME_KEY = "player2-game-key";

    private Player2SharedConfig() {}

    /**
     * Apply standard game headers for Player2 APIs.
     * Adds X-Game-Client-ID and X-Player-UUID (when provided).
     */
    public static void applyGameHeaders(HttpURLConnection conn, UUID playerUuid) {
        try {
            conn.setRequestProperty(HEADER_GAME_CLIENT_ID, GAME_CLIENT_ID);
            if (playerUuid != null) {
                conn.setRequestProperty(HEADER_PLAYER_UUID, playerUuid.toString());
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to set Player2 game headers: {}", e.getMessage());
        }
    }
}
