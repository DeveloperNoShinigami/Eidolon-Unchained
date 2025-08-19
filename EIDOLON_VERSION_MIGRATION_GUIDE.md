# Eidolon Version Migration Guide

## 🎯 Current State vs Future Versions

This document outlines what features are missing in our current Eidolon version (0.3.8+) and what newer versions will provide, allowing us to migrate from reflection to clean imports.

## 📋 Current Version (0.3.8+) Limitations

### ❌ **Missing Features We Need:**

#### 1. **CodexEvents System** (Most Important!)
```java
// ❌ NOT AVAILABLE in 0.3.8+
import elucent.eidolon.codex.CodexEvents;

@SubscribeEvent
public static void onCodexPreInit(CodexEvents.PreInit event) {
    // Direct access to categories list
    event.categories.add(newCategory);
    event.itemToEntryMap.put(item, entry);
}
```

#### 2. **Enhanced TitlePage Constructor**
```java
// ❌ CURRENT: Only accepts String
new TitlePage(title)

// ✅ FUTURE: Will accept String + ItemStack
new TitlePage(title, iconStack)
```

#### 3. **Public Category Fields**
```java
// ❌ CURRENT: Private/package-private fields
category.key // Cannot access

// ✅ FUTURE: Public accessors or fields
category.getKey() // or category.key (public)
```

#### 4. **Enhanced Chapter Constructor**
```java
// ❌ CURRENT: Limited constructor options
new Chapter(titleKey)

// ✅ FUTURE: Enhanced constructors with more options
new Chapter(titleKey, titlePage, icon)
```

## 🔄 Migration Roadmap

### **Phase 1: Immediate (Current 0.3.8+)**
- ✅ Use reflection where necessary (EidolonCodexIntegration.java)
- ✅ Focus on content injection to existing chapters
- ✅ Prepare JSON datapack system (disabled until events available)
- ✅ Document all compatibility issues

### **Phase 2: When CodexEvents Become Available**
- 🔄 Enable EidolonCategoryExtension.java event handlers
- 🔄 Activate DatapackCategoryExample.java full functionality
- 🔄 Remove reflection from category creation
- 🔄 Switch to event-driven approach

### **Phase 3: Full Modern API Support**
- 🔄 Remove all remaining reflection usage
- 🔄 Use enhanced constructors and public fields
- 🔄 Implement advanced category features

## 📁 File-by-File Migration Plan

### **EidolonCategoryExtension.java**
```java
// CURRENT STATE (Compatibility Mode)
@SubscribeEvent
public static void onFMLClientSetupEvent(FMLClientSetupEvent event) {
    LOGGER.info("CodexEvents not available - compatibility mode");
}

// FUTURE MIGRATION (When CodexEvents Available)
@SubscribeEvent
public static void onCodexPreInit(CodexEvents.PreInit event) {
    // Uncomment and activate:
    DatapackCategoryExample.addDatapackCategories(event.categories, event.itemToEntryMap);
}

@SubscribeEvent  
public static void onCodexPostInit(CodexEvents.PostInit event) {
    // Add chapters to existing categories
    addChaptersToExistingCategories(event.categories, event.itemToEntryMap);
}
```

### **DatapackCategoryExample.java**
```java
// CURRENT STATE (All methods commented out)
/* TEMPORARILY COMMENTED OUT - Restore when CodexEvents are available
 * All functionality disabled for compatibility
 */

// FUTURE MIGRATION (Uncomment everything when CodexEvents available)
public static void addDatapackCategories(List<Category> categories, 
                                        Map<Item, IndexPage.IndexEntry> itemToEntryMap) {
    // Uncomment all datapack loading logic
    // Full JSON-driven category creation
}
```

### **CustomCategoryBuilder.java**
```java
// CURRENT STATE (Working but limited)
public Category build(Map<Item, IndexPage.IndexEntry> itemToEntryMap) {
    // Basic category creation with current API
}

// FUTURE MIGRATION (Enhanced when better constructors available)
public Category build(Map<Item, IndexPage.IndexEntry> itemToEntryMap) {
    // Use enhanced Category constructors
    // Better icon and metadata support
}
```

