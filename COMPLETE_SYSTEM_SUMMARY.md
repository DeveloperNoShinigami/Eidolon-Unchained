# Eidolon Unchained - Complete System Documentation

## 🎯 System Overview

The **Eidolon Unchained Codex Extension System** is now **FULLY OPERATIONAL** and allows users to extend Eidolon Repraised's in-game codex through JSON datapacks. The system provides rich visual elements, formatting options, and multiple page types for creating professional-quality codex entries.

## ✅ Current Status

### System Health
- **JSON Loading**: ✅ 5 files loading successfully
- **Chapter Integration**: ✅ 4 Eidolon chapters extended
- **Page Conversion**: ✅ 5 page types fully supported
- **Visual Elements**: ✅ Icons, colors, formatting codes working
- **Error Handling**: ✅ Comprehensive logging and fallbacks

### Log Verification
```
[INFO] CodexDataManager: Successfully loaded codex entry 'eidolonunchained:advanced_monsters' with 4 pages
[INFO] EidolonCodexIntegration: ✓ Injecting 2 entries into chapter MONSTERS
[INFO] EidolonCodexIntegration: Codex integration complete!
```

## 📁 File Structure

```
src/main/resources/
├── data/
│   └── eidolonunchained/
│       ├── codex_entries/           # Main codex extension files
│       │   ├── advanced_monsters.json
│       │   ├── rare_monsters.json
│       │   ├── crystal_rituals.json
│       │   ├── advanced_summoning.json
│       │   └── void_mastery.json
│       └── research_entries/        # Research system files (future)
│           ├── ritual_master.json
│           └── advanced_soul_manipulation.json
└── assets/
    └── eidolonunchained/
        └── lang/
            └── en_us.json          # Translation keys for all entries
```

## 🎨 Supported Visual Elements

### 1. Icons and Items
Every page can display:
- **Page icons** - Displayed at top of each page
- **Item icons** - Show specific Minecraft/Eidolon items  
- **Side images** - Positioned anywhere on page with x/y coordinates
- **Entity models** - 3D rotatable models for creatures

### 2. Text Formatting
Full Minecraft formatting code support:
- **Colors**: `§4` (red), `§a` (green), `§d` (purple), `§6` (gold), etc.
- **Styles**: `§l` (bold), `§o` (italic), `§n` (underline), `§m` (strikethrough)
- **Reset**: `§r` (clears all formatting)

### 3. Layout Elements
- **Multi-column layouts** for lists and materials
- **Ritual circle diagrams** with top-down item placement
- **Recipe grids** showing crafting patterns
- **Progress indicators** and status information

## 📄 Complete Page Type Reference

### Title Page
```json
{
  "type": "title",
  "title": "§5Advanced Wraith Summoning§r",
  "subtitle": "§7Mastering Ethereal Bindings§r", 
  "icon": "eidolon:wraith_heart",
  "background_color": "#2d1b3d",
  "text_color": "#ffffff"
}
```
**Visual**: Large centered title with optional subtitle and background styling

### Text Page  
```json
{
  "type": "text",
  "title": "§dWraith Binding Protocols§r",
  "icon": "eidolon:unholy_symbol",
  "content": [
    "Advanced wraith summoning requires precise control and understanding",
    "of §dspectral energies§r and ethereal manipulation.",
    "",
    "§l§4⚠ WARNING:§r Each phase must be completed with exact timing"
  ],
  "side_images": [
    {"item": "eidolon:soul_gem", "x": 120, "y": 40}
  ]
}
```
**Visual**: Multi-line formatted text with side images and icons

### Entity Page
```json
{
  "type": "entity", 
  "title": "§dBound Wraith§r",
  "entity": "eidolon:wraith",
  "description": [
    "A §dBound Wraith§r is a spectral entity tethered to the summoner's will.",
    "§l§5Physical Manifestation:§r",
    "§7• Translucent, shifting ethereal form§r"
  ],
  "spawn_conditions": [
    "§l§6Summoning Requirements:§r",
    "§7• Ritual circle in darkened area§r"
  ],
  "drops": [
    {
      "item": "eidolon:wraith_heart",
      "chance": "100%", 
      "description": "Primary binding component"
    }
  ]
}
```
**Visual**: 3D entity model with detailed stats, spawn conditions, and drop table

