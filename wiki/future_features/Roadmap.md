# Eidolon Unchained — Feature Roadmap

Features planned for future implementation. Items are grouped by theme, not by priority or timeline.

---

## AI & Provider System

---

## Mob AI & Faction Behavior

### Team Mob Defense (Mobs Defend Allied Players and Team Members)
**Status:** Planned

All mobs that are members of a deity faction team should actively defend allied players and other team members when those allies are attacked, not just avoid friendly fire via scoreboard team flags.

**Desired behavior:**
- When a nearby allied player is hurt, team mobs target the attacker automatically.
- When a nearby allied mob is hurt, other team mobs join in targeting the attacker.
- Defense radius should be configurable (default ~16 blocks).
- Should not override current cast targets or ongoing chant sequences — defense targeting triggers only when the mob is idle or between casts.

**Implementation notes:**
- Create a new `DefendTeamMemberGoal extends TargetGoal` (PathfinderMob).
- Scans nearby `LivingEntity` for allies (`mob.isAlliedTo(candidate)` or same scoreboard team).
- If any ally has `lastHurtByMob` set to a non-allied entity within range, adopt that entity as target.
- Inject via `MobChantCastingGoal.onEntityJoin` alongside existing goal injection (enthrall goals already injected there).
- Goal priority should be lower than `MobChantCastingGoal` (e.g. priority 2–3) so chanting takes precedence.
- Only applicable to mobs that have chant rotation data or are enthralled — not all mobs in any team globally.

### Local LLM Provider Support (Ollama / LM Studio / OpenAI-compatible local endpoints)
**Status:** Planned

Add first-class support for local model backends so servers can run deity chat without external API costs or cloud dependency.

**Proposed providers:**
- `ollama` (direct)
- `openai_compatible` (for LM Studio, vLLM, LocalAI, text-generation-webui proxies)

**Proposed commands:**
```
/eu deity provider <deity> <ollama|openai_compatible>
/eu deity provider <deity> <ollama|openai_compatible> <model>
/eu config local-llm endpoint <url>
/eu config local-llm key <token|none>
```

**Implementation notes:**
- Add `LocalLLMAPIClient` with provider adapters (`OllamaAdapter`, `OpenAICompatibleAdapter`).
- Reuse existing async flow and timeout/retry handling from `GeminiAPIClient` integration path.
- Extend `AIDeityConfig` schema with optional `base_url`, `api_key_env`, `provider_options`.
- Add provider capability metadata (supports tools/JSON mode/system role) to normalize prompts safely.
- Add fallback chain option (example: local primary, Gemini backup).

### Per-Session AI Provider Override Command
**Status:** Planned

Add an in-game command to switch the AI provider (and optionally model) for a deity without editing the datapack JSON.

**Proposed commands:**
```
/eu deity provider <deity> <gemini|openrouter|player2ai>
/eu deity provider <deity> <gemini|openrouter|player2ai> <model>
/eu deity provider <deity> reset
/eu deity provider status
```

**Behavior:**
- Override is per-session and stored in memory — it does not persist across restarts
- `reset` clears the override and reverts to the value in `ai_deities/*.json`
- `status` shows the current effective provider for each active AI deity
- Only affects the calling player's deity conversations (not server-wide)
- OP-only (`permission level 2`)

**Example usage:**
```
# Switch dark deity to Gemini for this session
/eu deity provider eidolonunchained:dark_deity gemini

# Use a specific model
/eu deity provider eidolonunchained:dark_deity gemini gemini-2.0-flash-exp

# Use OpenRouter with a specific model
/eu deity provider eidolonunchained:dark_deity openrouter meta-llama/llama-3.1-8b-instruct:free

# Revert to datapack config
/eu deity provider eidolonunchained:dark_deity reset
```

**Implementation notes:**
- Add `Map<UUID, Map<ResourceLocation, String>> playerProviderOverrides` to `AIDeityManager`
- `TTSManager.generateAndSendTTS` and `DeityChat.startConversation` check this map before reading `AIDeityConfig.ai_provider`
- Model override stored alongside provider; if only provider is given, use the deity's configured model or provider default
- Wire into `UnifiedCommands` under `/eu deity`

---

