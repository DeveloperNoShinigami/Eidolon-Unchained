# Eidolon Unchained System Summary

## Overview
The codex extension system lets datapacks add custom knowledge entries to Eidolon Repraised. Entries are defined in JSON and can include icons, images and rich text formatting.

## Current Metrics
- **Codex entries**: 56 JSON files loaded from `data/eidolonunchained/codex_entries` and `data/eidolonunchained/codex`.
- **Chapters**: Content spans 10 chapters (8 base Eidolon chapters plus 2 custom mod chapters; a third custom chapter is available for future use).
- **Supported page types**: 10 – `text`, `title`, `entity`, `crafting`, `ritual`, `ritual_recipe`, `crucible`, `list`, `smelting`, and `workbench`.
  - `ritual` shows the full ritual circle using a ritual ID
  - `ritual_recipe` references a ritual by ID without the circle


## Example Entry
```json
{
  "target_chapter": "eidolon:arcane_gold",
  "title": "Arcane Gold",
  "icon": "eidolon:arcane_gold_ingot",
  "pages": [
    { "type": "text", "text": "Transmuted metal infused with void energy." },
    { "type": "crafting_recipe", "recipe": "eidolon:arcane_gold_ingot" }
  ]
}
```

## Debug and Translation Tools
- `/eidolonunchained reload_codex` – reload codex data at runtime.
- `/eidolonunchained test_translations` – verify translation keys and fallbacks.

## Further Documentation
- [Documentation Index](../README.md)
- [Datapack Overview](../datapack/overview.md)
- [Datapack Structure](../datapack/structure.md)
- [Research Conditions](../research/condition_types.md)
- [Research Entries](../research/entry_reference.md)
- [Codex Reference](../codex/reference.md)
- [Codex Tutorial](../codex/tutorial.md)
- [Best Practices](../datapack/best_practices.md)
- [UI Customization](ui_texture_customization.md)
- [Example Complete Codex Entry](../EXAMPLE_COMPLETE_CODEX_ENTRY.json)
