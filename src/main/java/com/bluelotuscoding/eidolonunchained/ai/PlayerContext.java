package com.bluelotuscoding.eidolonunchained.ai;

import net.minecraft.server.level.ServerPlayer;
import com.bluelotuscoding.eidolonunchained.deity.DatapackDeity;
import java.util.*;

/**
 * Context information about a player for AI decision making
 */
public class PlayerContext {
    public String playerName;
    public double reputation;
    public int researchCount;
    public String progressionLevel; // beginner, novice, intermediate, advanced, master
    public String location;
    public String biome;
    public String timeOfDay; // dawn, day, dusk, night, midnight
    public String weather; // clear, rain, thunder
    public List<String> recentActions = new ArrayList<>();
    public String inventorySummary;
    
    public PlayerContext(String playerName) {
        this.playerName = playerName;
    }
    
    public PlayerContext(ServerPlayer player, DatapackDeity deity) {
        this.playerName = player.getName().getString();
        this.reputation = deity.getPlayerReputation(player);
        this.progressionLevel = deity.getProgressionLevel(player);
        this.researchCount = deity.getResearchCount(player);
        
        // Initialize location and environmental context
        this.biome = player.level().getBiome(player.blockPosition()).toString();
        this.location = player.blockPosition().toString();
        
        // Initialize time context
        long dayTime = player.level().getDayTime() % 24000;
        if (dayTime < 6000) {
            this.timeOfDay = "day";
        } else if (dayTime < 12000) {
            this.timeOfDay = "afternoon"; 
        } else if (dayTime < 13000) {
            this.timeOfDay = "dusk";
        } else if (dayTime < 22000) {
            this.timeOfDay = "night";
        } else {
            this.timeOfDay = "dawn";
        }
        
        // Initialize weather context
        if (player.level().isThundering()) {
            this.weather = "thunder";
        } else if (player.level().isRaining()) {
            this.weather = "rain";
        } else {
            this.weather = "clear";
        }
        
        // Additional context can be added here
    }
}
