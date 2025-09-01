# AI Deity API Key Setup Guide

This guide explains how to configure API keys for the AI deity system in Eidolon Unchained.

## Overview

The AI deity system supports multiple ways to configure API keys for maximum flexibility:

1. **Server-side Configuration** (Recommended for servers)
2. **Environment Variables** (Good for development/containers)
3. **In-game Commands** (Quick setup for server operators)

## Method 1: Server-side Configuration Files

### Quick Setup
For the fastest setup, server operators can use the quick setup command:

```
/eidolon-config quick-setup gemini YOUR_API_KEY_HERE
```

This automatically:
- Creates the config file if it doesn't exist
- Sets the API key securely
- Validates the configuration
- Provides immediate feedback

### Manual Configuration
Create a configuration file at `config/eidolon_ai_keys.properties`:

```properties
# AI Provider API Keys Configuration
# Generated automatically - do not edit manually unless needed

# Google Gemini API Configuration
gemini.api_key=YOUR_GEMINI_API_KEY_HERE
gemini.model=gemini-1.5-flash
gemini.endpoint=https://generativelanguage.googleapis.com/v1beta/models/

# OpenAI Configuration (for future support)
openai.api_key=YOUR_OPENAI_API_KEY_HERE
openai.model=gpt-3.5-turbo
openai.endpoint=https://api.openai.com/v1/

# Proxy Service Configuration (optional)
proxy.enabled=false
proxy.service_url=https://api.player2.game/ai
proxy.api_key=YOUR_PROXY_KEY_HERE

# Security Settings
security.encrypt_keys=true
security.require_op_permission=true
```

### File Security
- The config file is automatically created with restricted permissions
- API keys are optionally encrypted when stored
- Only server operators can modify the configuration
- File is ignored by version control systems

## Method 2: Environment Variables

Set environment variables before starting the server:

```bash
# Linux/Mac
export EIDOLON_GEMINI_API_KEY="your_api_key_here"
export EIDOLON_GEMINI_MODEL="gemini-1.5-flash"

# Windows (Command Prompt)
set EIDOLON_GEMINI_API_KEY=your_api_key_here
set EIDOLON_GEMINI_MODEL=gemini-1.5-flash

# Windows (PowerShell)
$env:EIDOLON_GEMINI_API_KEY="your_api_key_here"
$env:EIDOLON_GEMINI_MODEL="gemini-1.5-flash"
```

### Docker/Container Setup
Add to your docker-compose.yml or container environment:

```yaml
environment:
  - EIDOLON_GEMINI_API_KEY=your_api_key_here
  - EIDOLON_GEMINI_MODEL=gemini-1.5-flash
```

## Method 3: In-game Commands (Server Operators Only)

Server operators with OP permissions can configure API keys in-game:

### Available Commands

```
# Configure Gemini API key
/eidolon-config set gemini.api_key YOUR_KEY_HERE

# Configure model (optional)
/eidolon-config set gemini.model gemini-1.5-flash

# List current configuration (keys are masked)
/eidolon-config list

# Test API connection
/eidolon-config test gemini

# Remove a configuration
/eidolon-config remove gemini.api_key

# Quick setup with validation
/eidolon-config quick-setup gemini YOUR_KEY_HERE

# Reload configuration from file
/eidolon-config reload
```

### Security Features
- Only players with OP level 4 can use configuration commands
- API keys are masked in chat and logs
- All configuration changes are logged for security
- Invalid keys are rejected with helpful error messages

## Getting API Keys

### Google Gemini API
1. Visit [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Sign in with your Google account
3. Click "Create API Key"
4. Copy the generated key
5. Recommended model: `gemini-1.5-flash` (fast and cost-effective)

### Alternative Services
If you prefer not to use Google directly, consider proxy services like:
- [Player2.game AI API](https://player2.game/) - Gaming-focused AI proxy
- [OpenRouter](https://openrouter.ai/) - Multi-model proxy service
- [AI21 Labs](https://www.ai21.com/) - Alternative AI provider

## Configuration Priority

The system checks for API keys in this order:
1. Server configuration file (`config/eidolon_ai_keys.properties`)
2. Environment variables (`EIDOLON_GEMINI_API_KEY`)
3. System properties (`-Deidolon.gemini.api_key=...`)

This allows for flexible deployment while maintaining security.

## Troubleshooting

### Common Issues

**"No API key configured" Error:**
- Check that your API key is properly set using one of the methods above
- Verify the key doesn't have extra spaces or characters
- Ensure you're using `/eidolon-config test gemini` to validate

**"Invalid API key" Error:**
- Verify your key is active at [Google AI Studio](https://makersuite.google.com/app/apikey)
- Check that billing is enabled for your Google Cloud project
- Ensure the key has access to the Gemini API

**"Permission denied" Error:**
- Only server operators (OP level 4) can configure API keys
- Use `/op <playername>` to grant operator permissions

**Deities not responding:**
- Check server logs for detailed error messages
- Verify API key configuration with `/eidolon-config test gemini`
- Ensure you've completed the chant sequence properly

### Debug Commands

```
# Check current configuration status
/eidolon-config status

# View recent API call logs (masked)
/eidolon-config logs

# Validate all configurations
/eidolon-config validate-all

# Reset to default configuration
/eidolon-config reset
```

## Best Practices

1. **Use server-side configuration** for production servers
2. **Keep API keys secure** - never share in chat or public files
3. **Use environment variables** for containerized deployments
4. **Monitor API usage** to stay within rate limits
5. **Test configuration** after any changes
6. **Back up your config** file before major updates
7. **Use least-privilege access** - only give OP to trusted players

## Advanced Configuration

### Multiple Providers
You can configure multiple AI providers for redundancy:

```properties
# Primary provider
gemini.api_key=your_gemini_key
gemini.priority=1

# Fallback provider
openai.api_key=your_openai_key
openai.priority=2

# Enable automatic fallback
fallback.enabled=true
fallback.retry_attempts=3
```

### Rate Limiting
Configure rate limiting to prevent API abuse:

```properties
# Rate limiting (requests per minute)
rate_limit.requests_per_minute=30
rate_limit.burst_allowance=10
rate_limit.enable_player_limits=true
```

### Custom Models
For advanced users, you can specify custom models:

```properties
# Use different models for different deity types
dark_deity.model=gemini-1.5-pro
light_deity.model=gemini-1.5-flash
nature_deity.model=gemini-1.5-flash-8b
```

## Migration from Previous Versions

If you were using environment variables before this update:

1. Your existing `GEMINI_API_KEY` environment variable will continue to work
2. For better security, consider migrating to file-based configuration:
   ```
   /eidolon-config migrate-from-env
   ```
3. This will copy your environment variables to the secure config file

## Support

If you encounter issues:

1. Check the server console for detailed error messages
2. Use `/eidolon-config validate-all` to test your configuration
3. Review this guide for common solutions
4. Report bugs with your configuration (API keys removed) to the mod author

---

**Security Note:** Never commit API keys to version control or share them publicly. The configuration file is automatically added to `.gitignore` patterns.
