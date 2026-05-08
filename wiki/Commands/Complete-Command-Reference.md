# Complete Command Reference

All commands are available under `/eidolon-unchained` (alias `/eu`).
Arguments shown in `<angle brackets>` are required; `[square brackets]` are optional.
All ID arguments support tab-completion.

---

## Config

```
/eu config reload
/eu config status
/eu config validate
/eu config reset
```

---

## API & Provider Setup

```
/eu api set player2ai
/eu api set <provider> <key>
/eu api set-model <model>
/eu api get-model
/eu api test <provider>
/eu api list
/eu api remove <provider>
```

Providers: `gemini`, `openrouter`, `player2ai`, `openai`, `anthropic`

---

## Deities

```
/eu deities list
/eu deities reload
/eu deities status <deity>
```

---

## Patron

```
/eu patron choose <deity>
/eu patron choose <player> <deity>
/eu patron abandon
/eu patron status
```

---

## Prayers

```
/eu prayers history
/eu prayers cooldowns
/eu prayers clear-cooldown <player>    (op)
```

---

## Chat  *(direct AI test without effigy)*

Send a message directly to a deity's AI. Bypasses the effigy-proximity and patron-allegiance checks — useful for testing AI responses and datapack personalities.

```
/eu chat <deity> <message>
```

Example: `/eu chat eidolonunchained:nature_deity Hello, are you there?`

The response comes back through the normal `DeityChat` pipeline (full personality, conversation history, command execution) and is displayed in chat.

---

## Fates  *(replaces the old `tasks` tree)*

All arguments have tab-completion suggestions.

```
/eu fates assign <player> <deity> <fateId>
/eu fates assignany <player> <fateId>
/eu fates complete <player> <fateId>
/eu fates list <player>
/eu fates reputation <player> <deity>
/eu fates repall <player>
/eu fates ritual <player> <ritualId>
```

---

## Conversations

```
/eu conversations stats
/eu conversations clear <deity>
/eu conversations clear-all
```

---

## Research

```
/eu research list
/eu research reload    (op)
/eu research clear <player>    (op)
```

---

## Player2AI

```
/eu player2ai auth auto
/eu player2ai login device
/eu player2ai login status
/eu player2ai logout
/eu player2ai test
/eu player2ai debug-chat <message>
/eu player2ai memory clear <deity>
/eu player2ai memory show <deity>
/eu player2ai characters list
/eu player2ai characters update-personality <deity>
```

`debug-chat` sends a raw message to the Player2AI service with a generic test personality (no deity config needed) — useful for verifying the Player2AI connection is working at the API level.

---

## Chant

Managed by `ChantSlotCommands`. See the [Chant system docs](../Systems/) for details.

---

## TTS

```
/eu tts enable
/eu tts disable
/eu tts status
/eu tts test <text>
/eu tts test <deity> <text>
/eu tts funding player-only
/eu tts funding server-only
/eu tts funding player-first
/eu tts voice <voice>
/eu tts voice auto
/eu tts volume <value>
/eu tts speed <value>
```

---

## Debug  *(op only — requires permission level 2)*

### AI

```
/eu debug ai context <player> <deity>
/eu debug ai test <player> <deity> <prompt>
/eu debug ai world-knowledge
/eu debug ai item-search <item>
/eu debug ai deity-context <deity>
/eu debug ai registry-stats
```

### Ritual

```
/eu debug ritual list
/eu debug ritual test <ritualId>
/eu debug ritual diagnose
/eu debug ritual diagnose <pos>
```

### Reputation & Progression

```
/eu debug reputation <player> <deity>
/eu debug progression <player> <deity>
/eu debug force-progression <player> <deity>
/eu debug tier <player> <deity>
```

### Facts

```
/eu debug facts grant <player> <fact>
/eu debug facts revoke <player> <fact>
/eu debug facts list <player>
```

### Rewards

```
/eu debug clear-rewards <player>
/eu debug clear-rewards <player> <deity>
```

---

## Removed Commands

The following are no longer registered:

| Old command | Replacement |
|---|---|
| `/eu tasks ...` | `/eu fates ...` |
| `/eu ai-debug ...` | `/eu debug ai ...` |
| `/eu ritual-diagnose <pos>` | `/eu debug ritual diagnose <pos>` |
| `/eu api retry ...` | Edit `eidolonunchained-common.toml` directly |
| `/eu patron tier-debug ...` | `/eu debug tier <player> <deity>` |
| `/eu patron facts ...` | `/eu debug facts ...` |
