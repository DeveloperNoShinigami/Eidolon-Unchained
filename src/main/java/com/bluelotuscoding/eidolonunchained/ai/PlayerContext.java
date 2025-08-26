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
        this.progressionLevel = "beginner"; // Default progression level
        this.recentActions = new ArrayList<>();
    }
    
    public PlayerContext(ServerPlayer player, DatapackDeity deity) {
        this.playerName = player.getName().getString();
        this.reputation = deity.getPlayerReputation(player);
        this.progressionLevel = "beginner"; // Default progression level
        this.recentActions = new ArrayList<>();
        // Additional context can be added here
    }
}
