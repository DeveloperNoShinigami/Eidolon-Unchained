# Universal Deity Configuration Template

This template provides a flexible, variable-driven system for creating any deity configuration with all integrated systems including AI, tasks, rituals, and crucible recipes.

## Core Template Variables

### Required Base Variables
```json
{
  "deity_id": "{theme}_deity",
  "deity_name": "{name}",
  "deity_title": "{title}",
  "deity_domains": "{domains}",
  "deity_theme": "{theme}",
  "deity_color": "{color}",
  "personality_description": "{personality}",
  "speaking_style": "{speaking_style}",
  "follower_relationship": "{relationship}",
  "primary_domain": "{primary_domain}",
  "secondary_domain": "{secondary_domain}",
  "preferred_time": "{time_preference}",
  "preferred_environment": "{environment}",
  "primary_biome": "{biome_1}",
  "secondary_biome": "{biome_2}",
  "tertiary_biome": "{biome_3}",
  "primary_effect": "{effect_1}",
  "secondary_effect": "{effect_2}",
  "communion_effect": "{communion_effect}",
  "blessing_effect": "{blessing_effect}",
  "task_resource_type": "{resource_type}",
  "task_entity_type": "{entity_type}",
  "ritual_type": "{ritual_type}",
  "crucible_item_type": "{crucible_item}",
  "exploration_location": "{location_type}"
}
```

## Universal Deity Configuration Template

