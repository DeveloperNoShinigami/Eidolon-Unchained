package com.bluelotuscoding.eidolonunchained.chant;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.bluelotuscoding.eidolonunchained.ai.MobChantCastingGoal;
import com.bluelotuscoding.eidolonunchained.registries.EidolonUnchainedAttributes;
import com.bluelotuscoding.eidolonunchained.network.AttributeSignStatusIconPacket;
import com.bluelotuscoding.eidolonunchained.network.EidolonUnchainedNetworking;
import elucent.eidolon.capability.ISoul;
import elucent.eidolon.network.Networking;
import elucent.eidolon.network.SoulUpdatePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

import java.util.List;
import java.util.ArrayList;

/**
 * Represents a custom chant that can be defined in datapacks
 * Chants are defined in data/modid/chants/ folder
 */
public class DatapackChant {
    private static final Logger LOGGER = LogUtils.getLogger();

    public enum CombatRole {
        OFFENSE,
        DEFENSE,
        SUPPORT,
        CC,
        MOVEMENT,
        MELEE;

        public static CombatRole fromString(String raw) {
            if (raw == null || raw.isBlank()) {
                return OFFENSE;
            }
            return switch (raw.toLowerCase(Locale.ROOT)) {
                case "offense" -> OFFENSE;
                case "defense" -> DEFENSE;
                case "support" -> SUPPORT;
                case "cc" -> CC;
                case "movement" -> MOVEMENT;
                case "melee" -> MELEE;
                default -> OFFENSE;
            };
        }

        public String serializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private final ResourceLocation id;
    private final String name;
    private final String description;
    private final List<ResourceLocation> signSequence;
    private final String category;
    private final ResourceLocation codexIcon; // Icon to use in codex
    private final int difficulty;
    private final String difficultyLabel; // optional: textual difficulty (easy/intermediate/hard) or explicit progression id
    private final int manaCost; // Soul/magic cost to cast this chant
    private final int cooldown; // Cooldown in seconds for this specific chant
    private final CombatRole combatRole; // Role used by mob planner when selecting chants
    private final int mobPriorityWeight; // Relative weight in-role for mob casting planner
    private final List<ChantEffect> effects;
    private final List<String> requirements;
    private final boolean showInCodex;
    private final ResourceLocation linkedDeity; // Optional deity connection
    private final String prayerEffectType; // Prayer type for AI deity interactions
    private final boolean requiresEffigy; // Whether this chant requires an effigy nearby (like Eidolon prayers)
    private final boolean requiresTarget; // Whether mob AI requires a combat target to cast this chant
    
    public DatapackChant(ResourceLocation id, String name, String description,
                        List<ResourceLocation> signSequence, String category, ResourceLocation codexIcon,
                        int difficulty, String difficultyLabel, int manaCost, int cooldown,
                        CombatRole combatRole, int mobPriorityWeight, List<ChantEffect> effects,
                        List<String> requirements, boolean showInCodex,
                        ResourceLocation linkedDeity, String prayerEffectType, boolean requiresEffigy,
                        boolean requiresTarget) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.signSequence = new ArrayList<>(signSequence);
        this.category = category;
        this.codexIcon = codexIcon;
        this.difficulty = difficulty;
        this.difficultyLabel = difficultyLabel;
        this.manaCost = Math.max(0, manaCost); // Ensure non-negative
        this.cooldown = Math.max(0, cooldown); // Ensure non-negative
        this.combatRole = combatRole == null ? CombatRole.OFFENSE : combatRole;
        this.mobPriorityWeight = Math.max(0, mobPriorityWeight);
        this.effects = new ArrayList<>(effects);
        this.requirements = new ArrayList<>(requirements);
        this.showInCodex = showInCodex;
        this.linkedDeity = linkedDeity;
        this.prayerEffectType = prayerEffectType;
        this.requiresEffigy = requiresEffigy;
        this.requiresTarget = requiresTarget;
    }
    
    public ResourceLocation getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<ResourceLocation> getSignSequence() { return new ArrayList<>(signSequence); }
    public String getCategory() { return category; }
    public ResourceLocation getCodexIcon() { return codexIcon; }
    public int getDifficulty() { return difficulty; }
    public String getDifficultyLabel() { return difficultyLabel; }
    public int getManaCost() { return manaCost; }
    public int getCooldown() { return cooldown; }
    public CombatRole getCombatRole() { return combatRole; }
    public int getMobPriorityWeight() { return mobPriorityWeight; }
    public List<ChantEffect> getEffects() { return new ArrayList<>(effects); }
    public List<String> getRequirements() { return new ArrayList<>(requirements); }
    public boolean shouldShowInCodex() { return showInCodex; }
    public ResourceLocation getLinkedDeity() { return linkedDeity; }
    public boolean hasLinkedDeity() { return linkedDeity != null; }
    public String getPrayerEffectType() { return prayerEffectType; }
    public boolean requiresEffigy() { return requiresEffigy; }
    public boolean requiresTarget() { return requiresTarget; }
    
    /**
     * Check if player meets requirements to perform this chant
     */
    public boolean canPerform(net.minecraft.server.level.ServerPlayer player) {
        for (String requirement : requirements) {
            if (!checkRequirement(player, requirement)) {
                return false;
            }
        }
        // Check progression requirement implied by difficulty label (if linked to a deity)
        try {
            if (!checkProgressionRequirement(player)) {
                return false;
            }
        } catch (Exception e) {
            // Don't block chant execution for unexpected errors in progression checks
            System.err.println("Error checking progression requirement: " + e.getMessage());
        }
        return true;
    }

    /**
     * Check progression requirement derived from `difficultyLabel` when the chant is linked to a deity.
     * Supports labels: easy, intermediate, hard, master or explicit progression stage ids.
     */
    private boolean checkProgressionRequirement(net.minecraft.server.level.ServerPlayer player) {
        if (difficultyLabel == null || difficultyLabel.isEmpty()) return true;
        if (linkedDeity == null) return true; // No deity to check against

        com.bluelotuscoding.eidolonunchained.deity.DatapackDeity deity =
            com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager.getDeity(linkedDeity);
        if (deity == null) return true; // Can't resolve deity, be permissive

        // Build ordered list of stage ids from deity progression
        java.util.List<String> stages = new java.util.ArrayList<>();
        for (elucent.eidolon.api.deity.Deity.Stage s : deity.getProgression().getSteps().values()) {
            stages.add(s.id().getPath());
        }

        if (stages.isEmpty()) return true; // Nothing to compare against

        String label = difficultyLabel.toLowerCase();
        int requiredIndex = -1;

        switch (label) {
            case "easy":
            case "novice":
                requiredIndex = 0;
                break;
            case "intermediate":
            case "adept":
                requiredIndex = stages.size() / 2;
                break;
            case "hard":
            case "expert":
            case "master":
                requiredIndex = Math.max(0, stages.size() - 1);
                break;
            default:
                // Treat label as explicit stage id if present
                int idx = stages.indexOf(label);
                if (idx >= 0) requiredIndex = idx;
                break;
        }

        if (requiredIndex < 0) {
            // Unknown label - allow for backward compatibility
            return true;
        }

        String playerStage = deity.getProgressionLevel(player); // returns stage id or 'unknown'
        int playerIndex = stages.indexOf(playerStage);
        if (playerIndex >= requiredIndex) {
            return true; // Player meets or exceeds required stage
        }

        // Send friendly failure message describing required and current tiers
        String requiredTitle = deity.getStageDisplayName(stages.get(requiredIndex));
        String currentTitle = playerStage != null && !"unknown".equals(playerStage) ? deity.getStageDisplayName(playerStage) : "Novice";
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cYou are not experienced enough with " + deity.getName() + " to perform that chant. Required: " + requiredTitle + ", your rank: " + currentTitle));

        return false;
    }
    
    /**
     * Check a specific requirement for the player
     */
    private boolean checkRequirement(net.minecraft.server.level.ServerPlayer player, String requirement) {
        if (requirement.startsWith("reputation:")) {
            // Format: "reputation:deity_id:min_amount"
            String[] parts = requirement.split(":");
            if (parts.length >= 3) {
                try {
                    net.minecraft.resources.ResourceLocation deityId = new net.minecraft.resources.ResourceLocation(parts[1]);
                    double minReputation = Double.parseDouble(parts[2]);
                    
                    // Get player's reputation with this deity using Eidolon's reputation system
                    elucent.eidolon.capability.IReputation reputationCap = player.getCapability(elucent.eidolon.capability.IReputation.INSTANCE).orElse(null);
                    if (reputationCap != null) {
                        double currentRep = reputationCap.getReputation(player.getUUID(), deityId);
                        return currentRep >= minReputation;
                    }
                } catch (Exception e) {
                    System.err.println("Invalid reputation requirement: " + requirement);
                }
            }
            return false;
        } else if (requirement.startsWith("item:")) {
            // Format: "item:minecraft:diamond:count" or "item:minecraft:diamond:count:nbt"
            String[] parts = requirement.split(":", 4);
            if (parts.length >= 3) {
                try {
                    net.minecraft.resources.ResourceLocation itemId = new net.minecraft.resources.ResourceLocation(parts[1]);
                    int requiredCount = Integer.parseInt(parts[2]);
                    String nbtData = parts.length >= 4 ? parts[3] : null;
                    
                    return hasRequiredItem(player, itemId, requiredCount, nbtData);
                } catch (Exception e) {
                    System.err.println("Invalid item requirement: " + requirement);
                }
            }
            return false;
        } else if (requirement.startsWith("has_item:")) {
            // Format: "has_item:minecraft:diamond" - just check if player has the item
            String[] parts = requirement.split(":");
            if (parts.length >= 2) {
                try {
                    net.minecraft.resources.ResourceLocation itemId = new net.minecraft.resources.ResourceLocation(parts[1]);
                    return hasRequiredItem(player, itemId, 1, null);
                } catch (Exception e) {
                    System.err.println("Invalid has_item requirement: " + requirement);
                }
            }
            return false;
        }
        else if (requirement.startsWith("fact:")) {
            // Format: "fact:namespace:id"
            String[] parts = requirement.split(":");
            if (parts.length >= 2) {
                try {
                    net.minecraft.resources.ResourceLocation factId = new net.minecraft.resources.ResourceLocation(parts[1]);
                    return elucent.eidolon.util.KnowledgeUtil.knowsFact(player, factId);
                } catch (Exception e) {
                    System.err.println("Invalid fact requirement: " + requirement);
                }
            }
            return false;
        } else if (requirement.startsWith("research:")) {
            // Format: "research:namespace:id"
            String[] parts = requirement.split(":");
            if (parts.length >= 2) {
                try {
                    net.minecraft.resources.ResourceLocation researchId = new net.minecraft.resources.ResourceLocation(parts[1]);
                    return elucent.eidolon.util.KnowledgeUtil.knowsResearch(player, researchId);
                } catch (Exception e) {
                    System.err.println("Invalid research requirement: " + requirement);
                }
            }
            return false;
        }
        
        // Unknown requirement type - assume it passes (for backward compatibility)
        System.err.println("Unknown requirement type: " + requirement);
        return true;
    }
    
