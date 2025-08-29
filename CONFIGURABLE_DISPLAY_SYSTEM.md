# Configurable Display System for AI Deity Responses

## Overview
Successfully implemented a comprehensive configurable display system for AI deity interactions that gives players full control over how deity responses are displayed.

## Features Implemented

### 1. Display Mode Configuration
- **`useProminentDisplay`** (default: true)
  - When `true`: Uses prominent title/subtitle display above action bar
  - When `false`: Uses standard chat messages with deity name prefix

### 2. Timing Configuration
- **`displayDurationTicks`** (default: 60) - How long the title/subtitle stays visible (3 seconds)
- **`fadeInTicks`** (default: 10) - Fade in duration (0.5 seconds)
- **`fadeOutTicks`** (default: 20) - Fade out duration (1 second)

### 3. Long Message Handling
- **`useChatForLongMessages`** (default: false)
  - When `true`: Long messages automatically use chat instead of prominent display
  - When `false`: Long messages are split/truncated for prominent display
- **`maxSubtitleLength`** (default: 60) - Maximum character length for subtitle display

## Configuration File Location
Configuration is stored in `config/eidolonunchained-common.toml` under the `[display]` section:

```toml
[display]
    useProminentDisplay = true
    displayDurationTicks = 60
    fadeInTicks = 10
    fadeOutTicks = 20
    useChatForLongMessages = false
    maxSubtitleLength = 60
```

## User Control Examples

### Example 1: Chat-Only Mode
```toml
[display]
    useProminentDisplay = false
```
Result: All deity responses appear as chat messages: `[Deity Name] response text`

### Example 2: Quick Prominent Display
```toml
[display]
    useProminentDisplay = true
    displayDurationTicks = 30    # 1.5 seconds instead of 3
    fadeInTicks = 5              # 0.25 seconds
    fadeOutTicks = 10            # 0.5 seconds
```
Result: Fast prominent display that doesn't stay on screen long

### Example 3: Chat Fallback for Long Messages
```toml
[display]
    useProminentDisplay = true
    useChatForLongMessages = true
    maxSubtitleLength = 40
```
Result: Short responses use prominent display, long responses automatically use chat

## Implementation Details

### Files Modified
1. **`EidolonUnchainedConfig.java`**: Added display configuration section
2. **`PrayerSystem.java`**: Updated `sendDeityMessage()` to use configuration
3. **`DeityChat.java`**: Updated `sendDeityResponse()` to use configuration

### Code Pattern
Both `PrayerSystem` and `DeityChat` now follow the same pattern:
1. Check `useProminentDisplay` configuration
2. If false, send chat message and return
3. If true, check message length against `maxSubtitleLength`
4. For long messages, check `useChatForLongMessages` setting
5. Use configurable timing values for prominent display

### Backward Compatibility
- All configuration values have sensible defaults
- Existing behavior (prominent display) is preserved by default
- Users can opt into chat-only mode if preferred

## Testing Recommendations

### In-Game Testing
1. **Test Chat Mode**: Set `useProminentDisplay = false`, pray to deity, verify chat output
2. **Test Timing**: Set short duration values, verify display timing
3. **Test Long Messages**: Create long deity response, test both fallback modes
4. **Test Configuration Reload**: Change config values, restart game, verify changes

### Configuration Commands
Use existing commands to test:
- `/eidolon-unchained config validate` - Verify configuration is valid
- `/eidolon-unchained debug status` - Check system health
- Prayer interactions to test display modes

## User Benefits
- **Accessibility**: Chat mode for users who prefer persistent text
- **Performance**: Faster display timing for users who want quick notifications  
- **Customization**: Flexible control over message display behavior
- **Compatibility**: Chat mode works better with other UI mods
- **Preference**: Users can choose their preferred notification style

## Success Criteria Met
✅ **Disable Action Bar**: Users can set `useProminentDisplay = false`  
✅ **Chat Responses**: Full chat mode support with deity name prefixes  
✅ **Control Timing**: Configurable fade in/out and duration timing  
✅ **Flexible Options**: Multiple configuration options for different use cases  
✅ **Maintains Quality**: Prominent display still available for users who prefer it  

The system successfully addresses the user's request to "disable the actionbar and put the response in chat and control how long the action bar appears" while providing even more flexibility than requested.
