# TTS Quick Setup

This is the shortest verified path to hearing deity voice output in the current build.

## 1. Make Sure A TTS-Capable Provider Exists

The current TTS-capable paths are:

- **Gemini** — server-side API key, runs on Google's infrastructure
- **Player2AI** — runs through the Player2 App or the Player2 web API; can be player-funded, server-funded, or both

## 2. Configure The Provider

### Gemini

Add a server-side API key and verify with:

```
/eu api set gemini <key>
/eu api test gemini
```

### Player2AI

The easiest path is the device login flow, which ties to the connecting player's Player2 account:

```
/eu player2ai login device
```

To check login status:

```
/eu player2ai login status
```

To log out:

```
/eu player2ai logout
```

If the server has a Player2AI API key configured, it can fund TTS on behalf of players who have not logged in. Add the key in `config/eidolonunchained/server-api-keys.properties` under `player2ai`.

## 3. Enable TTS As A Player

TTS is disabled by default. Each player must opt in:

```
/eu tts enable
```

Verify current settings:

```
/eu tts status
```

## 4. Pick A Funding Mode (Player2AI only)

If you are using Player2AI, pick how TTS usage is funded:

| Command | Behavior |
|---|---|
| `/eu tts funding player-first` | Your Player2AI account is tried first; server is the fallback |
| `/eu tts funding player-only` | Only your account is used — no server fallback |
| `/eu tts funding server-only` | Server funds all requests; your login is ignored |

## 5. Pick A Voice

Set a preferred voice for yourself:

```
/eu tts voice <voice>
/eu tts voice auto
```

`auto` lets the deity config choose the voice. Use `/eu tts status` to see which voice is active.

Adjust volume and speed:

```
/eu tts volume 1.0
/eu tts speed 1.0
```

## 6. Test Playback

```
/eu tts test <text>
/eu tts test <deity> <text>
```

## 7. Optional Spatial Playback

If Simple Voice Chat is installed, the mod attempts spatial playback instead of relying only on generic client-side audio. No extra configuration needed.

---

## Player2AI TTS — Deity Config Example

Set `tts_provider` to `player2ai` in your deity's `tts_config`. The `voice_id` value is passed directly to the Player2 TTS API.

```json
{
  "ai_provider": "player2ai",
  "tts_config": {
    "enabled": true,
    "tts_provider": "player2ai",
    "voice_id": "auto",
    "funding_preference": "player-first"
  }
}
```

`funding_preference` on the deity side sets the default when the player has not overridden it via `/eu tts funding`.

---

## Gemini TTS Models (as of May 2026)

Set the model in your deity JSON under `tts_config.tts_model`. Available models:

| Model ID | Notes |
|---|---|
| `gemini-3.1-flash-tts-preview` | **Newest** — powerful, low-latency, supports expressive audio tags |
| `gemini-2.5-flash-preview-tts` | **Recommended** — fast, controllable, real-time assistants. Currently verified working. |
| `gemini-2.5-pro-preview-tts` | Highest fidelity — best for structured/narrated content (higher cost) |

> **Deprecated (will 404):** `gemini-1.5-flash`, `gemini-1.5-pro`, `gemini-2.0-flash`, `gemini-2.0-flash-lite`

### Chat Models (used in `model` field)

| Model ID | Notes |
|---|---|
| `gemini-2.5-flash` | **Recommended default** — fast, cheap, strong reasoning |
| `gemini-2.5-flash-lite` | Fastest and cheapest in 2.5 family |
| `gemini-2.5-pro` | Most capable for complex tasks |
| `gemini-3-flash-preview` | Preview — frontier performance |
| `gemini-3.1-pro-preview` | Preview — advanced intelligence |

> The mod will automatically upgrade deprecated model names (1.5-flash, 2.0-flash, etc.) to `gemini-2.5-flash` and log a warning.

### Gemini Deity Config Example

```json
{
  "ai_provider": "gemini",
  "model": "gemini-2.5-flash",
  "tts_config": {
    "enabled": true,
    "tts_provider": "gemini",
    "tts_model": "gemini-2.5-flash-preview-tts",
    "voice_id": "Aoede"
  }
}
```

Set the model in your deity JSON under `tts_config.tts_model`. Available models:

| Model ID | Notes |
|---|---|
| `gemini-3.1-flash-tts-preview` | **Newest** — powerful, low-latency, supports expressive audio tags |
| `gemini-2.5-flash-preview-tts` | **Recommended** — fast, controllable, real-time assistants. Currently verified working. |
| `gemini-2.5-pro-preview-tts` | Highest fidelity — best for structured/narrated content (higher cost) |

> **Deprecated (will 404):** `gemini-1.5-flash`, `gemini-1.5-pro`, `gemini-2.0-flash`, `gemini-2.0-flash-lite`

### Chat Models (used in `model` field)

| Model ID | Notes |
|---|---|
| `gemini-2.5-flash` | **Recommended default** — fast, cheap, strong reasoning |
| `gemini-2.5-flash-lite` | Fastest and cheapest in 2.5 family |
| `gemini-2.5-pro` | Most capable for complex tasks |
| `gemini-3-flash-preview` | Preview — frontier performance |
| `gemini-3.1-pro-preview` | Preview — advanced intelligence |

> The mod will automatically upgrade deprecated model names (1.5-flash, 2.0-flash, etc.) to `gemini-2.5-flash` and log a warning.

### Example Deity Config

```json
{
  "ai_provider": "gemini",
  "model": "gemini-2.5-flash",
  "tts_config": {
    "enabled": true,
    "tts_provider": "gemini",
    "tts_model": "gemini-2.5-flash-preview-tts",
    "voice_id": "Aoede"
  }
}
```