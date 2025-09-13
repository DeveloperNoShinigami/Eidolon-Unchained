# Tasks & Reputation

Divine Tasks

- Command entrypoints for assigning/listing/completing tasks, and checking reputation.
- Implementation: `src/main/java/com/bluelotuscoding/eidolonunchained/commands/TaskCommands.java`

Overview

- Deity-driven objectives that reward reputation and can run commands.
- Defined in `task_config` of AI deity JSON (per deity).
- Persist per-player; auto-complete on ritual events, or manually via command.

Commands (canonical)

- ` /eidolon-unchained tasks assign <player> <deity> <taskId>`
- ` /eidolon-unchained tasks assignany <player> <taskId>`
- ` /eidolon-unchained tasks complete <player> <taskId>`
- ` /eidolon-unchained tasks list <player>`
- ` /eidolon-unchained tasks reputation <player> <deity>`
- ` /eidolon-unchained tasks repall <player>`

Legacy alias

- The older `/dtask` tree remains as a compatibility alias.

Chat & Feedback Keys

- See `assets/eidolonunchained/lang/en_us.json` under `eidolonunchained.task.*` and `eidolonunchained.chat.*`.

Notes

- Reputation integrates with AI Deity prayers to gate conversations/rewards.
- Autocomplete: deity IDs and task IDs are suggested dynamically.
- Full JSON spec: wiki/Datapacks/Task-JSON.md

How It Works

- Assignment: via command or AI; stored in `PlayerContextTracker` (NBT-backed).
- Automatic completion: ritual requirements are detected in `onRitualComplete`.
- Manual completion: checks item/ritual token-style requirements, awards rep, runs reward commands.

Requirement Styles

- Token: `item:<ns:item>:<count>`, `ritual:<ns:id>` (preferred for manual completion)
- JSON: `{ "type": "item"|"ritual"|..., ... }` (stored internally as `type:` + JSON)

Tips

- Provide both JSON (readable) and token entries for each requirement until the JSON validator is extended.
- Use `cooldown_hours` and `repeatable` to balance content.
