package com.bluelotuscoding.eidolonunchained.research.triggers.data;

import com.google.gson.annotations.SerializedName;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * Represents a research trigger definition from JSON
 */
public class ResearchTrigger {
    @SerializedName("type")
    private String type;
    
    // Entity kill triggers
    @SerializedName("entity")
    private String entity;
    
    // Block interaction triggers
    @SerializedName("block")
    private String block;
    
    // Ritual triggers
    @SerializedName("ritual")
    private String ritual;
    
    // Location triggers
    @SerializedName("dimension")
    private String dimension;
    
    @SerializedName("biome")
    private String biome;
    
    @SerializedName("structure")
    private String structure;
    
    @SerializedName("proximity_range")
    private double proximityRange = 1.0;
    
    // Coordinate-based triggers (optional)
    @SerializedName("coordinates")
    private Coordinates coordinates;
    
    // Common fields
    @SerializedName("nbt")
    private CompoundTag nbt;
    
    @SerializedName("item_requirements")
    private ItemRequirements itemRequirements;
    
    public static class Coordinates {
        @SerializedName("x")
        private Integer x;
        
        @SerializedName("z")
        private Integer z;
        
        @SerializedName("y")
        private Integer y;
        
        @SerializedName("range")
        private Double range = 50.0; // Default range for coordinate-based triggers
        
        public Integer getX() { return x; }
        public Integer getZ() { return z; }
        public Integer getY() { return y; }
        public Double getRange() { return range; }
        
        public void setX(Integer x) { this.x = x; }
        public void setZ(Integer z) { this.z = z; }
        public void setY(Integer y) { this.y = y; }
        public void setRange(Double range) { this.range = range; }
    }
    
    // Getters
    public String getType() {
        return type;
    }
    
    public ResourceLocation getEntity() {
        return entity != null ? new ResourceLocation(entity) : null;
    }
    
    public ResourceLocation getBlock() {
        return block != null ? new ResourceLocation(block) : null;
    }
    
    public ResourceLocation getRitual() {
        return ritual != null ? new ResourceLocation(ritual) : null;
    }
    
    public ResourceLocation getDimension() {
        return dimension != null ? new ResourceLocation(dimension) : null;
    }
    
    public ResourceLocation getBiome() {
        return biome != null ? new ResourceLocation(biome) : null;
    }
    
    public ResourceLocation getStructure() {
        return structure != null ? new ResourceLocation(structure) : null;
    }
    
    public double getProximityRange() {
        return proximityRange;
    }
    
    public Coordinates getCoordinates() {
        return coordinates;
    }
    
    public CompoundTag getNbt() {
        return nbt != null ? nbt : new CompoundTag();
    }
    
    public ItemRequirements getItemRequirements() {
        return itemRequirements;
    }
    
    // Setters
    public void setType(String type) {
        this.type = type;
    }
    
    public void setEntity(String entity) {
        this.entity = entity;
    }
    
    public void setBlock(String block) {
        this.block = block;
    }
    
    public void setRitual(String ritual) {
        this.ritual = ritual;
    }
    
    public void setDimension(String dimension) {
        this.dimension = dimension;
    }
    
    public void setBiome(String biome) {
        this.biome = biome;
    }
    
    public void setStructure(String structure) {
        this.structure = structure;
    }
    
    public void setProximityRange(double proximityRange) {
        this.proximityRange = proximityRange;
    }
    
    public void setCoordinates(Coordinates coordinates) {
        this.coordinates = coordinates;
    }
    
    public void setNbt(CompoundTag nbt) {
        this.nbt = nbt;
    }
    
    public void setItemRequirements(ItemRequirements itemRequirements) {
        this.itemRequirements = itemRequirements;
    }
}
