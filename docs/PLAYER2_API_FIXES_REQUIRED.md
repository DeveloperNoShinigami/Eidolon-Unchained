# Player2.game API Implementation Fixes Required

## Overview

Analysis of the actual Player2.game API specification (`api.yaml`) reveals several discrepancies with the current TTS implementation. This document outlines all required changes to properly integrate with the official Player2.game API.

## ❌ Current Implementation Issues

### 1. Incorrect API Endpoints
**Current Implementation:**
```java
private static final String PLAYER2_TTS_SPEAK = "https://api.player2.game/v1/tts/speak";
private static final String PLAYER2_AUTH_API = "https://api.player2.game/v1/auth";
```

**✅ Correct API Base URL (from api.yaml):**
```yaml
servers:
  - url: https://api.player2.game/v1
```

**Issue:** The current implementation is correct for the base URL, but authentication and request structure need fixes.

### 2. Wrong Request Structure
**Current Implementation:**
```java
// Incorrect structure - missing required fields
ttsRequest.addProperty("text", text);
ttsRequest.addProperty("audio_format", "mp3");
if (voice != null) {
    JsonArray ids = new JsonArray();
    ids.add(voice);
    ttsRequest.add("voice_ids", ids);
}
```

**✅ Correct Request Structure (from api.yaml):**
```yaml
TTSSpeakRequest:
  type: object
  required:
    - text
    - speed        # ❌ MISSING - REQUIRED!
    - audio_format # ✅ Present
  properties:
    text: string
    voice_ids: array[string]  # ✅ Correct
    speed: number (0.25-4.0)  # ❌ MISSING!
    audio_format: enum[mp3,opus,flac,wav,pcm]
    voice_gender: enum[male,female,other]     # ❌ MISSING!
    voice_language: enum[en_US,en_GB,...] # ❌ MISSING!
```

### 3. Missing Required Fields
The current implementation is **missing critical required fields:**

1. **`speed`** - Required field, number between 0.25-4.0
2. **`voice_gender`** - Fallback when no voice_ids provided
3. **`voice_language`** - Fallback when no voice_ids provided

### 4. Incorrect Response Handling
**Current Implementation Assumption:**
```java
// Assumes response might contain 'url' field
if (responseJson.has("url")) {
    return new TTSResponse(responseJson.get("url").getAsString(), null);
}
```

**✅ Actual Response Structure (from api.yaml):**
```yaml
TTSSpeakResponse:
  type: object
  required:
    - data  # ❌ Current code doesn't handle this correctly
  properties:
    data:
      type: string
      description: The audio data in base64 format
```

**Issue:** The API returns base64 encoded audio data in the `data` field, not a URL.

### 5. Authentication Header Issues
**Current Implementation:**
```java
// Uses custom header format
connection.setRequestProperty("Authorization", "Bearer " + apiKey);
```

**✅ Correct Authentication (from api.yaml):**
```yaml
# The API documentation shows authentication via:
# 1. Bearer token in Authorization header (current implementation is correct)
# 2. Cookie authentication for games hosted on player2.game
# 3. Local app authentication via localhost:4315
```

**Issue:** Authentication method is correct, but we need to handle the three different authentication flows properly.

## 🔧 Required Code Fixes

### Fix 1: Update TTSRequest Structure
**File:** `Player2TTSClient.java`

**Current:**
```java
ttsRequest.addProperty("text", text);
ttsRequest.addProperty("audio_format", "mp3");
if (voice != null) {
    JsonArray ids = new JsonArray();
    ids.add(voice);
    ttsRequest.add("voice_ids", ids);
}
```

**✅ Fixed:**
```java
ttsRequest.addProperty("text", text);
ttsRequest.addProperty("speed", 1.0); // REQUIRED FIELD - default speed
ttsRequest.addProperty("audio_format", "mp3");

if (voice != null && !voice.isEmpty()) {
    JsonArray ids = new JsonArray();
    ids.add(voice);
    ttsRequest.add("voice_ids", ids);
} else {
    // Use fallback gender/language when no specific voice
    ttsRequest.addProperty("voice_gender", "female"); // or from config
    ttsRequest.addProperty("voice_language", "en_US"); // or from config
}
```