### Per-Session TTS Provider Override Command
**Status:** Planned

Similar to the AI provider override, allow switching TTS provider per deity per session.

**Proposed commands:**
```
/eu deity tts-provider <deity> <gemini|player2ai>
/eu deity tts-provider <deity> reset
```

---

## TTS Enhancements

### Voice Effects Pipeline (Per-Deity and Per-Player)
**Status:** Planned

Add optional post-processing effects for generated deity voice lines to improve immersion and deity identity.

**Target effects (v1):**
- Reverb amount
- Echo/delay
- Low-pass / high-pass filter
- Distortion (very light)
- Pitch shift (subtle)

**Proposed commands:**
```
/eu tts effects <off|preset>
/eu tts effects custom <effect> <value>
/eu tts deity-effects <deity> <off|preset>
/eu tts deity-effects <deity> custom <effect> <value>
```

**Proposed presets:**
- `ethereal`
- `void`
- `cathedral`
- `whispered`
- `none`

**Implementation notes:**
- Store effect config in `tts_config.effects` per deity, with optional per-player overrides.
- Apply effects after TTS generation and before playback packet/send.
- Keep bounded ranges to avoid clipping and speaker damage.
- Add server-side toggle to disable effects globally for performance-sensitive servers.
- Cache processed clips by `(text hash + voice + effects hash)`.

### Persist Voice/Volume/Speed Across Sessions
**Status:** Planned

Currently `preferredVoice`, `volume`, and `speed` from `/eu tts voice|volume|speed` are lost on restart. Save per-player TTS settings to a file under `world/playerdata/eidolonunchained/<uuid>_tts.json`.

---

### Per-Deity Voice Override Command
**Status:** Planned

Allow overriding the voice for a specific deity without changing the global voice setting.

**Proposed command:**
```
/eu tts deity-voice <deity> <voice|auto>
```

---

## Command System

### Persistent AI Provider Override (saved to config)
**Status:** Planned

A server-operator command to permanently change a deity's AI provider in the running config (not the datapack JSON), written to `config/eidolonunchained/provider_overrides.json` so it survives restarts.

```
/eu config set-provider <deity> <provider> [model]   [op]
/eu config clear-provider <deity>                    [op]
```

---

## Multiplayer

### Full AI Config Sync to Clients
**Status:** Planned (partial fix landed)

The `DatapackSyncPacket` currently sends a stripped config (missing `tts_config`, `prayer_configs`, etc.) to clients. Full sync would serialize the complete `AIDeityConfig` including all TTS settings so client-side features (voice previews, `/eu tts test`) work correctly on dedicated servers.

---

## Research Triggers

### Entity NBT Filter for Kill Triggers
**Status:** Partially implemented — disabled

The `nbt` field on `kill_entity` triggers is parsed and stored but the comparison is not active. `TagParser.parseTag()` correctly reads the NBT string from JSON, but the `containsAllTags` equality check against `killedEntity.saveWithoutId()` output does not match — suspected tag type mismatch or serialization difference for fields like `CustomName`.

**JSON syntax (ready when fixed):**
```json
{
  "type": "kill_entity",
  "entity": "minecraft:zombie",
  "nbt": "{CustomName:'{\"text\":\"Debug Zombie\"}'}",
  "max_found": 1
}
```

**Files:**
- `research/triggers/KillResearchTriggers.java` — NBT check is commented out with `TODO`
- `research/triggers/ResearchTriggerLoader.java` — NBT parsing via `TagParser` is in place

---

### Inventory Item Trigger
**Status:** Planned

A new `inventory_item` trigger type that fires when a player picks up or already holds a specific item (with optional NBT). Would hook into `ItemPickupEvent` and check on login.

**Proposed JSON:**
```json
{
  "type": "inventory_item",
  "item": "minecraft:nether_star",
  "count": 1,
  "max_found": 1
}
```

**Implementation notes:**
- Add handler in a new `InventoryResearchTriggers.java` (follows same pattern as `KillResearchTriggers`)
- Hook `ItemPickupEvent` and `PlayerEvent.PlayerLoggedInEvent`
- Optional `nbt` field for item NBT matching (can reuse `ItemRequirementChecker.matchesItem` logic once entity NBT is fixed)
