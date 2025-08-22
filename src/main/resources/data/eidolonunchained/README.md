# Eidolon Unchained Datapack Examples

This directory contains comprehensive examples demonstrating the complete datapack system for Eidolon Unchained.

## Structure Overview

### Category System
- **Location**: `data/eidolonunchained/codex/examples/_category.json`
- **Purpose**: Defines a new category in the Eidolon codex
- **Features**: Custom icon, color, name, and description

### Chapter System
- **Location**: `data/eidolonunchained/codex/examples/getting_started.json`
- **Purpose**: Defines a chapter within the examples category
- **Features**: Title page, multiple text pages with translation support

### Codex Entry System
Located in `data/eidolonunchained/codex_entries/`:

1. **text_example.json** - Demonstrates basic text pages
2. **recipe_example.json** - Shows recipe page integration
3. **entity_example.json** - Displays 3D entity rendering

All entries target the "getting_started" chapter and include multiple page types.

### Research System
Located in `data/eidolonunchained/research_*`:

1. **research_chapters/basic_combat.json** - Research chapter containing entries
2. **research_entries/zombie_hunter.json** - Simple research with kill task
3. **research_entries/undead_researcher.json** - Research with dimension condition + kill task

### Translation System
- **Location**: `assets/eidolonunchained/lang/en_us.json`
- **Purpose**: Provides proper localization for all example content
- **Coverage**: Category names, chapter titles, entry content, research descriptions

## Key Features Demonstrated

1. **Category Creation**: Complete custom category with metadata
2. **Chapter Management**: Organized content within categories
3. **Multiple Page Types**: Text, recipe, entity rendering
4. **Research Integration**: Tasks, conditions, and rewards
5. **Translation Support**: Full localization compatibility
6. **Resource Scanning**: Proper datapack file organization

## File Naming Conventions

- Categories: `_category.json` in category folders
- Chapters: `{chapter_name}.json` in category folders
- Entries: `{entry_name}.json` in `codex_entries/`
- Research: `{research_name}.json` in `research_chapters/` or `research_entries/`
- Translations: `en_us.json` in `assets/{namespace}/lang/`

## Usage Instructions

1. Copy the example structure as a template
2. Modify content to match your specific needs
3. Update translation keys in the language file
4. Test in-game to verify proper loading

This example structure provides a complete foundation for creating custom Eidolon Unchained content through datapacks.
