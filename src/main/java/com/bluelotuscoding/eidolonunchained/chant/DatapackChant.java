package com.bluelotuscoding.eidolonunchained.chant;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.ArrayList;

/**
 * Represents a custom chant that can be defined in datapacks
 * Chants are defined in data/modid/chants/ folder
 */
public class DatapackChant {
    
    private final ResourceLocation id;
    private final String name;
    private final String description;
    private final List<ResourceLocation> signSequence;
    private final String category;
    private final ResourceLocation codexIcon; // Icon to use in codex
    private final int difficulty;
    private final int manaCost; // Soul/magic cost to cast this chant
    private final List<ChantEffect> effects;
    private final List<String> requirements;
    private final boolean showInCodex;
    private final ResourceLocation linkedDeity; // Optional deity connection
    
    public DatapackChant(ResourceLocation id, String name, String description, 
                        List<ResourceLocation> signSequence, String category, ResourceLocation codexIcon,
                        int difficulty, int manaCost, List<ChantEffect> effects,
                        List<String> requirements, boolean showInCodex, 
                        ResourceLocation linkedDeity) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.signSequence = new ArrayList<>(signSequence);
        this.category = category;
        this.codexIcon = codexIcon;
        this.difficulty = difficulty;
        this.manaCost = Math.max(0, manaCost); // Ensure non-negative
        this.effects = new ArrayList<>(effects);
        this.requirements = new ArrayList<>(requirements);
        this.showInCodex = showInCodex;
        this.linkedDeity = linkedDeity;
    }
    
    public ResourceLocation getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<ResourceLocation> getSignSequence() { return new ArrayList<>(signSequence); }
    public String getCategory() { return category; }
    public ResourceLocation getCodexIcon() { return codexIcon; }
    public int getDifficulty() { return difficulty; }
    public int getManaCost() { return manaCost; }
    public List<ChantEffect> getEffects() { return new ArrayList<>(effects); }
    public List<String> getRequirements() { return new ArrayList<>(requirements); }
    public boolean shouldShowInCodex() { return showInCodex; }
    public ResourceLocation getLinkedDeity() { return linkedDeity; }
    public boolean hasLinkedDeity() { return linkedDeity != null; }
    
    /**
     * Check if player meets requirements to perform this chant
     */
    public boolean canPerform(net.minecraft.server.level.ServerPlayer player) {
        for (String requirement : requirements) {
            if (!checkRequirement(player, requirement)) {
                return false;
            }
        }
        return true;
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
     */
    public void execute(net.minecraft.server.level.ServerPlayer player) {
        for (ChantEffect effect : effects) {
            effect.apply(player);
        }
    }
    
    /**
     * Create a chant from JSON data
     */
    public static DatapackChant fromJson(ResourceLocation id, JsonObject json) {
        String name = json.get("name").getAsString();
        String description = json.has("description") ? json.get("description").getAsString() : "";
        String category = json.has("category") ? json.get("category").getAsString() : "custom";
        
        // Parse codex icon
        ResourceLocation codexIcon = null;
        if (json.has("codex_icon")) {
            codexIcon = new ResourceLocation(json.get("codex_icon").getAsString());
        }
        
        int difficulty = json.has("difficulty") ? json.get("difficulty").getAsInt() : 1;
        int manaCost = json.has("mana_cost") ? json.get("mana_cost").getAsInt() : 
                      com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig.COMMON.defaultManaCost.get();
        boolean showInCodex = json.has("show_in_codex") ? json.get("show_in_codex").getAsBoolean() : true;
        
        // Parse optional linked deity
        ResourceLocation linkedDeity = null;
        if (json.has("linked_deity")) {
            linkedDeity = new ResourceLocation(json.get("linked_deity").getAsString());
        }
        
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
                               difficulty, manaCost, effects, requirements, showInCodex, linkedDeity);
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
        json.addProperty("show_in_codex", showInCodex);
        
        // Add optional linked deity
        if (linkedDeity != null) {
            json.addProperty("linked_deity", linkedDeity.toString());
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
        
        public ChantEffect(String type, JsonObject data) {
            this.type = type;
            this.data = data;
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
                default:
                    // Unknown effect type
                    break;
            }
        }
        
        private void applyGiveItem(net.minecraft.server.level.ServerPlayer player) {
            // TODO: Implement item giving
            String item = data.get("item").getAsString();
            int count = data.has("count") ? data.get("count").getAsInt() : 1;
            player.sendSystemMessage(Component.literal("§aWould give " + count + "x " + item));
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
        
        private void sendMessage(net.minecraft.server.level.ServerPlayer player) {
            String message = data.get("message").getAsString();
            player.sendSystemMessage(Component.literal(message));
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
