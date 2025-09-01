# Research and Codex Integration

**Connect research discovery with codex documentation**

## Integration Strategy

The research system and codex work together to create a seamless learning experience:

1. **Research discovers content** → Player learns about new entities/mechanics
2. **Codex provides documentation** → Player gets detailed information  
3. **Progressive revelation** → Advanced content unlocks naturally

## Linking Research to Codex Entries

### Method 1: Shared Entity Targeting

Both systems can target the same entities to create natural connections:

**Research File**: `data/yourmod/eidolon_research/zombie_study.json`
```json
{
  "id": "zombie_study",
  "stars": 2,
  "triggers": ["entity:minecraft:zombie"],
  "tasks": {
    "1": [{"type": "item", "item": "minecraft:rotten_flesh", "count": 5}]
  },
  "rewards": [
    {"type": "sign", "sign": "death"}
  ]
}
```

**Codex Entry**: `data/yourmod/codex_entries/undead_anatomy.json`
```json
{
  "name": "undead_anatomy",
  "category": "entities",
  "targets": ["entity:minecraft:zombie"],
  "pages": [
    {"type": "title", "title": "Undead Anatomy", "subtitle": "Understanding the Shambling Dead"},
    {"type": "text", "text": "Zombies are reanimated corpses driven by dark magic..."},
    {"type": "entity", "entity": "minecraft:zombie"}
  ]
}
```

### Method 2: Item-Based Connections

Research rewards can reference items featured in codex entries:

**Research rewards rare item** → **Codex explains how to use it**

```json
{
  "id": "soul_gem_discovery",
  "rewards": [
    {"type": "item", "item": "eidolon:soul_gem", "count": 1}
  ]
}
```

**Codex entry for soul gems**:
```json
{
  "name": "soul_gem_usage", 
  "targets": ["item:eidolon:soul_gem"],
  "pages": [
    {"type": "title", "title": "Soul Gems", "subtitle": "Capturing Ethereal Essence"},
    {"type": "text", "text": "Soul gems can capture and store spiritual energy..."}
  ]
}
```

## Progressive Discovery Examples

### Beginner Path: Basic Undead
```
1. Kill Zombie → Zombie Research → Death Sign
2. Auto-discover "Zombie Anatomy" codex entry
3. Learn about undead weaknesses and behavior
```

### Intermediate Path: Necromancy
```  
1. Kill Skeleton → Skeleton Research → Death Sign + Bone
2. Auto-discover "Bone Crafting" codex entry  
3. Learn bone tool recipes and uses
4. Craft bone wand → Auto-discover "Basic Necromancy" entry
```

### Advanced Path: Soul Magic
```
1. Kill Wraith → Soul Research → Soul Sign + Soul Gem
2. Auto-discover "Soul Manipulation" codex entry
3. Learn soul gem mechanics and recipes
4. Use soul in ritual → Auto-discover "Advanced Rituals" entry
```

## Content Categories

Organize related research and codex entries by theme:

### Death Magic Theme
- **Research**: `zombie_study`, `skeleton_research`, `wraith_encounter`
- **Codex**: `undead_anatomy`, `bone_crafting`, `soul_manipulation`
- **Progression**: Basic undead → Bone tools → Soul magic

### Fire Magic Theme  
- **Research**: `blaze_study`, `fire_elemental`, `phoenix_sighting`
- **Codex**: `elemental_theory`, `flame_control`, `fire_rituals`
- **Progression**: Basic fire → Elemental binding → Advanced pyromancy

### Nature Magic Theme
- **Research**: `treant_encounter`, `fae_interaction`, `grove_discovery`  
- **Codex**: `plant_communion`, `nature_spirits`, `druidic_practices`
- **Progression**: Plant magic → Spirit communication → Druidic mastery

## File Organization

Keep related files together for easier maintenance:

```
data/yourmod/
├── eidolon_research/
│   ├── undead/
│   │   ├── zombie_study.json
│   │   ├── skeleton_research.json
│   │   └── wraith_encounter.json
│   ├── elemental/
│   │   ├── blaze_study.json
│   │   └── fire_elemental.json
│   └── nature/
│       ├── treant_encounter.json
│       └── grove_discovery.json
└── codex_entries/
    ├── undead/
    │   ├── zombie_anatomy.json
    │   ├── bone_crafting.json
    │   └── soul_manipulation.json
    ├── elemental/
    │   ├── elemental_theory.json
    │   └── flame_control.json
    └── nature/
        ├── plant_communion.json
        └── nature_spirits.json
```

## Best Practices

### Research Design
1. **Start Simple** - 1-2 star research for common mobs
2. **Build Complexity** - 3-5 star research for rare encounters
3. **Meaningful Rewards** - Signs that match the content theme
4. **Reasonable Tasks** - Don't make grinding excessive

### Codex Design  
1. **Context First** - Title and text pages before complex content
2. **Visual Elements** - Use entity/item pages to show subjects
3. **Progressive Detail** - Basic concepts → Advanced techniques
4. **Cross-Reference** - Link related entries through shared targets

### Integration Timing
1. **Immediate Discovery** - Basic entries unlock with first kill
2. **Task Completion** - Advanced entries require research tasks  
3. **Item Usage** - Specialized entries unlock when using reward items
4. **Natural Flow** - Each discovery should feel earned and relevant

## Testing Your Integration

1. **Kill the target entity** - Does research trigger?
2. **Complete research tasks** - Do rewards arrive?
3. **Check codex discovery** - Does the related entry appear?
4. **Read the entry** - Does it provide useful information?
5. **Follow the progression** - Does it lead naturally to next content?

## Common Pitfalls

❌ **Research without context** - Giving signs with no explanation  
✅ **Research with codex backup** - Signs + entries explaining their use

❌ **Disconnected systems** - Research and codex covering different topics
✅ **Integrated themes** - Both systems supporting the same content areas

❌ **Information overload** - Too many discoveries at once
✅ **Paced revelation** - Gradual unlocking of related content

❌ **Dead-end content** - Research that doesn't lead anywhere
✅ **Progressive pathways** - Each discovery opening new possibilities
