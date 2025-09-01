# 🤖 AI Deity Configuration Guide

This mod includes a powerful AI deity system that allows players to have dynamic conversations with mystical beings through chat and chant-based interactions.

## 📚 **Setup Guides**

- **[Quick Start Guide](QUICK_START.md)** - Get up and running in 5 minutes
- **[Complete API Key Setup](API_KEY_SETUP.md)** - Comprehensive configuration guide
- **[Deity Examples](../DEITY_EXAMPLES.md)** - See example deity configurations

## 🎯 **Quick Setup (Recommended)**

### For Server Operators (Easiest)
1. Get a Google Gemini API key from [Google AI Studio](https://makersuite.google.com/app/apikey)
2. In-game, run: `/eidolon-config quick-setup gemini YOUR_API_KEY_HERE`
3. Test with: `/eidolon-config test gemini`
4. Done! Your players can now chat with AI deities.

### For Environment Variables
```bash
export EIDOLON_GEMINI_API_KEY="your-api-key-here"
export EIDOLON_GEMINI_MODEL="gemini-1.5-flash"
```

## 🔧 **Available Commands (OP Level 4 Required)**

```bash
# Configuration Commands
/eidolon-config quick-setup <provider> <api-key>    # Quick setup with validation
/eidolon-config set <key> <value>                   # Set configuration value
/eidolon-config get <key>                           # Get configuration value  
/eidolon-config list                                # List all configuration (keys masked)
/eidolon-config test <provider>                     # Test API connection
/eidolon-config remove <key>                        # Remove configuration
/eidolon-config reload                              # Reload from config file
/eidolon-config status                              # Show system status
/eidolon-config validate-all                        # Validate all configurations

# Provider Configuration
/eidolon-ai configure set-provider direct    # Use direct API (Gemini, OpenAI)
/eidolon-ai configure set-provider proxy     # Use proxy service (Player2.game)
/eidolon-ai configure set-provider hybrid    # Try proxy first, fallback to direct

# API Configuration
/eidolon-ai configure set-api-key "your-key"           # Set API key (hidden in logs)
/eidolon-ai configure set-proxy-url "https://api..."   # Set proxy service URL

# Testing
/eidolon-ai configure test                # Test AI connection
/eidolon-ai help                         # Show all commands
```

## 🌐 **Provider Options Explained**

### **Direct Mode**
- ✅ Full control over API calls
- ✅ Often cheaper per request
- ❌ Requires technical setup
- ❌ Each server needs own API key

**Supported APIs:**
- Google Gemini (default)
- OpenAI GPT (planned)
- Anthropic Claude (planned)

### **Proxy Mode** 
- ✅ Simple setup
- ✅ No need for individual API keys
- ✅ Managed service reliability
- ❌ Additional cost through proxy
- ❌ Depends on third-party service

**Supported Services:**
- Player2.game API
- Custom proxy services

### **Hybrid Mode**
- ✅ Best of both worlds
- ✅ Automatic fallback
- ✅ Maximum reliability
- ❌ More complex configuration

## 🔐 **Security Features**

- 🔑 **API keys never stored in chat logs**
- 🛡️ **OP-level permissions required for configuration**
- 🔒 **Keys stored securely server-side**
- 🚫 **Players cannot access server API keys**

## 🎮 **How Players Use AI Deities**

Once configured by server operators, players can:

1. **Perform chant sequences** at deity effigies:
   - Dark Deity: `Wicked → Death → Blood` (conversation)
   - Light Deity: `Sacred → Harmony → Warding` (conversation)  
   - Nature Deity: `Harmony → Soul → Sacred` (conversation)

2. **AI responds contextually** based on:
   - Player's reputation with the deity
   - Current location and biome
   - Time of day and weather
   - Player's magical research progress

3. **Deities can execute commands** like:
   - Give items and effects
   - Change weather
   - Summon creatures
   - Display mystical messages

## 🆘 **Troubleshooting**

### **"AI not configured" message:**
- Run `/eidolon-ai configure status` to check setup
- Ensure you've set an API key or proxy service
- Verify the provider is enabled

### **"Connection failed" errors:**
- Check your internet connection
- Verify API key is valid
- Test with `/eidolon-ai configure test`

### **Players can't see AI responses:**
- Ensure AI is enabled: `/eidolon-ai configure enable`
- Check that the deity exists in your datapack
- Verify chant sequences match configuration

## 💡 **Tips for Server Operators**

1. **Start with proxy mode** for easiest setup
2. **Test thoroughly** before enabling for players  
3. **Monitor API usage** to avoid overage charges
4. **Set clear rules** about AI deity interactions
5. **Backup configurations** before major changes

## 📞 **Support**

- Check `/eidolon-ai help` in-game
- Review server logs for error details
- Test connection with `/eidolon-ai configure test`
