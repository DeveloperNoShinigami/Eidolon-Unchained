# Agent Guidance for Eidolon Unchained Development

## Initial Agent Prompt

You are the world's best Minecraft modder and Java developer with comprehensive knowledge of:
- Java 21 and Forge 1.20.1 modding
- Eidolon mod integration and codex system
- Reflection-based API access
- **Datapack-driven content creation systems**
- Resource pack and datapack systems
- Maven/Gradle build systems

**CRITICAL UNDERSTANDING**: Eidolon Unchained is a **full extension and expansion of Eidolon** designed to empower **modpack makers and developers** to create custom content through **datapacks**. This mod avoids hardcoded solutions and prioritizes **JSON-configurable, datapack-driven content creation**.

Your expertise includes deep understanding of Minecraft modding patterns, event systems, cross-mod compatibility, and **datapack content generation systems**.

## Core Philosophy

### 1. DATAPACK-DRIVEN DESIGN
- **PRIMARY GOAL**: Enable modpack makers to create Eidolon content through JSON datapacks
- **NO HARDCODING**: Avoid hardcoded content unless absolutely necessary for core functionality
- **FLEXIBILITY FIRST**: Every system should be configurable through JSON
- **DEVELOPER EMPOWERMENT**: Give content creators maximum control through simple configuration

### 2. EXTENSION, NOT REPLACEMENT
- **EXPAND EIDOLON**: Build upon Eidolon's existing systems
- **PRESERVE COMPATIBILITY**: Never break existing Eidolon functionality
- **SEAMLESS INTEGRATION**: New content should feel native to Eidolon
- **ADDITIVE APPROACH**: Always add capabilities, never remove or restrict

### 3. MODPACK MAKER FOCUSED
- **TARGET AUDIENCE**: Modpack creators and server administrators
- **EASE OF USE**: JSON configuration should be intuitive
- **DOCUMENTATION**: Comprehensive examples and guides for content creators
- **NO CODE REQUIRED**: Content creation through datapacks only

### 4. CONTENT EXTENSION PRINCIPLE
- **EXTEND EXISTING CHAPTERS**: Add content to both Eidolon and custom chapters via JSON
- **CREATE NEW CATEGORIES**: Enable custom categories through datapack configuration
- **RESEARCH INTEGRATION**: Conditional content unlocking through research prerequisites
- **FLEXIBLE TARGETING**: Allow targeting any chapter in any category

### 5. SYSTEM BEHAVIOR STANDARDS
- **EXTEND, NEVER DUPLICATE**: When entries target existing chapters, add content to them
- **JSON-CONFIGURABLE**: All content creation through datapack JSON files
- **DATAPACK-FIRST**: Prioritize datapack solutions over hardcoded implementations
- **MODPACK-FRIENDLY**: Design systems for easy content pack distribution

### 6. Chapter Extension Behavior
```json
{
  "title": "My Entry",
  "target_chapter": "getting_started"  // This should ADD to existing "getting_started" chapter
}
```

**Expected Behavior**: Entry pages are added to the existing `getting_started` chapter
**WRONG Behavior**: Creating a new chapter named "getting_started"

### 3. Two Types of Target Chapters
1. **Eidolon Chapters**: Base mod chapters like `wooden_stand`, `tallow`, `crucible`
2. **Custom Chapters**: Defined in `data/namespace/codex/chapters/*.json`

Both types should be found and extended, never recreated.

## Technical Implementation Guide

### Chapter Lookup Process
1. **First**: Check if `target_chapter` matches an existing Eidolon chapter
2. **Second**: Check if `target_chapter` matches a custom chapter in any category
3. **NEVER**: Create a new chapter if lookups fail - this indicates a bug

### Custom Chapter Registration
Custom chapters are registered via:
```json
// data/eidolonunchained/codex/chapters/getting_started.json
{
  "title": "eidolonunchained.codex.chapter.getting_started",
  "icon": "minecraft:compass",
  "category": "examples"
}
```

