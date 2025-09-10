# Eidolon Unchained: Critical Security & Refactoring Plan

## ⚠️ CRITICAL STATUS: IMMEDIATE ACTION REQUIRED

This codebase contains **CRITICAL SECURITY VULNERABILITIES** that make it unsafe for production use. The issues range from command injection attacks to API key exposure. This document provides a step-by-step remediation plan.

---

## 🚨 PHASE 1: IMMEDIATE SECURITY FIXES (DO FIRST)

### 1.1 Remove API Key Exposure (CRITICAL - 30 minutes)

**Files to fix:**
- `src/main/java/com/bluelotuscoding/eidolonunchained/config/APIKeyManager.java`

**Actions:**
1. **Remove lines 28, 40-50**: Delete ALL `System.out.println()` statements that log API keys
2. **Replace with proper logging**: Use logger with masked output
3. **Remove lines 45-48**: Delete the property iteration that prints all keys/values

**Why:** API keys are currently being logged to console/files, exposing them to anyone with log access.

**Code to remove:**
```java
// DELETE THESE LINES:
System.out.println("API key present: " + (apiKey != null));
System.out.println("API key length: " + (apiKey != null ? apiKey.length() : 0));
// And any other System.out.println containing sensitive data
```

### 1.2 Fix Command Injection Vulnerability (CRITICAL - 2 hours)

**Files to fix:**
- `src/main/java/com/bluelotuscoding/eidolonunchained/integration/ai/EnhancedCommandExtractor.java`

**Actions:**
1. **Replace lines 284-285**: Current validation only checks for ".." and "//" - this is EASILY bypassed
2. **Implement whitelist validation**: Only allow specific, safe commands
3. **Add command argument sanitization**: Strip dangerous characters
4. **Lines 476-544**: Add proper validation before executing ANY AI-generated command

**Why:** AI responses can contain malicious commands that will be executed with server privileges. This is a **REMOTE CODE EXECUTION** vulnerability.

**Required changes:**
```java
// REPLACE the current weak validation with:
private static final Set<String> ALLOWED_COMMANDS = Set.of(
    "give", "effect", "tellraw", "playsound", "particle"
);

private boolean isCommandSafe(String command) {
    String[] parts = command.trim().split(" ");
    if (parts.length == 0) return false;
    
    String baseCommand = parts[0].toLowerCase();
    if (!ALLOWED_COMMANDS.contains(baseCommand)) {
        LOGGER.warn("Blocked dangerous command: " + baseCommand);
        return false;
    }
    
    // Add argument sanitization here
    return sanitizeCommandArguments(command);
}
```

### 1.3 Remove All Debug Output (CRITICAL - 1 hour)

**Files containing System.out.println():**
- `PlayerChantCasterRenderer.java` (lines 49, 58, 63)
- `PlayerChantRenderer.java` (line 49)
- `PlayerChantingSystem.java` (line 420)
- `APIKeyManager.java` (multiple locations)
- Search entire codebase for `System.out.println` and remove ALL instances

**Actions:**
1. **Delete ALL `System.out.println()` statements**
2. **Replace with proper logger calls where necessary**
3. **Use conditional debug logging**: `if (DEBUG_MODE) LOGGER.debug(...)`

**Why:** Debug output can leak sensitive information and impacts performance.

---

## 🔧 PHASE 2: ARCHITECTURAL FIXES (STABILITY)

### 2.1 Fix Memory Leaks (HIGH - 1 hour)

**File:** `EnhancedCommandExtractor.java` lines 62-65

**Problem:** Static maps store player data but never clean up on disconnect
```java
// CURRENT PROBLEMATIC CODE:
private static final Map<UUID, SomeData> playerData = new HashMap<>();
```

**Solution:**
1. **Add player disconnect handler**
2. **Implement automatic cleanup**
3. **Use WeakHashMap or implement proper lifecycle management**

**Implementation:**
```java
@SubscribeEvent
public static void onPlayerDisconnect(PlayerEvent.PlayerLoggedOutEvent event) {
    UUID playerId = event.getEntity().getUUID();
    playerData.remove(playerId);
    // Clean up other static collections
}
```

### 2.2 Fix Thread Safety Issues (HIGH - 2 hours)

**Files:** `DatapackChantManager.java` lines 57-59, 174-200

**Problem:** Static collections modified without synchronization

**Solution:**
1. **Replace HashMap with ConcurrentHashMap**
2. **Add synchronized blocks for complex operations**
3. **Use concurrent collections consistently**

### 2.3 Improve Error Handling (MEDIUM - 3 hours)

**Problem:** Inconsistent error handling across the codebase

**Files to standardize:**
- `DatapackChantManager.java` lines 96-100
- `GeminiAPIClient.java` lines 112-131
- `APIKeyManager.java` lines 70-75

**Actions:**
1. **Create unified exception hierarchy**
2. **Standardize error response patterns**
3. **Add proper logging levels**
4. **Remove generic catch blocks**

---

## 📋 PHASE 3: CODE QUALITY IMPROVEMENTS

### 3.1 Break Down God Classes (MEDIUM - 4 hours)

**Target files:**
- `EidolonUnchainedConfig.java` (582 lines)
- `EnhancedCommandExtractor.java` (1421 lines)

**Actions:**
1. **Split EidolonUnchainedConfig into feature-specific configs:**
   - `AIConfig.java`
   - `ChantConfig.java`
   - `SecurityConfig.java`
   - `NetworkConfig.java`

