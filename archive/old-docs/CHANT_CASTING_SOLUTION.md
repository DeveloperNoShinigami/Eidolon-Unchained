# Flexible Chant Casting System - Datapack-Driven Design

## Overview

This document describes the new **flexible chant casting system** that replaces hardcoded chant assignments with a fully datapack-driven approach. The system allows players to assign any datapack-defined chant to configurable slots and provides an intuitive casting interface.

## Design Philosophy

### ✅ **Datapack-First Approach**
- **No Hardcoded Chants**: System works with any chants defined in datapacks
- **Dynamic Discovery**: Automatically detects all available chants
- **Flexible Assignment**: Players can assign any chant to any slot
- **Extensible**: New chants work immediately without code changes

### ✅ **User-Centric Interface**
- **4 Configurable Slots**: Players can assign their preferred chants
- **Visual Sign Sequence**: Shows required sign patterns before casting
- **Smart Validation**: Checks effigy requirements for deity chants
- **Progressive Disclosure**: Interface appears only when needed

## System Architecture

### 1. Flexible Keybind System
**New Keybinds:**
- `G` - Chant Slot 1 (configurable)
- `H` - Chant Slot 2 (configurable)
- `J` - Chant Slot 3 (configurable)
- `K` - Chant Slot 4 (configurable)
- `C` - Open Chant Interface (management)

### 2. Chant Assignment System
**Core Classes:**
- `ChantSlotManager.java` - Manages slot assignments and persistence
- `ChantSlotActivationPacket.java` - Network communication for slot activation
- `ChantInterfacePacket.java` - Interface management and sync

**Features:**
- **Persistent Storage**: Assignments saved in player NBT data
- **Server-Side Validation**: Prevents cheating and ensures consistency
- **Automatic Caching**: Optimized performance with intelligent caching
- **Cross-Session Persistence**: Settings preserved across game sessions

### 3. Command Interface
**New Commands:**
- `/chant assign <slot> <chant_id>` - Assign chant to slot
- `/chant clear <slot>` - Clear specific slot
- `/chant list` - Show current assignments
- `/chant available` - List all available chants
- `/chant info <chant_id>` - Detailed chant information

## User Workflow

### Initial Setup
1. **Discover Available Chants**: `/chant available`
2. **Get Chant Details**: `/chant info eidolonunchained:shadow_communion`
3. **Assign to Slot**: `/chant assign 1 eidolonunchained:shadow_communion`
4. **Verify Assignment**: `/chant list`

### Casting Process
1. **Press Slot Key** (G/H/J/K) - Activates assigned chant
2. **System Validation**:
   - Checks if chant is assigned to slot
   - Validates deity requirements (effigy proximity)
   - Displays sign sequence instructions
3. **Player Input**: Draw the required sign sequence in the air
4. **Execution**: System validates sequence and triggers chant effects

### Smart Requirements
- **Deity Chants**: Must be cast near an effigy (16-block radius)
- **Non-Deity Chants**: Can be cast anywhere
- **Visual Feedback**: Clear messages indicate requirements and progress

## Technical Implementation

### Chant Discovery
The system automatically discovers all chants by:
```java
Collection<ResourceLocation> chantIds = DatapackChantManager.getAllChantIds();
```

### Assignment Persistence
Player assignments are stored in NBT:
```json
{
  "EidolonUnchained": {
    "EidolonUnchained_ChantSlots": {
      "slot_1": "eidolonunchained:shadow_communion",
      "slot_2": "eidolonunchained:divine_communion",
      "slot_3": "eidolonunchained:natures_communion",
      "slot_4": ""
    }
  }
}
```

### Network Architecture
```
Client Keybind → ChantSlotActivationPacket → Server Validation → ChantSlotManager.activateChantSlot()
```

### Effigy Detection
```java
private static boolean isNearEffigy(ServerPlayer player) {
    // Searches 16x16x16 area around player for effigy blocks
    // Returns true if any effigy is found within range
}
```

## Chant Recipe Integration

### Existing Recipe Files (for Eidolon compatibility)
These recipes enable traditional sign-sequence casting:

**shadow_communion.json:**
```json
{
  "type": "eidolon:chant",
  "signs": ["eidolon:wicked", "eidolon:wicked", "eidolon:blood"]
}
```

### Recipe Generation
All datapack chants automatically have corresponding recipes created for seamless integration with Eidolon's native chant system.

## Configuration Examples

### Example Assignment Session
```bash
# List available chants
/chant available

# Get information about a specific chant
/chant info eidolonunchained:shadow_communion

# Assign chants to slots
/chant assign 1 eidolonunchained:shadow_communion    # Slot 1 (G key)
/chant assign 2 eidolonunchained:divine_communion    # Slot 2 (H key)
/chant assign 3 eidolonunchained:natures_communion   # Slot 3 (J key)
/chant assign 4 eidolonunchained:gaias_wrath         # Slot 4 (K key)

# View current assignments
/chant list

# Clear a slot if needed
/chant clear 2
```

### Example Gameplay Flow
1. **Player presses G** (Slot 1 with Shadow Communion assigned)
2. **System responds**: "§6Starting chant: Shadow Communion"
3. **System displays**: "§7Draw the sign sequence: wicked → wicked → blood"
4. **Player draws signs** in the air near an effigy
5. **System validates** and executes chant effects
6. **AI conversation** begins with the linked deity

## Advantages Over Previous System

### ❌ **Old System Problems**
- Hardcoded chant names (`SHADOW_COMMUNION`, `DIVINE_COMMUNION`)
- Fixed keybind associations
- No flexibility for custom chants
- Limited to 3 predefined chants

### ✅ **New System Benefits**
- **Unlimited Chants**: Works with any datapack-defined chant
- **Player Choice**: Full control over slot assignments
- **Datapack Compatible**: New chants work immediately
- **Scalable**: 4 slots expandable if needed
- **User-Friendly**: Clear commands and feedback
- **Persistent**: Settings survive game restarts

## Future Enhancements

### Planned Features
1. **GUI Interface**: Visual chant assignment screen
2. **Chant Favorites**: Quick-access to frequently used chants
3. **Hotswap**: Change assignments without commands
4. **Gesture Patterns**: Mouse gesture recognition for casting
5. **Voice Commands**: Accessibility features

### Extensibility Points
1. **Slot Count**: Easily expandable to more than 4 slots
2. **Custom Keybinds**: Player-configurable key assignments
3. **Chant Groups**: Organize chants by category/deity
4. **Advanced Filtering**: Search and filter available chants

## Troubleshooting

### Common Issues
1. **"No chant assigned to slot X"**: Use `/chant assign <slot> <chant_id>`
2. **"Must be near an effigy"**: Deity chants require effigy proximity
3. **Chant not responding**: Check `/chant list` to verify assignment

### Debug Commands
- `/chant available` - Verify chants are loaded
- `/chant info <chant_id>` - Check specific chant details
- `/chant list` - Confirm current slot assignments

## Integration Notes

### Backward Compatibility
- All existing chant JSON files work without modification
- Recipe files provide traditional casting support
- Command system supplements but doesn't replace existing features

### Performance Considerations
- **Caching**: Player assignments cached in memory
- **Lazy Loading**: Chant data loaded only when needed
- **Efficient NBT**: Minimal storage footprint

This flexible system provides the foundation for a truly datapack-driven chant experience while maintaining the intuitive gameplay you requested.