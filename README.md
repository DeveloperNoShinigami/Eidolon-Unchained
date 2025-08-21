# Eidolon Unchained

An addon mod for **Eidolon: Repraised** that expands the mystical world with new chapters, rituals, and other Eidolon-focused content.

## Features

- New chapters and storylines
- Additional rituals and magical practices
- Enhanced mystical content
- Integration with Curios API for mystical accessories
- Advanced codex system with proper translation support
- Allows developers and modders to datapack Eidolon codex features and implement spells

### Enhanced Codex System
- **Advanced Monster Studies**: Comprehensive guide to magical creatures and their behaviors
- **Rare Monster Variants**: Documentation of unusual monster behaviors and variants
- **Advanced Summoning Techniques**: For experienced practitioners of spectral manipulation
- **Crystal Ritual Mastery**: Advanced techniques for crystal-based magical workings
- **Advanced Void Techniques**: Expanding upon void amulets and void manipulation

### Translation System Improvements
- Robust translation caching for better performance
- Fallback translation system for missing keys
- Consistent language file structure
- Proper TitlePage integration with automatic title generation

## Documentation

Comprehensive guides live in the [docs/](docs/) directory:

- [Documentation Index](docs/README.md)
- [Complete System Summary](docs/COMPLETE_SYSTEM_SUMMARY.md)
- [Datapack Overview](docs/datapack_overview.md)
- [Datapack Structure](docs/DATAPACK_STRUCTURE.md)
- [Research Conditions](docs/RESEARCH_CONDITIONS.md)
- [Research Entries](docs/research_entries.md)
- [Codex Reference](docs/codex_reference.md)
- [Codex Tutorial](docs/codex_tutorial.md)
- [Best Practices](docs/best_practices.md)
- [UI Customization](docs/ui_customization.md)
- [Example Complete Codex Entry](docs/EXAMPLE_COMPLETE_CODEX_ENTRY.json)

## Dependencies

This mod requires the following mods to function:

### Required Dependencies
- **Minecraft**: 1.20.1
- **Minecraft Forge**: 47.4.0+
- **Eidolon: Repraised**: 0.3.8+
- **Curios API**: 5.14.1+

## Installation

1. Install Minecraft Forge 47.4.0 or higher for Minecraft 1.20.1
2. Download and install Eidolon: Repraised
3. Download and install Curios API
4. Place the Eidolon Unchained mod file in your `mods` folder
5. Launch the game

## Codex Development Guide

For a step-by-step tutorial see [Codex Tutorial](docs/codex_tutorial.md). For a comprehensive reference see [Codex Reference](docs/codex_reference.md).

### Creating New Codex Entries

#### 1. JSON Entry Structure
Create JSON files in `src/main/resources/data/eidolonunchained/codex_entries/`:
These files add pages to the existing codex categories.
To introduce a brand new category, make a folder under
`src/main/resources/data/eidolonunchained/codex/` with a `_category.json`
and place the category's entry files inside it.

```json
{
  "target_chapter": "CHAPTER_NAME",
  "pages": [
    {
      "type": "title",
      "text": "eidolonunchained.codex.entry.your_entry"
    },
    {
      "type": "text",
      "text": "eidolonunchained.codex.entry.your_entry.details"
    },
    {
      "type": "entity",
      "entity": "minecraft:zombie"
    },
    {
      "type": "crafting",
      "recipe": "eidolon:arcane_gold_ingot"
    }
  ]
}
```

#### 2. Language File Structure
Add translations to `src/main/resources/assets/eidolonunchained/lang/en_us.json`:

```json
{
  "eidolonunchained.codex.entry.your_entry.title": "Your Entry Title",
  "eidolonunchained.codex.entry.your_entry": "Main content that appears on the title page alongside the title.",
  "eidolonunchained.codex.entry.your_entry.details": "Additional detailed information for subsequent pages."
}
```

#### 3. TitlePage System
The TitlePage system automatically handles title generation:
- **Input**: Base key (e.g., `"eidolonunchained.codex.entry.crystal_rituals"`)
- **Title**: Automatically appends `.title` to get the title text
- **Content**: Uses the base key for the main page content

This means each title page displays both the title and introductory content together.

#### 4. Page Types
Available page types:
- **`title`**: Creates a page with both title and content
- **`text`**: Plain text content
- **`entity`**: Displays an entity with information
- **`crafting`**: Shows a crafting recipe
- **`ritual`**: Displays ritual information

#### 5. Translation Best Practices
- Use consistent naming: `eidolonunchained.codex.entry.[entry_name].[section]`
- Always provide both base keys and `.title` versions
- Keep content concise but descriptive
- Use proper punctuation and formatting

## Translation System Features

### Caching System
The enhanced translation system includes:
- **Memory caching** for frequently accessed translations
- **Direct language file access** for fallback translations
- **Performance optimization** for large codex entries

### Error Handling
- Graceful fallback to English translations
- Debug logging for missing translation keys
- Automatic error recovery for malformed entries

## Translation Troubleshooting

### Common Format Errors

If you see a "Format error" in a codex page, it is often caused by a percent sign (`%`) in your translation string. Minecraft's translation system treats `%` as the start of a format specifier (like `%s` or `%1$s`). If your translation string contains a `%` but the code does not provide arguments, you will get a format error.

### How to Fix

- **Escape percent signs:** Use `%%` instead of `%` in your translation strings to display a literal percent sign.
- **Remove unused format specifiers:** If you do not intend to use arguments, make sure your translation string does not contain `%s`, `%1$s`, etc.

#### Example

**Incorrect:**

```json
"eidolonunchained.codex.entry.crystal_rituals.placement": "Proper crystal placement can increase ritual effectiveness by up to 300%."
```

**Correct:**

```json
"eidolonunchained.codex.entry.crystal_rituals.placement": "Proper crystal placement can increase ritual effectiveness by up to 300%%."
```

This will prevent Minecraft from showing a format error and display the percent sign as intended.

## Development

This mod is built using:
- Minecraft Forge 47.4.0
- ForgeGradle
- Java 17

### Building from Source

```bash
./gradlew build
```

### Dependencies Setup

The mod integrates with:
- Eidolon: Repraised API for mystical content
- Curios API for wearable mystical items

### Recent Improvements

#### Version 1.0.0 Updates
- **Fixed TitlePage Integration**: Resolved title display issues through bytecode analysis
- **Enhanced Translation System**: Added caching and fallback mechanisms
- **Consistent JSON Structure**: Standardized all codex entry formats
- **Language File Optimization**: Removed duplicate entries and improved organization
- **Recipe Page Support**: Fixed "air" display issues in crafting pages

## Future Features

- Additional codex chapters and research content
- Expanded ritual mechanics
- Improved integration with other magic mods

## Authors

**Blue Lotus Coding**

## License

All Rights Reserved

## Version

Current Version: 1.0.0

