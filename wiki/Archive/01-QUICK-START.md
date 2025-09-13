# Quick Start Guide

**Goal**: Get Eidolon Unchained running with AI deity conversations in 5 minutes.

---

## 🎯 Prerequisites

✅ **Minecraft 1.20.1** with **Forge 47.3.0+**  
✅ **Eidolon mod** installed and working  
✅ **AI API key** (Google Gemini, OpenRouter, or Player2AI)

---

## ⚡ Step 1: Install the Mod

1. Download `eidolon-unchained-1.20.1_v3.9.0.9.jar`
2. Place in your `mods/` folder
3. Start Minecraft to generate configuration files

---

## 🔑 Step 2: Configure AI Provider

Choose ONE of these AI providers:

### Option A: Google Gemini (Recommended)
```bash
# In-game command:
/eidolon-unchained api set gemini YOUR_GEMINI_API_KEY
```

### Option B: OpenRouter
```bash
# In-game command:
/eidolon-unchained api set openrouter YOUR_OPENROUTER_API_KEY
```

### Option C: Player2AI (Local)
```bash
# In-game command:
/eidolon-unchained api set player2ai local
```

---

## 🏛️ Step 3: Test AI Deity System

1. **Find or place an Effigy** (from Eidolon mod)
2. **Perform a chant sequence** near the effigy:
   - Hold a wand/staff
   - Right-click to cast: **Wicked Sign → Wicked Sign → Blood Sign**
3. **Start conversation**: Right-click the effigy
4. **Chat with the deity**: Type your message and press Enter

---

## ✅ Expected Results

You should see:
- **Title display**: "Nyxathel, Shadow Lord"
- **AI response**: Contextual deity message
- **Chat format**: Formatted conversation in chat

### 🎉 Success! You're now talking to an AI deity!

---

## 🔧 Quick Configuration

### Change AI Provider Globally
```bash
# Set default provider for all deities
/eidolon-unchained config set ai_provider gemini
```

### Test Different Deities
- **Shadow Deity**: `Wicked → Wicked → Blood`
- **Light Deity**: `Sacred → Harmony → Warding`
- **Nature Deity**: `Harmony → Soul → Sacred`

### Debug Commands
```bash
# Test API connection
/eidolon-unchained api test gemini

# Check deity status
/eidolon-unchained status

# View available deities
/eidolon-unchained deity list
```

---

## 🚨 Troubleshooting

### Problem: "AI provider not available"
**Solution**: Check your API key is set correctly
```bash
/eidolon-unchained api test gemini
```

### Problem: "Deity does not respond to mortals"
**Solution**: Your API key may be invalid or you need to perform the chant sequence first

### Problem: No response to effigy interaction
**Solution**: Make sure you performed the correct chant sequence near the effigy before right-clicking

---

## 🎯 What's Next?

- **[Create Custom Deities](05-DATAPACK-DEITIES.md)** - Add your own AI-powered deities
- **[Patron System](06-PATRON-SYSTEM.md)** - Become a follower of specific deities  
- **[Advanced AI Configuration](04-AI-DEITY-SYSTEM.md)** - Customize AI behavior
- **[Chant Creation](07-CHANT-SYSTEM.md)** - Design custom spell sequences

---

**🎉 Congratulations!** You've successfully set up AI deity conversations. The AI can see your health, inventory, location, and remember your previous interactions for dynamic, contextual responses!
