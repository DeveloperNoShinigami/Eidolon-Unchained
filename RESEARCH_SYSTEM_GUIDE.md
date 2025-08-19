# 🔬 Eidolon Unchained Research System - Complete Guide

## Overview

The Research System extends Eidolon's vanilla research tree with custom research entries that integrate seamlessly with the base mod. Research provides progression gates, unlock conditions, and structured learning paths for advanced magical techniques.

## 📁 File Structure

```
src/main/resources/data/eidolonunchained/
├── research_chapters/          # Research chapter definitions
│   ├── advanced_necromancy.json
│   ├── void_mastery.json
│   └── ritual_sciences.json
└── research_entries/           # Individual research entries
    ├── ritual_master.json
    ├── advanced_soul_manipulation.json
    ├── void_walker.json
    └── necromantic_mastery.json
```

## 🎯 Research System Components

### 1. Research Chapters
Organize research entries into thematic categories.

### 2. Research Entries
Individual research nodes with:
- **Prerequisites** - What research must be completed first
- **Tasks** - What the player must accomplish
- **Conditionals** - Dynamic requirements based on game state
- **Unlocks** - What this research grants access to
- **Type System** - Different research categories with unique properties

## 📋 Complete Research Entry Schema

### Basic Structure
```json
{
  "id": "eidolonunchained:research_name",
  "title": "Research Display Name",
  "description": "What this research accomplishes",
  "chapter": "eidolonunchained:chapter_name",
  "type": "advanced",
  "icon": {
    "item": "minecraft:item_name",
    "count": 1,
    "nbt": "{custom:tag}"
  },
  "position": {
    "x": 0,
    "y": 0
  },
  "prerequisites": [
    "eidolon:base_research",
    "eidolonunchained:other_research"
  ],
  "unlocks": [
    "eidolonunchained:advanced_research"
  ],
  "star_requirement": 3,
  "conditional_requirements": {
    "dimension": "minecraft:the_nether",
    "biome_tags": ["minecraft:is_overworld"],
    "time_range": {
      "min": 18000,
      "max": 6000
    },
    "weather": "thunderstorm",
    "moon_phase": "new_moon"
  },
  "tasks": {
    "tier_1": [...],
    "tier_2": [...],
    "tier_3": [...]
  },
  "rewards": {
    "knowledge_points": 50,
    "reputation": {
      "dark_deity": 10,
      "light_deity": -5
    },
    "items": [
      {
        "item": "eidolon:soul_gem",
        "count": 3
      }
    ],
    "unlock_recipes": [
      "eidolonunchained:advanced_ritual"
    ]
  }
}
```

## 🎮 Task System - Complete Examples

### Task Types Available

#### 1. Kill Entities
```json
{
  "type": "kill_entities",
  "entity": "minecraft:zombie",
  "count": 50,
  "conditions": {
    "dimension": "minecraft:overworld",
    "weapon_type": "eidolon:athame",
    "time_of_day": "night"
  }
}
```

#### 2. Craft Items
```json
{
  "type": "craft_items",
  "item": "eidolon:soul_gem",
  "count": 10,
  "conditions": {
    "crafting_station": "eidolon:worktable",
    "require_enchantment": "soul_harvest"
  }
}
```

#### 3. Use Rituals
```json
{
  "type": "use_ritual",
  "ritual": "eidolon:summon_wraith",
  "count": 5,
  "conditions": {
    "moon_phase": "full_moon",
    "location_type": "graveyard",
    "success_required": true
  }
}
```

#### 4. Collect Items
```json
{
  "type": "collect_items",
  "items": [
    {
      "item": "eidolon:death_essence",
      "count": 20
    },
    {
      "item": "eidolon:crimson_essence", 
      "count": 15
    }
  ],
  "conditions": {
    "harvest_method": "ritual_extraction",
    "purity_level": "high"
  }
}
```

#### 5. Visit Locations
```json
{
  "type": "visit_locations",
  "locations": [
    {
      "structure": "eidolon:catacomb",
      "count": 3
    },
    {
      "biome": "minecraft:soul_sand_valley",
      "count": 1
    }
  ]
}
```

#### 6. Interact with Blocks/Entities
```json
{
  "type": "interact",
  "targets": [
    {
      "type": "block",
      "block": "eidolon:crucible",
      "action": "successful_recipe",
      "count": 25
    },
    {
      "type": "entity",
      "entity": "minecraft:villager", 
      "action": "soul_drain",
      "count": 10
    }
  ]
}
```

