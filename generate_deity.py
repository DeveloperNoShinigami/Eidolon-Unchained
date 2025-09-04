#!/usr/bin/env python3
"""
Universal Deity Configuration Generator

This script generates complete deity configurations using the universal template system.
All generated deities include integrated AI, task systems, prayer configs, and crucible recipes.

Usage:
    python generate_deity.py --theme fire --name "Ignis" --title "Lord of Flames"
    python generate_deity.py --preset fire_basic
    python generate_deity.py --custom config.json
"""

import json
import argparse
import os
from typing import Dict, Any

class DeityGenerator:
    def __init__(self):
        self.template_path = "docs/UNIVERSAL_DEITY_TEMPLATE.md"
        self.output_dir = "src/main/resources/data/eidolonunchained/ai_deities/"
        
        # Smart defaults for common themes
        self.theme_defaults = {
            "fire": {
                "deity_color": "red",
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
            },
            "water": {
                "deity_color": "blue",
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
                "exploration_location": "ocean depths",
                "resource_item": "minecraft:prismarine_shard",
                "collection_method": "underwater_exploration",
                "target_entity": "minecraft:guardian",
                "interaction_method": "peaceful_encounter",
                "reward_item_1": "minecraft:heart_of_the_sea",
                "count_1": "1",
                "reward_item_2": "eidolon:water_gem",
                "count_2": "2", 
                "reward_item_3": "minecraft:trident",
                "count_3": "1",
                "reward_item_4": "minecraft:water_crystal",
                "count_4": "1",
                "reward_item_5": "minecraft:diamond",
                "count_5": "2"
            },
            "earth": {
                "deity_color": "yellow",
                "preferred_time": "any",
                "environment": "mountainous places",
                "primary_biome": "minecraft:mountains",
                "secondary_biome": "minecraft:cave",
                "tertiary_biome": "minecraft:deepslate",
                "primary_effect": "minecraft:resistance",
                "secondary_effect": "minecraft:absorption",
                "communion_effect": "minecraft:resistance",
                "blessing_effect": "minecraft:absorption",
                "task_resource_type": "stones",
                "task_entity_type": "golems",
                "ritual_type": "foundation",
                "crucible_item_type": "gems",
                "exploration_location": "mountain peaks",
                "resource_item": "minecraft:emerald",
                "collection_method": "deep_mining",
                "target_entity": "minecraft:iron_golem",
                "interaction_method": "respectful_approach",
                "reward_item_1": "minecraft:diamond",
                "count_1": "3",
                "reward_item_2": "eidolon:earth_gem",
                "count_2": "2",
                "reward_item_3": "minecraft:enchanted_book",
                "count_3": "1",
                "reward_item_4": "minecraft:earth_crystal",
                "count_4": "1", 
                "reward_item_5": "minecraft:emerald",
                "count_5": "4"
            },
            "air": {
                "deity_color": "white",
                "preferred_time": "day",
                "environment": "high places",
                "primary_biome": "minecraft:windswept_hills",
                "secondary_biome": "minecraft:plains",
                "tertiary_biome": "minecraft:mountains",
                "primary_effect": "minecraft:slow_falling",
                "secondary_effect": "minecraft:speed",
                "communion_effect": "minecraft:speed",
                "blessing_effect": "minecraft:jump_boost",
                "task_resource_type": "feathers",
                "task_entity_type": "phantoms",
                "ritual_type": "tempest",
                "crucible_item_type": "shards",
                "exploration_location": "sky peaks",
                "resource_item": "minecraft:feather",
                "collection_method": "sky_hunting",
                "target_entity": "minecraft:phantom",
                "interaction_method": "aerial_dance",
                "reward_item_1": "minecraft:elytra",
                "count_1": "1",
                "reward_item_2": "eidolon:air_gem",
                "count_2": "2",
                "reward_item_3": "minecraft:enchanted_book",
                "count_3": "1",
                "reward_item_4": "minecraft:wind_crystal",
                "count_4": "1",
                "reward_item_5": "minecraft:diamond",
                "count_5": "2"
            }
        }
        
        # Load the universal template
        self.load_template()
    
    def load_template(self):
        """Load the universal deity template from the markdown file."""
        try:
            with open(self.template_path, 'r') as f:
                content = f.read()
            
            # Extract the JSON template from the markdown
            start_marker = "```json\n{"
            end_marker = "}\n```"
            
            start_idx = content.find(start_marker)
            if start_idx == -1:
                raise ValueError("Could not find template start marker")
            
            start_idx += len("```json\n")
            end_idx = content.find(end_marker, start_idx)
            
            if end_idx == -1:
                raise ValueError("Could not find template end marker")
            
            end_idx += 1  # Include the closing brace
            
            self.template = content[start_idx:end_idx]
            
        except FileNotFoundError:
            raise FileNotFoundError(f"Template file not found: {self.template_path}")
    
    def create_deity_preset(self, theme: str, name: str, title: str, domains: str, **custom_vars) -> Dict[str, Any]:
        """Create a deity preset with smart defaults."""
        
        preset = {
            "deity_id": f"{theme}_deity",
            "deity_name": name,
            "deity_title": title,
            "deity_domains": domains,
            "deity_theme": theme,
            "personality": f"You speak with {theme}ic wisdom and power, rewarding those who embrace {domains}",
            "speaking_style": f"{theme}ic wisdom and commanding presence",
            "follower_relationship": f"You reward those who embrace {domains} and seek mastery of {theme}",
            "primary_domain": theme,
            "secondary_domain": domains.split(" and ")[-1] if " and " in domains else domains,
            "opposite_time": "night" if self.theme_defaults.get(theme, {}).get("preferred_time") == "day" else "day"
        }
        
        # Add theme defaults
        if theme in self.theme_defaults:
            preset.update(self.theme_defaults[theme])
        
        # Override with custom variables
        preset.update(custom_vars)
        
        return preset
    
    def generate_deity_config(self, preset_variables: Dict[str, Any], output_filename: str = None) -> str:
        """Generate a complete deity configuration from preset variables."""
        
        # Apply variable substitution
        result = self.template
        for variable, value in preset_variables.items():
            placeholder = "{" + variable + "}"
            result = result.replace(placeholder, str(value))
        
        # Check for unresolved placeholders
        import re
        unresolved = re.findall(r'\{([^}]+)\}', result)
        if unresolved:
            print(f"Warning: Unresolved template variables: {unresolved}")
        
        # Save to file if specified
        if output_filename:
            os.makedirs(self.output_dir, exist_ok=True)
            output_path = os.path.join(self.output_dir, output_filename)
            
            with open(output_path, 'w') as f:
                f.write(result)
            
            print(f"Generated deity configuration: {output_path}")
        
        return result
    
    def generate_from_preset(self, preset_name: str) -> str:
        """Generate a deity from a predefined preset."""
        
        presets = {
            "fire_basic": {
                "theme": "fire",
                "name": "Ignis", 
                "title": "Lord of Flames",
                "domains": "fire and forge"
            },
            "water_basic": {
                "theme": "water",
                "name": "Aquaria",
                "title": "Mistress of Tides", 
                "domains": "seas and storms"
            },
            "earth_basic": {
                "theme": "earth",
                "name": "Gaia",
                "title": "Mother of Stone",
                "domains": "earth and mountains"
            },
            "air_basic": {
                "theme": "air",
                "name": "Zephyr",
                "title": "Lord of Winds", 
                "domains": "sky and storms"
            }
        }
        
        if preset_name not in presets:
            raise ValueError(f"Unknown preset: {preset_name}. Available: {list(presets.keys())}")
        
        preset_config = presets[preset_name]
        theme = preset_config.pop("theme")
        
        preset_variables = self.create_deity_preset(theme, **preset_config)
        return self.generate_deity_config(preset_variables, f"{theme}_deity.json")

