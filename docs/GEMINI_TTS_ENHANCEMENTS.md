# Gemini TTS Voice Enhancements - Complete Reference

This document provides a comprehensive list of all supported voice enhancements for the Gemini TTS integration in Eidolon Unchained.

## Available Voices (30 Total)

All 30 Gemini TTS voices with their characteristics:

| Voice Name | Characteristic | Best For |
|------------|----------------|----------|
| **Zephyr** | Bright | Light, cheerful deities |
| **Puck** | Upbeat | Energetic, playful deities |
| **Charon** | Informative | Death, underworld, authoritative deities |
| **Kore** | Firm | Strong, determined deities |
| **Fenrir** | Excitable | War, primal, intense deities |
| **Leda** | Youthful | Young, vibrant deities |
| **Orus** | Firm | Solid, reliable deities |
| **Aoede** | Breezy | Music, art, flowing deities |
| **Callirrhoe** | Easy-going | Relaxed, nature deities |
| **Autonoe** | Bright | Illumination, knowledge deities |
| **Enceladus** | Breathy | Mystical, ethereal deities |
| **Iapetus** | Clear | Communication, clarity deities |
| **Umbriel** | Easy-going | Gentle, peaceful deities |
| **Algieba** | Smooth | Sophisticated, elegant deities |
| **Despina** | Smooth | Grace, beauty deities |
| **Erinome** | Clear | Truth, justice deities |
| **Algenib** | Gravelly | Dark, ancient, powerful deities |
| **Rasalgethi** | Informative | Wisdom, teaching deities |
| **Laomedeia** | Upbeat | Celebration, joy deities |
| **Achernar** | Soft | Gentle, nurturing deities |
| **Alnilam** | Firm | Leadership, command deities |
| **Schedar** | Even | Balanced, neutral deities |
| **Gacrux** | Mature | Experienced, elder deities |
| **Pulcherrima** | Forward | Progressive, ambitious deities |
| **Achird** | Friendly | Social, community deities |
| **Zubenelgenubi** | Casual | Approachable, informal deities |
| **Vindemiatrix** | Gentle | Healing, care deities |
| **Sadachbia** | Lively | Active, dynamic deities |
| **Sadaltager** | Knowledgeable | Scholar, sage deities |
| **Sulafat** | Warm | Love, compassion deities |

## Supported Emotions

### Primary Emotions
- **happy**, **joyful**, **cheerful** → "Speak with a happy, joyful tone."
- **sad**, **melancholy**, **sorrowful** → "Speak with a sad, melancholy tone."
- **angry**, **furious**, **wrathful** → "Speak with an angry, wrathful tone."
- **fearful**, **scared**, **terrified** → "Speak with a fearful, trembling tone."
- **excited**, **enthusiastic**, **energetic** → "Speak with excited enthusiasm."
- **calm**, **peaceful**, **serene** → "Speak with a calm, peaceful tone."

### Deity-Specific Emotions
- **menacing**, **threatening**, **ominous** → "Speak in a menacing, threatening whisper."
- **divine**, **holy**, **sacred** → "Speak with divine, holy reverence."
- **ancient**, **wise**, **timeless** → "Speak with ancient wisdom and gravitas."
- **mysterious**, **enigmatic**, **cryptic** → "Speak mysteriously with cryptic undertones."
- **ethereal**, **otherworldly**, **spectral** → "Speak with an ethereal, otherworldly quality."
- **commanding**, **authoritative**, **imperial** → "Speak with commanding authority."
- **gentle**, **kind**, **compassionate** → "Speak with gentle compassion."
- **dark**, **sinister**, **malevolent** → "Speak with dark, sinister undertones."
- **playful**, **mischievous**, **impish** → "Speak with playful mischief."
- **noble**, **regal**, **majestic** → "Speak with noble majesty."

### Intensity Levels
- **whispered**, **hushed**, **secretive** → "Speak in a secretive whisper."
- **booming**, **thunderous**, **powerful** → "Speak with booming, thunderous power."
- **sultry**, **seductive**, **alluring** → "Speak with sultry allure."
- **weary**, **tired**, **exhausted** → "Speak with weary exhaustion."
- **eager**, **impatient**, **restless** → "Speak with eager impatience."

## Supported Accents

### Mystical/Fantasy Accents
- **ancient**, **archaic**, **old** → "Use an ancient, archaic speaking style."
- **ethereal**, **otherworldly**, **mystical** → "Use an ethereal, otherworldly accent."
- **divine**, **celestial**, **heavenly** → "Use a divine, celestial accent."
- **demonic**, **infernal**, **hellish** → "Use a demonic, infernal accent."
- **draconic**, **draconian**, **dragon** → "Use a draconic, powerful accent."
- **elven**, **elvish**, **fae** → "Use an elegant, elven accent."
- **dwarven**, **dwarvish**, **gruff** → "Use a gruff, dwarven accent."
- **orcish**, **brutish**, **savage** → "Use a brutish, savage accent."

