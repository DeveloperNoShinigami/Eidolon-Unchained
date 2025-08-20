## Eidolon Unchained Changelog (Recent Major Changes)

### v3.9.0.9 (Minecraft 1.20.1, Eidolon Repraised 0.3.8.15)

- Refactored all integration to be fully datapack-driven; removed all reflection and legacy code.
- All codex and research entries now use `chapter` and `id` fields (no more `target_chapter`, `research_id`, or `target_research`).
- Normalized all chapter and category IDs to lowercase for consistency.
- Added and fixed lang keys for all categories, chapters, and entries.
- Fixed JSON errors (duplicate keys, mismatched IDs) in datapack files.
- Improved documentation and troubleshooting for datapack-driven content.
