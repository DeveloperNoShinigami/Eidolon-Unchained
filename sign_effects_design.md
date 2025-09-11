# Sign Effects System Design

## Overview

A datapack-driven system that allows configuring custom effects and commands to trigger when players cast specific Eidolon signs using keybinds. This system would be separate from chant effects and purely focused on individual sign casting.

## Architecture

### File Structure
```
data/
└── [modid]/
    └── sign_effects/
        └── sign_config.json
```

### Configuration Format

```json
{
  "format_version": "1.0",
  "description": "Sign effect configurations for individual Eidolon signs",
  
  "sign_effects": {
    "eidolon:sacred": {
      "enabled": true,
      "name": "Sacred Sign Effects",
      "description": "Effects triggered when casting Sacred sign",
      "effects": [
        {
          "type": "run_command",
          "command": "particle minecraft:end_rod ~ ~1 ~ 0.3 0.3 0.3 0.02 10",
          "delay_ticks": 0
        },
        {
          "type": "run_command", 
          "command": "playsound minecraft:block.beacon.power_select player @s ~ ~ ~ 0.5 1.2",
          "delay_ticks": 5
        },
        {
          "type": "apply_effect",
          "effect": "minecraft:glowing",
          "duration": 100,
          "amplifier": 0,
          "delay_ticks": 10
        }
      ],
      "cooldown_ticks": 60,
      "mana_cost": 5,
      "requirements": [
        "reputation:eidolonunchained:light_deity:10"
      ]
    },
    
    "eidolon:wicked": {
      "enabled": true,
      "name": "Wicked Sign Effects",
      "description": "Dark effects for Wicked sign casting",
      "effects": [
        {
          "type": "run_command",
          "command": "particle minecraft:soul_fire_flame ~ ~1 ~ 0.4 0.4 0.4 0.03 15",
          "delay_ticks": 0
        },
        {
          "type": "run_command",
          "command": "playsound minecraft:entity.wither.ambient player @s ~ ~ ~ 0.3 0.8",
          "delay_ticks": 3
        },
        {
          "type": "send_message",
          "message": "§5Dark energies surge through you...",
          "delay_ticks": 8
        }
      ],
      "cooldown_ticks": 40,
      "mana_cost": 8,
      "requirements": [
        "reputation:eidolonunchained:dark_deity:5"
      ]
    },
    
    "eidolon:harmony": {
      "enabled": true,
      "name": "Harmony Sign Effects", 
      "description": "Nature-themed effects for Harmony sign",
      "effects": [
        {
          "type": "run_command",
          "command": "particle minecraft:happy_villager ~ ~1 ~ 0.5 0.5 0.5 0.1 8",
          "delay_ticks": 0
        },
        {
          "type": "apply_effect",
          "effect": "minecraft:regeneration",
          "duration": 60,
          "amplifier": 0,
          "delay_ticks": 5
        }
      ],
      "cooldown_ticks": 100,
      "mana_cost": 12
    }
  },
  
  "global_settings": {
    "enable_sign_effects": true,
    "require_soul_cost": true,
    "default_cooldown_ticks": 60,
    "effect_range_blocks": 16,
    "debug_mode": false
  }
}
```

## Features

### 1. **Per-Sign Configuration**
- Each Eidolon sign can have custom effects
- Multiple effect types: commands, potions, messages, particles
- Configurable delays between effects
- Individual cooldowns and mana costs

### 2. **Effect Types**
- `run_command`: Execute Minecraft commands with player context
- `apply_effect`: Apply potion effects to the player
- `send_message`: Send formatted messages to the player
- `spawn_particles`: Create particle effects at player location
- `play_sound`: Play sounds with configurable volume/pitch

### 3. **Requirements System**
- Reputation requirements with specific deities
- Item requirements (consume or just check)
- Research unlock requirements
- Time-based requirements (day/night)

### 4. **Advanced Features**
- **Delayed Effects**: Each effect can have a delay in ticks
- **Cooldown Management**: Per-sign cooldowns to prevent spam
- **Mana Integration**: Optional soul/mana costs for sign effects
- **Range Limiting**: Effects only work within certain range of altars/effigies

### 5. **Integration Points**

#### Keybind Integration
```java
// In ChantInputHandler or similar
if (signEffectsEnabled && SignEffectsManager.hasEffects(signId)) {
    SignEffectsManager.triggerSignEffects(player, signId);
}
```

#### Effect Manager
```java
public class SignEffectsManager {
    private static Map<ResourceLocation, SignEffectConfig> signEffects;
    
    public static void triggerSignEffects(ServerPlayer player, ResourceLocation signId) {
        SignEffectConfig config = signEffects.get(signId);
        if (config != null && canTrigger(player, config)) {
            scheduleEffects(player, config.getEffects());
            applyCooldown(player, signId, config.getCooldownTicks());
        }
    }
}
```

## Benefits

### 1. **Enhanced Sign Casting Experience**
- Makes individual sign casting feel more impactful
- Provides immediate visual/audio feedback
- Allows for deity-themed sign effects

### 2. **Content Creator Friendly**
- Fully datapack configurable
- No code changes needed for new effects
- Easy to balance and adjust

### 3. **Gameplay Integration**
- Ties into existing reputation system
- Uses established mana/soul costs
- Respects cooldown mechanics

### 4. **Modular Design**
- Can be enabled/disabled globally
- Per-sign enable/disable options
- Won't interfere with existing chant system

## Implementation Considerations

### 1. **Performance**
- Effects should be lightweight
- Cooldown tracking to prevent spam
- Configurable range limitations

### 2. **Balance**
- Mana costs prevent overuse
- Cooldowns ensure meaningful timing
- Requirements gate advanced effects

### 3. **Compatibility**
- Should not interfere with chant casting
- Must work alongside existing sign system
- Respects Eidolon's design principles

### 4. **User Experience**
- Clear feedback when effects trigger
- Obvious when requirements aren't met
- Intuitive configuration format

## Future Extensions

1. **Combo Effects**: Trigger special effects when casting multiple signs in sequence
2. **Environmental Reactions**: Different effects based on biome/dimension
3. **Deity Synergy**: Enhanced effects when aligned with deity domains
4. **Progressive Enhancement**: Effects get stronger with higher reputation
5. **Team Effects**: Some effects can benefit nearby allied players

## Example Use Cases

### Light Deity Follower
- Sacred sign creates holy light particles
- Warding sign provides temporary protection
- Harmony sign offers healing effects

### Dark Deity Follower  
- Wicked sign spawns shadow particles
- Blood sign provides vampiric effects
- Soul sign enhances necromantic abilities

### Nature Deity Follower
- Harmony sign accelerates crop growth
- Earth sign provides temporary resistance
- Life sign offers regeneration bonuses

This system would add depth to sign casting while maintaining the datapack-driven philosophy of Eidolon Unchained.