# Eidolon Unchained - Future Expansion Roadmap

## **Vision: Complete Eidolon Content Creation Framework**

The Eidolon Unchained system is designed to evolve from a simple codex extension into a **complete content creation framework** that allows users to customize every aspect of Eidolon Repraised through JSON datapacks. This document outlines the technical roadmap, implementation details, and class structures for each development phase.

---

## **Development Roadmap & Technical Implementation**

### **v0.3.8.16 (Current) - Foundation: Codex System**
**Status**: ✅ **COMPLETE**
**Focus**: Establish the core JSON-to-game integration pattern

#### **Current Architecture**
```
CodexDataManager        → Loads JSON files from datapacks
EidolonCodexIntegration → Injects content into Eidolon's systems  
EidolonPageConverter    → Converts JSON to game objects
CodexDebugCommands     → Runtime debugging tools
```

#### **Key Classes (Implemented)**
- `CodexDataManager.java` - JSON loading and validation
- `EidolonCodexIntegration.java` - Runtime integration with Eidolon
- `EidolonPageConverter.java` - Object conversion with fallback systems
- `CodexDebugCommands.java` - Debug commands for testing

---

### **v0.4.0 - Research System & Custom Rituals**
**Status**: 🔄 **PLANNED**
**Focus**: Dynamic research trees and customizable ritual systems

#### **New File Structure**
```
src/main/resources/data/eidolonunchained/
├── research_entries/       # Custom research definitions
│   ├── advanced_necromancy.json
│   ├── soul_mastery.json
│   └── void_manipulation.json
└── rituals/               # Custom ritual definitions
    ├── soul_harvest.json
    ├── wraith_binding.json
    └── dimension_tear.json
```

#### **Research System JSON Example**
```json
{
  "research_id": "advanced_necromancy",
  "display_name": "eidolonunchained.research.advanced_necromancy.title",
  "description": "eidolonunchained.research.advanced_necromancy.desc",
  "icon": "eidolon:soul_gem",
  "prerequisites": ["basic_summoning", "soul_manipulation"],
  "unlock_requirements": {
    "items_crafted": [
      {"item": "eidolon:soul_gem", "count": 5},
      {"item": "eidolon:bone_wand", "count": 1}
    ],
    "entities_killed": {
      "minecraft:skeleton": 50,
      "eidolon:wraith": 10
    },
    "rituals_performed": ["eidolon:summon_skeleton"],
    "codex_entries_read": ["advanced_monsters", "crystal_rituals"]
  },
  "rewards": {
    "codex_entries": ["master_necromancy", "death_magic_theory"],
    "recipes": ["necromancer_staff", "soul_crystal"],
    "rituals": ["mass_summoning"],
    "abilities": ["spectral_sight", "soul_drain"]
  },
  "research_tree": {
    "position": {"x": 150, "y": 75},
    "connections": ["master_summoning", "void_mastery"],
    "tier": 3,
    "secret": false
  }
}
```

#### **Custom Ritual JSON Example**
```json
{
  "ritual_id": "soul_harvest",
  "display_name": "eidolonunchained.ritual.soul_harvest.title",
  "description": "eidolonunchained.ritual.soul_harvest.desc",
  "ritual_type": "crystal_ritual",
  "soul_cost": 25,
  "components": {
    "center": {"item": "eidolon:soul_gem", "required": true},
    "circle": [
      {"item": "eidolon:pewter_ingot", "count": 4, "positions": ["north", "south", "east", "west"]},
      {"item": "minecraft:bone", "count": 8, "positions": "outer_ring"},
      {"item": "eidolon:death_essence", "count": 2, "positions": ["northeast", "southwest"]}
    ]
  },
  "requirements": {
    "moon_phase": "new_moon",
    "time_range": {"start": 18000, "end": 6000},
    "biomes": ["minecraft:swamp", "minecraft:dark_forest"],
    "weather": "any",
    "research_required": ["advanced_necromancy"]
  },
  "effects": {
    "immediate": [
      {"type": "spawn_entities", "entities": [{"type": "eidolon:wraith", "count": 2}]},
      {"type": "particle_effect", "particle": "eidolon:soul_flame", "count": 50}
    ],
    "completion": [
      {"type": "give_items", "items": [{"item": "eidolon:death_essence", "count": 5}]},
      {"type": "player_effects", "effects": [{"effect": "minecraft:night_vision", "duration": 6000, "amplifier": 1}]},
      {"type": "advance_research", "research": ["soul_mastery"]}
    ]
  },
  "failure_effects": {
    "chance": 0.1,
    "effects": [
      {"type": "spawn_entities", "entities": [{"type": "eidolon:zombie_brute", "count": 3, "hostile": true}]},
      {"type": "damage_player", "damage": 10}
    ]
  }
}
```

