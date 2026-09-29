# Eidolon Unchained 2.0

A KubeJS-centric extension layer for **Eidolon: Repraised** on Minecraft 1.20.1 / Forge. It wraps Eidolon's real APIs
for KubeJS scripts and adds the systems Eidolon does not have: scriptable deities, deity-bound chants, a unified caster
model for players, mobs and bosses, deity damage with resistance and penetration, custom effigies and AI-driven deities.

This branch (`2.0-kubejs`) is a clean rewrite. The historical datapack version lives on `main`; the last Java version
of 1.x is on `1.20.1_v3.9.0.9_Conversion`.

## Status

Phase 1 of the implementation order ("clean base"): the mod loads, pins its dependency baseline, registers its KubeJS
plugin, fires one diagnostic event per script type and validates the Eidolon API surfaces it will wrap. Nothing
gameplay-facing exists yet.

## Requirements

| Mod | Version | Note |
|---|---|---|
| Minecraft / Forge | 1.20.1 / 47.4.10+ | Java 17 |
| Eidolon: Repraised | 0.3.13+ | mod ID `eidolon` |
| KubeJS | 2001.6.5+ | plus Rhino 2001.2.2+ and Architectury 9.1.12+ |
| Curios | 5.14.1+ | |
| GeckoLib | 4.8.4+ | deity avatars and effigies |

## Building

```bash
./gradlew build
```

The jar lands in `build/libs/`. `./gradlew runClient` starts a development client with every dependency.

## Script smoke test

Copy `examples/kubejs/` over your instance's `kubejs/` folder. On startup, server start and world join the log shows
`[EU smoke] startup ok`, `[EU smoke] server ok` and `[EU smoke] client ok`, plus the Eidolon API validation report.

```js
// startup_scripts
EidolonUnchainedEvents.init(event => {
    console.info(`EU ${EidolonUnchained.version()} on Eidolon ${EidolonUnchained.eidolonVersion()}`)
})
```

## Design

Requirements, decisions and the implementation order are kept outside this repository, in the project's ICM
(`projects/eidolon-unchained/`). The one rule that applies everywhere: EU never fakes an Eidolon API. It either calls an
existing Eidolon API, uses an existing KubeJS mechanism, or adds an EU abstraction that is labelled as such.

## License

All rights reserved. See `LICENSE`.
