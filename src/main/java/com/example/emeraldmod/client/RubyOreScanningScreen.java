package com.example.emeraldmod.client;

import com.example.emeraldmod.EmeraldMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Screen untuk scanning ruby ores di world
 * Muncul pertama kali saat join world untuk detect apakah world punya ruby ores
 */
@Environment(EnvType.CLIENT)
public class RubyOreScanningScreen extends Screen {

    private static RubyOreScanningScreen instance;
    private boolean isScanning = true;
    private boolean hasOres = false;
    private String statusMessage = "Initializing...";
    private int dotsAnimation = 0;
    private long lastDotUpdate = System.currentTimeMillis();

    public RubyOreScanningScreen() {
        super(Text.literal("Ruby Ore Scanner"));
    }

    public static RubyOreScanningScreen getInstance() {
        if (instance == null) {
            instance = new RubyOreScanningScreen();
        }
        return instance;
    }

    public static void show() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            RubyOreScanningScreen screen = getInstance();
            screen.reset(); // Reset state
            client.execute(() -> {
                EmeraldMod.LOGGER.info("[ScanningScreen] Showing scanning screen");
                client.setScreen(screen);
            });
        }
    }

    public static void hide() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.currentScreen instanceof RubyOreScanningScreen) {
            client.execute(() -> client.setScreen(null));
        }
        instance = null;
    }

    /**
     * Update scanning status
     */
    public static void updateStatus(String message) {
        if (instance != null) {
            instance.statusMessage = message;
        }
    }

    /**
     * Mark scanning as complete dengan result
     */
    public static void setScanComplete(boolean foundOres) {
        if (instance != null) {
            instance.isScanning = false;
            instance.hasOres = foundOres;

            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                if (foundOres) {
                    // Found ores - auto close after 2 seconds
                    EmeraldMod.LOGGER.info("[ScanningScreen] Found ores - will auto-close");
                    new Thread(() -> {
                        try {
                            Thread.sleep(2000);
                            client.execute(() -> {
                                hide();
                                EmeraldMod.LOGGER.info("[ScanningScreen] Auto-closed after found ores");
                            });
                        } catch (InterruptedException e) {
                            // Ignore
                        }
                    }).start();
                } else {
                    // No ores - show confirmation screen after 1.5 seconds
                    EmeraldMod.LOGGER.info("[ScanningScreen] No ores found - showing confirmation");
                    new Thread(() -> {
                        try {
                            Thread.sleep(1500);
                            client.execute(() -> {
                                hide();
                                RetrofitConfirmationScreen.show();
                                EmeraldMod.LOGGER.info("[ScanningScreen] Showing confirmation dialog");
                            });
                        } catch (InterruptedException e) {
                            // Ignore
                        }
                    }).start();
                }
            }
        }
    }

    /**
     * Reset screen state
     */
    private void reset() {
        isScanning = true;
        hasOres = false;
        statusMessage = "Initializing...";
        dotsAnimation = 0;
        lastDotUpdate = System.currentTimeMillis();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Update animation
        if (System.currentTimeMillis() - lastDotUpdate > 500) {
            dotsAnimation = (dotsAnimation + 1) % 4;
            lastDotUpdate = System.currentTimeMillis();
        }

        // Dark background
        context.fill(0, 0, this.width, this.height, 0xFF1a1a1a);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        if (isScanning) {
            // Title
            drawCenteredText(context, "🔍 Scanning for Ruby Ores",
                    centerX, centerY - 60, 0xFFAA00, 2.0f);

            // Status with animated dots
            String dots = ".".repeat(dotsAnimation);
            String status = statusMessage + dots;
            drawCenteredText(context, status,
                    centerX, centerY - 20, 0xFFFFFF, 1.2f);

            // Info text
            drawCenteredText(context, "Please wait, checking your world...",
                    centerX, centerY + 20, 0xAAAAAA, 1.0f);

            // Animated scanning bar
            renderScanningBar(context, centerX, centerY + 50);

        } else {
            if (hasOres) {
                // Found ores - success message
                drawCenteredText(context, "✅ Ruby Ores Found!",
                        centerX, centerY - 40, 0x00FF00, 2.0f);

                drawCenteredText(context, "Your world already has Ruby Ores",
                        centerX, centerY - 5, 0xFFFFFF, 1.2f);

                drawCenteredText(context, "No generation needed!",
                        centerX, centerY + 20, 0x00FF00, 1.0f);

                drawCenteredText(context, "Closing in a moment...",
                        centerX, centerY + 50, 0xAAAAAA, 0.9f);

            } else {
                // No ores found
                drawCenteredText(context, "❌ No Ruby Ores Detected",
                        centerX, centerY - 40, 0xFF5555, 2.0f);

                drawCenteredText(context, "Your world needs Ruby Ore generation",
                        centerX, centerY - 5, 0xFFFFFF, 1.2f);

                drawCenteredText(context, "Opening generation options...",
                        centerX, centerY + 20, 0xFFAA00, 1.0f);
            }
        }
    }

    /**
     * Render animated scanning bar
     */
    private void renderScanningBar(DrawContext context, int centerX, int centerY) {
        int barWidth = 300;
        int barHeight = 4;
        int barX = centerX - barWidth / 2;

        // Background
        context.fill(barX, centerY, barX + barWidth, centerY + barHeight, 0xFF333333);

        // Animated moving bar
        int animWidth = 80;
        int animPos = (int) ((System.currentTimeMillis() / 10) % (barWidth + animWidth)) - animWidth;

        if (animPos >= 0 && animPos < barWidth) {
            int startX = barX + animPos;
            int endX = Math.min(barX + animPos + animWidth, barX + barWidth);
            context.fill(startX, centerY, endX, centerY + barHeight, 0xFFFFAA00);
        }
    }

    private void drawCenteredText(DrawContext context, String text, int x, int y, int color, float scale) {
        context.getMatrices().push();

        if (scale != 1.0f) {
            context.getMatrices().scale(scale, scale, 1.0f);
            x = (int) (x / scale);
            y = (int) (y / scale);
        }

        int textWidth = this.textRenderer.getWidth(text);
        x = x - textWidth / 2;

        context.drawText(this.textRenderer, text, x, y, color, true);

        context.getMatrices().pop();
    }

    @Override
    public boolean shouldPause() {
        return true;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        // Don't allow ESC during scanning
        return !isScanning;
    }
}