package com.bluelotuscoding.eidolonunchained.data;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elucent.eidolon.api.research.Research;
import elucent.eidolon.api.research.ResearchTask;
import elucent.eidolon.registries.Researches;
// import elucent.eidolon.util.KnowledgeUtil;  // TODO: Find correct class name
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Datapack loader for Eidolon's actual research system.
 * Loads research definitions from JSON files and registers them with Eidolon's research registry.
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EidolonResearchDataManager extends SimpleJsonResourceReloadListener {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(EidolonResearchDataManager.class);
    private static final Gson GSON = com.bluelotuscoding.eidolonunchained.util.JsonUtils.GSON;
    
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
    @SuppressWarnings({"null", "all"})
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
                        JsonObject taskObj = taskElement.getAsJsonObject();

                        // Special handling: if this task uses an "items" array, expand into multiple
                        if (taskObj.has("items") && taskObj.get("items").isJsonArray()) {
                            JsonArray items = taskObj.getAsJsonArray("items");
                            for (JsonElement itemEl : items) {
                                if (!itemEl.isJsonObject()) continue;
                                JsonObject single = new JsonObject();
                                // Carry over type
                                single.addProperty("type", taskObj.has("type") ? taskObj.get("type").getAsString() : "item");
                                // Copy per-item fields
                                for (String k : new String[]{"item","count","nbt","command","commands"}) {
                                    if (itemEl.getAsJsonObject().has(k)) {
                                        single.add(k, itemEl.getAsJsonObject().get(k));
                                    }
                                }
                                // If per-item has no command(s), allow parent-level commands
                                if (!single.has("command") && !single.has("commands")) {
                                    if (taskObj.has("command")) single.add("command", taskObj.get("command"));
                                    if (taskObj.has("commands")) single.add("commands", taskObj.get("commands"));
                                }
                                ResearchTask t = parseTask(single);
                                if (t != null) taskList.add(t);
                            }
                        } else {
                            ResearchTask task = parseTask(taskObj);
                            if (task != null) {
                                taskList.add(task);
                            }
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
                // Support either a single item or an array of items for one TaskItems objective
                java.util.List<ItemStack> stacks = new java.util.ArrayList<>();

                if (taskJson.has("items") && taskJson.get("items").isJsonArray()) {
                    for (JsonElement el : taskJson.getAsJsonArray("items")) {
                        if (!el.isJsonObject()) continue;
                        JsonObject it = el.getAsJsonObject();
                        if (!it.has("item")) continue;
                        ResourceLocation iid = ResourceLocation.tryParse(it.get("item").getAsString());
                        Item itm = iid != null ? ForgeRegistries.ITEMS.getValue(iid) : null;
                        if (itm == null) {
                            LOGGER.warn("Unknown item in 'items' array: {}", it.get("item").getAsString());
                            continue;
                        }
                        int c = it.has("count") ? it.get("count").getAsInt() : 1;
                        ItemStack s = new ItemStack(itm, c);
                        if (it.has("nbt")) {
                            try { s.setTag(TagParser.parseTag(it.get("nbt").getAsString())); } catch (Exception ignored) {}
                        }
                        stacks.add(s);
                    }
                } else if (taskJson.has("item")) {
                    ResourceLocation itemId = ResourceLocation.tryParse(taskJson.get("item").getAsString());
                    Item item = itemId != null ? ForgeRegistries.ITEMS.getValue(itemId) : null;
                    if (item == null) {
                        LOGGER.warn("Unknown item: {}", taskJson.get("item").getAsString());
                        return null;
                    }
                    int count = taskJson.has("count") ? taskJson.get("count").getAsInt() : 1;
                    ItemStack stack = new ItemStack(item, count);
                    if (taskJson.has("nbt")) {
                        try { stack.setTag(TagParser.parseTag(taskJson.get("nbt").getAsString())); } catch (Exception ignored) {}
                    }
                    stacks.add(stack);
                } else {
                    LOGGER.warn("Item task missing 'item' or 'items' field");
                    return null;
                }

                ResearchTask base = new ResearchTask.TaskItems(stacks);
                return wrapTaskWithCommandsIfAny(base, taskJson);
            }
            
            case "xp", "experience" -> {
                int levels = taskJson.has("levels") ? taskJson.get("levels").getAsInt() : 1;
                ResearchTask base = new ResearchTask.XP(levels);
                return wrapTaskWithCommandsIfAny(base, taskJson);
            }
            
            default -> {
                LOGGER.warn("Unknown research task type: {}", type);
                return null;
            }
        }
    }

    private ResearchTask wrapTaskWithCommandsIfAny(ResearchTask base, JsonObject taskJson) {
        java.util.List<String> cmds = new java.util.ArrayList<>();
        try {
            if (taskJson.has("commands") && taskJson.get("commands").isJsonArray()) {
                for (JsonElement e : taskJson.getAsJsonArray("commands")) {
                    if (e.isJsonPrimitive()) cmds.add(e.getAsString());
                }
            }
            if (taskJson.has("command") && taskJson.get("command").isJsonPrimitive()) {
                cmds.add(taskJson.get("command").getAsString());
            }
        } catch (Exception ignored) {}

        return cmds.isEmpty() ? base : new CommandedResearchTask(base, cmds);
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
            try {
                // Support simple string commands like "command:give {player} ..."
                if (rewardElement.isJsonPrimitive()) {
                    String value = rewardElement.getAsString();
                    if (value.startsWith("command:")) {
                        String rawCmd = value.substring("command:".length()).trim();
                        executeCommandReward(player, rawCmd);
                    } else {
                        LOGGER.warn("Ignoring unsupported reward primitive: {}", value);
                    }
                    continue;
                }

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

                                // Optional NBT for reward items
                                if (reward.has("nbt")) {
                                    try {
                                        CompoundTag nbt = TagParser.parseTag(reward.get("nbt").getAsString());
                                        stack.setTag(nbt);
                                    } catch (Exception e) {
                                        LOGGER.warn("Failed to parse NBT for item reward: {}", e.getMessage());
                                    }
                                }

                                if (!player.getInventory().add(stack)) {
                                    player.drop(stack, false);
                                }
                            } else {
                                LOGGER.warn("Unknown item in reward: {}", reward.get("item").getAsString());
                            }
                        }
                    }

                    case "command" -> {
                        // Single command string under 'command'
                        if (reward.has("command")) {
                            String cmd = reward.get("command").getAsString();
                            executeCommandReward(player, cmd);
                        }
                        // Or an array under 'commands'
                        if (reward.has("commands") && reward.get("commands").isJsonArray()) {
                            for (JsonElement cmdEl : reward.getAsJsonArray("commands")) {
                                if (cmdEl.isJsonPrimitive()) {
                                    executeCommandReward(player, cmdEl.getAsString());
                                }
                            }
                        }
                    }

                    case "commands" -> {
                        if (reward.has("commands") && reward.get("commands").isJsonArray()) {
                            for (JsonElement cmdEl : reward.getAsJsonArray("commands")) {
                                if (cmdEl.isJsonPrimitive()) {
                                    executeCommandReward(player, cmdEl.getAsString());
                                }
                            }
                        }
                    }

                    default -> LOGGER.warn("Unknown reward type: {}", type);
                }
            } catch (Exception ex) {
                LOGGER.error("Error processing reward element: {}", ex.getMessage());
            }
        }
    }

    private void executeCommandReward(ServerPlayer player, String command) {
        if (player == null || player.getServer() == null) return;

        try {
            LOGGER.info("[Research] Executing command reward for {}: {}", player.getName().getString(), command);
            String processed = command
                .replace("{player}", player.getName().getString())
                .replace("{uuid}", player.getUUID().toString())
                .replace("{x}", String.valueOf((int) player.getX()))
                .replace("{y}", String.valueOf((int) player.getY()))
                .replace("{z}", String.valueOf((int) player.getZ()));
            // Allow canonical Minecraft selector usage by mapping @s to the player for console execution
            processed = processed.replace("@s", player.getName().getString());

            // Allow commands with or without leading '/'
            if (processed.startsWith("/")) {
                processed = processed.substring(1);
            }

            MinecraftServer server = player.getServer();
            if (server != null) {
                int result = server.getCommands().performPrefixedCommand(
                    server.createCommandSourceStack(),
                    processed
                );
                LOGGER.info("[Research] Command result={} for processed='{}'", result, processed);
            } else {
                LOGGER.warn("[Research] Skipped command execution; server was null for player {}", player.getName().getString());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to execute research command reward '{}': {}", command, e.getMessage());
        }
    }

    /**
     * Wrapper that delegates to an existing ResearchTask and executes commands when the task completes.
     */
    private class CommandedResearchTask extends ResearchTask {
        private final ResearchTask delegate;
        private final List<String> commands;

        private CommandedResearchTask(ResearchTask delegate, List<String> commands) {
            this.delegate = delegate;
            this.commands = commands != null ? commands : new ArrayList<>();
        }

        @Override
        public CompoundTag write() {
            return delegate.write();
        }

        @Override
        public void read(CompoundTag tag) {
            delegate.read(tag);
        }

        @Override
        public CompletenessResult isComplete(AbstractContainerMenu menu, Player player, int slotStart) {
            return delegate.isComplete(menu, player, slotStart);
        }

        @Override
        public void onComplete(AbstractContainerMenu menu, Player player, int slotStart) {
            // Preserve original completion behavior
            try {
                delegate.onComplete(menu, player, slotStart);
            } catch (Exception e) {
                LOGGER.warn("Delegate task onComplete threw: {}", e.getMessage());
            }

            if (player instanceof ServerPlayer sp) {
                LOGGER.info("[Research] Task '{}' completed. Executing {} command reward(s).", delegate.getClass().getSimpleName(), commands.size());
                for (String cmd : commands) {
                    executeCommandReward(sp, cmd);
                }
            }
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public int getWidth() {
            return delegate.getWidth();
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public void drawIcon(GuiGraphics stack, ResourceLocation texture, int x, int y) {
            delegate.drawIcon(stack, texture, x, y);
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public void drawTooltip(GuiGraphics stack, AbstractContainerScreen<?> gui, double mouseX, double mouseY) {
            delegate.drawTooltip(stack, gui, mouseX, mouseY);
        }
    }
    
    /**
     * Grant a mystical sign to the player through Eidolon's knowledge API.
     */
    private void grantSign(ServerPlayer player, String signName) {
        try {
            ResourceLocation signId = signName.contains(":")
                ? new ResourceLocation(signName)
                : new ResourceLocation("eidolon", signName.toLowerCase(java.util.Locale.ROOT));
            var sign = elucent.eidolon.registries.Signs.find(signId);
            if (sign == null) {
                LOGGER.warn("Unknown sign '{}' for player {}", signName, player.getName().getString());
                return;
            }

            elucent.eidolon.util.KnowledgeUtil.grantSign(player, sign);
            LOGGER.info("Granted sign {} to player {}", signId, player.getName().getString());
        } catch (Exception e) {
            LOGGER.error("Failed to grant sign {} to player {}: {}", signName, player.getName().getString(), e.getMessage());
        }
    }
}