### Fix 2: Update Response Handling
**Current:**
```java
if (responseJson.has("url")) {
    return new TTSResponse(responseJson.get("url").getAsString(), null);
} else if (responseJson.has("data")) {
    String base64Data = responseJson.get("data").getAsString();
    byte[] audioData = Base64.getDecoder().decode(base64Data);
    return new TTSResponse(null, audioData);
}
```

**✅ Fixed:**
```java
// API always returns base64 data in 'data' field
if (responseJson.has("data")) {
    String base64Data = responseJson.get("data").getAsString();
    byte[] audioData = Base64.getDecoder().decode(base64Data);
    return new TTSResponse(null, audioData);
} else {
    LOGGER.error("Player2 API response missing 'data' field: {}", responseJson);
    throw new RuntimeException("Invalid Player2 TTS API response");
}
```

### Fix 3: Add Speed Configuration to Deity Configs
**Files:** All deity JSON files in `src/main/resources/data/eidolonunchained/ai_deities/`

**Add to TTS config:**
```json
{
  "tts_config": {
    "voice_id": "female-natural-1",
    "speed": 0.9,           // ✅ ADD THIS - Required by API
    "audio_format": "mp3",  // ✅ ADD THIS - Required by API
    "voice_gender": "female", // ✅ ADD THIS - Fallback option
    "voice_language": "en_US", // ✅ ADD THIS - Fallback option
    // ... existing config
  }
}
```

### Fix 4: Update Authentication Flow Detection
**Add to Player2TTSClient.java:**

```java
/**
 * Detect which authentication method to use based on environment
 */
private static String determineApiBaseUrl() {
    // Method 1: Check if running on player2.game (use cookies)
    String hostname = System.getProperty("player2.hostname");
    if (hostname != null && hostname.endsWith("player2.game")) {
        return "https://games.player2.game/_api/v1";
    }

    // Method 2: Check if local Player2 app is running
    if (isLocalPlayer2AppRunning()) {
        return "http://localhost:4315/v1";
    }

    // Method 3: Use web API with Bearer token
    return "https://api.player2.game/v1";
}

private static boolean isLocalPlayer2AppRunning() {
    try {
        URL url = new URL("http://localhost:4315/v1/health");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(1000); // 1 second timeout
        int responseCode = conn.getResponseCode();
        return responseCode == 200;
    } catch (Exception e) {
        return false;
    }
}
```

### Fix 5: Update Voice Configuration System
**File:** `AIDeityConfig.java`

**Add missing fields to TTSConfig:**
```java
public static class TTSConfig {
    // Existing fields...
    public String voice_id = "auto";
    public float speed = 1.0f;              // ✅ ADD - Required by API
    public String audio_format = "mp3";     // ✅ ADD - Required by API
    public String voice_gender = "female";  // ✅ ADD - Fallback option
    public String voice_language = "en_US"; // ✅ ADD - Fallback option

    // Enhanced voice selection
    public Map<String, Float> context_speeds = new HashMap<>(); // Different speeds for different contexts
    // ... rest of existing config
}
```

### Fix 6: Handle Joules/Credits Properly
**According to api.yaml, the API uses "joules" for credits:**

```java
// Update error handling for insufficient credits
if (responseCode == 402) {
    LOGGER.warn("Insufficient joules for TTS request. Player needs to top up their account.");
    return new TTSResponse(null, null, "insufficient_joules");
}
```

## 🔄 Authentication Flow Fixes

### Current Authentication Issues
The current implementation only uses Bearer tokens, but Player2.game supports three authentication methods:

### ✅ Proper Authentication Implementation

```java
public class Player2AuthManager {
    public enum AuthMethod {
        HOSTED_ON_PLAYER2,  // Use cookies, base URL: https://games.player2.game/_api/v1
        LOCAL_APP,          // Use local app, base URL: http://localhost:4315/v1
        WEB_API            // Use Bearer token, base URL: https://api.player2.game/v1
    }

    public static AuthMethod detectAuthMethod() {
        // 1. Check if hosted on player2.game
        if (isHostedOnPlayer2Game()) {
            return AuthMethod.HOSTED_ON_PLAYER2;
        }

        // 2. Check if local app is running
        if (isLocalPlayer2AppRunning()) {
            return AuthMethod.LOCAL_APP;
        }

        // 3. Default to web API
        return AuthMethod.WEB_API;
    }

    public static void configureAuthentication(HttpURLConnection conn, AuthMethod method) {
        switch (method) {
            case HOSTED_ON_PLAYER2:
                // Cookies are automatically included when hosted
                break;
            case LOCAL_APP:
                // Get p2Key from local app
                String p2Key = getP2KeyFromLocalApp();
                conn.setRequestProperty("Authorization", "Bearer " + p2Key);
                break;
            case WEB_API:
                // Use configured API key
                String apiKey = EidolonUnchainedConfig.PLAYER2_API_KEY.get();
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                break;
        }
    }
}
```

