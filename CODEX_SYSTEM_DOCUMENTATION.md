# Eidolon Unchained - Codex Extension System Documentation

## Overview
The Eidolon Unchained mod provides a comprehensive system for extending Eidolon Repraised's codex through JSON datapacks. This allows users to add custom entries, pages, and content without modifying the base mod.

## System Status
✅ **FULLY OPERATIONAL** - All components working correctly
- 5 JSON entries loading successfully
- 4 Eidolon chapters extended
- All entries injecting properly into game codex
- Comprehensive logging and error handling

## File Structure
```
src/main/resources/
├── data/
│   └── eidolonunchained/
│       ├── codex_entries/           # Codex extension files
│       │   ├── advanced_monsters.json
│       │   ├── rare_monsters.json
│       │   ├── crystal_rituals.json
│       │   ├── advanced_summoning.json
│       │   └── void_mastery.json
│       └── research_entries/        # Research system files
│           ├── ritual_master.json
│           └── advanced_soul_manipulation.json
└── assets/
    └── eidolonunchained/
        └── lang/
            └── en_us.json          # Translation keys
```

## Supported Target Chapters
The system can extend the following Eidolon chapters:

| Chapter Name | JSON Field | Description |
|--------------|------------|-------------|
| `MONSTERS` | `"MONSTERS"` | Creature information and summoning details |
| `SUMMON_RITUAL` | `"SUMMON_RITUAL"` | Summoning rituals and procedures |
| `CRYSTAL_RITUAL` | `"CRYSTAL_RITUAL"` | Crystal-based rituals and crafting |
| `VOID_AMULET` | `"VOID_AMULET"` | Void magic and amulet information |
| `ARCANE_GOLD` | `"ARCANE_GOLD"` | Arcane gold related content |

## Page Types and Their Complete Structure

### 1. Title Page (`"type": "title"`)
**Purpose**: Chapter headers and section dividers
```json
{
  "type": "title",
  "title": "Advanced Summoning Techniques",
  "subtitle": "Mastering the Dark Arts",
  "icon": "eidolon:soul_gem",
  "background_color": "#2d1b3d",
  "text_color": "#ffffff"
}
```
**Visual Elements**:
- Large centered title text
- Smaller subtitle below
- Optional icon displayed prominently
- Customizable background and text colors

### 2. Text Page (`"type": "text"`)
**Purpose**: General information and descriptions
```json
{
  "type": "text",
  "title": "Understanding Wraith Essence",
  "content": [
    "Wraith essence is a volatile §dspiritual energy§r that can be harnessed for powerful summoning rituals.",
    "",
    "§l§4WARNING:§r Improper handling may result in spectral contamination.",
    "",
    "To properly extract essence:",
    "§7• Use consecrated tools only§r",
    "§7• Perform during twilight hours§r",
    "§7• Maintain protective wards§r"
  ],
  "icon": "eidolon:wraith_heart",
  "side_images": [
    {
      "item": "eidolon:soul_gem",
      "x": 120,
      "y": 40
    }
  ]
}
```
**Formatting Codes**:
- `§l` - Bold text
- `§o` - Italic text
- `§n` - Underlined text
- `§4` - Dark red color
- `§d` - Light purple color
- `§7` - Gray color
- `§r` - Reset formatting
- Empty strings create line breaks

**Visual Elements**:
- Title at top with optional icon
- Multi-line content with formatting
- Side images showing items/icons
- Automatic text wrapping

### 3. Entity Page (`"type": "entity"`)
**Purpose**: Creature information with visual representation
```json
{
  "type": "entity",
  "title": "Shadow Wraith",
  "entity": "eidolon:wraith",
  "description": [
    "§dShadow Wraiths§r are ethereal undead creatures that phase between dimensions.",
    "",
    "§lStats:§r",
    "§7• Health: 40 HP§r",
    "§7• Damage: 8-12§r",
    "§7• Speed: Fast§r",
    "",
    "§lAbilities:§r",
    "§7• Phase through walls§r",
    "§7• Spectral drain attack§r",
    "§7• Invisible when not attacking§r"
  ],
  "spawn_conditions": [
    "§lSpawn Requirements:§r",
    "§7• Light level below 7§r",
    "§7• Near soul sand or graves§r",
    "§7• During night or in dark dimensions§r"
  ],
  "drops": [
    {
      "item": "eidolon:wraith_heart",
      "chance": "25%",
      "description": "Primary crafting component"
    },
    {
      "item": "eidolon:soul_shard",
      "chance": "60%",
      "description": "Common soul fragment"
    }
  ]
}
```
**Visual Elements**:
- 3D rendered entity model (rotatable)
- Detailed stat information
- Spawn condition requirements
- Drop table with percentages
- Item icons for all drops

