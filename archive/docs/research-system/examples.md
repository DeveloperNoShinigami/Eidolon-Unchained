# Research System Examples

**Working research configurations you can copy and adapt**

## Simple Kill Research

### Basic Zombie Study
*Grants Death sign when killing zombies*

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

File location: `data/yourmod/eidolon_research/zombie_anatomy.json`

## Multi-Stage Research

### Advanced Necromancy
*Complex research with multiple stages and conditions*

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

## NBT Item Examples

### Enchanted Book Research
*Requires specific enchanted items*

```json
{
  "id": "enchantment_study",
  "stars": 3,
  "triggers": ["entity:minecraft:witch"],
  "tasks": {
    "1": [{
      "type": "item",
      "item": "minecraft:enchanted_book",
      "count": 1,
      "nbt": "{StoredEnchantments:[{id:\"minecraft:mending\",lvl:1s}]}"
    }]
  },
  "rewards": [
    {"type": "sign", "sign": "sacred"},
    {"type": "item", "item": "minecraft:experience_bottle", "count": 5}
  ]
}
```

## Condition Examples

### Nether Exclusive Research
*Only available in the Nether*

```json
{
  "id": "nether_studies",
  "stars": 4,
  "triggers": ["entity:minecraft:blaze"],
  "conditions": [
    {"type": "dimension", "dimension": "minecraft:the_nether"}
  ],
  "tasks": {
    "1": [{"type": "item", "item": "minecraft:blaze_rod", "count": 5}],
    "2": [{"type": "xp", "levels": 3}]
  },
  "rewards": [
    {"type": "sign", "sign": "flame"},
    {"type": "item", "item": "minecraft:fire_charge", "count": 10}
  ]
}
```

### Inventory Check Research
*Requires player to have specific items*

```json
{
  "id": "prepared_explorer",
  "stars": 2,
  "triggers": ["entity:minecraft:enderman"],
  "conditions": [
    {
      "type": "inventory",
      "item": "minecraft:ender_pearl",
      "count": 3
    }
  ],
  "tasks": {
    "1": [{"type": "item", "item": "minecraft:ender_eye", "count": 1}]
  },
  "rewards": [
    {"type": "sign", "sign": "mind"},
    {"type": "item", "item": "minecraft:end_stone", "count": 16}
  ]
}
```

## Time and Weather Conditions

### Night Research
*Only discoverable at night*

```json
{
  "id": "nocturnal_studies",
  "stars": 3,
  "triggers": ["entity:minecraft:spider"],
  "conditions": [
    {"type": "time", "min": 13000, "max": 23000}
  ],
  "tasks": {
    "1": [{"type": "item", "item": "minecraft:spider_eye", "count": 8}]
  },
  "rewards": [
    {"type": "sign", "sign": "wicked"},
    {"type": "item", "item": "minecraft:fermented_spider_eye", "count": 2}
  ]
}
```

### Storm Research
*Requires thunderstorm weather*

```json
{
  "id": "storm_calling",
  "stars": 4,
  "triggers": ["entity:minecraft:skeleton_horse"],
  "conditions": [
    {"type": "weather", "weather": "thunder"}
  ],
  "tasks": {
    "1": [{"type": "xp", "levels": 10}],
    "2": [{"type": "item", "item": "minecraft:lightning_rod", "count": 1}]
  },
  "rewards": [
    {"type": "sign", "sign": "flame"},
    {"type": "sign", "sign": "sacred"},
    {"type": "item", "item": "minecraft:trident", "count": 1}
  ]
}
```

## All Available Signs

Here are all the mystical signs you can grant as rewards:

- `flame` - Fire and destruction magic
- `death` - Necromancy and undeath  
- `soul` - Soul manipulation
- `blood` - Blood magic
- `mind` - Mental influence
- `winter` - Cold and ice magic
- `sacred` - Divine and holy magic
- `wicked` - Dark and forbidden magic

## Reward Types

### Sign Rewards
```json
"rewards": [
  {"type": "sign", "sign": "flame"}
]
```

### Item Rewards  
```json
"rewards": [
  {"type": "item", "item": "minecraft:diamond", "count": 3}
]
```

### Item Rewards with NBT
```json
"rewards": [
  {
    "type": "item", 
    "item": "minecraft:diamond_sword",
    "count": 1,
    "nbt": "{Enchantments:[{id:\"minecraft:sharpness\",lvl:5s}]}"
  }
]
```

### Multiple Rewards
```json
"rewards": [
  {"type": "sign", "sign": "death"},
  {"type": "sign", "sign": "soul"},
  {"type": "item", "item": "eidolon:soul_gem", "count": 5},
  {"type": "item", "item": "minecraft:experience_bottle", "count": 10}
]
```

## Integration Tips

1. **Link to Codex**: Create codex entries that reference the same entities/items as your research
2. **Progressive Difficulty**: Start with simple 1-star research, build to complex 5-star chains  
3. **Thematic Rewards**: Match sign types to the content theme (death signs for undead research)
4. **Reasonable Tasks**: Don't make stages too grindy - players should feel progression
5. **Test in Game**: Always verify your research triggers and rewards work as expected
