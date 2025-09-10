# Effigy Interaction Investigation - Custom Prayers vs Eidolon Chants

## Problem Summary
**Issue**: Custom prayers don't trigger effigy interactions, but Eidolon chants do.

## Root Cause Analysis

### 1. **Effigy Interaction is DISABLED by Default**
The key issue is in the configuration: **`enable_effigy_right_click = false`** (default)

**Location**: `EidolonUnchainedConfig.COMMON.enableEffigyRightClick.get()`
**Code**: 
```java
// EffigyInteractionHandler.java line 37
if (!com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig.COMMON.enableEffigyRightClick.get()) {
    return; // Feature disabled, let Eidolon handle it
}
```

### 2. **Different Prayer Systems**
There are **two separate prayer triggering systems**:

#### **Eidolon Chants (Work with Effigy)**:
- **Source**: Eidolon mod's built-in prayer spells
- **Mechanism**: Directly integrated with Eidolon's effigy system
- **Trigger**: Right-click effigy after performing Eidolon chant sequence
- **Integration**: Native Eidolon behavior

#### **Custom Prayers (Don't Work with Effigy)**:
- **Source**: Eidolon Unchained's datapack chant system
- **Mechanism**: `DatapackChantSpell.cast()` → Custom logic
- **Trigger**: Chant completion triggers `DeityChat.startConversation()` directly
- **Integration**: Bypasses effigy system entirely

## **Why This Happens**

### **Eidolon Chants Flow:**
```
Player → Eidolon Chant → Effigy Ready State → Right-Click Effigy → Eidolon Prayer System → Our Handler (if enabled)
```

### **Custom Prayers Flow:**
```
Player → Custom Chant → DatapackChantSpell.cast() → DeityChat.startConversation() 
  ↑                                                         ↓
  └─ BYPASSES EFFIGY COMPLETELY ───────────────────────────┘
```

## **Technical Details**

### **DatapackChantSpell.cast() Method (Lines 125-147)**:
```java
// 🔥 Only trigger deity conversation if prayer_effect_type is specified
if (chantData.hasLinkedDeity() && chantData.getPrayerEffectType() != null && !chantData.getPrayerEffectType().isEmpty()) {
    // Execute chant effects first
    executeChantEffects(serverPlayer, world, pos);
    
    // 🔥 Store this chant for prayer type detection
    com.bluelotuscoding.eidolonunchained.integration.ai.EnhancedCommandExtractor
        .setLastPerformedChant(serverPlayer, chantData);
    
    // Then trigger deity conversation using the prayer_effect_type
    java.lang.reflect.Method startConversation = deityChat.getDeclaredMethod("startConversation", 
        ServerPlayer.class, net.minecraft.resources.ResourceLocation.class);
    startConversation.invoke(null, serverPlayer, chantData.getLinkedDeity());
    // ↑ DIRECTLY CALLS CONVERSATION - BYPASSES EFFIGY SYSTEM
}
```

### **EffigyInteractionHandler.onEffigyRightClick() (Lines 37-39)**:
```java
// Check if effigy interaction is enabled in config
if (!com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig.COMMON.enableEffigyRightClick.get()) {
    return; // Feature disabled, let Eidolon handle it
}
```

## **Current System Behavior**

### **Working (Eidolon Chants)**:
1. Player performs Eidolon chant sequence (e.g., Sign sequences from base Eidolon mod)
2. Eidolon marks effigy as "ready for prayer"
3. Player right-clicks effigy
4. `EffigyInteractionHandler.onEffigyRightClick()` fires (if enabled)
5. Our custom AI deity conversation starts

### **Not Working (Custom Prayers)**:
1. Player performs custom chant (DatapackChantSpell)
2. **Custom chant immediately triggers AI conversation** (no effigy involvement)
3. Effigy is never marked as "ready"
4. Right-clicking effigy does nothing (Eidolon handles with default behavior)

## **Configuration Issue**

### **Default Configuration (Effigy Disabled)**:
```toml
enable_effigy_right_click = false  # ← EFFIGY INTERACTION DISABLED
enable_chat_interaction = true     # ← DIRECT CHAT INTERACTION ENABLED
require_chant_completion = true
```

### **Current System Design**:
- **Primary Mode**: Direct conversation after chant completion (no effigy needed)
- **Legacy Mode**: Effigy interaction (disabled by default)

## **Why This Design Exists**

### **From Code Comments**:
```java
/**
 * ⚠️ DEPRECATED: This handler is disabled by default.
 * The preferred method is the chant system through AIDeityPrayerSpell.
 * 
 * To enable effigy interactions, set "enableEffigyInteraction" to true 
 * in the eidolonunchained-ai.toml config file.
 */
```

### **Design Intent**:
1. **Primary System**: Chat-based interaction after chant completion
2. **Legacy System**: Effigy-based interaction (for backward compatibility)
3. **User Choice**: Configuration allows switching between modes

## **Solutions**

### **Option 1: Enable Effigy Interaction**
```bash
# Set in config file
enable_effigy_right_click = true
```

### **Option 2: Modify Custom Chants to Support Effigy**
- Don't trigger conversation immediately in `DatapackChantSpell.cast()`
- Instead, mark effigy as "ready" and require right-click
- This would make custom chants behave like Eidolon chants

### **Option 3: Hybrid Approach**
- Add configuration option to choose behavior per chant
- Some chants trigger immediately, others require effigy interaction

## **Recommended Fix**

### **Immediate Fix**: Enable effigy interaction and document the configuration
### **Long-term Fix**: Add per-chant configuration for interaction mode

## **Files Involved**
- `EffigyInteractionHandler.java` - Handles effigy right-click events
- `DatapackChantSpell.java` - Custom chant execution (bypasses effigy)
- `EidolonUnchainedConfig.java` - Configuration management
- `DeityChat.java` - AI conversation system

**Status**: ✅ IDENTIFIED - Configuration issue with clear solutions available
