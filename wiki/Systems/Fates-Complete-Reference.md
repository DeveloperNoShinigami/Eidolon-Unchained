# Fates System - Complete Field Reference

The Fates system is the task/quest system for Eidolon Unchained, where deities assign tasks to players to earn reputation and rewards. This document provides comprehensive coverage of all fields and their functionality.

## Overview

Fates are JSON-defined tasks stored in `data/modid/fates/deity_name/` directories. Each deity can have multiple fate files that define progression chains and standalone tasks.

## File Structure

```
data/eidolonunchained/fates/
├── fire_deity/
│   ├── forge_basics.json
│   ├── forge_mastery.json
│   ├── hellfire_trials.json
│   └── lord_of_flames.json
├── nature_deity/
│   ├── sacred_gathering.json
│   ├── grove_tender.json
│   ├── ritual_master.json
│   └── life_champion.json
└── [other deities...]
```

## Complete JSON Schema

### Root Level Fields

```json
{
  "task_id": "string",              // REQUIRED: Unique identifier for this fate
  "display_name": "string",         // REQUIRED: Human-readable name shown to players
  "description": "string",          // REQUIRED: Lore description of the fate
  "progression_tier": "string",     // REQUIRED: Difficulty tier (initiate/acolyte/master/champion)
  "ai_assignment_context": {},      // REQUIRED: AI assignment rules and prompts
  "requirements": [],               // REQUIRED: Array of requirement objects
  "rewards": {},                    // REQUIRED: Reputation and command rewards
  "cooldown_hours": number,         // REQUIRED: Hours between completions
  "repeatable": boolean             // REQUIRED: Whether fate can be repeated
}
```

## AI Assignment Context

Controls when and how the AI deity assigns this fate to players.

```json
"ai_assignment_context": {
  "trigger_conditions": {
    "min_reputation": 0,                    // Minimum reputation required
    "max_reputation": 30,                   // Maximum reputation (999 = no limit)
    "required_biomes": [],                  // Player must be in these biomes
    "prayer_types": ["conversation"],       // Prayer types that can trigger assignment
    "completed_tasks": [],                  // Tasks that must be completed first
    "unlocked_progressions": [],            // Progressions that must be unlocked
    "required_research": [],                // Eidolon research entries required
    "required_items": [],                   // Items player must possess
    "forbidden_tasks": [],                  // Tasks that prevent assignment
    "time_conditions": [],                  // Time-based requirements
    "dimension_requirements": [],           // Dimension requirements
    "weather_conditions": []                // Weather requirements
  },
  "assignment_prompt": "string",            // Prompt shown when AI assigns fate
  "completion_phrases": [                   // Random phrases when completing fate
    "Phrase 1",
    "Phrase 2"
  ]
}
```

### Trigger Conditions Details

#### Basic Conditions
- **`min_reputation`**: Minimum reputation points with this deity
- **`max_reputation`**: Maximum reputation (use 999 for no upper limit)  
- **`prayer_types`**: Array of prayer types that can trigger assignment
  - Valid values: `"conversation"`, `"blessing"`, `"guidance"`, `"trial"`, `"communion"`
  - Deity-specific types like `"fire"`, `"nature"`, `"growth"` also work

#### Progression Conditions  
- **`completed_tasks`**: Array of `task_id`s that must be completed before assignment
- **`unlocked_progressions`**: Array of progression keys that must be unlocked
- **`required_research`**: Array of Eidolon research entry IDs

#### Environmental Conditions
- **`required_biomes`**: Player must be in one of these biomes
  - Examples: `"minecraft:forest"`, `"minecraft:nether_wastes"`
- **`dimension_requirements`**: Player must be in specific dimensions  
  - Examples: `"minecraft:the_nether"`, `"minecraft:the_end"`
- **`weather_conditions`**: Weather requirements
  - Values: `"clear"`, `"rain"`, `"thunder"`

