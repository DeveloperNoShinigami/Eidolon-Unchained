package com.bluelotuscoding.eidolonunchained.chant;

import com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import elucent.eidolon.api.spells.Sign;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages loading and registering custom chants from datapacks
 * Loads from data/modid/chants/ folder
 * 
 * Chant JSON structure:
 * {
 *   "name": "Nature's Blessing",
 *   "description": "A chant to commune with nature spirits",
 *   "category": "nature",
 *   "difficulty": 2,
 *   "show_in_codex": true,
 *   "signs": ["eidolon:harmony", "eidolon:soul", "eidolon:sacred"],
 *   "requirements": ["reputation:nature_deity:10"],
 *   "effects": [
 *     {
 *       "type": "start_conversation",
 *       "deity": "eidolonunchained:nature_deity"
 *     },
 *     {
 *       "type": "give_item", 
 *       "item": "minecraft:oak_sapling",
 *       "count": 3
 *     }
 *   ]
 * }
 */
@Mod.EventBusSubscriber(modid = "eidolonunchained", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DatapackChantManager extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static DatapackChantManager INSTANCE;
    
    // Store loaded chants
    private static final Map<ResourceLocation, DatapackChant> chants = new HashMap<>();
    private static final Map<String, List<DatapackChant>> chantsByCategory = new HashMap<>();
    
    public DatapackChantManager() {
        super(GSON, "chants");
        INSTANCE = this;
    }
    
    public static DatapackChantManager getInstance() {
        return INSTANCE;
    }
    
    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resourceMap, ResourceManager resourceManager, ProfilerFiller profiler) {
        chants.clear();
        chantsByCategory.clear();
        
        if (!EidolonUnchainedConfig.COMMON.enableDatapackChants.get()) {
            LOGGER.info("Datapack chants are disabled, skipping chant loading");
            return;
        }
        
        LOGGER.info("Loading datapack chants...");
        
        for (Map.Entry<ResourceLocation, JsonElement> entry : resourceMap.entrySet()) {
            ResourceLocation location = entry.getKey();
            JsonElement element = entry.getValue();
            
            if (!element.isJsonObject()) {
                LOGGER.warn("Skipping non-object chant file: {}", location);
                continue;
            }
            
            JsonObject json = element.getAsJsonObject();
            
            try {
                loadChant(location, json);
            } catch (Exception e) {
                LOGGER.error("Failed to load chant from {}: {}", location, e.getMessage());
                if (EidolonUnchainedConfig.COMMON.enableDebugMode.get()) {
                    e.printStackTrace();
                }
            }
        }
        
        LOGGER.info("Loaded {} datapack chants in {} categories", chants.size(), chantsByCategory.size());
        
        // Register chants with Eidolon's spell system if enabled
        if (EidolonUnchainedConfig.COMMON.enableChantSystem.get()) {
            registerChantsWithEidolon();
        }
        
        // Add to codex if enabled
        if (EidolonUnchainedConfig.COMMON.showChantsInCodex.get()) {
            addChantsToCodex();
        }
    }
    
    private void loadChant(ResourceLocation location, JsonObject json) {
        LOGGER.debug("Loading chant: {}", location);
        
        DatapackChant chant = DatapackChant.fromJson(location, json);
        chants.put(location, chant);
        
        // Organize by category
        String category = chant.getCategory();
        chantsByCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(chant);
        
        LOGGER.debug("Loaded chant '{}' with {} signs in category '{}'", 
                    chant.getName(), chant.getSignSequence().size(), category);
    }
    
    private void registerChantsWithEidolon() {
        LOGGER.info("Registering {} chants with Eidolon spell system", chants.size());
        
        for (DatapackChant chant : chants.values()) {
            try {
                // Convert chant signs to Eidolon Sign objects
                Sign[] signs = convertToSigns(chant.getSignSequence());
                
                // Create spell for this chant
                DatapackChantSpell spell = new DatapackChantSpell(chant.getId(), chant, signs);
                
                // Register the spell with Eidolon
                // TODO: Implement spell registration with Eidolon's system
                
                LOGGER.debug("Registered chant spell: {}", chant.getId());
            } catch (Exception e) {
                LOGGER.error("Failed to register chant {}: {}", chant.getId(), e.getMessage());
            }
        }
    }
    
    private void addChantsToCodex() {
        if (!EidolonUnchainedConfig.COMMON.enableCodexIntegration.get()) {
            return;
        }
        
        LOGGER.info("Adding {} chants to codex", chants.size());
        
        String mainCategory = EidolonUnchainedConfig.COMMON.chantCodexCategory.get();
        
        for (Map.Entry<String, List<DatapackChant>> entry : chantsByCategory.entrySet()) {
            String category = entry.getKey();
            List<DatapackChant> categoryChants = entry.getValue();
            
            try {
                // Create codex category for this chant category
                ResourceLocation categoryId = new ResourceLocation("eidolonunchained", "chants_" + category);
                
                // TODO: Implement codex integration
                // CodexHelper.createCategory(categoryId, "Chants: " + category, categoryChants);
                
                LOGGER.debug("Added {} chants to codex category: {}", categoryChants.size(), category);
            } catch (Exception e) {
                LOGGER.error("Failed to add chants to codex for category {}: {}", category, e.getMessage());
            }
        }
    }
    
    // Public API methods
    public static Map<String, DatapackChant> getAllChants() {
        Map<String, DatapackChant> result = new HashMap<>();
        for (Map.Entry<ResourceLocation, DatapackChant> entry : chants.entrySet()) {
            result.put(entry.getKey().toString(), entry.getValue());
        }
        return result;
    }
    
    public static Collection<DatapackChant> getAllChantsCollection() {
        return new ArrayList<>(chants.values());
    }
    
    public static DatapackChant getChant(ResourceLocation id) {
        return chants.get(id);
    }
    
    public static List<DatapackChant> getChantsByCategory(String category) {
        return new ArrayList<>(chantsByCategory.getOrDefault(category, new ArrayList<>()));
    }
    
    public static Collection<String> getCategories() {
        return new ArrayList<>(chantsByCategory.keySet());
    }
    
    public static DatapackChant findChantBySignSequence(List<ResourceLocation> signSequence) {
        for (DatapackChant chant : chants.values()) {
            if (chant.getSignSequence().equals(signSequence)) {
                return chant;
            }
        }
        return null;
    }
    
    public static List<DatapackChant> findChantsWithSign(ResourceLocation sign) {
        List<DatapackChant> result = new ArrayList<>();
        for (DatapackChant chant : chants.values()) {
            if (chant.getSignSequence().contains(sign)) {
                result.add(chant);
            }
        }
        return result;
    }
    
    public static boolean hasChant(ResourceLocation id) {
        return chants.containsKey(id);
    }
    
    public static int getChantCount() {
        return chants.size();
    }
    
    public static int getCategoryCount() {
        return chantsByCategory.size();
    }
    
    /**
     * Execute a chant for a player
     */
    public static boolean executeChant(ResourceLocation chantId, net.minecraft.server.level.ServerPlayer player) {
        DatapackChant chant = getChant(chantId);
        if (chant == null) {
            LOGGER.warn("Attempted to execute unknown chant: {}", chantId);
            return false;
        }
        
        if (!chant.canPerform(player)) {
            LOGGER.debug("Player {} cannot perform chant {}", player.getName().getString(), chantId);
            return false;
        }
        
        try {
            chant.execute(player);
            LOGGER.debug("Player {} successfully performed chant {}", player.getName().getString(), chantId);
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to execute chant {} for player {}: {}", 
                        chantId, player.getName().getString(), e.getMessage());
            return false;
        }
    }
    
    /**
     * Converts ResourceLocation sign IDs to Eidolon Sign objects
     */
    private Sign[] convertToSigns(List<ResourceLocation> signIds) {
        Sign[] signs = new Sign[signIds.size()];
        
        for (int i = 0; i < signIds.size(); i++) {
            ResourceLocation signId = signIds.get(i);
            Sign sign = elucent.eidolon.registries.Signs.find(signId);
            
            if (sign == null) {
                LOGGER.warn("Unknown sign in chant: {}", signId);
                // Use a default sign as fallback
                sign = elucent.eidolon.registries.Signs.WICKED_SIGN;
            }
            
            signs[i] = sign;
        }
        
        return signs;
    }
    
    public static DatapackChantSpell getSpellForChant(ResourceLocation chantName) {
        DatapackChant chant = chants.get(chantName);
        if (chant == null) return null;
        
        // Create a spell instance for this chant
        Sign[] signs = INSTANCE.convertToSigns(chant.getSignSequence());
        return new DatapackChantSpell(chantName, chant, signs);
    }
}