#### **Key Classes to Implement**

**1. `ResearchDataManager.java`**
```java
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ResearchDataManager extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<ResourceLocation, ResearchEntry> researchEntries = new HashMap<>();
    
    public ResearchDataManager() {
        super(new Gson(), "research_entries");
    }
    
    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager manager, ProfilerFiller profiler) {
        researchEntries.clear();
        
        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            try {
                ResearchEntry research = loadResearchEntry(entry.getKey(), entry.getValue().getAsJsonObject());
                researchEntries.put(entry.getKey(), research);
                LOGGER.info("Loaded research entry: {}", entry.getKey());
            } catch (Exception e) {
                LOGGER.error("Failed to load research entry: {}", entry.getKey(), e);
            }
        }
    }
    
    private ResearchEntry loadResearchEntry(ResourceLocation location, JsonObject json) {
        String researchId = json.get("research_id").getAsString();
        Component displayName = Component.translatable(json.get("display_name").getAsString());
        Component description = Component.translatable(json.get("description").getAsString());
        
        // Parse prerequisites
        List<String> prerequisites = new ArrayList<>();
        if (json.has("prerequisites")) {
            for (JsonElement prereq : json.getAsJsonArray("prerequisites")) {
                prerequisites.add(prereq.getAsString());
            }
        }
        
        // Parse unlock requirements
        UnlockRequirements requirements = parseUnlockRequirements(json.getAsJsonObject("unlock_requirements"));
        
        // Parse rewards
        ResearchRewards rewards = parseResearchRewards(json.getAsJsonObject("rewards"));
        
        return new ResearchEntry(location, researchId, displayName, description, prerequisites, requirements, rewards);
    }
}
```

**2. `ResearchEntry.java`**
```java
public class ResearchEntry {
    private final ResourceLocation id;
    private final String researchId;
    private final Component displayName;
    private final Component description;
    private final List<String> prerequisites;
    private final UnlockRequirements requirements;
    private final ResearchRewards rewards;
    private final ResearchTreePosition treePosition;
    
    public ResearchEntry(ResourceLocation id, String researchId, Component displayName, 
                        Component description, List<String> prerequisites, 
                        UnlockRequirements requirements, ResearchRewards rewards) {
        this.id = id;
        this.researchId = researchId;
        this.displayName = displayName;
        this.description = description;
        this.prerequisites = prerequisites;
        this.requirements = requirements;
        this.rewards = rewards;
    }
    
    public boolean canUnlock(Player player) {
        // Check if player has completed all prerequisites
        for (String prereq : prerequisites) {
            if (!ResearchManager.hasResearch(player, prereq)) {
                return false;
            }
        }
        
        // Check unlock requirements
        return requirements.checkRequirements(player);
    }
    
    public void unlock(Player player) {
        if (!canUnlock(player)) return;
        
        // Grant research to player
        ResearchManager.grantResearch(player, researchId);
        
        // Apply rewards
        rewards.grantRewards(player);
        
        // Send unlock message
        player.sendSystemMessage(Component.translatable("eidolonunchained.research.unlocked", displayName));
    }
}
```

**3. `RitualDataManager.java`**
```java
public class RitualDataManager extends SimpleJsonResourceReloadListener {
    private static final Map<ResourceLocation, CustomRitual> customRituals = new HashMap<>();
    
    public RitualDataManager() {
        super(new Gson(), "rituals");
    }
    
    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager manager, ProfilerFiller profiler) {
        customRituals.clear();
        
        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            try {
                CustomRitual ritual = loadCustomRitual(entry.getKey(), entry.getValue().getAsJsonObject());
                customRituals.put(entry.getKey(), ritual);
                
                // Register ritual with Eidolon's system
                EidolonRitualIntegration.registerCustomRitual(ritual);
                
                LOGGER.info("Loaded custom ritual: {}", entry.getKey());
            } catch (Exception e) {
                LOGGER.error("Failed to load custom ritual: {}", entry.getKey(), e);
            }
        }
    }
}
```

