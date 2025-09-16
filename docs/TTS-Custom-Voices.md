# Custom TTS Voices and Overrides

You can define your own voice alias mappings and per-deity provider hints without modifying code or data JSONs.

## 1) Global voice aliases (server/client config)
Create `config/eidolon-unchained/tts_voices.json` with:

{
  "aliases": {
    "male-deep-1": "01955d76-ed5b-753f-9f74-c0674216f0f5",
    "neutral-1": "21m00Tcm4TlvDq8ikWAM"
  }
}

- Keys are the friendly names used in deity TTS configs and player settings.
- Values are provider-specific voice IDs (e.g., Player2 voice UUIDs or Web TTS IDs).
- These override any built-in aliases from deity JSONs.

The file is optional. If present, it’s loaded automatically at runtime.

## 2) Per-deity provider hints
Every deity JSON can now include optional fields inside `tts_config`:

- `audio_format`: "mp3" | "opus" | "flac" | "wav" | "pcm"
- `voice_gender`: "male" | "female" | "other"
- `voice_language`: e.g., "en_US", "ja_JP"

These are used when the provider needs defaults (e.g., when no explicit `voice_id` is set).
They override global defaults from the main config but never include secrets.

## 3) Priority order when choosing a voice
1. Player preference (if allowed by deity and set to a specific voice)
2. Deity context voice (biome/time/reputation) with alias resolution
3. Global alias mapping from `tts_voices.json`
4. Provider fallbacks using `voice_gender` / `voice_language`
5. Final default voice

## 4) Testing
- Enable and test TTS:
  - `/eidolon-unchained tts enable`
  - `/eidolon-unchained tts test "The gods are speaking"`
- Switch voices:
  - `/eidolon-unchained tts voice neutral-1`
- Watch logs for: "Loaded X TTS voice aliases"

## Notes
- No API keys belong in deity JSONs. Keep secrets in the main config only.
- For Web TTS providers, your server must provide `/tts/voices` and `/tts/speak`.
