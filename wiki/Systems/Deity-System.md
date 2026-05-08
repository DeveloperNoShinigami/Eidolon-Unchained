# Deity System

The deity system is loaded from `data/*/deities/*.json` through `DatapackDeityManager` and registered into Eidolon's deity registry at reload time.

## What A Deity Defines

Each deity JSON provides the base identity and progression structure for one deity:

- `id`
- `name`
- `description`
- `colors`
- `progression`
- `unlock_rewards`
- `prayer_types`
- `abandon`

The bundled project currently ships twelve built-in deities.

## Progression Model

Progression is JSON-driven, not hardcoded to a fixed five-stage ladder.

- Each stage declares its own `id`, `reputation`, `major`, and optional `title`.
- Stage rewards can be authored as direct command strings or as structured reward objects.
- The deity stores stage titles and unlock rewards when the datapack is loaded.

## Registration Flow

At reload time, the manager:

- clears the current deity map
- loads every deity JSON from the `deities` path
- creates a `DatapackDeity`
- registers it with Eidolon's deity registry immediately
- posts a deity-loaded event so linked systems can attach AI configs and related content

## Important Runtime Notes

- Stage thresholds are per-deity data, not a global constant.
- Abandon behavior is data-driven through the deity's `abandon` configuration.
- AI behavior does not live in deity JSON directly unless an `ai_configuration` block is embedded; the main authoring path is the separate `ai_deities` loader.# Deity System

The deity system is loaded from `data/*/deities/` through `DatapackDeityManager` and registered directly into Eidolon's deity registry.

## What A Deity Defines

Each deity JSON defines the identity and progression layer that other systems build on:

- base `id`, `name`, and `description`
- UI `colors`
- `progression.max_reputation`
- `progression.stages[]`
- optional `prayer_types`
- optional `abandon` behavior

## Progression Model

Reputation stages are data-driven, not hardcoded to one universal ladder.

- stage IDs are arbitrary resource-style identifiers
- each stage sets a reputation threshold
- stage titles come from the JSON itself
- stage rewards can be raw commands or typed reward objects

This means the mod does not have a fixed global five-stage rule. Different deity definitions can ship different stage layouts.

## Bundled Deities

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

## What Depends On Deities

The rest of the runtime links back to deity IDs:

- AI deity configs
- deity-linked chants
- deity-linked prayer spells
- deity-linked rituals
- patron rules
- progression and reputation updates

For the data shape, see the datapack reference pages in the `Datapacks` section.