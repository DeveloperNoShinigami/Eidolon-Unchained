# Datapack Chant System - Complete Guide

## Overview

The datapack chant system allows you to create custom spells that integrate seamlessly with Eidolon's existing codex and spell system. Chants appear as proper entries in the codex with custom categories, proper sign sequences, and full effect support including command execution.

## How Chants Appear In-Game

### Codex Integration

Your chants appear exactly like Eidolon's built-in spells in the codex:

1. **Custom Categories**: Each `category` field creates a new tab in the codex
   - Example: `"category": "examples"` creates an "Examples" tab
   - Categories get custom icons and colors based on their name

2. **Chapter Entries**: Each chant becomes a chapter in its category
   - Shows the chant name as the chapter title
   - Displays difficulty as star rating (1-5 stars)
   - Shows requirements (reputation, items, etc.)

3. **Chant Pages**: Each chapter contains:
   - **Title Page**: Chant name and introduction
   - **Chant Page**: Visual sign sequence (like Eidolon's built-in spells)
   - **Description Page**: Detailed explanation of effects

### Example Categories

```
📚 Codex Categories:
├── Examples (custom tab with book icon)
│   ├── Simple Heal ⭐
│   ├── Shadow Communion ⭐⭐⭐  
│   └── Divine Blessing ⭐⭐⭐⭐
├── Nature Chants (if you use "nature" category)
├── Combat Spells (if you use "combat" category)
└── Healing Arts (if you use "healing" category)
```

## JSON Format Specification

### Complete Chant Structure

```json
{
  "name": "Display Name",
  "description": "Detailed description of what this chant does",
  "category": "examples",
  "difficulty": 3,
  "show_in_codex": true,
  "signs": [
    "eidolon:sacred",
    "eidolon:wicked",
    "eidolon:harmony"
  ],
  "requirements": [
    "reputation:eidolonunchained:light_deity:25",
    "item:minecraft:diamond:1"
  ],
  "effects": [
    {
      "type": "apply_effect",
      "effect": "minecraft:strength",
      "duration": 1200,
      "amplifier": 2
    },
    {
      "type": "run_command",
      "command": "give @s minecraft:golden_apple 1"
    },
    {
      "type": "send_message",
      "message": "§6Divine power flows through you!"
    }
  ]
}
```

### Field Explanations

- **`name`**: Display name in codex and game
- **`description`**: Lore text shown on description page
- **`category`**: Creates/assigns to codex category tab
- **`difficulty`**: 1-5, shown as star rating in codex
- **`show_in_codex`**: Whether to appear in codex (true/false)
- **`signs`**: Array of Eidolon sign IDs for the chant sequence
- **`requirements`**: Array of unlock conditions
- **`effects`**: Array of actions when chant is cast

## Effect Types

### 1. Apply Potion Effects
```json
{
  "type": "apply_effect",
  "effect": "minecraft:regeneration",
  "duration": 600,
  "amplifier": 1
}
```

### 2. Execute Commands
```json
{
  "type": "run_command",
  "command": "give @s minecraft:diamond 3"
}
```
**Command Features:**
- Full Minecraft command support
- `@s` automatically targets the caster
- Supports particles, sounds, teleportation, etc.
- Examples:
  - `"command": "playsound minecraft:entity.experience_orb.pickup player @s"`
  - `"command": "particle minecraft:end_rod ~ ~1 ~ 1.0 1.0 1.0 0.1 50"`
  - `"command": "tp @s ~ ~10 ~"`

### 3. Send Messages
```json
{
  "type": "send_message", 
  "message": "§aHealing energies surround you!"
}
```

### 4. Start AI Deity Conversations
```json
{
  "type": "start_conversation",
  "deity": "eidolonunchained:nature_deity",
  "prayer_type": "conversation"
}
```

### 5. Give Items (TODO - Not yet implemented)
```json
{
  "type": "give_item",
  "item": "minecraft:golden_apple",
  "count": 2
}
```

## Available Eidolon Signs

Use these exact IDs in your `signs` array:

```
eidolon:wicked      - Dark magic, curses
eidolon:sacred      - Holy magic, blessings  
eidolon:blood       - Blood magic, sacrifice
eidolon:soul        - Soul manipulation
eidolon:mind        - Mental effects
eidolon:flame       - Fire magic
eidolon:winter      - Ice/cold magic
eidolon:harmony     - Nature, balance
eidolon:death       - Necromancy
eidolon:warding     - Protection
eidolon:magic       - General arcane
```

## Requirements System

Control who can learn/cast chants:

### Reputation Requirements
```json
"requirements": ["reputation:eidolonunchained:dark_deity:25"]
```

### Item Requirements (Planned)
```json
"requirements": ["item:minecraft:diamond:1"]
```

### Research Requirements (Planned)
```json
"requirements": ["research:eidolon:some_research"]
```

## Category Customization

Categories automatically get themed:

| Category Name | Icon | Color | Theme |
|---------------|------|-------|-------|
| `basic` | Book | Gray | Simple spells |
| `dark` | Obsidian | Purple | Shadow magic |
| `light` | Beacon | Gold | Holy magic |
| `nature` | Grass Block | Green | Natural magic |
| `healing` | Potion | Pink | Restoration |
| `combat` | Sword | Red | Battle magic |
| `examples` | Knowledge Book | Blue | Demonstrations |

### Custom Categories

Create any category name and it gets:
- Default enchanted book icon
- Gray color scheme  
- Proper codex integration

## File Locations

Place chant JSON files in:
```
data/
├── your_modpack_id/
│   └── chants/
│       ├── simple_heal.json
│       ├── shadow_communion.json  
│       ├── divine_blessing.json
│       └── your_custom_chant.json
└── eidolonunchained/
    └── chants/
        └── (example chants included)
```

## Configuration

Control the system via `config/eidolonunchained-common.toml`:

```toml
[chant_system]
    # Enable the datapack chant system
    enableDatapackChants = true
    
    # Show debug information for chant loading
    enableDebugMode = false
    
    # Allow chants to execute commands
    allowChantCommands = true
```

## How the Casting System Works

1. **Player performs chant sequence** at ritual brazier/altar
2. **Game validates sign sequence** against loaded chant recipes
3. **Requirements are checked** (reputation, items, etc.)
4. **Effects are executed** in order:
   - Potion effects applied instantly
   - Commands executed with player as source
   - Messages sent to player
   - AI conversations started if applicable

## Integration with Existing Systems

### AI Deity Integration
- Use `start_conversation` effect to trigger AI deity chats
- Chants can be required reputation gates for deity access
- AI responds based on which chant was used to contact them

### Eidolon Spell System
- Chants use Eidolon's existing sign system
- Show in codex alongside built-in spells
- Follow same casting mechanics as Eidolon prayers

### Command System
- Full access to all Minecraft commands
- Server operator permission level (level 2)
- Safe execution with error handling
- Player context automatically provided

## Example Use Cases

### Simple Utility Spell
```json
{
  "name": "Light",
  "category": "basic",
  "difficulty": 1,
  "signs": ["eidolon:flame"],
  "effects": [
    {"type": "run_command", "command": "give @s minecraft:torch 4"},
    {"type": "send_message", "message": "§eTorches materialize in your hands."}
  ]
}
```

### Complex Ritual
```json
{
  "name": "Summon Storm",
  "category": "nature", 
  "difficulty": 5,
  "signs": ["eidolon:harmony", "eidolon:winter", "eidolon:flame", "eidolon:harmony"],
  "requirements": ["reputation:eidolonunchained:nature_deity:50"],
  "effects": [
    {"type": "run_command", "command": "weather thunder 6000"},
    {"type": "run_command", "command": "particle minecraft:cloud ~ ~5 ~ 10 2 10 0.1 100"},
    {"type": "apply_effect", "effect": "minecraft:resistance", "duration": 1200, "amplifier": 1},
    {"type": "send_message", "message": "§8⚡ The sky answers your call! ⚡"}
  ]
}
```

### Deity Communication
```json
{
  "name": "Speak with Shadows",
  "category": "dark",
  "difficulty": 3,
  "signs": ["eidolon:wicked", "eidolon:wicked", "eidolon:sacred"],
  "requirements": ["reputation:eidolonunchained:dark_deity:10"],
  "effects": [
    {"type": "start_conversation", "deity": "eidolonunchained:dark_deity", "prayer_type": "conversation"},
    {"type": "apply_effect", "effect": "minecraft:night_vision", "duration": 300},
    {"type": "run_command", "command": "particle minecraft:soul_fire_flame ~ ~1 ~ 0.5 0.5 0.5 0.1 20"}
  ]
}
```

## Commands for Testing

Once implemented, use these commands to manage chants:

```bash
# Reload all datapacks (including chants)
/reload

# Check chant system status  
/eidolon-unchained status

# List loaded chants
/eidolon-unchained list-chants

# Test a specific chant
/eidolon-unchained test-chant simple_heal

# Validate all chant JSON files
/eidolon-unchained validate-chants
```

This system provides a powerful, flexible way to create custom spells that feel native to Eidolon while supporting modern Minecraft features like commands and complex effects.
