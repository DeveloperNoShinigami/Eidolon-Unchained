# Research System Overview

**Auto-discovery and progression mechanics for your content**

## What the Research System Does

The research system allows players to discover content automatically based on in-game actions. When triggered, research can:
- Grant mystical signs for Eidolon progression  
- Give items as rewards
- Unlock new codex entries
- Require specific tasks to complete

## Currently Implemented Features

Based on `FEATURES.md`, here's what actually works:

### ✅ Discovery Triggers
- **Kill Triggers** - Kill specific entities to discover research
- **Interaction Triggers** - Right-click entities/blocks with note-taking tools  
- **Location Triggers** - Auto-discovery when entering biomes/dimensions

### ✅ Research Conditions  
- **Dimension Condition** - Research only available in specific dimensions
- **Inventory Condition** - Check for items in inventory
- **Time Condition** - Time-based restrictions
- **Weather Condition** - Weather-based restrictions

### ✅ Task Types
- **Item Tasks** - Collect/consume specific items
- **XP Tasks** - Spend experience levels  
- **NBT Support** - Both item tasks and rewards support NBT data

### ✅ Reward Types
- **Sign Rewards** - Grant mystical signs (FLAME, DEATH, SOUL, etc.)
- **Item Rewards** - Give items to player
- **Action Bar Notifications** - Consistent notification system

## Basic Research Structure

```json
{
  "id": "research_name",
  "stars": 3,
  "triggers": ["entity:minecraft:zombie"],
  "conditions": [
    {"type": "dimension", "dimension": "minecraft:overworld"}
  ],
  "tasks": {
    "1": [{"type": "item", "item": "minecraft:rotten_flesh", "count": 5}],
    "2": [{"type": "xp", "levels": 3}]
  },
  "rewards": [
    {"type": "sign", "sign": "death"},
    {"type": "item", "item": "minecraft:diamond", "count": 1}
  ]
}
```

## File Location

Research files go in:
```
data/your_mod/eidolon_research/your_research.json
```

This matches the actual file structure used by the mod.

## Available Signs

The following mystical signs can be granted as rewards:
- `flame` - Fire and destruction magic
- `death` - Necromancy and undeath
- `soul` - Soul manipulation
- `blood` - Blood magic
- `mind` - Mental influence
- `winter` - Cold and ice magic
- `sacred` - Divine and holy magic
- `wicked` - Dark and forbidden magic

## Integration with Codex

Research can be linked to codex entries to create progressive discovery:

1. **Player triggers research** (kills zombie)
2. **Research grants rewards** (death sign)
3. **New codex entry unlocks** (zombie anatomy study)
4. **Player gains knowledge** (displayed in codex)

## Limitations

### ❌ Not Yet Implemented
According to `FEATURES.md`, these are NOT implemented yet:
- Enter Dimension tasks
- Kill Entity tasks (separate from triggers)
- Craft Item tasks
- Explore Biome tasks
- Use Ritual tasks
- Spell/Chant rewards
- Research unlock rewards
- Command rewards
- Prerequisite systems
- Research categories
- Progress tracking
- Multiplayer sync

## Working with the Current System

### Simple Kill Research
```json
{
  "id": "zombie_anatomy",
  "stars": 2,
  "triggers": ["entity:minecraft:zombie"],
  "tasks": {
    "1": [{"type": "item", "item": "minecraft:rotten_flesh", "count": 3}]
  },
  "rewards": [
    {"type": "sign", "sign": "death"}
  ]
}
```

### Multi-Stage Research
```json
{
  "id": "advanced_necromancy",
  "stars": 5,
  "triggers": ["entity:eidolon:wraith"],
  "conditions": [
    {"type": "dimension", "dimension": "minecraft:overworld"}
  ],
  "tasks": {
    "1": [{"type": "item", "item": "eidolon:soul_shard", "count": 3}],
    "2": [{"type": "xp", "levels": 5}],
    "3": [{"type": "item", "item": "minecraft:diamond", "count": 1}]
  },
  "rewards": [
    {"type": "sign", "sign": "soul"},
    {"type": "sign", "sign": "death"},
    {"type": "item", "item": "eidolon:greater_soul_gem", "count": 1}
  ]
}
```

## Next Steps

- **[Visual Diagrams](diagrams.md)** - See how the system flows
- **[Implementation Examples](examples.md)** - Working research files
- **[Integration Guide](integration.md)** - Connect with codex entries

## Trigger Types

### Entity Kills
```json
"triggers": ["entity:minecraft:zombie", "entity:eidolon:wraith"]
```
- Activates when player kills specified entities
- Supports any entity ID
- Can specify multiple entities

### Block Interactions
```json
"triggers": ["block:minecraft:diamond_ore"]
```
- Triggers on right-click interaction
- Requires note-taking tools in some cases
- Useful for environmental discoveries

### Location Discovery
```json
"triggers": ["location:minecraft:nether"]
```
- Auto-discovery when entering biomes/dimensions
- No action required from player
- Great for lore and background information

## Condition Types

### Dimension Condition
```json
{"type": "dimension", "dimension": "minecraft:nether"}
```
**Purpose**: Restrict research to specific dimensions  
**Usage**: Nether-only research, End exploration content

### Inventory Condition
```json
{"type": "inventory", "item": "eidolon:research_notes", "count": 1}
```
**Purpose**: Require specific items in inventory  
**Usage**: Tool requirements, prerequisite items

### Time Condition
```json
{"type": "time", "min": 13000, "max": 23000}
```
**Purpose**: Time-of-day restrictions  
**Usage**: Night-only research, dawn rituals

