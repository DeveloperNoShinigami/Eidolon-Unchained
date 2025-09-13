# Localization & Keys

File

- `assets/eidolonunchained/lang/en_us.json`

Conventions

- Use `_comment_structure_*` keys instead of `//` comments to keep JSON valid.
- Suggested key patterns:
  - Categories: `eidolonunchained.codex.category.<id>`
  - Chapters: `eidolonunchained.codex.chapter.<id>`
  - Entries/Pages: `eidolon.codex.entry.<id>.*` and `eidolon.codex.page.<id>.*` (for compatibility)
  - Tasks/Chat: `eidolonunchained.task.*`, `eidolonunchained.chat.*`

Example Comments

```json
{
  "_comment_structure_page_key_aliases": "Page-key aliases for chant chapters (TitlePage/ChantPage/TextPage expect eidolon.codex.page.*)",
  "eidolon.codex.page.shadow_communion.title": "Shadow Communion"
}
```
