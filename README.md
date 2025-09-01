# Eidolon Unchained

**AI-Powered Deity Conversations for Minecraft 1.20.1**

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-green.svg)](https://minecraft.net)
[![Forge](https://img.shields.io/badge/Forge-47.3.0+-orange.svg)](https://files.minecraftforge.net)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen.svg)](.)

**Eidolon Unchained** extends the Eidolon mod with **AI-powered deity conversations**, **JSON-driven datapack systems**, and **flexible patron allegiance mechanics**. Chat with intelligent deities that remember your conversations and respond based on your actions, reputation, and world state.

---

## 🚀 Quick Start

1. **Install Prerequisites**: Minecraft 1.20.1 + Forge 47.3.0+ + Eidolon mod
2. **Install Eidolon Unchained**: Add the .jar to your mods folder
3. **Configure AI Provider**: `/eidolon-unchained api set gemini YOUR_API_KEY`
4. **Test Deity Chat**: Perform chant sequence → Right-click effigy → Chat with AI deity

**👉 [Complete Setup Guide](wiki/01-QUICK-START.md)**

---

## ✨ Key Features

### 🧠 **AI Deity Conversations**
- **Real-time chat** with AI-powered deities using Google Gemini, OpenRouter, or Player2AI
- **Context awareness** of player health, inventory, location, reputation, and past actions
- **Dynamic personalities** that change based on your patron status and progression

### 🏛️ **JSON-Driven Deity System**  
- **Single-file configurations** combining deity data and AI behavior
- **No hardcoded values** - everything customizable via datapack JSON
- **Complete patron allegiance system** with opposition/alliance mechanics

### ⚡ **Flexible Chant Casting**
- **Configurable keybinds** for spells and sign sequences
- **Datapack-driven chants** with custom effects and requirements
- **Integration** with Eidolon's existing sign-based magic system

### 📚 **Comprehensive Documentation System**
- **9 different page types** for rich in-game content display
- **Automatic content discovery** from datapack configurations
- **Chapter-based organization** with category management

---

## 📖 Complete Documentation

### 🎯 **Getting Started**
- **[Quick Start Guide](wiki/01-QUICK-START.md)** - 5-minute setup
- **[Installation & Setup](wiki/02-INSTALLATION.md)** - Detailed installation
- **[Project Architecture](wiki/03-ARCHITECTURE.md)** - Understanding the codebase

### 🏛️ **Core Systems**  
- **[AI Deity System](wiki/04-AI-DEITY-SYSTEM.md)** - AI conversation mechanics
- **[Datapack Deities](wiki/05-DATAPACK-DEITIES.md)** - Creating custom deities
- **[Patron System](wiki/06-PATRON-SYSTEM.md)** - Player-deity relationships
- **[Chant System](wiki/07-CHANT-SYSTEM.md)** - Flexible spell casting

### 📚 **Content Creation**
- **[Codex System](wiki/08-CODEX-SYSTEM.md)** - In-game documentation
- **[Research System](wiki/09-RESEARCH-SYSTEM.md)** - Auto-discovery mechanics  
- **[Recipe Integration](wiki/10-RECIPE-INTEGRATION.md)** - Custom rituals

### 🔧 **Advanced Features**
- **[AI Provider System](wiki/11-AI-PROVIDERS.md)** - Multiple AI backends
- **[Command System](wiki/12-COMMANDS.md)** - Admin & debug commands
- **[Keybind System](wiki/13-KEYBINDS.md)** - Custom key assignments

### 📖 **Reference**
- **[JSON Field Reference](wiki/14-JSON-REFERENCE.md)** - Complete configuration guide
- **[Language Keys](wiki/15-LANGUAGE-KEYS.md)** - Localization standards
- **[API Reference](wiki/16-API-REFERENCE.md)** - Developer documentation

### 🛠️ **Development**
- **[Debugging Guide](wiki/17-DEBUGGING.md)** - Troubleshooting & testing
- **[Extension Guide](wiki/18-EXTENSIONS.md)** - Creating addon mods
- **[Future Roadmap](wiki/19-ROADMAP.md)** - Planned features

---

## 🎮 Example: AI Deity Interaction

```bash
# 1. Perform chant sequence near effigy
# Cast: Wicked Sign → Wicked Sign → Blood Sign

# 2. Right-click effigy to start conversation
# Effigy attunes to Nyxathel, Shadow Lord

# 3. Type your message
Player: "I seek knowledge of the shadow arts"

# 4. Receive contextual AI response
Nyxathel: "Ah, a mortal seeks the forbidden paths... I sense you have 
defeated 3 undead recently and carry a soul shard. Interesting. 
The shadows whisper that you show promise, but are you truly 
prepared for what the darkness demands?"
```

The AI has full awareness of your:
- **Recent actions** (defeating undead)
- **Inventory contents** (soul shard)
- **Reputation level** with the deity
- **Location & time** of interaction
- **Conversation history** and relationship

---

## 🏗️ Technical Highlights

### Architecture Principles
- **JSON-First Design**: All configuration via datapack JSON files
- **No Hardcoding**: Maximum flexibility and customization
- **Standards-Based**: Consistent patterns and structures
- **Performance Optimized**: Async processing and smart caching

### Current Status
- ✅ **100% Compilation Success** - No build errors
- ✅ **Multi-AI Support** - Gemini, OpenRouter, Player2AI
- ✅ **Complete Patron System** - Full allegiance mechanics  
- ✅ **Flexible Content Creation** - Easy datapack-based expansion
- ✅ **Comprehensive Testing** - All major features validated

---

## 🤝 Community & Support

### Getting Help
- **[Debugging Guide](wiki/17-DEBUGGING.md)** - Self-service troubleshooting
- **[Command Reference](wiki/12-COMMANDS.md)** - Built-in diagnostic tools
- **GitHub Issues** - Bug reports and feature requests

### Contributing
- **[Extension Guide](wiki/18-EXTENSIONS.md)** - Building on the platform
- **[JSON Reference](wiki/14-JSON-REFERENCE.md)** - Creating custom content
- **Pull Requests** - Code contributions welcome

### Roadmap
- **[Future Plans](wiki/19-ROADMAP.md)** - Upcoming features and vision
- **Community Input** - Feature requests and feedback valued

---

## 📜 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

---

## 🎯 Ready to Get Started?

**👉 [Start with the Quick Start Guide](wiki/01-QUICK-START.md)** to get AI deity conversations running in 5 minutes!

**🏛️ [Explore the Complete Wiki](wiki/00-HOME.md)** for comprehensive documentation and guides!

