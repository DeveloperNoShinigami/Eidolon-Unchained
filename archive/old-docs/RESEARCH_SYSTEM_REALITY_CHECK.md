# 🔬 Eidolon Unchained Research System - Current State & Future Expansion Plan

## 📋 **CURRENT WORKING SYSTEM (Reality Check)**

### ✅ **What's Actually Implemented & Working:**

#### **Core Infrastructure:**
- `ResearchEntry.java` - Basic research data structure
- `ResearchChapter.java` - Chapter organization
- `ResearchDataManager.java` - JSON loading from datapacks
- `EidolonResearchIntegration.java` - Injection into Eidolon's system

#### **Current Research Schema (Working):**
```json
{
  "target_research": "eidolon:soul_sorcery",
  "research_id": "eidolonunchained:advanced_soul_manipulation", 
  "title": "Advanced Soul Manipulation",
  "description": "Master the deepest secrets of soul manipulation.",
  "required_stars": 5,
  "special_tasks": [
    "Perform 50 soul enchantments"
  ],
  "tasks": {
    "tier_1": [{"type": "kill_entities", "entity": "minecraft:zombie", "count": 100}],
    "tier_2": [{"type": "craft_items", "item": "eidolon:soul_gem", "count": 10}],
    "tier_3": [{"type": "use_ritual", "ritual": "eidolon:summon_wraith", "count": 5}]
  }
}
```

#### **Supported Task Types (Confirmed Working):**
1. **`kill_entities`** - Kill specific mobs with count
2. **`craft_items`** - Craft specific items with count  
3. **`use_ritual`** - Perform rituals with count
4. **`collect_items`** - Gather materials (basic implementation)

#### **Research Types Available:**
- `BASIC` - 0 stars
- `ADVANCED` - 1 star
- `FORBIDDEN` - 2 stars  
- `RITUAL` - 1 star
- `CRAFTING` - 0 stars

#### **Integration Features:**
- Extends Eidolon's base research tree ✅
- Uses reflection to inject into `Researches` registry ✅
- Loads from datapacks automatically ✅
- Basic chapter organization ✅
- Prerequisite system (basic) ✅

---

## 🚀 **FUTURE EXPANSION ROADMAP**

### **Phase 1: Enhanced Task System (Immediate)**
*Build on existing foundation with more task types*

#### **New Task Types to Add:**
```json
{
  "type": "gain_experience",
  "experience_type": "player_level", 
  "amount": 30
},
{
  "type": "visit_structure",
  "structure": "eidolon:catacomb",
  "count": 3
},
{
  "type": "interact_block",
  "block": "eidolon:crucible",
  "action": "successful_recipe",
  "count": 25
},
{
  "type": "obtain_advancement",
  "advancement": "eidolon:first_ritual"
}
```

#### **Enhanced Existing Tasks:**
```json
{
  "type": "kill_entities",
  "entity": "minecraft:zombie",
  "count": 50,
  "conditions": {
    "weapon": "eidolon:athame",
    "dimension": "minecraft:overworld",
    "time": "night"
  }
}
```

### **Phase 2: Conditional Requirements (Short-term)**
*Add basic conditional gating without complex world state*

#### **Simple Conditionals:**
```json
"conditional_requirements": {
  "player_level": {"min": 30},
  "reputation": {
    "dark_deity": 25,
    "light_deity": -10
  },
  "items_in_inventory": [
    {"item": "eidolon:athame", "count": 1}
  ],
  "completed_research": [
    "eidolon:basic_necromancy",
    "eidolonunchained:soul_binding"
  ],
  "dimension": "minecraft:overworld"
}
```

#### **Research Conflicts:**
```json
"conflicts": [
  "eidolon:divine_blessing",
  "eidolon:purification_mastery"
]
```

### **Phase 3: Enhanced Rewards System (Medium-term)**
*More meaningful research completion rewards*

#### **Enhanced Rewards:**
```json
"rewards": {
  "knowledge_points": 100,
  "reputation": {
    "dark_deity": 15,
    "light_deity": -5
  },
  "items": [
    {
      "item": "eidolon:soul_gem",
      "count": 3,
      "nbt": "{Enhanced:true}"
    }
  ],
  "unlock_recipes": [
    "eidolonunchained:advanced_athame"
  ],
  "unlock_research": [
    "eidolonunchained:master_necromancer"
  ]
}
```

### **Phase 4: Dynamic Conditions (Long-term)**
*Complex world-state dependent research*

#### **World State Conditionals:**
```json
"advanced_conditions": {
  "time_requirements": {
    "time_of_day": "night",
    "moon_phase": "new_moon"
  },
  "location_requirements": {
    "structure_nearby": "eidolon:catacomb",
    "biome": "minecraft:swamp",
    "light_level": {"max": 7}
  },
  "world_state": {
    "weather": "thunderstorm",
    "difficulty": "hard"
  }
}
```

