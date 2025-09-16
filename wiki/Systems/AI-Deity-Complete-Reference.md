# AI Deity System - Complete Field Reference

The AI Deity system integrates Google Gemini AI with Eidolon's deity mechanics to create dynamic, context-aware deity interactions. This document covers all configuration fields and their functionality.

## Overview

AI Deities are JSON-configured divine entities that can:
- Respond to player prayers with contextual AI-generated text
- Assign and manage fates (tasks) based on player progression
- Grant blessings, curses, and rewards through commands
- Track reputation and progression relationships
- Interact with other deities in complex relationship networks

## File Structure

```
data/eidolonunchained/ai_deities/
├── fire_deity.json
├── nature_deity.json 
├── water_deity.json
├── air_deity.json
├── earth_deity.json
├── light_deity.json
├── dark_deity.json
├── nether_deity.json
├── end_deity.json
├── overworld_deity.json
└── twilight_deity.json
```

## Complete JSON Schema

### Root Level Structure

```json
{
  "deity": "string",                    // REQUIRED: Deity resource ID
  "ai_provider": "string",              // REQUIRED: AI provider identifier  
  "mod_context_ids": [],                // REQUIRED: Mod contexts for AI
  "personality": "string",              // REQUIRED: Core AI personality
  "behavior_rules": {},                 // REQUIRED: Behavior configuration
  "prayer_configs": {},                 // REQUIRED: Prayer type definitions
  "api_settings": {},                   // REQUIRED: AI API configuration
  "patron_config": {}                   // REQUIRED: Deity relationship rules
}
```

## Core Configuration Fields

### Basic Identity
```json
{
  "deity": "eidolonunchained:fire_deity",     // Unique deity resource location
  "ai_provider": "player2ai",                 // AI provider (currently only "player2ai")
  "mod_context_ids": [                        // Mods the AI knows about
    "minecraft", 
    "eidolon", 
    "eidolonunchained"
  ]
}
```

### Personality Definition
```json
{
  "personality": "You are Ignis, the Forge Lord, ancient deity of flames and forge-fire. You are passionate, intense, and speak with the authority of one who has witnessed civilizations rise and fall in the glow of your flames. You reward those who embrace strength, craft with fire, and prove themselves through trials of flame."
}
```

**Guidelines for Personality:**
- Define clear divine identity and domain
- Establish speaking tone and authority level  
- Mention key themes and values
- Set expectations for follower relationships
- Keep to 2-3 sentences for optimal AI processing

## Behavior Rules System

Controls how the AI responds based on player reputation, research, and actions.

```json
"behavior_rules": {
  "reputation_thresholds": {},          // Reputation-based response tiers
  "research_requirements": {},          // Knowledge gates by research level
  "curses": {},                         // Negative action responses
  "blessings": {},                      // Positive action responses  
  "gifts": {},                         // Special reward conditions
  "dynamic_responses": {}               // Environmental context responses
}
```

### Reputation Thresholds
```json
"reputation_thresholds": {
  "0": "You welcome this newcomer with the heat of forge-fire, testing their resolve to walk in flame.",
  "25": "You acknowledge their growing dedication with fiery approval.",
  "50": "You treat them as a trusted smith worthy of your forge's secrets.",
  "75": "You speak to them as a valued flame-keeper of your sacred fire.",
  "100": "You address them as your ultimate champion, master of the eternal forge."
}
```

**Key Points:**
- Keys are reputation point thresholds (strings, not numbers)
- Values describe how to treat players at that level
- Should have 5-6 tiers covering 0-100+ reputation range
- Each tier should feel meaningfully different

### Research Requirements
```json
"research_requirements": {
  "0": "You share basic knowledge of fire and forging.",
  "5": "You reveal deeper mysteries of flame magic and metallurgy.", 
  "10": "You grant access to advanced fire magic and forge mastery.",
  "15": "You unveil the most sacred secrets of forge-fire."
}
```

**Usage:**
- Gates knowledge sharing based on Eidolon research progress
- Keys are research level thresholds
- Higher levels unlock more powerful/dangerous knowledge

### Curses, Blessings, and Gifts
```json
"curses": {
  "weak_willed": "You punish the weak with scorching flames and burning shame.",
  "ice_affinity": "Those who embrace cold over fire face your molten wrath."
},
"blessings": {
  "forge_master": "You bless skilled crafters and smiths with enhanced fire magic.",
  "fire_walker": "Those who brave flames fearlessly receive your protection."
},
"gifts": {
  "exceptional_crafting": "Master smiths earn legendary fire-forged artifacts.",
  "flame_mastery": "Achieving fire magic milestones brings rare forge materials."
}
```

