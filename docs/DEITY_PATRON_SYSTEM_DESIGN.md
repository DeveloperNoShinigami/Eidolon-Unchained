# Deity Patron System Design - Full Implementation Plan

## Overview

Design a **D&D-style deity patron system** where players choose a primary deity that fundamentally affects their gameplay experience, relationships, and abilities.

## Core Concepts (Based on D&D/NWN)

### 1. Patron Selection System
- **Players choose a primary deity** (patron) at some point in progression
- **Exclusive relationship** - can only serve one patron at a time
- **Switching patrons** possible but with severe penalties
- **Neutral option** - serve no patron (balanced but no special benefits)

### 2. Patron Recognition & Titles
- **Dynamic titles** based on progression with chosen patron
- **Contextual recognition** in AI conversations
- **Custom greetings** and responses based on patron relationship
- **Visual indicators** (particles, UI elements) showing patron allegiance

### 3. Inter-Deity Conflicts
- **Opposing deities** have natural conflicts (Light vs Dark, Order vs Chaos)
- **Reputation penalties** when serving enemies of your patron
- **Aggressive responses** from opposing deities
- **Follower conflicts** - killing followers of your patron's enemies grants favor

### 4. Patron-Specific Benefits
- **Exclusive spells/chants** only available to followers
- **Enhanced rewards** from your patron deity
- **Special prayer responses** and AI behavior
- **Unique progression paths** and abilities

## Technical Implementation Plan

### Phase 1: Patron Selection System

#### 1.1 Player Data Storage
```java
public class PatronData {
    private ResourceLocation patronDeity;     // Current patron (null = none)
    private ResourceLocation formerPatron;    // For switching penalties
    private long patronSince;                 // When they became follower
    private String currentTitle;              // Current patron-granted title
    private Map<String, Integer> titleProgress; // Progress toward next title
    private boolean patronLocked;             // Prevent switching during quests
}
```

#### 1.2 Patron Selection Commands
```
/eidolon-unchained patron choose <deity_id>  - Choose patron (confirmation required)
/eidolon-unchained patron abandon            - Abandon current patron (penalties)
/eidolon-unchained patron status             - Show current patron info
/eidolon-unchained patron titles             - List available titles
```

#### 1.3 Selection Requirements
- **Minimum reputation** required to become follower (e.g., 25+ rep)
- **Patron conflict checks** - ensure not serving opposing deity
- **Cooldown period** after abandoning previous patron
- **Confirmation process** with warnings about consequences

### Phase 2: Title & Recognition System

#### 2.1 Dynamic Title System
```json
{
  "patron_titles": {
    "dark_deity": {
      "initiate": { "rep_required": 10, "display": "Shadow Initiate" },
      "acolyte": { "rep_required": 25, "display": "Acolyte of Shadows" },
      "priest": { "rep_required": 50, "display": "Shadow Priest" },
      "champion": { "rep_required": 100, "display": "Champion of Nyxathel" }
    }
  }
}
```

#### 2.2 AI Recognition Integration
```java
public class PatronAIContext {
    public String getPatronTitle(Player player) {
        PatronData data = getPatronData(player);
        if (data.patronDeity != null) {
            return getTitleForReputation(data.patronDeity, getReputation(player, data.patronDeity));
        }
        return "wanderer"; // Default for non-followers
    }
    
    public boolean isFollower(Player player, ResourceLocation deity) {
        return getPatronData(player).patronDeity.equals(deity);
    }
}
```

#### 2.3 Contextual AI Responses
- **Patron greets followers differently**: "My faithful champion..." vs "Mortal stranger..."
- **Recognition of service**: "You have served me well, Shadow Priest..."
- **Title progression acknowledgment**: "You have proven worthy of greater power..."
- **Conflicting deity responses**: "I smell the taint of my enemies upon you..."

### Phase 3: Inter-Deity Conflict System

#### 3.1 Deity Relationship Matrix
```json
{
  "deity_relationships": {
    "eidolonunchained:dark_deity": {
      "enemies": ["eidolonunchained:light_deity"],
      "neutral": ["eidolonunchained:nature_deity"],
      "allies": []
    },
    "eidolonunchained:light_deity": {
      "enemies": ["eidolonunchained:dark_deity"],
      "neutral": ["eidolonunchained:nature_deity"],
      "allies": []
    }
  }
}
```