### **EidolonCodexIntegration.java**
```java
// CURRENT STATE (Reflection required)
// ⚠️ REFLECTION USAGE - Only used where absolutely necessary
for (Field field : CodexChapters.class.getDeclaredFields()) {
    Chapter chapter = (Chapter) field.get(null);
}

// FUTURE MIGRATION (Reduce reflection)
// When public getters become available:
// - Replace field access with public methods
// - Keep only essential reflection usage
```

## 🔍 Detection Strategy for New Features

### **How to Check if CodexEvents Are Available:**
```java
try {
    Class.forName("elucent.eidolon.codex.CodexEvents");
    LOGGER.info("✅ CodexEvents available - enabling full functionality");
    return true;
} catch (ClassNotFoundException e) {
    LOGGER.info("ℹ️ CodexEvents not available - using compatibility mode");
    return false;
}
```

### **Version-Aware Initialization:**
```java
@SubscribeEvent
public static void onFMLClientSetupEvent(FMLClientSetupEvent event) {
    if (hasCodexEvents()) {
        LOGGER.info("🚀 Modern Eidolon version detected - using event system");
        // Enable event-based initialization
    } else {
        LOGGER.info("🔧 Legacy Eidolon version detected - using compatibility mode");  
        // Use current reflection-based approach
    }
}
```

## 📋 Compatibility Checklist

### **Before Updating Eidolon Version:**
- [ ] Check if `elucent.eidolon.codex.CodexEvents` class exists
- [ ] Test `TitlePage` constructor with 2 parameters
- [ ] Verify `Category` field accessibility
- [ ] Test enhanced `Chapter` constructors
- [ ] Check if `IndexPage.IndexEntry` constructor signature changed

### **Migration Steps:**
1. **Update Eidolon dependency** in build.gradle
2. **Run compatibility detection** in main mod class
3. **Uncomment event handlers** in EidolonCategoryExtension.java
4. **Restore DatapackCategoryExample** functionality
5. **Test all category creation** and chapter injection
6. **Remove deprecated reflection usage** where possible

## 🎨 Expected New API Usage

### **Full Category Creation (Future):**
```java
// Clean event-driven approach - no reflection!
@SubscribeEvent
public static void onCodexPreInit(CodexEvents.PreInit event) {
    // Direct access to internal structures
    List<Category> categories = event.categories;
    Map<Item, IndexPage.IndexEntry> itemToEntryMap = event.itemToEntryMap;
    
    // Create custom categories with full JSON support
    Category customCategory = new CustomCategoryBuilder("modded")
        .icon(new ItemStack(Items.COMMAND_BLOCK))
        .color(0xFF9900)
        .loadFromJson("data/eidolonunchained/codex/custom_spells/")
        .build();
    
    // Direct addition to Eidolon's system
    categories.add(customCategory);
    
    // Automatic item mapping registration
    customCategory.getEntries().forEach(entry -> 
        itemToEntryMap.put(entry.getIcon().getItem(), entry)
    );
}
```

### **Enhanced Chapter Injection:**
```java
@SubscribeEvent
public static void onCodexPostInit(CodexEvents.PostInit event) {
    // Access existing categories without reflection
    for (Category category : event.categories) {
        if ("nature".equals(category.getKey())) {
            Chapter customChapter = loadChapterFromJson("nature_additions/");
            category.addChapter(customChapter);
        }
    }
}
```

## 🚀 Benefits After Migration

### **Performance:**
- ❌ No more reflection overhead
- ✅ Direct method calls
- ✅ Compile-time optimization

### **Maintainability:**
- ❌ No more brittle field access
- ✅ Type-safe API usage
- ✅ IDE support and refactoring

### **Features:**
- ❌ No more compatibility limitations
- ✅ Full category customization
- ✅ Advanced datapack support

### **Reliability:**
- ❌ No more version-specific field names
- ✅ Stable public API usage
- ✅ Forward compatibility

## 📝 Action Items

### **Immediate (Current Version):**
1. ✅ Keep reflection usage minimal and documented
2. ✅ Prepare all event-based code (commented out)
3. ✅ Test existing content injection system
4. ✅ Document all compatibility workarounds

### **On Eidolon Update:**
1. 🔄 Run feature detection tests
2. 🔄 Uncomment event-based code
3. 🔄 Test category creation system
4. 🔄 Migrate away from reflection
5. 🔄 Enable full JSON datapack support

This migration guide ensures we can seamlessly transition from our current reflection-heavy approach to a clean, modern, imports-based system as soon as Eidolon provides the necessary APIs! 🎉
