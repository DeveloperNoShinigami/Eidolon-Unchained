# 📖 Community Rituals Category - Visual Layout Example

Based on your `community_summoning.json` file, here's what players would see in the Eidolon codex:

## 🎯 Category Structure

```
📚 EIDOLON CODEX
├── 🔥 Basics
├── 🌿 Nature  
├── ⚔️ Artifice
├── 💀 Soul
├── 🌌 Signs
├── 🔮 **Community Rituals** ← NEW CUSTOM CATEGORY
│   ├── 🔔 Community Summoning    ← Your JSON file
│   └── ⛓️ Ritual Binding         ← Another example
└── 🎆 Custom Spells              ← Another custom category
    ├── 🔥 Fire Mastery
    └── 🧊 Ice Control
```

## 📋 Community Summoning Chapter Layout

When players click on **🔔 Community Summoning**, they'd see:

```
╔══════════════════════════════════════════════════════════════════╗
║                        COMMUNITY SUMMONING                       ║
║                            (Page 1/4)                           ║
╠══════════════════════════════════════════════════════════════════╣
║                                                                  ║
║  When multiple practitioners combine their power, they can       ║
║  achieve feats impossible for a single mage. Community          ║
║  summoning requires perfect synchronization between              ║
║  participants.                                                   ║
║                                                                  ║
║                                                                  ║
║                        [< PREV]  [NEXT >]                       ║
╚══════════════════════════════════════════════════════════════════╝
```

**Page 2:**
```
╔══════════════════════════════════════════════════════════════════╗
║                        COMMUNITY SUMMONING                       ║
║                            (Page 2/4)                           ║
╠══════════════════════════════════════════════════════════════════╣
║                                                                  ║
║  The bell serves as both a focus and a timing device,           ║
║  ensuring all participants channel their energy at precisely    ║
║  the right moment.                                              ║
║                                                                  ║
║                                                                  ║
║                        [< PREV]  [NEXT >]                       ║
╚══════════════════════════════════════════════════════════════════╝
```

**Page 3 - Ritual Page:**
```
╔══════════════════════════════════════════════════════════════════╗
║                   GREATER SUMMONING CIRCLE                      ║
║                            (Page 3/4)                           ║
╠══════════════════════════════════════════════════════════════════╣
║                                                                  ║
║  Circle Size: 5 blocks                                          ║
║  Participants Required: 3                                       ║
║                                                                  ║
║  Required Components:                                            ║
║  🔔 Bell × 1                                                     ║
║  🟫 Soul Sand × 32                                              ║
║  💀 Wither Skeleton Skull × 3                                   ║
║  ⭐ Nether Star × 1                                             ║
║                                                                  ║
║  Description: Summons a powerful ally to aid the community      ║
║                                                                  ║
║                        [< PREV]  [NEXT >]                       ║
╚══════════════════════════════════════════════════════════════════╝
```

**Page 4:**
```
╔══════════════════════════════════════════════════════════════════╗
║                        COMMUNITY SUMMONING                       ║
║                            (Page 4/4)                           ║
╠══════════════════════════════════════════════════════════════════╣
║                                                                  ║
║  ⚠️ WARNING: Failed community rituals can have catastrophic     ║
║  consequences. Ensure all participants are experienced and      ║
║  prepared.                                                      ║
║                                                                  ║
║                                                                  ║
║                        [< PREV]  [NEXT >]                       ║
╚══════════════════════════════════════════════════════════════════╝
```

## 🎮 In-Game Flow

1. **Player opens Eidolon Codex** → Sees new "Community Rituals" category with bell icon
2. **Clicks Community Rituals** → Shows category page with 2 chapters
3. **Clicks "Community Summoning"** → Opens your 4-page chapter
4. **Navigation** → Players can flip through pages with arrows or mouse wheel

## 🔧 Your JSON Structure Breakdown

```json
{
  "title_key": "eidolonunchained.codex.community_rituals.summoning.title", ← Translation key
  "title": "Community Summoning",                                          ← Fallback text
  "icon": "minecraft:bell",                                               ← Chapter icon
  "pages": [                                                              ← Array of pages
    {"type": "text", "content": "..."},                                  ← Page 1: Text
    {"type": "text", "content": "..."},                                  ← Page 2: Text  
    {"type": "ritual", "content": "...", "data": {...}},                ← Page 3: Ritual
    {"type": "text", "content": "..."}                                   ← Page 4: Warning
  ]
}
```

## 🎨 Visual Representation

```
Category Icon: 🔮 Community Rituals
    ↓
Chapter Icon: 🔔 Community Summoning  
    ↓
Pages: [📄 Text] → [📄 Text] → [⚡ Ritual] → [⚠️ Warning]
```

This creates a fully integrated experience that feels native to Eidolon while adding your custom content! The JSON structure you created will be automatically converted into the proper Eidolon page format by our system. 🎉