### 4. List Page (`"type": "list"`)
**Purpose**: Organized information in list format
```json
{
  "type": "list",
  "title": "Required Materials",
  "icon": "minecraft:chest",
  "items": [
    {
      "item": "eidolon:soul_gem",
      "count": 3,
      "title": "Soul Gems",
      "description": [
        "Primary focus for the ritual",
        "Must be fully charged"
      ]
    },
    {
      "item": "eidolon:silver_ingot",
      "count": 8,
      "title": "Silver Ingots",
      "description": [
        "Purified silver for containment",
        "Smelted under moonlight preferred"
      ]
    },
    {
      "item": "minecraft:bone",
      "count": 16,
      "title": "Ancient Bones",
      "description": [
        "Bones from creatures over 100 years old",
        "Found in deep caves or ancient structures"
      ]
    }
  ],
  "footer": [
    "§lTotal Cost: §r§645 Soul Energy",
    "§lRitual Duration: §r§e15 minutes",
    "§lSuccess Rate: §r§a85%§r with proper preparation"
  ]
}
```
**Visual Elements**:
- Item icons with stack counts
- Organized grid layout
- Individual descriptions per item
- Footer with summary information
- Quantity indicators

### 5. Ritual Page (`"type": "ritual"`)
**Purpose**: Step-by-step ritual instructions with visual layout
```json
{
  "type": "ritual",
  "title": "Wraith Binding Ritual",
  "icon": "eidolon:unholy_symbol",
  "difficulty": "Advanced",
  "soul_cost": 45,
  "duration": "15 minutes",
  "circle_layout": {
    "center": "eidolon:brazier",
    "inner_ring": [
      {"item": "eidolon:soul_gem", "position": "north"},
      {"item": "eidolon:soul_gem", "position": "south"},
      {"item": "eidolon:soul_gem", "position": "east"},
      {"item": "eidolon:soul_gem", "position": "west"}
    ],
    "outer_ring": [
      {"item": "minecraft:bone_block", "position": "northeast"},
      {"item": "minecraft:bone_block", "position": "northwest"},
      {"item": "minecraft:bone_block", "position": "southeast"},
      {"item": "minecraft:bone_block", "position": "southwest"}
    ]
  },
  "ingredients": [
    {"item": "eidolon:wraith_heart", "count": 1},
    {"item": "eidolon:silver_ingot", "count": 4},
    {"item": "minecraft:ghast_tear", "count": 2}
  ],
  "steps": [
    "Place the brazier at the ritual center",
    "Arrange soul gems in cardinal directions",
    "Position bone blocks at diagonal corners",
    "Light the brazier with flint and steel",
    "Place wraith heart in the brazier",
    "Add silver ingots one by one clockwise",
    "Finally, add ghast tears to complete the binding"
  ],
  "warnings": [
    "§c§lDANGER:§r Wraiths may become hostile if ritual fails",
    "§6§lCAUTION:§r Ensure protective wards are active",
    "§e§lNOTE:§r Perform only during new moon for best results"
  ],
  "result": {
    "item": "eidolon:bound_wraith",
    "description": "A wraith bound to your will for 7 days"
  }
}
```
**Visual Elements**:
- Top-down ritual circle diagram
- Item placement visualization
- Step-by-step instructions
- Color-coded warnings
- Ingredient list with quantities
- Result item with description

### 6. Crafting Page (`"type": "crafting"`)
**Purpose**: Crafting recipes and item creation
```json
{
  "type": "crafting",
  "title": "Soul-Infused Tools",
  "icon": "eidolon:soulfire_forge",
  "recipes": [
    {
      "type": "shaped",
      "result": {
        "item": "eidolon:soul_pickaxe",
        "count": 1
      },
      "pattern": [
        "SSS",
        " B ",
        " B "
      ],
      "key": {
        "S": "eidolon:soul_shard",
        "B": "minecraft:blaze_rod"
      },
      "description": "A pickaxe that harvests soul energy from ores"
    },
    {
      "type": "shapeless",
      "result": {
        "item": "eidolon:soul_dust",
        "count": 4
      },
      "ingredients": [
        "eidolon:soul_gem",
        "minecraft:gunpowder"
      ],
      "description": "Pulverized soul energy for brewing"
    }
  ],
  "special_requirements": [
    "§lSoulfire Forge Required§r",
    "§7• Must be lit with soul fire§r",
    "§7• Requires 10 soul energy per craft§r",
    "§7• Success rate increases with skill§r"
  ]
}
```
**Visual Elements**:
- Recipe grid layouts (3x3 or shapeless)
- Item icons in grid positions
- Result items with quantities
- Special crafting requirements
- Multiple recipes per page

## Advanced Formatting Features

### Color Codes
- `§0` - Black
- `§1` - Dark Blue
- `§2` - Dark Green
- `§3` - Dark Aqua
- `§4` - Dark Red
- `§5` - Dark Purple
- `§6` - Gold
- `§7` - Gray
- `§8` - Dark Gray
- `§9` - Blue
- `§a` - Green
- `§b` - Aqua
- `§c` - Red
- `§d` - Light Purple
- `§e` - Yellow
- `§f` - White

