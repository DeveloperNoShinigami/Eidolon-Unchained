# 📖 Complete Page Types Guide for Eidolon Unchained

## 🎯 **All Supported Page Types**

### **📝 Basic Content Pages**

#### **Title Page**
```json
{
  "type": "title",
  "title": "Your Main Title",
  "text": "Subtitle or description text"
}
```
Creates a large title page with prominent heading.

#### **Text Page**
```json
{
  "type": "text", 
  "title": "Chapter Title",
  "text": "Your detailed explanation goes here. This can be multiple sentences describing the topic in depth."
}
```
Standard text page with title and body content.

---

### **🔧 Recipe & Crafting Pages**

#### **Crafting Table Recipe**
```json
{
  "type": "crafting",
  "recipe": "eidolonunchained:void_crystal",
  "title": "Void Crystal Creation",
  "text": "A crystallized fragment of pure void energy."
}
```

#### **Furnace Smelting Recipe**
```json
{
  "type": "smelting",
  "recipe": "eidolonunchained:purified_metal",
  "title": "Metal Purification",
  "text": "Remove impurities through magical smelting."
}
```

#### **Crucible Recipe**
```json
{
  "type": "crucible",
  "recipe": "eidolonunchained:soul_alloy",
  "title": "Soul Alloy Creation",
  "text": "Combine metals with soul essence in the crucible."
}
```

#### **Worktable Recipe**
```json
{
  "type": "worktable",
  "recipe": "eidolonunchained:enchanted_tool",
  "title": "Enchanted Tool Crafting",
  "text": "Create tools imbued with magical properties."
}
```

---

### **🔮 Advanced Magical Pages**

#### **Ritual Page**
```json
{
  "type": "ritual",
  "ritual": "eidolon:crystal_ritual",
  "title": "Crystal Ritual Setup",
  "text": "Shows the complete ritual circle and requirements."
}
```
Displays ritual layout, ingredients, and setup instructions.

#### **Entity/Mob Page**
```json
{
  "type": "entity",
  "entity": "minecraft:zombie",
  "title": "Undead Analysis", 
  "text": "Detailed study of zombie behavior and properties."
}
```
Shows a 3D model of the creature with information.

#### **List Page** *(Items/Collections)*
```json
{
  "type": "list",
  "list_key": "magical_herbs",
  "title": "Magical Plant Collection",
  "text": "A comprehensive list of magically significant plants."
}
```
Creates organized lists of related items.

---

### **🎵 Specialized Content Pages**

#### **Chant/Spell Page**
```json
{
  "type": "chant",
  "chant": "ancient_incantation",
  "title": "Ancient Words of Power",
  "text": "Sacred words that channel magical energy."
}
```

#### **Sign Page**
```json
{
  "type": "sign", 
  "sign": "protection_ward",
  "title": "Protective Symbols",
  "text": "Symbols that provide magical protection."
}
```

#### **Rune Description Page**
```json
{
  "type": "rune",
  "rune": "power_rune",
  "title": "Rune of Power",
  "text": "Ancient symbols that focus magical energy."
}
```

---

## 🎨 **Example Multi-Page Entry**

```json
{
  "target_chapter": "VOID_AMULET",
  "pages": [
    {
      "type": "title",
      "title": "Void Mastery Compendium",
      "text": "Complete guide to void manipulation"
    },
    {
      "type": "text",
      "title": "Understanding the Void",
      "text": "The void represents the space between worlds, a realm of infinite possibility and terrifying emptiness. Those who study it risk madness, but gain incredible power."
    },
    {
      "type": "entity",
      "entity": "eidolon:void_touched",
      "title": "Void-Touched Entities",
      "text": "Creatures that have been exposed to void energy exhibit strange properties."
    },
    {
      "type": "crafting",
      "recipe": "eidolonunchained:enhanced_void_amulet",
      "title": "Enhanced Void Amulet",
      "text": "An improved version with greater void channeling capacity."
    },
    {
      "type": "ritual",
      "ritual": "eidolonunchained:void_communion",
      "title": "Void Communion Ritual",
      "text": "Directly interface with void energies through this dangerous ritual."
    }
  ]
}
```

## 📋 **Available Eidolon Chapters to Extend**

### **🏺 Items & Artifacts**
- `"VOID_AMULET"` - Void manipulation items
- `"PEWTER"` - Pewter alloys and tools
- `"ENCHANTED_ASH"` - Magical ash applications
- `"DECORATIONS"` - Decorative magical items

### **🔮 Rituals & Magic**
- `"CRYSTAL_RITUAL"` - Crystal-based magic
- `"SUMMON_RITUAL"` - Creature summoning
- `"ALLURE_RITUAL"` - Charm and attraction spells
- `"REPELLING_RITUAL"` - Protection and warding
- `"DECEIT_RITUAL"` - Illusion magic
- `"TIME_RITUALS"` - Time manipulation
- `"PURIFY_RITUAL"` - Cleansing and purification
- `"SANGUINE_RITUAL"` - Blood magic
- `"RECHARGE_RITUAL"` - Energy restoration
- `"CAPTURE_RITUAL"` - Soul/essence binding

### **🌿 Natural Elements**
- `"MONSTERS"` - Creature studies
- `"CRITTERS"` - Small magical creatures
- `"ORES"` - Magical mining and geology
- `"PLANTS"` - Magical botany

### **⚙️ Mechanics & Tools**
- `"RESEARCHES"` - Research system information
- `"BRAZIER"` - Magical braziers and lighting
- `"ITEM_PROVIDERS"` - Item generation mechanics

## 🚀 **Tips for Content Creation**

1. **Mix Page Types**: Use different page types in sequence for engaging content
2. **Entity Pages**: Great for explaining monster behavior and properties
3. **Ritual Pages**: Perfect for showing complex magical procedures
4. **Progressive Complexity**: Start with simple text, build to complex rituals
5. **Cross-Reference**: Reference other chapters and recipes in your text

Your expanded system now supports **all major Eidolon page types** for comprehensive content creation! 🔮✨
