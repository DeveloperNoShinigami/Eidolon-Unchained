#!/usr/bin/env python3
"""
Deity Configuration Generator for Eidolon Unchained

This script generates standardized deity configurations using templates and presets.
"""

import json
import argparse
import os
from typing import Dict, Any, List


class DeityGenerator:
    """Generator for deity configurations using template system."""
    
    def __init__(self):
        self.template_base = self._load_base_template()
        self.presets = self._load_presets()
    
    def _load_base_template(self) -> str:
        """Load the base deity configuration template."""
        return '''{{
  "deity": "eidolonunchained:{deity_id}",
  "ai_provider": "player2ai",
  "item_context_id": "",
  "personality": "You are {deity_name}, {deity_title}, a powerful deity of {deity_domains}. You speak with {speaking_style} and {personality_traits}. You reward those who {rewarded_behaviors} and seek {sought_qualities}. Your followers find {follower_benefits} in {deity_realm}.",
  
  "behavior_rules": {{
    "reputation_thresholds": {{
      "0": "You are {low_rep_attitude} of this newcomer and speak with {low_rep_tone}.",
      "25": "You acknowledge their growing devotion with {mid_low_rep_tone}.",
      "50": "You treat them as a trusted acolyte worthy of your {mid_rep_benefits}.",
      "75": "You speak to them as a valued priest of your {high_rep_status}.",
      "100": "You address them as your ultimate champion, worthy of your {max_rep_rewards}."
    }},
    "research_requirements": {{
      "0": "You share only basic knowledge of {basic_knowledge}.",
      "5": "You reveal deeper mysteries of {intermediate_knowledge}.",
      "10": "You grant access to advanced {advanced_knowledge}.",
      "15": "You unveil the most {secret_knowledge}."
    }},
    "curses": {{
      "low_reputation": "You curse those who displease you with {curse_effects}.",
      "enemy_patron": "You inflict severe curses upon followers of {opposing_forces}.",
      "disrespectful": "Those who show disrespect receive your wrath in the form of {punishment_effects}."
    }},
    "blessings": {{
      "high_reputation": "You bestow powerful {blessing_type} upon your faithful.",
      "preferred_time": "Your blessings are strongest during {optimal_time}.",
      "preferred_location": "In {favored_places}, your blessings carry extra potency."
    }},
    "gifts": {{
      "special_occasion": "On rare occasions, you grant legendary {special_artifacts}.",
      "major_milestone": "Achieving major progression milestones earns unique {milestone_gifts}.",
      "exceptional_service": "Exceptional devotion is rewarded with rare {devotion_rewards}."
    }},
    "dynamic_responses": {{
      "time_of_day": {{
        "{preferred_time}": "You are at your strongest during {optimal_time} and speak with enhanced power.",
        "{non_preferred_time}": "Though {opposing_time} weakens your presence, your authority remains {authority_level}."
      }},
      "biome": {{
        "{primary_biome}": "This {biome_description} resonates with your very essence.",
        "{secondary_biome}": "This {secondary_description} pleases you greatly.",
        "{tertiary_biome}": "The {tertiary_description} here {tertiary_reaction}."
      }}
    }}
  }},
  
  "prayer_configs": {{
    "conversation": {{
      "cooldown_minutes": 5,
      "reputation_change": 1,
      "success_rate": 0.95,
      "responses": {{
        "success": [
          "{deity_name} listens to your words with {attention_style}.",
          "Your voice reaches {deity_name} through the {connection_medium}.",
          "{deity_name} acknowledges your {communication_type} with {acknowledgment_style}."
        ],
        "cooldown": [
          "{deity_name} needs time to {cooldown_reason}.",
          "The {connection_medium} must rest before another {interaction_type}.",
          "Your {communication_type} echoes still - patience is required."
        ],
        "failure": [
          "{deity_name} is {unavailability_reason} and cannot respond.",
          "The {connection_medium} is {interference_type} - try again later.",
          "Your {communication_type} does not reach {deity_name} through the {barrier_type}."
        ]
      }}
    }},
    "{deity_theme}_prayer": {{
      "cooldown_minutes": 15,
      "reputation_change": 2,
      "success_rate": 0.8,
      "special_effects": [
        "effect give {{player}} {primary_effect} {effect_duration} {effect_level}"
      ],
      "responses": {{
        "success": [
          "{deity_name} grants you {blessing_description}.",
          "Your {prayer_type} is answered with {divine_response}.",
          "The power of {deity_domain_singular} flows through you."
        ]
      }}
    }},
    "{deity_theme}_communion": {{
      "cooldown_minutes": 60,
      "reputation_change": 5,
      "success_rate": 0.6,
      "special_effects": [
        "effect give {{player}} {communion_effect_1} {effect_duration} {effect_level}",
        "effect give {{player}} {communion_effect_2} {effect_duration} {effect_level}"
      ],
      "item_requirements": [
        "{communion_item_1}",
        "{communion_item_2}"
      ],
      "responses": {{
        "success": [
          "You commune directly with {deity_name}, feeling {communion_experience}.",
          "The {communion_medium} connects your soul to {deity_realm}.",
          "{deity_name} shares {shared_knowledge} through the sacred {communion_ritual}."
        ]
      }}
    }}
  }},
  
  {task_config_json},
  
  "api_settings": {{
    "temperature": 0.8,
    "max_tokens": 400,
    "timeout_seconds": 30,
    "generation_config": {{
      "temperature": 0.8,
      "topK": 40,
      "topP": 0.9,
      "maxOutputTokens": 400
    }},
    "safety_settings": [
      {{
        "category": "HARM_CATEGORY_HARASSMENT",
        "threshold": "BLOCK_MEDIUM_AND_ABOVE"
      }},
      {{
        "category": "HARM_CATEGORY_HATE_SPEECH", 
        "threshold": "BLOCK_MEDIUM_AND_ABOVE"
      }},
      {{
        "category": "HARM_CATEGORY_SEXUALLY_EXPLICIT",
        "threshold": "BLOCK_MEDIUM_AND_ABOVE"
      }},
      {{
        "category": "HARM_CATEGORY_DANGEROUS_CONTENT",
        "threshold": "BLOCK_MEDIUM_AND_ABOVE"
      }}
    ]
  }}
}}'''
    
    def _load_presets(self) -> Dict[str, Dict[str, Any]]:
        """Load predefined deity presets."""
        return {
            "fire": {
                "deity_id": "fire_deity",
                "deity_name": "Ignis",
                "deity_title": "Lord of Flames",
                "deity_domains": "fire and forge",
                "deity_theme": "fire",
                "deity_color": "red",
                "speaking_style": "crackling intensity and fierce pride",
                "personality_traits": "unwavering determination and passionate zeal",
                "rewarded_behaviors": "embrace courage and forge their destiny",
                "sought_qualities": "inner fire and unbreakable will",
                "follower_benefits": "strength and resilience",
                "deity_realm": "blazing forges and eternal flames",
                "low_rep_attitude": "suspicious",
                "low_rep_tone": "cold formality",
                "mid_low_rep_tone": "grudging respect",
                "mid_rep_benefits": "guidance",
                "high_rep_status": "burning faith",
                "max_rep_rewards": "greatest fire magic and eternal flame",
                "basic_knowledge": "flames",
                "intermediate_knowledge": "fire magic",
                "advanced_knowledge": "forge mastery",
                "secret_knowledge": "forbidden fire arts",
                "curse_effects": "burning weakness and scorching pain",
                "opposing_forces": "ice and water deities",
                "punishment_effects": "blazing curses",
                "blessing_type": "fire blessings",
                "optimal_time": "the blazing hours of noon",
                "favored_places": "volcanic regions and forge-halls",
                "special_artifacts": "fire artifacts",
                "milestone_gifts": "flame gems",
                "devotion_rewards": "blazing essences and fire crystals",
                "preferred_time": "day",
                "non_preferred_time": "night",
                "opposing_time": "the cool darkness",
                "authority_level": "absolute",
                "primary_biome": "minecraft:desert",
                "secondary_biome": "minecraft:savanna",
                "tertiary_biome": "minecraft:nether_wastes",
                "biome_description": "scorching desert",
                "secondary_description": "sun-baked savanna",
                "tertiary_description": "infernal wasteland",
                "tertiary_reaction": "sings with familiar fire",
                "attention_style": "burning focus",
                "connection_medium": "flames",
                "communication_type": "prayer",
                "acknowledgment_style": "fiery approval",
                "cooldown_reason": "rekindle the sacred flames",
                "interaction_type": "communion",
                "unavailability_reason": "tending the eternal forge",
                "interference_type": "dampened by opposing forces",
                "barrier_type": "wall of competing elements",
                "primary_effect": "minecraft:fire_resistance",
                "effect_duration": "600",
                "effect_level": "0",
                "blessing_description": "protection from flames",
                "prayer_type": "fire prayer",
                "divine_response": "crackling power",
                "deity_domain_singular": "fire",
                "communion_effect_1": "minecraft:strength",
                "communion_effect_2": "minecraft:fire_resistance",
                "communion_item_1": "minecraft:blaze_powder",
                "communion_item_2": "minecraft:fire_charge",
                "communion_experience": "the heat of creation itself",
                "communion_medium": "sacred flames",
                "shared_knowledge": "the secrets of eternal fire",
                "communion_ritual": "conflagration",
                "resource_type": "flames",
                "entity_type": "blazes",
                "ritual_type": "inferno",
                "item_type": "essence",
                "location_type": "volcanoes"
            },
            "water": {
                "deity_id": "water_deity",
                "deity_name": "Aquaria",
                "deity_title": "Mistress of Tides",
                "deity_domains": "seas and storms",
                "deity_theme": "water",
                "deity_color": "blue",
                "speaking_style": "flowing wisdom and serene depths",
                "personality_traits": "patient understanding and adaptive nature",
                "rewarded_behaviors": "seek balance and flow with change",
                "sought_qualities": "adaptability and emotional depth",
                "follower_benefits": "peace and flexibility",
                "deity_realm": "endless oceans and gentle streams",
                "low_rep_attitude": "indifferent",
                "low_rep_tone": "distant coolness",
                "mid_low_rep_tone": "warming interest",
                "mid_rep_benefits": "wisdom",
                "high_rep_status": "flowing faith",
                "max_rep_rewards": "mastery over all waters and storm calling",
                "basic_knowledge": "currents",
                "intermediate_knowledge": "water magic",
                "advanced_knowledge": "storm mastery",
                "secret_knowledge": "forbidden tide arts",
                "curse_effects": "drowning weakness and crushing depths",
                "opposing_forces": "fire and earth deities",
                "punishment_effects": "tidal curses",
                "blessing_type": "water blessings",
                "optimal_time": "the flowing hours of rain",
                "favored_places": "coastal regions and deep waters",
                "special_artifacts": "tidal artifacts",
                "milestone_gifts": "water gems",
                "devotion_rewards": "flowing essences and tide crystals",
                "preferred_time": "any",
                "non_preferred_time": "drought",
                "opposing_time": "the scorching heat",
                "authority_level": "serene",
                "primary_biome": "minecraft:ocean",
                "secondary_biome": "minecraft:river",
                "tertiary_biome": "minecraft:swamp",
                "biome_description": "endless ocean",
                "secondary_description": "flowing river",
                "tertiary_description": "murky swampland",
                "tertiary_reaction": "whispers ancient secrets",
                "attention_style": "flowing focus",
                "connection_medium": "tides",
                "communication_type": "prayer",
                "acknowledgment_style": "gentle approval",
                "cooldown_reason": "let the tides flow",
                "interaction_type": "communion",
                "unavailability_reason": "guiding distant storms",
                "interference_type": "muddied by conflicting currents",
                "barrier_type": "barrier of opposing elements",
                "primary_effect": "minecraft:water_breathing",
                "effect_duration": "600",
                "effect_level": "0",
                "blessing_description": "mastery over water",
                "prayer_type": "tide prayer",
                "divine_response": "flowing power",
                "deity_domain_singular": "water",
                "communion_effect_1": "minecraft:dolphin_grace",
                "communion_effect_2": "minecraft:water_breathing",
                "communion_item_1": "minecraft:heart_of_the_sea",
                "communion_item_2": "minecraft:prismarine_crystals",
                "communion_experience": "the depths of all oceans",
                "communion_medium": "sacred tides",
                "shared_knowledge": "the mysteries of eternal flow",
                "communion_ritual": "tidal convergence",
                "resource_type": "pearls",
                "entity_type": "guardians",
                "ritual_type": "tide",
                "item_type": "crystal",
                "location_type": "depths"
            },
            "earth": {
                "deity_id": "earth_deity",
                "deity_name": "Gaia",
                "deity_title": "Mother of Stone",
                "deity_domains": "earth and mountains",
                "deity_theme": "earth",
                "deity_color": "yellow",
                "speaking_style": "deep rumbling wisdom and ancient patience",
                "personality_traits": "steadfast endurance and protective nurturing",
                "rewarded_behaviors": "build lasting foundations and protect the innocent",
                "sought_qualities": "stability and perseverance",
                "follower_benefits": "endurance and protection",
                "deity_realm": "deep caverns and towering peaks",
                "preferred_time": "any",
                "primary_biome": "minecraft:mountains",
                "secondary_biome": "minecraft:cave",
                "tertiary_biome": "minecraft:deepslate",
                "primary_effect": "minecraft:resistance",
                "communion_effect_1": "minecraft:resistance",
                "communion_effect_2": "minecraft:absorption",
                "resource_type": "stones",
                "entity_type": "golems",
                "ritual_type": "foundation",
                "item_type": "gem",
                "location_type": "peaks"
            },
            "air": {
                "deity_id": "air_deity",
                "deity_name": "Zephyr",
                "deity_title": "Lord of Winds",
                "deity_domains": "sky and storms",
                "deity_theme": "air",
                "deity_color": "white",
                "speaking_style": "swift intensity and boundless freedom",
                "personality_traits": "restless energy and unlimited potential",
                "rewarded_behaviors": "embrace freedom and reach new heights",
                "sought_qualities": "independence and vision",
                "follower_benefits": "swiftness and clarity",
                "deity_realm": "endless skies and storm clouds",
                "preferred_time": "day",
                "primary_biome": "minecraft:windswept_hills",
                "secondary_biome": "minecraft:plains",
                "tertiary_biome": "minecraft:mountains",
                "primary_effect": "minecraft:slow_falling",
                "communion_effect_1": "minecraft:speed",
                "communion_effect_2": "minecraft:jump_boost",
                "resource_type": "feathers",
                "entity_type": "phantoms",
                "ritual_type": "tempest",
                "item_type": "shard",
                "location_type": "peaks"
            }
        }
    
    def generate_task_config(self, preset: Dict[str, Any]) -> str:
        """Generate task configuration JSON for deity."""
        deity_theme = preset["deity_theme"]
        
        task_config = {
            "task_config": {
                "enabled": True,
                "max_active_tasks": 3,
                "task_assignment_behavior": {
                    "auto_assign_probability": 0.4,
                    "min_reputation_for_auto_assign": 3,
                    "conversation_triggers": [
                        "first_conversation",
                        "reputation_milestone",
                        "completed_previous_task",
                        f"{deity_theme}_prayer",
                        f"{deity_theme}_communion"
                    ],
                    "cooldown_between_assignments_hours": 24
                },
                "available_tasks": self._generate_tasks(preset),
                "progression_chains": self._generate_progression_chains(preset),
                "ai_integration": self._generate_ai_integration(preset)
            }
        }
        
        return json.dumps(task_config, indent=2)[1:-1]  # Remove outer braces
    
    def _generate_tasks(self, preset: Dict[str, Any]) -> List[Dict[str, Any]]:
        """Generate the 5 standard tasks for deity."""
        deity_theme = preset["deity_theme"]
        
        return [
            {
                "task_id": f"{deity_theme}_gather_{preset['resource_type']}",
                "display_name": f"Gather {preset['resource_type'].title()}",
                "description": f"Collect {preset['resource_type']} that resonate with {preset['deity_domains']}",
                "progression_tier": "initiate",
                "ai_assignment_context": {
                    "trigger_conditions": {
                        "min_reputation": 0,
                        "max_reputation": 30,
                        "required_time": [preset.get("preferred_time", "any")],
                        "prayer_types": ["conversation", deity_theme],
                        "completed_tasks": [],
                        "unlocked_progressions": []
                    },
                    "assignment_prompt": f"Young seeker, collect sacred {preset['resource_type']} to prove your devotion.",
                    "completion_phrases": [
                        f"Your gathering of {preset['resource_type']} shows dedication to {preset['deity_domains']}.",
                        f"The {preset['resource_type']} you've collected resonate with divine power.",
                        f"Your harvest strengthens the connection to {preset['deity_realm']}."
                    ]
                },
                "requirements": [
                    {
                        "type": "collect_items",
                        "items": [
                            {
                                "item": f"minecraft:{preset['resource_type'][:-1] if preset['resource_type'].endswith('s') else preset['resource_type']}",
                                "count": 16,
                                "source": "gathering",
                                "time_requirement": preset.get("preferred_time", "any"),
                                "display_name": f"Sacred {preset['resource_type'].title()}"
                            }
                        ]
                    }
                ],
                "rewards": {
                    "reputation": 8,
                    "commands": [
                        f"give {{player}} minecraft:diamond 2",
                        f"effect give {{player}} {preset['primary_effect']} 600 0",
                        f"tellraw {{player}} {{\"text\":\"✦ The power of {preset['deity_domains']} flows through you\",\"color\":\"{preset['deity_color']}\"}}"
                    ],
                    "progression_unlock": f"{deity_theme}_gatherer"
                },
                "cooldown_hours": 24,
                "repeatable": False
            }
            # Add other 4 tasks following similar pattern...
        ]
    
    def _generate_progression_chains(self, preset: Dict[str, Any]) -> Dict[str, Any]:
        """Generate progression chains for deity."""
        deity_theme = preset["deity_theme"]
        
        return {
            f"{deity_theme}_main_path": [
                f"{deity_theme}_gather_{preset['resource_type']}",
                f"{deity_theme}_combat_{preset['entity_type']}",
                f"{deity_theme}_ritual_{preset['ritual_type']}",
                f"{deity_theme}_crafting_{preset['item_type']}"
            ],
            f"{deity_theme}_exploration_path": [
                f"{deity_theme}_exploration_{preset['location_type']}"
            ],
            f"{deity_theme}_mastery_unlocks": {
                f"{deity_theme}_gatherer": [f"{deity_theme}_combat_{preset['entity_type']}"],
                f"{deity_theme}_warrior": [f"{deity_theme}_ritual_{preset['ritual_type']}"],
                f"{deity_theme}_ritualist": [f"{deity_theme}_crafting_{preset['item_type']}"],
                f"{deity_theme}_explorer": [f"{deity_theme}_combat_{preset['entity_type']}"],
                f"{deity_theme}_master": []
            }
        }
    
    def _generate_ai_integration(self, preset: Dict[str, Any]) -> Dict[str, Any]:
        """Generate AI integration configuration."""
        deity_name = preset["deity_name"]
        deity_theme = preset["deity_theme"]
        
        return {
            "task_suggestion_prompts": {
                "no_active_tasks": f"This faithful follower seeks purpose in serving {preset['deity_domains']}. Consider offering them a task that matches their devotion level and understanding.",
                "task_available": "A sacred duty awaits this devoted follower. Present this opportunity with divine authority.",
                "task_completion": f"Celebrate their completion with satisfaction. Their success brings more {preset.get('deity_influence', 'power')} into the world.",
                "progression_milestone": f"This soul has grown in {preset.get('power_type', 'divine power')}. Their advancement deserves recognition."
            },
            "context_integration": {
                "include_active_tasks_in_context": True,
                "include_completed_tasks_count": True,
                "include_available_task_hints": True,
                "task_context_format": f"{deity_name} Tasks - Active: {{active_tasks}} | Completed: {{completed_count}} sacred duties | Ready for: {{available_tasks}}",
                "progression_context_format": f"{deity_name} Progression: {{current_tier}} | Divine Path: {{unlocked_progressions}}"
            }
        }
    
    def generate_deity(self, preset_name: str) -> str:
        """Generate complete deity configuration from preset."""
        if preset_name not in self.presets:
            raise ValueError(f"Unknown preset: {preset_name}")
        
        preset = self.presets[preset_name]
        task_config_json = self.generate_task_config(preset)
        
        # Apply preset variables to template
        result = self.template_base.format(task_config_json=task_config_json, **preset)
        
        return result
    
    def generate_custom_deity(self, config_file: str) -> str:
        """Generate deity from custom configuration file."""
        with open(config_file, 'r') as f:
            custom_preset = json.load(f)
        
        task_config_json = self.generate_task_config(custom_preset)
        result = self.template_base.format(task_config_json=task_config_json, **custom_preset)
        
        return result


