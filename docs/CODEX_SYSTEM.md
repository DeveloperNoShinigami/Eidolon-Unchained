# Eidolon Unchained Codex System Documentation

## Overview

The Eidolon Unchained codex system provides an enhanced framework for creating interactive magical documentation within the Eidolon world. This system builds upon the base Eidolon codex functionality with improved translation handling, consistent structure, and robust error management.

## Architecture

### Core Components

1. **EidolonPageConverter**: Handles conversion of JSON page definitions to Eidolon Page objects
2. **Translation System**: Enhanced caching and fallback mechanisms
3. **TitlePage Integration**: Automatic title generation and content display
4. **JSON Data Structure**: Standardized format for codex entries

## File Structure

```
src/main/resources/
├── data/eidolonunchained/codex_entries/
│   ├── advanced_monsters.json
│   ├── crystal_rituals.json
│   ├── rare_monsters.json
│   ├── advanced_summoning.json
│   └── void_mastery.json
├── assets/eidolonunchained/lang/
│   └── en_us.json
└── java/com/eidolonunchained/
    └── EidolonPageConverter.java
```

## JSON Entry Format

### Basic Structure
```json
{
  "target_chapter": "CHAPTER_NAME",
  "pages": [
    {
      "type": "title",
      "text": "eidolonunchained.codex.entry.entry_name"
    },
    {
      "type": "text", 
      "text": "eidolonunchained.codex.entry.entry_name.section"
    }
  ]
}
```

### Page Types

#### Title Page
```json
{
  "type": "title",
  "text": "eidolonunchained.codex.entry.crystal_rituals"
}
```
- Displays both title and main content on the same page
- Title is automatically derived by appending `.title` to the base key
- Content comes from the base key itself

#### Text Page
```json
{
  "type": "text",
  "text": "eidolonunchained.codex.entry.crystal_rituals.placement"
}
```
- Displays plain text content
- Supports full translation system

#### Entity Page
```json
{
  "type": "entity",
  "entity": "eidolon:necromancer"
}
```
- Shows an entity with information
- Includes entity model and basic stats

#### Crafting Page
```json
{
  "type": "crafting",
  "recipe": "eidolon:arcane_gold_ingot"
}
```
- Displays a crafting recipe
- Shows ingredients and result

## Language File Structure

### Naming Convention
```
eidolonunchained.codex.entry.[entry_name].[section]
```

### Required Keys for Title Pages
For each entry, you need both:
```json
{
  "eidolonunchained.codex.entry.crystal_rituals.title": "Crystal Ritual Mastery",
  "eidolonunchained.codex.entry.crystal_rituals": "Advanced techniques for crystal-based magical workings..."
}
```

### Example Language Entry
```json
{
  "eidolonunchained.codex.entry.crystal_rituals.title": "Crystal Ritual Mastery",
  "eidolonunchained.codex.entry.crystal_rituals": "Advanced techniques for crystal-based magical workings. This improved approach to crystal rituals provides greater magical output through proper harmonic alignment.",
  "eidolonunchained.codex.entry.crystal_rituals.placement": "Proper crystal placement can increase ritual effectiveness by up to 300%.",
  "eidolonunchained.codex.entry.crystal_rituals.resonance": "Crystal Resonance Theory: Different crystals resonate at different magical frequencies."
}
```

## TitlePage System

### How It Works
1. **Input**: JSON passes base key (e.g., `"eidolonunchained.codex.entry.crystal_rituals"`)
2. **Title Generation**: TitlePage automatically appends `.title` to create title key
3. **Content Display**: Uses base key for main page content
4. **Result**: Single page with both title and content

### Bytecode Analysis Discovery
Through bytecode analysis, we discovered that TitlePage uses `invokedynamic` string concatenation to automatically append `.title` to the input string:
```
13: invokedynamic #45, 0 // InvokeDynamic #1:makeConcatWithConstants:(Ljava/lang/String;)Ljava/lang/String;
14: invokestatic  #46 // Method net/minecraft/network/chat/Component.translatable
```

This means the JSON should pass base keys WITHOUT the `.title` suffix.

## Translation System Features

### Caching Mechanism
```java
private static final Map<String, String> translationCache = new ConcurrentHashMap<>();
```
- Reduces repeated translation lookups
- Improves performance for large codex entries
- Thread-safe implementation

### Fallback System
1. **Primary**: Component.translatable() lookup
2. **Secondary**: Direct language file cache
3. **Tertiary**: Return key as-is with debug logging

### Error Handling
- Graceful degradation for missing keys
- Debug logging for troubleshooting
- Automatic recovery for malformed entries

## Chapter Targets

### Available Chapters
- `MONSTERS`: Creature studies and monster lore
- `CRYSTAL_RITUAL`: Crystal-based magical practices
- `SUMMON_RITUAL`: Summoning and binding techniques
- `VOID_AMULET`: Void manipulation and amulet crafting

## Best Practices

### JSON Structure
1. Always start with a title page
2. Use consistent key naming
3. Group related content logically
4. Include visual elements (entities, recipes) between text blocks

### Language Keys
1. Use descriptive section names
2. Keep content concise but informative
3. Maintain consistent tone and style
4. Always provide both base and `.title` versions

### Content Organization
1. **Title Page**: Introduction and overview
2. **Detail Pages**: Specific information and mechanics
3. **Visual Pages**: Entities, recipes, illustrations
4. **Summary Pages**: Key points and conclusions

## Troubleshooting

### Common Issues

#### Title Not Displaying
- **Cause**: Missing `.title` key in language file
- **Solution**: Add `"entry.name.title": "Title Text"` to language file

#### Content Showing Translation Keys
- **Cause**: Missing base key in language file
- **Solution**: Add `"entry.name": "Content Text"` to language file

#### Recipe Showing "Air"
- **Cause**: Invalid recipe ID or missing recipe
- **Solution**: Verify recipe exists and use correct namespaced ID

#### Page Not Loading
- **Cause**: Malformed JSON or missing translation
- **Solution**: Check JSON syntax and ensure all referenced keys exist

### Debug Logging
Enable debug logging to see translation system activity:
```
[DEBUG] Translation cache miss for key: eidolonunchained.codex.entry.crystal_rituals
[DEBUG] Direct translation lookup for key: eidolonunchained.codex.entry.crystal_rituals
[DEBUG] Translation found: Advanced techniques for crystal-based magical workings...
```

## Version History

### v1.0.0 - Translation System Overhaul
- Fixed TitlePage integration through bytecode analysis
- Enhanced translation caching system
- Standardized JSON entry format
- Removed duplicate language entries
- Improved error handling and fallback mechanisms
- Added comprehensive documentation

## Performance Considerations

### Translation Caching
- Cache hit rate improves with usage
- Memory usage scales with unique translation keys
- Cache is cleared on resource reload

### Rendering Optimization
- Title pages combine title and content in single render
- Entity pages cache model data
- Recipe pages use Minecraft's built-in recipe rendering

## Future Enhancements

### Planned Features
- Dynamic content generation
- Conditional page display
- Interactive elements
- Enhanced visual components
- Multi-language support improvements
