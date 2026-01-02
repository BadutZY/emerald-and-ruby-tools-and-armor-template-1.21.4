package com.example.emeraldmod.client;

import com.example.emeraldmod.EmeraldMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/**
 * Widget reminder di pojok kanan atas
 * - Press N to generate
 * - Press J to hide/show (TRUE TOGGLE)
 */
@Environment(EnvType.CLIENT)
public class RetrofitReminderWidget {

    private static boolean isActive = false;
    private static boolean isTemporarilyHidden = false;

    private static final int WIDGET_WIDTH = 320;
    private static final int WIDGET_HEIGHT = 80;
    private static final int MARGIN = 10;

    public static void register() {
        HudRenderCallback.EVENT.register(RetrofitReminderWidget::render);
        EmeraldMod.LOGGER.info("✅ Registered Retrofit Reminder Widget");
    }

    /**
     * Show widget (reset hidden state)
     */
    public static void show() {
        isActive = true;
        isTemporarilyHidden = false; // Reset ketika show
        EmeraldMod.LOGGER.info("[RetrofitReminder] Widget activated - Press N to generate, J to toggle hide");
    }

    /**
     * Hide widget permanently
     */
    public static void hide() {
        isActive = false;
        isTemporarilyHidden = false; // Reset state
        EmeraldMod.LOGGER.info("[RetrofitReminder] Widget deactivated");
    }

    /**
     * Check if widget is active
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
            EmeraldMod.LOGGER.info("[RetrofitReminder] Temporarily {} (J key)",
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
     * Render widget
     */
    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        // 🔧 Don't render if: not active OR temporarily hidden
        if (!isActive || isTemporarilyHidden) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }

        int screenWidth = client.getWindow().getScaledWidth();

        // Position at top-right corner
        int x = screenWidth - WIDGET_WIDTH - MARGIN;
        int y = MARGIN;

        // Border (orange glow)
        context.fill(x - 2, y - 2, x + WIDGET_WIDTH + 2, y + WIDGET_HEIGHT + 2, 0xFFFFAA00);

        // Background (dark)
        context.fill(x, y, x + WIDGET_WIDTH, y + WIDGET_HEIGHT, 0xEE1a1a1a);

        // Title/Question
        drawText(context, "Do you want to generate ruby ores now?",
                x + 8, y + 8, 0xFFFFFF, 0.85f);

        // Get keybind names
        String generateKey = "N";
        String hideKey = "J";

        try {
            generateKey = RetrofitKeybind.getGenerateNowKey().getBoundKeyLocalizedText().getString();
            hideKey = RetrofitKeybind.getToggleHideKey().getBoundKeyLocalizedText().getString();
        } catch (Exception e) {
            // Use defaults
        }

        // Instruction: Generate
        String generateInstruction = "Press [" + generateKey + "] to generate";
        int textWidth = (int)(client.textRenderer.getWidth(generateInstruction) * 0.9f);
        int centerX = x + (WIDGET_WIDTH - textWidth) / 2;
        drawText(context, generateInstruction, centerX, y + 38, 0xFFFF00, 0.9f);

        // Instruction: Hide/Show toggle
        String hideInstruction = "Press [" + hideKey + "] to hide";
        int hideTextWidth = (int)(client.textRenderer.getWidth(hideInstruction) * 0.75f);
        int hideCenterX = x + (WIDGET_WIDTH - hideTextWidth) / 2;
        drawText(context, hideInstruction, hideCenterX, y + 58, 0xAAAAAA, 0.75f);
    }

    private static void drawText(DrawContext context, String text, int x, int y, int color, float scale) {
        MinecraftClient client = MinecraftClient.getInstance();

        context.getMatrices().push();

        if (scale != 1.0f) {
            context.getMatrices().scale(scale, scale, 1.0f);
            x = (int) (x / scale);
            y = (int) (y / scale);
        }

        context.drawText(client.textRenderer, text, x, y, color, true);

        context.getMatrices().pop();
    }
}