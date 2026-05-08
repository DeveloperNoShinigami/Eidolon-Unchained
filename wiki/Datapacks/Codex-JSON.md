# Codex JSON

[Overview](Overview.md) | [AI Deity JSON](AI-Deity-JSON.md) | [Chant JSON](Chant-JSON.md) | [Deity JSON](Deity-JSON.md) | [Codex JSON](Codex-JSON.md) | [Research JSON](Research-JSON.md) | [Fate JSON](Fate-JSON.md) | [Facts JSON](Facts-JSON.md) | [Keybind Settings JSON](Keybind-Settings-JSON.md) | [Ritual And Recipe Notes](Ritual-Recipes.md) | [Localization](Localization.md)

Source of truth: `CodexDataManager`.

## Paths

- category metadata: `data/<namespace>/codex/<category_name>/_category.json`
- chapter definitions, new layout: `data/<namespace>/codex/<category_name>/<chapter_name>.json`
- chapter definitions, legacy layout: `data/<namespace>/codex_chapters/<chapter_name>.json`
- entry files: `data/<namespace>/codex_entries/<entry_name>.json`

## Category Schema

```json
{
  "key": "<category_key>",
  "name": "<translation_key>",
  "icon": "<namespace>:<item_name>",
  "color": "0xRRGGBB",
  "description": "<translation_key_or_literal>"
}
```

If omitted, `CodexDataManager` falls back to:

- `key`: folder name
- `name`: `eidolon.codex.category.<category_key>`
- `icon`: `minecraft:book`

Color note:

- `color` is parsed as a hexadecimal string after removing a leading `0x` prefix.

## Chapter Schema

```json
{
  "title": "<translation_key_or_literal>",
  "icon": "<namespace>:<item_name>",
  "category": "<category_key>",
  "unlock": {
    "fact": "<namespace>:<fact_id>",
    "facts": ["<namespace>:<fact_id>"],
    "research": "<namespace>:<research_id>",
    "researches": ["<namespace>:<research_id>"],
    "reputation": {
      "deity": "<namespace>:<deity_name>",
      "min": 0
    }
  }
}
```

Notes:

- In the new `codex/<category>/<chapter>.json` layout, `category` is inferred from the folder name.
- In legacy `codex_chapters/`, `category` can be supplied explicitly and defaults to `artifice`.
- `title` is effectively required for both chapter layouts; files without it are skipped.

## Entry Schema

```json
{
  "target_chapter": "<namespace>:<chapter_id>",
  "title": "<translation_key_or_literal>",
  "description": "<translation_key_or_literal>",
  "icon": {
    "item": "<namespace>:<item_name>",
    "count": 1,
    "nbt": "{<nbt_payload>}"
  },
  "prerequisites": ["<namespace>:<research_or_entry_id>"],
  "type": "text",
  "pages": [
    {
      "type": "<page_type>",
      "text": "<translation_key_or_literal>"
    }
  ],
  "<custom_field>": "<custom_value>"
}
```

Required entry fields:

- `target_chapter`
- `pages`

Behavior notes:

- The file path becomes the entry ID.
- If `target_chapter` is written without a namespace, the loader converts it to `eidolonunchained:<lowercased_value>`.
- Any unrecognized top-level fields are preserved as additional data.
- The renderer only supports page types implemented in Java; JSON alone cannot invent a new page renderer.

Accepted entry `type` values from `CodexEntry.EntryType` are:

- `text`
- `title`
- `entity`
- `crafting`
- `ritual`
- `crucible`
- `list`
- `smelting`
- `workbench`

Unknown entry types currently fall back to `text` with a warning.

## Icon Container

The `icon` field accepts either:

- a string item ID
- an object with `item`, optional `count`, and optional `nbt`

Page note:

- each `pages` element must be a JSON object to be kept
- pages missing `type` are still loaded, but the loader logs a warning

## Important Coupling

- `ResearchDataManager` imports custom codex chapters and turns them into research chapters.
- An invalid or missing chapter can therefore break both the codex and research surfaces.

## Custom Category Structure

Custom codex categories are placed at `data/<namespace>/codex/<category_name>/`. The folder name becomes the category key. Each category folder can contain:

- `_category.json` — category metadata (key, name, icon, color, description). If omitted, defaults are inferred from the folder name.
- `<chapter_name>.json` — chapter definitions that belong to this category.

Bundled examples:

- `codex/dark_rites/` — "Dark Rites" category with wither rose icon and purple color, containing the `shadow_observances` chapter.
- `codex/light_rites/` — "Light Rites" category with equivalent radiant styling, containing the `radiant_observances` chapter.

## Chapter Extension Mechanism

`CodexDataManager` maintains a `CHAPTER_EXTENSIONS` map for entries that extend an existing chapter rather than defining their own chapter. Entries that specify a `target_chapter` pointing to an already-loaded chapter are placed into that chapter directly, without needing to own the chapter definition. This allows separate datapack namespaces to add content into base chapters.

[Overview](Overview.md) | [AI Deity JSON](AI-Deity-JSON.md) | [Chant JSON](Chant-JSON.md) | [Deity JSON](Deity-JSON.md) | [Codex JSON](Codex-JSON.md) | [Research JSON](Research-JSON.md) | [Fate JSON](Fate-JSON.md) | [Facts JSON](Facts-JSON.md) | [Keybind Settings JSON](Keybind-Settings-JSON.md) | [Ritual And Recipe Notes](Ritual-Recipes.md) | [Localization](Localization.md)
