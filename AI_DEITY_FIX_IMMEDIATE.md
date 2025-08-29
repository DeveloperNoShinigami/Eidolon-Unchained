# AI Deity System - Immediate Fix Plan

## Current Issue Analysis
Based on the latest.log analysis and codebase review, the AI deity system is fully implemented but non-functional due to **missing API key configuration**.

### Root Cause
The system loads successfully and shows:
```
[INFO] Loading AI deity configurations...
[INFO] Queued 3 AI deity configurations with 0 errors
[INFO] Loaded AI configuration for deity: eidolonunchained:nature_deity
[INFO] Loaded AI configuration for deity: eidolonunchained:shadow_deity
[INFO] Loaded AI configuration for deity: eidolonunchained:divine_deity
```

But the API calls fail because there's no Gemini API key configured.

## Immediate Solution Steps

### Step 1: Build and Launch Current System
```bash
cd /workspaces/Eidolon-Unchained
./gradlew build
./gradlew runClient
```

### Step 2: Set Up API Key (In-Game)
1. Get Gemini API key from: https://aistudio.google.com/app/apikey
2. In-game, run: `/eidolon-unchained api set gemini YOUR_API_KEY_HERE`
3. Test connection: `/eidolon-unchained api test gemini`

### Step 3: Validate System Status
```
/eidolon-unchained config status
/eidolon-unchained config validate
```

### Step 4: Test AI Deity Interaction
1. Find or place an effigy
2. Right-click to start conversation
3. Alternatively, test chants that trigger AI conversations

## Expected Outcome
After API key configuration, the AI deity system should work immediately since all the infrastructure is in place.

## Command Reference
- **Set API Key**: `/eidolon-unchained api set gemini YOUR_KEY`
- **Test API**: `/eidolon-unchained api test gemini`  
- **Check Status**: `/eidolon-unchained config status`
- **Validate All**: `/eidolon-unchained config validate`
- **Reload Config**: `/eidolon-unchained config reload`

## API Key Format
- Gemini API keys start with `AIza`
- Example: `AIzaSyC9BqJhQr8X1mN5L7f3K2pD9vT4wE8sF6g`
- Keys are stored in: `config/eidolonunchained/server-api-keys.properties`

## Troubleshooting
If API calls still fail after key setup:
1. Check network connectivity
2. Verify API key hasn't expired
3. Check quota limits on Google AI Studio
4. Review latest.log for specific error messages

## File Locations for Manual Configuration (Alternative)
- Config file: `config/eidolonunchained/server-api-keys.properties`
- Format: `gemini.api_key=YOUR_KEY_HERE`
- Environment variable: `GEMINI_API_KEY=YOUR_KEY_HERE`

This should resolve the AI deity system completely as all other components are functional.
