# Codex Page Tutorial: Advanced Wraith Summoning

This guide walks through creating a Codex page for a custom ritual called **Advanced Wraith Summoning**.

## Step 1: Create the Entry File

Path: `data/eidolonunchained/codex_entries/rituals/advanced_wraith_summoning.json`

```json
{
  "target_chapter": "eidolon:summon_ritual",
  "title": "eidolonunchained.codex.entry.advanced_wraith_summoning.title",
  "icon": { "item": "eidolon:wraith_spawn_egg" },
  "pages": [
    { "type": "text", "text": "eidolonunchained.codex.entry.advanced_wraith_summoning.intro" },
    { "type": "ritual", "ritual": "eidolonunchained:advanced_wraith_summoning" }
  ]
}
```

## Step 2: Add Translations

Path: `assets/eidolonunchained/lang/en_us.json`

```json
{
  "eidolonunchained.codex.entry.advanced_wraith_summoning.title": "Advanced Wraith Summoning",
  "eidolonunchained.codex.entry.advanced_wraith_summoning.intro": "Bind wraiths with refined ritual techniques."
}
```

## Step 3: Define the Ritual

Path: `data/eidolonunchained/rituals/advanced_wraith_summoning.json`

```json
{
  "type": "eidolon:ritual",
  "activation_item": { "item": "eidolon:necrotic_focus" },
  "inputs": [
    { "item": "minecraft:bone" },
    { "item": "eidolon:shadow_gem" }
  ],
  "entity": "eidolon:wraith"
}
```

## Step 4: Verify In Game

Place the datapack in your world, run `/reload`, then open the Codex to confirm the new page appears under the Summon Ritual chapter.

Example Codex layout:

```
+-------------------------------+
| Advanced Wraith Summoning     |
| [Wraith Spawn Egg Icon]       |
| Bind wraiths with refined     |
| ritual techniques.            |
+-------------------------------+
```

