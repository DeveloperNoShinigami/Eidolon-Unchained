# 📁 Eidolon Unchained Datapack Structure

## 🎯 **Complete Datapack Layout**

```
📦 data/
├── 📁 eidolonunchained/                    # Your mod namespace
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
  "target_chapter": "CHAPTER_NAME",          // ← Must match Eidolon's field name
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

## 🔬 **Research Entry Template**

```json
{
  "target_research": "eidolon:existing_research_id",  // ← Research to extend
  "research_id": "yourmod:new_research_id",           // ← Your research ID
  "title": "Research Title",
  "description": "Research description",
  "required_stars": 3,                               // ← Stars needed to unlock
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

## 🎯 **Available Eidolon Chapters** 
*(Use these exact names for `target_chapter`)*

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

## 🔄 **How It Works**

1. **Codex System**: Your JSON files extend existing Eidolon chapters with new pages
2. **Research System**: Your JSON files create new research entries that fit into Eidolon's progression
3. **Multiple Namespaces**: Any mod/datapack can add entries using this structure
4. **Auto-Loading**: Both systems automatically load on server start and resource reload

## 🚀 **Example Usage**

Create a datapack in your world: `saves/YourWorld/datapacks/eidolon_extensions/`

Then use the same folder structure shown above! 📁✨
