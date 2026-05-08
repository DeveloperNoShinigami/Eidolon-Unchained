# Research JSON

[Overview](Overview.md) | [AI Deity JSON](AI-Deity-JSON.md) | [Chant JSON](Chant-JSON.md) | [Deity JSON](Deity-JSON.md) | [Codex JSON](Codex-JSON.md) | [Research JSON](Research-JSON.md) | [Fate JSON](Fate-JSON.md) | [Facts JSON](Facts-JSON.md) | [Keybind Settings JSON](Keybind-Settings-JSON.md) | [Ritual And Recipe Notes](Ritual-Recipes.md) | [Localization](Localization.md)

Source of truth: `ResearchDataManager`.

## Paths

- Canonical entry path: `data/<namespace>/research/<file>.json`
- Legacy chapter path still scanned: `data/<namespace>/research_chapters/<file>.json`
- Research chapters are also synthesized from custom codex chapters loaded by `CodexDataManager`

## Research Chapter Schema

`ResearchDataManager.loadResearchChapter` reads standalone chapter definitions from `research_chapters/`.

```json
{
  "id": "<namespace>:<chapter_id>",
  "title": "<translation_key_or_literal>",
  "description": "<translation_key_or_literal>",
  "icon": {
    "item": "<namespace>:<item_name>",
    "count": 1
  },
  "sort_order": 0,
  "secret": false,
  "background": "<namespace>:<background_texture_or_resource>",
  "category": "<eidolon_research_category_name>"
}
```

Field notes:

- `id` is required.
- `title` defaults to the chapter ID path when omitted.
- `description` defaults to an empty string.
- `icon` is only parsed as an object container here, not as a plain string item ID.
- `sort_order` defaults to `0`.
- `secret` defaults to `false`.
- `background` is optional.
- `category` defaults to `nature`.

## Supported Entry Shapes

`ResearchDataManager` accepts two different research formats.

### 1. Trigger-Based Research

This is the format selected when the JSON has both `triggers` and `stars`.

```json
{
  "id": "<research_entry_id>",
  "stars": 1,
  "triggers": [
    {
      "type": "<trigger_type>",
      "biome": "<namespace>:<biome_name>",
      "max_found": 1
    }
  ],
  "tasks": {
    "0": [
      {
        "type": "item",
        "item": "<namespace>:<item_name>",
        "count": 1,
        "command": "<command_string>",
        "commands": ["<command_string>"]
      }
    ]
  },
  "rewards": []
}
```

Parsed fields in this mode:

- `id`
- `stars`
- `triggers`
- `tasks`
- `rewards`

Behavior notes:

- `tasks` must be an object keyed by stringified tier numbers such as `"0"`, `"1"`, `"2"`.
- `ResearchDataManager` uses `triggers` to detect this format, and `ResearchTriggerLoader` separately parses the same `triggers` array for runtime discovery behavior.
- Trigger-based entries are placed into the generated chapter `eidolonunchained:trigger_research`.
- The generated chapter is created automatically with a book icon, sort order `100`, `secret: false`, background `eidolon:textures/gui/research_bg.png`, and category `basics`.

## Trigger Schema For Trigger-Based Research

The `triggers` array is also read by `ResearchTriggerLoader`, which loads trigger definitions from both:

- `data/<namespace>/research/<file>.json`
- legacy `data/<namespace>/eidolon_research/<file>.json`

Simple string triggers are skipped by `ResearchTriggerLoader` and left to Eidolon's native research system. Complex object triggers are deserialized through `ResearchTrigger`.

### Full Trigger Container

```json
{
  "type": "<trigger_type>",
  "entity": "<namespace>:<entity_name>",
  "block": "<namespace>:<block_name>",
  "ritual": "<namespace>:<ritual_name>",
  "dimension": "<namespace>:<dimension_name>",
  "biome": "<namespace>:<biome_name>",
  "structure": "<namespace>:<structure_name>",
  "proximity_range": 1.0,
  "max_found": 1,
  "coordinates": {
    "x": 0,
    "y": 64,
    "z": 0,
    "range": 50.0
  },
  "nbt": "{<nbt_payload>}",
  "item_requirements": {
    "check_inventory": true,
    "items": [
      {
        "item": "<namespace>:<item_name>",
        "count": 1,
        "nbt": "{<nbt_payload>}"
      }
    ]
  }
}
```

### Currently Handled Trigger Types

The current runtime handlers react to these trigger categories:

- `kill_entity`
- `block_interaction`
- `ritual`
- location-style triggers using `dimension`
- location-style triggers using `biome`
- location-style triggers using `structure`

