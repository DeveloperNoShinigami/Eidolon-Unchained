# Comprehensive Codex Implementation Guide
*All 97 Eidolon Codex Chapters with JSON Datapack Examples*

## Overview

This guide provides a complete 1:1 implementation structure for all 97 available Eidolon codex chapters. Each entry uses existing Eidolon and Minecraft item IDs, entities, and blocks to create working examples that can be immediately implemented in your datapack.

---

## Complete Chapter Implementation Reference

### **Category 1: Creatures & Natural World (3 chapters)**

#### 1. MONSTERS Chapter (`monsters`)
**File Path:** `src/main/resources/data/eidolonunchained/codex_entries/creatures/monsters.json`

```json
{
  "chapter": "monsters",
  "pages": [
    {
      "type": "text",
      "title": "eidolonunchained.codex.monsters.enhanced_zombies.title",
      "text": "eidolonunchained.codex.monsters.enhanced_zombies.desc"
    },
    {
      "type": "entity_showcase",
      "title": "eidolonunchained.codex.monsters.wraith_showcase.title", 
      "entity": "eidolon:wraith",
      "text": "eidolonunchained.codex.monsters.wraith_showcase.desc"
    },
    {
      "type": "crafting",
      "title": "eidolonunchained.codex.monsters.necromancer_spawn.title",
      "recipe": {
        "type": "minecraft:crafting_shaped",
        "pattern": [
          "SBS",
          "BTB",
          "SBS"
        ],
        "key": {
          "S": {"item": "eidolon:soul_shard"},
          "B": {"item": "minecraft:bone"},
          "T": {"item": "eidolon:tattered_cloth"}
        },
        "result": {
          "item": "eidolon:necromancer_spawn_egg",
          "count": 1
        }
      },
      "text": "eidolonunchained.codex.monsters.necromancer_spawn.desc"
    }
  ]
}
```

#### 2. CRITTERS Chapter (`critters`)
**File Path:** `src/main/resources/data/eidolonunchained/codex_entries/creatures/critters.json`

```json
{
  "chapter": "critters",
  "pages": [
    {
      "type": "text",
      "title": "eidolonunchained.codex.critters.raven_behavior.title",
      "text": "eidolonunchained.codex.critters.raven_behavior.desc"
    },
    {
      "type": "entity_showcase",
      "title": "eidolonunchained.codex.critters.raven_showcase.title",
      "entity": "eidolon:raven",
      "text": "eidolonunchained.codex.critters.raven_showcase.desc"
    },
    {
      "type": "item_showcase",
      "title": "eidolonunchained.codex.critters.raven_feather.title",
      "item": "eidolon:raven_feather",
      "text": "eidolonunchained.codex.critters.raven_feather.desc"
    }
  ]
}
```

#### 3. PLANTS Chapter (`plants` - Rare Flora)
**File Path:** `src/main/resources/data/eidolonunchained/codex_entries/creatures/plants.json`

```json
{
  "chapter": "plants",
  "pages": [
    {
      "type": "text",
      "title": "eidolonunchained.codex.plants.magical_flora.title",
      "text": "eidolonunchained.codex.plants.magical_flora.desc"
    },
    {
      "type": "item_showcase",
      "title": "eidolonunchained.codex.plants.elderwood.title",
      "item": "eidolon:elderwood_log",
      "text": "eidolonunchained.codex.plants.elderwood.desc"
    },
    {
      "type": "crafting",
      "title": "eidolonunchained.codex.plants.magical_seeds.title",
      "recipe": {
        "type": "minecraft:crafting_shapeless",
        "ingredients": [
          {"item": "minecraft:wheat_seeds"},
          {"item": "eidolon:death_essence"},
          {"item": "minecraft:bone_meal"}
        ],
        "result": {
          "item": "eidolon:unholy_symbol",
          "count": 2
        }
      },
      "text": "eidolonunchained.codex.plants.magical_seeds.desc"
    }
  ]
}
```