#### Time Conditions
- **`time_conditions`**: Time-based requirements
  - Values: `"day"`, `"night"`, `"dawn"`, `"dusk"`

#### Item Conditions
- **`required_items`**: Items player must have in inventory
```json
"required_items": [
  {
    "item": "minecraft:diamond_sword",
    "count": 1,
    "nbt_required": false
  }
]
```

#### Exclusion Conditions
- **`forbidden_tasks`**: Tasks that prevent assignment if completed
- **`forbidden_progressions`**: Progressions that block assignment

## Requirements System

Requirements define what players must do to complete the fate. Multiple requirement types are supported.

### Requirement Object Structure
```json
{
  "type": "requirement_type",
  "count": 1,
  "display_name": "Human Readable Name",
  // type-specific fields...
}
```

### Supported Requirement Types

#### 1. collect_items
Collect specific items in inventory.
```json
{
  "type": "collect_items",
  "items": [
    {
      "item": "minecraft:wheat",
      "count": 16,
      "display_name": "Sacred Herbs"
    }
  ]
}
```

#### 2. kill_entities  
Kill specific entities.
```json
{
  "type": "kill_entities", 
  "entities": [
    {
      "entity": "minecraft:blaze",
      "count": 8,
      "display_name": "Blazes Defeated"
    }
  ]
}
```

#### 3. mine_blocks
Mine specific blocks.
```json
{
  "type": "mine_blocks",
  "blocks": [
    {
      "block": "minecraft:iron_ore", 
      "count": 16,
      "tool_required": "pickaxe",
      "display_name": "Iron Ore Mined"
    }
  ]
}
```

#### 4. place_blocks
Place blocks in the world.
```json
{
  "type": "place_blocks",
  "blocks": [
    {
      "block": "minecraft:oak_sapling",
      "count": 8,
      "area_requirement": "8x8",
      "display_name": "Sacred Grove Saplings"
    }
  ]
}
```

#### 5. use_items
Use items (right-click, consume, etc).
```json
{
  "type": "use_items",
  "items": [
    {
      "item": "minecraft:flint_and_steel",
      "count": 50,
      "display_name": "Fire Started"
    }
  ]
}
```

#### 6. visit_biomes
Visit specific biomes.
```json
{
  "type": "visit_biomes", 
  "biomes": [
    "minecraft:forest",
    "minecraft:flower_forest",
    "minecraft:plains"
  ],
  "count": 3,
  "display_name": "Sacred Biomes Visited"
}
```

#### 7. visit_structures
Visit or discover structures.
```json
{
  "type": "visit_structures",
  "structures": [
    "minecraft:woodland_mansion",
    "minecraft:ocean_ruins"  
  ],
  "count": 2,
  "display_name": "Ancient Structures Found"
}
```

#### 8. complete_ritual
Complete Eidolon rituals.
```json
{
  "type": "complete_ritual",
  "rituals": [
    {
      "ritual_id": "eidolonunchained:nature_patronage_ritual",
      "count": 1,
      "success_required": true
    }
  ]
}
```

#### 9. complete_crucible_recipe
Complete Eidolon crucible recipes.
```json
{
  "type": "complete_crucible_recipe",
  "recipes": [
    {
      "recipe_id": "eidolonunchained:nature_soul_gem",
      "count": 1,
      "success_required": true
    }
  ]
}
```

#### 10. dimension_visit
Visit specific dimensions.
```json
{
  "type": "dimension_visit",
  "dimension": "minecraft:the_nether",
  "duration_minutes": 10,
  "display_name": "Nether Pilgrimage"
}
```

#### 11. time_requirement
Wait for specific time conditions.
```json
{
  "type": "time_requirement", 
  "condition": "night",
  "duration_minutes": 5,
  "display_name": "Nighttime Meditation"
}
```

#### 12. location_requirement
Be at specific coordinates or heights.
```json
{
  "type": "location_requirement",
  "y_min": 200,
  "y_max": 320,
  "display_name": "Sky High Location"
}
```

