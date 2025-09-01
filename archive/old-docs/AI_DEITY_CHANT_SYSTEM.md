# AI Deity Chant Integration System

## Overview

The AI Deity system now properly integrates with Eidolon's existing chant system instead of overriding effigy interactions. Players must perform the correct **sign sequences** (chants) near an effigy to trigger AI deity conversations.

## How It Works

### 1. Chant Sequences
Each AI deity has a specific chant sequence defined in their configuration:

```json
{
  "deity_id": "eidolonunchained:nature_deity",
  "api_settings": {
    "chant_sequence": ["earth_sign", "earth_sign", "earth_sign"]
  }
}
```

### 2. Default Chant Sequences
If no custom sequence is specified, the system automatically assigns chants based on deity personality:

- **Dark/Shadow Deities**: `wicked_sign, wicked_sign, wicked_sign`
- **Light/Holy Deities**: `sacred_sign, sacred_sign, sacred_sign`  
- **Nature Deities**: `earth_sign, earth_sign, earth_sign`
- **Fire Deities**: `flame_sign, flame_sign, flame_sign`
- **Water Deities**: `water_sign, water_sign, water_sign`
- **Air Deities**: `air_sign, air_sign, air_sign`
- **Generic Deities**: `magic_sign, magic_sign, magic_sign`

### 3. Player Interaction Process

1. **Approach an Effigy**: Player must be near an effigy (just like standard Eidolon prayers)
2. **Perform the Chant**: Use the correct sign sequence for the deity
3. **AI Conversation Starts**: Instead of standard prayer effects, opens AI chat interface
4. **Cooldown Applied**: Respects Eidolon's prayer cooldown system

## Integration Details

### Spell Registration
- **AIDeityPrayerSpell**: Custom spell class that extends PrayerSpell
- **Auto-Registration**: Automatically creates spells for all AI-enabled deities
- **Chant Recipes**: Generates data files that link sign sequences to spells

### Respects Eidolon Mechanics
- **Effigy Requirements**: Must be performed near a valid effigy
- **Altar Power**: Benefits from altar power (if applicable)
- **Reputation Checks**: Respects deity reputation requirements
- **Cooldown System**: Uses Eidolon's prayer cooldown mechanics
- **Research Gates**: Can be locked behind research requirements

## Configuration Examples

### Custom Chant Sequence
```json
{
  "deity_id": "mymod:storm_deity",
  "api_settings": {
    "chant_sequence": ["air_sign", "water_sign", "air_sign", "water_sign"]
  }
}
```

### Complex Chant
```json
{
  "deity_id": "mymod:arcane_deity", 
  "api_settings": {
    "chant_sequence": [
      "magic_sign", 
      "soul_sign", 
      "magic_sign", 
      "mind_sign", 
      "magic_sign"
    ]
  }
}
```

## Available Signs
Based on Eidolon's sign system:

- `wicked_sign` - Dark magic
- `sacred_sign` - Holy magic  
- `earth_sign` - Earth/nature magic
- `flame_sign` - Fire magic
- `water_sign` - Water magic
- `air_sign` - Air magic
- `magic_sign` - Generic magic
- `soul_sign` - Soul magic
- `mind_sign` - Mental magic
- `blood_sign` - Blood magic
- `death_sign` - Death magic
- `harmony_sign` - Balance magic
- `winter_sign` - Ice/cold magic

## Player Instructions

### For Server Owners
1. Configure AI deity chant sequences in the datapack
2. Players will need to discover or be taught the correct sequences
3. Consider adding codex entries explaining the chants
4. Sequences can be made more complex for higher-tier deities

### For Players
1. **Research**: Learn the correct chant sequence for each deity
2. **Setup**: Build an effigy and altar (standard Eidolon requirements)
3. **Perform Chant**: Use the sign sequence near the effigy
4. **Chat**: AI conversation interface opens for natural interaction
5. **Wait**: Respect cooldown periods between prayers

## Benefits of This Approach

### ✅ Proper Integration
- Works with existing Eidolon mechanics
- No conflicts with base mod functionality
- Respects player progression and research

### ✅ Configurability  
- Server owners can customize chant sequences
- Can create complex, memorable chant patterns
- Maintains immersion and ritual feel

### ✅ Balance
- Maintains Eidolon's cooldown and reputation systems
- Requires proper altar setup and resources
- Prevents spam and maintains significance of deity interactions

### ✅ Discoverability
- Chants can be documented in codex entries
- Players must learn/research the sequences
- Creates sense of mystery and accomplishment

## Technical Implementation

The system automatically:
1. **Scans** all loaded AI deity configurations
2. **Generates** AIDeityPrayerSpell instances with correct sign sequences  
3. **Registers** spells with Eidolon's spell system
4. **Creates** chant recipe data files
5. **Integrates** with existing prayer mechanics

This ensures seamless integration while maintaining all of Eidolon's intended gameplay mechanics.
