# Tasks & Reputation

Divine Tasks

- Command entrypoints for assigning/listing/completing tasks, and checking reputation.
- Implementation: `src/main/java/com/bluelotuscoding/eidolonunchained/commands/TaskCommands.java`

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
