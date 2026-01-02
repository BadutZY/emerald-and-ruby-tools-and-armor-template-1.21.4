package com.example.emeraldmod.world.gen;

import com.example.emeraldmod.EmeraldMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * System untuk instant retrofit semua existing chunks dengan Ruby Ores
 * ✨ ENHANCED: Resume support - melanjutkan dari checkpoint terakhir
 */
public class InstantRetrofitSystem {

    // Map: worldName -> RetrofitTaskInfo
    private static final Map<String, RetrofitTaskInfo> activeRetrofits = new ConcurrentHashMap<>();

    /**
     * Info tentang retrofit task yang sedang berjalan
     */
    private static class RetrofitTaskInfo {
        CompletableFuture<Void> task;
        AtomicBoolean running;
        String worldName;
        MinecraftServer server;
        long startTime;
        boolean isResume; // ✨ NEW: Track if this is a resume

        RetrofitTaskInfo(String worldName, MinecraftServer server, boolean isResume) {
            this.worldName = worldName;
            this.server = server;
            this.running = new AtomicBoolean(true);
            this.startTime = System.currentTimeMillis();
            this.isResume = isResume;
        }
    }

    // ============================================
    // PUBLIC API (ENHANCED)
    // ============================================

    /**
     * ✨ ENHANCED: Start retrofit dengan resume support
     */
    public static boolean runInitialRetrofit(MinecraftServer server) {
        String worldName = server.getSaveProperties().getLevelName();

        synchronized (activeRetrofits) {
            // Check if already running
            RetrofitTaskInfo existingTask = activeRetrofits.get(worldName);
            if (existingTask != null && existingTask.running.get()) {
                EmeraldMod.LOGGER.warn("[Retrofit] Already running for world '{}' - ignoring request", worldName);
                return false;
            }

            // Check states
            ServerWorld overworld = server.getWorld(World.OVERWORLD);
            ServerWorld nether = server.getWorld(World.NETHER);

            if (overworld == null && nether == null) {
                EmeraldMod.LOGGER.error("[Retrofit] No valid worlds found!");
                return false;
            }

            // ✨ NEW: Check if can resume
            boolean canResumeOverworld = false;
            boolean canResumeNether = false;

            if (overworld != null) {
                OreRetrofitState overworldState = OreRetrofitState.get(server, overworld);
                canResumeOverworld = overworldState.canResume();

                if (overworldState.isComplete()) {
                    EmeraldMod.LOGGER.info("[Retrofit] Overworld already complete for '{}'", worldName);
                } else if (canResumeOverworld) {
                    EmeraldMod.LOGGER.info("[Retrofit] 🔄 Can RESUME Overworld: {}",
                            overworldState.getResumeInfo());
                }
            }

            if (nether != null) {
                OreRetrofitState netherState = OreRetrofitState.get(server, nether);
                canResumeNether = netherState.canResume();

                if (netherState.isComplete()) {
                    EmeraldMod.LOGGER.info("[Retrofit] Nether already complete for '{}'", worldName);
                } else if (canResumeNether) {
                    EmeraldMod.LOGGER.info("[Retrofit] 🔄 Can RESUME Nether: {}",
                            netherState.getResumeInfo());
                }
            }

            // Check if both complete
            boolean overworldComplete = (overworld == null ||
                    OreRetrofitState.get(server, overworld).isComplete());
            boolean netherComplete = (nether == null ||
                    OreRetrofitState.get(server, nether).isComplete());

            if (overworldComplete && netherComplete) {
                EmeraldMod.LOGGER.info("[Retrofit] Already complete for world '{}'", worldName);
                return false;
            }

            // ✨ Determine if this is a resume
            boolean isResume = canResumeOverworld || canResumeNether;

            // Create new task info
            RetrofitTaskInfo taskInfo = new RetrofitTaskInfo(worldName, server, isResume);
            activeRetrofits.put(worldName, taskInfo);

            EmeraldMod.LOGGER.info("========================================");
            if (isResume) {
                EmeraldMod.LOGGER.info("[Retrofit] 🔄 RESUMING RETROFIT");
            } else {
                EmeraldMod.LOGGER.info("[Retrofit] ⚡ STARTING RETROFIT");
            }
            EmeraldMod.LOGGER.info("[Retrofit] World: {}", worldName);
            EmeraldMod.LOGGER.info("========================================");

            // Start async retrofit task
            startRetrofitTask(taskInfo);

            return true;
        }
    }