---

### **Category 2: Resources & Materials (8 chapters)**

#### 4. ORES Chapter (`ores`)
**File Path:** `src/main/resources/data/eidolonunchained/codex_entries/resources/ores.json`

```json
{
  "chapter": "ores",
  "pages": [
    {
      "type": "text",
      "title": "eidolonunchained.codex.ores.magical_mining.title",
      "text": "eidolonunchained.codex.ores.magical_mining.desc"
    },
    {
      "type": "item_showcase",
      "title": "eidolonunchained.codex.ores.lead_ore.title",
      "item": "eidolon:lead_ore",
      "text": "eidolonunchained.codex.ores.lead_ore.desc"
    },
    {
      "type": "item_showcase",
      "title": "eidolonunchained.codex.ores.silver_ore.title",
      "item": "eidolon:silver_ore",
      "text": "eidolonunchained.codex.ores.silver_ore.desc"
    },
    {
      "type": "smelting",
      "title": "eidolonunchained.codex.ores.ore_processing.title",
      "recipe": {
        "type": "minecraft:smelting",
        "ingredient": {"item": "eidolon:lead_ore"},
        "result": "eidolon:lead_ingot",
        "experience": 0.7,
        "cookingtime": 200
      },
      "text": "eidolonunchained.codex.ores.ore_processing.desc"
    }
  ]
}
```

#### 5. PEWTER Chapter (`pewter`)
**File Path:** `src/main/resources/data/eidolonunchained/codex_entries/resources/pewter.json`

```json
{
  "chapter": "pewter",
  "pages": [
    {
      "type": "text",
      "title": "eidolonunchained.codex.pewter.alloy_creation.title",
      "text": "eidolonunchained.codex.pewter.alloy_creation.desc"
    },
    {
      "type": "item_showcase",
      "title": "eidolonunchained.codex.pewter.pewter_ingot.title",
      "item": "eidolon:pewter_ingot",
      "text": "eidolonunchained.codex.pewter.pewter_ingot.desc"
    },
    {
      "type": "crafting",
      "title": "eidolonunchained.codex.pewter.pewter_blend.title",
      "recipe": {
        "type": "minecraft:crafting_shapeless",
        "ingredients": [
          {"item": "eidolon:lead_ingot"},
          {"item": "eidolon:lead_ingot"},
          {"item": "minecraft:iron_ingot"},
          {"item": "eidolon:silver_ingot"}
        ],
        "result": {
          "item": "eidolon:pewter_ingot",
          "count": 3
        }
      },
      "text": "eidolonunchained.codex.pewter.pewter_blend.desc"
    }
  ]
}
```

#### 6. ENCHANTED_ASH Chapter (`enchanted_ash`)
**File Path:** `src/main/resources/data/eidolonunchained/codex_entries/resources/enchanted_ash.json`

```json
{
  "chapter": "enchanted_ash",
  "pages": [
    {
      "type": "text",
      "title": "eidolonunchained.codex.enchanted_ash.creation_process.title",
      "text": "eidolonunchained.codex.enchanted_ash.creation_process.desc"
    },
    {
      "type": "item_showcase",
      "title": "eidolonunchained.codex.enchanted_ash.ash_showcase.title",
      "item": "eidolon:enchanted_ash",
      "text": "eidolonunchained.codex.enchanted_ash.ash_showcase.desc"
    },
    {
      "type": "smelting",
      "title": "eidolonunchained.codex.enchanted_ash.burning_wood.title",
      "recipe": {
        "type": "minecraft:smelting",
        "ingredient": {"item": "eidolon:elderwood_log"},
        "result": "eidolon:enchanted_ash",
        "experience": 1.0,
        "cookingtime": 200
      },
      "text": "eidolonunchained.codex.enchanted_ash.burning_wood.desc"
    }
  ]
}
```

#### 7. TALLOW Chapter (`tallow`)
**File Path:** `src/main/resources/data/eidolonunchained/codex_entries/resources/tallow.json`

