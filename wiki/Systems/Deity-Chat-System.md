# Deity Chat System

`DeityChat` is the central class for all real-time player-deity conversation. It exposes a static public API used by the fate system, chant effects, prayer flows, and debug commands.

## Public API

All methods are static on `DeityChat`.

### `startConversation(ServerPlayer player, ResourceLocation deityId)`

Opens a new conversation between the player and the specified deity.

Pre-conditions checked in order:
1. Deity must exist in `DatapackDeityManager`.
2. An AI config must exist in `AIDeityManager` for the deity.
3. `AIDeityConfig.canRespondToPlayer(player)` must return true (patron allegiance check).

If the player already has an active conversation, that conversation is ended silently before the new one opens.

On success:
- The player is added to `activeConversations` keyed by UUID.
- `EffigyEffectsManager.startConversationEffects()` is called to trigger effigy visual and audio feedback.
- Subsequent chat messages from the player are routed to the AI deity.

### `endConversation(ServerPlayer player)`

Ends the player's active conversation and sends a farewell message from the deity.

### `endConversationSilent(ServerPlayer player)`

Ends the active conversation without sending a farewell message. Used internally when switching deities.

### `isInConversation(ServerPlayer player)`

Returns `true` if the player currently has an open deity conversation. Used by `FateCompletionMonitor` to decide whether to inject a system message or start a new conversation.

### `processSystemConversation(ServerPlayer player, ResourceLocation deityId, String message, Runnable onComplete)`

Injects a system-side message into the deity conversation flow and invokes `onComplete` when the AI response cycle is done. Used by `FateCompletionMonitor` to deliver fate completion responses through an already-open conversation, or to start a temporary conversation if the player is not currently in one.

## Conversation State

Three in-memory maps track active conversation state per player UUID:

| Map | Key | Value |
|---|---|---|
| `activeConversations` | Player UUID | Current deity `ResourceLocation` |
| `conversationHistory` | Player UUID | List of in-session chat strings |
| `conversationCommandCounts` | Player UUID | Commands executed this session (enforces `max_commands`) |

State is cleared on `endConversation` and `endConversationSilent`.

## Patron Rejection

If `AIDeityConfig.canRespondToPlayer(player)` returns false, a patron-specific rejection message is sent through `sendPatronRejectionMessage(player, deity, aiConfig)`. The rejection message content is driven by the deity's `patron_config` conversation rules.

## ConversationHistoryManager

Long-term conversation history and server-side AI overrides are stored through `ConversationHistoryManager`, a `SavedData` instance written to the world data folder.

- Persists per-player per-deity conversation message history across restarts (default cap: 1,000 messages per deity).
- Stores server-operator-set API keys, per-deity settings, and global AI settings.
- Powers the `EffectiveAIConfig` priority system: server overrides take precedence over JSON defaults.

Only server operators can read or modify `ConversationHistoryManager` data.

## Effigy Effects Integration

`EffigyEffectsManager.startConversationEffects()` is called whenever `startConversation` succeeds. This triggers any configured effigy visual or audio effects tied to the nearest effigy block, independent of whether a physical effigy is required for this conversation.

[Systems](../Systems/) | [Prayer System](Prayer-System.md) | [Patron System](Patron-System.md) | [Fate System](Fate-System.md) | [AI Deity System](AI-Deity-System.md)
