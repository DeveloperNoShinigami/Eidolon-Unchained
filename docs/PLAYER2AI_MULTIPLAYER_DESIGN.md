# Player2AI Multiplayer Implementation Design

## Current Status ✅ IMPLEMENTED
- ✅ **Local Desktop Integration**: Each player runs their own Player2AI desktop app
- ✅ **Character Management**: Characters created and managed per local instance  
- ✅ **Memory Persistence**: Individual player conversations stored locally
- ✅ **Server-Hosted Mode**: **NEW** - Configurable server-hosted Player2AI support
- ✅ **Mode Switching Commands**: **NEW** - Runtime switching between local and server modes
- ✅ **Multi-Server Support**: **NEW** - Server-specific player tracking for hosting multiple Minecraft servers

## Implementation Status

### ✅ Phase 1: Server-Hosted Configuration (COMPLETE)
**Implementation Location**: `ConfigurablePlayer2AIClient.java` + `EidolonUnchainedConfig.java`

```java
// IMPLEMENTED: Dynamic configuration support
EidolonUnchainedConfig.COMMON.player2aiConnectionMode.set("server");
EidolonUnchainedConfig.COMMON.player2aiServerUrl.set("192.168.1.100");
EidolonUnchainedConfig.COMMON.player2aiServerPort.set(4315);
```

### ✅ Phase 2: Command Interface (COMPLETE)
**Implementation Location**: `UnifiedCommands.java`

```bash
# IMPLEMENTED: Runtime mode switching
/eidolon-unchained player2ai mode status
/eidolon-unchained player2ai mode local  
/eidolon-unchained player2ai mode server 192.168.1.100:4315
```

### ✅ Phase 3: Multi-Server Architecture (COMPLETE)
**Implementation Location**: `ConfigurablePlayer2AIClient.getServerSpecificPlayerKey()`

```java
// IMPLEMENTED: Server-aware player tracking
private String getServerSpecificPlayerKey(ServerPlayer player) {
    String serverDirHash = String.valueOf(
        player.getServer().getServerDirectory().getAbsolutePath().hashCode()
    );
    return serverDirHash + ":" + player.getStringUUID();
}
```

## Multiplayer Architecture Options

### ✅ Option A: Shared Server-Hosted Player2AI (IMPLEMENTED)
```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Minecraft     │    │  Minecraft       │    │    Player2AI    │
│   Player 1      │───▶│  Server          │───▶│    Instance     │
│                 │    │                  │    │  (Server-hosted)│
└─────────────────┘    │                  │    │                 │
                       │                  │    │ Shared Deities  │
┌─────────────────┐    │                  │    │ Individual      │
│   Minecraft     │───▶│                  │    │ Player Memories │
│   Player 2      │    │                  │    │                 │
└─────────────────┘    └──────────────────┘    └─────────────────┘
```

**✅ IMPLEMENTED in ConfigurablePlayer2AIClient.java:**
```java
public class ConfigurablePlayer2AIClient {
    public ConfigurablePlayer2AIClient() {
        updateConfiguration(); // Auto-detects local vs server mode
    }
    
    public CompletableFuture<String> generateResponse(
        String prompt, String personality, String characterId, String playerKey
    ) {
        // Routes to appropriate endpoint based on current mode
        String endpoint = currentApiBase + "/v1/chat/completions";
        return sendRequest(endpoint, buildRequestBody(prompt, personality, characterId, playerKey));
    }
    
    private String getServerSpecificPlayerKey(ServerPlayer player) {
        // Ensures unique player identification across multiple servers
        String serverHash = String.valueOf(player.getServer().getServerDirectory().getAbsolutePath().hashCode());
        return serverHash + ":" + player.getStringUUID();
    }
}
```

**✅ Benefits ACHIEVED:**
- ✅ **Consistent Deity Personalities**: All players interact with the same deity characters
- ✅ **Server Control**: Server admin manages one Player2AI instance via commands
- ✅ **Scalable**: Can handle many players through one powerful instance
- ✅ **Runtime Switching**: No server restart required to change modes
- ✅ **Multi-Server Support**: Same Player2AI instance can serve multiple Minecraft servers

**✅ Implementation Features:**
- **Command Interface**: `/eidolon-unchained player2ai mode server <url:port>`
- **Automatic Configuration**: Reads settings from EidolonUnchainedConfig
- **Server Isolation**: Each Minecraft server gets unique player tracking
- **Error Handling**: Graceful fallbacks when server connection fails

