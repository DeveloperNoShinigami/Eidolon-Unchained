# STT and Spatial Audio Plan

This outlines the minimal scaffolding to finish voice parity:

## Speech-to-Text (STT) pipeline
- Detect Simple Voice Chat presence (API already added in build.gradle).
- During an active deity conversation, when the player presses the voice chat key:
  - Show "(voice) Listening..." and buffer mic frames.
  - Send buffered audio to a configured STT provider (Player2 or Web STT).
  - On transcription, feed the text straight into `DeityChat` as if typed.
- Fallback: if Simple Voice Chat isn't present, remain text-only.

Contracts
- Input: 16k–48k PCM or OGG/Opus frames (as provided by Simple Voice Chat API hooks)
- Output: single final transcript string (for now)
- Error modes: timeout, empty audio, provider error → fall back to text prompts

## Spatial deity TTS playback
- If Simple Voice Chat is available, play deity TTS via a dedicated category/channel near the effigy or player.
- Fallback: continue using client playback from `TTSAudioPacket` (existing behavior).

Edge cases
- No STT provider configured
- Partial/whisper audio
- Multiple players simultaneous
- Conversations timing out mid-recording

Next steps
1. Add `VoiceChatIntegration` façade: detect availability and expose two methods: `playSpatial(audio, pos)` and `onMicBuffer(callback)`.
2. Wire `DeityChat` to trigger STT capture when voice key active during conversations.
3. Implement Player2 STT client and Web STT client (mirroring TTS structure, config-based)
4. Optional: interim push-to-talk hint and icon.
