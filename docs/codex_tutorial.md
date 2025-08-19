# Eidolon Unchained Codex Tutorial

This beginner-friendly guide walks through creating custom codex pages for the Eidolon Unchained mod.

## Datapack Layout

Place your files in a datapack using this structure:

```
data/
└── eidolonunchained/
    ├── codex_entries/       # JSON page definitions
    └── ...
assets/
└── eidolonunchained/
    └── lang/
        └── en_us.json       # Translation keys
```

## Creating a First Entry

1. **Create a JSON file** in `data/eidolonunchained/codex_entries/`:
   ```json
   {
     "target_chapter": "MONSTERS",
     "pages": [
       { "type": "title", "text": "eidolonunchained.codex.entry.example" },
       { "type": "text",  "text": "eidolonunchained.codex.entry.example.details" }
     ]
   }
   ```
2. **Add translations** in `assets/eidolonunchained/lang/en_us.json`:
   ```json
   {
     "eidolonunchained.codex.entry.example.title": "Example Entry",
     "eidolonunchained.codex.entry.example": "Overview shown on the title page.",
     "eidolonunchained.codex.entry.example.details": "Follow‑up information on a text page."
   }
   ```
3. **Test in game** to confirm the entry appears in the chosen chapter.

## Translation Basics

- Keys follow the pattern `eidolonunchained.codex.entry.<entry>.<section>`.
- Escape special characters:
  - Percent: use `%%`.
  - Backslash: use `\\`.
  - New line: use `\n`.
- Letters, numbers and common punctuation are safe to use.

## Reload / Restart Workflow

- Use `/reload` after changing JSON files.
- Use `/eidolonunchained reload_codex` or restart the game after editing translations.
- `/eidolonunchained test_translations` prints missing or broken keys for debugging.

## Defining a New Chapter

To create an entirely new chapter instead of extending an existing one:

1. **Create a chapter definition** in `data/yourmod/codex_chapters/`:
   ```json
   {
     "title": "yourmod.codex.chapter.mythology",
     "icon": "minecraft:book"
   }
   ```
2. **Target the chapter** from a codex entry using its namespaced ID:
   ```json
   {
     "target_chapter": "yourmod:mythology",
     "pages": [ { "type": "text", "text": "yourmod.codex.chapter.mythology.start" } ]
   }
   ```
3. **Add translations** for the chapter title and pages as usual.

When the datapack is loaded the new chapter will be created with the specified
title and icon and any entries targeting it will populate its pages.
