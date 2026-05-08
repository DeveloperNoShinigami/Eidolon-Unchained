# Player2AI

Player2AI supports both deity chat and TTS in the current codebase. It is unique among supported providers in that it does not require a server-owned API key — players can authenticate independently using the Player2 App or a device-login flow.

## Chat Layer

The provider is created through `AIProviderFactory` and backed by `Player2AIClient`.

Player2AI availability is resolved differently from key-based providers:

- Local Player2 App authentication is tried automatically at startup (if the app is running on `localhost:4315`).
- Per-player device login is supported via in-game command.
- Web API auth is supported with a static API key configured server-side.
- Provider availability does not require a preloaded server key — it returns `true` if any of the above paths resolves.

Player2AI also supports optional character memory (persistent conversation history) and relationship tracking (the AI learns player preferences over time). Both features are controlled via `config/eidolonunchained/eidolonunchained-common.toml`.

## Login Flow

Player-facing commands:

```
/eu player2ai login device
/eu player2ai login status
/eu player2ai logout
```

The device login flow generates a short-lived URL that the player opens in a browser to link their Player2 account. The resulting token is stored per-player server-side.

## Deity Config (Chat)

To use Player2AI for deity chat, set `ai_provider` in your deity JSON. The `model` field is optional — Player2AI does not use it.

```json
{
  "ai_provider": "player2ai"
}
```

## TTS Layer

Player2AI participates in `TTSManager` and supports the split-funding model.

### Funding Modes

The TTS system can route costs three ways:

| Mode | Behavior |
|---|---|
| `player-first` | Player's account is tried first; server API key is the fallback |
| `player-only` | Only the player's account is used — fails gracefully if not logged in |
| `server-only` | Server API key funds all requests; per-player auth is ignored |

Players set their mode with `/eu tts funding <mode>`. The deity can also suggest a default in `tts_config.funding_preference`.

### TTS Requests

Requests are sent to `https://api.player2.game/v1/tts/speak` (web) or `http://127.0.0.1:4315/v1/tts` (local Player2 App). The response returns base64-encoded audio data.

### Voice Selection

The `voice_id` field in `tts_config` is passed directly to the Player2 TTS API. Use `auto` to let the deity config resolve the voice.

Players can override voice selection with:

```
/eu tts voice <voice>
/eu tts voice auto
```

### Deity Config (TTS)

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

### Playback

Audio returned from the Player2 TTS API is delivered to the client through `TTSAudioPacket`. If Simple Voice Chat is installed, the mod can also attempt spatial playback through `VoiceChatIntegration`.

## Player2HealthSignal

When the Player2AI provider is initialized at server startup, `Player2HealthSignal.startHealthSignal()` is called as a background reachability check against the Player2 API. This verifies that the TTS path is reachable before any player request hits it. Failures are logged at WARN level and do not block the provider from being registered — they are advisory only. No player-visible output is produced by the health signal itself.