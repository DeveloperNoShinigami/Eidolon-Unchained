# Examples

Complete working examples demonstrating all features of the Eidolon Unchained datapack system.

## 📁 Current Examples

The mod includes a comprehensive example system located at:
- **Path**: `src/main/resources/data/eidolonunchained/`
- **Category**: `examples`
- **Purpose**: Demonstrate all system features

## 🎯 What's Included

### 1. Category Example
**File**: `codex/examples/_category.json`

A complete category definition showing:
- Custom icon (book)
- Custom color (blue)
- Proper translation keys
- Description text

### 2. Chapter Example
**File**: `codex/examples/getting_started.json`

A lightweight chapter definition containing:
- Title translation key
- Icon definition
- Category placement (by folder structure)

### 3. Entry Examples

#### Text Entry
**File**: `codex_entries/text_example.json`
- Demonstrates basic text pages
- Multiple paragraphs
- Proper translation structure

#### Recipe Entry  
**File**: `codex_entries/recipe_example.json`
- Shows recipe integration
- Crafting table recipe example
- Mixed text and recipe content

#### Entity Entry
**File**: `codex_entries/entity_example.json`
- 3D entity rendering
- Custom scaling
- Entity-specific descriptions

### 4. Research Examples

#### Simple Research
**File**: `research_entries/zombie_hunter.json`
- Basic kill task (1 zombie)
- Item reward
- No prerequisites

#### Conditional Research
**File**: `research_entries/undead_researcher.json` 
- Dimension condition (overworld only)
- Kill task (1 zombie)
- Book reward

## 📖 Complete Example Walkthrough

Let's examine each file in detail:

### Category Definition
```json
{
  "key": "examples",
  "name": "eidolonunchained.codex.category.examples",
  "icon": "minecraft:book",
  "color": "0x4169E1",
  "description": "Example content demonstrating the datapack system"
}
```

**Key Points**:
- `key` matches the folder name
- `name` references translation key
- Standard Minecraft item for icon
- Hex color code for theme

### Chapter Foundation
```json
{
  "title": "eidolonunchained.codex.chapter.getting_started",
  "icon": "minecraft:compass"
}
```

**Key Points**:
- Chapters are lightweight definitions with just title and icon
- No pages array - content is in separate entries
- `title` references translation key
- `icon` is a standard Minecraft item

### Entry with Pages
```json
{
  "target_chapter": "getting_started",
  "pages": [
    {
      "type": "title",
      "text": "eidolonunchained.codex.entry.text_example.title"
    },
    {
      "type": "text",
      "text": "eidolonunchained.codex.entry.text_example.description"
    }
  ]
}
```

**Key Points**:
- Always starts with title page
- Icon for chapter identification
- Translation keys for all text

### Entry Extension
```json
{
  "target_chapter": "getting_started",
  "pages": [
    {
      "type": "title",
      "text": "eidolonunchained.codex.entry.text_example.title"
    },
    {
      "type": "text",
      "text": "eidolonunchained.codex.entry.text_example.description"
    }
  ]
}
```

**Key Points**:
- `target_chapter` matches chapter file name
- Adds pages to existing chapter
- Own title page for organization

### Research Task
```json
{
  "title": "eidolonunchained.research.zombie_hunter.title",
  "description": "eidolonunchained.research.zombie_hunter.description",
  "icon": "minecraft:rotten_flesh",
  "tasks": [
    {
      "type": "kill_entities",
      "target": "minecraft:zombie", 
      "count": 1
    }
  ],
  "rewards": [
    {
      "type": "item",
      "item": "minecraft:iron_sword"
    }
  ]
}
```

**Key Points**:
- Clear task definition
- Specific entity targeting
- Concrete reward

## 🎨 Creating Your Own Examples

### Custom Category Example
Create your own themed category:

```json
{
  "key": "dark_arts",
  "name": "yourmod.codex.category.dark_arts",
  "icon": "minecraft:wither_skeleton_skull",
  "color": "0x4A0E4E",
  "description": "Forbidden knowledge and dark magic"
}
```

