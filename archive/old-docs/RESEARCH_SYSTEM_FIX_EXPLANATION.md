# Research System Fix - Complete Explanation

## Problem Overview

The research system in Eidolon Unchained was broken due to a fundamental mismatch between how research data was structured and how it was being parsed. This resulted in:

- ❌ Research entries showing as "0 custom research entries" in logs
- ❌ Research triggers working but not creating discoverable research
- ❌ Eidolon's default right-click triggers not functioning
- ❌ Research notes not appearing at the research table

## Root Cause Analysis

### The Issue: Format Mismatch

The system had **two different research formats** but only one parser:

#### 1. **Trigger-Based Research Format** (Original Working Format)
Located in: `src/main/resources/data/eidolonunchained/research/`

```json
{
  "id": "simple_research_example",
  "stars": 2,
  "triggers": [
    "minecraft:zombie",
    {
      "type": "kill_entity", 
      "entity": "minecraft:zombie",
      "nbt": {},
      "item_requirements": { ... }
    }
  ],
  "tasks": {
    "0": [
      { "type": "item", "item": "minecraft:stick", "count": 1 }
    ],
    "1": [
      { "type": "item", "item": "minecraft:cobblestone", "count": 5 }
    ]
  },
  "rewards": [ ... ]
}
```

#### 2. **Codex-Style Research Format** (New Format)
Located in: `src/main/resources/data/eidolonunchained/research_entries/`

```json
{
  "id": "simple_example_entry",
  "title": "eidolonunchained.research.entry.simple_example.title",
  "description": "eidolonunchained.research.entry.simple_example.description", 
  "target_chapter": "simple_examples",
  "prerequisites": ["eidolonunchained:simple_research_example"]
}
```

### The Problem

The `ResearchDataManager.loadResearchEntry()` method was designed to parse **codex-style** research (expecting `title`, `description`, `chapter` fields) but was being fed **trigger-based** research files (with `triggers`, `stars`, `tasks` fields).

This caused:
1. Research files to be skipped during loading
2. Empty research registry (0 entries loaded)
3. No integration with Eidolon's research system
4. Broken trigger functionality

## The Solution

### Multi-Format Parser Architecture

I redesigned the `ResearchDataManager` to intelligently detect and handle both formats:

```java
private void loadResearchEntry(ResourceLocation location, JsonObject json) {
    // Auto-detect format based on key fields
    if (json.has("triggers") && json.has("stars")) {
        // Original working format - trigger-based research
        loadTriggerBasedResearch(entryId, json);
    } else if (json.has("chapter") || json.has("target_chapter")) {
        // New format - codex-style research  
        loadCodexStyleResearch(entryId, location, json);
    } else {
        // Error: Unknown format
        LOGGER.warn("Research entry {} is neither trigger-based nor codex-style", entryId);
    }
}
```

### Trigger-Based Research Parser

Created `loadTriggerBasedResearch()` to handle the original format:

```java
private void loadTriggerBasedResearch(ResourceLocation entryId, JsonObject json) {
    // Convert trigger-based research to ResearchEntry format
    Component title = Component.translatable("eidolonunchained.research." + entryId.getPath() + ".title");
    Component description = Component.translatable("eidolonunchained.research." + entryId.getPath() + ".description");
    
    // Use default chapter for trigger-based research
    ResourceLocation chapter = new ResourceLocation(EidolonUnchained.MODID, "trigger_research");
    
    // Parse stars for required_stars field
    int requiredStars = json.has("stars") ? json.get("stars").getAsInt() : 1;
    
    // Parse tasks from trigger-based format
    Map<Integer, List<ResearchTask>> tasks = parseTriggerBasedTasks(json);
    
    // Create ResearchEntry and register it
    ResearchEntry entry = new ResearchEntry(entryId, title, description, chapter, ...);
    LOADED_RESEARCH_ENTRIES.put(entryId, entry);
}
```

### Task Format Conversion

Added `parseTask()` to convert trigger-based task definitions:

```java
private ResearchTask parseTask(JsonObject taskObj, ResourceLocation researchId) {
    String type = taskObj.get("type").getAsString();
    
    switch (type) {
        case "item":
            // Convert: {"type": "item", "item": "minecraft:stick", "count": 1}
            // To: CollectItemsTask
            return new CollectItemsTask(itemId, count);
            
        case "kill":
        case "kill_entity":
            // Convert: {"type": "kill_entity", "entity": "minecraft:zombie", "count": 1} 
            // To: KillEntitiesTask
            return new KillEntitiesTask(entityId, count);
            
        case "craft":
        case "craft_item":
            // Convert: {"type": "craft", "item": "minecraft:sword", "count": 1}
            // To: CraftItemsTask
            return new CraftItemsTask(itemId, count);
    }
}
```

## Folder Structure Clarification

The system now correctly uses these folders:

