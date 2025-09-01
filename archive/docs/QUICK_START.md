# Quick Start: AI Deity Setup

Get your AI deities running in just a few minutes!

## Step 1: Get a Google Gemini API Key

1. Go to [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Sign in with your Google account
3. Click "Create API Key"
4. Copy the key (starts with "AIza...")

## Step 2: Configure the Server

**Option A: Quick Command (Easiest)**
```
/op YourUsername
/eidolon-config quick-setup gemini YOUR_API_KEY_HERE
```

**Option B: Environment Variable**
```bash
export EIDOLON_GEMINI_API_KEY="YOUR_API_KEY_HERE"
```

## Step 3: Test Your Setup

In-game:
```
/eidolon-config test gemini
```

You should see: `✓ Gemini API connection successful`

## Step 4: Talk to Deities

1. Perform a chant using the configured signs (WICKED, SACRED, HARMONY)
2. After successful chant, type in chat: `@deity_name your message`
3. The deity will respond with AI-generated dialogue!

## Example Deity Chants

**Dark Deity (Malevolent Spirit):**
- WICKED + WICKED + SACRED

**Light Deity (Benevolent Guardian):**
- SACRED + SACRED + HARMONY  

**Nature Deity (Verdant Sage):**
- HARMONY + SACRED + HARMONY

## That's It!

Your AI deity system is now ready. For advanced configuration, see the full [API Key Setup Guide](API_KEY_SETUP.md).

## Need Help?

- **No response?** Check `/eidolon-config status`
- **Permission error?** Make sure you're OP with `/op YourUsername`  
- **Invalid key?** Verify at [Google AI Studio](https://makersuite.google.com/app/apikey)

Happy deity conversations! 🧙‍♂️✨
