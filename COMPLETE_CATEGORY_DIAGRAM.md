# 🎯 Complete Category System Diagram

## 📁 Your Directory Structure → 📖 In-Game Categories

```
🗂️ DATAPACK FILES                          🎮 IN-GAME RESULT

src/main/resources/data/eidolonunchained/codex/
├── community_rituals/                      ├── 🔮 Community Rituals
│   ├── community_summoning.json    ────────┤   ├── 🔔 Community Summoning
│   └── ritual_binding.json         ────────┤   └── ⛓️ Ritual Binding
│                                           │
├── custom_spells/                          ├── 🎆 Custom Spells  
│   ├── fire_mastery.json           ────────┤   ├── 🔥 Fire Mastery
│   └── ice_control.json            ────────┤   └── 🧊 Ice Control
│                                           │
└── expansions/                             └── 🚀 Expansions
    └── expansion_pack.json         ─────────    └── 💎 Expansion Content Pack
```

## 🎨 Your `community_summoning.json` In Action

### JSON Structure:
```json
{
  "title_key": "eidolonunchained.codex.community_rituals.summoning.title",
  "title": "Community Summoning",
  "icon": "minecraft:bell",
  "pages": [
    {"type": "text", "content": "When multiple practitioners..."},
    {"type": "text", "content": "The bell serves as both..."},  
    {"type": "ritual", "content": "Greater Summoning Circle", "data": {...}},
    {"type": "text", "content": "Warning: Failed community rituals..."}
  ]
}
```

### Becomes This In-Game Experience:

```
🎮 PLAYER EXPERIENCE FLOW:

1️⃣ Opens Eidolon Codex
   ┌─────────────────────────────────┐
   │       📚 EIDOLON CODEX          │
   ├─────────────────────────────────┤
   │ 🔥 Basics                       │
   │ 🌿 Nature                       │  
   │ ⚔️ Artifice                     │
   │ 💀 Soul                         │
   │ 🌌 Signs                        │
   │ 🔮 Community Rituals    ← NEW!  │
   │ 🎆 Custom Spells        ← NEW!  │
   │ 🚀 Expansions           ← NEW!  │
   └─────────────────────────────────┘

2️⃣ Clicks "🔮 Community Rituals"
   ┌─────────────────────────────────┐
   │     🔮 COMMUNITY RITUALS        │
   ├─────────────────────────────────┤
   │                                 │
   │ 🔔 Community Summoning          │
   │ ⛓️ Ritual Binding               │
   │                                 │
   │ Select a chapter to learn more  │
   └─────────────────────────────────┘

3️⃣ Clicks "🔔 Community Summoning" 
   ┌─────────────────────────────────┐
   │     🔔 COMMUNITY SUMMONING      │
   │              (Page 1/4)         │
   ├─────────────────────────────────┤
   │                                 │
   │ When multiple practitioners     │
   │ combine their power, they can   │
   │ achieve feats impossible for a  │
   │ single mage. Community          │
   │ summoning requires perfect      │
   │ synchronization between         │
   │ participants.                   │
   │                                 │
   │        [< PREV]  [NEXT >]       │
   └─────────────────────────────────┘

4️⃣ Page 3 - Ritual Display
   ┌─────────────────────────────────┐
   │   ⚡ GREATER SUMMONING CIRCLE   │
   │              (Page 3/4)         │
   ├─────────────────────────────────┤
   │                                 │
   │ 🔵 Circle Size: 5 blocks        │
   │ 👥 Participants: 3              │
   │                                 │
   │ 📦 Required Components:         │
   │   🔔 Bell × 1                   │
   │   🟫 Soul Sand × 32             │
   │   💀 Wither Skull × 3           │
   │   ⭐ Nether Star × 1            │
   │                                 │
   │ 📝 Summons a powerful ally      │
   │                                 │
   │        [< PREV]  [NEXT >]       │
   └─────────────────────────────────┘
```

## 🔧 Technical Flow

```
📄 JSON File
     ↓
🔄 CodexDataManager.loadEntriesFromDirectory()
     ↓  
🏗️ DatapackCategoryExample.createCategoryFromDatapack()
     ↓
📖 EidolonPageConverter.convertPage()
     ↓
🎮 Live Eidolon Chapter in Codex
```

## 🎯 Key Features Your JSON Enables

### ✅ What Players Get:
- **New Category**: "Community Rituals" appears in main codex menu
- **Custom Icon**: Bell icon (🔔) identifies the chapter
- **Multiple Pages**: 4 pages of content with different types
- **Ritual Display**: Special ritual page with components and requirements
- **Navigation**: Full page flipping with arrows
- **Translations**: Proper localization support

### ✅ What You Control:
- **Content**: All text, descriptions, and instructions
- **Structure**: Page order and organization  
- **Requirements**: Ritual components and quantities
- **Styling**: Icons and visual elements
- **Expansion**: Easy to add more chapters

This system gives you **complete control** over custom codex content while seamlessly integrating with Eidolon's native UI and functionality! 🚀