**4. `CustomRitual.java`**
```java
public class CustomRitual {
    private final ResourceLocation id;
    private final String ritualId;
    private final Component displayName;
    private final RitualType type;
    private final int soulCost;
    private final RitualComponents components;
    private final RitualRequirements requirements;
    private final RitualEffects effects;
    
    public boolean canPerform(Level level, BlockPos pos, Player player) {
        // Check components are present
        if (!components.validateComponents(level, pos)) {
            return false;
        }
        
        // Check requirements (moon phase, time, biome, etc.)
        return requirements.checkRequirements(level, pos, player);
    }
    
    public void performRitual(Level level, BlockPos pos, Player player) {
        if (!canPerform(level, pos, player)) return;
        
        // Consume soul energy
        if (!SoulEnergyManager.consumeSoul(player, soulCost)) {
            return;
        }
        
        // Consume components
        components.consumeComponents(level, pos);
        
        // Apply immediate effects
        effects.applyImmediateEffects(level, pos, player);
        
        // Schedule completion effects
        level.scheduleTick(pos, EidolonUnchainedBlocks.RITUAL_MARKER.get(), 100);
        
        // Handle potential failure
        if (Math.random() < getFailureChance(player)) {
            effects.applyFailureEffects(level, pos, player);
        } else {
            effects.applyCompletionEffects(level, pos, player);
        }
    }
}
```

---

### **v0.5.0 - Custom Entities & Advanced AI**
**Status**: 📋 **PLANNED**
**Focus**: Custom creature creation with sophisticated AI behaviors

#### **Entity JSON Example**
```json
{
  "entity_id": "shadow_necromancer",
  "display_name": "eidolonunchained.entity.shadow_necromancer.name",
  "base_entity": "eidolon:necromancer",
  "model_overrides": {
    "texture": "eidolonunchained:textures/entity/shadow_necromancer.png",
    "model": "eidolonunchained:shadow_necromancer",
    "scale": 1.2,
    "glow": true
  },
  "attributes": {
    "health": 60.0,
    "damage": 12.0,
    "speed": 0.35,
    "armor": 8.0,
    "armor_toughness": 4.0,
    "knockback_resistance": 0.6,
    "follow_range": 32.0
  },
  "ai_behaviors": [
    {
      "type": "summon_minions",
      "priority": 1,
      "entities": ["eidolon:wraith", "minecraft:skeleton"],
      "max_count": 5,
      "cooldown": 200,
      "range": 16,
      "conditions": ["health_below_50%", "target_in_range"]
    },
    {
      "type": "teleport_attack",
      "priority": 2,
      "range": 16,
      "cooldown": 100,
      "damage_multiplier": 1.5,
      "particle_effect": "eidolon:shadow_portal"
    },
    {
      "type": "soul_drain_aura",
      "priority": 3,
      "radius": 8,
      "drain_rate": 1,
      "heal_rate": 2,
      "continuous": true
    }
  ],
  "drops": [
    {
      "item": "eidolon:shadow_gem",
      "chance": 0.5,
      "count": {"min": 1, "max": 2},
      "conditions": ["killed_by_player"]
    },
    {
      "item": "eidolonunchained:necromancer_tome",
      "chance": 0.1,
      "count": 1,
      "enchantments": {"eidolon:soul_enchantment": {"min": 1, "max": 3}}
    }
  ],
  "spawn_conditions": {
    "biomes": ["minecraft:swamp", "minecraft:dark_forest"],
    "light_level": {"min": 0, "max": 7},
    "moon_phase": ["new_moon", "waning_crescent"],
    "structure_nearby": "eidolon:crypt",
    "spawn_weight": 5,
    "min_group": 1,
    "max_group": 2
  }
}
```

#### **Key Classes to Implement**

**1. `EntityDataManager.java`**
```java
public class EntityDataManager extends SimpleJsonResourceReloadListener {
    private static final Map<ResourceLocation, CustomEntityDefinition> customEntities = new HashMap<>();
    
    public EntityDataManager() {
        super(new Gson(), "entities");
    }
    
    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager manager, ProfilerFiller profiler) {
        customEntities.clear();
        
        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            try {
                CustomEntityDefinition entity = loadCustomEntity(entry.getKey(), entry.getValue().getAsJsonObject());
                customEntities.put(entry.getKey(), entity);
                
                // Register entity with Forge
                EntityIntegrationManager.registerCustomEntity(entity);
                
                LOGGER.info("Loaded custom entity: {}", entry.getKey());
            } catch (Exception e) {
                LOGGER.error("Failed to load custom entity: {}", entry.getKey(), e);
            }
        }
    }
}
```

