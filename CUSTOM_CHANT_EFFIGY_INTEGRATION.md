# Custom Chant Effigy Integration - IMPLEMENTED

## Overview
Custom chants now properly integrate with Eidolon's effigy system, just like native Eidolon prayers. Each chant can optionally require an effigy nearby to cast.

## New Features

### 1. **Effigy Requirement System**
Custom chants can now specify whether they require an effigy nearby to cast, just like Eidolon's prayer spells.

### 2. **JSON Configuration Option**
```json
{
  "name": "Divine Communion",
  "linked_deity": "eidolonunchained:nature_deity",
  "prayer_effect_type": "blessing", 
  "requires_effigy": true,  // ← NEW SETTING
  "signs": ["eidolon:harmony", "eidolon:soul"],
  "effects": [...]
}
```

### 3. **Smart Defaults**
- **Deity-linked chants**: `requires_effigy` defaults to `true`
- **Non-deity chants**: `requires_effigy` defaults to `false`
- **Override**: Set `"requires_effigy": false` to allow deity chants without effigy

## How It Works

### **Effigy-Required Chants (Default for deity chants)**:
```
1. Player performs chant near effigy → 
2. Chant checks: effigy.ready() → 
3. Executes chant effects → 
4. Calls effigy.pray() (sets cooldown) → 
5. Triggers deity conversation (if prayer_effect_type set)
```

### **Non-Effigy Chants**:
```  
1. Player performs chant anywhere → 
2. No effigy requirement → 
3. Executes chant effects → 
4. Triggers deity conversation (if prayer_effect_type set)
```

## Implementation Details

### **DatapackChantSpell.canCast() Logic**:
```java
if (chantData.requiresEffigy()) {
    EffigyTileEntity effigy = getEffigy(world, pos);
    if (effigy == null) {
        player.sendMessage("This chant requires an effigy nearby.");
        return false;
    }
    if (!effigy.ready()) {
        player.sendMessage("The effigy is not ready. Wait for the cooldown to end.");
        return false;
    }
}
```

### **DatapackChantSpell.cast() Integration**:
```java
if (chantData.requiresEffigy()) {
    EffigyTileEntity effigy = getEffigy(world, pos);
    if (effigy != null) {
        effigy.pray(); // ← Sets Eidolon cooldown like native prayers
    }
}
```

## Configuration Examples

### **1. Deity Chant (Requires Effigy by Default)**
```json
{
  "name": "Prayer of Nature's Blessing",
  "linked_deity": "eidolonunchained:nature_deity",
  "prayer_effect_type": "blessing",
  // requires_effigy defaults to true for deity chants
  "signs": ["eidolon:harmony", "eidolon:soul", "eidolon:sacred"],
  "effects": [
    {"type": "start_conversation", "deity": "eidolonunchained:nature_deity"}
  ]
}
```

### **2. Deity Chant (Override to Not Require Effigy)**
```json
{
  "name": "Nature's Whisper",
  "linked_deity": "eidolonunchained:nature_deity", 
  "prayer_effect_type": "conversation",
  "requires_effigy": false,  // ← Override: can be cast anywhere
  "signs": ["eidolon:harmony"],
  "effects": [
    {"type": "start_conversation", "deity": "eidolonunchained:nature_deity"}
  ]
}
```

### **3. Utility Chant (No Effigy Needed)**
```json
{
  "name": "Light Creation",
  // No linked_deity - requires_effigy defaults to false
  "signs": ["eidolon:flame"],
  "effects": [
    {"type": "spawn_light", "duration": 300}
  ]
}
```

### **4. Ritual Chant (Force Effigy Requirement)**
```json
{
  "name": "Ritual of Binding",
  // No linked_deity but force effigy requirement
  "requires_effigy": true,  // ← Force effigy for ritual atmosphere
  "signs": ["eidolon:wicked", "eidolon:soul", "eidolon:wicked"],
  "effects": [
    {"type": "ritual_effect", "power": "high"}
  ]
}
```

## Benefits

### **1. Authentic Eidolon Integration**
- Custom chants behave exactly like native Eidolon prayers
- Uses same effigy detection logic (`getEffigy()`)
- Respects Eidolon's cooldown system (`effigy.pray()`)

### **2. Flexible Configuration**
- Deity chants can require effigy interaction (immersive)
- Utility chants can be cast anywhere (convenient)
- Override system allows fine-tuned control

### **3. Future Expansion Ready**
- Foundation for custom effigy interactions
- Support for manifestation mechanics
- Compatible with existing deity conversation system

## Error Messages
- **No Effigy**: "This chant requires an effigy nearby."
- **Effigy On Cooldown**: "The effigy is not ready. Wait for the cooldown to end."

## Compatibility
- ✅ **Backward Compatible**: Existing chants without `requires_effigy` use smart defaults
- ✅ **Eidolon Integration**: Uses native effigy detection and cooldown systems
- ✅ **AI Deity System**: Works with existing conversation triggers

## Future Possibilities
- **Custom Effigy Types**: Different effigy materials for different deities
- **Manifestation System**: Gods appearing through effigies instead of just speaking
- **Effigy Progression**: Effigies gaining power through repeated use
- **Ritual Circles**: Multi-effigy setups for powerful chants

**Status**: ✅ **IMPLEMENTED** - Ready for testing and configuration
