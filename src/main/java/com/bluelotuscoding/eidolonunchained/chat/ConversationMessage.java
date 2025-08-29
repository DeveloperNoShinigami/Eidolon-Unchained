package com.bluelotuscoding.eidolonunchained.chat;

import net.minecraft.nbt.CompoundTag;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a single conversation message with NBT serialization support.
 * Used by the server-side conversation history system.
 */
public class ConversationMessage {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final String speaker;
    private final String message;
    private final LocalDateTime timestamp;
    
    public ConversationMessage(String speaker, String message, LocalDateTime timestamp) {
        this.speaker = speaker;
        this.message = message;
        this.timestamp = timestamp;
    }
    
    public String getSpeaker() {
        return speaker;
    }
    
    public String getMessage() {
        return message;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public String getFormattedTimestamp() {
        return timestamp.format(FORMATTER);
    }
    
    /**
     * Convert this message to NBT for world data storage
     */
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("speaker", speaker);
        tag.putString("message", message);
        tag.putString("timestamp", timestamp.format(FORMATTER));
        return tag;
    }
    
    /**
     * Create a message from NBT data
     */
    public static ConversationMessage fromNBT(CompoundTag tag) {
        String speaker = tag.getString("speaker");
        String message = tag.getString("message");
        String timestampStr = tag.getString("timestamp");
        
        LocalDateTime timestamp;
        try {
            timestamp = LocalDateTime.parse(timestampStr, FORMATTER);
        } catch (Exception e) {
            timestamp = LocalDateTime.now(); // Fallback to current time
        }
        
        return new ConversationMessage(speaker, message, timestamp);
    }
    
    /**
     * Get formatted string representation for display
     */
    public String getDisplayString() {
        return String.format("[%s] %s: %s", getFormattedTimestamp(), speaker, message);
    }
    
    @Override
    public String toString() {
        return getDisplayString();
    }
}
