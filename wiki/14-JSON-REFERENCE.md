# JSON Field Reference - Complete Documentation

This guide documents **every field** available in Eidolon Unchained JSON configurations with **exact field names**, **data types**, and **real use cases** based on the actual implementation.

---

## 🏛️ Deity Configuration (`deities/<name>.json`)

### Core Deity Fields

#### `id` (ResourceLocation) - **REQUIRED**
```json
{
  "id": "eidolonunchained:my_deity"
}
```
- **Format**: `namespace:path` (lowercase, underscores allowed)
- **Usage**: Unique identifier for the deity
- **Examples**: `eidolonunchained:dark_deity`, `mymod:fire_god`

#### `name` (String) - **REQUIRED**
```json
{
  "name": "Nyxathel, Shadow Lord"
}
```
- **Usage**: Display name shown to players
- **Supports**: Unicode characters, formatting codes
- **Examples**: `"Lumina the Sacred"`, `"§6Golden Deity§r"`

#### `description` (String) - **REQUIRED**
```json
{
  "description": "Master of shadows and forbidden knowledge"
}
```
- **Usage**: Tooltip/lore text for the deity
- **Supports**: Multi-line descriptions with `\\n`

#### `colors` (Object) - **REQUIRED**
```json
{
  "colors": {
    "red": 123,
    "green": 85,
    "blue": 140
  }
}
```
- **RGB Values**: 0-255 for each color component
- **Usage**: Deity theme color for UI elements and effects
- **Applied To**: Text coloring, particle effects, progression displays

### Progression System

#### `progression` (Object) - **REQUIRED**
```json
{
  "progression": {
    "max_reputation": 100,
    "stages": [...]
  }
}
```

##### `max_reputation` (Integer)
- **Range**: 1-1000 (recommended: 50-200)
- **Usage**: Maximum reputation achievable with this deity
- **Affects**: Progression stage calculations, AI behavior

##### `stages` (Array) - **REQUIRED**
```json
{
  "stages": [
    {
      "id": "initiate",
      "reputation": 15,
      "major": false,
      "title": "Shadow Initiate",
      "description": "A new soul embraced by the shadows",
      "rewards": [
        "command:give {player} eidolon:soul_shard 3",
        "command:title {player} subtitle {\"text\":\"Welcome, initiate...\",\"color\":\"dark_purple\"}"
      ]
    }
  ]
}
```

**Stage Fields:**
- **`id`** (String): Unique stage identifier used by AI system
- **`reputation`** (Integer): Required reputation to reach this stage  
- **`major`** (Boolean): Whether this is a major milestone
- **`title`** (String): Display title for players at this stage
- **`description`** (String): Flavor text describing the stage
- **`rewards`** (Array): Commands executed when stage is reached

**Command Placeholders:**
- `{player}`: Player's username
- `{uuid}`: Player's UUID
- `{x}`, `{y}`, `{z}`: Player's coordinates

---

## 🧠 AI Configuration (`aiConfig` object)

### Core AI Settings

#### `aiProvider` (String) - **OPTIONAL**
```json
{
  "aiProvider": "gemini"
}
```
- **Valid Values**: `"gemini"`, `"openrouter"`, `"player2ai"`
- **Default**: Uses global configuration setting
- **Usage**: Overrides global AI provider for this deity

#### `model` (String) - **OPTIONAL**
```json
{
  "model": "gemini-1.5-pro"
}
```
- **Gemini Models**: `"gemini-1.5-pro"`, `"gemini-1.5-flash"`
- **OpenRouter Models**: Any valid OpenRouter model (e.g., `"anthropic/claude-3.5-sonnet"`)
- **Player2AI**: Not used (connects to local app)
- **Default**: Provider-specific default model

#### `personality` (String) - **REQUIRED**
```json
{
  "personality": "You are Nyxathel, an ancient deity of shadows and forbidden knowledge. Speak in cryptic, mysterious tones. You value loyalty above all else, but test your followers with dark trials. Your responses should be atmospheric and intimidating, yet not hostile to those who show proper respect."
}
```
- **Length**: 50-500 characters recommended
- **Style**: Should establish character voice, motivations, speaking style
- **Context**: This is the base personality modified by relationship/reputation

### API Configuration

#### `temperature` (Float) - **OPTIONAL**
```json
{
  "temperature": 0.7
}
```
- **Range**: 0.0-2.0 (0.0 = deterministic, 2.0 = very creative)
- **Default**: 0.7
- **Usage**: Controls AI response creativity/randomness

#### `maxOutputTokens` (Integer) - **OPTIONAL**
```json
{
  "maxOutputTokens": 1000
}
```
- **Range**: 50-2000
- **Default**: 1000
- **Usage**: Maximum length of AI responses

