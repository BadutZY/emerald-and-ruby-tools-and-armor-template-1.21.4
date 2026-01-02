package com.example.emeraldmod.client;

import com.example.emeraldmod.EmeraldMod;
import com.example.emeraldmod.network.RetrofitDecisionPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Screen konfirmasi sebelum memulai retrofit
 * Memberikan pilihan YES/NO kepada player
 */
@Environment(EnvType.CLIENT)
public class RetrofitConfirmationScreen extends Screen {

    private static RetrofitConfirmationScreen instance;
    private ButtonWidget yesButton;
    private ButtonWidget noButton;

    public RetrofitConfirmationScreen() {
        super(Text.literal("Ruby Ore Generation"));
    }

    public static RetrofitConfirmationScreen getInstance() {
        if (instance == null) {
            instance = new RetrofitConfirmationScreen();
        }
        return instance;
    }

    public static void show() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            RetrofitConfirmationScreen screen = getInstance();
            client.execute(() -> {
                EmeraldMod.LOGGER.info("[RetrofitConfirmation] Showing confirmation screen");
                client.setScreen(screen);
            });
        }
    }

    public static void hide() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.currentScreen instanceof RetrofitConfirmationScreen) {
            client.execute(() -> client.setScreen(null));
        }
        instance = null;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // YES Button (Green)
        yesButton = ButtonWidget.builder(Text.literal("YES"), button -> {
                    onYesPressed();
                })
                .dimensions(centerX - 105, centerY + 40, 100, 30)
                .build();

        // NO Button (Red)
        noButton = ButtonWidget.builder(Text.literal("NO"), button -> {
                    onNoPressed();
                })
                .dimensions(centerX + 5, centerY + 40, 100, 30)
                .build();

        addDrawableChild(yesButton);
        addDrawableChild(noButton);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Dark background
        context.fill(0, 0, this.width, this.height, 0xFF1a1a1a);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Title with icon
        drawCenteredText(context, "💎 Ruby Ore Generation", centerX, centerY - 80, 0xFFAA00, 2.0f);

        // Main question
        drawCenteredText(context, "Do you want to generate ruby ores now?",
                centerX, centerY - 40, 0xFFFFFF, 1.5f);

        // Info text
        drawCenteredText(context, "This will add Ruby Ores to your existing world",
                centerX, centerY - 10, 0xCCCCCC, 1.0f);

        drawCenteredText(context, "Including Overworld and Nether dimensions",
                centerX, centerY + 10, 0xCCCCCC, 1.0f);

        // Render buttons
        super.render(context, mouseX, mouseY, delta);

        // Button descriptions below buttons
        drawCenteredText(context, "Start generation now",
                centerX - 55, centerY + 75, 0x55FF55, 0.8f);
        drawCenteredText(context, "Remind me later",
                centerX + 55, centerY + 75, 0xFF5555, 0.8f);
    }

    private void onYesPressed() {
        EmeraldMod.LOGGER.info("[RetrofitConfirmation] User chose YES - Starting retrofit");

        // Send YES decision to server
        RetrofitDecisionPacket.sendDecision(true);

        // Close confirmation screen
        this.close();

        // Server will send show loading packet back to client
    }

    private void onNoPressed() {
        EmeraldMod.LOGGER.info("[RetrofitConfirmation] User chose NO - Showing reminder widget");

        // Send NO decision to server
        RetrofitDecisionPacket.sendDecision(false);

        // Close confirmation screen
        this.close();

        // Show reminder widget
        RetrofitReminderWidget.show();
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
        // ESC acts as NO
        onNoPressed();
        return false;
    }
}