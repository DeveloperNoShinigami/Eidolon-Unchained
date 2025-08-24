package com.bluelotuscoding.eidolonunchained.ai;

import java.util.*;

/**
 * Configuration for automatic command judgment based on player reputation and context
 */
public class JudgmentConfig {
    public int blessingThreshold = 25;
    public int curseThreshold = -25;
    public List<String> blessingCommands = new ArrayList<>();
    public List<String> curseCommands = new ArrayList<>();
    public List<String> neutralCommands = new ArrayList<>();
    
    public JudgmentConfig() {
        // Default blessing commands
        blessingCommands.addAll(Arrays.asList(
            "give {player} minecraft:diamond 1",
            "effect give {player} minecraft:regeneration 30 1"
        ));
        
        // Default curse commands  
        curseCommands.addAll(Arrays.asList(
            "effect give {player} minecraft:weakness 60 0",
            "effect give {player} minecraft:slowness 60 0"
        ));
        
        // Default neutral commands
        neutralCommands.addAll(Arrays.asList(
            "playsound minecraft:block.bell.use neutral {player}"
        ));
    }
}
