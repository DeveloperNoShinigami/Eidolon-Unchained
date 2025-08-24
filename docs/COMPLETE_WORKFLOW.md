# 🔮 Eidolon Unchained: Complete Workflow Guide

## Overview

This document explains the complete workflow for using the new unified chant and AI deity system in Eidolon Unchained.

## 🎯 **Core Workflow**

### Step 1: Configuration
All settings are now consolidated in `config/eidolonunchained-common.toml`:

```toml
# ═══════════════════════════════════════════════════════════════════════
# AI DEITY SYSTEM CONFIGURATION
# Configure AI-powered deity interactions and responses
# ═══════════════════════════════════════════════════════════════════════
[ai_deities]
    # Enable AI-powered deity conversations and interactions
    enable_ai_deities = true
    # AI provider to use (gemini, openai, proxy)
    ai_provider = "gemini"
    # Google Gemini API key (leave empty to use environment variable EIDOLON_GEMINI_API_KEY)
    gemini_api_key = ""

# ═══════════════════════════════════════════════════════════════════════
# CHANT SYSTEM CONFIGURATION
# Configure the datapack chant system and sign combinations
# ═══════════════════════════════════════════════════════════════════════
[chant_system]
    # Enable the custom chant system
    enable_chant_system = true
    # Allow chants to be defined in datapacks (data/modid/chants/)
    enable_datapack_chants = true
    # Show custom chants in the Eidolon codex
    show_chants_in_codex = true

# ═══════════════════════════════════════════════════════════════════════
# DEITY INTERACTION CONFIGURATION
# Configure how players interact with deities
# ═══════════════════════════════════════════════════════════════════════
[deity_interaction]
    # Enable deity interaction by right-clicking effigies (legacy mode)
    enable_effigy_right_click = false
    # Enable deity conversation through chat after chant completion
    enable_chat_interaction = true
    # Require chant completion before deity interaction
    require_chant_completion = true
```

### Step 2: API Key Setup
Use the unified command system:

```bash
# Set up your API key
/eidolon-unchained api set gemini YOUR_API_KEY_HERE

# Test the connection
/eidolon-unchained api test gemini

# Check status
/eidolon-unchained config status
```

### Step 3: Chant System
The new datapack chant system allows you to define custom chants in `data/modid/chants/`:

#### Example Chant (`data/eidolonunchained/chants/nature_blessing.json`):
```json
{
  "name": "Nature's Blessing",
  "description": "Commune with the spirits of nature to receive their blessing",
  "category": "nature",
  "difficulty": 2,
  "show_in_codex": true,
  "signs": [
    "eidolon:harmony",
    "eidolon:soul", 
    "eidolon:sacred"
  ],
  "requirements": [
    "reputation:eidolonunchained:nature_deity:10"
  ],
  "effects": [
    {
      "type": "start_conversation",
      "deity": "eidolonunchained:nature_deity",
      "prayer_type": "blessing"
    },
    {
      "type": "send_message",
      "message": "§aThe spirits of nature stir... speak your intentions."
    }
  ]
}
```

## 🎮 **In-Game Workflow**

### 1. Perform the Chant
- Use Eidolon's spell table or signs to perform the exact sequence
- Example: Place signs in order: HARMONY → SOUL → SACRED
- Cast the spell using Eidolon's normal casting mechanics

### 2. Chant Completion Triggers AI
- When the chant completes successfully, the AI deity system activates
- You'll see a message like: "§aThe spirits of nature stir... speak your intentions."
- You're now in conversation mode with the deity

### 3. Chat with the Deity
- Type in chat normally to talk with the AI deity
- Example: "Hello nature spirit, I seek guidance for my crops"
- The AI will respond with contextually appropriate dialogue

### 4. Conversation Ends
- Conversations automatically timeout after 10 minutes (configurable)
- Or type "goodbye" to end early

## 🛠️ **Command Reference**

### Configuration Commands
```bash
/eidolon-unchained config status      # Show all configuration
/eidolon-unchained config reload     # Reload configuration 
/eidolon-unchained config validate   # Check configuration validity
/eidolon-unchained config reset      # Reset to defaults
```

### API Management
```bash
/eidolon-unchained api set <provider> <key>     # Set API key
/eidolon-unchained api test <provider>          # Test API connection
/eidolon-unchained api list                     # List configured providers
/eidolon-unchained api remove <provider>        # Remove API key
```

### Deity Management  
```bash
/eidolon-unchained deities list                 # List all deities
/eidolon-unchained deities reload               # Reload deity data
/eidolon-unchained deities status <deity>       # Show deity status
```

### Chant System
```bash
/eidolon-unchained chants list                  # List all chants
/eidolon-unchained chants reload                # Reload chant data
/eidolon-unchained chants generate              # Generate chant recipes
/eidolon-unchained chants test <chant>          # Test a specific chant
```

### Prayer System
```bash
/eidolon-unchained prayers history              # Show your prayer history
/eidolon-unchained prayers clear-cooldown <player>  # Clear prayer cooldown
```

### Debug Commands
```bash
/eidolon-unchained debug toggle                # Toggle debug mode
/eidolon-unchained debug logs                  # Show debug logs
/eidolon-unchained debug validate-all          # Validate all systems
```

### Convenience Aliases
```bash
/eu                    # Short alias for /eidolon-unchained
/eidolon-config        # Alias for /eidolon-unchained config
```

