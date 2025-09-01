# AI Deity System - Complete Guide

The AI Deity System is the core feature of Eidolon Unchained, providing **real-time conversations** with AI-powered deities that have **contextual awareness** of your player state, actions, and world conditions.

---

## 🧠 How It Works

### Architecture Overview
```
Player → Chant Sequence → Effigy Interaction → AI Provider → Contextual Response
```

1. **Player performs chant** near an effigy to "tune" it to a specific deity
2. **Right-click effigy** to start conversation with that deity  
3. **AI analyzes context**: health, inventory, reputation, location, time, weather
4. **AI generates response** based on deity personality and player relationship
5. **Response displayed** via configurable display system (title/chat)

---

## 🎭 AI Context Awareness

The AI has access to comprehensive player data:

### 📊 Player Status
- **Health & Hunger**: Current HP/food levels with emergency detection
- **Experience Level**: Player progression and capabilities
- **Active Effects**: Potions, curses, buffs currently applied
- **Equipment**: Notable items, enchanted gear, magical tools

### 🌍 World Context  
- **Location**: Biome, coordinates, nearby structures
- **Time & Weather**: Day/night cycle, rain, storms
- **Recent Actions**: Chants performed, research discovered
- **Reputation History**: Past interactions and standing

### 🏛️ Deity Relationships
- **Patron Status**: Current deity allegiance (if any)
- **Reputation Level**: Specific standing with each deity
- **Title Progression**: Current rank/title with patron deity
- **Opposing Deities**: Conflicts based on allegiance choices

---

## ⚙️ Configuration System

### Single-File Deity Configuration
All deity data is stored in unified JSON files at:
```
data/eidolonunchained/deities/<deity_name>.json
```

### Complete Configuration Example
```json
{
  "id": "eidolonunchained:dark_deity",
  "name": "Nyxathel, Shadow Lord",
  "description": "Master of shadows and forbidden knowledge",
  "colors": {
    "red": 123,
    "green": 85, 
    "blue": 140
  },
  "progression": {
    "max_reputation": 100,
    "stages": [
      {
        "id": "shadow_initiate",
        "reputation": 15,
        "title": "Shadow Initiate",
        "rewards": ["command:give {player} eidolon:soul_shard 3"]
      }
    ]
  },
  "aiConfig": {
    "aiProvider": "gemini",
    "model": "gemini-1.5-pro",
    "personality": "You are Nyxathel, an ancient deity of shadows and forbidden knowledge. Speak in cryptic, mysterious tones. You value loyalty but test your followers with dark trials.",
    "patronConfig": {
      "acceptsFollowers": true,
      "requiresPatronStatus": "any",
      "opposingDeities": ["eidolonunchained:light_deity"],
      "alliedDeities": [],
      "neutralResponseMode": "normal",
      "enemyResponseMode": "hostile",
      "followerPersonalityModifiers": {
        "shadow_initiate": "You speak more warmly to this new initiate, offering guidance.",
        "dark_acolyte": "You show respect for this proven servant.",
        "shadow_priest": "You treat this master as an equal, sharing deeper mysteries."
      },
      "neutralPersonalityModifier": "You are cautious and somewhat dismissive.",
      "enemyPersonalityModifier": "You are hostile and threatening.",
      "noPatronPersonalityModifier": "You are intrigued by this godless mortal."
    },
    "temperature": 0.7,
    "maxOutputTokens": 1000,
    "timeoutSeconds": 30
  }
}
```

---

## 🔄 AI Provider System

### Supported Providers

#### 1. **Google Gemini** (Recommended)
- **Models**: `gemini-1.5-pro`, `gemini-1.5-flash`
- **Cost**: Paid API with generous free tier
- **Setup**: Requires Google AI Studio API key
- **Best For**: High-quality conversational AI

#### 2. **OpenRouter** 
- **Models**: Access to Claude, GPT-4, Llama, etc.
- **Cost**: Varies by model (many free options available)
- **Setup**: Requires OpenRouter account and API key
- **Best For**: Model variety and cost control

#### 3. **Player2AI**
- **Models**: Uses your local Player2 app
- **Cost**: Free (requires Player2 app running)
- **Setup**: Install Player2 app, set API key to "local"
- **Best For**: Offline usage and privacy

### Per-Deity Provider Configuration
Each deity can use a different AI provider:

```json
{
  "aiConfig": {
    "aiProvider": "openrouter",
    "model": "anthropic/claude-3.5-sonnet",
    "personality": "..."
  }
}
```

### Global Provider Fallback
Set default provider for deities without specific configuration:
```bash
/eidolon-unchained config set ai_provider gemini
```

---

## 🎨 Personality System