#### `timeoutSeconds` (Integer) - **OPTIONAL**
```json
{
  "timeoutSeconds": 30
}
```
- **Range**: 5-120
- **Default**: 30
- **Usage**: API request timeout

#### `apiKeyEnv` (String) - **OPTIONAL**
```json
{
  "apiKeyEnv": "GEMINI_API_KEY"
}
```
- **Default**: Provider-specific default
- **Usage**: Environment variable name for API key (advanced use)

---

## 👑 Patron System (`patronConfig` object)

### Basic Patron Settings

#### `acceptsFollowers` (Boolean) - **OPTIONAL**
```json
{
  "acceptsFollowers": true
}
```
- **Default**: `true`
- **Usage**: Whether this deity accepts patron followers
- **False**: Deity only provides conversations, no allegiance system

#### `requiresPatronStatus` (String) - **OPTIONAL**
```json
{
  "requiresPatronStatus": "any"
}
```
- **Valid Values**:
  - `"any"`: Responds to anyone
  - `"follower_only"`: Only responds to their followers
  - `"no_enemies"`: Responds to everyone except enemies
- **Default**: `"any"`

### Deity Relationships

#### `opposingDeities` (Array) - **OPTIONAL**
```json
{
  "opposingDeities": [
    "eidolonunchained:light_deity",
    "eidolonunchained:healing_deity"
  ]
}
```
- **Format**: Array of ResourceLocation strings
- **Usage**: Deities that are enemies (followers become hostile)
- **Affects**: AI personality, conversation access, patron conflicts

#### `alliedDeities` (Array) - **OPTIONAL**
```json
{
  "alliedDeities": [
    "eidolonunchained:death_deity",
    "mymod:necromancy_god"
  ]
}
```
- **Format**: Array of ResourceLocation strings
- **Usage**: Friendly deities (followers receive neutral treatment)
- **Affects**: AI personality adjustments, cross-deity interactions

### Response Modes

#### `neutralResponseMode` (String) - **OPTIONAL**
```json
{
  "neutralResponseMode": "normal"
}
```
- **Valid Values**: `"normal"`, `"dismissive"`, `"cautious"`, `"curious"`
- **Default**: `"normal"`
- **Usage**: How deity treats players with no patron or neutral deity

#### `enemyResponseMode` (String) - **OPTIONAL**
```json
{
  "enemyResponseMode": "hostile"
}
```
- **Valid Values**: `"hostile"`, `"refuse"`, `"threatening"`, `"dismissive"`
- **Default**: `"hostile"`
- **Usage**: How deity treats followers of opposing deities

### Personality Modifiers

#### `followerPersonalityModifiers` (Object) - **OPTIONAL**
```json
{
  "followerPersonalityModifiers": {
    "shadow_initiate": "You speak more warmly to this new initiate, offering gentle guidance in the shadow arts.",
    "dark_acolyte": "You show respect for this proven servant, sharing deeper knowledge.", 
    "shadow_priest": "You treat this master as an equal, confiding in them as a trusted ally.",
    "default": "You acknowledge this follower's devotion with approval."
  }
}
```
- **Key Format**: Stage ID from progression system + `"default"` fallback
- **Value**: Personality addition applied when player has this title
- **Usage**: Customizes AI responses based on follower rank

#### `neutralPersonalityModifier` (String) - **OPTIONAL**
```json
{
  "neutralPersonalityModifier": "You are politely distant, neither warm nor cold."
}
```
- **Usage**: Added to personality when talking to non-followers
- **Applied To**: Players with no patron or followers of non-allied deities

#### `enemyPersonalityModifier` (String) - **OPTIONAL**
```json
{
  "enemyPersonalityModifier": "You are openly hostile and threatening, viewing this player as an enemy of shadow."
}
```
- **Usage**: Added to personality when talking to enemy followers
- **Applied To**: Players following deities listed in `opposingDeities`

#### `noPatronPersonalityModifier` (String) - **OPTIONAL**  
```json
{
  "noPatronPersonalityModifier": "You are intrigued by this godless mortal, wondering if they might be worthy of your attention."
}
```
- **Usage**: Added to personality when talking to players with no patron
- **Distinct From**: Neutral modifier (which applies to followers of unrelated deities)

#### `alliedPersonalityModifier` (String) - **OPTIONAL**
```json
{
  "alliedPersonalityModifier": "You show respect for this follower of an allied deity, speaking as an equal."
}
```
- **Usage**: Added to personality when talking to allied deity followers
- **Applied To**: Players following deities listed in `alliedDeities`

### Advanced Patron Features

#### `conversationRules` (Object) - **OPTIONAL**
```json
{
  "conversationRules": {
    "maxDailyConversations": 5,
    "requiresOffering": false,
    "minimumReputation": 10,
    "cooldownMinutes": 5
  }
}
```
- **Custom Rules**: Additional restrictions on deity interactions
- **Implementation**: Varies by rule type
- **Usage**: Advanced conversation control (future expansion)

