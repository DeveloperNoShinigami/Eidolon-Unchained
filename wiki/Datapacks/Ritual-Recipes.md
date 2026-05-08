# Ritual And Recipe Notes

[Overview](Overview.md) | [AI Deity JSON](AI-Deity-JSON.md) | [Chant JSON](Chant-JSON.md) | [Deity JSON](Deity-JSON.md) | [Codex JSON](Codex-JSON.md) | [Research JSON](Research-JSON.md) | [Fate JSON](Fate-JSON.md) | [Facts JSON](Facts-JSON.md) | [Keybind Settings JSON](Keybind-Settings-JSON.md) | [Ritual And Recipe Notes](Ritual-Recipes.md) | [Localization](Localization.md)

Source of truth: `RitualDataManager` for `rituals/`, plus the normal recipe serializers for `recipes/`.

## Two Different Surfaces

The codebase currently uses both `recipes/` and `rituals/`, but they are not interchangeable.

### `recipes/`

Bundled content currently includes chant and ritual recipe JSON under paths such as:

- `data/<namespace>/recipes/<chant_name>.json`
- `data/<namespace>/recipes/rituals/<ritual_name>.json`

These files are normal recipe JSON handled by recipe serializers such as:

- `eidolon:chant`
- `eidolon:ritual_brazier`
- `eidolon:ritual_brazier_command`

### `rituals/`

`RitualDataManager` specifically reads:

- `data/<namespace>/rituals/<file>.json`

These entries are stored as raw JSON and only become AI deity rituals when they include `linked_deity`.

## AI Ritual Loader Shape

```json
{
  "linked_deity": "<namespace>:<deity_name>",
  "color": {
    "r": 0.5,
    "g": 0.5,
    "b": 0.5
  },
  "symbol": "<namespace>:<particle_or_symbol_resource>"
}
```

Parsed keys used directly by `RitualDataManager.registerRitualsWithEidolon`:

- `linked_deity`
- `color.r`
- `color.g`
- `color.b`
- `symbol`

If `linked_deity` is missing, the ritual is loaded into the internal map but skipped for AI ritual registration.

Default behavior:

- missing `color` falls back to neutral gray `r=0.5`, `g=0.5`, `b=0.5`
- missing `symbol` falls back to `eidolon:particle/daylight_ritual`

Boundary note:

- other fields such as `description` remain available in the raw ritual JSON map, but `registerRitualsWithEidolon` does not currently use them when building `AIDeityRitual`

## Documentation Rule

- Document `recipes/` when you mean the shipped recipe content players author against for chant and brazier serializers.
- Document `rituals/` when you mean the dedicated Eidolon Unchained AI ritual loader.
- Do not collapse them into one schema page until the Java loaders are actually unified.

[Overview](Overview.md) | [AI Deity JSON](AI-Deity-JSON.md) | [Chant JSON](Chant-JSON.md) | [Deity JSON](Deity-JSON.md) | [Codex JSON](Codex-JSON.md) | [Research JSON](Research-JSON.md) | [Fate JSON](Fate-JSON.md) | [Facts JSON](Facts-JSON.md) | [Keybind Settings JSON](Keybind-Settings-JSON.md) | [Ritual And Recipe Notes](Ritual-Recipes.md) | [Localization](Localization.md)
