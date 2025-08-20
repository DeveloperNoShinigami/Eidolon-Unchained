# Research Condition Types

Eidolon Unchained research entries can specify **conditional_requirements** to gate
a research until certain world or player states are met. The following condition
types are supported:

## Dimension
```json
"conditional_requirements": {
  "dimension": "minecraft:the_nether"
}
```
*The player must be in the specified dimension.*

## Time Range
```json
"conditional_requirements": {
  "time_range": { "min": 13000, "max": 23000 }
}
```
*Only valid when the world time (0-24000) is within the range. Wraps around midnight when `min` is greater than `max`.*

## Weather
```json
"conditional_requirements": {
  "weather": "thunder"
}
```
*Valid weather values: `clear`, `rain`, `thunder`. The condition passes when the world's weather matches.*

## Inventory Items
```json
"conditional_requirements": {
  "inventory": [
    { "item": "eidolon:athame", "count": 1 }
  ]
}
```
*All listed items must be present in the player's inventory in at least the specified quantity.*