#### 3.2 Reputation Conflict Mechanics
- **Enemy deity reputation penalties**: Gaining rep with enemies of your patron causes automatic rep loss
- **Follower killing bonuses**: Killing followers of enemy deities grants patron favor
- **Patron abandonment penalties**: Switching patrons causes massive rep loss with former patron
- **Enemy patron rejection**: Enemy deities refuse to respond to followers of their enemies

#### 3.3 Conflict Resolution
```java
@EventHandler
public void onReputationGain(ReputationChangeEvent event) {
    PatronData patronData = getPatronData(event.getPlayer());
    if (patronData.patronDeity != null) {
        // Check if gaining rep with enemy of patron
        if (isEnemyDeity(patronData.patronDeity, event.getDeity())) {
            // Apply penalty to patron reputation
            applyPatronPenalty(event.getPlayer(), patronData.patronDeity, event.getAmount() * 0.5);
            
            // Send warning message
            sendConflictWarning(event.getPlayer(), patronData.patronDeity, event.getDeity());
        }
    }
}
```

### Phase 4: Patron-Specific Benefits

#### 4.1 Exclusive Content System
```json
{
  "patron_exclusive": {
    "chants": {
      "shadow_mastery": {
        "required_patron": "eidolonunchained:dark_deity",
        "required_title": "shadow_priest",
        "description": "Only true servants of shadow can master this art"
      }
    },
    "rewards": {
      "dark_blessing": {
        "patron": "eidolonunchained:dark_deity",
        "multiplier": 2.0,
        "description": "Followers receive enhanced rewards"
      }
    }
  }
}
```

#### 4.2 Enhanced AI Behavior
- **Patron followers** get priority in conversations
- **Enhanced reward calculations** for patron deity
- **Special quest lines** only available to followers
- **Patron-specific dialogue options** and responses

### Phase 5: Visual & UI Integration

#### 5.1 Patron Indicators
- **HUD element** showing current patron and title
- **Particle effects** based on patron allegiance
- **Chat formatting** with patron colors and symbols
- **Codex integration** with patron-specific sections

#### 5.2 Selection Interface
```java
public class PatronSelectionGUI extends Screen {
    // Show available deities
    // Display requirements and consequences
    // Confirmation dialogs
    // Conflict warnings
}
```

## Implementation Priority

### Phase 1: Core Infrastructure ⭐⭐⭐
1. **PatronData** capability system
2. **Patron selection** commands and logic
3. **Basic conflict detection**

### Phase 2: Recognition System ⭐⭐
1. **Title system** implementation
2. **AI integration** for patron recognition
3. **Dynamic responses** based on patron status

### Phase 3: Conflict Mechanics ⭐⭐
1. **Inter-deity conflicts**
2. **Reputation penalties**
3. **Enemy faction mechanics**

### Phase 4: Advanced Features ⭐
1. **Exclusive content**
2. **Enhanced benefits**
3. **Visual integration**

## Benefits of This System

### For Players
- **Meaningful choice** with lasting consequences
- **Deep roleplay opportunities** with deity relationships
- **Exclusive content** based on patron selection
- **Dynamic progression** with contextual recognition

### For Gameplay
- **Replayability** with different patron paths
- **Conflict mechanics** add strategy to deity relationships
- **Immersive AI** that acknowledges player choices
- **Long-term progression** with patron-specific goals

## Example Player Journey

1. **Early Game**: Player explores, gains reputation with multiple deities
2. **Patron Choice**: At 25+ rep, chooses Nyxathel as patron → becomes "Shadow Initiate"
3. **Recognition**: Nyxathel now greets as "my faithful initiate" in conversations
4. **Conflict**: Gaining rep with light deity causes Nyxathel rep penalty + warning
5. **Progression**: At 50 rep, becomes "Shadow Priest" → unlocks exclusive chants
6. **Mastery**: At 100 rep, becomes "Champion of Nyxathel" → maximum benefits

This creates a **living, breathing deity relationship system** rather than just reputation numbers!