Practical notes:

- `max_found` limits how many times a player can discover the same research from that trigger.
- `item_requirements` is checked by `ItemRequirementChecker` before discovery succeeds.
- `nbt` is parsed for object triggers, but entity NBT matching is still marked as pending/fixed later for kill triggers.
- `coordinates` and `proximity_range` exist on the trigger data model; document them as supported fields on the trigger container even though the currently visible handlers are centered on entity, block, ritual, biome, dimension, and structure checks.
- Most trigger discovery paths require Eidolon notetaking tools and award research notes rather than directly granting research completion.

### 2. Codex-Style Research

This is the format selected when the JSON has `chapter` or legacy `target_chapter`.

```json
{
  "id": "<namespace>:<research_entry_id>",
  "title": "<translation_key_or_literal>",
  "description": "<translation_key_or_literal>",
  "chapter": "<namespace>:<chapter_id>",
  "icon": {
    "item": "<namespace>:<item_name>",
    "count": 1,
    "nbt": "{<nbt_payload>}"
  },
  "prerequisites": ["<namespace>:<research_id>"],
  "unlocks": ["<namespace>:<research_id>"],
  "x": 0,
  "y": 0,
  "type": "basic",
  "required_stars": 0
}
```

Accepted aliases:

- `target_chapter` instead of `chapter`

Parsed fields in this mode:

- `id`
- `title`
- `description`
- `chapter` or `target_chapter`
- `icon`
- `prerequisites`
- `unlocks`
- `x`
- `y`
- `type`
- `required_stars`

Supported `type` values come from `ResearchEntry.ResearchType`:

- `basic`
- `advanced`
- `forbidden`
- `ritual`
- `crafting`

## Task Schema For Trigger-Based Research

The task parser currently recognizes these task types:

- `item`
- `kill`
- `kill_entity`
- `ritual_completion`
- `use_ritual`
- `craft`
- `craft_item`

### Item Task

```json
{
  "type": "item",
  "item": "<namespace>:<item_name>",
  "count": 1,
  "command": "<command_string>",
  "commands": ["<command_string>"]
}
```

### Kill Task

```json
{
  "type": "kill_entity",
  "entity": "<namespace>:<entity_name>",
  "count": 1,
  "commands": ["<command_string>"]
}
```

### Ritual Task

```json
{
  "type": "ritual_completion",
  "ritual": "<namespace>:<ritual_name>",
  "count": 1,
  "commands": ["<command_string>"]
}
```

### Craft Task

```json
{
  "type": "craft_item",
  "item": "<namespace>:<item_name>",
  "count": 1,
  "station": "<crafting_station_name>",
  "commands": ["<command_string>"]
}
```

Task command notes:

- `command` stores one completion command.
- `commands` stores multiple completion commands.
- if both are present, both sources are collected.

## Icon Container

The codex-style `icon` field accepts either:

- a string item ID such as `"<namespace>:<item_name>"`
- an object container with `item`, optional `count`, and optional `nbt`

## Loader Boundary

The broader `ResearchEntry` Java model contains additional surfaces such as:

- `conditions`
- `additionalData`
- richer task serialization support in `toJson()`

The broader trigger subsystem also includes runtime classes outside `ResearchDataManager`, especially:

- `ResearchTriggerLoader`
- `LocationResearchTriggers`
- `KillResearchTriggers`
- `InteractionResearchTriggers`
- `RitualResearchTriggers`
- `CraftResearchTriggers`

Those are part of the in-memory model, but this specific loader currently only parses the entry and task fields documented above. The page is intentionally documenting loader-accepted authoring fields, not every property that exists on the Java object.

## Important Coupling

- `ResearchDataManager` imports custom chapters from `CodexDataManager`, so a chapter problem can be caused by codex data even when the failing file lives under `research/`.
- Some custom research task types exist as Java wrappers, but the direct JSON parser in this class currently only creates the task types listed above.
- Task completion commands can be declared with either `command` or `commands`.

[Overview](Overview.md) | [AI Deity JSON](AI-Deity-JSON.md) | [Chant JSON](Chant-JSON.md) | [Deity JSON](Deity-JSON.md) | [Codex JSON](Codex-JSON.md) | [Research JSON](Research-JSON.md) | [Fate JSON](Fate-JSON.md) | [Facts JSON](Facts-JSON.md) | [Keybind Settings JSON](Keybind-Settings-JSON.md) | [Ritual And Recipe Notes](Ritual-Recipes.md) | [Localization](Localization.md)
