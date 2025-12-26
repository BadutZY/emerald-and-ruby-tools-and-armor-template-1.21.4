package com.example.emeraldmod.client;

import com.example.emeraldmod.network.ToggleEffectPacket;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Keybind registration untuk toggle effect
 */
public class ModKeybinds {

    private static final String CATEGORY = "key.category.emeraldmod";

    // Keybind untuk toggle tools effect (default: V)
    public static KeyBinding toggleToolsKey;

    // Keybind untuk toggle armor effect (default: B)
    public static KeyBinding toggleArmorKey;

    // Track apakah key sudah dipencet (untuk prevent spam)
    private static boolean toolsKeyWasPressed = false;
    private static boolean armorKeyWasPressed = false;

    // Client-side state untuk display
    private static boolean toolsEnabled = true;
    private static boolean armorEnabled = true;

    public static void register() {
        // Register keybinds
        toggleToolsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.emeraldmod.toggle_tools",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V, // Default key: V
                CATEGORY
        ));

        toggleArmorKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.emeraldmod.toggle_armor",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B, // Default key: B
                CATEGORY
        ));

        // Register tick handler untuk detect key press
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Handle tools toggle
            if (toggleToolsKey.isPressed()) {
                if (!toolsKeyWasPressed) {
                    toolsKeyWasPressed = true;

                    // Toggle state
                    toolsEnabled = !toolsEnabled;

                    // Send packet ke server
                    ClientPlayNetworking.send(new ToggleEffectPacket(
                            ToggleEffectPacket.EffectType.TOOLS,
                            toolsEnabled
                    ));

                    // Show message
                    if (client.player != null) {
                        client.player.sendMessage(
                                Text.translatable(
                                        toolsEnabled ? "message.emeraldmod.tools_enabled" : "message.emeraldmod.tools_disabled"
                                ),
                                true // Action bar
                        );
                    }
                }
            } else {
                toolsKeyWasPressed = false;
            }

            // Handle armor toggle
            if (toggleArmorKey.isPressed()) {
                if (!armorKeyWasPressed) {
                    armorKeyWasPressed = true;

                    // Toggle state
                    armorEnabled = !armorEnabled;

                    // Send packet ke server
                    ClientPlayNetworking.send(new ToggleEffectPacket(
                            ToggleEffectPacket.EffectType.ARMOR,
                            armorEnabled
                    ));

                    // Show message
                    if (client.player != null) {
                        client.player.sendMessage(
                                Text.translatable(
                                        armorEnabled ? "message.emeraldmod.armor_enabled" : "message.emeraldmod.armor_disabled"
                                ),
                                true // Action bar
                        );
                    }
                }
            } else {
                armorKeyWasPressed = false;
            }
        });
    }

    // Getters untuk client-side display (optional)
    public static boolean isToolsEnabled() {
        return toolsEnabled;
    }

    public static boolean isArmorEnabled() {
        return armorEnabled;
    }

    // Setters untuk sync dari server (optional)
    public static void setToolsEnabled(boolean enabled) {
        toolsEnabled = enabled;
    }

    public static void setArmorEnabled(boolean enabled) {
        armorEnabled = enabled;
    }
}