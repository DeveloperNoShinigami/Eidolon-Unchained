# Note on Research and Ritual Datapack Support

Currently, the Eidolon Unchained system provides robust datapack support for codex entries, including rituals as codex pages. However, full datapack-based extension of the underlying research system and actual ritual mechanics (beyond documentation) may require additional development. At present, you can document rituals and research in the codex, but adding new functional rituals or research entries via datapack is not fully supported out-of-the-box. Further code changes may be needed for full extensibility.

# Eidolon Unchained Codex System - Wiki Overview

Welcome to the comprehensive documentation for the Eidolon Unchained Codex Extension System! This guide is organized for easy conversion into a wiki, with clear sections for features, usage, page types, formatting, troubleshooting, and best practices.

---

## 1. System Overview

Eidolon Unchained allows you to extend the in-game Eidolon codex with custom entries, pages, and visual elements using JSON datapacks and language files. The system is fully operational, supporting rich formatting, multiple page types, and robust error handling.

---

## 2. Key Features

- **JSON-based Codex Extensions**: Add new entries and pages to existing Eidolon chapters.
- **Multiple Page Types**: Title, text, entity, crafting, ritual, list, chant, rune, sign, and more.
- **Visual Elements**: Icons, 3D entity models, item grids, ritual diagrams, and side images.
- **Full Minecraft Formatting Support**: Colors, bold, italic, underline, and more.
- **Localization**: Language file support for all content, with caching and fallback.
- **Debug Tools**: In-game commands for testing translations and reloading codex entries.
- **Comprehensive Logging**: Detailed logs for troubleshooting and validation.

---

## 3. File Structure

```
src/main/resources/
├── data/eidolonunchained/codex_entries/   # JSON entry files
├── assets/eidolonunchained/lang/          # Language files (en_us.json, etc.)
└── ...
```

---

## 4. Supported Page Types

### Basic Pages
- **Title Page**: Large heading, subtitle, optional icon.
- **Text Page**: Standard content, supports formatting and icons.

### Recipe & Crafting Pages
- **Crafting, Smelting, Crucible, Worktable**: Show recipes with icons and descriptions.

### Advanced Pages
- **Entity Page**: 3D model, stats, drops, and info.
- **Ritual Page**: Ritual circle, ingredients, step-by-step instructions.
- **List Page**: Item collections, grid layouts, descriptions.
- **Chant, Rune, Sign Pages**: Specialized magical content.

---

## 5. Visual Elements & Formatting

- **Icons**: Use Minecraft or Eidolon item IDs.
- **Entity Models**: 3D, rotatable, with stats and drops.
- **Ritual Diagrams**: Top-down layouts, item placement.
- **Formatting Codes**: `§4` (red), `§l` (bold), `§o` (italic), etc.
- **Color & Style Reference**: See full list in the Visual Elements Reference section.

---

## 6. Localization & Translation

- **Language Files**: Place in `assets/eidolonunchained/lang/en_us.json`.
- **Key Format**: `eidolonunchained.codex.entry.entry_name.section`
- **Best Practices**: Use descriptive keys, keep content concise, always provide both base and `.title` keys for title pages.
- **Special Characters**: Escape `%` as `%%`, use `\n` for newlines, and see the Codex Translation Tips for more.

---

## 7. Usage Instructions

1. **Create JSON Entry**: Place in `codex_entries/` with target chapter and pages.
2. **Add Language Keys**: Define all referenced keys in your language file.
3. **Test in Game**: Use `/reload` for JSON, restart for language changes.
4. **Debug**: Use `/eidolonunchained test_translations` and `/eidolonunchained reload_codex`.
5. **Check Logs**: Look for success and error messages in `latest.log`.

---

## 8. Troubleshooting & Debugging

- **Entries Not Appearing**: Check file placement and logs.
- **Format Errors**: Escape `%` as `%%`, avoid unused format specifiers.
- **Missing Translations**: Ensure all keys exist, restart game after changes.
- **Debug Commands**: `/eidolonunchained test_translations`, `/eidolonunchained reload_codex`.
- **Log Messages**: Look for CodexDataManager, EidolonCodexIntegration, and EidolonPageConverter entries.

---

## 9. Best Practices

- Plan text and structure before testing.
- Use consistent, descriptive keys.
- Group related content and use multiple pages for complex topics.
- Test formatting and icons in-game for clarity.
- Reference the Codex Translation Tips for safe character usage.

---

## 10. Reference & Examples

- See `docs/codex_reference.md` for all supported page types and JSON examples.
- See `VISUAL_ELEMENTS_REFERENCE.md` for visual breakdowns and formatting code samples.
- See `docs/codex_reference.md#advanced-formatting-codes` for translation and formatting tips.
- See `docs/codex_reference.md` for the full reference and `docs/codex_tutorial.md` for a hands-on guide.

---

## 11. Future Expansion

- Custom backgrounds and animations
- Interactive ritual builder
- Multi-language support improvements
- Additional page types and integrations

---

This structure is ready for wiki conversion. Each section can become a dedicated wiki page or subpage for easy navigation and reference.
