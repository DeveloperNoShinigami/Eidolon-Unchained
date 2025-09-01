# Research Trigger System

## Overview

This system provides a JSON-driven approach to research triggers embedded directly within research definitions. Triggers are defined in the research files themselves, not in separate folders. This keeps the datapack structure clean and unified.

## Features

- **Entity kills** with NBT matching
- **Block interactions** with NBT support  
- **Location-based triggers** (dimensions, biomes, structures)
- **Ritual completion** triggers
- **Inventory-wide item checking** with NBT support
- **Coordinate-based location triggers** (optional)

## Research File Structure

Triggers are embedded in the research JSON files under the `triggers` field:

```json
{
  "id": "research_name",
  "stars": 3,
  "triggers": [
    {
      "type": "trigger_type",
      "entity": "minecraft:zombie",
      "coordinates": {
        "x": 100,
        "z": 200,
        "range": 50.0
      },
      "item_requirements": {
        "check_inventory": true,
        "items": [
          {
            "item": "minecraft:diamond_sword",
            "nbt": {
              "Enchantments": [{"id": "minecraft:sharpness", "lvl": 3}]
            },
            "count": 1
          }
        ]
      }
    }
  ],
  "tasks": { ... },
  "rewards": [ ... ]
}
```

## Trigger Types

### 1. Kill Entity Triggers

```json
{
  "type": "kill_entity",
  "entity": "minecraft:zombie",
  "nbt": {
    "IsBaby": false
  },
  "item_requirements": {
    "check_inventory": true,
    "items": [
      {
        "item": "minecraft:iron_sword",
        "nbt": {
          "Enchantments": [
            {
              "id": "minecraft:smite", 
              "lvl": 2
            }
          ]
        }
      }
    ]
  }
}
```

### 2. Block Interaction Triggers

```json
{
  "type": "block_interaction",
  "block": "eidolon:wooden_altar",
  "nbt": {},
  "item_requirements": {
    "check_inventory": true,
    "items": [
      {
        "item": "eidolon:arcane_gold_ingot",
        "nbt": {}
      }
    ]
  }
}
```

### 3. Dimension Triggers

```json
{
  "type": "dimension",
  "dimension": "minecraft:the_nether",
  "item_requirements": {
    "check_inventory": true,
    "items": [
      {
        "item": "minecraft:obsidian",
        "nbt": {}
      }
    ]
  }
}
```

### 4. Biome Triggers

```json
{
  "type": "biome",
  "biome": "minecraft:desert",
  "coordinates": {
    "x": 1000,
    "z": -500,
    "range": 100.0
  },
  "item_requirements": {
    "check_inventory": true,
    "items": [
      {
        "item": "minecraft:sand",
        "nbt": {}
      }
    ]
  }
}
```

### 5. Structure Triggers

```json
{
  "type": "structure",
  "structure": "minecraft:desert_pyramid", 
  "coordinates": {
    "x": 200,
    "z": 300,
    "range": 50.0
  },
  "item_requirements": {
    "check_inventory": true,
    "items": [
      {
        "item": "minecraft:gold_ingot",
        "nbt": {}
      }
    ]
  }
}
```

### 6. Ritual Triggers

```json
{
  "type": "ritual",
  "ritual": "eidolonunchained:nature_blessing",
  "item_requirements": {
    "check_inventory": true,
    "items": []
  }
}
```

## Coordinate System

For biome and structure triggers, you can optionally specify exact coordinates:

```json
{
  "coordinates": {
    "x": 100,       // X coordinate
    "z": 200,       // Z coordinate  
    "y": 64,        // Y coordinate (optional)
    "range": 50.0   // Range in blocks (default: 50.0)
  }
}
```

- **If coordinates are specified**: Player must be within range of those coordinates AND match the biome/structure
- **If coordinates are omitted**: Automatic detection based on player's current location

## NBT Support

### Entity NBT Examples
- **Charged Creeper:** `{"powered": 1}`
- **Baby Zombie:** `{"IsBaby": true}`
- **Named Entity:** `{"CustomName": "{\"text\":\"Boss Name\"}"}`

### Item NBT Examples
- **Enchanted Items:**
```json
{
  "nbt": {
    "Enchantments": [
      {
        "id": "minecraft:sharpness",
        "lvl": 3
      }
    ]
  }
}
```

- **Named Items:**
```json
{
  "nbt": {
    "display": {
      "Name": "{\"text\":\"Custom Name\"}"
    }
  }
}
```

## Item Requirements

### Inventory Checking
- **`"check_inventory": true`** - Checks entire player inventory including armor slots
- **`"check_inventory": false`** - Only checks mainhand and offhand

### Count Requirements
```json
{
  "item": "minecraft:gold_ingot",
  "count": 5,
  "nbt": {}
}
```

## File Location

All research files with embedded triggers go in:
```
data/eidolonunchained/eidolon_research/
├── simple_research_example.json
├── nature_blessing_ritual.json
└── your_research.json
```

## Complete Example

```json
{
  "id": "desert_temple_explorer",
  "stars": 4,
  "triggers": [
    {
      "type": "structure",
      "structure": "minecraft:desert_pyramid",
      "coordinates": {
        "x": 1000,
        "z": 2000, 
        "range": 25.0
      },
      "item_requirements": {
        "check_inventory": true,
        "items": [
          {
            "item": "minecraft:diamond_pickaxe",
            "nbt": {
              "Enchantments": [
                {
                  "id": "minecraft:efficiency",
                  "lvl": 3
                }
              ]
            },
            "count": 1
          }
        ]
      }
    }
  ],
  "tasks": {
    "0": [
      {
        "type": "item",
        "item": "minecraft:gold_ingot",
        "count": 4
      }
    ]
  },
  "rewards": [
    {
      "type": "item", 
      "item": "minecraft:enchanted_book",
      "count": 1
    }
  ]
}
```

## Benefits

✅ **No separate trigger files** - everything in one place  
✅ **Dynamic loading** from research datapacks  
✅ **Full NBT support** for entities and items  
✅ **Inventory-wide checking** (not just mainhand)  
✅ **Coordinate precision** for location triggers  
✅ **Automatic integration** with Eidolon's research system
