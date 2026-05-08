# TTS System

The TTS system is orchestrated by `TTSManager` and used by deity conversation flows when TTS is enabled for the player and the selected provider can produce audio.

## Provider Model

The current codebase supports TTS through:

- Gemini (`GeminiTTSClient`)
- Player2AI (`Player2TTSClient`)
- Web TTS (`WebTTSClient`)

OpenRouter is chat-only in the current implementation.

## Resolution Rules

`TTSManager` resolves provider behavior from the deity's `tts_config` first, then falls back to the deity's `ai_provider` when needed.

It also resolves:

- voice selection
- funding preference
- per-player override permissions
- playback speed and volume
- biome, time, and reputation voice variants

## Delivery Paths

Audio can be delivered through:

- direct client packet playback via `TTSAudioPacket`
- optional Simple Voice Chat spatial playback through `VoiceChatIntegration`

## Conversation Modes

The conversation layer supports:

- hybrid mode, where the LLM responds and TTS speaks the response
- TTS-only mode, where the prompt is sent directly into voice generation

## Per-Deity `tts_config` Schema

The full set of fields supported in `tts_config` inside an AI deity JSON file:

```json
"tts_config": {
  "enabled": true,
  "tts_provider": "<provider_name>",
  "model": "<tts_model_name>",
  "voice_id": "<registered_voice_name_or_auto>",
  "backup_voice": "<registered_voice_name>",
  "reputation_voices": {
    "0": "<registered_voice_name>"
  },
  "time_voices": {
    "day": "<registered_voice_name>",
    "night": "<registered_voice_name>"
  },
  "biome_voices": {
    "<namespace>:<biome_name>": "<registered_voice_name>"
  },
  "voice_aliases": {
    "<local_alias>": "<registered_voice_name>"
  },
  "pitch": 1.0,
  "speed": 1.0,
  "volume": 1.0,
  "emotion": "<emotion_hint>",
  "accent": "<accent_hint>",
  "emphasis_level": 0,
  "voice_gender": "female",
  "voice_language": "en-US",
  "audio_format": "wav",
  "allow_player_override": true,
  "funding_preference": "player_first",
  "advanced_params": {
    "<provider_specific_key>": "<provider_specific_value>"
  }
}
```

Notes:

- `enabled` at the `tts_config` level disables TTS for this deity independent of the player's global TTS setting.
- `voice_gender` and `voice_language` are provider hints for automatic voice selection when `voice_id` is `auto`.
- `audio_format` selects the codec: `mp3`, `opus`, `flac`, `wav`, `pcm`. Not all providers support all formats.
- `pitch`, `emotion`, `accent`, `emphasis_level` are provider-specific hints. Not all values apply to all providers.
- `reputation_voices`, `time_voices`, `biome_voices` use the same priority chain: biome → time → reputation → default voice.
- `voice_aliases` maps local shorthand names to registered voice IDs for cleaner configuration.
- `advanced_params` passes provider-specific configuration that is not part of the unified schema.

## Player TTS Controls

The player-facing command subtree includes:

- enable and disable (`/eu tts enable` / `/eu tts disable`)
- status (`/eu tts status`)
- test (`/eu tts test <text>` or `/eu tts test <deity> <text>`)
- funding mode (`/eu tts funding player-only|server-only|player-first`)
- voice (`/eu tts voice <voice|auto>`)
- volume (`/eu tts volume <0.0–2.0>`)
- speed (`/eu tts speed <0.5–2.0>`)
- hybrid or TTS-only mode (`/eu tts mode hybrid|tts-only`)

## Funding Model

Player2AI supports split funding between the connecting player and the server.

The active strategy is set per player via `/eu tts funding`:

| Mode | Behavior |
|---|---|
| `player-first` | Tries player-funded path first; falls back to server if unavailable |
| `player-only` | Uses only the player's Player2AI account; no server fallback |
| `server-only` | Uses server-funded path only; ignores per-player auth |

The deity can also declare a preferred funding direction in `tts_config.funding_preference`.

## Player2HealthSignal

When the Player2AI provider is initialized, `Player2HealthSignal.startHealthSignal()` is called to verify the Player2AI TTS path is reachable. This is a background health-check and does not generate visible player-facing output unless the provider fails.

## Request Caching

`TTSManager` caches active TTS requests by a hash of player UUID + message text. Duplicate requests for the same text in rapid succession are short-circuited to the already-in-flight `CompletableFuture` rather than generating additional audio generation calls.

## Current Practical Limits

- Provider behavior is not symmetrical across Gemini and Player2AI — not all voice parameters apply to both.
- Voice playback falls back through multiple paths depending on installed integrations and returned audio format.
- Provider-specific advanced parameters can be stored in `tts_config`, but they are not a unified cross-provider schema.

## Important Boundary

TTS availability is not just a provider question. It also depends on:

- whether the player has enabled TTS (per-player opt-in via `/eu tts enable`)
- whether the selected provider is configured
- whether the chosen funding path is usable at the time of the request

For setup instructions, see [TTS Quick Setup](../TTS/Quick-Setup.md).

## Resolution Rules

`TTSManager` resolves provider behavior from the deity's `tts_config` first, then falls back to the deity's `ai_provider` when needed.

It also resolves:

- voice selection
- funding preference
- per-player override permissions
- playback speed and volume
- biome, time, and reputation voice variants

## Delivery Paths

Audio can be delivered through:

- direct client packet playback via `TTSAudioPacket`
- optional Simple Voice Chat spatial playback through `VoiceChatIntegration`

## Conversation Modes

The conversation layer supports:

- hybrid mode, where the LLM responds and TTS speaks the response
- TTS-only mode, where the prompt is sent directly into voice generation

## Player Controls

The player-facing command subtree includes:

- enable and disable (`/eu tts enable` / `/eu tts disable`)
- status (`/eu tts status`)
- test (`/eu tts test <text>` or `/eu tts test <deity> <text>`)
- funding mode (`/eu tts funding player-only|server-only|player-first`)
- voice (`/eu tts voice <voice|auto>`)
- volume (`/eu tts volume <0.0–2.0>`)
- speed (`/eu tts speed <0.5–2.0>`)
- hybrid or TTS-only mode (`/eu tts mode hybrid|tts-only`)

## Funding Model

Player2AI supports split funding between the connecting player and the server.

The active strategy is set per player via `/eu tts funding`:

| Mode | Behavior |
|---|---|
| `player-first` | Tries player-funded path first; falls back to server if unavailable |
| `player-only` | Uses only the player's Player2AI account; no server fallback |
| `server-only` | Uses server-funded path only; ignores per-player auth |

The deity can also declare a preferred funding direction in `tts_config.funding_preference`.

## Current Practical Limits

- Provider behavior is not symmetrical across Gemini and Player2AI — not all voice parameters apply to both.
- Voice playback falls back through multiple paths depending on installed integrations and returned audio format.
- Provider-specific advanced parameters can be stored in `tts_config`, but they are not a unified cross-provider schema.

## Important Boundary

TTS availability is not just a provider question. It also depends on:

- whether the player has enabled TTS (per-player opt-in via `/eu tts enable`)
- whether the selected provider is configured
- whether the chosen funding path is usable at the time of the request

For setup instructions, see [TTS Quick Setup](../TTS/Quick-Setup.md).