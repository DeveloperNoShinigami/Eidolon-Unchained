# Tasks & Reputation

Divine Tasks

- Command entrypoints for assigning/listing/completing tasks, and checking reputation.
- Implementation: `src/main/java/com/bluelotuscoding/eidolonunchained/commands/TaskCommands.java`

Command Summary (`/dtask`)

- ` /dtask assign <player> <deity> <taskId>` — assign a specific task for a deity
- ` /dtask assignany <player> <taskId>` — assign task without deity restriction
- ` /dtask complete <player> <taskId>` — mark task complete
- ` /dtask list <player>` — show active tasks
- ` /dtask reputation <player> <deity>` — show reputation for a deity
- ` /dtask repall <player>` — show reputation for all deities

Chat & Feedback Keys

- See `assets/eidolonunchained/lang/en_us.json` under `eidolonunchained.task.*` and `eidolonunchained.chat.*`.

Notes

- Reputation also integrates with AIDeity prayers to gate conversations/rewards.
