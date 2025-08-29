# Language Key Standardization Plan

## Current Issues
The current `en_us.json` file has inconsistent language key patterns:
- Mix of `eidolon.` and `eidolonunchained.` prefixes
- Inconsistent category structures
- No clear system boundaries
- Duplicate entries with different prefixes

## New Standardized Pattern
`eidolonunchained.<system>.<category>.<name>.<type>`

### System Categories:
- **codex** - All codex-related content
- **research** - Research system content
- **chant** - Chant system content
- **deity** - Deity-related content
- **command** - Command messages
- **keybind** - Keybinding labels
- **task** - Research task descriptions
- **ui** - User interface elements

### Examples:
- `eidolonunchained.codex.chapter.divine_rituals.title`
- `eidolonunchained.codex.entry.shadow_communion.description`
- `eidolonunchained.research.chapter.simple_examples.title`
- `eidolonunchained.chant.shadow_communion.name`
- `eidolonunchained.chant.shadow_communion.description`
- `eidolonunchained.deity.nature_deity.name`
- `eidolonunchained.command.research.cleared`
- `eidolonunchained.keybind.chant_slot_1`
- `eidolonunchained.task.explore_biome.description`

## Migration Plan
1. Create new standardized `en_us.json`
2. Update all JSON datapack files to use new keys
3. Update Java code that references language keys
4. Keep backward compatibility where possible

## Benefits
- Clear system boundaries
- Predictable key structure
- Easy to find and update translations
- Consistent for mod developers
- Future-proof for expansion
