# Custom Codex Categories

Eidolon Unchained allows datapacks to add entirely new categories of content to the in-game codex. Each category lives in its own folder and supplies metadata alongside individual chapter files.

## Directory Naming

- Create a folder under `data/<namespace>/codex/` for each new category.
- Use lowercase names with underscores.
- The folder name **must** match the `key` field in the category's `_category.json` file.
- Example: the `custom_spells` category resides in `data/eidolonunchained/codex/custom_spells/`.

## `_category.json`

Place a `_category.json` file inside the category folder to define how it appears in the codex. Required fields:

- `key` – Identifier for the category (must match folder name).
- `name` – Translation key for the display name.
- `icon` – Item ID to show on the tab.
- `color` – Hex color code used for the tab.
- `description` – Short explanation shown in the codex.

Example from `custom_spells`:

```json
{
  "key": "custom_spells",
  "name": "eidolonunchained.codex.category.custom_spells",
  "icon": "minecraft:enchanted_book",
  "color": "0x4169E1",
  "description": "Community-created magical techniques and spell combinations"
}
```

## Chapter JSON Files

Every additional JSON file within the category folder represents a chapter entry. Each file should contain:

- `target_chapter` – Existing chapter ID to attach the entry to.
- `title` – Display title or translation key.
- `icon` – Item icon shown on the title page.
- `pages` – Array of page definitions using the standard codex page types.

Example chapter:

```json
{
  "target_chapter": "rituals",
  "title": "Fire Mastery",
  "icon": "minecraft:fire_charge",
  "pages": [ ... ]
}
```

For fully worked examples, see the files in `src/main/resources/data/eidolonunchained/codex/custom_spells/`.