**2. `CustomEntityDefinition.java`**
```java
public class CustomEntityDefinition {
    private final ResourceLocation id;
    private final String entityId;
    private final Component displayName;
    private final EntityType<?> baseEntity;
    private final EntityAttributes attributes;
    private final List<AIBehavior> aiBehaviors;
    private final List<DropEntry> drops;
    private final SpawnConditions spawnConditions;
    
    public Entity createEntity(Level level) {
        // Create entity based on base type
        Entity baseEntityInstance = baseEntity.create(level);
        
        if (baseEntityInstance instanceof LivingEntity living) {
            // Apply custom attributes
            attributes.applyTo(living);
            
            // Add custom AI behaviors
            if (living instanceof Mob mob) {
                for (AIBehavior behavior : aiBehaviors) {
                    behavior.addToMob(mob);
                }
            }
            
            // Set custom drops
            CustomDropHandler.setCustomDrops(living, drops);
        }
        
        return baseEntityInstance;
    }
}
```

**3. `AIBehavior.java` (Abstract Base Class)**
```java
public abstract class AIBehavior {
    protected final int priority;
    protected final JsonObject config;
    
    public AIBehavior(int priority, JsonObject config) {
        this.priority = priority;
        this.config = config;
    }
    
    public abstract void addToMob(Mob mob);
    
    public static AIBehavior createBehavior(String type, JsonObject config) {
        return switch (type) {
            case "summon_minions" -> new SummonMinionsAI(config.get("priority").getAsInt(), config);
            case "teleport_attack" -> new TeleportAttackAI(config.get("priority").getAsInt(), config);
            case "soul_drain_aura" -> new SoulDrainAuraAI(config.get("priority").getAsInt(), config);
            default -> throw new IllegalArgumentException("Unknown AI behavior type: " + type);
        };
    }
}
```

**4. `SummonMinionsAI.java` (Concrete AI Implementation)**
```java
public class SummonMinionsAI extends AIBehavior {
    private final List<EntityType<?>> minionTypes;
    private final int maxCount;
    private final int cooldown;
    private final double range;
    
    public SummonMinionsAI(int priority, JsonObject config) {
        super(priority, config);
        
        this.minionTypes = parseEntityTypes(config.getAsJsonArray("entities"));
        this.maxCount = config.get("max_count").getAsInt();
        this.cooldown = config.get("cooldown").getAsInt();
        this.range = config.get("range").getAsDouble();
    }
    
    @Override
    public void addToMob(Mob mob) {
        mob.goalSelector.addGoal(priority, new Goal() {
            private int cooldownTimer = 0;
            
            @Override
            public boolean canUse() {
                if (cooldownTimer > 0) {
                    cooldownTimer--;
                    return false;
                }
                
                return mob.getTarget() != null && 
                       mob.getTarget().distanceTo(mob) <= range &&
                       countNearbyMinions() < maxCount;
            }
            
            @Override
            public void start() {
                // Summon random minion type
                EntityType<?> minionType = minionTypes.get(mob.getRandom().nextInt(minionTypes.size()));
                Entity minion = minionType.create(mob.level());
                
                if (minion instanceof Mob summonedMob) {
                    // Position near summoner
                    Vec3 spawnPos = findSafeSpawnPosition(mob.position(), 3.0);
                    summonedMob.setPos(spawnPos);
                    
                    // Set target to summoner's target
                    summonedMob.setTarget(mob.getTarget());
                    
                    // Add to world
                    mob.level().addFreshEntity(summonedMob);
                    
                    // Particle effects
                    spawnSummonParticles(spawnPos);
                }
                
                cooldownTimer = cooldown;
            }
        });
    }
}
```

---

### **v0.6.0 - Custom Items & Equipment**
**Status**: 📋 **PLANNED**  
**Focus**: Magical items, weapons, baubles, and enchantments

