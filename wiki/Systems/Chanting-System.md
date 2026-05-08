# Chanting System

Chanting is the primary player-facing casting system in the current build.

## Default Controls

The currently registered default keys are:

- `G` for chant slot 1
- `H` for chant slot 2
- `J` for chant slot 3
- `K` for chant slot 4
- `C` to open the chant interface

## Runtime Flow

The active chanting path is player-centered.

- `ActiveChantingSystem` forwards sign input into `PlayerChantingSystem`.
- The system appends each sign to the active chant sequence.
- Sign effects can fire through `KeybindSignEffectsManager`.
- When the sequence is ready, the system checks for a native Eidolon spell match first.
- If no native match is found, it falls back to a datapack chant match.
- Execution is delayed briefly before the resolved chant or spell completes.

## Datapack Chants

Datapack chants are loaded from `data/*/chants/` through `DatapackChantManager`.

- The manager normalizes chant IDs to the filename.
- Category can be inferred from the subfolder when it is not explicitly declared.
- Chants are registered into Eidolon's spell system as `DatapackChantSpell` instances.

## Deity-Linked Chant Behavior

Chants can remain standalone or link into deity systems.

- `linked_deity` ties the chant to a deity
- `prayer_effect_type` lets the chant resolve into a prayer flow
- `requires_effigy` can force an effigy-ready requirement
- deity-linked combat effects can inherit that deity's `deity_damage_type` when the effect does not declare its own `damage_type`
- non-dummy deity-colored damage values are rendered by Eidolon Unchained directly; dummy-target number colors still depend on Dummmmmmy client config

## Keybind Sign Effects

The keybind effect layer is loaded from `data/*/keybind_settings/sign_effects.json`.

- `per_sign` maps a sign ID to command strings
- `sequences` maps a chant ID to indexed sequence commands

This surface is centralized in one file today. It is data-driven, but it is not yet a general-purpose pluggable effect registry.# Chanting System

Chanting is the primary casting path in the current build.

The core flow runs through `ActiveChantingSystem` and `PlayerChantingSystem`, with chant definitions loaded from `data/*/chants/` by `DatapackChantManager`.

## Default Keys

The currently registered default keybinds are:

- `G` for chant slot 1
- `H` for chant slot 2
- `J` for chant slot 3
- `K` for chant slot 4
- `C` to open the chant interface

## Runtime Flow

The current player-centered chanting flow is:

- the player inputs signs into the active chant state
- the system checks whether the partial sequence is still valid
- after completion, the system waits briefly before resolving the result
- native Eidolon spells are checked first
- if no native spell matches, the datapack chant fallback is used

## Datapack Chant Features

Bundled and custom chants can define:

- sign sequences
- deity links
- codex visibility
- mana cost and cooldown
- reputation or item requirements
- effigy requirements
- typed effects such as `start_conversation`, `send_message`, `give_item`, `apply_effect`, and sound playback

## Keybind Effects

Keybind-side effects are managed separately from chant definitions through `KeybindSignEffectsManager`.

- per-sign commands can fire when a sign is input
- per-sequence commands can fire when a known chant is being entered

## Mob Chant Casting

Mobs can be configured to cast chants automatically in combat. This is a separate system from player chanting and does not require the player keybind flow.

**Configuration:** `data/<namespace>/chantable_mobs/<file>.json` — loaded by `ChantableMobManager`. See [Chantable Mobs JSON](../Datapacks/Chantable-Mobs-JSON.md).

**Runtime:** `MobChantCastingGoal` is an AI goal injected into each configured mob when it enters the world. It handles the full lifecycle:

- chant rotation (round-robin through `chant_rotation_ids`)
- per-chant cooldowns and inter-cast interval
- mana tracking and cost enforcement
- range and line-of-sight gating
- visual sync to clients via `MobChantBuildStatePacket`
- faith title and deity assignment on join
- config refresh on join (live config always wins over stale NBT)

Mobs cycle through all chants in order, skipping any chant that is on cooldown or whose mana cost cannot be met. Normal combat AI remains active between casts. Config changes take effect on existing mobs the next time they enter the world.

That keybind settings layer is currently command-driven, not handler-pluggable.