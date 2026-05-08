# Datapack Overview

[Overview](Overview.md) | [AI Deity JSON](AI-Deity-JSON.md) | [Chant JSON](Chant-JSON.md) | [Chantable Mobs JSON](Chantable-Mobs-JSON.md) | [Deity JSON](Deity-JSON.md) | [Codex JSON](Codex-JSON.md) | [Research JSON](Research-JSON.md) | [Fate JSON](Fate-JSON.md) | [Facts JSON](Facts-JSON.md) | [Keybind Settings JSON](Keybind-Settings-JSON.md) | [Ritual And Recipe Notes](Ritual-Recipes.md) | [Localization](Localization.md)

This section documents the live datapack surfaces from the Java loaders, not just the bundled sample files.

## Source Of Truth

- `DatapackChantManager` and `DatapackChant` for `chants/`
- `ChantableMobManager` for `chantable_mobs/`
- `DatapackDeityManager` and `DatapackDeity` for `deities/`
- `AIDeityManager` and `AIDeityConfig` for `ai_deities/`
- `ResearchDataManager` for `research/` and legacy `research_chapters/`
- `FateDataLoader` for `fates/`
- `FactsSuggestionManager` for `facts/`
- `CodexDataManager` for `codex/`, `codex_entries/`, and legacy `codex_chapters/`
- `KeybindSignEffectsManager` for `keybind_settings/`
- `RitualDataManager` for `rituals/`

## Live Datapack Roots

The current bundled namespace includes these top-level data folders:

- `data/<namespace>/ai_deities/`
- `data/<namespace>/chants/`
- `data/<namespace>/chantable_mobs/`
- `data/<namespace>/codex/`
- `data/<namespace>/codex_chapters/` (legacy-supported)
- `data/<namespace>/codex_entries/`
- `data/<namespace>/deities/`
- `data/<namespace>/facts/`
- `data/<namespace>/fates/`
- `data/<namespace>/keybind_settings/`
- `data/<namespace>/recipes/`
- `data/<namespace>/research/`
- `data/<namespace>/research_chapters/` (legacy-supported)
- `data/<namespace>/rituals/`

## Practical Boundaries

- `recipes/` contains normal recipe JSON consumed by vanilla or Eidolon recipe serializers.
- `rituals/` is the dedicated Eidolon Unchained AI ritual loader path.
- `codex/` and `codex_entries/` work together; category and chapter files are not enough by themselves.
- `research/` supports two shapes: trigger-based research and codex-style research entries.
- `ai_deities/` is the largest schema surface and includes nested configs for prayers, patrons, tasks, triggers, and TTS.
- `chantable_mobs/` configures which mob types cast chants in rotation, their assigned deity, and combat casting parameters.

## Authoring Conventions Used In This Section

- Resource locations are shown as placeholders such as `<namespace>:<deity_name>` or `<namespace>:<mob_entity_name>`.
- Paths are shown generically as `data/<namespace>/...` unless a loader is hard-wired differently.
- Compatibility aliases are called out explicitly when the Java code accepts them.
- If the runtime only infers metadata rather than reading a field directly, that is noted on the page.

## Recommended Reading Order

1. Start with [Deity JSON](Deity-JSON.md) and [AI Deity JSON](AI-Deity-JSON.md).
2. Then read [Chant JSON](Chant-JSON.md) and [Chantable Mobs JSON](Chantable-Mobs-JSON.md) and [Ritual And Recipe Notes](Ritual-Recipes.md).
3. Use [Codex JSON](Codex-JSON.md), [Research JSON](Research-JSON.md), [Facts JSON](Facts-JSON.md), and [Fate JSON](Fate-JSON.md) for progression content.
4. Use [Keybind Settings JSON](Keybind-Settings-JSON.md) and [Localization](Localization.md) for client-facing polish.

[Overview](Overview.md) | [AI Deity JSON](AI-Deity-JSON.md) | [Chant JSON](Chant-JSON.md) | [Chantable Mobs JSON](Chantable-Mobs-JSON.md) | [Deity JSON](Deity-JSON.md) | [Codex JSON](Codex-JSON.md) | [Research JSON](Research-JSON.md) | [Fate JSON](Fate-JSON.md) | [Facts JSON](Facts-JSON.md) | [Keybind Settings JSON](Keybind-Settings-JSON.md) | [Ritual And Recipe Notes](Ritual-Recipes.md) | [Localization](Localization.md)