#### **Custom Item JSON Example**
```json
{
  "item_id": "master_necromancer_staff",
  "display_name": "eidolonunchained.item.master_necromancer_staff.name",
  "lore": [
    "eidolonunchained.item.master_necromancer_staff.lore1",
    "eidolonunchained.item.master_necromancer_staff.lore2"
  ],
  "item_type": "staff",
  "base_item": "eidolon:bone_wand",
  "model_overrides": {
    "texture": "eidolonunchained:item/master_necromancer_staff",
    "model": "eidolonunchained:item/master_necromancer_staff",
    "glow": true
  },
  "properties": {
    "durability": 500,
    "enchantability": 25,
    "rarity": "epic",
    "fireproof": true,
    "repair_items": ["eidolon:soul_gem", "eidolon:death_essence"]
  },
  "abilities": [
    {
      "type": "summon_undead",
      "trigger": "right_click",
      "entities": ["minecraft:skeleton", "minecraft:zombie", "eidolon:wraith"],
      "max_count": 5,
      "soul_cost": 10,
      "cooldown": 200,
      "duration": 300
    },
    {
      "type": "soul_drain",
      "trigger": "passive",
      "range": 8,
      "drain_rate": 2,
      "healing": true,
      "particles": true
    },
    {
      "type": "spell_amplification",
      "trigger": "passive",
      "amplification": 1.5,
      "cost_reduction": 0.2
    }
  ],
  "crafting": {
    "type": "workbench",
    "pattern": [
      " SG ",
      " WS ",
      " BW ",
      "    "
    ],
    "ingredients": {
      "S": "eidolon:soul_gem",
      "G": "eidolon:arcane_gold_ingot", 
      "W": "eidolon:bone_wand",
      "B": "eidolon:death_essence"
    },
    "requirements": {
      "research": ["master_necromancy"],
      "soul_energy": 100
    }
  },
  "enchantments": {
    "allowed": ["minecraft:unbreaking", "eidolon:soul_efficiency"],
    "default": [
      {"enchantment": "eidolon:soul_efficiency", "level": 2}
    ]
  }
}
```

#### **Key Classes to Implement**

**1. `ItemDataManager.java`**
```java
public class ItemDataManager extends SimpleJsonResourceReloadListener {
    private static final Map<ResourceLocation, CustomItemDefinition> customItems = new HashMap<>();
    
    public ItemDataManager() {
        super(new Gson(), "items");
    }
    
    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager manager, ProfilerFiller profiler) {
        customItems.clear();
        
        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            try {
                CustomItemDefinition item = loadCustomItem(entry.getKey(), entry.getValue().getAsJsonObject());
                customItems.put(entry.getKey(), item);
                
                // Register item with Forge registry
                ItemRegistrationManager.registerCustomItem(item);
                
                LOGGER.info("Loaded custom item: {}", entry.getKey());
            } catch (Exception e) {
                LOGGER.error("Failed to load custom item: {}", entry.getKey(), e);
            }
        }
    }
}
```

**2. `CustomMagicalItem.java`**
```java
public class CustomMagicalItem extends Item {
    private final CustomItemDefinition definition;
    private final List<ItemAbility> abilities;
    
    public CustomMagicalItem(CustomItemDefinition definition) {
        super(createProperties(definition));
        this.definition = definition;
        this.abilities = definition.getAbilities();
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        // Handle right-click abilities
        for (ItemAbility ability : abilities) {
            if (ability.getTrigger() == AbilityTrigger.RIGHT_CLICK) {
                if (ability.canUse(player, stack)) {
                    ability.use(level, player, stack);
                    return InteractionResultHolder.success(stack);
                }
            }
        }
        
        return super.use(level, player, hand);
    }
    
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (entity instanceof Player player) {
            // Handle passive abilities
            for (ItemAbility ability : abilities) {
                if (ability.getTrigger() == AbilityTrigger.PASSIVE) {
                    ability.tickPassive(level, player, stack);
                }
            }
        }
        
        super.inventoryTick(stack, level, entity, slotId, isSelected);
    }
}
```

**3. `ItemAbility.java` (Abstract Base)**
```java
public abstract class ItemAbility {
    protected final AbilityTrigger trigger;
    protected final JsonObject config;
    protected final int cooldown;
    protected final int soulCost;
    
    public ItemAbility(AbilityTrigger trigger, JsonObject config) {
        this.trigger = trigger;
        this.config = config;
        this.cooldown = config.has("cooldown") ? config.get("cooldown").getAsInt() : 0;
        this.soulCost = config.has("soul_cost") ? config.get("soul_cost").getAsInt() : 0;
    }
    
    public abstract boolean canUse(Player player, ItemStack stack);
    public abstract void use(Level level, Player player, ItemStack stack);
    public abstract void tickPassive(Level level, Player player, ItemStack stack);
    
    protected boolean checkCooldown(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        long lastUse = tag.getLong("last_use");
        return level.getGameTime() - lastUse >= cooldown;
    }
    
    protected void setCooldown(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putLong("last_use", System.currentTimeMillis());
    }
    
    public static ItemAbility createAbility(String type, JsonObject config) {
        AbilityTrigger trigger = AbilityTrigger.valueOf(config.get("trigger").getAsString().toUpperCase());
        
        return switch (type) {
            case "summon_undead" -> new SummonUndeadAbility(trigger, config);
            case "soul_drain" -> new SoulDrainAbility(trigger, config);
            case "spell_amplification" -> new SpellAmplificationAbility(trigger, config);
            case "teleport" -> new TeleportAbility(trigger, config);
            default -> throw new IllegalArgumentException("Unknown ability type: " + type);
        };
    }
}
```