    /**
     * Check if player has required item with optional NBT matching
     */
    private boolean hasRequiredItem(net.minecraft.server.level.ServerPlayer player, net.minecraft.resources.ResourceLocation itemId, int requiredCount, String nbtData) {
        net.minecraft.world.item.Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(itemId);
        if (item == null) {
            return false;
        }
        
        int foundCount = 0;
        
        // Check player inventory
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            net.minecraft.world.item.ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == item) {
                // If NBT is required, check NBT match
                if (nbtData != null && !nbtData.isEmpty()) {
                    try {
                        net.minecraft.nbt.CompoundTag requiredNbt = net.minecraft.nbt.TagParser.parseTag(nbtData);
                        net.minecraft.nbt.CompoundTag stackNbt = stack.getTag();
                        
                        if (stackNbt == null || !nbtMatches(stackNbt, requiredNbt)) {
                            continue; // Skip this stack if NBT doesn't match
                        }
                    } catch (Exception e) {
                        System.err.println("Invalid NBT data in requirement: " + nbtData);
                        continue;
                    }
                }
                
                foundCount += stack.getCount();
                if (foundCount >= requiredCount) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Check if stack NBT contains all required NBT data
     */
    private boolean nbtMatches(net.minecraft.nbt.CompoundTag stackNbt, net.minecraft.nbt.CompoundTag requiredNbt) {
        for (String key : requiredNbt.getAllKeys()) {
            if (!stackNbt.contains(key)) {
                return false;
            }
            
            net.minecraft.nbt.Tag stackValue = stackNbt.get(key);
            net.minecraft.nbt.Tag requiredValue = requiredNbt.get(key);
            
            if (stackValue == null || !stackValue.equals(requiredValue)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Execute the chant effects
     * ðŸ"® UPDATED: Now sets parent chant context for effigy effects
     */
    public void execute(net.minecraft.server.level.ServerPlayer player) {
        for (ChantEffect effect : effects) {
            // Set parent chant context for effects that need it (like effigy_effects)
            effect.setParentChant(this);
            effect.apply(player);
        }
    }

    /**
     * Execute chant effects for non-player casters (e.g. enthralled mobs).
     */
    public void execute(net.minecraft.world.entity.LivingEntity caster) {
        for (ChantEffect effect : effects) {
            effect.setParentChant(this);
            effect.apply(caster);
        }
    }
    
    /**
     * Create a chant from JSON data
     */
    public static DatapackChant fromJson(ResourceLocation id, JsonObject json) {
        String name = json.get("name").getAsString();
        String description = json.has("description") ? json.get("description").getAsString() : "";
        String category;
        if (json.has("category")) {
            category = json.get("category").getAsString();
        } else {
            // Infer category from path folder if present: chants/<folder>/<file>.json
            String path = id.getPath();
            int slash = path.indexOf('/');
            category = (slash > 0) ? path.substring(0, slash) : "custom";
        }
        
        // Parse codex icon
        ResourceLocation codexIcon = null;
        if (json.has("codex_icon")) {
            codexIcon = new ResourceLocation(json.get("codex_icon").getAsString());
        }
        
        int difficulty = 1;
        String difficultyLabel = null;
        if (json.has("difficulty")) {
            try {
                var prim = json.get("difficulty");
                if (prim.isJsonPrimitive() && prim.getAsJsonPrimitive().isNumber()) {
                    difficulty = prim.getAsInt();
                } else {
                    difficultyLabel = prim.getAsString();
                    // keep numeric difficulty default as 1 for compatibility
                }
            } catch (Exception e) {
                // fallback
                difficulty = 1;
            }
        }
        int manaCost = json.has("mana_cost") ? json.get("mana_cost").getAsInt() : 
                      com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig.COMMON.defaultManaCost.get();
        int cooldown = json.has("cooldown") ? json.get("cooldown").getAsInt() :
                      com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig.COMMON.chantCooldownSeconds.get();
        DatapackChant.CombatRole combatRole = DatapackChant.CombatRole.fromString(
            json.has("combat_role") ? json.get("combat_role").getAsString() : "offense"
        );
        int mobPriorityWeight = json.has("mob_priority_weight")
            ? Math.max(0, json.get("mob_priority_weight").getAsInt())
            : 100;
        boolean showInCodex = json.has("show_in_codex") ? json.get("show_in_codex").getAsBoolean() : true;
        
        // Parse optional linked deity
        ResourceLocation linkedDeity = null;
        if (json.has("linked_deity")) {
            linkedDeity = new ResourceLocation(json.get("linked_deity").getAsString());
        }
        
        // Parse optional prayer effect type (for AI deity integration)
        String prayerEffectType = null;
        if (json.has("prayer_effect_type")) {
            prayerEffectType = json.get("prayer_effect_type").getAsString();
        }
        
        // Parse effigy requirement (default true for deity-linked chants, false otherwise)
        boolean requiresEffigy = json.has("requires_effigy") ? 
            json.get("requires_effigy").getAsBoolean() : 
            (linkedDeity != null); // Default: require effigy if linked to deity

        // Parse target requirement (default true; set false for chants like summoning that work without a combat target)
        boolean requiresTarget = !json.has("requires_target") || json.get("requires_target").getAsBoolean();

        // Parse sign sequence
        List<ResourceLocation> signSequence = new ArrayList<>();
        if (json.has("signs")) {
            JsonArray signs = json.getAsJsonArray("signs");
            for (JsonElement signElement : signs) {
                signSequence.add(new ResourceLocation(signElement.getAsString()));
            }
        }

        // Parse effects
        List<ChantEffect> effects = new ArrayList<>();
        if (json.has("effects")) {
            JsonArray effectArray = json.getAsJsonArray("effects");
            for (JsonElement effectElement : effectArray) {
                effects.add(ChantEffect.fromJson(effectElement.getAsJsonObject()));
            }
        }
        
        // Parse requirements
        List<String> requirements = new ArrayList<>();
        if (json.has("requirements")) {
            JsonArray reqArray = json.getAsJsonArray("requirements");
            for (JsonElement reqElement : reqArray) {
                requirements.add(reqElement.getAsString());
            }
        }
        
        return new DatapackChant(id, name, description, signSequence, category, codexIcon,
                       difficulty, difficultyLabel, manaCost, cooldown, combatRole, mobPriorityWeight,
                       effects, requirements, showInCodex, linkedDeity, prayerEffectType, requiresEffigy, requiresTarget);
    }
    
    /**
     * Convert to JSON for data generation
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("name", name);
        json.addProperty("description", description);
        json.addProperty("category", category);
        json.addProperty("difficulty", difficulty);
        json.addProperty("combat_role", combatRole.serializedName());
        json.addProperty("mob_priority_weight", mobPriorityWeight);
        json.addProperty("show_in_codex", showInCodex);
        if (!requiresTarget) {
            json.addProperty("requires_target", false);
        }
        
        // Add optional linked deity
        if (linkedDeity != null) {
            json.addProperty("linked_deity", linkedDeity.toString());
        }
        
        // Add optional prayer effect type
        if (prayerEffectType != null && !prayerEffectType.isEmpty()) {
            json.addProperty("prayer_effect_type", prayerEffectType);
        }
        
        // Add sign sequence
        JsonArray signs = new JsonArray();
        for (ResourceLocation sign : signSequence) {
            signs.add(sign.toString());
        }
        json.add("signs", signs);
        
        // Add effects
        JsonArray effectArray = new JsonArray();
        for (ChantEffect effect : effects) {
            effectArray.add(effect.toJson());
        }
        json.add("effects", effectArray);
        
        // Add requirements
        JsonArray reqArray = new JsonArray();
        for (String requirement : requirements) {
            reqArray.add(requirement);
        }
        json.add("requirements", reqArray);
        
        return json;
    }
    
    /**
     * Represents an effect that occurs when a chant is completed
     */
    public static class ChantEffect {
        private final String type;
        private final JsonObject data;
        private DatapackChant parentChant; // ðŸ"® NEW: Track parent chant for effigy effects
        private static final String TAG_DIVINE_RESISTANCES = "eu_divine_resistances";
        
        public ChantEffect(String type, JsonObject data) {
            this.type = type;
            this.data = data;
        }
        
        // ðŸ"® NEW: Set parent chant context for effigy effects
        public void setParentChant(DatapackChant chant) {
            this.parentChant = chant;
        }
        
        public String getType() { return type; }
        public JsonObject getData() { return data; }
        
        public void apply(net.minecraft.server.level.ServerPlayer player) {
            switch (type) {
                case "give_item":
                    applyGiveItem(player);
                    break;
                case "apply_effect":
                    applyEffect(player);
                    break;
                case "play_sound":
                    playSoundAtPlayer(player);
                    break;
                case "effigy_sound":
                    playSoundAtEffigy(player);
                    break;
                case "run_command":
                    runCommand(player);
                    break;
                case "send_message":
                    sendMessage(player);
                    break;
                case "start_conversation":
                case "communication":
                    // Both effect types do the same thing - start deity conversation
                    startConversation(player);
                    break;
                case "restore_mana":
                    restoreMana(player);
                    break;
                case "increase_max_mana":
                    increaseMaxMana(player);
                    break;
                case "modify_attribute":
                case "modify_magic_power":
                    // modify_magic_power is kept as a compatibility alias.
                    modifyAttribute(player);
                    break;
                case "apply_cooldown":
                    applyChantCooldown(player);
                    break;
                case "consume_alt_resource":
                    consumeAltResource(player);
                    break;
                case "spawn_projectile":
                    spawnProjectile(player);
                    break;
                case "projectile_effect":
                    spawnProjectileEffect(player);
                    break;
                case "raycast_effect":
                    applyRaycastEffect(player);
                    break;
                case "area_effect":
                    applyAreaEffect(player);
                    break;
                case "magic_weapon":
                    applyMagicWeapon(player);
                    break;
                case "summoning":
                case "summon_entity":
                    summonEnthralledEntity(player);
                    break;
                default:
                    LOGGER.debug("Unknown chant effect type '{}' — skipping", type);
                    break;
            }
        }

        public void apply(net.minecraft.world.entity.LivingEntity caster) {
            if (caster instanceof net.minecraft.server.level.ServerPlayer player) {
                apply(player);
                return;
            }

            switch (type) {
                case "apply_effect":
                    applyEffect(caster);
                    break;
                case "play_sound":
                    playSoundAtEntity(caster);
                    break;
                case "run_command":
                    runCommand(caster);
                    break;
                case "modify_attribute":
                case "modify_magic_power":
                    // Non-player casting currently supports self-application only.
                    modifyAttribute(caster);
                    break;
                case "spawn_projectile":
                    spawnProjectile(caster);
                    break;
                case "projectile_effect":
                    spawnProjectileEffect(caster);
                    break;
                case "raycast_effect":
                    applyRaycastEffect(caster);
                    break;
                case "area_effect":
                    applyAreaEffect(caster);
                    break;
                case "magic_weapon":
                    applyMagicWeapon(caster);
                    break;
                case "summoning":
                case "summon_entity":
                    summonEnthralledEntity(caster);
                    break;
                default:
                    LOGGER.debug("Effect '{}' requires a player caster; skipping for {}", type, caster.getType());
                    break;
            }
        }

        private void playSoundAtPlayer(net.minecraft.server.level.ServerPlayer player) {
            if (!data.has("sound")) return;
            String soundId = data.get("sound").getAsString();
            float volume = data.has("volume") ? (float)data.get("volume").getAsDouble() : 1.0f;
            float pitch = data.has("pitch") ? (float)data.get("pitch").getAsDouble() : 1.0f;
            String cat = data.has("category") ? data.get("category").getAsString() : "VOICE";
            net.minecraft.sounds.SoundSource source = safeSoundSource(cat);
            net.minecraft.sounds.SoundEvent evt = resolveSound(soundId);
            if (evt != null) {
                player.level().playSound(null, player.blockPosition(), evt, source, volume, pitch);
            } else {
                player.sendSystemMessage(Component.literal("\u00A7cUnknown sound: " + soundId));
            }
        }

        private void playSoundAtEntity(net.minecraft.world.entity.LivingEntity caster) {
            if (!data.has("sound")) return;
            String soundId = data.get("sound").getAsString();
            float volume = data.has("volume") ? (float)data.get("volume").getAsDouble() : 1.0f;
            float pitch = data.has("pitch") ? (float)data.get("pitch").getAsDouble() : 1.0f;
            String cat = data.has("category") ? data.get("category").getAsString() : "VOICE";
            net.minecraft.sounds.SoundSource source = safeSoundSource(cat);
            net.minecraft.sounds.SoundEvent evt = resolveSound(soundId);
            if (evt != null) {
                caster.level().playSound(null, caster.blockPosition(), evt, source, volume, pitch);
            }
        }

        private void playSoundAtEffigy(net.minecraft.server.level.ServerPlayer player) {
            if (!data.has("sound")) return;
            String soundId = data.get("sound").getAsString();
            float volume = data.has("volume") ? (float)data.get("volume").getAsDouble() : 1.0f;
            float pitch = data.has("pitch") ? (float)data.get("pitch").getAsDouble() : 1.0f;
            String cat = data.has("category") ? data.get("category").getAsString() : "BLOCKS";
            net.minecraft.sounds.SoundSource source = safeSoundSource(cat);
            net.minecraft.sounds.SoundEvent evt = resolveSound(soundId);
            if (evt == null) {
                player.sendSystemMessage(Component.literal("\u00A7cUnknown sound: " + soundId));
                return;
            }
            elucent.eidolon.common.tile.EffigyTileEntity effigy =
                com.bluelotuscoding.eidolonunchained.effects.EffigyEffectsManager.findNearbyEffigy(player, 10.0);
            if (effigy != null) {
                player.serverLevel().playSound(null, effigy.getBlockPos(), evt, source, volume, pitch);
            } else {
                // Fallback: play at player if no effigy found
                player.level().playSound(null, player.blockPosition(), evt, source, volume, pitch);
            }
        }

        private net.minecraft.sounds.SoundEvent resolveSound(String id) {
            try {
                net.minecraft.resources.ResourceLocation rl = net.minecraft.resources.ResourceLocation.tryParse(id);
                if (rl != null) {
                    return net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS.getValue(rl);
                }
            } catch (Exception ignored) {}
            return null;
        }

        private net.minecraft.sounds.SoundSource safeSoundSource(String name) {
            try {
                return net.minecraft.sounds.SoundSource.valueOf(name.toUpperCase());
            } catch (Exception e) {
                if ("VOICE".equalsIgnoreCase(name)) return net.minecraft.sounds.SoundSource.VOICE;
                return net.minecraft.sounds.SoundSource.BLOCKS;
            }
        }
        
        private void applyGiveItem(net.minecraft.server.level.ServerPlayer player) {
            if (!data.has("item")) return;
            String itemId = data.get("item").getAsString();
            int count = data.has("count") ? data.get("count").getAsInt() : 1;
            try {
                net.minecraft.resources.ResourceLocation itemLocation = new net.minecraft.resources.ResourceLocation(itemId);
                net.minecraft.world.item.Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(itemLocation);
                if (item != null && item != net.minecraft.world.item.Items.AIR) {
                    net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(item, Math.max(1, count));
                    boolean inserted = player.getInventory().add(stack);
                    if (!inserted) {
                        player.drop(stack, false);
                    }
                } else {
                    LOGGER.warn("give_item: unknown item '{}'", itemId);
                    player.sendSystemMessage(Component.literal("§cUnknown item: " + itemId));
                }
            } catch (Exception e) {
                LOGGER.error("give_item error: {}", e.getMessage());
            }
        }
        
        private void applyEffect(net.minecraft.server.level.ServerPlayer player) {
            if (!data.has("effect")) {
                return;
            }
            
            String effectId = data.get("effect").getAsString();
            int duration = data.has("duration") ? data.get("duration").getAsInt() : 600;
            int amplifier = data.has("amplifier") ? data.get("amplifier").getAsInt() : 0;
            
            try {
                net.minecraft.resources.ResourceLocation effectLocation = new net.minecraft.resources.ResourceLocation(effectId);
                net.minecraft.world.effect.MobEffect effect = net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getValue(effectLocation);
                
                if (effect != null) {
                    net.minecraft.world.effect.MobEffectInstance effectInstance = 
                        new net.minecraft.world.effect.MobEffectInstance(effect, duration, amplifier);
                    player.addEffect(effectInstance);
                } else {
                    player.sendSystemMessage(Component.literal("§cUnknown effect: " + effectId));
                }
            } catch (Exception e) {
                player.sendSystemMessage(Component.literal("§cError applying effect: " + e.getMessage()));
            }
        }

        private void applyEffect(net.minecraft.world.entity.LivingEntity caster) {
            if (!data.has("effect")) {
                return;
            }

            String effectId = data.get("effect").getAsString();
            int duration = data.has("duration") ? data.get("duration").getAsInt() : 600;
            int amplifier = data.has("amplifier") ? data.get("amplifier").getAsInt() : 0;

            try {
                net.minecraft.resources.ResourceLocation effectLocation = new net.minecraft.resources.ResourceLocation(effectId);
                net.minecraft.world.effect.MobEffect effect = net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getValue(effectLocation);

                if (effect != null) {
                    net.minecraft.world.effect.MobEffectInstance effectInstance =
                        new net.minecraft.world.effect.MobEffectInstance(effect, duration, amplifier);
                    caster.addEffect(effectInstance);
                } else {
                    LOGGER.warn("apply_effect: unknown effect '{}'", effectId);
                }
            } catch (Exception e) {
                LOGGER.error("apply_effect error: {}", e.getMessage());
            }
        }
        
        private void runCommand(net.minecraft.server.level.ServerPlayer player) {
            if (!data.has("command")) {
                return;
            }
            
            String command = data.get("command").getAsString();
            var server = player.getServer();
            
            if (server != null && server.isCommandBlockEnabled() && !command.isEmpty()) {
                try {
                    // Create command source with the player as the executor
                    net.minecraft.commands.CommandSourceStack commandSource = player.createCommandSourceStack()
                        .withPermission(2)
                        .withSuppressedOutput();
                    
                    // Replace @s with the player's name for targeting
                    String processedCommand = command.replace("@s", player.getName().getString());
                    
                    // Execute the command
                    server.getCommands().performPrefixedCommand(commandSource, processedCommand);
                    
                } catch (Exception e) {
                    player.sendSystemMessage(Component.literal("§cError executing command: " + e.getMessage()));
                }
            }
        }

        private void runCommand(net.minecraft.world.entity.LivingEntity caster) {
            if (!data.has("command")) {
                return;
            }

            String command = data.get("command").getAsString();
            if (!(caster.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
                return;
            }

            var server = serverLevel.getServer();
            if (server != null && server.isCommandBlockEnabled() && !command.isEmpty()) {
                try {
                    net.minecraft.commands.CommandSourceStack commandSource = caster.createCommandSourceStack()
                        .withPermission(2)
                        .withSuppressedOutput();

                    // Allow run_command templates to constrain targets to the caster's owner/faction/team context.
                    String processedCommand = resolveMobCommandTemplate(command, caster);

                    server.getCommands().performPrefixedCommand(commandSource, processedCommand);

                } catch (Exception e) {
                    LOGGER.error("run_command error: {}", e.getMessage());
                }
            }
        }

        private String resolveMobCommandTemplate(String command, net.minecraft.world.entity.LivingEntity caster) {
            CompoundTag casterData = caster.getPersistentData();

            String deityId = casterData.contains(com.bluelotuscoding.eidolonunchained.ai.MobChantCastingGoal.FAITH_DEITY_TAG, Tag.TAG_STRING)
                ? casterData.getString(com.bluelotuscoding.eidolonunchained.ai.MobChantCastingGoal.FAITH_DEITY_TAG)
                : "";

            String thrallKey = elucent.eidolon.util.EntityUtil.THRALL_KEY;
            String ownerUuid = "00000000-0000-0000-0000-000000000000";
            String ownerUuidIntArray = "[I;0,0,0,0]";

            if (casterData.contains(thrallKey, Tag.TAG_INT_ARRAY)) {
                UUID owner = casterData.getUUID(thrallKey);
                ownerUuid = owner.toString();
                ownerUuidIntArray = uuidToIntArraySnbt(owner);
            }

            String teamName = caster.getTeam() != null ? caster.getTeam().getName() : "";

            return command
                .replace("{faction_deity}", deityId)
                .replace("{thrall_key}", thrallKey)
                .replace("{owner_uuid}", ownerUuid)
                .replace("{owner_uuid_int_array}", ownerUuidIntArray)
                .replace("{caster_team}", teamName);
        }

        private String uuidToIntArraySnbt(UUID uuid) {
            int mostSigBitsHigh = (int) (uuid.getMostSignificantBits() >> 32);
            int mostSigBitsLow = (int) uuid.getMostSignificantBits();
            int leastSigBitsHigh = (int) (uuid.getLeastSignificantBits() >> 32);
            int leastSigBitsLow = (int) uuid.getLeastSignificantBits();
            return "[I;" + mostSigBitsHigh + "," + mostSigBitsLow + "," + leastSigBitsHigh + "," + leastSigBitsLow + "]";
        }
        
        private void sendMessage(net.minecraft.server.level.ServerPlayer player) {
            String message = data.get("message").getAsString();
            player.sendSystemMessage(Component.literal(message));
        }
        
        // ── Phase 2 mechanical effects ────────────────────────────────────────

        private void restoreMana(net.minecraft.server.level.ServerPlayer player) {
            float amount = data.has("amount") ? (float) data.get("amount").getAsDouble() : 0f;
            if (amount <= 0f) return;
            ISoul soul = player.getCapability(ISoul.INSTANCE).orElse(null);
            if (soul == null) return;
            soul.giveMagic(amount);
            Networking.sendToTracking(player.level(), player.getOnPos(), new SoulUpdatePacket(player));
        }

        private void increaseMaxMana(net.minecraft.server.level.ServerPlayer player) {
            float amount = data.has("amount") ? (float) data.get("amount").getAsDouble() : 0f;
            if (amount <= 0f) return;

            AttributeInstance soulManaAttr = player.getAttribute(EidolonUnchainedAttributes.SOUL_MANA.get());
            if (soulManaAttr != null) {
                soulManaAttr.setBaseValue(Math.max(0.0d, soulManaAttr.getBaseValue() + amount));
            }

            ISoul soul = player.getCapability(ISoul.INSTANCE).orElse(null);
            if (soul == null) return;
            float maxFromAttr = soulManaAttr != null ? (float) Math.max(0.0d, soulManaAttr.getValue()) : (soul.getMaxMagic() + amount);
            soul.setMaxMagic(maxFromAttr);
            if (soul.getMagic() > soul.getMaxMagic()) {
                soul.setMagic(soul.getMaxMagic());
            }
            Networking.sendToTracking(player.level(), player.getOnPos(), new SoulUpdatePacket(player));
        }

        private void modifyAttribute(net.minecraft.server.level.ServerPlayer player) {
            // Generic attribute modifier effect.
            // Supported ops: add, multiply_base, multiply_total
            // Compatibility alias: modify_magic_power defaults to eidolon:magic_power.
            String attributeId = data.has("attribute") ? data.get("attribute").getAsString() : "eidolon:magic_power";
            ResourceLocation attributeLocation = ResourceLocation.tryParse(attributeId);
            if (attributeLocation == null) {
                LOGGER.warn("modify_attribute: invalid attribute id '{}'", attributeId);
                return;
            }

            net.minecraft.world.entity.ai.attributes.Attribute attribute =
                net.minecraftforge.registries.ForgeRegistries.ATTRIBUTES.getValue(attributeLocation);
            if (attribute == null) {
                LOGGER.warn("modify_attribute: unknown attribute '{}'", attributeId);
                return;
            }

            double amount = data.has("amount") ? data.get("amount").getAsDouble()
                : (data.has("multiplier") ? data.get("multiplier").getAsDouble() : 0.0);
            if (amount == 0.0) return;

            String opRaw = data.has("operation") ? data.get("operation").getAsString() : "add";
            AttributeModifier.Operation op = parseModifierOperation(opRaw);
            int durationTicks = data.has("duration_ticks") ? data.get("duration_ticks").getAsInt() : 0;
            boolean useCustomTimedEffect = durationTicks > 0
                && data.has("sign_icon")
                && data.has("status_effect")
                && data.has("particle");

            // apply_self: true (default) applies to the caster. false = skip self, apply to looked-at entity.
            boolean applySelf = !data.has("apply_self") || data.get("apply_self").getAsBoolean();
            // positive = buff (coloured sign icon), negative = debuff (greyed icon), derived from amount sign.
            boolean positive = amount >= 0.0;

            String idSeed = data.has("modifier_id") ? data.get("modifier_id").getAsString()
                : ("eu_attr_" + attributeId + "_" + opRaw + "_" + amount);
            UUID modifierId = UUID.nameUUIDFromBytes(idSeed.getBytes(StandardCharsets.UTF_8));

            if (applySelf) {
                applyAttributeModifierToEntity(player, player, attribute, attributeLocation,
                    amount, op, modifierId, useCustomTimedEffect, durationTicks, positive);
                applyTypedResistancesToEntity(player, modifierId, durationTicks, useCustomTimedEffect);
            } else {
                // Robust entity raytrace to living targets the caster can interact with.
                final double range = 10.0;
                net.minecraft.world.phys.Vec3 eyePos = player.getEyePosition();
                net.minecraft.world.phys.Vec3 lookVec = player.getLookAngle();
                net.minecraft.world.phys.Vec3 endPos = eyePos.add(lookVec.scale(range));

                net.minecraft.world.phys.EntityHitResult entityHit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                    player,
                    eyePos,
                    endPos,
                    player.getBoundingBox().expandTowards(lookVec.scale(range)).inflate(1.0),
                    e -> e instanceof net.minecraft.world.entity.LivingEntity
                        && e.isAlive()
                        && e != player
                        && !e.isSpectator(),
                    range * range
                );

                if (entityHit != null && entityHit.getEntity() instanceof net.minecraft.world.entity.LivingEntity living) {
                    applyAttributeModifierToEntity(player, living, attribute, attributeLocation,
                        amount, op, modifierId, useCustomTimedEffect, durationTicks, positive);
                    applyTypedResistancesToEntity(living, modifierId, durationTicks, useCustomTimedEffect);
                } else {
                    LOGGER.debug("modify_attribute: no living entity target found for apply_self=false");
                }
            }
        }

        private void modifyAttribute(net.minecraft.world.entity.LivingEntity caster) {
            String attributeId = data.has("attribute") ? data.get("attribute").getAsString() : "eidolon:magic_power";
            ResourceLocation attributeLocation = ResourceLocation.tryParse(attributeId);
            if (attributeLocation == null) {
                LOGGER.warn("modify_attribute: invalid attribute id '{}'", attributeId);
                return;
            }

            net.minecraft.world.entity.ai.attributes.Attribute attribute =
                net.minecraftforge.registries.ForgeRegistries.ATTRIBUTES.getValue(attributeLocation);
            if (attribute == null) {
                LOGGER.warn("modify_attribute: unknown attribute '{}'", attributeId);
                return;
            }

            double amount = data.has("amount") ? data.get("amount").getAsDouble()
                : (data.has("multiplier") ? data.get("multiplier").getAsDouble() : 0.0);
            if (amount == 0.0) return;

            String opRaw = data.has("operation") ? data.get("operation").getAsString() : "add";
            AttributeModifier.Operation op = parseModifierOperation(opRaw);
            int durationTicks = data.has("duration_ticks") ? data.get("duration_ticks").getAsInt() : 0;
            boolean useCustomTimedEffect = durationTicks > 0
                && data.has("sign_icon")
                && data.has("status_effect")
                && data.has("particle");
            boolean positive = amount >= 0.0;

            String idSeed = data.has("modifier_id") ? data.get("modifier_id").getAsString()
                : ("eu_attr_" + attributeId + "_" + opRaw + "_" + amount);
            UUID modifierId = UUID.nameUUIDFromBytes(idSeed.getBytes(StandardCharsets.UTF_8));

            AttributeInstance instance = caster.getAttribute(attribute);
            if (instance == null) {
                return;
            }

            AttributeModifier existing = instance.getModifier(modifierId);
            if (existing != null) {
                instance.removeModifier(existing);
            }

            AttributeModifier modifier = new AttributeModifier(
                modifierId,
                "eu_chant_attr_" + attributeLocation.getPath(),
                amount,
                op);
            instance.addPermanentModifier(modifier);

            if (useCustomTimedEffect) {
                TimedAttributeModifierManager.registerOrRefresh(
                    caster,
                    attributeLocation,
                    modifierId,
                    durationTicks,
                    resolveParticleSpec()
                );
                emitConfiguredParticlesAt(caster);
            } else {
                TimedAttributeModifierManager.cancel(caster, modifierId);
            }

            applyTypedResistancesToEntity(caster, modifierId, durationTicks, useCustomTimedEffect);

            LOGGER.info(
                "modify_attribute applied to mob: target='{}' attribute='{}' after={} amount={} op={} modifierId={} timed={} durationTicks={}",
                caster.getName().getString(),
                attributeLocation,
                instance.getValue(),
                amount,
                op,
                modifierId,
                useCustomTimedEffect,
                durationTicks
            );
        }

        private void applyAttributeModifierToEntity(
                net.minecraft.server.level.ServerPlayer caster,
                net.minecraft.world.entity.LivingEntity target,
                net.minecraft.world.entity.ai.attributes.Attribute attribute,
                ResourceLocation attributeLocation,
                double amount,
                AttributeModifier.Operation op,
                UUID modifierId,
                boolean useCustomTimedEffect,
                int durationTicks,
                boolean positive) {

            AttributeInstance instance = target.getAttribute(attribute);
            if (instance == null) {
                LOGGER.debug("modify_attribute: target {} has no instance for '{}'", target.getName().getString(), attributeLocation);
                return;
            }

            double beforeBase = instance.getBaseValue();
            double beforeValue = instance.getValue();

            // Remove any existing modifier (permanent OR transient) before re-applying.
            // removeModifier(UUID) only removes permanent modifiers; we must use the object
            // overload to also clear transient ones from the modifiers set.
            AttributeModifier existing = instance.getModifier(modifierId);
            if (existing != null) {
                instance.removeModifier(existing);
            }

            AttributeModifier modifier = new AttributeModifier(
                modifierId,
                "eu_chant_attr_" + attributeLocation.getPath(),
                amount,
                op);

            // Always use addPermanentModifier — addTransientModifier is not reliably synced.
            // For timed effects we schedule an explicit removeModifier after durationTicks.
            instance.addPermanentModifier(modifier);

            double afterValue = instance.getValue();
            LOGGER.info(
                "modify_attribute applied: target='{}' attribute='{}' base={} before={} after={} amount={} op={} modifierId={} timed={} durationTicks={}",
                target.getName().getString(),
                attributeLocation,
                beforeBase,
                beforeValue,
                afterValue,
                amount,
                op,
                modifierId,
                useCustomTimedEffect,
                durationTicks
            );

            // Sync the attribute change to the client immediately
            if (target instanceof net.minecraft.server.level.ServerPlayer targetSP) {
                targetSP.connection.send(new net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket(
                    targetSP.getId(), java.util.List.of(instance)));
            }

            if (useCustomTimedEffect) {
                TimedAttributeModifierManager.registerOrRefresh(
                    target,
                    attributeLocation,
                    modifierId,
                    durationTicks,
                    resolveParticleSpec()
                );
            } else {
                // Permanent application should not be removed by an older timed registration.
                TimedAttributeModifierManager.cancel(target, modifierId);
            }

            // Visual bundle: particles at target, overlay packet only sent to caster (and target if they are a player).
            if (useCustomTimedEffect) {
                ResourceLocation appliedStatusEffect = resolveAttributeVisualEffectId();
                // Emit particles at the target's position.
                emitConfiguredParticlesAt(target);
                // Send overlay only to the target if they are a player.
                if (target instanceof net.minecraft.server.level.ServerPlayer targetPlayer) {
                    sendSignStatusIconMapping(targetPlayer, appliedStatusEffect, durationTicks, positive);
                }
            }
        }

        private void applyTypedResistancesToEntity(
                net.minecraft.world.entity.LivingEntity target,
                UUID modifierId,
                int durationTicks,
                boolean useCustomTimedEffect) {
            if (!data.has("typed_resistance_key") && !data.has("typed_resistances")) {
                return;
            }

            TimedAttributeModifierManager.ParticleSpec particleSpec = useCustomTimedEffect ? resolveParticleSpec() : null;

            if (data.has("typed_resistance_key")) {
                String key = data.get("typed_resistance_key").getAsString().trim();
                if (!key.isEmpty()) {
                    double amount = data.has("typed_resistance_amount") ? data.get("typed_resistance_amount").getAsDouble() : 0.0d;
                    applyTypedResistanceEntry(target, key, amount, modifierId, durationTicks, particleSpec);
                }
            }

            if (data.has("typed_resistances") && data.get("typed_resistances").isJsonObject()) {
                JsonObject typed = data.getAsJsonObject("typed_resistances");
                for (var entry : typed.entrySet()) {
                    String key = entry.getKey() == null ? "" : entry.getKey().trim();
                    if (key.isEmpty() || !entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isNumber()) {
                        continue;
                    }

                    double amount = entry.getValue().getAsDouble();
                    applyTypedResistanceEntry(target, key, amount, modifierId, durationTicks, particleSpec);
                }
            }
        }

        private void applyTypedResistanceEntry(
                net.minecraft.world.entity.LivingEntity target,
                String key,
                double amount,
                UUID baseModifierId,
                int durationTicks,
                TimedAttributeModifierManager.ParticleSpec particleSpec) {
            if (amount == 0.0d) {
                return;
            }

            UUID typedModifierId = UUID.nameUUIDFromBytes(
                (baseModifierId.toString() + "|typed|" + key).getBytes(StandardCharsets.UTF_8));

            if (durationTicks > 0) {
                TimedTypedResistanceManager.registerOrRefresh(
                    target,
                    key,
                    typedModifierId,
                    amount,
                    durationTicks,
                    particleSpec
                );
            } else {
                TimedTypedResistanceManager.applyPermanent(target, key, typedModifierId, amount);
            }

            CompoundTag persisted = target.getPersistentData();
            CompoundTag typed = persisted.contains(TAG_DIVINE_RESISTANCES, Tag.TAG_COMPOUND)
                ? persisted.getCompound(TAG_DIVINE_RESISTANCES)
                : new CompoundTag();
            double value = typed.contains(key, Tag.TAG_ANY_NUMERIC) ? typed.getDouble(key) : 0.0d;
            LOGGER.info(
                "modify_attribute typed resistance applied: target='{}' key='{}' amount={} resulting={} modifierId={} timed={} durationTicks={}",
                target.getName().getString(),
                key,
                amount,
                value,
                typedModifierId,
                durationTicks > 0,
                durationTicks
            );
        }

        private ResourceLocation resolveAttributeVisualEffectId() {
            if (parentChant == null) {
                return null;
            }

            // Resolution order:
            // 1) explicit status_effect if provided (treated as a custom timed effect ID)
            // 2) <chant>_effect fallback
            if (data.has("status_effect")) {
                ResourceLocation preferred = ResourceLocation.tryParse(data.get("status_effect").getAsString());
                if (preferred != null) {
                    return preferred;
                }
            }

            return resolveAttributeFallbackEffectId();
        }

        private ResourceLocation resolveAttributeFallbackEffectId() {
            if (parentChant == null) {
                return null;
            }

            ResourceLocation chantId = parentChant.getId();
            String basePath = chantId.getPath();
            return new ResourceLocation(chantId.getNamespace(), basePath + "_effect");
        }

        private void emitConfiguredParticlesAt(net.minecraft.world.entity.LivingEntity target) {
            if (!data.has("particle")) {
                return;
            }

            ResourceLocation particleId = ResourceLocation.tryParse(data.get("particle").getAsString());
            if (particleId == null) {
                return;
            }

            net.minecraft.core.particles.ParticleType<?> particleType =
                net.minecraftforge.registries.ForgeRegistries.PARTICLE_TYPES.getValue(particleId);
            if (!(particleType instanceof net.minecraft.core.particles.SimpleParticleType simpleParticle)) {
                LOGGER.debug("modify_attribute: particle '{}' is not a simple particle type", particleId);
                return;
            }

            int count = data.has("particle_count") ? data.get("particle_count").getAsInt() : 12;
            double spread = data.has("particle_spread") ? data.get("particle_spread").getAsDouble() : 0.3;
            double speed = data.has("particle_speed") ? data.get("particle_speed").getAsDouble() : 0.01;

            if (target.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.sendParticles(
                    simpleParticle,
                    target.getX(),
                    target.getY() + 1.0,
                    target.getZ(),
                    Math.max(1, count),
                    spread,
                    spread,
                    spread,
                    speed
                );
            }
        }

        private void sendSignStatusIconMapping(net.minecraft.server.level.ServerPlayer player, ResourceLocation effectId, int durationTicks, boolean positive) {
            if (effectId == null || parentChant == null) {
                return;
            }

            ResourceLocation signId = null;
            if (data.has("sign_icon")) {
                signId = ResourceLocation.tryParse(data.get("sign_icon").getAsString());
            }

            if (signId == null && !parentChant.getSignSequence().isEmpty()) {
                signId = parentChant.getSignSequence().get(0);
            }

            if (signId == null) {
                return;
            }

            EidolonUnchainedNetworking.sendToPlayer(player, new AttributeSignStatusIconPacket(effectId, signId, durationTicks, positive));
        }

        private TimedAttributeModifierManager.ParticleSpec resolveParticleSpec() {
            if (!data.has("particle")) {
                return null;
            }

            ResourceLocation particleId = ResourceLocation.tryParse(data.get("particle").getAsString());
            if (particleId == null) {
                return null;
            }

            int count = data.has("particle_count") ? data.get("particle_count").getAsInt() : 12;
            double spread = data.has("particle_spread") ? data.get("particle_spread").getAsDouble() : 0.3;
            double speed = data.has("particle_speed") ? data.get("particle_speed").getAsDouble() : 0.01;

            return new TimedAttributeModifierManager.ParticleSpec(particleId, Math.max(1, count), spread, speed);
        }

        private AttributeModifier.Operation parseModifierOperation(String value) {
            if (value == null) {
                return AttributeModifier.Operation.ADDITION;
            }

            return switch (value.toLowerCase()) {
                case "add", "addition" -> AttributeModifier.Operation.ADDITION;
                case "multiply_base", "multiplybase", "mul_base", "mulbase" -> AttributeModifier.Operation.MULTIPLY_BASE;
                case "multiply_total", "multiplytotal", "mul_total", "multotal" -> AttributeModifier.Operation.MULTIPLY_TOTAL;
                default -> AttributeModifier.Operation.ADDITION;
            };
        }

        private void applyChantCooldown(net.minecraft.server.level.ServerPlayer player) {
            // Lets a chant effect impose a cooldown on another chant
            if (!data.has("chant")) return;
            String chantIdStr = data.get("chant").getAsString();
            int durationSeconds = data.has("duration_seconds") ? data.get("duration_seconds").getAsInt() : 60;
            try {
                ResourceLocation chantId = new ResourceLocation(chantIdStr);
                DatapackChant target = DatapackChantManager.getChant(chantId);
                if (target != null) {
                    ChantCooldownManager.setCooldownSeconds(player, target, durationSeconds);
                } else {
                    LOGGER.warn("apply_cooldown: chant '{}' not found", chantIdStr);
                }
            } catch (Exception e) {
                LOGGER.error("apply_cooldown error: {}", e.getMessage());
            }
        }

        private void applyRaycastEffect(net.minecraft.world.entity.LivingEntity caster) {
            if (!(caster.level() instanceof net.minecraft.server.level.ServerLevel level)) {
                return;
            }

            double range = data.has("range") ? Math.max(0.5d, data.get("range").getAsDouble()) : 8.0d;
            double bbInflation = data.has("bb_inflation") ? Math.max(0.0d, data.get("bb_inflation").getAsDouble()) : 0.35d;
            boolean checkForBlocks = !data.has("check_for_blocks") || data.get("check_for_blocks").getAsBoolean();
            boolean applyToCasterOnMiss = data.has("apply_to_caster_on_miss") && data.get("apply_to_caster_on_miss").getAsBoolean();

            Vec3 start = caster.getEyePosition();
            Vec3 end = start.add(caster.getLookAngle().normalize().scale(range));
            Vec3 rayEnd = end;

            if (checkForBlocks) {
                net.minecraft.world.phys.BlockHitResult blockHit = level.clip(
                    new net.minecraft.world.level.ClipContext(
                        start,
                        end,
                        net.minecraft.world.level.ClipContext.Block.COLLIDER,
                        net.minecraft.world.level.ClipContext.Fluid.NONE,
                        caster
                    )
                );
                rayEnd = blockHit.getLocation();
            }

            AABB searchBox = caster.getBoundingBox().expandTowards(rayEnd.subtract(start)).inflate(bbInflation);
            List<net.minecraft.world.entity.LivingEntity> candidates = level.getEntitiesOfClass(
                net.minecraft.world.entity.LivingEntity.class,
                searchBox,
                entity -> entity != caster && entity.isAlive() && !caster.isAlliedTo(entity)
            );

            net.minecraft.world.entity.LivingEntity hitTarget = null;
            double bestDist = Double.MAX_VALUE;
            for (net.minecraft.world.entity.LivingEntity candidate : candidates) {
                java.util.Optional<Vec3> clip = candidate.getBoundingBox().inflate(bbInflation).clip(start, rayEnd);
                if (clip.isEmpty()) {
                    continue;
                }

                double dist = start.distanceToSqr(clip.get());
                if (dist < bestDist) {
                    bestDist = dist;
                    hitTarget = candidate;
                }
            }

            if (hitTarget != null) {
                applyNestedEffectsToTarget(caster, hitTarget);
                return;
            }

            if (applyToCasterOnMiss) {
                applyNestedEffectsToTarget(caster, caster);
            }
        }

        private void applyMagicWeapon(net.minecraft.world.entity.LivingEntity caster) {
            String slotName = data.has("slot") ? data.get("slot").getAsString().toLowerCase(java.util.Locale.ROOT) : "mainhand";
            net.minecraft.world.item.ItemStack target = slotName.equals("offhand")
                ? caster.getOffhandItem()
                : caster.getMainHandItem();

            if (target.isEmpty()) return;

            int duration = data.has("duration") ? data.get("duration").getAsInt() : -1;
            String damageType = resolveConfiguredDamageType();
            Integer hitColorRgb = resolveLinkedDeityHitColorRgb();
            long gameTime = caster.level().getGameTime();
            com.bluelotuscoding.eidolonunchained.events.MagicWeaponEventHandler.applyMagicWeaponTag(
                target, duration, gameTime, damageType, hitColorRgb
            );
        }

        private String resolveConfiguredDamageType() {
            if (data.has("damage_type") && !data.get("damage_type").getAsString().isBlank()) {
                return data.get("damage_type").getAsString();
            }

            if (parentChant == null || !parentChant.hasLinkedDeity()) {
                return null;
            }

            com.bluelotuscoding.eidolonunchained.deity.DatapackDeity deity =
                com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager.getDeity(parentChant.getLinkedDeity());
            if (deity == null || deity.getDeityDamageType() == null) {
                return null;
            }

            return deity.getDeityDamageType().toString();
        }

        private Integer resolveLinkedDeityHitColorRgb() {
            if (parentChant == null || !parentChant.hasLinkedDeity()) {
                return null;
            }

            com.bluelotuscoding.eidolonunchained.deity.DatapackDeity deity =
                com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager.getDeity(parentChant.getLinkedDeity());
            if (deity == null) {
                return null;
            }

            int r = Math.round(deity.getRed() * 255.0f);
            int g = Math.round(deity.getGreen() * 255.0f);
            int b = Math.round(deity.getBlue() * 255.0f);
            return ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
        }

        private void applyAreaEffect(net.minecraft.world.entity.LivingEntity caster) {
            if (!(caster.level() instanceof net.minecraft.server.level.ServerLevel level)) {
                return;
            }

            double radius = data.has("radius") ? Math.max(0.1d, data.get("radius").getAsDouble()) : 4.0d;
            boolean excludeCaster = !data.has("exclude_caster") || data.get("exclude_caster").getAsBoolean();
            int maxTargets = data.has("max_targets") ? Math.max(1, data.get("max_targets").getAsInt()) : Integer.MAX_VALUE;
            String shape = data.has("shape") ? data.get("shape").getAsString().toLowerCase(java.util.Locale.ROOT) : "sphere";
            // cylinder height: defaults to 2*radius (same as sphere bounding box), or explicit "height" field
            double halfHeight = shape.equals("cylinder")
                ? (data.has("height") ? data.get("height").getAsDouble() * 0.5 : radius)
                : radius;

            Vec3 center = resolveAreaCenter(caster);

            // Particle emission at the area center
            if (data.has("particle")) {
                net.minecraft.resources.ResourceLocation particleId =
                    net.minecraft.resources.ResourceLocation.tryParse(data.get("particle").getAsString());
                if (particleId != null) {
                    net.minecraft.core.particles.ParticleType<?> pt =
                        net.minecraftforge.registries.ForgeRegistries.PARTICLE_TYPES.getValue(particleId);
                    if (pt instanceof net.minecraft.core.particles.SimpleParticleType spt) {
                        int pCount = data.has("particle_count") ? Math.max(1, data.get("particle_count").getAsInt()) : 16;
                        double pSpread = data.has("particle_spread") ? data.get("particle_spread").getAsDouble() : radius * 0.5;
                        double pSpeed = data.has("particle_speed") ? data.get("particle_speed").getAsDouble() : 0.05;
                        level.sendParticles(spt, center.x, center.y + 0.5, center.z, pCount, pSpread, pSpread * 0.5, pSpread, pSpeed);
                    }
                }
            }

            AABB areaBox = new AABB(
                center.x - radius,
                center.y - halfHeight,
                center.z - radius,
                center.x + radius,
                center.y + halfHeight,
                center.z + radius
            );

            final double radiusSq = radius * radius;
            List<net.minecraft.world.entity.LivingEntity> targets = level.getEntitiesOfClass(
                net.minecraft.world.entity.LivingEntity.class,
                areaBox,
                entity -> {
                    if (!entity.isAlive()) return false;
                    if (excludeCaster && entity == caster) return false;
                    if (entity != caster && caster.isAlliedTo(entity)) return false;
                    Vec3 pos = entity.position();
                    return switch (shape) {
                        // cylinder: 2D circle on XZ plane, height already bounded by AABB
                        case "cylinder" -> {
                            double dx = pos.x - center.x;
                            double dz = pos.z - center.z;
                            yield (dx * dx + dz * dz) <= radiusSq;
                        }
                        // cube: AABB check is sufficient — all entities inside the box qualify
                        case "cube" -> true;
                        // sphere (default)
                        default -> entity.distanceToSqr(center) <= radiusSq;
                    };
                }
            );

            if (targets.size() > 1) {
                targets.sort(java.util.Comparator.comparingDouble(entity -> entity.distanceToSqr(center)));
            }

            int applied = 0;
            for (net.minecraft.world.entity.LivingEntity target : targets) {
                applyNestedEffectsToTarget(caster, target);
                applied++;
                if (applied >= maxTargets) {
                    break;
                }
            }
        }

        private Vec3 resolveAreaCenter(net.minecraft.world.entity.LivingEntity caster) {
            String centerMode = data.has("center") ? data.get("center").getAsString() : "caster";
            if ("look".equalsIgnoreCase(centerMode) || "target".equalsIgnoreCase(centerMode)) {
                double range = data.has("range") ? Math.max(0.5d, data.get("range").getAsDouble()) : 6.0d;
                Vec3 eyePos = caster.getEyePosition();
                Vec3 endPos = eyePos.add(caster.getLookAngle().scale(range));
                if (caster.level() instanceof net.minecraft.server.level.ServerLevel level) {
                    net.minecraft.world.phys.BlockHitResult hit = level.clip(
                        new net.minecraft.world.level.ClipContext(
                            eyePos,
                            endPos,
                            net.minecraft.world.level.ClipContext.Block.COLLIDER,
                            net.minecraft.world.level.ClipContext.Fluid.NONE,
                            caster
                        )
                    );
                    if (hit.getType() != HitResult.Type.MISS) {
                        return hit.getLocation();
                    }
                }
                return endPos;
            }
            return caster.position();
        }

        private void applyNestedEffectsToTarget(net.minecraft.world.entity.LivingEntity caster, net.minecraft.world.entity.LivingEntity target) {
            JsonArray nested = data.has("effects") && data.get("effects").isJsonArray() ? data.getAsJsonArray("effects") : null;
            if (nested == null || nested.isEmpty()) {
                return;
            }

            for (JsonElement element : nested) {
                if (element == null || !element.isJsonObject()) {
                    continue;
                }

                JsonObject nestedData = element.getAsJsonObject();
                if (!nestedData.has("type")) {
                    continue;
                }

                String nestedType = nestedData.get("type").getAsString();
                ChantEffect nestedEffect = new ChantEffect(nestedType, nestedData);
                nestedEffect.setParentChant(parentChant);

                // Player-only effects naturally no-op for non-player targets via apply(LivingEntity).
                nestedEffect.apply(target);
            }
        }

        private void spawnProjectileEffect(net.minecraft.server.level.ServerPlayer player) {
            spawnProjectile(player);
        }

        private void spawnProjectileEffect(net.minecraft.world.entity.LivingEntity caster) {
            spawnProjectile(caster);
        }

        private void spawnProjectile(net.minecraft.server.level.ServerPlayer player) {
            net.minecraft.server.level.ServerLevel level = player.serverLevel();

            String projectileType = data.has("projectile_id")
                ? data.get("projectile_id").getAsString().toLowerCase()
                : (data.has("projectile_type")
                    ? data.get("projectile_type").getAsString().toLowerCase()
                    : "arrow");
            projectileType = projectileType.trim();
            if (projectileType.isEmpty()) {
                projectileType = "arrow";
            }
            float speed = data.has("speed") ? (float) data.get("speed").getAsDouble() : 1.5f;
            float inaccuracy = data.has("inaccuracy") ? (float) data.get("inaccuracy").getAsDouble() : 0.0f;
            double gravity = data.has("gravity") ? data.get("gravity").getAsDouble() : 0.05d;
            double damage = data.has("damage") ? data.get("damage").getAsDouble() : -1.0d;

            net.minecraft.world.entity.projectile.Projectile projectile;

            switch (projectileType) {
                case "arrow" -> {
                    net.minecraft.world.entity.projectile.Arrow arrow =
                        new net.minecraft.world.entity.projectile.Arrow(level, player);
                    arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, speed, inaccuracy);
                    if (damage >= 0.0d) {
                        arrow.setBaseDamage(damage);
                    }
                    projectile = arrow;
                }
                case "snowball" -> {
                    net.minecraft.world.entity.projectile.Snowball snowball =
                        new net.minecraft.world.entity.projectile.Snowball(level, player);
                    snowball.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, speed, inaccuracy);
                    projectile = snowball;
                }
                case "small_fireball" -> {
                    net.minecraft.world.phys.Vec3 look = player.getLookAngle().normalize().scale(speed);
                    net.minecraft.world.entity.projectile.SmallFireball fireball =
                        new net.minecraft.world.entity.projectile.SmallFireball(level, player, look.x, look.y, look.z);
                    net.minecraft.world.phys.Vec3 eye = player.getEyePosition();
                    fireball.setPos(eye.x, eye.y, eye.z);
                    projectile = fireball;
                }
                case "blank" -> {
                    // Invisible vanilla projectile shell intended for datapack-defined impact particles.
                    net.minecraft.world.entity.projectile.Snowball blank =
                        new net.minecraft.world.entity.projectile.Snowball(level, player);
                    blank.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, speed, inaccuracy);
                    blank.setInvisible(true);
                    projectile = blank;
                }
                case "fireball", "large_fireball" -> {
                    net.minecraft.world.phys.Vec3 look = player.getLookAngle().normalize().scale(speed);
                    int explosionPower = data.has("explosion_power") ? data.get("explosion_power").getAsInt() : 1;
                    net.minecraft.world.entity.projectile.LargeFireball fireball =
                        new net.minecraft.world.entity.projectile.LargeFireball(level, player, look.x, look.y, look.z, explosionPower);
                    net.minecraft.world.phys.Vec3 eye = player.getEyePosition();
                    fireball.setPos(eye.x, eye.y, eye.z);
                    projectile = fireball;
                }
                default -> {
                    // Fallback: allow any projectile entity id (includes GeckoLib-backed projectile entities from other mods).
                    ResourceLocation entityId = ResourceLocation.tryParse(projectileType);
                    if (entityId == null) {
                        LOGGER.warn("spawn_projectile/projectile_effect: unsupported projectile id '{}'", projectileType);
                        return;
                    }

                    net.minecraft.world.entity.EntityType<?> type =
                        net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(entityId);
                    if (type == null) {
                        LOGGER.warn("spawn_projectile/projectile_effect: unknown entity type '{}'", entityId);
                        return;
                    }

                    net.minecraft.world.entity.Entity created = type.create(level);
                    if (!(created instanceof net.minecraft.world.entity.projectile.Projectile genericProjectile)) {
                        LOGGER.warn("spawn_projectile/projectile_effect: entity '{}' is not a projectile", entityId);
                        return;
                    }

                    genericProjectile.setOwner(player);
                    net.minecraft.world.phys.Vec3 eye = player.getEyePosition();
                    genericProjectile.setPos(eye.x, eye.y, eye.z);
                    net.minecraft.world.phys.Vec3 look = player.getLookAngle().normalize();
                    genericProjectile.shoot(look.x, look.y, look.z, speed, inaccuracy);

                    if (genericProjectile instanceof net.minecraft.world.entity.projectile.AbstractArrow arrow && damage >= 0.0d) {
                        arrow.setBaseDamage(damage);
                    }

                    projectile = genericProjectile;
                }
            }

            attachProjectileParticleConfig(projectile);

            // Projectiles only support gravity on/off, so treat <= 0 as no gravity.
            projectile.setNoGravity(gravity <= 0.0d);
            level.addFreshEntity(projectile);
        }

        private void spawnProjectile(net.minecraft.world.entity.LivingEntity caster) {
            if (!(caster.level() instanceof net.minecraft.server.level.ServerLevel level)) {
                return;
            }

            String projectileType = data.has("projectile_id")
                ? data.get("projectile_id").getAsString().toLowerCase()
                : (data.has("projectile_type")
                    ? data.get("projectile_type").getAsString().toLowerCase()
                    : "arrow");
            projectileType = projectileType.trim();
            if (projectileType.isEmpty()) {
                projectileType = "arrow";
            }
            float speed = data.has("speed") ? (float) data.get("speed").getAsDouble() : 1.5f;
            float inaccuracy = data.has("inaccuracy") ? (float) data.get("inaccuracy").getAsDouble() : 0.0f;
            double gravity = data.has("gravity") ? data.get("gravity").getAsDouble() : 0.05d;
            double damage = data.has("damage") ? data.get("damage").getAsDouble() : -1.0d;

            net.minecraft.world.entity.projectile.Projectile projectile;

            switch (projectileType) {
                case "arrow" -> {
                    net.minecraft.world.entity.projectile.Arrow arrow =
                        new net.minecraft.world.entity.projectile.Arrow(level, caster);
                    arrow.shootFromRotation(caster, caster.getXRot(), caster.getYRot(), 0.0f, speed, inaccuracy);
                    if (damage >= 0.0d) {
                        arrow.setBaseDamage(damage);
                    }
                    projectile = arrow;
                }
                case "snowball" -> {
                    net.minecraft.world.entity.projectile.Snowball snowball =
                        new net.minecraft.world.entity.projectile.Snowball(level, caster);
                    snowball.shootFromRotation(caster, caster.getXRot(), caster.getYRot(), 0.0f, speed, inaccuracy);
                    projectile = snowball;
                }
                case "small_fireball" -> {
                    net.minecraft.world.phys.Vec3 look = caster.getLookAngle().normalize().scale(speed);
                    net.minecraft.world.entity.projectile.SmallFireball fireball =
                        new net.minecraft.world.entity.projectile.SmallFireball(level, caster, look.x, look.y, look.z);
                    net.minecraft.world.phys.Vec3 eye = caster.getEyePosition();
                    fireball.setPos(eye.x, eye.y, eye.z);
                    projectile = fireball;
                }
                case "blank" -> {
                    net.minecraft.world.entity.projectile.Snowball blank =
                        new net.minecraft.world.entity.projectile.Snowball(level, caster);
                    blank.shootFromRotation(caster, caster.getXRot(), caster.getYRot(), 0.0f, speed, inaccuracy);
                    blank.setInvisible(true);
                    projectile = blank;
                }
                case "fireball", "large_fireball" -> {
                    net.minecraft.world.phys.Vec3 look = caster.getLookAngle().normalize().scale(speed);
                    int explosionPower = data.has("explosion_power") ? data.get("explosion_power").getAsInt() : 1;
                    net.minecraft.world.entity.projectile.LargeFireball fireball =
                        new net.minecraft.world.entity.projectile.LargeFireball(level, caster, look.x, look.y, look.z, explosionPower);
                    net.minecraft.world.phys.Vec3 eye = caster.getEyePosition();
                    fireball.setPos(eye.x, eye.y, eye.z);
                    projectile = fireball;
                }
                default -> {
                    ResourceLocation entityId = ResourceLocation.tryParse(projectileType);
                    if (entityId == null) {
                        LOGGER.warn("spawn_projectile/projectile_effect: unsupported projectile id '{}'", projectileType);
                        return;
                    }

                    net.minecraft.world.entity.EntityType<?> type =
                        net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(entityId);
                    if (type == null) {
                        LOGGER.warn("spawn_projectile/projectile_effect: unknown entity type '{}'", entityId);
                        return;
                    }

                    net.minecraft.world.entity.Entity created = type.create(level);
                    if (!(created instanceof net.minecraft.world.entity.projectile.Projectile genericProjectile)) {
                        LOGGER.warn("spawn_projectile/projectile_effect: entity '{}' is not a projectile", entityId);
                        return;
                    }

                    genericProjectile.setOwner(caster);
                    net.minecraft.world.phys.Vec3 eye = caster.getEyePosition();
                    genericProjectile.setPos(eye.x, eye.y, eye.z);
                    net.minecraft.world.phys.Vec3 look = caster.getLookAngle().normalize();
                    genericProjectile.shoot(look.x, look.y, look.z, speed, inaccuracy);

                    if (genericProjectile instanceof net.minecraft.world.entity.projectile.AbstractArrow arrow && damage >= 0.0d) {
                        arrow.setBaseDamage(damage);
                    }

                    projectile = genericProjectile;
                }
            }

            attachProjectileParticleConfig(projectile);
            projectile.setNoGravity(gravity <= 0.0d);
            level.addFreshEntity(projectile);
        }

        private void summonEnthralledEntity(net.minecraft.server.level.ServerPlayer player) {
            String entityId = data.has("entity")
                ? data.get("entity").getAsString()
                : (data.has("entity_id") ? data.get("entity_id").getAsString() : "");
            if (entityId.isEmpty()) {
                LOGGER.warn("summoning: missing required 'entity' or 'entity_id'");
                return;
            }

            ResourceLocation entityLocation = ResourceLocation.tryParse(entityId);
            if (entityLocation == null) {
                LOGGER.warn("summoning: invalid entity id '{}'", entityId);
                return;
            }

            net.minecraft.world.entity.EntityType<?> entityType =
                net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(entityLocation);
            if (entityType == null) {
                LOGGER.warn("summoning: unknown entity type '{}'", entityId);
                return;
            }

            int count = data.has("count") ? Math.max(1, Math.min(16, data.get("count").getAsInt())) : 1;
            double spread = data.has("spread") ? Math.max(0.0d, data.get("spread").getAsDouble()) : 0.75d;
            boolean disableBossBar = !data.has("disable_boss_bar") || data.get("disable_boss_bar").getAsBoolean();

            net.minecraft.server.level.ServerLevel level = player.serverLevel();
            net.minecraft.world.phys.Vec3 basePos = resolveSummonBasePosition(player);

            for (int i = 0; i < count; i++) {
                net.minecraft.world.entity.Entity created = entityType.create(level);
                if (!(created instanceof net.minecraft.world.entity.LivingEntity living)) {
                    LOGGER.warn("summoning: entity '{}' is not a LivingEntity; skipping", entityId);
                    continue;
                }

                double offsetX = (level.random.nextDouble() - 0.5d) * 2.0d * spread;
                double offsetZ = (level.random.nextDouble() - 0.5d) * 2.0d * spread;
                living.moveTo(
                    basePos.x + offsetX,
                    basePos.y,
                    basePos.z + offsetZ,
                    level.random.nextFloat() * 360.0f,
                    0.0f
                );

                if (living instanceof net.minecraft.world.entity.Mob mob) {
                    mob.finalizeSpawn(
                        level,
                        level.getCurrentDifficultyAt(net.minecraft.core.BlockPos.containing(living.position())),
                        net.minecraft.world.entity.MobSpawnType.MOB_SUMMONED,
                        null,
                        null
                    );
                    if (data.has("can_pickup_loot")) {
                        mob.setCanPickUpLoot(data.get("can_pickup_loot").getAsBoolean());
                    }
                }

                applySummonEquipment(living);
                applySummonChantRotation(living);
                inheritSummonFaith(living, player);

                // Mark as summoned: no loot drops, track owner for death propagation.
                living.getPersistentData().putBoolean(MobChantCastingGoal.SUMMON_NO_LOOT_TAG, true);
                if (living instanceof net.minecraft.world.entity.Mob summonMob) {
                    for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
                        summonMob.setDropChance(slot, 0.0f);
                    }
                }

                elucent.eidolon.util.EntityUtil.enthrall(player, living);

                level.addFreshEntity(living);
                MobChantCastingGoal.registerSummon(player.getUUID(), living.getUUID());

                if (disableBossBar) {
                    disableBossBars(living, level);
                }
            }
        }

        private void summonEnthralledEntity(net.minecraft.world.entity.LivingEntity caster) {
            String entityId = data.has("entity")
                ? data.get("entity").getAsString()
                : (data.has("entity_id") ? data.get("entity_id").getAsString() : "");
            if (entityId.isEmpty()) {
                LOGGER.warn("summoning: missing required 'entity' or 'entity_id'");
                return;
            }

            ResourceLocation entityLocation = ResourceLocation.tryParse(entityId);
            if (entityLocation == null) {
                LOGGER.warn("summoning: invalid entity id '{}'", entityId);
                return;
            }

            net.minecraft.world.entity.EntityType<?> entityType =
                net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(entityLocation);
            if (entityType == null) {
                LOGGER.warn("summoning: unknown entity type '{}'", entityId);
                return;
            }

            if (!(caster.level() instanceof net.minecraft.server.level.ServerLevel level)) {
                return;
            }

            int count = data.has("count") ? Math.max(1, Math.min(16, data.get("count").getAsInt())) : 1;
            double spread = data.has("spread") ? Math.max(0.0d, data.get("spread").getAsDouble()) : 0.75d;
            boolean disableBossBar = !data.has("disable_boss_bar") || data.get("disable_boss_bar").getAsBoolean();
            net.minecraft.world.phys.Vec3 basePos = resolveSummonBasePosition(caster);

            for (int i = 0; i < count; i++) {
                net.minecraft.world.entity.Entity created = entityType.create(level);
                if (!(created instanceof net.minecraft.world.entity.LivingEntity living)) {
                    LOGGER.warn("summoning: entity '{}' is not a LivingEntity; skipping", entityId);
                    continue;
                }

                double offsetX = (level.random.nextDouble() - 0.5d) * 2.0d * spread;
                double offsetZ = (level.random.nextDouble() - 0.5d) * 2.0d * spread;
                living.moveTo(
                    basePos.x + offsetX,
                    basePos.y,
                    basePos.z + offsetZ,
                    level.random.nextFloat() * 360.0f,
                    0.0f
                );

                if (living instanceof net.minecraft.world.entity.Mob mob) {
                    mob.finalizeSpawn(
                        level,
                        level.getCurrentDifficultyAt(net.minecraft.core.BlockPos.containing(living.position())),
                        net.minecraft.world.entity.MobSpawnType.MOB_SUMMONED,
                        null,
                        null
                    );
                    if (data.has("can_pickup_loot")) {
                        mob.setCanPickUpLoot(data.get("can_pickup_loot").getAsBoolean());
                    }
                }

                applySummonEquipment(living);
                applySummonChantRotation(living);
                inheritSummonFaith(living, caster);

                // Mark as summoned: no loot drops, track owner for death propagation.
                living.getPersistentData().putBoolean(MobChantCastingGoal.SUMMON_NO_LOOT_TAG, true);
                if (living instanceof net.minecraft.world.entity.Mob summonMob) {
                    for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
                        summonMob.setDropChance(slot, 0.0f);
                    }
                }

                elucent.eidolon.util.EntityUtil.enthrall(caster, living);
                level.addFreshEntity(living);
                MobChantCastingGoal.registerSummon(caster.getUUID(), living.getUUID());

                if (disableBossBar) {
                    disableBossBars(living, level);
                }
            }
        }