    /**
     * Check if retrofit is running untuk world ini
     */
    public static boolean isRetrofitRunning(String worldName) {
        if (worldName == null) {
            EmeraldMod.LOGGER.warn("[Retrofit] isRetrofitRunning called with null worldName");
            return false;
        }

        synchronized (activeRetrofits) {
            RetrofitTaskInfo taskInfo = activeRetrofits.get(worldName);
            boolean running = taskInfo != null && taskInfo.running.get();

            if (running) {
                long elapsed = (System.currentTimeMillis() - taskInfo.startTime) / 1000;
                EmeraldMod.LOGGER.debug("[Retrofit] World '{}' is retrofitting ({}s elapsed, resume={})",
                        worldName, elapsed, taskInfo.isResume);
            }

            return running;
        }
    }

    /**
     * Check if retrofit is complete
     */
    public static boolean isRetrofitComplete(MinecraftServer server) {
        ServerWorld overworld = server.getWorld(World.OVERWORLD);
        ServerWorld nether = server.getWorld(World.NETHER);

        if (overworld == null && nether == null) {
            return false;
        }

        boolean overworldComplete = (overworld == null ||
                OreRetrofitState.get(server, overworld).isComplete());
        boolean netherComplete = (nether == null ||
                OreRetrofitState.get(server, nether).isComplete());

        return overworldComplete && netherComplete;
    }

    /**
     * Cancel retrofit dengan proper cleanup
     */
    public static void cancelRetrofit(String worldName) {
        synchronized (activeRetrofits) {
            RetrofitTaskInfo taskInfo = activeRetrofits.get(worldName);
            if (taskInfo != null) {
                EmeraldMod.LOGGER.info("[Retrofit] ⚠️ Cancelling retrofit for world '{}'", worldName);

                taskInfo.running.set(false);

                if (taskInfo.task != null && !taskInfo.task.isDone()) {
                    taskInfo.task.cancel(true);
                    EmeraldMod.LOGGER.info("[Retrofit] Task cancelled for world '{}'", worldName);
                }

                activeRetrofits.remove(worldName);

                EmeraldMod.LOGGER.info("[Retrofit] ✅ Cleanup complete for world '{}'", worldName);
            }
        }
    }

    /**
     * Reset retrofit status
     */
    public static void resetRetrofitStatus(MinecraftServer server) {
        String worldName = server.getSaveProperties().getLevelName();

        synchronized (activeRetrofits) {
            cancelRetrofit(worldName);

            for (ServerWorld world : server.getWorlds()) {
                OreRetrofitState state = OreRetrofitState.get(server, world);
                state.clearAll();
            }

            EmeraldMod.LOGGER.info("[Retrofit] ✅ Reset status for world '{}'", worldName);
        }
    }

    // ============================================
    // INTERNAL IMPLEMENTATION (ENHANCED)
    // ============================================

