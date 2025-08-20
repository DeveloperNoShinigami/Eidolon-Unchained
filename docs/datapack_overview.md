# Datapack Overview

## Folder Layout

```
data/
└── <namespace>/
    ├── codex_categories/      # Top-level codex categories
    ├── codex_chapters/        # Chapter definitions referenced by entries
    ├── codex_entries/         # Page content for chapters
    ├── research_chapters/     # Research chapter definitions
    └── research_entries/      # Individual research nodes
```

## Content Flow: Category → Chapter → Page

1. **Category** (`codex_categories/`)
   - Groups related chapters under a shared icon and name.
   - Lists chapter IDs via `chapters`.
2. **Chapter** (`codex_chapters/`)
   - Holds pages belonging to the codex.
   - Targeted by entries with `target_chapter`.
3. **Page** (`codex_entries/`)
   - Contains the actual text, recipes, or other page types displayed in-game.
   - Ordered within the `pages` array of an entry.
4. **Research**
   - `research_chapters/` organize research topics and can reference a category ID.
   - `research_entries/` link to a research chapter and may unlock codex entries.
   - Entries may specify `conditions` that must be met before their `tasks` can progress. See [Research Conditions](RESEARCH_CONDITIONS.md) for available types.

## Required and Optional Fields

| File Type | Required Fields | Optional Fields |
|-----------|-----------------|-----------------|
| **Category** | `key`, `name`, `icon`, `chapters` | `description` |
| **Codex Chapter** | `title` | `icon` |
| **Codex Entry** | `target_chapter`, `pages` | `title`, `description`, `icon`, `prerequisites`, `type` |
| **Research Chapter** | `id`, `title`, `description`, `category`, `icon` | `background`, `position` |
| **Research Entry** | `id`, `title`, `description`, `chapter` | `icon`, `prerequisites`, `unlocks`, `star_requirement`, `conditions`, `tasks`, `rewards` |

### Cross References

- Category `chapters` values must match file names in `codex_chapters/`.
- Each codex entry's `target_chapter` must exist in `codex_chapters/`.
- Research chapters reference a category ID so they appear under the same codex section.
- Research entries reference their parent research chapter via `chapter`.
- Translation keys for titles and text belong in `assets/<namespace>/lang/<lang>.json`.

For more about research entry fields and available gating options, see [research_entries.md](research_entries.md) and [RESEARCH_CONDITIONS.md](RESEARCH_CONDITIONS.md).

## Best Practices

- Use **lowercase IDs** for file names and references.
- Keep a **consistent namespace** across categories, chapters, and entries.
- Provide language keys for every `title`, `name`, and page text.
- Validate JSON and ensure all cross-referenced IDs exist.

## JSON Examples

### Category (`data/<namespace>/codex_categories/magic.json`)
```json
{
  "key": "yourmod.codex.category.magic",
  "name": "yourmod.codex.category.magic.name",
  "icon": "minecraft:book",
  "chapters": [
    "yourmod:rituals",
    "yourmod:artifacts"
  ]
}
```

### Codex Chapter (`data/<namespace>/codex_chapters/rituals.json`)
```json
{
  "title": "yourmod.codex.chapter.rituals",
  "icon": "minecraft:bell"
}
```

### Codex Entry (`data/<namespace>/codex_entries/community_rite.json`)
```json
{
  "target_chapter": "yourmod:rituals",
  "title": "yourmod.codex.entry.community_rite",
  "pages": [
    { "type": "title", "text": "yourmod.codex.entry.community_rite.title" },
    { "type": "text",  "text": "yourmod.codex.entry.community_rite.body" }
  ]
}
```

### Research Chapter (`data/<namespace>/research_chapters/void_mastery.json`)
```json
{
  "id": "yourmod:void_mastery",
  "title": "yourmod.research.void_mastery",
  "description": "yourmod.research.void_mastery.desc",
  "category": "yourmod.codex.category.magic",
  "icon": { "item": "minecraft:ender_eye" }
}
```

### Research Entry (`data/<namespace>/research_entries/void_step.json`)
```json
{
  "id": "yourmod:void_step",
  "title": "yourmod.research.void_step",
  "description": "yourmod.research.void_step.desc",
  "chapter": "yourmod:void_mastery",
  "star_requirement": 2,
  "conditions": { "dimension": "minecraft:the_nether" },
  "tasks": {
    "tier_1": [
      { "type": "use_ritual", "ritual": "yourmod:void_portal", "count": 1 }
    ]
  },
  "rewards": {
    "unlock_codex": [ "yourmod:community_rite" ]
  }
}
```

In this example, tasks only track progress while the player is in the Nether because of the `dimension` condition. See [Research Conditions](RESEARCH_CONDITIONS.md) for more options.
