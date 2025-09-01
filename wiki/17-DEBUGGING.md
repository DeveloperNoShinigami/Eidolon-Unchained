# Debugging Guide - Troubleshooting & Testing

This guide covers **debugging tools**, **common issues**, and **testing procedures** for Eidolon Unchained based on actual implementation and real-world usage.

---

## 🔧 Built-in Debug Commands

### AI Provider Testing

#### Test API Connections
```bash
# Test Google Gemini
/eidolon-unchained api test gemini

# Test OpenRouter  
/eidolon-unchained api test openrouter

# Test Player2AI connection
/eidolon-unchained api test player2ai

# Test Player2AI with debug chat
/eidolon-unchained api player2ai debug-chat "Hello, testing connection"
```

**Expected Results:**
- ✅ **Success**: "API connection successful"
- ❌ **Failure**: Specific error message with troubleshooting info

#### Check API Key Status
```bash
# View configured API keys (masked for security)
/eidolon-unchained api status

# Set API keys
/eidolon-unchained api set gemini YOUR_API_KEY
/eidolon-unchained api set openrouter YOUR_API_KEY
/eidolon-unchained api set player2ai local
```

### System Status Commands

#### Overall System Status
```bash
# Complete system overview
/eidolon-unchained status

# Deity-specific debug info
/eidolon-unchained deity debug eidolonunchained:dark_deity

# List all available deities
/eidolon-unchained deity list
```

#### Configuration Management
```bash
# View current configuration
/eidolon-unchained config list

# Set configuration values
/eidolon-unchained config set ai_provider gemini
/eidolon-unchained config set ai_model gemini-1.5-pro
```

### Patron System Debugging

#### Patron Status Commands
```bash
# View current patron status
/eidolon-unchained patron status

# Choose a patron (for testing)
/eidolon-unchained patron choose eidolonunchained:dark_deity

# Abandon current patron  
/eidolon-unchained patron abandon

# Debug patron relationships
/eidolon-unchained patron debug
```

### Research System Testing

#### Research Commands  
```bash
# Clear all research progress (for testing)
/eidolon-unchained research clear

# List available research entries
/eidolon-unchained research list

# Force discover specific research
/eidolon-unchained research discover eidolon:soul_sorcery
```

---

## 🚨 Common Issues & Solutions

### 1. **"AI provider not available"**

#### Symptoms:
- Deity interactions show "does not respond to mortals"
- No AI responses to effigy interactions
- Chat messages are ignored

#### Diagnosis:
```bash
/eidolon-unchained api test gemini
```

#### Solutions:

**Issue**: No API key configured
```bash
# Solution: Set API key
/eidolon-unchained api set gemini YOUR_GEMINI_API_KEY
```

**Issue**: Invalid API key
```bash
# Check API key status
/eidolon-unchained api status

# Reset and reconfigure
/eidolon-unchained api set gemini NEW_API_KEY
```

**Issue**: Network connectivity
- Check internet connection
- Verify firewall settings
- Try different AI provider

### 2. **"Deity does not respond to mortals"**

#### Symptoms:
- Hardcoded fallback message instead of AI response
- Deity shows as loaded but won't engage

#### Diagnosis:
```bash
/eidolon-unchained deity debug eidolonunchained:dark_deity
```

#### Common Causes:

**Missing Chant Sequence**:
```bash
# Must perform chant sequence first:
# Wicked → Wicked → Blood (for dark deity)
# Then right-click effigy
```

**Patron Restrictions**:
- Check if deity requires follower status
- Verify patron allegiance conflicts
- Review `requiresPatronStatus` setting

**API Configuration Issues**:
- AI config failed to load
- Model field errors
- Provider mismatch

### 3. **Player2AI Connection Issues**

#### Symptoms:
- "Connection refused" errors
- Authentication failures
- Inconsistent responses

#### Diagnosis:
```bash
/eidolon-unchained api player2ai debug-chat "test"
```

#### Solutions:

**Player2 App Not Running**:
- Start Player2 application
- Verify ports 4315 (chat) and 4316 (auth) are available
- Check firewall settings

**Port Conflicts**:
```bash
# Check port status (Linux/Mac)
netstat -an | grep 4315
netstat -an | grep 4316

# Windows
netstat -an | findstr 4315
```

**Authentication Setup**:
```bash
# Use local connection
/eidolon-unchained api set player2ai local

# Or configure cloud authentication
/eidolon-unchained api set player2ai YOUR_PLAYER2_API_KEY
```

### 4. **OpenRouter Model Access Issues**

#### Symptoms:
- 404 "No endpoints found matching your data policy"
- Model not available errors

#### Diagnosis:
```bash
/eidolon-unchained api test openrouter
```

#### Solutions:

**Privacy Policy Restrictions**:
- Account configured with "Free model publication" policy
- Blocks access to paid models like Claude, GPT-4

**Fix Options**:

1. **Update Account Settings** (if desired):
   - Visit: https://openrouter.ai/settings/privacy
   - Change to "Default" policy for full model access

2. **Use Free Models** (recommended):
   ```json
   {
     "aiConfig": {
       "aiProvider": "openrouter", 
       "model": "huggingfaceh4/zephyr-7b-beta"
     }
   }
   ```

3. **Test Different Models**:
   ```bash
   # Try these free models:
   # huggingfaceh4/zephyr-7b-beta
   # mistralai/mistral-7b-instruct
   # meta-llama/llama-3.1-8b-instruct
   ```

### 5. **Chant System Issues**

#### Symptoms:
- Chants don't trigger deity conversations
- Signs don't register properly
- Keybinds not working

