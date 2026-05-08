# Quick Start

This quick start follows the current chant-first runtime, not the archived wiki flow.

## 1. Install The Required Mods

Make sure the game is running with:

- Minecraft `1.20.1`
- Forge `47.4.0`
- Eidolon Repraised
- Curios
- Eidolon Unchained

## 2. Configure An AI Provider

Set up at least one provider before testing deity conversation.

The most direct server-side path is either:

- add the API key to `config/eidolonunchained/server-api-keys.properties`
- use `/eidolon-unchained api set <provider> <key>`

If you want Player2AI, use the device-login flow:

- `/eidolon-unchained player2ai login device`

## 3. Learn The Default Chant Controls

The default chant keys currently registered in code are:

- `G` for chant slot 1
- `H` for chant slot 2
- `J` for chant slot 3
- `K` for chant slot 4
- `C` to open the chant interface

## 4. Use A Deity-Linked Chant

The main gameplay path is:

- open the chant interface
- assign or select a chant slot
- input the chant's sign sequence
- let the chanting system resolve the result

At runtime, the chanting flow checks native Eidolon spell matches first, then falls back to datapack-defined chants.

If the chant links to a deity and defines a `prayer_effect_type`, it can trigger deity conversation or a related prayer flow.

## 5. Use Effigy-Linked Paths When Required

Some content requires a ready effigy.

- Prayer spells use effigy readiness checks before they start deity conversation.
- Some chants can opt into effigy requirements through `requires_effigy`.
- Deity-linked rituals can start conversation for a nearby player when their recipe completes.

## 6. Enable TTS If You Want Voice Output

Players can toggle TTS through the in-game command surface:

- `/eidolon-unchained tts enable`
- `/eidolon-unchained tts status`
- `/eidolon-unchained tts test <text>`

If Simple Voice Chat is installed, the mod can also attempt spatial playback instead of relying only on client-side fallback audio handling.

## 7. Useful First Checks

If something is not responding, start with:

- `/eidolon-unchained deities list`
- `/eidolon-unchained api list`
- `/eidolon-unchained api test gemini`
- `/eidolon-unchained tts status`

If chants cast but deity conversation does not start, check the chant JSON first for `linked_deity` and `prayer_effect_type`.