package com.example.emeraldmod.client;

import com.example.emeraldmod.EmeraldMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class RetrofitLoadingScreen extends Screen {

    private static RetrofitLoadingScreen instance;
    private int totalChunks = 0;
    private int processedChunks = 0;
    private String currentDimension = "Preparing...";
    private long startTime = System.currentTimeMillis();
    private boolean isComplete = false;
    private boolean isMinimized = false;
    private boolean hasAutoMaximized = false;

    // Minimized widget dimensions
    private static final int MINI_WIDTH = 280;
    private static final int MINI_HEIGHT = 80;
    private static final int MINI_MARGIN = 10;

    // Button dimensions
    private int minimizeButtonX;
    private int minimizeButtonY;
    private int minimizeButtonSize = 20;

    public RetrofitLoadingScreen() {
        super(Text.literal("Ruby Ore Retrofit"));
    }

    public static RetrofitLoadingScreen getInstance() {
        if (instance == null) {
            instance = new RetrofitLoadingScreen();
        }
        return instance;
    }

    public static void show() {
        // ✨ KEY FIX: Check world sebelum show
        if (!ClientWorldTracker.shouldShowRetrofitUI()) {
            EmeraldMod.LOGGER.warn("[RetrofitScreen] Not showing - not in retrofit world");
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            RetrofitLoadingScreen screen = getInstance();
            client.execute(() -> {
                EmeraldMod.LOGGER.info("[RetrofitScreen] Showing retrofit loading screen");
                client.setScreen(screen);
            });
        } else {
            EmeraldMod.LOGGER.info("[RetrofitScreen] Cannot show loading screen - client is null");
        }
    }

    public static void hide() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.currentScreen instanceof RetrofitLoadingScreen) {
            client.execute(() -> client.setScreen(null));
        }
        instance = null;
    }

    public static void updateProgress(int processed, int total, String dimension) {
        // ✨ KEY FIX: Check world sebelum update
        if (!ClientWorldTracker.shouldShowRetrofitUI()) {
            return;
        }

        EmeraldMod.LOGGER.debug("[RetrofitScreen] Updating progress: " + processed + "/" + total + " - " + dimension);
        if (instance != null) {
            instance.processedChunks = processed;
            instance.totalChunks = total;
            instance.currentDimension = dimension;

            // Update overlay if active
            if (RetrofitOverlayRenderer.isActive()) {
                RetrofitOverlayRenderer.updateProgress(processed, total, dimension);
            }

            // Auto-maximize at 95%
            instance.checkAutoMaximize();
        }
    }

    public static void setComplete() {
        // ✨ KEY FIX: Check world sebelum complete
        if (!ClientWorldTracker.shouldShowRetrofitUI()) {
            EmeraldMod.LOGGER.warn("[RetrofitScreen] Not setting complete - not in retrofit world");
            return;
        }

        EmeraldMod.LOGGER.info("[RetrofitScreen] Setting retrofit complete");
        if (instance != null) {
            instance.isComplete = true;
            instance.isMinimized = false;

            // Deactivate overlay
            RetrofitOverlayRenderer.deactivate();

            // Show fullscreen completion
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                client.execute(() -> {
                    client.setScreen(instance);
                });
            }

            // Auto-hide after 3 seconds
            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                    EmeraldMod.LOGGER.info("[RetrofitScreen] Auto-hiding loading screen");
                    hide();
                } catch (InterruptedException e) {
                    // Ignore
                }
            }).start();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // ✨ KEY FIX: Double check world saat render
        if (!ClientWorldTracker.shouldShowRetrofitUI() && !isComplete) {
            EmeraldMod.LOGGER.warn("[RetrofitScreen] Closing - not in retrofit world anymore");
            this.close();
            return;
        }

        if (isMinimized) {
            renderMinimized(context, mouseX, mouseY);
        } else {
            renderFullScreen(context, mouseX, mouseY);
        }
    }

    // ... rest of render methods sama seperti sebelumnya ...

    private void renderMinimized(DrawContext context, int mouseX, int mouseY) {
        int x = this.width - MINI_WIDTH - MINI_MARGIN;
        int y = MINI_MARGIN;

        context.fill(x - 2, y - 2, x + MINI_WIDTH + 2, y + MINI_HEIGHT + 2, 0xFFFFFFFF);
        context.fill(x, y, x + MINI_WIDTH, y + MINI_HEIGHT, 0xEE2a2a2a);

        drawSharpText(context, "⚡ Ruby Ore Retrofit", x + 5, y + 5, 0xFFAA00, false, 1.0f);

        if (totalChunks > 0) {
            int percentage = (processedChunks * 100) / totalChunks;
            String progressText = percentage + "% - " + currentDimension;
            drawSharpText(context, progressText, x + 5, y + 22, 0xFFFFFF, false, 0.85f);

            int barWidth = MINI_WIDTH - 10;
            int barHeight = 18;
            int barX = x + 5;
            int barY = y + 40;

            context.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF1a1a1a);

            int fillWidth = (int) ((processedChunks / (float) totalChunks) * barWidth);
            int color = getProgressColor(percentage);
            context.fill(barX, barY, barX + fillWidth, barY + barHeight, color);

            String percentText = percentage + "%";
            int textWidth = this.textRenderer.getWidth(percentText);
            drawSharpText(context, percentText, barX + (barWidth - textWidth) / 2, barY + 5, 0xFFFFFF, false, 0.9f);

            if (processedChunks > 0) {
                long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                long estimated = (elapsed * totalChunks / processedChunks);
                long remaining = Math.max(0, estimated - elapsed);
                String timeText = "ETA: " + formatTime(remaining);
                drawSharpText(context, timeText, x + 5, y + 62, 0xAAAAAA, false, 0.75f);
            }
        }

        int maxButtonX = x + MINI_WIDTH - 25;
        int maxButtonY = y + 5;
        int maxButtonSize = 18;

        boolean hoveringMax = mouseX >= maxButtonX && mouseX <= maxButtonX + maxButtonSize &&
                mouseY >= maxButtonY && mouseY <= maxButtonY + maxButtonSize;
        context.fill(maxButtonX, maxButtonY, maxButtonX + maxButtonSize, maxButtonY + maxButtonSize,
                hoveringMax ? 0xFF55FF55 : 0xFF2a8a2a);

        drawSharpText(context, "□", maxButtonX + 5, maxButtonY + 3, 0xFFFFFF, false, 1.0f);
    }

    private void renderFullScreen(DrawContext context, int mouseX, int mouseY) {
        context.fill(0, 0, this.width, this.height, 0xFF1a1a1a);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        if (isComplete) {
            drawSharpText(context, "✅ Ruby Ore Retrofit Complete!",
                    centerX, centerY - 40, 0x00FF00, true, 2.0f);

            drawSharpText(context, "Ruby Ores are now available throughout your world!",
                    centerX, centerY - 10, 0xFFFFFF, true, 1.0f);

            drawSharpText(context, "This includes Overworld AND Nether dimensions!",
                    centerX, centerY + 10, 0xFFAA00, true, 1.0f);

            drawSharpText(context, "Closing in a moment...",
                    centerX, centerY + 40, 0xAAAAAA, true, 1.0f);

        } else {
            drawSharpText(context, "⚡ Generating Ruby Ores",
                    centerX, centerY - 80, 0xFFAA00, true, 2.0f);

            drawSharpText(context, "Processing: " + currentDimension,
                    centerX, centerY - 50, 0xFFFFFF, true, 1.5f);

            if (totalChunks > 0) {
                int percentage = (processedChunks * 100) / totalChunks;

                String progressText = processedChunks + " / " + totalChunks + " chunks (" + percentage + "%)";
                drawSharpText(context, progressText,
                        centerX, centerY - 25, 0xFFFFFF, true, 1.0f);

                int barWidth = 400;
                int barHeight = 30;
                int barX = centerX - barWidth / 2;
                int barY = centerY;

                context.fill(barX - 2, barY - 2, barX + barWidth + 2, barY + barHeight + 2, 0xFFFFFFFF);
                context.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF2a2a2a);

                int fillWidth = (int) ((processedChunks / (float) totalChunks) * barWidth);
                int color = getProgressColor(percentage);
                context.fill(barX, barY, barX + fillWidth, barY + barHeight, color);

                long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                long estimated = totalChunks > 0 ? (elapsed * totalChunks / Math.max(1, processedChunks)) : 0;
                long remaining = Math.max(0, estimated - elapsed);

                String timeText = "Elapsed: " + formatTime(elapsed) + " | ETA: " + formatTime(remaining);
                drawSharpText(context, timeText,
                        centerX, centerY + 45, 0xCCCCCC, true, 1.0f);
            } else {
                drawSharpText(context, "Scanning world...",
                        centerX, centerY - 20, 0xFFFFFF, true, 1.0f);
            }

            drawSharpText(context, "Please wait - this only happens once!",
                    centerX, centerY + 75, 0x999999, true, 1.0f);

            drawSharpText(context, "Adding Ruby Ores to Overworld AND Nether...",
                    centerX, centerY + 95, 0x999999, true, 1.0f);

            drawSharpText(context, "You can minimize and play. Press 'M' to maximize back.",
                    centerX, centerY + 115, 0xFFFF00, true, 0.9f);

            minimizeButtonX = this.width - minimizeButtonSize - 10;
            minimizeButtonY = 10;

            boolean hoveringMin = mouseX >= minimizeButtonX && mouseX <= minimizeButtonX + minimizeButtonSize &&
                    mouseY >= minimizeButtonY && mouseY <= minimizeButtonY + minimizeButtonSize;

            context.fill(minimizeButtonX, minimizeButtonY,
                    minimizeButtonX + minimizeButtonSize, minimizeButtonY + minimizeButtonSize,
                    hoveringMin ? 0xFFFFAA00 : 0xFF8a6a00);

            drawSharpText(context, "_", minimizeButtonX + 6, minimizeButtonY + 2, 0xFFFFFF, false, 1.5f);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (isMinimized) {
                int x = this.width - MINI_WIDTH - MINI_MARGIN;
                int y = MINI_MARGIN;
                int maxButtonX = x + MINI_WIDTH - 25;
                int maxButtonY = y + 5;
                int maxButtonSize = 18;

                if (mouseX >= maxButtonX && mouseX <= maxButtonX + maxButtonSize &&
                        mouseY >= maxButtonY && mouseY <= maxButtonY + maxButtonSize) {
                    isMinimized = false;
                    RetrofitOverlayRenderer.deactivate();
                    MinecraftClient.getInstance().setScreen(this);
                    return true;
                }
            } else if (!isComplete) {
                if (mouseX >= minimizeButtonX && mouseX <= minimizeButtonX + minimizeButtonSize &&
                        mouseY >= minimizeButtonY && mouseY <= minimizeButtonY + minimizeButtonSize) {
                    isMinimized = true;
                    RetrofitOverlayRenderer.activate();
                    RetrofitOverlayRenderer.updateProgress(processedChunks, totalChunks, currentDimension);
                    MinecraftClient.getInstance().setScreen(null);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void checkAutoMaximize() {
        if (isMinimized && totalChunks > 0 && !hasAutoMaximized) {
            int percentage = (processedChunks * 100) / totalChunks;
            if (percentage >= 95) {
                EmeraldMod.LOGGER.info("[RetrofitScreen] Auto-maximizing at 95%");
                isMinimized = false;
                hasAutoMaximized = true;

                RetrofitOverlayRenderer.deactivate();

                MinecraftClient client = MinecraftClient.getInstance();
                if (client != null) {
                    client.execute(() -> {
                        client.setScreen(this);
                    });
                }
            }
        }
    }

    public void setMinimized(boolean minimized) {
        this.isMinimized = minimized;
    }

    public boolean isMinimized() {
        return this.isMinimized;
    }

    private void drawSharpText(DrawContext context, String text, int x, int y, int color, boolean centered, float scale) {
        context.getMatrices().push();

        if (centered) {
            int textWidth = (int) (this.textRenderer.getWidth(text) * scale);
            x = x - textWidth / 2;
        }

        if (scale != 1.0f) {
            context.getMatrices().scale(scale, scale, 1.0f);
            x = (int) (x / scale);
            y = (int) (y / scale);
        }

        context.drawText(this.textRenderer, text, x, y, color, false);

        context.getMatrices().pop();
    }

    private int getProgressColor(int percentage) {
        if (percentage < 30) {
            return 0xFFFF4444;
        } else if (percentage < 70) {
            return 0xFFFFBB33;
        } else if (percentage < 95) {
            return 0xFF44FF44;
        } else {
            return 0xFF00FFFF;
        }
    }

    private String formatTime(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        } else {
            long minutes = seconds / 60;
            long secs = seconds % 60;
            return minutes + "m " + secs + "s";
        }
    }

    @Override
    public boolean shouldPause() {
        return !isMinimized;
    }

    @Override
    public void close() {
        if (isComplete) {
            RetrofitOverlayRenderer.deactivate();
            super.close();
        } else {
            isMinimized = true;
            RetrofitOverlayRenderer.activate();
            RetrofitOverlayRenderer.updateProgress(processedChunks, totalChunks, currentDimension);
            MinecraftClient.getInstance().setScreen(null);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        if (!isComplete) {
            isMinimized = true;
            RetrofitOverlayRenderer.activate();
            RetrofitOverlayRenderer.updateProgress(processedChunks, totalChunks, currentDimension);
            MinecraftClient.getInstance().setScreen(null);
            return false;
        }
        return true;
    }
}