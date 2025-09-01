# Codex System Overview

**How the Eidolon Unchained codex integration actually works**

## System Architecture

Eidolon Unchained integrates with the existing Eidolon codex by:

1. **Loading entry definitions** from `data/modid/codex_entries/*.json`
2. **Converting JSON pages** using `EidolonPageConverter` to native Eidolon page types
3. **Injecting entries** into target chapters during codex initialization
4. **Handling translations** through Minecraft's I18n system with caching

## Entry Structure

### Basic Entry Format
```json
{
  "target_chapter": "chapter_name",
  "pages": [
    {"type": "title", "text": "translation.key"},
    {"type": "text", "text": "translation.key.details"},
    {"type": "recipe", "recipe": "minecraft:item_id"}
  ]
}
```

### How Targeting Works
- `target_chapter` must match an existing Eidolon chapter resource location
- Entries are added as new content to the target chapter
- Multiple entries can target the same chapter
- **You can only extend existing chapters, not create new ones**

### Target Chapter Format
Target chapters use resource location format: `namespace:chapter_name`
- Example: `eidolon:getting_started`, `eidolon:wooden_stand`
- If no namespace is provided, defaults to `eidolon:`

## Page Types

### Currently Implemented (20+ types)
The system supports extensive page types through `EidolonPageConverter`:

#### Basic Content Pages
- **`text`** - Plain text content with translation support
- **`title`** - Entry header with title and content
- **`list`** - Bullet-point lists with translation keys

#### Recipe Pages  
- **`crafting`** - Crafting table recipes
- **`recipe`** - Alias for crafting (auto-detects recipe type)
- **`crafting_recipe`** - Specific crafting recipe display
- **`smelting`** - Furnace/smelting recipes
- **`crucible`** - Eidolon crucible recipes
- **`workbench`** - Eidolon workbench recipes
- **`ritual`** - Eidolon ritual display
- **`ritual_recipe`** - Specific ritual recipe format

#### Interactive Pages
- **`entity`** - 3D entity display with scale support
- **`image`** - Custom image display
- **`item_showcase`** - Item display with descriptions

#### Eidolon-Specific Pages
- **`sign`** - Mystical sign information
- **`chant`** - Spell chant details  
- **`rune_desc`** - Rune description pages
- **`rune_index`** - Rune index listings
- **`sign_index`** - Sign index listings
- **`index`** - General index pages
- **`titled_index`** - Index pages with titles

### Page Conversion Process
```
JSON Definition → EidolonPageConverter.convertPage() → Native Eidolon Page → Codex Display
```

## Translation System

### Key Structure
The system uses Minecraft's standard translation format:
```json
{
  "modid.codex.entry.example.title": "Page Title",
  "modid.codex.entry.example": "Content for title page",
  "modid.codex.entry.example.section": "Additional content"
}
```

### Translation Processing
1. **Key Detection** - Checks if text contains dots and valid namespace
2. **Cache Lookup** - Uses `TranslationCache` for performance
3. **I18n Translation** - Falls back to Minecraft's I18n.get()
4. **Error Handling** - Graceful fallback for missing keys

### Title Page Behavior
Title pages automatically:
- Use base key for content text
- Append `.title` to base key for title text
- Require both translation keys to exist

## Working Examples

### Text Example (Actual Implementation)
```json
{
  "target_chapter": "getting_started",
  "pages": [
    {
      "type": "title",
      "text": "eidolonunchained.codex.entry.text_example"
    },
    {
      "type": "text",
      "text": "eidolonunchained.codex.entry.text_example.details"
    }
  ]
}
```

### Entity Example (With Scale)
```json
{
  "target_chapter": "getting_started",
  "pages": [
    {
      "type": "title",
      "text": "eidolonunchained.codex.entry.entity_example"
    },
    {
      "type": "entity",
      "entity": "minecraft:zombie",
      "scale": 0.8
    }
  ]
}
```

### Recipe Example
```json
{
  "target_chapter": "getting_started",
  "pages": [
    {
      "type": "title",
      "text": "eidolonunchained.codex.entry.recipe_example"
    },
    {
      "type": "recipe",
      "recipe": "minecraft:crafting_table"
    }
  ]
}
```

## File Organization

### Actual Directory Structure
```
src/main/resources/
├── data/eidolonunchained/
│   ├── codex_entries/          # Entry JSON files
│   │   ├── text_example.json
│   │   ├── recipe_example.json
│   │   └── entity_example.json
│   └── eidolon_research/       # Research definitions
└── assets/eidolonunchained/
    └── lang/                   # Translation files
        └── en_us.json
```

### Data Loading Process
1. **CodexDataManager** scans `codex_entries/` folders
2. **Validates JSON structure** and required fields
3. **Creates CodexEntry objects** with page data
4. **EidolonCodexIntegration** injects entries into chapters
5. **Translation system** resolves text during display

