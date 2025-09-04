# Task System Templates

This document defines the templated, flexible task system that can be applied to any deity in Eidolon Unchained.

## Core Template Structure

### Task Configuration Template
```json
{
  "task_config": {
    "enabled": true,
    "max_active_tasks": 3,
    "task_assignment_behavior": {
      "auto_assign_probability": 0.4,
      "min_reputation_for_auto_assign": 3,
      "conversation_triggers": [
        "first_conversation",
        "reputation_milestone", 
        "completed_previous_task",
        "{deity_theme}_prayer",
        "{deity_theme}_communion"
      ],
      "cooldown_between_assignments_hours": 24
    },
    
    "available_tasks": [
      // Array of tasks following the task template
    ],
    
    "progression_chains": {
      "{deity_theme}_main_path": [],
      "{deity_theme}_exploration_path": [],
      "{deity_theme}_mastery_unlocks": {}
    },
    
    "ai_integration": {
      "task_suggestion_prompts": {
        "no_active_tasks": "This {follower_title} seeks purpose in serving {deity_domain}. Consider offering them a {task_type} that matches their devotion level ({reputation}) and their understanding of {deity_knowledge}.",
        "task_available": "A {task_nature} awaits this devoted follower: {task_description}. Present this {opportunity_type} with the {deity_speaking_style}.",
        "task_completion": "Celebrate their completion of '{completed_task}' with {celebration_emotion}. Their success brings more {deity_influence} into the world. Guide them toward their next step in {advancement_path} if they are ready.",
        "progression_milestone": "This {follower_descriptor} soul has grown in {power_type} from {old_tier} to {new_tier}. Their {advancement_type} deserves recognition and greater {responsibility_type}."
      },
      "context_integration": {
        "include_active_tasks_in_context": true,
        "include_completed_tasks_count": true,
        "include_available_task_hints": true,
        "task_context_format": "{deity_name} Tasks - Active: {active_tasks} | Completed: {completed_count} {deed_type} | Ready for: {available_tasks}",
        "progression_context_format": "{deity_name} Progression: {current_tier} | {path_name}: {unlocked_progressions}"
      }
    }
  }
}
```

## Task Template Categories

### 1. Collection Tasks
**Template**: Gather resources that align with deity's domain
```json
{
  "task_id": "{deity_theme}_gather_{resource_type}",
  "display_name": "Gather {Resource_Name}",
  "description": "Collect {resource_description} that resonate with {deity_domain}",
  "progression_tier": "initiate",
  "requirements": [
    {
      "type": "collect_items",
      "items": [
        {
          "item": "{resource_item}",
          "count": "{configurable_count}",
          "source": "{collection_method}",
          "time_requirement": "{deity_preferred_time}",
          "display_name": "{themed_resource_name}"
        }
      ]
    }
  ]
}
```

### 2. Combat/Interaction Tasks
**Template**: Defeat or interact with entities
```json
{
  "task_id": "{deity_theme}_combat_{entity_type}",
  "display_name": "Defeat {Entity_Name}",
  "description": "Use {deity_power} to overcome {entity_description}",
  "progression_tier": "acolyte",
  "requirements": [
    {
      "type": "kill_entities",
      "entities": [
        {
          "entity_type": "{target_entity}",
          "count": "{configurable_count}",
          "time_requirement": "{deity_preferred_time}",
          "weapon_type": "{deity_preferred_weapon}",
          "display_name": "{themed_entity_name}"
        }
      ]
    }
  ]
}
```

### 3. Ritual Tasks
**Template**: Perform deity-specific rituals
```json
{
  "task_id": "{deity_theme}_ritual_{ritual_type}",
  "display_name": "Perform the Rite of {Ritual_Name}",
  "description": "Conduct the {ritual_description} to prove your mastery of {deity_arts}",
  "progression_tier": "priest",
  "requirements": [
    {
      "type": "complete_ritual",
      "rituals": [
        {
          "ritual_id": "eidolonunchained:{deity_theme}_patronage_ritual",
          "count": 1,
          "success_required": true,
          "time_requirement": "{deity_preferred_time}",
          "location_requirement": "{deity_preferred_location}"
        }
      ]
    }
  ]
}
```