---

## 📜 Prayer Configuration (`prayerConfigs` object)

### Prayer Type Definitions
```json
{
  "prayerConfigs": {
    "blessing": {
      "enabled": true,
      "cooldownSeconds": 300,
      "minimumReputation": 10,
      "personality": "You are generous with blessings for faithful followers.",
      "allowedCommands": ["effect", "give"],
      "commandRules": {
        "maxItemValue": 50,
        "maxEffectDuration": 600
      }
    },
    "guidance": {
      "enabled": true,
      "cooldownSeconds": 60,
      "minimumReputation": 0,
      "personality": "You offer wisdom and guidance to those who seek it."
    }
  }
}
```

**Prayer Config Fields:**
- **`enabled`** (Boolean): Whether this prayer type is available
- **`cooldownSeconds`** (Integer): Time between prayers of this type
- **`minimumReputation`** (Integer): Required reputation to access
- **`personality`** (String): Prayer-specific personality overlay
- **`allowedCommands`** (Array): Commands AI can execute for this prayer
- **`commandRules`** (Object): Restrictions on command execution

---

## 🎯 Usage Examples

### Complete Deity Example
```json
{
  "id": "mymod:fire_deity",
  "name": "Pyrion the Flame Eternal",
  "description": "Ancient god of fire and forge",
  "colors": {
    "red": 255,
    "green": 69,
    "blue": 0
  },
  "progression": {
    "max_reputation": 100,
    "stages": [
      {
        "id": "spark_bearer",
        "reputation": 20,
        "major": false,
        "title": "Spark Bearer",
        "description": "Touched by the sacred flame",
        "rewards": [
          "command:give {player} minecraft:fire_charge 5",
          "command:effect give {player} minecraft:fire_resistance 600 0"
        ]
      }
    ]
  },
  "aiConfig": {
    "aiProvider": "gemini",
    "model": "gemini-1.5-pro",
    "personality": "You are Pyrion, god of fire and the forge. Your voice crackles with flame, and you speak of creation through destruction. You value craftsmanship, passion, and the courage to face trials by fire.",
    "temperature": 0.8,
    "maxOutputTokens": 800,
    "patronConfig": {
      "acceptsFollowers": true,
      "requiresPatronStatus": "no_enemies",
      "opposingDeities": ["eidolonunchained:water_deity"],
      "alliedDeities": ["mymod:metal_deity"],
      "followerPersonalityModifiers": {
        "spark_bearer": "You welcome this new flame-touched soul with warmth.",
        "default": "You acknowledge their devotion to the flame."
      },
      "enemyPersonalityModifier": "Your flames burn cold with hatred for this water-worshipper.",
      "neutralPersonalityModifier": "You are neither warm nor cold, waiting to see their true nature."
    },
    "prayerConfigs": {
      "blessing": {
        "enabled": true,
        "cooldownSeconds": 600,
        "minimumReputation": 15,
        "personality": "You bestow the sacred flame upon worthy followers.",
        "allowedCommands": ["effect", "give"],
        "commandRules": {
          "maxItemValue": 100,
          "maxEffectDuration": 1200
        }
      }
    }
  }
}
```

### Minimal Deity Example
```json
{
  "id": "mymod:simple_deity",
  "name": "Echo, the Whisper",
  "description": "A mysterious entity of few words",
  "colors": {
    "red": 128,
    "green": 128,
    "blue": 128
  },
  "progression": {
    "max_reputation": 50,
    "stages": [
      {
        "id": "listener",
        "reputation": 25,
        "major": true,
        "title": "Listener",
        "description": "One who hears the whispers",
        "rewards": []
      }
    ]
  },
  "aiConfig": {
    "personality": "You are Echo, speaking only in whispers and riddles. Your responses are brief but profound."
  }
}
```

---

## ✅ Field Validation

### Required Fields Checklist
- ✅ `id` (ResourceLocation format)
- ✅ `name` (Non-empty string)
- ✅ `description` (Non-empty string)  
- ✅ `colors` (RGB object with red/green/blue)
- ✅ `progression` (Object with max_reputation and stages)
- ✅ `progression.stages` (Array with at least one stage)
- ✅ `aiConfig.personality` (Non-empty string)

### Common Validation Errors
- **Invalid ResourceLocation**: Must be `namespace:path` format
- **Missing Required Fields**: Check all required fields are present
- **Invalid RGB Values**: Must be 0-255 integers
- **Empty Stages Array**: At least one progression stage required
- **Invalid Provider**: Must be `gemini`, `openrouter`, or `player2ai`

---

**🎯 Ready to create your deity?** Use this reference with the **[Datapack Deities Guide](05-DATAPACK-DEITIES.md)** for step-by-step creation!