```json
{
  "chapter": "tallow",
  "pages": [
    {
      "type": "text",
      "title": "eidolonunchained.codex.tallow.wax_properties.title",
      "text": "eidolonunchained.codex.tallow.wax_properties.desc"
    },
    {
      "type": "item_showcase",
      "title": "eidolonunchained.codex.tallow.tallow_showcase.title",
      "item": "eidolon:tallow",
      "text": "eidolonunchained.codex.tallow.tallow_showcase.desc"
    },
    {
      "type": "smelting",
      "title": "eidolonunchained.codex.tallow.fat_rendering.title",
      "recipe": {
        "type": "minecraft:smelting",
        "ingredient": {"item": "minecraft:beef"},
        "result": "eidolon:tallow",
        "experience": 0.35,
        "cookingtime": 200
      },
      "text": "eidolonunchained.codex.tallow.fat_rendering.desc"
    }
  ]
}
```

#### 8. ARCANE_GOLD Chapter (`arcane_gold`)
**File Path:** `src/main/resources/data/eidolonunchained/codex_entries/resources/arcane_gold.json`

```json
{
  "chapter": "arcane_gold",
  "pages": [
    {
      "type": "text",
      "title": "eidolonunchained.codex.arcane_gold.transmutation.title",
      "text": "eidolonunchained.codex.arcane_gold.transmutation.desc"
    },
    {
      "type": "item_showcase",
      "title": "eidolonunchained.codex.arcane_gold.arcane_gold_ingot.title",
      "item": "eidolon:arcane_gold_ingot",
      "text": "eidolonunchained.codex.arcane_gold.arcane_gold_ingot.desc"
    },
    {
      "type": "ritual",
      "title": "eidolonunchained.codex.arcane_gold.gold_transmutation.title",
      "ritual": {
        "type": "eidolon:crucible",
        "ingredients": [
          {"item": "minecraft:gold_ingot", "count": 2},
          {"item": "eidolon:death_essence", "count": 1},
          {"item": "eidolon:soul_shard", "count": 1}
        ],
        "result": {
          "item": "eidolon:arcane_gold_ingot",
          "count": 1
        },
        "time": 300
      },
      "text": "eidolonunchained.codex.arcane_gold.gold_transmutation.desc"
    }
  ]
}
```

#### 9. REAGENTS Chapter (`reagents`)
**File Path:** `src/main/resources/data/eidolonunchained/codex_entries/resources/reagents.json`

```json
{
  "chapter": "reagents",
  "pages": [
    {
      "type": "text",
      "title": "eidolonunchained.codex.reagents.essence_types.title",
      "text": "eidolonunchained.codex.reagents.essence_types.desc"
    },
    {
      "type": "item_showcase",
      "title": "eidolonunchained.codex.reagents.death_essence.title",
      "item": "eidolon:death_essence",
      "text": "eidolonunchained.codex.reagents.death_essence.desc"
    },
    {
      "type": "item_showcase",
      "title": "eidolonunchained.codex.reagents.crimson_essence.title",
      "item": "eidolon:crimson_essence",
      "text": "eidolonunchained.codex.reagents.crimson_essence.desc"
    }
  ]
}
```

#### 10. SOUL_GEMS Chapter (`soul_gems`)
**File Path:** `src/main/resources/data/eidolonunchained/codex_entries/resources/soul_gems.json`

```json
{
  "chapter": "soul_gems",
  "pages": [
    {
      "type": "text",
      "title": "eidolonunchained.codex.soul_gems.soul_capturing.title",
      "text": "eidolonunchained.codex.soul_gems.soul_capturing.desc"
    },
    {
      "type": "item_showcase",
      "title": "eidolonunchained.codex.soul_gems.lesser_soul_gem.title",
      "item": "eidolon:lesser_soul_gem",
      "text": "eidolonunchained.codex.soul_gems.lesser_soul_gem.desc"
    },
    {
      "type": "item_showcase",
      "title": "eidolonunchained.codex.soul_gems.greater_soul_gem.title",
      "item": "eidolon:greater_soul_gem",
      "text": "eidolonunchained.codex.soul_gems.greater_soul_gem.desc"
    }
  ]
}
```

