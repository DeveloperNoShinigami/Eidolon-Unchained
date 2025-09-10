# AI Item Extraction System Issues

## Critical Problems Identified

### Issue #1: Prayer Type Matching Completely Broken
**What Should Happen:**
- Custom chants should use their configured `prayer_effect_type` field
- Each prayer type has different blessing limits and behaviors
- System should match the chant's prayer type to execute appropriate logic

**What's Actually Happening:**
```
? Available prayer types for deity eidolonunchained:dark_deity: [guidance, curse, conversation, blessing]
? No specific prayer type match - defaulting to 'conversation'
```

**Problem:** The system finds available prayer types but fails to match the performed chant to its configured prayer type, defaulting to "conversation" instead of using the chant's actual prayer_effect_type.

### Issue #2: Multi-Word Item Scoring System Completely Non-Functional
**What Should Happen (and used to work in earlier commits):**
1. Player requests: `"bone paladin helm"`
2. System breaks into words: `["bone", "paladin", "helm"]`
3. Searches each mod's item registry for partial matches
4. Scores items based on word matches:
   - `eidolon:bonelord_helmet` = 2/3 words match ("bone" + "helm") = HIGH score
   - `minecraft:bone` = 1/3 words match ("bone") = LOW score
5. Returns best scoring item: `eidolon:bonelord_helmet`

**What's Actually Happening (completely broken):**
```
? Enhanced search for 'bone paladin helm' found 0 qualifying items in 2 mods
? No items met the strict matching criteria for 'bone paladin helm'
```

Then falls back to random registry scanning and returns completely wrong items:
- `minecraft:lime_banner` ❌
- `minecraft:amethyst_shard` ❌  
- `minecraft:name_tag` ❌
- `minecraft:bone` ❌ (only matches 1/3 words)

**Critical Evidence:**
The system actually FOUND relevant items but completely ignored them:
```
? Found 12 matching items for 'bone' in mods [minecraft, eidolon, eidolonunchained]: 
[minecraft:bone, eidolon:bone_pile, eidolon:bone_pile_slab, eidolon:bone_pile_stairs, eidolon:bonechill_wand]
```

Notice it found `eidolon:bonechill_wand` (contains "bone") but completely missed the most relevant item that should exist: `eidolon:bonelord_helmet` or similar.

### Issue #3: Word Breakdown Algorithm Failure
**Root Cause:** The scoring system is treating `"bone paladin helm"` as a single exact string match instead of:
1. Splitting into individual words
2. Scoring each item based on how many words it contains
3. Prioritizing items with higher word match ratios

**Evidence from logs:**
- System correctly identifies the request: `'bone paladin helm'`
- Fails at multi-word search: `found 0 qualifying items`
- Falls back to irrelevant single-word matches

### Issue #4: Contextual Mod Prioritization Broken
**Expected Behavior:**
- Dark deity conversations should prioritize Eidolon mod items
- Items matching the deity's theme should score higher
- Multi-word matches should always beat single-word matches

**Actual Behavior:**
- Returns random Minecraft items instead of thematic Eidolon items
- No contextual scoring based on deity type
- Single-word fallbacks take priority over better partial matches

## System Status
The AI item extraction system that previously worked correctly in earlier commits has been severely degraded. The multi-word scoring algorithm that could intelligently match partial word combinations (like "bone paladin helm" → "bonelord_helmet") is no longer functional.

## Required Fixes
1. **Prayer Type Resolution:** Fix chant → prayer_effect_type matching
2. **Multi-Word Breakdown:** Restore word-by-word scoring algorithm  
3. **Scoring Logic:** Implement proper partial match scoring (2/3 words > 1/3 words)
4. **Contextual Prioritization:** Prioritize mod-appropriate items based on deity theme
5. **Fallback Logic:** Ensure fallbacks don't override better matches

## Test Case for Verification
**Input:** `"bone paladin helm"` request to dark deity
**Expected Output:** `eidolon:bonelord_helmet` or similar 2+ word match from Eidolon mod
**Current Broken Output:** Random items like `minecraft:lime_banner`, `minecraft:amethyst_shard`

This system regression represents a critical failure in the AI-driven blessing system that directly impacts the user experience of deity conversations.