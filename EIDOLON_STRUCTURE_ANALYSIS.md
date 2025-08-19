# 🔍 COMPLETE Eidolon Codex Structure Analysis

## 🏗️ **ACTUAL Eidolon Structure** (Based on Decompiled Classes)

### **Structure Hierarchy**:
```
📚 Categories (Top Level) 
├── 📖 Index Pages (Category Overview)
└── 📄 Chapters (Individual Topics)
    └── 📝 Pages (Content Within Chapters)
```

## 📚 **Categories** (6 Total)

Eidolon has **6 main categories** that organize all content:

### 1. **NATURE** Category
- **Icon**: Nature-themed item
- **Index**: NATURE_INDEX
- **Contains**: Monsters, Critters, Ores, Materials, Plants, etc.

### 2. **RITUALS** Category  
- **Icon**: Ritual-themed item
- **Index**: RITUALS_INDEX
- **Contains**: All ritual chapters (Crystal, Summon, Allure, etc.)

### 3. **ARTIFICE** Category
- **Icon**: Crafting-themed item  
- **Index**: ARTIFICE_INDEX
- **Contains**: Equipment, Tools, Weapons, Armor

### 4. **THEURGY** Category
- **Icon**: Divine/Religious item
- **Index**: THEURGY_INDEX  
- **Contains**: Altars, Prayers, Divine magic

### 5. **SIGNS** Category
- **Icon**: Sign-themed item
- **Index**: SIGNS_INDEX
- **Contains**: All sign chapters (Wicked, Sacred, Blood, etc.)

### 6. **SPELLS** Category
- **Icon**: Spell-themed item
- **Index**: SPELLS_INDEX
- **Contains**: All chant chapters (Light, Fire, Water, etc.)

## 📖 **Chapters by Category** (90+ Total)

Based on the decompiled CodexChapters class:

### **NATURE Category Chapters**:
```java
MONSTERS, CRITTERS, ORES, PEWTER, ENCHANTED_ASH, PLANTS, 
RESEARCHES, DECORATIONS
```

### **RITUALS Category Chapters**:
```java
CRYSTAL_RITUAL, SUMMON_RITUAL, ALLURE_RITUAL, REPELLING_RITUAL, 
DECEIT_RITUAL, TIME_RITUALS, PURIFY_RITUAL, SANGUINE_RITUAL, 
RECHARGE_RITUAL, CAPTURE_RITUAL, LOCATE_RITUAL
```

### **ARTIFICE Category Chapters**:
```java
BRAZIER, ITEM_PROVIDERS, WOODEN_STAND, TALLOW, CRUCIBLE, ARCANE_GOLD, 
REAGENTS, SOUL_GEMS, SHADOW_GEM, WARPED_SPROUTS, BASIC_ALCHEMY, INLAYS, 
BASIC_BAUBLES, MAGIC_WORKBENCH, VOID_AMULET, WARDED_MAIL, SOULFIRE_WAND, 
BONECHILL_WAND, REAPER_SCYTHE, CLEAVING_AXE, SOUL_ENCHANTER, REVERSAL_PICK, 
WARLOCK_ARMOR, GRAVITY_BELT, PRESTIGIOUS_PALM, MIND_SHIELDING_PLATE, 
RESOLUTE_BELT, GLASS_HAND, SOULBONE, RAVEN_CLOAK, ARROW_RING, NECROMANCER_STAFF
```

### **THEURGY Category Chapters**:
```java
INTRO_SIGNS, EFFIGY, ALTARS, ALTAR_LIGHTS, ALTAR_SKULLS, ALTAR_HERBS, 
GOBLET, CENSER, DARK_PRAYER, ANIMAL_SACRIFICE, DARK_TOUCH, STONE_ALTAR, 
UNHOLY_EFFIGY, HOLY_EFFIGY, VILLAGER_SACRIFICE, LIGHT_PRAYER, INCENSE_BURN, 
HEAL, HOLY_TOUCH
```

