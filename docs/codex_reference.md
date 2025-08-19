# Eidolon Unchained Codex Reference

This document lists all features of the codex extension system.

## Complete Page Types

### `title`
```json
{ "type": "title", "text": "eidolonunchained.codex.entry.sample" }
```
Displays both title and introductory text.

### `text`
```json
{ "type": "text", "text": "eidolonunchained.codex.entry.sample.section" }
```
Standard text page; supports formatting and icons.

### `entity`
```json
{ "type": "entity", "entity": "eidolon:wraith" }
```
Shows a 3D entity model with description lines.

### `crafting`
```json
{ "type": "crafting", "recipe": "eidolon:arcane_gold_ingot" }
```
Displays a crafting recipe grid.

### `ritual`
```json
{ "type": "ritual", "ritual_id": "eidolon:summon_wraith" }
```
Top‑down ritual circle with ingredients and steps.

### `list`
```json
{ "type": "list", "entries": [ { "item": "eidolon:soul_gem" } ] }
```
Item lists or material checklists.

### Other recipe pages
The system also accepts `smelting`, `crucible`, and `workbench` page types for specialized recipes.

## Advanced Formatting Codes

| Code | Effect |
|------|--------|
| `§0`‑`§f` | Minecraft color codes |
| `§l` | Bold |
| `§o` | Italic |
| `§n` | Underline |
| `§m` | Strikethrough |
| `§r` | Reset formatting |

Additional tips:
- Escape `%` as `%%`.
- Use `\\` for a literal backslash and `\n` for new lines.
- Unicode characters are allowed if supported by the font.

## Debug Commands

- `/eidolonunchained reload_codex` – Reload JSON entries without restarting.
- `/eidolonunchained test_translations` – Report missing or malformed translation keys.

## Custom Chapters

Define new chapters by adding files under `data/<namespace>/codex_chapters/`:

```json
{
  "title": "yourmod.codex.chapter.mythology",
  "icon": "minecraft:book"
}
```

Entries can then target the chapter with `"target_chapter": "yourmod:mythology"`.

## Troubleshooting

| Problem | Cause | Fix |
|---------|-------|-----|
| Title shows as key | Missing `.title` translation | Add `<key>.title` to the language file |
| Page displays raw key | Base translation missing | Provide the key in the language file |
| Recipe page shows "Air" | Invalid or missing recipe ID | Verify the namespaced ID exists |
| Page fails to load | JSON syntax or page type error | Validate JSON and supported types |
| Format error with `%` | Unescaped percent sign | Replace `%` with `%%` |