## 📋 **Chant Effect Types**

When creating custom chants, you can use these effect types:

### `start_conversation`
Begins an AI conversation with a deity:
```json
{
  "type": "start_conversation",
  "deity": "eidolonunchained:nature_deity",
  "prayer_type": "blessing"
}
```

### `give_item`
Gives items to the player:
```json
{
  "type": "give_item",
  "item": "minecraft:oak_sapling",
  "count": 3
}
```

### `apply_effect`
Applies potion effects:
```json
{
  "type": "apply_effect",
  "effect": "minecraft:regeneration",
  "duration": 200,
  "amplifier": 0
}
```

### `run_command`
Executes a server command:
```json
{
  "type": "run_command",
  "command": "give @p minecraft:diamond 1"
}
```

### `send_message`
Sends a message to the player:
```json
{
  "type": "send_message",
  "message": "§aA gentle warmth flows through your body."
}
```

## 🔧 **Customization**

### Adding New Deities
1. Create deity definition in `data/modid/deities/my_deity.json`
2. Create AI configuration in `data/modid/ai_deities/my_deity_ai.json`
3. Create chants that reference your deity in `data/modid/chants/`

### Adding New Chants
1. Create JSON file in `data/modid/chants/my_chant.json`
2. Define sign sequence using valid Eidolon signs
3. Add effects that occur when chant completes
4. Reload chants: `/eidolon-unchained chants reload`

### Chant Categories
Organize your chants with categories that appear in the codex:
- `basic` - Simple utility chants
- `nature` - Nature-themed chants
- `dark` - Shadow/death magic
- `light` - Holy/protection magic
- `custom` - Your own categories

## 🚀 **Migration from Old System**

If you were using the old system:

1. **Configuration**: Settings moved from multiple files to `eidolonunchained-common.toml`
2. **Commands**: All commands unified under `/eidolon-unchained` or `/eu`
3. **Effigy Interaction**: Now optional and disabled by default
4. **Chant Triggering**: AI now triggers after successful chant completion
5. **API Keys**: Managed through unified system with multiple options

## 🔍 **Troubleshooting**

### Chants Don't Work
1. Check signs are spelled correctly (`eidolon:harmony` not `harmony`)
2. Verify chant is loaded: `/eidolon-unchained chants list`
3. Check requirements are met
4. Enable debug mode: `/eidolon-unchained debug toggle`

### AI Doesn't Respond
1. Check API key: `/eidolon-unchained api test gemini`
2. Verify deity has AI config: `/eidolon-unchained deities list`
3. Check chat interaction is enabled in config
4. Validate configuration: `/eidolon-unchained config validate`

### Commands Not Found
1. Ensure you have OP permissions
2. Check required OP level in config (default: 4)
3. Try aliases: `/eu` or `/eidolon-config`
4. Check logs for registration errors

## 📖 **Examples**

### Complete Example: Creating a Fire Deity

1. **Deity Definition** (`data/mypack/deities/fire_deity.json`):
```json
{
  "id": "mypack:fire_deity",
  "name": "Ignis the Flame Lord",
  "description": "Master of fire and forge",
  "colors": {"red": 255, "green": 69, "blue": 0},
  "progression": {
    "max_reputation": 100,
    "stages": [
      {
        "id": "mypack:flame_apprentice",
        "reputation": 10,
        "major": false,
        "rewards": [
          {"type": "item", "data": "minecraft:flint_and_steel", "count": 1}
        ]
      }
    ]
  },
  "prayer_types": ["conversation", "blessing", "knowledge", "forge"]
}
```

2. **AI Configuration** (`data/mypack/ai_deities/fire_deity_ai.json`):
```json
{
  "deity_id": "mypack:fire_deity",
  "ai_provider": "gemini",
  "model": "gemini-1.5-flash",
  "personality": "You are Ignis, Lord of Flames...",
  "chant_sequences": [
    {
      "name": "Flame Communion",
      "description": "Speak with the Flame Lord",
      "signs": ["eidolon:wicked", "eidolon:sacred", "eidolon:wicked"],
      "prayer_type": "conversation"
    }
  ],
  "base_prompt": {
    "conversation": "Speak with authority about fire and forge..."
  }
}
```

3. **Custom Chant** (`data/mypack/chants/flame_communion.json`):
```json
{
  "name": "Flame Communion",
  "description": "Reach into the heart of fire to speak with Ignis",
  "category": "fire",
  "difficulty": 3,
  "signs": ["eidolon:wicked", "eidolon:sacred", "eidolon:wicked"],
  "effects": [
    {
      "type": "start_conversation",
      "deity": "mypack:fire_deity",
      "prayer_type": "conversation"
    },
    {
      "type": "apply_effect",
      "effect": "minecraft:fire_resistance",
      "duration": 600,
      "amplifier": 0
    }
  ]
}
```

4. **In-Game Usage**:
```bash
# Setup (server operator)
/eidolon-unchained api set gemini YOUR_KEY
/eidolon-unchained chants reload

# Player performs chant: WICKED → SACRED → WICKED
# After successful cast:
# Player: "Ignis, I seek knowledge of the forge"
# AI: "Mortal, the flames whisper of ancient techniques..."
```

This unified system provides much more flexibility and proper integration with Eidolon's existing spell system!