def main():
    parser = argparse.ArgumentParser(description="Generate deity configurations")
    
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--theme", help="Deity theme (fire, water, earth, air, etc.)")
    group.add_argument("--preset", help="Use predefined preset (fire_basic, water_basic, etc.)")
    group.add_argument("--custom", help="Path to custom configuration JSON file")
    
    parser.add_argument("--name", help="Deity name (required with --theme)")
    parser.add_argument("--title", help="Deity title (required with --theme)")
    parser.add_argument("--domains", help="Deity domains (required with --theme)")
    parser.add_argument("--output", help="Output filename (defaults to {theme}_deity.json)")
    
    args = parser.parse_args()
    
    generator = DeityGenerator()
    
    try:
        if args.theme:
            if not all([args.name, args.title, args.domains]):
                parser.error("--theme requires --name, --title, and --domains")
            
            preset_variables = generator.create_deity_preset(
                args.theme, args.name, args.title, args.domains
            )
            
            output_filename = args.output or f"{args.theme}_deity.json"
            generator.generate_deity_config(preset_variables, output_filename)
            
        elif args.preset:
            generator.generate_from_preset(args.preset)
            
        elif args.custom:
            with open(args.custom, 'r') as f:
                custom_config = json.load(f)
            
            theme = custom_config.get("theme")
            if not theme:
                parser.error("Custom config must include 'theme' field")
            
            preset_variables = generator.create_deity_preset(theme, **custom_config)
            
            output_filename = args.output or f"{theme}_deity.json"
            generator.generate_deity_config(preset_variables, output_filename)
            
    except Exception as e:
        print(f"Error: {e}")
        return 1
    
    return 0

if __name__ == "__main__":
    exit(main())