```json
{
  "deity": "eidolonunchained:{deity_id}",
  "ai_provider": "player2ai",
  "item_context_id": "",
  "personality": "You are {deity_name}, {deity_title}, a powerful deity of {deity_domains}. {personality_description} You speak with {speaking_style}. {follower_relationship}",
  
  "behavior_rules": {
    "reputation_thresholds": {
      "0": "You are cautious with this newcomer and speak with measured formality.",
      "25": "You acknowledge their growing dedication with {deity_theme}ic approval.",
      "50": "You treat them as a trusted follower worthy of your guidance.",
      "75": "You speak to them as a valued servant of your {primary_domain}.",
      "100": "You address them as your ultimate champion, worthy of your greatest power and deepest secrets."
    },
    "research_requirements": {
      "0": "You share only basic knowledge of {primary_domain}.",
      "5": "You reveal deeper mysteries of {secondary_domain}.",
      "10": "You grant access to advanced {deity_theme} magic.",
      "15": "You unveil the most sacred secrets of {deity_domains}."
    },
    "curses": {
      "low_reputation": "You curse those who displease you with weakness and confusion.",
      "enemy_patron": "You inflict severe curses upon followers of opposing deities.",
      "disrespectful": "Those who show disrespect receive your wrath."
    },
    "blessings": {
      "high_reputation": "You bestow powerful {deity_theme} blessings upon your faithful.",
      "preferred_time": "Your blessings are strongest during {preferred_time}.",
      "preferred_environment": "In {preferred_environment}, your blessings carry extra potency."
    },
    "gifts": {
      "special_occasion": "On rare occasions, you grant legendary {deity_theme} artifacts.",
      "major_milestone": "Achieving major progression milestones earns unique gifts.",
      "exceptional_service": "Exceptional devotion is rewarded with rare {task_resource_type} and essences."
    },
    "dynamic_responses": {
      "time_of_day": {
        "{preferred_time}": "You are at your strongest during {preferred_time} and speak with enhanced power.",
        "opposite_time": "Though {opposite_time} weakens your presence, your authority remains absolute."
      },
      "biome": {
        "{primary_biome}": "This realm resonates perfectly with your {deity_theme}ic essence.",
        "{secondary_biome}": "This environment pleases you greatly.",
        "{tertiary_biome}": "The {deity_theme} energy here strengthens your presence."
      }
    }
  },
  
  "prayer_configs": {
    "conversation": {
      "enabled": true,
      "cooldown_seconds": 300,
      "reputation_change": 1,
      "effects": [
        {
          "effect": "{primary_effect}",
          "duration": 300,
          "amplifier": 0
        }
      ]
    },
    "{deity_theme}_communion": {
      "enabled": true,
      "cooldown_seconds": 1800,
      "reputation_change": 3,
      "minimum_reputation": 10,
      "effects": [
        {
          "effect": "{communion_effect}",
          "duration": 600,
          "amplifier": 1
        },
        {
          "effect": "{secondary_effect}",
          "duration": 600,
          "amplifier": 0
        }
      ]
    },
    "{deity_theme}_blessing": {
      "enabled": true,
      "cooldown_seconds": 3600,
      "reputation_change": 5,
      "minimum_reputation": 25,
      "effects": [
        {
          "effect": "{blessing_effect}",
          "duration": 1200,
          "amplifier": 2
        }
      ]
    }
  },
  
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
      "cooldown_between_assignments_hours": 20
    },
    
    "available_tasks": [
      {
        "task_id": "{deity_theme}_gather_resources",
        "display_name": "Gather {task_resource_type}",
        "description": "Collect the essence of {primary_domain} from places where {deity_theme} energy gathers",
        "progression_tier": "initiate",
        "ai_assignment_context": {
          "trigger_conditions": {
            "min_reputation": 0,
            "max_reputation": 25,
            "required_time": ["{preferred_time}"],
            "prayer_types": ["conversation", "{deity_theme}_communion"],
            "completed_tasks": [],
            "unlocked_progressions": []
          },
          "assignment_prompt": "Young seeker of {primary_domain}, the {deity_theme} calls to you. Venture into places where {deity_theme} energy pools and gather {item_count} {task_resource_type}. This service will earn you {reputation_reward} divine favor.",
          "completion_phrases": [
            "The {deity_theme} recognizes your dedication - you have proven yourself worthy.",
            "You have gathered the essence of {primary_domain} with skill and devotion.",
            "Your collection of {task_resource_type} strengthens our divine connection."
          ]
        },
        "requirements": [
          {
            "type": "collect_items",
            "items": [
              {
                "item": "{resource_item}",
                "count": 16,
                "source": "{collection_method}",
                "time_requirement": "{preferred_time}",
                "display_name": "{deity_theme}ic {task_resource_type}"
              }
            ]
          }
        ],
        "rewards": {
          "reputation": 6,
          "commands": [
            "give {player} {reward_item_1} {count_1}",
            "effect give {player} {primary_effect} 600 0",
            "tellraw {player} {\"text\":\"✦ The {deity_theme} flows through you - {primary_domain} bends to your will\",\"color\":\"{deity_color}\"}"
          ],
          "progression_unlock": "{deity_theme}_gatherer"
        },
        "cooldown_hours": 20,
        "repeatable": false
      },
      
      {
        "task_id": "{deity_theme}_master_entities",
        "display_name": "Master the {task_entity_type}",
        "description": "Use your {deity_theme}ic powers to interact with creatures of {secondary_domain}",
        "progression_tier": "acolyte",
        "ai_assignment_context": {
          "trigger_conditions": {
            "min_reputation": 15,
            "max_reputation": 45,
            "prayer_types": ["{deity_theme}_communion", "conversation"],
            "completed_tasks": ["{deity_theme}_gather_resources"],
            "unlocked_progressions": ["{deity_theme}_gatherer"]
          },
          "assignment_prompt": "Devoted acolyte, the {task_entity_type} respond to your growing power. Use your {deity_theme}ic abilities to interact with {entity_count} of these creatures. This mastery will grant you {reputation_reward} blessed favor.",
          "completion_phrases": [
            "The {task_entity_type} now recognize your {deity_theme}ic authority.",
            "Your mastery over {secondary_domain} creatures grows ever stronger.",
            "You have proven your dominion over the beings of {deity_domains}."
          ]
        },
        "requirements": [
          {
            "type": "interact_entities",
            "entities": [
              {
                "entity_type": "{target_entity}",
                "count": 5,
                "interaction_type": "{interaction_method}",
                "time_requirement": "{preferred_time}",
                "display_name": "{deity_theme}ic {task_entity_type}"
              }
            ]
          }
        ],
        "rewards": {
          "reputation": 12,
          "commands": [
            "give {player} {reward_item_2} {count_2}",
            "effect give {player} {secondary_effect} 600 1",
            "tellraw {player} {\"text\":\"✦ Your {deity_theme}ic mastery over {secondary_domain} deepens\",\"color\":\"{deity_color}\"}"
          ],
          "progression_unlock": "{deity_theme}_master"
        },
        "cooldown_hours": 36,
        "repeatable": false
      },
      
      {
        "task_id": "{deity_theme}_sacred_ritual",
        "display_name": "Perform the Rite of {ritual_type}",
        "description": "Conduct the sacred ritual to prove your devotion to {primary_domain}",
        "progression_tier": "priest",
        "ai_assignment_context": {
          "trigger_conditions": {
            "min_reputation": 35,
            "max_reputation": 70,
            "required_time": ["{preferred_time}"],
            "prayer_types": ["{deity_theme}_blessing", "{deity_theme}_communion"],
            "completed_tasks": ["{deity_theme}_master_entities"],
            "unlocked_progressions": ["{deity_theme}_master"],
            "required_research": ["eidolonunchained:{deity_theme}_mysteries"]
          },
          "assignment_prompt": "Faithful priest, the time has come to prove your mastery of the {deity_theme}ic arts. Perform the ancient Rite of {ritual_type} to demonstrate your unity with {primary_domain}. This holy act will bless you with {reputation_reward} supreme favor.",
          "completion_phrases": [
            "The ritual circle blazes with {deity_theme}ic power - you have touched the source of {primary_domain}.",
            "Your devotion has opened a direct channel to {deity_domains}. You are truly blessed.",
            "The sacred {deity_theme} recognizes your worthiness. You have earned the title of {deity_theme}-Bearer."
          ]
        },
        "requirements": [
          {
            "type": "complete_ritual",
            "rituals": [
              {
                "ritual_id": "eidolonunchained:{deity_theme}_patronage_ritual",
                "count": 1,
                "success_required": true,
                "time_requirement": "{preferred_time}"
              }
            ]
          }
        ],
        "rewards": {
          "reputation": 20,
          "commands": [
            "give {player} {reward_item_3} {count_3}",
            "effect give {player} {blessing_effect} 900 2",
            "tellraw {player} {\"text\":\"✦ You are blessed as a true {deity_theme}-Bearer - divine power courses through your soul\",\"color\":\"{deity_color}\"}"
          ],
          "progression_unlock": "{deity_theme}_bearer"
        },
        "cooldown_hours": 84,
        "repeatable": false
      },
      
      {
        "task_id": "{deity_theme}_divine_crucible",
        "display_name": "Forge {crucible_item_type} of {primary_domain}",
        "description": "Create blessed items using the sacred crucible of {deity_theme}",
        "progression_tier": "high_priest",
        "ai_assignment_context": {
          "trigger_conditions": {
            "min_reputation": 60,
            "max_reputation": 999,
            "required_time": ["{preferred_time}"],
            "prayer_types": ["{deity_theme}_blessing"],
            "completed_tasks": ["{deity_theme}_sacred_ritual"],
            "unlocked_progressions": ["{deity_theme}_bearer"],
            "required_research": ["eidolonunchained:{deity_theme}_alchemy"]
          },
          "assignment_prompt": "My chosen champion, you have proven worthy of the highest mysteries. Use the sacred crucible to forge {crucible_item_type} blessed with {deity_theme}ic power. This ultimate test will grant you {reputation_reward} celestial favor.",
          "completion_phrases": [
            "Your crucible glows with {deity_theme}ic fire - you have mastered the art of divine creation.",
            "The blessed {crucible_item_type} you've forged will serve as beacons of {primary_domain} for generations.",
            "You have achieved the pinnacle of {deity_theme}ic craftsmanship. Your skill honors {deity_domains}."
          ]
        },
        "requirements": [
          {
            "type": "complete_crucible_recipe",
            "recipes": [
              {
                "recipe_id": "eidolonunchained:{deity_theme}_essence",
                "count": 1,
                "success_required": true,
                "time_requirement": "{preferred_time}"
              }
            ]
          }
        ],
        "rewards": {
          "reputation": 30,
          "commands": [
            "give {player} {reward_item_4} {count_4}",
            "effect give {player} {blessing_effect} 2400 3",
            "tellraw {player} {\"text\":\"✦ You are blessed with the ultimate power of {deity_theme}ic creation\",\"color\":\"{deity_color}\"}"
          ],
          "progression_unlock": "{deity_theme}_champion"
        },
        "cooldown_hours": 192,
        "repeatable": true
      },
      
      {
        "task_id": "{deity_theme}_exploration",
        "display_name": "Explore {exploration_location}",
        "description": "Journey to places where {deity_theme} energy flows strongest",
        "progression_tier": "wanderer",
        "ai_assignment_context": {
          "trigger_conditions": {
            "min_reputation": 8,
            "max_reputation": 40,
            "required_time": ["{preferred_time}"],
            "prayer_types": ["conversation"],
            "completed_tasks": [],
            "unlocked_progressions": []
          },
          "assignment_prompt": "Faithful wanderer, seek out the {exploration_location} where my {deity_theme} energy flows strongest. Journey to {location_count} sacred locations to receive divine blessings. Your pilgrimage will earn {reputation_reward} sacred favor.",
          "completion_phrases": [
            "Your exploration has connected you to the sacred places where {deity_theme} dwells.",
            "Each blessed location you've visited has strengthened your bond with {primary_domain}.",
            "Your journey has opened new pathways of {deity_theme}ic understanding."
          ]
        },
        "requirements": [
          {
            "type": "visit_biomes", 
            "biomes": [
              {
                "biome": "{primary_biome}",
                "duration_seconds": 180,
                "time_requirement": "{preferred_time}",
                "display_name": "Sacred {primary_domain}"
              },
              {
                "biome": "{secondary_biome}",
                "duration_seconds": 180,
                "time_requirement": "{preferred_time}",
                "display_name": "Blessed {secondary_domain}"
              }
            ]
          }
        ],
        "rewards": {
          "reputation": 8,
          "commands": [
            "give {player} {reward_item_5} {count_5}",
            "effect give {player} {primary_effect} 1000 1"
          ],
          "progression_unlock": "{deity_theme}_explorer"
        },
        "cooldown_hours": 30,
        "repeatable": true
      }
    ],
    
    "progression_chains": {
      "{deity_theme}_main_path": [
        "{deity_theme}_gather_resources",
        "{deity_theme}_master_entities",
        "{deity_theme}_sacred_ritual", 
        "{deity_theme}_divine_crucible"
      ],
      "{deity_theme}_exploration_path": [
        "{deity_theme}_exploration"
      ],
      "{deity_theme}_mastery_unlocks": {
        "{deity_theme}_gatherer": ["{deity_theme}_master_entities"],
        "{deity_theme}_master": ["{deity_theme}_sacred_ritual"],
        "{deity_theme}_bearer": ["{deity_theme}_divine_crucible"],
        "{deity_theme}_explorer": ["{deity_theme}_master_entities"],
        "{deity_theme}_champion": []
      }
    },
    
    "ai_integration": {
      "task_suggestion_prompts": {
        "no_active_tasks": "This faithful soul seeks purpose in serving {primary_domain}. Consider offering them a {deity_theme}ic duty that matches their devotion level ({reputation}) and their understanding of {deity_domains}.",
        "task_available": "A sacred mission awaits this devoted follower: {task_description}. Present this divine opportunity with the power of {deity_theme}ic authority.",
        "task_completion": "Celebrate their completion of '{completed_task}' with {deity_theme}ic satisfaction. Their success brings more {primary_domain} into the world. Guide them toward their next step in divine service if they are ready.",
        "progression_milestone": "This blessed soul has grown in {deity_theme}ic power from {old_tier} to {new_tier}. Their spiritual advancement deserves recognition and greater responsibility."
      },
      "context_integration": {
        "include_active_tasks_in_context": true,
        "include_completed_tasks_count": true,
        "include_available_task_hints": true,
        "task_context_format": "{deity_theme}ic Tasks - Active: {active_tasks} | Completed: {completed_count} sacred duties | Ready for: {available_tasks}",
        "progression_context_format": "{deity_theme}ic Progression: {current_tier} | Blessed Paths: {unlocked_progressions}"
      }
    }
  },
  
  "api_settings": {
    "temperature": 0.8,
    "max_tokens": 400,
    "timeout_seconds": 30,
    "generation_config": {
      "temperature": 0.8,
      "topK": 40,
      "topP": 0.9,
      "topLogprobs": null,
      "maxTokens": 400
    },
    "safety_config": {
      "block_threshold": "BLOCK_ONLY_HIGH",
      "blocked_categories": [
        "HARM_CATEGORY_HARASSMENT",
        "HARM_CATEGORY_HATE_SPEECH",
        "HARM_CATEGORY_SEXUALLY_EXPLICIT",
        "HARM_CATEGORY_DANGEROUS_CONTENT"
      ]
    }
  }
}
```