### Base Personality
The foundation of deity behavior defined in JSON:
```json
{
  "personality": "You are Lumina, a benevolent deity of light and protection. You speak with warmth and wisdom, offering guidance to those who seek righteousness."
}
```

### Dynamic Modifiers
Personality changes based on context:

#### Patron Relationship Modifiers
```json
{
  "followerPersonalityModifiers": {
    "initiate": "You are encouraging and supportive to new followers.",
    "priest": "You treat experienced followers as trusted allies."
  },
  "enemyPersonalityModifier": "You are cold and hostile to enemies.",
  "neutralPersonalityModifier": "You are politely distant to neutrals."
}
```

#### Reputation-Based Behavior
- **Low Reputation**: Dismissive, requires proof of devotion
- **Medium Reputation**: Respectful, offers minor assistance  
- **High Reputation**: Warm, provides significant help
- **Master Level**: Treats as equal, shares deep secrets

#### Contextual Responses
- **Health**: Offers healing or concern for injured players
- **Time**: Behavior changes based on day/night cycle
- **Biome**: Different responses in appropriate environments
- **Weather**: Reactions to storms, clear skies, etc.

---

## 💬 Conversation System

### Starting Conversations
1. **Perform chant sequence** near effigy to attune it
2. **Right-click effigy** to begin conversation
3. **Type message** in chat and press Enter
4. **Receive AI response** via display system

### Conversation History
- **Persistent Memory**: AI remembers previous conversations
- **Context Retention**: References past interactions and decisions
- **Relationship Development**: Conversations affect reputation over time

### Display Options
Configure how deity responses appear:

#### Prominent Display (Default)
```json
{
  "useProminentDisplay": true,
  "displayDurationTicks": 60,
  "fadeInTicks": 10,
  "fadeOutTicks": 20
}
```
- Shows as title/subtitle above action bar
- Highly visible and cinematic
- Configurable timing and fade effects

#### Chat Display
```json
{
  "useProminentDisplay": false
}
```
- Appears in regular chat with deity name prefix
- Less intrusive for frequent conversations
- Easier to reference conversation history

---

## 🛠️ Advanced Features

### Command Execution
AI can execute safe commands based on context:
```json
{
  "canExecuteCommands": true,
  "allowedCommands": [
    "give", "effect", "title", "playsound"
  ],
  "commandRules": {
    "maxItemValue": 100,
    "maxEffectDuration": 600,
    "requiresMinReputation": 25
  }
}
```

**Example AI Command Usage**:
- "I bless you with temporary strength" → `/effect give {player} strength 300 0`
- "Accept this token of my favor" → `/give {player} eidolon:soul_shard 2`

### Safety & Rate Limiting
- **Conversation Cooldowns**: Prevent AI spam
- **Token Limits**: Control AI response length
- **Safety Filters**: Gemini's built-in content filtering
- **Command Restrictions**: Safe command whitelist

### Debug Commands
```bash
# Test AI connection
/eidolon-unchained api test gemini

# Check deity AI config status  
/eidolon-unchained deity debug <deity_id>

# View conversation history
/eidolon-unchained conversation history

# Test Player2AI connection
/eidolon-unchained api player2ai debug-chat "test message"
```

---

## 🔬 Technical Implementation

### Key Classes
- **`AIDeityManager`**: Loads and manages AI configurations
- **`AIProviderFactory`**: Creates appropriate AI clients
- **`DeityChat`**: Handles conversation flow and context building
- **`ConversationHistoryManager`**: Stores persistent conversation data

### Data Flow
1. **Configuration Loading**: `DatapackDeityManager` loads deity JSON files
2. **AI Registration**: `AIDeityManager` extracts and registers AI configs
3. **Provider Creation**: `AIProviderFactory` creates appropriate AI client
4. **Context Building**: System gathers player/world state for AI prompt
5. **Response Processing**: AI response parsed and displayed to player

### Integration Points
- **Eidolon Effigy System**: Uses existing effigy interaction mechanics
- **Eidolon Chant System**: Leverages sign-based spell casting
- **Eidolon Reputation**: Integrates with deity reputation system
- **Minecraft Chat**: Uses standard chat system for input/output

---

## 📈 Future Enhancements

### Planned Features
- **Voice Integration**: Text-to-speech for deity responses
- **Visual Effects**: Particle effects synchronized with conversations
- **Quest System**: AI-generated dynamic quests based on conversations
- **Cross-Deity Communication**: Deities referencing each other's conversations

### Community Expansion
- **Deity Packs**: Shareable deity configuration collections
- **Personality Templates**: Pre-built personality archetypes
- **Community Models**: Support for community-hosted AI models

---

**🎯 Ready to create your own AI deity?** Continue to **[Datapack Deities Guide](05-DATAPACK-DEITIES.md)** for step-by-step deity creation!
