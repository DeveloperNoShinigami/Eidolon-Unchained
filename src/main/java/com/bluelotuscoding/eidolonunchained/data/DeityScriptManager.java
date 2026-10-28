package com.bluelotuscoding.eidolonunchained.data;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages deterministic deity scripts — weighted event-response system.
 * Scripts are defined in deity JSON under "deity_scripts" and fire on game events
 * without requiring AI. Supports weighted random selection, cooldowns, conditions,
 * and multi-action responses.
 *
 * Events: on_prayer, on_reputation_milestone, on_kill, on_fate_assigned, on_fate_completed
 */
public class DeityScriptManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Random RANDOM = new Random();

    // deityId -> eventType -> list of script entries
    private static final Map<ResourceLocation, Map<String, List<ScriptEntry>>> scripts = new ConcurrentHashMap<>();
    
    // New (Phase 3a): deityId -> eventType -> list of rule objects (priority-sorted)
    private static final Map<ResourceLocation, Map<String, List<RuleObject>>> rules = new ConcurrentHashMap<>();

    // Per-player cooldown tracking: playerId -> (deityId:eventType:entryIndex) -> lastFireTime
    private static final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();
    
    // Per-rule trigger tracking: playerId -> (deityId:eventType:ruleId) -> triggerCount
    private static final Map<UUID, Map<String, Integer>> ruleTriggerCounts = new ConcurrentHashMap<>();
    
    // Global rule trigger tracking: (deityId:eventType:ruleId) -> triggerCount
    private static final Map<String, Integer> globalRuleTriggerCounts = new ConcurrentHashMap<>();

    // ========== Data Model ==========

    /**
     * New rule-based model (Phase 3a schema).
     * Supports priority-based execution, stop_on_match, per-rule cooldowns,
     * max_triggers limits, and AI enhancement toggles.
     */
    public static class RuleObject {
        public String id; // stable unique id per event type
        public boolean enabled = true;
        public int priority = 100; // higher evaluated first
        public boolean stopOnMatch = true; // if true, stop after this rule matches
        public int cooldownTicks = 0;
        public Integer maxTriggersPerPlayer; // null = unlimited
        public Integer maxTriggersGlobal; // null = unlimited
        public List<String> tags = new ArrayList<>();
        
        public ScriptConditions conditions;
        public List<ScriptAction> baseActions = new ArrayList<>();
        public Map<String, Object> aiEnhancement = new HashMap<>(); // {enabled: true/false}
    }

    public static class ScriptEntry {
        public int weight = 1;
        public int cooldownTicks = 0;
        public ScriptConditions conditions;
        public List<ScriptAction> responses = new ArrayList<>();
    }

    public static class ScriptConditions {
        public int minReputation = Integer.MIN_VALUE;
        public int maxReputation = Integer.MAX_VALUE;
        public String timeOfDay; // "day" or "night"
        public String minStage;  // stage_id required
        
        // Event filter fields (phase 3a)
        public String stageId;      // on_reputation_milestone
        public String fateId;       // on_fate_assigned, on_fate_completed
        public String researchId;   // on_research_gained, on_research_completed
        public String factId;       // on_fact_gained
    }

    public static class ScriptAction {
        public String type; // message, command, particle, sound, title, effect, emit_event
        public String value;
        public int delayTicks = 0;
        public int count = 1; // for particles
        public float chance = 1.0f; // probability to execute
        public Map<String, Object> args = new HashMap<>(); // complex action args
    }

    // ========== Loading ==========

    public static void loadScriptsForDeity(ResourceLocation deityId, JsonObject deityScriptsJson) {
        Map<String, List<ScriptEntry>> eventMap = new HashMap<>();

        for (Map.Entry<String, JsonElement> eventEntry : deityScriptsJson.entrySet()) {
            String eventType = eventEntry.getKey(); // e.g. "on_prayer"
            if (!eventEntry.getValue().isJsonArray()) continue;

            List<ScriptEntry> entries = new ArrayList<>();
            for (JsonElement el : eventEntry.getValue().getAsJsonArray()) {
                if (!el.isJsonObject()) continue;
                entries.add(parseEntry(el.getAsJsonObject()));
            }
            eventMap.put(eventType, entries);
        }

        scripts.put(deityId, eventMap);
        LOGGER.info("Loaded deity scripts for {} ({} event types)", deityId, eventMap.size());
    }
    
    /**
     * Load Phase 3a rules from deity_responses JSON.
     * Rules are sorted by priority (descending) for evaluation order.
     */
    public static void loadRulesForDeity(ResourceLocation deityId, JsonObject deityRulesJson) {
        Map<String, List<RuleObject>> eventRules = new HashMap<>();
        
        for (Map.Entry<String, JsonElement> eventEntry : deityRulesJson.entrySet()) {
            String eventType = eventEntry.getKey(); // e.g. "on_prayer", "on_reputation_milestone"
            if (!eventEntry.getValue().isJsonArray()) continue;
            
            List<RuleObject> ruleList = new ArrayList<>();
            for (JsonElement el : eventEntry.getValue().getAsJsonArray()) {
                if (!el.isJsonObject()) continue;
                ruleList.add(parseRuleObject(el.getAsJsonObject()));
            }
            
            // Sort by priority (descending: higher priority first)
            ruleList.sort((a, b) -> Integer.compare(b.priority, a.priority));
            
            eventRules.put(eventType, ruleList);
        }
        
        rules.put(deityId, eventRules);
        LOGGER.info("Loaded deity rules for {} ({} event types)", deityId, eventRules.size());
    }

    private static ScriptEntry parseEntry(JsonObject json) {
        ScriptEntry entry = new ScriptEntry();
        if (json.has("weight")) entry.weight = json.get("weight").getAsInt();
        if (json.has("cooldown_ticks")) entry.cooldownTicks = json.get("cooldown_ticks").getAsInt();

        if (json.has("conditions")) {
            entry.conditions = parseConditions(json.getAsJsonObject("conditions"));
        }

        if (json.has("responses")) {
            for (JsonElement respEl : json.getAsJsonArray("responses")) {
                if (respEl.isJsonObject()) {
                    entry.responses.add(parseAction(respEl.getAsJsonObject()));
                }
            }
        }
        return entry;
    }

    private static ScriptConditions parseConditions(JsonObject json) {
        ScriptConditions cond = new ScriptConditions();
        if (json.has("min_reputation")) cond.minReputation = json.get("min_reputation").getAsInt();
        if (json.has("max_reputation")) cond.maxReputation = json.get("max_reputation").getAsInt();
        if (json.has("time_of_day")) cond.timeOfDay = json.get("time_of_day").getAsString();
        if (json.has("min_stage")) cond.minStage = json.get("min_stage").getAsString();
        
        // Event filter fields (Phase 3a)
        if (json.has("stage_id")) cond.stageId = json.get("stage_id").getAsString();
        if (json.has("fate_id")) cond.fateId = json.get("fate_id").getAsString();
        if (json.has("research_id")) cond.researchId = json.get("research_id").getAsString();
        if (json.has("fact_id")) cond.factId = json.get("fact_id").getAsString();
        
        return cond;
    }

    private static ScriptAction parseAction(JsonObject json) {
        ScriptAction action = new ScriptAction();
        action.type = json.has("type") ? json.get("type").getAsString() : "message";
        action.value = json.has("value") ? json.get("value").getAsString() :
                       json.has("text") ? json.get("text").getAsString() : "";
        if (json.has("delay_ticks")) action.delayTicks = json.get("delay_ticks").getAsInt();
        if (json.has("count")) action.count = json.get("count").getAsInt();
        if (json.has("chance")) action.chance = json.get("chance").getAsFloat();
        if (json.has("particle")) action.value = json.get("particle").getAsString();
        
        // Parse args object for complex actions (title, sound, effect, emit_event, etc.)
        if (json.has("args") && json.get("args").isJsonObject()) {
            JsonObject argsObj = json.getAsJsonObject("args");
            for (String key : argsObj.keySet()) {
                JsonElement elem = argsObj.get(key);
                if (elem.isJsonPrimitive()) {
                    JsonPrimitive prim = elem.getAsJsonPrimitive();
                    if (prim.isBoolean()) {
                        action.args.put(key, prim.getAsBoolean());
                    } else if (prim.isNumber()) {
                        Number num = prim.getAsNumber();
                        if (num.doubleValue() == num.intValue()) {
                            action.args.put(key, num.intValue());
                        } else {
                            action.args.put(key, num.doubleValue());
                        }
                    } else {
                        action.args.put(key, prim.getAsString());
                    }
                } else if (elem.isJsonObject()) {
                    action.args.put(key, elem.getAsJsonObject());
                } else if (elem.isJsonArray()) {
                    action.args.put(key, elem.getAsJsonArray());
                }
            }
        }
        
        return action;
    }
    
    private static RuleObject parseRuleObject(JsonObject json) {
        RuleObject rule = new RuleObject();
        
        if (json.has("id")) rule.id = json.get("id").getAsString();
        else rule.id = "unnamed_" + System.nanoTime(); // fallback
        
        if (json.has("enabled")) rule.enabled = json.get("enabled").getAsBoolean();
        if (json.has("priority")) rule.priority = json.get("priority").getAsInt();
        if (json.has("stop_on_match")) rule.stopOnMatch = json.get("stop_on_match").getAsBoolean();
        if (json.has("cooldown_ticks")) rule.cooldownTicks = json.get("cooldown_ticks").getAsInt();
        
        if (json.has("max_triggers_per_player")) {
            JsonElement elem = json.get("max_triggers_per_player");
            if (!elem.isJsonNull()) {
                rule.maxTriggersPerPlayer = elem.getAsInt();
            }
        }
        if (json.has("max_triggers_global")) {
            JsonElement elem = json.get("max_triggers_global");
            if (!elem.isJsonNull()) {
                rule.maxTriggersGlobal = elem.getAsInt();
            }
        }
        
        if (json.has("tags")) {
            rule.tags = new ArrayList<>();
            for (JsonElement tag : json.getAsJsonArray("tags")) {
                rule.tags.add(tag.getAsString());
            }
        }
        
        if (json.has("conditions")) {
            rule.conditions = parseConditions(json.getAsJsonObject("conditions"));
        }
        
        if (json.has("base_actions")) {
            rule.baseActions = new ArrayList<>();
            for (JsonElement actionEl : json.getAsJsonArray("base_actions")) {
                if (actionEl.isJsonObject()) {
                    rule.baseActions.add(parseAction(actionEl.getAsJsonObject()));
                }
            }
        }
        
        if (json.has("ai_enhancement")) {
            JsonObject aiObj = json.getAsJsonObject("ai_enhancement");
            if (aiObj.has("enabled")) {
                rule.aiEnhancement.put("enabled", aiObj.get("enabled").getAsBoolean());
            }
        }
        
        return rule;
    }

    // ========== Execution ==========

    /**
     * Fire a Phase 3a rule event. Rules are evaluated in priority order (descending).
     * Each rule's conditions are checked; if all pass and limits allow, execute base_actions
     * and respect stop_on_match flag for subsequent rules.
     *
     * @return true if any rule was executed
     */
    public static boolean fireRuleEvent(ServerPlayer player, ResourceLocation deityId, String eventType) {
        return fireRuleEvent(player, deityId, eventType, 0, null);
    }
    
    public static boolean fireRuleEvent(ServerPlayer player, ResourceLocation deityId, String eventType, 
                                         int playerReputation, Map<String, String> eventFilters) {
        Map<String, List<RuleObject>> eventRules = rules.get(deityId);
        if (eventRules == null) return false;
        
        List<RuleObject> ruleList = eventRules.get(eventType);
        if (ruleList == null || ruleList.isEmpty()) return false;
        
        boolean anyRuleFired = false;
        
        // Evaluate rules in priority order (already sorted descending)
        for (RuleObject rule : ruleList) {
            if (!rule.enabled) continue;
            
            // Check cooldown
            if (isOnCooldownRule(player.getUUID(), deityId, eventType, rule.id, rule.cooldownTicks)) {
                continue;
            }
            
            // Check max_triggers limits
            if (!checkTriggerLimits(player.getUUID(), deityId, eventType, rule.id, rule.maxTriggersPerPlayer, rule.maxTriggersGlobal)) {
                continue;
            }
            
            // Check conditions (including event filters)
            if (!checkRuleConditions(player, deityId, rule, playerReputation, eventFilters)) {
                continue;
            }
            
            // Execute rule actions
            executeResponses(player, rule.baseActions, deityId);
            recordRuleTrigger(player.getUUID(), deityId, eventType, rule.id);
            setCooldownRule(player.getUUID(), deityId, eventType, rule.id);
            
            anyRuleFired = true;
            LOGGER.debug("Fired deity rule {}/{}/{} for player {}", deityId, eventType, rule.id, player.getName().getString());
            
            // If stop_on_match is true, don't evaluate further rules
            if (rule.stopOnMatch) {
                break;
            }
        }
        
        return anyRuleFired;
    }

    /**
     * Fire a deity script event (legacy). Selects a weighted random entry that passes conditions
     * and isn't on cooldown, then executes its responses.
     *
     * @return true if a script was executed
     */
    public static boolean fireEvent(ServerPlayer player, ResourceLocation deityId, String eventType) {
        return fireEvent(player, deityId, eventType, 0);
    }

    public static boolean fireEvent(ServerPlayer player, ResourceLocation deityId, String eventType, int playerReputation) {
        Map<String, List<ScriptEntry>> eventMap = scripts.get(deityId);
        if (eventMap == null) return false;

        List<ScriptEntry> entries = eventMap.get(eventType);
        if (entries == null || entries.isEmpty()) return false;

        // Filter eligible entries
        List<ScriptEntry> eligible = new ArrayList<>();
        int totalWeight = 0;
        for (int i = 0; i < entries.size(); i++) {
            ScriptEntry entry = entries.get(i);
            if (!checkConditions(player, deityId, entry, playerReputation)) continue;
            if (isOnCooldown(player.getUUID(), deityId, eventType, i, entry.cooldownTicks)) continue;
            eligible.add(entry);
            totalWeight += entry.weight;
        }

        if (eligible.isEmpty() || totalWeight <= 0) return false;

        // Weighted random selection
        int roll = RANDOM.nextInt(totalWeight);
        ScriptEntry selected = null;
        int cumulative = 0;
        for (ScriptEntry entry : eligible) {
            cumulative += entry.weight;
            if (roll < cumulative) {
                selected = entry;
                break;
            }
        }
        if (selected == null) return false;

        // Set cooldown
        int originalIndex = entries.indexOf(selected);
        setCooldown(player.getUUID(), deityId, eventType, originalIndex);

        // Execute responses
        executeResponses(player, selected.responses, deityId);

        LOGGER.debug("Fired deity script {}/{} for player {}", deityId, eventType, player.getName().getString());
        return true;
    }

    private static boolean checkConditions(ServerPlayer player, ResourceLocation deityId, ScriptEntry entry, int playerReputation) {
        if (entry.conditions == null) return true;

        ScriptConditions cond = entry.conditions;

        if (playerReputation < cond.minReputation) return false;
        if (playerReputation > cond.maxReputation) return false;

        if (cond.timeOfDay != null) {
            boolean isNight = player.level().isNight();
            if ("night".equalsIgnoreCase(cond.timeOfDay) && !isNight) return false;
            if ("day".equalsIgnoreCase(cond.timeOfDay) && isNight) return false;
        }

        // min_stage check deferred — requires stage lookup from deity data
        // (will be integrated when deity_scripts are wired into DatapackDeity)

        return true;
    }
    
    /**
     * Check rule conditions including event filter fields.
     */
    private static boolean checkRuleConditions(ServerPlayer player, ResourceLocation deityId, RuleObject rule, 
                                                int playerReputation, Map<String, String> eventFilters) {
        if (rule.conditions == null) return true;
        
        ScriptConditions cond = rule.conditions;
        
        if (playerReputation < cond.minReputation) return false;
        if (playerReputation > cond.maxReputation) return false;
        
        if (cond.timeOfDay != null) {
            boolean isNight = player.level().isNight();
            if ("night".equalsIgnoreCase(cond.timeOfDay) && !isNight) return false;
            if ("day".equalsIgnoreCase(cond.timeOfDay) && isNight) return false;
        }
        
        if (cond.minStage != null) {
            // TODO: integrate stage lookup from DatapackDeity
        }
        
        // Event filter matching
        if (eventFilters != null) {
            if (cond.stageId != null && !cond.stageId.equals(eventFilters.get("stage_id"))) return false;
            if (cond.fateId != null && !cond.fateId.equals(eventFilters.get("fate_id"))) return false;
            if (cond.researchId != null && !cond.researchId.equals(eventFilters.get("research_id"))) return false;
            if (cond.factId != null && !cond.factId.equals(eventFilters.get("fact_id"))) return false;
        }
        
        return true;
    }
    
    /**
     * Check if rule has hit max_triggers limits.
     */
    private static boolean checkTriggerLimits(UUID playerId, ResourceLocation deityId, String eventType, String ruleId,
                                               Integer maxPerPlayer, Integer maxGlobal) {
        String key = deityId + ":" + eventType + ":" + ruleId;
        
        if (maxPerPlayer != null) {
            Integer count = ruleTriggerCounts.getOrDefault(playerId, new HashMap<>()).get(key);
            if (count != null && count >= maxPerPlayer) return false;
        }
        
        if (maxGlobal != null) {
            Integer count = globalRuleTriggerCounts.get(key);
            if (count != null && count >= maxGlobal) return false;
        }
        
        return true;
    }
    
    /**
     * Record that a rule was triggered (for max_triggers counting).
     */
    private static void recordRuleTrigger(UUID playerId, ResourceLocation deityId, String eventType, String ruleId) {
        String key = deityId + ":" + eventType + ":" + ruleId;
        
        // Per-player count
        ruleTriggerCounts.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
            .merge(key, 1, Integer::sum);
        
        // Global count
        globalRuleTriggerCounts.merge(key, 1, Integer::sum);
    }
    
    /**
     * Check if rule is on cooldown.
     */
    private static boolean isOnCooldownRule(UUID playerId, ResourceLocation deityId, String eventType, String ruleId, int cooldownTicks) {
        if (cooldownTicks <= 0) return false;
        
        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) return false;
        
        String key = deityId + ":" + eventType + ":" + ruleId;
        Long lastFire = playerCooldowns.get(key);
        if (lastFire == null) return false;
        
        long elapsed = System.currentTimeMillis() - lastFire;
        long cooldownMs = cooldownTicks * 50L; // 20 ticks/sec, 50ms/tick
        return elapsed < cooldownMs;
    }
    
    /**
     * Set rule cooldown timestamp.
     */
    private static void setCooldownRule(UUID playerId, ResourceLocation deityId, String eventType, String ruleId) {
        String key = deityId + ":" + eventType + ":" + ruleId;
        cooldowns.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>()).put(key, System.currentTimeMillis());
    }

    private static void executeResponses(ServerPlayer player, List<ScriptAction> actions, ResourceLocation deityId) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        for (ScriptAction action : actions) {
            if (action.delayTicks > 0) {
                // Schedule delayed execution
                final ScriptAction delayedAction = action;
                server.tell(new net.minecraft.server.TickTask(server.getTickCount() + action.delayTicks, () -> {
                    executeSingleAction(player, delayedAction, deityId);
                }));
            } else {
                executeSingleAction(player, action, deityId);
            }
        }
    }

    private static void executeSingleAction(ServerPlayer player, ScriptAction action, ResourceLocation deityId) {
        // Apply chance roll
        if (action.chance < 1.0f && RANDOM.nextFloat() > action.chance) {
            return; // Skip this action
        }
        
        String resolved = action.value.replace("{player}", player.getName().getString());

        switch (action.type.toLowerCase()) {
            case "message" -> {
                String text = resolved;
                // Handle translation keys
                Boolean isTranslationKey = (Boolean) action.args.getOrDefault("translation_key", false);
                java.util.Map<String, String> placeholders = (java.util.Map<String, String>) action.args.get("placeholders");
                
                if (isTranslationKey && placeholders != null) {
                    for (Map.Entry<String, String> ph : placeholders.entrySet()) {
                        text = text.replace("{" + ph.getKey() + "}", ph.getValue());
                    }
                }
                
                player.sendSystemMessage(Component.literal("§d" + text));
            }
            case "command" -> {
                MinecraftServer server = player.getServer();
                if (server != null) {
                    CommandSourceStack source = server.createCommandSourceStack()
                        .withSuppressedOutput()
                        .withPermission(2);
                    server.getCommands().performPrefixedCommand(source, resolved);
                }
            }
            case "title" -> {
                String subtitle = (String) action.args.getOrDefault("subtitle", "");
                int fadeIn = ((Number) action.args.getOrDefault("fade_in", 10)).intValue();
                int duration = ((Number) action.args.getOrDefault("duration", 40)).intValue();
                int fadeOut = ((Number) action.args.getOrDefault("fade_out", 10)).intValue();
                
                player.connection.send(new ClientboundSetTitlesAnimationPacket(fadeIn, duration, fadeOut));
                player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(resolved)));
                if (!subtitle.isEmpty()) {
                    player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(subtitle)));
                }
            }
            case "sound" -> {
                try {
                    ResourceLocation soundId = new ResourceLocation(resolved);
                    SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(soundId);
                    if (sound != null) {
                        float volume = ((Number) action.args.getOrDefault("volume", 1.0)).floatValue();
                        float pitch = ((Number) action.args.getOrDefault("pitch", 1.0)).floatValue();
                        String sourceStr = (String) action.args.getOrDefault("source", "AMBIENT");
                        SoundSource source = SoundSource.AMBIENT;
                        try {
                            source = SoundSource.valueOf(sourceStr);
                        } catch (IllegalArgumentException ignored) {}
                        
                        player.playNotifySound(sound, source, volume, pitch);
                    }
                } catch (Exception e) {
                    LOGGER.warn("Invalid sound in deity action: {}", resolved);
                }
            }
            case "particle" -> {
                if (player.level() instanceof ServerLevel serverLevel) {
                    float spread = ((Number) action.args.getOrDefault("spread", 0.5f)).floatValue();
                    float speed = ((Number) action.args.getOrDefault("speed", 0.1f)).floatValue();
                    
                    serverLevel.sendParticles(ParticleTypes.ENCHANT,
                        player.getX(), player.getY() + 1, player.getZ(),
                        action.count, spread, spread, spread, speed);
                }
            }
            case "effect" -> {
                MinecraftServer server = player.getServer();
                if (server != null) {
                    int amplifier = ((Number) action.args.getOrDefault("amplifier", 0)).intValue();
                    int durationTicks = ((Number) action.args.getOrDefault("duration_ticks", 200)).intValue();
                    
                    String cmd = "effect give @s " + resolved + " " + durationTicks + " " + amplifier;
                    CommandSourceStack source = server.createCommandSourceStack()
                        .withSuppressedOutput()
                        .withPermission(2);
                    server.getCommands().performPrefixedCommand(source, cmd);
                }
            }
            case "emit_event" -> {
                // Emit event with optional args
                String eventId = (String) action.args.get("event");
                boolean requireAllowlist = (Boolean) action.args.getOrDefault("require_allowlist", true);
                
                // TODO: integrate with event system once defined
                LOGGER.debug("Deity event emission: {} (allowlist={}) for player {}", 
                    eventId, requireAllowlist, player.getName().getString());
            }
            default -> LOGGER.warn("Unknown deity action type: {}", action.type);
        }
    }

    // ========== Cooldown Tracking ==========

    private static boolean isOnCooldown(UUID playerId, ResourceLocation deityId, String eventType, int index, int cooldownTicks) {
        if (cooldownTicks <= 0) return false;

        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) return false;

        String key = deityId + ":" + eventType + ":" + index;
        Long lastFire = playerCooldowns.get(key);
        if (lastFire == null) return false;

        long elapsed = System.currentTimeMillis() - lastFire;
        long cooldownMs = cooldownTicks * 50L; // 20 ticks/sec, 50ms/tick
        return elapsed < cooldownMs;
    }

    private static void setCooldown(UUID playerId, ResourceLocation deityId, String eventType, int index) {
        String key = deityId + ":" + eventType + ":" + index;
        cooldowns.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>()).put(key, System.currentTimeMillis());
    }

    // ========== Utility ==========

    public static boolean hasScripts(ResourceLocation deityId) {
        return scripts.containsKey(deityId) && !scripts.get(deityId).isEmpty();
    }

    public static boolean hasEvent(ResourceLocation deityId, String eventType) {
        Map<String, List<ScriptEntry>> eventMap = scripts.get(deityId);
        return eventMap != null && eventMap.containsKey(eventType) && !eventMap.get(eventType).isEmpty();
    }
    
    public static boolean hasRules(ResourceLocation deityId) {
        return rules.containsKey(deityId) && !rules.get(deityId).isEmpty();
    }
    
    public static boolean hasRuleEvent(ResourceLocation deityId, String eventType) {
        Map<String, List<RuleObject>> eventMap = rules.get(deityId);
        return eventMap != null && eventMap.containsKey(eventType) && !eventMap.get(eventType).isEmpty();
    }

    public static void clearAll() {
        scripts.clear();
        rules.clear();
        cooldowns.clear();
        ruleTriggerCounts.clear();
        globalRuleTriggerCounts.clear();
    }

    public static void clearPlayerCooldowns(UUID playerId) {
        cooldowns.remove(playerId);
        ruleTriggerCounts.remove(playerId);
    }
    
    /**
     * Clear all global rule trigger counts (typically on server restart/reload).
     */
    public static void clearGlobalRuleTriggerCounts() {
        globalRuleTriggerCounts.clear();
    }
}
