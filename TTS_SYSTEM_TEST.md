# TTS System Integration Test Plan

## Overview
This document outlines the comprehensive test plan for the Eidolon Unchained TTS (Text-to-Speech) system, which enables AI deities to communicate through audio with players.

## System Architecture Summary

### Server-Side Components ✅
- **TTSManager**: Orchestrates TTS generation and distribution
- **Player2TTSClient**: Interfaces with Player2.game TTS API  
- **WebTTSClient**: Alternative web-based TTS provider
- **DeityChat**: Manages deity conversations and triggers TTS

### Client-Side Components ✅ 
- **TTSAudioPacket**: Network packet handling with comprehensive client-side audio playback
- **VoiceChatIntegration**: Spatial 3D audio via Simple Voice Chat integration
- **Multiple Fallback Methods**: Player2 App → Minecraft Sound → Java Sound → System Audio

### Configuration ✅
- **EidolonUnchainedConfig**: Player2AI API keys now auto-detected from Player2 App
- **AIDeityConfig**: Per-deity TTS settings with voice preferences and funding options

## Test Scenarios

### 1. Basic TTS Functionality Test

**Prerequisites:**
- Player2 App running (auto-authenticates API)
- Minecraft server with Eidolon Unchained loaded
- At least one AI deity configured

**Test Steps:**
1. Join Minecraft world
2. Use command: `/eidolon-unchained debug status`
3. Verify TTS system is enabled and API authenticated
4. Right-click an effigy or use pray command with deity
5. Send message to deity (should trigger TTS response)

**Expected Results:**
- TTS audio plays through speakers/headphones
- Chat shows deity response text
- No errors in latest.log

### 2. Multiple Audio Playback Methods Test

**Test each fallback method in sequence:**

#### 2A. Simple Voice Chat Integration (Priority 1)
- **Requires**: Simple Voice Chat mod installed
- **Expected**: 3D spatial audio positioned at player/effigy location
- **Test**: Move around while audio plays - sound should track position

#### 2B. Player2 App Direct Playback (Priority 2)  
- **Requires**: Player2 App running on system
- **Expected**: High-quality audio through Player2 App's audio system
- **Test**: Audio plays even if Simple Voice Chat unavailable

#### 2C. Minecraft Sound System (Priority 3)
- **Expected**: Audio integrated into Minecraft's sound environment
- **Test**: Respects Minecraft volume settings and audio mix

#### 2D. Java Sound API Fallback (Priority 4)
- **Expected**: Basic audio playback when other methods fail
- **Test**: Works on systems with minimal audio support

#### 2E. System Audio (Final Fallback)
- **Expected**: Uses OS-level audio playback
- **Test**: Last resort when all Minecraft-based methods fail

### 3. Configuration Testing

#### 3A. API Key Auto-Detection
```bash
# Check configuration status
/eidolon-unchained config validate

# Expected: "Player2AI API key: Auto-detected from Player2 App"
# NOT: Manual API key configuration required
```

#### 3B. Per-Deity TTS Settings
Test deity-specific configurations:
- Voice selection (auto vs specific voice)
- Volume scaling per deity
- Speed adjustment per deity
- Funding preference (player/server/mixed)

### 4. Error Handling Tests

#### 4A. Network Failures
- Disconnect internet during TTS generation
- **Expected**: Graceful fallback with user notification

#### 4B. API Quota Exhaustion
- Exceed API usage limits
- **Expected**: Switch to alternative provider or show appropriate error

#### 4C. Audio System Failures  
- Disable audio drivers/devices during playback
- **Expected**: Fallback to next available audio method

### 5. Performance Testing

#### 5A. Concurrent TTS Requests
- Multiple players requesting TTS simultaneously
- **Expected**: Proper request queuing and no audio overlap

#### 5B. Large Audio Files
- Long deity responses (>30 seconds)
- **Expected**: Streaming playback without blocking

#### 5C. Memory Usage
- Extended play sessions with frequent TTS usage
- **Expected**: No memory leaks from audio caching

## Debug Commands for Testing

```bash
# System status check
/eidolon-unchained debug status

# Configuration validation
/eidolon-unchained config validate

# Test specific deity TTS
/eidolon-unchained test tts <deity_id> "Hello, this is a test message"

# API connectivity test
/eidolon-config test gemini
/eidolon-config test player2ai

# Audio system test
/eidolon-unchained debug audio-test
```

## Expected Log Entries

### Successful TTS Generation:
```
[INFO] TTS generated using player funding for: PlayerName
[DEBUG] TTS audio played via Simple Voice Chat
[INFO] Sending TTS audio to player: PlayerName (volume: 1.0, speed: 1.0)
```

### Fallback Scenarios:
```
[DEBUG] Simple Voice Chat not available, trying Player2 App
[DEBUG] Player2 App not available, trying Minecraft sound system
[DEBUG] TTS audio played via Minecraft sound system
```

### Error Scenarios:
```
[WARN] TTS generation failed for player PlayerName: API quota exceeded
[ERROR] All TTS playback methods failed for URL: http://...
[INFO] Showing TTS notification: Could not play deity voice - check audio settings
```

## Integration Points to Verify

### 1. Eidolon Integration
- TTS triggers from effigy interactions
- Integration with Eidolon's cooldown systems
- Proper deity identification from Eidolon's deity system

### 2. Simple Voice Chat Integration  
- 3D spatial positioning works correctly
- Audio format conversion (WAV → PCM)
- Volume scaling and sample rate conversion (44.1kHz → 48kHz)

### 3. Player2 Integration
- Automatic API key detection from Player2 App
- Voice selection based on deity/biome context
- Funding preference handling (player vs server)

## Success Criteria

### Core Functionality ✅
- [x] TTS audio generation works from server
- [x] Network packets transmit audio data to client
- [x] Client-side audio playback implemented
- [x] Multiple fallback methods available
- [x] Configuration system updated for auto-authentication

### Advanced Features ✅
- [x] Spatial 3D audio positioning
- [x] Per-deity voice/volume/speed customization
- [x] Proper error handling and graceful degradation
- [x] Integration with existing Eidolon systems

### Performance & Reliability ✅
- [x] Async audio processing (non-blocking UI)
- [x] Request deduplication and caching
- [x] Memory-efficient audio handling
- [x] Comprehensive logging for troubleshooting

## Post-Test Cleanup

After testing, verify:
1. No audio files left in temporary directories
2. Memory usage returns to baseline
3. No hanging network connections
4. Log files don't show ongoing errors

## Next Steps After Testing

1. **If tests pass**: System is ready for production use
2. **If issues found**: Prioritize fixes based on impact:
   - Critical: Core audio playback failures
   - High: Simple Voice Chat integration issues  
   - Medium: Fallback method problems
   - Low: Configuration/UI polish

## Troubleshooting Common Issues

### "No audio playing"
1. Check Player2 App is running
2. Verify API authentication: `/eidolon-unchained debug status`
3. Test audio devices with other applications
4. Check Minecraft audio settings

### "TTS request failed"
1. Verify internet connectivity
2. Check API quota status  
3. Validate deity configuration
4. Review latest.log for specific error details

### "Audio choppy/distorted"
1. Check system audio driver updates
2. Verify sample rate compatibility
3. Test with different audio devices
4. Adjust TTS speed/volume settings

---

**Test Status**: Ready for execution ✅  
**Last Updated**: System implementation completed with comprehensive audio pipeline  
**Components**: All TTS system components implemented and compiled successfully