        private net.minecraft.world.phys.Vec3 resolveSummonBasePosition(net.minecraft.server.level.ServerPlayer player) {
            String spawnAt = data.has("spawn_at") ? data.get("spawn_at").getAsString() : "player";
            if ("look".equalsIgnoreCase(spawnAt) || "target".equalsIgnoreCase(spawnAt)) {
                double range = data.has("range") ? Math.max(1.0d, data.get("range").getAsDouble()) : 6.0d;
                net.minecraft.world.phys.Vec3 eyePos = player.getEyePosition();
                net.minecraft.world.phys.Vec3 endPos = eyePos.add(player.getLookAngle().scale(range));
                net.minecraft.world.phys.BlockHitResult hit = player.level().clip(
                    new net.minecraft.world.level.ClipContext(
                        eyePos,
                        endPos,
                        net.minecraft.world.level.ClipContext.Block.COLLIDER,
                        net.minecraft.world.level.ClipContext.Fluid.NONE,
                        player
                    )
                );

                if (hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
                    net.minecraft.world.phys.Vec3 hitPos = hit.getLocation();
                    return new net.minecraft.world.phys.Vec3(hitPos.x, hitPos.y + 0.1d, hitPos.z);
                }
            }

            return player.position().add(0.0d, 0.1d, 0.0d);
        }

