# Codex System

The codex system is driven by `CodexDataManager` for categories, chapters, and entry injection, and by `EidolonPageConverter` for page rendering.

## Main Data Paths

Current codex content is authored across:

- `data/*/codex/`
- `data/*/codex_entries/`

## Runtime Page Conversion

`EidolonPageConverter` uses a fixed switch statement to map JSON page types into Eidolon page objects.

The supported runtime types currently include:

- `text`
- `title`
- `entity`
- `crafting`
- `recipe`
- `crafting_recipe`
- `ritual`
- `ritual_recipe`
- `crucible`
- `list`
- `image`
- `item_showcase`
- `workbench`
- `smelting`
- `sign`
- `chant`
- `rune_desc`
- `rune_index`
- `sign_index`
- `index`
- `titled_index`

Because this dispatch is fixed in code, codex page rendering is data-driven but not yet fully extensible.

## Ownership Caveat

The codex and research systems are coupled.

`ResearchDataManager` converts codex chapters into research chapters, so codex does not fully own chapter topology by itself.# Codex System

The codex system is loaded through `CodexDataManager` and rendered through `EidolonPageConverter`.

## Content Surfaces

The current codex content is split across:

- `data/*/codex/` for categories and chapter-style structure
- `data/*/codex_entries/` for individual entry payloads

## Current Page Model

The runtime supports a wider page set than the old docs usually describe.

Supported page types include:

- `text`
- `title`
- `entity`
- `crafting`
- `crafting_recipe`
- `ritual`
- `ritual_recipe`
- `crucible`
- `list`
- `image`
- `item_showcase`
- `workbench`
- `smelting`
- `sign`
- `chant`
- `rune_desc`
- `rune_index`
- `sign_index`
- `index`
- `titled_index`

## Coupling With Research

Codex and research are not cleanly isolated.

`ResearchDataManager` converts codex chapters into research chapters, which means chapter ownership is shared in the current implementation.

That coupling is real and should be documented, not hidden.