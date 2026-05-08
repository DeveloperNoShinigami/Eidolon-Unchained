# Eidolon Unchained Wiki

This wiki is the implementation-first reference for the current 1.20.1 Forge build of Eidolon Unchained. It is written against the live code and bundled datapack resources, not the archived README-era behavior.

Eidolon Unchained extends Eidolon Repraised with AI-linked deities, chant-driven interactions, deity-linked prayer spells, deity-linked rituals, reputation and patron systems, codex content, research, fates, facts, and optional TTS playback.

## Current Runtime Scope

- Minecraft `1.20.1`
- Forge `47.4.0`
- Eidolon Repraised `0.3.12+`
- Curios `5.14.1+1.20.1`
- Simple Voice Chat API `2.6.0` as an optional integration

## Core Interaction Model

The current runtime is chant-first.

- Players use chant slots and sign input as the primary casting path.
- Deity-linked chants can resolve into AI prayer flows when they declare `linked_deity` and `prayer_effect_type`.
- Deity-linked prayer spells are registered from chants after AI configs are linked.
- Deity-linked rituals are loaded from datapacks and registered into Eidolon's ritual system.

## Start Here

- [Getting Started Overview](Getting-Started/Overview.md)
- [Installation](Getting-Started/Installation.md)
- [Quick Start](Getting-Started/Quick-Start.md)

## Core References

- [Deity System](Systems/Deity-System.md)
- [AI Deity System](Systems/AI-Deity-System.md)
- [Chanting System](Systems/Chanting-System.md)
- [Spell System](Systems/Spell-System.md)
- [Prayer System](Systems/Prayer-System.md)
- [Ritual System](Systems/Ritual-System.md)
- [Patron System](Systems/Patron-System.md)
- [Reputation System](Systems/Reputation-System.md)
- [Codex System](Systems/Codex-System.md)
- [Research System](Systems/Research-System.md)
- [Fate System](Systems/Fate-System.md)
- [TTS System](Systems/TTS-System.md)

## Data And Commands

- [Datapack Overview](Datapacks/Overview.md)
- [Command Overview](Commands/Commands.md)
- [Eidolon Base Commands](Commands/Eidolon-Base-Commands.md)
- [Gemini Provider](AI-Providers/Gemini.md)
- [OpenRouter Provider](AI-Providers/OpenRouter.md)
- [Player2AI Provider](AI-Providers/Player2AI.md)
- [TTS Quick Setup](TTS/Quick-Setup.md)
- [Examples](Examples/Examples.md)

## Documentation Direction

This rewrite uses canonical field names and current loader paths.

- Legacy compatibility aliases that still parse in code are treated as compatibility-only, not as the recommended authoring format.
- Archived wiki material remains preserved under `wiki/Archive/` but is not the source of truth for new pages.