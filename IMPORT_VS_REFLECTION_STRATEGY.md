# Import vs Reflection Strategy

## 🎯 Philosophy: Imports First, Reflection Only When Necessary

Our project prioritizes **clean imports and direct access** over reflection for better code maintainability, performance, and compatibility.

## ✅ What We Use Instead of Reflection

### 1. **Event System Integration** (PREFERRED)
```java
// ✅ GOOD: Using Eidolon's event system
@SubscribeEvent
public static void onCodexPreInit(elucent.eidolon.codex.CodexEvents.PreInit event) {
    // Direct access to categories list - no reflection needed!
    event.categories.add(newCategory);
    event.itemToEntryMap.put(item, entry);
}
```

### 2. **Direct Imports** (PREFERRED)
```java
// ✅ GOOD: Direct imports
import elucent.eidolon.codex.*;
import net.minecraft.world.item.ItemStack;

// Direct object creation - no reflection
Category category = new Category("key", icon, color, index);
```

### 3. **Builder Pattern** (CLEAN)
```java
// ✅ GOOD: Clean builder pattern for complex objects
Category category = new CustomCategoryBuilder("modded")
    .icon(new ItemStack(Items.COMMAND_BLOCK))
    .color(0xFF9900)
    .addChapter("Chapter Title", new ItemStack(Items.BOOK))
    .build(itemToEntryMap);
```

## ⚠️ When We Use Reflection (Minimized)

### Only in 2 Specific Cases:

#### 1. **Legacy Chapter Lookup** (EidolonCodexIntegration.java)
```java
// ⚠️ REFLECTION USAGE - Only used here because we need access to existing static fields
// This is necessary to access CodexChapters' existing static Chapter fields
for (Field field : CodexChapters.class.getDeclaredFields()) {
    Chapter chapter = (Chapter) field.get(null);
    CHAPTER_LOOKUP.put(new ResourceLocation("eidolon", name), chapter);
}
```
**Why Necessary**: Need to access existing static fields from Eidolon for compatibility.

#### 2. **Research System Integration** (EidolonResearchIntegration.java) 
```java
// ⚠️ REFLECTION USAGE - Only used where Eidolon's research API requires it
// Research system currently requires reflection due to API limitations
Method registerMethod = Researches.class.getDeclaredMethod("register", ...);
```
**Why Necessary**: Eidolon's research API doesn't provide direct registration methods.

## 📁 File Breakdown

### **PRIMARY APPROACH: Event-Driven + JSON Datapacks**

#### `EidolonCategoryExtension.java` ✅ **NO REFLECTION**
- Uses Eidolon's `CodexEvents.PreInit/PostInit` 
- Direct access to categories list via event parameters
- Clean, maintainable, future-proof

#### `DatapackCategoryExample.java` ✅ **NO REFLECTION**
- JSON-driven category creation
- Direct imports and object instantiation  
- Fully datapack-configurable

#### `CustomCategoryBuilder.java` ✅ **NO REFLECTION**
- Builder pattern for easy category creation
- Direct API usage throughout

### **LEGACY COMPATIBILITY: Minimal Reflection**

#### `EidolonCodexIntegration.java` ⚠️ **REFLECTION ONLY WHERE NEEDED**
- Reflection marked and explained
- Used only for accessing existing static fields
- Kept for backward compatibility with existing content

#### `EidolonResearchIntegration.java` ⚠️ **REFLECTION ONLY WHERE NEEDED** 
- Reflection clearly marked and justified
- Used only where Eidolon API requires it
- Minimal scope and usage

## 🚀 Benefits of Our Approach

### **Performance**
- Direct method calls vs reflection overhead
- Compile-time optimization opportunities
- No runtime field/method lookup

### **Maintainability** 
- Clear dependency relationships
- IDE support (autocomplete, refactoring, etc.)
- Easier debugging and stack traces

### **Compatibility**
- Less likely to break with Eidolon updates
- Uses intended extension points
- Type-safe interactions

### **Clarity**
- Code intent is obvious
- New developers can understand easily
- Clear separation of concerns

## 🎮 Current Strategy Summary

### **For NEW Features:**
1. **Use Events** - `CodexEvents.PreInit/PostInit` for categories
2. **Use JSON Datapacks** - All content driven by JSON files
3. **Use Direct Imports** - Standard Java object instantiation
4. **Use Builder Pattern** - For complex object creation

### **For LEGACY Compatibility:**
1. **Keep existing reflection** - Only where absolutely necessary
2. **Document WHY** - Clear comments explaining necessity  
3. **Minimize scope** - Smallest possible reflection footprint
4. **Plan migration** - Eventually move to event-driven approach

## 🔧 Implementation Examples

### **Creating New Categories (Recommended)**
```java
// JSON datapack approach - fully configurable
DatapackCategoryExample.addDatapackCategories(categories, itemToEntryMap);

// Builder approach - clean code
Category custom = new CustomCategoryBuilder("expansions")
    .icon(new ItemStack(Items.END_CRYSTAL))
    .color(0x9966FF)
    .addChapter("Custom Spells", new ItemStack(Items.BOOK))
    .build(itemToEntryMap);
```

### **Adding to Existing Categories (Event-Driven)**
```java
// Direct access via events - no reflection
for (Category category : event.categories) {
    if ("nature".equals(category.key)) {
        // Add content directly using available APIs
    }
}
```

This approach gives us the best of both worlds: clean, maintainable code for new features while preserving compatibility where needed! 🎉
