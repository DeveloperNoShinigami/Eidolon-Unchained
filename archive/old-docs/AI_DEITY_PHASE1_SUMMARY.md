# AI Deity System - Phase 1 Implementation Summary

## Overview
This Phase 1 implementation provides the foundation for AI-powered deities in Eidolon Unchained, allowing players to interact with datapack-defined deities through Google Gemini AI integration.

## Architecture

### Two-File System
The implementation uses a clean separation of concerns:

1. **Core Deity Definitions** (`data/modid/deities/`)
   - Basic deity properties (name, description, colors)
   - Progression stages and reputation requirements
   - Unlock rewards (signs, items, effects)
   - Prayer types allowed

2. **AI Configurations** (`data/modid/ai_deities/`)
   - Links to existing deities via ResourceLocation
   - Dynamic personality based on player progression
   - Prayer-specific prompts and command restrictions
   - Google Gemini API settings and safety controls

## Key Components

### Core Classes

- **DatapackDeityManager**: Loads deity definitions from JSON files
- **DatapackDeity**: Deity implementation with progression and rewards
- **AIDeityManager**: Loads AI configurations and links them to deities
- **AIDeityConfig**: Configuration classes for AI behavior
- **GeminiAPIClient**: Google Gemini API integration with safety features
- **PrayerSystem**: Handles player prayer requests and AI responses
- **PrayerCommands**: Command interface for deity interactions

### Data Structures

#### Example Deity Definition (`nature_deity.json`)
```json
{
  "id": "mymod:nature_deity",
  "name": "Verdania, Guardian of Nature",
  "description": "Ancient protector of forests and wildlife",
  "color": { "red": 34, "green": 139, "blue": 34 },
  "progression": {
    "max_reputation": 100,
    "stages": [
      {
        "id": "mymod:nature_novice",
        "reputation": 10,
        "major": false,
        "rewards": [
          { "type": "sign", "data": "mymod:nature_blessing" },
          { "type": "item", "data": "minecraft:oak_sapling", "count": 4 }
        ]
      }
    ]
  },
  "prayer_types": ["blessing", "guidance", "nature_ritual"]
}
```

#### Example AI Configuration (`nature_deity_ai.json`)
```json
{
  "deity_id": "mymod:nature_deity",
  "ai_provider": "gemini",
  "model": "gemini-1.5-pro",
  "personality": "You are Verdania, the ancient Nature Guardian...",
  "behavior_rules": {
    "reputation_thresholds": {
      "0": "You are cautious and distant...",
      "25": "You recognize them as a friend of nature..."
    },
    "biome_behaviors": {
      "minecraft:forest": "Here in my domain, among the ancient oaks...",
      "minecraft:desert": "Even in this seeming wasteland, life finds a way..."
    }
  },
  "prayer_configs": {
    "blessing": {
      "base_prompt": "Player {player} asks for your blessing...",
      "max_commands": 2,
      "cooldown_minutes": 30,
      "reputation_required": 5,
      "allowed_commands": ["give", "effect", "summon", "playsound"]
    }
  }
}
```

## Usage

### For Players
```
/pray mymod:nature_deity blessing
/pray mymod:nature_deity guidance "Help me find rare plants"
/deities                    # List all available deities
/deity mymod:nature_deity   # Show deity information and your progress
```

### For Modpack/Server Developers

1. **Create deity definitions** in `data/modid/deities/`
2. **Configure AI behavior** in `data/modid/ai_deities/`
3. **Set environment variable** `GEMINI_API_KEY` with your Google AI Studio API key
4. **Customize prayer types** and command restrictions as needed

## Security Features

- **Command Whitelisting**: Only specified commands can be executed per prayer type
- **Reputation Gating**: Prayer types require minimum reputation levels
- **Cooldown System**: Prevents spam and rate limiting issues
- **Google Gemini Safety**: Built-in content filtering and safety settings
- **Permission Levels**: Deity commands execute with controlled op permissions

## Integration Points

- **Eidolon Reputation System**: Uses existing deity reputation mechanics
- **Eidolon Signs**: Rewards can grant mystical knowledge signs
- **Minecraft Commands**: AI responses can trigger vanilla and modded commands
- **Datapack Reloading**: Hot-reload support for both deity and AI configurations

## Phase 1 Limitations

- **Basic Prayer System**: Simple command-based interaction (GUI planned for Phase 2)
- **Static Context**: Limited player context passed to AI (research integration planned)
- **Single AI Provider**: Only Google Gemini (OpenAI/Anthropic support planned)
- **No Visual Effects**: Text-based responses only (particle effects planned)

## Environment Setup

1. Get a Google AI Studio API key from https://makersuite.google.com/app/apikey
2. Set environment variable: `export GEMINI_API_KEY="your-api-key-here"`
3. Create deity and AI configuration files in your datapack
4. Use `/reload` to load configurations
5. Test with `/pray` commands

## Next Steps (Phase 2+)

- Prayer GUI with contextual options
- Integration with Eidolon research system  
- Multiple AI provider support
- Advanced event listeners for dynamic deity responses
- Visual and audio enhancements
- Player action tracking for better AI context

## File Structure
```
data/
├── modid/
│   ├── deities/
│   │   └── nature_deity.json        # Core deity definition
│   └── ai_deities/
│       └── nature_deity_ai.json     # AI behavior configuration
└── ...

src/main/java/com/bluelotuscoding/eidolonunchained/
├── deity/
│   ├── DatapackDeityManager.java    # Deity loader
│   └── DatapackDeity.java           # Deity implementation
├── ai/
│   ├── AIDeityManager.java          # AI config loader
│   └── AIDeityConfig.java           # AI configuration classes
├── integration/gemini/
│   └── GeminiAPIClient.java         # Google Gemini API client
├── prayer/
│   └── PrayerSystem.java            # Prayer handling system
├── command/
│   └── PrayerCommands.java          # Command interface
└── AIDeityIntegration.java          # Main integration class
```

This Phase 1 implementation provides a solid foundation for AI-powered deity interactions while maintaining clean separation between core functionality and AI behavior configuration.
