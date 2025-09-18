package com.bluelotuscoding.eidolonunchained.integration.googletts;

import com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig;
import com.google.gson.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Minimal Google Cloud Text-to-Speech client using service account JWT flow (no external deps).
 */
public class GoogleTTSClient {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final transient Executor EXECUTOR = Executors.newCachedThreadPool();

    public static class VoiceInfo {
        public final String id;
        public VoiceInfo(String id) { this.id = id; }
    }

    public static class TTSRequest {
        public final String text;
        public final List<String> voiceIds; // first entry used as name when present
        public final double speed;
        public final String audioFormat; // mp3|wav|opus|pcm|flac
        public final String voiceGender; // male|female|other
        public final String voiceLanguage; // en_US etc.

        public TTSRequest(String text, List<String> voiceIds, double speed, String audioFormat,
                          String voiceGender, String voiceLanguage) {
            this.text = text;
            this.voiceIds = voiceIds;
            this.speed = speed;
            this.audioFormat = audioFormat;
            this.voiceGender = voiceGender;
            this.voiceLanguage = voiceLanguage;
        }
    }

    public static class TTSResponse {
        public final boolean success;
        public final String audioUrl;  // not used here
        public final byte[] audioData;
        public final String error;

        private TTSResponse(boolean success, String audioUrl, byte[] audioData, String error) {
            this.success = success; this.audioUrl = audioUrl; this.audioData = audioData; this.error = error;
        }

        public static TTSResponse successData(byte[] data){ return new TTSResponse(true, null, data, null);}
        public static TTSResponse failure(String error){ return new TTSResponse(false, null, null, error);}
    }

    private static class TokenCache {
        String accessToken;
        long expiresAtMillis;
    }

    private final TokenCache tokenCache = new TokenCache();

