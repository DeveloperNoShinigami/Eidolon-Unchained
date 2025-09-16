# TTS Quick Setup Guide

Get deity voices working in 5 minutes or less!

## For Players

### Step 1: Enable TTS
```bash
/eidolon-unchained tts enable
```

### Step 2: Test Your Setup
```bash
/eidolon-unchained tts test "The deities speak to you"
```

### Step 3: Configure Funding (Optional)
```bash
# Let players pay when they can, server covers when needed (recommended)
/eidolon-unchained tts funding player-first

# Server pays for everything
/eidolon-unchained tts funding server-only

# Only use your own Player2 App (no server cost)
/eidolon-unchained tts funding player-only
```

### Step 4: Talk to a Deity!
Start a conversation with any deity and their responses will be spoken aloud.

## For Server Admins

### Option 1: Server-Funded TTS
1. Get a Player2.game API key
2. Add to `run/config/eidolonunchained-common.toml`:
   ```toml
   player2ai_api_key = "your_api_key_here"
   ```
3. Restart server
4. Monitor costs with `/eidolon-unchained tts stats`

### Option 2: Player-Funded TTS (Free for Server)
1. Tell players to install Player2 App
2. Players configure their own API keys
3. Server has zero TTS costs!

### Option 3: Hybrid Approach (Recommended)
- Set up server API key as backup
- Encourage players to use Player2 App
- Server only pays when player funding unavailable

## Voice Customization Examples

### Give Each Deity a Unique Voice
Add to your deity JSON files:

```json
{
  "tts_config": {
    "voice_id": "male-deep-1",
    "emotion": "ominous"
  }
}
```

### Make Voices Change with Reputation
```json
{
  "tts_config": {
    "reputation_voices": {
      "0": "male-whisper-1",
      "50": "male-deep-1",
      "100": "male-deep-intense-1"
    }
  }
}
```

### Context-Aware Voices
```json
{
  "tts_config": {
    "time_voices": {
      "night": "male-deep-1",
      "day": "female-warm-1"
    },
    "biome_voices": {
      "minecraft:deep_dark": "male-whisper-1"
    }
  }
}
```

## Troubleshooting

### No Audio?
1. Check: `/eidolon-unchained tts status`
2. Test: `/eidolon-unchained tts test "hello"`
3. Try different voice: `/eidolon-unchained tts voice neutral-1`

### API Errors?
1. Verify API key configuration
2. Check Player2 App is running (for player funding)
3. Check server logs for detailed errors

### Still Not Working?
See the [complete TTS reference](TTS-Complete-Reference.md) for detailed troubleshooting.

## Quick Commands Reference

| Command | Purpose |
|---------|---------|
| `/eidolon-unchained tts enable` | Turn on TTS |
| `/eidolon-unchained tts disable` | Turn off TTS |
| `/eidolon-unchained tts status` | Check TTS status |
| `/eidolon-unchained tts test <text>` | Test TTS with custom text |
| `/eidolon-unchained tts voice auto` | Use deity-appropriate voices |
| `/eidolon-unchained tts voice <id>` | Use specific voice |
| `/eidolon-unchained tts volume <0-2>` | Adjust volume |
| `/eidolon-unchained tts speed <0.5-2>` | Adjust speech speed |
| `/eidolon-unchained tts funding player-first` | Smart funding mode |

Ready to hear the gods speak? Start with `/eidolon-unchained tts enable` and talk to your first deity!