#### 7. Gain Experience
```json
{
  "type": "gain_experience",
  "experience_type": "soul_manipulation",
  "amount": 1000,
  "conditions": {
    "source_restriction": "ritual_only"
  }
}
```

## 🔀 Advanced Conditionals System

### Time-Based Conditionals
```json
"conditional_requirements": {
  "time_conditions": {
    "day_time_range": {
      "start": 18000,
      "end": 6000,
      "wrap_midnight": true
    },
    "moon_phase": "new_moon",
    "season": "winter",
    "weather": ["thunderstorm", "rain"]
  }
}
```

### Location-Based Conditionals
```json
"conditional_requirements": {
  "location_conditions": {
    "dimension": "minecraft:the_nether",
    "biome_categories": ["nether", "end"],
    "structure_nearby": {
      "structure": "eidolon:catacomb",
      "max_distance": 100
    },
    "altitude_range": {
      "min": 0,
      "max": 64
    },
    "light_level": {
      "max": 7
    }
  }
}
```

### Player State Conditionals
```json
"conditional_requirements": {
  "player_conditions": {
    "min_reputation": {
      "dark_deity": 50,
      "light_deity": -20
    },
    "required_effects": [
      "eidolon:blessed",
      "minecraft:night_vision"
    ],
    "equipment_requirements": {
      "helmet": "eidolon:warlock_hat",
      "mainhand": "eidolon:athame"
    },
    "soul_energy": {
      "min": 100,
      "consume_on_completion": 50
    }
  }
}
```

### Research Dependencies
```json
"conditional_requirements": {
  "research_conditions": {
    "completed_research": [
      "eidolon:basic_necromancy",
      "eidolonunchained:soul_mastery"
    ],
    "research_points": {
      "necromancy": 25,
      "theurgy": 10
    },
    "forbidden_if": [
      "eidolon:divine_blessing"
    ]
  }
}
```

## 📚 Complete Research Entry Examples

### Example 1: Master Necromancer Research
```json
{
  "id": "eidolonunchained:master_necromancer", 
  "title": "Master Necromancer",
  "description": "Achieve ultimate mastery over death and undeath",
  "chapter": "eidolonunchained:advanced_necromancy",
  "type": "forbidden",
  "icon": {
    "item": "eidolon:deathbringer_scythe",
    "nbt": "{Enchantments:[{id:'eidolon:soul_reaper',lvl:3}]}"
  },
  "position": {
    "x": 5,
    "y": 8
  },
  "prerequisites": [
    "eidolon:advanced_necromancy",
    "eidolonunchained:soul_mastery",
    "eidolonunchained:death_essence_mastery"
  ],
  "star_requirement": 7,
  "conditional_requirements": {
    "time_conditions": {
      "moon_phase": "new_moon",
      "time_range": {
        "start": 0,
        "end": 6000
      }
    },
    "location_conditions": {
      "structure_nearby": {
        "structure": "eidolon:catacomb",
        "max_distance": 50
      },
      "light_level": {
        "max": 3
      }
    },
    "player_conditions": {
      "min_reputation": {
        "dark_deity": 100
      },
      "required_kills": {
        "total": 1000,
        "undead": 500
      }
    }
  },
  "tasks": {
    "tier_1": [
      {
        "type": "kill_entities",
        "entity": "minecraft:zombie",
        "count": 200,
        "conditions": {
          "weapon_type": "eidolon:athame",
          "soul_harvest_required": true
        }
      },
      {
        "type": "use_ritual",
        "ritual": "eidolon:animate_dead",
        "count": 50,
        "conditions": {
          "success_rate": 0.9,
          "moon_phase": ["new_moon", "waning_crescent"]
        }
      }
    ],
    "tier_2": [
      {
        "type": "craft_items",
        "item": "eidolon:greater_soul_gem",
        "count": 10,
        "conditions": {
          "soul_purity": "corrupted",
          "crafting_location": "catacomb"
        }
      },
      {
        "type": "collect_items",
        "items": [
          {
            "item": "eidolon:death_essence",
            "count": 100,
            "source": "player_kills_only"
          }
        ]
      }
    ],
    "tier_3": [
      {
        "type": "perform_ritual_sequence",
        "rituals": [
          "eidolon:raise_skeleton",
          "eidolon:bind_wraith", 
          "eidolon:death_mastery"
        ],
        "conditions": {
          "time_limit": 3600,
          "no_failures": true,
          "location": "ancient_altar"
        }
      }
    ]
  },
  "rewards": {
    "knowledge_points": 200,
    "reputation": {
      "dark_deity": 50,
      "light_deity": -100
    },
    "items": [
      {
        "item": "eidolonunchained:necromancer_crown",
        "count": 1,
        "nbt": "{Master:true,SoulBound:true}"
      }
    ],
    "unlock_recipes": [
      "eidolonunchained:lich_transformation_ritual",
      "eidolonunchained:soul_phylactery"
    ],
    "unlock_spells": [
      "eidolonunchained:mass_animate_dead",
      "eidolonunchained:death_nova"
    ],
    "special_abilities": [
      "immunity_to_undead_damage",
      "soul_sight",
      "speak_with_dead"
    ]
  }
}
```

