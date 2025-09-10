# Effigy Interaction Deep Dive Analysis

## ACTUAL Problem Identified

The user wants this flow:
```
1. Perform custom chant near effigy → 2. Effigy becomes "ready" → 3. Right-click effigy → 4. Conversation starts
```

But we're doing this:
```
1. Perform custom chant → 2. Conversation starts IMMEDIATELY (bypassing effigy)
```

## Key Discovery: Eidolon's Pattern

### **Eidolon PrayerSpell.cast()** (Lines 119-133):
```java
@Override
public void cast(Level world, BlockPos pos, Player player) {
    EffigyTileEntity effigy = getEffigy(world, pos);
    if (effigy == null) return;
    if (!world.isClientSide) {
        effigy.pray();  // ← SETS COOLDOWN BUT NO CONVERSATION
        // ... reputation/magic handling ...
    }
    // NO startConversation() call!
}
```

### **Our DatapackChantSpell.cast()** (Lines 125-147):
```java
// Execute chant effects first
executeChantEffects(serverPlayer, world, pos);

// 🔥 Store this chant for prayer type detection
com.bluelotuscoding.eidolonunchained.integration.ai.EnhancedCommandExtractor
    .setLastPerformedChant(serverPlayer, chantData);

// Then trigger deity conversation using the prayer_effect_type  ← PROBLEM!
java.lang.reflect.Method startConversation = deityChat.getDeclaredMethod("startConversation", 
    ServerPlayer.class, net.minecraft.resources.ResourceLocation.class);
startConversation.invoke(null, serverPlayer, chantData.getLinkedDeity());
```

## The Real Issue

### **Effigy Ready State**
- `effigy.ready()` returns `true` because it's **commented out** in Eidolon: `return true; // world.getGameTime() - previous >= 24000;`
- `effigy.pray()` sets `previous = level.getGameTime()` for future cooldown tracking

### **Missing Link: Chant-to-Effigy Association**
Our system doesn't track WHICH chant was performed near WHICH effigy. We need:

1. **Chant Tracking Per Effigy**: Store which chant was last performed near each effigy
2. **Effigy Right-Click Handler**: Check what chant was performed, then start appropriate conversation
3. **Cooldown Integration**: Respect Eidolon's effigy cooldown system

## Current System Gaps

### **1. No Effigy-Chant Association**
- `setLastPerformedChant()` is **player-global**, not **effigy-specific**
- Multiple effigies near a player would all share the same "last chant"

### **2. Immediate Conversation Trigger**
- `DatapackChantSpell.cast()` calls `startConversation()` immediately
- Should instead mark effigy as "ready with chant data" and wait for right-click

### **3. Configuration Conflict**
- `enable_effigy_right_click = false` (default) disables the interaction system
- But this is the ONLY way to get the proper flow working

## Correct Implementation Pattern

### **Step 1: Store Chant Per Effigy** (Not Per Player)
```java
// In EnhancedCommandExtractor or new EffigyChantTracker
private static final Map<BlockPos, DatapackChant> effigyLastChants = new ConcurrentHashMap<>();

public static void setEffigyLastChant(BlockPos effigyPos, DatapackChant chant) {
    effigyLastChants.put(effigyPos, chant);
}

public static DatapackChant getEffigyLastChant(BlockPos effigyPos) {
    return effigyLastChants.get(effigyPos);
}
```

### **Step 2: Modify DatapackChantSpell.cast()**
```java
// Find nearby effigy
EffigyTileEntity effigy = getEffigy(world, pos);
if (effigy != null) {
    // Store chant on this specific effigy
    setEffigyLastChant(effigy.getBlockPos(), chantData);
    
    // Call effigy.pray() to respect Eidolon cooldown
    effigy.pray();
    
    // DON'T call startConversation() here!
    player.sendMessage("The effigy glows with divine energy. Right-click to commune.");
} else {
    // No effigy nearby - direct conversation (current behavior)
    startConversation(serverPlayer, chantData.getLinkedDeity());
}
```

### **Step 3: Enhanced EffigyInteractionHandler**
```java
@SubscribeEvent(priority = EventPriority.HIGH)
public static void onEffigyRightClick(PlayerInteractEvent.RightClickBlock event) {
    // ... existing checks ...
    
    EffigyTileEntity effigy = (EffigyTileEntity) event.getLevel().getBlockEntity(pos);
    
    // Check if effigy has stored chant
    DatapackChant lastChant = getEffigyLastChant(effigy.getBlockPos());
    if (lastChant != null && lastChant.hasLinkedDeity()) {
        // Use the stored chant for prayer type resolution
        EnhancedCommandExtractor.setLastPerformedChant(player, lastChant);
        
        // Start conversation with deity from chant
        DeityChat.startConversation(player, lastChant.getLinkedDeity());
        
        // Clear the stored chant (one-time use)
        clearEffigyLastChant(effigy.getBlockPos());
        
        // Trigger effigy cooldown
        effigy.pray();
        
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
        return;
    }
    
    // Fallback to existing deity detection logic
    // ...
}
```

## Configuration Requirements

### **Must Enable Effigy Interaction**
```toml
enable_effigy_right_click = true  # ← REQUIRED for this flow
```

### **Optional: Behavior Mode Selection**
```toml
chant_interaction_mode = "effigy_required"  # vs "immediate" (current behavior)
```

## Summary

The user is **100% correct**. The intended flow is:

1. **Chant near effigy** → Effigy stores chant data + enters cooldown
2. **Right-click effigy** → Conversation starts using stored chant data  
3. **Effigy cooldown** → Prevents spam clicking

Our current system **bypasses the effigy entirely** and starts conversations immediately after chant completion.

**Root Fix**: 
- **Don't call `startConversation()` in `DatapackChantSpell.cast()`**
- **Store chant data per-effigy, not per-player**  
- **Enable effigy right-click by default**
- **Use stored chant data in `EffigyInteractionHandler`**

This would make custom chants behave **exactly like Eidolon chants** - they prepare the effigy, then require right-click to complete the interaction.