### String-Based Requirements (Legacy)

Simple requirements can be specified as strings for backwards compatibility:

```json
"requirements": [
  "collect_items:minecraft:wheat:16",
  "kill_entities:minecraft:zombie:10", 
  "mine_blocks:minecraft:stone:32",
  "dimension:minecraft:the_nether",
  "time:night",
  "location:y>200"
]
```

## Rewards System

Defines what players receive for completing fates.

```json
"rewards": {
  "reputation": 15,                     // REQUIRED: Reputation points awarded
  "commands": [                         // REQUIRED: Commands to execute
    "give {player} minecraft:diamond 1",
    "effect give {player} minecraft:strength 600 1",
    "tellraw {player} {\"text\":\"Congratulations!\",\"color\":\"green\"}"
  ],
  "progression_unlock": "deity_step1"   // OPTIONAL: Unlocks progression for next fate
}
```

### Command Variables

Available variables in reward commands:
- **`{player}`**: Player's username
- **`{deity}`**: Deity ID (e.g., "eidolonunchained:fire_deity")
- **`{reputation}`**: Player's current reputation with deity
- **`{progression_title}`**: Player's progression title

### Progression Unlocks

The `progression_unlock` field unlocks a progression key that subsequent fates can require in their `unlocked_progressions` array. This creates progression chains.

**Example Chain:**
1. Task A completes → unlocks `"deity_step1"`
2. Task B requires `"deity_step1"` → unlocks `"deity_step2"`  
3. Task C requires `"deity_step2"` → unlocks `"deity_step3"`

## Field Validation & Error Handling

### Required Fields
All fates must include:
- `task_id`, `display_name`, `description`, `progression_tier`
- `ai_assignment_context` with `trigger_conditions` 
- `requirements` array (can be empty)
- `rewards` object with `reputation` and `commands`
- `cooldown_hours`, `repeatable`

### Field Constraints
- **`task_id`**: Must be unique within deity, alphanumeric + underscores
- **`cooldown_hours`**: Positive integer, typical range 12-168 hours
- **`reputation`**: Positive integer, typical range 5-25 points
- **`min_reputation`/`max_reputation`**: 0-999 range
- **`repeatable`**: Boolean, most progression fates should be false

### Error Behaviors
- Missing required fields: Fate won't load, error logged
- Invalid requirement types: Requirement ignored, warning logged  
- Invalid command syntax: Command skipped, error logged
- Circular progression dependencies: Chain broken, error logged

## Best Practices

### Progression Design
1. **Start Simple**: First fate needs no requirements
2. **Clear Progression**: Each tier should build on the previous
3. **Balanced Rewards**: Scale reputation (5→10→15→25) and cooldowns (12→24→48→72h)
4. **Thematic Consistency**: Match deity personality and domain

### Requirement Design
1. **Mix Complexity**: Combine simple and complex requirements
2. **Clear Goals**: Use descriptive `display_name` fields
3. **Achievable Counts**: Don't make grindy requirements  
4. **Environmental Context**: Use biome/dimension requirements for flavor

### AI Integration
1. **Appropriate Triggers**: Match `prayer_types` to fate theme
2. **Engaging Prompts**: Write immersive `assignment_prompt` text
3. **Varied Completion**: Provide multiple `completion_phrases`
4. **Logical Gating**: Use reputation and progression requirements appropriately

## Command Integration

### Manual Assignment
```bash
/eidolon-unchained fates assign <player> <deity> <fateId>
```

### Check Progress  
```bash
/eidolon-unchained fates list <player>
```

### Complete Fate
```bash  
/eidolon-unchained fates complete <player> <fateId>
```

### Reputation Check
```bash
/eidolon-unchained fates reputation <player> <deity>
```

## File Examples

See the `data/eidolonunchained/fates/` directory for complete examples of each deity's progression chains.