### 4. Crafting Tasks
**Template**: Create items using deity-specific methods
```json
{
  "task_id": "{deity_theme}_crafting_{item_type}",
  "display_name": "Forge {Item_Name}",
  "description": "Create {item_description} using the {crafting_method} of {deity_domain}",
  "progression_tier": "high_priest",
  "requirements": [
    {
      "type": "complete_crucible_recipe",
      "recipes": [
        {
          "recipe_id": "eidolonunchained:{deity_theme}_essence",
          "count": 1,
          "success_required": true,
          "time_requirement": "{deity_preferred_time}",
          "location_requirement": "{deity_preferred_location}"
        }
      ]
    }
  ]
}
```

### 5. Exploration Tasks
**Template**: Visit locations that align with deity's nature
```json
{
  "task_id": "{deity_theme}_exploration_{location_type}",
  "display_name": "Explore {Location_Name}",
  "description": "Journey to places where {deity_influence} is strongest",
  "progression_tier": "wanderer",
  "requirements": [
    {
      "type": "visit_biomes",
      "biomes": [
        {
          "biome": "{deity_preferred_biome}",
          "duration_seconds": 180,
          "time_requirement": "{deity_preferred_time}",
          "display_name": "{themed_location_name}"
        }
      ]
    }
  ]
}
```

## Deity Configuration Variables

### Core Theme Variables
- `{deity_theme}`: Short identifier (e.g., "nature", "light", "dark", "fire", "water")
- `{deity_name}`: Full deity name (e.g., "Verdania", "Lyralei", "Nyxathel")
- `{deity_domain}`: Deity's area of influence (e.g., "nature", "light", "shadows")
- `{deity_influence}`: What the deity brings to the world (e.g., "growth", "radiance", "darkness")

### Personality Variables
- `{deity_speaking_style}`: How the deity communicates (e.g., "warmth of nature", "divine love", "majesty of shadow dominion")
- `{follower_title}`: How the deity addresses followers (e.g., "faithful soul", "devoted follower", "ambitious soul")
- `{celebration_emotion}`: How the deity celebrates (e.g., "radiant joy", "warm satisfaction", "dark satisfaction")

### Mechanical Variables
- `{deity_preferred_time}`: When deity is strongest ("day", "night", "any")
- `{deity_preferred_location}`: Where deity prefers rituals ("surface", "underground", "any")
- `{deity_preferred_weapon}`: Weapon type aligned with deity (optional)
- `{deity_preferred_biome}`: Biomes that resonate with deity

### Progression Variables
- `{power_type}`: Type of power gained (e.g., "divine grace", "natural wisdom", "shadow power")
- `{advancement_type}`: Type of advancement (e.g., "spiritual advancement", "dark advancement")
- `{responsibility_type}`: What greater responsibility means (e.g., "responsibility", "forbidden knowledge")

## Template Application Examples

### Fire Deity Template Application
```json
{
  "deity_theme": "fire",
  "deity_name": "Ignis",
  "deity_domain": "flames and forge",
  "deity_influence": "heat and creation",
  "deity_speaking_style": "crackling intensity of flame",
  "follower_title": "flame-bearer",
  "celebration_emotion": "blazing pride",
  "deity_preferred_time": "day",
  "deity_preferred_location": "surface",
  "deity_preferred_biome": "minecraft:desert",
  "power_type": "fiery power",
  "advancement_type": "blazing advancement"
}
```

### Water Deity Template Application  
```json
{
  "deity_theme": "water",
  "deity_name": "Aquaria",
  "deity_domain": "seas and storms",
  "deity_influence": "flow and change",
  "deity_speaking_style": "flowing wisdom of tides",
  "follower_title": "tide-walker", 
  "celebration_emotion": "flowing satisfaction",
  "deity_preferred_time": "any",
  "deity_preferred_location": "surface",
  "deity_preferred_biome": "minecraft:ocean",
  "power_type": "tidal power",
  "advancement_type": "flowing advancement"
}
```

## Implementation Benefits

1. **Scalability**: New deities can be added by filling in template variables
2. **Consistency**: All deities follow the same structural patterns
3. **Maintainability**: Changes to the template system apply to all deities
4. **Flexibility**: Each deity can still have unique characteristics within the template
5. **Automation**: Template application can be automated with variable substitution

## Template Validation Rules

1. All `{variable}` placeholders must be defined in deity configuration
2. Task IDs must follow pattern: `{deity_theme}_{task_category}_{specific_name}`
3. Progression chains must be logically ordered
4. AI integration prompts must include all required context variables
5. Requirements must be achievable within the deity's thematic constraints