### **SIGNS Category Chapters**:
```java
WICKED_SIGN, SACRED_SIGN, BLOOD_SIGN, SOUL_SIGN, MIND_SIGN, FLAME_SIGN, 
WINTER_SIGN, HARMONY_SIGN, DEATH_SIGN, WARDING_SIGN, MAGIC_SIGN
```

### **SPELLS Category Chapters**:
```java
MANA, LIGHT, FIRE_TOUCH, CHILL_TOUCH, WATER, ZOMBIFY, CURE_ZOMBIE, 
ENTHRALL, SMITE, SUNDER_ARMOR, REINFORCE_ARMOR
```

## 🔧 **Can We Create New Categories?**

### **Short Answer**: NO - Categories are hardcoded

**Why?**:
```java
public static final List<Category> categories;
public static Category NATURE;
public static Category RITUALS;
public static Category ARTIFICE;
public static Category THEURGY;
public static Category SIGNS;
public static Category SPELLS;
```

Categories are **static final** fields created during class initialization. We cannot add new categories.

### **What We CAN Do**:
✅ **Add entries to existing chapters** (what our system does)
❌ **Create new categories** (hardcoded in Eidolon)
❌ **Create new chapters** (would require reflection hacks)

## 🎯 **Our Current System Status**

### **What Our System Does** ✅:
```java
// We inject entries into existing chapters like this:
Chapter existingChapter = CHAPTER_LOOKUP.get("eidolon:monsters");
existingChapter.addPage(newPage);  // ✅ This works!
```

### **What We CANNOT Do** ❌:
```java
// We CANNOT create new categories:
Category newCategory = new Category(...);  // ❌ Won't appear in GUI

// We CANNOT create new chapters:
Chapter newChapter = new Chapter(...);     // ❌ Won't appear in category
```

## 📊 **Corrected Directory Structure**

Based on the actual categories, our directory should reflect the real structure:

```
src/main/resources/data/eidolonunchained/codex_entries/
├── nature/           # Extends NATURE category chapters
│   ├── monsters.json      → MONSTERS chapter
│   ├── critters.json      → CRITTERS chapter
│   ├── plants.json        → PLANTS chapter
│   ├── ores.json          → ORES chapter
│   └── pewter.json        → PEWTER chapter
├── rituals/          # Extends RITUALS category chapters
│   ├── crystal_ritual.json    → CRYSTAL_RITUAL chapter
│   ├── summon_ritual.json     → SUMMON_RITUAL chapter
│   └── allure_ritual.json     → ALLURE_RITUAL chapter
├── artifice/         # Extends ARTIFICE category chapters
│   ├── void_amulet.json       → VOID_AMULET chapter
│   ├── arcane_gold.json       → ARCANE_GOLD chapter
│   ├── inlays.json            → INLAYS chapter
│   └── basic_baubles.json     → BASIC_BAUBLES chapter
├── theurgy/          # Extends THEURGY category chapters
│   ├── altars.json            → ALTARS chapter
│   ├── dark_prayer.json       → DARK_PRAYER chapter
│   └── effigy.json            → EFFIGY chapter
├── signs/            # Extends SIGNS category chapters
│   ├── wicked_sign.json       → WICKED_SIGN chapter
│   ├── sacred_sign.json       → SACRED_SIGN chapter
│   └── blood_sign.json        → BLOOD_SIGN chapter
└── spells/           # Extends SPELLS category chapters
    ├── light.json             → LIGHT chapter
    ├── fire_touch.json        → FIRE_TOUCH chapter
    └── mana.json              → MANA chapter
```

## 🔍 **Key Findings**:

1. **Categories**: 6 hardcoded categories (cannot add new ones)
2. **Chapters**: 90+ existing chapters (we extend these)
3. **Pages**: Unlimited pages per chapter (this is where we add content)
4. **Structure**: Category → Index → Chapter → Pages
5. **Our Role**: We add **Pages** to existing **Chapters**

## ✅ **Our System is Correct**

Our current approach is **architecturally sound**:
- We extend existing chapters ✅
- We add rich page content ✅  
- We respect the category structure ✅
- We cannot break the core system ✅

The system works exactly as intended - we make existing chapters more content-rich rather than trying to create entirely new organizational structures.
