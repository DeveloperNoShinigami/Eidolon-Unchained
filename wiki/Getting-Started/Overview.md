# Overview

Eidolon Unchained adds a data-driven deity layer on top of Eidolon Repraised. In the current build, the mod ships with twelve deities and a chant-centered interaction loop that can branch into AI conversation, deity-linked prayer spells, deity-linked rituals, patron progression, and codex or research unlocks.

## What Players Actually Use

The main runtime interaction surfaces are:

- Chanting: the primary casting path, driven by chant slots, sign input, and deity-linked chant JSON.
- Prayer spells: generated from chants that declare a `prayer_effect_type`, then registered into Eidolon's spell system.
- Rituals: loaded from datapacks and registered into Eidolon's ritual system when they link to a deity.
- Patron and reputation systems: layered on top of deity relationships and AI response rules.
- Codex, research, fates, and facts: used to gate knowledge, progression, and deity-specific content.

## What Is Data-Driven Today

The bundled data lives under `src/main/resources/data/eidolonunchained/` and currently includes these authoring surfaces:

- `deities/`
- `ai_deities/`
- `chants/`
- `codex/`
- `codex_entries/`
- `research/`
- `fates/`
- `facts/`
- `recipes/`
- `keybind_settings/`

These files are editable and reloadable, but not every surface is fully extensible. Some systems still depend on fixed switch statements, fixed registration steps, or cross-loader sequencing.

## Built-In Deities

The current bundled deity set is:

- `air_deity`
- `dark_deity`
- `earth_deity`
- `end_deity`
- `fire_deity`
- `light_deity`
- `myrkul`
- `nature_deity`
- `nether_deity`
- `overworld_deity`
- `twilight_deity`
- `water_deity`

## Recommended Reading Order

- Read [Installation](Installation.md) to set up the required mods and API keys.
- Read [Quick Start](Quick-Start.md) for the shortest verified path into chanting and deity interaction.
- Move on to the Systems, Datapacks, Commands, AI Providers, and TTS sections once the rest of this rewrite is in place.