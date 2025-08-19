# 🔄 Eidolon Update Migration Checklist

## Pre-Update Preparation ✅
- [x] Documented all current limitations
- [x] Created migration guide 
- [x] Added version detection system
- [x] Prepared all event-based code (commented out)
- [x] Clearly marked all reflection usage

## When Updating Eidolon Version:

### 1. **Update Dependencies** 
- [ ] Update `eidolon_version` in `gradle.properties`
- [ ] Update CurseForge dependency in `build.gradle` if needed
- [ ] Run `./gradlew build` to test compilation

### 2. **Run Feature Detection**
- [ ] Launch the mod and check logs for feature detection results
- [ ] Look for: `🔍 Eidolon Feature Detection Results:`
- [ ] Check if mode shows `Modern - Event System Available`

### 3. **Migration Steps (If CodexEvents Available)**

#### In `EidolonCategoryExtension.java`:
- [ ] Remove the basic `onFMLClientSetupEvent` method
- [ ] Uncomment the `onCodexPreInit` and `onCodexPostInit` methods
- [ ] Change `@Mod.EventBusSubscriber` bus from `MOD` to `FORGE`
- [ ] Test category creation

#### In `DatapackCategoryExample.java`:
- [ ] Uncomment entire method body in `addDatapackCategories()`
- [ ] Uncomment `createCategoryFromDatapack()` method
- [ ] Uncomment `convertJsonToChapter()` method
- [ ] Test JSON datapack loading

#### In `CustomCategoryBuilder.java`:
- [ ] Test if enhanced constructors are available
- [ ] Update to use better TitlePage constructor if available
- [ ] Test category building

### 4. **Remove Reflection (Gradual)**

#### Check `EidolonCodexIntegration.java`:
- [ ] Test if Category fields are now public
- [ ] Replace field access with getters if available
- [ ] Keep minimal reflection only where necessary

### 5. **Test Everything**
- [ ] Test existing chapter injection (should still work)
- [ ] Test new category creation (should now work)
- [ ] Test JSON datapack loading (should now work)
- [ ] Test in-game codex GUI (should show new categories)

### 6. **Clean Up**
- [ ] Remove compatibility warnings from logs
- [ ] Update documentation to reflect new capabilities
- [ ] Remove deprecated reflection usage
- [ ] Update version migration guide

## Quick Test Commands

```bash
# Build and test
./gradlew build

# Run client to test
./gradlew runClient

# Check logs for:
# "✅ CodexEvents detected - modern Eidolon version available"
# "🚀 Running in Modern Mode - using event system and direct imports"
```

## Expected Log Messages After Migration

```
[INFO] 🔍 Eidolon Feature Detection Results:
[INFO]    Mode: Modern - Event System Available  
[INFO]    CodexEvents: ✅ Available
[INFO]    Enhanced TitlePage: ✅ Available
[INFO]    Public Category Fields: ✅ Available
[INFO] 🚀 Running in Modern Mode - using event system and direct imports

[INFO] 🎯 CodexEvents.PreInit - Adding custom categories from JSON datapacks!
[INFO] ✅ Successfully added datapack-driven categories!

[INFO] 🎯 CodexEvents.PostInit - Adding chapters to existing categories!
[INFO] ✅ Successfully added custom chapters!
```

## Rollback Plan (If Issues)

If migration fails:
1. Revert Eidolon version in `gradle.properties`
2. Re-comment the event-based code  
3. Re-enable basic compatibility mode
4. Check migration guide for missing steps

## Files to Monitor During Migration

- `EidolonCategoryExtension.java` - Main category system
- `DatapackCategoryExample.java` - JSON datapack integration  
- `EidolonCodexIntegration.java` - Reflection usage
- `CustomCategoryBuilder.java` - Category construction
- `EidolonVersionDetection.java` - Feature detection

The goal is to go from **"Legacy - Reflection Required"** to **"Modern - Event System Available"** mode! 🎉
