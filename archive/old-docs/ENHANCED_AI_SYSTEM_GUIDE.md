# Enhanced AI Deity System - Features Guide

## 🎯 **Complete Answer to User Questions:**

### **Question 1: Can the AI see player data (inventory, etc.)?**
✅ **YES** - The AI can now access:
- **Inventory**: Notable magical items, enchanted equipment, valuable materials
- **Health & Status**: Current health, experience level, effects
- **Location Context**: Biome, coordinates, time of day, weather
- **Ritual History**: Recent chants/rituals performed and their success rates
- **Favor Points**: Separate reputation system for task completion
- **Active Tasks**: Current divine assignments and progress

### **Question 2: Can it remember rituals and give favor?**
✅ **YES** - Comprehensive tracking system:
- **Ritual Memory**: Last 20 rituals/chants with timestamps and success status
- **Favor Points**: Earned through successful rituals (separate from reputation)
- **Persistent Storage**: All data saved to player NBT, survives server restarts
- **Contextual Responses**: AI considers ritual history when responding

### **Question 3: Task system with rewards?**
✅ **YES** - Full task management:
- **Task Assignment**: AI can give players specific divine tasks
- **Requirement Checking**: Validates completion (items, actions, etc.)
- **Favor Rewards**: Completion grants favor points + configured rewards
- **Cooldown System**: Prevents task spam with configurable timers

---

## 🔧 **API Error Fix:**

### **What the error meant:**
```
"finishReason": "MAX_TOKENS"
```
- AI response was truncated due to hitting token limits
- No actual text content was returned, just metadata
- Caused by overly long context or high token settings

### **Solutions implemented:**
1. **Token Limit Management**: Reduced max tokens from 1000 to 800
2. **Enhanced Error Handling**: Detects `MAX_TOKENS` and `SAFETY` finish reasons
3. **Context Optimization**: Truncates inventory lists and ritual history
4. **Graceful Degradation**: Fallback messages when AI fails

---

## 🎮 **Usage Examples:**

### **For Players:**
```bash
# Check your divine favor
/eidolon task favor nature_deity

# List active tasks  
/eidolon task list

# Complete a task
/eidolon task complete plant_seeds

# Check all favor points
/eidolon task favor
```

### **For AI Deities:**
When players pray, the AI can now:
- See their inventory: "I notice you carry ancient runes..."
- Reference past rituals: "Your recent communion with shadow energies concerns me..."
- Assign tasks: "Prove your devotion by gathering sacred herbs."
- Award favor: Player gains favor points for successful rituals

---

## 📊 **Player Context Tracking:**

### **Data Collected:**
```json
{
  "favor_points": {
    "eidolonunchained:nature_deity": 15,
    "eidolonunchained:dark_deity": 3
  },
  "ritual_history": [
    {
      "name": "divine_communion",
      "timestamp": 1693520400000,
      "successful": true,
      "deity": "eidolonunchained:nature_deity"
    }
  ],
  "active_tasks": {
    "plant_seeds": {
      "description": "Plant 32 seeds to spread nature's bounty",
      "favor_reward": 5,
      "assigned_time": 1693520400000
    }
  },
  "recent_actions": [
    "chanted divine_communion successfully",
    "entered minecraft:forest biome"
  ]
}
```

### **AI Context Example:**
```
Player: TestPlayer
Location: 245, 64, -123
Biome: minecraft:forest
Time: morning
Weather: clear
Health: 20/20
Experience Level: 15
Notable Items: Eidolon Soul Gem x3, Enchanted Diamond Sword
Recent Rituals: divine_communion (successful, 2 hours ago), shadow_communion (failed, 1 day ago)
Favor Points: 15 (earned through successful rituals and completed tasks)
Recent Activity: chanted divine_communion successfully, entered forest biome
```

---

## ⚙️ **Configuration:**

### **Task System Setup (nature_deity_ai.json):**
```json
{
  "task_config": {
    "enabled": true,
    "max_active_tasks": 3,
    "available_tasks": [
      {
        "task_id": "plant_seeds",
        "description": "Plant 32 seeds to spread nature's bounty", 
        "requirements": ["item:minecraft:wheat_seeds:32"],
        "favor_reward": 5,
        "reputation_required": 0,
        "cooldown_hours": 6,
        "reward_commands": [
          "give {player} minecraft:bone_meal 16",
          "effect give {player} minecraft:regeneration 300 0"
        ]
      }
    ]
  }
}
```

### **Enhanced Prayer Prompts:**
```json
{
  "base_prompt": "Player {player} approaches your sacred grove. Reputation: {reputation}/100, favor points: {favor}. Recent activity: {context}. Ritual history: {ritual_history}. Consider their devotion through favor points when deciding rewards."
}
```

---

## 🛠 **Technical Implementation:**

### **Core Components:**
1. **PlayerContextTracker**: Manages comprehensive player data tracking
2. **TaskSystemConfig**: Defines available tasks per deity
3. **Enhanced GeminiAPIClient**: Includes inventory and ritual context
4. **TaskCommands**: Command system for task management
5. **Ritual Integration**: Tracks chant/ritual success automatically

### **Data Flow:**
```
Player performs ritual → DatapackChantManager → PlayerContextTracker
PlayerContextTracker → Stores in NBT → Persists across sessions
Player prays → PrayerSystem → Enhanced context → AI response
AI assigns task → TaskCommands → PlayerContextTracker → Rewards
```

### **Performance Optimizations:**
- **Memory Management**: Limits to last 20 rituals, 15 actions
- **Context Truncation**: Max 8 notable items to prevent token overflow
- **Lazy Loading**: Player contexts created on-demand
- **Background Tracking**: Minimal overhead biome/action tracking

---

## 🎯 **Results:**

### **Before:**
- AI had basic location/health context
- No memory of player actions
- Simple reputation system only
- Token limit errors causing failures

### **After:**
- **Rich Context**: Inventory, ritual history, favor points
- **Persistent Memory**: Survives server restarts
- **Task System**: Divine assignments with rewards
- **Error Handling**: Graceful degradation for API issues
- **Performance**: Optimized context to prevent token overflow

The AI deities can now truly see into the player's mystical journey, remember their devotion, and guide them with meaningful tasks that reflect their dedication to the divine path! 🔮✨