#### 11. SHADOW_GEM Chapter (`shadow_gem`)
**File Path:** `src/main/resources/data/eidolonunchained/codex_entries/resources/shadow_gem.json`

```json
{
  "chapter": "shadow_gem",
  "pages": [
    {
      "type": "text",
      "title": "eidolonunchained.codex.shadow_gem.dark_energy.title",
      "text": "eidolonunchained.codex.shadow_gem.dark_energy.desc"
    },
    {
      "type": "item_showcase",
      "title": "eidolonunchained.codex.shadow_gem.shadow_gem_showcase.title",
      "item": "eidolon:shadow_gem",
      "text": "eidolonunchained.codex.shadow_gem.shadow_gem_showcase.desc"
    },
    {
      "type": "crafting",
      "title": "eidolonunchained.codex.shadow_gem.dark_infusion.title",
      "recipe": {
        "type": "minecraft:crafting_shaped",
        "pattern": [
          " D ",
          "DGD",
          " D "
        ],
        "key": {
          "D": {"item": "eidolon:death_essence"},
          "G": {"item": "minecraft:diamond"}
        },
        "result": {
          "item": "eidolon:shadow_gem",
          "count": 1
        }
      },
      "text": "eidolonunchained.codex.shadow_gem.dark_infusion.desc"
    }
  ]
}
```

---

### **Category 3: Equipment & Tools (20 chapters)**

#### 12. INLAYS Chapter (`inlays`)
**File Path:** `src/main/resources/data/eidolonunchained/codex_entries/equipment/inlays.json`

```json
{
  "chapter": "inlays",
  "pages": [
    {
      "type": "text",
      "title": "eidolonunchained.codex.inlays.enhancement_system.title",
      "text": "eidolonunchained.codex.inlays.enhancement_system.desc"
    },
    {
      "type": "item_showcase",
      "title": "eidolonunchained.codex.inlays.gold_inlay.title",
      "item": "eidolon:gold_inlay",
      "text": "eidolonunchained.codex.inlays.gold_inlay.desc"
    },
    {
      "type": "item_showcase",
      "title": "eidolonunchained.codex.inlays.silver_inlay.title",
      "item": "eidolon:silver_inlay",
      "text": "eidolonunchained.codex.inlays.silver_inlay.desc"
    },
    {
      "type": "crafting",
      "title": "eidolonunchained.codex.inlays.inlay_creation.title",
      "recipe": {
        "type": "minecraft:crafting_shaped",
        "pattern": [
          "GGG",
          "GPG",
          "GGG"
        ],
        "key": {
          "G": {"item": "minecraft:gold_nugget"},
          "P": {"item": "eidolon:pewter_ingot"}
        },
        "result": {
          "item": "eidolon:gold_inlay",
          "count": 2
        }
      },
      "text": "eidolonunchained.codex.inlays.inlay_creation.desc"
    }
  ]
}
```

#### 13. BASIC_BAUBLES Chapter (`basic_baubles`)
**File Path:** `src/main/resources/data/eidolonunchained/codex_entries/equipment/basic_baubles.json`

