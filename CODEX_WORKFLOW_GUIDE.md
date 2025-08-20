# Eidolon Unchained Codex Workflow Guide

This guide explains how to implement categories, chapters, and pages (entries) in the Eidolon Unchained codex system, and how to ensure everything is mapped and appears in-game.

---

## 1. Categories (Optional)
- **Purpose:** Group chapters in the codex UI for better organization.
- **How:**
  - Create a `_category.json` in a subfolder of `codex/` (e.g., `codex/my_category/_category.json`).
  - Example:
    ```json
    {
      "title": "Elemental Magic",
      "icon": "minecraft:blaze_powder"
    }
    ```
- **Note:** Categories are for UI grouping only and do not affect chapter logic.

---

## 2. Chapters
- **Purpose:** Define the main sections/tabs in the codex.
- **How:**
  - Go to `src/main/resources/data/eidolonunchained/codex_chapters/`.
  - Create a file named `<chapter_id>.json` (e.g., `rituals.json`).
  - Example:
    ```json
    {
      "title": "Rituals",
      "icon": "minecraft:enchanting_table"
    }
    ```
- **Filename:** The filename (without `.json`) is the chapter’s ID.

---

## 3. Pages (Entries)
- **Purpose:** The actual content/pages inside a chapter.
- **How:**
  - Go to `src/main/resources/data/eidolonunchained/codex/` (or a subfolder).
  - Create a file for each entry (e.g., `custom_spells/fire_mastery.json`).
  - Example:
    ```json
    {
      "target_chapter": "rituals",
      "title": "Fire Mastery",
      "icon": "minecraft:fire_charge",
      "pages": [
        { "type": "text", "content": "Harness the power of fire." }
      ]
    }
    ```
  - The `"target_chapter"` must match the chapter ID you created above.

---

## 4. Mapping & Validation
- **Scan Results:**
  - Your entries use these chapters: `rituals`, `summon_ritual`.
  - You have these chapters defined: `community_rituals`, `rituals`, `summon_ritual`.
  - **All required chapter files exist.**
- **To add a new entry:**
  - Set its `"target_chapter"` to the desired chapter’s ID.
  - If you use a new chapter ID, create a matching file in `codex_chapters/`.

---

## 5. Troubleshooting
- If entries do not appear in-game:
  - Ensure every `target_chapter` has a matching file in `codex_chapters/`.
  - The chapter file must have at least a `"title"` field.
  - Check for valid, non-empty JSON files.

---

## 6. Example Table
| Entry File Path                                         | target_chapter   | Chapter File Needed                      |
|--------------------------------------------------------|------------------|------------------------------------------|
| codex/community_rituals/community_summoning.json       | summon_ritual    | codex_chapters/summon_ritual.json        |
| codex/community_rituals/ritual_binding.json            | rituals          | codex_chapters/rituals.json              |
| codex/custom_spells/elemental_fusion.json              | rituals          | codex_chapters/rituals.json              |
| codex/expansions/expansion_pack.json                   | rituals          | codex_chapters/rituals.json              |

---

For more details, see also: `CODEX_SYSTEM_OVERVIEW.md`.
