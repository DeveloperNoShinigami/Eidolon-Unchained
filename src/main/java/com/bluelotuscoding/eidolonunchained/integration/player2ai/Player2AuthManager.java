package com.bluelotuscoding.eidolonunchained.integration.player2ai;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Manages Player2 Web API authentication using the Device Authorization Flow.
 * Stores per-player p2Key in-memory (not persisted) to comply with Player2 ToS.
 */
public class Player2AuthManager {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String DEVICE_NEW_URL = "https://api.player2.game/v1/login/device/new";
    private static final String DEVICE_TOKEN_URL = "https://api.player2.game/v1/login/device/token";
    private static final String GAME_CLIENT_ID = Player2SharedConfig.GAME_CLIENT_ID;

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    private static class DeviceFlowState {
        final String deviceCode;
        final int intervalSec;
        final Instant expiresAt;

    DeviceFlowState(String deviceCode, int intervalSec, int expiresInSec) {
            this.deviceCode = deviceCode;
            this.intervalSec = Math.max(5, intervalSec);
            this.expiresAt = Instant.now().plusSeconds(Math.max(60, expiresInSec));
        }

        boolean isExpired() { return Instant.now().isAfter(expiresAt); }
    }

    private static final ConcurrentMap<UUID, String> p2KeyByPlayer = new ConcurrentHashMap<>();
    private static final ConcurrentMap<UUID, DeviceFlowState> deviceFlowByPlayer = new ConcurrentHashMap<>();

    public static String getCachedP2Key(ServerPlayer player) {
        if (player == null) return null;
        return p2KeyByPlayer.get(player.getUUID());
    }

    /**
     * Get cached Player2 p2Key by player UUID (for contexts where we only have the UUID string).
     */
    public static String getCachedP2Key(UUID playerUUID) {
        if (playerUUID == null) return null;
        return p2KeyByPlayer.get(playerUUID);
    }

    public static void clearCachedP2Key(ServerPlayer player) {
        if (player != null) {
            p2KeyByPlayer.remove(player.getUUID());
        }
    }

    public static void setCachedP2Key(ServerPlayer player, String p2Key) {
        if (player != null && p2Key != null && !p2Key.isEmpty()) {
            p2KeyByPlayer.put(player.getUUID(), p2Key);
        }
    }

    public static boolean hasActiveDeviceFlow(ServerPlayer player) {
        if (player == null) return false;
        DeviceFlowState s = deviceFlowByPlayer.get(player.getUUID());
        return s != null && !s.isExpired();
    }

    public static class DeviceStart {
        public final boolean started;
        public final String message;
        public final String verificationUri;
        public final String verificationUriComplete;
        public final String userCode;
        public final int intervalSec;

        private DeviceStart(boolean started, String message, String verificationUri,
                            String verificationUriComplete, String userCode, int intervalSec) {
            this.started = started;
            this.message = message;
            this.verificationUri = verificationUri;
            this.verificationUriComplete = verificationUriComplete;
            this.userCode = userCode;
            this.intervalSec = intervalSec;
        }

        public static DeviceStart ok(String verificationUri, String verificationUriComplete, String userCode, int intervalSec) {
            return new DeviceStart(true, null, verificationUri, verificationUriComplete, userCode, intervalSec);
        }

        public static DeviceStart fail(String msg) { return new DeviceStart(false, msg, null, null, null, 0); }
    }

    /**
     * Start device flow: returns the URLs and code to display to the user.
     * Does not block. Use pollForToken to complete.
     */
    public static DeviceStart startDeviceFlow(ServerPlayer player) {
        try {
            URL url = URI.create(DEVICE_NEW_URL).toURL();
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);

            JsonObject req = new JsonObject();
            req.addProperty("client_id", GAME_CLIENT_ID);

            try (OutputStreamWriter w = new OutputStreamWriter(conn.getOutputStream())) {
                w.write(req.toString());
                w.flush();
            }

            int code = conn.getResponseCode();
            if (code != 200) {
                String err;
                try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                    err = r.lines().reduce("", (a, b) -> a + b);
                }
                return DeviceStart.fail("Device flow start failed (" + code + "): " + err);
            }