### **Phase 5: Research Tree Visualization (Long-term)**
*Better UI integration*

#### **Features:**
- Custom research GUI overlays
- Progress tracking visualization  
- Interactive research tree browser
- Research goal suggestions

---

## 🛠 **IMPLEMENTATION PLAN**

### **Step 1: Expand Task System (Week 1-2)**

1. **Add New Task Types:**
   - Extend `ResearchTask` enum
   - Create task validators for each type
   - Update JSON parsing in `ResearchDataManager`

2. **Enhance Existing Tasks:**
   - Add `conditions` object to existing task types
   - Implement basic condition checking (weapon, dimension, etc.)

### **Step 2: Basic Conditionals (Week 3-4)**

1. **Implement Requirement System:**
   - Create `ResearchRequirement` class
   - Add requirement checking to research availability
   - Integrate with Eidolon's reputation system

2. **Add Research Conflicts:**
   - Track completed research globally
   - Implement mutual exclusion logic

### **Step 3: Enhanced Rewards (Week 5-6)**

1. **Expand Reward Types:**
   - Item rewards with NBT support
   - Recipe unlocking integration
   - Research chain unlocking

2. **Reputation Integration:**
   - Hook into Eidolon's deity system
   - Add reputation-based unlocks

### **Step 4: Advanced Features (Month 2+)**

1. **World State Integration:**
   - Moon phase detection
   - Weather condition checking
   - Time-based requirements

2. **Location-Based Research:**
   - Structure proximity detection
   - Biome-specific research
   - Dimension requirements

---

## 📊 **REALISTIC EXAMPLES** 
*Based on current capabilities + planned expansions*

### **Current Capability (Working Now):**
```json
{
  "research_id": "eidolonunchained:ritual_specialist",
  "title": "Ritual Specialist",
  "description": "Master multiple ritual types through practice",
  "required_stars": 3,
  "tasks": {
    "tier_1": [
      {"type": "use_ritual", "ritual": "eidolon:crystal_ritual", "count": 10},
      {"type": "kill_entities", "entity": "minecraft:skeleton", "count": 50}
    ],
    "tier_2": [
      {"type": "craft_items", "item": "eidolon:ritual_brazier", "count": 3},
      {"type": "use_ritual", "ritual": "eidolon:summon_ritual", "count": 5}
    ]
  }
}
```

### **Phase 1 Enhancement (Near Future):**
```json
{
  "research_id": "eidolonunchained:catacomb_explorer", 
  "title": "Catacomb Explorer",
  "description": "Explore ancient burial sites and master their secrets",
  "required_stars": 2,
  "tasks": {
    "tier_1": [
      {"type": "visit_structure", "structure": "eidolon:catacomb", "count": 5},
      {"type": "kill_entities", "entity": "minecraft:zombie", "count": 100, 
       "conditions": {"location": "structure:eidolon:catacomb"}}
    ],
    "tier_2": [
      {"type": "gain_experience", "experience_type": "player_level", "amount": 25},
      {"type": "interact_block", "block": "eidolon:brazier", "action": "light", "count": 10}
    ]
  }
}
```

### **Phase 2 Enhancement (Short-term Future):**
```json
{
  "research_id": "eidolonunchained:dark_path_initiate",
  "title": "Dark Path Initiate", 
  "description": "Begin the journey into forbidden knowledge",
  "required_stars": 4,
  "conditional_requirements": {
    "reputation": {"dark_deity": 15, "light_deity": -5},
    "completed_research": ["eidolon:basic_necromancy"],
    "items_in_inventory": [{"item": "eidolon:athame", "count": 1}]
  },
  "conflicts": ["eidolon:divine_blessing"],
  "tasks": {
    "tier_1": [
      {"type": "kill_entities", "entity": "minecraft:villager", "count": 10,
       "conditions": {"weapon": "eidolon:athame", "time": "night"}},
      {"type": "use_ritual", "ritual": "eidolon:dark_ritual", "count": 3}
    ]
  },
  "rewards": {
    "reputation": {"dark_deity": 10},
    "unlock_research": ["eidolonunchained:necromantic_mastery"]
  }
}
```

---

## 📈 **SUCCESS METRICS**

### **Phase 1 Goals:**
- [ ] 5 new task types implemented
- [ ] Enhanced conditions for existing tasks
- [ ] Backward compatibility maintained

### **Phase 2 Goals:**
- [ ] Basic conditional requirements working
- [ ] Research conflict system active
- [ ] Reputation integration functional

### **Phase 3+ Goals:**
- [ ] Dynamic world-state conditions
- [ ] Complex research trees
- [ ] Full GUI integration

This roadmap builds systematically on what's **actually working** while creating a path toward the advanced features I described earlier!
