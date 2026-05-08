# Facts JSON

[Overview](Overview.md) | [AI Deity JSON](AI-Deity-JSON.md) | [Chant JSON](Chant-JSON.md) | [Deity JSON](Deity-JSON.md) | [Codex JSON](Codex-JSON.md) | [Research JSON](Research-JSON.md) | [Fate JSON](Fate-JSON.md) | [Facts JSON](Facts-JSON.md) | [Keybind Settings JSON](Keybind-Settings-JSON.md) | [Ritual And Recipe Notes](Ritual-Recipes.md) | [Localization](Localization.md)

Source of truth: `FactsSuggestionManager`.

## Path

`data/<namespace>/facts/<file>.json`

The file name defines the fact ID. For example, `facts/<fact_name>.json` becomes `<namespace>:<fact_name>`.

## Full Schema

```json
{
  "title": "<readable_fact_title>",
  "description": "<description_text>",
  "tags": [
    "category:<category_name>",
    "deity:<namespace>:<deity_name>",
    "<custom_tag_text>"
  ]
}
```

## Field Notes

- `title` defaults to the file name when omitted.
- `description` defaults to an empty string.
- `tags` defaults to an empty list.
- extra top-level keys are ignored by `FactsSuggestionManager`

## Boundary

These JSON files describe known facts for suggestion and lookup purposes. They do not define the gameplay logic that grants or revokes facts.

[Overview](Overview.md) | [AI Deity JSON](AI-Deity-JSON.md) | [Chant JSON](Chant-JSON.md) | [Deity JSON](Deity-JSON.md) | [Codex JSON](Codex-JSON.md) | [Research JSON](Research-JSON.md) | [Fate JSON](Fate-JSON.md) | [Facts JSON](Facts-JSON.md) | [Keybind Settings JSON](Keybind-Settings-JSON.md) | [Ritual And Recipe Notes](Ritual-Recipes.md) | [Localization](Localization.md)
