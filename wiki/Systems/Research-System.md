# Research System

The research system is loaded by `ResearchDataManager` from `data/*/research/`, and it also supports dedicated `research_chapters/` resources when they exist.

## What The Loader Does

At reload time, the manager:

- registers built-in research task types
- loads custom research chapters
- converts codex chapters into research chapters
- loads research entries from the `research` path
- triggers client-side Eidolon research injection after resource loading completes

## Conditions And Tasks

Research tasks are broader than the current condition layer.

The implemented condition classes currently cover:

- dimension
- inventory
- time
- weather

## Important Coupling

Research is not isolated from the codex system.

- codex chapters can become research chapters
- documentation must treat this as a real runtime coupling, not as two totally independent systems

## Practical Guidance

Use the research system for gated progression and milestone tracking, but document its current condition surface conservatively. The code supports more task shapes than it does dedicated reusable condition classes.# Research System

The research system is loaded by `ResearchDataManager` from `data/*/research/`, with legacy chapter loading still scanning `data/*/research_chapters/` when present.

## Current Scope

The system supports:

- custom research chapters
- research entries and tasks
- a set of built-in task types
- a small set of explicit condition classes
- client-side injection into Eidolon's research integration

## Task Types

The current built-in research task families include combat, crafting, collection, ritual use, inventory, dimension, time, weather, biome, and NBT-oriented checks.

## Conditions

The condition surface is narrower than some archived docs suggest.

The currently implemented condition classes are centered on:

- dimension
- inventory
- time
- weather

## Codex Coupling

Research chapters can come from dedicated research JSON, but the system also converts codex chapters into research chapters. That means research authors need to understand both surfaces when debugging missing chapters or unlock structure.