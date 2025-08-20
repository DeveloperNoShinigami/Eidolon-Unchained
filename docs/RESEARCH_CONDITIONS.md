# Research Condition Types

Eidolon Unchained research entries can specify **conditions** to gate
a research until certain world or player states are met. The following condition
types are supported:

Each condition mirrors a task-based equivalent, allowing you to gate an entry globally or require the same context as part of its tasks.

## Dimension
```json
"conditions": {
  "dimension": "minecraft:the_nether"
}
```
*The player must be in the specified dimension.* Task-based equivalent: `EnterDimensionTask`.

## Time Range
```json
"conditions": {
  "time_range": { "min": 13000, "max": 23000 }
}
```
*Only valid when the world time (0-24000) is within the range. Wraps around midnight when `min` is greater than `max`.* Task-based equivalent: `TimeWindowTask`.

## Weather
```json
"conditions": {
  "weather": "thunder"
}
```
*Valid weather values: `clear`, `rain`, `thunder`. The condition passes when the world's weather matches.* Task-based equivalent: `WeatherTask`.

## Inventory Items
```json
"conditions": {
  "inventory": [
    { "item": "eidolon:athame", "count": 1 }
  ]
}
```
*All listed items must be present in the player's inventory in at least the specified quantity.* Task-based equivalent: `InventoryTask`.
