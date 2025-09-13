# Research JSON Reference

Location

- `data/<namespace>/eidolon_research/*.json`

Top-Level Fields

- `id` (string): Namespaced research ID
- `stars` (int): 1–10
- `tasks` (object): Step → array of task objects
- `triggers` (array): Blocks/entities/rituals to activate the research
- `rewards` (array, optional): Items/commands awarded on completion

Task Object Examples

- Item(s): `{ "type":"item", "item":"minecraft:iron_ingot", "count":4 }`
- Multiple items (expanded by loader): `{ "type":"items", "items":[ {"item":"minecraft:iron_ingot"}, {"item":"minecraft:gold_ingot"} ] }`
- Ritual: `{ "type":"use_ritual", "ritual":"eidolon:crystallization", "count":1 }`
- Kill with NBT: `{ "type":"kill_entity_nbt", "entity":"minecraft:zombie", "nbt":"{IsBaby:1b}", "count":3 }`

Trigger Examples

- Block: `{ "block": "minecraft:crafting_table" }`
- Entity: `{ "entity": "minecraft:villager" }`
- Ritual: `{ "ritual": "eidolon:crystallization" }`
