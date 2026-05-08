# Localization

[Overview](Overview.md) | [AI Deity JSON](AI-Deity-JSON.md) | [Chant JSON](Chant-JSON.md) | [Deity JSON](Deity-JSON.md) | [Codex JSON](Codex-JSON.md) | [Research JSON](Research-JSON.md) | [Fate JSON](Fate-JSON.md) | [Facts JSON](Facts-JSON.md) | [Keybind Settings JSON](Keybind-Settings-JSON.md) | [Ritual And Recipe Notes](Ritual-Recipes.md) | [Localization](Localization.md)

Localization is not its own loader, but some datapack loaders convert selected string fields into `Component.translatable` values.

## Datapack Fields With Loader-Side Translation Handling

- codex category `name`
- codex category `description`
- codex chapter `title`
- codex entry `title`
- codex entry `description`
- research `title`
- research `description`

Heuristic note:

- in the codex and research loaders, strings containing `.` or `:` are generally treated as translation keys; other strings are treated as literals

Non-goal note:

- chant names, deity display text, and AI deity prompt/personality fields are not generically run through `Component.translatable` by their loaders

## Practical Rule

If a datapack field is rendered through `Component.translatable`, the localization key must exist in the active language file or the raw key text will appear in game.

## Current Workspace Language Files

- `src/main/resources/assets/eidolonunchained/lang/en_us.json`
- `src/main/resources/assets/eidolonunchained/lang/en_us_standardized.json`
- `src/main/resources/assets/eidolonunchained/lang/en_us_backup.json`

For normal authoring, treat `en_us.json` as the live file unless you are intentionally editing a cleanup or backup file.

[Overview](Overview.md) | [AI Deity JSON](AI-Deity-JSON.md) | [Chant JSON](Chant-JSON.md) | [Deity JSON](Deity-JSON.md) | [Codex JSON](Codex-JSON.md) | [Research JSON](Research-JSON.md) | [Fate JSON](Fate-JSON.md) | [Facts JSON](Facts-JSON.md) | [Keybind Settings JSON](Keybind-Settings-JSON.md) | [Ritual And Recipe Notes](Ritual-Recipes.md) | [Localization](Localization.md)