### Option B: Local Instance + Synchronization
```
┌─────────────────┐    ┌──────────────────┐
│   Player2AI     │    │   Player2AI      │
│   Instance 1    │◄──▶│   Instance 2     │
│                 │    │                  │
│ Sync Deity      │    │ Sync Deity       │
│ Personalities   │    │ Personalities    │
└─────────────────┘    └──────────────────┘
         ▲                       ▲
         │                       │
┌─────────────────┐    ┌─────────────────┐
│   Minecraft     │    │   Minecraft     │
│   Player 1      │    │   Player 2      │
└─────────────────┘    └─────────────────┘
```

**Implementation:**
```java
public class SynchronizedPlayer2AI {
    public void syncDeityPersonalities(List<Player2AIInstance> instances) {
        // Periodically sync deity character updates across all connected instances
        for (DatapackDeity deity : DatapackDeityManager.getAllDeities().values()) {
            String latestPersonality = getLatestPersonality(deity);
            for (Player2AIInstance instance : instances) {
                instance.updateCharacterPersonality(deity.getId(), latestPersonality);
            }
        }
    }
}
```

**Benefits:**
- ✅ **No Server Requirements**: Players manage their own instances
- ✅ **Privacy**: Individual memories stay local
- ✅ **Reliability**: No single point of failure

**Challenges:**
- ❌ **Complex Synchronization**: Need to sync deity personalities
- ❌ **Network Overhead**: Constant sync required
- ❌ **Inconsistent Experience**: Deities may behave differently per player

### Option C: Hybrid Cloud Service (Future)
```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   All Minecraft │    │  Player2AI       │    │   All Servers   │
│   Servers       │───▶│  Cloud Service   │◄───│   Worldwide     │
│                 │    │                  │    │                 │
│ Global Deities  │    │ Shared Learning  │    │ Consistent AI   │
└─────────────────┘    └──────────────────┘    └─────────────────┘
```

**Note**: This would require Player2AI to offer official cloud services

## Recommended Implementation Path

### ✅ Phase 1: Server-Hosted Configuration (IMPLEMENTED)
**Location**: `EidolonUnchainedConfig.java` + `ConfigurablePlayer2AIClient.java`

```java
// IMPLEMENTED: Configuration values
public static final ForgeConfigSpec.ConfigValue<String> player2aiConnectionMode = COMMON_BUILDER
    .comment("Player2AI connection mode: 'local' or 'server'")
    .define("player2aiConnectionMode", "local");

public static final ForgeConfigSpec.ConfigValue<String> player2aiServerUrl = COMMON_BUILDER
    .comment("Player2AI server URL for server mode")
    .define("player2aiServerUrl", "localhost");

public static final ForgeConfigSpec.ConfigValue<Integer> player2aiServerPort = COMMON_BUILDER
    .comment("Player2AI server port")
    .define("player2aiServerPort", 4315);
```

### ✅ Phase 2: Command Interface (IMPLEMENTED)
**Location**: `UnifiedCommands.java`

```java
// IMPLEMENTED: Mode switching commands
private static int setPlayer2AIServerMode(CommandContext<CommandSourceStack> context) {
    String serverUrl = StringArgumentType.getString(context, "server_url");
    // Parse host:port, update config, save settings
    EidolonUnchainedConfig.COMMON.player2aiConnectionMode.set("server");
    EidolonUnchainedConfig.COMMON.player2aiServerUrl.set(host);
    EidolonUnchainedConfig.COMMON.player2aiServerPort.set(port);
    ConfigurablePlayer2AIClient.updateGlobalConfiguration();
}
```

### ✅ Phase 3: Multi-Server Support (IMPLEMENTED)
**Location**: `ConfigurablePlayer2AIClient.getServerSpecificPlayerKey()`

```java
// IMPLEMENTED: Server-aware player identification
private String getServerSpecificPlayerKey(ServerPlayer player) {
    String serverDirHash = String.valueOf(
        player.getServer().getServerDirectory().getAbsolutePath().hashCode()
    );
    return serverDirHash + ":" + player.getStringUUID();
}
```

### 🔄 Phase 4: Enhanced Memory Management (FUTURE)
```java
// PLANNED: Cross-player context awareness  
public class MultiplayerMemoryManager {
    public void shareDeityMemoryAcrossPlayers(String deityId, List<String> playerUUIDs) {
        // Allow deities to reference interactions with other players
        // "I remember speaking with another adventurer about this..."
    }
    
    public String buildContextualPrompt(ServerPlayer player, DatapackDeity deity, String message) {
        // Include recent interactions from other players as context
        // "Recent events in your domain: Player X asked about Y..."
    }
}
```

