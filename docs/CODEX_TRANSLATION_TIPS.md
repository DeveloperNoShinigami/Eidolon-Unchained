# Codex Translation Tips

This guide provides tips for writing translation strings for the Eidolon Unchained codex system in Minecraft.

## Safe Characters
- Letters (A-Z, a-z)
- Numbers (0-9)
- Punctuation: . , ! ? : ; ' " ( ) [ ] { } - _ + = / \ | < > @ # $ ^ & * ~ `
- Spaces and newlines

## Characters to Avoid or Escape
- Percent sign `%`: Use `%%` to display a literal percent sign.
- Format specifiers: `%s`, `%d`, `%1$s`, etc. (only use if you intend to provide arguments)
- Backslash `\`: Use `\\` in JSON to represent a literal backslash.
- Double quote `"`: Escape as `\"` in JSON strings.
- Newline: Use `\n` for line breaks in translation strings.

## Unicode
- Unicode characters (e.g., accented letters, symbols, emojis) are supported if your font and Minecraft version allow them.

## Not Recommended
- Control characters (ASCII 0-31 except for `\n`)
- Unescaped special JSON characters

## Example
```json
"eidolonunchained.codex.entry.example": "This is 100%% safe! Use \\"quotes\\" and newlines\nhere."
```

If you need to use a special character and are unsure, check this guide or ask for help!
