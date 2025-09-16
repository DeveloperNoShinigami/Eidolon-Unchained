package com.bluelotuscoding.eidolonunchained.integration.voicechat;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStoppedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Simple Voice Chat plugin. This is only loaded if the Simple Voice Chat mod and API are present.
 * We keep it minimal for now: mark availability and prepare for future event registration.
 */
@ForgeVoicechatPlugin
public class EidolonVoiceChatPlugin implements VoicechatPlugin {
    private static final Logger LOGGER = LogManager.getLogger();
    // Throttle per-player action bar updates to avoid spamming on every mic packet
    private static final Map<UUID, Long> LAST_LISTEN_NOTIFY = new ConcurrentHashMap<>();
    private static final long LISTEN_NOTIFY_COOLDOWN_MS = 1500L;

    // Try to get UUID from API player across versions (getUuid vs getUUID)
    private static UUID getApiPlayerUuid(Object apiPlayer) {
        if (apiPlayer == null) return null;
        try {
            try {
                var m = apiPlayer.getClass().getMethod("getUuid");
                return (UUID) m.invoke(apiPlayer);
            } catch (NoSuchMethodException nsme1) {
                try {
                    var m = apiPlayer.getClass().getMethod("getUUID");
                    return (UUID) m.invoke(apiPlayer);
                } catch (NoSuchMethodException nsme2) {
                    LOGGER.debug("VoiceChat API player UUID method not found (getUuid/getUUID)");
                }
            }
        } catch (Throwable reflect) {
            LOGGER.debug("VoiceChat API UUID reflection failed: {}", reflect.getMessage());
        }
        return null;
    }

    @Override
    public String getPluginId() {
        return "eidolonunchained";
    }

    @Override
    public void initialize(VoicechatApi api) {
        // API is available; mark integration as present
        VoiceChatIntegration.setAvailable(true);
    // Store API handle in an Object-safe way (no compile-time dependency in facade)
    VoiceChatIntegration.setApi(api);
        LOGGER.info("Simple Voice Chat API detected; voice features enabled");
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        // Capture server API on lifecycle
        registration.registerEvent(VoicechatServerStartedEvent.class, evt -> {
            try {
                Object serverApi = evt.getVoicechat();
                VoiceChatIntegration.setServerApi(serverApi);
                LOGGER.info("Voice chat server API available");
            } catch (Throwable t) {
                LOGGER.debug("Failed to capture voice chat server API: {}", t.getMessage());
            }
        });

        registration.registerEvent(VoicechatServerStoppedEvent.class, evt -> {
            VoiceChatIntegration.setServerApi(null);
            LOGGER.info("Voice chat server API cleared (stopped)");
        });

        // Minimal proof-of-life: detect player speech during active deity conversations
        registration.registerEvent(MicrophonePacketEvent.class, event -> {
            try {
                var sender = event.getSenderConnection();
                if (sender == null || sender.getPlayer() == null) return;
        // Resolve UUID across API versions
                final UUID playerUuid = getApiPlayerUuid(sender.getPlayer());
                if (playerUuid == null) return;

                net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
                if (server == null) return;

                // Ensure all Minecraft interactions happen on the server thread
                server.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            var player = server.getPlayerList().getPlayer(playerUuid);
                            if (player == null) return;

                            boolean inConversation = com.bluelotuscoding.eidolonunchained.chat.DeityChat.isInConversation(player);
                            if (!inConversation) {
                                LAST_LISTEN_NOTIFY.remove(playerUuid);
                                return;
                            }

                            // Only show voice listening indicator if TTS is enabled for this player
                            com.bluelotuscoding.eidolonunchained.ai.TTSManager ttsManager = com.bluelotuscoding.eidolonunchained.ai.TTSManager.getInstance();
                            com.bluelotuscoding.eidolonunchained.ai.TTSManager.TTSSettings settings = ttsManager.getPlayerSettings(player);
                            if (!settings.enabled) {
                                LAST_LISTEN_NOTIFY.remove(playerUuid);
                                return; // Don't show voice indicators when TTS is disabled
                            }

                            long now = System.currentTimeMillis();
                            long last = LAST_LISTEN_NOTIFY.getOrDefault(playerUuid, 0L);
                            if (now - last < LISTEN_NOTIFY_COOLDOWN_MS) return; // throttle updates

                            LAST_LISTEN_NOTIFY.put(playerUuid, now);
                            // Hint that voice input is detected. STT wiring to follow.
                            player.displayClientMessage(net.minecraft.network.chat.Component.literal("§7(voice) §eListening..."), true);
                        } catch (Throwable inner) {
                            LOGGER.debug("Voice chat server-thread hook error: {}", inner.getMessage());
                        }
                    }
                });
            } catch (Throwable t) {
                // Never break voice chat; just log
                LOGGER.debug("Voice chat hook error: {}", t.getMessage());
            }
        });
    }
}