### Weather Condition
```json
{"type": "weather", "weather": "rain"}
```
**Purpose**: Weather-based requirements  
**Usage**: Storm magic, seasonal content

## Task Types

### Item Tasks
```json
{"type": "item", "item": "minecraft:diamond", "count": 3, "consume": true}
```
**Fields**:
- `item` - Item ID to collect
- `count` - Number required
- `consume` - Whether to remove from inventory (default: true)
- `nbt` - Optional NBT data matching

### XP Tasks
```json
{"type": "xp", "levels": 5}
```
**Purpose**: Spend experience levels  
**Usage**: Knowledge investment, spell learning

### NBT Item Matching
```json
{
  "type": "item",
  "item": "minecraft:written_book",
  "count": 1,
  "nbt": {
    "title": "Ancient Tome",
    "author": "Unknown Scholar"
  }
}
```

## Reward Types

### Sign Rewards
```json
{"type": "sign", "sign": "flame"}
```
**Available Signs**: `flame`, `death`, `soul`, `blood`, `mind`, `winter`, `sacred`, `wicked`  
**Purpose**: Grant mystical signs for Eidolon progression

### Item Rewards
```json
{"type": "item", "item": "minecraft:enchanted_book", "count": 1}
```
**Features**:
- Any item ID supported
- Custom NBT data
- Multiple items per reward

### NBT Item Rewards
```json
{
  "type": "item",
  "item": "minecraft:enchanted_book",
  "count": 1,
  "nbt": {
    "StoredEnchantments": [
      {"id": "minecraft:sharpness", "lvl": 3}
    ]
  }
}
```

## Multi-Stage Research

```json
{
  "id": "advanced_study",
  "stars": 5,
  "triggers": ["entity:eidolon:wraith"],
  "conditions": [
    {"type": "dimension", "dimension": "minecraft:overworld"}
  ],
  "tasks": {
    "1": [
      {"type": "item", "item": "eidolon:soul_shard", "count": 3},
      {"type": "xp", "levels": 2}
    ],
    "2": [
      {"type": "item", "item": "minecraft:diamond", "count": 1},
      {"type": "item", "item": "eidolon:arcane_gold_ingot", "count": 2}
    ],
    "3": [
      {"type": "item", "item": "eidolon:lesser_soul_gem", "count": 1}
    ]
  },
  "rewards": [
    {"type": "sign", "sign": "soul"},
    {"type": "sign", "sign": "death"},
    {"type": "item", "item": "eidolon:greater_soul_gem", "count": 1}
  ]
}
```

## File Organization

```
data/your_mod/
├── research/
│   ├── basic/
│   │   ├── zombie_study.json
│   │   └── skeleton_analysis.json
│   ├── advanced/
│   │   ├── wraith_research.json
│   │   └── soul_manipulation.json
│   └── endgame/
│       └── master_necromancy.json
└── codex_entries/
    ├── zombie_study_entry.json
    └── wraith_research_entry.json
```

## Integration with Codex

### Research-Locked Entries
```json
{
  "target_chapter": "advanced_studies",
  "research_requirement": "advanced_study",
  "pages": [
    {"type": "title", "text": "mod.codex.advanced.title"},
    {"type": "text", "text": "mod.codex.advanced.unlocked"}
  ]
}
```

### Progressive Disclosure
```json
{
  "target_chapter": "necromancy",
  "pages": [
    {"type": "title", "text": "mod.necromancy.basics"},
    {"type": "text", "text": "mod.necromancy.theory"},
    {
      "type": "text",
      "text": "mod.necromancy.advanced",
      "research_requirement": "master_necromancy"
    }
  ]
}
```

## Common Patterns

### Monster Study Chain
1. **Kill Trigger** → Basic entity information
2. **Item Collection** → Anatomical study
3. **XP Investment** → Advanced understanding
4. **Sign Reward** → Mystical mastery

### Environmental Discovery
1. **Location Trigger** → Area awareness
2. **Weather Condition** → Optimal timing
3. **Tool Requirement** → Proper investigation
4. **Knowledge Reward** → Understanding gained

### Crafting Progression
1. **Block Interaction** → Discovery of recipe
2. **Resource Collection** → Gather materials
3. **XP Investment** → Learn technique
4. **Item Reward** → Master recipe

## Debug and Testing

### Research Status Commands
```
/research list - Show all research
/research grant <id> - Grant specific research
/research reset - Clear all research progress
```

### Common Issues
- **Research not triggering**: Check trigger spelling and entity IDs
- **Conditions failing**: Verify dimension names and item IDs
- **Tasks not completing**: Ensure exact item matching with NBT
- **Rewards not granting**: Check sign names and item availability

## Best Practices

### Progression Design
- **Start simple**: Basic kills → item collection → advanced techniques
- **Build logically**: Each research should lead to the next
- **Reward appropriately**: Match difficulty with valuable rewards

### Player Experience
- **Clear objectives**: Make requirements obvious
- **Fair challenges**: Don't require excessive grinding
- **Meaningful rewards**: Provide progression that matters

### Content Integration
- **Link to codex**: Every research should unlock related entries
- **Build themes**: Group related research together
- **Progressive complexity**: Gradually increase difficulty

## Next Steps

- **[Datapack Structure](datapack-structure.md)** - Organize your research files
- **[Tips & Future Features](tips-and-future.md)** - Advanced research techniques
- **[Getting Started](getting-started.md)** - Return to basics if needed
