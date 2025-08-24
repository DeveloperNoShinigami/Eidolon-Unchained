# Codex System Overview

**Add custom documentation pages to existing Eidolon chapters**

## How the System Works

The codex system allows you to add custom entries to existing Eidolon chapters through datapacks. The structure follows a three-tier hierarchy:

```
Categories → Chapters → Entries
```

### The Three-Tier Structure

#### 1. Categories (`codex/category_name/_category.json`)
Categories are the main sections in the codex (like "Fundamentals", "Research", etc.)

```json
{
  "key": "examples",
  "name": "eidolonunchained.codex.category.examples",
  "icon": "minecraft:book",
  "color": "0x4169E1",
  "description": "Example content demonstrating the datapack system"
}
```

#### 2. Chapters (`codex/category_name/chapter_name.json`)
Chapters are subsections within categories that group related content

```json
{
  "title": "eidolonunchained.codex.chapter.getting_started",
  "icon": "minecraft:compass"
}
```

#### 3. Entries (`codex_entries/entry_name.json`)
Individual entries are the actual content pages that target specific chapters

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

## File Structure

Your datapack must follow this exact structure:

```
data/your_mod_id/
├── codex/                          # Category and chapter definitions
│   ├── category_name/
│   │   ├── _category.json          # Category definition
│   │   ├── chapter1.json           # Chapter definition
│   │   └── chapter2.json           # Another chapter
│   └── another_category/
│       ├── _category.json
│       └── chapter3.json
│
├── codex_entries/                  # Individual entry content
│   ├── entry1.json                 # Entry targeting a chapter
│   ├── entry2.json
│   └── subfolder/
│       └── entry3.json
│
└── eidolon_research/               # Research system (optional)
    ├── research1.json
    └── research2.json
```

## Category System

Categories organize the main sections of your codex content.

### Category File Location
```
data/your_mod_id/codex/category_name/_category.json
```

### Category Structure
```json
{
  "key": "unique_category_key",
  "name": "translation.key.for.category.name",
  "icon": "minecraft:item_id",
  "color": "0xHEXCOLOR",
  "description": "Brief description of category contents"
}
```

### Built-in Categories
The mod includes these categories by default:
- `examples` - Example content and tutorials
- `custom_research` - Research-unlocked chapters

## Chapter System

Chapters are subsections within categories that group related entries.

### Chapter File Location
```
data/your_mod_id/codex/category_name/chapter_name.json
```

### Chapter Structure
```json
{
  "title": "translation.key.for.chapter.title", 
  "icon": "minecraft:item_id"
}
```

### Available Chapters
Based on the actual implementation, you can target these existing chapters:
- `getting_started` - Basic introduction content
- Any custom chapters you create in your categories

## Entry System

Entries are the actual content pages that appear within chapters.

### Entry File Location
```
data/your_mod_id/codex_entries/entry_name.json
```

### Entry Structure
```json
{
  "target_chapter": "chapter_name",
  "pages": [
    {"type": "title", "text": "translation.key"},
    {"type": "text", "text": "translation.key.details"}
  ]
}
```

### Entry Targeting
Entries must specify which chapter they belong to using `target_chapter`. This must match:
- An existing Eidolon chapter name, OR
- A chapter you've defined in your codex structure

## Auto-Discovery Integration

Entries can be automatically discovered through gameplay:

### Method 1: Target-Based Discovery
```json
{
  "target_chapter": "getting_started",
  "targets": ["block:minecraft:crafting_table"],
  "pages": [...]
}
```

### Method 2: Research Integration
1. Player triggers research through gameplay
2. Research rewards unlock specific chapters
3. Entries in those chapters become available

## Translation System

All text uses translation keys that must be defined in your lang files:

### Lang File Location
```
assets/your_mod_id/lang/en_us.json
```

### Translation Structure
```json
{
  "eidolonunchained.codex.category.examples": "Examples",
  "eidolonunchained.codex.chapter.getting_started": "Getting Started",
  "eidolonunchained.codex.entry.text_example": "Text Example",
  "eidolonunchained.codex.entry.text_example.details": "This is example content..."
}
```

## Research Integration

Research can unlock entire chapters or individual entries:

### Chapter Unlocking
1. Research targets a specific chapter name
2. When completed, all entries in that chapter become available
3. Perfect for gating advanced content behind progression

### Content Progression
```
Kill Entity → Research Progress → Chapter Unlock → Entries Available
```

## Working Example

Here's a complete working example:

### 1. Create Category
`data/mymod/codex/mystical_arts/_category.json`:
```json
{
  "key": "mystical_arts",
  "name": "mymod.codex.category.mystical_arts",
  "icon": "minecraft:enchanted_book",
  "color": "0x9932CC",
  "description": "Advanced magical techniques and theory"
}
```

### 2. Create Chapter  
`data/mymod/codex/mystical_arts/necromancy_basics.json`:
```json
{
  "title": "mymod.codex.chapter.necromancy_basics",
  "icon": "minecraft:wither_skeleton_skull"
}
```

### 3. Create Entry
`data/mymod/codex_entries/zombie_anatomy.json`:
```json
{
  "target_chapter": "necromancy_basics",
  "targets": ["entity:minecraft:zombie"],
  "pages": [
    {
      "type": "title",
      "text": "mymod.codex.entry.zombie_anatomy"
    },
    {
      "type": "text",
      "text": "mymod.codex.entry.zombie_anatomy.intro"
    },
    {
      "type": "entity",
      "entity": "minecraft:zombie"
    }
  ]
}
```

### 4. Add Translations
`assets/mymod/lang/en_us.json`:
```json
{
  "mymod.codex.category.mystical_arts": "Mystical Arts",
  "mymod.codex.chapter.necromancy_basics": "Necromancy Basics", 
  "mymod.codex.entry.zombie_anatomy": "Zombie Anatomy",
  "mymod.codex.entry.zombie_anatomy.intro": "Understanding the undead begins with studying their physical form..."
}
```

This creates a complete content hierarchy: **Mystical Arts** → **Necromancy Basics** → **Zombie Anatomy**
