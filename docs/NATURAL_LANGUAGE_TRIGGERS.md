# Natural-Language Triggers

This system now supports two complementary ways to react to natural language:

- JSON-driven triggers you define (data-first)
- AI-decided triggers emitted by the model at runtime (AI-first)

Both paths go through the same gating: reputation checks, cooldowns, safety filters, and consent rules remain enforced by the game.

## Configuration

Control how natural-language triggers behave and whether cooldowns are surfaced to the AI using the common config:

- natural_language_trigger_mode: one of
  # Natural-Language Triggers

  This system supports two complementary ways to react to player text:

  - AI-decided triggers emitted by the model at runtime (AI-first)
  - JSON-driven triggers you define (data-first)

  All paths use the same safety gates: reputation checks, cooldowns, effect/blessing validation, and consent rules.

  ## Defaults and configuration

  AI-first is now the default. Modpack authors can switch modes in the common config:

  - natural_language_trigger_mode
    - ai_only — Default. Only AI-decided [TRIGGER:...] tags are parsed after the AI responds
    - json_only — Only JSON-defined triggers are evaluated before AI
    - both — Enable both flows (JSON pre-AI + AI post-AI)
  - expose_cooldowns_in_ai_context (default: true)
    - When true, the deity prompt includes current cooldowns so the AI can politely decline or suggest alternatives.

  Location of these options: generated common config (standard Forge config handling). Changes are hot-reloadable where supported.

  ---

  ## AI-decided triggers (AI-first)

  Include special markup at the end of the AI’s message. The player never sees these tags; the game parses one tag per message and applies all gates before executing.

  Supported tags:
  - [TRIGGER:offer_fate] — Offer a fate to the player. Assignment still requires “yes/sure/ok”.
  - [TRIGGER:send_message text="short immersive hint"] — Send a short message to the player.
  - [TRIGGER:run_commands commands="/effect give {player} minecraft:resistance 300 1"] — Suggest utility commands; validated and executed server-side.

  Rules:
  - At most one trigger is handled per AI message to avoid spam.
  - If a fate offer is already pending, additional offer_fate tags are ignored.
  - Placeholders like {player} resolve server-side.
  - When a trigger implies a blessing (e.g., effects, potion grants), blessing gates and cooldowns are applied automatically.

  Example AI output (what the AI produces):

  > “You’ve endured much. Steady yourself; I will shield you.” [TRIGGER:run_commands commands="/effect give {player} minecraft:resistance 300 1"]

  What the player sees:

  > “You’ve endured much. Steady yourself; I will shield you.”

  What actually happens:
  - After gating and cooldown checks, the command runs silently.

  ---

  ## JSON-driven triggers (data-first)

  You can still define lightweight triggers in any AI deity JSON via a `natural_language_triggers` array in `src/main/resources/data/<namespace>/ai_deities/<deity>.json`.

  Schema:
  - id: string (unique per deity; used for cooldown keys)
  - contains: [string] (case-insensitive; OR with regex)
  - regex: [string] (Java regex; OR with contains)
  - min_reputation: int (optional)
  - cooldown_seconds: long (optional)
  - action: one of
    - offer_fate — Starts a consent-only fate offer
    - run_commands — Executes commands in params.commands
    - send_message — Sends params.text to the player
  - params: object (by action)
    - run_commands: { commands: ["give {player} minecraft:bread 3", ...], prayer_type?: "blessing" }
    - send_message: { text: "§6⟦ Deity ⟧ §f..." }

  Blessing-aware routing:
  - If params includes prayer_type:"blessing" for run_commands, blessing gates and cooldowns apply consistently with prayer systems.

  Security and limits:
  - Commands are validated and placeholders are resolved server-side.
  - Only one trigger is executed per message to prevent spam.

  Minimal examples

  Offer a fate when the player asks for one:
  ```json
  "natural_language_triggers": [
    {
      "id": "ask_task",
      "contains": ["quest", "task", "fate"],
      "cooldown_seconds": 60,
      "action": "offer_fate"
    }
  ]
  ```

  Apply a short blessing via commands with proper gating:
  ```json
  "natural_language_triggers": [
    {
      "id": "short_blessing",
      "contains": ["bless me"],
      "min_reputation": 10,
      "cooldown_seconds": 180,
      "action": "run_commands",
      "params": {
        "prayer_type": "blessing",
        "commands": [
          "effect give {player} minecraft:resistance 300 1"
        ]
      }
    }
  ]
  ```

  Send a lightweight warning message:
  ```json
  "natural_language_triggers": [
    {
      "id": "warn_deforest",
      "regex": ["(?i)(burn|cut) (the )?(forest|trees)"],
      "cooldown_seconds": 180,
      "action": "send_message",
      "params": { "text": "§6⟦ Deity ⟧ §fHarm the green world and it will turn against you." }
    }
  ]
  ```

  ---

  ## Advanced: negative effects with safeguards

  Admins may optionally enable curated negative effects using a dedicated action in AI adjudication:

  - Action: curse_target (server-side only)
  - Targeting: restricted to team/tag filters; limited radius and max_targets
  - Safety: strict cooldowns and reputation thresholds recommended

  Note: If your data defines such behaviors, ensure conservative defaults and clear player feedback.

  ---

  ## Tips
  - Use min_reputation to reserve stronger effects for trusted followers.
  - Keep cooldown_seconds non-zero for impactful actions.
  - Prefer offer_fate when the player asks for work so assignments remain consent-based.
  - JSON triggers should be lightweight nudges; let the AI carry immersion and decide most actions.
This pairs nicely with JSON triggers: use JSON for guaranteed patterns, and let the AI sprinkle context-aware moments during conversation.

---

## Current Capabilities and Limits

Supported today:
- Triggers activate only from player chat content (contains/regex matching).
- Actions supported: offer_fate, run_commands (simple effects/items), send_message.
 - Optional: curse_target for targeted negative effects (uses per-trigger cooldowns).
- Cooldowns and min_reputation gate execution; one AI-decided trigger per response.

Not yet supported (avoid relying on these):
- Automatic detection of world events like cutting trees, biome entry, or block changes via NL triggers.
- Complex context like inventory scanning in NL triggers (AI may still infer context, but enforcement happens server-side).
- Multi-step workflows inside a single trigger.

Recommendation: When in doubt, prefer small, clear, player-initiated phrases and conservative helper effects.

---

## Command targeting and placeholders

When using `run_commands` (from JSON or AI-decided), targeting follows vanilla Minecraft syntax and a small set of placeholders resolved server-side:

- Supported selectors: `@s` (source player), `@p` (nearest player), `@a` (all players), `@e[...]` (entity queries)
- Placeholders: `{player}` resolves to the current player name before execution
- Execution context: commands run as the player; `@s` resolves correctly due to the command source stack being the player

Notes and safety:
- Use selectors (e.g., `@e[type=minecraft:zombie,distance=..8]`) for mob effects/items instead of custom placeholders
- `{mobid}` is not implemented; prefer selectors for clarity and server-side validation
- Namespaced IDs must be colon-safe (e.g., `minecraft:regeneration`) — suggestions and validators already account for this

Examples:
- `effect give {player} minecraft:resistance 300 1`
- `effect give @e[type=minecraft:zombie,distance=..8,limit=3] minecraft:slowness 120 1`

If you need more targeting helpers (e.g., last-hit target), open an issue with the desired behavior and constraints.