        private net.minecraft.world.phys.Vec3 resolveSummonBasePosition(net.minecraft.world.entity.LivingEntity caster) {
            String spawnAt = data.has("spawn_at") ? data.get("spawn_at").getAsString() : "player";
            if (("look".equalsIgnoreCase(spawnAt) || "target".equalsIgnoreCase(spawnAt))
                && caster.level() instanceof net.minecraft.server.level.ServerLevel level) {
                double range = data.has("range") ? Math.max(1.0d, data.get("range").getAsDouble()) : 6.0d;
                net.minecraft.world.phys.Vec3 eyePos = caster.getEyePosition();
                net.minecraft.world.phys.Vec3 endPos = eyePos.add(caster.getLookAngle().scale(range));
                net.minecraft.world.phys.BlockHitResult hit = level.clip(
                    new net.minecraft.world.level.ClipContext(
                        eyePos,
                        endPos,
                        net.minecraft.world.level.ClipContext.Block.COLLIDER,
                        net.minecraft.world.level.ClipContext.Fluid.NONE,
                        caster
                    )
                );

                if (hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
                    net.minecraft.world.phys.Vec3 hitPos = hit.getLocation();
                    return new net.minecraft.world.phys.Vec3(hitPos.x, hitPos.y + 0.1d, hitPos.z);
                }
            }

            return caster.position().add(0.0d, 0.1d, 0.0d);
        }