def main():
    """Command line interface for deity generator."""
    parser = argparse.ArgumentParser(description='Generate deity configurations for Eidolon Unchained')
    parser.add_argument('--preset', choices=['fire', 'water', 'earth', 'air'], 
                       help='Use predefined deity preset')
    parser.add_argument('--config', help='Path to custom deity configuration JSON')
    parser.add_argument('--output', required=True, help='Output file path')
    parser.add_argument('--batch', choices=['elemental'], help='Generate batch of deities')
    parser.add_argument('--output-dir', help='Output directory for batch generation')
    
    args = parser.parse_args()
    
    generator = DeityGenerator()
    
    if args.batch == 'elemental':
        if not args.output_dir:
            print("Error: --output-dir required for batch generation")
            return
        
        os.makedirs(args.output_dir, exist_ok=True)
        
        for preset_name in ['fire', 'water', 'earth', 'air']:
            deity_config = generator.generate_deity(preset_name)
            output_file = os.path.join(args.output_dir, f"{preset_name}_deity.json")
            
            with open(output_file, 'w') as f:
                f.write(deity_config)
            
            print(f"Generated {preset_name} deity: {output_file}")
    
    elif args.preset:
        deity_config = generator.generate_deity(args.preset)
        
        with open(args.output, 'w') as f:
            f.write(deity_config)
        
        print(f"Generated {args.preset} deity: {args.output}")
    
    elif args.config:
        deity_config = generator.generate_custom_deity(args.config)
        
        with open(args.output, 'w') as f:
            f.write(deity_config)
        
        print(f"Generated custom deity: {args.output}")
    
    else:
        print("Error: Must specify --preset, --config, or --batch")


if __name__ == '__main__':
    main()
