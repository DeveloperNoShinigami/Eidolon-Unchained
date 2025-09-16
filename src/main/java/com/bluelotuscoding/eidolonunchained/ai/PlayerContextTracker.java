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
import java.util.function.BiConsumer;

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
    
    // SMART: Biome change listener system (avoids duplicate ticking)
    private static final List<BiConsumer<ServerPlayer, String>> biomeChangeListeners = new ArrayList<>();
    
    // NBT keys for persistence
    private static final String NBT_RITUAL_HISTORY = "ritual_history";
    private static final String NBT_ACTIVE_TASKS = "active_tasks";
    private static final String NBT_COMPLETED_TASKS = "completed_tasks";
    private static final String NBT_COMPLETED_RITUALS = "completed_rituals";
    private static final String NBT_CHANT_COUNT = "chant_count";
    private static final String NBT_UNLOCKED_PROG = "unlocked_progressions";
    private static final String NBT_KILLS = "kills_by_entity";
    private static final String NBT_MINED = "mined_blocks";
    private static final String NBT_ITEMS_USED = "items_used";
    private static final String NBT_VISITED_BIOMES = "visited_biomes";
    private static final String NBT_TRIGGERED_RESEARCH = "triggered_research";
    
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
        
        // Task system
        public Map<String, PlayerTask> activeTasks = new HashMap<>();
        public List<String> completedTasks = new ArrayList<>();
        public List<String> completedRituals = new ArrayList<>(); // Track completed rituals for task checking
        public java.util.Set<String> unlockedProgressions = new java.util.HashSet<>();
        
        // Telemetry counters for JSON requirements
        public Map<String, Integer> killsByEntity = new HashMap<>(); // entity id -> kills
        public Map<String, Integer> minedBlocks = new HashMap<>(); // block id -> count
        public Map<String, Integer> itemsUsed = new HashMap<>(); // item id -> uses
        public java.util.Set<String> visitedBiomes = new java.util.HashSet<>();
        public java.util.Set<String> visitedStructures = new java.util.HashSet<>();
        public java.util.Set<String> triggeredResearchTracking = new java.util.HashSet<>(); // researchId:timestamp tracking
        
        // Behavioral tracking
        public long lastPrayerTime = 0;
        public int consecutivePrayers = 0;
        public String currentBiome = "";
        public long sessionStartTime = System.currentTimeMillis();

    // Fate offer flow
    public String pendingFateOfferTaskId = null; // currently offered fate (taskId)
    public ResourceLocation pendingFateOfferDeity = null; // deity who offered
    // Cooldown tracking for offers per deity id -> last offer epoch millis
    public Map<String, Long> lastFateOfferByDeity = new HashMap<>();

    // Natural language trigger cooldowns: key = deityId::triggerId -> last fired millis
    public Map<String, Long> triggerCooldowns = new HashMap<>();
        
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
            
            addAction("performed " + ritualName + (successful ? " successfully" : " unsuccessfully"));
        }
        
        public void addChant(ResourceLocation chantId, ResourceLocation deityId, boolean successful) {
            int currentCount = chantCounts.getOrDefault(chantId, 0);
            chantCounts.put(chantId, currentCount + 1);
            
            addAction("chanted " + chantId.getPath() + (successful ? " successfully" : " unsuccessfully"));
        }
        
        public void addAction(String action) {
            recentActions.add(0, action); // Add to front
            
            // Keep only last 15 actions
            if (recentActions.size() > 15) {
                recentActions = recentActions.subList(0, 15);
            }
        }
        
        public void assignTask(String taskId, String description, ResourceLocation deityId, int reputationReward) {
            PlayerTask task = new PlayerTask(taskId, description, deityId, System.currentTimeMillis(), reputationReward);
            activeTasks.put(taskId, task);
            addAction("received task: " + description);
        }
        
        public boolean completeTask(String taskId) {
            PlayerTask task = activeTasks.remove(taskId);
            if (task != null) {
                completedTasks.add(0, taskId); // Add to front
                
                addAction("completed task: " + task.description);
                
                // Keep only last 50 completed tasks
                if (completedTasks.size() > 50) {
                    completedTasks = completedTasks.subList(0, 50);
                }
                
                return true;
            }
            return false;
        }

        public void addTriggeredResearch(String researchId) {
            String trackingEntry = researchId + ":" + System.currentTimeMillis();
            triggeredResearchTracking.add(trackingEntry);
            addAction("discovered research: " + researchId);
        }

        public long getTriggeredResearchCount(String researchId) {
            String trackingPrefix = researchId + ":";
            return triggeredResearchTracking.stream()
                .filter(entry -> entry.startsWith(trackingPrefix))
                .count();
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
                taskEntryTag.putInt("reputation_reward", task.reputationReward);
                taskTag.put(entry.getKey(), taskEntryTag);
            }
            modData.put(NBT_ACTIVE_TASKS, taskTag);

            // Save completed tasks (last 30)
            CompoundTag completedTag = new CompoundTag();
            for (int i = 0; i < Math.min(completedTasks.size(), 30); i++) {
                completedTag.putString("task_" + i, completedTasks.get(i));
            }
            modData.put(NBT_COMPLETED_TASKS, completedTag);

            // Save completed rituals (last 50)
            CompoundTag ritualsTag = new CompoundTag();
            for (int i = 0; i < Math.min(completedRituals.size(), 50); i++) {
                ritualsTag.putString("ritual_" + i, completedRituals.get(i));
            }
            modData.put(NBT_COMPLETED_RITUALS, ritualsTag);

            // Save unlocked progressions
            CompoundTag progTag = new CompoundTag();
            int idx = 0;
            for (String p : unlockedProgressions) {
                progTag.putString("prog_" + (idx++), p);
            }
            modData.put(NBT_UNLOCKED_PROG, progTag);

            // Save counters
            CompoundTag killTag = new CompoundTag();
            for (Map.Entry<String, Integer> e : killsByEntity.entrySet()) killTag.putInt(e.getKey(), e.getValue());
            modData.put(NBT_KILLS, killTag);

            CompoundTag minedTag = new CompoundTag();
            for (Map.Entry<String, Integer> e : minedBlocks.entrySet()) minedTag.putInt(e.getKey(), e.getValue());
            modData.put(NBT_MINED, minedTag);

            CompoundTag usedTag = new CompoundTag();
            for (Map.Entry<String, Integer> e : itemsUsed.entrySet()) usedTag.putInt(e.getKey(), e.getValue());
            modData.put(NBT_ITEMS_USED, usedTag);

            CompoundTag biomesTag = new CompoundTag();
            int bi = 0;
            for (String b : visitedBiomes) biomesTag.putString("biome_" + (bi++), b);
            modData.put(NBT_VISITED_BIOMES, biomesTag);

            // Save triggered research tracking
            CompoundTag triggeredTag = new CompoundTag();
            int tri = 0;
            for (String t : triggeredResearchTracking) triggeredTag.putString("triggered_" + (tri++), t);
            modData.put(NBT_TRIGGERED_RESEARCH, triggeredTag);

            // Save pending fate offer and cooldowns
            if (pendingFateOfferTaskId != null) {
                modData.putString("pending_fate_offer_task", pendingFateOfferTaskId);
            }
            if (pendingFateOfferDeity != null) {
                modData.putString("pending_fate_offer_deity", pendingFateOfferDeity.toString());
            }
            CompoundTag offerCd = new CompoundTag();
            for (Map.Entry<String, Long> e : lastFateOfferByDeity.entrySet()) {
                offerCd.putLong(e.getKey(), e.getValue());
            }
            modData.put("fate_offer_cooldowns", offerCd);

            // Save NL trigger cooldowns
            CompoundTag trigCd = new CompoundTag();
            for (Map.Entry<String, Long> e : triggerCooldowns.entrySet()) {
                trigCd.putLong(e.getKey(), e.getValue());
            }
            modData.put("nl_trigger_cooldowns", trigCd);
            
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
                    int reputationReward = taskEntryTag.getInt("reputation_reward");
                    activeTasks.put(key, new PlayerTask(key, description, deityId, assignedTime, reputationReward));
                }
            }
            
            // Load completed tasks
            if (modData.contains(NBT_COMPLETED_TASKS)) {
                CompoundTag completedTag = modData.getCompound(NBT_COMPLETED_TASKS);
                for (String key : completedTag.getAllKeys()) {
                    completedTasks.add(completedTag.getString(key));
                }
            }
            
            // Load completed rituals
            if (modData.contains(NBT_COMPLETED_RITUALS)) {
                CompoundTag ritualsTag = modData.getCompound(NBT_COMPLETED_RITUALS);
                for (String key : ritualsTag.getAllKeys()) {
                    completedRituals.add(ritualsTag.getString(key));
                }
            }

            // Load unlocked progressions
            if (modData.contains(NBT_UNLOCKED_PROG)) {
                CompoundTag progTag = modData.getCompound(NBT_UNLOCKED_PROG);
                for (String key : progTag.getAllKeys()) unlockedProgressions.add(progTag.getString(key));
            }

            // Load counters
            if (modData.contains(NBT_KILLS)) {
                CompoundTag killTag = modData.getCompound(NBT_KILLS);
                for (String key : killTag.getAllKeys()) killsByEntity.put(key, killTag.getInt(key));
            }
            if (modData.contains(NBT_MINED)) {
                CompoundTag minedTag = modData.getCompound(NBT_MINED);
                for (String key : minedTag.getAllKeys()) minedBlocks.put(key, minedTag.getInt(key));
            }
            if (modData.contains(NBT_ITEMS_USED)) {
                CompoundTag usedTag = modData.getCompound(NBT_ITEMS_USED);
                for (String key : usedTag.getAllKeys()) itemsUsed.put(key, usedTag.getInt(key));
            }
            if (modData.contains(NBT_VISITED_BIOMES)) {
                CompoundTag biomesTag = modData.getCompound(NBT_VISITED_BIOMES);
                for (String key : biomesTag.getAllKeys()) visitedBiomes.add(biomesTag.getString(key));
            }

            // Load triggered research tracking
            if (modData.contains(NBT_TRIGGERED_RESEARCH)) {
                CompoundTag triggeredTag = modData.getCompound(NBT_TRIGGERED_RESEARCH);
                for (String key : triggeredTag.getAllKeys()) {
                    triggeredResearchTracking.add(triggeredTag.getString(key));
                }
            }

            // Load pending fate offer and cooldowns
            if (modData.contains("pending_fate_offer_task")) {
                pendingFateOfferTaskId = modData.getString("pending_fate_offer_task");
            }
            if (modData.contains("pending_fate_offer_deity")) {
                String d = modData.getString("pending_fate_offer_deity");
                if (d != null && !d.isEmpty()) pendingFateOfferDeity = new ResourceLocation(d);
            }
            if (modData.contains("fate_offer_cooldowns")) {
                CompoundTag offerCd = modData.getCompound("fate_offer_cooldowns");
                for (String key : offerCd.getAllKeys()) {
                    lastFateOfferByDeity.put(key, offerCd.getLong(key));
                }
            }

            if (modData.contains("nl_trigger_cooldowns")) {
                CompoundTag trigCd = modData.getCompound("nl_trigger_cooldowns");
                for (String key : trigCd.getAllKeys()) {
                    triggerCooldowns.put(key, trigCd.getLong(key));
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
        public final int reputationReward;
        
        public PlayerTask(String taskId, String description, ResourceLocation deityId, long assignedTime, int reputationReward) {
            this.taskId = taskId;
            this.description = description;
            this.deityId = deityId;
            this.assignedTime = assignedTime;
            this.reputationReward = reputationReward;
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
                // Use canonical biome ID (namespace:path), not Holder#toString()
                String currentBiome = player.level().getBiome(player.blockPosition())
                    .unwrapKey()
                    .map(key -> key.location().toString())
                    .orElse("");
                if (!currentBiome.equals(context.currentBiome)) {
                    context.currentBiome = currentBiome;
                    context.addAction("entered " + currentBiome);
                    context.visitedBiomes.add(currentBiome);
                    
                    // SMART: Notify research triggers about biome change (avoid duplicate ticking!)
                    notifyBiomeChangeListeners(player, currentBiome);
                }
            }
        }
    }

    // Telemetry: track kills/blocks mined/item uses/dimension changes
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onEntityKilled(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (event.getSource() != null && event.getSource().getEntity() instanceof ServerPlayer player) {
            EnhancedPlayerContext context = getOrCreateContext(player.getUUID(), player);
            String id = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType()).toString();
            context.killsByEntity.put(id, context.killsByEntity.getOrDefault(id, 0) + 1);
        }
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onBlockBreak(net.minecraftforge.event.level.BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            EnhancedPlayerContext context = getOrCreateContext(player.getUUID(), player);
            String id = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(event.getState().getBlock()).toString();
            context.minedBlocks.put(id, context.minedBlocks.getOrDefault(id, 0) + 1);
        }
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onItemUse(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            EnhancedPlayerContext context = getOrCreateContext(player.getUUID(), player);
            String id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem()).toString();
            context.itemsUsed.put(id, context.itemsUsed.getOrDefault(id, 0) + 1);
        }
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onChangeDimension(net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            EnhancedPlayerContext context = getOrCreateContext(player.getUUID(), player);
            context.addAction("changed_dimension:" + event.getTo().location());
        }
    }
    
    // ===== BIOME CHANGE LISTENER SYSTEM =====
    
    /**
     * Register a listener for biome changes (avoids duplicate ticking)
     */
    public static void addBiomeChangeListener(BiConsumer<ServerPlayer, String> listener) {
        biomeChangeListeners.add(listener);
    }
    
    /**
     * Notify all registered listeners about biome changes
     */
    private static void notifyBiomeChangeListeners(ServerPlayer player, String newBiome) {
        for (BiConsumer<ServerPlayer, String> listener : biomeChangeListeners) {
            try {
                listener.accept(player, newBiome);
            } catch (Exception e) {
                // Don't let listener errors break the AI system
                System.err.println("Error in biome change listener: " + e.getMessage());
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
     * Assign a task to a player
     */
    public static void assignTask(ServerPlayer player, String taskId, String description, ResourceLocation deityId, int reputationReward) {
        EnhancedPlayerContext context = getOrCreateContext(player.getUUID(), player);
        context.assignTask(taskId, description, deityId, reputationReward);
    }
    
    /**
     * Complete a task and award reputation
     */
    /**
     * Track ritual completion for task requirements (manually called by commands)
     */
    public static void onRitualComplete(ServerPlayer player, ResourceLocation ritualId, boolean successful) {
        EnhancedPlayerContext context = getOrCreateContext(player.getUUID(), player);
        context.addRitual(ritualId.toString(), null, successful);
        
        if (successful) {
            // Record the ritual completion for requirement checking
            context.completedRituals.add(ritualId.toString());
            
            // Check if this completes any active tasks
            checkRitualTaskCompletion(player, ritualId, context);
            
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§6[Ritual] §eCompleted: " + ritualId.getPath()));
        }
    }
    
    private static void checkRitualTaskCompletion(ServerPlayer player, ResourceLocation ritualId, EnhancedPlayerContext context) {
        // Check active tasks for ritual requirements
        for (Map.Entry<String, PlayerTask> entry : context.activeTasks.entrySet()) {
            String taskId = entry.getKey();
            
            // Find task template
            for (com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig config : com.bluelotuscoding.eidolonunchained.ai.AIDeityManager.getInstance().getAllConfigs()) {
                for (com.bluelotuscoding.eidolonunchained.ai.TaskSystemConfig.TaskTemplate template : config.task_config.availableTasks) {
                    if (template.taskId.equals(taskId)) {
                        // Check if this ritual completes the task
                        for (String requirement : template.requirements) {
                            if (requirement.startsWith("ritual:") && requirement.contains(ritualId.toString())) {
                                completeTask(player, taskId);
                                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§6[Divine Task] §eCompleted: " + template.description));
                                return; // Exit early since task is completed
                            }
                        }
                    }
                }
            }
        }
    }

    public static boolean completeTask(ServerPlayer player, String taskId) {
        EnhancedPlayerContext context = getOrCreateContext(player.getUUID(), player);
        boolean completed = context.completeTask(taskId);
        
        if (completed) {
            // Find the task to get deity and reputation reward
            for (com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig config : com.bluelotuscoding.eidolonunchained.ai.AIDeityManager.getInstance().getAllConfigs()) {
                for (com.bluelotuscoding.eidolonunchained.ai.TaskSystemConfig.TaskTemplate template : config.task_config.availableTasks) {
                    if (template.taskId.equals(taskId)) {
                        // Award reputation using Eidolon's system
                        try {
                            player.getCapability(elucent.eidolon.capability.IReputation.INSTANCE).ifPresent(rep -> {
                                rep.addReputation(player, config.deity_id, template.reputationReward);
                            });
                        } catch (Exception e) {
                            // Fallback to datapack deity system
                            com.bluelotuscoding.eidolonunchained.deity.DatapackDeity deity = 
                                com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager.getDeity(config.deity_id);
                            if (deity != null) {
                                // Use reputation capability instead since DatapackDeity doesn't have addReputation method
                                player.getCapability(elucent.eidolon.capability.IReputation.INSTANCE).ifPresent(rep -> {
                                    rep.addReputation(player, config.deity_id, template.reputationReward);
                                });
                            }
                        }
                        break;
                    }
                }
            }
        }
        
        return completed;
    }
    
    /**
     * Get enhanced context summary for AI prompts
     */
    public static String getContextSummary(ServerPlayer player) {
        EnhancedPlayerContext context = getOrCreateContext(player.getUUID(), player);
        return context.getContextSummary();
    }

    /**
     * Track research trigger for persistent counting
     */
    public static void trackTriggeredResearch(ServerPlayer player, String researchId) {
        EnhancedPlayerContext context = getOrCreateContext(player.getUUID(), player);
        context.addTriggeredResearch(researchId);
        context.saveToNBT(player); // Save immediately for persistence
    }

    /**
     * Get count of how many times research has been triggered for this player
     */
    public static long getTriggeredResearchCount(ServerPlayer player, String researchId) {
        EnhancedPlayerContext context = getOrCreateContext(player.getUUID(), player);
        return context.getTriggeredResearchCount(researchId);
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
