## Task System JSON

This guide fully documents the deity Task system: the JSON schema, how tasks execute and complete, and how it ties into commands, AI, and player persistence.

**Overview**
- Per‑deity tasks are defined under `task_config` in each AI deity JSON file (`data/<ns>/ai_deities/*.json`).
- Tasks are assigned to players (manually via commands or by AI logic), tracked in the player’s context, and completed automatically or manually.
- Rewards include reputation and optional server commands.

**Data Model (Code)**
- `TaskSystemConfig.TaskTemplate`: src/main/java/com/bluelotuscoding/eidolonunchained/ai/TaskSystemConfig.java:12
- Loader: `AIDeityManager.loadTaskConfig(...)`: src/main/java/com/bluelotuscoding/eidolonunchained/ai/AIDeityManager.java:519
- Player state: `PlayerContextTracker`: src/main/java/com/bluelotuscoding/eidolonunchained/ai/PlayerContextTracker.java
- Commands: `TaskCommands`: src/main/java/com/bluelotuscoding/eidolonunchained/commands/TaskCommands.java

**task_config Schema**
- `enabled` (bool, default true): Toggle this deity’s task set.
- `max_active_tasks` (int): Max active tasks per player for this deity.
- `available_tasks` (array<object>): List of task templates:
  - `task_id` (string, required): Unique per deity (e.g., `harvest_shadows`).
  - `display_name` (string): Human‑friendly label. Used in UIs and autocomplete comments.
  - `description` (string): What the player must do; shown in `/tasks list`.
  - `progression_tier` (string): Optional label you can use in docs/promo.
  - `requirements` (array<object|string>): The conditions to complete the task. See “Requirement Types”.
  - `rewards` (object):
    - `reputation` (int): Reputation awarded upon completion.
    - `commands` (array<string>): Server commands to run (supports `{player}` token).
  - `cooldown_hours` (int): Hours before this task can be reassigned (per player).
  - `repeatable` (bool): Whether the task can be taken again later.
  - `ai_assignment_context` (object): Arbitrary JSON for AI prompt logic (stored as string).

**Requirement Types**
Requirements may be defined in two interoperable styles:
1) Token style (simple strings) — recommended for manual validation
- Item count: `item:<namespace:item_name>:<count>` (e.g., `item:minecraft:wheat:16`)
- Ritual performed: `ritual:<namespace:ritual_id>` (e.g., `ritual:eidolonunchained:shadow_communion`)
2) JSON style (objects) — convenient authoring; stored internally as `type:` + JSON
- Item: `{ "type": "item", "item": "minecraft:wheat", "count": 16 }`
- Ritual: `{ "type": "ritual", "ritual": "eidolonunchained:shadow_communion" }`
- Entity kills: `{ "type": "entity_kill", "entity": "minecraft:zombie", "count": 10 }`
Notes
- Automatic ritual completion checks for `ritual:` and the ritual id; both styles work.
- Manual completion currently validates token style for item/ritual. Provide tokens or extend validator to parse JSON.

**Rewards**
- Reputation: added via Eidolon’s `IReputation` capability.
- Commands: Each command supports `{player}` token replacement and runs as server console.

**Runtime Flow**
1) Assignment
- Manual: `/eidolon-unchained tasks assign <player> <deity> <taskId>`
- Any‑deity lookup: `/eidolon-unchained tasks assignany <player> <taskId>`
- AI‑driven: call `PlayerContextTracker.assignTask(...)` from AI logic.
2) Tracking & Persistence
- Active tasks: `EnhancedPlayerContext.activeTasks` (NBT‑backed)
3) Completion
- Automatic (ritual) via `onRitualComplete`
- Manual: `/eidolon-unchained tasks complete <player> <taskId>`
4) Listing & Reputation
- `/eidolon-unchained tasks list <player>` and `reputation`/`repall`

**Commands & Autocomplete**
- Canonical root: `/eidolon-unchained tasks ...` (legacy `/dtask` works)
- Autocomplete: deities (DatapackDeityManager), taskIds (AIDeityManager), players (online)

**Example Snippet**
```
{
  "task_config": {
    "enabled": true,
    "max_active_tasks": 3,
    "available_tasks": [
      {
        "task_id": "bind_restless_souls",
        "display_name": "Bind the Restless Souls",
        "description": "Perform the Rite of Shadows and bring a soul gem.",
        "requirements": [
          "ritual:eidolonunchained:shadow_communion",
          { "type": "item", "item": "eidolon:soul_shard", "count": 1 }
        ],
        "rewards": {
          "reputation": 8,
          "commands": [
            "give {player} eidolon:death_essence 2",
            "effect give {player} minecraft:resistance 120 1"
          ]
        },
        "cooldown_hours": 24,
        "repeatable": false
      }
    ]
  }
}
```

**Best Practices**
- Always include `display_name` and a clear `description`.
- Prefer token‑style requirements for manual completion; mirror JSON for readability if desired.
- Use `cooldown_hours` for powerful tasks; set `repeatable` accordingly.
- Keep reward commands safe; avoid admin/maintenance commands.
- Tie tasks to deities thematically.

**Troubleshooting**
- Task not in autocomplete: check JSON loads and logs.
- Ritual not auto‑completing: ensure `onRitualComplete` is fired or use `/eidolon-unchained tasks ritual`.
- Manual completion fails: ensure token‑style requirement entries exist.

**Related Docs**
- AI Deity JSON: wiki/Datapacks/AI-Deity-JSON.md
- Commands: wiki/Systems/Tasks-Reputation.md