### List Page
```json
{
  "type": "list",
  "title": "§6Required Summoning Materials§r",
  "icon": "minecraft:chest",
  "items": [
    {
      "item": "eidolon:wraith_heart",
      "count": 1,
      "title": "§dWraith Heart§r",
      "description": [
        "Primary focus for the binding ritual",
        "Must be harvested during new moon"
      ]
    }
  ],
  "footer": [
    "§l§6Total Soul Cost: §r§645 Energy",
    "§l§aSuccess Rate: §r§a85%§r with proper preparation"
  ]
}
```
**Visual**: Organized grid of items with counts, descriptions, and summary footer

### Ritual Page
```json
{
  "type": "ritual",
  "title": "§4Wraith Binding Ritual§r", 
  "icon": "eidolon:unholy_symbol",
  "difficulty": "§l§4Advanced§r",
  "soul_cost": 45,
  "duration": "15-20 minutes",
  "circle_layout": {
    "center": "eidolon:brazier",
    "inner_ring": [
      {"item": "eidolon:soul_gem", "position": "north"},
      {"item": "eidolon:soul_gem", "position": "south"}
    ],
    "outer_ring": [
      {"item": "minecraft:bone_block", "position": "northeast"}
    ]
  },
  "ingredients": [
    {"item": "eidolon:wraith_heart", "count": 1},
    {"item": "eidolon:silver_ingot", "count": 8}
  ],
  "steps": [
    "§61.§r Place the brazier at the ritual center during new moon",
    "§62.§r Arrange soul gems in cardinal directions (N, S, E, W)"
  ],
  "warnings": [
    "§l§4⚠ EXTREME DANGER:§r Wraiths become hostile if ritual fails"
  ],
  "result": {
    "item": "eidolon:bound_wraith",
    "description": "A wraith bound to your will for exactly 7 days."
  }
}
```
**Visual**: Top-down ritual circle diagram with step-by-step instructions and warnings

### Crafting Page ⚠️
```json
{
  "type": "crafting",
  "title": "Soul-Infused Tools",
  "icon": "eidolon:soulfire_forge", 
  "recipes": [
    {
      "type": "shaped",
      "result": {"item": "eidolon:soul_pickaxe", "count": 1},
      "pattern": ["SSS", " B ", " B "],
      "key": {
        "S": "eidolon:soul_shard",
        "B": "minecraft:blaze_rod"
      },
      "description": "A pickaxe that harvests soul energy from ores"
    }
  ]
}
```
**Status**: ⚠️ Falls back to text format (converter not yet implemented)

## 🎯 Target Chapters

| Chapter | JSON Field | Current Extensions | Description |
|---------|------------|-------------------|-------------|
| **MONSTERS** | `"MONSTERS"` | 2 entries | Creature summoning and information |
| **SUMMON_RITUAL** | `"SUMMON_RITUAL"` | 1 entry | Summoning procedures and rituals |
| **CRYSTAL_RITUAL** | `"CRYSTAL_RITUAL"` | 1 entry | Crystal-based magical workings |
| **VOID_AMULET** | `"VOID_AMULET"` | 1 entry | Void magic and amulet crafting |
| **ARCANE_GOLD** | `"ARCANE_GOLD"` | 0 entries | Arcane gold applications |

## 🔧 Technical Implementation

### Core Components
- **CodexDataManager**: Handles JSON loading and parsing with comprehensive error checking
- **EidolonCodexIntegration**: Uses reflection to inject entries into Eidolon's codex system
- **EidolonPageConverter**: Converts JSON page definitions to Eidolon's internal page objects
- **Logging System**: Detailed debug information for troubleshooting

### Integration Flow
```
1. Minecraft loads → Resource reload triggered
2. CodexDataManager.apply() → Scans for JSON files
3. JSON parsed → CodexEntry objects created
4. Server startup → EidolonCodexIntegration.attemptIntegrationIfNeeded()
5. Reflection → Eidolon chapter objects located
6. Page conversion → JSON pages converted to Eidolon pages
7. Injection → Custom entries added to Eidolon chapters
8. Success → Entries visible in-game codex
```

