# Eidolon Unchained - Visual Elements Reference

## Page Type Visual Breakdown

### Title Page Visual Elements
```
┌─────────────────────────────────────┐
│              [ICON]                 │
│                                     │
│         MAIN TITLE TEXT             │
│           Subtitle Text             │
│                                     │
│                                     │
│                                     │
└─────────────────────────────────────┘
```

### Text Page Visual Elements
```
┌─────────────────────────────────────┐
│ [ICON] Page Title              [IMG]│
│                                     │
│ Line 1 of content text              │
│ Line 2 with §dformatting§r          │
│                                     │
│ • Bullet point 1               [IMG]│
│ • Bullet point 2                    │
│                                     │
│ More content here...                │
└─────────────────────────────────────┘
```

### Entity Page Visual Elements
```
┌─────────────────────────────────────┐
│ [ICON] Entity Name                  │
│                                     │
│ ┌─────────┐ Description text        │
│ │ 3D      │ goes here with          │
│ │ Entity  │ multiple lines          │
│ │ Model   │                         │
│ │(Rotate) │ Stats and abilities     │
│ └─────────┘ listed below            │
│                                     │
│ Drop Items: [ITEM] [ITEM] [ITEM]    │
└─────────────────────────────────────┘
```

### List Page Visual Elements
```
┌─────────────────────────────────────┐
│ [ICON] List Title                   │
│                                     │
│ [ITEM] x3  Item Name                │
│            Description line 1       │
│            Description line 2       │
│                                     │
│ [ITEM] x8  Another Item             │
│            Its description          │
│                                     │
│ Footer information here...          │
└─────────────────────────────────────┘
```

### Ritual Page Visual Elements
```
┌─────────────────────────────────────┐
│ [ICON] Ritual Name        Difficulty│
│                                     │
│    [ITEM]     Ritual Circle         │
│ [I] [CTR] [I] Top-down view         │
│    [ITEM]     showing placement     │
│                                     │
│ Ingredients: [ITEM] [ITEM] [ITEM]   │
│                                     │
│ Steps: 1. First step                │
│        2. Second step               │
└─────────────────────────────────────┘
```

## Color Code Visual Reference

### Text Colors
- `§0` Black: §0This is black text§r
- `§1` Dark Blue: §1This is dark blue text§r
- `§2` Dark Green: §2This is dark green text§r
- `§3` Dark Aqua: §3This is dark aqua text§r
- `§4` Dark Red: §4This is dark red text§r
- `§5` Dark Purple: §5This is dark purple text§r
- `§6` Gold: §6This is gold text§r
- `§7` Gray: §7This is gray text§r
- `§8` Dark Gray: §8This is dark gray text§r
- `§9` Blue: §9This is blue text§r
- `§a` Green: §aThis is green text§r
- `§b` Aqua: §bThis is aqua text§r
- `§c` Red: §cThis is red text§r
- `§d` Light Purple: §dThis is light purple text§r
- `§e` Yellow: §eThis is yellow text§r
- `§f` White: §fThis is white text§r

### Style Effects
- `§l` **Bold**: §lThis is bold text§r
- `§o` *Italic*: §oThis is italic text§r
- `§n` Underlined: §nThis is underlined text§r
- `§m` ~~Strikethrough~~: §mThis is strikethrough text§r

### Common Combinations
- `§l§4` **Dark Red**: §l§4WARNING TEXT§r
- `§l§a` **Green**: §l§aIMPORTANT INFO§r
- `§o§7` *Gray Italic*: §o§7Flavor text§r
- `§n§9` Blue Underlined: §n§9Clickable link style§r

## Item Icon Reference

### Eidolon Items (Common)
- `eidolon:soul_gem` - Primary magical focus
- `eidolon:wraith_heart` - Undead essence core
- `eidolon:silver_ingot` - Purified metal
- `eidolon:unholy_symbol` - Dark magic symbol
- `eidolon:brazier` - Ritual brazier
- `eidolon:soul_shard` - Soul fragment
- `eidolon:pewter_ingot` - Base metal
- `eidolon:arcane_gold_ingot` - Magical gold
- `eidolon:zombie_heart` - Undead component
- `eidolon:tattered_cloth` - Spectral fabric

### Minecraft Items (Useful)
- `minecraft:book` - Knowledge/information
- `minecraft:chest` - Storage/materials
- `minecraft:cauldron` - Brewing/mixing
- `minecraft:bone` - Undead components
- `minecraft:skull` - Dark magic
- `minecraft:nether_star` - Powerful magic
- `minecraft:ghast_tear` - Ethereal essence
- `minecraft:blaze_rod` - Fire magic
- `minecraft:ender_pearl` - Teleportation
- `minecraft:diamond` - Valuable/rare

