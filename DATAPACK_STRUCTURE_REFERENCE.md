## Eidolon Unchained Datapack Structure Reference

### Folder Layout

- `data/eidolonunchained/codex/` — Category folders, each with a `_category.json` and entry JSONs
- `data/eidolonunchained/codex_chapters/` — Chapter JSONs
- `data/eidolonunchained/codex_entries/` — Entry JSONs (may be in subfolders)
- `data/eidolonunchained/research_chapters/` — Research chapter JSONs
- `data/eidolonunchained/research_entries/` — Research entry JSONs

### Required Fields
- **Codex Entry:**
  - `chapter`: (string) — must match a chapter ID
  - `pages`: (array) — content pages
- **Codex Chapter:**
  - `title`: (string)
  - `icon`: (string or object)
- **Category:**
  - `key`: (string)
  - `name`: (string)
  - `icon`: (string)
- **Research Entry:**
  - `id`: (string)
  - `chapter`: (string)
  - `title`: (string)
  - `description`: (string)
- **Research Chapter:**
  - `id`: (string)
  - `title`: (string)
  - `description`: (string)
  - `category`: (string)
  - `icon`: (object)

### Best Practices
- Use lowercase IDs for all keys and references.
- Ensure all referenced chapters/categories exist.
- Add lang keys for every category, chapter, and entry.
- Avoid duplicate keys in JSON files.
