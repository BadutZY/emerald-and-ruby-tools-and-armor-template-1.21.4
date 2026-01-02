package com.example.emeraldmod.world.gen;

import com.example.emeraldmod.EmeraldMod;
import com.example.emeraldmod.network.RetrofitPackets;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Manages retrofit progress notifications for both client and server
 * ✨ UPDATED: Removed annoying chat progress messages (only loading screen)
 */
public class RetrofitProgressManager {

    private static int currentTotal = 0;
    private static int currentProcessed = 0;
    private static String currentDimension = "";
    private static long startTime = 0;
    private static boolean loadingShown = false;
    private static boolean isResume = false;

    /**
     * ✨ Start retrofit RESUME
     */
    public static void startResume(MinecraftServer server) {
        isResume = true;
        String worldName = server.getSaveProperties().getLevelName();
        startTime = System.currentTimeMillis();
        loadingShown = false;

        EmeraldMod.LOGGER.info("[RetrofitProgress] 🔄 Resuming retrofit for world '{}'", worldName);

        // ✅ SIMPLIFIED: Only essential start message (no spam)
        broadcastToPlayers(server,
                Text.literal("🔄 Resuming Ruby Ore Generation...").formatted(Formatting.AQUA));
        broadcastToPlayers(server,
                Text.literal("Check loading screen for progress!").formatted(Formatting.GRAY));

        sendShowLoadingWithRetry(server, worldName, 0);
    }

    /**
     * Start retrofit and notify players
     */
    public static void start(MinecraftServer server) {
        isResume = false;
        String worldName = server.getSaveProperties().getLevelName();
        startTime = System.currentTimeMillis();
        loadingShown = false;

        EmeraldMod.LOGGER.info("[RetrofitProgress] Starting retrofit notification for world '{}'", worldName);

        // ✅ SIMPLIFIED: Only essential start message (no spam)
        broadcastToPlayers(server,
                Text.literal("⚡ Ruby Ore Generation Starting...").formatted(Formatting.GOLD));
        broadcastToPlayers(server,
                Text.literal("Check loading screen for progress!").formatted(Formatting.GRAY));

        sendShowLoadingWithRetry(server, worldName, 0);
    }

    /**
     * Send show loading packet dengan retry mechanism
     */
    private static void sendShowLoadingWithRetry(MinecraftServer server, String worldName, int attempt) {
        if (attempt >= 5) {
            EmeraldMod.LOGGER.error("[RetrofitProgress] Failed to send show loading after 5 attempts");
            return;
        }

        server.execute(() -> {
            List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();

            if (players.isEmpty()) {
                EmeraldMod.LOGGER.warn("[RetrofitProgress] No players online yet, retrying in 2s (attempt {})", attempt + 1);
                new Thread(() -> {
                    try {
                        Thread.sleep(2000);
                        sendShowLoadingWithRetry(server, worldName, attempt + 1);
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                }).start();
                return;
            }

            EmeraldMod.LOGGER.info("[RetrofitProgress] Sending show loading packet to {} players for world '{}' (resume={})",
                    players.size(), worldName, isResume);

            for (ServerPlayerEntity player : players) {
                try {
                    RetrofitPackets.sendShowLoading(player, worldName);
                    EmeraldMod.LOGGER.info("[RetrofitProgress] Sent loading screen to player: {} for world '{}'",
                            player.getName().getString(), worldName);
                } catch (Exception e) {
                    EmeraldMod.LOGGER.error("[RetrofitProgress] Failed to send loading screen to player {}: {}",
                            player.getName().getString(), e.getMessage());
                }
            }

            loadingShown = true;
        });
    }

    /**
     * ✨ UPDATED: Update progress - NO MORE CHAT SPAM!
     * Progress only shown in loading screen, not in chat
     */
    public static void updateProgress(MinecraftServer server, int processed, int total, String dimension) {
        String worldName = server.getSaveProperties().getLevelName();
        currentProcessed = processed;
        currentTotal = total;
        currentDimension = dimension;

        // Send progress update to loading screen ONLY (not chat)
        server.execute(() -> {
            List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();

            if (!players.isEmpty()) {
                for (ServerPlayerEntity player : players) {
                    try {
                        RetrofitPackets.sendUpdateProgress(player, worldName, processed, total, dimension);
                    } catch (Exception e) {
                        EmeraldMod.LOGGER.error("[RetrofitProgress] Failed to send progress to {}: {}",
                                player.getName().getString(), e.getMessage());
                    }
                }
            }
        });

        // ❌ REMOVED: Annoying chat progress messages every 25%
        // Progress is now ONLY shown in the loading screen
        // This keeps chat clean and prevents spam!
    }

    /**
     * Mark as complete
     */
    public static void complete(MinecraftServer server, int totalChunks) {
        String worldName = server.getSaveProperties().getLevelName();
        long totalTime = (System.currentTimeMillis() - startTime) / 1000;

        EmeraldMod.LOGGER.info("[RetrofitProgress] Sending completion notification for world '{}'", worldName);

        // ✅ SIMPLIFIED: Only completion message (no spam)
        String completeMessage = isResume ?
                "✅ Ruby Ore Generation Resumed & Complete!" :
                "✅ Ruby Ore Generation Complete!";

        broadcastToPlayers(server,
                Text.literal(completeMessage).formatted(Formatting.GREEN));
        broadcastToPlayers(server,
                Text.literal(String.format("Completed in %s - Ruby Ores now available!", formatTime(totalTime)))
                        .formatted(Formatting.YELLOW));

        // Send complete packet to loading screen
        server.execute(() -> {
            List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();

            for (ServerPlayerEntity player : players) {
                try {
                    RetrofitPackets.sendComplete(player, worldName);
                    EmeraldMod.LOGGER.info("[RetrofitProgress] Sent complete packet to {} for world '{}'",
                            player.getName().getString(), worldName);
                } catch (Exception e) {
                    EmeraldMod.LOGGER.error("[RetrofitProgress] Failed to send complete to {}: {}",
                            player.getName().getString(), e.getMessage());
                }
            }
        });

        // Reset resume flag
        isResume = false;
    }

    /**
     * Broadcast message to all online players
     */
    private static void broadcastToPlayers(MinecraftServer server, Text message) {
        if (server == null) return;

        server.execute(() -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                try {
                    player.sendMessage(message, false);
                } catch (Exception e) {
                    EmeraldMod.LOGGER.error("[RetrofitProgress] Failed to send chat message to {}: {}",
                            player.getName().getString(), e.getMessage());
                }
            }
        });
    }

    /**
     * Format seconds to readable time
     */
    private static String formatTime(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        } else {
            long minutes = seconds / 60;
            long secs = seconds % 60;
            return minutes + "m " + secs + "s";
        }
    }

    /**
     * Check if loading screen has been shown
     */
    public static boolean isLoadingShown() {
        return loadingShown;
    }

    /**
     * Check if current operation is a resume
     */
    public static boolean isResume() {
        return isResume;
    }
}