**4. `SummonUndeadAbility.java`**
```java
public class SummonUndeadAbility extends ItemAbility {
    private final List<EntityType<?>> entityTypes;
    private final int maxCount;
    private final int duration;
    
    public SummonUndeadAbility(AbilityTrigger trigger, JsonObject config) {
        super(trigger, config);
        
        this.entityTypes = parseEntityTypes(config.getAsJsonArray("entities"));
        this.maxCount = config.get("max_count").getAsInt();
        this.duration = config.get("duration").getAsInt();
    }
    
    @Override
    public boolean canUse(Player player, ItemStack stack) {
        return checkCooldown(stack) && 
               SoulEnergyManager.getSoulEnergy(player) >= soulCost &&
               countPlayerMinions(player) < maxCount;
    }
    
    @Override
    public void use(Level level, Player player, ItemStack stack) {
        if (!canUse(player, stack)) return;
        
        // Consume soul energy
        SoulEnergyManager.consumeSoul(player, soulCost);
        
        // Summon random undead
        EntityType<?> entityType = entityTypes.get(level.random.nextInt(entityTypes.size()));
        Entity entity = entityType.create(level);
        
        if (entity instanceof Mob mob) {
            // Position near player
            Vec3 spawnPos = findSafeSpawnPosition(player.position(), 3.0);
            mob.setPos(spawnPos);
            
            // Make friendly to player
            if (mob instanceof TamableAnimal tamable) {
                tamable.tame(player);
            } else {
                // Add custom AI goal to follow player
                mob.goalSelector.addGoal(1, new FollowOwnerGoal(mob, player, 1.0, 10.0, 2.0));
            }
            
            // Add despawn timer
            MinionManager.addTimedMinion(mob, duration);
            
            // Add to world
            level.addFreshEntity(mob);
            
            // Visual effects
            spawnSummonParticles(spawnPos);
            player.playSound(SoundEvents.EVOKER_CAST_SPELL, 1.0f, 1.0f);
        }
        
        setCooldown(stack);
    }
}
```

---

### **v0.7.0 - Structure Generation & Dungeons**
**Status**: 📋 **PLANNED**
**Focus**: Custom magical structures and procedural dungeons

#### **Structure JSON Example**
```json
{
  "structure_id": "ancient_necropolis", 
  "display_name": "eidolonunchained.structure.ancient_necropolis.name",
  "structure_type": "dungeon",
  "generation": {
    "biomes": ["minecraft:swamp", "minecraft:dark_forest", "minecraft:dark_oak_forest"],
    "rarity": 0.01,
    "y_level": {"min": 10, "max": 40},
    "spacing": 32,
    "separation": 16,
    "avoid_structures": ["minecraft:village", "minecraft:pillager_outpost"],
    "require_surface": false
  },
  "layout": {
    "size": {"x": 32, "y": 16, "z": 32},
    "entrance": {"type": "stairs", "direction": "down", "hidden": true},
    "rooms": [
      {
        "type": "ritual_chamber",
        "size": {"x": 15, "y": 8, "z": 15},
        "position": "center",
        "features": [
          {"type": "ritual_circle", "materials": ["eidolon:stone_bricks", "eidolon:candle"]},
          {"type": "altar", "item": "eidolon:soul_gem"},
          {"type": "trapped_chest", "loot_table": "eidolonunchained:necropolis_ritual"}
        ]
      },
      {
        "type": "crypt_corridor",
        "size": {"x": 20, "y": 4, "z": 4},
        "count": 4,
        "features": [
          {"type": "spawners", "entities": ["minecraft:skeleton", "eidolon:wraith"], "density": 0.3},
          {"type": "wall_graves", "loot_table": "eidolonunchained:crypt_bones"}
        ]
      },
      {
        "type": "treasure_chamber",
        "size": {"x": 12, "y": 6, "z": 12},
        "position": "deepest",
        "access_requirements": ["key_item:necropolis_key"],
        "features": [
          {"type": "boss_spawner", "entity": "eidolonunchained:shadow_necromancer"},
          {"type": "treasure_chest", "loot_table": "eidolonunchained:necropolis_treasure", "locked": true}
        ]
      }
    ]
  },
  "materials": {
    "primary": "minecraft:stone_bricks",
    "secondary": "minecraft:cobblestone", 
    "accent": "eidolon:stone_bricks",
    "floor": "minecraft:stone_brick_slab",
    "ceiling": "minecraft:stone_brick_stairs"
  },
  "inhabitants": [
    {"entity": "minecraft:skeleton", "spawn_weight": 40, "min_group": 2, "max_group": 4},
    {"entity": "eidolon:wraith", "spawn_weight": 20, "min_group": 1, "max_group": 2},
    {"entity": "eidolonunchained:shadow_necromancer", "spawn_weight": 5, "min_group": 1, "max_group": 1, "boss": true}
  ],
  "loot_tables": {
    "common": "eidolonunchained:necropolis_common",
    "rare": "eidolonunchained:necropolis_rare", 
    "boss": "eidolonunchained:necropolis_boss"
  }
}
```