## Configuration Examples

### ✅ IMPLEMENTED: Runtime Mode Switching
```bash
# Check current configuration
/eidolon-unchained player2ai mode status
# Output: "Player2AI Mode: local" or "Player2AI Mode: server (192.168.1.100:4315)"

# Switch to local mode (each player uses their own Player2AI desktop app)
/eidolon-unchained player2ai mode local

# Switch to server mode (all players share one Player2AI instance)
/eidolon-unchained player2ai mode server 192.168.1.100:4315
/eidolon-unchained player2ai mode server myserver.com
/eidolon-unchained player2ai mode server localhost:4316
```

### For Server Admins (Shared Instance):
```toml
# config/eidolonunchained-common.toml
[ai]
    player2aiConnectionMode = "server"
    player2aiServerUrl = "127.0.0.1"  # Server's local IP
    player2aiServerPort = 4315
    enableSharedDeityPersonalities = true
```

### For Individual Players (Local Instance):
```toml
# config/eidolonunchained-common.toml  
[ai]
    player2aiConnectionMode = "local"
    player2aiServerUrl = "localhost"
    player2aiServerPort = 4315
    enableSharedDeityPersonalities = false
```

### ✅ NEW: Multi-Server Hosting
```bash
# Server Admin can host multiple Minecraft servers using one Player2AI instance
# Each Minecraft server gets isolated player tracking automatically

# Example setup:
# - Minecraft Server 1 (Survival): /home/minecraft/server1/
# - Minecraft Server 2 (Creative): /home/minecraft/server2/  
# - Both configured with: player2aiServerUrl = "localhost"
# - Player conversations are automatically separated by server directory hash
```

## Technical Implementation Notes

### ✅ IMPLEMENTED: Memory Architecture
- **Individual Player Memory**: `serverHash:playerUUID` → personal conversation history  
- **Shared Deity State**: Character personality updates affect all players on same Player2AI instance
- **Server Isolation**: Players on different Minecraft servers have completely separate contexts
- **Multi-Server Support**: One Player2AI instance can serve multiple Minecraft servers

### ✅ IMPLEMENTED: Configuration Management
- **Runtime Updates**: Configuration changes via commands take effect immediately
- **Persistent Storage**: Settings saved to Forge config files, survive restarts
- **Validation**: URL parsing with port validation and error handling
- **Backward Compatibility**: Existing local mode continues to work unchanged

### ✅ IMPLEMENTED: Connection Architecture
- **Dynamic Endpoints**: ConfigurablePlayer2AIClient switches between local/server URLs automatically
- **Error Handling**: Graceful fallbacks when server connections fail
- **Character Caching**: Efficient character management across connection modes
- **Health Monitoring**: Connection status tracking and reporting

