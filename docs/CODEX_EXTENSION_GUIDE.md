# Eidolon Unchained: Codex Extension System - Complete Guide

This system allows you to add new entries to existing Eidolon codex chapters through JSON datapacks with proper localization support.

## Overview

The Eidolon Unchained system extends existing Eidolon chapters with new entries through a datapack approach that includes:
- **JSON Entry Definitions**: Define page structure and content references
- **Language File Support**: Proper localization following Minecraft standards
- **Multiple Page Types**: Title, text, entity, and crafting pages
- **Runtime Integration**: Automatically injects into Eidolon's codex system

## File Structure

Your datapack should follow this structure:
```
src/main/resources/
├── data/
│   └── eidolonunchained/
│       ├── codex_entries/           # Main codex extension files
│       │   ├── advanced_monsters.json
│       │   ├── crystal_rituals.json
│       │   ├── rare_monsters.json
│       │   ├── advanced_summoning.json
│       │   ├── void_mastery.json
│       │   └── [other_entries].json
│       └── research_entries/        # Research system files (future expansion)
│           ├── ritual_master.json
│           └── advanced_soul_manipulation.json
└── assets/
    └── eidolonunchained/
        └── lang/
            └── en_us.json          # Translation keys for all entries
```

## JSON Entry Format

### Entry Structure
```json
{
  "target_chapter": "CHAPTER_NAME",
  "pages": [
    {
      "type": "title",
      "text": "translation.key.for.title"
    },
    {
      "type": "text", 
      "text": "translation.key.for.content"
    },
    {
      "type": "entity",
      "entity": "mod:entity_name"
    },
    {
      "type": "crafting",
      "recipe": "mod:recipe_name"
    }
  ]
}
```

