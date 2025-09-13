# Effigy & Prayer

Purpose

- Mirrors and enhances Eidolon’s effigy interaction during AI conversations.
- Adds ramp‑up, steady, pulsing, and completion visual states; optional start sound.

Core Classes

- `EffigyEffectsManager` — `src/main/java/com/bluelotuscoding/eidolonunchained/effects/EffigyEffectsManager.java`
  - Manages `ActiveEffigyEffect` state machine
  - Server tick subscriber drives particle/sound effects
  - Hooks for "AI responding" pulses
- `AIDeityPrayerSpell` — computes chant‑caster position for accurate effigy lookup

Config

- `effigyConversationSoundId` / `Volume` / `Pitch` (set to `none` to disable)

Best Practices

- Place the effigy within typical altar radius of the player.
- Keep visual intensity moderate for readability in multiplayer.
