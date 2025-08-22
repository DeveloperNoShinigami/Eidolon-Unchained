# Eidolon Unchained Datapack System

Welcome to the comprehensive documentation for the Eidolon Unchained datapack system. This wiki-style documentation will guide you through all aspects of creating custom content for Eidolon Unchained.

## 📚 Table of Contents

### Getting Started
- [Quick Start Guide](getting-started.md) - Jump right in with a basic example
- [System Overview](system-overview.md) - Understanding the architecture

### Core Systems
- [Codex Entries](codex-entries.md) - Adding content to existing chapters

### Advanced Features
- [Page Types](page-types.md) - All available page types and their usage

### Reference
- [File Structure](file-structure.md) - Complete directory layout
- [Examples](examples.md) - Complete working examples

### API Documentation
- [API Reference](api/) - Developer documentation folder

## 🎯 Quick Navigation

### For Content Creators
- **New to datapacks?** Start with the [Quick Start Guide](getting-started.md)
- **Adding lore content?** Check out [Codex Entries](codex-entries.md)
- **Understanding page types?** See [Page Types](page-types.md)

### For Developers
- **Understanding the code?** Browse the [API Documentation](api/)
- **Contributing?** Check [file-structure.md](file-structure.md) for standards

### For Server Admins
- **Need examples?** See [Examples](examples.md)

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