2. **Split EnhancedCommandExtractor into:**
   - `CommandValidator.java`
   - `CommandExecutor.java`
   - `AIResponseParser.java`

### 3.2 Add Proper Resource Management (MEDIUM - 2 hours)

**Files:** `GeminiAPIClient.java` lines 232-312

**Problem:** HTTP connections may not be properly closed

**Solution:**
```java
// Use try-with-resources for ALL network operations
try (CloseableHttpClient client = HttpClients.createDefault();
     CloseableHttpResponse response = client.execute(request)) {
    // Process response
} catch (IOException e) {
    // Handle error
}
```

### 3.3 Add Input Validation (MEDIUM - 3 hours)

**All AI processing files need:**
1. **JSON schema validation**
2. **Input length limits**
3. **Character filtering**
4. **Command argument validation**

---

## 🎯 PHASE 4: SECURITY HARDENING

### 4.1 Implement Security Policies (HIGH - 2 hours)

**Create new file:** `SecurityPolicy.java`

**Required features:**
1. **Command execution limits per player**
2. **Rate limiting for AI requests**
3. **Privilege escalation prevention**
4. **Audit logging for all AI commands**

### 4.2 Add Configuration Validation (MEDIUM - 2 hours)

**Files:** All datapack JSON files

**Actions:**
1. **Create JSON schemas for all datapack content**
2. **Add validation on load**
3. **Reject invalid configurations with clear error messages**

### 4.3 Implement Proper Logging (MEDIUM - 1 hour)

**Replace all System.out with:**
```java
private static final Logger LOGGER = LoggerFactory.getLogger(ClassName.class);

// Use appropriate levels:
LOGGER.error("Critical errors");
LOGGER.warn("Security violations");
LOGGER.info("Important operations");
LOGGER.debug("Debugging info - only in debug mode");
```

---

## 🧪 PHASE 5: TESTING & VALIDATION

### 5.1 Security Testing (CRITICAL - 2 hours)

**Required tests:**
1. **Command injection testing**: Try to inject malicious commands through AI
2. **API key exposure testing**: Verify no keys appear in logs
3. **Rate limiting testing**: Verify limits are enforced
4. **Resource cleanup testing**: Verify no memory leaks

### 5.2 Integration Testing (HIGH - 2 hours)

**Test scenarios:**
1. **AI provider failure handling**
2. **Malformed datapack handling**
3. **Player disconnect cleanup**
4. **Concurrent access to shared resources**

---

## 📊 PRIORITY MATRIX

| Priority | Time Required | Risk Level | Impact |
|----------|---------------|------------|---------|
| **Phase 1** | 3.5 hours | CRITICAL | Security |
| **Phase 2** | 6 hours | HIGH | Stability |
| **Phase 3** | 9 hours | MEDIUM | Maintainability |
| **Phase 4** | 5 hours | HIGH | Security |
| **Phase 5** | 4 hours | HIGH | Validation |

**Total estimated time: 27.5 hours**

---

## 🚨 BRUTAL HONESTY SECTION

### Current State Assessment:
- **Security Rating: 2/10** - Critical vulnerabilities present
- **Code Quality: 4/10** - Functional but messy
- **Architecture: 5/10** - Working but poorly structured
- **Maintainability: 3/10** - Hard to modify safely

### Reality Check:
1. **This code should NOT be used in production** without Phase 1 fixes
2. **The command injection vulnerability is SEVERE** - it allows remote code execution
3. **API key exposure could lead to financial loss** if using paid AI services
4. **The architecture will become unmaintainable** without refactoring

### What Works Well:
- Minecraft integration is solid
- Datapack system is well-designed
- Translation system is comprehensive
- Build configuration is correct

### What's Broken:
- Security is fundamentally compromised
- Error handling is inconsistent
- Resource management has leaks
- Code organization is poor

---

## 🎯 SUCCESS CRITERIA

### Phase 1 Complete When:
- [ ] No API keys appear in any logs
- [ ] All AI commands are validated against whitelist
- [ ] No System.out.println statements remain
- [ ] Security scan shows no critical vulnerabilities

### Phase 2 Complete When:
- [ ] No memory leaks in player data
- [ ] Thread safety verified with concurrent testing
- [ ] Error handling is consistent across all modules
- [ ] Resource cleanup is automatic

### Phase 3 Complete When:
- [ ] No class exceeds 300 lines
- [ ] All network resources use try-with-resources
- [ ] Input validation covers all user inputs
- [ ] Code passes quality analysis tools

### Phase 4 Complete When:
- [ ] Security policies are enforced
- [ ] All configurations are validated
- [ ] Audit logging captures security events
- [ ] Rate limiting prevents abuse

### Phase 5 Complete When:
- [ ] Security tests pass
- [ ] Integration tests cover failure scenarios
- [ ] Performance tests show no degradation
- [ ] Documentation is updated

---

## 🔧 AGENT INSTRUCTIONS

**For the agent fixing this codebase:**

1. **START WITH PHASE 1** - Do not proceed to other phases until security is fixed
2. **Test each fix immediately** - Don't accumulate broken code
3. **Back up original files** before making changes
4. **Use proper Git commits** for each logical change
5. **Add TODO comments** for any temporary workarounds
6. **Document all security decisions** in code comments
7. **Run security scans** after each phase
8. **Verify functionality** after each major change

**Do NOT:**
- Skip security fixes to work on "more interesting" features
- Introduce new dependencies without security review
- Leave temporary debugging code in place
- Assume any user input is safe
- Trust AI responses without validation

**This is a security-critical system that handles user input and executes commands. Treat it accordingly.**