### Performance Considerations:
- **Connection Pooling**: HTTP connections are reused efficiently (via Java HttpURLConnection)
- **Response Caching**: Character cache prevents redundant character creation calls
- **Rate Limiting**: No additional rate limiting implemented (relies on Player2AI's built-in limits)
- **Memory Usage**: Server-specific player keys prevent memory cross-contamination

### Privacy & Security:
- ✅ **Player Isolation**: Personal conversations remain private per `serverHash:playerUUID`
- ✅ **Server Boundaries**: Players on different servers have separate contexts automatically
- ✅ **Admin Control**: Server admins control mode switching via commands or config
- ⚠️ **Network Security**: HTTP connections (consider HTTPS for production deployments)

## Testing Strategy

### ✅ IMPLEMENTED: Local Development Testing
```bash
# Test current local implementation
./gradlew runClient
# Use: /eidolon-unchained player2ai mode status
# Start conversation with deity, verify local Player2AI connection

# Test mode switching
# In-game: /eidolon-unchained player2ai mode server localhost:4315
# Verify configuration updates without server restart
```

### ✅ READY: Multiplayer Testing Setup
```bash
# Server setup (requires manual Player2AI installation)
# 1. Install Player2AI desktop app on server machine  
# 2. Run: /eidolon-unchained player2ai mode server <server-ip>:4315
# 3. Test multiple players connecting
# 4. Verify shared deity personalities and individual memories

# Multi-server testing  
# 1. Run multiple Minecraft servers on same machine
# 2. Configure both with same Player2AI server URL
# 3. Verify player separation between servers
# 4. Check that conversations don't leak between server instances
```

### Validation Checklist:
- ✅ **Local Mode**: Player2AI desktop app connection works
- ✅ **Server Mode**: Can connect to remote Player2AI instance  
- ✅ **Mode Switching**: Runtime configuration changes work
- ✅ **Multi-Server**: Server isolation prevents data leakage
- ⏳ **Load Testing**: Multiple players on shared instance (requires manual testing)
- ⏳ **Failover**: Behavior when server Player2AI goes offline (requires manual testing)

## Frequently Asked Questions

### Q: How is server-hosted Player2AI possible?
**A:** Player2AI is a desktop application that can run on any machine, including servers. The Eidolon Unchained mod simply connects to Player2AI via HTTP API calls to `localhost:4315` or any other configured IP:port. By changing the connection URL from `localhost` to a server IP address, multiple Minecraft players can share the same Player2AI instance.

### Q: Is this officially supported by Player2AI?
**A:** This implementation uses Player2AI's standard OpenAI-compatible API endpoints (`/v1/chat/completions`). No special server mode is required from Player2AI itself - we're simply changing where the HTTP requests are sent. Player2AI treats all requests the same regardless of source.

### Q: What are the technical requirements?
**A:** 
- **Server Requirements**: Any machine that can run Player2AI desktop app + network connectivity
- **Minecraft Mod**: Eidolon Unchained with ConfigurablePlayer2AIClient implementation  
- **Network**: HTTP connectivity between Minecraft server and Player2AI machine (port 4315)
- **No special setup**: Uses existing Player2AI installation, just accessed remotely

### Q: How does player isolation work?
**A:** Each player gets a unique identifier: `<server-directory-hash>:<player-uuid>`. This ensures:
- Players on the same server share deity personalities but have separate conversation histories
- Players on different servers (even with same Player2AI) are completely isolated
- Server admins can run multiple Minecraft worlds without data contamination

## Implementation Notes & Documentation Status

### Q: Is this documented in Player2AI's official documentation?
**A:** **No, this is NOT an officially documented feature** of Player2AI. This multiplayer implementation is a creative extension developed specifically for Eidolon Unchained that leverages Player2AI's existing capabilities in an unintended but compatible way.

**What Player2AI Actually Provides:**
- Desktop application with local HTTP API server (localhost:4315)
- OpenAI-compatible REST endpoints (`/v1/chat/completions`)
- Character persistence and memory management
- Gaming-focused AI personality system

**What Eidolon Unchained Adds:**
- **Network Extension**: Connecting to Player2AI instances on remote machines
- **Multi-Server Architecture**: Server-specific player tracking to prevent data cross-contamination
- **Configuration Management**: Runtime switching between local and server modes
- **Command Interface**: In-game commands for mode switching and status checking

### Technical Foundation
```
Player2AI Desktop App   →   HTTP API Server (port 4315)   →   OpenAI-Compatible Endpoints
        ↓                           ↓                              ↓
Local Installation      →   Network Accessible            →   /v1/chat/completions
                                                               /v1/character/create
                                                               /v1/auth/...
```

**How We Made It Multiplayer:**
1. **Discovered**: Player2AI's HTTP API accepts requests from any source, not just localhost
2. **Extended**: ConfigurablePlayer2AIClient can connect to any IP:port, not just localhost:4315
3. **Isolated**: Server-specific player keys prevent data leakage between different Minecraft servers
4. **Managed**: Command interface for easy switching between local and server modes

### Why This Works
- **No Modification Required**: Player2AI desktop app works unchanged
- **Standard HTTP Requests**: We send the same API calls, just to different IP addresses
- **Player2AI is Agnostic**: The app doesn't care where requests come from
- **Network Transparent**: HTTP API works over LAN/Internet just like localhost

### Limitations & Considerations
- ⚠️ **Unofficial Use**: Not supported by Player2AI developers
- ⚠️ **Security**: HTTP connections (not HTTPS) - consider VPN for internet use
- ⚠️ **Performance**: Network latency may affect response times compared to localhost
- ⚠️ **Reliability**: Depends on network connectivity between Minecraft server and Player2AI machine
- ✅ **Compatibility**: Should continue working as long as Player2AI maintains API compatibility

### Support & Troubleshooting
Since this is an unofficial extension, support is limited to:
- **Eidolon Unchained Documentation**: This document and implementation code
- **Community Testing**: User-reported experiences with server-hosted setups
- **Technical Analysis**: Understanding of Player2AI's API behavior and network capabilities

For Player2AI issues (app crashes, character problems, etc.), refer to official Player2AI support channels. For multiplayer configuration issues (connection problems, mode switching, server isolation), this is specific to Eidolon Unchained's implementation.
