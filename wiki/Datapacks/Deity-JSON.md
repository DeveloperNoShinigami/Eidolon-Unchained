# Deity JSON

[Overview](Overview.md) | [AI Deity JSON](AI-Deity-JSON.md) | [Chant JSON](Chant-JSON.md) | [Deity JSON](Deity-JSON.md) | [Codex JSON](Codex-JSON.md) | [Research JSON](Research-JSON.md) | [Fate JSON](Fate-JSON.md) | [Facts JSON](Facts-JSON.md) | [Keybind Settings JSON](Keybind-Settings-JSON.md) | [Ritual And Recipe Notes](Ritual-Recipes.md) | [Localization](Localization.md)

Source of truth: `DatapackDeityManager`.

## Path

`data/<namespace>/deities/<file>.json`

## Full Authoring Shape

```json
{
  "id": "<namespace>:<deity_name>",
  "name": "<display_name>",
  "description": "<description_text>",
  "colors": {
    "red": 255,
    "green": 255,
    "blue": 255
  },
  "progression": {
    "max_reputation": 100,
    "stages": [
      {
        "id": "<namespace>:<stage_id>",
        "reputation": 0,
        "major": false,
        "title": "<stage_display_title>",
        "requirements": [
          "research:<namespace>:<research_id>",
          "sign:<namespace>:<sign_name>"
        ],
        "rewards": [
          {
            "type": "item",
            "data": "<namespace>:<item_name>",
            "count": 1
          },
          {
            "type": "effect",
            "data": "<namespace>:<effect_name>",
            "duration": 200,
            "amplifier": 0
          },
          {
            "type": "sign",
            "data": "<namespace>:<sign_name>"
          },
          "<command_string>"
        ]
      }
    ]
  },
  "unlock_rewards": {
    "<stage_id_or_stage_name>": {
      "signs": ["<namespace>:<sign_name>"],
      "items": [
        {
          "item": "<namespace>:<item_name>",
          "count": 1
        }
      ],
      "effects": [
        {
          "effect": "<namespace>:<effect_name>",
          "duration": 200,
          "amplifier": 0
        }
      ]
    }
  },
  "prayer_types": ["<prayer_type_name>"],
  "deity_damage_type": "<namespace>:<damage_type_name>",
  "abandon": {
    "reputation_penalty": 1.0,
    "reset_reputation": true,
    "message": "<message_text>"
  },
  "linked_eidolon_deity": "<namespace>:<eidolon_deity_name>",
  "ai_configuration": {
    "deity": "<namespace>:<deity_name>",
    "ai_provider": "<provider_name>",
    "personality": "<system_prompt_text>"
  }
}
```

## Required Fields

- `id`
- `name`
- `colors.red`
- `colors.green`
- `colors.blue`

Everything else is optional but important for gameplay.

## Progression Notes

- `progression.max_reputation` defaults to `100` if omitted.
- Each stage `id` is parsed as a resource location.
- `title` is stored and later reused by systems such as patron enthrall gating (see `stageRequiredForEntrall` in `patron_config`).
- `requirements` currently recognizes at least `research:` and `sign:` prefixes during stage loading.
- `description` is a lore-flavored string displayed at milestone unlock. All bundled deities carry a `description` per stage.
- stage `major` defaults to `false`.

## Stage Schema (Full)

```json
{
  "id": "<stage_id>",
  "reputation": 0,
  "major": false,
  "title": "<stage_display_title>",
  "description": "<lore_flavor_text>",
  "requirements": [
    "research:<namespace>:<research_id>",
    "sign:<namespace>:<sign_name>"
  ],
  "rewards": [
    { "type": "item", "data": "<namespace>:<item_name>", "count": 1 },
    { "type": "effect", "data": "<namespace>:<effect_name>", "duration": 200, "amplifier": 0 },
    { "type": "sign", "data": "<namespace>:<sign_name>" },
    "<command_string>"
  ]
}
```

The `description` field is displayed to the player at milestone unlock and is also available for AI system context.

## Reward Notes

Stage `rewards` accepts two styles:

- structured objects with `type` and `data`
- raw command strings

Structured reward types handled in `DatapackDeityManager.loadProgression` are:

- `item`
- `effect`
- `sign`

Runtime note:

- command-string rewards are stored as direct commands and executed later by `DatapackDeity.applyReward`.
- stage rewards are tracked to avoid duplicate grants for the same player and stage unlock.

## Unlock Reward Container

`unlock_rewards` is still parsed separately from stage-local `rewards`. It supports:

- `signs`
- `items`
- `effects`

## Prayer And Abandon Blocks

- `prayer_types` is a simple list of prayer type names.
- `deity_damage_type` is optional and defines the base combat damage identity used by deity-linked chant effects such as `projectile_effect` and `magic_weapon` when those effects omit their own `damage_type`.
- `abandon` supports `reputation_penalty`, `reset_reputation`, and `message`.

## `deity_damage_type`

- value must be a valid damage type resource ID
- bundled examples now use `eidolonunchained:light_divine_power` and `eidolonunchained:dark_divine_power`
- if you want Eidolon magic-power scaling to apply, include the referenced damage type in `forge:is_magic`
- the damage type keeps its own identity and death-message key; it is not rewritten to generic magic

## `linked_eidolon_deity`

When present, `DatapackDeity` mirrors reputation changes to that native Eidolon deity at `0.5x` the delta and then manually triggers that deity's reputation-change handler.

## `ai_configuration`

`DatapackDeityManager` also supports a consolidated authoring pattern where AI config is embedded directly inside the deity JSON under `ai_configuration`.

Important note:

- this block is parsed through GSON into `AIDeityConfig` and registered with `AIDeityManager`
- if you are using the dedicated `ai_deities/` files as the primary authoring path, treat `ai_configuration` as an optional alternate surface rather than the canonical one

[Overview](Overview.md) | [AI Deity JSON](AI-Deity-JSON.md) | [Chant JSON](Chant-JSON.md) | [Deity JSON](Deity-JSON.md) | [Codex JSON](Codex-JSON.md) | [Research JSON](Research-JSON.md) | [Fate JSON](Fate-JSON.md) | [Facts JSON](Facts-JSON.md) | [Keybind Settings JSON](Keybind-Settings-JSON.md) | [Ritual And Recipe Notes](Ritual-Recipes.md) | [Localization](Localization.md)
