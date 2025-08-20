package com.bluelotuscoding.eidolonunchained.research;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.bluelotuscoding.eidolonunchained.research.tasks.ResearchTask;
import com.bluelotuscoding.eidolonunchained.research.tasks.ResearchTaskTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import com.bluelotuscoding.eidolonunchained.research.conditions.DimensionCondition;
import com.bluelotuscoding.eidolonunchained.research.conditions.InventoryCondition;
import com.bluelotuscoding.eidolonunchained.research.conditions.ResearchCondition;
import com.bluelotuscoding.eidolonunchained.research.conditions.TimeCondition;
import com.bluelotuscoding.eidolonunchained.research.conditions.WeatherCondition;

/**
 * Represents a custom research entry that can be added to Eidolon's research system
 * through datapacks. This allows for easy extension of the research tree.
 */
public class ResearchEntry {
    private final ResourceLocation id;
    private final Component title;
    private final Component description;
    private final ResourceLocation chapter;
    private final ItemStack icon;
    private final List<ResourceLocation> prerequisites;
    private final List<ResourceLocation> unlocks;
    private final List<ResearchCondition> conditions;
    private final int x;
    private final int y;
    private final ResearchType type;
    private final int requiredStars;
    private final JsonObject additionalData;
    private final java.util.Map<Integer, java.util.List<ResearchTask>> tasks;
    private final List<ResearchCondition> conditions;

    public enum ResearchType {
        BASIC("basic"),
        ADVANCED("advanced"),
        FORBIDDEN("forbidden"),
        RITUAL("ritual"),
        CRAFTING("crafting");

        private final String name;

        ResearchType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public ResearchEntry(ResourceLocation id, Component title, Component description,
                        ResourceLocation chapter, ItemStack icon, List<ResourceLocation> prerequisites,
                        List<ResourceLocation> unlocks, int x, int y, ResearchType type,
                        int requiredStars, JsonObject additionalData,
                        java.util.Map<Integer, java.util.List<ResearchTask>> tasks,
                        List<ResearchCondition> conditions) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.chapter = chapter;
        this.icon = icon;
        this.prerequisites = prerequisites != null ? prerequisites : new ArrayList<>();
        this.unlocks = unlocks != null ? unlocks : new ArrayList<>();
        this.conditions = conditions != null ? conditions : new ArrayList<>();
        this.x = x;
        this.y = y;
        this.type = type;
        this.requiredStars = requiredStars;
        this.additionalData = additionalData != null ? additionalData : new JsonObject();
        this.tasks = tasks != null ? tasks : new java.util.HashMap<>();
        this.conditions = conditions != null ? conditions : new ArrayList<>();
    }

    // Getters
    public ResourceLocation getId() { return id; }
    public Component getTitle() { return title; }
    public Component getDescription() { return description; }
    public ResourceLocation getChapter() { return chapter; }
    public ItemStack getIcon() { return icon; }
    public List<ResourceLocation> getPrerequisites() { return prerequisites; }
    public List<ResourceLocation> getUnlocks() { return unlocks; }
    public int getX() { return x; }
    public int getY() { return y; }
    public ResearchType getType() { return type; }
    public int getRequiredStars() { return requiredStars; }
    public JsonObject getAdditionalData() { return additionalData; }
    public List<ResearchCondition> getConditions() {
        return Collections.unmodifiableList(new ArrayList<>(conditions));
    }
    public java.util.Map<Integer, java.util.List<ResearchTask>> getTasks() { return tasks; }
    public List<ResearchCondition> getConditions() { return conditions; }