        private void applySummonEquipment(net.minecraft.world.entity.LivingEntity living) {
            if (!(living instanceof net.minecraft.world.entity.Mob) || !data.has("equipment")) {
                return;
            }

            JsonObject equipment = data.getAsJsonObject("equipment");
            applySummonSlot(living, equipment, "head", net.minecraft.world.entity.EquipmentSlot.HEAD);
            applySummonSlot(living, equipment, "chest", net.minecraft.world.entity.EquipmentSlot.CHEST);
            applySummonSlot(living, equipment, "legs", net.minecraft.world.entity.EquipmentSlot.LEGS);
            applySummonSlot(living, equipment, "feet", net.minecraft.world.entity.EquipmentSlot.FEET);
            applySummonSlot(living, equipment, "mainhand", net.minecraft.world.entity.EquipmentSlot.MAINHAND);
            applySummonSlot(living, equipment, "offhand", net.minecraft.world.entity.EquipmentSlot.OFFHAND);
        }

        private void applySummonSlot(
                net.minecraft.world.entity.LivingEntity living,
                JsonObject equipment,
                String key,
                net.minecraft.world.entity.EquipmentSlot slot) {
            if (!equipment.has(key)) {
                return;
            }

            net.minecraft.world.item.ItemStack stack = parseSummonItemStack(equipment.get(key));
            if (stack.isEmpty()) {
                return;
            }

            living.setItemSlot(slot, stack);
            if (living instanceof net.minecraft.world.entity.Mob mob) {
                float dropChance = data.has("equipment_drop_chance")
                    ? (float) data.get("equipment_drop_chance").getAsDouble()
                    : 0.0f;
                mob.setDropChance(slot, Math.max(0.0f, Math.min(1.0f, dropChance)));
            }
        }

