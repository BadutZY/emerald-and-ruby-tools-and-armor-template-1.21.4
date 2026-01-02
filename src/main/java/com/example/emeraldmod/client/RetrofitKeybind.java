package com.example.emeraldmod.client;

import com.example.emeraldmod.EmeraldMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Keybinds untuk retrofit system:
 * - M: Maximize loading screen (saat minimized)
 * - N: Generate now (saat widget reminder active)
 * - J: Toggle hide/show widgets (TRUE TOGGLE)
 */
@Environment(EnvType.CLIENT)
public class RetrofitKeybind {

    private static KeyBinding maximizeKey;
    private static KeyBinding generateNowKey;
    private static KeyBinding toggleHideKey;

    public static void register() {
        // Keybind: Maximize loading screen (Default: M)
        maximizeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.emeraldmod.maximize_retrofit",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                "category.emeraldmod.retrofit"
        ));

        // Keybind: Generate now (Default: N)
        generateNowKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.emeraldmod.generate_now",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_N,
                "category.emeraldmod.retrofit"
        ));

        // Keybind: Toggle hide widgets (Default: J)
        toggleHideKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.emeraldmod.toggle_hide_widget",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                "category.emeraldmod.retrofit"
        ));

        // Register tick event to check keybinds
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Check maximize key (M)
            if (maximizeKey.wasPressed() && RetrofitOverlayRenderer.isActive()) {
                maximizeRetrofitScreen(client);
            }

            // Check generate now key (N)
            if (generateNowKey.wasPressed() && RetrofitReminderWidget.isActive()) {
                generateNowPressed(client);
            }

            // Check toggle hide key (J)
            if (toggleHideKey.wasPressed()) {
                toggleHideWidgets(client);
            }
        });

        EmeraldMod.LOGGER.info("✅ Registered Retrofit Keybinds (M, N, J)");
    }

    /**
     * Maximize retrofit loading screen (M key)
     */
    private static void maximizeRetrofitScreen(MinecraftClient client) {
        EmeraldMod.LOGGER.info("[RetrofitKeybind] Maximize triggered");

        // Deactivate overlay
        RetrofitOverlayRenderer.deactivate();

        // Show fullscreen
        client.execute(() -> {
            RetrofitLoadingScreen screen = RetrofitLoadingScreen.getInstance();
            if (screen != null) {
                screen.setMinimized(false);
                client.setScreen(screen);
            }
        });
    }

    /**
     * Generate now pressed (N key - from reminder widget)
     */
    private static void generateNowPressed(MinecraftClient client) {
        EmeraldMod.LOGGER.info("[RetrofitKeybind] Generate now triggered");

        // Hide widget
        RetrofitReminderWidget.hide();

        // Show confirmation screen again
        client.execute(() -> {
            RetrofitConfirmationScreen.show();
        });
    }

    /**
     * 🔧 FIXED: True toggle hide/show widgets (J key)
     * Press J → Hide
     * Press J → Show
     * Press J → Hide
     * (and so on...)
     */
    private static void toggleHideWidgets(MinecraftClient client) {
        // Check current state dari widgets
        boolean reminderActive = RetrofitReminderWidget.isActive();
        boolean overlayActive = RetrofitOverlayRenderer.isActive();

        // 🔧 FIX: Check if CURRENTLY HIDDEN (bukan check if active)
        boolean reminderHidden = RetrofitReminderWidget.isTemporarilyHidden();
        boolean overlayHidden = RetrofitOverlayRenderer.isTemporarilyHidden();

        // Determine action based on CURRENT visibility
        boolean shouldHide = false;

        if (reminderActive && !reminderHidden) {
            // Reminder is visible → should hide
            shouldHide = true;
        } else if (overlayActive && !overlayHidden) {
            // Overlay is visible → should hide
            shouldHide = true;
        } else if (reminderActive && reminderHidden) {
            // Reminder is hidden → should show
            shouldHide = false;
        } else if (overlayActive && overlayHidden) {
            // Overlay is hidden → should show
            shouldHide = false;
        }

        if (shouldHide) {
            // Currently VISIBLE → HIDE IT
            EmeraldMod.LOGGER.info("[RetrofitKeybind] J pressed - HIDING widgets");

            if (reminderActive) {
                RetrofitReminderWidget.setTemporarilyHidden(true);
            }

            if (overlayActive) {
                RetrofitOverlayRenderer.setTemporarilyHidden(true);
            }
        } else {
            // Currently HIDDEN → SHOW IT
            EmeraldMod.LOGGER.info("[RetrofitKeybind] J pressed - SHOWING widgets");

            if (reminderActive) {
                RetrofitReminderWidget.setTemporarilyHidden(false);
            }

            if (overlayActive) {
                RetrofitOverlayRenderer.setTemporarilyHidden(false);
            }
        }
    }

    // ============================================
    // GETTERS
    // ============================================

    public static KeyBinding getMaximizeKey() {
        return maximizeKey;
    }

    public static KeyBinding getGenerateNowKey() {
        return generateNowKey;
    }

    public static KeyBinding getToggleHideKey() {
        return toggleHideKey;
    }
}