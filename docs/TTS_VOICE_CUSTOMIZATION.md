# TTS Voice Customization Guide

## Overview

The Eidolon Unchained TTS system integrates with Player2.game's Text-to-Speech API to provide dynamic, context-aware deity voices. This guide covers voice customization options, custom voice file support, and advanced configuration.

## Player2.game API Integration

### API Endpoints
- **Get Available Voices**: `GET /tts/voices`
- **Generate Speech**: `POST /tts/speak`
- **Stream Speech**: `POST /tts/stream`

### Voice Selection
The API supports two voice selection methods:

1. **Specific Voice IDs**: Use `voice_ids` array with specific voice identifiers
2. **Default Selection**: Use `voice_gender` ("male", "female", "other") and `voice_language` for automatic selection

### Supported Languages
- `en_US` (English - US)
- `en_GB` (English - UK)
- `ja_JP` (Japanese)
- `zh_CN` (Chinese)
- `es_ES` (Spanish)
- `fr_FR` (French)
- `hi_IN` (Hindi)
- `it_IT` (Italian)
- `pt_BR` (Portuguese - Brazil)

### Audio Formats
- `mp3` - High compatibility
- `opus` - Excellent compression
- `flac` - Lossless quality
- `wav` - Uncompressed
- `pcm` - Raw audio data

## Deity Voice Configuration

### TTS Config Structure
Each deity JSON file supports a comprehensive `tts_config` section:

```json
{
  "tts_config": {
    "voice_id": "female-natural-1",
    "backup_voice": "female-warm-1",
    "pitch": 1.0,
    "speed": 0.9,
    "volume": 1.0,
    "emotion": "nurturing",
    "accent": "forest",
    "emphasis_level": 0,
    "enabled": true,
    "allow_player_override": true,
    "funding_preference": "player_first",

    "voice_aliases": {
      "forest_spirit": "female-natural-1",
      "green_whisper": "female-warm-1",
      "wild_voice": "female-flowing-1"
    },

    "reputation_voices": {
      "0": "female-whisper-1",
      "25": "female-natural-1",
      "75": "female-natural-divine-1"
    },

    "time_voices": {
      "day": "female-natural-1",
      "night": "female-warm-1"
    },

    "biome_voices": {
      "minecraft:forest": "female-natural-1",
      "minecraft:jungle": "female-flowing-1",
      "minecraft:flower_forest": "female-warm-1"
    },

    "advanced_params": {
      "nature_harmony": 0.3,
      "forest_echo": 0.2,
      "growth_resonance": true
    }
  }
}
```

### Configuration Fields

#### Core Settings
- `voice_id`: Primary voice identifier
- `backup_voice`: Fallback voice if primary fails
- `pitch`: Voice pitch adjustment (0.5-2.0)
- `speed`: Speech speed multiplier (0.25-4.0)
- `volume`: Audio volume level (0.0-2.0)
- `emotion`: Voice emotional tone
- `accent`: Regional or thematic accent
- `emphasis_level`: Intensity of speech emphasis (0-3)

#### Behavior Controls
- `enabled`: Enable/disable TTS for this deity
- `allow_player_override`: Allow players to customize voice settings
- `funding_preference`: Payment strategy ("player_first", "server_only", "player_only")

#### Context-Aware Voice Changes
- `reputation_voices`: Different voices based on player reputation
- `time_voices`: Voices that change with time of day
- `biome_voices`: Location-specific voice variations
- `voice_aliases`: Named voice presets for special occasions

#### Advanced Parameters
Custom deity-specific audio processing:
- `nature_harmony`: Nature-themed audio filter
- `forest_echo`: Echo effect for forest environments
- `growth_resonance`: Dynamic resonance effects
- `flame_crackle`: Fire-themed audio distortion
- `water_echo`: Aquatic reverb effects

## Custom Voice File Support

### Current Implementation Status
The TTS system currently supports two audio delivery methods:

1. **URL-based**: Player2.game provides a URL to download audio
2. **Data-based**: Raw audio data sent directly via network packets

### Asset Folder Integration
Based on the current codebase analysis:

#### ❌ Direct Asset Folder Support
- No existing `sounds.json` definitions for TTS voices
- No audio files (.ogg, .wav) in the asset folder
- Current implementation doesn't integrate with Minecraft's sound system

#### ✅ Potential Integration Paths

**Method 1: Resource Pack Integration**
```
src/main/resources/assets/eidolonunchained/
├── sounds.json
└── sounds/
    ├── deity/
    │   ├── nature_deity_conversation.ogg
    │   ├── fire_deity_blessing.ogg
    │   └── water_deity_healing.ogg
    └── voices/
        ├── female_natural_1.ogg
        └── male_intense_1.ogg
```

