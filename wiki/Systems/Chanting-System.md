# Chanting System

Overview

- Player‑centered, event‑driven chant input and execution.
- No ticking loops; spells schedule to resolve ~1s after completion.
- Code: `src/main/java/com/bluelotuscoding/eidolonunchained/chant/PlayerChantingSystem.java`

Key Behavior

- Signs are added with visual feedback via `KeybindSignEffectsManager.runSignEffects(...)`.
- Checks native Eidolon spells first, then datapack chants.
- If a matching Eidolon spell exists, it is cast with a small delay.
- If a matching datapack chant exists, its mapped `DatapackChantSpell` is cast.

Datapack Chants

- Location: `src/main/resources/data/eidolonunchained/chants/*.json`
- Example: `shadow_communion.json` (working)

Minimal JSON Example

```json
{
  "name": "Shadow Communion",
  "id": "eidolonunchained:shadow_communion",
  "signs": [
    "eidolon:wicked",
    "eidolon:wicked",
    "eidolon:blood"
  ],
  "spell": "eidolonunchained:shadow_communion"
}
```

Tips

- Ensure sign order matches exactly when `requireExactSignOrder` is enabled.
- Keep sequences short and readable; 3–4 signs works well.
- Add `*_comment_structure` keys in `en_us.json` to document localization groups instead of `//` comments.
