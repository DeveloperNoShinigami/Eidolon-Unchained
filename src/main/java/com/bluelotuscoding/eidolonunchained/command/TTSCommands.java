package com.bluelotuscoding.eidolonunchained.command;

import com.bluelotuscoding.eidolonunchained.ai.TTSManager;
// import com.bluelotuscoding.eidolonunchained.integration.player2ai.Player2TTSClient;
// import com.mojang.brigadier.CommandDispatcher;
// import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
// import org.apache.logging.log4j.LogManager;
// import org.apache.logging.log4j.Logger;

/**
 * Commands for managing TTS (Text-To-Speech) settings for deity interactions
 */
public class TTSCommands {
    // private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Build the TTS command node for integration into UnifiedCommands
     */
    public static LiteralArgumentBuilder<CommandSourceStack> buildNode() {
        return Commands.literal("tts")
                    .then(Commands.literal("enable")
                        .executes(TTSCommands::enableTTS))
                    .then(Commands.literal("disable")
                        .executes(TTSCommands::disableTTS))
                    .then(Commands.literal("status")
                        .executes(TTSCommands::showTTSStatus))
                    .then(Commands.literal("test")
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                            .executes(TTSCommands::testTTS)))
                    .then(Commands.literal("funding")
                        .then(Commands.literal("player-only")
                            .executes(TTSCommands::setPlayerOnlyFunding))
                        .then(Commands.literal("server-only")
                            .executes(TTSCommands::setServerOnlyFunding))
                        .then(Commands.literal("player-first")
                            .executes(TTSCommands::setPlayerFirstFunding))
                        .then(Commands.literal("status")
                            .executes(TTSCommands::showFundingStatus)))
                    .then(Commands.literal("voice")
                        .then(Commands.argument("voice", StringArgumentType.word())
                            .executes(TTSCommands::setVoice))
                        .then(Commands.literal("auto")
                            .executes(TTSCommands::setAutoVoice))
                        .then(Commands.literal("list")
                            .executes(TTSCommands::listVoices)))
                    .then(Commands.literal("volume")
                        .then(Commands.argument("volume", FloatArgumentType.floatArg(0.0f, 2.0f))
                            .executes(TTSCommands::setVolume)))
                    .then(Commands.literal("speed")
                        .then(Commands.argument("speed", FloatArgumentType.floatArg(0.5f, 2.0f))
                            .executes(TTSCommands::setSpeed)))
                    .then(Commands.literal("mode")
                        .then(Commands.literal("hybrid")
                            .executes(TTSCommands::setHybridMode))
                        .then(Commands.literal("tts-only")
                            .executes(TTSCommands::setTTSOnlyMode)))
                    .then(Commands.literal("stats")
                        .executes(TTSCommands::showStats)
                        .requires(source -> source.hasPermission(2))) // OP only
                    .then(Commands.literal("reset-stats")
                        .executes(TTSCommands::resetStats)
                        .requires(source -> source.hasPermission(2))); // OP only
    }

    private static int enableTTS(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }

        TTSManager.getInstance().setTTSEnabled(player, true);
        context.getSource().sendSuccess(() -> Component.literal("§a✓ TTS enabled! Deities will now speak to you."), false);

            if (!TTSManager.getInstance().isTTSAvailable(player)) {
                context.getSource().sendSuccess(() -> Component.literal("§eNote: No TTS funding configured. Link Player2: /eidolon-unchained player2ai login device"), false);
        }

        return 1;
    }

    private static int disableTTS(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }

        TTSManager.getInstance().setTTSEnabled(player, false);
        context.getSource().sendSuccess(() -> Component.literal("§c✗ TTS disabled. Deities will use text only."), false);
        return 1;
    }

    private static int showTTSStatus(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }

        TTSManager.TTSSettings settings = TTSManager.getInstance().getPlayerSettings(player);
        boolean available = TTSManager.getInstance().isTTSAvailable(player);
        String lastPath = TTSManager.getInstance().getLastUsedPath(player);

        context.getSource().sendSuccess(() -> Component.literal("§6=== TTS Status ==="), false);
        context.getSource().sendSuccess(() -> Component.literal("§7Enabled: " + (settings.enabled ? "§a✓" : "§c✗")), false);
        context.getSource().sendSuccess(() -> Component.literal("§7Available: " + (available ? "§a✓" : "§c✗")), false);
        context.getSource().sendSuccess(() -> Component.literal("§7Voice: §e" + settings.preferredVoice), false);
        context.getSource().sendSuccess(() -> Component.literal("§7Volume: §e" + settings.volume), false);
        context.getSource().sendSuccess(() -> Component.literal("§7Speed: §e" + settings.speed), false);
        if (lastPath != null && !lastPath.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("§7Last Path: §e" + lastPath), false);
        }

        String fundingMode;
        if (settings.usePlayerFunding && settings.allowServerFallback) {
            fundingMode = "§ePlayer-first with server fallback";
        } else if (settings.usePlayerFunding) {
            fundingMode = "§bPlayer-only";
        } else if (settings.allowServerFallback) {
            fundingMode = "§dServer-only";
        } else {
            fundingMode = "§cDisabled";
        }
        context.getSource().sendSuccess(() -> Component.literal("§7Funding: " + fundingMode), false);

        // Show Player2 p2Key presence for web fallback visibility (per-player and server)
        boolean hasPerPlayer = com.bluelotuscoding.eidolonunchained.integration.player2ai.Player2AuthManager.getCachedP2Key(player) != null;
        context.getSource().sendSuccess(() -> Component.literal("§7Player2 p2Key (you): " + (hasPerPlayer ? "§aYES" : "§cNO")), false);
        try {
            String p2 = com.bluelotuscoding.eidolonunchained.config.APIKeyManager.getAPIKey("player2ai");
            boolean hasKey = p2 != null && !p2.trim().isEmpty();
            context.getSource().sendSuccess(() -> Component.literal("§7Server p2Key (legacy): " + (hasKey ? "§aYES" : "§cNO")), false);
        } catch (Exception ignored) {}

        return 1;
    }


    private static int testTTS(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }

        String text = StringArgumentType.getString(context, "text");

        if (!TTSManager.getInstance().isTTSAvailable(player)) {
            context.getSource().sendFailure(Component.literal("§cTTS not available. Link Player2: /eidolon-unchained player2ai login device"));
            return 0;
        }

        context.getSource().sendSuccess(() -> Component.literal("§7Testing TTS: \"" + text + "\""), false);

        TTSManager.getInstance().generateAndSendTTS(player, text, "test_deity")
            .thenAccept(success -> {
                if (success) {
                    player.sendSystemMessage(Component.literal("§a✓ TTS test completed"));
                } else {
                    player.sendSystemMessage(Component.literal("§c✗ TTS test failed"));
                }
            });

        return 1;
    }

    private static int setPlayerOnlyFunding(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }

        TTSManager.getInstance().setPlayerFundingPreference(player, true, false);
        context.getSource().sendSuccess(() -> Component.literal("§b✓ TTS funding set to player-only. You'll pay for your own TTS requests."), false);
        return 1;
    }

    private static int setServerOnlyFunding(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }

        TTSManager.getInstance().setPlayerFundingPreference(player, false, true);
        context.getSource().sendSuccess(() -> Component.literal("§d✓ TTS funding set to server-only. The server will pay for your TTS requests."), false);
        return 1;
    }

    private static int setPlayerFirstFunding(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }

        TTSManager.getInstance().setPlayerFundingPreference(player, true, true);
        context.getSource().sendSuccess(() -> Component.literal("§e✓ TTS funding set to player-first with server fallback. You'll pay when possible, server covers when needed."), false);
        return 1;
    }

    private static int showFundingStatus(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }

        TTSManager.TTSSettings settings = TTSManager.getInstance().getPlayerSettings(player);

        context.getSource().sendSuccess(() -> Component.literal("§6=== TTS Funding Status ==="), false);
        context.getSource().sendSuccess(() -> Component.literal("§7Player funding: " + (settings.usePlayerFunding ? "§a✓" : "§c✗")), false);
        context.getSource().sendSuccess(() -> Component.literal("§7Server fallback: " + (settings.allowServerFallback ? "§a✓" : "§c✗")), false);

        // Show availability
        // TODO: Check actual availability from Player2 client and server config
        context.getSource().sendSuccess(() -> Component.literal("§7Player2 App: §eChecking..."), false);
        context.getSource().sendSuccess(() -> Component.literal("§7Server API key: §eChecking..."), false);

        return 1;
    }

    private static int setVoice(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }

        String voice = StringArgumentType.getString(context, "voice");
        TTSManager.TTSSettings settings = TTSManager.getInstance().getPlayerSettings(player);
        settings.preferredVoice = voice;
        TTSManager.getInstance().updatePlayerSettings(player, settings);

        context.getSource().sendSuccess(() -> Component.literal("§a✓ Voice set to: " + voice), false);
        return 1;
    }

    private static int setAutoVoice(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }

        TTSManager.TTSSettings settings = TTSManager.getInstance().getPlayerSettings(player);
        settings.preferredVoice = "auto";
        TTSManager.getInstance().updatePlayerSettings(player, settings);

        context.getSource().sendSuccess(() -> Component.literal("§a✓ Voice set to auto. Deities will use their characteristic voices."), false);
        return 1;
    }

    private static int listVoices(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("§6=== Available Voices ==="), false);
        // Try dynamic list from provider (if web API is active)
        TTSManager.getInstance().listVoices().thenAccept(voices -> {
            if (voices != null && !voices.isEmpty()) {
                context.getSource().sendSuccess(() -> Component.literal("§7auto §8- Use deity-appropriate voice"), false);
                for (var v : voices) {
                    String line = String.format("§7%s", v.id);
                    context.getSource().sendSuccess(() -> Component.literal(line), false);
                }
            } else {
                // Fallback static list
                context.getSource().sendSuccess(() -> Component.literal("§7auto §8- Use deity-appropriate voice"), false);
                context.getSource().sendSuccess(() -> Component.literal("§7male-deep-1 §8- Deep, ominous voice"), false);
                context.getSource().sendSuccess(() -> Component.literal("§7female-warm-1 §8- Warm, comforting voice"), false);
                context.getSource().sendSuccess(() -> Component.literal("§7male-intense-1 §8- Intense, passionate voice"), false);
                context.getSource().sendSuccess(() -> Component.literal("§7female-flowing-1 §8- Flowing, calm voice"), false);
                context.getSource().sendSuccess(() -> Component.literal("§7male-steady-1 §8- Steady, grounded voice"), false);
                context.getSource().sendSuccess(() -> Component.literal("§7female-light-1 §8- Light, airy voice"), false);
                context.getSource().sendSuccess(() -> Component.literal("§7female-natural-1 §8- Natural, earthy voice"), false);
                context.getSource().sendSuccess(() -> Component.literal("§7male-whisper-1 §8- Whispered, eerie voice"), false);
                context.getSource().sendSuccess(() -> Component.literal("§7neutral-1 §8- Default neutral voice"), false);
            }
        }).exceptionally(err -> {
            // On error, show fallback list
            context.getSource().sendSuccess(() -> Component.literal("§7auto §8- Use deity-appropriate voice"), false);
            context.getSource().sendSuccess(() -> Component.literal("§7male-deep-1 §8- Deep, ominous voice"), false);
            context.getSource().sendSuccess(() -> Component.literal("§7female-warm-1 §8- Warm, comforting voice"), false);
            context.getSource().sendSuccess(() -> Component.literal("§7male-intense-1 §8- Intense, passionate voice"), false);
            context.getSource().sendSuccess(() -> Component.literal("§7female-flowing-1 §8- Flowing, calm voice"), false);
            context.getSource().sendSuccess(() -> Component.literal("§7male-steady-1 §8- Steady, grounded voice"), false);
            context.getSource().sendSuccess(() -> Component.literal("§7female-light-1 §8- Light, airy voice"), false);
            context.getSource().sendSuccess(() -> Component.literal("§7female-natural-1 §8- Natural, earthy voice"), false);
            context.getSource().sendSuccess(() -> Component.literal("§7male-whisper-1 §8- Whispered, eerie voice"), false);
            context.getSource().sendSuccess(() -> Component.literal("§7neutral-1 §8- Default neutral voice"), false);
            return null;
        });
        return 1;
    }

    private static int setVolume(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }

        float volume = FloatArgumentType.getFloat(context, "volume");
        TTSManager.TTSSettings settings = TTSManager.getInstance().getPlayerSettings(player);
        settings.volume = volume;
        TTSManager.getInstance().updatePlayerSettings(player, settings);

        context.getSource().sendSuccess(() -> Component.literal("§a✓ Volume set to: " + volume), false);
        return 1;
    }

    private static int setSpeed(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }

        float speed = FloatArgumentType.getFloat(context, "speed");
        TTSManager.TTSSettings settings = TTSManager.getInstance().getPlayerSettings(player);
        settings.speed = speed;
        TTSManager.getInstance().updatePlayerSettings(player, settings);

        context.getSource().sendSuccess(() -> Component.literal("§a✓ Speed set to: " + speed), false);
        return 1;
    }

    private static int showStats(CommandContext<CommandSourceStack> context) {
        TTSManager.TTSStats stats = TTSManager.getInstance().getStats();

        context.getSource().sendSuccess(() -> Component.literal("§6=== TTS Statistics ==="), false);
        context.getSource().sendSuccess(() -> Component.literal("§7Player-funded requests: §e" + stats.playerFundedRequests), false);
        context.getSource().sendSuccess(() -> Component.literal("§7Server-funded requests: §e" + stats.serverFundedRequests), false);
        context.getSource().sendSuccess(() -> Component.literal("§7Failed requests: §c" + stats.failedRequests), false);
        context.getSource().sendSuccess(() -> Component.literal("§7Total characters: §e" + stats.totalCharacters), false);

        int totalRequests = stats.playerFundedRequests + stats.serverFundedRequests;
        if (totalRequests > 0) {
            float playerPercentage = (stats.playerFundedRequests * 100.0f) / totalRequests;
            context.getSource().sendSuccess(() -> Component.literal("§7Player funding rate: §e" + String.format("%.1f%%", playerPercentage)), false);
        }

        return 1;
    }

    private static int resetStats(CommandContext<CommandSourceStack> context) {
        TTSManager.getInstance().resetStats();
        context.getSource().sendSuccess(() -> Component.literal("§a✓ TTS statistics reset"), false);
        return 1;
    }
    
    private static int setHybridMode(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }

        TTSManager.TTSSettings settings = TTSManager.getInstance().getPlayerSettings(player);
        settings.ttsOnly = false;
        
        context.getSource().sendSuccess(() -> Component.literal("§a✓ Hybrid Mode enabled: LLM generates text + TTS speaks it"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e⚠ Warning: This uses BOTH LLM and TTS API calls (double billing)"), false);
        return 1;
    }
    
    private static int setTTSOnlyMode(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }

        TTSManager.TTSSettings settings = TTSManager.getInstance().getPlayerSettings(player);
        settings.ttsOnly = true;
        
        // Automatically enable TTS if it's not already enabled
        if (!settings.enabled) {
            settings.enabled = true;
            context.getSource().sendSuccess(() -> Component.literal("§a✓ TTS automatically enabled for TTS-only mode"), false);
        }
        
        context.getSource().sendSuccess(() -> Component.literal("§a✓ TTS-Only Mode enabled: Skip LLM, direct to TTS"), false);
        context.getSource().sendSuccess(() -> Component.literal("§2💰 Money saver: Only TTS API calls, no LLM charges!"), false);
        return 1;
    }
}