### Regional/Cultural Accents
- **british**, **english**, **posh** → "Use a refined British accent."
- **scottish**, **highland** → "Use a Scottish Highland accent."
- **irish**, **gaelic** → "Use an Irish accent."
- **american**, **western** → "Use an American accent."
- **southern**, **drawl** → "Use a Southern drawl."
- **northern**, **yankee** → "Use a Northern accent."

### Vocal Qualities
- **gravelly**, **rough**, **hoarse** → "Use a gravelly, rough vocal quality."
- **smooth**, **silky**, **refined** → "Use a smooth, refined vocal quality."
- **breathy**, **wispy**, **airy** → "Use a breathy, wispy vocal quality."
- **nasally**, **sharp**, **piercing** → "Use a sharp, piercing vocal quality."

### Character Types
- **scholarly**, **academic**, **learned** → "Use a scholarly, academic accent."
- **noble**, **aristocratic**, **royal** → "Use a noble, aristocratic accent."
- **peasant**, **common**, **rustic** → "Use a rustic, common accent."
- **military**, **commanding**, **stern** → "Use a stern, military accent."

## Voice Control Parameters

### Speed Control
- **< 0.7** → "Speak very slowly and deliberately with long pauses."
- **< 0.85** → "Speak slowly and thoughtfully."
- **> 1.3** → "Speak rapidly with intense urgency."
- **> 1.15** → "Speak with energy and pace."

### Pitch Control
- **< 0.7** → "Use a deep, resonant, otherworldly voice."
- **< 0.85** → "Use a deeper, more authoritative voice."
- **> 1.3** → "Use a higher, ethereal, mystical voice."
- **> 1.15** → "Use a slightly higher, more spiritual voice."

### Volume/Intensity Control
- **< 0.7** → "Speak in a quiet whisper."
- **> 1.3** → "Speak with powerful, booming projection."

### Emphasis Levels
- **1** → "Use moderate dramatic emphasis."
- **2+** → "Use strong dramatic emphasis with theatrical delivery."

## Configuration Examples

### Basic Configuration
```json
{
  "tts_config": {
    "provider": "gemini",
    "voice_id": "Charon",
    "emotion": "menacing",
    "accent": "ancient"
  }
}
```

### Advanced Configuration
```json
{
  "tts_config": {
    "provider": "gemini",
    "model": "gemini-2.5-flash-preview-tts",
    "voice_id": "Charon",
    "backup_voice": "Algenib",
    "emotion": "menacing",
    "accent": "ancient",
    "pitch": 0.8,
    "speed": 0.85,
    "volume": 0.9,
    "emphasis_level": 2,
    "audio_format": "wav",
    "voice_aliases": {
      "death_voice": "Charon",
      "shadow_voice": "Algenib"
    },
    "reputation_voices": {
      "0": "Fenrir",
      "50": "Charon",
      "100": "Algenib"
    }
  }
}
```

## Audio Format Support

Gemini TTS supports multiple output formats that can be specified in the `audio_format` field:

### Supported Formats
- **wav** - WAV format (recommended for compatibility)
- **mp3** - MP3 format (smaller file size)
- **pcm** or **l16** - Raw PCM audio (may need conversion for playback)

### Format Selection
```json
{
  "tts_config": {
    "provider": "gemini",
    "audio_format": "wav"  // Choose: "wav", "mp3", "pcm", or "l16"
  }
}
```

### Complete Myrkul Example
```json
{
  "tts_config": {
    "provider": "gemini",
    "model": "gemini-2.5-flash-preview-tts",
    "voice_id": "Charon",
    "backup_voice": "Fenrir",
    "pitch": 0.8,
    "speed": 0.85,
    "volume": 0.9,
    "emotion": "menacing",
    "accent": "ancient",
    "emphasis_level": 2,
    "audio_format": "wav",
    "voice_aliases": {
      "bone_lord": "Charon",
      "shadow_king": "Fenrir",
      "death_voice": "Charon"
    },
    "reputation_voices": {
      "0": "Fenrir",
      "50": "Charon",
      "100": "Charon"
    },
    "biome_voices": {
      "nether": "Charon",
      "end": "Charon",
      "desert": "Fenrir"
    }
  }
}
```

## Natural Language Style Prompting

The Gemini TTS system uses sophisticated natural language prompting to achieve the desired voice characteristics. For example:

**Input Configuration:**
```json
{
  "emotion": "menacing",
  "accent": "ancient",
  "pitch": 0.8,
  "speed": 0.85,
  "emphasis_level": 2
}
```

**Generated Style Prompt:**
```
[Style: Speak in a menacing, threatening whisper. Use an ancient, archaic speaking style. Use a deeper, more authoritative voice. Speak slowly and thoughtfully. Use strong dramatic emphasis with theatrical delivery.] Your actual deity response text here...
```

This comprehensive enhancement system allows for incredibly detailed voice characterization that matches the personality and nature of each deity in your mod.