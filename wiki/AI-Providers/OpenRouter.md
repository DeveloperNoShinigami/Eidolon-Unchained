# OpenRouter

OpenRouter is currently a chat provider only.

## Current Scope

- chat generation through `AIProviderFactory`
- model selection from config or deity JSON
- no native TTS path in the current implementation

## Configuration

Use:

- `ai_provider: openrouter`
- `api_settings.model` or the global OpenRouter model setting

If no model is provided, the factory falls back to a default model string instead of refusing to initialize.

## Practical Use

OpenRouter is a good fit when you want deity conversation through an alternate chat backend without also depending on that provider for TTS.