```
src/main/resources/data/eidolonunchained/
├── research/                    # Trigger-based research (original format)
│   ├── simple_research_example.json
│   ├── nature_blessing_ritual.json
│   └── divine_ritual_discovery.json
├── research_entries/            # Codex-style research (new format)  
│   ├── simple_example_entry.json
│   ├── master_necromancer.json
│   └── void_walker.json
└── research_chapters/           # Research chapter definitions
    ├── simple_examples.json
    ├── advanced_necromancy.json
    └── divine_rituals.json
```

## Integration Flow

### Before Fix (Broken)
```
1. ResearchDataManager loads files from research/ 
2. Tries to parse as codex-style (expects "chapter" field)
3. Trigger-based files don't have "chapter" field
4. Files rejected/skipped
5. 0 research entries loaded
6. Nothing registered with Eidolon
7. Triggers work but create no discoverable research
```

### After Fix (Working)
```
1. ResearchDataManager loads files from research/
2. Detects trigger-based format (has "triggers" + "stars")
3. Converts to ResearchEntry format via loadTriggerBasedResearch()
4. Parses tasks and creates proper ResearchTask objects
5. Registers entries with LOADED_RESEARCH_ENTRIES
6. EidolonResearchIntegration injects into Eidolon system
7. Research appears in game and triggers work correctly
```

## Key Features of the Fix

### 1. **Backward Compatibility**
- ✅ Existing trigger-based research files work without changes
- ✅ New codex-style research files also supported
- ✅ Both formats can coexist

### 2. **Automatic Chapter Creation**
- ✅ Creates default "trigger_research" chapter for trigger-based entries
- ✅ Prevents missing chapter errors
- ✅ Organizes trigger-based research separately

### 3. **Task Format Support**
- ✅ Supports `item`, `kill`/`kill_entity`, `craft`/`craft_item` task types
- ✅ Converts to proper ResearchTask objects
- ✅ Maintains task tier structure (0, 1, 2, etc.)

### 4. **Error Handling**
- ✅ Graceful handling of unknown formats
- ✅ Detailed logging for debugging
- ✅ Continues loading other files if one fails

### 5. **Translation Support**
- ✅ Auto-generates translation keys for trigger-based research
- ✅ Format: `eidolonunchained.research.{id}.title` and `.description`
- ✅ Falls back to literal text if translations missing

## Testing the Fix

### Log Verification
Look for these log messages indicating success:

```
[INFO] Loaded trigger-based research: eidolonunchained:simple_research_example
[INFO] Loaded trigger-based research: eidolonunchained:nature_blessing_ritual  
[INFO] Attempting to inject 3 custom research entries  (should be > 0 now!)
[INFO] ✓ Injected research entry: eidolonunchained:simple_research_example
```

### In-Game Testing
1. **Trigger Activation**: Kill entities, interact with blocks, etc.
2. **Research Note Creation**: Triggers should create research notes in inventory
3. **Research Table**: Notes should be discoverable at the research table
4. **Eidolon Integration**: Default Eidolon right-click triggers should work

## Technical Details

### ResearchEntry Creation
Trigger-based research gets converted to full ResearchEntry objects:

```java
ResearchEntry entry = new ResearchEntry(
    entryId,                    // Resource location
    title,                      // Translatable component
    description,                // Translatable component  
    chapter,                    // Default trigger_research chapter
    icon,                       // Default book icon
    new ArrayList<>(),          // No prerequisites (for now)
    new ArrayList<>(),          // No unlocks (for now)
    0, 0,                      // Default x,y coordinates
    ResearchEntry.ResearchType.BASIC,  // Default type
    requiredStars,              // From "stars" field
    new JsonObject(),           // No additional data
    tasks,                      // Converted task map
    new ArrayList<>()           // No conditions
);
```

### Task Tier Processing
Tasks are organized by tiers from the JSON:

```json
"tasks": {
  "0": [{"type": "item", "item": "minecraft:stick", "count": 1}],
  "1": [{"type": "item", "item": "minecraft:cobblestone", "count": 5}]
}
```

Becomes:
```java
Map<Integer, List<ResearchTask>> tasks:
- 0 → [CollectItemsTask(minecraft:stick, 1)]
- 1 → [CollectItemsTask(minecraft:cobblestone, 5)]
```

## Future Enhancements

### Potential Improvements
1. **Prerequisites Support**: Parse prerequisites from trigger-based research
2. **Custom Icons**: Support icon definitions in trigger-based format
3. **Unlock Chains**: Support research unlock relationships
4. **Conditions**: Convert trigger conditions to research conditions
5. **Rewards Integration**: Connect rewards to research completion

### Migration Path
- Existing trigger-based research continues working
- New research can use either format
- Gradual migration to codex-style format possible
- Hybrid approach allows flexibility

## Conclusion

This fix restores the research system functionality by:

1. **Recognizing** that two different research formats existed
2. **Adapting** the parser to handle both formats intelligently  
3. **Converting** trigger-based research to the expected ResearchEntry format
4. **Maintaining** backward compatibility with existing content
5. **Enabling** proper integration with Eidolon's research system

The research system should now work as originally intended, with triggers creating discoverable research notes that can be studied at the research table.