## 📚 Example Files

### Complete Working Examples
- `EXAMPLE_COMPLETE_CODEX_ENTRY.json` - Shows all visual elements and formatting
- `advanced_summoning.json` - Multi-page entry with all page types
- `crystal_rituals.json` - Ritual-focused content with detailed instructions

### Documentation Files
- `CODEX_SYSTEM_DOCUMENTATION.md` - Complete technical reference
- `VISUAL_ELEMENTS_REFERENCE.md` - Visual design guide and formatting codes

## 🚀 Usage Instructions

### 1. Create Your JSON File
Place in: `src/main/resources/data/eidolonunchained/codex_entries/your_name.json`

### 2. Choose Target Chapter
Use one of: `"MONSTERS"`, `"SUMMON_RITUAL"`, `"CRYSTAL_RITUAL"`, `"VOID_AMULET"`, `"ARCANE_GOLD"`

### 3. Design Your Pages
- Start with a title page for new sections
- Use text pages for general information
- Add entity pages for creature details
- Include list pages for materials/components
- Use ritual pages for step-by-step procedures

### 4. Test and Debug
- Check `latest.log` for loading confirmation
- Verify entries appear in-game codex
- Test all formatting codes and icons

## 🎨 Design Best Practices

### Visual Consistency
- Use consistent color schemes (purple/gray for Eidolon theme)
- Choose appropriate icons for each page type
- Test icon visibility and contrast

### Content Organization  
- Logical page flow (overview → details → instructions)
- Use warnings for dangerous procedures
- Include material lists before ritual instructions

### Formatting Guidelines
- Use `§l§4` for warnings and dangers
- Use `§l§a` for important tips and success info
- Use `§7` for secondary/flavor text
- Always use `§r` to reset formatting

## 🔍 Troubleshooting

### Common Issues
1. **JSON not loading**: Check file placement and JSON syntax
2. **Missing icons**: Verify item IDs are correct
3. **Formatting issues**: Ensure color codes are properly closed
4. **Page not converting**: Check page type is supported

### Debug Information
Monitor `latest.log` for:
- `CodexDataManager: Successfully loaded codex entry...`
- `EidolonCodexIntegration: ✓ Injecting X entries into chapter...` 
- `EidolonPageConverter: No converter found for page type: X`

## 🛠️ Debug Tools (NEW!)

### Debug Commands
For troubleshooting and development, these commands are available (requires operator permissions):

- **`/eidolonunchained test_translations`**: Tests the translation system and shows results
- **`/eidolonunchained reload_codex`**: Forces a reload of the codex integration

### Translation System Features
- **Multi-layer fallback**: Component translation → Direct file loading → Generated fallback
- **Real-time testing**: Debug commands work without game restart
- **Comprehensive logging**: Detailed translation attempt logs
- **Automatic fallback**: Never shows raw translation keys to players

## 🎯 System Completeness

### ✅ Fully Implemented
- JSON file loading and parsing
- 5 page types (title, text, entity, list, ritual)
- All Minecraft formatting codes
- Icon and image positioning
- Error handling and logging
- Eidolon chapter integration
- **Enhanced translation system with fallbacks**
- **Debug commands for real-time testing**

### ⚠️ Partial Implementation
- Crafting pages (fallback to text)

### 🔮 Future Enhancements
- Custom background images
- Animation support for entity pages
- Interactive ritual circle builder
- Multi-language support
- Additional target chapters

---

## 🏆 Summary

The **Eidolon Unchained Codex Extension System** is a complete, production-ready solution for extending Eidolon Repraised's codex with custom content. It provides rich visual elements, comprehensive formatting options, and professional-quality results that integrate seamlessly with the base mod.

**Status**: ✅ **FULLY OPERATIONAL** with enhanced debugging  
**Files**: 5 JSON entries loading successfully  
**Integration**: 4 Eidolon chapters extended
**Compatibility**: Minecraft 1.20.1, Forge 47.1.0, Eidolon Repraised 0.3.8.15+  
**Debug Tools**: Translation testing and runtime reloading available

The system is ready for users to create their own custom codex entries with robust translation handling, comprehensive debugging tools, and all the visual richness demonstrated in the working examples.
