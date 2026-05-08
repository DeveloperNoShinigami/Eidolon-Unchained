# Ritual System

Ritual behavior in the current build sits on top of Eidolon's ritual infrastructure.

## Runtime Loader

`RitualDataManager` listens to `data/*/rituals/` and registers deity-linked ritual entries into Eidolon's ritual registry.

When a ritual JSON includes `linked_deity`, the manager creates an `AIDeityRitual` and registers it through `RitualRegistry`.

## AI Deity Ritual Behavior

`AIDeityRitual` is a lightweight bridge into deity conversation.

- it looks for the nearest player within sixteen blocks
- it starts deity conversation for that player
- it then terminates immediately

## Current Authoring Caveat

The bundled resource pack also contains many ritual-like entries under `data/eidolonunchained/recipes/`.

That means the codebase currently has two ritual-adjacent surfaces to keep in mind:

- `rituals/` for the `RitualDataManager` loader
- `recipes/` for bundled Eidolon recipe content already shipping with the mod

Document both honestly. Do not collapse them into one fictional unified path.# Ritual System

Eidolon Unchained currently spans two ritual-related surfaces that should not be confused.

## 1. Recipe-Based Ritual Content

The bundled datapack content lives under `data/eidolonunchained/recipes/` and uses Eidolon's recipe system for ritual-style content.

This is the resource surface you will actually see in the shipped pack today.

## 2. AI Deity Ritual Registration

`RitualDataManager` separately listens to `data/*/rituals/` and registers `AIDeityRitual` instances for entries that declare `linked_deity`.

That code path is real, but the bundled data currently emphasizes `recipes/`, not `rituals/`.

## Runtime Behavior

`AIDeityRitual`:

- finds the nearest player within sixteen blocks
- starts deity conversation for that player
- terminates immediately after firing

## Documentation Hazard

Do not collapse `recipes/` and `rituals/` into one invented folder in docs.

- `recipes/` is the main bundled surface today
- `rituals/` is the dedicated AI deity ritual loader path in code

Both need to be acknowledged until the data layout is unified.