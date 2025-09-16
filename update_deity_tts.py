#!/usr/bin/env python3
"""
Script to add TTS configurations to all deity JSON files
"""

import json
import os
import re

# Define TTS configurations for each deity type
TTS_CONFIGS = {
    "water_deity": {
        "voice_id": "female-flowing-1",
        "backup_voice": "female-light-1",
        "pitch": 1.0,
        "speed": 0.9,
        "volume": 1.0,
        "emotion": "serene",
        "accent": "oceanic",
        "emphasis_level": 0,
        "enabled": True,
        "allow_player_override": True,
        "funding_preference": "player_first",
        "voice_aliases": {
            "ocean_spirit": "female-flowing-1",
            "tidal_whisper": "female-light-1",
            "depths_call": "female-deep-1"
        },
        "reputation_voices": {
            "0": "female-light-1",
            "25": "female-flowing-1",
            "75": "female-flowing-divine-1"
        },
        "time_voices": {
            "day": "female-flowing-1",
            "night": "female-deep-1"
        },
        "biome_voices": {
            "minecraft:ocean": "female-flowing-1",
            "minecraft:deep_ocean": "female-deep-1",
            "minecraft:river": "female-light-1",
            "minecraft:beach": "female-flowing-1"
        },
        "advanced_params": {
            "water_echo": 0.3,
            "tidal_resonance": 0.2,
            "depth_filter": True
        }
    },

    "earth_deity": {
        "voice_id": "male-steady-1",
        "backup_voice": "male-deep-1",
        "pitch": 0.9,
        "speed": 0.8,
        "volume": 1.1,
        "emotion": "stoic",
        "accent": "mountainous",
        "emphasis_level": 1,
        "enabled": True,
        "allow_player_override": True,
        "funding_preference": "player_first",
        "voice_aliases": {
            "stone_guardian": "male-steady-1",
            "mountain_voice": "male-deep-1",
            "cavern_echo": "male-whisper-1"
        },
        "reputation_voices": {
            "0": "male-whisper-1",
            "25": "male-steady-1",
            "75": "male-steady-divine-1"
        },
        "time_voices": {
            "day": "male-steady-1",
            "night": "male-deep-1"
        },
        "biome_voices": {
            "minecraft:mountains": "male-deep-1",
            "minecraft:stone_shore": "male-steady-1",
            "minecraft:dripstone_caves": "male-whisper-1"
        },
        "advanced_params": {
            "stone_resonance": 0.4,
            "mountain_echo": 0.3,
            "earthen_filter": True
        }
    },

    "air_deity": {
        "voice_id": "female-light-1",
        "backup_voice": "female-flowing-1",
        "pitch": 1.2,
        "speed": 1.1,
        "volume": 0.9,
        "emotion": "ethereal",
        "accent": "windswept",
        "emphasis_level": 0,
        "enabled": True,
        "allow_player_override": True,
        "funding_preference": "player_first",
        "voice_aliases": {
            "wind_spirit": "female-light-1",
            "sky_whisper": "female-flowing-1",
            "storm_voice": "female-intense-1"
        },
        "reputation_voices": {
            "0": "female-whisper-1",
            "25": "female-light-1",
            "75": "female-light-divine-1"
        },
        "time_voices": {
            "day": "female-light-1",
            "night": "female-flowing-1"
        },
        "biome_voices": {
            "minecraft:windswept_hills": "female-light-1",
            "minecraft:windswept_forest": "female-flowing-1",
            "minecraft:peaks": "female-intense-1"
        },
        "advanced_params": {
            "wind_filter": True,
            "atmospheric_echo": 0.2,
            "breeze_modulation": 0.1
        }
    },

    "nature_deity": {
        "voice_id": "female-natural-1",
        "backup_voice": "female-warm-1",
        "pitch": 1.0,
        "speed": 0.9,
        "volume": 1.0,
        "emotion": "nurturing",
        "accent": "forest",
        "emphasis_level": 0,
        "enabled": True,
        "allow_player_override": True,
        "funding_preference": "player_first",
        "voice_aliases": {
            "forest_spirit": "female-natural-1",
            "green_whisper": "female-warm-1",
            "wild_voice": "female-flowing-1"
        },
        "reputation_voices": {
            "0": "female-whisper-1",
            "25": "female-natural-1",
            "75": "female-natural-divine-1"
        },
        "time_voices": {
            "day": "female-natural-1",
            "night": "female-warm-1"
        },
        "biome_voices": {
            "minecraft:forest": "female-natural-1",
            "minecraft:jungle": "female-flowing-1",
            "minecraft:flower_forest": "female-warm-1",
            "minecraft:birch_forest": "female-light-1"
        },
        "advanced_params": {
            "nature_harmony": 0.3,
            "forest_echo": 0.2,
            "growth_resonance": True
        }
    },

    "end_deity": {
        "voice_id": "male-whisper-1",
        "backup_voice": "male-deep-1",
        "pitch": 0.8,
        "speed": 0.8,
        "volume": 0.9,
        "emotion": "ethereal",
        "accent": "void",
        "emphasis_level": 1,
        "enabled": True,
        "allow_player_override": True,
        "funding_preference": "player_first",
        "voice_aliases": {
            "void_whisper": "male-whisper-1",
            "end_echo": "male-deep-1",
            "dimension_voice": "male-ethereal-1"
        },
        "reputation_voices": {
            "0": "male-whisper-1",
            "25": "male-deep-1",
            "75": "male-ethereal-divine-1"
        },
        "time_voices": {
            "day": "male-whisper-1",
            "night": "male-deep-1"
        },
        "biome_voices": {
            "minecraft:the_end": "male-ethereal-1",
            "minecraft:end_highlands": "male-deep-1",
            "minecraft:end_midlands": "male-whisper-1"
        },
        "advanced_params": {
            "void_resonance": 0.5,
            "dimensional_echo": 0.4,
            "end_distortion": True
        }
    },

    "nether_deity": {
        "voice_id": "male-intense-1",
        "backup_voice": "male-deep-1",
        "pitch": 0.9,
        "speed": 1.0,
        "volume": 1.2,
        "emotion": "fierce",
        "accent": "infernal",
        "emphasis_level": 2,
        "enabled": True,
        "allow_player_override": True,
        "funding_preference": "player_first",
        "voice_aliases": {
            "nether_lord": "male-intense-1",
            "infernal_voice": "male-deep-1",
            "hell_whisper": "male-whisper-1"
        },
        "reputation_voices": {
            "0": "male-whisper-1",
            "25": "male-intense-1",
            "75": "male-intense-divine-1"
        },
        "time_voices": {
            "day": "male-intense-1",
            "night": "male-deep-1"
        },
        "biome_voices": {
            "minecraft:nether_wastes": "male-intense-1",
            "minecraft:soul_sand_valley": "male-whisper-1",
            "minecraft:crimson_forest": "male-deep-1",
            "minecraft:warped_forest": "male-ethereal-1"
        },
        "advanced_params": {
            "infernal_echo": 0.4,
            "nether_distortion": 0.3,
            "hell_fire_filter": True
        }
    },

    "overworld_deity": {
        "voice_id": "neutral-1",
        "backup_voice": "male-steady-1",
        "pitch": 1.0,
        "speed": 1.0,
        "volume": 1.0,
        "emotion": "balanced",
        "accent": "worldly",
        "emphasis_level": 0,
        "enabled": True,
        "allow_player_override": True,
        "funding_preference": "player_first",
        "voice_aliases": {
            "world_guardian": "neutral-1",
            "realm_voice": "male-steady-1",
            "creation_whisper": "female-warm-1"
        },
        "reputation_voices": {
            "0": "neutral-1",
            "25": "male-steady-1",
            "75": "divine-balanced-1"
        },
        "time_voices": {
            "day": "male-steady-1",
            "night": "female-warm-1"
        },
        "biome_voices": {
            "minecraft:plains": "neutral-1",
            "minecraft:forest": "female-natural-1",
            "minecraft:mountains": "male-steady-1"
        },
        "advanced_params": {
            "world_harmony": 0.3,
            "balance_filter": True,
            "creation_resonance": 0.2
        }
    },

    "twilight_deity": {
        "voice_id": "female-ethereal-1",
        "backup_voice": "female-whisper-1",
        "pitch": 0.9,
        "speed": 0.9,
        "volume": 0.9,
        "emotion": "mysterious",
        "accent": "twilight",
        "emphasis_level": 1,
        "enabled": True,
        "allow_player_override": True,
        "funding_preference": "player_first",
        "voice_aliases": {
            "dusk_spirit": "female-ethereal-1",
            "twilight_whisper": "female-whisper-1",
            "shadow_light": "female-flowing-1"
        },
        "reputation_voices": {
            "0": "female-whisper-1",
            "25": "female-ethereal-1",
            "75": "female-ethereal-divine-1"
        },
        "time_voices": {
            "day": "female-light-1",
            "evening": "female-ethereal-1",
            "night": "female-whisper-1"
        },
        "biome_voices": {
            "twilightforest:twilight_forest": "female-ethereal-1",
            "twilightforest:dark_forest": "female-whisper-1",
            "twilightforest:enchanted_forest": "female-flowing-1"
        },
        "advanced_params": {
            "twilight_filter": True,
            "dusk_resonance": 0.3,
            "liminal_echo": 0.2
        }
    }
}