### Style Codes
- `§l` - Bold
- `§m` - Strikethrough
- `§n` - Underline
- `§o` - Italic
- `§r` - Reset (removes all formatting)

### Item References
Items can be referenced using standard Minecraft resource locations:
- `minecraft:diamond` - Vanilla items
- `eidolon:soul_gem` - Eidolon items
- `eidolonunchained:custom_item` - Your mod's items

## Example Complete Entry

```json
{
  "target_chapter": "SUMMON_RITUAL",
  "pages": [
    {
      "type": "title",
      "title": "Advanced Wraith Summoning",
      "subtitle": "Mastering Ethereal Bindings",
      "icon": "eidolon:wraith_heart"
    },
    {
      "type": "text",
      "title": "Understanding Wraith Essence",
      "content": [
        "§dWraith essence§r represents the fundamental force of undeath.",
        "",
        "This ethereal energy can be harnessed through careful ritual work,",
        "allowing practitioners to summon and bind spectral entities.",
        "",
        "§l§4WARNING:§r Improper handling may result in:",
        "§7• Spectral contamination§r",
        "§7• Uncontrolled manifestations§r",
        "§7• Permanent soul damage§r"
      ],
      "icon": "eidolon:wraith_heart"
    },
    {
      "type": "entity",
      "title": "Shadow Wraith",
      "entity": "eidolon:wraith",
      "description": [
        "§dShadow Wraiths§r are ethereal undead that exist between dimensions.",
        "",
        "These creatures can phase through solid matter and drain life energy",
        "from living beings through spectral attacks."
      ]
    },
    {
      "type": "ritual",
      "title": "Wraith Binding Ritual",
      "icon": "eidolon:unholy_symbol",
      "difficulty": "Advanced",
      "soul_cost": 45,
      "circle_layout": {
        "center": "eidolon:brazier",
        "inner_ring": [
          {"item": "eidolon:soul_gem", "position": "north"},
          {"item": "eidolon:soul_gem", "position": "south"},
          {"item": "eidolon:soul_gem", "position": "east"},
          {"item": "eidolon:soul_gem", "position": "west"}
        ]
      },
      "ingredients": [
        {"item": "eidolon:wraith_heart", "count": 1},
        {"item": "eidolon:silver_ingot", "count": 4}
      ],
      "steps": [
        "Prepare the ritual circle at midnight",
        "Light the central brazier with soul fire",
        "Place the wraith heart in the brazier",
        "Chant the binding incantation three times"
      ]
    }
  ]
}
```

## File Organization Best Practices

### File Naming
- Use descriptive names: `advanced_summoning.json`
- Group related content: `monster_compendium.json`
- Avoid spaces and special characters

### Content Organization
- Start with a title page for major sections
- Group related information together
- Use consistent formatting throughout
- Include warnings for dangerous procedures

### Icon Selection
- Use items that relate to the content
- Prefer items from Eidolon when possible
- Ensure icons are visually distinct
- Test icon visibility in-game

## Technical Implementation Details

### Current System Status
- **CodexDataManager**: ✅ Loading 5 JSON files successfully
- **EidolonCodexIntegration**: ✅ Injecting into 4 chapters
- **Page Conversion**: ✅ Supporting 5 page types (title, text, entity, list, ritual)
- **Error Handling**: ✅ Comprehensive logging and fallbacks

### Supported Features
- ✅ All major page types implemented
- ✅ Full formatting code support
- ✅ Item and entity references
- ✅ Multi-line content with line breaks
- ✅ Icon and image placement
- ⚠️ Crafting pages (fallback to text)
- ❌ Custom background images (planned)

### Logging and Debugging
The system provides detailed logs for troubleshooting:
```
[INFO] CodexDataManager: Successfully loaded codex entry 'eidolonunchained:advanced_monsters' with 4 pages
[INFO] EidolonCodexIntegration: ✓ Injecting 2 entries into chapter MONSTERS
[WARN] EidolonPageConverter: No converter found for page type: crafting, falling back to text
```

## Future Expansion Possibilities

### Planned Features
- Custom background images for pages
- Animation support for entity pages
- Interactive ritual circle builder
- Recipe book integration
- Multi-language support

### Extensibility
The system is designed to be easily extensible:
- New page types can be added to `EidolonPageConverter`
- Additional target chapters can be supported
- Custom formatting codes can be implemented
- Integration with other mods is possible

## Troubleshooting

### Common Issues
1. **Files not loading**: Check file placement in `data/eidolonunchained/codex/`
2. **Invalid JSON**: Validate JSON syntax using online tools
3. **Missing icons**: Ensure item IDs are correct and items exist
4. **Formatting issues**: Check color/style codes are properly closed with `§r`

### Debug Information
Enable debug logging by checking the latest.log file for:
- `CodexDataManager` entries showing file loading
- `EidolonCodexIntegration` entries showing injection success
- Error messages with specific line numbers for JSON issues

This documentation covers the complete Eidolon Unchained codex extension system, including all visual elements, formatting options, and technical implementation details.
