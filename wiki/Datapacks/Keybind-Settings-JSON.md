# Keybind Settings JSON

[Overview](Overview.md) | [AI Deity JSON](AI-Deity-JSON.md) | [Chant JSON](Chant-JSON.md) | [Deity JSON](Deity-JSON.md) | [Codex JSON](Codex-JSON.md) | [Research JSON](Research-JSON.md) | [Fate JSON](Fate-JSON.md) | [Facts JSON](Facts-JSON.md) | [Keybind Settings JSON](Keybind-Settings-JSON.md) | [Ritual And Recipe Notes](Ritual-Recipes.md) | [Localization](Localization.md)

Source of truth: `KeybindSignEffectsManager`.

## Path

`data/<namespace>/keybind_settings/sign_effects.json`

`KeybindSignEffectsManager` also normalizes sequence entries through `UnifiedDynamicSystemLoader`, so the top-level `sequences` container is treated as a map of chant IDs to sequence definitions.

## Full Schema

```json
{
  "per_sign": {
    "<namespace>:<sign_name>": "<command_string>",
    "<namespace>:<sign_name>": [
      "<command_string>",
      "<command_string>"
    ]
  },
  "sequences": {
    "<namespace>:<chant_name>": {
      "commands": [
        "<command_for_sign_index_0>",
        "<command_for_sign_index_1>",
        "<command_for_sign_index_2>"
      ]
    }
  }
}
```

## `per_sign`

`per_sign` maps a sign resource ID to either:

- one command string
- an array of command strings

All commands in the stored list run when that sign is pressed.

## `sequences`

`sequences` maps a chant ID to a positional command list.

Each sequence value may be either:

- an object with a `commands` array
- a raw array of command strings

Important behavior:

- the command at index `0` runs on the first sign press of that chant
- the command at index `1` runs on the second sign press
- and so on

This is not a generic run-all effect block. It is indexed by sign position within the chant input sequence.

Loader note:

- sequence entries are also normalized across datapack files through `UnifiedDynamicLoader.normalizeResourceMap`, so both aggregated `sign_effects.json` authoring and file-per-entry sequence authoring are supported

## Runtime Notes

- commands run from the player's command source with elevated permission
- blank commands are ignored
- `@s` is replaced with the player's name string before execution
- chant IDs without a namespace default to `eidolonunchained:<chant_name>`

[Overview](Overview.md) | [AI Deity JSON](AI-Deity-JSON.md) | [Chant JSON](Chant-JSON.md) | [Deity JSON](Deity-JSON.md) | [Codex JSON](Codex-JSON.md) | [Research JSON](Research-JSON.md) | [Fate JSON](Fate-JSON.md) | [Facts JSON](Facts-JSON.md) | [Keybind Settings JSON](Keybind-Settings-JSON.md) | [Ritual And Recipe Notes](Ritual-Recipes.md) | [Localization](Localization.md)
