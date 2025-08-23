# Eidolon Unchained Integration System Reference

## Overview
Eidolon Unchained is a **datapack-driven extension system** that empowers **modpack makers and developers** to create custom Eidolon content through JSON configuration. This system provides a full expansion of Eidolon's capabilities while maintaining seamless compatibility.

## Core Philosophy
- **Datapack-First**: All content creation through JSON datapacks
- **No Hardcoding**: Avoid hardcoded solutions, prioritize configuration
- **Modpack-Friendly**: Designed for easy content pack distribution
- **Extension-Focused**: Expand Eidolon without breaking existing functionality

## Chapter Extension System

### Purpose
Enable content creators to add custom entries to existing chapters (both Eidolon and custom) through simple JSON datapack configuration, with no coding required.

### Supported Target Types
1. **Existing Eidolon Chapters**: Chapters from the base Eidolon mod
   - Examples: `wooden_stand`, `tallow`, `crucible`, `soul_gems`
   - Located in: Eidolon's `CodexChapters` class
   
2. **Custom Chapters**: Chapters defined by this mod or other mods
   - Examples: `getting_started` (in `examples` category)
   - Defined in: `data/namespace/codex/chapters/*.json`

### Key Principle
**EXTEND, NEVER CREATE**: When a codex entry specifies a `target_chapter`, it should always add pages to an existing chapter, never create a new chapter with the same name.

## File Structure

### Chapter Definitions
```
data/eidolonunchained/codex/chapters/getting_started.json
```
```json
{
  "title": "eidolonunchained.codex.chapter.getting_started",
  "icon": "minecraft:compass", 
  "category": "examples"
}
```

### Category Definitions  
```
data/eidolonunchained/codex/categories/examples.json
```
```json
{
  "name": "eidolonunchained.codex.category.examples",
  "icon": "minecraft:book",
  "color": "0x8B4513"
}
```

### Codex Entries (Extension Content)
```
data/eidolonunchained/codex_entries/text_example.json
```
```json
{
  "title": "eidolonunchained.codex.entry.text_example.title",
  "target_chapter": "getting_started",  // Extends existing chapter
  "pages": [
    {
      "type": "title", 
      "title": "eidolonunchained.codex.entry.text_example.title",
      "text": "eidolonunchained.codex.entry.text_example.description"
    },
    {
      "type": "text",
      "text": "eidolonunchained.codex.entry.text_example.details" 
    }
  ]
}
```

## Integration Process

### Loading Order
1. **Resource Loading**: Categories and chapters loaded from JSON
2. **Registration**: Custom categories/chapters registered with Eidolon
3. **Entry Processing**: Codex entries processed and added to target chapters
4. **Integration**: Entries grouped by `target_chapter` and added as pages

### Expected Behavior
For 3 entries all targeting `"target_chapter": "getting_started"`:

1. **Lookup**: Find existing `getting_started` chapter in `examples` category
2. **Group**: All 3 entries grouped together by target chapter
3. **Inject**: All 3 entries added as pages to the single existing chapter
4. **Result**: One `getting_started` chapter with pages from all 3 entries

### Current Issue
Instead of extending the existing `getting_started` chapter, the integration creates a new chapter, causing duplicates or confusion.

## Required Implementation

### Missing Method
The integration needs a method to find existing custom chapters:

```java
/**
 * Find an existing custom chapter by ID in any category
 */
private static Chapter findExistingCustomChapter(ResourceLocation chapterId) {
    // Use reflection to access Eidolon's category system
    // Search all categories for a chapter matching chapterId
    // Return the Chapter object if found, null if not found
}
```

### Corrected Integration Logic
```java
// For each unique target_chapter
Chapter existingChapter = null;

// 1. Check Eidolon chapters first
existingChapter = findExistingEidolonChapter(chapterId);

// 2. If not found, check custom chapters  
if (existingChapter == null) {
    existingChapter = findExistingCustomChapter(chapterId);
}

// 3. Extend existing chapter
if (existingChapter != null) {
    for (CodexEntry entry : entries) {
        injectEntryIntoChapter(existingChapter, entry);
    }
    LOGGER.info("Extended existing chapter '{}' with {} entries", chapterId, entries.size());
} else {
    LOGGER.error("Target chapter '{}' not found - check chapter definitions", chapterId);
    // DO NOT create new chapter - this indicates configuration error
}
```

## Data Flow Verification

### Correct Log Sequence
```
[CodexDataManager] Registering custom chapter: eidolonunchained:getting_started (category: examples)
[EidolonCategoryExtension] Created category 'examples' with chapter 'getting_started'
[EidolonCodexIntegration] Found existing custom chapter 'getting_started' in category 'examples'
[EidolonCodexIntegration] Adding entry 'text_example' as pages to existing chapter
[EidolonCodexIntegration] Adding entry 'recipe_example' as pages to existing chapter
[EidolonCodexIntegration] Adding entry 'entity_example' as pages to existing chapter
[EidolonCodexIntegration] Extended existing chapter with 3 entries
```

### Incorrect Log Sequence (Current Issue)
```
[CodexDataManager] Registering custom chapter: eidolonunchained:getting_started (category: examples)
[EidolonCategoryExtension] Created category 'examples' with chapter 'getting_started'
[EidolonCodexIntegration] Created single new chapter 'Getting Started' for 3 codex entries  // ❌ WRONG
[EidolonCategoryExtension] Attached single chapter with 3 entries to category examples
```

## Testing Verification

### In-Game Expected Result
- Open Eidolon codex
- Navigate to `examples` category  
- See one `getting_started` chapter
- Chapter contains pages from all 3 entries:
  - Text Example (title + pages)
  - Recipe Example (title + pages) 
  - Entity Example (title + pages)

### Current Issue Result
- Multiple chapters with same/similar names
- Or only one entry showing (last processed)
- Or entries not appearing in correct chapter

## Debugging Steps

1. **Verify Chapter Registration**: Check logs for custom chapter creation
2. **Verify Entry Loading**: Check logs for entry processing 
3. **Verify Chapter Lookup**: Check if existing chapter is found
4. **Verify Page Injection**: Check if pages are added to correct chapter

## Success Criteria

- ✅ Single `getting_started` chapter in `examples` category
- ✅ Chapter contains pages from all targeting entries
- ✅ Individual entry titles visible within chapter
- ✅ No duplicate chapters created
- ✅ System works for both Eidolon and custom target chapters