## Integration Points

### With Eidolon Core
- Direct integration with Eidolon's Chapter and Page system
- Uses existing category and chapter structure
- Leverages Eidolon's GUI rendering system

### With Recipe System
- Automatic recipe type detection
- Support for vanilla and modded recipes
- Graceful handling of missing recipes

### With Entity System
- 3D entity rendering with scale support
- Entity information display
- Compatible with vanilla and modded entities

## Limitations

### What You Cannot Do
- ❌ Create new categories or chapters
- ❌ Modify existing Eidolon content
- ❌ Change chapter ordering
- ❌ Override built-in pages

### What You Can Do
- ✅ Add entries to any existing chapter
- ✅ Use 20+ different page types
- ✅ Create multi-page entries
- ✅ Add translation support
- ✅ Display recipes, entities, and custom content

## Error Handling

### Common Issues
- **Invalid target_chapter**: Must be valid Eidolon chapter
- **Missing translations**: Falls back gracefully but logs warnings
- **Invalid recipe IDs**: Page displays empty or error state
- **Malformed JSON**: Entry fails to load, logged to console

### Debug Information
The system provides extensive logging:
- Entry loading success/failure
- Translation key resolution
- Page conversion errors
- Target chapter validation

## Performance Considerations

### Translation Caching
- `TranslationCache` reduces I18n lookups
- Cache invalidation on language changes
- Memory-efficient key storage

### Page Conversion
- Lazy page creation during codex display
- Efficient JSON parsing with Gson
- Minimal overhead for unused entries
7. **`crucible`** - Eidolon crucible recipes
8. **`workbench`** - Eidolon workbench recipes
9. **`smelting`** - Furnace recipes

### Page Conversion Process
```
JSON Page Definition → EidolonPageConverter → Native Eidolon Page → Codex Display
```

## Translation System

### Key Structure
```json
{
  "mod.codex.entry.example.title": "Page Title",
  "mod.codex.entry.example": "Content for title page",
  "mod.codex.entry.example.section": "Additional content"
}
```

### Title Page Behavior
- Uses base translation key for content
- Automatically appends `.title` for the title text
- Both keys must exist in language file

### Translation Processing
1. **Key Detection** - Checks if text contains dots and starts with mod namespace
2. **Cache Lookup** - Attempts cached translation first
3. **I18n Fallback** - Uses Minecraft's translation system
4. **Error Handling** - Graceful fallback for missing keys

## File Organization

### Recommended Structure
```
data/your_mod/
├── codex_entries/
│   ├── basic_entries/
│   │   ├── crystal_basics.json
│   │   └── soul_theory.json
│   └── advanced_entries/
│       ├── ritual_mastery.json
│       └── necromancy.json
└── assets/your_mod/lang/
    └── en_us.json
```

### Naming Conventions
- Use descriptive file names: `crystal_basics.json`
- Group related entries in folders
- Match translation keys to file structure

## Integration Points

### With Eidolon Research
- Research system can unlock specific entries
- Conditional page display based on research progress
- Integration with Eidolon's sign system

### With Recipe System
- Recipe pages automatically detect recipe types
- Supports vanilla and modded recipes
- Handles missing recipes gracefully

### With Entity System  
- Entity pages display 3D models
- Shows entity stats and information
- Works with vanilla and modded entities

## Limitations

### What You Cannot Do
- ❌ Create new categories
- ❌ Create new chapters
- ❌ Modify existing Eidolon content
- ❌ Change chapter order or structure
- ❌ Override existing pages

### What You Can Do
- ✅ Add new entries to existing chapters
- ✅ Use all 9 page types
- ✅ Add multi-language support
- ✅ Create complex page sequences
- ✅ Reference any valid recipe/entity

## Best Practices

### Content Design
1. **Start with title page** - Introduce the entry topic
2. **Build progressively** - Simple concepts first
3. **Use varied page types** - Keep content engaging
4. **End with practical info** - Recipes, entities, etc.

### Technical Implementation
1. **Validate JSON** - Use online validators
2. **Test frequently** - Reload with `/reload`
3. **Check logs** - Monitor for errors
4. **Organize files** - Use logical folder structure

## Troubleshooting

### Entry Not Appearing
- Check `target_chapter` spelling
- Verify JSON syntax
- Ensure datapack is loaded (`/datapack list`)
- Check game logs for errors

### Translation Issues
- Verify all keys exist in language file
- Use `%%` for literal percent signs
- Check key spelling matches exactly
- Ensure proper namespace format

### Recipe/Entity Pages Empty
- Verify the ID exists and is spelled correctly
- Check that required mods are loaded
- Ensure the item/entity can be accessed in-game

## Next Steps

- **[Page Types Reference](page-types.md)** - Detailed page type guide
- **[Translation Guide](translation-guide.md)** - Advanced translation techniques
- **[Examples](examples.md)** - Working code samples