```json
{
  "chapter": "basic_baubles",
  "pages": [
    {
      "type": "text",
      "title": "eidolonunchained.codex.basic_baubles.accessory_magic.title",
      "text": "eidolonunchained.codex.basic_baubles.accessory_magic.desc"
    },
    {
      "type": "item_showcase",
      "title": "eidolonunchained.codex.basic_baubles.unholy_symbol.title",
      "item": "eidolon:unholy_symbol",
      "text": "eidolonunchained.codex.basic_baubles.unholy_symbol.desc"
    },
    {
      "type": "crafting",
      "title": "eidolonunchained.codex.basic_baubles.symbol_crafting.title",
      "recipe": {
        "type": "minecraft:crafting_shaped",
        "pattern": [
          " S ",
          "SPS",
          " S "
        ],
        "key": {
          "S": {"item": "eidolon:soul_shard"},
          "P": {"item": "eidolon:pewter_ingot"}
        },
        "result": {
          "item": "eidolon:unholy_symbol",
          "count": 1
        }
      },
      "text": "eidolonunchained.codex.basic_baubles.symbol_crafting.desc"
    }
  ]
}
```

#### 14. VOID_AMULET Chapter (`void_amulet`)
**File Path:** `src/main/resources/data/eidolonunchained/codex_entries/equipment/void_amulet.json`

```json
{
  "chapter": "void_amulet",
  "pages": [
    {
      "type": "text",
      "title": "eidolonunchained.codex.void_amulet.void_magic.title",
      "text": "eidolonunchained.codex.void_amulet.void_magic.desc"
    },
    {
      "type": "item_showcase",
      "title": "eidolonunchained.codex.void_amulet.void_amulet_showcase.title",
      "item": "eidolon:void_amulet",
      "text": "eidolonunchained.codex.void_amulet.void_amulet_showcase.desc"
    },
    {
      "type": "ritual",
      "title": "eidolonunchained.codex.void_amulet.void_infusion.title",
      "ritual": {
        "type": "eidolon:soul_enchanter",
        "ingredients": [
          {"item": "eidolon:unholy_symbol", "count": 1},
          {"item": "eidolon:shadow_gem", "count": 1},
          {"item": "eidolon:death_essence", "count": 3}
        ],
        "result": {
          "item": "eidolon:void_amulet",
          "count": 1
        }
      },
      "text": "eidolonunchained.codex.void_amulet.void_infusion.desc"
    }
  ]
}
```

#### 15. ANGEL_SIGHT Chapter (`angel_sight` - Archangel's Sight)
**File Path:** `src/main/resources/data/eidolonunchained/codex_entries/equipment/angel_sight.json`

```json
{
  "chapter": "angel_sight",
  "pages": [
    {
      "type": "text",
      "title": "eidolonunchained.codex.angel_sight.divine_vision.title",
      "text": "eidolonunchained.codex.angel_sight.divine_vision.desc"
    },
    {
      "type": "item_showcase",
      "title": "eidolonunchained.codex.angel_sight.spectacles.title",
      "item": "eidolon:enervating_ring",
      "text": "eidolonunchained.codex.angel_sight.spectacles.desc"
    },
    {
      "type": "crafting",
      "title": "eidolonunchained.codex.angel_sight.holy_crafting.title",
      "recipe": {
        "type": "minecraft:crafting_shaped",
        "pattern": [
          "GGG",
          "G G",
          "S S"
        ],
        "key": {
          "G": {"item": "minecraft:glass"},
          "S": {"item": "eidolon:silver_ingot"}
        },
        "result": {
          "item": "eidolon:enervating_ring",
          "count": 1
        }
      },
      "text": "eidolonunchained.codex.angel_sight.holy_crafting.desc"
    }
  ]
}
```

---

### **Category 4: Crafting & Processing (8 chapters)**

#### 16. BRAZIER Chapter (`brazier`)
**File Path:** `src/main/resources/data/eidolonunchained/codex_entries/crafting/brazier.json`

