# 🤝 Hybrid AI + Registry Item Extraction Approach

## **Overview**
This document describes the **Hybrid approach** for natural language item extraction as an alternative to the current **Pure AI approach**. The Hybrid method combines enhanced regex patterns with AI analysis as a backup.

## **Why Hybrid Approach?**

### **Advantages:**
- **⚡ Faster**: Uses regex first, only calls AI when needed
- **💰 Cost-effective**: Reduces AI API calls for simple requests
- **🎯 Reliable**: Combines pattern matching with AI understanding
- **🔄 Fallback**: If regex fails, AI takes over

### **Disadvantages:**
- **🔧 Complex**: More code to maintain
- **🎭 Less Natural**: Regex can miss nuanced language
- **⚖️ Trade-off**: Speed vs natural language understanding

## **How It Works**

### **Phase 1: Enhanced Pattern Matching**
```java
// Enhanced regex patterns catch more natural language
Pattern ENHANCED_REQUEST_PATTERN = Pattern.compile(
    "\\b(?:give me|grant me|bless me with|bestow upon me|i need|i want|can you give|please give|" +
    "may i have|could i get|i would like|i seek|i desire|grant unto me|provide me with)\\s+" +
    "(?:the\\s+)?([\\w\\s:_-]+?)(?:\\s*[?!.]|$)", 
    Pattern.CASE_INSENSITIVE
);

// Example matches:
"My lord will you bestow upon me the raven cloak?" → extracts "raven cloak"
"I seek the blade of shadows" → extracts "blade of shadows"
"Grant unto me dark armor" → extracts "dark armor"
```

### **Phase 2: AI Analysis (Fallback)**
```java
// If regex doesn't find anything, ask AI to analyze
String analysisPrompt = buildAIAnalysisPrompt(playerMessage, player);
AI Response: "[ANALYZED_ITEM:raven cloak]"
```

### **Registry Validation (Both Phases)**
Both phases use the same advanced registry system:
```java
List<ResourceLocation> matches = RegistryContextProvider.findMatchingItemsWithScoring(item, modContextIds);
// Breaks down "raven cloak" → ["raven", "cloak"] and scores matches
```

## **Implementation Code (Backup Reference)**

### **Core Hybrid Extractor**
```java
public class HybridItemExtractor {
    // Phase 1: Enhanced pattern matching
    private static List<String> extractViaEnhancedPatterns(String playerMessage, ServerPlayer player) {
        // Uses enhanced regex patterns to catch more natural language
        // Falls back to registry validation
    }
    
    // Phase 2: AI analysis for complex requests  
    private static CompletableFuture<List<String>> extractViaAIAnalysis(String playerMessage, ServerPlayer player) {
        // Asks AI to analyze player intent
        // Uses [ANALYZED_ITEM:...] pattern
        // Validates through registry
    }
    
    // Main entry point
    public static CompletableFuture<List<String>> extractItemsHybrid(String playerMessage, ServerPlayer player) {
        // Try patterns first, then AI if needed
        // Combine results intelligently
    }
}
```

### **Integration Example**
```java
// In DeityChat.java conversation processing:
HybridItemExtractor.extractItemsHybrid(message, player)
    .thenAccept(commands -> {
        // Execute commands with tier enforcement
        executeCommands(commands, player);
        // Continue with regular response
        processRegularResponse(player, deity, rawResponse, history, playerId, deityId, commandsExecuted);
    })
    .exceptionally(error -> {
        // Fallback to regular processing
        processRegularResponse(player, deity, rawResponse, history, playerId, deityId, 0);
        return null;
    });
```

## **When to Consider Hybrid Approach**

### **Use Hybrid If:**
- ⚡ Performance is critical (many players, limited API quota)
- 💰 API costs are a concern  
- 🎯 You need consistent pattern matching for common requests
- 🔄 You want redundancy (regex + AI backup)

### **Keep Pure AI If:**
- 🎭 Natural language understanding is priority
- 🧠 You want AI to handle complex/nuanced requests
- 🔮 You prefer AI creativity in interpreting requests
- 📈 API usage/cost is not a constraint

## **Performance Comparison**

| Metric | Pure AI | Hybrid |
|--------|---------|--------|
| **Speed** | ~1-3 seconds (AI call) | ~0.1s (regex) or ~1-3s (AI fallback) |
| **API Calls** | Every request | Only complex requests |
| **Accuracy** | High (AI understands context) | High (patterns + AI backup) |
| **Maintenance** | Simple | Complex (two systems) |

## **Configuration Options**

If implementing Hybrid, consider these config options:
```java
// In EidolonUnchainedConfig.java
public final ForgeConfigSpec.BooleanValue useHybridExtraction;
public final ForgeConfigSpec.BooleanValue preferRegexOverAI;
public final ForgeConfigSpec.IntValue aiAnalysisThreshold; // Message length trigger
```

## **Current Status**

**✅ ACTIVE**: Pure AI approach (AIItemExtractor.java)
**📋 DOCUMENTED**: Hybrid approach (this document)
**❌ REMOVED**: HybridItemExtractor.java implementation

**Switch to Hybrid**: Import the code from this document if needed in the future.
