# Codex Entry Reference

This guide lists the fields available in codex entry JSON files for **Eidolon Unchained** and demonstrates valid values taken from the bundled data examples.

## Entry Fields

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| `target_chapter` | string | Chapter ID that will contain this entry. | `"rituals"` |
| `title` | string | Displayed title for the entry. | `"Fire Mastery"` |
| `icon` | string | Item ID used as the entry icon. | `"minecraft:fire_charge"` |
| `pages` | array | Ordered list of page objects. Each page requires a `type`. | see below |

### Sample Entry
```json
{
  "target_chapter": "rituals",
  "title": "Fire Mastery",
  "icon": "minecraft:fire_charge",
  "pages": [
    {
      "type": "text",
      "text": "Master the ancient art of fire magic with these powerful techniques. Fire spells require intense concentration and respect for the destructive force you wield."
    },
    {
      "type": "crafting",
      "content": "Fire Focus Crystal",
      "data": {
        "result": { "item": "minecraft:fire_charge", "count": 4 },
        "pattern": [" R ", "RGR", " R "],
        "key": { "R": "minecraft:redstone", "G": "minecraft:gold_ingot" }
      }
    }
  ]
}
```

## Page Types

### `text`
```
{ "type": "text", "text": "..." }
```
Displays a paragraph of text.

### `list`
```
{
  "type": "list",
  "text": "eidolon.codex.altar_overview",
  "entries": [
    { "item": "minecraft:gold_block", "text": "Gold Block" },
    { "item": "minecraft:lapis_block", "text": "Lapis Block" }
  ]
}
```
Displays a list of items and shows each one's altar power and capacity based on its `AltarEntry`.

### `crafting`
```
{
  "type": "crafting",
  "content": "Fire Focus Crystal",
  "data": {
    "result": { "item": "minecraft:fire_charge", "count": 4 },
    "pattern": [" R ", "RGR", " R "],
    "key": { "R": "minecraft:redstone", "G": "minecraft:gold_ingot" }
  }
}
```
Displays a shaped crafting recipe.

### `crucible`
```
{
  "type": "crucible",
  "content": "Crystal Essence Extraction",
  "data": {
    "result": { "item": "minecraft:prismarine_crystals", "count": 4 },
    "steps": [
      {"item": "minecraft:end_crystal", "count": 1},
      {"item": "minecraft:water_bucket", "count": 1},
      {"item": "minecraft:soul_sand", "count": 8}
    ],
    "description": "Extracts pure magical essence from End Crystals"
  }
}
```
Specialized recipe page for crucible crafting.

### `ritual`
```
{
  "type": "ritual",
  "content": "Greater Summoning Circle",
  "data": {
    "circle_size": 5,
    "participants": 3,
    "components": [
      {"item": "minecraft:bell", "count": 1},
      {"item": "minecraft:soul_sand", "count": 32},
      {"item": "minecraft:wither_skeleton_skull", "count": 3},
      {"item": "minecraft:nether_star", "count": 1}
    ],
    "description": "Summons a powerful ally to aid the community"
  }
}
```
Shows a top‑down ritual circle with components.

### `workbench`
```
{
  "type": "workbench",
  "content": "Soul Chain Creation",
  "data": {
    "result": { "item": "minecraft:chain", "count": 8 },
    "pattern": ["SIS", "ICI", "SIS"],
    "key": {
      "S": "minecraft:soul_sand",
      "I": "minecraft:iron_ingot",
      "C": "minecraft:chain"
    }
  }
}
```
Displays an Eidolon workbench recipe.

### `crafting_recipe`
```
{ "type": "crafting_recipe", "recipe": "eidolonunchained:shadow_crystal", "text": "The Shadow Crystal serves as a focus for shadow-based spells." }
```
Links to an existing crafting recipe ID.

### `ritual_recipe`
```
{ "type": "ritual_recipe", "ritual": "eidolonunchained:shadow_bind", "text": "The Shadow Bind ritual allows you to temporarily merge with your own shadow." }
```
References a predefined ritual by ID.

### `image`
```
{ "type": "image", "image": "eidolonunchained:textures/gui/codex/shadow_diagram.png", "width": 128, "height": 96 }
```
Embeds a texture into the page.

These examples are pulled directly from the sample codex data shipped with the project and represent the currently supported page types.
