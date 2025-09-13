# AI Deity JSON Reference

This page documents every field supported by the AI Deity configuration files under `data/<namespace>/ai_deities/*.json` and how they map to code.

Top‑Level Fields

- `deity` (string, required): Target deity ID. Must be an existing `DatapackDeity` ID, e.g. `eidolonunchained:dark_deity`.
- `ai_provider` (string, required): One of `gemini`, `openrouter`, `openai`, `player2ai`, `proxy`.
- `model` (string, optional): Model name for providers that use it (e.g., `gemini-1.5-pro`). If omitted, `EidolonUnchainedConfig.COMMON.geminiModel` is used where applicable.
- `personality` (string, required): Long‑form persona text the AI uses as system context.
- `mod_context_ids` (array<string>, optional): Namespaces that the AI context builder includes when sampling world/registry data (e.g., `["minecraft","eidolon","eidolonunchained"]`).
- `behavior_rules` (object, optional): Dynamic behavior mapping. See below.
- `prayer_configs` (object, optional but recommended): Per‑prayer‑type AI control. See below.
- `api_settings` (object, optional): Low‑level API tuning. See below.
- `task_config` (object, optional): Datapack‑driven task definitions for this deity. See below.
- `detailed_inter_deity_relationships` (object, optional): Narrative detail per other deity.
- `cross_deity_interactions` (object, optional): Short responses when player mentions/serves allies or enemies.
- `conversation_rules` (object, optional): Rules influencing follower/enemy interactions (penalties, hostile chance, etc.).
- `follower_personality_modifiers` (object, optional): Title → personality snippet.
- `enemy_personality_modifier` / `neutral_personality_modifier` / `no_patron_personality_modifier` / `allied_personality_modifier` (strings, optional): Persona add‑ons based on relationship.

Behavior Rules (`behavior_rules`)

- `reputation_thresholds` (object<int,string>): Tiered overlays based on player reputation. The highest threshold not exceeding current reputation is applied.
- `research_requirements` (object<int,string>): Persona overlays based on count of research entries completed.
- `dynamic_responses` (object): Context‑aware snippets, e.g.:
  - `time_of_day` → `{ "night": "...", "day": "..." }`
  - `biome` → `{ "minecraft:deep_dark": "..." }`
- `blessings` / `curses` / `gifts` (object<string,string>): Narrative hooks referenced by the AI when granting effects.
- Personality shifts: custom keys like `personality_shifts` may be provided; `AIDeityConfig` exposes `addPersonalityShift`, `addTimeBehavior`, `addBiomeBehavior`.

Prayer Configs (`prayer_configs`)

Each key under `prayer_configs` is a prayer type (`conversation`, `blessing`, `knowledge`, `guidance`, `ritual`, etc.). Mapped to `PrayerAIConfig`.

Required fields per prayer type:
- `base_prompt` (string): System prompt template. Tokens available: `{player}`, `{reputation}`, `{progression_title}`, `{prayer_type}`.
- `max_commands` (int): Maximum server commands the AI may execute per response (0–2 typical).
- `cooldown_minutes` (int): Per‑player cooldown.
- `reputation_required` (int): Minimum reputation to use this prayer type.
- `allowed_commands` (array<string>): White‑list of minecraft commands the AI may choose from (e.g., `give {player} ...`).

Optional fields:
- `additional_prompts` (array<string>): Extra steering instructions.
- `reference_commands` (array<string>): Examples the AI can imitate; useful for constraining style.
- `auto_judge_commands` (bool): If true, the engine can choose blessing/curse/neutral command sets based on reputation thresholds.
- `judgment_config` (object): `JudgmentConfig`
  - `blessing_threshold` (int)
  - `curse_threshold` (int)
  - `blessingCommands` (array<string>)
  - `curseCommands` (array<string>)
  - `neutralCommands` (array<string>)

API Settings (`api_settings`)

Maps to `APISettings` and its children.
- `api_key_env` (string): Env var name to read API key from (e.g., `EIDOLON_OPENROUTER_API_KEY`).
- `model` (string): Provider model name; overrides the config default if set.
- `timeout_seconds` (int): Request timeout.
- `generation_config` (object): `GenerationConfig`
  - `temperature` (float)
  - `max_output_tokens` (int)
  - `top_k` (int)
  - `top_p` (float)
