package com.bluelotuscoding.eidolonunchained.integration.tts;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads custom TTS voice alias mappings from config/eidolon-unchained/tts_voices.json
 * Format:
 * {
 *   "aliases": { "male-deep-1": "01955d76-ed5b-...", "neutral-1": "..." }
 * }
 */
public class TTSVoiceRegistry {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final TTSVoiceRegistry INSTANCE = new TTSVoiceRegistry();

    private final Map<String, String> aliases = new HashMap<>();
    private boolean loaded = false;

    public static TTSVoiceRegistry getInstance() {
        return INSTANCE;
    }

    public synchronized void loadIfNeeded(File gameDir) {
        if (loaded) return;
        loaded = true;
        try {
            File cfgDir = new File(gameDir, "config/eidolon-unchained");
            File file = new File(cfgDir, "tts_voices.json");
            if (!file.exists()) {
                return; // Nothing to load
            }
            try (BufferedReader r = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
                String json = r.lines().reduce("", (a, b) -> a + b);
                JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                if (root.has("aliases") && root.get("aliases").isJsonObject()) {
                    JsonObject obj = root.getAsJsonObject("aliases");
                    for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                        String key = e.getKey();
                        String val = e.getValue().getAsString();
                        aliases.put(key, val);
                    }
                }
                LOGGER.info("Loaded {} TTS voice aliases", aliases.size());
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load TTS voice aliases: {}", e.getMessage());
        }
    }

    public Map<String, String> getAliases() {
        return Collections.unmodifiableMap(aliases);
    }

    public String resolve(String voiceId) {
        return aliases.getOrDefault(voiceId, voiceId);
    }
}
