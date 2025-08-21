# Translation Troubleshooting

Translations under `assets/*/lang/*.json` use Java's `String.format` style placeholders. Missing or malformed format specifiers are a common source of crashes or broken strings.

## Placeholders
- `%s` inserts a string such as an entity or player name.
- `%d` inserts a number.
- Keep placeholders exactly as they appear in the English file; changing or removing them will cause formatting errors.

Examples:
```json
"death.attack.eidolon.ritual": "%s got too involved in a ritual",
"death.attack.eidolon.ritual.player": "%s was sacrificed by %s",
"eidolon.jei.health_sacrifice": "Sacrifice %d hearts."
```

## Escaping percent signs
To display a literal `%` in text, escape it with another `%` (`%%`). Otherwise the game will look for a missing format specifier and crash.

Examples from different languages:
```
"effect.eidolon.reinforced.desc": "Increases armor points by 25%%" (en_us)
"effect.eidolon.reinforced.desc": "护甲值增加25%%" (zh_cn)
```

## Tips
- Double any percent sign that is not part of a format specifier.
- Verify that the number and order of placeholders match the original string.
- Test translations in game to catch formatting issues early.
