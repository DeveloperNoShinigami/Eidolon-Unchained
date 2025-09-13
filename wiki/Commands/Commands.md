# Commands

Root

- `/eidolon-unchained` — unified command tree

Admin/Config

- ` /eidolon-unchained config reload|status|validate|reset`

API & Models

- ` /eidolon-unchained api set <provider> <key>`
- ` /eidolon-unchained api set player2ai` (no key needed)
- ` /eidolon-unchained api set-model <model>` / `get-model`
- ` /eidolon-unchained api test <provider>` / `list` / `remove <provider>`

Debug (selected)

- Progression/debug tools are available under ` /eidolon-unchained debug ...`
- Reputation helpers: ` /eidolon-unchained debug reputation <player> "<deity_id>"`

Tasks

- See ` /dtask` commands: wiki/Systems/Tasks-Reputation.md

Autocomplete

- Deity, ritual, chant, and player suggestions are wired for convenience.

References

- `src/main/java/com/bluelotuscoding/eidolonunchained/command/UnifiedCommands.java`
- `src/main/java/com/bluelotuscoding/eidolonunchained/commands/TaskCommands.java`