### Advanced Chapter Example
More complex chapter with multiple page types:

```json
{
  "title": "yourmod.codex.chapter.necromancy_basics",
  "icon": "minecraft:bone",
  "pages": [
    {
      "type": "title",
      "text": "yourmod.codex.chapter.necromancy_basics"
    },
    {
      "type": "text",
      "text": "yourmod.codex.chapter.necromancy_basics.warning"
    },
    {
      "type": "entity",
      "entity": "minecraft:skeleton",
      "scale": 0.7
    },
    {
      "type": "text",
      "text": "yourmod.codex.chapter.necromancy_basics.theory"
    }
  ]
}
```

### Cross-Mod Entry Example
Adding content to base Eidolon chapters:

```json
{
  "target_chapter": "eidolon:monsters",
  "pages": [
    {
      "type": "title",
      "text": "yourmod.codex.entry.enhanced_monsters.title"
    },
    {
      "type": "text", 
      "text": "yourmod.codex.entry.enhanced_monsters.intro"
    }
  ]
}
```

### Complex Research Example
Research with multiple conditions and tasks:

```json
{
  "title": "yourmod.research.master_summoner.title",
  "description": "yourmod.research.master_summoner.description", 
  "icon": "eidolon:void_amulet",
  "conditions": [
    {
      "type": "dimension",
      "dimension": "minecraft:the_nether"
    },
    {
      "type": "time",
      "time": "night"
    }
  ],
  "tasks": [
    {
      "type": "kill_entities",
      "target": "minecraft:wither_skeleton",
      "count": 5
    },
    {
      "type": "collect_items",
      "item": "minecraft:nether_star",
      "count": 1
    }
  ],
  "rewards": [
    {
      "type": "item",
      "item": "eidolon:void_amulet"
    },
    {
      "type": "unlock_chapter",
      "chapter": "advanced_summoning"
    }
  ]
}
```

## 🔧 Testing Your Examples

### In-Game Verification
1. **Load the world** with your datapack
2. **Run `/reload`** to refresh content
3. **Open Eidolon codex** and verify:
   - Category appears with correct icon/color
   - Chapter loads within category
   - All entries appear in chapter
   - Translations display correctly

### Console Checking
Look for these log messages:
```
[CodexDataManager]: Found category: your_category
[CodexDataManager]: Loaded chapter: your_chapter
[CodexDataManager]: Processing entry: your_entry → your_chapter
[ResearchDataManager]: Loaded research: your_research
```

### Common Issues
- **Missing translations**: Raw translation keys display
- **Invalid JSON**: Content doesn't load, check console
- **Wrong target**: Entry doesn't appear in expected chapter
- **File location**: Content not found during scanning

## 📋 Example Checklist

When creating examples, ensure:

- [ ] **Valid JSON syntax** in all files
- [ ] **Translation keys exist** for all text
- [ ] **File paths correct** according to system structure
- [ ] **Icons reference valid items** (preferably vanilla/Eidolon)
- [ ] **Target chapters exist** for entries
- [ ] **Research tasks are achievable** in normal gameplay
- [ ] **Rewards are appropriate** for task difficulty

## 🚀 From Examples to Production

### Scaling Up
1. **Start with examples** to understand the system
2. **Create focused prototypes** for your specific content
3. **Build incrementally** with frequent testing
4. **Add complexity gradually** as you learn

### Best Practices
1. **Document your structure** like these examples
2. **Use consistent naming** across all files
3. **Test cross-mod compatibility** early
4. **Plan for translation** from the beginning

### Quality Assurance
1. **Test with minimal mods** to isolate issues
2. **Verify on clean worlds** to ensure reproducibility
3. **Check with different languages** if providing translations
4. **Test research progression** from start to finish

## 📚 Related Documentation

- [Quick Start Guide](getting-started.md) - Step-by-step tutorial
- [Page Types](page-types.md) - All available page types
- [Research System](research-system.md) - Research mechanics
- [Troubleshooting](troubleshooting.md) - Common issues and solutions
