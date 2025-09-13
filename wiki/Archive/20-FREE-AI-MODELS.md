# Free AI Models & API Providers for Eidolon Unchained

This document provides a comprehensive list of free AI models and providers that can be integrated with Eidolon Unchained's AI deity system.

---

## 🎯 Currently Supported Providers

### ✅ **Google Gemini** (Fully Implemented)
**Status**: Production Ready | **Free Tier**: Yes | **API Key Required**: Yes

#### **Available Models:**
- `gemini-1.5-flash` - Fast, lightweight model (recommended for real-time chat)
- `gemini-1.5-pro` - Advanced reasoning and longer context
- `gemini-1.0-pro` - Legacy model, still functional

#### **Free Tier Limits:**
- **Rate Limit**: 15 requests per minute, 1,500 requests per day
- **Context Window**: Up to 1M tokens (gemini-1.5-pro), 1M tokens (gemini-1.5-flash)
- **Safety Features**: Built-in content filtering
- **Cost**: Free up to limits, then pay-per-use

#### **API Key Setup:**
1. Visit: [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Sign in with Google account
3. Click "Create API Key"
4. Copy the key (starts with `AIza...`)
5. Use command: `/eidolon-unchained api set gemini YOUR_API_KEY`

#### **Configuration Example:**
```json
{
  "ai_provider": "gemini",
  "api_settings": {
    "model": "gemini-1.5-flash",
    "temperature": 0.8,
    "max_tokens": 1000
  },
  "safety_settings": {
    "harassment": "BLOCK_MEDIUM_AND_ABOVE",
    "hate_speech": "BLOCK_MEDIUM_AND_ABOVE",
    "sexually_explicit": "BLOCK_MEDIUM_AND_ABOVE",
    "dangerous_content": "BLOCK_MEDIUM_AND_ABOVE"
  }
}
```

---

### ✅ **OpenRouter** (Implemented, Needs Testing)
**Status**: Beta Implementation | **Free Tier**: Limited | **API Key Required**: Yes

#### **Available Free Models:**
- `meta-llama/llama-3.2-3b-instruct:free` - Meta's Llama 3.2 3B
- `meta-llama/llama-3.2-1b-instruct:free` - Meta's Llama 3.2 1B  
- `microsoft/phi-3-mini-128k-instruct:free` - Microsoft Phi-3 Mini
- `microsoft/phi-3-medium-128k-instruct:free` - Microsoft Phi-3 Medium
- `google/gemma-2-9b-it:free` - Google Gemma 2 9B
- `google/gemma-2-27b-it:free` - Google Gemma 2 27B
- `qwen/qwen-2-7b-instruct:free` - Alibaba Qwen 2 7B
- `openchat/openchat-7b:free` - OpenChat 7B
- `gryphe/mythomist-7b:free` - Mythomist 7B (Creative writing)
- `undi95/toppy-m-7b:free` - Toppy M 7B

#### **Free Tier Limits:**
- **Rate Limit**: 10-20 requests per minute (varies by model)
- **Daily Limit**: $1 worth of free credits per day
- **Context Window**: Varies by model (usually 4K-128K tokens)
- **Queue**: Free tier requests may be queued during high usage

#### **API Key Setup:**
1. Visit: [OpenRouter](https://openrouter.ai/keys)
2. Sign up for account
3. Go to "Keys" section
4. Create a new API key
5. Use command: `/eidolon-unchained api set openrouter YOUR_API_KEY`

#### **Configuration Example:**
```json
{
  "ai_provider": "openrouter",
  "api_settings": {
    "model": "meta-llama/llama-3.2-3b-instruct:free",
    "temperature": 0.7,
    "max_tokens": 800,
    "endpoint": "https://openrouter.ai/api/v1/chat/completions"
  }
}
```

---

### ✅ **Player2AI** (Experimental Implementation)
**Status**: Experimental | **Free Tier**: Yes | **API Key Required**: Yes

#### **Features:**
- Gaming-focused AI responses
- Character persistence and memory
- RPG-style conversation system
- Minecraft-aware context understanding

#### **Free Tier Limits:**
- **Rate Limit**: Variable based on server load
- **Features**: Basic character creation and conversation
- **Memory**: Limited conversation history
- **Specialization**: Gaming and RPG content

#### **API Key Setup:**
1. Visit: [Player2AI](https://player2.ai/) (Note: Service availability varies)
2. Sign up for beta access
3. Request API access
4. Use command: `/eidolon-unchained api set player2ai YOUR_API_KEY`

---

## 🚀 Potential Future Providers

### **HuggingFace Inference API** (Not Yet Implemented)
**Priority**: High | **Complexity**: Medium

#### **Available Free Models:**
- `microsoft/DialoGPT-large` - Conversational AI
- `facebook/blenderbot-400M-distill` - Facebook's Blenderbot
- `microsoft/DialoGPT-medium` - Medium-sized conversation model
- `EleutherAI/gpt-neo-2.7B` - Open-source GPT alternative
- `google/flan-t5-large` - Google's instruction-following model

#### **Free Tier:**
- **Rate Limit**: 1,000 requests per hour
- **Models**: Access to 100,000+ open-source models
- **Cost**: Free for public models

#### **API Key Setup:**
1. Visit: [HuggingFace](https://huggingface.co/settings/tokens)
2. Create account and generate access token
3. Choose "Read" permissions for inference

---

### **Ollama (Local Models)** (Not Yet Implemented)
**Priority**: Medium | **Complexity**: High

#### **Available Models:**
- `llama3.2:3b` - Meta Llama 3.2 3B (2GB RAM)
- `llama3.2:1b` - Meta Llama 3.2 1B (1.3GB RAM)
- `phi3:mini` - Microsoft Phi-3 Mini (2.3GB RAM)
- `gemma2:2b` - Google Gemma 2 2B (1.6GB RAM)
- `qwen2:1.5b` - Alibaba Qwen 2 1.5B (934MB RAM)
- `mistral:7b` - Mistral 7B (4.1GB RAM)

#### **Advantages:**
- **No API Keys**: Completely local, no internet required
- **No Rate Limits**: Limited only by hardware
- **Privacy**: All data stays on local machine
- **Cost**: Free after initial setup

#### **Requirements:**
- **RAM**: 8GB+ recommended for 7B models, 4GB+ for smaller models
- **Storage**: 1-5GB per model
- **CPU**: Modern multi-core processor recommended

---

### **Cohere Free Tier** (Not Yet Implemented)
**Priority**: Medium | **Complexity**: Low

#### **Available Models:**
- `command` - General-purpose conversation model
- `command-light` - Faster, lighter version

#### **Free Tier:**
- **Monthly Limit**: 100 requests per month (very limited)
- **Rate Limit**: 5 requests per minute
- **Features**: Good conversation quality

#### **API Key Setup:**
1. Visit: [Cohere Dashboard](https://dashboard.cohere.ai/api-keys)
2. Sign up for free account
3. Generate trial API key

---

### **Anthropic Claude (Limited Free)** (Not Yet Implemented)
**Priority**: Low | **Complexity**: Medium

#### **Available Models:**
- `claude-3-haiku-20240307` - Fast and affordable
- `claude-3-sonnet-20240229` - Balanced performance

#### **Free Tier:**
- **Monthly Credit**: $5 worth of free credits
- **Rate Limit**: Variable based on model
- **Quality**: Very high-quality responses

---

## 🔧 Implementation Priority

### **Immediate (Next Release):**
1. **OpenRouter Testing** - Complete integration testing and documentation
2. **HuggingFace Integration** - Add support for Inference API
3. **Error Handling** - Improve fallback systems for all providers

### **Short Term (2-3 Releases):**
1. **Ollama Integration** - Local model support for privacy-conscious users
2. **Provider Auto-Switching** - Automatically switch providers on rate limit
3. **Cost Tracking** - Monitor API usage and costs

### **Long Term (6+ Months):**
1. **Custom Fine-Tuning** - Train models specifically for deity interactions
2. **Multi-Provider Conversations** - Different deities use different models
3. **Advanced Features** - Image generation, voice synthesis, etc.

---

## 📊 Provider Comparison

| Provider | Free Tier | Rate Limit | Quality | Context | Setup Difficulty |
|----------|-----------|------------|---------|---------|------------------|
| **Gemini** | ⭐⭐⭐⭐⭐ | 15/min | ⭐⭐⭐⭐⭐ | 1M tokens | ⭐⭐⭐⭐⭐ |
| **OpenRouter** | ⭐⭐⭐⭐ | 10-20/min | ⭐⭐⭐⭐ | Variable | ⭐⭐⭐⭐ |
| **Player2AI** | ⭐⭐⭐ | Variable | ⭐⭐⭐ | Gaming | ⭐⭐ |
| **HuggingFace** | ⭐⭐⭐⭐ | 1000/hour | ⭐⭐⭐ | Variable | ⭐⭐⭐ |
| **Ollama** | ⭐⭐⭐⭐⭐ | Unlimited | ⭐⭐⭐⭐ | Variable | ⭐⭐ |
| **Cohere** | ⭐⭐ | 5/min | ⭐⭐⭐⭐ | 128K tokens | ⭐⭐⭐⭐ |

---

## 🛠️ Configuration Templates

### **Multi-Provider Deity Setup:**
```json
{
  "deities": {
    "shadow_deity": {
      "ai_provider": "openrouter",
      "model": "gryphe/mythomist-7b:free"
    },
    "light_deity": {
      "ai_provider": "gemini",
      "model": "gemini-1.5-flash"
    },
    "nature_deity": {
      "ai_provider": "ollama",
      "model": "llama3.2:3b"
    }
  }
}
```

### **Fallback Configuration:**
```json
{
  "api_settings": {
    "primary_provider": "gemini",
    "fallback_providers": ["openrouter", "huggingface"],
    "retry_attempts": 3,
    "fallback_delay": 5000
  }
}
```

---

## 📝 Getting Started Checklist

### **For Gemini (Recommended for Beginners):**
- [ ] Create Google account if needed
- [ ] Visit [Google AI Studio](https://makersuite.google.com/app/apikey)
- [ ] Generate API key
- [ ] Set key: `/eidolon-unchained api set gemini YOUR_KEY`
- [ ] Test with: `/eidolon-unchained debug test-api gemini`

### **For OpenRouter (Multiple Models):**
- [ ] Create account at [OpenRouter](https://openrouter.ai/keys)
- [ ] Generate API key
- [ ] Set key: `/eidolon-unchained api set openrouter YOUR_KEY`
- [ ] Choose free model from list above
- [ ] Update deity configuration with selected model

### **For Local Setup (Ollama):**
- [ ] Install Ollama on server/client machine
- [ ] Download desired model: `ollama pull llama3.2:3b`
- [ ] Configure deity to use local endpoint
- [ ] Test connectivity and performance

---

## 🚨 Important Notes

### **Rate Limiting:**
- Always respect provider rate limits to avoid IP bans
- Implement proper retry logic with exponential backoff
- Consider caching responses for repeated prayers

### **Content Safety:**
- All providers have content filtering
- Some responses may be blocked or modified
- Test deity personalities thoroughly before deployment

### **Privacy Considerations:**
- All cloud providers store conversation data temporarily
- Use Ollama for complete privacy
- Review each provider's data retention policies

### **Performance:**
- Gemini 1.5-flash offers best speed/quality balance
- Local models (Ollama) have no network latency
- OpenRouter may have queue delays during peak usage

---

## 📞 Support & Resources

### **Official Documentation:**
- [Gemini API Docs](https://ai.google.dev/docs)
- [OpenRouter Documentation](https://openrouter.ai/docs)
- [HuggingFace Inference API](https://huggingface.co/docs/api-inference/index)
- [Ollama Documentation](https://ollama.ai/docs)

### **Community Support:**
- Check mod GitHub issues for provider-specific problems
- Join Discord for real-time troubleshooting
- Share working configurations in community forums

### **Debugging Commands:**
- `/eidolon-unchained debug status` - Check provider connectivity
- `/eidolon-unchained debug test-api <provider>` - Test specific provider
- `/eidolon-unchained config validate` - Validate all configurations

---

**🎯 Recommendation for New Users:** Start with **Google Gemini** for the best balance of features, reliability, and ease of setup. Once familiar with the system, experiment with **OpenRouter** for access to multiple models and **Ollama** for privacy-focused local deployment.
