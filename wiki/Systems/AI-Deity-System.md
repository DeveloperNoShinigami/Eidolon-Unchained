# AI Deity System

The AI deity system is loaded from `data/*/ai_deities/*.json` through `AIDeityManager`. It layers provider selection, personality rules, prayer behavior, patron rules, task data, and TTS settings on top of a base deity.

## Core Authoring Surface

The canonical AI config fields currently center on:

- `deity`
- `ai_provider`
- `personality`
- `behavior_rules`
- `prayer_configs`
- `api_settings`
- `tts_config`
- `patron_config`
- `task_config`

## Conversation Routing

`DeityChat` resolves provider usage in this order:

- use the deity's configured `ai_provider` when available
- fall back to the global provider if the deity-specific key is missing
- create the runtime provider through `AIProviderFactory`

The current provider factory supports:

- Gemini
- Player2AI
- OpenRouter
- OpenAI as a future fallback stub, not a real implementation

## Prayer Configuration

Prayer behavior is driven by the `prayer_configs` map.

- Each prayer type can define its own prompt, command whitelist, reference commands, reputation gate, and cooldown.
- The prayer type resolver prefers chant context first, then falls back to message-based inference.

## Patron And Task Coupling

AI configs also own:

- patron relationship rules
- follower and enemy personality modifiers
- task or fate templates attached into `task_config.availableTasks`

This means the AI layer is where deity conversation, patron gating, and fate assignment meet.# AI Deity System

The AI deity layer is loaded from `data/*/ai_deities/` through `AIDeityManager`. It attaches conversation behavior, prayer configuration, patron behavior, task templates, provider selection, and TTS metadata to base deity records.

## Runtime Role

An AI deity config is what turns a normal datapack deity into an interactive deity.

It provides:

- `ai_provider`
- `personality`
- `prayer_configs`
- `api_settings`
- `tts_config`
- `patron_config`
- `task_config`

## Provider Selection

Conversation routing uses `AIProviderFactory`.

- `gemini` is supported
- `player2ai` is supported
- `openrouter` is supported
- `openai` currently falls back instead of providing a real implementation

If a deity-specific provider is not configured with a usable key, the chat layer can fall back to the global provider configuration.

## Linking And Timing

The data-loading order matters.

- deities load first
- AI configs link to deity IDs afterward
- prayer spell registration happens after AI configs are linked
- fate attachment may be deferred if AI configs are not ready yet

## Patron And Prayer Layers

The AI config is also where the more advanced interaction rules live:

- patron allegiance filtering
- opposing and allied deity relationships
- prayer-type prompts and command pools
- response modifiers based on patron status, progression, biome, and time

This means the base deity JSON is not enough on its own to produce AI behavior.