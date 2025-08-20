# Eidolon Unchained Codex System: Chapters vs. Entries

## Overview
The codex system is organized into **chapters** (sections/tabs) and **entries** (pages/articles). This allows you to group related content and control how it appears in the in-game codex.

---

## 1. Codex Chapters
- **Location:** `src/main/resources/data/eidolonunchained/codex_chapters/`
- **Purpose:** Each file here defines a chapter (a top-level section/tab in the codex).
- **Format:** Each file is a JSON object with at least a `"title"` field, and optionally an `"icon"`.
- **Example:**
  ```json
  {
    "title": "Community Rituals",
    "icon": "minecraft:book"
  }
  ```
- **Filename:** The filename (without `.json`) is the chapter's ID. For example, `community_rituals.json` defines the chapter ID `community_rituals`.

---

## 2. Codex Entries
- **Location:** `src/main/resources/data/eidolonunchained/codex/` (and subfolders)
- **Purpose:** Each file here is an entry (a page or article) that appears inside a chapter.
- **Format:** Each entry must have a `"target_chapter"` field, which tells the mod which chapter it belongs to.
- **Example:**
  ```json
  {
    "target_chapter": "community_rituals",
    "title": "Community Summoning",
    ...
  }
  ```
- **Mapping:** The value of `"target_chapter"` must match the ID of a chapter defined in `codex_chapters/`.

---

## 3. How to Map Entries to Chapters
- For every entry, check its `"target_chapter"` value.
- Make sure there is a file in `codex_chapters/` with the same name as the `target_chapter` value.
- Example: If an entry has `"target_chapter": "rituals"`, you need a file `codex_chapters/rituals.json`.

---

## 4. Example Mapping Table
| Entry File Path                                         | target_chapter   | Chapter File Needed                      |
|--------------------------------------------------------|------------------|------------------------------------------|
| codex/community_rituals/community_summoning.json       | summon_ritual    | codex_chapters/summon_ritual.json        |
| codex/community_rituals/ritual_binding.json            | rituals          | codex_chapters/rituals.json              |
| codex/custom_spells/elemental_fusion.json              | rituals          | codex_chapters/rituals.json              |
| codex/expansions/expansion_pack.json                   | rituals          | codex_chapters/rituals.json              |

---

## 5. Troubleshooting
- If your entries do not appear in-game, check that:
  - Every `target_chapter` has a matching file in `codex_chapters/`.
  - The chapter file has at least a `"title"` field.
  - Your JSON files are valid and not empty.

---

## 6. Adding New Chapters or Entries
- To add a new chapter: create a new file in `codex_chapters/`.
- To add a new entry: create a new file in `codex/` and set its `target_chapter` to the desired chapter's ID.

---

For more details, see the code in `CodexDataManager.java` and your datapack structure.