## Deity Theme Presets

### Fire Deity Example
```json
{
  "deity_id": "fire_deity",
  "deity_name": "Ignis",
  "deity_title": "Lord of Flames",
  "deity_domains": "fire and forge",
  "deity_theme": "fire",
  "deity_color": "red",
  "personality": "You speak with crackling intensity and fierce pride, rewarding those who embrace courage and forge their destiny",
  "speaking_style": "crackling intensity and fierce pride",
  "follower_relationship": "You reward those who embrace the flames and seek to forge their destiny in fire",
  "primary_domain": "flames",
  "secondary_domain": "forge",
  "preferred_time": "day",
  "environment": "blazing places",
  "primary_biome": "minecraft:desert",
  "secondary_biome": "minecraft:savanna",
  "tertiary_biome": "minecraft:nether_wastes",
  "primary_effect": "minecraft:fire_resistance",
  "secondary_effect": "minecraft:strength",
  "communion_effect": "minecraft:strength",
  "blessing_effect": "minecraft:fire_resistance",
  "task_resource_type": "flames",
  "task_entity_type": "blazes",
  "ritual_type": "inferno",
  "crucible_item_type": "essence",
  "exploration_location": "volcanic peaks",
  "resource_item": "minecraft:magma_cream",
  "collection_method": "nether_exploration",
  "target_entity": "minecraft:blaze",
  "interaction_method": "peaceful_encounter",
  "reward_item_1": "minecraft:fire_charge",
  "count_1": "8",
  "reward_item_2": "eidolon:fire_gem",
  "count_2": "2",
  "reward_item_3": "minecraft:enchanted_book",
  "count_3": "1",
  "reward_item_4": "minecraft:fire_crystal",
  "count_4": "1",
  "reward_item_5": "minecraft:diamond",
  "count_5": "2"
}
```

