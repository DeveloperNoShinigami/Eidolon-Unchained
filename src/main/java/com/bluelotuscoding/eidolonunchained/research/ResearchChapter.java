package com.bluelotuscoding.eidolonunchained.research;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Represents a research chapter that can be added to Eidolon's research system.
 * Chapters organize research entries into thematic groups.
 */
public class ResearchChapter {
    private final ResourceLocation id;
    private final Component title;
    private final Component description;
    private final ItemStack icon;
    private final int sortOrder;
    private final boolean isSecret;
    private final ResourceLocation backgroundTexture;
    private final String category; // Eidolon category this chapter belongs to
    private final JsonObject additionalData;

    public ResearchChapter(ResourceLocation id, Component title, Component description,
                          ItemStack icon, int sortOrder, boolean isSecret,
                          ResourceLocation backgroundTexture, String category, JsonObject additionalData) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.icon = icon;
        this.sortOrder = sortOrder;
        this.isSecret = isSecret;
        this.backgroundTexture = backgroundTexture;
        this.category = category;
        this.additionalData = additionalData != null ? additionalData : new JsonObject();
    }

    // Getters
    public ResourceLocation getId() { return id; }
    public Component getTitle() { return title; }
    public Component getDescription() { return description; }
    public ItemStack getIcon() { return icon; }
    public int getSortOrder() { return sortOrder; }
    public boolean isSecret() { return isSecret; }
    public ResourceLocation getBackgroundTexture() { return backgroundTexture; }
    public String getCategory() { return category; }
    public JsonObject getAdditionalData() { return additionalData; }

    /**
     * Converts this research chapter to JSON format for datapack generation
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        
        json.addProperty("id", id.toString());
        json.addProperty("title", title.getString());
        json.addProperty("description", description.getString());
        json.addProperty("sort_order", sortOrder);
        json.addProperty("secret", isSecret);

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

        // Background texture
        if (backgroundTexture != null) {
            json.addProperty("background", backgroundTexture.toString());
        }

        // Merge additional data
        additionalData.entrySet().forEach(entry -> 
            json.add(entry.getKey(), entry.getValue())
        );

        return json;
    }

    /**
     * Create ResearchChapter from JSON data
     */
    public static ResearchChapter fromJson(JsonObject json) {
        try {
            ResourceLocation id = new ResourceLocation(json.get("id").getAsString());
            Component title = Component.literal(json.get("title").getAsString());
            Component description = Component.literal(json.get("description").getAsString());
            int sortOrder = json.has("sort_order") ? json.get("sort_order").getAsInt() : 0;
            boolean isSecret = json.has("secret") ? json.get("secret").getAsBoolean() : false;
            
            // Parse icon
            ItemStack icon = net.minecraft.world.item.Items.BOOK.getDefaultInstance();
            if (json.has("icon")) {
                JsonObject iconData = json.getAsJsonObject("icon");
                try {
                    net.minecraft.world.item.Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
                        new ResourceLocation(iconData.get("item").getAsString()));
                    if (item != null) {
                        int count = iconData.has("count") ? iconData.get("count").getAsInt() : 1;
                        icon = new ItemStack(item, count);
                        // TODO: Handle NBT if needed
                    }
                } catch (Exception e) {
                    // Use default if icon parsing fails
                }
            }
            
            // Parse background texture
            ResourceLocation backgroundTexture = null;
            if (json.has("background")) {
                backgroundTexture = new ResourceLocation(json.get("background").getAsString());
            }
            
            // Parse category
            String category = json.has("category") ? json.get("category").getAsString() : "unknown";
            
            // Parse additional data
            JsonObject additionalData = new JsonObject();
            json.entrySet().forEach(entry -> {
                String key = entry.getKey();
                if (!key.equals("id") && !key.equals("title") && !key.equals("description") && 
                    !key.equals("sort_order") && !key.equals("secret") && !key.equals("icon") && 
                    !key.equals("background") && !key.equals("category")) {
                    additionalData.add(key, entry.getValue());
                }
            });
            
            return new ResearchChapter(id, title, description, icon, sortOrder, isSecret, 
                                     backgroundTexture, category, additionalData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse ResearchChapter from JSON", e);
        }
    }

    /**
     * Builder pattern for easier chapter creation
     */
    public static class Builder {
    private ResourceLocation id;
    private Component title;
    private Component description;
    private ItemStack icon;
    private int sortOrder = 0;
    private boolean isSecret = false;
    private ResourceLocation backgroundTexture;
    private String category = "nature";
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

        public Builder icon(ItemStack icon) {
            this.icon = icon;
            return this;
        }

        public Builder sortOrder(int sortOrder) {
            this.sortOrder = sortOrder;
            return this;
        }

        public Builder secret(boolean isSecret) {
            this.isSecret = isSecret;
            return this;
        }

        public Builder background(ResourceLocation backgroundTexture) {
            this.backgroundTexture = backgroundTexture;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder additionalData(String key, String value) {
            this.additionalData.addProperty(key, value);
            return this;
        }

        public ResearchChapter build() {
            return new ResearchChapter(id, title, description, icon, sortOrder, 
                                     isSecret, backgroundTexture, category, additionalData);
        }
    }
}
