# 📁 Eidolon Unchained Datapack Structure

See [Codex Tutorial](codex_tutorial.md) for a guided walkthrough and [Codex Reference](codex_reference.md) for full details.

## 🎯 **Complete Datapack Layout**

```
📦 data/
├── 📁 eidolonunchained/                    # Your mod namespace
│   ├── 📁 codex_chapters/                  # Optional new chapter definitions
│   │   └── 📄 mythology.json               # Defines a new chapter
│   │
│   ├── 📁 codex_entries/                   # Codex system extensions
│   │   ├── 📄 void_amulet_advanced.json    # Extends VOID_AMULET chapter
│   │   ├── 📄 ritual_mastery.json          # Extends SUMMON_RITUAL chapter
│   │   ├── 📄 crystal_techniques.json      # Extends CRYSTAL_RITUAL chapter
│   │   ├── 📄 blood_magic_secrets.json     # Extends SANGUINE_RITUAL chapter
│   │   ├── 📄 time_manipulation.json       # Extends TIME_RITUALS chapter
│   │   ├── 📄 monster_studies.json         # Extends MONSTERS chapter
│   │   └── 📄 pewter_mastery.json          # Extends PEWTER chapter
│   │
│   └── 📁 research_entries/                # Research system extensions
│       ├── 📄 advanced_void_research.json  # New void research line
│       ├── 📄 ritual_master.json           # Advanced ritual research
│       ├── 📄 blood_scholar.json           # Blood magic research
│       ├── 📄 time_mage.json               # Time magic research
│       └── 📄 monster_hunter.json          # Monster research
│
├── 📁 yourmodpack/                         # Other mods can also use this!
│   ├── 📁 codex_entries/
│   │   └── 📄 modpack_lore.json
│   └── 📁 research_entries/
│       └── 📄 modpack_challenges.json
│
└── 📁 minecraft/                           # Even vanilla namespace works
    └── 📁 codex_entries/
        └── 📄 vanilla_integration.json
```

## 📖 **Codex Entry Template**

```json
{
  "target_chapter": "CHAPTER_NAME",          // ← Eidolon field name or custom ID
  "pages": [
    {
      "type": "title",                       // ← Page types: title, text, crafting
      "title": "Your Title",
      "text": "Your description"
    },
    {
      "type": "text", 
      "title": "Chapter Title",
      "text": "Detailed explanation of the topic..."
    },
    {
      "type": "crafting",
      "recipe": "modid:item_id",             // ← Recipe resource location
      "title": "Recipe Title", 
      "text": "Recipe description"
    }
  ]
}
```

## 🗂️ **Codex Chapter Template**

```json
{
  "title": "yourmod.codex.chapter.mythology",
  "icon": "minecraft:book"
}
```

## 🔬 **Research Entry Template**

```json
{
  "target_research": "eidolon:existing_research_id",  // ← Research to extend
  "research_id": "yourmod:new_research_id",           // ← Your research ID
  "title": "Research Title",
  "description": "Research description",
  "required_stars": 3,                               // ← Stars needed to unlock; defaults by type if omitted
  "conditions": {                                    // ← Optional gating
    "dimension": "minecraft:the_nether"
  },
  "special_tasks": [                                 // ← Special requirements
    "Kill 100 zombies with void magic"
  ],
  "tasks": {                                         // ← Progression tasks
    "tier_1": [
      {
        "type": "kill_entities",
        "entity": "minecraft:zombie", 
        "count": 50
      }
    ],
    "tier_2": [
      {
        "type": "craft_items",
        "item": "eidolon:void_amulet",
        "count": 5
      }
    ]
  }
}
```

Conditions act as prerequisites for the entry; tasks only begin tracking once all conditions are satisfied.

## 🎯 **Available Eidolon Chapters**
*(Use these exact names for built-in chapters or a namespaced ID for custom ones)*

### **🏺 Artifacts & Items**
- `"VOID_AMULET"`        - Void amulet crafting and uses
- `"PEWTER"`             - Pewter alloy and tools
- `"ENCHANTED_ASH"`      - Enchanted ash applications
- `"DECORATIONS"`        - Decorative items

### **🔮 Rituals** 
- `"CRYSTAL_RITUAL"`     - Crystal-based rituals
- `"SUMMON_RITUAL"`      - Summoning creatures
- `"ALLURE_RITUAL"`      - Attraction/charm rituals
- `"REPELLING_RITUAL"`   - Protection rituals
- `"DECEIT_RITUAL"`      - Illusion/deception magic
- `"TIME_RITUALS"`       - Time manipulation
- `"PURIFY_RITUAL"`      - Cleansing rituals
- `"SANGUINE_RITUAL"`    - Blood magic rituals
- `"RECHARGE_RITUAL"`    - Energy restoration
- `"CAPTURE_RITUAL"`     - Soul/essence capture

### **🌿 Natural Elements**
- `"MONSTERS"`           - Creature information
- `"CRITTERS"`           - Small creatures
- `"ORES"`               - Magical ores and mining
- `"PLANTS"`             - Magical plants and herbs

### **⚙️ Mechanics**
- `"RESEARCHES"`         - Research system info
- `"BRAZIER"`            - Brazier crafting and use
- `"ITEM_PROVIDERS"`     - Item generation mechanics

## 🌟 **Research Task Types**

```json
"tasks": {
  "tier_1": [
    {
      "type": "kill_entities",
      "entity": "minecraft:zombie",
      "count": 100
    },
    {
      "type": "craft_items", 
      "item": "eidolon:soul_gem",
      "count": 10
    },
    {
      "type": "collect_items",
      "item": "minecraft:diamond", 
      "count": 20
    },
    {
      "type": "use_ritual",
      "ritual": "eidolon:summon_wraith",
      "count": 5
    },
    {
      "type": "explore_biomes",
      "biome": "minecraft:dark_forest",
      "count": 1
    }
  ]
}
```

### Available Task Types

| Type | Required Fields | Description |
|------|----------------|-------------|
| `kill_entities` | `entity`, `count` | Kill a specific entity a number of times. |
| `craft_items` | `item`, `count` | Craft the given item the specified number of times. |
| `use_ritual` | `ritual`, `count` | Perform a ritual a certain number of times. |
| `collect_items` | `item`, `count` | Gather items and submit them to the research table. |

Example for each type:

```json
{ "type": "kill_entities", "entity": "minecraft:zombie", "count": 5 }
{ "type": "craft_items",   "item": "eidolon:soul_gem",     "count": 3 }
{ "type": "use_ritual",    "ritual": "eidolon:summon_wraith", "count": 2 }
{ "type": "collect_items", "item": "minecraft:diamond",    "count": 10 }
```

## 🔄 **How It Works**

1. **Codex System**: Your JSON files extend existing Eidolon chapters with new pages
2. **Research System**: Your JSON files create new research entries that fit into Eidolon's progression
3. **Multiple Namespaces**: Any mod/datapack can add entries using this structure
4. **Auto-Loading**: Both systems automatically load on server start and resource reload

## 🚀 **Example Usage**

Create a datapack in your world: `saves/YourWorld/datapacks/eidolon_extensions/`

Then use the same folder structure shown above! 📁✨
