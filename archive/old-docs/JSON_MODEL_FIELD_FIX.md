# JSON Model Field Fix - Removed Hardcoded Model Mapping

## Issue Identified
The user correctly identified that the OpenRouter model selection was hardcoded in `AIProviderFactory.java` instead of using the JSON field that was set in the AI deity configurations. This prevented the JSON-driven system from working as intended.

## Root Cause
In `AIProviderFactory.java`, the `mapToOpenRouterModel()` function was performing hardcoded model name mapping instead of using the model name directly from the JSON configuration:

```java
// WRONG: Hardcoded mapping overrode JSON field
String openRouterModel = mapToOpenRouterModel(model);
OpenRouterClient client = new OpenRouterClient(apiKey, openRouterModel, timeout);
```

## Fix Applied
1. **Removed Hardcoded Mapping**: Eliminated the entire `mapToOpenRouterModel()` function and its hardcoded switch statement
2. **Direct JSON Usage**: Now uses the model name directly from the JSON configuration
3. **Minimal Default**: Only provides a default if no model is specified in JSON

### Before (Hardcoded)
```java
private static String mapToOpenRouterModel(String model) {
    switch (model.toLowerCase()) {
        case "claude-3.5-sonnet":
            return "anthropic/claude-3.5-sonnet";
        // ... many hardcoded mappings
        default:
            return "anthropic/claude-3.5-sonnet";
    }
}
```

### After (JSON-Driven)
```java
// Use the model directly from JSON configuration - no hardcoded mapping
if (model == null || model.trim().isEmpty()) {
    model = "huggingfaceh4/zephyr-7b-beta"; // Only default if no model specified
}

OpenRouterClient client = new OpenRouterClient(apiKey, model, timeout);
```

## Data Flow Verification
The system now properly follows this flow:
1. **JSON File**: `ai_deities/example.json` contains `"model": "anthropic/claude-3.5-sonnet"`
2. **AIDeityManager**: Loads model field: `config.model = json.has("model") ? json.get("model").getAsString() : null;`
3. **DeityChat**: Passes model to factory: `AIProviderFactory.createProvider(deityProvider, aiConfig.model)`
4. **AIProviderFactory**: Uses model directly: `OpenRouterClient client = new OpenRouterClient(apiKey, model, timeout);`

## Benefits
- **JSON-Driven**: Model selection now comes entirely from JSON configuration as intended
- **Flexible**: Supports any OpenRouter model name without code changes
- **No Hardcoding**: Eliminates the hardcoded model mapping that was overriding JSON settings
- **Cleaner Code**: Removed unnecessary mapping function and its maintenance burden

## Testing Required
1. Verify that AI deity configurations with specific model fields work correctly
2. Test that OpenRouter uses the exact model specified in JSON
3. Confirm that deities with different models can coexist
4. Validate that the free model default still works when no model is specified

## Key Lesson
This fix highlights the importance of following the JSON-driven architecture principle throughout the system. Hardcoded mappings should be avoided when the system is designed to be configuration-driven.