    /**
     * Start async retrofit task
     */
    private static void startRetrofitTask(RetrofitTaskInfo taskInfo) {
        taskInfo.task = CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(3000);

                retrofitAllDimensions(taskInfo);

            } catch (InterruptedException e) {
                EmeraldMod.LOGGER.warn("[Retrofit] Task interrupted for world '{}'", taskInfo.worldName);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                EmeraldMod.LOGGER.error("[Retrofit] Task failed for world '{}'", taskInfo.worldName, e);
            } finally {
                synchronized (activeRetrofits) {
                    taskInfo.running.set(false);
                    activeRetrofits.remove(taskInfo.worldName);
                    EmeraldMod.LOGGER.info("[Retrofit] ✅ Task cleanup completed for world '{}'",
                            taskInfo.worldName);
                }
            }
        });
    }

    /**
     * ✨ ENHANCED: Retrofit all dimensions dengan resume support
     */
    private static void retrofitAllDimensions(RetrofitTaskInfo taskInfo) {
        MinecraftServer server = taskInfo.server;
        String worldName = taskInfo.worldName;

        long startTime = System.currentTimeMillis();
        int totalChunks = 0;

        // Notify start (dengan info resume)
        server.execute(() -> {
            if (taskInfo.isResume) {
                RetrofitProgressManager.startResume(server);
            } else {
                RetrofitProgressManager.start(server);
            }
        });

        safeSleep(2000);

        // Retrofit Overworld
        ServerWorld overworld = server.getWorld(World.OVERWORLD);

        if (!taskInfo.running.get()) {
            EmeraldMod.LOGGER.warn("[Retrofit] Cancelled before Overworld processing");
            return;
        }

        if (overworld != null) {
            OreRetrofitState overworldState = OreRetrofitState.get(server, overworld);

            if (!overworldState.isComplete()) {
                overworldState.setInProgress(true);
                overworldState.setCurrentDimension("Overworld");

                int count = retrofitWorld(taskInfo, overworld, "Overworld");
                totalChunks += count;
            }
        }

        if (!taskInfo.running.get()) {
            EmeraldMod.LOGGER.warn("[Retrofit] Cancelled before Nether processing");
            return;
        }

        // Retrofit Nether
        ServerWorld nether = server.getWorld(World.NETHER);
        if (nether != null) {
            OreRetrofitState netherState = OreRetrofitState.get(server, nether);

            if (!netherState.isComplete()) {
                netherState.setInProgress(true);
                netherState.setCurrentDimension("Nether");

                int count = retrofitWorld(taskInfo, nether, "Nether");
                totalChunks += count;
            }
        }

        long duration = System.currentTimeMillis() - startTime;

        // Mark as complete
        if (overworld != null) {
            OreRetrofitState.get(server, overworld).setComplete(true);
        }
        if (nether != null) {
            OreRetrofitState.get(server, nether).setComplete(true);
        }

        final int finalTotalChunks = totalChunks;
        final long finalDuration = duration;

        EmeraldMod.LOGGER.info("========================================");
        EmeraldMod.LOGGER.info("[Retrofit] ✅ COMPLETE FOR WORLD: {}", worldName);
        EmeraldMod.LOGGER.info("[Retrofit] Total chunks: {}", finalTotalChunks);
        EmeraldMod.LOGGER.info("[Retrofit] Time: {} seconds", finalDuration / 1000.0);
        EmeraldMod.LOGGER.info("========================================");

        server.execute(() -> {
            RetrofitProgressManager.complete(server, finalTotalChunks);
        });
    }

    /**
     * ✨ ENHANCED: Retrofit single world dengan resume support
     */
    private static int retrofitWorld(RetrofitTaskInfo taskInfo, ServerWorld world, String dimensionName) {
        MinecraftServer server = taskInfo.server;
        OreRetrofitState state = OreRetrofitState.get(server, world);

        // ✨ Get chunks to process (excludes already retrofitted)
        List<ChunkPos> chunksToProcess = getChunksToRetrofit(server, world, state);

        if (chunksToProcess.isEmpty()) {
            EmeraldMod.LOGGER.info("[Retrofit] No chunks to process in {} (all done)", dimensionName);
            return 0;
        }

        int totalChunks = chunksToProcess.size();

        // ✨ Set total chunks jika belum di-set (untuk resume)
        if (state.getTotalChunks() == 0) {
            // Get TOTAL chunks including already processed
            int allChunks = totalChunks + state.getRetrofittedChunkCount();
            state.setTotalChunks(allChunks);
        }

        int processed = 0;
        int skipped = 0;
        long startTime = System.currentTimeMillis();

        // ✨ Log resume info
        if (state.canResume()) {
            EmeraldMod.LOGGER.info("[Retrofit] 🔄 RESUMING {}: {} remaining chunks (was at {}%)",
                    dimensionName, totalChunks, state.getPercentage());
        } else {
            EmeraldMod.LOGGER.info("[Retrofit] ⚡ STARTING {}: {} chunks to process",
                    dimensionName, totalChunks);
        }

        final int BATCH_SIZE = 50;

        for (int i = 0; i < totalChunks; i++) {
            if (!taskInfo.running.get() || Thread.currentThread().isInterrupted()) {
                EmeraldMod.LOGGER.warn("[Retrofit] ⚠️ Stopped processing {} at chunk {}/{}",
                        dimensionName, i, totalChunks);
                break;
            }

            ChunkPos pos = chunksToProcess.get(i);

            try {
                WorldChunk chunk = world.getChunk(pos.x, pos.z);

                if (chunk != null) {
                    if (OreRetrofitGenerator.chunkHasRubyOres(world, chunk)) {
                        state.markChunkRetrofitted(pos);
                        skipped++;
                    } else if (OreRetrofitGenerator.retrofitChunk(world, chunk)) {
                        processed++;
                    }
                }

                if ((i + 1) % BATCH_SIZE == 0 || (i + 1) == totalChunks) {
                    // ✨ Use state's processed count untuk accurate progress
                    updateProgress(server, state.getProcessedChunks(), state.getTotalChunks(),
                            dimensionName, processed, skipped, startTime);
                }

                if ((i + 1) % BATCH_SIZE == 0) {
                    safeSleep(100);
                }

            } catch (Exception e) {
                EmeraldMod.LOGGER.error("[Retrofit] Error processing chunk ({}, {}): {}",
                        pos.x, pos.z, e.getMessage());
            }
        }

        long totalTime = (System.currentTimeMillis() - startTime) / 1000;

        EmeraldMod.LOGGER.info("[Retrofit] {} complete:", dimensionName);
        EmeraldMod.LOGGER.info("[Retrofit]   New ores: {} chunks", processed);
        EmeraldMod.LOGGER.info("[Retrofit]   Had ores: {} chunks", skipped);
        EmeraldMod.LOGGER.info("[Retrofit]   Total: {}/{} chunks ({}%)",
                state.getProcessedChunks(), state.getTotalChunks(), state.getPercentage());
        EmeraldMod.LOGGER.info("[Retrofit]   Time: {}s", totalTime);

        return processed + skipped;
    }

    /**
     * ✨ ENHANCED: Get chunks to retrofit (excludes already retrofitted)
     */
    private static List<ChunkPos> getChunksToRetrofit(MinecraftServer server, ServerWorld world,
                                                      OreRetrofitState state) {
        List<ChunkPos> chunks = new ArrayList<>();

        File worldDir = server.getSavePath(WorldSavePath.ROOT).toFile();
        File regionDir = getRegionFolder(worldDir, world);

        if (regionDir == null || !regionDir.exists()) {
            return chunks;
        }

        File[] regionFiles = regionDir.listFiles((dir, name) -> name.endsWith(".mca"));

        if (regionFiles == null || regionFiles.length == 0) {
            return chunks;
        }

        Pattern pattern = Pattern.compile("r\\.(-?\\d+)\\.(-?\\d+)\\.mca");

        for (File regionFile : regionFiles) {
            Matcher matcher = pattern.matcher(regionFile.getName());

            if (matcher.matches()) {
                int regionX = Integer.parseInt(matcher.group(1));
                int regionZ = Integer.parseInt(matcher.group(2));

                for (int cx = 0; cx < 32; cx++) {
                    for (int cz = 0; cz < 32; cz++) {
                        ChunkPos pos = new ChunkPos(regionX * 32 + cx, regionZ * 32 + cz);

                        // ✨ Only add if NOT already retrofitted
                        if (!state.isChunkRetrofitted(pos)) {
                            chunks.add(pos);
                        }
                    }
                }
            }
        }

        return chunks;
    }

    private static File getRegionFolder(File worldDir, ServerWorld world) {
        if (world.getRegistryKey() == World.OVERWORLD) {
            return new File(worldDir, "region");
        } else if (world.getRegistryKey() == World.NETHER) {
            return new File(new File(worldDir, "DIM-1"), "region");
        } else if (world.getRegistryKey() == World.END) {
            return new File(new File(worldDir, "DIM1"), "region");
        }
        return null;
    }

    private static void updateProgress(MinecraftServer server, int current, int total,
                                       String dimension, int processed, int skipped,
                                       long startTime) {
        int percentage = total > 0 ? (current * 100) / total : 0;
        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        long estimated = total > 0 && current > 0 ? (elapsed * total / current) : 0;
        long remaining = Math.max(0, estimated - elapsed);

        EmeraldMod.LOGGER.info("[Retrofit] Progress: {}/{} ({}%) | Processed: {} | Skipped: {} | ETA: {}s",
                current, total, percentage, processed, skipped, remaining);

        server.execute(() -> {
            RetrofitProgressManager.updateProgress(server, current, total, dimension);
        });
    }

    private static void safeSleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ============================================
    // DEBUG & UTILITY
    // ============================================

    public static String getStatusInfo(String worldName) {
        if (worldName == null) {
            return "INVALID_WORLD_NAME";
        }

        RetrofitTaskInfo taskInfo = activeRetrofits.get(worldName);
        if (taskInfo == null || !taskInfo.running.get()) {
            return "NOT_RUNNING";
        }

        long elapsed = (System.currentTimeMillis() - taskInfo.startTime) / 1000;
        return String.format("%s[world='%s', elapsed=%ds]",
                taskInfo.isResume ? "RESUMING" : "RUNNING", worldName, elapsed);
    }

    public static Set<String> getActiveWorlds() {
        return new HashSet<>(activeRetrofits.keySet());
    }

    public static String getDebugInfo() {
        synchronized (activeRetrofits) {
            if (activeRetrofits.isEmpty()) {
                return "No active retrofits";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Active Retrofits:\n");

            for (Map.Entry<String, RetrofitTaskInfo> entry : activeRetrofits.entrySet()) {
                RetrofitTaskInfo info = entry.getValue();
                long elapsed = (System.currentTimeMillis() - info.startTime) / 1000;
                sb.append(String.format("  - World: '%s', %s: %s, Elapsed: %ds\n",
                        entry.getKey(),
                        info.isResume ? "Resuming" : "Running",
                        info.running.get(), elapsed));
            }

            return sb.toString();
        }
    }
}