### Water Deity Example
```json
{
  "deity_id": "water_deity",
  "deity_name": "Aquaria",
  "deity_title": "Mistress of Tides",
  "deity_domains": "seas and storms",
  "deity_theme": "water",
  "deity_color": "blue",
  "personality": "You speak with flowing wisdom and serene depths, guiding those who seek balance and adaptability",
  "speaking_style": "flowing wisdom and serene depths",
  "follower_relationship": "You reward those who seek balance and flow with change like the eternal tides",
  "primary_domain": "waters",
  "secondary_domain": "tides",
  "preferred_time": "any",
  "environment": "aquatic places",
  "primary_biome": "minecraft:ocean",
  "secondary_biome": "minecraft:river",
  "tertiary_biome": "minecraft:swamp",
  "primary_effect": "minecraft:water_breathing",
  "secondary_effect": "minecraft:dolphin_grace",
  "communion_effect": "minecraft:dolphin_grace",
  "blessing_effect": "minecraft:conduit_power",
  "task_resource_type": "pearls",
  "task_entity_type": "guardians",
  "ritual_type": "tide",
  "crucible_item_type": "crystals",
  "exploration_location": "ocean depths"
}
```

## Generation Scripts

### Python Template Processor
```python
def generate_deity_config(template_path, preset_variables, output_path):
    """Generate a complete deity configuration from template and variables."""
    
    # Load template
    with open(template_path, 'r') as f:
        template_content = f.read()
    
    # Apply variable substitution
    result = template_content
    for variable, value in preset_variables.items():
        placeholder = "{" + variable + "}"
        result = result.replace(placeholder, str(value))
    
    # Write output
    with open(output_path, 'w') as f:
        f.write(result)
    
    return result

def create_deity_preset(theme, name, title, domains, **custom_vars):
    """Create a deity preset with smart defaults."""
    
    preset = {
        "deity_id": f"{theme}_deity",
        "deity_name": name,
        "deity_title": title,
        "deity_domains": domains,
        "deity_theme": theme,
        # Add intelligent defaults based on theme
        **get_theme_defaults(theme),
        # Override with custom variables
        **custom_vars
    }
    
    return preset

def get_theme_defaults(theme):
    """Get intelligent defaults based on deity theme."""
    
    defaults = {
        "fire": {
            "deity_color": "red",
            "preferred_time": "day",
            "primary_effect": "minecraft:fire_resistance",
            "task_resource_type": "flames"
        },
        "water": {
            "deity_color": "blue", 
            "preferred_time": "any",
            "primary_effect": "minecraft:water_breathing",
            "task_resource_type": "pearls"
        },
        "earth": {
            "deity_color": "yellow",
            "preferred_time": "any", 
            "primary_effect": "minecraft:resistance",
            "task_resource_type": "stones"
        },
        "air": {
            "deity_color": "white",
            "preferred_time": "day",
            "primary_effect": "minecraft:slow_falling", 
            "task_resource_type": "feathers"
        }
    }
    
    return defaults.get(theme, {})
```