### Available Chapter Names
- `MONSTERS` - Monster documentation
- `CRYSTAL_RITUAL` - Crystal ritual techniques  
- `SUMMON_RITUAL` - Summoning rituals
- `VOID_AMULET` - Void manipulation
- `ARCANE_GOLD` - Arcane gold crafting
- `SILVER_ARMOR` - Silver equipment
- And others (check Eidolon's CodexChapters class)

### Page Types

#### Title Page
```json
{
  "type": "title",
  "text": "translation.key"
}
```

#### Text Page
```json
{
  "type": "text",
  "text": "translation.key"
}
```

#### Entity Page (3D Model Display)
```json
{
  "type": "entity",
  "entity": "eidolon:necromancer"
}
```

#### Crafting/Recipe Page
```json
{
  "type": "crafting",
  "recipe": "eidolon:crystallization"
}
```

## Language File System

### Required Language File
Create: `src/main/resources/assets/eidolonunchained/lang/en_us.json`

### Translation Key Format
```json
{
  "eidolonunchained.codex.entry.{entry_name}.title": "Display Title",
  "eidolonunchained.codex.entry.{entry_name}.intro": "Introduction text",
  "eidolonunchained.codex.entry.{entry_name}.{section}": "Section content"
}
```

### Example Language File
```json
{
  "eidolonunchained.codex.entry.advanced_monsters.title": "Advanced Monster Studies",
  "eidolonunchained.codex.entry.advanced_monsters.intro": "Comprehensive guide to magical creatures and their behaviors.",
  "eidolonunchained.codex.entry.advanced_monsters.necromancer": "Necromancers are powerful spellcasters that command undead minions.",
  
  "eidolonunchained.codex.entry.crystal_rituals.title": "Crystal Ritual Mastery",
  "eidolonunchained.codex.entry.crystal_rituals.intro": "Advanced techniques for crystal-based magical workings."
}
```

## Complete Example

### Entry JSON (`crystal_rituals.json`)
```json
{
  "target_chapter": "CRYSTAL_RITUAL",
  "pages": [
    {
      "type": "title",
      "text": "eidolonunchained.codex.entry.crystal_rituals.title"
    },
    {
      "type": "text",
      "text": "eidolonunchained.codex.entry.crystal_rituals.intro"
    },
    {
      "type": "crafting",
      "recipe": "eidolon:crystallization"
    },
    {
      "type": "text",
      "text": "eidolonunchained.codex.entry.crystal_rituals.advanced"
    }
  ]
}
```

### Corresponding Lang Entries
```json
{
  "eidolonunchained.codex.entry.crystal_rituals.title": "Crystal Ritual Mastery",
  "eidolonunchained.codex.entry.crystal_rituals.intro": "Advanced techniques for crystal-based magical workings. This improved approach provides greater magical output through proper harmonic alignment.",
  "eidolonunchained.codex.entry.crystal_rituals.advanced": "Master practitioners can create complex crystal networks that maintain their charge for extended periods."
}
```

## Important Notes

### Restart vs Reload Behavior
⚠️ **Important**: Changes to language files require a **full game restart**, not just `/reload`!

- **JSON Structure Changes**: Can be reloaded with `/reload`
- **Language File Changes**: Require complete game restart
- **New Files**: Require game restart

This is because:
1. Language files are loaded during game initialization
2. Asset reloading is limited in scope compared to data reloading
3. Minecraft caches language resources aggressively

### Best Practices

1. **Plan Your Text First**: Write all your lang file content before testing
2. **Use Descriptive Keys**: Make translation keys self-documenting
3. **Test Structure First**: Use placeholder text to test JSON structure with `/reload`
4. **Final Polish**: Add proper lang entries and restart to see final result

### Text Formatting

- **No Manual Line Breaks**: Don't use `\n` - text wraps automatically
- **Natural Flow**: Write text as continuous prose
- **Section Separation**: Use separate pages for different topics
- **Reasonable Length**: Keep individual text pages to 2-3 sentences

### Debug Commands (Added in v0.3.8.16+)

For testing and debugging translation issues, the following commands are available (requires operator permissions):

- **`/eidolonunchained test_translations`**: Tests the translation system and shows results in chat and logs
- **`/eidolonunchained reload_codex`**: Forces a reload of the codex integration system

These commands help identify translation problems without needing full game restarts during development.

## Integration Details

The system works by:
1. **Loading Phase**: CodexDataManager loads all JSON files during server start
2. **Integration Phase**: EidolonCodexIntegration injects entries into Eidolon chapters  
3. **Page Conversion**: EidolonPageConverter creates proper Eidolon page objects
4. **Localization**: Minecraft's translation system resolves text keys

## Troubleshooting

### Common Issues

1. **Entries Not Appearing**: Check logs for loading errors, ensure integration succeeded
2. **Text Shows Translation Keys** (e.g., "chapter.title"): Use debug commands to test, ensure full game restart after lang changes
3. **Format Errors**: Remove `\n` line breaks, use proper JSON structure  
4. **Missing Pages**: Verify page types are supported (title, text, entity, crafting)

### Debug Commands (New!)

Use these commands for troubleshooting (requires op/creative):

```
/eidolonunchained test_translations    # Test translation system
/eidolonunchained reload_codex        # Force reload integration
```

The test command will show in chat whether translations are working and provide detailed logs.

### Debug Logs
Look for these log messages:
```
[INFO] CodexDataManager: Successfully loaded codex entry 'name' with X pages
[INFO] EidolonCodexIntegration: ✓ Injecting X entries into chapter Y
[INFO] EidolonPageConverter: Successfully translated: key -> text
[WARN] EidolonPageConverter: Translation key not found, using fallback
```

### Translation Issues

If you see raw translation keys instead of proper text:

1. **Run `/eidolonunchained test_translations`** to identify the problem
2. **Check the logs** for specific error messages  
3. **Verify language file location**: `assets/eidolonunchained/lang/en_us.json`
4. **Ensure full game restart** - language changes require complete restart, not `/reload`
5. **Try `/eidolonunchained reload_codex`** as a last resort

## System Files Overview

- **CodexDataManager**: Loads and manages JSON entries
- **EidolonCodexIntegration**: Injects entries into Eidolon's system
- **EidolonPageConverter**: Converts JSON to Eidolon page objects
- **Lang Files**: Provide localized text content

This system provides a clean, maintainable way to extend Eidolon's documentation with properly formatted, localized content that integrates seamlessly with the existing codex.
- `eidolon:pewter` - Pewter
- `eidolon:enchanted_ash` - Enchanted Ash
- `eidolon:arcane_gold` - Arcane Gold
- `eidolon:reagents` - Reagents
- `eidolon:soul_gems` - Soul Gems
- `eidolon:shadow_gem` - Shadow Gem
- `eidolon:inlays` - Inlays
- `eidolon:tallow` - Tallow

**Equipment & Tools:**
- `eidolon:basic_baubles` - Basic Baubles
- `eidolon:void_amulet` - Void Amulet
- `eidolon:warded_mail` - Warded Mail
- `eidolon:soulfire_wand` - Soulfire Wand
- `eidolon:bonechill_wand` - Bonechill Wand
- `eidolon:angel_sight` - Archangel's Sight

**Rituals & Magic:**
- `eidolon:rituals` - Rituals
- `eidolon:crystal_ritual` - Crystallization Ritual
- `eidolon:summon_ritual` - Lesser Summoning
- `eidolon:sanguine_ritual` - Sanguine Items
- `eidolon:allure_ritual` - Ritual of Alluring
- `eidolon:repelling_ritual` - Ritual of Repelling
- `eidolon:deceit_ritual` - Ritual of Deceit
- `eidolon:time_rituals` - Time Rituals
- `eidolon:purify_ritual` - Ritual of Purifying
- `eidolon:recharge_ritual` - Ritual of Recharging
- `eidolon:capture_ritual` - Ritual of Capturing
- `eidolon:locate_ritual` - Ritual of Locating

**Crafting Systems:**
- `eidolon:crucible` - Crucible
- `eidolon:basic_alchemy` - Basic Alchemy
- `eidolon:wooden_stand` - Apothecary Brewing
- `eidolon:magic_workbench` - Magic Workbench
- `eidolon:brazier` - Brazier
- `eidolon:artifice` - Artifice

**Nature & Creatures:**
- `eidolon:plants` - Rare Flora
- `eidolon:monsters` - Monsters
- `eidolon:critters` - Critters
- `eidolon:nature_index` - Natural Phenomena

**Blocks & Decoration:**
- `eidolon:decorations` - Decorative Blocks
- `eidolon:item_providers` - Item Receptacles

**Other:**
- `eidolon:researches` - Researching [WIP]
- `eidolon:warped_sprouts` - Warped Sprouts

### Entry Types

- `"text"` - Text-only pages
- `"recipe"` - Crafting recipes
- `"ritual"` - Ritual instructions
- `"entity"` - Creature information
- `"crafting"` - General crafting information
- `"smelting"` - Furnace recipes
- `"crucible"` - Crucible recipes
- `"workbench"` - Magic workbench recipes
- `"list"` - Lists of items/entities

### Page Types

#### Text Page
```json
{
  "type": "text",
  "title": "Page Title",
  "content": "Your text content here. Can include multiple paragraphs."
}
```

#### Ritual Page
```json
{
  "type": "ritual",
  "title": "Ritual Name",
  "ritual_id": "yourmod:ritual_name",
  "pedestals": [
    {
      "item": "eidolon:shadow_gem",
      "count": 2
    }
  ],
  "center_item": "eidolon:unholy_symbol",
  "description": "Instructions for performing the ritual."
}
```

#### Crafting Page
```json
{
  "type": "crafting",
  "title": "Item Name",
  "recipe": "yourmod:recipe_name",
  "description": "Description of the crafted item and its uses."
}
```

#### List Page
```json
{
  "type": "list",
  "title": "List Title",
  "entries": [
    {
      "item": "yourmod:item1",
      "name": "Item Display Name",
      "description": "Description of this item."
    }
  ]
}
```

## Example Use Cases

1. **Adding a new ritual to the Rituals chapter:**
   - `target_chapter: "eidolon:rituals"`
   - `type: "ritual"`

2. **Adding new plants to the Rare Flora chapter:**
   - `target_chapter: "eidolon:plants"`
   - `type: "list"`

3. **Adding new tools made from Arcane Gold:**
   - `target_chapter: "eidolon:arcane_gold"`
   - `type: "crafting"`

4. **Adding new monsters:**
   - `target_chapter: "eidolon:monsters"`
   - `type: "entity"`

## Implementation Status

✅ **Currently Implemented:**
- JSON data loading system
- Chapter extension framework
- Basic validation

🚧 **In Progress:**
- Full JSON to CodexEntry parsing
- Integration with Eidolon's codex system
- Page rendering

📋 **Planned:**
- Custom chapter creation (after extending existing ones works)
- Advanced unlock criteria
- Custom page types
- GUI integration

## Testing

Once implemented, your entries will:
1. Load automatically on server start
2. Appear in the targeted Eidolon chapter
3. Respect prerequisite requirements
4. Display properly in the codex GUI

Check the server logs for loading confirmation and any errors.
