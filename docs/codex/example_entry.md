# Example Codex Entry with Translation Keys

This example demonstrates how codex JSON files reference translations.  
All user-facing strings are defined with namespaced keys following the pattern:

```
<namespace>.codex.entry.<entry_id>.<section>
```

For the shadow manipulation entry, the JSON uses keys like
`eidolonunchained.codex.entry.shadow_manipulation.title` and
`eidolonunchained.codex.entry.shadow_manipulation.intro`.

Add the corresponding text to the language file:
`src/main/resources/assets/eidolonunchained/lang/en_us.json`.
When the game loads, these keys resolve to the localized strings.
