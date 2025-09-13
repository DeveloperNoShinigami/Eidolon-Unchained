# Research System (Datapack)

Overview

- Loads Eidolon research via datapacks; registers with `Researches.register(...)`.
- Supports steps with special tasks and triggers (blocks/entities/rituals).
- Code: `src/main/java/com/bluelotuscoding/eidolonunchained/data/EidolonResearchDataManager.java`

Datapack Location

- `data/<namespace>/eidolon_research/*.json`
- Example: `src/main/resources/data/eidolonunchained/research/simple_research_example.json`

Minimal JSON Example

```json
{
  "id": "yourmod:starter",
  "stars": 2,
  "tasks": {
    "1": [
      { "type": "item", "item": "minecraft:iron_ingot", "count": 4 }
    ]
  },
  "triggers": [
    { "block": "minecraft:crafting_table" }
  ]
}
```

Task Types (selected)

- Items/Item (single or expanded array)
- KillEntities / KillEntityWithNbt
- CraftItems
- UseRitual
- CollectItems / Inventory
- EnterDimension
- TimeWindow / Weather
- HasItemWithNbt / HasNbt
- ExploreBiomes

Tips

- Stars must be 1–10.
- `tasks` keys are step numbers as strings (e.g., "1", "2").
- Rewards can be emitted on `onLearned` via a `rewards` array.
