# Fate JSON

[Overview](Overview.md) | [AI Deity JSON](AI-Deity-JSON.md) | [Chant JSON](Chant-JSON.md) | [Deity JSON](Deity-JSON.md) | [Codex JSON](Codex-JSON.md) | [Research JSON](Research-JSON.md) | [Fate JSON](Fate-JSON.md) | [Facts JSON](Facts-JSON.md) | [Keybind Settings JSON](Keybind-Settings-JSON.md) | [Ritual And Recipe Notes](Ritual-Recipes.md) | [Localization](Localization.md)

Source of truth: `FateDataLoader`.

## Path

`data/<namespace>/fates/<deity_id>/<file>.json`

The folder name helps organize files, but `FateDataLoader` still requires an explicit `linked_deity` field inside the JSON.

## Full Schema

```json
{
  "linked_deity": "<namespace>:<deity_name>",
  "task_id": "<task_id>",
  "display_name": "<display_name>",
  "description": "<description_text>",
  "progression_tier": "<progression_title_or_stage>",
  "requirements": [
    "<requirement_string>",
    {
      "type": "<requirement_type>",
      "<field_name>": "<field_value>"
    }
  ],
  "rewards": {
    "reputation": 0,
    "commands": ["<command_string>"],
    "progression_unlock": "<progression_stage>"
  },
  "cooldown_hours": 0,
  "repeatable": false,
  "ai_assignment_context": {
    "<context_key>": "<context_value>"
  }
}
```

## Required Field

- `linked_deity`

Without it, the file is skipped.

Loader note:

- `task_id`, `display_name`, `description`, and the other task fields are optional at parse time.
- if `task_id` is omitted, the task can still be attached to the deity, but it will not be stored in `FateDataLoader`'s lookup cache.

## Requirement Container Rules

`FateDataLoader.parseFate` accepts two requirement styles:

- a plain string requirement
- a structured object with at least a `type` field

Structured objects are serialized into the task template model as `type:<raw_json>` rather than being strongly typed at this loader layer.

## Rewards Container

The parsed rewards keys are:

- `reputation`
- `commands`
- `progression_unlock`

Anything else inside `rewards` is ignored at this loader layer.

## Load Order Note

If the matching AI deity config is not available yet, the fate is stored in a pending map and attached later.

[Overview](Overview.md) | [AI Deity JSON](AI-Deity-JSON.md) | [Chant JSON](Chant-JSON.md) | [Deity JSON](Deity-JSON.md) | [Codex JSON](Codex-JSON.md) | [Research JSON](Research-JSON.md) | [Fate JSON](Fate-JSON.md) | [Facts JSON](Facts-JSON.md) | [Keybind Settings JSON](Keybind-Settings-JSON.md) | [Ritual And Recipe Notes](Ritual-Recipes.md) | [Localization](Localization.md)
