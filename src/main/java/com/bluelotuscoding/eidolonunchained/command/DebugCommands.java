package com.bluelotuscoding.eidolonunchained.command;

import com.bluelotuscoding.eidolonunchained.network.AttributeSignStatusIconPacket;
import com.bluelotuscoding.eidolonunchained.network.EidolonUnchainedNetworking;
import com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig;
import com.bluelotuscoding.eidolonunchained.ai.AIDeityManager;
import com.bluelotuscoding.eidolonunchained.ai.UniversalAIContext;
import com.bluelotuscoding.eidolonunchained.config.APIKeyManager;
import com.bluelotuscoding.eidolonunchained.integration.ai.RegistryContextProvider;
import com.bluelotuscoding.eidolonunchained.integration.gemini.GeminiAPIClient;
import com.bluelotuscoding.eidolonunchained.data.DivineResistanceTypeManager;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import elucent.eidolon.capability.ISoul;
import elucent.eidolon.network.Networking;
import elucent.eidolon.network.SoulUpdatePacket;
import elucent.eidolon.registries.EidolonAttributes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * All /eu debug subcommands in one place.
 * Merged from: AIDebugCommand, AIDebugCommands, RitualDiagnoseCommand.
 */
public class DebugCommands {

    private static final Logger LOGGER = LoggerFactory.getLogger(DebugCommands.class);
    private static final String TAG_DIVINE_RESISTANCES = "eu_divine_resistances";

    private static final SuggestionProvider<CommandSourceStack> DIVINE_RESISTANCE_KEY_SUGGESTIONS = (context, builder) ->
        SharedSuggestionProvider.suggest(new ArrayList<>(DivineResistanceTypeManager.getAllResistanceKeys()), builder);

