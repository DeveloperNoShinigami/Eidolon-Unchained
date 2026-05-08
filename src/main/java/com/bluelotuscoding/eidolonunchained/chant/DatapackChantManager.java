package com.bluelotuscoding.eidolonunchained.chant;

import com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import elucent.eidolon.api.spells.Sign;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.common.util.LazyOptional;
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
 * "name": "Nature's Blessing",
 * "description": "A chant to commune with nature spirits",
 * "category": "nature",
 * "difficulty": 2,
 * "show_in_codex": true,
 * "signs": ["eidolon:harmony", "eidolon:soul", "eidolon:sacred"],
 * "requirements": ["reputation:nature_deity:10"],
 * "effects": [
 * {
 * "type": "start_conversation",
 * "deity": "eidolonunchained:nature_deity"
 * },
 * {
 * "type": "give_item",
 * "item": "minecraft:oak_sapling",
 * "count": 3
 * }
 * ]
 * }
 */
@Mod.EventBusSubscriber(modid = "eidolonunchained", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DatapackChantManager extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = com.bluelotuscoding.eidolonunchained.util.JsonUtils.GSON;
    private static DatapackChantManager INSTANCE;

    // Store loaded chants
    private static final Map<ResourceLocation, DatapackChant> chants = new HashMap<>();
    private static final Map<String, List<DatapackChant>> chantsByCategory = new HashMap<>();

    // CRITICAL: Track registered chants to prevent duplicate config registration
    // Forge config system throws errors if we try to register the same config twice
    private static final java.util.Set<ResourceLocation> registeredChantConfigs = new java.util.HashSet<>();

    public DatapackChantManager() {
        super(GSON, "chants");
        INSTANCE = this;
    }

    public static DatapackChantManager getInstance() {
        return INSTANCE;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resourceMap, ResourceManager resourceManager,
            ProfilerFiller profiler) {
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

        try {
            // Normalize ID to the filename only, so moving into subfolders doesn\'t change
            // the chant id
            String path = location.getPath();
            String base = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
            ResourceLocation chantId = new ResourceLocation(location.getNamespace(), base);

            // Ensure category is set based on subfolder if not provided
            if (!json.has("category")) {
                String cat = "common";
                if (path.contains("/")) {
                    cat = path.substring(0, path.indexOf('/'));
                }
                // Create a shallow copy with injected category
                JsonObject patched = new JsonObject();
                for (var e : json.entrySet())
                    patched.add(e.getKey(), e.getValue());
                patched.addProperty("category", cat);
                json = patched;
            }

            DatapackChant chant = DatapackChant.fromJson(chantId, json);
            chants.put(chantId, chant);

            // Organize by category
            String category = chant.getCategory();
            chantsByCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(chant);

            LOGGER.debug("Loaded chant '{}' with {} signs in category '{}'",
                    chant.getName(), chant.getSignSequence().size(), category);
        } catch (Exception e) {
            LOGGER.error("Failed to load chant from {}: {}", location, e.getMessage());
            if (EidolonUnchainedConfig.COMMON.enableDebugMode.get()) {
                e.printStackTrace();
            }
        }
    }

    public void registerChantsWithEidolon() {
        LOGGER.info("Registering {} chants with Eidolon spell system", chants.size());

        int registered = 0;
        int skipped = 0;

        for (DatapackChant chant : chants.values()) {
            try {
                // CRITICAL FIX: Check if this chant's config is already registered
                // This prevents "Config file conflict" errors when chants are registered
                // multiple times
                if (registeredChantConfigs.contains(chant.getId())) {
                    LOGGER.debug("Chant {} already registered, skipping to avoid config conflict", chant.getId());
                    skipped++;
                    continue;
                }

                // Convert chant signs to Eidolon Sign objects using static method
                Sign[] signs = convertSignsStatically(chant.getSignSequence());

                // Create spell for this chant
                DatapackChantSpell spell = new DatapackChantSpell(chant.getId(), chant, signs);

                // CRITICAL FIX: Set the sign sequence after construction
                // This is required because StaticSpell constructor doesn't initialize signs
                spell.setSigns(new elucent.eidolon.api.spells.SignSequence(signs));

                // Register the spell with Eidolon's spell system
                elucent.eidolon.registries.Spells.registerWithFallback(spell);

                // Mark this chant as registered
                registeredChantConfigs.add(chant.getId());
                registered++;

                LOGGER.info("Successfully registered chant spell: {} with signs: {}",
                        chant.getId(), chant.getSignSequence());
            } catch (Exception e) {
                LOGGER.error("Failed to register chant {}: {}", chant.getId(), e.getMessage());
                if (e.getMessage() != null && e.getMessage().contains("Config conflict detected")) {
                    LOGGER.error("Config conflict for chant {} - this should not happen with registration tracking!",
                            chant.getId());
                }
                e.printStackTrace();
            }
        }

        LOGGER.info("Registered {} new chants, skipped {} already-registered chants", registered, skipped);
    }

    private void addChantsToCodex() {
        if (!EidolonUnchainedConfig.COMMON.enableCodexIntegration.get()) {
            return;
        }

        LOGGER.info("Deferring chant codex registration until CodexEvents.PostInit so datapack categories survive codex rebuilds");
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

    public static Collection<ResourceLocation> getAllChantIds() {
        return new ArrayList<>(chants.keySet());
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

    /**
     * Get all chants that are linked to a specific deity
     */
    public static List<DatapackChant> getChantsForDeity(ResourceLocation deityId) {
        List<DatapackChant> result = new ArrayList<>();
        for (DatapackChant chant : chants.values()) {
            if (chant.hasLinkedDeity() && chant.getLinkedDeity().equals(deityId)) {
                result.add(chant);
            }
        }
        return result;
    }

    public static DatapackChant findChantBySignSequence(List<ResourceLocation> signSequence) {
        for (DatapackChant chant : chants.values()) {
            if (chant.getSignSequence().equals(signSequence)) {
                return chant;
            }
        }
        return null;
    }

    /**
     * Check if the given sign sequence is the start of any valid chant
     */
    public static boolean hasValidChantPrefix(List<ResourceLocation> signSequence) {
        if (signSequence.isEmpty()) {
            return true; // Empty sequence is valid (start of any chant)
        }

        for (DatapackChant chant : chants.values()) {
            List<ResourceLocation> chantSigns = chant.getSignSequence();
            if (chantSigns.size() >= signSequence.size()) {
                // Check if this chant starts with the given sequence
                boolean matches = true;
                for (int i = 0; i < signSequence.size(); i++) {
                    if (!chantSigns.get(i).equals(signSequence.get(i))) {
                        matches = false;
                        break;
                    }
                }
                if (matches) {
                    return true; // Found a chant that starts with this sequence
                }
            }
        }
        return false; // No chant starts with this sequence
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

        // Check cooldown
        if (!ChantCooldownManager.canCastChant(player, chant)) {
            int remainingSeconds = ChantCooldownManager.getRemainingCooldown(player, chant);
            player.sendSystemMessage(
                    Component.literal("§cChant is on cooldown for " + remainingSeconds + " more seconds"));
            return false;
        }

        // Check mana cost (if enabled in config)
        int manaCost = chant.getManaCost();
        if (com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig.COMMON.enableManaCosts.get()
                && manaCost > 0) {
            LazyOptional<elucent.eidolon.capability.ISoul> soulCap = player
                    .getCapability(elucent.eidolon.capability.ISoul.INSTANCE);
            if (soulCap.isPresent()) {
                elucent.eidolon.capability.ISoul soul = soulCap.orElse(null);
                if (soul != null && soul.getMagic() < manaCost) {
                    player.sendSystemMessage(Component.literal(
                            "§cNot enough mana! Required: " + manaCost + ", Available: " + (int) soul.getMagic()));
                    return false;
                }
            }
        }

        if (!chant.canPerform(player)) {
            LOGGER.debug("Player {} cannot perform chant {}", player.getName().getString(), chantId);
            return false;
        }

        try {
            // Deduct mana cost before execution (if enabled)
            if (com.bluelotuscoding.eidolonunchained.config.EidolonUnchainedConfig.COMMON.enableManaCosts.get()
                    && manaCost > 0) {
                elucent.eidolon.capability.ISoul.expendMana(player, manaCost);
            }

            // Set cooldown
            ChantCooldownManager.setCooldown(player, chant);

            chant.execute(player);
            LOGGER.debug("Player {} successfully performed chant {} (cost: {} mana)",
                    player.getName().getString(), chantId, manaCost);

            // Record chant performance for AI context tracking
            try {
                com.bluelotuscoding.eidolonunchained.ai.PlayerContextTracker.recordChant(
                        player, chantId, chant.getLinkedDeity(), true);
            } catch (Exception e) {
                LOGGER.warn("Failed to record chant for AI context", e);
            }

            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to execute chant {} for player {}: {}",
                    chantId, player.getName().getString(), e.getMessage());

            // Record failed chant for AI context
            try {
                com.bluelotuscoding.eidolonunchained.ai.PlayerContextTracker.recordChant(
                        player, chantId, chant.getLinkedDeity(), false);
            } catch (Exception e2) {
                LOGGER.warn("Failed to record failed chant for AI context", e2);
            }

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
        if (chant == null)
            return null;

        // Create a spell instance for this chant using static method
        Sign[] signs = convertSignsStatically(chant.getSignSequence());
        DatapackChantSpell spell = new DatapackChantSpell(chantName, chant, signs);

        // CRITICAL FIX: Set the sign sequence after construction
        spell.setSigns(new elucent.eidolon.api.spells.SignSequence(signs));

        return spell;
    }

    // CLIENT-SIDE METHODS FOR MULTIPLAYER SYNC

    /**
     * Clears client-side chant data for multiplayer sync
     * Called when receiving sync packet from server
     */
    public static void clearClientChants() {
        if (chants != null) {
            chants.clear();
        }
        if (chantsByCategory != null) {
            chantsByCategory.clear();
        }
        LOGGER.info("Cleared client-side chant data for sync");
    }

    /**
     * Adds a chant to client-side storage during multiplayer sync
     */
    public static void addClientChant(ResourceLocation id, DatapackChant chant) {
        chants.put(id, chant);

        String category = chant.getCategory();
        chantsByCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(chant);

        LOGGER.debug("Added client chant: {} in category: {}", id, category);
    }

    /**
     * CRITICAL: Register client-side chants with Eidolon's spell system
     * This ensures that spell resolution works in multiplayer
     * Uses client-safe registration to avoid config conflicts
     */
    public static void registerClientChantsWithEidolon() {
        if (!EidolonUnchainedConfig.COMMON.enableChantSystem.get()) {
            LOGGER.info("Chant system disabled, skipping client-side spell registration");
            return;
        }

        LOGGER.info("Registering {} client-side chants with Eidolon spell system", chants.size());

        int registered = 0;
        for (DatapackChant chant : chants.values()) {
            try {
                // Convert chant signs to Eidolon Sign objects - use static method to avoid
                // INSTANCE dependency
                Sign[] signs = convertSignsStatically(chant.getSignSequence());

                // Create spell for this chant
                DatapackChantSpell spell = new DatapackChantSpell(chant.getId(), chant, signs);

                // CRITICAL: Set the sign sequence after construction
                spell.setSigns(new elucent.eidolon.api.spells.SignSequence(signs));

                // CLIENT-SAFE REGISTRATION: Add directly to collections without config creation
                // This avoids "config file conflict" errors since server already created
                // configs
                elucent.eidolon.registries.Spells.getSpellMap().put(spell.getRegistryName(), spell);
                elucent.eidolon.registries.Spells.getSpells().add(spell);

                registered++;
                LOGGER.info("CLIENT: Successfully registered chant spell: {} with signs: {}",
                        chant.getId(), chant.getSignSequence());
            } catch (Exception e) {
                LOGGER.error("CLIENT: Failed to register chant {}: {}", chant.getId(), e.getMessage());
                if (e.getMessage().contains("Config conflict detected")) {
                    LOGGER.error("CLIENT: Skipping config creation for client-side spell registration");
                }
                e.printStackTrace();
            }
        }

        LOGGER.info("CLIENT: Successfully registered {} chant spells with Eidolon", registered);
    }

    /**
     * Static version of convertToSigns that doesn't depend on INSTANCE
     * This prevents null pointer exceptions in client-side multiplayer
     */
    private static Sign[] convertSignsStatically(List<ResourceLocation> signIds) {
        Sign[] signs = new Sign[signIds.size()];

        for (int i = 0; i < signIds.size(); i++) {
            ResourceLocation signId = signIds.get(i);

            // Alias a few common, lore-friendly names to known signs
            ResourceLocation resolved = signId;
            if ("eidolon".equals(signId.getNamespace())) {
                if ("life".equals(signId.getPath())) {
                    // Map to 'harmony' as closest vanilla sign meaning life/nature
                    resolved = new ResourceLocation("eidolon", "harmony");
                } else if ("shadow".equals(signId.getPath())) {
                    // Map to 'wicked' as closest vanilla sign meaning shadow/dark
                    resolved = new ResourceLocation("eidolon", "wicked");
                }
            }

            Sign sign = elucent.eidolon.registries.Signs.find(resolved);

            if (sign == null) {
                LOGGER.warn("Unknown sign in chant: {} - using fallback", signId);
                // Use a default sign as fallback
                sign = elucent.eidolon.registries.Signs.WICKED_SIGN;
            }

            signs[i] = sign;
        }

        return signs;
    }
}
