# Research System Visual Guide

## Research Flow Diagram

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   TRIGGER       │ -> │   CONDITIONS    │ -> │     TASKS       │ -> │    REWARDS      │
│                 │    │                 │    │                 │    │                 │
│ • Kill Entity   │    │ • Dimension     │    │ • Collect Items │    │ • Grant Signs   │
│ • Interact      │    │ • Inventory     │    │ • Spend XP      │    │ • Give Items    │
│ • Enter Area    │    │ • Time of Day   │    │ • Multi-Stage   │    │ • Action Bar    │
│                 │    │ • Weather       │    │ • NBT Support   │    │   Notifications │
└─────────────────┘    └─────────────────┘    └─────────────────┘    └─────────────────┘
```

## Example: Zombie Research (Actually Implemented)

```
TRIGGER: Kill Zombie
          |
          v
CONDITIONS: In Overworld
          |
          v
TASKS: Stage 1 -> Collect 5 Rotten Flesh
       Stage 2 -> Spend 3 XP Levels  
          |
          v
REWARDS: Death Sign + Diamond
```

## Available Signs System

```
┌─────────────────────────────────────────────────────────────┐
│                    MYSTICAL SIGNS SYSTEM                   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   FLAME      DEATH       SOUL        BLOOD      MIND       │
│     │          │          │           │          │         │
│     v          v          v           v          v         │
│  Fire &    Necromancy   Soul      Blood Magic  Mental      │
│ Destruction  Undeath   Manipulation              Influence  │
│                                                             │
│   WINTER     SACRED      WICKED                             │
│     │          │          │                                 │
│     v          v          v                                 │
│   Cold &     Divine &   Dark &                              │
│ Ice Magic   Holy Magic Forbidden                            │
│                        Magic                                │
└─────────────────────────────────────────────────────────────┘
```

## Content Integration Flow

```
┌───────────────┐    ┌───────────────┐    ┌───────────────┐
│   RESEARCH    │    │  CODEX ENTRY  │    │   GAMEPLAY    │
│   COMPLETED   │ -> │   UNLOCKED    │ -> │   ENHANCED    │
│               │    │               │    │               │
│ Kill 10 Zombies   │ │ "Zombie Anatomy"   │ │ Better Drops  │
│ Collect Items      │ │ Entry Appears      │ │ New Recipes   │
│ Spend XP           │ │ In Codex           │ │ Sign Powers   │
└───────────────┘    └───────────────┘    └───────────────┘
```

## Translation System Structure

```
Language File Structure:
assets/your_mod/lang/en_us.json
{
  "mod.codex.entry.zombie_study.title": "Zombie Anatomy",
  "mod.codex.entry.zombie_study": "Basic zombie research introduction",
  "mod.codex.entry.zombie_study.details": "Detailed anatomical findings",
  "mod.codex.entry.zombie_study.conclusion": "Research conclusions"
}

┌─────────────────┐
│   Title Page    │  Uses: base key + ".title"
│                 │  Content: base key
├─────────────────┤
│   Text Page     │  Uses: ".details" key
│                 │  
├─────────────────┤
│   Text Page     │  Uses: ".conclusion" key
│                 │  
└─────────────────┘
```

## Page Type Relationships

```
                    ┌─────────────────┐
                    │   TITLE PAGE    │ <- Always first
                    │                 │
                    └─────┬───────────┘
                          │
                          v
                 ┌────────┴────────┐
                 │   CONTENT       │
                 │   PAGES         │
                 └─────────────────┘
                          │
            ┌─────────────┼─────────────┐
            │             │             │
            v             v             v
    ┌─────────────┐ ┌───────────┐ ┌─────────────┐
    │    TEXT     │ │  RECIPE   │ │   ENTITY    │
    │   PAGES     │ │  PAGES    │ │   PAGES     │
    └─────────────┘ └───────────┘ └─────────────┘
            │             │             │
            │             │             │
            v             v             v
    "Explanations   "Crafting     "3D Models
     and Lore"       Rituals       & Info"
                     Crucible"
```

## File Organization Diagram

```
your_datapack/
├── pack.mcmeta
├── data/
│   └── your_mod/
│       ├── codex_entries/           <- JSON entry definitions
│       │   ├── basic/
│       │   ├── intermediate/
│       │   └── advanced/
│       └── research/                <- Research definitions
│           ├── discovery/
│           ├── progression/ 
│           └── mastery/
└── assets/
    └── your_mod/
        └── lang/                    <- Translation files
            ├── en_us.json
            ├── es_es.json
            └── de_de.json
```

## Data Flow Architecture

```
Game Event -> Trigger Check -> Condition Evaluation -> Task Tracking -> Reward Processing
     │              │               │                      │               │
     │              │               │                      │               │
     v              v               v                      v               v
Kill Zombie -> Entity Match -> Dimension OK -> Collect Items -> Grant Sign
Block Click -> Block Match -> Time Check -> Spend XP -> Give Items  
Enter Nether -> Location -> Weather OK -> Multi-Stage -> Unlock Entry
Use Tool -> Item Match -> Inventory -> NBT Check -> Enable Recipe
```
