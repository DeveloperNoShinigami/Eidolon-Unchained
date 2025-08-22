# Eidolon Unchained Datapack System

Welcome to the comprehensive documentation for the Eidolon Unchained datapack system. This wiki-style documentation will guide you through all aspects of creating custom content for Eidolon Unchained.

## 📚 Table of Contents

### Getting Started
- [Quick Start Guide](getting-started.md) - Jump right in with a basic example
- [System Overview](system-overview.md) - Understanding the architecture
- [Installation & Setup](installation.md) - Setting up your development environment

### Core Systems
- [Category System](categories.md) - Creating and organizing content categories
- [Chapter System](chapters.md) - Defining chapters within categories
- [Codex Entries](codex-entries.md) - Adding content to existing chapters
- [Research System](research-system.md) - Creating unlock requirements and progression

### Advanced Features
- [Page Types](page-types.md) - All available page types and their usage
- [Translation System](translations.md) - Localization and language support
- [Conditions & Tasks](conditions-tasks.md) - Research requirements and objectives
- [Integration Guide](integration.md) - Working with other mods

### Reference
- [File Structure](file-structure.md) - Complete directory layout
- [JSON Schemas](json-schemas.md) - Data format specifications
- [Examples](examples.md) - Complete working examples
- [Troubleshooting](troubleshooting.md) - Common issues and solutions

### API Documentation
- [CodexDataManager](api/codex-data-manager.md) - Core data loading system
- [ResearchDataManager](api/research-data-manager.md) - Research progression system
- [Integration Classes](api/integration.md) - Mod compatibility layer

## 🎯 Quick Navigation

### For Content Creators
- **New to datapacks?** Start with the [Quick Start Guide](getting-started.md)
- **Adding lore content?** Check out [Codex Entries](codex-entries.md)
- **Creating progression?** See the [Research System](research-system.md)

### For Developers
- **Integrating with other mods?** Read the [Integration Guide](integration.md)
- **Understanding the code?** Browse the [API Documentation](api/)
- **Contributing?** Check [file-structure.md](file-structure.md) for standards

### For Server Admins
- **Installing datapacks?** See [Installation & Setup](installation.md)
- **Issues with loading?** Try [Troubleshooting](troubleshooting.md)

## 🔧 System Architecture

The Eidolon Unchained datapack system consists of several interconnected components:

```
Categories (organize content)
    ↓
Chapters (group related topics)
    ↓
Entries (extend chapters with pages)
    ↓
Research (progression requirements)
```

## 📁 Quick File Overview

```
data/yourmod/
├── codex/
│   └── category_name/
│       ├── _category.json      # Category definition
│       └── chapter.json        # Chapter in this category
├── codex_entries/
│   └── entry.json             # Adds pages to existing chapters
├── research_chapters/
│   └── research.json          # Research progression chapters
└── research_entries/
    └── task.json              # Individual research tasks

assets/yourmod/lang/
└── en_us.json                 # Translations for all content
```

## 🚀 What's Next?

1. **New users**: Start with [getting-started.md](getting-started.md)
2. **Experienced users**: Jump to [examples.md](examples.md)
3. **Developers**: Review [api/](api/) documentation
4. **Contributors**: Check [file-structure.md](file-structure.md)

---

*This documentation covers Eidolon Unchained v3.9.0.9 and later.*
