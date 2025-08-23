# Eidolon Unchained - Datapack Content Creation System

## Project Vision

**Eidolon Unchained** is a comprehensive **datapack-driven extension system** for the Eidolon mod that empowers modpack makers and developers to create rich, custom content through JSON configuration alone.

## Target Audience

### Primary Users
- **Modpack Creators**: Building themed modpacks with custom Eidolon content
- **Server Administrators**: Adding server-specific magical content and lore
- **Content Creators**: Developing expansion content packs for distribution
- **Developers**: Creating addon mods that integrate with Eidolon

### Design Philosophy
- **Zero Code Required**: All content creation through datapacks
- **Maximum Flexibility**: Every system configurable through JSON
- **Seamless Integration**: New content feels native to Eidolon
- **Distribution Friendly**: Easy sharing through resource/data packs

## Content Creation Capabilities

### 1. Codex System Extensions
```json
// Add entries to any existing chapter
{
  "title": "My Custom Entry",
  "target_chapter": "wooden_stand",  // Extend Eidolon chapter
  "pages": [...]
}

// Create custom chapters in custom categories
{
  "title": "My Chapter",
  "icon": "minecraft:book",
  "category": "my_custom_category"
}
```

### 2. Research System Integration
```json
// Conditional content unlocking
{
  "title": "Advanced Technique",
  "prerequisites": ["eidolon:basic_research"],
  "category": "advanced_magic"
}
```

### 3. Custom Categories
```json
// Brand new content categories
{
  "name": "Elemental Magic",
  "icon": "minecraft:fire_charge",
  "color": "0xFF6B35"
}
```

## Content Distribution

### Datapack Structure
```
my_eidolon_expansion/
├── pack.mcmeta
└── data/
    └── my_namespace/
        ├── codex/
        │   ├── categories/
        │   │   └── elemental.json
        │   └── chapters/
        │       └── fire_magic.json
        ├── codex_entries/
        │   ├── flame_spells.json
        │   └── fire_rituals.json
        └── eidolon_research/
            └── pyromancy_tree.json
```

### Installation
1. **Modpack Creators**: Include datapacks in modpack distribution
2. **Server Admins**: Place in `world/datapacks/` folder
3. **Players**: Install in `.minecraft/saves/world/datapacks/`

## Expansion Capabilities

### What Modpack Makers Can Create
- **Custom Magical Schools**: Entirely new categories of magic
- **Themed Content**: Horror, nature, elemental, cosmic themes
- **Progressive Systems**: Unlock content through research chains
- **Cross-Mod Integration**: References to other mods' items/blocks
- **Server Lore**: Custom worldbuilding and magical systems
- **Challenge Content**: Advanced techniques and rare knowledge

### Example Use Cases
1. **"Arcane Academy" Modpack**: Student progression through magical schools
2. **"Forbidden Knowledge" Server**: Dark magic unlocked through dangerous research
3. **"Elemental Mastery" Pack**: Separate codex sections for each element
4. **"Integration Hub"**: Connecting Eidolon with Thaumcraft, Botania, etc.

## Development Principles

### For Extension Developers
- **Datapack-First Design**: Avoid hardcoding, enable JSON configuration
- **Backward Compatibility**: Never break existing Eidolon functionality
- **Modular Systems**: Features should work independently
- **Documentation Focus**: Comprehensive guides for content creators

### For Content Creators
- **Simple JSON**: No programming knowledge required
- **Rich Examples**: Copy-paste templates for common patterns
- **Validation Tools**: Clear error messages for configuration issues
- **Flexible Targeting**: Reference any existing content for extension

## Technical Architecture

### Core Systems
1. **CodexDataManager**: Loads and manages custom codex content
2. **ResearchDataManager**: Handles conditional unlocking systems
3. **Integration Layer**: Seamlessly adds content to Eidolon's existing structure
4. **Reflection Bridge**: Accesses Eidolon's internal systems safely

### Extension Points
- **Chapter Extensions**: Add entries to existing chapters
- **Category Creation**: Define new content categories
- **Research Integration**: Prerequisite-based content unlocking
- **Page Types**: Support for all Eidolon page types
- **Icon Systems**: Custom icons for chapters and categories

## Success Metrics

### For Modpack Makers
- ✅ Create custom content without touching code
- ✅ Distribute content through standard datapack mechanisms
- ✅ Seamless integration that feels like base Eidolon
- ✅ Progressive content unlocking through research

### For the Ecosystem
- ✅ Rich library of community-created content packs
- ✅ Easy sharing and remixing of magical content
- ✅ Cross-modpack compatibility for content packs
- ✅ Thriving content creator community

This system transforms Eidolon from a fixed magic mod into a **platform for magical content creation**, enabling the community to build the magical experiences they envision.
