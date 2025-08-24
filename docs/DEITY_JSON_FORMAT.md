# Deity JSON Format Specification

This document defines the correct JSON format for deity files that the code expects.

## File Locations

- **Deity definitions**: `src/main/resources/data/eidolonunchained/deities/`
- **AI configurations**: `src/main/resources/data/eidolonunchained/ai_deities/`

## Deity Definition Format (`deities/*.json`)

```json
{
  "id": "eidolonunchained:deity_name",
  "name": "Display Name",
  "description": "Description of the deity's nature and powers",
  "colors": {
    "red": 255,
    "green": 100,
    "blue": 50
  },
  "progression": {
    "max_reputation": 100,
    "stages": [
      {
        "id": "eidolonunchained:stage_name",
        "reputation": 10,
        "major": false,
        "rewards": [
          { "type": "item", "data": "minecraft:item_id", "count": 3 },
          { "type": "effect", "data": "minecraft:effect_id", "duration": 300, "amplifier": 0 },
          { "type": "sign", "data": "eidolon:sign_id" }
        ]
      }
    ]
  },
  "prayer_types": ["conversation", "blessing", "knowledge", "balance"]
}
```

## AI Configuration Format (`ai_deities/*.json`)

```json
{
  "deity_id": "eidolonunchained:deity_name",
  "ai_provider": "gemini",
  "model": "gemini-1.5-pro",
  "personality": "Base personality description for the deity",
  
  "chant_sequences": [
    {
      "name": "Chant Name",
      "description": "What this chant does",
      "signs": ["eidolon:wicked", "eidolon:sacred", "eidolon:harmony"],
      "prayer_type": "conversation"
    }
  ],
  
  "base_prompt": {
    "conversation": "You are [Deity]. Respond to conversations...",
    "blessing": "You are [Deity]. Grant blessings...",
    "knowledge": "You are [Deity]. Share wisdom...",
    "balance": "You are [Deity]. Restore balance..."
  },
  
  "api_settings": {
    "model": "gemini-1.5-flash",
    "timeoutSeconds": 30,
    "generation_config": {
      "temperature": 0.7,
      "max_output_tokens": 500
    },
    "safety_settings": {
      "harassment": "BLOCK_MEDIUM_AND_ABOVE",
      "hate_speech": "BLOCK_MEDIUM_AND_ABOVE"
    }
  }
}
```

## Field Explanations

### Deity Definition Fields

- **`id`**: Unique identifier matching the AI config's `deity_id`
- **`name`**: Display name shown to players
- **`description`**: Lore description of the deity
- **`colors`**: RGB values for deity-related UI elements
- **`progression.max_reputation`**: Maximum reputation players can achieve
- **`progression.stages`**: Array of reputation milestones with rewards
- **`prayer_types`**: Array of supported prayer types (must match AI config)

### Progression Stage Fields

- **`id`**: Unique identifier for this progression stage
- **`reputation`**: Reputation level required to unlock this stage
- **`major`**: Whether this is a major milestone (affects UI)
- **`rewards`**: Array of rewards given when stage is unlocked

### Reward Types

- **`item`**: Give items to player
  - `data`: Item ID (e.g., "minecraft:diamond")
  - `count`: Number of items to give
- **`effect`**: Apply potion effects
  - `data`: Effect ID (e.g., "minecraft:regeneration")
  - `duration`: Duration in ticks (20 ticks = 1 second)
  - `amplifier`: Effect level (0 = level 1)
- **`sign`**: Grant Eidolon signs/knowledge
  - `data`: Sign ID from Eidolon mod

## Prayer Types

The system supports these prayer types (must be consistent between deity and AI config):

- **`conversation`**: General chat/communication with deity
- **`blessing`**: Request boons, buffs, or beneficial effects
- **`knowledge`**: Ask for information, guidance, or wisdom
- **`balance`**: Request intervention to restore natural/moral balance
- **`judgment`**: Seek divine judgment or justice (dark deities)

## Important Notes

1. **IDs must match**: The `id` in deity config must match `deity_id` in AI config
2. **Prayer types must align**: Both files must have matching prayer type arrays
3. **Sign validation**: Only use sign IDs that exist in the Eidolon mod
4. **Item validation**: Ensure item IDs are valid for the mod pack
5. **Color range**: RGB values must be 0-255

## Migration from Legacy Format

If you have old format files with `stage_rewards` and `reputation_stages`, convert them:

**Old Format:**
```json
{
  "stage_rewards": {
    "eidolonunchained:stage_name": [
      "give @p minecraft:diamond 3",
      "effect give @p minecraft:strength 600 1"
    ]
  },
  "reputation_stages": {
    "10": "eidolonunchained:stage_name"
  }
}
```

**New Format:**
```json
{
  "progression": {
    "max_reputation": 100,
    "stages": [
      {
        "id": "eidolonunchained:stage_name",
        "reputation": 10,
        "major": false,
        "rewards": [
          { "type": "item", "data": "minecraft:diamond", "count": 3 },
          { "type": "effect", "data": "minecraft:strength", "duration": 600, "amplifier": 1 }
        ]
      }
    ]
  }
}
```

## Validation

After editing JSON files:

1. **Syntax check**: Ensure valid JSON syntax
2. **Build test**: Run `./gradlew build` to check for errors
3. **In-game test**: Use `/eidolon-config validate-all` to test configurations
4. **Chant test**: Perform chant sequences to verify AI responses

## Current Examples

The mod includes three working examples:

- **`dark_deity.json`** + **`dark_deity_ai.json`**: Dark/shadow themed deity
- **`light_deity.json`** + **`light_deity_ai.json`**: Light/holy themed deity  
- **`nature_deity.json`** + **`nature_deity_ai.json`**: Nature/balance themed deity

Use these as templates for creating new deities.