## 🎯 Voice ID Mapping Issues

### Current Problem
The current implementation uses placeholder voice IDs like `"female-natural-1"` which don't exist in the Player2.game system.

### ✅ Solution: Dynamic Voice Loading
```java
public class Player2VoiceManager {
    private static List<TTSVoice> availableVoices = null;

    public static CompletableFuture<List<TTSVoice>> getAvailableVoices() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new URL(getApiBaseUrl() + "/tts/voices");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                configureAuthentication(conn);

                // Parse TTSVoicesResponse
                JsonObject response = parseResponse(conn);
                JsonArray voices = response.getAsJsonArray("voices");

                return parseVoices(voices);
            } catch (Exception e) {
                LOGGER.error("Failed to load available voices", e);
                return Collections.emptyList();
            }
        });
    }

    public static String mapDeityVoiceToActualVoice(String deityVoiceId) {
        // Map placeholder voice IDs to actual Player2 voice IDs
        // This would need to be populated with real voice IDs from the API
        Map<String, String> voiceMapping = new HashMap<>();
        voiceMapping.put("female-natural-1", "actual_voice_id_from_api");
        voiceMapping.put("male-deep-1", "another_actual_voice_id");

        return voiceMapping.getOrDefault(deityVoiceId, null);
    }
}
```

## 📋 Required File Updates

### High Priority (Critical for Functionality)
1. **`Player2TTSClient.java`** - Fix request structure, add required fields
2. **`AIDeityConfig.java`** - Add missing TTS config fields
3. **All deity JSON files** - Add speed, audio_format, voice_gender, voice_language

### Medium Priority (Enhanced Functionality)
4. **`TTSManager.java`** - Update to handle new authentication methods
5. **`EidolonUnchainedConfig.java`** - Add voice mapping configuration
6. **Create `Player2AuthManager.java`** - Handle multiple auth methods

### Low Priority (Future Enhancement)
7. **Create `Player2VoiceManager.java`** - Dynamic voice loading
8. **Update deity configs** - Map to real voice IDs once available

## 🧪 Testing Required

### API Integration Testing
1. Test with actual Player2.game API key
2. Verify required fields are sent correctly
3. Test authentication with local Player2 app
4. Test fallback voice selection (gender/language)
5. Verify base64 audio data handling

### Error Handling Testing
1. Test insufficient joules (402 error)
2. Test invalid voice IDs
3. Test network timeouts
4. Test malformed API responses

## 🚀 Migration Steps

### Step 1: Fix Critical API Issues
1. Add required `speed` field to all TTS requests
2. Fix response parsing to handle `data` field correctly
3. Add fallback `voice_gender` and `voice_language`

### Step 2: Update Configuration
1. Add new fields to deity JSON configurations
2. Update `AIDeityConfig.java` with new TTS options

### Step 3: Test Integration
1. Test with real Player2.game API
2. Verify audio playback works with base64 data
3. Test different authentication methods

### Step 4: Voice ID Mapping
1. Get actual voice IDs from Player2.game API
2. Create mapping from placeholder IDs to real IDs
3. Update all deity configurations

## 💡 Additional Recommendations

### 1. Configuration Validation
Add validation to ensure TTS configs have required fields:
```java
public void validateTTSConfig(TTSConfig config) {
    if (config.speed < 0.25f || config.speed > 4.0f) {
        throw new IllegalArgumentException("TTS speed must be between 0.25 and 4.0");
    }
    // ... other validations
}
```

### 2. Error Recovery
Implement graceful fallbacks when TTS fails:
```java
// If specific voice fails, try with gender/language fallback
// If TTS completely fails, continue without audio
```

### 3. Performance Optimization
- Cache voice list to avoid repeated API calls
- Implement request queuing to avoid rate limits
- Add connection pooling for better performance

---

**Next Steps:** Start with the critical API fixes (Step 1) to get basic functionality working, then incrementally add the enhanced features.

**Priority:** The missing `speed` field is causing all TTS requests to fail with 400 Bad Request errors.