    /**
     * Converts this research entry to a JSON format for datapack generation
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        
        json.addProperty("id", id.toString());
        json.addProperty("title", title.getString());
        json.addProperty("description", description.getString());
        json.addProperty("chapter", chapter.toString());
        json.addProperty("type", type.getName());
        if (requiredStars >= 0) {
            json.addProperty("required_stars", requiredStars);
        }
        json.addProperty("x", x);
        json.addProperty("y", y);

        // Icon data
        JsonObject iconData = new JsonObject();
        iconData.addProperty("item", icon.getItem().toString());
        if (icon.getCount() > 1) {
            iconData.addProperty("count", icon.getCount());
        }
        if (icon.hasTag()) {
            iconData.addProperty("nbt", icon.getTag().toString());
        }
        json.add("icon", iconData);

        // Prerequisites
        if (!prerequisites.isEmpty()) {
            JsonArray prereqArray = new JsonArray();
            prerequisites.forEach(prereq -> prereqArray.add(prereq.toString()));
            json.add("prerequisites", prereqArray);
        }

        // Unlocks
        if (!unlocks.isEmpty()) {
            JsonArray unlocksArray = new JsonArray();
            unlocks.forEach(unlock -> unlocksArray.add(unlock.toString()));
            json.add("unlocks", unlocksArray);
        }

        // Conditional requirements
        if (!conditions.isEmpty()) {
            JsonObject cond = new JsonObject();
            for (ResearchCondition c : conditions) {
                if (c instanceof DimensionCondition dc) {
                    cond.addProperty("dimension", dc.getDimension().toString());
                } else if (c instanceof TimeCondition tc) {
                    JsonObject time = new JsonObject();
                    time.addProperty("min", tc.getMin());
                    time.addProperty("max", tc.getMax());
                    cond.add("time_range", time);
                } else if (c instanceof WeatherCondition wc) {
                    cond.addProperty("weather", wc.getWeather().name().toLowerCase());
                } else if (c instanceof InventoryCondition ic) {
                    JsonArray arr = cond.has("inventory") ? cond.getAsJsonArray("inventory") : new JsonArray();
                    JsonObject itemObj = new JsonObject();
                    itemObj.addProperty("item", ic.getItem().toString());
                    itemObj.addProperty("count", ic.getCount());
                    arr.add(itemObj);
                    cond.add("inventory", arr);
                }
            }
            json.add("conditional_requirements", cond);
        }

        // Merge additional data
        additionalData.entrySet().forEach(entry ->
            json.add(entry.getKey(), entry.getValue())
        );

        // Tasks
        if (!tasks.isEmpty()) {
            JsonObject tasksObj = new JsonObject();
            for (var entry : tasks.entrySet()) {
                JsonArray array = new JsonArray();
                for (ResearchTask task : entry.getValue()) {
                    JsonObject tObj = new JsonObject();
                    tObj.addProperty("type", task.getType().getId());
                    switch (task.getType()) {
                        case KILL_ENTITIES -> {
                            var t = (com.bluelotuscoding.eidolonunchained.research.tasks.KillEntitiesTask) task;
                            tObj.addProperty("entity", t.getEntity().toString());
                            tObj.addProperty("count", t.getCount());
                        }
                        case CRAFT_ITEMS -> {
                            var t = (com.bluelotuscoding.eidolonunchained.research.tasks.CraftItemsTask) task;
                            tObj.addProperty("item", t.getItem().toString());
                            tObj.addProperty("count", t.getCount());
                        }
                        case USE_RITUAL -> {
                            var t = (com.bluelotuscoding.eidolonunchained.research.tasks.UseRitualTask) task;
                            tObj.addProperty("ritual", t.getRitual().toString());
                            tObj.addProperty("count", t.getCount());
                        }
                        case COLLECT_ITEMS -> {
                            var t = (com.bluelotuscoding.eidolonunchained.research.tasks.CollectItemsTask) task;
                            tObj.addProperty("item", t.getItem().toString());
                            tObj.addProperty("count", t.getCount());
                        }
                        case ENTER_DIMENSION -> {
                            var t = (com.bluelotuscoding.eidolonunchained.research.tasks.EnterDimensionTask) task;
                            tObj.addProperty("dimension", t.getDimension().toString());
                        }
                        case TIME_WINDOW -> {
                            var t = (com.bluelotuscoding.eidolonunchained.research.tasks.TimeWindowTask) task;
                            tObj.addProperty("min", t.getMin());
                            tObj.addProperty("max", t.getMax());
                        }
                        case WEATHER -> {
                            var t = (com.bluelotuscoding.eidolonunchained.research.tasks.WeatherTask) task;
                            tObj.addProperty("weather", t.getWeather().name().toLowerCase());
                        }
                        case INVENTORY -> {
                            var t = (com.bluelotuscoding.eidolonunchained.research.tasks.InventoryTask) task;
                            tObj.addProperty("item", t.getItem().toString());
                            tObj.addProperty("count", t.getCount());
                        }
                        case HAS_ITEM_NBT -> {
                            var t = (com.bluelotuscoding.eidolonunchained.research.tasks.HasItemWithNbtTask) task;
                            tObj.addProperty("item", t.getItem().toString());
                            tObj.addProperty("count", t.getCount());
                            if (t.getNbt() != null && !t.getNbt().isEmpty()) {
                                tObj.addProperty("nbt", t.getNbt().toString());
                            }
                        }
                    }
                    array.add(tObj);
                }
                tasksObj.add("tier_" + entry.getKey(), array);
            }
            json.add("tasks", tasksObj);
        }

        return json;
    }

    /**
     * Builder pattern for easier research entry creation
     */
    public static class Builder {
        private ResourceLocation id;
        private Component title;
        private Component description;
        private ResourceLocation chapter;
        private ItemStack icon;
        private List<ResourceLocation> prerequisites = new ArrayList<>();
        private List<ResourceLocation> unlocks = new ArrayList<>();
        private List<ResearchCondition> conditions = new ArrayList<>();
        private int x = 0;
        private int y = 0;
        private ResearchType type = ResearchType.BASIC;
        private int requiredStars = -1;
        private JsonObject additionalData = new JsonObject();
        private java.util.Map<Integer, java.util.List<ResearchTask>> tasks = new java.util.HashMap<>();
        private List<ResearchCondition> conditions = new ArrayList<>();