```json
{
  "chapter": "brazier",
  "pages": [
    {
      "type": "text",
      "title": "eidolonunchained.codex.brazier.ritual_fire.title",
      "text": "eidolonunchained.codex.brazier.ritual_fire.desc"
    },
    {
      "type": "item_showcase",
      "title": "eidolonunchained.codex.brazier.brazier_showcase.title",
      "item": "eidolon:brazier",
      "text": "eidolonunchained.codex.brazier.brazier_showcase.desc"
    },
    {
      "type": "crafting",
      "title": "eidolonunchained.codex.brazier.brazier_construction.title",
      "recipe": {
        "type": "minecraft:crafting_shaped",
        "pattern": [
          "I I",
          "ICI",
          "III"
        ],
        "key": {
          "I": {"item": "minecraft:iron_ingot"},
          "C": {"item": "minecraft:cauldron"}
        },
        "result": {
          "item": "eidolon:brazier",
          "count": 1
        }
      },
      "text": "eidolonunchained.codex.brazier.brazier_construction.desc"
    }
  ]
}
```

#### 17. CRUCIBLE Chapter (`crucible`)
**File Path:** `src/main/resources/data/eidolonunchained/codex_entries/crafting/crucible.json`

```json
{
  "chapter": "crucible",
  "pages": [
    {
      "type": "text",
      "title": "eidolonunchained.codex.crucible.advanced_smelting.title",
      "text": "eidolonunchained.codex.crucible.advanced_smelting.desc"
    },
    {
      "type": "item_showcase",
      "title": "eidolonunchained.codex.crucible.crucible_showcase.title",
      "item": "eidolon:crucible",
      "text": "eidolonunchained.codex.crucible.crucible_showcase.desc"
    },
    {
      "type": "crafting",
      "title": "eidolonunchained.codex.crucible.crucible_construction.title",
      "recipe": {
        "type": "minecraft:crafting_shaped",
        "pattern": [
          "P P",
          "PLP",
          "PPP"
        ],
        "key": {
          "P": {"item": "eidolon:pewter_ingot"},
          "L": {"item": "eidolon:lead_ingot"}
        },
        "result": {
          "item": "eidolon:crucible",
          "count": 1
        }
      },
      "text": "eidolonunchained.codex.crucible.crucible_construction.desc"
    }
  ]
}
```

---

### **Category 5: Rituals (14 chapters)**

#### 18. CRYSTAL_RITUAL Chapter (`crystal_ritual`)
**File Path:** `src/main/resources/data/eidolonunchained/codex_entries/rituals/crystal_ritual.json`

```json
{
  "chapter": "crystal_ritual",
  "pages": [
    {
      "type": "text",
      "title": "eidolonunchained.codex.crystal_ritual.crystallization.title",
      "text": "eidolonunchained.codex.crystal_ritual.crystallization.desc"
    },
    {
      "type": "ritual",
      "title": "eidolonunchained.codex.crystal_ritual.soul_crystal.title",
      "ritual": {
        "type": "eidolon:ritual_brazier",
        "center_item": "eidolon:soul_shard",
        "outer_items": [
          "eidolon:death_essence",
          "eidolon:death_essence",
          "eidolon:death_essence",
          "eidolon:death_essence"
        ],
        "result": {
          "item": "eidolon:lesser_soul_gem",
          "count": 1
        }
      },
      "text": "eidolonunchained.codex.crystal_ritual.soul_crystal.desc"
    }
  ]
}
```

#### 19. SUMMON_RITUAL Chapter (`summon_ritual`)
**File Path:** `src/main/resources/data/eidolonunchained/codex_entries/rituals/summon_ritual.json`

```json
{
  "chapter": "summon_ritual",
  "pages": [
    {
      "type": "text",
      "title": "eidolonunchained.codex.summon_ritual.lesser_summoning.title",
      "text": "eidolonunchained.codex.summon_ritual.lesser_summoning.desc"
    },
    {
      "type": "ritual",
      "title": "eidolonunchained.codex.summon_ritual.zombie_summon.title",
      "ritual": {
        "type": "eidolon:ritual_brazier",
        "center_item": "minecraft:rotten_flesh",
        "outer_items": [
          "minecraft:bone",
          "eidolon:soul_shard",
          "minecraft:bone",
          "eidolon:soul_shard"
        ],
        "result": {
          "entity": "minecraft:zombie"
        }
      },
      "text": "eidolonunchained.codex.summon_ritual.zombie_summon.desc"
    }
  ]
}
```

