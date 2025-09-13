# Future Features & Roadmap

This roadmap focuses on practical additions aligned with the current codebase.

Planned

- Custom NPC Bridge
  - Bridge deity conversations to custom NPCs (dialog triggers, tasks, and reputation on NPCs)
  - Dialogue states that react to research, tasks, and player reputation
  - Server‑side memory hooks for persistent NPC relationships

- Iron Spells & Spellbook Support
  - Add “Iron” spell school with themed chants and mechanics
  - Spellbook UI for browsing unlocked chants and native Eidolon spells
  - Integration with research to unlock tiers and add crafting/rituals

- Expanded Datapack APIs
  - Per‑deity conversation templates and safety rails
  - Fine‑grained display controls per spell/chant (e.g., pulsing sequences)
  - JSON‑first registration for new research task types

- Mod Interop
  - Player/NPC state signals via a stable bridge (health, biome, time, nearby events)
  - Optional hooks into questing/skills systems for cross‑mod reputation gating

Tooling

- Generators for deity/chants/research from minimal prompts (see `tools/`)
- Simple “validate datapack” Gradle task for CI checks

Notes

- Items in the screenshot (CLAUDE.md, CODEX_AGENT_CONFIG.md, CODEX_AGENT_OPTIMAL.md) are preserved as‑is.

Naming & UX

- Transition `tasks` to a more thematic name such as `fate` or `mission`.
- Keep `/eidolon-unchained tasks` as a compatibility alias during migration.

AI-Driven Adventures

- Let AI deities originate adventures/requests (task chains) based on player state.
- Persist per-player history: accepted/completed tasks, outcomes, failures.
- Cross-deity memory: track tasks from rival deities and react (alliance/opposition).