    /** Build the "debug" subtree to be attached to the /eu root. */
    public static LiteralArgumentBuilder<CommandSourceStack> buildNode(
            SuggestionProvider<CommandSourceStack> playerSuggestions,
            SuggestionProvider<CommandSourceStack> deitySuggestions,
            SuggestionProvider<CommandSourceStack> ritualSuggestions,
            SuggestionProvider<CommandSourceStack> factSuggestions) {

        return Commands.literal("debug")
            .requires(cs -> cs.hasPermission(2))

            // ── ai subtree ───────────────────────────────────────────────────────
            .then(Commands.literal("ai")
                .then(Commands.literal("context")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("deity", ResourceLocationArgument.id())
                            .suggests(deitySuggestions)
                            .executes(DebugCommands::showAIContext))))
                .then(Commands.literal("test")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("deity", ResourceLocationArgument.id())
                            .suggests(deitySuggestions)
                            .then(Commands.argument("prompt", StringArgumentType.greedyString())
                                .executes(DebugCommands::testAIResponse)))))
                .then(Commands.literal("world-knowledge")
                    .executes(DebugCommands::testWorldKnowledge))
                .then(Commands.literal("item-search")
                    .then(Commands.argument("item", StringArgumentType.greedyString())
                        .executes(DebugCommands::testItemSearch)))
                .then(Commands.literal("deity-context")
                    .then(Commands.argument("deity", ResourceLocationArgument.id())
                        .suggests(deitySuggestions)
                        .executes(DebugCommands::testDeityContext)))
                .then(Commands.literal("registry-stats")
                    .executes(DebugCommands::showRegistryStats)))

            // ── ritual subtree ───────────────────────────────────────────────────
            .then(Commands.literal("ritual")
                .then(Commands.literal("list")
                    .executes(DebugCommands::listLoadedRituals))
                .then(Commands.literal("test")
                    .then(Commands.argument("ritualId", ResourceLocationArgument.id())
                        .suggests(ritualSuggestions)
                        .executes(DebugCommands::testRitualExecution)))
                .then(Commands.literal("diagnose")
                    .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(DebugCommands::diagnoseRitualAtPos))
                    .executes(DebugCommands::diagnoseRitualSystem)))

            // ── reputation ───────────────────────────────────────────────────────
            .then(Commands.literal("reputation")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("deity", ResourceLocationArgument.id())
                        .suggests(deitySuggestions)
                        .executes(DebugCommands::debugPlayerReputation))))

            // ── mana / soul ──────────────────────────────────────────────────────
            .then(Commands.literal("mana")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(DebugCommands::showPlayerMana)
                    .then(Commands.literal("get")
                        .executes(DebugCommands::showPlayerMana))
                    .then(Commands.literal("set")
                        .then(Commands.argument("value", FloatArgumentType.floatArg(0))
                            .executes(DebugCommands::setPlayerMana)))
                    .then(Commands.literal("restore")
                        .then(Commands.argument("amount", FloatArgumentType.floatArg(0))
                            .executes(DebugCommands::restorePlayerMana)))
                    .then(Commands.literal("set-max")
                        .then(Commands.argument("value", FloatArgumentType.floatArg(0))
                            .executes(DebugCommands::setPlayerMaxMana)))
                    .then(Commands.literal("increase-max")
                        .then(Commands.argument("amount", FloatArgumentType.floatArg(0))
                            .executes(DebugCommands::increasePlayerMaxMana)))))

            // ── custom status effect overlay ───────────────────────────────────
            .then(Commands.literal("custom-effect")
                .then(Commands.literal("apply")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("effect", ResourceLocationArgument.id())
                            .then(Commands.argument("sign", ResourceLocationArgument.id())
                                .then(Commands.argument("duration_ticks", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                                    .executes(DebugCommands::applyCustomEffectOverlay))))))
                .then(Commands.literal("apply-magic-power")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("effect", ResourceLocationArgument.id())
                            .then(Commands.argument("sign", ResourceLocationArgument.id())
                                .then(Commands.argument("duration_ticks", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                                    .then(Commands.argument("amount", FloatArgumentType.floatArg(-10.0f, 10.0f))
                                        .executes(DebugCommands::applyCustomEffectWithMagicPower)))))))
                .then(Commands.literal("clear")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("effect", ResourceLocationArgument.id())
                            .executes(DebugCommands::clearCustomEffectOverlay)))))

            // ── progression ──────────────────────────────────────────────────────
            .then(Commands.literal("progression")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("deity", ResourceLocationArgument.id())
                        .suggests(deitySuggestions)
                        .executes(DebugCommands::debugPlayerProgression))))

            .then(Commands.literal("force-progression")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("deity", ResourceLocationArgument.id())
                        .suggests(deitySuggestions)
                        .executes(DebugCommands::forceProgressionCheck))))

            .then(Commands.literal("tier")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("deity", ResourceLocationArgument.id())
                        .suggests(deitySuggestions)
                        .executes(DebugCommands::debugTierTracking))))

            // ── facts ────────────────────────────────────────────────────────────
            .then(Commands.literal("facts")
                .then(Commands.literal("grant")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("fact", ResourceLocationArgument.id())
                            .suggests(factSuggestions)
                            .executes(DebugCommands::grantFact))))
                .then(Commands.literal("revoke")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("fact", ResourceLocationArgument.id())
                            .suggests(factSuggestions)
                            .executes(DebugCommands::revokeFact))))
                .then(Commands.literal("list")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(DebugCommands::listFacts))))

            // ── attribute ────────────────────────────────────────────────────────
            .then(Commands.literal("attribute")
                .then(Commands.argument("entity", EntityArgument.entity())
                    .then(Commands.literal("get")
                        .then(Commands.argument("attribute", ResourceLocationArgument.id())
                            .executes(DebugCommands::getEntityAttribute)))
                    .then(Commands.literal("set")
                        .then(Commands.argument("attribute", ResourceLocationArgument.id())
                            .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                .executes(DebugCommands::setEntityAttributeBase))))
                    .then(Commands.literal("add")
                        .then(Commands.argument("attribute", ResourceLocationArgument.id())
                            .then(Commands.argument("amount", DoubleArgumentType.doubleArg())
                                .executes(DebugCommands::addEntityAttributeModifier))))))

            // ── divine-resistance tag editor ────────────────────────────────────
            .then(Commands.literal("divine-resistance")
                .then(Commands.argument("entity", EntityArgument.entity())
                    .then(Commands.literal("set")
                        .then(Commands.argument("key", StringArgumentType.string())
                            .suggests(DIVINE_RESISTANCE_KEY_SUGGESTIONS)
                            .then(Commands.argument("value", DoubleArgumentType.doubleArg(-10.0d, 10.0d))
                                .executes(DebugCommands::setDivineResistanceTagValue))))
                    .then(Commands.literal("get")
                        .then(Commands.argument("key", StringArgumentType.string())
                            .suggests(DIVINE_RESISTANCE_KEY_SUGGESTIONS)
                            .executes(DebugCommands::getDivineResistanceTagValue)))
                    .then(Commands.literal("clear")
                        .then(Commands.argument("key", StringArgumentType.string())
                            .suggests(DIVINE_RESISTANCE_KEY_SUGGESTIONS)
                            .executes(DebugCommands::clearDivineResistanceTagValue)))))

            // ── clear-rewards ────────────────────────────────────────────────────
            .then(Commands.literal("clear-rewards")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(DebugCommands::clearAllPlayerRewards)
                    .then(Commands.argument("deity", ResourceLocationArgument.id())
                        .suggests(deitySuggestions)
                        .executes(DebugCommands::clearPlayerRewards))));
    }

    // ── AI context ───────────────────────────────────────────────────────────────

    private static int showAIContext(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
            ResourceLocation deityId = ResourceLocationArgument.getId(ctx, "deity");

            AIDeityConfig aiConfig = AIDeityManager.getInstance().getAIConfig(deityId);
            if (aiConfig == null) {
                ctx.getSource().sendFailure(Component.literal("No AI config found for deity: " + deityId));
                return 0;
            }

            String fullContext = UniversalAIContext.buildFullContext(player, aiConfig);
            ctx.getSource().sendSuccess(() -> Component.literal("§a=== AI Context for " + player.getName().getString() + " → " + deityId + " ==="), false);

            String[] lines = fullContext.split("\\n");
            StringBuilder chunk = new StringBuilder();
            for (String line : lines) {
                if (chunk.length() + line.length() + 1 > 500) {
                    String s = chunk.toString();
                    ctx.getSource().sendSuccess(() -> Component.literal("§7" + s), false);
                    chunk = new StringBuilder();
                }
                chunk.append(line).append("\\n");
            }
            if (chunk.length() > 0) {
                String s = chunk.toString();
                ctx.getSource().sendSuccess(() -> Component.literal("§7" + s), false);
            }
            ctx.getSource().sendSuccess(() -> Component.literal("§a=== End AI Context ==="), false);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error showing AI context", e);
            ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int testAIResponse(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
            ResourceLocation deityId = ResourceLocationArgument.getId(ctx, "deity");
            String prompt = StringArgumentType.getString(ctx, "prompt");

            AIDeityConfig aiConfig = AIDeityManager.getInstance().getAIConfig(deityId);
            if (aiConfig == null) {
                ctx.getSource().sendFailure(Component.literal("No AI config found for deity: " + deityId));
                return 0;
            }

            ctx.getSource().sendSuccess(() -> Component.literal("Testing AI for " + player.getName().getString() + " → " + deityId), false);
            switch (aiConfig.ai_provider.toLowerCase()) {
                case "gemini" -> {
                    GeminiAPIClient client = new GeminiAPIClient(
                        APIKeyManager.getAPIKey("gemini"), aiConfig.model, 30);
                    client.generateResponse(prompt, "You are a helpful test deity.", null, null)
                        .thenAccept(resp -> {
                            ctx.getSource().sendSuccess(() -> Component.literal("§a✓ Gemini response: " + resp.dialogue), false);
                        })
                        .exceptionally(err -> {
                            ctx.getSource().sendFailure(Component.literal("§c✗ Gemini failed: " + err.getMessage()));
                            return null;
                        });
                }
                case "openrouter" ->
                    ctx.getSource().sendSuccess(() -> Component.literal("§eOpenRouter testing not yet implemented"), false);
                case "player2ai" ->
                    ctx.getSource().sendSuccess(() -> Component.literal("§ePlayer2AI testing not yet implemented"), false);
                default -> {
                    ctx.getSource().sendFailure(Component.literal("Unsupported AI provider: " + aiConfig.ai_provider));
                    return 0;
                }
            }
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error testing AI response", e);
            ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    // ── world knowledge / registry ───────────────────────────────────────────────

    private static int testWorldKnowledge(CommandContext<CommandSourceStack> ctx) {
        try {
            List<String> mods = Arrays.asList("minecraft", "eidolon", "eidolonunchained");
            String worldContext = RegistryContextProvider.generateContextForMods(mods);

            ctx.getSource().sendSuccess(() -> Component.literal("§a=== AI World Knowledge Test ==="), false);
            ctx.getSource().sendSuccess(() -> Component.literal("§7Context length: " + worldContext.length() + " chars"), false);

            List<ResourceLocation> diamond = RegistryContextProvider.findMatchingItems("diamond", mods);
            ctx.getSource().sendSuccess(() -> Component.literal("§7'diamond' matches: " + diamond.size()), false);

            List<ResourceLocation> soul = RegistryContextProvider.findMatchingItems("soul", mods);
            ctx.getSource().sendSuccess(() -> Component.literal("§7'soul' matches: " + soul.size()), false);

            boolean hasItems = worldContext.contains("diamond") && worldContext.contains("item");
            ctx.getSource().sendSuccess(() -> Component.literal(hasItems ? "§a✓ AI receives item context" : "§c✗ Item context missing"), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("World knowledge test failed: " + e.getMessage()));
            return 0;
        }
    }

    private static int testItemSearch(CommandContext<CommandSourceStack> ctx) {
        try {
            String search = StringArgumentType.getString(ctx, "item");
            List<String> mods = Arrays.asList("minecraft", "eidolon", "eidolonunchained");
            List<ResourceLocation> matches = RegistryContextProvider.findMatchingItems(search, mods);

            ctx.getSource().sendSuccess(() -> Component.literal("§a=== Item Search: '" + search + "' ==="), false);
            if (matches.isEmpty()) {
                ctx.getSource().sendSuccess(() -> Component.literal("§cNo matches found"), false);
            } else {
                ctx.getSource().sendSuccess(() -> Component.literal("§aFound " + matches.size() + " match(es):"), false);
                for (int i = 0; i < Math.min(10, matches.size()); i++) {
                    ResourceLocation m = matches.get(i);
                    final int idx = i + 1;
                    ctx.getSource().sendSuccess(() -> Component.literal("§7  " + idx + ". " + m), false);
                }
                if (matches.size() > 10)
                    ctx.getSource().sendSuccess(() -> Component.literal("§7  ... and " + (matches.size() - 10) + " more"), false);
            }
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Item search failed: " + e.getMessage()));
            return 0;
        }
    }

    private static int testDeityContext(CommandContext<CommandSourceStack> ctx) {
        try {
            ResourceLocation deityId = ResourceLocationArgument.getId(ctx, "deity");
            AIDeityConfig config = AIDeityManager.getInstance().getAIConfig(deityId);
            if (config == null) {
                ctx.getSource().sendFailure(Component.literal("Deity not found: " + deityId));
                return 0;
            }

            List<String> modIds = config.mod_context_ids;
            if (modIds == null || modIds.isEmpty()) {
                ctx.getSource().sendFailure(Component.literal("No mod_context_ids configured for " + deityId));
                return 0;
            }

            String aiContext = RegistryContextProvider.generateContextForMods(modIds);
            String sample = aiContext.length() > 200 ? aiContext.substring(0, 200) + "..." : aiContext;

            ctx.getSource().sendSuccess(() -> Component.literal("§a=== Deity Context: " + deityId + " ==="), false);
            ctx.getSource().sendSuccess(() -> Component.literal("§7Mod IDs: " + modIds), false);
            ctx.getSource().sendSuccess(() -> Component.literal("§7Context length: " + aiContext.length() + " chars"), false);
            ctx.getSource().sendSuccess(() -> Component.literal("§7Sample: " + sample), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Deity context test failed: " + e.getMessage()));
            return 0;
        }
    }

    private static int showRegistryStats(CommandContext<CommandSourceStack> ctx) {
        try {
            int items = ForgeRegistries.ITEMS.getValues().size();
            int blocks = ForgeRegistries.BLOCKS.getValues().size();
            int effects = ForgeRegistries.MOB_EFFECTS.getValues().size();
            int entities = ForgeRegistries.ENTITY_TYPES.getValues().size();
            int biomes = ForgeRegistries.BIOMES.getValues().size();

            ctx.getSource().sendSuccess(() -> Component.literal("§a=== Registry Statistics ==="), false);
            ctx.getSource().sendSuccess(() -> Component.literal("§7Items: " + items + "  Blocks: " + blocks + "  Effects: " + effects), false);
            ctx.getSource().sendSuccess(() -> Component.literal("§7Entities: " + entities + "  Biomes: " + biomes), false);

            ForgeRegistries.ITEMS.getValues().stream()
                .collect(Collectors.groupingBy(
                    i -> ForgeRegistries.ITEMS.getKey(i).getNamespace(),
                    Collectors.counting()))
                .entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(10)
                .forEach(e -> ctx.getSource().sendSuccess(() -> Component.literal("§7  " + e.getKey() + ": " + e.getValue() + " items"), false));
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Registry stats failed: " + e.getMessage()));
            return 0;
        }
    }

    private static int applyCustomEffectOverlay(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
            ResourceLocation effectId = ResourceLocationArgument.getId(ctx, "effect");
            ResourceLocation signId = ResourceLocationArgument.getId(ctx, "sign");
            int durationTicks = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "duration_ticks");

            EidolonUnchainedNetworking.sendToPlayer(player, new AttributeSignStatusIconPacket(effectId, signId, durationTicks, true));
            ctx.getSource().sendSuccess(() -> Component.literal("§aApplied custom effect overlay " + effectId + " with sign " + signId + " for " + durationTicks + " ticks to " + player.getName().getString()), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Failed to apply custom effect overlay: " + e.getMessage()));
            return 0;
        }
    }

    private static int clearCustomEffectOverlay(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
            ResourceLocation effectId = ResourceLocationArgument.getId(ctx, "effect");

            EidolonUnchainedNetworking.sendToPlayer(player, new AttributeSignStatusIconPacket(effectId, effectId, 0, true));
            ctx.getSource().sendSuccess(() -> Component.literal("§aCleared custom effect overlay " + effectId + " from " + player.getName().getString()), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Failed to clear custom effect overlay: " + e.getMessage()));
            return 0;
        }
    }

    private static int applyCustomEffectWithMagicPower(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
            ResourceLocation effectId = ResourceLocationArgument.getId(ctx, "effect");
            ResourceLocation signId = ResourceLocationArgument.getId(ctx, "sign");
            int durationTicks = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "duration_ticks");
            float amount = FloatArgumentType.getFloat(ctx, "amount");

            AttributeInstance instance = player.getAttribute(EidolonAttributes.MAGIC_POWER.get());
            if (instance == null) {
                ctx.getSource().sendFailure(Component.literal("Player has no eidolon:magic_power attribute instance"));
                return 0;
            }

            UUID modifierId = UUID.nameUUIDFromBytes(
                ("eu_debug_magic_power_" + effectId + "_" + amount).getBytes(StandardCharsets.UTF_8));

            instance.removeModifier(modifierId);
            AttributeModifier modifier = new AttributeModifier(
                modifierId,
                "eu_debug_magic_power_custom_effect",
                amount,
                AttributeModifier.Operation.ADDITION
            );

            double before = instance.getValue();
            instance.addTransientModifier(modifier);

            if (player.getServer() != null) {
                player.getServer().tell(new net.minecraft.server.TickTask(
                    player.getServer().getTickCount() + durationTicks,
                    () -> instance.removeModifier(modifierId)));
            }

            EidolonUnchainedNetworking.sendToPlayer(player, new AttributeSignStatusIconPacket(effectId, signId, durationTicks, true));
            double after = instance.getValue();

            ctx.getSource().sendSuccess(() -> Component.literal(
                "§aApplied custom effect " + effectId + " with magic power delta " + amount
                    + " for " + durationTicks + " ticks. Value: " + before + " -> " + after), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Failed to apply custom effect magic power test: " + e.getMessage()));
            return 0;
        }
    }

    // ── ritual ───────────────────────────────────────────────────────────────────

    private static int listLoadedRituals(CommandContext<CommandSourceStack> ctx) {
        try {
            var server = ctx.getSource().getServer();
            var rm = server.getRecipeManager();
            var rituals = java.util.stream.Stream.concat(
                rm.getAllRecipesFor(elucent.eidolon.registries.EidolonRecipes.COMMAND_RITUAL_TYPE.get()).stream(),
                rm.getAllRecipesFor(elucent.eidolon.registries.EidolonRecipes.RITUAL_TYPE.get()).stream()
            ).collect(Collectors.toList());

            ctx.getSource().sendSuccess(() -> Component.literal("§aLoaded rituals (" + rituals.size() + "):"), false);
            for (var r : rituals)
                ctx.getSource().sendSuccess(() -> Component.literal("§7  " + r.getId()), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error listing rituals: " + e.getMessage()));
            return 0;
        }
    }

    private static int testRitualExecution(CommandContext<CommandSourceStack> ctx) {
        try {
            ResourceLocation ritualId = ResourceLocationArgument.getId(ctx, "ritualId");
            ctx.getSource().sendSuccess(() -> Component.literal("§eRitual test for: " + ritualId + " — use /eu fates ritual <player> <id> to fire it manually."), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int diagnoseRitualAtPos(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        BlockPos pos;
        try {
            pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            source.sendFailure(Component.literal("Invalid or unloaded position: " + e.getMessage()));
            return 0;
        }

        var level = source.getLevel();
        if (!level.isLoaded(pos)) {
            source.sendFailure(Component.literal("Chunk not loaded at that position."));
            return 0;
        }

        var be = level.getBlockEntity(pos);
        if (be == null) {
            source.sendSuccess(() -> Component.literal("No block entity at " + pos.toShortString()), false);
            return 1;
        }

        source.sendSuccess(() -> Component.literal("BlockEntity: " + be.getClass().getSimpleName()), false);
        try {
            var clazz = be.getClass();
            java.lang.reflect.Field stackField = null;
            try { stackField = clazz.getDeclaredField("stack"); stackField.setAccessible(true); } catch (NoSuchFieldException ignored) {}

            if (stackField != null) {
                Object obj = stackField.get(be);
                if (obj instanceof ItemStack s)
                    source.sendSuccess(() -> Component.literal("Brazier reagent: " + s.getHoverName().getString()), false);
            }

            try {
                java.lang.reflect.Method provide = clazz.getMethod("providePedestalItems",
                    java.util.List.class, java.util.List.class);
                java.util.List<ItemStack> pedestal = new java.util.ArrayList<>();
                java.util.List<ItemStack> focus = new java.util.ArrayList<>();
                provide.invoke(be, pedestal, focus);

                source.sendSuccess(() -> Component.literal("Pedestal items:"), false);
                if (pedestal.isEmpty()) source.sendSuccess(() -> Component.literal("  <empty>"), false);
                for (var it : pedestal) source.sendSuccess(() -> Component.literal("  - " + it.getHoverName().getString()), false);

                source.sendSuccess(() -> Component.literal("Focus items:"), false);
                if (focus.isEmpty()) source.sendSuccess(() -> Component.literal("  <empty>"), false);
                for (var it : focus) source.sendSuccess(() -> Component.literal("  - " + it.getHoverName().getString()), false);
            } catch (NoSuchMethodException ignored) {
                source.sendSuccess(() -> Component.literal("Not a brazier-like tile (no providePedestalItems)."), false);
            }
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error inspecting tile: " + e.getMessage()));
            return 0;
        }
        return 1;
    }

    private static int diagnoseRitualSystem(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> Component.literal("§aRitual system: use '/eu debug ritual diagnose <pos>' to inspect a specific brazier."), false);
        return 1;
    }

    // ── reputation / progression ─────────────────────────────────────────────────

    private static int debugPlayerReputation(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
            ResourceLocation deityId = ResourceLocationArgument.getId(ctx, "deity");
            com.bluelotuscoding.eidolonunchained.deity.DatapackDeity deity =
                com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager.getDeity(deityId);
            if (deity == null) {
                ctx.getSource().sendFailure(Component.literal("Unknown deity: " + deityId));
                return 0;
            }
            double rep = deity.getPlayerReputation(player);
            ctx.getSource().sendSuccess(() -> Component.literal("§7" + player.getName().getString() + " → " + deityId.getPath() + ": §e" + rep), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int showPlayerMana(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
            ISoul soul = player.getCapability(ISoul.INSTANCE).orElse(null);
            if (soul == null) {
                ctx.getSource().sendFailure(Component.literal("Player has no Eidolon soul capability: " + player.getName().getString()));
                return 0;
            }

            float magic = soul.getMagic();
            float maxMagic = soul.getMaxMagic();
            float percent = maxMagic > 0 ? (magic / maxMagic) * 100.0f : 0.0f;

            ctx.getSource().sendSuccess(() -> Component.literal(String.format(
                "§7%s mana: §b%.2f§7/§3%.2f §8(%.1f%%)",
                player.getName().getString(), magic, maxMagic, percent)), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int setPlayerMana(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
            float value = FloatArgumentType.getFloat(ctx, "value");
            return updatePlayerMana(ctx, player, soul -> soul.setMagic(value),
                String.format("Set %s mana to %.2f", player.getName().getString(), value));
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int restorePlayerMana(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
            float amount = FloatArgumentType.getFloat(ctx, "amount");
            return updatePlayerMana(ctx, player, soul -> soul.giveMagic(amount),
                String.format("Restored %.2f mana to %s", amount, player.getName().getString()));
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int setPlayerMaxMana(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
            float value = FloatArgumentType.getFloat(ctx, "value");
            return updatePlayerMana(ctx, player, soul -> soul.setMaxMagic(value),
                String.format("Set %s max mana to %.2f", player.getName().getString(), value));
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int increasePlayerMaxMana(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
            float amount = FloatArgumentType.getFloat(ctx, "amount");
            return updatePlayerMana(ctx, player, soul -> soul.setMaxMagic(soul.getMaxMagic() + amount),
                String.format("Increased %s max mana by %.2f", player.getName().getString(), amount));
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int updatePlayerMana(
        CommandContext<CommandSourceStack> ctx,
        ServerPlayer player,
        java.util.function.Consumer<ISoul> mutator,
        String successMessage
    ) {
        ISoul soul = player.getCapability(ISoul.INSTANCE).orElse(null);
        if (soul == null) {
            ctx.getSource().sendFailure(Component.literal("Player has no Eidolon soul capability: " + player.getName().getString()));
            return 0;
        }

        mutator.accept(soul);
        Networking.sendToTracking(player.level(), player.getOnPos(), new SoulUpdatePacket(player));

        float magic = soul.getMagic();
        float maxMagic = soul.getMaxMagic();
        ctx.getSource().sendSuccess(() -> Component.literal(String.format(
            "§a%s §7→ §b%.2f§7/§3%.2f", successMessage, magic, maxMagic)), true);
        return 1;
    }

    private static int debugPlayerProgression(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
            ResourceLocation deityId = ResourceLocationArgument.getId(ctx, "deity");
            com.bluelotuscoding.eidolonunchained.ai.PlayerContextTracker.EnhancedPlayerContext pctx =
                com.bluelotuscoding.eidolonunchained.ai.PlayerContextTracker.getContext(player.getUUID());
            if (pctx == null) {
                ctx.getSource().sendSuccess(() -> Component.literal("§7No context tracked for " + player.getName().getString()), false);
                return 1;
            }
            ctx.getSource().sendSuccess(() -> Component.literal("§aProgression for " + player.getName().getString() + " / " + deityId + ":"), false);
            ctx.getSource().sendSuccess(() -> Component.literal("§7Unlocked tiers: " + pctx.unlockedProgressions), false);
            ctx.getSource().sendSuccess(() -> Component.literal("§7Completed rituals: " + pctx.completedRituals), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int forceProgressionCheck(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
            ResourceLocation deityId = ResourceLocationArgument.getId(ctx, "deity");
            ctx.getSource().sendSuccess(() -> Component.literal("§eForce-progression check for " + player.getName().getString() + " / " + deityId + " — not yet wired to progression handler."), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int debugTierTracking(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
            ResourceLocation deityId = ResourceLocationArgument.getId(ctx, "deity");
            com.bluelotuscoding.eidolonunchained.deity.DatapackDeity deity =
                com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager.getDeity(deityId);
            if (deity == null) {
                ctx.getSource().sendFailure(Component.literal("Unknown deity: " + deityId));
                return 0;
            }
            double rep = deity.getPlayerReputation(player);
            // Find which stage title applies at this rep level
            String stage = "unknown";
            double bestThreshold = Double.NEGATIVE_INFINITY;
            for (var entry : deity.getProgressionStages().entrySet()) {
                @SuppressWarnings("unchecked")
                var stageMap = (java.util.Map<?, ?>) entry.getValue();
                Object threshObj = stageMap.get("threshold");
                if (threshObj instanceof Number thresh) {
                    double t = thresh.doubleValue();
                    if (rep >= t && t > bestThreshold) {
                        bestThreshold = t;
                        stage = entry.getKey();
                    }
                }
            }
            final String stageFinal = stage;
            ctx.getSource().sendSuccess(() -> Component.literal("§aPlayer: " + player.getName().getString()), false);
            ctx.getSource().sendSuccess(() -> Component.literal("§7Deity: " + deityId), false);
            ctx.getSource().sendSuccess(() -> Component.literal("§7Reputation: §e" + rep), false);
            ctx.getSource().sendSuccess(() -> Component.literal("§7Stage: §b" + stageFinal), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    // ── facts ────────────────────────────────────────────────────────────────────

    private static int grantFact(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
            ResourceLocation fact = ResourceLocationArgument.getId(ctx, "fact");
            com.bluelotuscoding.eidolonunchained.ai.PlayerContextTracker.EnhancedPlayerContext pctx =
                com.bluelotuscoding.eidolonunchained.ai.PlayerContextTracker.getContext(player.getUUID());
            if (pctx != null) pctx.unlockedProgressions.add(fact.toString());
            ctx.getSource().sendSuccess(() -> Component.literal("Granted fact '" + fact + "' to " + player.getName().getString()), true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int revokeFact(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
            ResourceLocation fact = ResourceLocationArgument.getId(ctx, "fact");
            com.bluelotuscoding.eidolonunchained.ai.PlayerContextTracker.EnhancedPlayerContext pctx =
                com.bluelotuscoding.eidolonunchained.ai.PlayerContextTracker.getContext(player.getUUID());
            if (pctx != null) pctx.unlockedProgressions.remove(fact.toString());
            ctx.getSource().sendSuccess(() -> Component.literal("Revoked fact '" + fact + "' from " + player.getName().getString()), true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int listFacts(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
            com.bluelotuscoding.eidolonunchained.ai.PlayerContextTracker.EnhancedPlayerContext pctx =
                com.bluelotuscoding.eidolonunchained.ai.PlayerContextTracker.getContext(player.getUUID());
            if (pctx == null || pctx.unlockedProgressions.isEmpty()) {
                ctx.getSource().sendSuccess(() -> Component.literal(player.getName().getString() + " has no granted facts."), false);
                return 1;
            }
            ctx.getSource().sendSuccess(() -> Component.literal("Facts for " + player.getName().getString() + ":"), false);
            for (String f : pctx.unlockedProgressions)
                ctx.getSource().sendSuccess(() -> Component.literal("  - " + f), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    // ── attribute ────────────────────────────────────────────────────────────────

    private static int getEntityAttribute(CommandContext<CommandSourceStack> ctx) {
        try {
            Entity raw = EntityArgument.getEntity(ctx, "entity");
            if (!(raw instanceof LivingEntity living)) {
                ctx.getSource().sendFailure(Component.literal("Entity is not a living entity."));
                return 0;
            }
            ResourceLocation attrId = ResourceLocationArgument.getId(ctx, "attribute");
            var attrType = net.minecraftforge.registries.ForgeRegistries.ATTRIBUTES.getValue(attrId);
            if (attrType == null) {
                ctx.getSource().sendFailure(Component.literal("Unknown attribute: " + attrId));
                return 0;
            }
            AttributeInstance instance = living.getAttribute(attrType);
            if (instance == null) {
                ctx.getSource().sendFailure(Component.literal(living.getName().getString() + " does not have attribute " + attrId));
                return 0;
            }
            ctx.getSource().sendSuccess(() -> Component.literal(
                "§a=== " + attrId + " on " + living.getName().getString() + " ==="), false);
            ctx.getSource().sendSuccess(() -> Component.literal(
                "§7Base: §e" + instance.getBaseValue()), false);
            ctx.getSource().sendSuccess(() -> Component.literal(
                "§7Effective: §b" + instance.getValue()), false);
            var modifiers = instance.getModifiers();
            if (modifiers.isEmpty()) {
                ctx.getSource().sendSuccess(() -> Component.literal("§7No modifiers."), false);
            } else {
                ctx.getSource().sendSuccess(() -> Component.literal("§7Modifiers (" + modifiers.size() + "):"), false);
                for (AttributeModifier mod : modifiers) {
                    ctx.getSource().sendSuccess(() -> Component.literal(
                        "§8  [" + mod.getOperation().name() + "] §7" + mod.getName()
                            + " §8(" + mod.getId() + ")§7: §e" + mod.getAmount()), false);
                }
            }
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int setEntityAttributeBase(CommandContext<CommandSourceStack> ctx) {
        try {
            Entity raw = EntityArgument.getEntity(ctx, "entity");
            if (!(raw instanceof LivingEntity living)) {
                ctx.getSource().sendFailure(Component.literal("Entity is not a living entity."));
                return 0;
            }
            ResourceLocation attrId = ResourceLocationArgument.getId(ctx, "attribute");
            double value = DoubleArgumentType.getDouble(ctx, "value");
            var attrType = net.minecraftforge.registries.ForgeRegistries.ATTRIBUTES.getValue(attrId);
            if (attrType == null) {
                ctx.getSource().sendFailure(Component.literal("Unknown attribute: " + attrId));
                return 0;
            }
            AttributeInstance instance = living.getAttribute(attrType);
            if (instance == null) {
                ctx.getSource().sendFailure(Component.literal(living.getName().getString() + " does not have attribute " + attrId));
                return 0;
            }
            double before = instance.getBaseValue();
            instance.setBaseValue(value);
            ctx.getSource().sendSuccess(() -> Component.literal(
                "§a" + living.getName().getString() + " " + attrId.getPath()
                    + " base: §e" + before + " §7→ §b" + value), true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int addEntityAttributeModifier(CommandContext<CommandSourceStack> ctx) {
        try {
            Entity raw = EntityArgument.getEntity(ctx, "entity");
            if (!(raw instanceof LivingEntity living)) {
                ctx.getSource().sendFailure(Component.literal("Entity is not a living entity."));
                return 0;
            }
            ResourceLocation attrId = ResourceLocationArgument.getId(ctx, "attribute");
            double amount = DoubleArgumentType.getDouble(ctx, "amount");
            var attrType = net.minecraftforge.registries.ForgeRegistries.ATTRIBUTES.getValue(attrId);
            if (attrType == null) {
                ctx.getSource().sendFailure(Component.literal("Unknown attribute: " + attrId));
                return 0;
            }
            AttributeInstance instance = living.getAttribute(attrType);
            if (instance == null) {
                ctx.getSource().sendFailure(Component.literal(living.getName().getString() + " does not have attribute " + attrId));
                return 0;
            }
            UUID modId = UUID.nameUUIDFromBytes(
                ("eu_debug_attr_" + attrId + "_" + amount).getBytes(StandardCharsets.UTF_8));
            instance.removeModifier(modId);
            AttributeModifier modifier = new AttributeModifier(modId,
                "eu_debug_attr_" + attrId.getPath(), amount, AttributeModifier.Operation.ADDITION);
            instance.addTransientModifier(modifier);
            double effective = instance.getValue();
            ctx.getSource().sendSuccess(() -> Component.literal(
                "§aAdded transient §e" + amount + " §7ADDITION to §b" + attrId.getPath()
                    + " §7on " + living.getName().getString() + ". Effective: §b" + effective), true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int setDivineResistanceTagValue(CommandContext<CommandSourceStack> ctx) {
        try {
            Entity raw = EntityArgument.getEntity(ctx, "entity");
            if (!(raw instanceof LivingEntity living)) {
                ctx.getSource().sendFailure(Component.literal("Entity is not a living entity."));
                return 0;
            }

            String key = StringArgumentType.getString(ctx, "key").trim();
            double value = DoubleArgumentType.getDouble(ctx, "value");
            if (key.isEmpty()) {
                ctx.getSource().sendFailure(Component.literal("Resistance key cannot be empty."));
                return 0;
            }

            CompoundTag persisted = living.getPersistentData();
            CompoundTag typed = persisted.contains(TAG_DIVINE_RESISTANCES, CompoundTag.TAG_COMPOUND)
                ? persisted.getCompound(TAG_DIVINE_RESISTANCES)
                : new CompoundTag();
            typed.putDouble(key, value);
            persisted.put(TAG_DIVINE_RESISTANCES, typed);

            ctx.getSource().sendSuccess(() -> Component.literal(
                "§aSet " + living.getName().getString() + " tag §b" + TAG_DIVINE_RESISTANCES + "." + key
                    + " §7= §e" + value), true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int getDivineResistanceTagValue(CommandContext<CommandSourceStack> ctx) {
        try {
            Entity raw = EntityArgument.getEntity(ctx, "entity");
            if (!(raw instanceof LivingEntity living)) {
                ctx.getSource().sendFailure(Component.literal("Entity is not a living entity."));
                return 0;
            }

            String key = StringArgumentType.getString(ctx, "key").trim();
            if (key.isEmpty()) {
                ctx.getSource().sendFailure(Component.literal("Resistance key cannot be empty."));
                return 0;
            }

            CompoundTag persisted = living.getPersistentData();
            if (!persisted.contains(TAG_DIVINE_RESISTANCES, CompoundTag.TAG_COMPOUND)) {
                ctx.getSource().sendSuccess(() -> Component.literal(
                    "§e" + living.getName().getString() + " has no §b" + TAG_DIVINE_RESISTANCES + "§e compound."), false);
                return 1;
            }

            CompoundTag typed = persisted.getCompound(TAG_DIVINE_RESISTANCES);
            if (!typed.contains(key, CompoundTag.TAG_ANY_NUMERIC)) {
                ctx.getSource().sendSuccess(() -> Component.literal(
                    "§e" + living.getName().getString() + " has no value for key §b" + key), false);
                return 1;
            }

            double value = typed.getDouble(key);
            ctx.getSource().sendSuccess(() -> Component.literal(
                "§a" + TAG_DIVINE_RESISTANCES + "." + key + " on " + living.getName().getString() + " = §e" + value), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int clearDivineResistanceTagValue(CommandContext<CommandSourceStack> ctx) {
        try {
            Entity raw = EntityArgument.getEntity(ctx, "entity");
            if (!(raw instanceof LivingEntity living)) {
                ctx.getSource().sendFailure(Component.literal("Entity is not a living entity."));
                return 0;
            }

            String key = StringArgumentType.getString(ctx, "key").trim();
            if (key.isEmpty()) {
                ctx.getSource().sendFailure(Component.literal("Resistance key cannot be empty."));
                return 0;
            }

            CompoundTag persisted = living.getPersistentData();
            if (!persisted.contains(TAG_DIVINE_RESISTANCES, CompoundTag.TAG_COMPOUND)) {
                ctx.getSource().sendSuccess(() -> Component.literal(
                    "§eNothing to clear; no §b" + TAG_DIVINE_RESISTANCES + "§e compound found."), false);
                return 1;
            }

            CompoundTag typed = persisted.getCompound(TAG_DIVINE_RESISTANCES);
            typed.remove(key);
            persisted.put(TAG_DIVINE_RESISTANCES, typed);

            ctx.getSource().sendSuccess(() -> Component.literal(
                "§aCleared key §b" + key + " §afrom §b" + TAG_DIVINE_RESISTANCES + " §aon " + living.getName().getString()), true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    // ── clear rewards ─────────────────────────────────────────────────────────────

    private static int clearPlayerRewards(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
            ResourceLocation deityId = ResourceLocationArgument.getId(ctx, "deity");
            // Clear research-trigger tracking for this player; per-deity reward clearing
            // requires a dedicated method — use clearTriggeredResearchTracking for now.
            com.bluelotuscoding.eidolonunchained.ai.PlayerContextTracker.clearTriggeredResearchTracking(player);
            com.bluelotuscoding.eidolonunchained.research.tasks.KillEntitiesTask.clearProgress(player);
            com.bluelotuscoding.eidolonunchained.research.tasks.CraftItemsTask.clearProgress(player);
            com.bluelotuscoding.eidolonunchained.research.tasks.UseRitualTask.clearProgress(player);
            ctx.getSource().sendSuccess(() -> Component.literal("Cleared reward tracking for " + player.getName().getString() + " (deity: " + deityId + ")"), true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int clearAllPlayerRewards(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
            com.bluelotuscoding.eidolonunchained.ai.PlayerContextTracker.clearTriggeredResearchTracking(player);
            com.bluelotuscoding.eidolonunchained.research.tasks.KillEntitiesTask.clearProgress(player);
            com.bluelotuscoding.eidolonunchained.research.tasks.CraftItemsTask.clearProgress(player);
            com.bluelotuscoding.eidolonunchained.research.tasks.UseRitualTask.clearProgress(player);
            ctx.getSource().sendSuccess(() -> Component.literal("Cleared all reward tracking for " + player.getName().getString()), true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
}
