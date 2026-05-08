# Gemini

Gemini is the most complete built-in provider path because it is used for both normal deity chat and the dedicated prayer flow.

## Current Scope

- chat provider through `AIProviderFactory`
- direct prayer handling in `PrayerSystem`
- TTS support through the Gemini TTS client

## Configuration

Gemini can be configured globally or per deity.

Important canonical fields include:

- `ai_provider: gemini`
- `api_settings.model`
- `api_settings.timeout_seconds`
- `api_settings.generation_config.temperature`
- `api_settings.generation_config.max_output_tokens`

## Compatibility Aliases

The code still accepts some old names, but this wiki treats them as compatibility-only:

- `max_tokens`
- `maxOutputTokens`

Use `max_output_tokens` for new authoring.

## TTS

Gemini also supports deity voice generation through the TTS layer, including voice selection that can respond to deity config, biome, time, and reputation-aware mappings.