# Enhanced Proactive AI Deity System

## Overview

The AI deity system has been significantly enhanced to provide comprehensive player status analysis and proactive assistance. Deities now intelligently assess player conditions and offer assistance based on reputation levels and current needs.

## Key Enhancements

### 1. Comprehensive Player Context Analysis

The system now tracks and analyzes:

**Health Status:**
- Health percentage with critical injury detection
- Emergency thresholds: Critical (≤25%), Badly Hurt (≤50%), Wounded (≤75%)

**Hunger Analysis:**
- Food level with starvation detection  
- Hunger states: Starving (≤6), Hungry (≤12), Peckish (≤17)

**Equipment Assessment:**
- Armor coverage and enchantment status
- Weapon availability and quality
- Vulnerability analysis (no armor/weapons)

**Environmental Status:**
- Dangerous conditions (lava, fire, drowning)
- Dimension detection (Nether, End)
- Height-based danger assessment

**Active Effects:**
- Beneficial and harmful effect tracking
- Duration analysis for expiring effects

**Needs Assessment:**
- Automatic priority identification (URGENT HEALING, URGENT FOOD, protection, weapons)
- Environmental protection requirements

### 2. Proactive Assistance System

**Trigger Conditions:**
- Health below 50% + positive reputation → healing offer
- Hunger below 12 + decent reputation → food offer  
- Environmental danger + any positive reputation → emergency assistance
- High reputation (25+) + urgent needs → proactive intervention

**Deity-Specific Thresholds:**
- **Nature Deity (Verdania)**: Proactive at 5+ reputation, emergency at 0+
- **Light Deity (Lumina)**: Proactive at 3+ reputation, emergency at 0+
- **Dark Deity (Nyxathel)**: Proactive at 8+ reputation, emergency at 5+

### 3. Enhanced AI Prompting

**Conversation Flow:**
1. **Status Assessment**: AI first analyzes player's current condition
2. **Proactive Offer**: If conditions warrant, deity offers specific assistance
3. **Consent Request**: "I sense your [condition]. Shall I grant you [assistance]?"
4. **Response Processing**: Player accepts/declines, deity acts accordingly

**Example Proactive Interactions:**
```
Verdania: "I sense your wounds run deep, faithful guardian. Your life force dims to but 30% of its strength. Shall I grant you nature's healing touch?"

Lumina: "My radiant sight reveals your suffering, devoted soul. You hunger greatly and bear grievous wounds. Shall I bestow healing light and divine sustenance?"

Nyxathel: "The shadows whisper of your peril, devoted one. Fire consumes your mortal form. Shall I grant you power through darkness to endure?"
```

### 4. Improved Command System

**Enhanced Blessing Commands:**
- **Nature**: Regeneration, bread, saturation, golden apple, resistance
- **Light**: Regeneration, resistance, bread, golden apple, fire resistance  
- **Dark**: Soul shards, night vision, strength, cooked beef, regeneration

**Context-Aware Command Selection:**
- Fire/lava danger → fire resistance effects
- Low health → regeneration + healing items
- Starvation → food items + saturation
- Combat situations → resistance + strength

## Technical Implementation

### Player Context Builder (`GeminiAPIClient.buildPlayerContext`)

```java
// Enhanced context now includes:
- Health percentage analysis with status labels
- Hunger level assessment with conditions
- Active effects with duration warnings
- Equipment analysis (armor, weapons, enchantments)
- Environmental danger detection
- Automatic needs assessment
- Comprehensive status summary
```

### Conversation Prompt Enhancement (`DeityChat.buildConversationPrompt`)

```java
// New proactive guidance section:
"IMPORTANT GUIDANCE FOR PROACTIVE ASSISTANCE:
- If player is badly hurt (under 50% health) and has good reputation (15+), 
  sense their weakness and offer healing assistance
- If player is starving and has decent reputation, notice their hunger
- If player has urgent needs and high reputation, proactively offer intervention
- Always assess current state first, then respond or offer assistance"
```

### AI Configuration Updates

All three deity configurations updated with:
- Lower blessing thresholds for proactive assistance
- Enhanced command pools with healing/sustenance options
- Explicit proactive assistance prompting
- Emergency assistance thresholds
- Comprehensive blessing command arrays

## Testing Scenarios

### Scenario 1: Wounded High-Reputation Player
1. Player has 40% health, reputation 30+ with Nature Deity
2. Perform nature communion chant
3. **Expected**: Verdania immediately senses wounds, offers healing
4. Player responds "yes" → Receives regeneration + golden apple

### Scenario 2: Starving Player Emergency
1. Player has 4 hunger, reputation 15+ with Light Deity  
2. Start conversation with Lumina
3. **Expected**: Lumina detects starvation, offers divine sustenance
4. Player accepts → Receives bread + saturation effect

### Scenario 3: Environmental Danger
1. Player is on fire, reputation 10+ with any deity
2. Initiate deity communication
3. **Expected**: Deity immediately offers emergency protection
4. Player accepts → Receives fire resistance + healing

### Scenario 4: Low Reputation Test
1. Player badly wounded, reputation below threshold
2. Contact deity
3. **Expected**: Deity acknowledges condition but doesn't offer assistance
4. May provide advice or request proof of devotion instead

## Configuration Files Modified

1. **`GeminiAPIClient.java`**: Enhanced player context analysis
2. **`DeityChat.java`**: Proactive conversation prompting  
3. **`nature_deity_ai.json`**: Lowered thresholds, enhanced commands
4. **`light_deity_ai.json`**: Compassionate assistance parameters
5. **`dark_deity_ai.json`**: Shadow-based aid with higher requirements

## Usage Instructions

### For Players:
1. Build reputation with desired deity through offerings/rituals
2. When injured/hungry/in danger, perform deity communion chant
3. Listen for deity's assessment of your condition
4. Accept or decline offered assistance
5. Receive appropriate divine intervention based on reputation

### For Administrators:
1. Adjust reputation thresholds in AI deity JSON files
2. Modify blessing commands to fit server balance
3. Configure assistance levels per deity personality
4. Monitor player interactions for system effectiveness

## Future Enhancements

- **Reputation-Based Assistance Quality**: Higher reputation = better healing/items
- **Situational Awareness**: Deity responses based on nearby monsters/threats
- **Resource Management**: Cooldowns and limits on emergency assistance
- **Progressive Assistance**: Multiple tiers of aid based on relationship depth
- **Cross-Deity Interactions**: Deities working together for major emergencies

This enhanced system transforms AI deities from reactive responders to proactive guardians who genuinely care for their followers' wellbeing, creating more immersive and engaging divine relationships.