        private net.minecraft.world.item.ItemStack parseSummonItemStack(JsonElement element) {
            if (element == null || element.isJsonNull()) {
                return net.minecraft.world.item.ItemStack.EMPTY;
            }

            String itemId;
            int count = 1;

            if (element.isJsonPrimitive()) {
                itemId = element.getAsString();
            } else if (element.isJsonObject()) {
                JsonObject itemObject = element.getAsJsonObject();
                if (!itemObject.has("item")) {
                    return net.minecraft.world.item.ItemStack.EMPTY;
                }
                itemId = itemObject.get("item").getAsString();
                if (itemObject.has("count")) {
                    count = Math.max(1, itemObject.get("count").getAsInt());
                }
            } else {
                return net.minecraft.world.item.ItemStack.EMPTY;
            }

            ResourceLocation itemLocation = ResourceLocation.tryParse(itemId);
            if (itemLocation == null) {
                LOGGER.warn("summoning: invalid equipment item '{}'", itemId);
                return net.minecraft.world.item.ItemStack.EMPTY;
            }

            net.minecraft.world.item.Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(itemLocation);
            if (item == null || item == net.minecraft.world.item.Items.AIR) {
                LOGGER.warn("summoning: unknown equipment item '{}'", itemId);
                return net.minecraft.world.item.ItemStack.EMPTY;
            }

            return new net.minecraft.world.item.ItemStack(item, count);
        }

