# Eidolon Unchained Codex Reference

This document lists all features of the codex extension system.

## Entry Metadata

Each codex entry JSON supports the following top‑level fields:

| Field | Type | Description |
|-------|------|-------------|
| `title` | string | Display title for the entry. |
| `description` | string | Optional introductory text shown before the pages. |
| `icon` | object/string | Item to display on the title page. Accepts `{ "item": "mod:item", "count": n, "nbt": "..." }` or a simple item ID string. |
| `prerequisites` | array | List of research IDs required to view the entry. |
| `type` | string | Hint for the entry's primary content type (e.g. `text`, `ritual`). |
| `pages` | array | Page definitions as described below. |

Unknown custom fields are preserved and exposed through the API for addon use.

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

### `smelting`
```json
{ "type": "smelting", "input": "minecraft:iron_ore", "result": "minecraft:iron_ingot" }
```
Shows a furnace recipe with input and output items.

### `workbench`
```json
{ "type": "workbench", "item": "eidolon:wooden_altar" }
```
Displays an Eidolon workbench recipe.

### `crucible`
```json
{ "type": "crucible", "recipe": "eidolon:arcane_gold_ingot" }
```
Specialized recipe page for crucible crafting.

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

## Troubleshooting

| Problem | Cause | Fix |
|---------|-------|-----|
| Title shows as key | Missing `.title` translation | Add `<key>.title` to the language file |
| Page displays raw key | Base translation missing | Provide the key in the language file |
| Recipe page shows "Air" | Invalid or missing recipe ID | Verify the namespaced ID exists |
| Page fails to load | JSON syntax or page type error | Validate JSON and supported types |
| Format error with `%` | Unescaped percent sign | Replace `%` with `%%` |
