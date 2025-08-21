# Custom Categories Example Guide

This directory contains example JSON files that demonstrate how the **Eidolon Unchained** datapack system works for creating custom codex categories and chapters.

## 📁 Directory Structure

```
src/main/resources/data/eidolonunchained/codex/
├── custom_spells/              ← Custom magic spells category
│   ├── fire_mastery.json
│   └── ice_control.json
├── community_rituals/          ← Community-driven rituals category  
│   ├── community_summoning.json
│   └── ritual_binding.json
└── expansions/                 ← Expansion content category
    └── expansion_pack.json
```

## 🎯 How It Works

### 1. **Category Creation**
- Each directory under `codex/` becomes a custom category
- Directory name becomes the category key (e.g., `custom_spells`)
- Categories are automatically added to the Eidolon codex

### 2. **Chapter Structure**
Each JSON file defines a chapter with:
- `title_key`: Translation key for the title
- `title`: Fallback title if translation missing
- `icon`: Minecraft item ID for the chapter icon
- `pages[]`: Array of page objects

### 3. **Page Types Supported**
- `text`: Basic text pages
- `crafting`: Crafting table recipes
- `ritual`: Eidolon ritual circles  
- `workbench`: Worktable recipes
- `crucible`: Crucible processes
- `list`: Bulleted lists
- `entity`: Entity information pages
-
## 📦 Final JSON Structure

When real file loading is in place, datapacks should mirror the following
layout:

```text
data/<namespace>/codex/<category>/_category.json      # Category definition
data/<namespace>/codex/<category>/<chapter>.json     # Chapter entries
```

### `_category.json`

```json
{
  "key": "yourmod.codex.category.magic",
  "name": "yourmod.codex.category.magic.name",
  "icon": "minecraft:book",
  "color": "0x9966FF",
  "description": "Optional description",
  "chapters": ["yourmod:rituals"]
}
```

### `<chapter>.json`

```json
{
  "title_key": "yourmod.codex.chapter.rituals.title",
  "title": "Rituals",                 // fallback title
  "icon": "minecraft:bell",           // optional
  "prerequisites": ["yourmod:starter_research"],
  "pages": [ { "type": "text", "content": "..." } ]
}
```

### Translation Key Conventions

- Categories: `yourmod.codex.category.<id>.name`
- Chapters: `yourmod.codex.chapter.<id>.title`
- Entries and page text: `yourmod.codex.entry.<id>.*`

Place these keys in `assets/<namespace>/lang/<lang>.json`.

### Research Chapters

Codex chapters can be gated behind research. Use the `prerequisites`
array in chapter JSON to require research IDs. Research chapters reside
under `data/<namespace>/research_chapters/` and may reference the
category via their own `category` field.

## 📝 Example JSON Structure

```json
{
  "title_key": "eidolonunchained.codex.custom_spells.fire_mastery.title",
  "title": "Fire Mastery",
  "icon": "minecraft:fire_charge",
  "pages": [
    {
      "type": "text",
      "content": "Your magical text content here..."
    },
    {
      "type": "crafting",
      "content": "Recipe Name",
      "data": {
        "result": {"item": "minecraft:fire_charge", "count": 4},
        "pattern": [" R ", "RGR", " R "],
        "key": {"R": "minecraft:redstone", "G": "minecraft:gold_ingot"}
      }
    }
  ]
}
```

## 🔧 Implementation Status

### ✅ **Currently Working** (via Reflection)
- ✅ Directory scanning and JSON loading
- ✅ Category creation with custom icons and colors
- ✅ Chapter generation from JSON files  
- ✅ Page conversion to Eidolon format
- ✅ Integration with existing Eidolon categories

### 🚀 **Future Enhancement** (when CodexEvents available)
- Direct event-based integration (no reflection)
- Better error handling and validation
- Hot-reloading of JSON changes
- Advanced page type support

## 🎮 Testing Your Custom Categories

1. **Add JSON Files**: Place your JSON files in the appropriate directories
2. **Add Translations**: Update `lang/en_us.json` with your title keys
3. **Build & Test**: Run `./gradlew build` and test in-game
4. **Check Logs**: Look for `[EidolonUnchained]` log entries for status

## 📚 Current Categories

| Category | Description | Example Chapters |
|----------|-------------|------------------|
| **Custom Spells** | Community-created spell techniques | Fire Mastery, Ice Control |
| **Community Rituals** | Multi-player collaborative rituals | Community Summoning, Ritual Binding |
| **Expansions** | Additional content packs | Expansion Content Pack |

## 🔮 Advanced Usage

### Custom Icons
Use any Minecraft item ID:
```json
"icon": "minecraft:enchanted_book"
"icon": "eidolon:soul_gem"  
"icon": "minecraft:nether_star"
```

### Page Data Objects
Each `ritual` page requires both the ritual ID and a translation key base for the
title:
```json
{
  "type": "ritual",
  "ritual": "eidolon:crystallization",
  "text": "eidolonunchained.codex.page.crystal_ritual"
}
```

This system provides **full datapack functionality** while maintaining compatibility with current Eidolon versions through reflection-based integration! 🎉
