# Commands

The main command root is:

- `/eidolon-unchained`
- `/eu` as a short alias

## Command Families

| Family | Access | Description |
|---|---|---|
| `config` | All | Config reload, status, validate, reset |
| `api` | All | API key management, model selection |
| `deities` | All | List, reload, status of loaded deities |
| `patron` | All | Choose, abandon, status for patron deity |
| `prayers` | All/Op | Prayer history, cooldowns; clear-cooldown (op) |
| `fates` | Op | Assign, complete, list, reputation for fate/task system |
| `conversations` | All | Conversation history stats and clearing |
| `research` | All/Op | List entries; reload and clear (op) |
| `player2ai` | Op | Auth, login, memory, characters, test |
| `chant` | All | Chant slot management |
| `tts` | All | Text-to-speech configuration |
| `debug` | Op only | AI, ritual, reputation, progression, tier, facts, rewards |

## Notes

- `tasks` has been removed. `fates` is the canonical name for that system.
- `debug` is fully permission-gated (`hasPermission(2)`) and includes all developer utilities.
- All ID arguments (deity, fateId, ritualId, fact, player) have tab-completion suggestion providers.
- `ritual-diagnose` is now under `debug ritual diagnose <pos>`.
- `ai-debug` standalone subtree has been removed; all AI debug commands are under `debug ai`.

For full syntax see [Complete Command Reference](Complete-Command-Reference.md).

For commands registered by base Eidolon (not Eidolon Unchained), see [Eidolon Base Commands](Eidolon-Base-Commands.md).