**Method 2: Custom Audio Manager**
- Implement `CustomVoiceManager.java`
- Load audio files from assets at runtime
- Integrate with existing `TTSAudioPacket` system

**Method 3: Hybrid Approach**
- Use Player2.game API as primary
- Fall back to local asset files when API unavailable
- Allow resource pack overrides for custom voices

### Implementation Requirements

#### For Asset Folder Support
1. **Create Sound Registry**
   ```java
   public class EidolonTTSSounds {
       public static final DeferredRegister<SoundEvent> SOUNDS =
           DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, EidolonUnchained.MODID);

       public static final RegistryObject<SoundEvent> NATURE_DEITY_VOICE =
           SOUNDS.register("nature_deity_voice", () ->
               SoundEvent.createVariableRangeEvent(new ResourceLocation(EidolonUnchained.MODID, "nature_deity_voice")));
   }
   ```

2. **Create sounds.json**
   ```json
   {
     "nature_deity_voice": {
       "subtitle": "subtitles.nature_deity_speaks",
       "sounds": [
         {
           "name": "eidolonunchained:deity/nature_conversation",
           "stream": true
         }
       ]
     }
   }
   ```

3. **Modify TTSAudioPacket**
   - Add support for ResourceLocation-based audio
   - Integrate with Minecraft's SoundManager
   - Handle both streaming and local audio

4. **Update Voice Selection Logic**
   - Check for local asset files first
   - Fall back to Player2.game API
   - Allow configuration preference

## Voice Customization Workflows

### For Players
1. **Basic Customization**
   ```
   /tts voice <deity> <voice_id>
   /tts volume <deity> <0.0-2.0>
   /tts speed <deity> <0.25-4.0>
   ```

2. **Advanced Settings**
   ```
   /tts emotion <deity> <emotion_name>
   /tts accent <deity> <accent_name>
   /tts funding <player_first|server_only|player_only>
   ```

### For Server Admins
1. **Global Configuration**
   - Edit deity JSON files in `data/eidolonunchained/ai_deities/`
   - Set server-wide voice policies
   - Configure funding preferences

2. **Custom Voice Installation**
   - Add audio files to resource pack
   - Update deity configurations
   - Restart server for changes

### For Modpack Creators
1. **Voice Pack Creation**
   - Create custom resource pack with deity voices
   - Configure voice mappings in deity JSON files
   - Package with modpack distribution

2. **Integration Examples**
   ```json
   {
     "voice_aliases": {
       "custom_nature_pack": "resource:nature_custom_voice",
       "epic_voice_mod": "resource:epic_nature_voice"
     }
   }
   ```

## Funding Strategies

### Player-First Strategy (Recommended)
```json
{
  "funding_preference": "player_first"
}
```
- Players pay for their own TTS via Player2 client
- Server provides fallback funding when player credits unavailable
- Reduces server operator costs

### Server-Only Strategy
```json
{
  "funding_preference": "server_only"
}
```
- All TTS costs charged to server API key
- Consistent experience for all players
- Higher server costs

### Player-Only Strategy
```json
{
  "funding_preference": "player_only"
}
```
- TTS only works if player has credits
- No server costs
- May exclude players without credits

## Troubleshooting

### Common Issues

**Voice Not Playing**
1. Check Player2.game API key configuration
2. Verify deity TTS config is enabled
3. Confirm player has sufficient credits (if using player funding)
4. Check server logs for API errors

**Wrong Voice Selected**
1. Verify voice_id exists in Player2.game system
2. Check context-aware voice mappings
3. Test with backup_voice configuration

**Audio Quality Issues**
1. Adjust pitch, speed, volume settings
2. Try different audio_format options
3. Check network connectivity for streaming

### Debug Commands
```
/tts debug <deity> - Show current voice configuration
/tts test <deity> <text> - Test TTS generation
/tts reload - Reload TTS configurations
```

## Future Enhancements

### Planned Features
1. **Visual Voice Indicators**
   - Chat formatting based on deity voice
   - Visual waveforms during speech
   - Voice selection GUI

2. **Advanced Context Awareness**
   - Mood-based voice changes
   - Battle vs. peaceful voice variants
   - Seasonal voice modifications

3. **Community Voice Packs**
   - Standardized voice pack format
   - Community voice sharing platform
   - Voice pack rating system

### Integration Opportunities
1. **Other Mods**
   - MCA (Minecraft Comes Alive) compatibility
   - ViveCraft VR voice integration
   - Voice chat mod synchronization

2. **External Services**
   - Amazon Polly integration
   - Google Cloud TTS support
   - Azure Speech Services compatibility

## Support

For issues or questions:
- Check the [GitHub Issues](https://github.com/your-repo/issues)
- Join the Discord community
- Review the API documentation at Player2.game

---

*This guide covers the TTS voice customization system as implemented in Eidolon Unchained v3.9.0.9+*