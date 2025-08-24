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
    private final int difficulty;
    private final List<ChantEffect> effects;
    private final List<String> requirements;
    private final boolean showInCodex;
    
    public DatapackChant(ResourceLocation id, String name, String description, 
                        List<ResourceLocation> signSequence, String category,
                        int difficulty, List<ChantEffect> effects,
                        List<String> requirements, boolean showInCodex) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.signSequence = new ArrayList<>(signSequence);
        this.category = category;
        this.difficulty = difficulty;
        this.effects = new ArrayList<>(effects);
        this.requirements = new ArrayList<>(requirements);
        this.showInCodex = showInCodex;
    }
    
    public ResourceLocation getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<ResourceLocation> getSignSequence() { return new ArrayList<>(signSequence); }
    public String getCategory() { return category; }
    public int getDifficulty() { return difficulty; }
    public List<ChantEffect> getEffects() { return new ArrayList<>(effects); }
    public List<String> getRequirements() { return new ArrayList<>(requirements); }
    public boolean shouldShowInCodex() { return showInCodex; }
    
    /**
     * Check if player meets requirements to perform this chant
     */
    public boolean canPerform(net.minecraft.server.level.ServerPlayer player) {
        // TODO: Implement requirement checking (reputation, items, etc.)
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
        int difficulty = json.has("difficulty") ? json.get("difficulty").getAsInt() : 1;
        boolean showInCodex = json.has("show_in_codex") ? json.get("show_in_codex").getAsBoolean() : true;
        
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
        
        return new DatapackChant(id, name, description, signSequence, category, 
                               difficulty, effects, requirements, showInCodex);
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
            String deity = data.get("deity").getAsString();
            player.sendSystemMessage(Component.literal("§aStarting conversation with " + deity));
            // TODO: Start deity conversation
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