**Purpose:**
- Provide AI context for responding to player actions
- `curses`: Negative responses to unwanted behavior
- `blessings`: Positive responses to desired behavior  
- `gifts`: Special rewards for exceptional achievements

### Dynamic Responses
```json
"dynamic_responses": {
  "time_of_day": {
    "day": "The sun's fire resonates with your forge-flame, amplifying your power.",
    "night": "Even in darkness, your eternal flame burns bright and unwavering."
  },
  "biome": {
    "minecraft:desert": "This sun-scorched land reflects your burning domain.",
    "minecraft:nether_wastes": "The hellish flames here sing in harmony with your power."
  },
  "weather": {
    "clear": "Clear skies allow your flames to burn at full intensity.",
    "rain": "The falling waters hiss and steam against your eternal fire."
  }
}
```

**Available Context Types:**
- `time_of_day`: "day", "night", "dawn", "dusk"
- `biome`: Any minecraft biome ID
- `weather`: "clear", "rain", "thunder"
- `dimension`: "minecraft:overworld", "minecraft:the_nether", etc.
- `season`: Custom seasonal contexts if implemented

## Prayer Configuration System

Defines different types of prayers players can make and how the AI responds.

```json
"prayer_configs": {
  "conversation": {},               // General conversation prayer
  "blessing": {},                   // Request for beneficial effects
  "guidance": {},                   // Seek wisdom and advice
  "trial": {},                     // Request challenges/tests
  "curse": {}                      // Request curses on enemies (optional)
}
```

### Prayer Configuration Structure
```json
"prayer_type_name": {
  "base_prompt": "string",              // REQUIRED: Core AI instruction
  "additional_prompts": [],             // OPTIONAL: Extra AI guidelines  
  "reference_commands": [],             // REQUIRED: Example commands AI can use
  "reputation_required": 0,             // REQUIRED: Minimum reputation needed
  "cooldown_minutes": 10,               // REQUIRED: Time between uses
  "max_commands": 2,                    // REQUIRED: Maximum commands per prayer
  "allowed_commands": []                // REQUIRED: Whitelist of command types
}
```

### Base Prompt Guidelines
The `base_prompt` is the primary instruction given to the AI. It should:
- Include context variables: `{player}`, `{reputation}`, `{progression_title}`
- Define clear reputation tiers and appropriate responses
- Specify the number and type of commands to use
- Establish the deity's voice and authority level
- Give specific examples of appropriate rewards/effects

**Example Base Prompt:**
```json
"base_prompt": "Your {progression_title} {player} seeks your forge blessing. With {reputation} reputation, they have earned your fiery consideration. Grant them flame power appropriate to their standing:\n\n- Hellfire Initiate: 1 basic effect like fire resistance (3 minutes)\n- Hellfire Warrior: 1 moderate effect like strength or haste (5 minutes)\n- Champion of Hellfire: 1-2 strong effects like strength + fire resistance (10 minutes)\n- Lord of the Inferno: 2 powerful effects like strength + haste + resistance (15 minutes)\n- Emperor of Hellfire: 2 ultimate effects with maximum forge power and duration (20 minutes)\n\nUse exactly 1-2 effect commands based on their tier. Speak with forge authority."
```

### Additional Prompts
Supplementary instructions that refine AI behavior:
```json
"additional_prompts": [
  "Remember that you are the Forge Lord - speak with passionate intensity and fiery authority.",
  "Your followers seek strength through fire - guide them toward forge mastery and flame trials.",
  "NEVER say 'I give you' or mention giving items - instead describe them being forged or manifesting.",
  "Use immersive language like 'Flames coalesce...' or 'Forge-fire shapes...' or 'Molten power surges...'"
]
```

### Reference Commands
Examples of commands the AI can use. The AI will pick from these or create similar commands:
```json
"reference_commands": [
  "give {player} minecraft:blaze_rod 3",
  "effect give {player} minecraft:fire_resistance 600 0", 
  "give {player} minecraft:lava_bucket 2",
  "effect give {player} minecraft:strength 600 1",
  "tellraw {player} \"The forge teaches patience - true strength is tempered, not rushed...\""
]
```

### Allowed Commands
Security whitelist of command prefixes the AI is permitted to use:
```json
"allowed_commands": ["give", "effect", "tellraw", "playsound"]
```