#### **Key Classes to Implement**

**1. `StructureDataManager.java`**
```java
public class StructureDataManager extends SimpleJsonResourceReloadListener {
    private static final Map<ResourceLocation, CustomStructureDefinition> customStructures = new HashMap<>();
    
    public StructureDataManager() {
        super(new Gson(), "structures");
    }
    
    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager manager, ProfilerFiller profiler) {
        customStructures.clear();
        
        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            try {
                CustomStructureDefinition structure = loadCustomStructure(entry.getKey(), entry.getValue().getAsJsonObject());
                customStructures.put(entry.getKey(), structure);
                
                // Register with world generation
                StructureGenerationManager.registerCustomStructure(structure);
                
                LOGGER.info("Loaded custom structure: {}", entry.getKey());
            } catch (Exception e) {
                LOGGER.error("Failed to load custom structure: {}", entry.getKey(), e);
            }
        }
    }
}
```

**2. `CustomStructure.java`**
```java
public class CustomStructure extends Structure {
    private final CustomStructureDefinition definition;
    
    public CustomStructure(Codec<CustomStructureConfiguration> codec, CustomStructureDefinition definition) {
        super(codec);
        this.definition = definition;
    }
    
    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        
        // Check biome requirements
        if (!definition.canGenerateInBiome(context.biomeSource().getNoiseBiome(chunkPos.x, 0, chunkPos.z))) {
            return Optional.empty();
        }
        
        // Check spacing requirements
        if (!checkSpacing(context, chunkPos)) {
            return Optional.empty();
        }
        
        // Find suitable Y level
        int y = findSuitableYLevel(context, chunkPos);
        if (y == -1) {
            return Optional.empty();
        }
        
        BlockPos pos = new BlockPos(chunkPos.getMiddleBlockX(), y, chunkPos.getMiddleBlockZ());
        
        return Optional.of(new GenerationStub(pos, (structureBuilder) -> {
            generateStructure(structureBuilder, pos, context.random());
        }));
    }
    
    private void generateStructure(StructurePiecesBuilder builder, BlockPos pos, RandomSource random) {
        // Create main structure piece
        CustomStructurePiece mainPiece = new CustomStructurePiece(definition, pos, random);
        builder.addPiece(mainPiece);
        
        // Generate additional rooms based on definition
        for (RoomDefinition room : definition.getRooms()) {
            BlockPos roomPos = calculateRoomPosition(pos, room, random);
            CustomRoomPiece roomPiece = new CustomRoomPiece(room, roomPos, random);
            builder.addPiece(roomPiece);
        }
    }
}
```

**3. `CustomStructurePiece.java`**
```java
public class CustomStructurePiece extends StructurePiece {
    private final CustomStructureDefinition definition;
    private final RandomSource random;
    
    public CustomStructurePiece(CustomStructureDefinition definition, BlockPos pos, RandomSource random) {
        super(EidolonUnchainedStructurePieces.CUSTOM_STRUCTURE.get(), 0, 
              BoundingBox.fromCorners(pos, pos.offset(definition.getSize())));
        this.definition = definition;
        this.random = random;
    }
    
    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, 
                           ChunkGenerator chunkGenerator, RandomSource random, 
                           BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        
        // Generate base structure
        generateWalls(level, chunkBox);
        generateFloors(level, chunkBox);
        generateCeiling(level, chunkBox);
        
        // Add rooms
        for (RoomDefinition room : definition.getRooms()) {
            generateRoom(level, room, chunkBox, random);
        }
        
        // Add features (chests, spawners, etc.)
        addFeatures(level, chunkBox, random);
        
        // Spawn inhabitants
        spawnInhabitants(level, chunkBox, random);
    }
    
    private void generateRoom(WorldGenLevel level, RoomDefinition room, BoundingBox bounds, RandomSource random) {
        switch (room.getType()) {
            case RITUAL_CHAMBER -> generateRitualChamber(level, room, bounds, random);
            case CRYPT_CORRIDOR -> generateCryptCorridor(level, room, bounds, random);
            case TREASURE_CHAMBER -> generateTreasureChamber(level, room, bounds, random);
        }
    }
    
    private void generateRitualChamber(WorldGenLevel level, RoomDefinition room, BoundingBox bounds, RandomSource random) {
        BlockPos center = room.getCenterPosition();
        
        // Create ritual circle
        createRitualCircle(level, center, 3, definition.getMaterials().accent);
        
        // Add altar
        level.setBlock(center, Blocks.ENCHANTING_TABLE.defaultBlockState(), 2);
        
        // Add candles around circle
        for (int i = 0; i < 8; i++) {
            double angle = (i * Math.PI * 2) / 8;
            BlockPos candlePos = center.offset(
                (int)(Math.cos(angle) * 4),
                0,
                (int)(Math.sin(angle) * 4)
            );
            level.setBlock(candlePos, Blocks.CANDLE.defaultBlockState(), 2);
        }
        
        // Add treasure chest
        BlockPos chestPos = center.offset(random.nextInt(6) - 3, 0, random.nextInt(6) - 3);
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 2);
        
        // Set loot table
        if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            chest.setLootTable(new ResourceLocation("eidolonunchained", "necropolis_ritual"), random.nextLong());
        }
    }
}
```