def add_tts_config_to_file(filepath, deity_key):
    """Add TTS config to a deity JSON file"""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()

        # Find the api_settings section and add TTS config after it
        api_settings_pattern = r'(\s*"api_settings":\s*{[^}]*}\s*},)'
        tts_config = TTS_CONFIGS.get(deity_key, TTS_CONFIGS['overworld_deity'])  # Default fallback

        tts_json = json.dumps({"tts_config": tts_config}, indent=2)
        # Remove the outer braces and adjust indentation
        tts_section = tts_json[1:-1].replace('  "tts_config":', '  "tts_config":')

        replacement = r'\1\n\n' + tts_section + ','

        if re.search(api_settings_pattern, content, re.DOTALL):
            new_content = re.sub(api_settings_pattern, replacement, content, flags=re.DOTALL)

            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(new_content)
            print(f"[OK] Updated {deity_key}")
        else:
            print(f"[SKIP] Could not find api_settings in {deity_key}")

    except Exception as e:
        print(f"[ERROR] Error updating {deity_key}: {e}")

def main():
    """Update all deity files with TTS configurations"""
    base_path = "src/main/resources/data/eidolonunchained/ai_deities"

    # List of deity files to update (excluding already updated ones)
    deities_to_update = [
        "water_deity.json",
        "earth_deity.json",
        "air_deity.json",
        "nature_deity.json",
        "end_deity.json",
        "nether_deity.json",
        "overworld_deity.json",
        "twilight_deity.json"
    ]

    for deity_file in deities_to_update:
        filepath = os.path.join(base_path, deity_file)
        deity_key = deity_file.replace('.json', '')

        if os.path.exists(filepath):
            add_tts_config_to_file(filepath, deity_key)
        else:
            print(f"[ERROR] File not found: {filepath}")

if __name__ == "__main__":
    main()