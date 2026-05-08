# Prayer System

The unified prayer flow is handled by `PrayerSystem` and `PrayerTypeResolver`.

## What PrayerSystem Does

`PrayerSystem.handlePrayer(...)` is responsible for the main runtime checks before a response is generated:

- deity lookup
- AI config lookup
- patron eligibility checks
- prayer type lookup
- reputation requirement checks
- cooldown checks
- prompt construction
- AI call and command execution

## Prayer Type Resolution

Prayer type resolution is chant-first.

- if the player just performed a chant, that chant is examined first
- `prayer_effect_type` is preferred when it is present and valid
- message-based inference is only the fallback path
- the final fallback is `conversation`

## Effigy And Entry Points

Prayer spells still require a ready effigy.

- `AIDeityPrayerSpell` checks the effigy path directly before opening conversation
- some chants can also require an effigy through `requires_effigy`

The configuration surface still includes an effigy right-click toggle, but this rewrite documents chant, prayer spell, and ritual entry points as the primary paths because those are the clearest current authored systems.

## Provider Note

The broader chat system uses the provider factory, but `PrayerSystem` currently constructs a Gemini client directly for its asynchronous prayer flow. That difference is real and should be treated as current behavior, not as a documentation typo.

## Contextual Prayer Generation

`PrayerSystem.generateContextualPrayer()` enriches the prayer prompt with live player state before sending to the AI. Included context:

- current player health and max health
- hunger level
- time of day (day/night)
- whether the player is in danger (low health or hostile nearby)

This means the deity's response can vary based on how the player is actually doing in-world, without the player needing to describe their situation.

## Enemy Restriction Rules

Prayer configs can define `enemy_restrictions` under `conversation_rules` to gate prayer responses against enemy players or mobs. The resolver checks `conversationRules.get("enemy_restrictions")` and applies the appropriate filter before generating the prayer response.