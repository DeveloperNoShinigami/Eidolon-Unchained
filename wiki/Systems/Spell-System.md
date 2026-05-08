# Spell System

The current build does not expose a fully separate spell-authoring surface apart from chanting.

## What "Spell System" Means Here

There are two runtime spell paths that matter for documentation:

- native Eidolon spell resolution during chanting
- chant-derived deity prayer spell registration through `AIDeitySpells`

## Native Resolution

`PlayerChantingSystem` checks Eidolon's native spell registry before it falls back to datapack-defined chants. This is why some chant inputs can resolve through Eidolon's own spell layer without using a custom chant definition.

## Deity Prayer Spells

`AIDeitySpells` registers `AIDeityPrayerSpell` instances after AI configs are linked.

- only chants that declare `prayer_effect_type` are used
- the spell ID is generated from the deity ID and chant ID
- the resulting spell is registered with Eidolon's spell system

## Effigy Requirement

`AIDeityPrayerSpell` uses an effigy readiness check before it starts deity conversation.

This is the important distinction from ordinary chant fallback: the deity prayer spell path is explicitly tied to ready-effigy behavior.

## Documentation Rule

Use this page to explain spell resolution and chant-derived prayer spell registration. Do not treat spells as a separate standalone content system with their own independent datapack format, because that is not how the current code works.# Spell System

There is no separate standalone spell authoring surface in Eidolon Unchained.

## The Important Boundary

In the current runtime, chants are what feed the spell layer.

- `DatapackChantManager` loads chant JSON
- each chant becomes a `DatapackChantSpell`
- that spell is registered into Eidolon's spell registry

So the practical spell system is the chanting system plus spell registration, not a second parallel content format.

## AI Prayer Spells

There is one additional spell-specific layer: `AIDeitySpells`.

After AI configs are linked, it scans deity-linked chants and registers deity prayer spells for chants that declare `prayer_effect_type`.

That means:

- not every chant becomes an AI prayer spell
- AI prayer spell generation is derived from chant data
- the spell surface still starts from chant JSON

## Documentation Consequence

When authoring content, treat chant JSON as the canonical spell authoring path for this mod.