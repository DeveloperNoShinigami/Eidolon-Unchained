package com.bluelotuscoding.eidolonunchained.codex;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.ArrayList;

/**
 * Represents a custom codex entry that can be added to existing Eidolon chapters.
 * This allows for easy extension of the existing codex system via datapacks.
 */
public class CodexEntry {
    private final ResourceLocation id;
    private final Component title;
    private final Component description;
    private final ResourceLocation targetChapter; // Which existing chapter to add this to
    private final ItemStack icon;
    private final List<ResourceLocation> prerequisites;
    private final List<JsonObject> pages;
    private final EntryType type;
    private final JsonObject additionalData;

    public enum EntryType {
        TEXT("text"),
        RECIPE("recipe"),
        RITUAL("ritual"),
        ENTITY("entity"),
        CRAFTING("crafting"),
        SMELTING("smelting"),
        CRUCIBLE("crucible"),
        WORKBENCH("workbench"),
        LIST("list");

        private final String name;

        EntryType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public CodexEntry(ResourceLocation id, Component title, Component description,
                     ResourceLocation targetChapter, ItemStack icon, List<ResourceLocation> prerequisites,
                     List<JsonObject> pages, EntryType type, JsonObject additionalData) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.targetChapter = targetChapter;
        this.icon = icon;
        this.prerequisites = prerequisites != null ? prerequisites : new ArrayList<>();
        this.pages = pages != null ? pages : new ArrayList<>();
        this.type = type;
        this.additionalData = additionalData != null ? additionalData : new JsonObject();
    }
    
    /**
     * Simple constructor for datapack entries
     */
    public static CodexEntry fromDatapack(ResourceLocation id, String title, JsonArray pagesArray) {
        List<JsonObject> pages = new ArrayList<>();
        for (int i = 0; i < pagesArray.size(); i++) {
            pages.add(pagesArray.get(i).getAsJsonObject());
        }
        
        return new CodexEntry(
            id,
            Component.literal(title),
            Component.literal(""), // No description for simple entries
            null, // Target chapter handled separately
            ItemStack.EMPTY, // No icon
            new ArrayList<>(), // No prerequisites
            pages,
            EntryType.TEXT, // Default type
            new JsonObject() // No additional data
        );
    }

    // Getters
    public ResourceLocation getId() { return id; }
    public Component getTitle() { return title; }
    public Component getDescription() { return description; }
    public ResourceLocation getTargetChapter() { return targetChapter; }
    public ItemStack getIcon() { return icon; }
    public List<ResourceLocation> getPrerequisites() { return prerequisites; }
    public List<JsonObject> getPages() { return pages; }
    public EntryType getType() { return type; }
    public JsonObject getAdditionalData() { return additionalData; }

    /**
     * Converts this codex entry to JSON format for datapack generation
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        
        json.addProperty("id", id.toString());
        json.addProperty("title", title.getString());
        json.addProperty("description", description.getString());
        json.addProperty("target_chapter", targetChapter.toString());
        json.addProperty("type", type.getName());

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

        // Pages
        if (!pages.isEmpty()) {
            JsonArray pagesArray = new JsonArray();
            pages.forEach(pagesArray::add);
            json.add("pages", pagesArray);
        }

        // Merge additional data
        additionalData.entrySet().forEach(entry -> 
            json.add(entry.getKey(), entry.getValue())
        );

        return json;
    }

    /**
     * Builder pattern for easier codex entry creation
     */
    public static class Builder {
        private ResourceLocation id;
        private Component title;
        private Component description;
        private ResourceLocation targetChapter;
        private ItemStack icon;
        private List<ResourceLocation> prerequisites = new ArrayList<>();
        private List<JsonObject> pages = new ArrayList<>();
        private EntryType type = EntryType.TEXT;
        private JsonObject additionalData = new JsonObject();

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

        public Builder targetChapter(ResourceLocation targetChapter) {
            this.targetChapter = targetChapter;
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

        public Builder page(JsonObject page) {
            this.pages.add(page);
            return this;
        }

        public Builder type(EntryType type) {
            this.type = type;
            return this;
        }

        public Builder additionalData(String key, String value) {
            this.additionalData.addProperty(key, value);
            return this;
        }

        public CodexEntry build() {
            return new CodexEntry(id, title, description, targetChapter, icon, 
                                prerequisites, pages, type, additionalData);
        }
    }
}