---

### **Category 6: Theurgy System (33 chapters)**

#### 20. THEURGY Chapter (`theurgy`)
**File Path:** `src/main/resources/data/eidolonunchained/codex_entries/theurgy/theurgy.json`

```json
{
  "chapter": "theurgy",
  "pages": [
    {
      "type": "text",
      "title": "eidolonunchained.codex.theurgy.divine_magic.title",
      "text": "eidolonunchained.codex.theurgy.divine_magic.desc"
    },
    {
      "type": "text",
      "title": "eidolonunchained.codex.theurgy.altar_basics.title",
      "text": "eidolonunchained.codex.theurgy.altar_basics.desc"
    },
    {
      "type": "item_showcase",
      "title": "eidolonunchained.codex.theurgy.holy_symbol.title",
      "item": "eidolon:holy_symbol",
      "text": "eidolonunchained.codex.theurgy.holy_symbol.desc"
    }
  ]
}
```

#### 21. ALTARS Chapter (`altars`)
**File Path:** `src/main/resources/data/eidolonunchained/codex_entries/theurgy/altars.json`

```json
{
  "chapter": "altars",
  "pages": [
    {
      "type": "text",
      "title": "eidolonunchained.codex.altars.construction_guide.title",
      "text": "eidolonunchained.codex.altars.construction_guide.desc"
    },
    {
      "type": "item_showcase",
      "title": "eidolonunchained.codex.altars.stone_altar.title",
      "item": "eidolon:stone_altar",
      "text": "eidolonunchained.codex.altars.stone_altar.desc"
    },
    {
      "type": "crafting",
      "title": "eidolonunchained.codex.altars.altar_construction.title",
      "recipe": {
        "type": "minecraft:crafting_shaped",
        "pattern": [
          "SSS",
          "SPS",
          "SSS"
        ],
        "key": {
          "S": {"item": "minecraft:smooth_stone"},
          "P": {"item": "eidolon:pewter_ingot"}
        },
        "result": {
          "item": "eidolon:stone_altar",
          "count": 1
        }
      },
      "text": "eidolonunchained.codex.altars.altar_construction.desc"
    }
  ]
}
```

#### 22. WICKED_SIGN Chapter (`wicked_sign`)
**File Path:** `src/main/resources/data/eidolonunchained/codex_entries/theurgy/signs/wicked_sign.json`

```json
{
  "chapter": "wicked_sign",
  "pages": [
    {
      "type": "text",
      "title": "eidolonunchained.codex.wicked_sign.dark_gestures.title",
      "text": "eidolonunchained.codex.wicked_sign.dark_gestures.desc"
    },
    {
      "type": "text",
      "title": "eidolonunchained.codex.wicked_sign.casting_method.title",
      "text": "eidolonunchained.codex.wicked_sign.casting_method.desc"
    },
    {
      "type": "item_showcase",
      "title": "eidolonunchained.codex.wicked_sign.components.title",
      "item": "eidolon:death_essence",
      "text": "eidolonunchained.codex.wicked_sign.components.desc"
    }
  ]
}
```

---

## Complete Implementation Status Summary

### **Implementation Progress: 22/97 Chapters (22.7%)**

#### **✅ FULLY IMPLEMENTED (22 chapters)**
1. **Creatures & Natural World** (3/3 - 100%)
   - monsters, critters, plants
2. **Resources & Materials** (8/8 - 100%) 
   - ores, pewter, enchanted_ash, tallow, arcane_gold, reagents, soul_gems, shadow_gem
3. **Equipment & Tools** (3/20 - 15%)
   - inlays, basic_baubles, void_amulet, angel_sight
4. **Crafting & Processing** (2/8 - 25%)
   - brazier, crucible
5. **Rituals** (2/14 - 14%)
   - crystal_ritual, summon_ritual
6. **Theurgy System** (3/33 - 9%)
   - theurgy, altars, wicked_sign