            String body;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                body = r.lines().reduce("", (a, b) -> a + b);
            }
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();

            String deviceCode = getString(json, "deviceCode", getString(json, "device_code", null));
            String verificationUri = getString(json, "verificationUri", getString(json, "verification_uri", null));
            String verificationUriComplete = getString(json, "verificationUriComplete", getString(json, "verification_uri_complete", null));
            String userCode = getString(json, "userCode", getString(json, "user_code", null));
            int interval = getInt(json, "interval", 5);
            int expiresIn = getInt(json, "expiresIn", 600);

            if (deviceCode == null || verificationUri == null) {
                return DeviceStart.fail("Malformed response from auth server");
            }

            DeviceFlowState state = new DeviceFlowState(deviceCode, interval, expiresIn);
            deviceFlowByPlayer.put(player.getUUID(), state);
            LOGGER.info("Started Player2 device flow for {} (interval={}s)", player.getGameProfile().getName(), interval);
            return DeviceStart.ok(verificationUri, verificationUriComplete, userCode, interval);

        } catch (Exception e) {
            LOGGER.warn("Failed to start device flow: {}", e.getMessage());
            return DeviceStart.fail("Device flow error: " + e.getMessage());
        }
    }

    /**
     * Poll the token endpoint once. Returns p2Key if approved, or null if pending/failed.
     */
    public static String pollForToken(ServerPlayer player) {
        try {
            DeviceFlowState state = deviceFlowByPlayer.get(player.getUUID());
            if (state == null || state.isExpired()) {
                if (state != null && state.isExpired()) {
                    // Notify expiry on main thread
                    var server = player != null && player.level() != null ? player.level().getServer() : null;
                    if (server != null) {
                        server.execute(() -> player.sendSystemMessage(
                            net.minecraft.network.chat.Component.literal("§cDevice login expired. Run /eidolon-unchained player2ai login device to try again.")));
                    }
                    deviceFlowByPlayer.remove(player.getUUID());
                }
                return null;
            }

            URL url = URI.create(DEVICE_TOKEN_URL).toURL();
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);

            JsonObject req = new JsonObject();
            req.addProperty("client_id", GAME_CLIENT_ID);
            // Use standard OAuth 2.0 Device Authorization Grant type
            // See RFC 8628: "urn:ietf:params:oauth:grant-type:device_code"
            req.addProperty("grant_type", "urn:ietf:params:oauth:grant-type:device_code");
            // API spec uses camelCase; be liberal and include both just in case
            req.addProperty("deviceCode", state.deviceCode);
            req.addProperty("device_code", state.deviceCode);

            try (OutputStreamWriter w = new OutputStreamWriter(conn.getOutputStream())) {
                w.write(req.toString());
                w.flush();
            }

            int code = conn.getResponseCode();
            if (code != 200) {
                // Pending or error. Treat 400/428 as pending; others log.
                try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                    String err = r.lines().reduce("", (a, b) -> a + b);
                    LOGGER.debug("Device token poll non-200 ({}): {}", code, err);
                } catch (Exception ignored) {}
                return null;
            }

            String body;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                body = r.lines().reduce("", (a, b) -> a + b);
            }
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            // Accept multiple potential token field names per API variants
            String p2Key = getString(json, "p2Key",
                    getString(json, "access_token",
                        getString(json, "token", null)));
            if (p2Key != null && !p2Key.isEmpty()) {
                p2KeyByPlayer.put(player.getUUID(), p2Key);
                deviceFlowByPlayer.remove(player.getUUID());
                LOGGER.info("Player2 device flow completed for {}", player.getGameProfile().getName());
                // Notify success on main thread
                var server = player != null && player.level() != null ? player.level().getServer() : null;
                if (server != null) {
                    server.execute(() -> player.sendSystemMessage(
                        net.minecraft.network.chat.Component.literal("§aLogin approved! TTS web API ready.")));
                }
                return p2Key;
            }
            return null;
        } catch (Exception e) {
            LOGGER.warn("Device token poll error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Start background polling until success or expiry.
     */
    public static void beginBackgroundPolling(ServerPlayer player) {
        DeviceFlowState state = deviceFlowByPlayer.get(player.getUUID());
        if (state == null) return;
        EXECUTOR.submit(() -> {
            try {
                while (!state.isExpired() && getCachedP2Key(player) == null) {
                    String k = pollForToken(player);
                    if (k != null) break;
                    TimeUnit.SECONDS.sleep(state.intervalSec);
                }
                if (state.isExpired() && getCachedP2Key(player) == null) {
                    // Expired without approval, ensure the user sees it
                    var server = player != null && player.level() != null ? player.level().getServer() : null;
                    if (server != null) {
                        server.execute(() -> player.sendSystemMessage(
                            net.minecraft.network.chat.Component.literal("§eDevice login timed out. Please run /eidolon-unchained player2ai login device again.")));
                    }
                }
            } catch (InterruptedException ignored) {}
        });
    }

    private static String getString(JsonObject o, String key, String def) {
        try { return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : def; } catch (Exception e) { return def; }
    }
    private static int getInt(JsonObject o, String key, int def) {
        try { return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsInt() : def; } catch (Exception e) { return def; }
    }
}
