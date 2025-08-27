package com.bluelotuscoding.eidolonunchained.ai;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bluelotuscoding.eidolonunchained.EidolonUnchained;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks comprehensive player context for AI deity interactions including:
 * - Ritual and chant history
 * - Favor points system (separate from reputation)
 * - Task assignments and completion
 * - Recent actions and behavior patterns
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerContextTracker {
    
    // In-memory tracking for active sessions
    private static final Map<UUID, EnhancedPlayerContext> playerContexts = new ConcurrentHashMap<>();
    
    // NBT keys for persistence
    private static final String NBT_RITUAL_HISTORY = "ritual_history";
    private static final String NBT_FAVOR_POINTS = "favor_points";
    private static final String NBT_ACTIVE_TASKS = "active_tasks";
    private static final String NBT_COMPLETED_TASKS = "completed_tasks";
    private static final String NBT_CHANT_COUNT = "chant_count";
    
    /**
     * Enhanced player context with comprehensive tracking
     */
    public static class EnhancedPlayerContext {
        public String playerName;
        public UUID playerId;
        
        // Ritual and chant tracking
        public List<RitualEntry> ritualHistory = new ArrayList<>();
        public Map<ResourceLocation, Integer> chantCounts = new HashMap<>();
        public List<String> recentActions = new ArrayList<>();
        
        // Favor system (separate from reputation)
        public Map<ResourceLocation, Integer> favorPoints = new HashMap<>();
        
        // Task system
        public Map<String, PlayerTask> activeTasks = new HashMap<>();
        public List<String> completedTasks = new ArrayList<>();
        
        // Behavioral tracking
        public long lastPrayerTime = 0;
        public int consecutivePrayers = 0;
        public String currentBiome = "";
        public long sessionStartTime = System.currentTimeMillis();
        
        public EnhancedPlayerContext(ServerPlayer player) {
            this.playerName = player.getName().getString();
            this.playerId = player.getUUID();
            loadFromNBT(player);
        }
        
        public void addRitual(String ritualName, ResourceLocation deityId, boolean successful) {
            RitualEntry entry = new RitualEntry(ritualName, deityId, System.currentTimeMillis(), successful);
            ritualHistory.add(0, entry); // Add to front
            
            // Keep only last 20 rituals to prevent memory bloat
            if (ritualHistory.size() > 20) {
                ritualHistory = ritualHistory.subList(0, 20);
            }
            
            // Award favor points for successful rituals
            if (successful && deityId != null) {
                int currentFavor = favorPoints.getOrDefault(deityId, 0);
                favorPoints.put(deityId, currentFavor + 1);
            }
            
            addAction("performed " + ritualName + (successful ? " successfully" : " unsuccessfully"));
        }
        
        public void addChant(ResourceLocation chantId, ResourceLocation deityId, boolean successful) {
            int currentCount = chantCounts.getOrDefault(chantId, 0);
            chantCounts.put(chantId, currentCount + 1);
            
            // Award favor for successful chants
            if (successful && deityId != null) {
                int currentFavor = favorPoints.getOrDefault(deityId, 0);
                favorPoints.put(deityId, currentFavor + 1);
            }
            
            addAction("chanted " + chantId.getPath() + (successful ? " successfully" : " unsuccessfully"));
        }
        
        public void addAction(String action) {
            recentActions.add(0, action); // Add to front
            
            // Keep only last 15 actions
            if (recentActions.size() > 15) {
                recentActions = recentActions.subList(0, 15);
            }
        }
        
        public void assignTask(String taskId, String description, ResourceLocation deityId, int favorReward) {
            PlayerTask task = new PlayerTask(taskId, description, deityId, System.currentTimeMillis(), favorReward);
            activeTasks.put(taskId, task);
            addAction("received task: " + description);
        }
        
        public boolean completeTask(String taskId) {
            PlayerTask task = activeTasks.remove(taskId);
            if (task != null) {
                completedTasks.add(0, taskId); // Add to front
                
                // Award favor points
                if (task.deityId != null) {
                    int currentFavor = favorPoints.getOrDefault(task.deityId, 0);
                    favorPoints.put(task.deityId, currentFavor + task.favorReward);
                }
                
                addAction("completed task: " + task.description);
                
                // Keep only last 50 completed tasks
                if (completedTasks.size() > 50) {
                    completedTasks = completedTasks.subList(0, 50);
                }
                
                return true;
            }
            return false;
        }
        
        public String getContextSummary() {
            StringBuilder summary = new StringBuilder();
            
            // Recent actions
            if (!recentActions.isEmpty()) {
                summary.append("Recent actions: ").append(String.join(", ", recentActions.subList(0, Math.min(5, recentActions.size()))));
            }
            
            // Ritual summary
            if (!ritualHistory.isEmpty()) {
                long successfulRituals = ritualHistory.stream().filter(r -> r.successful).count();
                summary.append(". Rituals performed: ").append(ritualHistory.size())
                       .append(" (").append(successfulRituals).append(" successful)");
            }
            
            // Active tasks
            if (!activeTasks.isEmpty()) {
                summary.append(". Active tasks: ").append(activeTasks.size());
            }
            
            return summary.toString();
        }
        
        public void saveToNBT(ServerPlayer player) {
            CompoundTag playerData = player.getPersistentData();
            CompoundTag modData = playerData.getCompound(EidolonUnchained.MODID);
            
            // Save ritual history
            CompoundTag ritualTag = new CompoundTag();
            for (int i = 0; i < Math.min(ritualHistory.size(), 20); i++) {
                RitualEntry ritual = ritualHistory.get(i);
                CompoundTag entryTag = new CompoundTag();
                entryTag.putString("name", ritual.ritualName);
                entryTag.putString("deity", ritual.deityId != null ? ritual.deityId.toString() : "");
                entryTag.putLong("time", ritual.timestamp);
                entryTag.putBoolean("successful", ritual.successful);
                ritualTag.put("ritual_" + i, entryTag);
            }
            modData.put(NBT_RITUAL_HISTORY, ritualTag);
            
            // Save favor points
            CompoundTag favorTag = new CompoundTag();
            for (Map.Entry<ResourceLocation, Integer> entry : favorPoints.entrySet()) {
                favorTag.putInt(entry.getKey().toString(), entry.getValue());
            }
            modData.put(NBT_FAVOR_POINTS, favorTag);
            
            // Save chant counts
            CompoundTag chantTag = new CompoundTag();
            for (Map.Entry<ResourceLocation, Integer> entry : chantCounts.entrySet()) {
                chantTag.putInt(entry.getKey().toString(), entry.getValue());
            }
            modData.put(NBT_CHANT_COUNT, chantTag);
            
            // Save active tasks
            CompoundTag taskTag = new CompoundTag();
            for (Map.Entry<String, PlayerTask> entry : activeTasks.entrySet()) {
                PlayerTask task = entry.getValue();
                CompoundTag taskEntryTag = new CompoundTag();
                taskEntryTag.putString("description", task.description);
                taskEntryTag.putString("deity", task.deityId != null ? task.deityId.toString() : "");
                taskEntryTag.putLong("assigned_time", task.assignedTime);
                taskEntryTag.putInt("favor_reward", task.favorReward);
                taskTag.put(entry.getKey(), taskEntryTag);
            }
            modData.put(NBT_ACTIVE_TASKS, taskTag);
            
            // Save completed tasks (last 30)
            CompoundTag completedTag = new CompoundTag();
            for (int i = 0; i < Math.min(completedTasks.size(), 30); i++) {
                completedTag.putString("task_" + i, completedTasks.get(i));
            }
            modData.put(NBT_COMPLETED_TASKS, completedTag);
            
            playerData.put(EidolonUnchained.MODID, modData);
        }
        
        private void loadFromNBT(ServerPlayer player) {
            CompoundTag playerData = player.getPersistentData();
            CompoundTag modData = playerData.getCompound(EidolonUnchained.MODID);
            
            // Load ritual history
            if (modData.contains(NBT_RITUAL_HISTORY)) {
                CompoundTag ritualTag = modData.getCompound(NBT_RITUAL_HISTORY);
                for (String key : ritualTag.getAllKeys()) {
                    CompoundTag entryTag = ritualTag.getCompound(key);
                    String name = entryTag.getString("name");
                    String deityStr = entryTag.getString("deity");
                    ResourceLocation deityId = deityStr.isEmpty() ? null : new ResourceLocation(deityStr);
                    long time = entryTag.getLong("time");
                    boolean successful = entryTag.getBoolean("successful");
                    ritualHistory.add(new RitualEntry(name, deityId, time, successful));
                }
            }
            
            // Load favor points
            if (modData.contains(NBT_FAVOR_POINTS)) {
                CompoundTag favorTag = modData.getCompound(NBT_FAVOR_POINTS);
                for (String key : favorTag.getAllKeys()) {
                    favorPoints.put(new ResourceLocation(key), favorTag.getInt(key));
                }
            }
            
            // Load chant counts
            if (modData.contains(NBT_CHANT_COUNT)) {
                CompoundTag chantTag = modData.getCompound(NBT_CHANT_COUNT);
                for (String key : chantTag.getAllKeys()) {
                    chantCounts.put(new ResourceLocation(key), chantTag.getInt(key));
                }
            }
            
            // Load active tasks
            if (modData.contains(NBT_ACTIVE_TASKS)) {
                CompoundTag taskTag = modData.getCompound(NBT_ACTIVE_TASKS);
                for (String key : taskTag.getAllKeys()) {
                    CompoundTag taskEntryTag = taskTag.getCompound(key);
                    String description = taskEntryTag.getString("description");
                    String deityStr = taskEntryTag.getString("deity");
                    ResourceLocation deityId = deityStr.isEmpty() ? null : new ResourceLocation(deityStr);
                    long assignedTime = taskEntryTag.getLong("assigned_time");
                    int favorReward = taskEntryTag.getInt("favor_reward");
                    activeTasks.put(key, new PlayerTask(key, description, deityId, assignedTime, favorReward));
                }
            }
            
            // Load completed tasks
            if (modData.contains(NBT_COMPLETED_TASKS)) {
                CompoundTag completedTag = modData.getCompound(NBT_COMPLETED_TASKS);
                for (String key : completedTag.getAllKeys()) {
                    completedTasks.add(completedTag.getString(key));
                }
            }
        }
    }
    
    /**
     * Represents a ritual or chant entry in history
     */
    public static class RitualEntry {
        public final String ritualName;
        public final ResourceLocation deityId;
        public final long timestamp;
        public final boolean successful;
        
        public RitualEntry(String ritualName, ResourceLocation deityId, long timestamp, boolean successful) {
            this.ritualName = ritualName;
            this.deityId = deityId;
            this.timestamp = timestamp;
            this.successful = successful;
        }
        
        public String getFormattedTime() {
            long minutes = (System.currentTimeMillis() - timestamp) / (1000 * 60);
            if (minutes < 60) return minutes + " minutes ago";
            long hours = minutes / 60;
            if (hours < 24) return hours + " hours ago";
            long days = hours / 24;
            return days + " days ago";
        }
    }
    
    /**
     * Represents a task assigned to a player
     */
    public static class PlayerTask {
        public final String taskId;
        public final String description;
        public final ResourceLocation deityId;
        public final long assignedTime;
        public final int favorReward;
        
        public PlayerTask(String taskId, String description, ResourceLocation deityId, long assignedTime, int favorReward) {
            this.taskId = taskId;
            this.description = description;
            this.deityId = deityId;
            this.assignedTime = assignedTime;
            this.favorReward = favorReward;
        }
    }
    
    // ===== EVENT HANDLERS =====
    
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            getOrCreateContext(player.getUUID(), player);
        }
    }
    
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            EnhancedPlayerContext context = playerContexts.get(player.getUUID());
            if (context != null) {
                context.saveToNBT(player);
                playerContexts.remove(player.getUUID());
            }
        }
    }
    
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            // Every 5 seconds, check for biome changes
            if (player.tickCount % 100 == 0) {
                EnhancedPlayerContext context = getOrCreateContext(player.getUUID(), player);
                String currentBiome = player.level().getBiome(player.blockPosition()).toString();
                if (!currentBiome.equals(context.currentBiome)) {
                    context.currentBiome = currentBiome;
                    context.addAction("entered " + currentBiome);
                }
            }
        }
    }
    
    // ===== PUBLIC API =====
    
    public static EnhancedPlayerContext getOrCreateContext(UUID playerId, ServerPlayer player) {
        return playerContexts.computeIfAbsent(playerId, k -> new EnhancedPlayerContext(player));
    }
    
    public static EnhancedPlayerContext getContext(UUID playerId) {
        return playerContexts.get(playerId);
    }
    
    /**
     * Record a ritual or chant performance
     */
    public static void recordRitual(ServerPlayer player, String ritualName, ResourceLocation deityId, boolean successful) {
        EnhancedPlayerContext context = getOrCreateContext(player.getUUID(), player);
        context.addRitual(ritualName, deityId, successful);
    }
    
    /**
     * Record a chant performance
     */
    public static void recordChant(ServerPlayer player, ResourceLocation chantId, ResourceLocation deityId, boolean successful) {
        EnhancedPlayerContext context = getOrCreateContext(player.getUUID(), player);
        context.addChant(chantId, deityId, successful);
    }
    
    /**
     * Get favor points for a deity
     */
    public static int getFavorPoints(ServerPlayer player, ResourceLocation deityId) {
        EnhancedPlayerContext context = getOrCreateContext(player.getUUID(), player);
        return context.favorPoints.getOrDefault(deityId, 0);
    }
    
    /**
     * Add favor points for a deity
     */
    public static void addFavorPoints(ServerPlayer player, ResourceLocation deityId, int points) {
        EnhancedPlayerContext context = getOrCreateContext(player.getUUID(), player);
        int current = context.favorPoints.getOrDefault(deityId, 0);
        context.favorPoints.put(deityId, current + points);
        context.addAction("gained " + points + " favor with " + deityId.getPath());
    }
    
    /**
     * Assign a task to a player
     */
    public static void assignTask(ServerPlayer player, String taskId, String description, ResourceLocation deityId, int favorReward) {
        EnhancedPlayerContext context = getOrCreateContext(player.getUUID(), player);
        context.assignTask(taskId, description, deityId, favorReward);
    }
    
    /**
     * Complete a task
     */
    public static boolean completeTask(ServerPlayer player, String taskId) {
        EnhancedPlayerContext context = getOrCreateContext(player.getUUID(), player);
        return context.completeTask(taskId);
    }
    
    /**
     * Get enhanced context summary for AI prompts
     */
    public static String getContextSummary(ServerPlayer player) {
        EnhancedPlayerContext context = getOrCreateContext(player.getUUID(), player);
        return context.getContextSummary();
    }
    
    /**
     * Get ritual history summary
     */
    public static String getRitualHistorySummary(ServerPlayer player) {
        EnhancedPlayerContext context = getOrCreateContext(player.getUUID(), player);
        if (context.ritualHistory.isEmpty()) return "No recent rituals";
        
        StringBuilder summary = new StringBuilder();
        List<RitualEntry> recent = context.ritualHistory.subList(0, Math.min(5, context.ritualHistory.size()));
        for (RitualEntry ritual : recent) {
            summary.append(ritual.ritualName)
                   .append(" (").append(ritual.getFormattedTime()).append(", ")
                   .append(ritual.successful ? "successful" : "failed").append("), ");
        }
        
        return summary.length() > 2 ? summary.substring(0, summary.length() - 2) : summary.toString();
    }
}
