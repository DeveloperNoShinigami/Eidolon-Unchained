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
