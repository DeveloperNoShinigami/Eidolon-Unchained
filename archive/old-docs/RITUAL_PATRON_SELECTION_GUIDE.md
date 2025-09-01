# Ritual-Based Patron Selection System

## Overview

The Eidolon Unchained mod now supports ritual-based patron selection, allowing players to choose their patron deity through sacred rituals rather than just commands. This system integrates with the existing AI deity configuration and provides a more immersive D&D-style experience.

## System Components

### 1. Ritual Integration Configuration (JSON)

Each AI deity can define patron selection rituals in their JSON configuration:

```json
{
  "ritual_integration": {
    "patron_selection_ritual": {
      "enabled": true,
      "ritual_id": "eidolonunchained:nature_patronage",
      "completion_commands": [
        "execute as @p[distance=..10] run eidolon-unchained patron choose eidolonunchained:nature_deity",
        "tellraw @p {\"text\":\"§2Nature's blessing flows through you!\",\"color\":\"green\"}",
        "playsound minecraft:block.enchantment_table.use player @p ~ ~ ~ 1.0 1.2",
        "particle minecraft:happy_villager ~ ~1 ~ 2 2 2 0.1 50 force"
      ],
      "failure_commands": [
        "tellraw @p {\"text\":\"§cThe deity rejects your offering.\",\"color\":\"red\"}",
        "playsound minecraft:block.fire.extinguish player @p ~ ~ ~ 1.0 0.8"
      ],
      "requirements": {
        "min_reputation": 25,
        "cooldown_hours": 24,
        "required_items": ["minecraft:oak_sapling", "minecraft:wheat_seeds"],
        "forbidden_patrons": ["eidolonunchained:shadow_deity"]
      }
    }
  }
}
```

### 2. Ritual Recipe Definition

The actual ritual components are defined in datapack ritual recipes:

```json
{
  "type": "eidolon:command_ritual",
  "ritual": "eidolonunchained:nature_patronage",
  "reagent": { "item": "eidolon:soul_shard" },
  "pedestals": [
    { "item": "minecraft:oak_sapling" },
    { "item": "minecraft:wheat_seeds" },
    { "item": "minecraft:bone_meal" },
    { "item": "eidolon:arcane_gold_ingot" }
  ],
  "focusItems": [{ "item": "eidolon:codex" }],
  "commands": [
    "eidolon-unchained debug fire-ritual-completion eidolonunchained:nature_patronage"
  ]
}
```

### 3. Event Handler (RitualPatronHandler)

The `RitualPatronHandler` class automatically:
- Detects when patron selection rituals are completed
- Validates requirements (reputation, cooldowns, forbidden patrons)
- Executes the patron selection using `PatronSystem.choosePatron()`
- Runs success/failure commands from the AI configuration

## How It Works

### Step 1: Player Preparation
1. Player gains sufficient reputation (25+) with the desired deity
2. Player gathers required ritual components
3. Player ensures they don't have conflicting patron allegiances

### Step 2: Ritual Setup
1. Player places the Brazier and focuses
2. Player arranges pedestals with required items
3. Player places the reagent (Soul Shard) in the brazier

### Step 3: Ritual Execution
1. Player activates the ritual through normal Eidolon mechanics
2. The ritual executes and fires completion events
3. `RitualPatronHandler` intercepts the event and validates requirements
4. If successful, `PatronSystem.choosePatron()` is called
5. Success/failure commands are executed (particles, sounds, messages)

### Step 4: Patron Selection
1. Player's patron is officially set in the capability system
2. AI deity filtering begins working immediately
3. Player receives confirmation and any immediate benefits

## Key Features

### Datapack-Driven Configuration
- Everything important is defined in JSON configuration files
- No code changes needed to add new patron rituals
- Modpack creators can customize requirements and effects

### Requirement Validation
- **Minimum Reputation**: Players must earn the deity's favor first
- **Cooldown System**: Prevents rapid patron switching
- **Forbidden Patrons**: Some deities won't accept followers of enemies
- **Item Requirements**: Ritual components prove dedication

### Rich Feedback System
- Custom success messages and effects per deity
- Failure explanations help players understand requirements
- Sound effects and particle systems enhance immersion

### Integration with Existing Systems
- Uses the existing `PatronSystem.choosePatron()` method
- Respects all patron switching rules and penalties
- Works with the AI allegiance filtering system

## Testing and Debugging

### Manual Testing Commands
```bash
# Fire a ritual completion event for testing
/eidolon-unchained debug fire-ritual-completion eidolonunchained:nature_patronage

# Check patron status
/eidolon-unchained patron status

# Choose patron directly (for comparison)
/eidolon-unchained patron choose eidolonunchained:nature_deity
```

### Configuration Validation
- AI deity configurations are validated on load
- Ritual recipes are checked for proper command integration
- Debug logs track ritual completion and requirement checking

## Example Patron Ritual Configurations

### Nature Deity (Verdania)
- **Requirements**: 25+ reputation, nature-related items
- **Forbidden**: Shadow/corruption deities
- **Effects**: Regeneration, nature sounds, happy particles

### Shadow Deity (Umbraxis)
- **Requirements**: 50+ reputation, dark items, nighttime only
- **Forbidden**: Light/nature deities
- **Effects**: Darkness effects, ominous sounds, smoke particles

### War Deity (Ironbane)
- **Requirements**: 75+ reputation, weapons/armor, combat achievements
- **Forbidden**: Peace/healing deities
- **Effects**: Strength buffs, battle sounds, fire particles

## Benefits of This System

### For Players
- More immersive patron selection process
- Clear requirements and progression goals
- Rich feedback and ceremonial experience
- Integration with existing Eidolon ritual mechanics

### For Server Administrators
- Fully configurable through datapacks
- No need for custom plugins or mods
- Balanced progression requirements
- Prevents easy patron switching abuse

### for Modpack Creators
- Easy to customize patron requirements
- Can integrate with other mod content
- Supports complex requirement chains
- Enables unique deity personalities and restrictions

## Future Expansions

### Planned Features
- **Multi-stage Rituals**: Complex patron selection requiring multiple steps
- **Faction Integration**: Guild/faction requirements for certain deities
- **Seasonal Restrictions**: Some deities only accept followers during specific times
- **Collective Rituals**: Group ceremonies for powerful patron selections

### Integration Opportunities
- **Quest Mod Integration**: Patron selection as quest rewards
- **Reputation System**: More complex reputation requirements
- **World Events**: Special patron selection during server events
- **Custom Titles**: Unique titles based on ritual completion method

This system provides a comprehensive, datapack-driven approach to patron selection that enhances the role-playing experience while maintaining full customization flexibility.