#### Diagnosis:
```bash
# Check chant registration
/eidolon-unchained status

# Verify keybind assignments  
/eidolon-unchained chant status
```

#### Solutions:

**Chant Registration Failed**:
- Check datapack chant JSON files for errors
- Verify sign sequences are valid
- Ensure proper spell registration

**Keybind Issues**:
- Check key assignments in Options > Controls
- Look for "Eidolon Unchained" category
- Re-assign keys if needed

**Sign Sequence Problems**:
- Must perform exact sequence near effigy
- Hold appropriate wand/staff
- Right-click in air to cast signs

---

## 📊 Log Analysis

### Important Log Files

#### Minecraft Logs
```
# Latest log
.minecraft/logs/latest.log

# Debug log (if enabled)
.minecraft/logs/debug.log
```

#### Key Log Patterns

**Successful AI Configuration Loading**:
```
[INFO] [eidolonunchained] Loading AI deity configurations...
[INFO] [eidolonunchained] Loaded AI configuration for deity: eidolonunchained:dark_deity
[INFO] [eidolonunchained] Queued 3 AI deity configurations with 0 errors
```

**API Connection Errors**:
```
[ERROR] [eidolonunchained] Gemini API request failed with status 401: Invalid API key
[ERROR] [eidolonunchained] OpenRouter API request failed with status 404: No endpoints found
[ERROR] [eidolonunchained] Player2AI connection failed: Connection refused
```

**Configuration Issues**:
```
[ERROR] [eidolonunchained] Failed to load AI config from: missing model field
[WARN] [eidolonunchained] AI provider 'unknown' not recognized, falling back to Gemini
```

### Log Debugging Commands

#### Enable Debug Logging
```bash
# Set log level to DEBUG in logging configuration
# Edit: .minecraft/config/forge-client.toml
# Set: logging.level.com.bluelotuscoding.eidolonunchained = "DEBUG"
```

#### Filter Relevant Logs
```bash
# Linux/Mac
grep "eidolonunchained" ~/.minecraft/logs/latest.log

# Windows (PowerShell)
Select-String "eidolonunchained" ~/.minecraft/logs/latest.log
```

---

## 🧪 Testing Procedures

### 1. **Fresh Installation Test**

#### Setup:
1. Clean Minecraft instance
2. Install Eidolon + Eidolon Unchained
3. Start game, generate configs

#### Test Sequence:
```bash
# 1. Configure API
/eidolon-unchained api set gemini YOUR_KEY

# 2. Test connection
/eidolon-unchained api test gemini

# 3. Find/place effigy
# 4. Perform chant: Wicked → Wicked → Blood

# 5. Start conversation
# Right-click effigy, type message

# 6. Verify response
# Should see AI deity response
```

### 2. **Multi-Provider Test**

#### Test All Providers:
```bash
# Configure each provider
/eidolon-unchained api set gemini YOUR_GEMINI_KEY
/eidolon-unchained api set openrouter YOUR_OPENROUTER_KEY  
/eidolon-unchained api set player2ai local

# Test each
/eidolon-unchained api test gemini
/eidolon-unchained api test openrouter
/eidolon-unchained api test player2ai

# Create deities using different providers
# Test conversations with each
```

### 3. **Patron System Test**

#### Test Sequence:
```bash
# 1. Start with no patron
/eidolon-unchained patron status

# 2. Choose patron
/eidolon-unchained patron choose eidolonunchained:dark_deity

# 3. Test follower interactions
# Should receive warmer responses

# 4. Choose opposing patron
/eidolon-unchained patron choose eidolonunchained:light_deity

# 5. Test enemy interactions  
# Dark deity should be hostile

# 6. Test progression
# Gain reputation, verify title changes
```

### 4. **Performance Testing**

#### Load Testing:
```bash
# Rapid conversation test
# Send multiple messages quickly
# Monitor for memory leaks or slowdowns

# Long conversation test  
# Sustained 10-15 message conversation
# Check response times remain consistent

# Multiple deity test
# Switch between different deities
# Verify no cross-contamination
```

---

## 🔍 Advanced Debugging

### Custom Debug Commands

#### Enable Enhanced Logging:
```java
// Add to config
debug.enableAIDeityLogging = true
debug.enableConversationHistoryLogging = true
debug.enablePatronSystemLogging = true
```

#### Network Debugging:
```bash
# Monitor network traffic (advanced)
# Use tools like Wireshark to monitor API calls
# Check for SSL/TLS issues
```

### Configuration File Debugging

#### Validate JSON Files:
```bash
# Use JSON validator
# Check syntax errors in deity configurations
# Verify field names match expected structure
```

#### Test Configuration Loading:
```bash
# Force reload datapacks
/reload

# Check for loading errors in logs
# Verify all expected deities loaded
```

---

## 📋 Debugging Checklist

### Quick Diagnostic Steps:

#### ✅ **Basic Checks**
- [ ] API key configured and valid
- [ ] Internet connectivity working
- [ ] Eidolon mod loaded and functional
- [ ] Effigy placed and accessible

#### ✅ **AI System Checks**  
- [ ] Deity configurations loaded successfully
- [ ] AI provider test passes
- [ ] Chant sequence performed correctly
- [ ] No patron conflicts preventing interaction

#### ✅ **Advanced Checks**
- [ ] Log files show no errors
- [ ] Model access permissions (OpenRouter)
- [ ] Local app running (Player2AI)
- [ ] Memory and performance normal

---

**🎯 Still having issues?** Check the **[Command System Guide](12-COMMANDS.md)** for more administrative tools and debugging commands!
