# Codex Entries

Codex entries are the primary way to **add content to existing chapters**. They don't create new chapters themselves - instead, they extend existing chapters with additional pages and information.

## 📖 What Are Codex Entries?

Codex entries are JSON files that inject pages into existing chapters. Think of them as "expansion packs" for chapters that allow multiple datapacks to contribute content to the same topic.

### How It Works

1. **Chapters** define the basic structure and foundation
2. **Entries** add additional pages to those chapters
3. **Multiple entries** can target the same chapter
4. **All content appears together** in the final chapter

## 📁 File Location

```
data/yourmod/codex_entries/
├── monster_lore.json
├── advanced_rituals.json
└── spell_variations.json
```

## 📋 JSON Structure

```json
{
  "target_chapter": "chapter_name",
  "pages": [
    {
      "type": "title",
      "text": "yourmod.codex.entry.title"
    },
    {
      "type": "text", 
      "text": "yourmod.codex.entry.description"
    }
  ]
}
```

### Required Fields

- **`target_chapter`** - The chapter this entry will add content to
- **`pages`** - Array of pages to add to the chapter

## 🎯 Target Chapter Formats

The `target_chapter` field supports multiple formats:

### 1. Simple Name
```json
"target_chapter": "monsters"
```
Targets `yourmod:monsters` chapter in your namespace.

### 2. Full Resource Location
```json
"target_chapter": "eidolon:void_amulet"
```
Targets a specific chapter in another mod's namespace.

### 3. Cross-Mod Compatibility
```json
"target_chapter": "eidolonunchained:getting_started"
```
Targets chapters from other Eidolon Unchained datapacks.

## 📄 Page Types

See [Page Types](page-types.md) for complete documentation, but here are the most common:

### Text Pages
```json
{
  "type": "text",
  "text": "yourmod.codex.entry.description"
}
```

### Recipe Pages
```json
{
  "type": "recipe",
  "recipe": "minecraft:crafting_table"
}
```

### Entity Pages
```json
{
  "type": "entity",
  "entity": "minecraft:zombie",
  "scale": 0.8
}
```

## 💡 Best Practices

### 1. Always Start with a Title Page
```json
{
  "pages": [
    {
      "type": "title",
      "text": "yourmod.codex.entry.your_entry.title"
    },
    // ... other pages
  ]
}
```

### 2. Use Translation Keys
```json
{
  "type": "text",
  "text": "yourmod.codex.entry.description"
}
```

Never hardcode text - always use translation keys for proper localization.

### 3. Logical Page Flow
Structure your pages logically:
1. Title page
2. Introduction/overview
3. Detailed information
4. Examples or recipes
5. Related information

### 4. Consistent Naming
Use descriptive, consistent file names:
- `monster_advanced_studies.json` ✅
- `stuff.json` ❌

## 🔗 Integration Examples

### Adding to Base Eidolon Chapters
```json
{
  "target_chapter": "eidolon:monsters",
  "pages": [
    {
      "type": "title",
      "text": "yourmod.codex.monsters.custom_creatures"
    }
  ]
}
```

### Adding to Custom Chapters
```json
{
  "target_chapter": "getting_started",
  "pages": [
    {
      "type": "title", 
      "text": "yourmod.codex.getting_started.advanced_tips"
    }
  ]
}
```

### Cross-Datapack Content
```json
{
  "target_chapter": "otherdatapack:spell_theory",
  "pages": [
    {
      "type": "text",
      "text": "yourmod.codex.spell_theory.additional_notes"
    }
  ]
}
```

## ⚠️ Common Issues

### Chapter Not Found
If the target chapter doesn't exist, the entry will be ignored. Make sure:
- The chapter exists in the target namespace
- The namespace is spelled correctly
- The chapter name matches exactly

### Pages Not Appearing
Check that:
- JSON syntax is valid
- Translation keys exist
- File is in the correct directory
- Target chapter is accessible

### Multiple Entries Conflict
Entries don't conflict - they all add content to the same chapter. If content seems duplicated, check for:
- Duplicate files
- Multiple datapacks with similar content
- Incorrect target chapters

## 🎨 Real-World Example

Here's a complete example that adds monster lore to an existing chapter:

**File**: `data/yourmod/codex_entries/zombie_research.json`
```json
{
  "target_chapter": "monsters",
  "pages": [
    {
      "type": "title",
      "text": "yourmod.codex.zombie_research.title"
    },
    {
      "type": "entity",
      "entity": "minecraft:zombie",
      "scale": 1.0
    },
    {
      "type": "text",
      "text": "yourmod.codex.zombie_research.behavior"
    },
    {
      "type": "text", 
      "text": "yourmod.codex.zombie_research.weaknesses"
    }
  ]
}
```

**Translations**: `assets/yourmod/lang/en_us.json`
```json
{
  "yourmod.codex.zombie_research.title": "Zombie Behavioral Studies",
  "yourmod.codex.zombie_research.behavior": "Through careful observation, zombies exhibit predictable patterns...",
  "yourmod.codex.zombie_research.weaknesses": "Key vulnerabilities include sunlight sensitivity and..."
}
```

This entry will add 4 pages to the existing "monsters" chapter, providing additional zombie-specific information alongside any other monster-related content.

## 📚 Related Documentation

- [Chapters](chapters.md) - Creating the chapters that entries target
- [Page Types](page-types.md) - Complete list of available page types
- [Translations](translations.md) - Setting up proper localization
- [Examples](examples.md) - More complete working examples