---

### **v0.8.0 - Advanced Loot & Trading Systems**
**Status**: 📋 **PLANNED**
**Focus**: Dynamic loot generation and custom trading

### **v0.9.0 - World Generation & Biome Modifications**  
**Status**: 📋 **PLANNED**
**Focus**: Custom biomes and world features

### **v1.0.0 - Complete Framework**
**Status**: 📋 **PLANNED** 
**Focus**: Polish, optimization, and comprehensive documentation

---

## **Integration Philosophy & Architecture**

### **Core Design Principles**

1. **JSON-First Architecture**: Every system configurable through JSON files
2. **Modular Independence**: Each content type works independently but interconnects seamlessly
3. **Backward Compatibility**: New versions never break existing content
4. **Comprehensive Debugging**: Debug commands and detailed logging for every system
5. **Performance Optimized**: Lazy loading and efficient caching systems

### **Universal Class Architecture Pattern**

Every content system follows this pattern:

```
[ContentType]DataManager.java    → Loads and validates JSON files
[ContentType]Definition.java     → Represents the JSON data structure
[ContentType]Integration.java    → Integrates with Minecraft/Eidolon systems
Custom[ContentType].java         → The actual game object implementation
[ContentType]DebugCommands.java  → Debug tools for testing
```

### **Cross-System Integration**

- **Research System** unlocks rituals, items, and structures
- **Custom Rituals** can create items and spawn entities
- **Custom Entities** drop custom items and guard structures
- **Custom Structures** contain loot tables and spawn custom entities
- **All Systems** integrate with the codex for documentation

---

## **User Benefits & Use Cases**

This complete framework will enable users to:

### **🎮 Content Creators**
- Create **total conversion mods** without Java programming
- Develop **themed content packs** (Egyptian Necromancy, Nordic Death Magic, etc.)
- Build **adventure maps** with custom progression and quests

### **🔧 Modpack Developers** 
- **Balance and customize** all Eidolon content for their modpacks
- Create **unique gameplay experiences** with interconnected systems
- **Fine-tune difficulty** and progression curves

### **🏗️ Server Administrators**
- Add **server-exclusive content** without client-side mods
- Create **seasonal events** with temporary custom content
- **Customize gameplay** for different server themes

### **📚 Educators & Researchers**
- Use as a **teaching tool** for game design and JSON data structures
- **Experiment with game balance** and content design
- Create **educational content** about magical systems and mythology

---

## **Technical Implementation Timeline**

- **Current (v0.3.8.16)**: ✅ Foundation established with codex system
- **Q4 2025 (v0.4.0)**: 🔄 Research & Ritual systems 
- **Q1 2026 (v0.5.0)**: 📋 Custom entities & AI
- **Q2 2026 (v0.6.0)**: 📋 Items & equipment
- **Q3 2026 (v0.7.0)**: 📋 Structure generation
- **Q4 2026 (v0.8.0)**: 📋 Loot & trading systems
- **Q1 2027 (v0.9.0)**: 📋 World generation
- **Q2 2027 (v1.0.0)**: 🎯 Complete framework release

---

**This roadmap represents a comprehensive vision for transforming Eidolon Unchained from a simple codex extension into the most powerful JSON-based content creation framework for magical mods in Minecraft.**