### Example 2: Conditional Void Walker Research
```json
{
  "id": "eidolonunchained:void_walker",
  "title": "Void Walker",
  "description": "Master interdimensional travel through void manipulation",
  "chapter": "eidolonunchained:void_mastery", 
  "type": "forbidden",
  "icon": {
    "item": "eidolon:void_amulet",
    "count": 1
  },
  "position": {
    "x": 3,
    "y": 4
  },
  "prerequisites": [
    "eidolon:void_amulet"
  ],
  "conditional_requirements": {
    "dimension_access": [
      "minecraft:the_nether",
      "minecraft:the_end"
    ],
    "location_conditions": {
      "visited_structures": [
        "minecraft:end_city",
        "minecraft:nether_fortress"
      ]
    },
    "player_conditions": {
      "void_exposure_time": 7200,
      "dimensional_shifts": 100
    }
  },
  "tasks": {
    "tier_1": [
      {
        "type": "visit_locations", 
        "locations": [
          {
            "dimension": "minecraft:the_end",
            "duration": 1200
          },
          {
            "dimension": "minecraft:the_nether",
            "biome": "minecraft:soul_sand_valley",
            "duration": 600
          }
        ]
      },
      {
        "type": "collect_items",
        "items": [
          {
            "item": "minecraft:ender_pearl",
            "count": 64,
            "method": "enderman_kills_only"
          }
        ]
      }
    ],
    "tier_2": [
      {
        "type": "use_ritual",
        "ritual": "eidolonunchained:void_portal",
        "count": 10,
        "conditions": {
          "portal_distance": 1000,
          "stable_duration": 300
        }
      },
      {
        "type": "survive_challenge",
        "challenge": "void_exposure",
        "conditions": {
          "duration": 600,
          "damage_taken": 0,
          "void_energy_absorbed": 50
        }
      }
    ],
    "tier_3": [
      {
        "type": "master_technique",
        "technique": "dimensional_step",
        "requirements": {
          "successful_teleports": 100,
          "max_distance": 500,
          "accuracy": 0.95
        }
      }
    ]
  },
  "rewards": {
    "unlock_spells": [
      "eidolonunchained:void_step",
      "eidolonunchained:dimensional_anchor"
    ],
    "special_abilities": [
      "void_immunity",
      "dimensional_sight",
      "phase_walk"
    ]
  }
}
```

## 🔧 Integration with Eidolon

### Research Prerequisites
All research entries can reference base Eidolon research:
- `eidolon:basic_necromancy`
- `eidolon:soul_sorcery`
- `eidolon:theurgy`
- And all others from the base mod

### Codex Integration
Research unlocks can automatically add codex entries:
```json
"rewards": {
  "unlock_codex": [
    "eidolonunchained:advanced_summoning",
    "eidolonunchained:void_techniques"
  ]
}
```

## 🎯 Research Types & Properties

### Basic Research
- Simple unlock requirements
- Standard progression
- Visible from start

### Advanced Research  
- Complex multi-tier tasks
- Higher star requirements
- Often requires multiple prerequisites

### Forbidden Research
- Hidden until prerequisites met
- Often requires dark reputation
- May have negative consequences

### Ritual Research
- Focus on ritual mastery
- Often location-dependent
- Requires specific conditions

### Crafting Research
- Unlocks new recipes
- Material-focused tasks
- Workshop/crafting integration

## 📊 Dynamic Difficulty & Conditionals

Research entries can scale based on:
- **Player Level** - Tasks become more demanding
- **World State** - Moon phases, seasons, weather
- **Reputation** - Deity standing affects availability
- **Previous Choices** - Some research locks out others
- **Multiplayer** - Group challenges and shared progress

This creates a truly dynamic research system where player choices, world state, and progression create unique research paths for every playthrough!