        private void applySummonChantRotation(net.minecraft.world.entity.LivingEntity living) {
            // If this mob type has a ChantableMobConfig with a rotation, skip injection here.
            // MobChantCastingGoal.onEntityJoin() will write the rotation tags on EntityJoinLevelEvent.
            if (living instanceof net.minecraft.world.entity.Mob mob) {
                ChantableMobManager.ChantableMobConfig cfg = ChantableMobManager.getConfigForMob(mob);
                if (cfg != null && !cfg.chantRotationIds().isEmpty()) {
                    return;
                }
            }

            JsonArray chants = null;
            if (data.has("chant_rotation_ids") && data.get("chant_rotation_ids").isJsonArray()) {
                chants = data.getAsJsonArray("chant_rotation_ids");
            } else if (data.has("chant_ids") && data.get("chant_ids").isJsonArray()) {
                chants = data.getAsJsonArray("chant_ids");
            }

            if (chants == null || chants.isEmpty()) {
                return;
            }

            net.minecraft.nbt.ListTag listTag = new net.minecraft.nbt.ListTag();
            for (JsonElement element : chants) {
                if (element == null || element.isJsonNull()) {
                    continue;
                }
                String chantId = element.getAsString();
                ResourceLocation parsed = ResourceLocation.tryParse(chantId);
                if (parsed != null) {
                    listTag.add(net.minecraft.nbt.StringTag.valueOf(parsed.toString()));
                }
            }

            if (listTag.isEmpty()) {
                return;
            }

            var tag = living.getPersistentData();
            tag.put("eu_chant_rotation_ids", listTag);
            tag.putInt("eu_chant_rotation_index", 0);
            if (data.has("chant_rotation_interval_ticks")) {
                tag.putInt("eu_chant_rotation_interval_ticks", Math.max(1, data.get("chant_rotation_interval_ticks").getAsInt()));
            }
            if (data.has("chant_rotation_min_range")) {
                tag.putDouble("eu_chant_rotation_min_range", Math.max(0.0d, data.get("chant_rotation_min_range").getAsDouble()));
            } else if (data.has("min_range")) {
                tag.putDouble("eu_chant_rotation_min_range", Math.max(0.0d, data.get("min_range").getAsDouble()));
            }
            if (data.has("chant_rotation_max_range")) {
                tag.putDouble("eu_chant_rotation_max_range", Math.max(0.5d, data.get("chant_rotation_max_range").getAsDouble()));
            } else if (data.has("max_range")) {
                tag.putDouble("eu_chant_rotation_max_range", Math.max(0.5d, data.get("max_range").getAsDouble()));
            }
            if (data.has("chant_rotation_require_los")) {
                tag.putBoolean("eu_chant_rotation_require_los", data.get("chant_rotation_require_los").getAsBoolean());
            }
        }

