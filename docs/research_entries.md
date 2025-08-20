# 📚 Research Entry Reference

This guide explains how to define research entries for Eidolon Unchained. Each entry extends the research tree with custom progression nodes loaded by the [`ResearchDataManager`](../src/main/java/com/bluelotuscoding/eidolonunchained/data/ResearchDataManager.java).

## 🔑 Field Definitions

| Field | Description |
|-------|-------------|
| `id` | Unique resource ID for the entry. |
| `chapter` | Chapter ID the entry belongs to. |
| `icon` | Item used as the entry's icon (supports `item`, optional `count` and `nbt`). |
| `prerequisites` | Array of research IDs required before this entry unlocks. |
| `unlocks` | Research IDs made available after completion. |
| `type` | Entry category: `basic`, `advanced`, `forbidden`, `ritual`, or `crafting`. |
| `x`, `y` | Coordinates on the research screen grid. |
| `conditions` | Environmental or player state requirements that must be met before progress begins. Conditions gate when tasks are eligible to run, letting you restrict entries by dimension, time, weather, or inventory. |
| `tasks` | Objectives the player must complete. Each task uses a specific task class such as `KillEntitiesTask`, `CollectItemsTask`, `UseRitualTask`, `EnterDimensionTask`, `TimeWindowTask`, `WeatherTask`, or `InventoryTask`. |
| `rewards` | Extra benefits granted on completion (items, advancements, unlocks). |

> **Note:** The JSON key for conditions is `"conditions"`. Earlier drafts of the documentation used `"conditional_requirements"`, but that name has been retired.

Conditions and tasks complement each other—conditions set the scene, limiting when a research entry is active, while tasks define the actions the player must perform once those conditions are met.

## ⭐ Star Requirements

The `required_stars` value gates access to higher‑tier research. Star costs should scale with entry complexity and reward. If omitted, star costs fall back to the entry type: 0 for `basic` or `crafting`, 1 for `advanced` or `ritual`, and 2 for `forbidden` entries.

## 📊 Task Tiers

Tasks are grouped into difficulty tiers:

- `tier_1` – introductory tasks
- `tier_2` – intermediate challenges
- `tier_3` – end‑game objectives

Higher tiers may require more stars or advanced resources. Task classes handle validation and completion logic.

## ⚖️ Balancing Tips

- Start with low star costs and simple tasks, increasing difficulty gradually.
- Mix task types (combat, crafting, exploration) to keep progression varied.
- Ensure rewards justify the required stars and effort.
- Use conditions to gate entries to specific environments or player states.

## 🔗 Related Code

- [`ResearchDataManager`](../src/main/java/com/bluelotuscoding/eidolonunchained/data/ResearchDataManager.java): loads chapters and entries from datapacks.
- Task implementations (e.g., `ResearchTask` and its subclasses) validate tiered objectives.
- Condition classes evaluate context such as biome, time, or player properties before tasks become available.