## Layout Positioning

### Side Images
Side images can be positioned using x/y coordinates:
```json
"side_images": [
  {
    "item": "eidolon:soul_gem",
    "x": 120,  // Right side of page
    "y": 40    // Middle height
  },
  {
    "item": "minecraft:book",
    "x": 10,   // Left side
    "y": 80    // Lower position
  }
]
```

### Ritual Circle Positions
For ritual circles, use cardinal and ordinal directions:
- `"north"`, `"south"`, `"east"`, `"west"` - Cardinal directions
- `"northeast"`, `"northwest"`, `"southeast"`, `"southwest"` - Diagonal positions
- `"center"` - Central position

## Best Practices for Visual Design

### Icon Selection
1. **Relevance**: Choose icons that relate to the content
   - Creatures → `eidolon:wraith_heart`
   - Rituals → `eidolon:unholy_symbol`
   - Materials → `minecraft:chest`
   - Magic → `eidolon:soul_gem`

2. **Visibility**: Ensure icons are recognizable
   - Avoid very dark or very bright items
   - Test in-game to verify clarity
   - Use contrasting colors against page background

3. **Consistency**: Use similar icon types for related content
   - All monster pages use creature-related icons
   - All ritual pages use magical symbols
   - All material lists use container icons

### Color Usage
1. **Hierarchy**: Use colors to show importance
   - `§l§4` Red bold for warnings/dangers
   - `§l§a` Green bold for important tips
   - `§7` Gray for secondary information
   - `§d` Purple for magical elements

2. **Readability**: Ensure text remains readable
   - Avoid very dark colors on dark backgrounds
   - Use `§r` to reset formatting when changing contexts
   - Test color combinations for accessibility

3. **Thematic Consistency**: Match Eidolon's aesthetic
   - Purples and grays for magical content
   - Reds for warnings and danger
   - Greens for positive outcomes
   - Golds for valuable information

### Content Organization
1. **Page Flow**: Organize content logically
   - Title page → Overview → Details → Instructions
   - General information before specific procedures
   - Warnings before dangerous content

2. **Information Density**: Don't overcrowd pages
   - Use multiple pages for complex topics
   - Break up text with images and spacing
   - Group related information together

3. **Visual Breaks**: Use spacing and formatting
   - Empty strings ("") create line breaks
   - Different page types for different content types
   - Icons and images to break up text blocks

## Example Implementations

### Perfectly Formatted Warning Section
```json
{
  "type": "text",
  "title": "Safety Protocols",
  "content": [
    "§l§4⚠ CRITICAL SAFETY INFORMATION ⚠§r",
    "",
    "Before attempting any summoning ritual, ensure you have:",
    "",
    "§a✓ Proper protective wards active§r",
    "§a✓ Emergency banishment materials ready§r",
    "§a✓ At least one experienced practitioner present§r",
    "",
    "§l§cDANGER LEVELS:§r",
    "§7• §2Low Risk:§7 Basic soul work§r",
    "§7• §6Medium Risk:§7 Wraith summoning§r",
    "§7• §cHigh Risk:§7 Demon binding§r",
    "§7• §4EXTREME RISK:§7 Void manipulation§r",
    "",
    "§o§7Remember: Your safety and that of others depends on",
    "following these protocols exactly.§r"
  ],
  "icon": "minecraft:barrier"
}
```

### Rich Entity Description
```json
{
  "type": "entity",
  "title": "§dSpectral Wraith§r",
  "entity": "eidolon:wraith",
  "description": [
    "§dSpectral Wraiths§r are among the most dangerous undead entities,",
    "existing partially outside normal reality.",
    "",
    "§l§5Physical Characteristics:§r",
    "§7• Translucent, shifting form§r",
    "§7• Glowing purple essence core§r",
    "§7• Leaves frost trail when moving§r",
    "",
    "§l§cCombat Abilities:§r",
    "§7• §dSpectral Drain:§7 Steals life force through walls§r",
    "§7• §bPhase Walk:§7 Moves through solid matter§r",
    "§7• §8Shadow Cloak:§7 Becomes invisible when still§r",
    "",
    "§l§6Behavioral Notes:§r",
    "§7• Attracted to soul energy and negative emotions§r",
    "§7• Becomes more aggressive in groups§r",
    "§7• Vulnerable to silver weapons and holy symbols§r"
  ]
}
```

This visual reference guide provides complete information about how each element appears in-game and how to use them effectively for creating professional-looking codex entries.
