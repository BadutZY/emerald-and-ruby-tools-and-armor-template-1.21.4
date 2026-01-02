package com.example.emeraldmod.client;

import com.example.emeraldmod.EmeraldMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/**
 * Progress overlay (minimized retrofit screen)
 * - Press M to maximize
 * - Press J to hide/show (TRUE TOGGLE)
 */
@Environment(EnvType.CLIENT)
public class RetrofitOverlayRenderer {

    private static boolean isActive = false;
    private static boolean isTemporarilyHidden = false;

    private static int totalChunks = 0;
    private static int processedChunks = 0;
    private static String currentDimension = "Processing...";
    private static long startTime = System.currentTimeMillis();

    private static final int MINI_WIDTH = 300;
    private static final int MINI_HEIGHT = 110;
    private static final int MINI_MARGIN = 10;

    public static void register() {
        HudRenderCallback.EVENT.register(RetrofitOverlayRenderer::render);
        EmeraldMod.LOGGER.info("✅ Registered Retrofit Overlay Renderer");
    }

    /**
     * Activate overlay (reset hidden state)
     */
    public static void activate() {
        isActive = true;
        isTemporarilyHidden = false; // Reset ketika activate
        startTime = System.currentTimeMillis();
        EmeraldMod.LOGGER.info("[RetrofitOverlay] Activated - Press M to maximize, J to toggle hide");
    }

    /**
     * Deactivate overlay permanently
     */
    public static void deactivate() {
        isActive = false;
        isTemporarilyHidden = false; // Reset state
        EmeraldMod.LOGGER.info("[RetrofitOverlay] Deactivated");
    }

    /**
     * Update progress
     */
    public static void updateProgress(int processed, int total, String dimension) {
        processedChunks = processed;
        totalChunks = total;
        currentDimension = dimension;
    }

    /**
     * Check if overlay is active
     */
    public static boolean isActive() {
        return isActive;
    }

    /**
     * 🔧 Set temporary hidden state (for J keybind toggle)
     */
    public static void setTemporarilyHidden(boolean hidden) {
        if (isActive) { // Only set if active
            isTemporarilyHidden = hidden;
            EmeraldMod.LOGGER.info("[RetrofitOverlay] Temporarily {} (J key)",
                    hidden ? "HIDDEN" : "SHOWN");
        }
    }

    /**
     * Check if temporarily hidden
     */
    public static boolean isTemporarilyHidden() {
        return isTemporarilyHidden;
    }

    /**
     * Render overlay
     */
    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        // 🔧 Don't render if: not active OR temporarily hidden
        if (!isActive || isTemporarilyHidden) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options == null) {
            return;
        }

        int screenWidth = client.getWindow().getScaledWidth();

        // Position at top-right corner
        int x = screenWidth - MINI_WIDTH - MINI_MARGIN;
        int y = MINI_MARGIN;

        // Border (orange)
        context.fill(x - 2, y - 2, x + MINI_WIDTH + 2, y + MINI_HEIGHT + 2, 0xFFFFAA00);

        // Background (dark)
        context.fill(x, y, x + MINI_WIDTH, y + MINI_HEIGHT, 0xEE1a1a1a);

        // Title
        drawText(context, "⚡ Ruby Ore Retrofit", x + 5, y + 5, 0xFFFFAA00);

        // Progress
        if (totalChunks > 0) {
            int percentage = (processedChunks * 100) / totalChunks;
            String progressText = percentage + "% - " + currentDimension;
            drawText(context, progressText, x + 5, y + 23, 0xFFFFFF);

            // Progress bar
            int barWidth = MINI_WIDTH - 10;
            int barHeight = 20;
            int barX = x + 5;
            int barY = y + 42;

            // Bar background
            context.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF0a0a0a);

            // Bar border
            context.fill(barX - 1, barY - 1, barX + barWidth + 1, barY + barHeight + 1, 0xFF444444);
            context.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF1a1a1a);

            // Bar fill
            int fillWidth = (int) ((processedChunks / (float) totalChunks) * barWidth);
            int color = getProgressColor(percentage);
            context.fill(barX, barY, barX + fillWidth, barY + barHeight, color);

            // Percentage text in bar
            String percentText = percentage + "%";
            int textWidth = client.textRenderer.getWidth(percentText);
            drawText(context, percentText, barX + (barWidth - textWidth) / 2, barY + 6, 0xFFFFFF);
        }

        // Get keybind names
        String maximizeKey = "M";
        String hideKey = "J";

        try {
            maximizeKey = RetrofitKeybind.getMaximizeKey().getBoundKeyLocalizedText().getString();
            hideKey = RetrofitKeybind.getToggleHideKey().getBoundKeyLocalizedText().getString();
        } catch (Exception e) {
            // Use defaults
        }

        // Instruction: Maximize
        drawTextSmall(context, "Press [" + maximizeKey + "] to maximize", x + 5, y + 67, 0xFFFF00);

        // Instruction: Hide/Show toggle
        drawTextSmall(context, "Press [" + hideKey + "] to hide", x + 5, y + 82, 0xAAAAAA);

        // Time estimate
        if (processedChunks > 0 && totalChunks > 0) {
            long elapsed = (System.currentTimeMillis() - startTime) / 1000;
            long estimated = (elapsed * totalChunks / processedChunks);
            long remaining = Math.max(0, estimated - elapsed);
            String timeText = "ETA: " + formatTime(remaining);
            drawTextSmall(context, timeText, x + MINI_WIDTH - 80, y + 67, 0xAAAAAA);
        }
    }

    private static void drawText(DrawContext context, String text, int x, int y, int color) {
        MinecraftClient client = MinecraftClient.getInstance();
        context.drawText(client.textRenderer, text, x, y, color, true);
    }

    private static void drawTextSmall(DrawContext context, String text, int x, int y, int color) {
        MinecraftClient client = MinecraftClient.getInstance();
        context.getMatrices().push();
        context.getMatrices().scale(0.8f, 0.8f, 1.0f);
        context.drawText(client.textRenderer, text, (int)(x / 0.8f), (int)(y / 0.8f), color, true);
        context.getMatrices().pop();
    }

    private static int getProgressColor(int percentage) {
        if (percentage < 30) {
            return 0xFFFF4444; // Red
        } else if (percentage < 70) {
            return 0xFFFFBB33; // Orange
        } else if (percentage < 95) {
            return 0xFF44FF44; // Green
        } else {
            return 0xFF00FFFF; // Cyan
        }
    }

    private static String formatTime(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        } else {
            long minutes = seconds / 60;
            long secs = seconds % 60;
            return minutes + "m " + secs + "s";
        }
    }
}