- `safety_settings` (object): `SafetySettings`
  - `harassment`, `hate_speech`, `sexually_explicit`, `dangerous_content` (strings) — provider‑specific thresholds.
- `chant_sequence` (array<string>, optional): Preferred chant signs for AI‑initiated rituals.

Task Config (`task_config`)

Defines deity‑scoped tasks; loaded by `AIDeityManager.loadTaskConfig` into `TaskSystemConfig.TaskTemplate`.

- `enabled` (bool, default true): Toggle this deity’s tasks.
- `max_active_tasks` (int): Cap per player.
- `available_tasks` (array<object>): List of task templates. Fields:
  - `task_id` (string, required): Unique id within this deity (e.g., `harvest_shadows`).
  - `display_name` (string, recommended): Shown in UIs/autocomplete comments.
  - `description` (string, recommended): What the player must do.
  - `progression_tier` (string, optional): Dev label for your own gating.
  - `requirements` (array<object>): Requirement objects; stored internally as `type:` + JSON string for now. Recommended types:
    - `{ "type": "item", "item": "minecraft:wheat", "count": 16 }`
    - `{ "type": "ritual", "ritual": "eidolonunchained:shadow_communion" }`
    - `{ "type": "entity_kill", "entity": "minecraft:zombie", "count": 10 }`
  - `rewards` (object):
    - `reputation` (int): Points awarded.
    - `commands` (array<string>): Server commands to run on completion (supports `{player}` token).
  - `cooldown_hours` (int): Time before it can be reassigned.
  - `repeatable` (bool)
  - `ai_assignment_context` (object): Arbitrary JSON for your AI prompts (stored as string, not interpreted by loader).

Notes on requirement parsing

- Automatic completion works for ritual requirements because the engine looks for `"ritual:"` and the ritual id inside the requirement string.
- Manual completion (`/eidolon-unchained tasks complete`) currently validates a simplified token form. To ensure consistency, either:
  - Provide simple tokens in `requirements` (e.g., `"item:minecraft:wheat:16"`, `"ritual:eidolonunchained:shadow_communion"`), or
  - Extend the validator to parse JSON bodies. If you want, we can implement JSON parsing so both styles work.

Command Integration

- Canonical tasks commands live under `/eidolon-unchained tasks ...` with autocomplete for `player`, `deity`, and `taskId`.
- Legacy alias `/dtask` remains available.

Example (minimal)

```json
{
  "deity": "yourmod:example_deity",
  "ai_provider": "gemini",
  "personality": "You are the wise guardian of starlight...",
  "prayer_configs": {
    "conversation": {
      "base_prompt": "Player {player} with {reputation} reputation approaches...",
      "max_commands": 2,
      "cooldown_minutes": 5,
      "reputation_required": 0,
      "allowed_commands": [
        "give {player} minecraft:glow_berries 4",
        "effect give {player} minecraft:night_vision 120 0"
      ],
      "additional_prompts": ["Speak warmly but with cosmic detachment."],
      "reference_commands": ["effect give {player} minecraft:regeneration 30 0"],
      "auto_judge_commands": false
    }
  },
  "task_config": {
    "enabled": true,
    "max_active_tasks": 3,
    "available_tasks": [
      {
        "task_id": "collect_starlight",
        "display_name": "Collect Starlight",
        "description": "Gather 16 glow berries under the night sky",
        "requirements": [ { "type": "item", "item": "minecraft:glow_berries", "count": 16 } ],
        "rewards": { "reputation": 5, "commands": ["playsound minecraft:block.amethyst_block.chime player {player}"] },
        "cooldown_hours": 12,
        "repeatable": true
      }
    ]
  }
}
```

Troubleshooting

- “Deity not found”: Ensure the target `deity` exists as a DatapackDeity.
- Prayer config errors: All of `base_prompt`, `max_commands`, `cooldown_minutes`, `reputation_required`, `allowed_commands` are required.
- Tasks not auto‑completing: For ritual‑based tasks, confirm `PlayerContextTracker.onRitualComplete` is triggered (either by real ritual or via `/eidolon-unchained tasks ritual ...`). For item tasks, ensure validator recognizes your requirement format.

See Also

- Systems overview: wiki/Systems/AI-Deity-System.md
- Task commands: wiki/Systems/Tasks-Reputation.md