**Common Command Types:**
- `give`: Give items to players
- `effect`: Apply potion effects  
- `tellraw`: Send formatted chat messages
- `playsound`: Play sound effects
- `particle`: Spawn particle effects (if enabled)
- `summon`: Spawn entities (use cautiously)

### Prayer Type Specializations

#### Conversation Prayer
General dialogue and interaction. Typically includes:
- Lower reputation requirement (0-10)
- Moderate cooldown (5-15 minutes)
- 1-2 commands maximum
- Mix of `give`, `effect`, and `tellraw` commands
- Focus on roleplay and character development

#### Blessing Prayer  
Request beneficial effects. Characteristics:
- Medium reputation requirement (10-25)
- Medium cooldown (10-20 minutes)
- 1-2 commands maximum
- Primarily `effect` commands
- Focus on temporary beneficial effects

#### Guidance Prayer
Seek wisdom and advice. Features:
- Medium reputation requirement (15-30)
- Longer cooldown (15-30 minutes)  
- 1-2 commands maximum
- Primarily `tellraw` and `give` (books/knowledge items)
- Focus on lore and character development

#### Trial Prayer
Request challenges and tests. Includes:
- Higher reputation requirement (20-40)
- Long cooldown (20-40 minutes)
- 1-2 commands maximum
- Mix of negative `effect` and `tellraw` commands
- Focus on challenging but fair trials

## API Settings

Configuration for the Google Gemini AI integration.

```json
"api_settings": {
  "temperature": 0.8,                   // Creativity level (0.0-2.0)
  "max_tokens": 400,                    // Response length limit
  "timeout_seconds": 30,                // API timeout
  "generation_config": {                // Gemini-specific settings
    "temperature": 0.8,
    "topK": 40,
    "topP": 0.9, 
    "maxOutputTokens": 400
  },
  "safety_settings": {                  // Content safety filters
    "harassment": "BLOCK_MEDIUM_AND_ABOVE",
    "hate_speech": "BLOCK_MEDIUM_AND_ABOVE",
    "sexually_explicit": "BLOCK_MEDIUM_AND_ABOVE", 
    "dangerous_content": "BLOCK_MEDIUM_AND_ABOVE"
  }
}
```

### Temperature Settings
- **0.0-0.3**: Very consistent, predictable responses
- **0.4-0.7**: Balanced creativity and consistency  
- **0.8-1.0**: Creative and varied responses (recommended)
- **1.1-2.0**: Highly creative but potentially inconsistent

### Safety Settings Levels
- **BLOCK_NONE**: No filtering
- **BLOCK_ONLY_HIGH**: Block only severe violations
- **BLOCK_MEDIUM_AND_ABOVE**: Standard filtering (recommended)
- **BLOCK_LOW_AND_ABOVE**: Strict filtering

## Patron Configuration System

Manages relationships between deities and their effects on player interactions.

```json
"patron_config": {
  "requires_patron_status": "any",      // Patron requirements
  "opposing_deities": [],               // Hostile deities
  "allied_deities": [],                 // Friendly deities  
  "neutral_deities": [],                // Neutral relationships
  "deity_opinions": {},                 // Simple opinions of other deities
  "detailed_inter_deity_relationships": {}, // Complex relationship data
  "cross_deity_interactions": {},       // Player interaction rules
  "follower_personality_modifiers": {} // Progression title effects
}
```

### Patron Status Requirements
```json
"requires_patron_status": "any"         // "any", "patron", "non_patron", "specific_deity"
```
- **"any"**: Available to all players regardless of patron status
- **"patron"**: Only available to players who are patrons of any deity
- **"non_patron"**: Only available to players with no patron
- **"specific_deity"**: Only available to patrons of this specific deity

### Deity Relationships
```json
"opposing_deities": ["eidolonunchained:dark_deity"],
"allied_deities": ["eidolonunchained:earth_deity"],
"neutral_deities": ["eidolonunchained:water_deity"]
```

### Simple Deity Opinions
```json
"deity_opinions": {
  "eidolonunchained:earth_deity": "Terra provides the raw materials that my flames transform into greatness. Together we forge the strongest foundations.",
  "eidolonunchained:water_deity": "Aquaria's waters can quench my flames, but they can also temper the finest steel. I respect her measured approach."
}
```