### Usage Examples
```python
# Create a fire deity
fire_preset = create_deity_preset(
    theme="fire",
    name="Vulcanos", 
    title="Master of the Forge",
    domains="fire and metalwork",
    speaking_style="booming authority and molten passion"
)

generate_deity_config(
    "universal_deity_template.json",
    fire_preset,
    "fire_deity.json"
)

# Create a custom crystal deity
crystal_preset = create_deity_preset(
    theme="crystal",
    name="Prisma",
    title="Guardian of Gems", 
    domains="crystals and light",
    deity_color="purple",
    preferred_time="day",
    task_resource_type="gems",
    primary_effect="minecraft:night_vision"
)

generate_deity_config(
    "universal_deity_template.json", 
    crystal_preset,
    "crystal_deity.json"
)
```

This universal template system allows for:

1. **Complete Flexibility**: Any deity theme can be created by changing variables
2. **Consistent Structure**: All deities follow the same proven pattern
3. **Smart Defaults**: Common themes have intelligent default values
4. **Easy Customization**: Override any aspect while keeping the structure
5. **Scalable**: Add new deity themes without modifying the template
6. **Integration Ready**: Includes all systems (AI, tasks, rituals, crucible)

The template covers all the systems we've built while remaining completely generic and reusable for any deity concept.
