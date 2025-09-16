# TTS (Text-to-Speech) System - Complete Reference

The Eidolon Unchained TTS system brings deity voices to life through advanced text-to-speech synthesis powered by Player2.game's API. This system features smart cost distribution, context-aware voice selection, and extensive customization options.

## Table of Contents

- [Overview](#overview)
- [Quick Start](#quick-start)
- [Player Commands](#player-commands)
- [Configuration](#configuration)
- [Voice System](#voice-system)
- [Cost Distribution](#cost-distribution)
- [Deity Voice Configuration](#deity-voice-configuration)
- [Advanced Features](#advanced-features)
- [Troubleshooting](#troubleshooting)
- [For Modpack Creators](#for-modpack-creators)

## Overview

### Key Features

- **Smart Cost Distribution**: Players can fund their own TTS requests using Player2 App, with server fallback
- **Context-Aware Voices**: Deity voices change based on reputation, biome, time of day, and other factors
- **Per-Deity Configuration**: Each deity can have unique voice settings, emotions, and characteristics
- **Player Customization**: Individual TTS preferences including voice selection, volume, and speed
- **Funding Flexibility**: Choose between player-only, server-only, or player-first funding models
- **Custom Voice Support**: Modpack creators can add custom voices and voice aliases

### How It Works

1. **Player talks to deity** → AI generates text response
2. **TTS system activates** → Determines appropriate voice based on context
3. **Smart funding logic** → Uses player funding when available, falls back to server
4. **Audio delivery** → Sends synthesized speech to player's client
5. **Cost tracking** → Monitors usage for cost optimization

## Quick Start

### For Players

1. **Enable TTS**: `/eidolon-unchained tts enable`
2. **Test your setup**: `/eidolon-unchained tts test "Hello from the shadows"`
3. **Check status**: `/eidolon-unchained tts status`
4. **Configure funding**: `/eidolon-unchained tts funding player-first`

### For Server Admins

1. **Install Player2 App** (optional, for player funding)
2. **Configure server API key** in `eidolonunchained-common.toml`:
   ```toml
   player2ai_api_key = "your_player2_api_key_here"
   ```
3. **Monitor usage**: `/eidolon-unchained tts stats`

## Player Commands

### Basic Commands

```bash
# Enable/disable TTS
/eidolon-unchained tts enable
/eidolon-unchained tts disable

# Check TTS status
/eidolon-unchained tts status

# Test TTS with custom text
/eidolon-unchained tts test "The shadows speak to you"
```

### Funding Configuration

```bash
# Player pays for their own TTS (requires Player2 App)
/eidolon-unchained tts funding player-only

# Server pays for all TTS requests
/eidolon-unchained tts funding server-only

# Use player funding when available, fall back to server
/eidolon-unchained tts funding player-first

# Check funding status
/eidolon-unchained tts funding status
```

### Voice Customization

```bash
# Use automatic deity-appropriate voices
/eidolon-unchained tts voice auto

# Set specific voice
/eidolon-unchained tts voice male-deep-1

# List available voices
/eidolon-unchained tts voice list

# Adjust volume (0.0 - 2.0)
/eidolon-unchained tts volume 1.2

# Adjust speech speed (0.5 - 2.0)
/eidolon-unchained tts speed 0.9
```

### Admin Commands (OP only)

```bash
# View TTS usage statistics
/eidolon-unchained tts stats

# Reset statistics
/eidolon-unchained tts reset-stats
```

## Configuration

### Server Configuration

Edit `run/config/eidolonunchained-common.toml`:

```toml
[ai_deity_system]
# Player2 API key for server-funded TTS
player2ai_api_key = ""

# Whether AI deities are enabled
enable_ai_deities = true

# Log AI interactions for debugging
log_ai_interactions = false
```

### Per-Player Settings

Player TTS settings are automatically saved and include:

- **Enabled**: Whether TTS is active
- **Voice preference**: Auto or specific voice ID
- **Volume**: Audio volume level (0.0-2.0)
- **Speed**: Speech speed (0.5-2.0)
- **Funding mode**: Player-first, server-only, or player-only

## Voice System

### Available Voices

| Voice ID | Description | Best For |
|----------|-------------|----------|
| `neutral-1` | Default neutral voice | Generic deities |
| `male-deep-1` | Deep, ominous voice | Dark, death, shadow deities |
| `female-warm-1` | Warm, comforting voice | Light, healing deities |
| `male-intense-1` | Intense, passionate voice | Fire, war deities |
| `female-flowing-1` | Flowing, calm voice | Water, ocean deities |
| `male-steady-1` | Steady, grounded voice | Earth, stone deities |
| `female-light-1` | Light, airy voice | Air, wind deities |
| `female-natural-1` | Natural, earthy voice | Nature, forest deities |
| `male-whisper-1` | Whispered, eerie voice | Death, necromancy deities |

### Voice Selection Priority

The system selects voices in this order:

1. **Player override** (if set to specific voice)
2. **Reputation-based voice** (from deity config)
3. **Biome-based voice** (context-aware)
4. **Time-based voice** (day/night variations)
5. **Deity default voice** (from deity config)
6. **Fallback voice** (based on deity type)

## Cost Distribution

### Funding Models

#### Player-First (Recommended)
- Uses player's Player2 App when available
- Falls back to server funding if player funding fails
- Optimal cost distribution for servers

#### Player-Only
- Only uses player's Player2 App
- No TTS if player doesn't have Player2 App configured
- Zero cost to server

#### Server-Only
- Server pays for all TTS requests
- Works for all players regardless of Player2 App
- Higher cost to server

### Cost Optimization

**For Players:**
- Install Player2 App to reduce server costs
- Use `player-first` funding mode
- Consider `player-only` if you want full control

**For Server Admins:**
- Monitor usage with `/eidolon-unchained tts stats`
- Encourage player funding to reduce costs
- Set reasonable usage limits if needed

## Deity Voice Configuration

### Basic TTS Config

Add to your deity JSON file:

```json
{
  "tts_config": {
    "voice_id": "male-deep-1",
    "backup_voice": "neutral-1",
    "pitch": 1.0,
    "speed": 1.0,
    "volume": 1.0,
    "emotion": "neutral",
    "enabled": true,
    "allow_player_override": true,
    "funding_preference": "player_first"
  }
}
```

### Advanced TTS Config

```json
{
  "tts_config": {
    "voice_id": "male-deep-1",
    "backup_voice": "male-whisper-1",
    "pitch": 0.8,
    "speed": 0.9,
    "volume": 1.1,
    "emotion": "ominous",
    "accent": "ancient",
    "emphasis_level": 1,
    "enabled": true,
    "allow_player_override": true,
    "funding_preference": "player_first",

    "voice_aliases": {
      "shadow_lord": "male-deep-1",
      "dark_whisper": "male-whisper-1"
    },

    "reputation_voices": {
      "0": "male-whisper-1",
      "25": "male-deep-1",
      "75": "male-deep-intense-1"
    },

    "time_voices": {
      "night": "male-deep-1",
      "day": "male-whisper-1"
    },

    "biome_voices": {
      "minecraft:deep_dark": "male-deep-intense-1",
      "minecraft:soul_sand_valley": "male-whisper-1"
    },

    "advanced_params": {
      "reverb": 0.3,
      "echo": 0.2,
      "darkness_filter": true
    }
  }
}
```

### TTS Configuration Fields

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `voice_id` | string | "auto" | Primary voice ID or "auto" |
| `backup_voice` | string | "neutral-1" | Fallback voice if primary fails |
| `pitch` | float | 1.0 | Voice pitch (0.5-2.0) |
| `speed` | float | 1.0 | Speech speed (0.5-2.0) |
| `volume` | float | 1.0 | Volume level (0.0-2.0) |
| `emotion` | string | "neutral" | Voice emotion |
| `accent` | string | "default" | Voice accent |
| `emphasis_level` | int | 0 | Speech emphasis (0-2) |
| `enabled` | boolean | true | Whether TTS is enabled |
| `allow_player_override` | boolean | true | Allow player voice changes |
| `funding_preference` | string | "player_first" | Funding strategy |
| `custom_voice_file` | string | "" | Path to custom voice file |
| `voice_aliases` | object | {} | Custom voice name mappings |
| `reputation_voices` | object | {} | Voices by reputation threshold |
| `time_voices` | object | {} | Voices by time of day |
| `biome_voices` | object | {} | Voices by biome |
| `advanced_params` | object | {} | Provider-specific parameters |

## Advanced Features

### Context-Aware Voice Changes

#### Reputation-Based Voices
```json
"reputation_voices": {
  "0": "male-whisper-1",    // Untrusted (0-24 rep)
  "25": "male-deep-1",      // Acknowledged (25-49 rep)
  "50": "male-deep-2",      // Trusted (50-74 rep)
  "75": "male-deep-intense-1" // Revered (75+ rep)
}
```

#### Time-Based Voices
```json
"time_voices": {
  "day": "female-warm-1",
  "afternoon": "female-light-1",
  "evening": "female-flowing-1",
  "night": "male-deep-1"
}
```

#### Biome-Based Voices
```json
"biome_voices": {
  "minecraft:deep_dark": "male-whisper-1",
  "minecraft:soul_sand_valley": "male-deep-1",
  "minecraft:warped_forest": "male-intense-1",
  "minecraft:crimson_forest": "male-deep-2"
}
```

### Custom Voice Aliases

Create memorable names for voices:

```json
"voice_aliases": {
  "shadow_lord": "male-deep-1",
  "ancient_whisper": "male-whisper-1",
  "divine_light": "female-warm-1",
  "nature_spirit": "female-natural-1"
}
```

### Advanced Parameters

Provider-specific audio effects:

```json
"advanced_params": {
  "reverb": 0.3,           // Echo effect
  "echo": 0.2,             // Sound delay
  "darkness_filter": true,  // Dark audio processing
  "divine_resonance": 1.2, // Otherworldly effect
  "ancient_distortion": 0.1 // Age effect
}
```

## Troubleshooting

### Common Issues

#### TTS Not Working
1. Check if TTS is enabled: `/eidolon-unchained tts status`
2. Verify funding configuration: `/eidolon-unchained tts funding status`
3. Test with: `/eidolon-unchained tts test "hello"`

#### No Audio Playback
1. Check client audio settings
2. Verify network connectivity
3. Try different voice: `/eidolon-unchained tts voice neutral-1`

#### API Errors
1. Verify Player2 API key configuration
2. Check Player2 App authentication
3. Monitor server logs for errors

### Debug Commands

```bash
# Check TTS availability
/eidolon-unchained tts status

# Test specific voice
/eidolon-unchained tts test "Testing voice quality"

# View funding options
/eidolon-unchained tts funding status

# Check server statistics (OP only)
/eidolon-unchained tts stats
```

### Log Messages

Monitor `logs/latest.log` for TTS-related messages:

```
[INFO] TTS generated using player funding for: PlayerName
[INFO] TTS generated using server funding for: PlayerName
[WARN] TTS generation failed for player PlayerName: error message
[DEBUG] TTS request already in progress for player: PlayerName
```

## For Modpack Creators

### Custom Voice Packages

Create custom voice packages by:

1. **Adding voice definitions** to deity configs
2. **Creating voice aliases** for thematic consistency
3. **Configuring context-aware voices** for immersion
4. **Setting appropriate funding preferences**

### Example Modpack Integration

```json
{
  "tts_config": {
    "voice_id": "custom_dragon_lord",
    "voice_aliases": {
      "custom_dragon_lord": "male-deep-intense-1",
      "dragon_whisper": "male-whisper-1",
      "ancient_roar": "male-deep-2"
    },
    "reputation_voices": {
      "0": "dragon_whisper",
      "50": "custom_dragon_lord",
      "100": "ancient_roar"
    },
    "biome_voices": {
      "minecraft:end_highlands": "ancient_roar",
      "minecraft:dragon_cave": "custom_dragon_lord"
    },
    "advanced_params": {
      "dragon_resonance": true,
      "ancient_echo": 0.4,
      "power_amplification": 1.3
    }
  }
}
```

### Best Practices

1. **Consistent Voice Themes**: Use voice aliases for thematic consistency
2. **Context Awareness**: Configure biome/time-based voices for immersion
3. **Player Choice**: Allow player overrides for accessibility
4. **Cost Consideration**: Use player-first funding to reduce server costs
5. **Testing**: Thoroughly test voice configurations before release

### Distribution Guidelines

- Include TTS configuration in your deity JSON files
- Document custom voices in your modpack documentation
- Provide fallback voices for compatibility
- Consider server cost implications when setting defaults

---

*This documentation covers the complete TTS system as of Eidolon Unchained v3.9.0.9. For the latest updates and additional features, check the mod's changelog and GitHub repository.*