        public Builder(ResourceLocation id) {
            this.id = id;
        }

        public Builder title(Component title) {
            this.title = title;
            return this;
        }

        public Builder description(Component description) {
            this.description = description;
            return this;
        }

        public Builder chapter(ResourceLocation chapter) {
            this.chapter = chapter;
            return this;
        }

        public Builder icon(ItemStack icon) {
            this.icon = icon;
            return this;
        }

        public Builder prerequisite(ResourceLocation prereq) {
            this.prerequisites.add(prereq);
            return this;
        }

        public Builder unlock(ResourceLocation unlock) {
            this.unlocks.add(unlock);
            return this;
        }

        public Builder condition(ResearchCondition condition) {
            this.conditions.add(condition);
            return this;
        }

        public Builder position(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public Builder type(ResearchType type) {
            this.type = type;
            return this;
        }

        public Builder requiredStars(int stars) {
            this.requiredStars = stars;
            return this;
        }

        public Builder additionalData(String key, String value) {
            this.additionalData.addProperty(key, value);
            return this;
        }

        public Builder task(int tier, ResearchTask task) {
            this.tasks.computeIfAbsent(tier, k -> new java.util.ArrayList<>()).add(task);
            return this;
        }

        public Builder condition(ResearchCondition condition) {
            this.conditions.add(condition);
            return this;
        }

        public Builder condition(ResearchCondition condition) {
            this.conditions.add(condition);
            return this;
        }

        public Builder condition(ResearchCondition condition) {
            this.conditions.add(condition);
            return this;
        }

        public ResearchEntry build() {
            return new ResearchEntry(id, title, description, chapter, icon,
                                   prerequisites, unlocks, x, y, type, requiredStars, additionalData, tasks, conditions);

        }
    }
}