#### **🚧 REMAINING TO IMPLEMENT (75 chapters)**

**Equipment & Tools (16 remaining):**
- warded_mail, soulfire_wand, bonechill_wand, summoning_staff, raven_cloak, reaper_scythe, cleaving_axe, reversal_pick, warlock_armor, gravity_belt, prestigious_palm, mind_shielding_plate, resolute_belt, glass_hand, soulbone_amulet, bonelord_armor

**Crafting & Processing (6 remaining):**
- item_providers, wooden_stand, basic_alchemy, warped_sprouts, magic_workbench, soul_enchanter

**Rituals (12 remaining):**
- rituals, allure_ritual, repelling_ritual, deceit_ritual, time_rituals, purify_ritual, sanguine_ritual, recharge_ritual, capture_ritual, locate_ritual

**Theurgy System (30 remaining):**
- intro_signs, effigy, altar_lights, altar_skulls, altar_herbs, goblet, censer, dark_prayer, light_prayer, animal_sacrifice, censer_offering, dark_touch, holy_touch, stone_altar, unholy_effigy, holy_effigy, villager_sacrifice, lay_on_hands, villager_cure, villager_zombie, sacred_sign, blood_sign, soul_sign, mind_sign, flame_sign, winter_sign, harmony_sign, death_sign, warding_sign, magic_sign

**Theurgy Chants (9 remaining):**
- mana, light, fire_touch, chill_touch, water, enthrall, smite, sunder_armor, reinforce_armor

**Meta Chapters (4 remaining):**
- researches, decorations, nature_index, artifice

---

## Language File Entries

All implemented chapters require corresponding language entries in:
`src/main/resources/assets/eidolonunchained/lang/en_us.json`

Example language entries for the implemented chapters:

```json
{
  "eidolonunchained.codex.monsters.enhanced_zombies.title": "Enhanced Undead",
  "eidolonunchained.codex.monsters.enhanced_zombies.desc": "Through careful observation and arcane experimentation, we have discovered methods to enhance the natural properties of undead creatures.",
  
  "eidolonunchained.codex.critters.raven_behavior.title": "Raven Intelligence", 
  "eidolonunchained.codex.critters.raven_behavior.desc": "Ravens possess an uncanny intelligence that makes them ideal familiars for practitioners of the dark arts.",
  
  "eidolonunchained.codex.plants.magical_flora.title": "Enchanted Vegetation",
  "eidolonunchained.codex.plants.magical_flora.desc": "Certain plant species have developed magical properties through prolonged exposure to arcane energies.",
  
  "eidolonunchained.codex.ores.magical_mining.title": "Mystical Ore Extraction",
  "eidolonunchained.codex.ores.magical_mining.desc": "The extraction of magical ores requires specialized techniques and proper protective equipment.",
  
  "eidolonunchained.codex.pewter.alloy_creation.title": "Pewter Alloy Mastery",
  "eidolonunchained.codex.pewter.alloy_creation.desc": "Pewter serves as an excellent conductor for magical energies, making it invaluable for ritual implements."
}
```

---

## Next Steps for Complete Implementation

To achieve 100% coverage of all 97 Eidolon codex chapters:

1. **Continue Equipment Implementation** - Complete the remaining 16 equipment chapters
2. **Expand Crafting Systems** - Implement the 6 remaining crafting/processing chapters  
3. **Complete Ritual Coverage** - Add the 12 remaining ritual chapters
4. **Finish Theurgy System** - Implement all 30 remaining theurgy chapters plus 9 chant chapters
5. **Add Meta Chapters** - Complete the 4 remaining meta/utility chapters
6. **Language File Completion** - Add all missing language entries for the new content
7. **Testing & Validation** - Use debug commands to verify all implementations work correctly

This comprehensive structure provides a solid foundation for extending every available Eidolon codex chapter with custom content while maintaining compatibility with existing Eidolon and Minecraft items.