        private void inheritSummonFaith(
                net.minecraft.world.entity.LivingEntity summon,
                net.minecraft.world.entity.LivingEntity caster) {
            net.minecraft.nbt.CompoundTag casterData = caster.getPersistentData();
            net.minecraft.nbt.CompoundTag summonData = summon.getPersistentData();

            String deityTag = com.bluelotuscoding.eidolonunchained.ai.MobChantCastingGoal.FAITH_DEITY_TAG;
            String titleTag = com.bluelotuscoding.eidolonunchained.ai.MobChantCastingGoal.FAITH_TITLE_TAG;

            if (!summonData.contains(deityTag, net.minecraft.nbt.Tag.TAG_STRING)
                && casterData.contains(deityTag, net.minecraft.nbt.Tag.TAG_STRING)) {
                summonData.putString(deityTag, casterData.getString(deityTag));
            }

            if (!summonData.contains(titleTag, net.minecraft.nbt.Tag.TAG_STRING)
                && casterData.contains(titleTag, net.minecraft.nbt.Tag.TAG_STRING)) {
                summonData.putString(titleTag, casterData.getString(titleTag));
            }
        }

        private void disableBossBars(net.minecraft.world.entity.LivingEntity living, net.minecraft.server.level.ServerLevel level) {
            Class<?> type = living.getClass();
            while (type != null && type != Object.class) {
                java.lang.reflect.Field[] fields = type.getDeclaredFields();
                for (java.lang.reflect.Field field : fields) {
                    if (!net.minecraft.server.level.ServerBossEvent.class.isAssignableFrom(field.getType())) {
                        continue;
                    }

                    try {
                        field.setAccessible(true);
                        Object value = field.get(living);
                        if (value instanceof net.minecraft.server.level.ServerBossEvent bossEvent) {
                            bossEvent.setVisible(false);
                            for (net.minecraft.server.level.ServerPlayer viewer : level.players()) {
                                bossEvent.removePlayer(viewer);
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.debug("summoning: unable to disable boss bar on {} field {}: {}",
                            living.getType(), field.getName(), e.getMessage());
                    }
                }
                type = type.getSuperclass();
            }
        }

        private void attachProjectileParticleConfig(net.minecraft.world.entity.projectile.Projectile projectile) {
            String resolvedDamageType = resolveConfiguredDamageType();
            Integer hitColorRgb = resolveLinkedDeityHitColorRgb();
            if (data.has("damage") && resolvedDamageType != null) {
                double impactDamage = data.get("damage").getAsDouble();
                if (impactDamage > 0.0d) {
                    var tag = projectile.getPersistentData();
                    tag.putBoolean("eu_impact_damage_enabled", true);
                    tag.putDouble("eu_impact_damage", impactDamage);
                    tag.putString("eu_impact_damage_type", resolvedDamageType);
                    if (hitColorRgb != null) {
                        tag.putInt("eu_impact_hit_color", hitColorRgb);
                    }
                }
            }

            if (data.has("impact_particle")) {
                ResourceLocation particleId = ResourceLocation.tryParse(data.get("impact_particle").getAsString());
                if (particleId == null) {
                    LOGGER.warn("spawn_projectile: invalid impact_particle '{}'", data.get("impact_particle").getAsString());
                } else {
                    var tag = projectile.getPersistentData();
                    tag.putBoolean("eu_impact_particles_enabled", true);
                    tag.putString("eu_impact_particle", particleId.toString());
                    tag.putInt("eu_impact_particle_count", data.has("impact_particle_count") ? data.get("impact_particle_count").getAsInt() : 24);
                    tag.putDouble("eu_impact_particle_spread", data.has("impact_particle_spread") ? data.get("impact_particle_spread").getAsDouble() : 0.35d);
                    tag.putDouble("eu_impact_particle_speed", data.has("impact_particle_speed") ? data.get("impact_particle_speed").getAsDouble() : 0.02d);
                    tag.putString("eu_impact_particle_mode", data.has("impact_particle_mode") ? data.get("impact_particle_mode").getAsString() : "burst");
                }
            }

            if (!data.has("trail_particle")) {
                return;
            }

            ResourceLocation trailId = ResourceLocation.tryParse(data.get("trail_particle").getAsString());
            if (trailId == null) {
                LOGGER.warn("spawn_projectile: invalid trail_particle '{}'", data.get("trail_particle").getAsString());
                return;
            }

            var tag = projectile.getPersistentData();
            tag.putBoolean("eu_trail_particles_enabled", true);
            tag.putString("eu_trail_particle", trailId.toString());
            tag.putInt("eu_trail_particle_count", data.has("trail_particle_count") ? data.get("trail_particle_count").getAsInt() : 3);
            tag.putDouble("eu_trail_particle_spread", data.has("trail_particle_spread") ? data.get("trail_particle_spread").getAsDouble() : 0.05d);
            tag.putDouble("eu_trail_particle_speed", data.has("trail_particle_speed") ? data.get("trail_particle_speed").getAsDouble() : 0.0d);
            tag.putInt("eu_trail_particle_interval", data.has("trail_particle_interval") ? Math.max(1, data.get("trail_particle_interval").getAsInt()) : 1);

            if (data.has("on_hit") && data.get("on_hit").isJsonArray()) {
                // Store nested projectile on-hit effect array as raw JSON for impact-time execution.
                tag.putString("eu_projectile_on_hit_json", data.getAsJsonArray("on_hit").toString());
            }
        }

        private void consumeAltResource(net.minecraft.server.level.ServerPlayer player) {
            // Hook point for alternative resource systems (e.g. Iron's mana bridge, future XP cost, etc.)
            // Currently supports: "xp" resource type.
            // Iron's compat will be wired here in Phase 6.
            String resourceType = data.has("resource_type") ? data.get("resource_type").getAsString() : "";
            float amount = data.has("amount") ? (float) data.get("amount").getAsDouble() : 0f;
            switch (resourceType) {
                case "xp":
                    player.giveExperiencePoints(-(int) amount);
                    break;
                default:
                    LOGGER.debug("consume_alt_resource: unknown resource type '{}'", resourceType);
                    break;
            }
        }

        private void startConversation(net.minecraft.server.level.ServerPlayer player) {
            if (data.has("deity")) {
                String deityId = data.get("deity").getAsString();
                try {
                    net.minecraft.resources.ResourceLocation deityLocation = new net.minecraft.resources.ResourceLocation(deityId);
                    // Use the DeityChat system to start the conversation
                    com.bluelotuscoding.eidolonunchained.chat.DeityChat.startConversation(player, deityLocation);
                } catch (Exception e) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cFailed to start conversation with " + deityId + ": " + e.getMessage()));
                }
            } else {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cNo deity specified for conversation"));
            }
        }
        
        public static ChantEffect fromJson(JsonObject json) {
            String type = json.get("type").getAsString();
            return new ChantEffect(type, json);
        }
        
        // Effigy effects are now handled by EffigyEffectsManager during conversations
        
        
        /**
         * Get the parent chant that contains this effect (needed for linked deity info)
         */
        private DatapackChant getParentChant(net.minecraft.server.level.ServerPlayer player) {
            return this.parentChant;
        }
        
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", type);
            
            // Copy all data properties
            for (String key : data.keySet()) {
                if (!key.equals("type")) {
                    json.add(key, data.get(key));
                }
            }
            
            return json;
        }
    }
}