    public CompletableFuture<List<VoiceInfo>> listVoices() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String token = getAccessToken();
                if (token == null) return Collections.emptyList();
                URL url = URI.create("https://texttospeech.googleapis.com/v1/voices").toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(30000);
                int code = conn.getResponseCode();
                if (code == 200) {
                    try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String body = r.lines().reduce("", (a,b)->a+b);
                        JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                        JsonArray arr = json.getAsJsonArray("voices");
                        List<VoiceInfo> out = new ArrayList<>();
                        if (arr != null) {
                            for (JsonElement el : arr) {
                                JsonObject v = el.getAsJsonObject();
                                String name = v.has("name") ? v.get("name").getAsString() : null;
                                if (name != null) out.add(new VoiceInfo(name));
                            }
                        }
                        return out;
                    }
                } else {
                    LOGGER.warn("Google voices failed: HTTP {}", code);
                }
            } catch (Exception e) {
                LOGGER.error("Google listVoices error: {}", e.getMessage());
            }
            return Collections.emptyList();
        }, EXECUTOR);
    }

    public CompletableFuture<TTSResponse> speak(TTSRequest req) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String token = getAccessToken();
                if (token == null) return TTSResponse.failure("Missing Google credentials or token");

                URL url = URI.create("https://texttospeech.googleapis.com/v1/text:synthesize").toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(60000);

                JsonObject payload = new JsonObject();
                JsonObject input = new JsonObject();
                input.addProperty("text", req.text);
                payload.add("input", input);

                JsonObject voice = new JsonObject();
                String langCode = deriveLanguageCode(req.voiceLanguage);
                if (!langCode.isEmpty()) voice.addProperty("languageCode", langCode);
                String name = (req.voiceIds != null && !req.voiceIds.isEmpty()) ? req.voiceIds.get(0) : defaultVoiceName();
                if (name != null && !name.isBlank()) voice.addProperty("name", name);
                String gender = mapGender(req.voiceGender);
                if (gender != null) voice.addProperty("ssmlGender", gender);
                payload.add("voice", voice);

                JsonObject audio = new JsonObject();
                audio.addProperty("audioEncoding", mapAudioEncoding(req.audioFormat));
                if (req.speed > 0) audio.addProperty("speakingRate", req.speed);
                payload.add("audioConfig", audio);

                String body = payload.toString();
                try (OutputStreamWriter w = new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8)) {
                    w.write(body);
                }

                int code = conn.getResponseCode();
                InputStream is = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
                if (is == null) return TTSResponse.failure("Empty response");
                try (BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String resp = r.lines().reduce("", (a,b)->a+b);
                    if (code == 200) {
                        JsonObject json = JsonParser.parseString(resp).getAsJsonObject();
                        if (json.has("audioContent")) {
                            String b64 = json.get("audioContent").getAsString();
                            byte[] data = Base64.getDecoder().decode(b64);
                            return TTSResponse.successData(data);
                        }
                        return TTSResponse.failure("No audioContent in response");
                    } else {
                        return TTSResponse.failure("HTTP " + code + ": " + resp);
                    }
                }
            } catch (Exception e) {
                return TTSResponse.failure("Speak error: " + e.getMessage());
            }
        }, EXECUTOR);
    }

    private static String mapAudioEncoding(String fmt) {
        if (fmt == null) return "MP3";
        switch (fmt.toLowerCase(Locale.ROOT)) {
            case "mp3": return "MP3";
            case "wav": return "LINEAR16";
            case "pcm": return "LINEAR16";
            case "opus": return "OGG_OPUS";
            default: return "MP3"; // flac and others fallback to mp3
        }
    }

    private static String mapGender(String g) {
        if (g == null) return null;
        switch (g.toLowerCase(Locale.ROOT)) {
            case "male": return "MALE";
            case "female": return "FEMALE";
            case "other": return "NEUTRAL";
            default: return null;
        }
    }

    private static String deriveLanguageCode(String cfg) {
        // Prefer explicit Google config if set
        String googleLang = EidolonUnchainedConfig.GOOGLE_TTS_DEFAULT_LANGUAGE_CODE.get();
        if (googleLang != null && !googleLang.isBlank()) return googleLang;

        // Convert en_US -> en-US style
        String base = (cfg != null && !cfg.isBlank()) ? cfg : EidolonUnchainedConfig.TTS_DEFAULT_LANGUAGE.get();
        if (base == null || base.isBlank()) return "en-US";
        return base.replace('_','-');
    }

    private static String defaultVoiceName() {
        String v = EidolonUnchainedConfig.GOOGLE_TTS_DEFAULT_VOICE_NAME.get();
        return (v == null || v.isBlank()) ? null : v;
    }

    private String getAccessToken() {
        try {
            long now = System.currentTimeMillis();
            if (tokenCache.accessToken != null && now < tokenCache.expiresAtMillis - 60_000) {
                return tokenCache.accessToken;
            }

            String path = EidolonUnchainedConfig.GOOGLE_TTS_CREDENTIALS_PATH.get();
            if (path == null || path.isBlank()) {
                LOGGER.warn("Google TTS credentials path is not configured");
                return null;
            }
            File f = new File(path);
            if (!f.exists()) {
                LOGGER.warn("Google TTS credentials file not found: {}", f.getAbsolutePath());
                return null;
            }
            String json;
            try (BufferedReader r = new BufferedReader(new FileReader(f, StandardCharsets.UTF_8))) {
                json = r.lines().reduce("", (a,b)->a+b);
            }
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            String clientEmail = obj.get("client_email").getAsString();
            String privateKeyPem = obj.get("private_key").getAsString();
            String tokenUri = obj.has("token_uri") ? obj.get("token_uri").getAsString() : "https://oauth2.googleapis.com/token";

            String assertion = createJwtAssertion(clientEmail, tokenUri, privateKeyPem);
            URL url = URI.create(tokenUri).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoOutput(true);
            String form = "grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=" +
                    java.net.URLEncoder.encode(assertion, StandardCharsets.UTF_8.name());
            try (OutputStream os = conn.getOutputStream()) {
                os.write(form.getBytes(StandardCharsets.UTF_8));
            }
            int code = conn.getResponseCode();
            InputStream is = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
            if (is == null) return null;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String body = r.lines().reduce("", (a,b)->a+b);
                if (code == 200) {
                    JsonObject tok = JsonParser.parseString(body).getAsJsonObject();
                    String access = tok.get("access_token").getAsString();
                    int expires = tok.get("expires_in").getAsInt();
                    tokenCache.accessToken = access;
                    tokenCache.expiresAtMillis = System.currentTimeMillis() + (expires * 1000L);
                    return access;
                } else {
                    LOGGER.warn("Google OAuth token failed: HTTP {}: {}", code, body);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Google OAuth error: {}", e.getMessage());
        }
        return null;
    }

    private static String createJwtAssertion(String clientEmail, String tokenUri, String privateKeyPem) throws Exception {
        long nowSec = Instant.now().getEpochSecond();
        long expSec = nowSec + 3600; // 1 hour

        String headerJson = "{\"alg\":\"RS256\",\"typ\":\"JWT\"}";
        String scope = "https://www.googleapis.com/auth/cloud-platform";
        String claimsJson = new JsonObjectBuilder()
                .add("iss", clientEmail)
                .add("scope", scope)
                .add("aud", tokenUri)
                .add("iat", nowSec)
                .add("exp", expSec)
                .build();

        String headerB64 = base64Url(headerJson.getBytes(StandardCharsets.UTF_8));
        String claimsB64 = base64Url(claimsJson.getBytes(StandardCharsets.UTF_8));
        String signingInput = headerB64 + "." + claimsB64;
        byte[] signature = signRs256(signingInput.getBytes(StandardCharsets.UTF_8), parsePrivateKey(privateKeyPem));
        String sigB64 = base64Url(signature);
        return signingInput + "." + sigB64;
    }

    private static byte[] signRs256(byte[] data, PrivateKey key) throws Exception {
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(key);
        sig.update(data);
        return sig.sign();
    }

    private static PrivateKey parsePrivateKey(String pem) throws Exception {
        String cleaned = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] pkcs8 = Base64.getDecoder().decode(cleaned);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(pkcs8);
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    private static String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // Minimal JSON builder to avoid adding libs
    private static class JsonObjectBuilder {
        private final JsonObject obj = new JsonObject();
        JsonObjectBuilder add(String k, String v) { obj.addProperty(k, v); return this; }
        JsonObjectBuilder add(String k, long v) { obj.addProperty(k, v); return this; }
        String build() { return obj.toString(); }
    }
}