# AI Deity System

What It Does

- Replaces standard prayer effects with an AI conversation when using special prayers.
- Initiated by `AIDeityPrayerSpell`, which opens `DeityChat` with the target deity.
- Effigy presence and readiness are verified before conversation begins.

Code Highlights

- `AIDeityPrayerSpell` — `src/main/java/com/bluelotuscoding/eidolonunchained/spells/AIDeityPrayerSpell.java`
  - Validates effigy, reputation, and config
  - Starts conversation: `DeityChat.startConversation(serverPlayer, aiDeityId)`
- `EffigyEffectsManager` — conversation‑linked effigy visuals and audio
- `EidolonUnchainedConfig` — AI provider, models, retry, and display options

Deity Definitions

- Managed by `DatapackDeityManager`
- Provide deity IDs, display names/colors, and (optionally) per‑deity AI settings

Providers

- `gemini`, `player2ai`, `openrouter`, `openai`, `proxy`
- Set via command: ` /eidolon-unchained api set <provider> <key>`
- Model selection: ` /eidolon-unchained api set-model <model>`

Display Modes

- Title/subtitle or action bar typing with configurable speed/wrapping
- Config toggles for logging and retry/backoff on transient errors

Troubleshooting

- "Deity not found" ⇒ missing/unloaded datapack deity ID
- "No effigy found" ⇒ ensure altar/effigy placement near the player
- Conversation not starting ⇒ check `enable_ai_deities` and provider API key/model
