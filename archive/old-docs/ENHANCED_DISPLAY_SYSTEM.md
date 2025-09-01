# Enhanced Deity Response Display System

## Problem Solved

The original deity response system had several issues:
1. **Quick Disappearance**: Title/subtitle messages disappeared too quickly
2. **Full Screen Width**: Text stretched across entire screen, making it hard to read on wide monitors
3. **Poor Readability**: Players had to expand their screen just to read deity responses
4. **Limited Control**: Few options to customize the display experience

## New Display Options

### 1. **ACTION_BAR** (Recommended Default)
- **Location**: Appears above the hotbar in center of screen
- **Readability**: Text is centered and compact, easy to read
- **Persistence**: Messages sent multiple times for longer visibility
- **Best For**: Short to medium deity responses

**Example Display:**
```
[Above hotbar] Verdania: I sense your wounds run deep, faithful one. Shall I heal you?
```

### 2. **ENHANCED_CHAT** 
- **Location**: Chat area with beautiful formatting
- **Features**: Visual separators, proper word wrapping, deity name headers
- **Persistence**: Stays in chat history for reference
- **Best For**: Long deity conversations and detailed responses

**Example Display:**
```
═══════════════════════════════════════
⟦ Verdania the Nature Guardian ⟧
I sense your wounds run deep, faithful guardian. Your life 
force dims to but 40% of its full strength. The forest 
spirits whisper of your pain.

Shall I grant you nature's healing touch to restore your 
vitality and mend your broken form?
═══════════════════════════════════════
[Verdania has spoken to you]
```

### 3. **TITLE_SUBTITLE** (Legacy)
- **Location**: Large text across center of screen
- **Features**: Original system with configurable timing
- **Best For**: Dramatic moments and short announcements
- **Note**: May stretch across wide screens

### 4. **AUTO** (Smart Selection)
- **Logic**: Automatically chooses best display method based on message length
- **Short messages**: Uses ACTION_BAR for readability
- **Long messages**: Uses ENHANCED_CHAT for full content
- **Best For**: Users who want optimal display without manual configuration

## Configuration Options

### In-Game Configuration
```
/eidolon-unchained config set display_method ACTION_BAR
/eidolon-unchained config set display_method ENHANCED_CHAT  
/eidolon-unchained config set display_method TITLE_SUBTITLE
/eidolon-unchained config set display_method AUTO
```

### Config File (`eidolonunchained-common.toml`)
```toml
[display]
    # How deity responses are displayed to players
    # Options: "TITLE_SUBTITLE", "ACTION_BAR", "ENHANCED_CHAT", "AUTO"
    display_method = "ACTION_BAR"
    
    # Whether to use prominent display (when false, uses simple chat)
    use_prominent_display = true
    
    # Duration for title/subtitle display (in ticks, 20 = 1 second)
    display_duration_ticks = 60
    
    # Maximum characters before switching display methods
    max_subtitle_length = 60
```

## Display Method Comparison

| Method | Readability | Persistence | Screen Space | Best Use Case |
|--------|-------------|-------------|--------------|---------------|
| **ACTION_BAR** | ★★★★★ | ★★★★☆ | ★★★★★ | Daily interactions |
| **ENHANCED_CHAT** | ★★★★★ | ★★★★★ | ★★★☆☆ | Long conversations |
| **TITLE_SUBTITLE** | ★★☆☆☆ | ★★☆☆☆ | ★★☆☆☆ | Dramatic moments |
| **AUTO** | ★★★★★ | ★★★★☆ | ★★★★☆ | Set and forget |

## Technical Implementation

### Action Bar System
- **Multiple Sends**: Messages sent 4 times over 3 seconds for persistence
- **Centered Display**: Text appears in optimal reading position
- **Compact Format**: `Deity Name: Message` in readable length

### Enhanced Chat System  
- **Visual Separators**: Decorative borders to distinguish deity messages
- **Word Wrapping**: Intelligent line breaks at 50-60 characters
- **Sentence Splitting**: Long responses broken into readable segments
- **Color Coding**: Deity names in gold, messages in white

### Legacy Compatibility
- **Original System**: Still available for users who prefer it
- **Configurable Timing**: Fade in/out and display duration settings
- **Fallback Support**: Graceful degradation if new systems fail

## Performance Considerations

### Action Bar Persistence
- Uses asynchronous threading for multiple message sends
- Minimal performance impact (4 packets over 3 seconds)
- Automatically cleaned up after completion

### Enhanced Chat Formatting
- Pre-calculated word wrapping prevents runtime lag
- Efficient string building for complex messages
- No excessive object creation

### Memory Usage
- Action bar messages: Temporary, automatic cleanup
- Chat messages: Stored in normal chat history
- No additional persistent storage required

## User Experience Improvements

### Before (Title/Subtitle)
```
Issues:
❌ Text stretched across full screen width
❌ Disappeared too quickly (2-3 seconds)
❌ Hard to read on wide monitors
❌ No reference after disappearing
```

### After (ACTION_BAR/ENHANCED_CHAT)
```
Improvements:
✅ Text centered and compact
✅ Visible for 3-4 seconds with repeated display
✅ Easy to read on any screen size
✅ Enhanced chat provides permanent reference
✅ Smart auto-selection based on content length
✅ Professional formatting with visual appeal
```

## Migration Guide

### For Players
1. **Default Experience**: No action needed - ACTION_BAR is now default
2. **Prefer Chat**: Use `/eidolon-unchained config set display_method ENHANCED_CHAT`
3. **Keep Old Style**: Use `/eidolon-unchained config set display_method TITLE_SUBTITLE`
4. **Auto-Optimize**: Use `/eidolon-unchained config set display_method AUTO`

### For Server Administrators
1. **Server Config**: Set `display_method = "AUTO"` in server config
2. **Player Choice**: Allow players to set individual preferences
3. **Performance**: ACTION_BAR and ENHANCED_CHAT have better performance than title/subtitle

## Examples in Action

### Short Message (ACTION_BAR)
```
Player casts nature communion at 45% health
[Action Bar] Verdania: I sense your wounds, faithful one. Shall I heal you?
[Chat] [Verdania has spoken]
```

### Long Message (ENHANCED_CHAT)
```
Player requests guidance from Lumina
═══════════════════════════════════════
⟦ Lumina the Sacred Guardian ⟧
My radiant sight perceives your noble heart, devoted 
follower. You seek wisdom in these troubled times, and 
your faith burns bright like a beacon in the darkness.

The path ahead is treacherous, but remember that even 
the smallest light can banish the greatest shadow. Trust 
in your convictions and let righteousness guide your steps.
═══════════════════════════════════════
```

### Auto Mode Behavior
```
Short request: "Please help" 
→ Uses ACTION_BAR (quick and clear)

Long conversation: "Tell me about the nature spirits and..."
→ Uses ENHANCED_CHAT (full formatting)
```

This new system provides a much better user experience while maintaining full backward compatibility and giving players complete control over how they receive divine messages.
