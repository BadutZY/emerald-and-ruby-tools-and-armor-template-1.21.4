package com.example.emeraldmod.command;

import com.example.emeraldmod.world.gen.InstantRetrofitSystem;
import com.example.emeraldmod.world.gen.OreRetrofitGenerator;
import com.example.emeraldmod.world.gen.OreRetrofitState;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

/**
 * Command untuk manual retrofit chunks
 * 🔧 FIXED: Error fixes dan removed duplicate progress (karena sudah ada loading screen)
 */
public class RetrofitCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                CommandRegistryAccess registryAccess,
                                CommandManager.RegistrationEnvironment environment) {

        dispatcher.register(CommandManager.literal("emeraldmod")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("retrofit")
                        .then(CommandManager.argument("radius", IntegerArgumentType.integer(1, 50))
                                .executes(RetrofitCommand::retrofitArea))
                        .executes(RetrofitCommand::retrofitAreaDefault) // 🔧 FIX: Added default handler
                )
                .then(CommandManager.literal("retrofit-here")
                        .executes(RetrofitCommand::retrofitHere))
                .then(CommandManager.literal("retrofit-verify")
                        .executes(RetrofitCommand::verifyOres))
                .then(CommandManager.literal("retrofit-all")
                        .executes(RetrofitCommand::retrofitAllExisting))
                .then(CommandManager.literal("retrofit-stats")
                        .executes(RetrofitCommand::showRetrofitStats))
                .then(CommandManager.literal("retrofit-reset")
                        .executes(RetrofitCommand::resetRetrofit))
        );
    }

    /**
     * Verify ores di sekitar player (count blocks)
     */
    private static int verifyOres(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();

        BlockPos playerPos = BlockPos.ofFloored(source.getPosition());

        source.sendFeedback(() -> Text.literal("🔍 Scanning for Ruby Ores in 50 block radius...")
                .formatted(Formatting.YELLOW), false);

        int rubyOreCount = 0;
        int deepslateRubyOreCount = 0;
        int netherRubyOreCount = 0;
        int rubyDebrisCount = 0;

        int scanRadius = 50;

        for (int x = -scanRadius; x <= scanRadius; x++) {
            for (int y = -scanRadius; y <= scanRadius; y++) {
                for (int z = -scanRadius; z <= scanRadius; z++) {
                    BlockPos pos = playerPos.add(x, y, z);

                    try {
                        net.minecraft.block.BlockState state = world.getBlockState(pos);

                        if (state.getBlock() == com.example.emeraldmod.block.ModBlocks.RUBY_ORE) {
                            rubyOreCount++;
                        } else if (state.getBlock() == com.example.emeraldmod.block.ModBlocks.DEEPSLATE_RUBY_ORE) {
                            deepslateRubyOreCount++;
                        } else if (state.getBlock() == com.example.emeraldmod.block.ModBlocks.NETHER_RUBY_ORE) {
                            netherRubyOreCount++;
                        } else if (state.getBlock() == com.example.emeraldmod.block.ModBlocks.RUBY_DEBRIS) {
                            rubyDebrisCount++;
                        }
                    } catch (Exception e) {
                        // Skip
                    }
                }
            }
        }

        int totalOres = rubyOreCount + deepslateRubyOreCount + netherRubyOreCount + rubyDebrisCount;

        int finalRubyOreCount = rubyOreCount;
        int finalDeepslateRubyOreCount = deepslateRubyOreCount;
        int finalNetherRubyOreCount = netherRubyOreCount;
        int finalRubyDebrisCount = rubyDebrisCount;
        int finalTotalOres = totalOres;

        source.sendFeedback(() -> Text.literal("=== Ruby Ore Scan Results ===")
                .formatted(Formatting.GOLD), false);
        source.sendFeedback(() -> Text.literal("Scan radius: 50 blocks")
                .formatted(Formatting.GRAY), false);
        source.sendFeedback(() -> Text.literal("Ruby Ore: " + finalRubyOreCount)
                .formatted(Formatting.AQUA), false);
        source.sendFeedback(() -> Text.literal("Deepslate Ruby Ore: " + finalDeepslateRubyOreCount)
                .formatted(Formatting.AQUA), false);
        source.sendFeedback(() -> Text.literal("Nether Ruby Ore: " + finalNetherRubyOreCount)
                .formatted(Formatting.AQUA), false);
        source.sendFeedback(() -> Text.literal("Ruby Debris: " + finalRubyDebrisCount)
                .formatted(Formatting.AQUA), false);
        source.sendFeedback(() -> Text.literal("Total: " + finalTotalOres + " Ruby Ores found")
                .formatted(finalTotalOres > 0 ? Formatting.GREEN : Formatting.RED), false);

        if (totalOres == 0) {
            source.sendFeedback(() -> Text.literal("⚠ No ores found! Try: /emeraldmod retrofit-here")
                    .formatted(Formatting.RED), false);
        }

        return totalOres;
    }

    /**
     * Retrofit chunk di posisi player sekarang (instant)
     */
    private static int retrofitHere(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();

        BlockPos playerPos = BlockPos.ofFloored(source.getPosition());
        ChunkPos chunkPos = new ChunkPos(playerPos);

        source.sendFeedback(() -> Text.literal("⚡ Retrofitting current chunk...")
                .formatted(Formatting.YELLOW), false);

        try {
            WorldChunk chunk = world.getChunk(chunkPos.x, chunkPos.z);

            // Force retrofit dengan reset flag dulu
            OreRetrofitState state = OreRetrofitState.get(world.getServer(), world);

            // Check if chunk already has ruby ores
            if (OreRetrofitGenerator.chunkHasRubyOres(world, chunk)) {
                source.sendFeedback(() -> Text.literal("This chunk already has Ruby Ores!")
                        .formatted(Formatting.YELLOW), false);

                boolean wasRetrofitted = state.isChunkRetrofitted(chunkPos);
                if (!wasRetrofitted) {
                    // Mark as retrofitted untuk skip di future
                    state.markChunkRetrofitted(chunkPos);
                    source.sendFeedback(() -> Text.literal("✓ Marked as retrofitted for future scans")
                            .formatted(Formatting.GREEN), false);
                }
            } else if (OreRetrofitGenerator.retrofitChunk(world, chunk)) {
                source.sendFeedback(() -> Text.literal("✓ Current chunk retrofitted!")
                        .formatted(Formatting.GREEN), false);
                source.sendFeedback(() -> Text.literal("Ruby Ores should now be visible here")
                        .formatted(Formatting.AQUA), false);
            } else {
                source.sendFeedback(() -> Text.literal("Chunk was already retrofitted")
                        .formatted(Formatting.GRAY), false);
            }

        } catch (Exception e) {
            source.sendFeedback(() -> Text.literal("✗ Error: " + e.getMessage())
                    .formatted(Formatting.RED), false);
        }

        return 1;
    }

    /**
     * 🔧 FIXED: Retrofit ALL existing chunks (manual trigger)
     * ✨ IMPROVEMENT: Removed duplicate progress messages (karena sudah ada loading screen)
     */
    private static int retrofitAllExisting(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String worldName = source.getServer().getSaveProperties().getLevelName();

        // 🔧 FIX: Check if already running untuk WORLD INI
        if (InstantRetrofitSystem.isRetrofitRunning(worldName)) {
            source.sendFeedback(() -> Text.literal("⚠ Retrofit is already running for this world!")
                    .formatted(Formatting.YELLOW), false);
            source.sendFeedback(() -> Text.literal("Check your game screen for progress")
                    .formatted(Formatting.GRAY), false);
            return 0;
        }

        // Check if already complete
        if (InstantRetrofitSystem.isRetrofitComplete(source.getServer())) {
            source.sendFeedback(() -> Text.literal("⚠ Retrofit already complete for this world!")
                    .formatted(Formatting.YELLOW), false);
            source.sendFeedback(() -> Text.literal("Use /emeraldmod retrofit-reset to force re-run")
                    .formatted(Formatting.GRAY), false);
            return 0;
        }

        // ✨ IMPROVEMENT: Simplified feedback (no duplicate progress)
        source.sendFeedback(() -> Text.literal("⚡ Starting Ruby Ore Generation...")
                .formatted(Formatting.YELLOW), true);
        source.sendFeedback(() -> Text.literal("💡 Check your game screen for progress!")
                .formatted(Formatting.AQUA), false);
        source.sendFeedback(() -> Text.literal("You can minimize the screen and continue playing")
                .formatted(Formatting.GRAY), false);

        // Run retrofit
        boolean started = InstantRetrofitSystem.runInitialRetrofit(source.getServer());

        if (started) {
            source.sendFeedback(() -> Text.literal("✓ Generation started successfully!")
                    .formatted(Formatting.GREEN), false);
        } else {
            source.sendFeedback(() -> Text.literal("✗ Failed to start generation")
                    .formatted(Formatting.RED), false);
        }

        return started ? 1 : 0;
    }

    /**
     * 🔧 FIX: Default handler untuk /emeraldmod retrofit (no argument)
     */
    private static int retrofitAreaDefault(CommandContext<ServerCommandSource> context) {
        return retrofitArea(context, 10); // Default radius 10
    }

    /**
     * Execute retrofit di area sekitar player
     */
    private static int retrofitArea(CommandContext<ServerCommandSource> context) {
        int radius = IntegerArgumentType.getInteger(context, "radius");
        return retrofitArea(context, radius);
    }

    private static int retrofitArea(CommandContext<ServerCommandSource> context, int radius) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();

        // Get player position dan convert ke ChunkPos dengan benar
        BlockPos playerPos = BlockPos.ofFloored(source.getPosition());
        ChunkPos centerChunk = new ChunkPos(playerPos);

        source.sendFeedback(() -> Text.literal("⚡ Starting ore retrofit in radius " + radius + " chunks...")
                .formatted(Formatting.YELLOW), true);

        // Retrofit chunks di area sekitar player
        int retrofittedCount = 0;
        int skippedCount = 0;
        int totalChunks = 0;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                ChunkPos chunkPos = new ChunkPos(centerChunk.x + x, centerChunk.z + z);
                totalChunks++;

                try {
                    // Load chunk jika belum loaded
                    WorldChunk chunk = world.getChunk(chunkPos.x, chunkPos.z);

                    // Check if has ruby ores
                    if (OreRetrofitGenerator.chunkHasRubyOres(world, chunk)) {
                        OreRetrofitState state = OreRetrofitState.get(world.getServer(), world);
                        state.markChunkRetrofitted(chunkPos);
                        skippedCount++;
                    } else if (OreRetrofitGenerator.retrofitChunk(world, chunk)) {
                        retrofittedCount++;
                    }

                } catch (Exception e) {
                    int finalX = chunkPos.x;
                    int finalZ = chunkPos.z;
                    source.sendFeedback(() -> Text.literal("Error retrofitting chunk (" +
                                    finalX + ", " + finalZ + "): " + e.getMessage())
                            .formatted(Formatting.RED), false);
                }
            }
        }

        int finalRetrofittedCount = retrofittedCount;
        int finalSkippedCount = skippedCount;
        int finalTotalChunks = totalChunks;

        source.sendFeedback(() -> Text.literal("✓ Retrofit complete!")
                .formatted(Formatting.GREEN), true);
        source.sendFeedback(() -> Text.literal("New ores placed: " + finalRetrofittedCount + " chunks")
                .formatted(Formatting.AQUA), false);
        source.sendFeedback(() -> Text.literal("Already had ores: " + finalSkippedCount + " chunks")
                .formatted(Formatting.GRAY), false);
        source.sendFeedback(() -> Text.literal("Total checked: " + finalTotalChunks + " chunks")
                .formatted(Formatting.YELLOW), false);

        return retrofittedCount + skippedCount;
    }

    /**
     * 🔧 FIXED: Show retrofit statistics
     */
    private static int showRetrofitStats(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        String worldName = source.getServer().getSaveProperties().getLevelName();

        OreRetrofitState state = OreRetrofitState.get(world.getServer(), world);
        int retrofittedCount = state.getRetrofittedChunkCount();
        boolean isComplete = state.isComplete();
        boolean inProgress = state.isInProgress();

        source.sendFeedback(() -> Text.literal("=== Ore Retrofit Statistics ===")
                .formatted(Formatting.GOLD, Formatting.BOLD), false);

        // 🔧 FIX: Use world name instead of registry key
        source.sendFeedback(() -> Text.literal("World: " + worldName)
                .formatted(Formatting.YELLOW), false);
        source.sendFeedback(() -> Text.literal("Dimension: " + world.getRegistryKey().getValue().getPath())
                .formatted(Formatting.GRAY), false);
        source.sendFeedback(() -> Text.literal("Retrofitted Chunks: " + retrofittedCount)
                .formatted(Formatting.AQUA), false);

        // Status with emoji
        String status;
        Formatting statusColor;
        if (isComplete) {
            status = "✓ COMPLETE";
            statusColor = Formatting.GREEN;
        } else if (inProgress) {
            status = "⚡ IN PROGRESS";
            statusColor = Formatting.YELLOW;
        } else {
            status = "⏳ NOT STARTED";
            statusColor = Formatting.GRAY;
        }

        String finalStatus = status;
        source.sendFeedback(() -> Text.literal("Status: " + finalStatus)
                .formatted(statusColor), false);

        // 🔧 FIX: Check if currently running untuk WORLD INI
        if (InstantRetrofitSystem.isRetrofitRunning(worldName)) {
            source.sendFeedback(() -> Text.literal("⚡ Retrofit is currently running")
                    .formatted(Formatting.YELLOW), false);
            source.sendFeedback(() -> Text.literal("💡 Check your game screen for progress!")
                    .formatted(Formatting.AQUA), false);
        }

        // Suggestion
        if (!isComplete && !inProgress) {
            source.sendFeedback(() -> Text.literal("💡 Run /emeraldmod retrofit-all to start")
                    .formatted(Formatting.AQUA), false);
        } else if (inProgress && !InstantRetrofitSystem.isRetrofitRunning(worldName)) {
            source.sendFeedback(() -> Text.literal("💡 Retrofit will resume on next world load")
                    .formatted(Formatting.AQUA), false);
        }

        // 🔧 NEW: Show debug info
        String debugInfo = InstantRetrofitSystem.getStatusInfo(worldName);
        source.sendFeedback(() -> Text.literal("Debug: " + debugInfo)
                .formatted(Formatting.DARK_GRAY), false);

        return 1;
    }

    /**
     * 🔧 FIXED: Reset retrofit data (untuk debugging)
     */
    private static int resetRetrofit(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        String worldName = source.getServer().getSaveProperties().getLevelName();

        source.sendFeedback(() -> Text.literal("⚠️ WARNING: This will reset ALL retrofit data!")
                .formatted(Formatting.RED, Formatting.BOLD), false);
        source.sendFeedback(() -> Text.literal("All chunks will be retrofitted again on next generation.")
                .formatted(Formatting.YELLOW), false);

        // Get state dan clear
        OreRetrofitState state = OreRetrofitState.get(world.getServer(), world);
        int previousCount = state.getRetrofittedChunkCount();
        state.clearAll();

        // Reset system status
        InstantRetrofitSystem.resetRetrofitStatus(world.getServer());

        int finalPreviousCount = previousCount;
        source.sendFeedback(() -> Text.literal("✓ Retrofit data cleared for world '" + worldName + "'")
                .formatted(Formatting.GREEN), true);
        source.sendFeedback(() -> Text.literal("Cleared " + finalPreviousCount + " chunk records")
                .formatted(Formatting.GRAY), false);
        source.sendFeedback(() -> Text.literal("Use /emeraldmod retrofit-all to start fresh generation")
                .formatted(Formatting.AQUA), false);

        return 1;
    }
}