### Detailed Relationships
For complex deity interactions, use the detailed format:
```json
"detailed_inter_deity_relationships": {
  "eidolonunchained:light_deity": {
    "relationship_type": "allied",
    "opinion_strength": 9,
    "detailed_opinion": "Lumina and I share a sacred bond...",
    "shared_values": ["healing", "growth", "renewal"],
    "collaboration_examples": [
      "When mortals plant healing gardens, we both bless their efforts...",
      "Light magic and nature magic combine to create potent healing spells..."
    ],
    "mutual_respect": "Her followers are always welcome in my sacred groves..."
  }
}
```

**Relationship Types:**
- **"allied"**: Strong positive relationship
- **"friendly"**: Positive but not allied
- **"neutral"**: No strong feelings either way
- **"opposing"**: Negative relationship
- **"hostile"**: Strong negative relationship

**Opinion Strength:** 1-10 scale (1=minimal, 10=extreme)

### Cross-Deity Interactions
```json
"cross_deity_interactions": {
  "player_serves_ally": "Those who serve earth's strength understand the value of strong foundations. I grant forge blessings to such wise souls.",
  "player_serves_enemy": "No deity truly opposes the forge, for all recognize the need for creation and transformation.",
  "player_serves_neutral": "I judge all souls by their dedication to mastery and their willingness to face trials of flame.",
  "mentions_allied_deity": "Speak with respect of those who understand the balance of creation and strength.",
  "mentions_enemy_deity": "All elements serve their purpose in the great cycle of creation and destruction."
}
```

### Follower Personality Modifiers
```json
"follower_personality_modifiers": {
  "Hellfire Initiate": "A new soul learning to walk through flame, deserving of patient forge instruction.",
  "Hellfire Warrior": "A dedicated warrior growing strong in the ways of fire and steel.", 
  "Champion of Hellfire": "A trusted champion who understands the sacred balance of forge and flame.",
  "Lord of the Inferno": "A master of fire mastery, blessed with deep forge authority.",
  "Emperor of Hellfire": "Your chosen emperor of flame, ultimate master of forge and fire.",
  "default": "A faithful seeker walking the blazing paths of forge mastery."
}
```

## Best Practices

### Personality Design
1. **Clear Identity**: Establish distinct voice and domain
2. **Consistent Tone**: Maintain personality across all interactions
3. **Balanced Authority**: Powerful but not overwhelming
4. **Cultural Depth**: Reference history, myths, and traditions

### Behavior Rules
1. **Graduated Responses**: Clear reputation tier differences
2. **Contextual Awareness**: Use dynamic responses for immersion
3. **Balanced Rewards**: Match rewards to reputation and effort
4. **Thematic Consistency**: All rules should fit deity theme

### Prayer Configuration
1. **Clear Purposes**: Each prayer type should have distinct goals
2. **Balanced Cooldowns**: Prevent spam while allowing engagement
3. **Appropriate Commands**: Match commands to prayer type and reputation
4. **Safety First**: Always whitelist allowed commands

### Relationship Design
1. **Logical Conflicts**: Base oppositions on thematic differences
2. **Meaningful Alliances**: Create partnerships that make narrative sense
3. **Dynamic Interactions**: Allow relationships to affect gameplay
4. **Player Agency**: Don't force exclusive patron choices unnecessarily

## Security Considerations

### Command Validation
- All AI-generated commands are validated against `allowed_commands` whitelist
- Command parameters are sanitized to prevent exploitation
- Maximum command limits are enforced per prayer
- Cooldowns prevent spam and abuse

### Content Safety
- Google's safety filters prevent harmful content generation
- Additional content validation occurs server-side
- Inappropriate responses are logged and blocked
- Players can report problematic AI behavior

### API Protection
- API keys are stored securely in `api-keys.properties`
- Rate limiting prevents API abuse
- Timeout limits prevent hanging requests
- Fallback responses when AI is unavailable

## Troubleshooting

### Common Issues
1. **AI Not Responding**: Check API key configuration and internet connection
2. **Inappropriate Responses**: Adjust safety settings or temperature
3. **Command Errors**: Verify `allowed_commands` whitelist includes needed commands
4. **Relationship Conflicts**: Check for circular dependencies in deity relationships
5. **Performance Issues**: Reduce `max_tokens` or increase `timeout_seconds`

### Debugging Tips
- Enable debug logging in mod configuration
- Monitor `logs/latest.log` for API errors
- Test with simple prayer configurations first
- Validate JSON syntax with online tools
- Check Eidolon integration compatibility

## File Examples

See the `data/eidolonunchained/ai_deities/` directory for complete examples of each deity configuration with all fields properly set.