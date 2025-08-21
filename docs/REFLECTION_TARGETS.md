# Reflection Targets

This mod currently uses reflection to access internal classes and fields of Eidolon Repraised
(version 0.3.8.15). Update the paths below if upstream names change.

| Purpose | Class Path | Field | Notes |
|--------|------------|-------|-------|
| Categories list | `elucent.eidolon.codex.CodexChapters` | `categories` | Replace with official API when available |
| Category key | `elucent.eidolon.codex.Category` | `key` | Use `Category#getKey()` once exposed |
| Category index holder | `elucent.eidolon.codex.Category` | `chapter` | Should become public chapter/index accessor |
| Chapter page list | `elucent.eidolon.codex.Chapter` | `pages` | Replace with public getter when provided |
| IndexPage entries | `elucent.eidolon.codex.IndexPage` | `entries` | Replace with accessor or builder |

If any of these targets change, adjust the constants in `EidolonCategoryExtension`
and update this document to reflect the new paths.
