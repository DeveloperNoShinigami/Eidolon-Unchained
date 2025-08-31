package com.bluelotuscoding.eidolonunchained.integration.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Real-time world data reader that accesses .dat files and provides dynamic context to AI
 */
public class WorldDataReader {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldDataReader.class);
    
    /**
     * Build comprehensive real-time world context for AI
     */
    public static String buildRealTimeWorldContext(ServerPlayer player) {
        StringBuilder context = new StringBuilder();
        ServerLevel level = player.serverLevel();
        
        // Basic player info
        context.append("=== PLAYER STATUS ===\n");
        context.append("Player: ").append(player.getName().getString()).append("\n");
        context.append("Dimension: ").append(level.dimension().location()).append("\n");
        
        // Real-time world data
        context.append("\n=== WORLD STATE ===\n");
        context.append(getWorldTimeData(level));
        context.append(getWeatherData(level));
        context.append(getDifficultyData(level));
        
        // Player location analysis
        context.append("\n=== LOCATION ANALYSIS ===\n");
        context.append(getLocationData(player, level));
        context.append(getBiomeData(player, level));
        
        // Nearby entities and threats
        context.append("\n=== NEARBY ENTITIES ===\n");
        context.append(getNearbyEntitiesData(player, level));
        
        // World statistics from level.dat
        context.append("\n=== WORLD STATISTICS ===\n");
        context.append(getWorldStatistics(level));
        
        // Player data from playerdata files
        context.append("\n=== PLAYER STATISTICS ===\n");
        context.append(getPlayerStatistics(player));
        
        return context.toString();
    }
    
    private static String getWorldTimeData(ServerLevel level) {
        StringBuilder data = new StringBuilder();
        
        long worldTime = level.getGameTime();
        long dayTime = level.getDayTime() % 24000;
        long day = level.getDayTime() / 24000;
        
        String timeOfDay = getTimeOfDay(dayTime);
        
        data.append("World Time: ").append(worldTime).append(" ticks\n");
        data.append("Day: ").append(day).append("\n");
        data.append("Time of Day: ").append(timeOfDay).append(" (").append(dayTime).append(" ticks)\n");
        
        return data.toString();
    }
    
    private static String getWeatherData(ServerLevel level) {
        StringBuilder data = new StringBuilder();
        
        boolean isRaining = level.isRaining();
        boolean isThundering = level.isThundering();
        float rainLevel = level.getRainLevel(0.0f);
        float thunderLevel = level.getThunderLevel(0.0f);
        
        data.append("Weather: ");
        if (isThundering) {
            data.append("Thunderstorm");
        } else if (isRaining) {
            data.append("Raining");
        } else {
            data.append("Clear");
        }
        data.append("\n");
        
        if (rainLevel > 0) {
            data.append("Rain Intensity: ").append(String.format("%.2f", rainLevel)).append("\n");
        }
        if (thunderLevel > 0) {
            data.append("Thunder Intensity: ").append(String.format("%.2f", thunderLevel)).append("\n");
        }
        
        return data.toString();
    }
    
    private static String getDifficultyData(ServerLevel level) {
        StringBuilder data = new StringBuilder();
        
        data.append("Difficulty: ").append(level.getDifficulty().getDisplayName().getString()).append("\n");
        data.append("Spawn Protection: ").append(level.getServer().getSpawnProtectionRadius()).append(" blocks\n");
        
        return data.toString();
    }
    
    private static String getLocationData(ServerPlayer player, ServerLevel level) {
        StringBuilder data = new StringBuilder();
        
        BlockPos pos = player.blockPosition();
        ChunkPos chunkPos = new ChunkPos(pos);
        
        data.append("Position: ").append(pos.getX()).append(", ").append(pos.getY()).append(", ").append(pos.getZ()).append("\n");
        data.append("Chunk: ").append(chunkPos.x).append(", ").append(chunkPos.z).append("\n");
        
        // Check if in structure
        String structures = getStructuresAtPosition(player, level);
        if (!structures.isEmpty()) {
            data.append("Structures: ").append(structures).append("\n");
        }
        
        // Light level analysis
        int skyLight = level.getBrightness(net.minecraft.world.level.LightLayer.SKY, pos);
        int blockLight = level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, pos);
        
        data.append("Light - Sky: ").append(skyLight).append(", Block: ").append(blockLight).append("\n");
        
        return data.toString();
    }
    
    private static String getBiomeData(ServerPlayer player, ServerLevel level) {
        StringBuilder data = new StringBuilder();
        
        BlockPos pos = player.blockPosition();
        Biome biome = level.getBiome(pos).value();
        ResourceLocation biomeId = ForgeRegistries.BIOMES.getKey(biome);
        
        data.append("Biome: ").append(biomeId != null ? biomeId.toString() : "unknown").append("\n");
        data.append("Temperature: ").append(String.format("%.2f", biome.getBaseTemperature())).append("\n");
        data.append("Humidity: ").append(String.format("%.2f", biome.getPrecipitationAt(pos))).append("\n");
        
        return data.toString();
    }
    
    private static String getNearbyEntitiesData(ServerPlayer player, ServerLevel level) {
        StringBuilder data = new StringBuilder();
        
        // Get entities within 32 blocks
        List<Entity> nearbyEntities = level.getEntitiesOfClass(Entity.class, 
            player.getBoundingBox().inflate(32), 
            entity -> entity != player);
        
        Map<String, Integer> entityCounts = new HashMap<>();
        int monsters = 0;
        int animals = 0;
        int players = 0;
        
        for (Entity entity : nearbyEntities) {
            String entityType = entity.getType().getDescription().getString();
            entityCounts.merge(entityType, 1, Integer::sum);
            
            if (entity instanceof Monster) monsters++;
            else if (entity instanceof Animal) animals++;
            else if (entity instanceof Player) players++;
        }
        
        data.append("Nearby Entities (32 blocks): ").append(nearbyEntities.size()).append(" total\n");
        data.append("- Monsters: ").append(monsters).append("\n");
        data.append("- Animals: ").append(animals).append("\n");
        data.append("- Players: ").append(players).append("\n");
        
        if (!entityCounts.isEmpty()) {
            data.append("Entity Types: ");
            data.append(entityCounts.entrySet().stream()
                .map(entry -> entry.getKey() + "(" + entry.getValue() + ")")
                .collect(Collectors.joining(", ")));
            data.append("\n");
        }
        
        return data.toString();
    }
    
    private static String getWorldStatistics(ServerLevel level) {
        StringBuilder data = new StringBuilder();
        
        try {
            // Try to read level.dat
            File worldDir = level.getServer().getWorldPath(LevelResource.ROOT).toFile();
            File levelDat = new File(worldDir, "level.dat");
            
            if (levelDat.exists()) {
                CompoundTag levelData = NbtIo.readCompressed(levelDat);
                CompoundTag data_tag = levelData.getCompound("Data");
                
                data.append("World Name: ").append(data_tag.getString("LevelName")).append("\n");
                data.append("World Seed: ").append(data_tag.getLong("RandomSeed")).append("\n");
                data.append("Game Mode: ").append(data_tag.getInt("GameType")).append("\n");
                data.append("Hardcore: ").append(data_tag.getBoolean("hardcore")).append("\n");
                data.append("Allow Commands: ").append(data_tag.getBoolean("allowCommands")).append("\n");
                
                if (data_tag.contains("SpawnX")) {
                    int spawnX = data_tag.getInt("SpawnX");
                    int spawnY = data_tag.getInt("SpawnY"); 
                    int spawnZ = data_tag.getInt("SpawnZ");
                    data.append("World Spawn: ").append(spawnX).append(", ").append(spawnY).append(", ").append(spawnZ).append("\n");
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Could not read level.dat: {}", e.getMessage());
            data.append("World data: Unable to access level.dat\n");
        }
        
        return data.toString();
    }
    
    private static String getPlayerStatistics(ServerPlayer player) {
        StringBuilder data = new StringBuilder();
        
        try {
            // Player stats from server data
            data.append("Health: ").append(String.format("%.1f", player.getHealth())).append("/").append(String.format("%.1f", player.getMaxHealth())).append("\n");
            data.append("Food: ").append(player.getFoodData().getFoodLevel()).append("/20\n");
            data.append("Experience Level: ").append(player.experienceLevel).append("\n");
            data.append("Score: ").append(player.getScore()).append("\n");
            
            // Game mode
            data.append("Game Mode: ").append(player.gameMode.getGameModeForPlayer().getName()).append("\n");
            
            // Last death location if available
            if (player.getLastDeathLocation().isPresent()) {
                var deathLoc = player.getLastDeathLocation().get();
                data.append("Last Death: ").append(deathLoc.dimension().location()).append(" at ").append(deathLoc.pos()).append("\n");
            }
            
        } catch (Exception e) {
            LOGGER.warn("Could not read player statistics: {}", e.getMessage());
            data.append("Player data: Limited access\n");
        }
        
        return data.toString();
    }
    
    private static String getStructuresAtPosition(ServerPlayer player, ServerLevel level) {
        // This would require more complex structure detection
        // For now, return empty - can be expanded later
        return "";
    }
    
    private static String getTimeOfDay(long dayTime) {
        if (dayTime < 6000) return "Morning";
        else if (dayTime < 12000) return "Day";
        else if (dayTime < 13800) return "Evening";
        else if (dayTime < 22200) return "Night";
        else return "Late Night";
    }
}