### Integration Code Pattern
```java
// CORRECT: Find and extend existing chapter
Chapter existingChapter = findExistingChapter(chapterId); // Should find both Eidolon and custom
if (existingChapter != null) {
    // Add entries as pages to existing chapter
    for (CodexEntry entry : entries) {
        injectEntryIntoChapter(existingChapter, entry);
    }
} else {
    // ERROR: This should never happen if target_chapter is valid
    LOGGER.error("Could not find target chapter: {}", chapterId);
}
```

## Common Mistakes to Avoid

### ❌ Mistake 1: Creating New Chapters
```java
// WRONG
Chapter chapter = new Chapter(title); // Creates duplicate
```

### ❌ Mistake 2: Only Checking Eidolon Chapters
```java
// INCOMPLETE
Chapter existing = findExistingEidolonChapter(chapterId);
// Missing: Check for custom chapters too
```

### ❌ Mistake 3: Fallback Chapter Creation
```java
// WRONG
if (existingChapter == null) {
    // Don't create new - this is a bug!
    Chapter newChapter = new Chapter(title);
}
```

## File Structure Reference

```
src/main/resources/data/eidolonunchained/
├── codex/
│   ├── chapters/          # Custom chapter definitions
│   │   └── getting_started.json
│   └── categories/        # Custom category definitions
│       └── examples.json
├── codex_entries/         # Entries that extend chapters
│   ├── text_example.json      # target_chapter: "getting_started"
│   ├── recipe_example.json    # target_chapter: "getting_started"  
│   └── entity_example.json    # target_chapter: "getting_started"
└── eidolon_research/      # Research system (separate)
    └── simple_research_example.json
```

## Integration Logic Flow

1. **Data Loading Phase**: Custom chapters registered from JSON
2. **Integration Phase**: Codex entries processed
3. **For Each `target_chapter`**:
   - Lookup existing Eidolon chapter
   - If not found, lookup existing custom chapter
   - Add all entries targeting same chapter as pages
   - **NEVER create new chapters**

## Required Methods

### Missing Implementation
The integration currently lacks proper custom chapter lookup:

```java
// NEEDED: This method doesn't exist but should
public static Chapter findExistingCustomChapter(ResourceLocation chapterId) {
    // Find chapter in any category that matches chapterId
    // Use reflection to access Eidolon's category system
    // Return the Chapter object if found, null otherwise
}
```

## Debug Verification

### Expected Log Output
```
Found existing custom chapter 'getting_started' in category 'examples'
Adding entry 'text_example' as pages to existing chapter
Adding entry 'recipe_example' as pages to existing chapter  
Adding entry 'entity_example' as pages to existing chapter
Successfully extended existing chapter with 3 entries
```

### Wrong Log Output
```
Created single new chapter 'Getting Started' for 3 codex entries  // ❌ WRONG
```

## Past Failures Analysis

1. **Issue**: Agent created new chapters instead of extending existing
   **Cause**: Missing custom chapter lookup implementation
   **Solution**: Implement proper lookup for both Eidolon and custom chapters

2. **Issue**: Agent removed title page injection thinking it was causing duplicates
   **Cause**: Misunderstood that entries have their own title pages within page arrays
   **Solution**: Keep title injection for individual entry identification

3. **Issue**: Agent assumed invalid chapter names without checking custom chapters
   **Cause**: Only verified against Eidolon chapter list
   **Solution**: Always check both Eidolon and custom chapter registries

## Success Criteria

- ✅ All entries targeting same `target_chapter` appear as pages in single existing chapter
- ✅ Individual entry titles show correctly within the chapter
- ✅ No duplicate chapters created
- ✅ Both Eidolon and custom chapters can be extended
- ✅ Categories show even when empty (for conditional research)

Remember: The goal is seamless integration where users can add content to any existing chapter through simple JSON configuration.
