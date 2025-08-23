package com.bluelotuscoding.eidolonunchained.data;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elucent.eidolon.api.research.Research;
import elucent.eidolon.api.research.ResearchTask;
import elucent.eidolon.api.spells.Spell;
import elucent.eidolon.capability.IKnowledge;
import elucent.eidolon.registries.Researches;
import elucent.eidolon.registries.Spells;
import elucent.eidolon.util.KnowledgeUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Datapack loader for Eidolon's actual research system.
 * Loads research definitions from JSON files and registers them with Eidolon's research registry.
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EidolonResearchDataManager extends SimpleJsonResourceReloadListener {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(EidolonResearchDataManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private static EidolonResearchDataManager INSTANCE;
    
    public EidolonResearchDataManager() {
        super(GSON, "eidolon_research");
        INSTANCE = this;
    }
    
    public static EidolonResearchDataManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new EidolonResearchDataManager();
        }
        return INSTANCE;
    }
    
    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(getInstance());
        LOGGER.info("Registered Eidolon Research datapack reload listener");
    }
    
    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resourceLocationJsonElementMap, 
                         ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        
        LOGGER.info("Loading datapack Eidolon research definitions...");
        
        int loaded = 0;
        int errors = 0;
        
        for (Map.Entry<ResourceLocation, JsonElement> entry : resourceLocationJsonElementMap.entrySet()) {
            ResourceLocation location = entry.getKey();
            JsonElement element = entry.getValue();
            
            if (!element.isJsonObject()) {
                LOGGER.warn("Skipping non-object JSON at {}", location);
                continue;
            }
            
            try {
                loadResearch(location, element.getAsJsonObject());
                loaded++;
            } catch (Exception e) {
                LOGGER.error("Failed to load research from {}", location, e);
                errors++;
            }
        }
        
        LOGGER.info("Loaded {} Eidolon research definitions with {} errors", loaded, errors);
    }
    
    /**
     * Loads a single research definition from JSON and registers it with Eidolon.
     */
    private void loadResearch(ResourceLocation location, JsonObject json) {
        // Parse basic research info
        String id = json.has("id") ? json.get("id").getAsString() : location.getPath();
        ResourceLocation researchId = new ResourceLocation(location.getNamespace(), id);
        
        int stars = json.has("stars") ? json.get("stars").getAsInt() : 1;
        if (stars < 1 || stars > 10) {
            throw new IllegalArgumentException("Research stars must be between 1 and 10, got: " + stars);
        }
        
        // Create the research object
        Research research = new Research(researchId, stars) {
            @Override
            public void onLearned(ServerPlayer serverPlayer) {
                // Handle rewards when research is completed
                if (json.has("rewards")) {
                    processRewards(json.getAsJsonArray("rewards"), serverPlayer);
                }
            }
        };
        
        // Add special tasks for specific steps
        if (json.has("tasks")) {
            JsonObject tasks = json.getAsJsonObject("tasks");
            for (String stepStr : tasks.keySet()) {
                try {
                    int step = Integer.parseInt(stepStr);
                    JsonArray stepTasks = tasks.getAsJsonArray(stepStr);
                    List<ResearchTask> taskList = new ArrayList<>();
                    
                    for (JsonElement taskElement : stepTasks) {
                        ResearchTask task = parseTask(taskElement.getAsJsonObject());
                        if (task != null) {
                            taskList.add(task);
                        }
                    }
                    
                    if (!taskList.isEmpty()) {
                        research.addSpecialTasks(step, taskList.toArray(new ResearchTask[0]));
                    }
                } catch (NumberFormatException e) {
                    LOGGER.warn("Invalid step number '{}' in research {}", stepStr, researchId);
                }
            }
        }
        
        // Parse trigger sources (blocks/entities that trigger this research)
        List<Object> sources = new ArrayList<>();
        if (json.has("triggers")) {
            JsonArray triggers = json.getAsJsonArray("triggers");
            for (JsonElement triggerElement : triggers) {
                Object source = parseTrigger(triggerElement);
                if (source != null) {
                    sources.add(source);
                }
            }
        }
        
        // Register the research with Eidolon
        Researches.register(research, sources.toArray());
        
        LOGGER.info("Registered research: {} ({}★) with {} triggers", 
                   researchId, stars, sources.size());
    }
    
    /**
     * Parse a research task from JSON.
     */
    private ResearchTask parseTask(JsonObject taskJson) {
        String type = taskJson.has("type") ? taskJson.get("type").getAsString() : "item";
        
        switch (type.toLowerCase()) {
            case "item", "items" -> {
                if (!taskJson.has("item")) {
                    LOGGER.warn("Item task missing 'item' field");
                    return null;
                }
                
                ResourceLocation itemId = ResourceLocation.tryParse(taskJson.get("item").getAsString());
                Item item = itemId != null ? ForgeRegistries.ITEMS.getValue(itemId) : null;
                if (item == null) {
                    LOGGER.warn("Unknown item: {}", taskJson.get("item").getAsString());
                    return null;
                }
                
                int count = taskJson.has("count") ? taskJson.get("count").getAsInt() : 1;
                ItemStack stack = new ItemStack(item, count);
                
                if (taskJson.has("nbt")) {
                    try {
                        CompoundTag nbt = TagParser.parseTag(taskJson.get("nbt").getAsString());
                        stack.setTag(nbt);
                    } catch (Exception e) {
                        LOGGER.warn("Failed to parse NBT for item task: {}", e.getMessage());
                    }
                }
                
                return new ResearchTask.TaskItems(stack);
            }
            
            case "xp", "experience" -> {
                int levels = taskJson.has("levels") ? taskJson.get("levels").getAsInt() : 1;
                return new ResearchTask.XP(levels);
            }
            
            default -> {
                LOGGER.warn("Unknown research task type: {}", type);
                return null;
            }
        }
    }
    
    /**
     * Parse a trigger source (block or entity) from JSON.
     */
    private Object parseTrigger(JsonElement triggerElement) {
        if (triggerElement.isJsonPrimitive()) {
            String triggerStr = triggerElement.getAsString();
            
            // Try as entity type first
            ResourceLocation entityId = ResourceLocation.tryParse(triggerStr);
            if (entityId != null) {
                EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(entityId);
                if (entityType != null) {
                    return entityType;
                }
                
                // Try as block type
                Block block = ForgeRegistries.BLOCKS.getValue(entityId);
                if (block != null) {
                    return block;
                }
            }
            
            LOGGER.warn("Unknown trigger: {}", triggerStr);
            return null;
        } else if (triggerElement.isJsonObject()) {
            JsonObject triggerObj = triggerElement.getAsJsonObject();
            String type = triggerObj.has("type") ? triggerObj.get("type").getAsString() : "entity";
            String id = triggerObj.has("id") ? triggerObj.get("id").getAsString() : "";
            
            ResourceLocation resourceId = ResourceLocation.tryParse(id);
            if (resourceId == null) {
                LOGGER.warn("Invalid trigger id: {}", id);
                return null;
            }
            
            if ("entity".equals(type)) {
                EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(resourceId);
                if (entityType != null) {
                    return entityType;
                }
            } else if ("block".equals(type)) {
                Block block = ForgeRegistries.BLOCKS.getValue(resourceId);
                if (block != null) {
                    return block;
                }
            }
            
            LOGGER.warn("Unknown {} trigger: {}", type, id);
            return null;
        }
        
        return null;
    }
    
    /**
     * Process research completion rewards.
     */
    private void processRewards(JsonArray rewards, ServerPlayer player) {
        for (JsonElement rewardElement : rewards) {
            if (!rewardElement.isJsonObject()) continue;
            
            JsonObject reward = rewardElement.getAsJsonObject();
            String type = reward.has("type") ? reward.get("type").getAsString() : "";
            
            switch (type.toLowerCase()) {
                case "sign", "mystical_sign" -> {
                    if (reward.has("sign")) {
                        String signName = reward.get("sign").getAsString();
                        grantSign(player, signName);
                    }
                }
                
                case "item" -> {
                    if (reward.has("item")) {
                        ResourceLocation itemId = ResourceLocation.tryParse(reward.get("item").getAsString());
                        Item item = itemId != null ? ForgeRegistries.ITEMS.getValue(itemId) : null;
                        if (item != null) {
                            int count = reward.has("count") ? reward.get("count").getAsInt() : 1;
                            ItemStack stack = new ItemStack(item, count);
                            
                            if (!player.getInventory().add(stack)) {
                                player.drop(stack, false);
                            }
                        }
                    }
                }
                
                default -> LOGGER.warn("Unknown reward type: {}", type);
            }
        }
    }
    
    /**
     * Grant a mystical sign to the player using reflection.
     */
    private void grantSign(ServerPlayer player, String signName) {
        try {
            // Try to find the sign in Signs registry using reflection
            Class<?> signsClass = Class.forName("elucent.eidolon.registries.Signs");
            Object sign = signsClass.getField(signName.toUpperCase() + "_SIGN").get(null);
            
            // Grant the sign using KnowledgeUtil (correct method signature: Entity, Sign)
            Class<?> signClass = Class.forName("elucent.eidolon.api.research.Sign");
            Method grantSignMethod = KnowledgeUtil.class.getMethod("grantSign", Entity.class, signClass);
            grantSignMethod.invoke(null, player, sign);
            
            LOGGER.info("Granted sign {} to player {}", signName, player.getName().getString());
            
            // Send action bar notification for immediate feedback
            player.connection.send(new ClientboundSetActionBarTextPacket(
                Component.literal("§6✦ New mystical sign learned: " + signName.toUpperCase() + " ✦")
            ));
        } catch (Exception e) {
            LOGGER.error("Failed to grant sign {} to player {}: {}", signName, player.getName().getString(), e.getMessage());
            e.printStackTrace(); // This will help debug the specific issue
        }
    }
}
}
