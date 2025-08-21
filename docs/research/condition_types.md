# Research Condition Classes

Research entries can require certain world or player states before they become available. These checks are implemented as [`ResearchCondition`](../../src/main/java/com/bluelotuscoding/eidolonunchained/research/conditions/ResearchCondition.java) interfaces.

## InventoryCondition – item requirements
Requires the player to hold at least a specified number of an item.

```java
// simplified
public class InventoryCondition implements ResearchCondition {
    private final Item item;
    private final int count;
    public boolean test(Player player) {
        int found = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                found += stack.getCount();
                if (found >= count) return true;
            }
        }
        return false;
    }
}
```
*Source: [`InventoryCondition.java`](../../src/main/java/com/bluelotuscoding/eidolonunchained/research/conditions/InventoryCondition.java)*

Example usage:
```json
"conditions": {
  "inventory": [ { "item": "eidolon:athame", "count": 1 } ]
}
```

## DimensionCondition – dimension check
Valid only when the player is in a specific dimension.

```java
public boolean test(Player player) {
    return player.level().dimension().location().equals(dimension);
}
```
*Source: [`DimensionCondition.java`](../../src/main/java/com/bluelotuscoding/eidolonunchained/research/conditions/DimensionCondition.java)*

Example usage:
```json
"conditions": {
  "dimension": "minecraft:the_nether"
}
```

## TimeCondition – time window
Requires the world time to fall within a range.

```java
long time = player.level().getDayTime() % 24000L;
if (min <= max) {
    return time >= min && time <= max;
} else {
    return time >= min || time <= max; // wrap around midnight
}
```
*Source: [`TimeCondition.java`](../../src/main/java/com/bluelotuscoding/eidolonunchained/research/conditions/TimeCondition.java)*

Example usage:
```json
"conditions": {
  "time_range": { "min": 13000, "max": 23000 }
}
```

## WeatherCondition – weather state
Checks the current weather against `clear`, `rain`, or `thunder`.

```java
return switch (weather) {
    case CLEAR -> !level.isRaining();
    case RAIN -> level.isRaining() && !level.isThundering();
    case THUNDER -> level.isThundering();
};
```
*Source: [`WeatherCondition.java`](../../src/main/java/com/bluelotuscoding/eidolonunchained/research/conditions/WeatherCondition.java)*

Example usage:
```json
"conditions": {
  "weather": "thunder"
}
```

## Advancement-based conditions
The codebase currently includes no dedicated `AdvancementCondition`. Advancement-related gating can instead be expressed using tasks or custom triggers.
